package com.krx2.employeedatamanagement.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsnMaskerTest {

    @Test
    void masksAllButLastFourDigits() {
        assertThat(SsnMasker.mask("123-45-6789")).isEqualTo("***-**-6789");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> SsnMasker.mask(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsFewerThanFourDigits() {
        assertThatThrownBy(() -> SsnMasker.mask("12"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
