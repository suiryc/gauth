package suiryc.gauth.core;

public class TimeInterval {

    private long interval;
    private long value;

    /**
     * Creates current time interval information.
     *
     * @param interval time interval in seconds
     */
    public TimeInterval(long interval) {
        interval *= 1000;
        this.interval = interval;
        long now = System.currentTimeMillis();
        value = now / interval;
    }

    /** Gets interval duration (ms). */
    public long getInterval() {
        return interval;
    }

    /** Gets this time interval (index) value. */
    public long getValue() {
        return value;
    }

    /** Gets how many ms were elapsed in the current interval. */
    public long getElapsed() {
        long now = System.currentTimeMillis();
        return now - value * interval;
    }

    /** Gets how many ms remain before reaching the next interval. */
    public long getRemaining() {
        return Math.max(0, interval - getElapsed());
    }

}
