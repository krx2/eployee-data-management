package com.krx2.employeedatamanagment.crypto;

public final class SsnMasker {

    private SsnMasker() {
    }

    public static String mask(String plaintextSsn) {
        String digitsOnly = plaintextSsn.replaceAll("[^0-9]", "");
        String last4 = digitsOnly.substring(digitsOnly.length() - 4);
        return "***-**-" + last4;
    }
}
