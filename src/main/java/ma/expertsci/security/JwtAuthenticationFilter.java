package ma.expertsci.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request); // Extract token from header or cookies

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Extract username from the token
            String username = jwtService.extractEmail(token);

            // Extract role from the token
            String role = jwtService.extractClaim(
                    token,
                    claims -> claims.get("role", String.class)
            );

            if (username != null) {
                // Set authentication in the SecurityContextHolder
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    // Helper method to extract token either from Authorization header or cookies
    private String extractToken(HttpServletRequest request) {
        // First check the Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Extract token from Authorization header
        }

        // Fallback: Check for JWT token in cookies
        Optional<String> jwtCookie = getJwtFromCookies(request);
        return jwtCookie.orElse(null);
    }

    // Helper method to get JWT token from cookies
    private Optional<String> getJwtFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("JWT".equals(cookie.getName())) { // Look for the JWT cookie
                    return Optional.of(cookie.getValue());
                }
            }
        }
        return Optional.empty();
    }
}


