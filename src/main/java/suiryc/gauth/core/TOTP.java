package suiryc.gauth.core;

public class TOTP {

    private Secret secret;
    private TimeInterval timeInterval;
    private String otp;
    private String nextOtp;

    public TOTP(Secret secret, TimeInterval timeInterval) throws Exception {
        this.secret = secret;
        this.timeInterval = timeInterval;
        this.otp = TOTPGenerator.generate(secret.getValue(), timeInterval.getValue());
        this.nextOtp = TOTPGenerator.generate(secret.getValue(), timeInterval.getValue() + 1);
    }

    /** Gets TOTP secret. */
    public Secret getSecret() {
        return secret;
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

}
