/**
 * OWASP Benchmark Project
 *
 * <p>This file is part of the Open Web Application Security Project (OWASP) Benchmark Project For
 * details, please see <a
 * href="https://owasp.org/www-project-benchmark/">https://owasp.org/www-project-benchmark/</a>.
 *
 * <p>The OWASP Benchmark is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, version 2.
 *
 * <p>The OWASP Benchmark is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details
 *
 * @author Juan Gama
 * @created 2015
 */
package org.owasp.benchmark.helpers;
// adding a random comment
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.owasp.benchmark.service.pojo.XMLMessage;
import org.owasp.esapi.ESAPI;

public class DatabaseHelper {
    private static Connection conn;
    public static org.springframework.jdbc.core.JdbcTemplate JDBCtemplate;
    public static org.owasp.benchmark.helpers.HibernateUtil hibernateUtil =
            new org.owasp.benchmark.helpers.HibernateUtil(false);
    public static org.owasp.benchmark.helpers.HibernateUtil hibernateUtilClassic =
            new org.owasp.benchmark.helpers.HibernateUtil(true);
    public static final boolean hideSQLErrors =
            false; // If we want SQL Exceptions to be suppressed from being displayed to the user of
    // the web app.

    static {
        initDataBase();
        org.owasp.benchmark.helpers.DatabaseHelper.hibernateUtil.initData();
        org.owasp.benchmark.helpers.DatabaseHelper.hibernateUtilClassic.initClassicData();

        System.out.println("Spring context init() ");
        @SuppressWarnings("resource")
        org.springframework.context.ApplicationContext ac =
                new org.springframework.context.support.ClassPathXmlApplicationContext(
                        "/context.xml", DatabaseHelper.class);
        javax.sql.DataSource data = (javax.sql.DataSource) ac.getBean("dataSource");
        JDBCtemplate = new org.springframework.jdbc.core.JdbcTemplate(data);
        System.out.println("Spring context loaded!");
    }

    public static void initDataBase() {
        try {
            executeSQLCommand("DROP PROCEDURE IF EXISTS verifyUserPassword");
            executeSQLCommand("DROP PROCEDURE IF EXISTS verifyEmployeeSalary");
            executeSQLCommand("DROP TABLE IF EXISTS USERS");
            executeSQLCommand("DROP TABLE IF EXISTS EMPLOYEE");
            executeSQLCommand("DROP TABLE IF EXISTS CERTIFICATE");
            executeSQLCommand("DROP TABLE IF EXISTS SCORE");

            executeSQLCommand(
                    "CREATE TABLE USERS (userid int NOT NULL GENERATED BY DEFAULT AS IDENTITY, username varchar(50), password varchar(50),PRIMARY KEY (userid));");
            executeSQLCommand(
                    "CREATE TABLE SCORE (userid int NOT NULL GENERATED BY DEFAULT AS IDENTITY, nick varchar(50), score INTEGER,PRIMARY KEY (userid));");
            executeSQLCommand(
                    "CREATE PROCEDURE verifyUserPassword(IN username_ varchar(50), IN password_ varchar(50))"
                            + " READS SQL DATA"
                            + " DYNAMIC RESULT SETS 1"
                            + " BEGIN ATOMIC"
                            + " DECLARE resultSet SCROLL CURSOR WITH HOLD WITH RETURN FOR SELECT * FROM USERS WHERE USERNAME = username_ AND PASSWORD = password_;"
                            + " OPEN resultSet;"
                            + "END;");

            executeSQLCommand(
                    "create table EMPLOYEE ("
                            + "	   id INT NOT NULL GENERATED BY DEFAULT AS IDENTITY,"
                            + "	   first_name VARCHAR(20) default NULL,"
                            + "   last_name  VARCHAR(20) default NULL,"
                            + " salary     INT  default NULL,"
                            + " PRIMARY KEY (id)"
                            + "	);");

            executeSQLCommand(
                    "create table CERTIFICATE ("
                            + "	   id INT NOT NULL GENERATED BY DEFAULT AS IDENTITY,"
                            + " certificate_name VARCHAR(30) default NULL,"
                            + " employee_id INT default NULL,"
                            + " PRIMARY KEY (id)"
                            + ");");

            executeSQLCommand(
                    "CREATE PROCEDURE verifyEmployeeSalary(IN user_ varchar(50))"
                            + " READS SQL DATA"
                            + " DYNAMIC RESULT SETS 1"
                            + " BEGIN ATOMIC"
                            + " DECLARE resultSet SCROLL CURSOR WITH RETURN FOR SELECT * FROM EMPLOYEE WHERE FIRST_NAME = user_;"
                            + " OPEN resultSet;"
                            + "END;");
            conn.commit();
            initData();

            System.out.println("DataBase tables/procedures created.");
        } catch (Exception e1) {
            System.out.println(
                    "Problem with database table/procedure creations: " + e1.getMessage());
        }
    }

    public static java.sql.Statement getSqlStatement() {
        if (conn == null) {
            getSqlConnection();
        }
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            System.out.println("Problem with database init.");
        }

        return stmt;
    }

    public static void reset() {
        initData();
    }

    private static void initData() {
        try {
            executeSQLCommand(
                    "INSERT INTO USERS (username, password) VALUES('User01', 'P455w0rd')");
            executeSQLCommand(
                    "INSERT INTO USERS (username, password) VALUES('User02', 'B3nchM3rk')");
            executeSQLCommand("INSERT INTO USERS (username, password) VALUES('User03', 'a$c11')");
            executeSQLCommand("INSERT INTO USERS (username, password) VALUES('foo', 'bar')");

            executeSQLCommand("INSERT INTO SCORE (nick, score) VALUES('User03', 155)");
            executeSQLCommand("INSERT INTO SCORE (nick, score) VALUES('foo', 40)");

            executeSQLCommand(
                    "INSERT INTO EMPLOYEE (first_name, last_name, salary) VALUES('foo', 'bar', 34567)");
            conn.commit();
        } catch (Exception e1) {
            System.out.println("Problem with database init/reset: " + e1.getMessage());
        }
    }

    public static java.sql.Connection getSqlConnection() {
        if (conn == null) {
            try {
                InitialContext ctx = new InitialContext();
                DataSource datasource = (DataSource) ctx.lookup("java:comp/env/jdbc/BenchmarkDB");
                conn = datasource.getConnection();
                conn.setAutoCommit(false);
            } catch (SQLException | NamingException e) {
                System.out.println("Problem with getSqlConnection.");
                e.printStackTrace();
            }
        }
        return conn;
    }

    public static void executeSQLCommand(String sql) throws Exception {
        Statement stmt = getSqlStatement();
        stmt.executeUpdate(sql);
    }

    public static void outputUpdateComplete(String sql, HttpServletResponse response)
            throws java.sql.SQLException, IOException {

        PrintWriter out = response.getWriter();

        out.write("<!DOCTYPE html>\n<html>\n<body>\n<p>");
        out.write(
                "Update complete for query: "
                        + org.owasp.esapi.ESAPI.encoder().encodeForHTML(sql)
                        + "<br>\n");
        out.write("</p>\n</body>\n</html>");
    }

    public static void outputUpdateComplete(String sql, List<XMLMessage> resp)
            throws java.sql.SQLException, IOException {
        resp.add(new XMLMessage("Update complete for query: " + sql + "\n"));
    }

    public static void printResults(
            java.sql.Statement statement, String sql, HttpServletResponse response)
            throws java.sql.SQLException, IOException {

        PrintWriter out = response.getWriter();
        out.write(
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
                        + "<html>\n"
                        + "<head>\n"
                        + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
                        + "</head>\n"
                        + "<body>\n"
                        + "<p>\n");

        try {
            ResultSet rs = statement.getResultSet();
            if (rs == null) {
                out.write(
                        "Results set is empty for query: "
                                + org.owasp.esapi.ESAPI.encoder().encodeForHTML(sql));
                return;
            }

            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
            out.write("Your results are:<br>\n");

            while (rs.next()) {
                for (int i = 1; i <= numberOfColumns; i++) {
                    if (i > 1) {
                        out.write(",  ");
                    }
                    String columnValue = rs.getString(i);
                    out.write(ESAPI.encoder().encodeForHTML(columnValue));
                }
                out.write("<br>\n");
            }

        } finally {
            out.write("</p>\n</body>\n</html>");
        }
    }

    /**
     * Used by the Web Services XML in/out test cases.
     *
     * @param statement
     * @param sql
     * @param resp
     * @throws java.sql.SQLException
     * @throws IOException
     */
    public static void printResults(java.sql.Statement statement, String sql, List<XMLMessage> resp)
            throws java.sql.SQLException, IOException {

        ResultSet rs = statement.getResultSet();
        if (rs == null) {
            resp.add(new XMLMessage("Results set is empty for query: " + sql + "\n"));
            return;
        }
        ResultSetMetaData rsmd = rs.getMetaData();
        int numberOfColumns = rsmd.getColumnCount();
        resp.add(new XMLMessage("Your results are:\n"));
        while (rs.next()) {
            for (int i = 1; i <= numberOfColumns; i++) {
                String columnValue = rs.getString(i);
                if (i == 0) {
                    resp.add(new XMLMessage(columnValue));
                } else resp.add(new XMLMessage(",  " + columnValue));
            }
            resp.add(new XMLMessage("\n"));
        }
    }

    public static void printResults(java.sql.ResultSet rs, String sql, HttpServletResponse response)
            throws java.sql.SQLException, IOException {

        PrintWriter out = response.getWriter();
        out.write("<!DOCTYPE html>\n<html>\n<body>\n<p>");

        try {
            if (rs == null) {
                out.write(
                        "Results set is empty for query: "
                                + org.owasp.esapi.ESAPI.encoder().encodeForHTML(sql));
                return;
            }
            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
            out.write("Your results are:<br>\n");
            while (rs.next()) {
                for (int i = 1; i <= numberOfColumns; i++) {
                    String columnValue = rs.getString(i);
                    out.write(ESAPI.encoder().encodeForHTML(columnValue));
                }
                out.write("<br>\n");
            }

        } finally {
            out.write("</p>\n</body>\n</html>");
        }
    }

    public static void printResults(java.sql.ResultSet rs, String sql, List<XMLMessage> resp)
            throws java.sql.SQLException, IOException {

        if (rs == null) {
            resp.add(new XMLMessage("Results set is empty for query: " + sql + "\n"));
            return;
        }
        ResultSetMetaData rsmd = rs.getMetaData();
        int numberOfColumns = rsmd.getColumnCount();
        resp.add(new XMLMessage("Your results are:\n"));
        while (rs.next()) {
            for (int i = 1; i <= numberOfColumns; i++) {
                String columnValue = rs.getString(i);
                resp.add(new XMLMessage(columnValue));
            }
            resp.add(new XMLMessage("\n"));
        }
    }

    public static void printResults(String query, int[] counts, HttpServletResponse response)
            throws IOException {
        PrintWriter out = response.getWriter();
        out.write("<!DOCTYPE html>\n<html>\n<body>\n<p>");
        out.write("For query: " + ESAPI.encoder().encodeForHTML(query) + "<br>");
        try {
            if (counts.length > 0) {
                if (counts[0] == Statement.SUCCESS_NO_INFO) {
                    out.write(
                            "The SQL query was processed successfully but the number of rows affected is unknown.");
                } else if (counts[0] == Statement.EXECUTE_FAILED) {
                    out.write(
                            "The SQL query failed to execute successfully and occurs only if a driver continues to process commands after a command fails");
                } else {
                    out.write("The number of affected rows are: " + counts[0]);
                }
            }
        } finally {
            out.write("</p>\n</body>\n</html>");
        }
    }

    public static void printResults(String query, int[] counts, List<XMLMessage> resp)
            throws IOException {

        resp.add(new XMLMessage("For query:\n"));

        if (counts.length > 0) {
            if (counts[0] == Statement.SUCCESS_NO_INFO) {
                resp.add(
                        new XMLMessage(
                                "The SQL query was processed successfully but the number of rows affected is unknown."));
            } else if (counts[0] == Statement.EXECUTE_FAILED) {
                resp.add(
                        new XMLMessage(
                                "The SQL query failed to execute successfully and occurs only if a driver continues to process commands after a command fails"));
            } else {
                resp.add(new XMLMessage("The number of affected rows are: " + counts[0]));
            }
        }
    }

    public static void printColTypes(ResultSetMetaData rsmd, PrintWriter out)
            throws java.sql.SQLException {
        int columns = rsmd.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            int jdbcType = rsmd.getColumnType(i);
            String name = rsmd.getColumnTypeName(i);
            out.write(
                    "Column "
                            + i
                            + " is JDBC type "
                            + jdbcType
                            + ", which the DBMS calls "
                            + name
                            + "<br>\n");
        }
    }
}
