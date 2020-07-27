package org.wso2.carbon.identity.cors.mgt.core.constant;

/**
 * Constants related to CORS operations.
 */
public class SQLConstants {

    public static final String GET_CORS_ORIGINS_BY_TENANT_ID = "SELECT ID, ORIGIN, IS_TENANT_LEVEL " +
            "FROM IDN_CORS_ORIGIN " +
            "WHERE TENANT_ID = ? " +
            "ORDER BY ID ASC";

    public static final String GET_CORS_ORIGINS_BY_APPLICATION_ID = "SELECT IDN_CORS_ORIGIN.ID, " +
            "IDN_CORS_ORIGIN.ORIGIN, IDN_CORS_ORIGIN.IS_TENANT_LEVEL " +
            "FROM IDN_CORS_ORIGIN " +
            "INNER JOIN IDN_CORS_ASSOCIATION ON IDN_CORS_ORIGIN.ID = IDN_CORS_ASSOCIATION.IDN_CORS_ORIGIN_ID " +
            "WHERE IDN_CORS_ORIGIN.TENANT_ID = ? AND IDN_CORS_ASSOCIATION.SP_APP_ID = ? " +
            "ORDER BY ID ASC";

    public static final String DELETE_TENANT_ASSOCIATION = "UPDATE IDN_CORS_ORIGIN " +
            "SET IS_TENANT_LEVEL = 0 " +
            "WHERE ID = ?";

    public static final String DELETE_TENANT_ASSOCIATIONS = "UPDATE IDN_CORS_ORIGIN " +
            "SET IS_TENANT_LEVEL = 0 " +
            "WHERE TENANT_ID = ?";

    public static final String DELETE_APPLICATION_ASSOCIATION = "DELETE FROM IDN_CORS_ASSOCIATION " +
            "WHERE IDN_CORS_ORIGIN_ID = ? AND SP_APP_ID = ?";

    public static final String DELETE_ORIGIN = "DELETE FROM IDN_CORS_ORIGIN " +
            "WHERE ID = ?";

    public static final String DELETE_APPLICATION_ASSOCIATIONS = "DELETE FROM IDN_CORS_ASSOCIATION " +
            "WHERE IDN_CORS_ORIGIN_ID = ? AND SP_APP_ID = ?";

    public static final String INSERT_CORS_ORIGIN = "INSERT INTO IDN_CORS_ORIGIN (TENANT_ID, ORIGIN, IS_TENANT_LEVEL) "
            + "VALUES (?, ?, ?)";

    public static final String INSERT_CORS_ASSOCIATION = "INSERT INTO IDN_CORS_ASSOCIATION (IDN_CORS_ORIGIN_ID, " +
            "SP_APP_ID) " +
            "VALUES (?, ?)";

    public static final String GET_CORS_ORIGIN_ID = "SELECT ID FROM IDN_CORS_ORIGIN " +
            "WHERE TENANT_ID = ? AND ORIGIN = ?";

    public static final String UPDATE_TENANT_ASSOCIATIONS = "UPDATE IDN_CORS_ORIGIN " +
            "SET IS_TENANT_LEVEL = ? " +
            "WHERE TENANT_ID = ? AND ORIGIN = ?";

    public static final String GET_CORS_APPLICATION_IDS_BY_CORS_ORIGIN_ID = "SELECT SP_APP_ID " +
            "FROM IDN_CORS_ASSOCIATION " +
            "WHERE IDN_CORS_ORIGIN_ID = ?";

    public static final String GET_CORS_APPLICATIONS_BY_CORS_ORIGIN_ID = "SELECT ID, APP_NAME FROM SP_APP " +
            "INNER JOIN IDN_CORS_ASSOCIATION ON IDN_CORS_ASSOCIATION.SP_APP_ID = SP_APP.ID " +
            "WHERE IDN_CORS_ASSOCIATION.IDN_CORS_ORIGIN_ID = ?";
}
