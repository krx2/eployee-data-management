package com.krx2.employeedatamanagement.crypto;

public final class SsnMasker {

    private SsnMasker() {
    }

    public static String mask(String plaintextSsn) {
        if (plaintextSsn == null) {
            throw new IllegalArgumentException("plaintextSsn must not be null");
        }
        String digitsOnly = plaintextSsn.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 4) {
            throw new IllegalArgumentException("plaintextSsn must contain at least 4 digits");
        }
        String last4 = digitsOnly.substring(digitsOnly.length() - 4);
        return "***-**-" + last4;
    }
}
