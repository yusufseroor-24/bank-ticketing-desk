package com.ga.bankdesk.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class JwtUtils {

    private final Logger logger = Logger.getLogger(JwtUtils.class.getName());

    @Value("${jwt-secret")
    private String jwtSecret;

    @Value("${jwt-expiration-ms")
    private long jwtExpirationMs;

    //converts secret key text into a key object
    private Key signingKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    //creates a token associated with the user email with the time of when it was made and when it expires
    public String generateToken(String email){
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    //gets the email from the token
    public String getEmailFromToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    //validates the token
    public boolean validateToken(String token){
        try{
            Jwts.parserBuilder().setSigningKey(signingKey()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e){
            logger.log(Level.WARNING, "Invalid JWT: {0}", e.getMessage());
            return false;
        }
    }

}
