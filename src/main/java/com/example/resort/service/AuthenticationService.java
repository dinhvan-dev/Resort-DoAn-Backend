package com.example.resort.service;

import com.example.resort.dto.request.AuthenticationRequest;
import com.example.resort.dto.request.IntrospectRequest;
import com.example.resort.dto.request.RefreshTokenRequest;
import com.example.resort.dto.response.AuthenticationResponse;
import com.example.resort.dto.response.IntrospectResponse;
import com.example.resort.entity.InvalidatedToken;
import com.example.resort.entity.RefreshToken;
import com.example.resort.entity.User;
import com.example.resort.exception.AppException;
import com.example.resort.exception.ErrorCode;
import com.example.resort.repository.InValidatedTokenRepository;
import com.example.resort.repository.RefreshTokenRepository;
import com.example.resort.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    private final UserRepository userRepository;
    private  final PasswordEncoder passwordEncoder;
    private final InValidatedTokenRepository inValidatedTokenRepository;
    private final RefreshTokenRepository  refreshTokenRepository;


    @Value("${jwt.signerKey}")
    protected String signerKey;

    private static final long ACCESS_TOKEN_EXPIRY_HOUSE = 1;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;


    // Đăng nhập trả về token
    @Transactional
    public AuthenticationResponse authenticated(AuthenticationRequest request)
    {
        log.info("Login attempt for username: {}", request.getUsername());

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getUsername());
                    return new AppException(ErrorCode.USER_NOT_EXISTS);
                });

        log.info("User found: {}, checking password...", user.getUsername());
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("Password match result: {}", authenticated);

        if (!authenticated)
        {
            log.error("Password mismatch for user: {}", request.getUsername());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String accessToken = generateAccessToken(user);
        String refreshTokenValue = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenValue)
                .authenticated(true)
                .build();
    }


    private String generateAccessToken(User user)
    {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(user.getUsername())
                .issuer("resort-app")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .claim("scope", "ROLE_"+ user.getRole().name())
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try
        {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();

        }
        catch (JOSEException e)
        {
            throw new RuntimeException("Cannot Generate token", e);
        }
    }

    private String generateRefreshToken(User user)
    {
        String tokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .username(user.getUsername())
                .expiryTime(LocalDateTime.now().plusDays(REFRESH_TOKEN_EXPIRY_DAYS))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
    }

    // dùng refresh token để lấy token mới
    @Transactional
    public AuthenticationResponse refreshToken(RefreshTokenRequest request)
    {
        String requestToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (refreshToken.isRevoked())
        {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REVOKED);
        }

        if (refreshToken.getExpiryTime().isBefore(LocalDateTime.now()))
        {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        User user = userRepository.findByUsername(refreshToken.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTS));

        String newAccessToken = generateAccessToken(user);

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        String newRefreshToken = generateRefreshToken(user);

        log.info("Refresh token rotated for user {}", user.getUsername());

        return AuthenticationResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .authenticated(true)
                .build();

    }

    // kiểm tra token còn hợp lệ không
    public IntrospectResponse introspect(IntrospectRequest request)
        throws JOSEException, ParseException
    {
        String token = request.getToken();
        try
        {
            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean verified = signedJWT.verify(new MACVerifier(signerKey.getBytes()));
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            boolean notInvalidate = !inValidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID());

            return IntrospectResponse.builder()
                    .valid(verified && expiryTime.after(new Date()) && notInvalidate)
                    .build();
        }
        catch (Exception e)
        {
            return IntrospectResponse.builder().valid(false).build();
        }
    }

    @Transactional
    public void logout (IntrospectRequest request)
        throws ParseException
    {
        SignedJWT signedJWT = SignedJWT.parse(request.getToken());

        String tokenId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        String username = signedJWT.getJWTClaimsSet().getSubject();

        if (expiryTime.after(new Date()))
        {
            inValidatedTokenRepository.save(
                    InvalidatedToken.builder()
                            .tokenId(tokenId)
                            .expiryTime(expiryTime)
                            .build()
            );

        }
        refreshTokenRepository.deleteByUsername(username);
    }

    // dọn dẹp token
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens()
    {
        refreshTokenRepository.deleteExpiredRevokedTokens(LocalDateTime.now());
        log.info("Cleaning expired refresh tokens");
    }

}
