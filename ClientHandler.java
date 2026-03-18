import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    String name;
    private OutputStream out;
    private boolean wsConnected = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.name = ChatServer.nextName();
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            out = socket.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            // Read HTTP request
            String requestLine = reader.readLine();
            if (requestLine == null) return;

            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim().toLowerCase(),
                                line.substring(colon + 1).trim());
                }
            }

            String wsKey = headers.get("sec-websocket-key");
            if (wsKey != null) {
                // WebSocket upgrade
                String accept = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1")
                        .digest((wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));
                String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
                out.write(response.getBytes());
                out.flush();
                wsConnected = true;
                ChatServer.addClient(this);
                // Send assigned name
                sendWs("{\"type\":\"name\",\"name\":\"" + name + "\"}");
                listenWs(in);
            } else {
                // Serve HTML page
                serveHtml();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            if (wsConnected) ChatServer.removeClient(this);
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void listenWs(InputStream in) throws Exception {
        while (true) {
            int b1 = in.read();
            if (b1 == -1) break;
            int b2 = in.read();
            if (b2 == -1) break;
            boolean masked = (b2 & 0x80) != 0;
            int payloadLen = b2 & 0x7F;
            if (payloadLen == 126) {
                payloadLen = (in.read() << 8) | in.read();
            } else if (payloadLen == 127) {
                // skip 8 bytes
                for (int i = 0; i < 8; i++) in.read();
                payloadLen = 0;
            }
            byte[] mask = new byte[4];
            if (masked) in.read(mask);
            byte[] data = new byte[payloadLen];
            int read = 0;
            while (read < payloadLen) {
                int r = in.read(data, read, payloadLen - read);
                if (r == -1) break;
                read += r;
            }
            if (masked) {
                for (int i = 0; i < data.length; i++) data[i] ^= mask[i % 4];
            }
            int opcode = b1 & 0x0F;
            if (opcode == 8) break; // close frame
            String msg = new String(data, "UTF-8");
            if (!msg.trim().isEmpty()) {
                String json = "{\"type\":\"message\",\"name\":\"" + name + "\",\"text\":\"" + escape(msg) + "\"}";
                ChatServer.broadcast(json);
            }
        }
    }

    synchronized void sendWs(String text) {
        try {
            byte[] payload = text.getBytes("UTF-8");
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write(0x81); // FIN + text opcode
            if (payload.length <= 125) {
                frame.write(payload.length);
            } else if (payload.length <= 65535) {
                frame.write(126);
                frame.write((payload.length >> 8) & 0xFF);
                frame.write(payload.length & 0xFF);
            } else {
                frame.write(127);
                for (int i = 7; i >= 0; i--) frame.write((int)((payload.length >> (8 * i)) & 0xFF));
            }
            frame.write(payload);
            out.write(frame.toByteArray());
            out.flush();
        } catch (Exception e) {
            // client disconnected
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private void serveHtml() throws Exception {
        File f = new File("index.html");
        byte[] content;
        String contentType = "text/html";
        if (f.exists()) {
            content = java.nio.file.Files.readAllBytes(f.toPath());
        } else {
            content = "<h1>Chat server running</h1>".getBytes();
        }
        String header = "HTTP/1.1 200 OK\r\nContent-Type: " + contentType
            + "\r\nContent-Length: " + content.length + "\r\nConnection: close\r\n\r\n";
        out.write(header.getBytes());
        out.write(content);
        out.flush();
    }
}