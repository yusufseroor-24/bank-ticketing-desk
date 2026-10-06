package com.ga.bankdesk.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
//tracks endpoints and how many requests are made / if they exceed the limit (5 requests per min)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/auth/login", "/api/auth/register", "/api/auth/forget-password");

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException{
        if(RATE_LIMITED_PATHS.contains(request.getRequestURI())){
            String key = request.getRemoteAddr() + ":" + request.getRequestURI();
            //look for a bucket with key, if it doesn't exist create one
            Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());

            if(!bucket.tryConsume(1)){
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many requests, please try again later\"}");
                return; //stops
            }
        }
        //continue
        filterChain.doFilter(request, response);
    }

    private Bucket newBucket(){
        //5 requests, and refill the requests after 1 min
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
