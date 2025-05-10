package com.dsm.utils;

import com.dsm.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:your-secret-key}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private Long expiration;

    /**
     * 从token中获取用户名
     *
     * @param token JWT token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从token中获取过期时间
     *
     * @param token JWT token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从token中获取指定的claim
     *
     * @param token JWT token
     * @param claimsResolver claim解析器
     * @return claim值
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中获取所有claims
     *
     * @param token JWT token
     * @return 所有claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 检查token是否过期
     *
     * @param token JWT token
     * @return 是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 生成token
     *
     * @param user 用户信息
     * @return JWT token
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("level", user.getLevel());
        return doGenerateToken(claims, user.getUsername());
    }

    /**
     * 生成token的具体方法
     *
     * @param claims 需要存储的数据
     * @param subject 主题（用户名）
     * @return JWT token
     */
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 验证token
     *
     * @param token JWT token
     * @param userDetails 用户信息
     * @return 是否有效
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 从token中获取当前用户信息
     *
     * @return 用户信息
     */
    public User getCurrentUser() {
        // 从SecurityContext中获取认证信息
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) {
            return null;
        }
        
        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        
        // 从token中获取用户等级
        String token = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getHeader("Authorization").substring(7);
        Claims claims = getAllClaimsFromToken(token);
        user.setLevel((String) claims.get("level"));
        
        return user;
    }
} 