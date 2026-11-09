package com.ga.bankdesk.security;

import com.ga.bankdesk.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.logging.Level;
import java.util.logging.Logger;

//only validates and reads tokens, tokens are created by identity-service (same JWT_SECRET)
@Component
public class JwtUtils {

    private final Logger logger = Logger.getLogger(JwtUtils.class.getName());

    @Value("${jwt-secret}")
    private String jwtSecret;

    private Key signingKey(){
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    //validates the token and returns the user inside it (null if invalid)
    public CurrentUser parse(String token){
        try{
            Claims claims = Jwts.parserBuilder().setSigningKey(signingKey()).build()
                    .parseClaimsJws(token).getBody();
            Object uid = claims.get("uid");
            Object role = claims.get("role");
            if (uid == null || role == null) {
                logger.log(Level.WARNING, "JWT has no uid/role claim");
                return null;
            }
            return new CurrentUser(((Number) uid).longValue(), claims.getSubject(), Role.valueOf(role.toString()));
        } catch (Exception e){
            logger.log(Level.WARNING, "Invalid JWT: {0}", e.getMessage());
            return null;
        }
    }
}
