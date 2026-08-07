package com.nhnacademy.core.service;

import com.nhnacademy.core.domain.normalizer.TeamInvitationCodeNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

@Component
public final class InvitationCodeHasher {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_BYTES = 32;
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    private final SecretKey secretKey;

    public InvitationCodeHasher(
            @Value("${invitation-code.hmac-secret}") String base64Secret
    ) {
        this.secretKey = createSecretKey(base64Secret);
    }

    public String hash(String invitationCode) {
        String normalizedCode = TeamInvitationCodeNormalizer.normalizeCode(invitationCode);

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(secretKey);

            return HEX_FORMAT.formatHex(
                    mac.doFinal(normalizedCode.getBytes(StandardCharsets.UTF_8))
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("초대 코드 해시를 생성할 수 없습니다.", exception);
        }
    }

    private static SecretKey createSecretKey(String base64Secret) {
        byte[] secret = decodeSecret(base64Secret);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("초대 코드 HMAC Secret 키는 32바이트 이상이어야 합니다.");
        }

        return new SecretKeySpec(secret, HMAC_ALGORITHM);
    }

    private static byte[] decodeSecret(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("초대 코드 HMAC Secret 키가 비어있습니다.");
        }

        try {
            return Base64.getDecoder().decode(base64Secret.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("초대 코드 HMAC Secret 키는 Base64 형식이어야 합니다.", e);
        }
    }
}
