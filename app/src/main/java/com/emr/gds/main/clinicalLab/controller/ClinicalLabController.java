package com.emr.gds.main.clinicalLab.controller;

import com.emr.gds.main.clinicalLab.db.ClinicalLabDatabase;
import com.emr.gds.main.clinicalLab.model.ClinicalLabItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ClinicalLabController implements Initializable {

    // Left Panel
    @FXML private ListView<String> selectedItemsList;

    // Center Panel
    @FXML private TextField searchField;
    @FXML private TableView<ClinicalLabItem> labTable;
    @FXML private TableColumn<ClinicalLabItem, String> colCategory;
    @FXML private TableColumn<ClinicalLabItem, String> colTestName;
    @FXML private TableColumn<ClinicalLabItem, String> colUnit;
    @FXML private TableColumn<ClinicalLabItem, String> colMaleRef;
    @FXML private TableColumn<ClinicalLabItem, String> colFemaleRef;

    // Right Panel (Details)
    @FXML private Label lblTestName;
    @FXML private Label lblCategory;
    @FXML private Label lblUnit;
    @FXML private Label lblMaleRange;
    @FXML private Label lblMaleRef;
    @FXML private Label lblFemaleRange;
    @FXML private Label lblFemaleRef;

    private final ClinicalLabDatabase database = new ClinicalLabDatabase();
    private final ObservableList<ClinicalLabItem> masterData = FXCollections.observableArrayList();
    private final ObservableList<String> selectedItems = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        setupSelectionModel();
        selectedItemsList.setItems(selectedItems);
        loadData();
    }

    private void setupTable() {
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colTestName.setCellValueFactory(new PropertyValueFactory<>("testName"));
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        colMaleRef.setCellValueFactory(new PropertyValueFactory<>("maleReferenceRange"));
        colFemaleRef.setCellValueFactory(new PropertyValueFactory<>("femaleReferenceRange"));
    }

    private void setupSelectionModel() {
        labTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showDetails(newVal);
            } else {
                clearDetails();
            }
        });
    }

    @FXML
    private void loadData() {
        masterData.clear();
        masterData.addAll(database.getAllItems());
        labTable.setItems(masterData);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        if (query == null || query.trim().isEmpty()) {
            labTable.setItems(masterData);
        } else {
            // Use database search directly
            java.util.List<ClinicalLabItem> results = database.searchItems(query);
            labTable.setItems(FXCollections.observableArrayList(results));
        }
    }

    private void showDetails(ClinicalLabItem item) {
        lblTestName.setText(item.getTestName());
        lblCategory.setText(item.getCategory());
        lblUnit.setText(item.getUnit());

        String mLow = item.getMaleRangeLow() != null ? String.valueOf(item.getMaleRangeLow()) : "-";
        String mHigh = item.getMaleRangeHigh() != null ? String.valueOf(item.getMaleRangeHigh()) : "-";
        lblMaleRange.setText(mLow + " - " + mHigh);
        lblMaleRef.setText(item.getMaleReferenceRange() != null ? item.getMaleReferenceRange() : "");

        String fLow = item.getFemaleRangeLow() != null ? String.valueOf(item.getFemaleRangeLow()) : "-";
        String fHigh = item.getFemaleRangeHigh() != null ? String.valueOf(item.getFemaleRangeHigh()) : "-";
        lblFemaleRange.setText(fLow + " - " + fHigh);
        lblFemaleRef.setText(item.getFemaleReferenceRange() != null ? item.getFemaleReferenceRange() : "");
    }

    private void clearDetails() {
        lblTestName.setText("-");
        lblCategory.setText("-");
        lblUnit.setText("-");
        lblMaleRange.setText("-");
        lblMaleRef.setText("-");
        lblFemaleRange.setText("-");
        lblFemaleRef.setText("-");
    }

    @FXML
    private void addToSelection() {
        ClinicalLabItem selected = labTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String itemStr = selected.getTestName() + " (" + selected.getCategory() + ")";
            if (!selectedItems.contains(itemStr)) {
                selectedItems.add(itemStr);
            }
        }
    }

    @FXML
    private void copySelected() {
        if (selectedItems.isEmpty()) return;
        String content = String.join("\n", selectedItems);
        ClipboardContent clipboardContent = new ClipboardContent();
        clipboardContent.putString(content);
        Clipboard.getSystemClipboard().setContent(clipboardContent);
    }

    @FXML
    private void clearSelection() {
        selectedItems.clear();
    }
}
