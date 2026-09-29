package com.petmgmt.security;

import com.petmgmt.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokens;
    private final UserRepository users;
    private final RbacMapper rbac;
    private final TokenRevocationService revocations;
    public JwtAuthenticationFilter(JwtTokenProvider tokens, UserRepository users, RbacMapper rbac, TokenRevocationService revocations) {
        this.tokens = tokens; this.users = users; this.rbac = rbac; this.revocations=revocations;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (!"token".equals(cookie.getName()) || !tokens.validateToken(cookie.getValue())
                        || revocations.isRevoked(cookie.getValue())) continue;
                Long id = tokens.getUserIdFromToken(cookie.getValue());
                if (users.existsById(id)) {
                    var authorities = rbac.permissionsForUser(id).stream().map(SimpleGrantedAuthority::new).toList();
                    SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(id, null, authorities));
                }
                break;
            }
        }
        chain.doFilter(request, response);
    }
}
