package com.etendoerp.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * Base class for TelemetryUsageInfo tests containing common setup and helper methods.
 */
public abstract class TelemetryUsageInfoTestBase {

  protected static final String TEST_SESSION = "test-session";
  protected static final String TEST_COMMAND = "test-command";
  protected static final String TEST_USER = "test-user";
  protected static final String TEST_MODULE = "test-module";
  protected static final String TEST_OBJECT = "test-object";
  protected static final String TEST_CLASS = "TestClass";
  protected static final String OBJECT_TYPE_PROCESS = "P";
  protected static final String SESSION_PREFIX = "session-";
  protected static final long TEST_TIME = 123456789L;
  protected static final int TIMEOUT_SECONDS = 10;
  protected static final String DEFAULT_PROFILE = "default";
  protected static final String USER1 = "user1";
  protected static final String SESSION1 = "session1";
  protected static final String OBJECT1 = "object1";
  protected static final String MODULE1 = "module1";
  protected static final String COMMAND1 = "command1";
  protected static final String TIME_123456 = "123456";
  protected static final String JSON_TEST_DATA = "{\"test\":\"data\"}";

  @Mock
  protected ConnectionProvider mockConnectionProvider;

  @Mock
  protected PreparedStatement mockPreparedStatement;

  @Mock
  protected QueryTimeOutUtil mockQueryTimeOutUtil;

  protected AutoCloseable mocks;
  protected MockedStatic<SessionInfo> mockedSessionInfo;
  protected MockedStatic<UtilSql> mockedUtilSql;
  protected MockedStatic<QueryTimeOutUtil> mockedQueryTimeOutUtil;

  /**
   * Sets up test environment before each test.
   * Initializes mocks and clears thread-local instances.
   */
  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);

    // Clear any existing thread-local instances before each test
    TelemetryUsageInfo.clear();

    // Setup static mocks
    mockedSessionInfo = mockStatic(SessionInfo.class);
    mockedUtilSql = mockStatic(UtilSql.class);
    mockedQueryTimeOutUtil = mockStatic(QueryTimeOutUtil.class);

    when(QueryTimeOutUtil.getInstance()).thenReturn(mockQueryTimeOutUtil);
  }

  /**
   * Cleans up test environment after each test.
   * Closes mocks and clears thread-local instances.
   *
   * @throws Exception
   *     if cleanup fails
   */
  @After
  public void tearDown() throws Exception {
    // Clean up thread-local instances after each test
    TelemetryUsageInfo.clear();

    if (mockedSessionInfo != null) {
      mockedSessionInfo.close();
    }
    if (mockedUtilSql != null) {
      mockedUtilSql.close();
    }
    if (mockedQueryTimeOutUtil != null) {
      mockedQueryTimeOutUtil.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Helper method to create and configure a basic TelemetryUsageInfo instance.
   *
   * @param sessionId the session ID to set
   * @param command the command to set
   * @return configured TelemetryUsageInfo instance
   */
  protected TelemetryUsageInfo createBasicInstance(String sessionId, String command) {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    instance.setSessionId(sessionId);
    instance.setCommand(command);
    return instance;
  }

  /**
   * Helper method to setup SessionInfo mocks with default values.
   */
  protected void setupSessionInfoMocks() {
    mockedSessionInfo.when(SessionInfo::getUserId).thenReturn("session-user-id");
    mockedSessionInfo.when(SessionInfo::getModuleId).thenReturn("session-module-id");
    mockedSessionInfo.when(SessionInfo::getProcessType).thenReturn("session-process-type");
    mockedSessionInfo.when(SessionInfo::getProcessId).thenReturn("session-process-id");
    mockedSessionInfo.when(SessionInfo::getQueryProfile).thenReturn(DEFAULT_PROFILE);
  }

  /**
   * Helper method to setup mocks for successful saveUsageAudit execution.
   *
   * @return configured MockedConstruction for DalConnectionProvider
   */
  protected MockedConstruction<DalConnectionProvider> setupSuccessfulSaveAuditMocks() {
    return mockConstruction(DalConnectionProvider.class,
        (mock, context) -> when(mock.getPreparedStatement(anyString())).thenReturn(mockPreparedStatement));
  }

  /**
   * Helper method to setup TelemetryUsageInfo static mocks for successful execution.
   *
   * @param mockedTelemetry the MockedStatic instance to configure
   */
  protected void setupTelemetryStaticMocks(MockedStatic<TelemetryUsageInfo> mockedTelemetry) {
    mockedTelemetry.when(TelemetryUsageInfo::getInstance).thenCallRealMethod();
    mockedTelemetry.when(() -> TelemetryUsageInfo.insertUsageAudit(
            any(ConnectionProvider.class), any(TelemetryUsageInfo.UsageAuditData.class)))
        .thenReturn(1);
  }

  /**
   * Helper method to create a fully configured instance for testing.
   *
   * @return configured TelemetryUsageInfo instance with all properties set
   */
  protected TelemetryUsageInfo createFullyConfiguredInstance() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    instance.setSessionId(TEST_SESSION);
    instance.setCommand(TEST_COMMAND);
    instance.setUserId(TEST_USER);
    instance.setModuleId(TEST_MODULE);
    instance.setObjecttype(OBJECT_TYPE_PROCESS);
    instance.setObjectId(TEST_OBJECT);
    instance.setClassname(TEST_CLASS);
    instance.setTimeMillis(TEST_TIME);
    return instance;
  }

  /**
   * Helper method to configure mocks for insertUsageAudit testing.
   *
   * @throws SQLException if there's an error configuring mocks
   */
  protected void configureMocksForInsertUsageAudit() throws Exception {
    when(mockConnectionProvider.getPreparedStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeUpdate()).thenReturn(1);
  }

  /**
   * Helper method to setup common mocks and create test audit data.
   *
   * @return configured UsageAuditData for testing
   */
  protected TelemetryUsageInfo.UsageAuditData setupCommonMocksAndCreateAuditData() {
    mockedSessionInfo.when(SessionInfo::getQueryProfile).thenReturn(DEFAULT_PROFILE);

    // Mock UtilSql.setValue to do nothing
    mockedUtilSql.when(() -> UtilSql.setValue(any(PreparedStatement.class), anyInt(), anyInt(), any(), anyString()))
        .thenAnswer(invocation -> null);

    // Create UsageAuditData using Builder
    return new TelemetryUsageInfo.UsageAuditData.Builder()
        .userId(USER1)
        .sessionId(SESSION1)
        .objectId(OBJECT1)
        .moduleId(MODULE1)
        .command(COMMAND1)
        .classname(TEST_CLASS)
        .objecttype(OBJECT_TYPE_PROCESS)
        .time(TIME_123456)
        .json(JSON_TEST_DATA)
        .build();
  }
}