package ru.geekbrains.server.SQL;

public interface SQLConnection {
    void connectToSQL();
    void disconnectFromSQL();
    void updateTheColumnByLogin(String column, String columnValue, String loginValue);
    void addUser(String login);
}
