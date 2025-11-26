# Etendo Telemetry Module

## Overview

The Etendo Telemetry Module provides a thread-safe telemetry system for tracking and auditing usage information across the Etendo ERP platform. It allows developers to capture detailed metrics about user actions, module usage, and system operations for analytics and monitoring purposes.

## Key Features

- **Thread-Safe Design**: Uses ThreadLocal pattern to ensure each thread has isolated telemetry data
- **Flexible Data Structure**: Supports JSON-based metadata for extensible telemetry information
- **Automatic Audit Trail**: Persists usage data to the database for historical analysis
- **Session Tracking**: Associates telemetry data with user sessions
- **Module-Aware**: Tracks usage per module for granular analytics

## Core Component: TelemetryUsageInfo

`TelemetryUsageInfo` is a thread-safe singleton class that manages telemetry data collection and persistence. It uses the ThreadLocal pattern to maintain separate instances per thread, preventing data corruption in multi-threaded environments.

### Thread-Local Instance Management

```java
// Get the current thread's instance
TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();

// Clear the instance when done (important for thread pools)
TelemetryUsageInfo.clear();
```

**Note**: Always call `clear()` when working with thread pools to prevent memory leaks.

## How to Extract Session ID from a Request

The session ID is crucial for telemetry tracking as it links usage data to specific user sessions. Etendo provides multiple ways to extract the session ID depending on your context:

### Method 1: Using VariablesSecureApp (Recommended for Servlets)

When working within a servlet or HTTP request handler, use `VariablesSecureApp`:

```java
import org.openbravo.base.secureApp.VariablesSecureApp;

public void trackFromServlet(HttpServletRequest request) {
  // Create VariablesSecureApp from the request
  VariablesSecureApp vars = new VariablesSecureApp(request);
  
  // Get the database session ID
  String sessionId = vars.getSessionValue("#AD_Session_ID");
  
  // Use it in telemetry
  TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
  telemetry.setSessionId(sessionId);
  // ... set other fields
  telemetry.saveUsageAudit();
}
```

### Method 2: Using SessionInfo (For Already Initialized Contexts)

If your code runs in a context where `SessionInfo` is already initialized (e.g., within secure servlets):

```java
import org.openbravo.database.SessionInfo;

public void trackFromInitializedContext() {
  // Get session ID from SessionInfo
  String sessionId = SessionInfo.getSessionId();
  
  TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
  telemetry.setSessionId(sessionId);
  // ... set other fields
  telemetry.saveUsageAudit();
}
```

### Method 3: From HttpSession Directly

For custom servlets or filters where you have direct access to the HTTP session:

```java
import javax.servlet.http.HttpSession;

public void trackFromHttpSession(HttpServletRequest request) {
  HttpSession session = request.getSession(false);
  
  if (session != null) {
    // Get the database session ID from session attributes
    String sessionId = (String) session.getAttribute("#AD_Session_ID");
    
    // Alternatively, get the HTTP session ID
    String httpSessionId = session.getId();
    
    TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
    telemetry.setSessionId(sessionId); // Use AD session ID for database tracking
    // ... set other fields
    telemetry.saveUsageAudit();
  }
}
```

### Method 4: Auto-Population (For Simple Cases)

If you don't explicitly set the session ID, `TelemetryUsageInfo` will attempt to retrieve it from `SessionInfo`:

```java
public void trackWithAutoSession() {
  TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
  // Session ID will be auto-populated from SessionInfo if not set
  telemetry.setCommand("MY_COMMAND");
  telemetry.setObjectId("MY_OBJECT_ID");
  // ... other required fields
  telemetry.saveUsageAudit(); // Will use SessionInfo.getSessionId() automatically
}
```

### Important Notes

- **AD_Session_ID vs HTTP Session ID**: 
  - `#AD_Session_ID` is the Etendo database session identifier stored in `ad_session` table
  - `HttpSession.getId()` is the servlet container's session ID
  - For telemetry, use `#AD_Session_ID` for consistency with Etendo's session tracking

- **Session Availability**: Always check if the session exists before accessing it:
  ```java
  HttpSession session = request.getSession(false); // false = don't create if doesn't exist
  if (session != null) {
    // Safe to access session
  }
  ```

- **Thread Safety**: The session ID should be set early in the request lifecycle and accessed from the same thread.

### Complete Example: Tracking in a REST Service

```java
import org.openbravo.base.secureApp.VariablesSecureApp;
import javax.servlet.http.HttpServletRequest;

public class MyRestService {
  
  public void trackAPICall(HttpServletRequest request, String apiEndpoint) {
    try {
      // Extract session ID from request
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String sessionId = vars.getSessionValue("#AD_Session_ID");
      
      // Build telemetry data
      JSONObject metadata = new JSONObject();
      metadata.put("endpoint", apiEndpoint);
      metadata.put("method", request.getMethod());
      metadata.put("timestamp", System.currentTimeMillis());
      
      // Configure telemetry
      TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
      telemetry.setSessionId(sessionId);
      telemetry.setCommand("API_CALL");
      telemetry.setObjectId(apiEndpoint);
      telemetry.setObjecttype("REST");
      telemetry.setJsonObject(metadata);
      telemetry.setClassname(MyRestService.class.getName());
      
      // Save to audit
      telemetry.saveUsageAudit();
      
    } catch (Exception e) {
      logger.error("Failed to track API call", e);
    } finally {
      TelemetryUsageInfo.clear();
    }
  }
}
```

## Usage Examples

### Example 1: Basic Usage Pattern (from TrackingUtil)

This example shows how to send telemetry data for a Copilot agent with comprehensive metadata:

```java
public static void sendUsageData(CopilotApp agent) {
  try {
    // 1. Build your telemetry data as a JSONObject
    JSONObject jsonData = buildTelemetryJson(agent);
    
    // 2. Get the current context
    OBContext context = OBContext.getOBContext();
    
    // 3. Configure the telemetry instance
    TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
    
    // 4. Set required fields
    telemetry.setModuleId(COPILOT_MODULE_ID);
    telemetry.setUserId(context.getUser().getId());
    telemetry.setObjectId(agent.getId());
    telemetry.setClassname(TrackingUtil.class.getName());
    
    // 5. Set timing and data
    telemetry.setTimeMillis(System.currentTimeMillis());
    telemetry.setJsonObject(jsonData);
    
    // 6. Persist to database
    telemetry.saveUsageAudit();
    
  } catch (Exception e) {
    // Handle errors appropriately
    logger.error("Error sending telemetry data", e);
  }
}
```

### Example 2: Tracking Command Execution Time

```java
public void trackCommandExecution(String command, String objectId) {
  try {
    // Start timing
    long startTime = System.currentTimeMillis();
    
    // Get telemetry instance
    TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
    
    // Set session and command info
    telemetry.setSessionId(getCurrentSessionId());
    telemetry.setCommand(command);
    
    // Execute your command
    executeCommand(command);
    
    // Calculate execution time
    long executionTime = System.currentTimeMillis() - startTime;
    telemetry.setTimeMillis(executionTime);
    
    // Build metadata
    JSONObject metadata = new JSONObject();
    metadata.put("execution_time_ms", executionTime);
    metadata.put("command_type", "user_action");
    metadata.put("status", "success");
    telemetry.setJsonObject(metadata);
    
    // Set additional fields
    telemetry.setObjectId(objectId);
    telemetry.setObjecttype("P"); // P for Process
    
    // Save to audit trail
    telemetry.saveUsageAudit();
    
  } catch (Exception e) {
    logger.error("Error tracking command execution", e);
  }
}
```

### Example 3: Tracking Feature Usage with Rich Metadata

```java
public void trackFeatureUsage(String featureId, Map<String, Object> featureData) {
  try {
    TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
    
    // Build rich JSON metadata
    JSONObject jsonData = new JSONObject();
    jsonData.put("feature_id", featureId);
    jsonData.put("timestamp", System.currentTimeMillis());
    
    // Add custom feature data
    for (Map.Entry<String, Object> entry : featureData.entrySet()) {
      jsonData.put(entry.getKey(), entry.getValue());
    }
    
    // Configure telemetry
    telemetry.setCommand("FEATURE_USAGE");
    telemetry.setObjectId(featureId);
    telemetry.setObjecttype("F"); // Custom type for Feature
    telemetry.setJsonObject(jsonData);
    
    // Save
    telemetry.saveUsageAudit();
    
  } catch (Exception e) {
    logger.error("Error tracking feature usage", e);
  }
}
```

## Required Fields

Before calling `saveUsageAudit()`, ensure the following fields are set:

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `sessionId` | String | User session identifier | Yes |
| `command` | String | Command or action being tracked | Yes |
| `userId` | String | User identifier (auto-populated if not set) | Yes* |
| `moduleId` | String | Module identifier (auto-populated if not set) | Yes* |
| `objectId` | String | Object/entity identifier (auto-populated if not set) | Yes* |
| `objecttype` | String | Object type (defaults to "P" if not set) | No |
| `classname` | String | Java class performing the action | No |
| `timeMillis` | long | Time in milliseconds (auto-set if 0) | No |
| `jsonObject` | JSONObject | Custom metadata in JSON format | No |

\* These fields will be auto-populated from `SessionInfo` if not explicitly set, but validation will fail if they remain empty.

## Field Auto-Population

The `saveUsageAudit()` method automatically populates missing fields from `SessionInfo`:

- If `userId` is not set, it uses `SessionInfo.getUserId()`
- If `moduleId` is not set, it uses `SessionInfo.getModuleId()`
- If `objecttype` is not set, it uses `SessionInfo.getProcessType()` (defaults to "P")
- If `objectId` is not set, it uses `SessionInfo.getProcessId()`
- If `timeMillis` is 0, it sets the current time

## Error Handling

The `saveUsageAudit()` method validates all required fields before insertion:

```java
try {
  telemetry.saveUsageAudit();
} catch (ServletException e) {
  // Database error occurred
  logger.error("Failed to save usage audit", e);
} catch (JSONException e) {
  // JSON formatting error
  logger.error("Invalid JSON data", e);
}
```

If any required field is missing or empty, the method logs an error and returns without inserting data.

## Best Practices

### 1. Always Clear Thread-Local Instance

When working with thread pools or long-running threads:

```java
try {
  TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
  // ... use telemetry
  telemetry.saveUsageAudit();
} finally {
  TelemetryUsageInfo.clear(); // Prevent memory leaks
}
```

### 2. Set Session ID Early

Set the session ID at the start of your request/operation:

```java
TelemetryUsageInfo.getInstance().setSessionId(currentSessionId);
TelemetryUsageInfo.getInstance().setCommand(commandName);
```

### 3. Use Meaningful Command Names

Use descriptive, consistent command names:

```java
// Good
telemetry.setCommand("AGENT_EXECUTION");
telemetry.setCommand("REPORT_GENERATION");

// Avoid
telemetry.setCommand("action");
telemetry.setCommand("doSomething");
```

### 4. Structure JSON Data Consistently

Create a consistent JSON structure for similar operations:

```java
JSONObject data = new JSONObject();
data.put("operation_type", "agent_execution");
data.put("agent_name", agentName);
data.put("agent_id", agentId);
data.put("model", modelInfo);
data.put("execution_time_ms", executionTime);
```

### 5. Fail Silently in Production

Telemetry should never break your application:

```java
try {
  TelemetryUsageInfo.getInstance().saveUsageAudit();
} catch (Exception e) {
  // Log but don't propagate
  logger.error("Telemetry failed", e);
}
```

## Database Schema

Telemetry data is stored in the `ad_session_usage_audit` table with the following structure:

- `ad_session_usage_audit_id`: Primary key (UUID)
- `ad_session_id`: Session identifier
- `object_id`: Object/entity identifier
- `ad_module_id`: Module identifier
- `command`: Command/action name
- `classname`: Java class name
- `object_type`: Type of object
- `process_time`: Execution time in milliseconds
- `em_ettelem_jsondata`: JSON metadata

## Advanced Usage: Custom Telemetry Wrapper

For complex applications, consider creating a wrapper class:

```java
public class MyAppTelemetry {
  private static final String MODULE_ID = "MY_MODULE_ID";
  
  public static void trackAgentUsage(String agentId, JSONObject metadata) {
    try {
      TelemetryUsageInfo telemetry = TelemetryUsageInfo.getInstance();
      telemetry.setModuleId(MODULE_ID);
      telemetry.setCommand("AGENT_USAGE");
      telemetry.setObjectId(agentId);
      telemetry.setObjecttype("AGENT");
      telemetry.setClassname(MyAppTelemetry.class.getName());
      telemetry.setJsonObject(metadata);
      telemetry.saveUsageAudit();
    } catch (Exception e) {
      logger.error("Failed to track agent usage", e);
    } finally {
      TelemetryUsageInfo.clear();
    }
  }
}
```

## Troubleshooting

### Data Not Being Saved

Check that all required fields are set:

```java
// Enable debug logging
if (logger.isDebugEnabled()) {
  logger.debug("SessionId: " + telemetry.getSessionId());
  logger.debug("Command: " + telemetry.getCommand());
  logger.debug("UserId: " + telemetry.getUserId());
  logger.debug("ModuleId: " + telemetry.getModuleId());
  logger.debug("ObjectId: " + telemetry.getObjectId());
}
```

### Memory Leaks in Thread Pools

Always call `TelemetryUsageInfo.clear()` after use:

```java
try {
  // Use telemetry
} finally {
  TelemetryUsageInfo.clear();
}
```

### JSON Formatting Errors

Validate JSON before setting:

```java
try {
  JSONObject data = new JSONObject();
  data.put("key", value);
  telemetry.setJsonObject(data);
} catch (JSONException e) {
  logger.error("Invalid JSON", e);
}
```

## License

This module is part of Etendo ERP and follows the Etendo license terms.

## Support

For issues or questions, please contact Etendo support or visit the Etendo documentation.
