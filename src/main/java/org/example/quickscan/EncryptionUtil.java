package org.example.quickscan;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * EncryptionUtil
 * - Derives AES key from password using PBKDF2WithHmacSHA256
 * - Uses AES/CBC/PKCS5Padding with random IV
 * - Returns a Base64 string formatted as: base64(salt) + ":" + base64(iv) + ":" + base64(ciphertext)
 *
 * NOTE: you can adjust ITERATIONS and KEY_LENGTH for your security/performance needs.
 */
public final class EncryptionUtil {

    private static final String KDF_ALGO = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";
    private static final int SALT_LENGTH = 16;           // bytes
    private static final int IV_LENGTH = 16;             // bytes (AES block size)
    private static final int ITERATIONS = 65536;         // PBKDF2 iterations
    private static final int KEY_LENGTH = 256;           // bits

    private static final SecureRandom RANDOM = new SecureRandom();

    private EncryptionUtil() { /* no instantiation */ }

    /**
     * Encrypts the plainText using a password and returns a Base64 encoded payload:
     * base64(salt) + ":" + base64(iv) + ":" + base64(ciphertext)
     */
    public static String encrypt(String plainText, String password) throws Exception {
        if (plainText == null) plainText = "";
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        // generate salt
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);

        // derive key
        SecretKey secretKey = deriveKey(password.toCharArray(), salt);

        // init cipher with random IV
        byte[] iv = new byte[IV_LENGTH];
        RANDOM.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

        // encode pieces using Base64 and join with colon
        String b64Salt = Base64.getEncoder().encodeToString(salt);
        String b64Iv = Base64.getEncoder().encodeToString(iv);
        String b64Cipher = Base64.getEncoder().encodeToString(cipherBytes);

        return b64Salt + ":" + b64Iv + ":" + b64Cipher;
    }

    /**
     * Decrypts the payload produced by encrypt(...) using the same password.
     * The payload must be: base64(salt) + ":" + base64(iv) + ":" + base64(ciphertext)
     */
    public static String decrypt(String payload, String password) throws Exception {
        if (payload == null) return null;
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty");
        }

        String[] parts = payload.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid payload format");
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] iv = Base64.getDecoder().decode(parts[1]);
        byte[] cipherBytes = Base64.getDecoder().decode(parts[2]);

        SecretKey secretKey = deriveKey(password.toCharArray(), salt);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] plainBytes = cipher.doFinal(cipherBytes);

        return new String(plainBytes, "UTF-8");
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGO);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}