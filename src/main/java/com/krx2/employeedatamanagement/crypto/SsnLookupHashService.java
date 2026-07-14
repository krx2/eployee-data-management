package com.krx2.employeedatamanagement.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class SsnLookupHashService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public SsnLookupHashService(@Value("${app.ssn-lookup-pepper}") String pepper) {
        this.key = new SecretKeySpec(pepper.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
    }

    public String hash(String plaintextSsn) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            byte[] rawHmac = mac.doFinal(plaintextSsn.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to compute SSN lookup hash", e);
        }
    }
}
