package suiryc.gauth.core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

/**
 * Generates TOTP codes.
 *
 * See RFC 4226: https://tools.ietf.org/html/rfc4226
 * See https://en.wikipedia.org/wiki/Google_Authenticator
 */
public class TOTPGenerator {

    // OTP time interval: 30s.
    public static int TIME_INTERVAL = 30;

    // Generated OTP length: 6 digits.
    private static final int OTP_LENGTH = 6;
    private static final int OTP_MODULO = (int)Math.pow(10, OTP_LENGTH);

    public static String generate(String secret, long message) throws Exception {
        return generate(Base32Decoder.decode(secret), message);
    }

    public static String generate(byte[] secret, long message) throws Exception {
        // HMAC-SHA1 hasher initialized with secret key
        Mac hasher = Mac.getInstance("HMACSHA1");
        hasher.init(new SecretKeySpec(secret, "RAW"));

        // Hash (big-endian) message.
        byte[] hash = hasher.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(message).array());

        // Get last nibble as an offset value.
        int offset = hash[hash.length - 1] & 0x0F;
        // Read integer (big-endian) at offset, and remove the most significant
        // bit (makes it positive).
        int code = ByteBuffer.wrap(hash, offset, Integer.BYTES).getInt() & 0x7FFFFFFF;
        // Keep requested length.
        code %= OTP_MODULO;
        // Left-pad with 0 the computed value.
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

}
