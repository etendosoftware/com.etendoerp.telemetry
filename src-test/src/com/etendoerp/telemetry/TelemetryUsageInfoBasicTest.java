package com.etendoerp.telemetry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

/**
 * Test suite for TelemetryUsageInfo basic functionality.
 * Tests cover thread safety, singleton behavior, data management, and state management.
 */
public class TelemetryUsageInfoBasicTest extends TelemetryUsageInfoTestBase {

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