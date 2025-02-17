package chat_app.controllers;

import chat_app.models.User;
import chat_app.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        try {
            userService.registerUser(username, password);
            return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Username already exists!"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (userService.authenticateUser(username, password)) {
            return ResponseEntity.ok(Map.of("message", "Login successful!"));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password!"));
    }
}
