package com.emr.gds;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * An editor for creating and managing EMR templates stored in a SQLite database.
 * The templates can be parsed and formatted according to a standard EMR structure.
 */
public class IAFMainEdit extends JFrame {
    // === Canonical section titles (labels) ===
    public static final String[] TEXT_AREA_TITLES = {
            "CC>", "PI>", "ROS>", "PMH>", "S>",
            "O>", "Physical Exam>", "A>", "P>", "Comment>"
    };

    private static final Pattern HEADER_PATTERN = Pattern.compile(
            "^\\s*(CC>|PI>|ROS>|PMH>|S>|O>|Physical Exam>|A>|P>|Comment>)\\s*(.*)$"
    );

    // === GUI Components ===
    private JTable templateTable;
    private DefaultTableModel tableModel;
    private JTextField templateNameField;
    private JTextArea templateContentArea;
    private final JButton newButton = new JButton("New");
    private final JButton saveButton = new JButton("Save");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton useTemplateButton = new JButton("Use Template");

    // === Data / Logic ===
    private final DatabaseManager dbManager;
    private int selectedTemplateId = -1;
    private final Consumer<String> onTemplateSelectedCallback;

    // --- DB Path Helpers ---
    private static Path repoRoot() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("gradlew")) && !Files.exists(p.resolve(".git"))) {
            p = p.getParent();
        }
        return (p != null) ? p : Paths.get("").toAbsolutePath();
    }
    
    private static Path dbPath() {
        return repoRoot().resolve("app").resolve("db").resolve("emr_templates.db");
    }

    public IAFMainEdit(Consumer<String> onTemplateSelectedCallback) {
        this.onTemplateSelectedCallback = onTemplateSelectedCallback;
        this.dbManager = new DatabaseManager();
        initUI();
    }
    
    // Main Method for local testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
            new IAFMainEdit(content -> {
                JTextArea preview = new JTextArea(content, 25, 80);
                JOptionPane.showMessageDialog(null, new JScrollPane(preview), "Selected Template Output", JOptionPane.INFORMATION_MESSAGE);
            }).setVisible(true)
        );
    }
    
    // === UI Initialization ===
    private void initUI() {
        setTitle("EMR Template Editor");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 245, 245));
        
        initComponents();
        layoutComponents();
        attachListeners();
        loadTemplatesIntoTable();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                dbManager.closeConnection();
            }
        });
    }

    private void initComponents() {
        String[] columnNames = {"ID", "Template Name"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };
        templateTable = new JTable(tableModel);
        templateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        templateTable.setRowHeight(28);
        templateTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        templateTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        templateTable.getTableHeader().setBackground(new Color(70, 130, 180));
        templateTable.getTableHeader().setForeground(Color.WHITE);
        templateTable.setSelectionBackground(new Color(184, 207, 229));
        templateTable.setGridColor(new Color(200, 200, 200));
        templateTable.removeColumn(templateTable.getColumnModel().getColumn(0));

        templateNameField = new JTextField(35);
        templateNameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        templateNameField.setPreferredSize(new Dimension(350, 30));
        
        templateContentArea = new JTextArea();
        templateContentArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        templateContentArea.setLineWrap(true);
        templateContentArea.setWrapStyleWord(true);
        templateContentArea.setBackground(Color.WHITE);
        templateContentArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private void layoutComponents() {
        JScrollPane tableScrollPane = new JScrollPane(templateTable);
        tableScrollPane.setPreferredSize(new Dimension(350, 0));
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapInTitledPanel(tableScrollPane, "Templates"),
                wrapInTitledPanel(createEditorPanel(), "Editor"));
        splitPane.setDividerLocation(380);
        splitPane.setResizeWeight(0.3);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        add(splitPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        namePanel.setBackground(new Color(245, 245, 245));
        JLabel nameLabel = new JLabel("Template Name:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        namePanel.add(nameLabel);
        namePanel.add(templateNameField);
        
        JScrollPane contentScrollPane = new JScrollPane(templateContentArea);
        contentScrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150)), 
            "Template Content", 
            0, 0, 
            new Font("SansSerif", Font.BOLD, 12), 
            new Color(70, 70, 70)));
        
        panel.add(namePanel, BorderLayout.NORTH);
        panel.add(contentScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        // Style buttons
        Font buttonFont = new Font("SansSerif", Font.BOLD, 12);
        Dimension buttonSize = new Dimension(100, 35);
        
        newButton.setFont(buttonFont);
        newButton.setPreferredSize(buttonSize);
        newButton.setBackground(new Color(70, 130, 180));
        newButton.setForeground(Color.WHITE);
        
        saveButton.setFont(buttonFont);
        saveButton.setPreferredSize(buttonSize);
        saveButton.setBackground(new Color(60, 179, 113));
        saveButton.setForeground(Color.WHITE);
        
        deleteButton.setFont(buttonFont);
        deleteButton.setPreferredSize(buttonSize);
        deleteButton.setBackground(new Color(220, 20, 60));
        deleteButton.setForeground(Color.WHITE);
        
        useTemplateButton.setFont(buttonFont);
        useTemplateButton.setPreferredSize(new Dimension(120, 35));
        useTemplateButton.setBackground(new Color(255, 140, 0));
        useTemplateButton.setForeground(Color.WHITE);
        
        panel.add(newButton);
        panel.add(saveButton);
        panel.add(deleteButton);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(useTemplateButton);
        return panel;
    }
    
    private JComponent wrapInTitledPanel(JComponent component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150)), 
            title, 
            0, 0, 
            new Font("SansSerif", Font.BOLD, 14), 
            new Color(70, 70, 70)));
        panel.setBackground(new Color(250, 250, 250));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void attachListeners() {
        templateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) handleTableSelection();
        });
        newButton.addActionListener(e -> clearEditor());
        saveButton.addActionListener(e -> saveTemplate());
        deleteButton.addActionListener(e -> deleteTemplate());
        useTemplateButton.addActionListener(e -> useTemplate());
    }

    // === Core Logic ===
    private void loadTemplatesIntoTable() {
        tableModel.setRowCount(0);
        dbManager.getAllTemplates().forEach(tableModel::addRow);
        if (tableModel.getRowCount() > 0) {
            templateTable.setRowSelectionInterval(0, 0);
        }
    }
    
    private void handleTableSelection() {
        int selectedRow = templateTable.getSelectedRow();
        if (selectedRow != -1) {
            int modelRow = templateTable.convertRowIndexToModel(selectedRow);
            selectedTemplateId = (int) tableModel.getValueAt(modelRow, 0);
            String name = (String) tableModel.getValueAt(modelRow, 1);
            String content = dbManager.getTemplateContent(selectedTemplateId);
            templateNameField.setText(name);
            templateContentArea.setText(content);
        }
    }
    
    private void clearEditor() {
        templateTable.clearSelection();
        selectedTemplateId = -1;
        templateNameField.setText("");
        templateContentArea.setText("");
        templateNameField.requestFocus();
    }
    
    private void saveTemplate() {
        String name = templateNameField.getText().trim();
        String content = templateContentArea.getText();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Template name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedTemplateId == -1) {
            dbManager.createTemplate(name, content);
        } else {
            dbManager.updateTemplate(selectedTemplateId, name, content);
        }
        loadTemplatesIntoTable();
    }
    
    private void deleteTemplate() {
        if (selectedTemplateId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a template to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int response = JOptionPane.showConfirmDialog(this, "Delete this template?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            dbManager.deleteTemplate(selectedTemplateId);
            loadTemplatesIntoTable();
            clearEditor();
        }
    }
    
    private void useTemplate() {
        String rawContent = selectedTemplateId != -1 ?
                dbManager.getTemplateContent(selectedTemplateId) :
                templateContentArea.getText();
        
        LinkedHashMap<String, List<String>> sections = parseSections(rawContent);
        String finalOutput = buildOrderedOutput(sections);
        
        deliverAndClose(finalOutput);
    }
    
    private void deliverAndClose(String text) {
        if (onTemplateSelectedCallback != null) {
            onTemplateSelectedCallback.accept(text);
        } else {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
            JOptionPane.showMessageDialog(this, "Content copied to clipboard.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
//        dispose();
    }

    // --- Template Parsing & Formatting ---
    private static LinkedHashMap<String, List<String>> parseSections(String content) {
        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        for (String title : TEXT_AREA_TITLES) {
            sections.put(title, new ArrayList<>());
        }
        String currentSection = null;
        for (String line : content.split("\\r?\\n", -1)) {
            Matcher m = HEADER_PATTERN.matcher(line);
            if (m.matches()) {
                currentSection = m.group(1);
                String afterHeader = m.group(2).trim();
                if (!afterHeader.isEmpty()) {
                    sections.get(currentSection).add(afterHeader);
                }
            } else if (currentSection != null) {
                sections.get(currentSection).add(line);
            } else {
                sections.get("Comment>").add(line);
            }
        }
        return sections;
    }
    
    private static String buildOrderedOutput(LinkedHashMap<String, List<String>> sections) {
        StringBuilder out = new StringBuilder();
        List<String> order = Arrays.asList("CC>", "PI>", "PMH>", "S>", "ROS>", "O>", "Physical Exam>", "A>", "P>", "Comment>");

        for (String label : order) {
            List<String> lines = sections.getOrDefault(label, Collections.emptyList());
            if (lines.isEmpty() || lines.stream().allMatch(String::isBlank)) continue;

            out.append(label);
            String firstLineContent = lines.get(0).trim();
            if (!firstLineContent.isEmpty()) {
                out.append(' ').append(firstLineContent);
            }
            out.append('\n');

            for (int i = 1; i < lines.size(); i++) {
                out.append("\t").append(lines.get(i)).append('\n');
            }
        }
        return out.toString().trim();
    }

    // === Database helper class ===
    private static class DatabaseManager {
        private Connection conn;

        DatabaseManager() {
            try {
                Class.forName("org.sqlite.JDBC");
                Path db = dbPath();
                Files.createDirectories(db.getParent());
                String url = "jdbc:sqlite:" + db.toAbsolutePath();
                this.conn = DriverManager.getConnection(url);
                createTableIfNotExists();
            } catch (Exception e) {
                throw new RuntimeException("Failed to open emr_templates.db", e);
            }
        }

        private void createTableIfNotExists() {
            String sql = "CREATE TABLE IF NOT EXISTS templates (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, content TEXT);";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                System.err.println("Failed to create templates table: " + e.getMessage());
            }
        }

        public List<Object[]> getAllTemplates() {
            List<Object[]> list = new ArrayList<>();
            String sql = "SELECT id, name FROM templates ORDER BY name;";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getInt("id"), rs.getString("name")});
                }
            } catch (SQLException e) {
                System.err.println("Failed to load templates: " + e.getMessage());
            }
            return list;
        }

        public String getTemplateContent(int id) {
            String sql = "SELECT content FROM templates WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("content") : "";
                }
            } catch (SQLException e) {
                System.err.println("Failed to get template content: " + e.getMessage());
                return "";
            }
        }

        public void createTemplate(String name, String content) {
            String sql = "INSERT INTO templates (name, content) VALUES (?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, content);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Failed to create template: " + e.getMessage());
            }
        }

        public void updateTemplate(int id, String name, String content) {
            String sql = "UPDATE templates SET name = ?, content = ? WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, content);
                ps.setInt(3, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Failed to update template: " + e.getMessage());
            }
        }

        public void deleteTemplate(int id) {
            String sql = "DELETE FROM templates WHERE id = ?;";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Failed to delete template: " + e.getMessage());
            }
        }

        public void closeConnection() {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing template database: " + e.getMessage());
                }
            }
        }
    }
}
