package tools;

import java.sql.*;

public class DBConnection {

    private final String host = "project.maschini.de";
    private final String database = "famco";
    private final String username = "famcoadmin";
    private final String password = "N%au7X43r6";
    private Connection connection = null;
    private boolean isConnectionValid = false;

    public DBConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(5);
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/" + database, username, password);
            isConnectionValid = true;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid() {
        return this.isConnectionValid;
    }


    public ResultSet execute(String query) {
        try {
            Statement statement = connection.createStatement();
            return statement.executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ResultSet execute(String query, Object... params) {
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void update(String query) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(String query, Object... params) {
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i+1, params[i]);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}