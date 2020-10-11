package suiryc.totp.core;

public class TimeInterval {

    private final long interval;
    private long value;

    /**
     * Creates time interval handler.
     *
     * @param interval time interval in seconds
     */
    public TimeInterval(long interval) {
        interval *= 1000;
        this.interval = interval;
        refresh();
    }

    /** Gets interval duration (ms). */
    public long getInterval() {
        return interval;
    }

    /** Gets interval duration in seconds. */
    public long getIntervalSeconds() {
        return interval / 1000;
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

    /**
     * Refreshes interval to current time.
     *
     * @return whether current interval did change
     */
    public boolean refresh() {
        long value = System.currentTimeMillis() / interval;
        if (value == this.value) return false;
        this.value = value;
        return true;
    }

}
