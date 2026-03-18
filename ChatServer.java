import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    static int userCount = 0;

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("AnonChat server running on port " + PORT);
        ExecutorService pool = Executors.newCachedThreadPool();
        while (true) {
            Socket socket = serverSocket.accept();
            pool.execute(new ClientHandler(socket));
        }
    }

    static void broadcast(String json) {
        synchronized (clients) {
            for (ClientHandler c : new ArrayList<>(clients)) {
                c.sendWs(json);
            }
        }
    }

    static void addClient(ClientHandler c) {
        clients.add(c);
        broadcast("{\"type\":\"system\",\"text\":\"" + c.name + " joined the chat\",\"count\":" + clients.size() + "}");
        System.out.println(c.name + " connected. Online: " + clients.size());
    }

    static void removeClient(ClientHandler c) {
        clients.remove(c);
        broadcast("{\"type\":\"system\",\"text\":\"" + c.name + " left the chat\",\"count\":" + clients.size() + "}");
        System.out.println(c.name + " disconnected. Online: " + clients.size());
    }

    static synchronized String nextName() {
        return "Anon" + (++userCount);
    }
}