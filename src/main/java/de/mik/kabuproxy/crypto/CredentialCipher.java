package de.mik.kabuproxy.crypto;

import de.mik.kabuproxy.config.KabuConfig;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM for stored digikabu passwords. Output is base64(iv || ciphertext+tag); the digikabu
 * username is bound as additional authenticated data so a ciphertext can't be moved to another account.
 */
@ApplicationScoped
public class CredentialCipher
{
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();

    @Inject private KabuConfig config;

    private SecretKey key;

    public CredentialCipher()
    {
    }

    CredentialCipher(String base64Key)
    {
        this.key = parseKey(base64Key);
    }

    public boolean isConfigured()
    {
        return key != null;
    }

    public String encrypt(String plaintext, String associatedData)
    {
        requireKey();
        try
        {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    public String decrypt(String encoded, String associatedData)
    {
        requireKey();
        try
        {
            byte[] data = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException e)
        {
            throw new IllegalStateException("decryption failed - wrong KABU_CRED_KEY?", e);
        }
    }

    @PostConstruct
    void init()
    {
        if (key == null)
        {
            key = parseKey(config.getCredKey());
        }
    }

    private void requireKey()
    {
        if (key == null)
        {
            throw new IllegalStateException("KABU_CRED_KEY is not configured");
        }
    }

    private static SecretKey parseKey(String base64Key)
    {
        if (base64Key == null || base64Key.isBlank())
        {
            return null;
        }
        byte[] raw;
        try
        {
            raw = Base64.getDecoder().decode(base64Key.trim());
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
        return raw.length == KEY_BYTES ? new SecretKeySpec(raw, "AES") : null;
    }
}
