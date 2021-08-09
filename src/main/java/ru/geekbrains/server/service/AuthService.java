package ru.geekbrains.server.service;

public interface AuthService {
    void start();
    String getNickByLoginPass(String login, String password);
    boolean changeOfNickname(String newNick, String password);
    void stop();
}