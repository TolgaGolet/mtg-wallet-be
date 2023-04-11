package com.mtg.mtgwalletbe.security.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtg.mtgwalletbe.security.dto.CustomAuthenticationFilterConstructorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mtg.mtgwalletbe.security.SecurityParams.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final AuthenticationManager authenticationManager;
    private final String jwtSecretKey;
    private final int jwtAccessTokenExpirationDuration;
    private final int jwtRefreshTokenExpirationDuration;

    public CustomAuthenticationFilter(CustomAuthenticationFilterConstructorDTO customAuthenticationFilterConstructorDTO) {
        this.authenticationManager = customAuthenticationFilterConstructorDTO.getAuthenticationManager();
        this.jwtSecretKey = customAuthenticationFilterConstructorDTO.getJwtSecretKey();
        this.jwtAccessTokenExpirationDuration = customAuthenticationFilterConstructorDTO.getJwtAccessTokenExpirationDuration();
        this.jwtRefreshTokenExpirationDuration = customAuthenticationFilterConstructorDTO.getJwtRefreshTokenExpirationDuration();
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        return authenticationManager.authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        User user = (User) authentication.getPrincipal();
        Algorithm algorithm = Algorithm.HMAC256(jwtSecretKey.getBytes());
        String accessToken = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtAccessTokenExpirationDuration * 60 * 1000))
                .withIssuer(request.getRequestURL().toString())
                .withClaim(JWT_TOKEN_ROLES_CLAIM_KEY, user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .withClaim(JWT_TOKEN_USERNAME_CLAIM_KEY, user.getUsername())
                .sign(algorithm);
        String refreshToken = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtRefreshTokenExpirationDuration * 60 * 1000))
                .withIssuer(request.getRequestURL().toString())
                .sign(algorithm);
        Map<String, String> tokens = new HashMap<>();
        tokens.put(JWT_ACCESS_TOKEN_KEY, accessToken);
        tokens.put(JWT_REFRESH_TOKEN_KEY, refreshToken);
        response.setContentType(APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getOutputStream(), tokens);
    }
}
