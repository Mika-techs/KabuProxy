package de.mik.kabuproxy.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialCipherTest
{
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void roundTripsWithRandomIv()
    {
        CredentialCipher cipher = new CredentialCipher(KEY);
        String first = cipher.encrypt("geheim!", "user1");
        String second = cipher.encrypt("geheim!", "user1");

        assertTrue(cipher.isConfigured());
        assertNotEquals(first, second);
        assertEquals("geheim!", cipher.decrypt(first, "user1"));
        assertEquals("geheim!", cipher.decrypt(second, "user1"));
    }

    @Test
    void rejectsCiphertextOfOtherAccount()
    {
        CredentialCipher cipher = new CredentialCipher(KEY);
        String encrypted = cipher.encrypt("geheim!", "user1");

        assertThrows(IllegalStateException.class, () -> cipher.decrypt(encrypted, "user2"));
    }

    @Test
    void rejectsOtherKey()
    {
        String encrypted = new CredentialCipher(KEY).encrypt("geheim!", "user1");
        byte[] otherKey = new byte[32];
        otherKey[0] = 1;
        CredentialCipher other = new CredentialCipher(Base64.getEncoder().encodeToString(otherKey));

        assertThrows(IllegalStateException.class, () -> other.decrypt(encrypted, "user1"));
    }

    @Test
    void invalidKeysAreNotConfigured()
    {
        assertFalse(new CredentialCipher("").isConfigured());
        assertFalse(new CredentialCipher("not base64!!").isConfigured());
        assertFalse(new CredentialCipher(Base64.getEncoder().encodeToString(new byte[16])).isConfigured());
        assertThrows(IllegalStateException.class, () -> new CredentialCipher("").encrypt("x", "y"));
    }
}
