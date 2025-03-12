package chat_app.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.Base64;

// Signs username with a secret key, provides message integrity not confidentiality
public class JwtUtil {
    private static final Key generatedKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final String SECRET_KEY = Base64.getEncoder().encodeToString(generatedKey.getEncoded());
    private static final long EXPIRATION_TIME = 86400000; // 1 day

    private static final Key key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_KEY));

    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            return null;
        }
    }
}
