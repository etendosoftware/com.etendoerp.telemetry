# Telemetry Module Tests

This directory contains comprehensive unit tests for the Telemetry module.

## Test Coverage

### TelemetryUsageInfoTest

Provides extensive test coverage for the `TelemetryUsageInfo` class, including:

#### Thread Safety Tests
- **shouldReturnNonNullInstance**: Verifies getInstance() returns a non-null instance
- **shouldReturnSameInstanceForSameThread**: Ensures singleton behavior within a thread
- **shouldReturnDifferentInstancesForDifferentThreads**: Confirms thread isolation
- **shouldEnsureThreadSafetyWithMultipleConcurrentThreads**: Tests concurrent access with multiple threads
- **shouldRemoveThreadLocalInstanceOnClear**: Validates clear() functionality

#### Basic Functionality Tests
- **shouldHaveCorrectInitialState**: Verifies initial state of new instances
- **shouldSetAndGet[Property]**: Tests all getter/setter pairs for all properties:
  - sessionId
  - command
  - userId
  - moduleId
  - objecttype
  - objectId
  - classname
  - timeMillis
  - jsonObject

#### Database Operations Tests
- **shouldSkipSaveUsageAuditWithNullSessionId**: Validates handling of null session ID
- **shouldSkipSaveUsageAuditWithEmptySessionId**: Validates handling of empty session ID
- **shouldSkipSaveUsageAuditWithNullCommand**: Validates handling of null command
- **shouldSaveUsageAuditWithSessionInfoFallbacks**: Tests fallback to SessionInfo values
- **shouldSaveUsageAuditWithPreSetValues**: Tests with pre-configured values
- **shouldInsertUsageAuditSuccessfully**: Tests successful database insertion
- **shouldThrowServletExceptionOnSQLException**: Tests SQL exception handling
- **shouldThrowServletExceptionOnGeneralException**: Tests general exception handling
- **shouldHandleReleaseStatementException**: Tests connection cleanup exception handling

#### Edge Cases
- **shouldSkipSaveUsageAuditWithMissingUserIdFromSessionInfo**: Tests missing user ID
- **shouldSkipSaveUsageAuditWithMissingObjectIdFromSessionInfo**: Tests missing object ID
- **shouldSetDefaultObjectTypeWhenNull**: Tests default object type assignment
- **shouldSetTimeMillisIfZero**: Tests automatic timestamp setting

## Running Tests

To run the tests, use the following Gradle command from the module root:

```bash
./gradlew test
```

## Test Dependencies

The tests use the following frameworks:
- **JUnit 5**: Main testing framework
- **Mockito**: Mocking framework for dependencies
- **AssertJ**: Additional assertions (if needed)

## Code Coverage

The test suite provides comprehensive coverage of:
- All public methods in TelemetryUsageInfo
- Thread safety scenarios
- Error handling paths
- Database interaction edge cases
- Validation logic

## Test Patterns

The tests follow these patterns:
- Descriptive method names using "should" prefix
- Clear Given/When/Then structure in comments
- Proper mock setup and teardown
- Thread-local cleanup between tests
- Constant usage to avoid string duplication
- Comprehensive exception testing