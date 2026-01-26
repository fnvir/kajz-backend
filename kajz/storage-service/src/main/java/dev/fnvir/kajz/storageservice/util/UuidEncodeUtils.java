package dev.fnvir.kajz.storageservice.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidEncodeUtils {
    
    private static final char[] ENCODE_TABLE = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final short[] DECODE_TABLE = new short[128];

    static {
        for (short i = 0; i < DECODE_TABLE.length; i++)
            DECODE_TABLE[i] = -1;
        for (short i = 0; i < ENCODE_TABLE.length; i++) {
            char c = ENCODE_TABLE[i];
            DECODE_TABLE[c] = i;
            if (Character.isLetter(c))
                DECODE_TABLE[Character.toLowerCase(c)] = i; // case-insensitive
        }
        // aliases per Crockford spec
        DECODE_TABLE['O'] = DECODE_TABLE['o'] = 0;
        DECODE_TABLE['I'] = DECODE_TABLE['i'] = DECODE_TABLE['L'] = DECODE_TABLE['l'] = 1;
    }
    
    public static String encodeCrockford(UUID uuid) {
        byte[] bytes = uuidToBytes(uuid);
        StringBuilder sb = new StringBuilder(26);
        int bitBuffer = 0, bitCount = 0;
        for (byte b : bytes) {
            bitBuffer = (bitBuffer << 8) | (b & 0xFF);
            bitCount += 8;
            while (bitCount >= 5) {
                bitCount -= 5;
                sb.append(ENCODE_TABLE[(bitBuffer >> bitCount) & 0x1F]);
            }
        }
        if (bitCount > 0) {
            sb.append(ENCODE_TABLE[(bitBuffer << (5 - bitCount)) & 0x1F]);
        }
        return sb.toString().toLowerCase();
    }

    public static UUID decodeCrockford(String base32) {
        if (base32 == null || base32.isBlank()) {
            throw new IllegalArgumentException("Encoded string cannot be null or empty");
        }

        base32 = base32.strip().replace("-", ""); // remove hyphens for flexibility

        if (base32.length() != 26) {
            throw new IllegalArgumentException("Encoded UUID must be 26 characters long, got: " + base32.length());
        }
        
        int bitBuffer = 0, bitCount = 0, byteCount = 0;
        byte[] bytes = new byte[16];
        for (char c : base32.toCharArray()) {
            if (c >= DECODE_TABLE.length || DECODE_TABLE[c] < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }
            bitBuffer = (bitBuffer << 5) | DECODE_TABLE[c];
            bitCount += 5;
            if (bitCount >= 8) {
                bitCount -= 8;
                if (byteCount < 16) {
                    bytes[byteCount++] = (byte) ((bitBuffer >> bitCount) & 0xFF);
                }
            }
        }
        return bytesToUuid(bytes);
    }
    
    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
    
    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Byte array must be 16 bytes long to represent a UUID.");
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long msb = bb.getLong();
        long lsb = bb.getLong();
        return new UUID(msb, lsb);
    }
}
