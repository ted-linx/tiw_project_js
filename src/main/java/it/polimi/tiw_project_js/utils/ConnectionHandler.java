package it.polimi.tiw_project_js.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContext;
import jakarta.servlet.UnavailableException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionHandler {

    private static final String DATASOURCE_ATTR = "DataSource";

    public static DataSource initPool(ServletContext context) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(context.getInitParameter("dbUrl"));
        config.setUsername(context.getInitParameter("dbUser"));
        config.setPassword(context.getInitParameter("dbPassword"));
        config.setDriverClassName(context.getInitParameter("dbDriver"));

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);

        HikariDataSource ds = new HikariDataSource(config);
        context.setAttribute(DATASOURCE_ATTR, ds);
        return ds;
    }

    public static Connection getConnection(ServletContext context) throws UnavailableException {
        DataSource ds = (DataSource) context.getAttribute(DATASOURCE_ATTR);
        if (ds == null) throw new UnavailableException("DataSource not initialized");
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            throw new UnavailableException("Couldn't get db connection from pool");
        }
    }

    public static Connection getConnection(DBConfig config) throws UnavailableException {
        Connection connection = null;
        try {

            String url = config.url();
            String user = config.username();
            String password = config.password();
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new UnavailableException("Can't load database driver");
        } catch (SQLException e) {
            throw new UnavailableException("Couldn't get db connection");
        }
        return connection;
    }


    // Rimane invariato: chiamare .close() su una HikariCP connection la restituisce al pool
    public static void closeConnection(Connection connection) throws SQLException {
        if (connection != null) {
            connection.close(); // restituisce al pool, non chiude davvero
        }
    }
}
