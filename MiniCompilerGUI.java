import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;

// Class to limit text field input length and type
class JTextFieldLimit extends PlainDocument {
    private final int limit;
    private final boolean digitsOnly;

    JTextFieldLimit(int limit, boolean digitsOnly) {
        this.limit = limit;
        this.digitsOnly = digitsOnly;
    }

    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
        if (str == null) return;
        if ((getLength() + str.length()) <= limit) {
            if (!digitsOnly || str.matches("\\d*")) super.insertString(offset, str, attr);
        }
    }
}

public class MiniCompilerGUI extends JFrame {

    private final JTextArea codeArea = new JTextArea();
    private final JTextArea outputArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JPanel inputPanel = new JPanel(new BorderLayout());
    private final DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> tests = new JComboBox<>(fileModel);

    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar();
    private boolean isRunning = false;
    private String currentFileName = null;
    private final File savedFilesDir = new File("saved");
    private JButton deleteBtn;
    private File currentUploadedFile = null; // Track uploaded file path

    public MiniCompilerGUI() {
        super("Mini Compiler IDE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Create saved files directory if it doesn't exist
        if (!savedFilesDir.exists()) {
            savedFilesDir.mkdirs();
        }

        // Apply a dark, modernized Nimbus look without external dependencies
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        applyDarkTheme();

        // Initialize file dropdown with default files and saved files
        initializeFileList();

        setLayout(new BorderLayout());
        createMenuBar();
        createToolbar();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.6);
        split.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(new Color(75, 0, 130), 1)));
        split.setDividerSize(8);
        add(split, BorderLayout.CENTER);

        setupCodeEditor();
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setRowHeaderView(new LineNumberView(codeArea));
        codeScroll.getViewport().setBackground(new Color(15, 15, 15));
        TitledBorder codeTitle = new TitledBorder(new LineBorder(new Color(138, 43, 226), 1), "Code Editor",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(200, 150, 255));
        codeScroll.setBorder(new CompoundBorder(codeTitle, new EmptyBorder(6, 6, 6, 6)));
        split.setLeftComponent(codeScroll);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(new EmptyBorder(10, 5, 10, 10));
        split.setRightComponent(right);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        TitledBorder controlsTitle = new TitledBorder(new LineBorder(new Color(138, 43, 226), 1), "Controls",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(200, 150, 255));
        top.setBorder(new CompoundBorder(controlsTitle, new EmptyBorder(10, 10, 10, 10)));

        JPanel row1 = new JPanel(new BorderLayout(8, 8));
        JLabel testLbl = new JLabel("Choose Test Case:");
        testLbl.setForeground(new Color(200, 150, 255));
        row1.add(testLbl, BorderLayout.WEST);
        tests.setPreferredSize(new Dimension(20, 15));
        row1.add(tests, BorderLayout.CENTER);
        top.add(row1);
        top.add(Box.createVerticalStrut(15));

        JPanel row3 = new JPanel(new BorderLayout(8, 8));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton newFileBtn = createStyledButton("New File", new Color(138, 43, 226)); // Purple
        JButton uploadBtn = createStyledButton("Upload", new Color(138, 43, 226)); // Purple
        JButton saveBtn = createStyledButton("Save", new Color(138, 43, 226)); // Purple
        deleteBtn = createStyledButton("Delete", new Color(220, 20, 60)); // Red for delete
        JButton runBtn = createStyledButton("Run", new Color(138, 43, 226)); // Purple
        JButton clearBtn = createStyledButton("Clear", new Color(75, 0, 130)); // Dark purple
        newFileBtn.setToolTipText("Create a new file");
        uploadBtn.setToolTipText("Upload a C++ file from your computer");
        saveBtn.setToolTipText("Save current file");
        deleteBtn.setToolTipText("Delete selected saved file");
        deleteBtn.setEnabled(false);
        runBtn.setToolTipText("Run program (F5)");
        clearBtn.setToolTipText("Clear output");
        buttonPanel.add(newFileBtn);
        buttonPanel.add(uploadBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(runBtn);
        buttonPanel.add(clearBtn);
        row3.add(buttonPanel, BorderLayout.NORTH);
        row3.add(inputPanel, BorderLayout.CENTER);
        top.add(row3);
        // We'll place controls and output into a vertical split for better balance

        setupOutputArea();
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.getViewport().setBackground(new Color(15, 15, 15));
        TitledBorder outputTitle = new TitledBorder(new LineBorder(new Color(138, 43, 226), 1), "Program Output",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(200, 150, 255));
        outScroll.setBorder(new CompoundBorder(outputTitle, new EmptyBorder(6, 6, 6, 6)));
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, outScroll);
        rightSplit.setResizeWeight(0.35); // allocate ~35% for controls/input, 65% for output
        rightSplit.setDividerSize(8);
        rightSplit.setBorder(new LineBorder(new Color(75, 0, 130), 1));
        right.add(rightSplit, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);

        tests.addActionListener(e -> {
            applySelection();
            updateDeleteButtonState();
        });
        newFileBtn.addActionListener(this::createNewFile);
        uploadBtn.addActionListener(this::uploadFile);
        saveBtn.addActionListener(this::saveCurrentFile);
        deleteBtn.addActionListener(this::deleteCurrentFile);
        runBtn.addActionListener(this::runProgram);
        clearBtn.addActionListener(e -> { outputArea.setText(""); updateStatus("Output cleared"); });

        // Show PL1 by default
        applySelection();
        updateDeleteButtonState();
    }

    private void applyDarkTheme() {
        ThemeUtil.applyPurpleDarkTheme();
        getContentPane().setBackground(new Color(20, 20, 20));
    }

    private void initializeFileList() {
        fileModel.removeAllElements();
        fileModel.addElement("C++ (PL1)");
        fileModel.addElement("C++ (PL2)");
        fileModel.addElement("C++ (PL3)");
        
        // Load saved files
        if (savedFilesDir.exists() && savedFilesDir.isDirectory()) {
            File[] savedFiles = savedFilesDir.listFiles((dir, name) -> name.endsWith(".cpp"));
            if (savedFiles != null) {
                Arrays.sort(savedFiles, Comparator.comparing(File::getName));
                for (File file : savedFiles) {
                    fileModel.addElement("Saved: " + file.getName());
                }
            }
        }
    }

    private void createNewFile(ActionEvent e) {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name (without extension):", "New File", JOptionPane.PLAIN_MESSAGE);
        if (fileName != null && !fileName.trim().isEmpty()) {
            fileName = fileName.trim();
            if (!fileName.endsWith(".cpp")) {
                fileName += ".cpp";
            }
            
            // Check if file already exists
            File newFile = new File(savedFilesDir, fileName);
            if (newFile.exists()) {
                int result = JOptionPane.showConfirmDialog(this, 
                    "File already exists. Overwrite?", 
                    "File Exists", 
                    JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Clear code area and set new file
            codeArea.setText("");
            currentFileName = fileName;
            updateStatus("New file: " + fileName);
            
            // Add to dropdown if not already there
            String displayName = "Saved: " + fileName;
            if (!fileModelContains(displayName)) {
                fileModel.addElement(displayName);
                tests.setSelectedItem(displayName);
            } else {
                tests.setSelectedItem(displayName);
            }
            
            inputPanel.setVisible(false);
            outputArea.setText("New file created. Start coding!");
        }
    }

    private String runLexer(String code) {
        try {
            // Step 1: Create a temporary file
            File tempFile = File.createTempFile("input", ".cpp");  // Creates a file with a random name, ending in ".cpp"
            
            // Step 2: Write the code to the temporary file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(code);  // Write the code entered by the user in the codeArea
            }
    
            // Step 3: Run the lexer on the temp file
            // Here we assume that the lexer is a compiled executable that reads from a file.
            Process process = Runtime.getRuntime().exec("./lexer " + tempFile.getAbsolutePath());
    
            // Step 4: Capture the output of the lexer (tokens)
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
    
            // Wait for the process to finish
            process.waitFor();
    
            // Step 5: Delete the temporary file after processing
            tempFile.delete();
    
            // Return the output from the lexer (tokens)
            return output.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error: Lexer execution failed.";
        }
    }
    

    private boolean fileModelContains(String item) {
        for (int i = 0; i < fileModel.getSize(); i++) {
            if (fileModel.getElementAt(i).equals(item)) {
                return true;
            }
        }
        return false;
    }

    private void saveCurrentFile(ActionEvent e) {
        String fileName;
        
        if (currentFileName == null && currentUploadedFile == null) {
            // Prompt for file name
            fileName = JOptionPane.showInputDialog(this, "Enter file name (without extension):", "Save File", JOptionPane.PLAIN_MESSAGE);
            if (fileName == null || fileName.trim().isEmpty()) {
                return;
            }
            fileName = fileName.trim();
            if (!fileName.endsWith(".cpp")) {
                fileName += ".cpp";
            }
            currentFileName = fileName;
        } else if (currentUploadedFile != null) {
            // If it's an uploaded file, copy it to saved directory
            String uploadedFileName = currentUploadedFile.getName();
            fileName = uploadedFileName;
            currentFileName = fileName;
            currentUploadedFile = null; // Clear uploaded file reference
        } else {
            fileName = currentFileName;
        }
        
        File fileToSave = new File(savedFilesDir, fileName);
        try (FileWriter writer = new FileWriter(fileToSave)) {
            writer.write(codeArea.getText());
            updateStatus("File saved: " + fileName);
            
            // Add to dropdown if not already there
            String displayName = "Saved: " + fileName;
            if (!fileModelContains(displayName)) {
                fileModel.addElement(displayName);
            }
            tests.setSelectedItem(displayName);
            currentFileName = fileName;
            currentUploadedFile = null;
            updateDeleteButtonState();
            
            JOptionPane.showMessageDialog(this, "File saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            updateStatus("Error saving file");
        }
    }

    private void uploadFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select C++ File to Upload");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".cpp") || 
                       f.getName().toLowerCase().endsWith(".c") || 
                       f.getName().toLowerCase().endsWith(".cc");
            }
            
            @Override
            public String getDescription() {
                return "C++ Files (*.cpp, *.c, *.cc)";
            }
        });
        
        // Apply dark theme to file chooser
        applyFileChooserTheme(fileChooser);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                StringBuilder content = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                codeArea.setText(content.toString());
                currentUploadedFile = selectedFile;
                currentFileName = null; // Clear saved file name
                
                // Ask if user wants to save it to the saved directory
                int saveOption = JOptionPane.showConfirmDialog(this,
                    "File loaded successfully!\n\nDo you want to add it to your saved files?",
                    "File Uploaded",
                    JOptionPane.YES_NO_OPTION);
                
                if (saveOption == JOptionPane.YES_OPTION) {
                    String fileName = selectedFile.getName();
                    File fileToSave = new File(savedFilesDir, fileName);
                    
                    // Check if file already exists in saved directory
                    if (fileToSave.exists()) {
                        int overwrite = JOptionPane.showConfirmDialog(this,
                            "File already exists in saved files. Overwrite?",
                            "File Exists",
                            JOptionPane.YES_NO_OPTION);
                        if (overwrite != JOptionPane.YES_OPTION) {
                            // Don't save, but keep the file loaded
                            outputArea.setText("File loaded: " + fileName + "\nYou can edit and run it, or save it with a different name.");
                            updateStatus("File loaded: " + fileName);
                            updateDeleteButtonState();
                            return;
                        }
                    }
                    
                    // Copy file to saved directory
                    try (FileWriter writer = new FileWriter(fileToSave)) {
                        writer.write(content.toString());
                    }
                    
                    // Add to dropdown if not already there
                    String displayName = "Saved: " + fileName;
                    if (!fileModelContains(displayName)) {
                        fileModel.addElement(displayName);
                    }
                    tests.setSelectedItem(displayName);
                    currentFileName = fileName;
                    currentUploadedFile = null;
                    
                    JOptionPane.showMessageDialog(this, "File saved and added to your files!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    updateStatus("File uploaded and saved: " + fileName);
                } else {
                    outputArea.setText("File loaded: " + selectedFile.getName() + "\nYou can edit and run it. Use Save to add it to your saved files.");
                    updateStatus("File loaded: " + selectedFile.getName());
                }
                
                inputPanel.setVisible(false);
                updateDeleteButtonState();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                updateStatus("Error loading file");
            }
        }
    }

    private void deleteCurrentFile(ActionEvent e) {
        String selected = (String) tests.getSelectedItem();
        if (selected == null || !selected.startsWith("Saved: ")) {
            return;
        }
        
        String fileName = selected.substring(7); // Remove "Saved: " prefix
        File fileToDelete = new File(savedFilesDir, fileName);
        
        if (!fileToDelete.exists()) {
            JOptionPane.showMessageDialog(this, "File not found: " + fileName, "Error", JOptionPane.ERROR_MESSAGE);
            // Remove from dropdown if file doesn't exist
            fileModel.removeElement(selected);
            updateDeleteButtonState();
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete:\n" + fileName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            if (fileToDelete.delete()) {
                fileModel.removeElement(selected);
                updateStatus("File deleted: " + fileName);
                
                // If this was the current file, clear the editor
                if (fileName.equals(currentFileName)) {
                    codeArea.setText("");
                    currentFileName = null;
                    outputArea.setText("File deleted. Select another file or create a new one.");
                }
                
                // Select first item in dropdown
                if (fileModel.getSize() > 0) {
                    tests.setSelectedIndex(0);
                    applySelection();
                }
                
                updateDeleteButtonState();
                JOptionPane.showMessageDialog(this, "File deleted successfully!", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete file: " + fileName, "Error", JOptionPane.ERROR_MESSAGE);
                updateStatus("Error deleting file");
            }
        }
    }

    private void updateDeleteButtonState() {
        String selected = (String) tests.getSelectedItem();
        deleteBtn.setEnabled(selected != null && selected.startsWith("Saved: "));
    }

    private void applyFileChooserTheme(JFileChooser fileChooser) {
        // Try to apply dark theme to file chooser components
        try {
            UIManager.put("FileChooser.background", new Color(25, 25, 25));
            UIManager.put("FileChooser.foreground", new Color(220, 220, 220));
            SwingUtilities.updateComponentTreeUI(fileChooser);
        } catch (Exception ex) {
            // Ignore if theme application fails
        }
    }

    private void setupCodeEditor() {
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeArea.setBackground(new Color(15, 15, 15));
        codeArea.setForeground(new Color(220, 220, 220));
        codeArea.setCaretColor(new Color(200, 150, 255));
        codeArea.setSelectionColor(new Color(138, 43, 226));
        codeArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        codeArea.setLineWrap(false);
        codeArea.setTabSize(4);

        inputPanel.setBackground(new Color(20, 20, 20));
        inputPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel inputLbl = new JLabel("Input: ");
        inputLbl.setForeground(new Color(200, 150, 255));
        inputPanel.add(inputLbl, BorderLayout.WEST);
        inputField.setDocument(new JTextFieldLimit(20, false)); // allow all input
        inputField.setBackground(new Color(25, 25, 25));
        inputField.setForeground(new Color(230, 230, 230));
        inputField.setCaretColor(new Color(200, 150, 255));
        inputField.setBorder(new CompoundBorder(new LineBorder(new Color(138, 43, 226), 1), new EmptyBorder(4, 6, 4, 6)));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.setVisible(false);

        codeArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "run");
        codeArea.getActionMap().put("run", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { runProgram(e); }
        });
        codeArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), "selectAll");
        codeArea.getActionMap().put("selectAll", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { codeArea.selectAll(); }
        });
    }

    private void setupOutputArea() {
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputArea.setBackground(new Color(15, 15, 15));
        outputArea.setForeground(new Color(220, 220, 220));
        outputArea.setCaretColor(new Color(200, 150, 255));
        outputArea.setBorder(new EmptyBorder(6, 6, 6, 6));
    }

    private JButton createStyledButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(color.darker(), 1, true), new EmptyBorder(6, 12, 6, 12)));
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(96, 32));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(color.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(color); }
        });
        return b;
    }

    private void createMenuBar() {
        // Remove menu bar as requested
        setJMenuBar(null);
    }

    private void createToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new CompoundBorder(new LineBorder(new Color(75, 0, 130), 1), new EmptyBorder(2, 6, 2, 6)));
        tb.setBackground(new Color(20, 20, 20));
        JLabel title = new JLabel("C++ Mini Compiler");
        title.setForeground(new Color(200, 150, 255));
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tb.setLayout(new BorderLayout());
        tb.add(title, BorderLayout.WEST);
        add(tb, BorderLayout.NORTH);
    }

    private JPanel createStatusBar() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBorder(new CompoundBorder(new EmptyBorder(6, 12, 6, 12), new LineBorder(new Color(75, 0, 130), 1)));
        sb.setBackground(new Color(20, 20, 20));
        sb.setMinimumSize(new Dimension(0, 36)); // Ensure minimum height for larger text
        progressBar.setPreferredSize(new Dimension(220, 28));
        progressBar.setMinimumSize(new Dimension(220, 28));
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setForeground(new Color(138, 43, 226));
        progressBar.setBackground(new Color(30, 30, 30));
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Larger font
        statusLabel.setForeground(new Color(200, 150, 255));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Larger font for status text (14pt)
        statusLabel.setBorder(new EmptyBorder(0, 0, 0, 15)); // Add right padding
        sb.add(statusLabel, BorderLayout.WEST);
        sb.add(progressBar, BorderLayout.EAST);
        return sb;
    }

    private void updateStatus(String msg) {
        SwingUtilities.invokeLater(() -> { statusLabel.setText(msg); progressBar.setString(msg); });
    }

    private void showAbout(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "Mini Compiler IDE v2.0\n\nBuilt-in evaluator for PL1/PL2/PL3.",
                "About Mini Compiler IDE", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showShortcuts(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "F5 - Run Program\nCtrl+A - Select All",
                "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    // Simple file loader
    private String loadFileContent(String filename) {
        try {
            File file;
            // Check if it's a saved file
            if (filename.startsWith("Saved: ")) {
                String actualFileName = filename.substring(7); // Remove "Saved: " prefix
                file = new File(savedFilesDir, actualFileName);
            } else {
                file = new File(filename);
            }
            
            if (!file.exists()) {
                String defaultContent = switch (filename) {
                    case "PL1.cpp" -> "cin >> x;\ncout << x;";
                    case "PL2.cpp" -> "x = 3;\ny = 7;\ncout<< x + y;";
                    case "PL3.cpp" -> "cout<< \"Hello, World!\";";
                    default -> "";
                };
                if (!filename.startsWith("Saved: ")) {
                    try (FileWriter w = new FileWriter(file)) { w.write(defaultContent); }
                }
                return defaultContent;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            return "// Error: Could not load file.";
        }
    }

    // Selection logic for test cases
    private void applySelection() {
        String selected = (String) tests.getSelectedItem();
        if (selected == null) return;
        
        if (selected.equals("C++ (PL1)")) {
            codeArea.setText(loadFileContent("PL1.cpp"));
            outputArea.setText("PL1: Enter input and click Run");
            inputPanel.setVisible(true);
            inputField.setEnabled(true);
            inputField.requestFocusInWindow();
            updateStatus("PL1 ready");
            currentFileName = null;
            currentUploadedFile = null;
        } else if (selected.equals("C++ (PL2)")) {
            codeArea.setText(loadFileContent("PL2.cpp"));
            outputArea.setText("PL2: Click Run");
            inputPanel.setVisible(false);
            updateStatus("PL2 ready");
            currentFileName = null;
            currentUploadedFile = null;
        } else if (selected.equals("C++ (PL3)")) {
            codeArea.setText(loadFileContent("PL3.cpp"));
            outputArea.setText("PL3: Click Run");
            inputPanel.setVisible(false);
            updateStatus("PL3 ready");
            currentFileName = null;
            currentUploadedFile = null;
        } else if (selected.startsWith("Saved: ")) {
            String fileName = selected.substring(7); // Remove "Saved: " prefix
            codeArea.setText(loadFileContent(selected));
            outputArea.setText("Loaded: " + fileName + " - Click Run to execute");
            inputPanel.setVisible(false);
            currentFileName = fileName;
            currentUploadedFile = null;
            updateStatus("Loaded: " + fileName);
        }
    }

    // --- Simple evaluator for PL1/PL2/PL3 ---
    private String interpretMini(String code, int mode) throws Exception {
        String normalized = code.replaceAll("//.*", "").replace("\r", "").trim();
        if (mode == 1) {
            String in = inputField.getText().trim();
            return in.isEmpty() ? "No input provided" : in;
        }

        Map<String, Integer> vars = new HashMap<>();
        Pattern assign = Pattern.compile("\\b([a-zA-Z_]\\w*)\\s*=\\s*(-?\\d+)\\s*;");
        Pattern coutNum = Pattern.compile("cout\\s*<<\\s*([a-zA-Z_]\\w*|\\d+)\\s*(?:\\+\\s*([a-zA-Z_]\\w*|\\d+))?\\s*;?");
        Pattern coutStr = Pattern.compile("cout\\s*<<\\s*\"(.*?)\"\\s*;?");
        String[] lines = normalized.split("\\n+");
        String lastOutput = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            Matcher mAssign = assign.matcher(line);
            Matcher mCoutStr = coutStr.matcher(line);
            Matcher mCoutNum = coutNum.matcher(line);

            if (mAssign.matches()) {
                vars.put(mAssign.group(1), Integer.parseInt(mAssign.group(2)));
            } else if (mCoutStr.matches()) {
                lastOutput = mCoutStr.group(1);
            } else if (mCoutNum.matches()) {
                int a = resolveValue(mCoutNum.group(1), vars);
                String bTok = mCoutNum.group(2);
                int val = (bTok == null) ? a : a + resolveValue(bTok, vars);
                lastOutput = String.valueOf(val);
            }
        }
        if (lastOutput == null) throw new Exception("No output from program");
        return lastOutput;
    }

    private int resolveValue(String token, Map<String, Integer> vars) throws Exception {
        if (token == null) return 0;
        token = token.trim();
        if (token.matches("-?\\d+")) return Integer.parseInt(token);
        Integer v = vars.get(token);
        if (v == null) throw new Exception("Undefined variable: " + token);
        return v;
    }

    private void runProgram(ActionEvent e) {
        if (isRunning) return; // Prevent running if already running
        isRunning = true;
        progressBar.setIndeterminate(true);
        updateStatus("Running program...");
    
        final String code = codeArea.getText().trim();
        if (code.isEmpty()) {
            outputArea.setText("Error: No code to execute");
            updateStatus("Error: No code to execute");
            isRunning = false;
            progressBar.setIndeterminate(false);
            return;
        }
    
        // Step 1: Pass code to lexer
        String lexerOutput = runLexer(code);
        if (lexerOutput.contains("Error")) {
            outputArea.setText(lexerOutput);
            updateStatus("Lexer error");
            isRunning = false;
            progressBar.setIndeterminate(false);
            return;
        }
    
        // Step 2: Pass lexer output (tokens) to parser
        String parserOutput = runParser(lexerOutput);
        if (parserOutput.contains("Error")) {
            outputArea.setText(parserOutput);
            updateStatus("Parser error");
            isRunning = false;
            progressBar.setIndeterminate(false);
            return;
        }
    
        // Step 3: Display the result from the parser
        outputArea.setText("Program Output:\n" + parserOutput);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
        updateStatus("Program executed successfully");
        isRunning = false;
        progressBar.setIndeterminate(false);
    }

    private String runParser(String lexerOutput) {
        // Step 1: Assuming lexerOutput is a string of tokens
        // Step 2: You may need to process these tokens and pass them to a parser
        // If the lexer output is a file, you can write it to a temp file just like in runLexer()
    
        // Example: Parsing the tokens (simplified)
        try {
            Process parserProcess = Runtime.getRuntime().exec("./parser " + lexerOutput);
            BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            parserProcess.waitFor();
            return output.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error: Parser execution failed.";
        }
    }
    
    
    private String runLexerAndParser(String code) {
        try {
            // Write the code to a temporary file
            File tempFile = File.createTempFile("input", ".cpp");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(code);
            }
    
            // Run the lexer and parser on the temp file (assuming your lexer/parser are compiled as executables)
            Process process = Runtime.getRuntime().exec("./lexer_parser " + tempFile.getAbsolutePath());
    
            // Read the output from the lexer and parser
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
    
            // Wait for the process to finish
            process.waitFor();
    
            return output.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error: Could not run lexer/parser.";
        }
    }
        
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MiniCompilerGUI().setVisible(true));
    }
}

// --- Theming helpers ---
class ThemeUtil {
    static void applyPurpleDarkTheme() {
        // Core Nimbus palette overrides with purple theme
        UIManager.put("control", new Color(25, 25, 25));
        UIManager.put("info", new Color(30, 30, 30));
        UIManager.put("nimbusBase", new Color(10, 10, 10));
        UIManager.put("nimbusBlueGrey", new Color(50, 50, 50));
        UIManager.put("nimbusLightBackground", new Color(20, 20, 20));
        UIManager.put("text", new Color(220, 220, 220));
        UIManager.put("nimbusSelectionBackground", new Color(138, 43, 226));
        UIManager.put("nimbusFocus", new Color(200, 150, 255));

        // General components
        UIManager.put("Menu.background", new Color(20, 20, 20));
        UIManager.put("MenuItem.background", new Color(20, 20, 20));
        UIManager.put("Menu.foreground", new Color(200, 150, 255));
        UIManager.put("MenuItem.foreground", new Color(200, 150, 255));
        UIManager.put("Panel.background", new Color(20, 20, 20));
        UIManager.put("ScrollPane.background", new Color(20, 20, 20));
        UIManager.put("ToolBar.background", new Color(20, 20, 20));
        UIManager.put("Table.background", new Color(25, 25, 25));
        UIManager.put("Table.foreground", new Color(220, 220, 220));
        UIManager.put("Label.foreground", new Color(200, 150, 255));
        UIManager.put("ComboBox.background", new Color(25, 25, 25));
        UIManager.put("ComboBox.foreground", new Color(220, 220, 220));
        UIManager.put("TextField.background", new Color(25, 25, 25));
        UIManager.put("TextField.foreground", new Color(220, 220, 220));

        // Font: apply a saner default for a professional look
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        for (Object key : UIManager.getDefaults().keySet()) {
            Object val = UIManager.get(key);
            if (val instanceof Font) {
                UIManager.put(key, base);
            }
        }
    }
}

// --- Line number gutter for code editor ---
class LineNumberView extends JComponent {
    private static final int MARGIN = 6;
    private final JTextArea textArea;
    private final Font numberFont = new Font("Segoe UI", Font.PLAIN, 11);

    LineNumberView(JTextArea textArea) {
        this.textArea = textArea;
        setForeground(new Color(150, 100, 200));
        setBackground(new Color(10, 10, 10));
        setOpaque(true);

        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { repaint(); revalidate(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { repaint(); revalidate(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); revalidate(); }
        });
        textArea.addCaretListener(e -> repaint());
    }

    @Override
    public Dimension getPreferredSize() {
        int lineCount = Math.max(1, textArea.getLineCount());
        int digits = String.valueOf(lineCount).length();
        FontMetrics fm = getFontMetrics(numberFont);
        int width = MARGIN * 2 + fm.charWidth('0') * digits;
        return new Dimension(width, textArea.getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle clip = g.getClipBounds();
        g.setColor(getBackground());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        g.setFont(numberFont);
        g.setColor(getForeground());

        int startOffset = textArea.viewToModel(new Point(0, clip.y));
        int endOffset = textArea.viewToModel(new Point(0, clip.y + clip.height));

        while (startOffset <= endOffset) {
            try {
                int line = textArea.getLineOfOffset(startOffset);
                Rectangle r = textArea.modelToView(startOffset);
                if (r == null) break;
                String lineNumber = String.valueOf(line + 1);
                int x = getPreferredSize().width - MARGIN - g.getFontMetrics().stringWidth(lineNumber);
                int y = r.y + r.height - g.getFontMetrics().getDescent();
                g.drawString(lineNumber, x, y);
                startOffset = textArea.getLineEndOffset(line) + 1;
            } catch (Exception ex) {
                break;
            }
        }

        // Right separator line
        g.setColor(new Color(138, 43, 226));
        g.drawLine(getWidth() - 1, clip.y, getWidth() - 1, clip.y + clip.height);
    }
}

