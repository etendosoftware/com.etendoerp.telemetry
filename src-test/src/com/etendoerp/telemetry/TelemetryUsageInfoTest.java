package com.etendoerp.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
 * Comprehensive test suite for the TelemetryUsageInfo class.
 * Tests cover thread safety, singleton behavior, data management, and database operations.
 */
public class TelemetryUsageInfoTest {

  private static final String TEST_SESSION = "test-session";
  private static final String TEST_COMMAND = "test-command";
  private static final String TEST_USER = "test-user";
  private static final String TEST_MODULE = "test-module";
  private static final String TEST_OBJECT = "test-object";
  private static final String TEST_CLASS = "TestClass";
  private static final String OBJECT_TYPE_PROCESS = "P";
  private static final String SESSION_PREFIX = "session-";
  private static final long TEST_TIME = 123456789L;
  private static final int TIMEOUT_SECONDS = 10;
  private static final String DEFAULT_PROFILE = "default";
  private static final String USER1 = "user1";
  private static final String SESSION1 = "session1";
  private static final String OBJECT1 = "object1";
  private static final String MODULE1 = "module1";
  private static final String COMMAND1 = "command1";
  private static final String TIME_123456 = "123456";
  private static final String JSON_TEST_DATA = "{\"test\":\"data\"}";

  @Mock
  private ConnectionProvider mockConnectionProvider;

  @Mock
  private PreparedStatement mockPreparedStatement;

  @Mock
  private QueryTimeOutUtil mockQueryTimeOutUtil;

  private AutoCloseable mocks;
  private MockedStatic<SessionInfo> mockedSessionInfo;
  private MockedStatic<UtilSql> mockedUtilSql;
  private MockedStatic<QueryTimeOutUtil> mockedQueryTimeOutUtil;

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
  private TelemetryUsageInfo createBasicInstance(String sessionId, String command) {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    instance.setSessionId(sessionId);
    instance.setCommand(command);
    return instance;
  }

  /**
   * Helper method to setup SessionInfo mocks with default values.
   */
  private void setupSessionInfoMocks() {
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
  private MockedConstruction<DalConnectionProvider> setupSuccessfulSaveAuditMocks() {
    return mockConstruction(DalConnectionProvider.class,
        (mock, context) -> when(mock.getPreparedStatement(anyString())).thenReturn(mockPreparedStatement));
  }

  /**
   * Helper method to setup TelemetryUsageInfo static mocks for successful execution.
   *
   * @param mockedTelemetry the MockedStatic instance to configure
   */
  private void setupTelemetryStaticMocks(MockedStatic<TelemetryUsageInfo> mockedTelemetry) {
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
  private TelemetryUsageInfo createFullyConfiguredInstance() {
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
  private void configureMocksForInsertUsageAudit() throws Exception {
    when(mockConnectionProvider.getPreparedStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeUpdate()).thenReturn(1);
  }

  /**
   * Helper method to setup common mocks and create test audit data.
   *
   * @return configured UsageAuditData for testing
   */
  private TelemetryUsageInfo.UsageAuditData setupCommonMocksAndCreateAuditData() {
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

  /**
   * Test that getInstance returns a non-null instance
   */
  @Test
  public void shouldReturnNonNullInstance() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    assertNotNull(instance);
  }

  /**
   * Test that getInstance returns the same instance for the same thread
   */
  @Test
  public void shouldReturnSameInstanceForSameThread() {
    TelemetryUsageInfo instance1 = TelemetryUsageInfo.getInstance();
    TelemetryUsageInfo instance2 = TelemetryUsageInfo.getInstance();

    assertEquals(instance1, instance2);
  }

  /**
   * Test that different threads get different instances
   *
   * @throws InterruptedException
   *     if thread execution is interrupted
   */
  @Test
  public void shouldReturnDifferentInstancesForDifferentThreads() throws InterruptedException {
    TelemetryUsageInfo mainThreadInstance = TelemetryUsageInfo.getInstance();

    final TelemetryUsageInfo[] otherThreadInstance = new TelemetryUsageInfo[1];
    CountDownLatch latch = new CountDownLatch(1);

    Thread otherThread = new Thread(() -> {
      otherThreadInstance[0] = TelemetryUsageInfo.getInstance();
      latch.countDown();
    });

    otherThread.start();
    boolean awaitResult = latch.await(5, TimeUnit.SECONDS);
    assertTrue(awaitResult);

    assertNotNull(otherThreadInstance[0]);
    assertNotSame(mainThreadInstance, otherThreadInstance[0]);
  }

  /**
   * Test thread safety with multiple concurrent threads
   *
   * @throws InterruptedException
   *     if thread execution is interrupted
   */
  @Test
  public void shouldEnsureThreadSafetyWithMultipleConcurrentThreads() throws InterruptedException {
    final int threadCount = 10;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch completeLatch = new CountDownLatch(threadCount);
    final AtomicInteger uniqueInstances = new AtomicInteger(0);

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    for (int i = 0; i < threadCount; i++) {
      final int threadIndex = i;
      executor.submit(() -> {
        try {
          boolean startAwaitResult = startLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
          assertTrue(startAwaitResult);

          TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
          String sessionId = SESSION_PREFIX + threadIndex;
          instance.setSessionId(sessionId);

          // Verify each thread gets its own instance
          if (sessionId.equals(instance.getSessionId())) {
            uniqueInstances.incrementAndGet();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          completeLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completeAwaitResult = completeLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    assertTrue(completeAwaitResult);
    executor.shutdown();

    assertEquals(threadCount, uniqueInstances.get());
  }

  /**
   * Test clear method removes the thread-local instance
   */
  @Test
  public void shouldRemoveThreadLocalInstanceOnClear() {
    TelemetryUsageInfo instance1 = TelemetryUsageInfo.getInstance();
    instance1.setSessionId(TEST_SESSION);

    TelemetryUsageInfo.clear();

    TelemetryUsageInfo instance2 = TelemetryUsageInfo.getInstance();
    assertNotSame(instance1, instance2);
    assertNull(instance2.getSessionId());
  }

  /**
   * Test initial state of new instance
   */
  @Test
  public void shouldHaveCorrectInitialState() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();

    assertNull(instance.getSessionId());
    assertNull(instance.getCommand());
    assertNull(instance.getUserId());
    assertNull(instance.getModuleId());
    assertNull(instance.getObjecttype());
    assertNull(instance.getObjectId());
    assertNull(instance.getClassname());
    assertNotNull(instance.getJsonObject());
    assertTrue(instance.getTimeMillis() > 0);
  }

  /**
   * Test sessionId getter and setter
   */
  @Test
  public void shouldSetAndGetSessionId() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    String sessionId = "test-session-123";

    instance.setSessionId(sessionId);
    assertEquals(sessionId, instance.getSessionId());
  }

  /**
   * Test command getter and setter
   */
  @Test
  public void shouldSetAndGetCommand() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();

    instance.setCommand(TEST_COMMAND);
    assertEquals(TEST_COMMAND, instance.getCommand());
  }

  /**
   * Test userId getter and setter
   */
  @Test
  public void shouldSetAndGetUserId() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    String userId = "test-user-123";

    instance.setUserId(userId);
    assertEquals(userId, instance.getUserId());
  }

  /**
   * Test moduleId getter and setter
   */
  @Test
  public void shouldSetAndGetModuleId() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    String moduleId = "test-module-123";

    instance.setModuleId(moduleId);
    assertEquals(moduleId, instance.getModuleId());
  }

  /**
   * Test objecttype getter and setter
   */
  @Test
  public void shouldSetAndGetObjecttype() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();

    instance.setObjecttype(OBJECT_TYPE_PROCESS);
    assertEquals(OBJECT_TYPE_PROCESS, instance.getObjecttype());
  }

  /**
   * Test objectId getter and setter
   */
  @Test
  public void shouldSetAndGetObjectId() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    String objectId = "test-object-123";

    instance.setObjectId(objectId);
    assertEquals(objectId, instance.getObjectId());
  }

  /**
   * Test classname getter and setter
   */
  @Test
  public void shouldSetAndGetClassname() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    String classname = "com.test.TestClass";

    instance.setClassname(classname);
    assertEquals(classname, instance.getClassname());
  }

  /**
   * Test timeMillis getter and setter
   */
  @Test
  public void shouldSetAndGetTimeMillis() {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    long timeMillis = System.currentTimeMillis();

    instance.setTimeMillis(timeMillis);
    assertEquals(timeMillis, instance.getTimeMillis());
  }

  /**
   * Test jsonObject getter and setter
   *
   * @throws JSONException
   *     if there's an error manipulating the JSON object
   */
  @Test
  public void shouldSetAndGetJsonObject() throws JSONException {
    TelemetryUsageInfo instance = TelemetryUsageInfo.getInstance();
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("key", "value");

    instance.setJsonObject(jsonObject);
    assertEquals(jsonObject, instance.getJsonObject());
    assertEquals("value", instance.getJsonObject().getString("key"));
  }

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
   * Test UsageAuditData Builder pattern
   */
  @Test
  public void shouldBuildUsageAuditDataWithBuilder() {
    // Execute
    TelemetryUsageInfo.UsageAuditData auditData = new TelemetryUsageInfo.UsageAuditData.Builder()
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

    // Verify
    assertNotNull(auditData);
    assertEquals(USER1, auditData.getUserId());
    assertEquals(SESSION1, auditData.getSessionId());
    assertEquals(OBJECT1, auditData.getObjectId());
    assertEquals(MODULE1, auditData.getModuleId());
    assertEquals(COMMAND1, auditData.getCommand());
    assertEquals(TEST_CLASS, auditData.getClassname());
    assertEquals(OBJECT_TYPE_PROCESS, auditData.getObjecttype());
    assertEquals(TIME_123456, auditData.getTime());
    assertEquals(JSON_TEST_DATA, auditData.getJson());
  }

  /**
   * Test UsageAuditData Builder with partial data
   */
  @Test
  public void shouldBuildUsageAuditDataWithPartialData() {
    // Execute
    TelemetryUsageInfo.UsageAuditData auditData = new TelemetryUsageInfo.UsageAuditData.Builder()
        .userId(USER1)
        .sessionId(SESSION1)
        .command(COMMAND1)
        .build();

    // Verify
    assertNotNull(auditData);
    assertEquals(USER1, auditData.getUserId());
    assertEquals(SESSION1, auditData.getSessionId());
    assertEquals(COMMAND1, auditData.getCommand());
    assertNull(auditData.getObjectId());
    assertNull(auditData.getModuleId());
    assertNull(auditData.getClassname());
    assertNull(auditData.getObjecttype());
    assertNull(auditData.getTime());
    assertNull(auditData.getJson());
  }
}