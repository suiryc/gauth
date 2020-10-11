package suiryc.totp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import suiryc.totp.Main;
import suiryc.totp.core.TOTP;
import suiryc.totp.core.TimeInterval;

// Notes:
// We don't need 'Initializible.initialize', especially because we wish to
// access JavaFX (Skins) resources that are not yet created at this point.
// Also beware of not declaring a public parameter-less 'initialize'
// function because JavaFX would run it upon creating the controller.
public class MainController {

    @FXML
    private GridPane totpsPane;

    private final Map<TOTP, TOTPRow> totpsRows = new HashMap<>();

    protected void initialize(List<TOTP> totps) {
        // Add totps rows.
        // Note: row 0 (table headers) already used.
        int totpIdx = 1;
        for (TOTP totp : totps) {
            RowConstraints rowConstraints = new RowConstraints(Region.USE_COMPUTED_SIZE) {{ setValignment(VPos.CENTER); }};
            totpsPane.getRowConstraints().add(rowConstraints);

            TOTPRow totpRow = new TOTPRow(totp);
            totpsRows.put(totp, totpRow);
            totpsPane.addRow(totpIdx++,
                totpRow.getLabel(),
                totpRow.getOtpLabel(),
                totpRow.getProgressIndicator(),
                totpRow.getNextOtpLabel()
            );
        }

        // Now that rows have been added, wait a bit (defer execution in JavaFX)
        // to resize the stage taking into account the preferred (computed)
        // scene size.
        Platform.runLater(() -> {
            Scene scene = totpsPane.getScene();
            Parent root = scene.getRoot();
            Stage stage = (Stage)scene.getWindow();
            // Keep the decoration dimensions (between scene and stage).
            stage.setWidth(root.prefWidth(-1) + stage.getWidth() - scene.getWidth());
            stage.setHeight(root.prefHeight(-1) + stage.getHeight() - scene.getHeight());
        });
    }

    public void updateTOTP(TOTP totp) {
        // Display OTP for given secret.
        TOTPRow totpRow = totpsRows.get(totp);
        if (totpRow != null) totpRow.update();
    }

    public static void build(Stage stage, List<TOTP> totps) throws Exception {
        // 32px icon
        stage.getIcons().setAll(new Image("/gauth-32.png", 0.0, 0.0, true, false, false));

        // Load FXML
        FXMLLoader loader = new FXMLLoader(MainController.class.getResource("/main.fxml"));
        Parent root = loader.load();

        // Get controller to initialize it in a second step (we need to wait
        // some resources - Skins - to be built).
        MainController controller = loader.getController();
        Platform.runLater(() -> {
            controller.initialize(totps);
            Main.DisplayTask.setController(controller);
        });

        // Set and customize (CSS) scene.
        stage.setScene(new Scene(root));
        stage.getScene().getStylesheets().add(MainController.class.getResource("/main.css").toExternalForm());

        stage.setTitle("Time-based One-Time Passwords");
        // Upon closing, stop background task then exit JavaFX.
        stage.setOnCloseRequest(evt -> {
            Main.DisplayTask.stop();
            Platform.exit();
        });
        stage.show();
    }

    private static class TOTPRow {

        private final TOTP totp;
        private final Label label;
        private final Label otpLabel;
        private final Label nextOtpLabel;
        private final ProgressIndicatorBar progressIndicator;

        public TOTPRow(TOTP totp) {
            this.totp = totp;

            label = new Label(totp.getLabel()) {{ setFont(new Font(16)); }};

            // Customize (CSS) OTP text nodes, and copy code in clipboard when
            // clicked.
            // Setting the current OTP text helps defining the minimum Label
            // size.
            otpLabel = new Label(totp.getOtp()) {{ setFont(new Font(20)); }};
            otpLabel.getStyleClass().add("otp-code");
            handleClipboard(otpLabel);
            nextOtpLabel = new Label(totp.getNextOtp()) {{ setFont(new Font(20)); }};
            nextOtpLabel.getStyleClass().add("otp-code");
            handleClipboard(nextOtpLabel);

            progressIndicator = new ProgressIndicatorBar() {
                @Override
                public String formatProgressText(double progress) {
                    if (progress >= 0) {
                        // Notes:
                        // We wish to display the remaining number of seconds.
                        // The progress is the elapsed number of seconds, with
                        // a 1s offset.
                        return (int)Math.round(TOTP.TIME_INTERVAL * (1 - progress) + 1) + "s";
                    } else {
                        return super.formatProgressText(progress);
                    }
                }
            };

            update();
        }

        private void handleClipboard(Label label) {
            label.setOnMouseClicked(evt -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(label.getText());
                Clipboard.getSystemClipboard().setContent(content);
            });
        }

        public Label getLabel() {
            return label;
        }

        public Label getOtpLabel() {
            return otpLabel;
        }

        public Label getNextOtpLabel() {
            return nextOtpLabel;
        }

        public ProgressIndicatorBar getProgressIndicator() {
            return progressIndicator;
        }

        public void update() {
            // Notes:
            // To get proper second-precision rounding, we should either do
            // floor(elapsed/1000) or ceil(remaining/1000).
            // This would work with ProgressIndicator, but we don't use it
            // because it doesn't fit in the UI if we want one per TOTP.
            // (there is no easy way - or at all - to hide or change the
            // progress text and tweak it as we wish).
            // And when using a ProgressBar, somehow it appears a bit biased
            // when dealing with the first (left side) percents of progress:
            // they aren't properly shown, even though there is no such issue
            // with the last (right side) percents of progress; as if the
            // filling bar was actually starting in an invisible part of the
            // left side of the control.
            // As a workaround, offset progress by one second (which as side
            // effect makes us reach 100% on the last second).
            Platform.runLater(() -> {
                // Set OTPs and autosize the Labels (not strictly necessary when
                // text size does not change).
                otpLabel.setText(totp.getOtp());
                otpLabel.autosize();
                nextOtpLabel.setText(totp.getNextOtp());
                nextOtpLabel.autosize();
                TimeInterval timeInterval = totp.getTimeInterval();
                double elapsed = Math.floor(timeInterval.getElapsed() / 1000D) + 1;
                double interval = timeInterval.getInterval() / 1000D;
                progressIndicator.setProgress(elapsed / interval);
            });
        }

    }

    private static class ProgressIndicatorBar extends StackPane {

        private static final int LABEL_PADDING = 5;

        private final ProgressBar bar = new ProgressBar();
        private final Text text = new Text();

        ProgressIndicatorBar() {
            // Fill horizontal space.
            bar.setMaxWidth(Double.MAX_VALUE);

            // Adapt minimum bar size to text size.
            text.textProperty().addListener((observable, v0, v) -> {
                bar.setMinHeight(text.getBoundsInLocal().getHeight() + LABEL_PADDING * 2);
                bar.setMinWidth(text.getBoundsInLocal().getWidth() + LABEL_PADDING * 2);
            });

            progressProperty().addListener((observable, v0, v) -> text.setText(formatProgressText(v.doubleValue())));

            getChildren().setAll(bar, text);
        }

        public final void setProgress(double value) {
            bar.setProgress(value);
        }

        public final DoubleProperty progressProperty() {
            return bar.progressProperty();
        }

        public String formatProgressText(double progress) {
            if (progress < 0) return "";
            if (progress >= 1) return "100%";
            return Math.round(progress * 100.0) + "%";
        }

    }

}
