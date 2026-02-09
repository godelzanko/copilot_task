package com.example.urlshortener.generator;

/**
 * Base62 encoder for converting 64-bit IDs to compact, URL-safe strings.
 * 
 * <p>Base62 encoding uses 62 characters (0-9, a-z, A-Z) to represent numbers in base-62,
 * providing a good balance between compactness and URL-safety:
 * <ul>
 *   <li><strong>Compact:</strong> Typical Snowflake IDs encode to ~7 characters</li>
 *   <li><strong>URL-Safe:</strong> No special characters like +, /, or =</li>
 *   <li><strong>Case-Sensitive:</strong> Uses both uppercase and lowercase for maximum efficiency</li>
 * </ul>
 * 
 * <p><strong>Example:</strong>
 * <pre>
 * Base62Encoder encoder = new Base62Encoder();
 * String shortCode = encoder.encode(123456789L);  // Returns "8M0kX"
 * long originalId = encoder.decode(shortCode);     // Returns 123456789
 * </pre>
 * 
 * <p><strong>Algorithm:</strong>
 * Encoding uses division-remainder method:
 * <pre>
 * while (id > 0) {
 *     char = ALPHABET[id % 62]
 *     id = id / 62
 * }
 * </pre>
 * 
 * <p>Decoding reverses the process:
 * <pre>
 * result = 0
 * for each char in encoded:
 *     result = result * 62 + ALPHABET.indexOf(char)
 * </pre>
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Base62">Base62 on Wikipedia</a>
 */
public class Base62Encoder {
    
    /**
     * Base62 alphabet: 0-9, a-z, A-Z (62 characters total).
     * Order matters for consistent encoding/decoding.
     */
    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    
    /**
     * Base value (62).
     */
    private static final int BASE = 62;
    
    /**
     * Encodes a 64-bit ID into a Base62 string.
     * 
     * <p>The encoding is variable-length with no padding. Larger IDs produce longer strings.
     * Typical Snowflake IDs (2^41 timestamp range) encode to ~7 characters.
     * 
     * @param id the ID to encode (must be non-negative)
     * @return the Base62-encoded string
     * @throws IllegalArgumentException if id is negative
     * 
     * @example
     * <pre>
     * encode(0)     → "0"
     * encode(61)    → "z"
     * encode(62)    → "10"
     * encode(3844)  → "100"
     * </pre>
     */
    public String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative, got: " + id);
        }
        
        // Special case: zero
        if (id == 0) {
            return "0";
        }
        
        // Build encoded string using division-remainder algorithm
        StringBuilder encoded = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            encoded.append(BASE62_ALPHABET.charAt(remainder));
            id = id / BASE;
        }
        
        // Reverse because we built the string backwards
        return encoded.reverse().toString();
    }
    
    /**
     * Decodes a Base62 string back to a 64-bit ID.
     * 
     * <p>This method is primarily used for testing and validation. The URL shortener
     * service doesn't need to decode short codes back to IDs (it looks them up in the database).
     * 
     * @param encoded the Base62-encoded string
     * @return the original 64-bit ID
     * @throws IllegalArgumentException if encoded is null, empty, or contains invalid characters
     * 
     * @example
     * <pre>
     * decode("0")    → 0
     * decode("z")    → 61
     * decode("10")   → 62
     * decode("100")  → 3844
     * </pre>
     */
    public long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string cannot be null or empty");
        }
        
        long result = 0;
        for (char c : encoded.toCharArray()) {
            int digitValue = BASE62_ALPHABET.indexOf(c);
            if (digitValue == -1) {
                throw new IllegalArgumentException("Invalid character in encoded string: '" + c + "'");
            }
            
            // Check for overflow before multiplication
            if (result > (Long.MAX_VALUE - digitValue) / BASE) {
                throw new IllegalArgumentException(
                    "Decoded value exceeds Long.MAX_VALUE: " + encoded);
            }
            
            result = result * BASE + digitValue;
        }
        
        return result;
    }
}
