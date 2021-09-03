package ru.geekbrains.server.service;

import ru.geekbrains.server.SQL.impl.PostgresSQLConnection;

public class BaseAuthService implements AuthService {

    private final PostgresSQLConnection connection = new PostgresSQLConnection();

    @Override
    public void start() {
        connection.connectToSQL();
    }

    @Override
    public String getNickByLoginPass(String login, String password) {
        connection.addUser(login);
        if (checkingPassword(password)) {
            return connection.getUser().getNick();
        }
        return null;
    }

    @Override
    public boolean changeOfNickname(String newNick, String password){
        if (checkingPassword(password)) {
            connection.updateTheColumnByLogin(
                    "nick", newNick, connection.getUser().getLogin());
            connection.getUser().setNick(newNick);
            return true;
        }
        return false;
    }

    @Override
    public void stop(){
        connection.disconnectFromSQL();
    }

    private boolean checkingPassword(String password) {
        try {
            return connection.getUser().getPassword().equals(password);
        } catch (NullPointerException e) {
            return false;
        }
    }
}