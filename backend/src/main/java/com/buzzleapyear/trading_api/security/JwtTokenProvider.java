package com.buzzleapyear.trading_api.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

//This file handles JWT token creation and validation
// Responsibilities:
// generateToken(username), Create JWT token
//validateToken(token), Checks if token is valid
//getUsernameFromJWT(token), Extracts username from token
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:MySecretKeyForJWTTokenGenerationAndValidation123}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**\
     * Generate a JWT token for a user
     * Called when user successfully logs in 
     * @param username  The username to put in the token
     * @return JWT token as a string
     */
    public String generateToken(String username) {
        // Creating the secret key
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                //specifying the signing key and algorithm
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Extracts username from a valid JWT token
     * Called when a request comes in with a token
     * Called when we need to extract the username from the token
     * @param token The JWT token string
     * @return The username stored in the token
     */
    public String getUsernameFromJWT(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Claims claims = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Validate if a JWT token is valid
     * Called by the authentication filter for every request
     * 
     * @param token The JWT token string
     * @return true if token is valid, false if expired/tampered/invalid
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
            Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
