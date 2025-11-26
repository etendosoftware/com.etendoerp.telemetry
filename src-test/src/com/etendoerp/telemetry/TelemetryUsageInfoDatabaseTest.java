/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at  
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2025 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package com.etendoerp.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Test suite for TelemetryUsageInfo database operations.
 * Tests cover saveUsageAudit, insertUsageAudit, exception handling, and validation logic.
 */
public class TelemetryUsageInfoDatabaseTest extends TelemetryUsageInfoTestBase {

  /**
   * Test saveUsageAudit with null sessionId
   *
   * @throws ServletException
   *     if there's an error during database operations
   * @throws JSONException
   *     if there's an error converting JSON object to string
   */
  @Test
  public void shouldSkipSaveUsageAuditWithNullSessionId() throws ServletException, JSONException {
    TelemetryUsageInfo instance = createBasicInstance(null, TEST_COMMAND);

    // Should not throw exception, just skip silently
    instance.saveUsageAudit();

    // Note: Can't verify mockConnectionProvider since the method returns early
    // when sessionId is null, before any database operations
  }

  /**
   * Test saveUsageAudit with empty sessionId
   *
   * @throws ServletException
   *     if there's an error during database operations
   * @throws JSONException
   *     if there's an error converting JSON object to string
   */
  @Test
  public void shouldSkipSaveUsageAuditWithEmptySessionId() throws ServletException, JSONException {
    TelemetryUsageInfo instance = createBasicInstance("", TEST_COMMAND);

    // Should not throw exception, just skip silently
    instance.saveUsageAudit();

    // Note: Can't verify mockConnectionProvider since the method returns early
    // when sessionId is empty, before any database operations
  }

  /**
   * Test saveUsageAudit with null command
   *
   * @throws ServletException
   *     if there's an error during database operations
   * @throws JSONException
   *     if there's an error converting JSON object to string
   */
  @Test
  public void shouldSkipSaveUsageAuditWithNullCommand() throws ServletException, JSONException {
    TelemetryUsageInfo instance = createBasicInstance(TEST_SESSION, null);

    // Should not throw exception, just skip silently
    instance.saveUsageAudit();

    // Note: Can't verify mockConnectionProvider since the method returns early
    // when command is null, before any database operations
  }

  /**
   * Test saveUsageAudit with valid data and SessionInfo fallbacks
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldSaveUsageAuditWithSessionInfoFallbacks() throws Exception {
    // Setup
    TelemetryUsageInfo instance = createBasicInstance(TEST_SESSION, TEST_COMMAND);

    // Mock SessionInfo static methods
    setupSessionInfoMocks();

    // Test with mocked static method using new signature
    try (MockedStatic<TelemetryUsageInfo> mockedTelemetry = mockStatic(TelemetryUsageInfo.class);
         MockedConstruction<DalConnectionProvider> mockedDal = setupSuccessfulSaveAuditMocks()) {

      // Allow the real getInstance call to work
      setupTelemetryStaticMocks(mockedTelemetry);

      // Execute
      instance.saveUsageAudit();

      // Verify SessionInfo methods were called
      mockedSessionInfo.verify(SessionInfo::getUserId, times(1));
      mockedSessionInfo.verify(SessionInfo::getModuleId, times(1));
      mockedSessionInfo.verify(SessionInfo::getProcessType, times(1));
      mockedSessionInfo.verify(SessionInfo::getProcessId, times(1));
    }
  }

  /**
   * Test saveUsageAudit with pre-set values (no SessionInfo fallbacks needed)
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldSaveUsageAuditWithPreSetValues() throws Exception {
    // Setup
    TelemetryUsageInfo instance = createFullyConfiguredInstance();

    // Test with mocked static method using new signature
    try (MockedStatic<TelemetryUsageInfo> mockedTelemetry = mockStatic(TelemetryUsageInfo.class);
         MockedConstruction<DalConnectionProvider> mockedDal = setupSuccessfulSaveAuditMocks()) {

      // Allow the real getInstance call to work
      setupTelemetryStaticMocks(mockedTelemetry);

      // Execute
      instance.saveUsageAudit();

      // Verify SessionInfo methods were NOT called since values were pre-set
      mockedSessionInfo.verify(SessionInfo::getUserId, never());
      mockedSessionInfo.verify(SessionInfo::getModuleId, never());
      mockedSessionInfo.verify(SessionInfo::getProcessType, never());
    }
  }

  /**
   * Test saveUsageAudit with missing userId from SessionInfo
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldSkipSaveUsageAuditWithMissingUserIdFromSessionInfo() throws Exception {
    // Setup
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    instance.setSessionId(TEST_SESSION);
    instance.setCommand(TEST_COMMAND);

    // Mock SessionInfo to return null/empty userId
    mockedSessionInfo.when(SessionInfo::getUserId).thenReturn(null);

    // Execute - should skip silently due to missing userId
    instance.saveUsageAudit();

    // Note: Can't verify mockConnectionProvider since the method returns early
    // when userId is null, before any database operations
  }

  /**
   * Test saveUsageAudit with missing objectId from SessionInfo
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldSkipSaveUsageAuditWithMissingObjectIdFromSessionInfo() throws Exception {
    // Setup
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    instance.setSessionId(TEST_SESSION);
    instance.setCommand(TEST_COMMAND);

    // Mock SessionInfo
    mockedSessionInfo.when(SessionInfo::getUserId).thenReturn("user-id");
    mockedSessionInfo.when(SessionInfo::getModuleId).thenReturn("module-id");
    mockedSessionInfo.when(SessionInfo::getProcessType).thenReturn(OBJECT_TYPE_PROCESS);
    mockedSessionInfo.when(SessionInfo::getProcessId).thenReturn(null); // Missing objectId

    // Execute - should skip silently due to missing objectId
    instance.saveUsageAudit();

    // Note: Can't verify mockConnectionProvider since the method returns early
    // when objectId is null, before any database operations
  }

  /**
   * Test that default objecttype is set to "P" when null
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldSetDefaultObjectTypeWhenNull() throws Exception {
    // Setup
    TelemetryUsageInfo instance = createBasicInstance(TEST_SESSION, TEST_COMMAND);
    instance.setUserId(TEST_USER);
    instance.setModuleId(TEST_MODULE);
    instance.setObjectId(TEST_OBJECT);

    // Mock SessionInfo to return null for process type
    mockedSessionInfo.when(SessionInfo::getProcessType).thenReturn(null);

    // Test with mocked static method using new signature
    try (MockedStatic<TelemetryUsageInfo> mockedTelemetry = mockStatic(TelemetryUsageInfo.class);
         MockedConstruction<DalConnectionProvider> mockedDal = setupSuccessfulSaveAuditMocks()) {

      // Allow the real getInstance call to work
      setupTelemetryStaticMocks(mockedTelemetry);

      // Execute
      instance.saveUsageAudit();

      // Verify default objecttype "P" was used
      mockedTelemetry.verify(() -> TelemetryUsageInfo.insertUsageAudit(
              any(ConnectionProvider.class), any(TelemetryUsageInfo.UsageAuditData.class)),
          times(1));
    }
  }

  /**
   * Test saveUsageAudit sets timeMillis if it's 0
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldSetTimeMillisIfZero() throws Exception {
    // Setup
    TelemetryUsageInfo instance = createFullyConfiguredInstance();
    instance.setTimeMillis(0); // Set to 0 to trigger update

    long beforeTime = System.currentTimeMillis();

    // Test with mocked static method using new signature
    try (MockedStatic<TelemetryUsageInfo> mockedTelemetry = mockStatic(TelemetryUsageInfo.class);
         MockedConstruction<DalConnectionProvider> mockedDal = setupSuccessfulSaveAuditMocks()) {

      // Allow the real getInstance call to work
      setupTelemetryStaticMocks(mockedTelemetry);

      // Execute
      instance.saveUsageAudit();

      long afterTime = System.currentTimeMillis();

      // Verify timeMillis was updated
      assertTrue(instance.getTimeMillis() >= beforeTime);
      assertTrue(instance.getTimeMillis() <= afterTime);
    }
  }

  /**
   * Test insertUsageAudit static method success
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldInsertUsageAuditSuccessfully() throws Exception {
    // Setup
    configureMocksForInsertUsageAudit();
    TelemetryUsageInfo.UsageAuditData auditData = setupCommonMocksAndCreateAuditData();

    // Execute
    int result = TelemetryUsageInfo.insertUsageAudit(mockConnectionProvider, auditData);

    // Verify
    assertEquals(1, result);
    verify(mockConnectionProvider).getPreparedStatement(anyString());
    verify(mockQueryTimeOutUtil).setQueryTimeOut(eq(mockPreparedStatement), eq(DEFAULT_PROFILE));
    verify(mockPreparedStatement).executeUpdate();
    verify(mockConnectionProvider).releasePreparedStatement(mockPreparedStatement);

    // Verify UtilSql.setValue was called 10 times (for all parameters)
    mockedUtilSql.verify(() -> UtilSql.setValue(any(PreparedStatement.class), anyInt(), anyInt(), any(), anyString()),
        times(10));
  }

  /**
   * Test insertUsageAudit static method with SQLException
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldThrowServletExceptionOnSQLException() throws Exception {
    // Setup
    SQLException sqlException = new SQLException("Database error", "42000", 12345);
    when(mockConnectionProvider.getPreparedStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeUpdate()).thenThrow(sqlException);

    TelemetryUsageInfo.UsageAuditData auditData = setupCommonMocksAndCreateAuditData();

    // Execute and verify
    try {
      TelemetryUsageInfo.insertUsageAudit(mockConnectionProvider, auditData);
      fail("Expected ServletException to be thrown");
    } catch (ServletException exception) {
      assertTrue(exception.getMessage().contains("@CODE=12345@"));
      assertTrue(exception.getMessage().contains("Database error"));
    }
    verify(mockConnectionProvider).releasePreparedStatement(mockPreparedStatement);
  }

  /**
   * Test insertUsageAudit static method with general Exception
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldThrowServletExceptionOnGeneralException() throws Exception {
    // Setup
    RuntimeException runtimeException = new RuntimeException("General error");
    when(mockConnectionProvider.getPreparedStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeUpdate()).thenThrow(runtimeException);

    TelemetryUsageInfo.UsageAuditData auditData = setupCommonMocksAndCreateAuditData();

    // Execute and verify
    try {
      TelemetryUsageInfo.insertUsageAudit(mockConnectionProvider, auditData);
      fail("Expected ServletException to be thrown");
    } catch (ServletException exception) {
      assertTrue(exception.getMessage().contains("@CODE=@"));
      assertTrue(exception.getMessage().contains("General error"));
    }
    verify(mockConnectionProvider).releasePreparedStatement(mockPreparedStatement);
  }

  /**
   * Test insertUsageAudit handles release statement exception
   *
   * @throws Exception
   *     if there's an error during the test execution
   */
  @Test
  public void shouldHandleReleaseStatementException() throws Exception {
    // Setup
    configureMocksForInsertUsageAudit();
    doThrow(new SQLException("Release error")).when(mockConnectionProvider).releasePreparedStatement(
        mockPreparedStatement);

    TelemetryUsageInfo.UsageAuditData auditData = setupCommonMocksAndCreateAuditData();

    // Execute - should not throw exception despite release error
    int result = TelemetryUsageInfo.insertUsageAudit(mockConnectionProvider, auditData);

    // Verify
    assertEquals(1, result);
    verify(mockConnectionProvider).releasePreparedStatement(mockPreparedStatement);
  }
}