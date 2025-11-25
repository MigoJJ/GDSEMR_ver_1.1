package com.emr.gds.main.glp1;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Demo launcher for Glp1SemaglutidePane.
 * You can run this as a standalone JavaFX application.
 */
public class Glp1SemaglutideMain extends Application {

    @Override
    public void start(Stage stage) {
        Glp1SemaglutidePane medPane = new Glp1SemaglutidePane();

        // Output area for problem-list text
        TextArea txtOutput = new TextArea();
        txtOutput.setPromptText("Problem-list output will appear here...");
        txtOutput.setPrefRowCount(8);
        txtOutput.setWrapText(true);

        // Buttons
        Button btnExport = new Button("Export to EMR Text");
        Button btnClear  = new Button("Clear All");

        btnExport.setOnAction(e -> txtOutput.setText(medPane.toProblemListString()));
        btnClear.setOnAction(e -> {
            medPane.clearAll();
            txtOutput.clear();
        });

        HBox buttonBox = new HBox(10, btnExport, btnClear);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(6));

        BorderPane bottomPane = new BorderPane();
        bottomPane.setTop(new Separator());
        bottomPane.setCenter(txtOutput);
        bottomPane.setBottom(buttonBox);

        BorderPane root = new BorderPane();
        root.setCenter(medPane);
        root.setBottom(bottomPane);

        Scene scene = new Scene(root, 900, 700);
        stage.setTitle("GLP-1RA (Semaglutide) - EMR Module Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
