package com.mtg.mtgwalletbe.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Profile("!disabled-security")
@Service
public class JwtService {
    @Value("${mtgWallet.security.jwtSecretKey}")
    private String jwtSecretKey;
    @Value("${mtgWallet.security.jwtAccessTokenExpirationDuration}")
    private int jwtAccessTokenExpirationDuration;
    @Value("${mtgWallet.security.jwtRefreshTokenExpirationDuration}")
    private int jwtRefreshTokenExpirationDuration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(authoritiesToClaims(userDetails.getAuthorities()), userDetails);
    }

    private Map<String, Object> authoritiesToClaims(Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        if (authorities == null) {
            return claims;
        }
        claims.put("roles", authorities.stream().map(GrantedAuthority::getAuthority).toArray(String[]::new));
        return claims;
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtAccessTokenExpirationDuration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtRefreshTokenExpirationDuration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, int expirationDuration) {
        // Add the current time in nanoseconds and the current thread ID to the claims in order to make token unique
        extraClaims.put("jti", System.nanoTime() + "-" + Thread.currentThread().threadId());

        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + (long) expirationDuration * 60 * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
