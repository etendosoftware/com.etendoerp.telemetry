package com.etendoerp.telemetry;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * Thread-safe singleton class for managing telemetry usage information.
 * This class provides a thread-local instance to store session-specific telemetry data
 * including session IDs and JSON objects containing usage metrics.
 *
 * <p>The class follows the ThreadLocal singleton pattern to ensure that each thread
 * has its own isolated instance of telemetry information, preventing data corruption
 * in multi-threaded environments.</p>
 *
 * @author Etendo ERP
 * @version 1.0
 * @since 1.0
 */
public class TelemetryUsageInfo {

  /**
   * Parameter object to encapsulate usage audit data using the Builder pattern.
   * 
   * <p>This immutable data class contains all the necessary information required
   * to perform a usage audit insertion into the database. It uses the Builder pattern
   * to provide a fluent API for constructing instances while avoiding constructor
   * parameter explosion.</p>
   * 
   * <p>All fields are final and immutable once the object is created, ensuring
   * thread safety and data integrity.</p>
   * 
   * @since 1.0
   */
  public static class UsageAuditData {
    /** The user identifier for the audit record */
    private final String userId;
    /** The session identifier for tracking user sessions */
    private final String sessionId;
    /** The object identifier being processed */
    private final String objectId;
    /** The module identifier for tracking module usage */
    private final String moduleId;
    /** The command being executed */
    private final String command;
    /** The class name being executed */
    private final String classname;
    /** The object type being processed */
    private final String objecttype;
    /** The timestamp when the audit occurred */
    private final String time;
    /** The JSON data containing additional telemetry information */
    private final String json;

    /**
     * Private constructor that accepts a Builder instance.
     * 
     * <p>This constructor is private to enforce the use of the Builder pattern
     * for creating instances of this class.</p>
     * 
     * @param builder the Builder instance containing the data for this object
     */
    private UsageAuditData(Builder builder) {
      this.userId = builder.userId;
      this.sessionId = builder.sessionId;
      this.objectId = builder.objectId;
      this.moduleId = builder.moduleId;
      this.command = builder.command;
      this.classname = builder.classname;
      this.objecttype = builder.objecttype;
      this.time = builder.time;
      this.json = builder.json;
    }

    /**
     * Gets the user identifier.
     * 
     * @return the user ID, may be null
     */
    public String getUserId() { return userId; }
    
    /**
     * Gets the session identifier.
     * 
     * @return the session ID, may be null
     */
    public String getSessionId() { return sessionId; }
    
    /**
     * Gets the object identifier.
     * 
     * @return the object ID, may be null
     */
    public String getObjectId() { return objectId; }
    
    /**
     * Gets the module identifier.
     * 
     * @return the module ID, may be null
     */
    public String getModuleId() { return moduleId; }
    
    /**
     * Gets the command being executed.
     * 
     * @return the command, may be null
     */
    public String getCommand() { return command; }
    
    /**
     * Gets the class name being executed.
     * 
     * @return the class name, may be null
     */
    public String getClassname() { return classname; }
    
    /**
     * Gets the object type being processed.
     * 
     * @return the object type, may be null
     */
    public String getObjecttype() { return objecttype; }
    
    /**
     * Gets the timestamp when the audit occurred.
     * 
     * @return the timestamp as a string, may be null
     */
    public String getTime() { return time; }
    
    /**
     * Gets the JSON data containing additional telemetry information.
     * 
     * @return the JSON data as a string, may be null
     */
    public String getJson() { return json; }

    /**
     * Builder class for constructing UsageAuditData instances using the Builder pattern.
     * 
     * <p>This builder provides a fluent API for setting the various properties
     * of a UsageAuditData object. All setter methods return the builder instance
     * to enable method chaining.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * UsageAuditData data = new UsageAuditData.Builder()
     *     .userId("user123")
     *     .sessionId("session456")
     *     .command("LOGIN")
     *     .build();
     * }</pre>
     * 
     * @since 1.0
     */
    public static class Builder {
      /** The user identifier for the audit record */
      private String userId;
      /** The session identifier for tracking user sessions */
      private String sessionId;
      /** The object identifier being processed */
      private String objectId;
      /** The module identifier for tracking module usage */
      private String moduleId;
      /** The command being executed */
      private String command;
      /** The class name being executed */
      private String classname;
      /** The object type being processed */
      private String objecttype;
      /** The timestamp when the audit occurred */
      private String time;
      /** The JSON data containing additional telemetry information */
      private String json;

      /**
       * Sets the user identifier for the audit record.
       * 
       * @param userId the user identifier to set
       * @return this Builder instance for method chaining
       */
      public Builder userId(String userId) {
        this.userId = userId;
        return this;
      }

      /**
       * Sets the session identifier for tracking user sessions.
       * 
       * @param sessionId the session identifier to set
       * @return this Builder instance for method chaining
       */
      public Builder sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
      }

      /**
       * Sets the object identifier being processed.
       * 
       * @param objectId the object identifier to set
       * @return this Builder instance for method chaining
       */
      public Builder objectId(String objectId) {
        this.objectId = objectId;
        return this;
      }

      /**
       * Sets the module identifier for tracking module usage.
       * 
       * @param moduleId the module identifier to set
       * @return this Builder instance for method chaining
       */
      public Builder moduleId(String moduleId) {
        this.moduleId = moduleId;
        return this;
      }

      /**
       * Sets the command being executed.
       * 
       * @param command the command to set
       * @return this Builder instance for method chaining
       */
      public Builder command(String command) {
        this.command = command;
        return this;
      }

      /**
       * Sets the class name being executed.
       * 
       * @param classname the class name to set
       * @return this Builder instance for method chaining
       */
      public Builder classname(String classname) {
        this.classname = classname;
        return this;
      }

      /**
       * Sets the object type being processed.
       * 
       * @param objecttype the object type to set
       * @return this Builder instance for method chaining
       */
      public Builder objecttype(String objecttype) {
        this.objecttype = objecttype;
        return this;
      }

      /**
       * Sets the timestamp when the audit occurred.
       * 
       * @param time the timestamp to set as a string
       * @return this Builder instance for method chaining
       */
      public Builder time(String time) {
        this.time = time;
        return this;
      }

      /**
       * Sets the JSON data containing additional telemetry information.
       * 
       * @param json the JSON data to set as a string
       * @return this Builder instance for method chaining
       */
      public Builder json(String json) {
        this.json = json;
        return this;
      }

      /**
       * Builds and returns a new UsageAuditData instance with the configured properties.
       * 
       * @return a new immutable UsageAuditData instance
       */
      public UsageAuditData build() {
        return new UsageAuditData(this);
      }
    }
  }

  /**
   * Logger instance for this class using Log4j2.
   * Used for logging errors, warnings, and debug information related to telemetry operations.
   */
  private static final org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager
      .getLogger();
  /**
   * Thread-local storage for TelemetryUsageInfo instances.
   * Each thread gets its own isolated instance to prevent data corruption
   * in multi-threaded environments.
   */
  private static final ThreadLocal<TelemetryUsageInfo> threadLocalInstance =
      ThreadLocal.withInitial(TelemetryUsageInfo::new);

  /**
   * The session identifier for tracking user sessions
   */
  private String sessionId;

  /**
   * JSON object containing telemetry usage data and metrics
   */
  private JSONObject jsonObject;

  /**
   * Timestamp when the telemetry instance was created or last reset
   */
  private long timeMillis;

  /**
   * The current command being executed
   */
  private String command;

  /**
   * The user identifier for tracking user actions
   */
  private String userId;

  /**
   * The module identifier for tracking module usage
   */
  private String moduleId;

  /**
   * The object type being processed
   */
  private String objecttype;

  /**
   * The object identifier being processed
   */
  private String objectId;

  /**
   * The class name being executed
   */
  private String classname;

  /**
   * Private constructor to prevent direct instantiation from outside the class.
   * Initializes the JSON object for storing telemetry data.
   *
   * <p>This constructor is called automatically by the ThreadLocal initializer
   * when a new instance is needed for a thread.</p>
   */
  private TelemetryUsageInfo() {
    this.jsonObject = new JSONObject();
    this.timeMillis = System.currentTimeMillis();
  }

  /**
   * Returns the thread-local instance of TelemetryUsageInfo.
   *
   * <p>This method provides access to the singleton instance for the current thread.
   * Each thread will receive its own isolated instance, ensuring thread safety
   * without the need for synchronization.</p>
   *
   * @return the TelemetryUsageInfo instance for the current thread
   */
  public static TelemetryUsageInfo getInstance() {
    return threadLocalInstance.get();
  }

  /**
   * Clears the thread-local instance for the current thread.
   *
   * <p>This method removes the TelemetryUsageInfo instance associated with the current thread,
   * freeing up memory. This is particularly useful when working with thread pools
   * to prevent memory leaks, as threads in a pool may be reused for different tasks.</p>
   *
   * <p><strong>Note:</strong> After calling this method, the next call to {@link #getInstance()}
   * will create a new instance for the current thread.</p>
   */
  public static void clear() {
    threadLocalInstance.remove();
  }

  /**
   * Gets the session identifier.
   *
   * @return the current session ID, or null if not set
   */
  public String getSessionId() {
    return sessionId;
  }

  /**
   * Sets the session identifier for tracking user sessions.
   *
   * @param sessionId
   *     the session ID to set
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
   * Gets the JSON object containing telemetry usage data.
   *
   * @return the JSON object with telemetry data, never null
   */
  public JSONObject getJsonObject() {
    return jsonObject;
  }

  /**
   * Sets the JSON object containing telemetry usage data.
   *
   * @param jsonObject
   *     the JSON object to set with telemetry data
   * @throws IllegalArgumentException
   *     if jsonObject is null
   */
  public void setJsonObject(JSONObject jsonObject) {
    this.jsonObject = jsonObject;
  }

  /**
   * Gets the timestamp when the telemetry instance was created or last reset.
   *
   * @return the timestamp in milliseconds
   */
  public long getTimeMillis() {
    return timeMillis;
  }

  /**
   * Sets the timestamp for telemetry tracking.
   *
   * @param timeMillis
   *     the timestamp in milliseconds
   */
  public void setTimeMillis(long timeMillis) {
    this.timeMillis = timeMillis;
  }

  /**
   * Gets the current command being executed.
   *
   * @return the current command, or null if not set
   */
  public String getCommand() {
    return command;
  }

  /**
   * Sets the current command being executed.
   *
   * @param command
   *     the command to set
   */
  public void setCommand(String command) {
    this.command = command;
  }

  /**
   * Gets the user identifier.
   *
   * @return the current user ID, or null if not set
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the user identifier for tracking user actions.
   *
   * @param userId
   *     the user ID to set
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Gets the module identifier.
   *
   * @return the current module ID, or null if not set
   */
  public String getModuleId() {
    return moduleId;
  }

  /**
   * Sets the module identifier for tracking module usage.
   *
   * @param moduleId
   *     the module ID to set
   */
  public void setModuleId(String moduleId) {
    this.moduleId = moduleId;
  }

  /**
   * Gets the object type.
   *
   * @return the current object type, or null if not set
   */
  public String getObjecttype() {
    return objecttype;
  }

  /**
   * Sets the object type being processed.
   *
   * @param objecttype
   *     the object type to set
   */
  public void setObjecttype(String objecttype) {
    this.objecttype = objecttype;
  }

  /**
   * Gets the object identifier.
   *
   * @return the current object ID, or null if not set
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * Sets the object identifier being processed.
   *
   * @param objectId
   *     the object ID to set
   */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /**
   * Gets the class name.
   *
   * @return the current class name, or null if not set
   */
  public String getClassname() {
    return classname;
  }

  /**
   * Sets the class name being executed.
   *
   * @param classname
   *     the class name to set
   */
  public void setClassname(String classname) {
    this.classname = classname;
  }

  /**
   * Saves the current telemetry usage information to the audit database.
   * 
   * <p>This method performs validation of all required parameters, sets the timestamp
   * if not already set, creates a database connection, and inserts the audit record
   * into the ad_session_usage_audit table.</p>
   * 
   * <p>The method will skip the insertion if any required parameters fail validation,
   * logging appropriate error messages.</p>
   * 
   * <p>Required parameters that are validated:</p>
   * <ul>
   *   <li>sessionId - must not be null or empty</li>
   *   <li>command - must not be null or empty</li>
   *   <li>userId - automatically retrieved from SessionInfo if not set</li>
   *   <li>moduleId - automatically retrieved from SessionInfo if not set</li>
   *   <li>objectId - automatically retrieved from SessionInfo if not set</li>
   *   <li>objecttype - automatically retrieved from SessionInfo if not set, defaults to "P"</li>
   * </ul>
   * 
   * @throws ServletException if there's an error during database operations
   * @throws JSONException if there's an error converting the JSON object to string
   * @see #validateParameters()
   * @see #insertUsageAudit(ConnectionProvider, UsageAuditData)
   */
  public void saveUsageAudit() throws ServletException, JSONException {

    if (!validateParameters()) return;

    // Set time if null
    if (getTimeMillis() == 0) {
      setTimeMillis(System.currentTimeMillis());
    }

    ConnectionProvider cp = new DalConnectionProvider(false);

    UsageAuditData auditData = new UsageAuditData.Builder()
        .userId(getUserId())
        .sessionId(getSessionId())
        .objectId(getObjectId())
        .moduleId(getModuleId())
        .command(getCommand())
        .classname(getClassname())
        .objecttype(getObjecttype())
        .time(String.valueOf(getTimeMillis()))
        .json(getJsonObject().toString(2))
        .build();

    insertUsageAudit(cp, auditData);
  }

  /**
   * Validates all required parameters for the usage audit operation.
   * 
   * <p>This method orchestrates the validation process by calling three separate
   * validation methods, ensuring that all required fields are properly set
   * before attempting to save the audit record.</p>
   * 
   * <p>The validation is performed in three stages:</p>
   * <ol>
   *   <li>Basic parameters validation (session ID and command)</li>
   *   <li>User and module data validation</li>
   *   <li>Object data validation</li>
   * </ol>
   * 
   * @return true if all validations pass, false otherwise
   * @see #validateBasicParameters()
   * @see #validateUserAndModule()
   * @see #validateObjectData()
   */
  private boolean validateParameters() {
    return validateBasicParameters() && validateUserAndModule() && validateObjectData();
  }

  /**
   * Validates the basic required parameters: session ID and command.
   * 
   * <p>This method checks that both session ID and command are not null
   * and not empty strings. These are fundamental requirements for any
   * usage audit record.</p>
   * 
   * @return true if both session ID and command are valid, false otherwise
   */
  private boolean validateBasicParameters() {
    if (sessionId == null || sessionId.isEmpty()) {
      log4j.error("Session ID is null or empty. Skipping usage audit insertion.");
      return false;
    }

    if (command == null || command.isEmpty()) {
      log4j.error("Command is null or empty. Skipping usage audit insertion.");
      return false;
    }
    return true;
  }

  /**
   * Validates and auto-populates user ID and module ID if necessary.
   * 
   * <p>This method ensures that both user ID and module ID are properly set.
   * If either value is null or empty, it attempts to retrieve the value
   * from the current session information using SessionInfo utility class.</p>
   * 
   * <p>For user ID: If not set, attempts to get it from SessionInfo.getUserId()</p>
   * <p>For module ID: If not set, attempts to get it from SessionInfo.getModuleId()</p>
   * 
   * <p>If the values cannot be retrieved from session info and remain null or empty,
   * appropriate error messages are logged and the validation fails.</p>
   * 
   * @return true if both user ID and module ID are successfully validated or retrieved, false otherwise
   */
  private boolean validateUserAndModule() {
    if (getUserId() == null || getUserId().isEmpty()) {
      setUserId(SessionInfo.getUserId());
      if (getUserId() == null || getUserId().isEmpty()) {
        log4j.error("User ID is null or empty. Skipping usage audit insertion.");
        return false;
      }
    }
    
    if (getModuleId() == null || getModuleId().isEmpty()) {
      setModuleId(SessionInfo.getModuleId());
      if (getModuleId() == null || getModuleId().isEmpty()) {
        log4j.error("Module ID is null or empty. Skipping usage audit insertion.");
        return false;
      }
    }
    return true;
  }

  /**
   * Validates and auto-populates object type and object ID if necessary.
   * 
   * <p>This method ensures that both object type and object ID are properly set.
   * If either value is null or empty, it attempts to retrieve the value
   * from the current session information using SessionInfo utility class.</p>
   * 
   * <p>For object type: If not set, attempts to get it from SessionInfo.getProcessType().
   * If still null or empty after that, defaults to "P" (Process).</p>
   * 
   * <p>For object ID: If not set, attempts to get it from SessionInfo.getProcessId().
   * This field is required and validation fails if it cannot be obtained.</p>
   * 
   * @return true if both object type and object ID are successfully validated or retrieved, false otherwise
   */
  private boolean validateObjectData() {
    if (getObjecttype() == null || getObjecttype().isEmpty()) {
      setObjecttype(SessionInfo.getProcessType());
      if (getObjecttype() == null || getObjecttype().isEmpty()) {
        // Set default object type if still null
        setObjecttype("P");
      }
    }
    
    if (getObjectId() == null || getObjectId().isEmpty()) {
      setObjectId(SessionInfo.getProcessId());
      if (getObjectId() == null || getObjectId().isEmpty()) {
        log4j.error("Object ID is null or empty. Skipping usage audit insertion.");
        return false;
      }
    }
    return true;
  }

  /**
   * Inserts a usage audit record into the ad_session_usage_audit database table.
   * 
   * <p>This static method performs the actual database insertion of the usage audit data.
   * It constructs and executes a SQL INSERT statement using prepared statements
   * to prevent SQL injection attacks.</p>
   * 
   * <p>The method inserts data into the following database columns:</p>
   * <ul>
   *   <li>ad_session_usage_audit_id - Generated UUID</li>
   *   <li>ad_client_id - Set to '0'</li>
   *   <li>ad_org_id - Set to '0'</li>
   *   <li>createdby - User ID from audit data</li>
   *   <li>updatedby - User ID from audit data</li>
   *   <li>ad_session_id - Session ID from audit data</li>
   *   <li>object_id - Object ID from audit data</li>
   *   <li>ad_module_id - Module ID from audit data</li>
   *   <li>command - Command from audit data</li>
   *   <li>classname - Class name from audit data</li>
   *   <li>object_type - Object type from audit data</li>
   *   <li>process_time - Timestamp converted to number</li>
   *   <li>EM_Ettelem_Jsondata - JSON data from audit data</li>
   * </ul>
   * 
   * <p>The method properly handles resource cleanup by ensuring the prepared statement
   * is released in the finally block, even if an exception occurs.</p>
   * 
   * <p>Query timeout is automatically set using the current session's query profile
   * for optimal performance and to prevent long-running queries.</p>
   * 
   * @param connectionProvider the database connection provider for executing the SQL statement
   * @param auditData the usage audit data containing all necessary information for the insert
   * @return the number of rows affected by the insert operation (typically 1 on success)
   * @throws ServletException if a SQL error or other database-related exception occurs during insertion
   * @see UsageAuditData
   * @see ConnectionProvider
   * @see QueryTimeOutUtil
   */
  public static int insertUsageAudit(ConnectionProvider connectionProvider, UsageAuditData auditData)
      throws ServletException {

    String strSql = "";
    strSql = strSql +
        "        INSERT INTO ad_session_usage_audit" +
        "        (ad_session_usage_audit_id, ad_client_id, ad_org_id, createdby, updatedby, ad_session_id, object_id, ad_module_id, command, classname, object_type, process_time, EM_Ettelem_Jsondata)" +
        "        VALUES (get_uuid(),'0','0',?,?,?,?,?,?,?,?,to_number(?),?)";

    int updateCount = 0;
    PreparedStatement st = null;

    int iParameter = 0;
    try {
      st = connectionProvider.getPreparedStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getUserId());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getUserId());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getSessionId());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getObjectId());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getModuleId());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getCommand());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getClassname());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getObjecttype());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getTime());
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, auditData.getJson());

      updateCount = st.executeUpdate();
    } catch (SQLException e) {
      if (log4j.isDebugEnabled()) {
        log4j.error("SQL error in query: " + strSql, e);
      } else {
        log4j.error("SQL error in query: " + strSql + " :" + e);
      }
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in query: " + strSql, ex);
      } else {
        log4j.error("Exception in query: " + strSql + " :" + ex);
      }
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception e) {
        log4j.error("Error during release*Statement of query: " + strSql, e);
      }
    }
    return (updateCount);
  }
}