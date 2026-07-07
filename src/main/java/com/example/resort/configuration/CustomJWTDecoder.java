package com.example.resort.configuration;

import com.example.resort.repository.InValidatedTokenRepository;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CustomJWTDecoder implements JwtDecoder {

    @Value("${jwt.signerKey}")
    private String signerKey;

    private final InValidatedTokenRepository inValidatedTokenRepository;

    private NimbusJwtDecoder nimbusJwtDecoder;

    @Override
    public Jwt decode(String token) throws JwtException
    {
        try
        {
            verifyToken(token);
        }
        catch (Exception e)
        {
            throw new JwtException(e.getMessage());
        }
        if (Objects.isNull(nimbusJwtDecoder))
        {
            SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
            nimbusJwtDecoder = NimbusJwtDecoder
                    .withSecretKey(secretKeySpec)
                    .macAlgorithm(MacAlgorithm.HS512)
                    .build();
        }
        return nimbusJwtDecoder.decode(token);
    }

    private void verifyToken(String token)
        throws Exception
    {
        SignedJWT signedJWT = SignedJWT.parse(token);

        boolean verified = signedJWT.verify(new MACVerifier(signerKey.getBytes()));
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        if (!verified || !expiryTime.after(new Date()))
        {
            throw new JwtException("Token invalid or expired");
        }

        String tokenId = signedJWT.getJWTClaimsSet().getJWTID();

        if (inValidatedTokenRepository.existsById(tokenId))
        {
            throw new JwtException("Token has been invalidate");
        }
    }

}
