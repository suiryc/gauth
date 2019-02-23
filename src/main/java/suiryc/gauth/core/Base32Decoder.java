package suiryc.gauth.core;

/**
 * Base32 decoder.
 *
 * See RFC 4648: https://tools.ietf.org/html/rfc4648
 */
public class Base32Decoder {

    // The base32 alphabet.
    // 0 and 1 being not present, there is no confusion with O, l or I.
    private static String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    // The base (number of values).
    private static int BASE = 32;
    // How many bits per base32 character.
    private static int BASE_BITS = Integer.numberOfTrailingZeros(BASE);
    // How many bits per byte.
    private static int BYTE_BITS = 8;

    public static byte[] decode(String encoded) throws Exception {
        // Remove any whitespace, and make uppercase (for alphabet).
        // Also trim trailing padding.
        encoded = encoded
                .trim().replaceAll(" ", "")
                .replaceAll("=+$", "")
                .toUpperCase();

        char[] chars = encoded.toCharArray();
        // Allocate the decoded buffer.
        // Note: if there are trailing bits (last character least significant
        // bits are part of a new byte which is not complete), they are simply
        // ignored. Actually in this case there should be an adequate padding
        // to distinguish this from a truncated base32 value.
        // Thus we can use the implicit 'floor' from dividing an integer value
        // to determine how many (complete) bytes will be decoded.
        int decodedLength = (chars.length * BASE_BITS) / BYTE_BITS;
        byte[] decoded = new byte[decodedLength];

        // Offset in decoded buffer.
        int decodedIdx = 0;
        // Characters are decoded in a temporary integer, and complete bytes
        // are written to the decoded buffer once available.
        int decodedValue = 0;
        int decodedValueBits = 0;
        for (char c : chars) {
            int value = ALPHABET.indexOf(c);
            if (value < 0) throw new Exception("Invalid base32 character: " + c);
            // Push this decoded value in the integer.
            decodedValue <<= BASE_BITS;
            decodedValue |= value;
            decodedValueBits += BASE_BITS;
            // Once a complete byte is available, move it in the decoded buffer.
            if (decodedValueBits >= BYTE_BITS) {
                decoded[decodedIdx++] = (byte)(decodedValue >> (decodedValueBits - BYTE_BITS));
                decodedValueBits -= BYTE_BITS;
            }
        }
        // As explained above, if there are trailing bits (bufferBits > 0),
        // there should be an adequate trailing padding '='.
        // For our usage, there is no real need to enforce it.

        return decoded;
    }

}
