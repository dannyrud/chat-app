package chat_client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatClient extends WebSocketClient {
    private String username;

    public ChatClient(URI serverURI, String username, String password) {
        super(serverURI);
        this.username = username;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("✅ Connected to the chat server as " + username);
        System.out.println("📩 Type your message and press ENTER to send.");
        System.out.println("🚪 Type `/exit` to leave the chat.");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        HttpClient httpClient = HttpClient.newHttpClient();

        System.out.print("👤 Enter username: ");
        String username = scanner.nextLine();
        System.out.print("🔒 Enter password: ");
        String password = scanner.nextLine();

        // Authenticate the user via REST API
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:8080/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            System.out.println("⚠️ User not found. Registering a new account...");
            request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}"))
                .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("✅ User registered successfully!");
        } else {
            System.out.println("✅ Login successful!");
        }
        
        ChatClient client = new ChatClient(new URI("ws://localhost:8080/chat"), username, password);
        client.connectBlocking();

        // Start message loop
        while (true) {
            String message = scanner.nextLine();
            if (message.equalsIgnoreCase("/exit")) {
                client.close();
                System.out.println("🚪 You left the chat.");
                break;
            }
            client.send(message);
        }
    }
}
