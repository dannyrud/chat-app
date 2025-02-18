package chat_app.controllers;

import chat_app.services.UserService;
import chat_app.utils.JwtUtil;
import chat_app.models.User;
import chat_app.repositories.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return ResponseEntity.status(400).body(Map.of("error", "Username and password are required!"));
        }

        try {
            userService.registerUser(username, password);
            String token = JwtUtil.generateToken(username);
            return ResponseEntity.ok(Map.of("message", "User registered successfully!", "token", token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Username already exists!"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return ResponseEntity.status(400).body(Map.of("error", "Username and password are required!"));
        }

        Optional<User> user = userRepository.findByUsername(username);
        
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User does not exist!"));
        }

        if (!userService.authenticateUser(username, password)) {
            return ResponseEntity.status(401).body(Map.of("error", "Incorrect password!"));
        }

        String token = JwtUtil.generateToken(username);
        return ResponseEntity.ok(Map.of("message", "Login successful!", "token", token));
    }
}
