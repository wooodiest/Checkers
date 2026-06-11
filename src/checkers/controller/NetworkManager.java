package checkers.controller;

import checkers.model.PieceColor;
import checkers.view.ConnectionDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetworkManager implements Runnable {

    private final ConnectionDialog.ConnectionConfig config;
    private NetworkListener listener;

    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private volatile boolean running;

    public NetworkManager(ConnectionDialog.ConnectionConfig config, NetworkListener listener) {
        this.config = config;
        this.listener = listener;
    }

    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    public void start() {
        Thread networkThread = new Thread(this, "NetworkManager");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    @Override
    public void run() {
        try {
            if (config.getMode() == ConnectionDialog.ConnectionMode.HOST) {
                startHost();
            } else {
                startClient();
            }
            running = true;
            readLoop();
        } catch (IOException exception) {
            if (running) {
                listener.onDisconnected();
            }
        } finally {
            closeResources();
        }
    }

    private void startHost() throws IOException {
        serverSocket = new ServerSocket(config.getPort());
        socket = serverSocket.accept();
        initializeStreams();
        sendRaw("START;" + PieceColor.BLACK.name());
        listener.onConnected(PieceColor.WHITE);
    }

    private void startClient() throws IOException {
        socket = new Socket(config.getHost(), config.getPort());
        initializeStreams();
    }

    private void initializeStreams() throws IOException {
        writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private void readLoop() throws IOException {
        String line;
        while (running && (line = reader.readLine()) != null) {
            handleCommand(line.trim());
        }
        if (running) {
            listener.onDisconnected();
        }
    }

    private void handleCommand(String line) {
        if (line.isEmpty()) {
            return;
        }
        String[] parts = line.split(";", -1);
        String command = parts[0];
        switch (command) {
            case "START" -> {
                if (parts.length >= 2) {
                    PieceColor color = PieceColor.valueOf(parts[1]);
                    listener.onConnected(color);
                }
            }
            case "CHAT" -> {
                if (parts.length >= 2) {
                    listener.onChatMessage("Opponent", parts[1]);
                }
            }
            case "MOVE" -> {
                if (parts.length >= 5) {
                    int fromX = Integer.parseInt(parts[1]);
                    int fromY = Integer.parseInt(parts[2]);
                    int toX = Integer.parseInt(parts[3]);
                    int toY = Integer.parseInt(parts[4]);
                    listener.onRemoteMove(fromX, fromY, toX, toY);
                }
            }
            case "END" -> {
                String reason = parts.length >= 2 ? parts[1] : "UNKNOWN";
                listener.onGameEnd(reason);
            }
            default -> {
            }
        }
    }

    public synchronized void sendChat(String message) {
        sendRaw("CHAT;" + message);
    }

    public synchronized void sendMove(int fromX, int fromY, int toX, int toY) {
        sendRaw("MOVE;" + fromX + ";" + fromY + ";" + toX + ";" + toY);
    }

    public synchronized void sendGameEnd(String reason) {
        sendRaw("END;" + reason);
        running = false;
        closeResources();
    }

    private void sendRaw(String command) {
        if (writer != null) {
            writer.println(command);
        }
    }

    public void shutdown() {
        running = false;
        closeResources();
    }

    private void closeResources() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
