package ru.geekbrains.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.geekbrains.server.service.AuthService;
import ru.geekbrains.server.service.BaseAuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger LOG = LogManager.getLogger(Server.class.getName());
    private static Server server;
    private boolean isActive;

    private List<ClientHandler> clients;
    private AuthService authService;
    private final ExecutorService singleService;

    public Server(int port) {
        server = this;
        isActive = true;
        singleService = Executors.newSingleThreadExecutor();
        try(ServerSocket server = new ServerSocket(port)) {
            authService = new BaseAuthService();
            authService.start();
            clients = new ArrayList<>();
            while (isActive) {
                singleService.execute(() -> {
                    try {
                        while (isActive) {
                            LOG.info("Ожидание подключения клиента...");
                            Socket socket = server.accept();
                            new ClientHandler(socket);
                            LOG.info("Клиент подключился...");
                        }
                    } catch (IOException e) {
                        LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
                        LOG.fatal(Arrays.toString(e.getStackTrace()));
                        e.printStackTrace();
                    }
                });
                scannerIsStop();
            }
        } catch (IOException e) {
            LOG.fatal("Непредвиденная ошибка! {}", e.getMessage());
            LOG.fatal(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        } finally {
            LOG.info("Остановка сервера...");
            if (authService != null) {
                authService.stop();
            }
        }
    }

    private void scannerIsStop() {
        Scanner scanner = new Scanner(System.in);
        String str = scanner.nextLine();
        if (str.equals("/end")) {
            stopServer();
        }
        scanner.close();
    }

    public void stopServer() {
        singleService.shutdown();
        isActive = false;
        broadcastMsg("/end");
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList();
    }

    public synchronized void broadcastMsg(String msg) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMsg(msg);
        }
    }

    public synchronized void msgForNick(ClientHandler client, String msg, String nick) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getName().equals(nick)) {
                clientHandler.sendMsg("От " + client.getName() + ": " + msg);
                client.sendMsg("Пользователю " + clientHandler.getName() + ": " + msg);
                return;
            } else {
                client.sendMsg("Пользователя " + nick + " нет в чате");
            }
        }
    }

    public synchronized void broadcastClientList() {
        StringBuilder builder = new StringBuilder("/clients");
        for (ClientHandler clientHandler : clients) {
            builder.append(clientHandler.getName()).append(":");
        }
        broadcastMsg(builder.toString());
    }

    public synchronized boolean isNickBusy(String nick) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public static Server getServer() {
        return server;
    }
}