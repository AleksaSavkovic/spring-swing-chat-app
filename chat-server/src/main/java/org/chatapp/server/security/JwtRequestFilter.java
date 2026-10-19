package org.chatapp.server.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@AllArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private CustomUserDetailsService userDetailsService;
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Getting the authorization header from the request
        final String authorizationHeader = request.getHeader("Authorization");

        String id = null;
        String jwt = null;

        // Checking if the authorization header has content, and it starts with Bearer
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                id = jwtUtil.extractId(jwt);
            } catch (JwtException e) {
                // invalid token → ignore and continue
                chain.doFilter(request, response);
                return;
            }
        }

        // Checks if the username is non-null and the user hasn't authenticated yet
        if (id != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails = (CustomUserDetails) this.userDetailsService.loadUserById(id);

            if(jwtUtil.validateToken(jwt, userDetails)) {
                // Creating a new SecurityContext instead of setting authentication on the existing context in order
                // to avoid race conditions across multiple threads.
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(authentication);

                // Set the new context in the SecurityContextHolder
                SecurityContextHolder.setContext(context);
            } else {
                log.info("Validate tokens failed");
            }
        }

        chain.doFilter(request, response);
    }
}
