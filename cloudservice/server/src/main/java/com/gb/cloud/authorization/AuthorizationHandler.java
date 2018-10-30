package com.gb.cloud.authorization;

import java.sql.*;

public class AuthorizationHandler {

    private Connection connection;
    private PreparedStatement preparedStatement;


    public void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server/src/main/resources/cloudDB.db");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addNewUser(String login, String password) {
        String sql = "INSERT INTO users(login, password) VALUES(?,?);";
        try {
            PreparedStatement insertPrepared = connection.prepareStatement(sql);
            insertPrepared.setString(1,login);
            insertPrepared.setString(2,password);
            insertPrepared.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isUserExist(String checkLogin, String checkPassword){
        String sql = "SELECT * FROM users WHERE login = ? AND password = ?;";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,checkLogin);
            preparedStatement.setString(2,checkPassword);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                if(resultSet.getString("login").equals(checkLogin)
                        && resultSet.getString("password").equals(checkPassword))
                    return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isLoginNameNotBusy(String checkLogin){
        String sql = "SELECT login FROM users WHERE login = ?;";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,checkLogin);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                if(resultSet.getString("login").equals(checkLogin))
                    return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean isLoginNameExist(String checkLogin){
        String sql = "SELECT * FROM users WHERE login = ?;";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,checkLogin);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                if(resultSet.getString("login").equals(checkLogin))
                    return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void disconnect() {
        try {
            if (!preparedStatement.isClosed())
                preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
