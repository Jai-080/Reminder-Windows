package com.reminder.desktop.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;

public class TimePicker extends HBox {
    private final Spinner<Integer> hourSpinner;
    private final Spinner<Integer> minSpinner;

    public TimePicker(int initialHour, int initialMin) {
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(6);
        this.getStyleClass().add("time-picker-box");
        this.setStyle("-fx-background-color: transparent; -fx-alignment: center-left;");

        // Hour Spinner (0-23)
        SpinnerValueFactory<Integer> hourFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, initialHour);
        hourFactory.setWrapAround(true);
        hourSpinner = new Spinner<>(hourFactory);
        hourSpinner.setPrefWidth(64);
        hourSpinner.setEditable(true);

        Label colon = new Label(":");
        colon.setStyle("-fx-text-fill: -color-text-secondary; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Minute Spinner (0-59)
        SpinnerValueFactory<Integer> minFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, initialMin);
        minFactory.setWrapAround(true);
        minSpinner = new Spinner<Integer>(minFactory) {
            @Override
            public void increment(int steps) {
                int oldVal = getValue();
                super.increment(steps);
                int newVal = getValue();
                if (oldVal == 59 && newVal == 0) {
                    hourSpinner.increment(1);
                }
            }

            @Override
            public void decrement(int steps) {
                int oldVal = getValue();
                super.decrement(steps);
                int newVal = getValue();
                if (oldVal == 0 && newVal == 59) {
                    hourSpinner.decrement(1);
                }
            }
        };
        minSpinner.setPrefWidth(64);
        minSpinner.setEditable(true);

        // Standardize focus listeners to force committing typing on focus lost
        commitOnFocusLost(hourSpinner);
        commitOnFocusLost(minSpinner);

        // Select all text when clicked or focused
        setupSelectAllOnFocusAndClick(hourSpinner);
        setupSelectAllOnFocusAndClick(minSpinner);

        // Restrict text input to max 2 digits
        setupValidation(hourSpinner.getEditor(), 23);
        setupValidation(minSpinner.getEditor(), 59);

        // Auto advance hour to minute after two digits entered
        hourSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (hourSpinner.getEditor().isFocused() && newText.length() == 2) {
                commitSpinnerValue(hourSpinner);
                minSpinner.getEditor().requestFocus();
            }
        });

        // Auto commit minute after two digits entered
        minSpinner.getEditor().textProperty().addListener((obs, oldText, newText) -> {
            if (minSpinner.getEditor().isFocused() && newText.length() == 2) {
                commitSpinnerValue(minSpinner);
            }
        });

        this.getChildren().addAll(hourSpinner, colon, minSpinner);
    }

    private void commitOnFocusLost(Spinner<Integer> spinner) {
        spinner.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                commitSpinnerValue(spinner);
            }
        });
        // Also support pressing Enter key
        spinner.getEditor().setOnAction(e -> commitSpinnerValue(spinner));
    }

    private void commitSpinnerValue(Spinner<Integer> spinner) {
        if (!spinner.isEditable()) return;
        String text = spinner.getEditor().getText().trim();
        try {
            int value = Integer.parseInt(text);
            SpinnerValueFactory<Integer> factory = spinner.getValueFactory();
            if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory) {
                SpinnerValueFactory.IntegerSpinnerValueFactory intFactory = 
                    (SpinnerValueFactory.IntegerSpinnerValueFactory) factory;
                // Clamp within range
                if (value < intFactory.getMin()) value = intFactory.getMin();
                if (value > intFactory.getMax()) value = intFactory.getMax();
            }
            spinner.getValueFactory().setValue(value);
        } catch (NumberFormatException e) {
            // Revert to current value on parse failure
            spinner.getEditor().setText(String.format("%02d", spinner.getValue()));
        }
    }

    public int getHour() {
        return hourSpinner.getValue();
    }

    public int getMinute() {
        return minSpinner.getValue();
    }

    public void setTime(int hour, int minute) {
        hourSpinner.getValueFactory().setValue(hour);
        minSpinner.getValueFactory().setValue(minute);
    }

    private void setupValidation(javafx.scene.control.TextField field, int max) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                if (newText.isEmpty()) {
                    return change;
                }
                try {
                    int val = Integer.parseInt(newText);
                    if (val <= max && newText.length() <= 2) {
                        return change;
                    }
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }));
    }

    private void setupSelectAllOnFocusAndClick(Spinner<Integer> spinner) {
        spinner.getEditor().focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(spinner.getEditor()::selectAll);
            }
        });
        spinner.getEditor().setOnMouseClicked(e -> {
            Platform.runLater(spinner.getEditor()::selectAll);
        });
    }
}
