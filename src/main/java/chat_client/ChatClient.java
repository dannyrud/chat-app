package chat_client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class ChatClient extends WebSocketClient {
    private String username;

    public ChatClient(URI serverURI, String token, String username) {
        super(serverURI);
        this.username = username;
        addHeader("Authorization", "Bearer " + token);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("✅ Connected as " + username);
        System.out.println("🚪 Type `/exit` to leave the chat.");
    }

    @Override
    public void onMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("❌ Disconnected: " + reason);
        System.exit(0);
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
        System.out.print("🏠 Which chat room would you like to join?: ");
        String chatRoom = scanner.nextLine();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://localhost:8080/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}"))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        JSONObject json = new JSONObject(responseBody);

        if (response.statusCode() == 404) {
            System.out.println("⚠️ Username does not exist! Registering a new account...");

            request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}"))
                .build();
            
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            responseBody = response.body();
            json = new JSONObject(responseBody);

            System.out.println("✅ User registered successfully!");
        } else if (response.statusCode() == 401) {
            System.out.println("❌ Incorrect password!");
            return;
        } else if (response.statusCode() != 200) {
            System.out.println("❌ Unexpected error: " + json.optString("error", "Unknown error"));
            return;
        }

        String token = json.getString("token");
        System.out.println("✅ Login successful!");

        ChatClient client = new ChatClient(new URI("ws://localhost:8080/chat?room=" + URLEncoder.encode(chatRoom, StandardCharsets.UTF_8)), token, username);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        client.connectBlocking();
        
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
