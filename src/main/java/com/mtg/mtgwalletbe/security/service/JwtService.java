package com.mtg.mtgwalletbe.security.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Profile("!disabled-security")
@Service
public class JwtService {
    public static final String TOKEN_TYPE_CLAIM = "typ";
    public static final String ACCESS_TOKEN_TYPE = "access";
    public static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey signingKey;
    private final JwtParser jwtParser;
    private final int jwtAccessTokenExpirationDuration;
    private final int jwtRefreshTokenExpirationDuration;

    public JwtService(@Value("${mtgWallet.security.jwtSecretKey}") String jwtSecretKey,
                      @Value("${mtgWallet.security.jwtAccessTokenExpirationDuration}") int jwtAccessTokenExpirationDuration,
                      @Value("${mtgWallet.security.jwtRefreshTokenExpirationDuration}") int jwtRefreshTokenExpirationDuration) {
        // Key and parser built once, not on every sign/verify
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
        this.jwtParser = Jwts.parser().verifyWith(signingKey).build(); // immutable, thread-safe
        this.jwtAccessTokenExpirationDuration = jwtAccessTokenExpirationDuration;
        this.jwtRefreshTokenExpirationDuration = jwtRefreshTokenExpirationDuration;
    }

    /**
     * Verifies the signature and expiry and returns the claims. Parse once per request and reuse the result.
     */
    public Claims parseClaims(String token) {
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    /**
     * @return "access", "refresh", or null for tokens issued before the claim existed
     */
    public static String tokenTypeOf(Claims claims) {
        return claims.get(TOKEN_TYPE_CLAIM, String.class);
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
        extraClaims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        return buildToken(extraClaims, userDetails, jwtAccessTokenExpirationDuration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        return buildToken(claims, userDetails, jwtRefreshTokenExpirationDuration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, int expirationDuration) {
        long now = System.currentTimeMillis();
        return Jwts
                .builder()
                .claims(extraClaims)
                .id(UUID.randomUUID().toString()) // unique jti, so two tokens issued in the same instant differ
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + (long) expirationDuration * 60 * 1000))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(Claims claims, UserDetails userDetails) {
        return userDetails.getUsername().equals(claims.getSubject()) && claims.getExpiration().after(new Date());
    }
}
