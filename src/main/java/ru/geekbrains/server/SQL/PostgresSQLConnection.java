package ru.geekbrains.server.SQL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.geekbrains.server.User;

import java.sql.*;
import java.util.Arrays;

public class PostgresSQLConnection implements SQLConnection{
    private static final Logger LOG = LogManager.getLogger(PostgresSQLConnection.class.getName());

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            LOG.fatal("Драйвер БД не смог подключится... {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private Connection postgresConnection;
    private User user;

    @Override
    public void addUser(String login) {
        try {
            PreparedStatement preparedStatement = postgresConnection.prepareStatement("select * from users where login=?");
            preparedStatement.setString(1, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                if (!resultSet.getString("login").isEmpty()) {
                    LOG.info("Добавление пользователя {} в память...", login);
                    user = new User(
                            resultSet.getString("login"),
                            resultSet.getString("pass"),
                            resultSet.getString("nick")
                    );
                }
            }
        } catch (SQLException e) {
            LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
            LOG.fatal(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    @Override
    public void updateTheColumnByLogin(String column, String columnValue, String loginValue) {
        try {
            LOG.info("Смена никнейма у пользователя {} ({})",columnValue, loginValue);
            String SQRequest = String.format("update users set %s=? where login=?", column);
            PreparedStatement preparedStatement = postgresConnection.prepareStatement(SQRequest);
            preparedStatement.setString(1, columnValue);
            preparedStatement.setString(2, loginValue);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
            LOG.fatal(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    @Override
    public void connectToSQL() {
        try {
            LOG.info("Подключение БД...");
            postgresConnection = DriverManager.
                    getConnection("jdbc:postgresql://localhost:5432/chat", "postgres", "DZXdR8Vqc6");
        } catch (SQLException e) {
            LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
            LOG.fatal(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    @Override
    public void disconnectFromSQL() {
        try {
            LOG.info("Отключение БД...");
            postgresConnection.close();
        } catch (SQLException e) {
            LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
            LOG.fatal(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    public User getUser() {
        return user;
    }
}
