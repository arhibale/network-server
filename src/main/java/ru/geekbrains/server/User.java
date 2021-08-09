package ru.geekbrains.server;

public class User {
    private final String login;
    private String password;
    private String nick;

    public User(String login, String password, String nick) {
        this.login = login;
        this.password = password;
        this.nick = nick;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}