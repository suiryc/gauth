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

    private static final TimeInterval timeInterval = new TimeInterval(TOTP.TIME_INTERVAL);
    private static final List<TOTP> totps = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            String[] split = arg.split("=", 2);
            if (split.length != 2) throw new Exception("Invalid TOTP format: expected 'label=value', got '" + arg + "'");
            String label = split[0].trim();
            split = split[1].split(",", 3);
            if (split.length == 1) {
                TOTP totp = new TOTP(label, split[0].trim(), timeInterval);
                totps.add(totp);
            } else if (split.length == 3) {
                TOTP totp = new TOTP(label, split[2].trim(), split[0].trim(), Integer.parseInt(split[1].trim()), timeInterval);
                totps.add(totp);
            } else {
                throw new Exception("Invalid TOTP format: expected 'label=hash-alg,otp-length,secret', got '" + arg + "'");
            }
        }

        // Start task that will display the codes in console and UI.
        new DisplayTask(true);
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

        public DisplayTask(boolean auto) {
            if (auto) run(true);
        }

        @Override
        public void run() {
            run(false);
        }

        private void run(boolean force) {
            // Make sure we do refresh the time interval first.
            boolean changed = timeInterval.refresh();
            // We at least need to display the progress indicator.
            updateControllerIndicator(false);
            if (!changed && !force) {
                // Next time interval not reached yet, nothing else to do.
                reschedule();
                return;
            }
            // Next time interval reached, time to update displayed codes.
            displayCodes(true);
            reschedule();
        }

        private static void displayCodes(boolean stdout) {
            int labelLength = 0;
            int otpLength = 0;

            if (stdout) {
                System.out.printf("Interval: %8s; Elapsed: %2ss; Remaining: %2ss%n",
                    timeInterval.getValue(), Math.round(timeInterval.getElapsed() / 1000D),
                    Math.round(timeInterval.getRemaining() / 1000D));
                labelLength = totps.stream().map(v -> v.getLabel().length()).max(Integer::compareTo).orElse(0);
                otpLength = totps.stream().map(v -> v.getOtp().length()).max(Integer::compareTo).orElse(0);
            }
            for (TOTP totp : totps) {
                try {
                    totp.refresh();
                    if (stdout) {
                        // Display new codes in console.
                        System.out.printf("  %" + labelLength + "s: OTP= %-" + otpLength + "s  OTP+1= %-" + otpLength + "s%n",
                            totp.getLabel(), totp.getOtp(), totp.getNextOtp());
                    }
                    // Display new codes in UI.
                    if (controller != null) controller.updateTOTP(totp);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (stdout) System.out.println("------------------------------------------------");
        }

        private void reschedule() {
            // Run again at the next second.
            long nextSecond = 1000 - (System.currentTimeMillis() % 1000);
            timer.schedule(new DisplayTask(false), nextSecond);
        }

        private static void updateControllerIndicator(boolean first) {
            // When first setting the controller (once initialized), we want to
            // immediately display the current codes (in the UI only).
            if (first) displayCodes(false);
            // Update progress indicator.
            if (controller != null) controller.updateIndicator(timeInterval);
        }

        public static void setController(MainController controller) {
            DisplayTask.controller = controller;
            updateControllerIndicator(true);
        }

        public static void stop() {
            timer.cancel();
        }

    }

}