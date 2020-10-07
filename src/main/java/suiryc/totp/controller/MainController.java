package suiryc.totp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
    private ProgressIndicator progressIndicator;

    @FXML
    private GridPane totpsPane;

    private Text percentageText;

    private final Map<TOTP, TOTPRow> totpsRows = new HashMap<>();

    protected void initialize(List<TOTP> totps) {
        // If the height of the progress indicator is larger than its width, the
        // (almost) square graphic is displayed at the top of the node. Due to
        // this, with FXML only it does not appear possible to center it inside
        // its parent while allowing it to be resized with the window (to fill
        // the available space).
        // Instead we need to let it be centered in the parent and use the same
        // width and height when its parent is resized.
        StackPane stackPane = (StackPane)progressIndicator.getParent();
        stackPane.widthProperty().addListener((observable) -> resizeIndicator(stackPane));
        stackPane.heightProperty().addListener((observable) -> resizeIndicator(stackPane));
        resizeIndicator(stackPane);

        // We wish to display the remaining time (seconds) instead of a
        // percentage in the progress indicator. Get the corresponding Text node
        // to do this.
        percentageText = (Text)progressIndicator.lookup(".percentage");
        percentageText.setText("");
        progressIndicator.progressProperty().addListener(
            (observable, oldValue, newValue) -> percentageText.setText((int)Math.ceil(TOTP.TIME_INTERVAL * newValue.doubleValue()) + "s")
        );

        // We don't want to display the 'check mark' when progress is at 100%.
        // This node parent is a Region, which means we cannot remove the mark
        // itself. The mark size and visibility is recomputed depending on the
        // progress value, so we cannot try to hide it this way.
        // We can only remove the 'tick' CSS class from the Node, which has the
        // same visible effect as hiding it.
        StackPane tick = (StackPane)progressIndicator.lookup(".tick");
        tick.getStyleClass().remove("tick");

        // Add totps rows.
        // Note: row 0 (table headers) already used.
        int totpIdx = 1;
        for (TOTP totp : totps) {
            RowConstraints rowConstraints = new RowConstraints(Region.USE_COMPUTED_SIZE) {{ setValignment(VPos.CENTER); }};
            totpsPane.getRowConstraints().add(rowConstraints);

            TOTPRow totpRow = new TOTPRow(totp);
            totpsRows.put(totp, totpRow);
            totpsPane.addRow(totpIdx++, totpRow.getLabel(), totpRow.getOtpLabel(), totpRow.getNextOtpLabel());
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

    private void resizeIndicator(StackPane stackPane) {
        // Resize indicator with same width and height, so that it can remain
        // centered in its parent.
        double size = Math.min(stackPane.getWidth(), stackPane.getHeight());
        progressIndicator.setPrefSize(size, size);
    }

    public void updateIndicator(TimeInterval timeInterval) {
        // Compute progress using 'ceil' (for whole seconds values).
        long interval = timeInterval.getInterval() / 1000;
        double remaining = Math.ceil(timeInterval.getRemaining() / 1000D);
        Platform.runLater(() -> progressIndicator.setProgress(remaining / interval));
    }

    public void updateTOTP(TOTP totp) {
        // Display OTP for given secret.
        TOTPRow totpRow = totpsRows.get(totp);
        if (totpRow == null) return;
        Platform.runLater(() -> {
            totpRow.getOtpLabel().setText(totp.getOtp());
            totpRow.getNextOtpLabel().setText(totp.getNextOtp());
        });
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

        private final Label label;
        private final Label otpLabel;
        private final Label nextOtpLabel;

        public TOTPRow(TOTP totp) {
            label = new Label(totp.getLabel()) {{ setFont(new Font(16)); }};
            // Customize (CSS) OTP text nodes, and copy code in clipboard when
            // clicked.
            otpLabel = new Label() {{ setFont(new Font(24)); }};
            otpLabel.getStyleClass().add("otp-code");
            handleClipboard(otpLabel);
            nextOtpLabel = new Label() {{ setFont(new Font(24)); }};
            nextOtpLabel.getStyleClass().add("otp-code");
            handleClipboard(nextOtpLabel);
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

    }

}
