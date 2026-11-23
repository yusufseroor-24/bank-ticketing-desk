package com.ga.bankdesk.security;

import com.ga.bankdesk.enums.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JwtUtils {

    private final Logger logger = Logger.getLogger(JwtUtils.class.getName());

    @Value("${jwt-secret}")
    private String jwtSecret;

    @Value("${jwt-expiration-ms}")
    private long jwtExpirationMs;

    //converts secret key text into a key object
    private SecretKey signingKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    //creates a token associated with the user email with the time of when it was made and when it expires
    //uid and role are added so the other services can read who the user is from the token (no call to identity-service)
    public String generateToken(String email, Long userId, Role role){
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    //gets the email from the token
    public String getEmailFromToken(String token){
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    //validates the token
    public boolean validateToken(String token){
        try{
            Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e){
            logger.log(Level.WARNING, "Invalid JWT: {0}", e.getMessage());
            return false;
        }
    }

}