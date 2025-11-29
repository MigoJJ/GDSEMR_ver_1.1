package com.emr.gds.main.thyroid;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Utility to size stages to a comfortable, screen-aware default.
 */
public final class StageSizing {

    private static final double DEFAULT_WIDTH_RATIO = 0.9;
    private static final double DEFAULT_HEIGHT_RATIO = 0.9;
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 700;

    private StageSizing() {
    }

    public static void fitToScreen(Stage stage) {
        fitToScreen(stage, DEFAULT_WIDTH_RATIO, DEFAULT_HEIGHT_RATIO);
    }

    public static void fitToScreen(Stage stage, double widthRatio, double heightRatio) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double effectiveMinWidth = Math.min(MIN_WIDTH, bounds.getWidth());
        double effectiveMinHeight = Math.min(MIN_HEIGHT, bounds.getHeight());

        double width = clamp(bounds.getWidth() * widthRatio, effectiveMinWidth, bounds.getWidth());
        double height = clamp(bounds.getHeight() * heightRatio, effectiveMinHeight, bounds.getHeight());

        stage.setMinWidth(effectiveMinWidth);
        stage.setMinHeight(effectiveMinHeight);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.centerOnScreen();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
