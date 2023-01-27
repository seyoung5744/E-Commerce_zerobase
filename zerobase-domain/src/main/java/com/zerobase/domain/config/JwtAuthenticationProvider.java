package com.zerobase.domain.config;

import com.zerobase.domain.domain.common.UserType;
import com.zerobase.domain.domain.common.UserVo;
import com.zerobase.domain.util.Aes256Util;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.Objects;

public class JwtAuthenticationProvider {

    private static final String KEY_ROLES = "roles";
    private static final long TOKEN_VALID_TIME = 1000L * 60 * 60 * 24;

    private String secretKey = "secretKey";

    public String createToken(String userPk, Long id, UserType userType) {
        Claims claims = Jwts.claims().setSubject(Aes256Util.encrypt(userPk)).setId(Aes256Util.encrypt(id.toString()));
        claims.put(KEY_ROLES, userType);
        Date now = new Date();
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + TOKEN_VALID_TIME))
            .signWith(SignatureAlgorithm.HS256, this.secretKey)
            .compact();
    }

    public boolean isValidateToken(String jwtToken) {
        Claims claims = parseClaims(jwtToken);

        // 만료 시간이 현재 시간보다 적을 때만 유효함.
        return !claims.getExpiration().before(new Date());
    }

    // token을 받아서 user로 변환
    public UserVo getUserVo(String token) {
        Claims claims = this.parseClaims(token);

        return new UserVo(
            Long.valueOf(Objects.requireNonNull(Aes256Util.decrypt(claims.getId()))),
            Aes256Util.decrypt(claims.getSubject())
        );
    }

    private Claims parseClaims(String jwtToken) {
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(jwtToken).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
