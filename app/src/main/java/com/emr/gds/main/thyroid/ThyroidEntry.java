package com.emr.gds.main.thyroid;

import com.emr.gds.input.IAIMain;
import com.emr.gds.input.IAITextAreaManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Main Entry point for Thyroid Module.
 * Contains nested classes for specific sub-menus and forms.
 */
public class ThyroidEntry {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> MainMenu.show());
    }

    // =================================================================================
    // 1. MAIN MENU
    // =================================================================================
    public static class MainMenu {
        public static void show() {
            JFrame frame = UIHelper.createFrame("Select category ...", 300, 460);
            frame.setLocation(0, 610);
            frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

            String[] menuItems = {
                "Thyroid Physical examination",
                "Pregnancy with Thyroid Disease",
                "Hyperthyroidism Symptom",
                "Hypothyroidism Symptom",
                "Medications",
                "Abnormal TFT on Routine check",
                "Thyroidal nodule",
                "Post operation F/U PTC",
                "Quit"
            };

            for (String item : menuItems) {
                JButton button = UIHelper.createStyledButton(item);
                button.setPreferredSize(new Dimension(200, 40));
                button.setMaximumSize(new Dimension(200, 40));
                button.setAlignmentX(Box.CENTER_ALIGNMENT);
                
                button.addActionListener(e -> handleAction(frame, item));
                
                frame.add(button);
                frame.add(Box.createVerticalStrut(10));
            }

            // Auto-close timer (5 mins)
            UIHelper.addAutoCloseTimer(frame, 300000);
            frame.setVisible(true);
        }

        private static void handleAction(JFrame frame, String command) {
            switch (command) {
                case "Quit" -> frame.dispose();
                case "Thyroid Physical examination" -> new PhysicalExam();
                case "Pregnancy with Thyroid Disease" -> PregnancyMenu.show();
                case "Hyperthyroidism Symptom", "Hypothyroidism Symptom", "Medications" -> 
                    JOptionPane.showMessageDialog(frame, command + " module not available.");
                default -> { /* Placeholder for others */ }
            }
        }
    }

    // =================================================================================
    // 2. PHYSICAL EXAM FORM
    // =================================================================================
    public static class PhysicalExam extends JFrame {
        private static final String[][] EXAM_SECTIONS = {
            {"Goiter Ruled", "Goiter ruled out", "Goiter ruled in Diffuse Enlargement", "Goiter ruled in Nodular Enlargement", "Single Nodular Goiter", "Multiple Nodular Goiter"},
            {"Detect any nodules", "None", "Single nodule", "Multinodular Goiter"},
            {"Thyroid gland consistency", "Soft", "Soft to Firm", "Firm", "Cobble-stone", "Firm to Hard", "Hard"},
            {"Evaluate the thyroid gland for tenderness", "Tender", "Non-tender"},
            {"Systolic or continuous Bruit (y/n)", "Yes", "No"},
            {"DTR deep tendon reflex", "1+ = present but depressed", "2+ = normal / average", "3+ = increased", "4+ = clonus", "Doctor has not performed DTR test"},
            {"TED: Thyroid Eye Disease", "Class 0: No signs", "Class 1: Only signs", "Class 2: Soft tissue", "Class 3: Proptosis", "Class 4: EOM involvement", "Class 5: Corneal", "Class 6: Sight loss"}
        };

        private final JTextField goiterSizeField = new JTextField(10);
        private final JTextArea outputArea = new JTextArea();
        private final JCheckBox[][] checkBoxGroups = new JCheckBox[EXAM_SECTIONS.length][];

        public PhysicalExam() {
            UIHelper.setupFrame(this, "Thyroid Physical Exam", 1000, 1000);
            setLayout(new BorderLayout());

            // --- Top Panel (Inputs) ---
            JPanel inputPanel = new JPanel(new GridLayout(3, 3));
            
            // Goiter Size Section
            JPanel sizePanel = new JPanel();
            sizePanel.add(new JLabel("Goiter size (mL) "));
            goiterSizeField.setPreferredSize(new Dimension(100, 35));
            sizePanel.add(goiterSizeField);
            inputPanel.add(sizePanel);

            // Checkbox Sections
            for (int i = 0; i < EXAM_SECTIONS.length; i++) {
                JPanel sectionPanel = new JPanel();
                sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
                sectionPanel.setBorder(BorderFactory.createTitledBorder(EXAM_SECTIONS[i][0]));

                checkBoxGroups[i] = new JCheckBox[EXAM_SECTIONS[i].length - 1];
                for (int j = 0; j < checkBoxGroups[i].length; j++) {
                    checkBoxGroups[i][j] = new JCheckBox(EXAM_SECTIONS[i][j + 1]);
                    sectionPanel.add(checkBoxGroups[i][j]);
                }
                inputPanel.add(sectionPanel);
            }

            // --- Bottom Panel (Buttons) ---
            JPanel btnPanel = new JPanel(new FlowLayout());
            JButton btnClear = new JButton("Clear");
            JButton btnExec = new JButton("Execute");
            JButton btnSave = new JButton("Save and Quit");

            btnClear.addActionListener(e -> clearForm());
            btnExec.addActionListener(e -> generateReport());
            btnSave.addActionListener(e -> {
                IAIMain.getTextAreaManager().insertBlockIntoArea(IAITextAreaManager.AREA_PE, outputArea.getText(), true);
                dispose();
            });

            btnPanel.add(btnClear);
            btnPanel.add(btnExec);
            btnPanel.add(btnSave);

            add(inputPanel, BorderLayout.NORTH);
            add(new JScrollPane(outputArea), BorderLayout.CENTER);
            add(btnPanel, BorderLayout.SOUTH);
            setVisible(true);
        }

        private void generateReport() {
            StringBuilder sb = new StringBuilder("<Thyroid Exam>\n");
            sb.append("   Goiter size  :\t[ ").append(goiterSizeField.getText()).append("  ] cc\n");

            String[] labels = {"Goiter", "Nodules", "Consistency", "Tenderness", "Bruit", "DTR", "Werner's Report"};
            for (int i = 0; i < checkBoxGroups.length; i++) {
                sb.append(String.format("   %-12s:\t%s\n", labels[i], UIHelper.getSelectedText(checkBoxGroups[i])));
            }
            outputArea.setText(sb.toString());
        }

        private void clearForm() {
            goiterSizeField.setText("");
            outputArea.setText("");
            for (JCheckBox[] group : checkBoxGroups) {
                for (JCheckBox box : group) box.setSelected(false);
            }
        }
    }

    // =================================================================================
    // 3. PREGNANCY CHIEF COMPLAINT
    // =================================================================================
    public static class PregnancyCC extends JFrame {
        private static final String[] LABELS = {"Pregnancy #:", "Weeks:", "Due Date:", "Diagnosis:", "Transferred from GY:"};
        private final JTextField[] inputs = new JTextField[LABELS.length];

        public PregnancyCC() {
            UIHelper.setupFrame(this, "Thyroid Pregnancy Input", 0, 0); // Pack will handle size
            JPanel mainPanel = new JPanel(new GridLayout(0, 1));

            for (int i = 0; i < LABELS.length; i++) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                inputs[i] = new JTextField(10);
                inputs[i].setHorizontalAlignment(SwingConstants.CENTER);
                
                // Enter key navigation
                int finalI = i;
                inputs[i].addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_ENTER && finalI < LABELS.length - 1) 
                            inputs[finalI + 1].requestFocus();
                    }
                });

                row.add(new JLabel(LABELS[i]));
                row.add(inputs[i]);
                mainPanel.add(row);
            }

            JButton btnAdd = new JButton("Add Pregnancy");
            btnAdd.addActionListener(e -> saveAndClose());
            mainPanel.add(btnAdd);

            add(mainPanel);
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }

        private void saveAndClose() {
            String diag = switch (inputs[3].getText().trim()) {
                case "o" -> "Hypothyroidism Diagnosed";
                case "e" -> "Hyperthyroidism diagnosed";
                case "n" -> "TFT abnormality";
                default -> "Unknown Diagnosis";
            };
            
            String hospital = switch (inputs[4].getText().trim()) {
                case "c" -> "Cheongdam Marie OBGYN";
                case "d" -> "Dogok Hamchoon OBGYN";
                case "o" -> "Other OBGYN";
                default -> "Unknown Hospital";
            };

            String result = String.format("# %s pregnancy  %s weeks  Due-date %s \n\t%s at %s%n",
                    inputs[0].getText(), inputs[1].getText(), inputs[2].getText(), diag, hospital);

            IAIMain.getTextAreaManager().insertBlockIntoArea(IAITextAreaManager.AREA_CC, result, true);
            IAIMain.getTextAreaManager().insertBlockIntoArea(IAITextAreaManager.AREA_A, result, false);
            dispose();
        }
    }

    // =================================================================================
    // 4. PREGNANCY HISTORY FORMS (Hyper & Hypo)
    // =================================================================================
    public static class PregnancyHistoryBase extends JFrame {
        public PregnancyHistoryBase(String title, String[] sections, String[][] items) {
            UIHelper.setupFrame(this, title, 0, 0);
            setLayout(new GridLayout(4, 2));

            for (int i = 0; i < sections.length; i++) {
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.setBorder(BorderFactory.createTitledBorder(sections[i]));
                
                if (i < items.length) {
                    for (String item : items[i]) panel.add(new JCheckBox(item));
                }
                add(panel);
            }
            pack();
            setLocationRelativeTo(null);
            setVisible(true);
        }
    }

    public static class PregnancyHyperHistory extends PregnancyHistoryBase {
        public PregnancyHyperHistory() {
            super("Hyperthyroidism with pregnancy Medical History", 
                new String[]{
                    "Personal Medical History", "Family Medical History", "Current Pregnancy Details",
                    "Symptoms", "Medications", "Previous Pregnancy History", "Laboratory Tests", "Other Medical Conditions"
                },
                new String[][]{
                    {"Previous diagnosis of hyperthyroidism", "Any previous treatments", "History of thyroid surgery/RAI"},
                    {"Family history of thyroid disorders", "Family history of hyperthyroidism during pregnancy"},
                    {"Gestational age (weeks)", "Complications/High-risk factors"},
                    {"Weight loss/No gain", "Rapid heartbeat", "Tremors", "Heat sensitivity", "Fatigue", "Sleep issues", "Diarrhea", "Mood changes"},
                    {"Current medications", "Changes in dosage"},
                    {"History in previous pregnancies", "Complications in previous"},
                    {"TFT results (TSH, T3, T4)", "Antibody tests"},
                    {"Other chronic illnesses", "Autoimmune conditions", "Graves' disease", "Thyroid storm", "Thyroid eye disease"}
                }
            );
        }
    }

    public static class PregnancyHypoHistory extends PregnancyHistoryBase {
        public PregnancyHypoHistory() {
            super("Hypothyroidism with pregnancy Medical History",
                new String[]{
                    "Personal Medical History", "Family Medical History", "Current Pregnancy Details",
                    "Symptoms", "Medications", "Previous Pregnancy History", "Laboratory Tests", "Other Medical Conditions"
                },
                new String[][]{
                    {"Previous diagnosis of hypothyroidism", "Previous treatments", "History of surgery"},
                    {"Family history of thyroid disorders", "Family history during pregnancy"},
                    {"Gestational age (weeks)", "Complications/High-risk factors"},
                    {"Weight gain", "Fatigue", "Dry skin/hair", "Cold intolerance", "Depression"},
                    {"Current medications", "Changes in dosage"},
                    {"History in previous pregnancies", "Complications in previous"},
                    {"TFT results", "Antibody tests"},
                    {"Other chronic illnesses", "Autoimmune conditions"}
                }
            );
        }
    }

    // =================================================================================
    // 5. PREGNANCY MENU
    // =================================================================================
    public static class PregnancyMenu {
        public static void show() {
            JFrame frame = UIHelper.createFrame("Thyroid Pregnancy Management", 410, 400);
            frame.setLayout(new GridLayout(0, 1));
            
            // Position bottom right
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation(screen.width - 410, screen.height - 400);

            String[] buttons = {
                "New Patient for Pregnancy with Thyroid disease",
                "F/U Pregnancy with Normal Thyroid Function (TAb+)",
                "Infertility and Thyroid Function Evaluation",
                "F/U Pregnancy with Hyperthyroidism",
                "F/U Pregnancy with TSH low (Hyperthyroidism/GTT)",
                "F/U Pregnancy with Hypothyroidism",
                "F/U Pregnancy with TSH elevation (Subclinical Hypothyroidism)",
                "Postpartum Thyroiditis",
                "Support Files",
                "Quit"
            };

            for (String txt : buttons) {
                JButton btn = UIHelper.createStyledButton(txt);
                // Custom lighter gradient for this menu
                btn.putClientProperty("colorTop", new Color(240, 230, 210));
                btn.putClientProperty("colorBot", new Color(225, 215, 185));
                
                btn.addActionListener(e -> handleClick(frame, txt));
                frame.add(btn);
            }

            UIHelper.addAutoCloseTimer(frame, 300000);
            frame.setVisible(true);
        }

        private static void handleClick(JFrame frame, String text) {
            if ("Quit".equals(text)) {
                frame.dispose();
                return;
            }
            if ("Support Files".equals(text)) {
                JOptionPane.showMessageDialog(frame, "Module not available");
                frame.dispose();
                return;
            }
            
            // Logic actions
            if (text.startsWith("New Patient")) new PregnancyCC();
            else if (text.contains("Hyperthyroidism")) new PregnancyHyperHistory();
            else if (text.contains("Hypothyroidism")) new PregnancyHypoHistory();

            // EMR Text Update
            if (!text.startsWith("New Patient")) {
                String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                String condition = text.replace("F/U ", "");
                
                IAIMain.getTextAreaManager().insertBlockIntoArea(IAITextAreaManager.AREA_CC, 
                    String.format("F/U [   ] weeks    %s%n\t%s", date, condition), true);
                IAIMain.getTextAreaManager().insertBlockIntoArea(IAITextAreaManager.AREA_A, 
                    String.format("%n  #  %s  [%s]", text, date), false);
                IAIMain.getTextAreaManager().insertBlockIntoArea(IAITextAreaManager.AREA_P, 
                    String.format("...Plan F/U [   ] weeks%n\t %s", condition), false);
            }
        }
    }

    // =================================================================================
    // 6. UI HELPERS (Handling repetitive tasks)
    // =================================================================================
    private static class UIHelper {
        public static JFrame createFrame(String title, int w, int h) {
            JFrame f = new JFrame(title);
            f.setSize(w, h);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            return f;
        }

        public static void setupFrame(JFrame f, String title, int w, int h) {
            f.setTitle(title);
            if (w > 0 && h > 0) f.setSize(w, h);
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }

        public static JButton createStyledButton(String text) {
            JButton btn = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    if (g instanceof Graphics2D g2d) {
                        // Check for custom colors or use default
                        Color c1 = (Color) getClientProperty("colorTop");
                        Color c2 = (Color) getClientProperty("colorBot");
                        if (c1 == null) c1 = new Color(210, 180, 140);
                        if (c2 == null) c2 = new Color(180, 150, 110);
                        
                        g2d.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                        g2d.fillRect(0, 0, getWidth(), getHeight());
                    }
                    super.paintComponent(g);
                }
            };
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            return btn;
        }

        public static void addAutoCloseTimer(JFrame frame, int delay) {
            Timer t = new Timer(delay, e -> frame.dispose());
            t.setRepeats(false);
            t.start();
        }

        public static String getSelectedText(JCheckBox[] boxes) {
            return Arrays.stream(boxes)
                    .filter(JCheckBox::isSelected)
                    .map(JCheckBox::getText)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
    }
}