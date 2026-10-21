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
// generateToken(username) create JWT token
//validateToken(token), Checks if token is valid
//getUsernameFromJWT(token), this extracts the username from the token
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * Generate a JWT token for a user
     * Called when user successfully logs in 
     * @param username  The username to put in the token
     * @return JWT token as a string
     */
    public String generateToken(String username) {
        return createToken(username, jwtExpirationMs);
    }

    //generating access token every 15 minutes
    //changed from 24 hours to 15 minutes for enhanced security
    //@param username The username to put in the token
    //@return JWT access token as a string
    public String generateAccessToken(String username) {
        return generateAccessToken(username, 0);
    }

    public String generateAccessToken(String username, long tokenVersion) {
        //15 minutes in milliseconds
        long accessTokenExpiration = 900000;
        return createToken(username, accessTokenExpiration, tokenVersion);
    }

    /**
     * genersating new access tokens without re-entering password
     * @param username The username to put in the token
     * @return JWT refresh token as a string
     */
    public String generateRefreshToken(String username) {
        //set to 7 days in milliseconds
        long refreshTokenExpiration = 604800000;
        return createToken(username, refreshTokenExpiration);
    }

    /**
     * Helper method to create JWT token with custom expiration
     * @param username The username to put in the token
     * @param expirationMs The expiration time in milliseconds
     * @return JWT token as a string
     */
    private String createToken(String username, long expirationMs) {
        return createToken(username, expirationMs, 0);
    }

    private String createToken(String username, long expirationMs, long tokenVersion) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
            .claim("tokenVersion", tokenVersion)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public long getTokenVersionFromJWT(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        Object version = Jwts.parser()
                .setSigningKey(key)
                .parseClaimsJws(token)
                .getBody()
                .get("tokenVersion");
        return version instanceof Number ? ((Number) version).longValue() : 0;
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
