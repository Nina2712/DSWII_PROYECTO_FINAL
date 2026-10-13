package com.medicita.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(UserDetails usuario) {
        Date ahora = new Date();
        String rol = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).findFirst().orElse("");
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMinutos * 60_000))
                .signWith(key)
                .compact();
    }

    public String extraerUsername(String token) {
        return claims(token).getSubject();
    }

    public boolean esValido(String token, UserDetails usuario) {
        try {
            return claims(token).getSubject().equals(usuario.getUsername());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpiracionMinutos() {
        return expiracionMinutos;
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
