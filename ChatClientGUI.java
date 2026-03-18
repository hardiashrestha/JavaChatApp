import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;   // store socket as a field
    private String username;

    public ChatClientGUI(String serverAddress, int port) {

        username = JOptionPane.showInputDialog(null, "Enter your username:");

        try {
            socket = new Socket(serverAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            createGUI();

            out.println("🔵 " + username + " has joined the chat.");

            new Thread(() -> {
                String message;
                try {
                    while ((message = in.readLine()) != null) {
                        chatArea.append(message + "\n");
                    }
                } catch (IOException e) {
                    chatArea.append("Connection closed.\n");
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server.");
            System.exit(0);
        }
    }

    private void createGUI() {
        frame = new JFrame("Chat Application - " + username);
        chatArea = new JTextArea();
        messageField = new JTextField();
        sendButton = new JButton("Send");

        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(messageField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        frame.add(panel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();

        if (!message.isEmpty()) {
            if (message.equalsIgnoreCase("/exit")) {
                out.println("🔴 " + username + " has left the chat.");
                closeResources();   
                System.exit(0);
            } else {
                out.println(username + ": " + message);
            }
        }

        messageField.setText("");
    }

    private void closeResources() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatClientGUI("192.168.1.2", 1234);
    }
}
