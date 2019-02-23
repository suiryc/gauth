package suiryc.gauth;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Application;
import javafx.stage.Stage;
import suiryc.gauth.controller.MainController;
import suiryc.gauth.core.Secret;
import suiryc.gauth.core.TOTP;
import suiryc.gauth.core.TOTPGenerator;
import suiryc.gauth.core.TimeInterval;

public class Main extends Application {

    private static List<Secret> secrets = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            String[] split = arg.split("=", 2);
            if (split.length != 2) throw new Exception("Invalid secret format: expected 'label=value', got '" + arg + "'");
            secrets.add(new Secret(split[0], split[1]));
        }

        // Start task that will display the codes in console and UI.
        new DisplayTask(true);
        // Start UI.
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Build and display the main window.
        MainController.build(stage, secrets);
    }

    public static class DisplayTask extends TimerTask {

        private static MainController controller;
        private static Timer timer = new Timer();
        private static TimeInterval lastTimeInterval;

        public DisplayTask(boolean auto) {
            if (auto) run();
        }

        @Override
        public void run() {
            TimeInterval timeInterval = currentTimeInterval();
            // We at least need to display the progress indicator.
            updateControllerIndicator(timeInterval);
            if ((lastTimeInterval != null) && (lastTimeInterval.getValue() == timeInterval.getValue())) {
                // Next time interval not reached yet, nothing else to do.
                reschedule();
                return;
            }
            // Next time interval reached, time to update displayed codes.
            lastTimeInterval = timeInterval;
            System.out.println(
                String.format("Interval: %8s; Elapsed: %2ss; Remaining: %2ss",
                    timeInterval.getValue(), Math.round(timeInterval.getElapsed() / 1000D),
                    Math.round(timeInterval.getRemaining() / 1000D))
            );
            displayCodes(timeInterval, true);
            System.out.println("------------------------------------------------");
            reschedule();
        }

        private static void displayCodes(TimeInterval timeInterval, boolean stdout) {
            int labelLength = stdout ? secrets.stream().map(s -> s.getLabel().length()).max(Integer::compareTo).orElse(0) : 0;
            for (Secret secret : secrets) {
                try {
                    TOTP totp = new TOTP(secret, timeInterval);
                    if (stdout) {
                        // Display new codes in console.
                        System.out.println(
                            String.format("  %" + labelLength + "s: OTP=%s  OTP+1=%s",
                                totp.getSecret().getLabel(), totp.getOtp(), totp.getNextOtp())
                        );
                    }
                    // Display new codes in UI.
                    if (controller != null) controller.updateSecret(secret, totp);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        private void reschedule() {
            // Run again at the next second.
            long nextSecond = 1000 - (System.currentTimeMillis() % 1000);
            timer.schedule(new DisplayTask(false), nextSecond);
        }

        private static TimeInterval currentTimeInterval() {
            return new TimeInterval(TOTPGenerator.TIME_INTERVAL);
        }

        private static void updateControllerIndicator(TimeInterval timeInterval) {
            if (timeInterval == null) {
                // This happens when the controller is set (once initialized).
                // In this particular case we want to immediately display the
                // current codes (in the UI only).
                timeInterval = currentTimeInterval();
                displayCodes(timeInterval, false);
            }
            // Update progress indicator.
            if (controller != null) controller.updateIndicator(timeInterval);
        }

        public static void setController(MainController controller) {
            DisplayTask.controller = controller;
            updateControllerIndicator(null);
        }

        public static void stop() {
            timer.cancel();
        }

    }

}
