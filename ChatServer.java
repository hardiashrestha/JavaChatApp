import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    private static Set<ClientHandler> clientHandlers = 
            Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {

        int port = 1234;

        System.out.println("=================================");
        System.out.println("        CHAT SERVER STARTED");
        System.out.println("        Port: " + port);
        System.out.println("=================================");

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // Broadcast message to all clients
    static void broadcast(String message, ClientHandler excludeUser) {
        synchronized (clientHandlers) {
            for (ClientHandler client : clientHandlers) {
                if (client != excludeUser) {
                    client.sendMessage(message);
                }
            }
        }
        System.out.println("Broadcasted: " + message);
    }

    static class ClientHandler implements Runnable {

        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // First message should be join message with username
                String joinMessage = in.readLine();
                if (joinMessage != null) {
                    username = extractUsername(joinMessage);
                    broadcast(joinMessage, this);
                }

                String message;

                while ((message = in.readLine()) != null) {

                    // If user leaves
                    if (message.contains("has left the chat")) {
                        broadcast(message, this);
                        break;
                    }

                    broadcast(message, this);
                }

            } catch (IOException e) {
                System.out.println("Connection lost with " + username);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                clientHandlers.remove(this);

                if (username != null) {
                    String leaveMsg = "🔴 " + username + " disconnected.";
                    broadcast(leaveMsg, this);
                }
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }

        private String extractUsername(String message) {
            return message.replace("🔵 ", "")
                          .replace(" has joined the chat.", "")
                          .trim();
        }
    }
}