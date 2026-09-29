package com.petmgmt.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class TokenRevocationService {
    private final JdbcTemplate jdbc;
    private final JwtTokenProvider tokens;
    public TokenRevocationService(JdbcTemplate jdbc,JwtTokenProvider tokens) {
        this.jdbc=jdbc; this.tokens=tokens;
    }
    public boolean isRevoked(String token) {
        String id=tokens.getTokenId(token);
        if (id==null) return true;
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM revoked_tokens WHERE token_id=?",Integer.class,hash(id));
        return count!=null && count>0;
    }
    public void revoke(String token) {
        if (!tokens.validateToken(token)) return;
        String id=tokens.getTokenId(token);
        if (id!=null && !isRevoked(token)) {
            jdbc.update("INSERT INTO revoked_tokens (token_id,expires_at) VALUES (?,?)",
                    hash(id),new Timestamp(tokens.getExpiration(token).getTime()));
        }
    }

    @Scheduled(fixedDelay=3_600_000)
    public void removeExpired() {
        jdbc.update("DELETE FROM revoked_tokens WHERE expires_at<CURRENT_TIMESTAMP");
    }

    private String hash(String id) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(id.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
