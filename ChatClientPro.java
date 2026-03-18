import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class ChatClientPro {

    private JFrame frame;
    private JPanel chatPanel;
    private JTextField messageField;
    private JButton sendButton;

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;   // keep socket as a field
    private String username;

    public ChatClientPro(String serverAddress, int port) {

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
                        addMessageBubble(message, false);
                    }
                } catch (IOException e) {
                    addMessageBubble("Connection closed.", false);
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server.");
            System.exit(0);
        }
    }

    private void createGUI() {
        frame = new JFrame("Chat - " + username);
        frame.setSize(450, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeResources();
            }
        });

        frame.setLayout(new BorderLayout());


        JLabel header = new JLabel("  Chat Application", JLabel.LEFT);
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(new Color(25, 118, 210));
        header.setPreferredSize(new Dimension(100, 50));
        frame.add(header, BorderLayout.NORTH);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(chatPanel);
        scrollPane.setBorder(null);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        messageField = new JTextField();
        messageField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(25, 118, 210));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());

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
                addMessageBubble("You: " + message, true);
            }
            messageField.setText("");
        }
    }

    private void addMessageBubble(String message, boolean isSender) {
        JPanel bubblePanel = new JPanel();
        bubblePanel.setLayout(new BorderLayout());

        JLabel messageLabel = new JLabel("<html><p style='width: 200px'>" + message + "</p></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (isSender) {
            bubblePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
            messageLabel.setBackground(new Color(220, 248, 198));
        } else {
            bubblePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            messageLabel.setBackground(Color.WHITE);
        }

        messageLabel.setOpaque(true);
        messageLabel.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        bubblePanel.add(messageLabel);
        bubblePanel.setBackground(new Color(245, 245, 245));

        chatPanel.add(bubblePanel);
        chatPanel.revalidate();
        chatPanel.repaint();
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
        new ChatClientPro("192.168.1.2", 1234);
    }
}
