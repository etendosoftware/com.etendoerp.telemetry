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

  public void saveUsageAudit() throws ServletException, JSONException {

    if (sessionId == null || sessionId.isEmpty()) {
      log4j.error("Session ID is null or empty. Skipping usage audit insertion.");
      return;
    }

    if (command == null || command.isEmpty()) {
      log4j.error("Command is null or empty. Skipping usage audit insertion.");
      return;
    }

    if (getUserId() == null || getUserId().isEmpty()) {
      setUserId(SessionInfo.getUserId());
      if (getUserId() == null || getUserId().isEmpty()) {
        log4j.error("User ID is null or empty. Skipping usage audit insertion.");
        return;
      }
    }
    if (getModuleId() == null || getModuleId().isEmpty()) {
      setModuleId(SessionInfo.getModuleId());
      if (getModuleId() == null || getModuleId().isEmpty()) {
        log4j.error("Module ID is null or empty. Skipping usage audit insertion.");
        return;
      }
    }
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
        return;
      }
    }

    // Set time if null
    if (getTimeMillis() == 0) {
      setTimeMillis(System.currentTimeMillis());
    }

    ConnectionProvider cp = new DalConnectionProvider(false);

    insertUsageAudit(cp, getUserId(), getSessionId(), getObjectId(), getModuleId(), getCommand(),
        getClassname(), getObjecttype(), String.valueOf(getTimeMillis()), getJsonObject().toString(2));
  }

  public static int insertUsageAudit(ConnectionProvider connectionProvider, String userId, String sessionId,
      String objectId, String moduleId, String command, String classname, String objecttype,
      String time, String json) throws ServletException {

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
      UtilSql.setValue(st, iParameter, 12, null, userId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, userId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, sessionId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, objectId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, moduleId);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, command);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, classname);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, objecttype);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, time);
      iParameter++;
      UtilSql.setValue(st, iParameter, 12, null, json);

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