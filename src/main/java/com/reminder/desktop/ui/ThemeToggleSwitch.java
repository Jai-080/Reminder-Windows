package com.reminder.desktop.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Custom horizontal pill-shaped animated theme toggle matching Android ThemeToggleView:
 * - Width: 64px, Height: 32px (full pill shape, corner radius 16px)
 * - Light Mode:
 *     - Track: #F2F5EA with 1.2px graphite stroke #191D19
 *     - Sun ☀️ on the right (opacity 1.0, scale 1.0)
 *     - Moon 🌙 on the left (opacity 0.0, scale 0.6)
 *     - Thumb: pure white #FFFFFF on LEFT (x = 16) with 1.2px stroke #191D19 and soft drop shadow
 * - Dark Mode:
 *     - Track: #1C1E1C with subtle stroke #373B36
 *     - Moon 🌙 on the left (opacity 1.0, scale 1.0)
 *     - Sun ☀️ on the right (opacity 0.0, scale 0.6)
 *     - Thumb: pure white #FFFFFF on RIGHT (x = 48) borderless with soft drop shadow
 * - Smooth transitions for thumb translation, icon cross-fade & scale, and colors.
 */
public class ThemeToggleSwitch extends Pane implements ThemeManager.ThemeListener {

    private static final double WIDTH = 64;
    private static final double HEIGHT = 32;
    private static final double THUMB_RADIUS = 12;
    private static final double TRAVEL_DISTANCE = 32; // from x=16 to x=48

    // Track colors
    private static final Color LIGHT_TRACK_BG = Color.web("#F2F5EA");
    private static final Color DARK_TRACK_BG = Color.web("#1C1E1C");
    private static final Color LIGHT_TRACK_BORDER = Color.web("#191D19");
    private static final Color DARK_TRACK_BORDER = Color.web("#373B36");

    // Thumb styling
    private static final Color THUMB_FILL = Color.WHITE;
    private static final Color LIGHT_THUMB_BORDER = Color.web("#191D19");

    private final Rectangle track;
    private final Label sunLabel;
    private final Label moonLabel;
    private final Circle thumb;
    private final Tooltip tooltip;

    private boolean isDark;
    private ParallelTransition currentAnimation;
    private long lastToggleTime = 0;

    public ThemeToggleSwitch() {
        this.isDark = ThemeManager.isDarkMode();

        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setCursor(Cursor.HAND);
        setFocusTraversable(true);

        // 1. Pill Track
        track = new Rectangle(WIDTH, HEIGHT);
        track.setArcWidth(HEIGHT);
        track.setArcHeight(HEIGHT);
        track.setStrokeWidth(1.2);

        // 2. Sun Emoji (on the right side)
        sunLabel = new Label("☀️");
        sunLabel.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji', sans-serif; -fx-font-size: 13px;");
        sunLabel.setPrefSize(20, 20);
        sunLabel.setAlignment(Pos.CENTER);
        sunLabel.setLayoutX(WIDTH - 24); // centered around x=48
        sunLabel.setLayoutY((HEIGHT - 20) / 2.0);
        sunLabel.setMouseTransparent(true);

        // 3. Moon Emoji (on the left side)
        moonLabel = new Label("🌙");
        moonLabel.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Apple Color Emoji', 'Noto Color Emoji', sans-serif; -fx-font-size: 13px;");
        moonLabel.setPrefSize(20, 20);
        moonLabel.setAlignment(Pos.CENTER);
        moonLabel.setLayoutX(4); // centered around x=16
        moonLabel.setLayoutY((HEIGHT - 20) / 2.0);
        moonLabel.setMouseTransparent(true);

        // 4. Circular Sliding Thumb
        thumb = new Circle(THUMB_RADIUS);
        thumb.setCenterX(HEIGHT / 2.0); // 16
        thumb.setCenterY(HEIGHT / 2.0); // 16
        thumb.setFill(THUMB_FILL);
        thumb.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(0, 0, 0, 0.28), 4, 0, 0, 1.2));
        thumb.setMouseTransparent(true);

        getChildren().addAll(track, sunLabel, moonLabel, thumb);

        tooltip = new Tooltip();
        Tooltip.install(this, tooltip);

        // Initialize state without animation
        applyVisualState(this.isDark, false);

        // Click handler
        setOnMouseClicked(e -> handleToggleRequest());

        // Keyboard accessibility
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                handleToggleRequest();
                e.consume();
            }
        });

        // Register with ThemeManager when attached to scene
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                ThemeManager.addListener(this);
                syncWithThemeManager(false);
            } else {
                ThemeManager.removeListener(this);
            }
        });
    }

    private void handleToggleRequest() {
        long now = System.currentTimeMillis();
        if (now - lastToggleTime < 350) {
            return; // Debounce rapid clicks
        }
        lastToggleTime = now;

        boolean targetDark = !ThemeManager.isDarkMode();
        ThemeManager.setDarkMode(targetDark);
    }

    @Override
    public void onThemeChanged(boolean isDarkActive) {
        Platform.runLater(() -> {
            if (this.isDark != isDarkActive) {
                this.isDark = isDarkActive;
                applyVisualState(isDarkActive, true);
            }
        });
    }

    public void syncWithThemeManager(boolean animate) {
        boolean darkActive = ThemeManager.isDarkMode();
        if (this.isDark != darkActive || !animate) {
            this.isDark = darkActive;
            applyVisualState(darkActive, animate);
        }
    }

    private void applyVisualState(boolean dark, boolean animate) {
        tooltip.setText(dark ? "Switch to Light Mode" : "Switch to Dark Mode");

        double targetTranslateX = dark ? TRAVEL_DISTANCE : 0;
        double targetSunOpacity = dark ? 0.0 : 1.0;
        double targetSunScale = dark ? 0.6 : 1.0;
        double targetMoonOpacity = dark ? 1.0 : 0.0;
        double targetMoonScale = dark ? 1.0 : 0.6;

        Color targetTrackFill = dark ? DARK_TRACK_BG : LIGHT_TRACK_BG;
        Color targetTrackStroke = dark ? DARK_TRACK_BORDER : LIGHT_TRACK_BORDER;
        Color targetThumbStroke = dark ? Color.TRANSPARENT : LIGHT_THUMB_BORDER;
        double targetThumbStrokeWidth = dark ? 0 : 1.2;

        if (!animate) {
            if (currentAnimation != null) {
                currentAnimation.stop();
            }
            thumb.setTranslateX(targetTranslateX);
            sunLabel.setOpacity(targetSunOpacity);
            sunLabel.setScaleX(targetSunScale);
            sunLabel.setScaleY(targetSunScale);
            moonLabel.setOpacity(targetMoonOpacity);
            moonLabel.setScaleX(targetMoonScale);
            moonLabel.setScaleY(targetMoonScale);

            track.setFill(targetTrackFill);
            track.setStroke(targetTrackStroke);
            thumb.setStroke(targetThumbStroke);
            thumb.setStrokeWidth(targetThumbStrokeWidth);
            return;
        }

        if (currentAnimation != null) {
            currentAnimation.stop();
        }

        Duration duration = Duration.millis(280);
        Interpolator ease = Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0);

        // Thumb translation
        TranslateTransition thumbTrans = new TranslateTransition(duration, thumb);
        thumbTrans.setToX(targetTranslateX);
        thumbTrans.setInterpolator(ease);

        // Sun transitions
        FadeTransition sunFade = new FadeTransition(duration, sunLabel);
        sunFade.setToValue(targetSunOpacity);
        sunFade.setInterpolator(ease);

        ScaleTransition sunScale = new ScaleTransition(duration, sunLabel);
        sunScale.setToX(targetSunScale);
        sunScale.setToY(targetSunScale);
        sunScale.setInterpolator(ease);

        // Moon transitions
        FadeTransition moonFade = new FadeTransition(duration, moonLabel);
        moonFade.setToValue(targetMoonOpacity);
        moonFade.setInterpolator(ease);

        ScaleTransition moonScale = new ScaleTransition(duration, moonLabel);
        moonScale.setToX(targetMoonScale);
        moonScale.setToY(targetMoonScale);
        moonScale.setInterpolator(ease);

        currentAnimation = new ParallelTransition(thumbTrans, sunFade, sunScale, moonFade, moonScale);
        currentAnimation.setOnFinished(e -> {
            currentAnimation = null;
        });

        // Instant or fast color swap during motion
        track.setFill(targetTrackFill);
        track.setStroke(targetTrackStroke);
        thumb.setStroke(targetThumbStroke);
        thumb.setStrokeWidth(targetThumbStrokeWidth);

        currentAnimation.play();
    }
}
