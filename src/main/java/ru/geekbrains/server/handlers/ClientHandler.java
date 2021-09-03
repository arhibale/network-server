package ru.geekbrains.server.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.geekbrains.server.Server;
import ru.geekbrains.server.config.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientHandler {
    private static final Logger LOG = LogManager.getLogger(ClientHandler.class.getName());
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String name = "";

    public ClientHandler(Socket socket) {
        try {
            this.server = Server.getServer();
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            ExecutorService cashExecutor = Executors.newCachedThreadPool();
            cashExecutor.execute(() -> {
                try {
                    socket.setSoTimeout(300000); // 5 минут
                    auth();
                    readMsg();
                } catch (SocketTimeoutException e) {
                    LOG.warn("Отключение пользователя {} за бейсдействие...", name);
                } catch (IOException | SQLException e) {
                    LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
                    LOG.fatal(Arrays.toString(e.getStackTrace()));
                } finally {
                    LOG.info("Пользователь {} отключился...", name);
                    closeConnection();
                }
            });
            cashExecutor.shutdown();
        } catch (IOException e) {
            LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
            LOG.fatal(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    private void auth() throws IOException, SQLException {
        LOG.info("Авторизация пользователя...");
        while(true) {
            String str = in.readUTF();
            if (str.startsWith("/" + Commands.AUTH)) {
                String[] parts = str.split(" ");
                String login = parts[1];
                String password = parts[2];
                String nick = server.getAuthService().getNickByLoginPass(login, password);
                if (nick != null) {
                    if (!server.isNickBusy(nick)) {
                        sendMsg("/" + Commands.AUTHOK + " " + nick);
                        name = nick;
                        server.broadcastMsg(name + " вошёл в чат.");
                        server.subscribe(this);
                        LOG.info("Пользователь авторизовался под логином: {}, никнейм: {}", login, name);
                        return;
                    } else {
                        sendMsg(String.format("/%s:Учётная запись уже используеться.", Commands.WARN));
                    }
                } else {
                    sendMsg(String.format("/%s:Неверные логин/пароль.", Commands.WARN));
                }
            } else {
                sendMsg("Перед тем как отправлять сообщения авторизуйтесь!");
            }
        }
    }

    private void readMsg() throws IOException, SQLException {
        while (true) {
            String strFromClient = in.readUTF();
            LOG.info("Пользователь {} отправил сообщение: {}", name, strFromClient);
            if (strFromClient.startsWith("/")) {
                if (strFromClient.equals("/" + Commands.END)) {
                    return;
                } else {
                    commands(strFromClient);
                }
            } else {
                server.broadcastMsg(timeForMsg() + " " + name + ": " + strFromClient);
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
            LOG.info("Отправка сообщения пользователю: {}", msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void commands(String strFromClient) throws IOException {
        if (strFromClient.startsWith("/" + Commands.PM)) {
            String[] str = strFromClient.split(" ");
            String nick = str[1];
            String msg = strFromClient.substring(4 + nick.length());
            server.msgForNick(this, msg, nick);
            return;
        }
        if (strFromClient.startsWith("/" + Commands.NN)) {
            newNickForUser(strFromClient);
            return;
        }
        if (strFromClient.startsWith("/" + Commands.CLIENTS)) {
            server.broadcastClientList();
            return;
        }
        if (strFromClient.startsWith("/" + Commands.INFO)) {
            sendMsg("Приватное сообщение: «/PM nick msg»\n" +
                    "Смена никнейма: «/NN pass newNick»\n" +
                    "Выйти из чата: /END");
        }
    }

    private void newNickForUser(String strFromClient) throws IOException {
        String[] str = strFromClient.split(" ");
        if (str.length < 3) {
            return;
        }
        String pass = str[1];
        String newNick = str[2];
        if (server.getAuthService().changeOfNickname(newNick, pass)) {
            server.broadcastMsg(name + " поменял никнейм на: " + newNick);
            out.writeUTF("/" + Commands.NN + " " + newNick);
            LOG.info("Смена никнейма у пользователя {} на новый {}", name, newNick);
            name = newNick;
        }
    }

    private void closeConnection() {
        server.unsubscribe(this);
        server.broadcastMsg(name + " вышел из чата.");
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    private String timeForMsg() {
        LocalTime time = LocalTime.now();
        return "[" + time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] ";
    }
}