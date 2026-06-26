package com.mdb.user_data_gateway_service.utils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils implements Serializable {

    private static final long serialVersionUID = 8874940660777362064L;
    private static final int ITERATIONS = 20 * 1000;
    private static final int SALTLEN = 32;
    private static final int DESIREKEYLEN = 256;

    private PasswordUtils() {}

    public static String getSaltedHash(String password) throws Exception {
        byte[] salt = SecureRandom.getInstance("SHA1PRNG").generateSeed(SALTLEN);
        return Base64.getEncoder().encodeToString(salt) + "$" + hash(password, salt);
    }

    public static boolean check(String password, String stored) throws Exception {
        String[] saltAndPass = stored.split("\\$");
        if (saltAndPass.length != 2) {
            throw new IllegalStateException("The stored password have the form 'salt$hash'");
        }
        String hashOfInput = hash(password, Base64.getDecoder().decode(saltAndPass[0]));
        return hashOfInput.equals(saltAndPass[1]);
    }

    private static String hash(String password, byte[] salt) throws Exception {
        if (password == null || password.length() == 0)
            throw new IllegalArgumentException("Empty passwords are not supported.");
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        SecretKey key = f.generateSecret(new PBEKeySpec(
                password.toCharArray(), salt, ITERATIONS, DESIREKEYLEN)
        );
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
