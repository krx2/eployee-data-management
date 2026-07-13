package com.krx2.employeedatamanagement.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SsnEncryptionServiceTest {

    private final SsnEncryptionService service =
            new SsnEncryptionService(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void encryptThenDecryptReturnsOriginalValue() {
        String ciphertext = service.encrypt("123-45-6789");

        assertThat(service.decrypt(ciphertext)).isEqualTo("123-45-6789");
    }

    @Test
    void encryptingSameValueTwiceProducesDifferentCiphertext() {
        String first = service.encrypt("123-45-6789");
        String second = service.encrypt("123-45-6789");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void ciphertextDoesNotContainOriginalPlaintext() {
        String ciphertext = service.encrypt("123-45-6789");

        assertThat(ciphertext).doesNotContain("123-45-6789");
    }
}
