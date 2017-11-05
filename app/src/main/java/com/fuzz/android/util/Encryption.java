package com.fuzz.android.util;

import android.util.Base64;
import android.util.Log;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryption/decryption methods.
 */
public abstract class Encryption {
    private static byte[] key = {
            6, 23, 112, 79, 20, 80, 67, 47
    };
    private static byte[] iv = {
            0, 36, 23, 72, 64, 120, 19, 60
    };

    private Encryption() {

    }

    private static Cipher initCipher(boolean forEncrypt) {
        try {
            SecretKeySpec key = new SecretKeySpec(Encryption.key, "DES");
            IvParameterSpec iv = new IvParameterSpec(Encryption.iv);

            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(forEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, iv);

            return cipher;
        } catch (Exception ex) {
            Log.e(Encryption.class.getSimpleName(), "Cipher error", ex);
            return null;
        }
    }

    public static String encrypt(String source) {
        Cipher cipher = initCipher(true);

        byte[] input = source.getBytes();

        try {

            byte[] enc = cipher.doFinal(input);

            return Base64.encodeToString(enc, Base64.DEFAULT);

        } catch (Exception ex) {
            Log.e(Encryption.class.getSimpleName(), "Encrypt error", ex);
            return null;
        }
    }


    public static String decrypt(String encrypted) {
        Cipher cipher = initCipher(false);

        try {
            byte[] encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT);
            byte[] decrypted = cipher.doFinal(encryptedBytes);

            return new String(decrypted);
        } catch (Exception ex) {
            Log.e(Encryption.class.getSimpleName(), "Decrypt error", ex);
            return null;
        }
    }
}
