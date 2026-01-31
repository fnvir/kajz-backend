package dev.fnvir.kajz.storageservice.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link UuidEncodeUtils}.
 */
public class UuidEncodeUtilsTest {

    @Test
    @DisplayName("encodeCrockford should encode UUID to 26-character lowercase string")
    void encodeCrockford_shouldEncodeUuidCorrectly() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        
        String encoded = UuidEncodeUtils.encodeCrockford(uuid);
        
        assertNotNull(encoded);
        assertEquals(26, encoded.length());
        assertTrue(encoded.equals(encoded.toLowerCase()), "Encoded string should be lowercase");
    }

    @Test
    @DisplayName("decodeCrockford should decode back to original UUID")
    void decodeCrockford_shouldDecodeToOriginalUuid() {
        UUID original = UUID.randomUUID();
        
        String encoded = UuidEncodeUtils.encodeCrockford(original);
        UUID decoded = UuidEncodeUtils.decodeCrockford(encoded);
        
        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("decodeCrockford should be case-insensitive")
    void decodeCrockford_shouldBeCaseInsensitive() {
        UUID original = UUID.randomUUID();
        String encoded = UuidEncodeUtils.encodeCrockford(original);
        
        UUID fromLower = UuidEncodeUtils.decodeCrockford(encoded.toLowerCase());
        UUID fromUpper = UuidEncodeUtils.decodeCrockford(encoded.toUpperCase());
        
        assertEquals(original, fromLower);
        assertEquals(original, fromUpper);
    }

    @Test
    @DisplayName("decodeCrockford should throw exception for null input")
    void decodeCrockford_shouldThrowForNullInput() {
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.decodeCrockford(null));
    }

    @Test
    @DisplayName("decodeCrockford should throw exception for empty input")
    void decodeCrockford_shouldThrowForEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.decodeCrockford(""));
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.decodeCrockford("   "));
    }

    @Test
    @DisplayName("decodeCrockford should throw exception for wrong length")
    void decodeCrockford_shouldThrowForWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.decodeCrockford("abc123"));
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.decodeCrockford("a".repeat(30)));
    }

    @Test
    @DisplayName("decodeCrockford should handle hyphens in input")
    void decodeCrockford_shouldHandleHyphens() {
        UUID original = UUID.randomUUID();
        String encoded = UuidEncodeUtils.encodeCrockford(original);
        
        // Add hyphens in the middle
        String withHyphens = encoded.substring(0, 8) + "-" + encoded.substring(8, 16) + "-" + encoded.substring(16);
        
        UUID decoded = UuidEncodeUtils.decodeCrockford(withHyphens);
        assertEquals(original, decoded);
    }

    @Test
    @DisplayName("uuidToBytes should convert UUID to 16-byte array")
    void uuidToBytes_shouldConvertTo16Bytes() {
        UUID uuid = UUID.randomUUID();
        
        byte[] bytes = UuidEncodeUtils.uuidToBytes(uuid);
        
        assertNotNull(bytes);
        assertEquals(16, bytes.length);
    }

    @Test
    @DisplayName("bytesToUuid should convert 16-byte array back to UUID")
    void bytesToUuid_shouldConvertBackToUuid() {
        UUID original = UUID.randomUUID();
        
        byte[] bytes = UuidEncodeUtils.uuidToBytes(original);
        UUID restored = UuidEncodeUtils.bytesToUuid(bytes);
        
        assertEquals(original, restored);
    }

    @Test
    @DisplayName("bytesToUuid should throw exception for wrong byte array length")
    void bytesToUuid_shouldThrowForWrongLength() {
        byte[] tooShort = new byte[8];
        byte[] tooLong = new byte[20];
        
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.bytesToUuid(tooShort));
        assertThrows(IllegalArgumentException.class, () -> UuidEncodeUtils.bytesToUuid(tooLong));
    }

    @Test
    @DisplayName("encode and decode should work for multiple random UUIDs")
    void encodeAndDecode_shouldWorkForMultipleUuids() {
        for (int i = 0; i < 10; i++) {
            UUID original = UUID.randomUUID();
            String encoded = UuidEncodeUtils.encodeCrockford(original);
            UUID decoded = UuidEncodeUtils.decodeCrockford(encoded);
            assertEquals(original, decoded, "Round-trip failed for UUID: " + original);
        }
    }

    @Test
    @DisplayName("encode should handle min and max UUID values")
    void encode_shouldHandleEdgeCases() {
        UUID minUuid = new UUID(0L, 0L);
        UUID maxUuid = new UUID(-1L, -1L);
        
        String encodedMin = UuidEncodeUtils.encodeCrockford(minUuid);
        String encodedMax = UuidEncodeUtils.encodeCrockford(maxUuid);
        
        assertEquals(minUuid, UuidEncodeUtils.decodeCrockford(encodedMin));
        assertEquals(maxUuid, UuidEncodeUtils.decodeCrockford(encodedMax));
    }
}
