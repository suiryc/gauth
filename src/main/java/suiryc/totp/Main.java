package suiryc.totp;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Application;
import javafx.stage.Stage;
import suiryc.totp.controller.MainController;
import suiryc.totp.core.TOTP;
import suiryc.totp.core.TimeInterval;

public class Main {

    private static final List<TimeInterval> timeIntervals = new ArrayList<>();
    private static final List<TOTP> totps = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            String secret = null;
            String hashAlgorithm = TOTP.HASH_ALGORITHM;
            int otpLength = TOTP.OTP_LENGTH;
            int interval = TOTP.TIME_INTERVAL;

            String[] split = arg.split("=", 2);
            if (split.length != 2) throw new Exception("Invalid TOTP format: expected 'label=value', got '" + arg + "'");
            String label = split[0].trim();
            split = split[1].split(",");
            if (split.length == 1) {
                secret = split[0].trim();
            } else {
                for (String s: split) {
                    String[] param = s.split(":", 2);
                    if (param.length != 2) {
                        throw new Exception("Invalid TOTP format: expected 'label=key1:value1,... with a secret', got '" + arg + "'");
                    }
                    String paramValue = param[1].trim();
                    switch (param[0].trim().toLowerCase()) {
                        case "secret":
                            secret = paramValue;
                            break;
                        case "hash":
                            hashAlgorithm = paramValue;
                            break;
                        case "len":
                            otpLength = Integer.parseInt(paramValue);
                            break;
                        case "interval":
                            interval = Integer.parseInt(paramValue);
                            break;
                        default:
                            throw new Exception("Unhandled key=<" + label + "> parameter=<" + param[0] + ">");
                    }
                }
                if (secret == null) {
                    throw new Exception("Invalid TOTP format: expected 'label=key1:value1,... with a secret', got '" + arg + "'");
                }
            }
            int finalInterval = interval;
            TimeInterval timeInterval = timeIntervals.stream().filter(v -> v.getIntervalSeconds() == finalInterval).findFirst().orElseGet(() -> {
                TimeInterval ti = new TimeInterval(finalInterval);
                timeIntervals.add(ti);
                return ti;
            });
            TOTP totp = new TOTP(label, secret, hashAlgorithm, otpLength, timeInterval);
            totps.add(totp);
        }

        // Sort time intervals in reverse order: to display the longer ones
        // (refreshed less often) before the shorted ones.
        timeIntervals.sort((v1, v2) -> (int)(v2.getInterval() - v1.getInterval()));

        // Start tasks that will display the codes in console and refresh UI.
        timeIntervals.forEach(timeInterval -> new DisplayTask(timeInterval, true));
        // Start UI.
        new App().start(args);
    }

    public static class App extends Application {

        public void start(String... args) {
            launch(args);
        }

        @Override
        public void start(Stage stage) throws Exception {
            // Build and display the main window.
            MainController.build(stage, totps);
        }

    }

    public static class DisplayTask extends TimerTask {

        private static MainController controller;
        private static final Timer timer = new Timer();

        private final TimeInterval timeInterval;

        public DisplayTask(TimeInterval timeInterval, boolean auto) {
            this.timeInterval = timeInterval;
            if (auto) run(true);
        }

        @Override
        public void run() {
            run(false);
        }

        private void run(boolean force) {
            // Make sure we do refresh the time interval first.
            boolean changed = timeInterval.refresh() || force;

            int labelLength = 0;
            int otpLength = 0;

            if (changed) {
                System.out.printf("Interval: %8s; Elapsed: %2ss; Remaining: %2ss%n",
                        timeInterval.getValue(), Math.round(timeInterval.getElapsed() / 1000D),
                        Math.round(timeInterval.getRemaining() / 1000D));
                labelLength = totps.stream().map(v -> v.getLabel().length()).max(Integer::compareTo).orElse(0);
                otpLength = totps.stream().map(v -> v.getOtp().length()).max(Integer::compareTo).orElse(0);
            }
            for (TOTP totp : totps) {
                // Only deal with TOTPs using our time interval.
                if (totp.getTimeInterval() != timeInterval) continue;
                try {
                    if (changed) {
                        totp.refresh();
                        // Display new codes in console.
                        System.out.printf("  %" + labelLength + "s: OTP= %-" + otpLength + "s  OTP+1= %-" + otpLength + "s%n",
                                totp.getLabel(), totp.getOtp(), totp.getNextOtp());
                    }
                    // Update UI (progress, and possibly codes).
                    if (controller != null) controller.updateTOTP(totp);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (changed) System.out.println("------------------------------------------------");

            // Run again at the next second.
            long nextSecond = 1000 - (System.currentTimeMillis() % 1000);
            timer.schedule(new DisplayTask(timeInterval, false), nextSecond);
        }

        public static void setController(MainController controller) {
            DisplayTask.controller = controller;
        }

        public static void stop() {
            timer.cancel();
        }

    }

}
