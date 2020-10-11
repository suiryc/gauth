package suiryc.totp.core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;

/**
 * TOTP handler.
 *
 * See RFC 4226: https://tools.ietf.org/html/rfc4226
 * See RFC 6238: https://tools.ietf.org/html/rfc6238
 * See https://en.wikipedia.org/wiki/Google_Authenticator
 */
public class TOTP {

    // Dfault OTP hash algorithm: HMAC-SHA-1.
    private static final String HASH_ALGORITHM = "HMACSHA1";

    // Default generated OTP length: 6 digits.
    private static final int OTP_LENGTH = 6;

    // Default OTP time interval: 30s.
    public static int TIME_INTERVAL = 30;

    private final String label;
    private final byte[] secret;
    private final String hashAlgorithm;
    private final int otpLength;
    private final TimeInterval timeInterval;
    private String otp;
    private String nextOtp;

    public TOTP(String label, String secret, String hashAlgorithm, int otpLength, TimeInterval timeInterval) throws Exception {
        this.label = label;
        this.secret = Base32Decoder.decode(secret);
        if ("SHA1".equals(hashAlgorithm) || "SHA-1".equals(hashAlgorithm)) hashAlgorithm = "HMACSHA1";
        else if ("SHA256".equals(hashAlgorithm) || "SHA-256".equals(hashAlgorithm)) hashAlgorithm = "HMACSHA256";
        else if ("SHA512".equals(hashAlgorithm) || "SHA-512".equals(hashAlgorithm)) hashAlgorithm = "HMACSHA512";
        try {
            Mac.getInstance(hashAlgorithm);
        } catch (Exception ex) {
            System.err.println("Algorithm " + hashAlgorithm + " not available: fallback to HMACSHA1.");
            hashAlgorithm = HASH_ALGORITHM;
        }
        this.hashAlgorithm = hashAlgorithm;
        this.otpLength = otpLength;
        this.timeInterval = timeInterval;
        refresh();
    }

    public TOTP(String label, String secret, TimeInterval timeInterval) throws Exception {
        this(label, secret, HASH_ALGORITHM, OTP_LENGTH, timeInterval);
    }

    /** Gets TOTP label. */
    public String getLabel() {
        return label;
    }

    /** Gets TOTP time interval. */
    public TimeInterval getTimeInterval() {
        return timeInterval;
    }

    /** Gets TOTP code. */
    public String getOtp() {
        return otp;
    }

    /** Gets next TOTP code. */
    public String getNextOtp() {
        return nextOtp;
    }

    /** Refreshes TOTP: recomputes OTP. */
    public void refresh() throws Exception {
        this.otp = generate(timeInterval.getValue());
        this.nextOtp = generate(timeInterval.getValue() + 1);
    }

    /** Generates OTP for given counter. */
    public String generate(long counter) throws Exception {
        // MAC hasher initialized with secret key
        Mac hasher = Mac.getInstance(hashAlgorithm);
        hasher.init(new SecretKeySpec(secret, "RAW"));

        // Hash (big-endian) counter.
        byte[] hash = hasher.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(counter).array());

        // Get last nibble as an offset value.
        int offset = hash[hash.length - 1] & 0x0F;
        // Read integer (big-endian) at offset, and remove the most significant
        // bit (makes it positive).
        int code = ByteBuffer.wrap(hash, offset, Integer.BYTES).getInt() & 0x7FFFFFFF;
        // Keep requested length.
        code %= (int)Math.pow(10, otpLength);
        // Left-pad with 0 the computed value.
        return String.format("%0" + otpLength + "d", code);
    }

}
