// Import statements for AWT (Abstract Window Toolkit) components
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
    // Maximum number of characters allowed
    private final int limit;
    // Flag to restrict input to digits only
    private final boolean digitsOnly;

    // Constructor to initialize limit and digitsOnly flag
    JTextFieldLimit(int limit, boolean digitsOnly) {
        this.limit = limit;
        this.digitsOnly = digitsOnly;
    }

    // Override method to insert string with validation
    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
        // Return if string is null
        if (str == null) return;
        // Check if adding string doesn't exceed limit
        if ((getLength() + str.length()) <= limit) {
            // If digitsOnly is false or string contains only digits, insert it
            if (!digitsOnly || str.matches("\\d*")) super.insertString(offset, str, attr);
        }
    }
}

// Main GUI class extending JFrame
public class MiniCompilerGUI extends JFrame {

    // Text area for writing code
    private final JTextArea codeArea = new JTextArea();
    // Text area for displaying program output
    private final JTextArea outputArea = new JTextArea();
    // Text field for user input
    private final JTextField inputField = new JTextField();
    // Panel containing the input field
    private final JPanel inputPanel = new JPanel(new BorderLayout());
    // Model for combo box to store file names
    private final DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
    // Combo box for selecting test cases/files
    private final JComboBox<String> tests = new JComboBox<>(fileModel);

    // Label to display status messages
    private final JLabel statusLabel = new JLabel("Ready");
    // Progress bar to show program execution status
    private final JProgressBar progressBar = new JProgressBar();
    // Flag to track if program is currently running
    private boolean isRunning = false;
    // Current file name being edited
    private String currentFileName = null;
    // Directory to store saved files
    private final File savedFilesDir = new File("saved");
    // Button to delete files
    private JButton deleteBtn;
    // Track the current uploaded file path
    private File currentUploadedFile = null; // Track uploaded file path

    // Constructor to set up the GUI
    public MiniCompilerGUI() {
        // Set window title
        super("Mini Compiler IDE");
        // Exit application when window is closed
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Set window size
        setSize(1200, 800);
        // Center window on screen
        setLocationRelativeTo(null);

        // Create saved files directory if it doesn't exist
        if (!savedFilesDir.exists()) {
            // Create directory including parent directories
            savedFilesDir.mkdirs();
        }

        // Apply a dark, modernized Nimbus look without external dependencies
        try {
            // Set Nimbus look and feel
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {} // Ignore if Nimbus is not available
        // Apply custom dark theme
        applyDarkTheme();

        // Initialize file dropdown with default files and saved files
        initializeFileList();

        // Set layout manager to BorderLayout
        setLayout(new BorderLayout());
        // Create and add menu bar
        createMenuBar();
        // Create and add toolbar
        createToolbar();

        // Create split pane to divide editor and output areas
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // Set resize weight (60% for left, 40% for right)
        split.setResizeWeight(0.6);
        // Set border with padding and colored line
        split.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(new Color(75, 0, 130), 1)));
        // Set divider size
        split.setDividerSize(8);
        // Add split pane to center of frame
        add(split, BorderLayout.CENTER);

        // Set up code editor with styling
        setupCodeEditor();
        // Create scroll pane for code area
        JScrollPane codeScroll = new JScrollPane(codeArea);
        // Add line numbers to the left of code editor
        codeScroll.setRowHeaderView(new LineNumberView(codeArea));
        // Set background color for viewport
        codeScroll.getViewport().setBackground(new Color(15, 15, 15));
        // Create titled border for code editor
        TitledBorder codeTitle = new TitledBorder(new LineBorder(new Color(138, 43, 226), 1), "Code Editor",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(200, 150, 255));
        // Set border with title and padding
        codeScroll.setBorder(new CompoundBorder(codeTitle, new EmptyBorder(6, 6, 6, 6)));
        // Add code scroll pane to left side of split pane
        split.setLeftComponent(codeScroll);

        // Create panel for right side (controls and output)
        JPanel right = new JPanel(new BorderLayout(10, 10));
        // Set border with padding
        right.setBorder(new EmptyBorder(10, 5, 10, 10));
        // Add right panel to split pane
        split.setRightComponent(right);

        // Create panel for controls at top
        JPanel top = new JPanel();
        // Set vertical box layout
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        // Create titled border for controls
        TitledBorder controlsTitle = new TitledBorder(new LineBorder(new Color(138, 43, 226), 1), "Controls",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(200, 150, 255));
        // Set border with title and padding
        top.setBorder(new CompoundBorder(controlsTitle, new EmptyBorder(10, 10, 10, 10)));

        // Create first row for test case selection
        JPanel row1 = new JPanel(new BorderLayout(8, 8));
        // Create label for test case dropdown
        JLabel testLbl = new JLabel("Choose Test Case:");
        // Set label color
        testLbl.setForeground(new Color(200, 150, 255));
        // Add label to left of row
        row1.add(testLbl, BorderLayout.WEST);
        // Set preferred size for combo box
        tests.setPreferredSize(new Dimension(20, 15));
        // Add combo box to center of row
        row1.add(tests, BorderLayout.CENTER);
        // Add row to top panel
        top.add(row1);
        // Add vertical spacing
        top.add(Box.createVerticalStrut(15));

        // Create third row for buttons
        JPanel row3 = new JPanel(new BorderLayout(8, 8));
        // Create panel for buttons with flow layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        // Create "New File" button with purple color
        JButton newFileBtn = createStyledButton("New File", new Color(138, 43, 226)); // Purple
        // Create "Upload" button with purple color
        JButton uploadBtn = createStyledButton("Upload", new Color(138, 43, 226)); // Purple
        // Create "Save" button with purple color
        JButton saveBtn = createStyledButton("Save", new Color(138, 43, 226)); // Purple
        // Create "Delete" button with red color
        deleteBtn = createStyledButton("Delete", new Color(220, 20, 60)); // Red for delete
        // Create "Run" button with purple color
        JButton runBtn = createStyledButton("Run", new Color(138, 43, 226)); // Purple
        // Create "Clear" button with dark purple color
        JButton clearBtn = createStyledButton("Clear", new Color(75, 0, 130)); // Dark purple
        // Set tooltip for new file button
        newFileBtn.setToolTipText("Create a new file");
        // Set tooltip for upload button
        uploadBtn.setToolTipText("Upload a C++ file from your computer");
        // Set tooltip for save button
        saveBtn.setToolTipText("Save current file");
        // Set tooltip for delete button
        deleteBtn.setToolTipText("Delete selected saved file");
        // Disable delete button initially
        deleteBtn.setEnabled(false);
        // Set tooltip for run button
        runBtn.setToolTipText("Run program (F5)");
        // Set tooltip for clear button
        clearBtn.setToolTipText("Clear output");
        // Add new file button to panel
        buttonPanel.add(newFileBtn);
        // Add upload button to panel
        buttonPanel.add(uploadBtn);
        // Add save button to panel
        buttonPanel.add(saveBtn);
        // Add delete button to panel
        buttonPanel.add(deleteBtn);
        // Add run button to panel
        buttonPanel.add(runBtn);
        // Add clear button to panel
        buttonPanel.add(clearBtn);
        // Add button panel to top of row3
        row3.add(buttonPanel, BorderLayout.NORTH);
        // Add input panel to center of row3
        row3.add(inputPanel, BorderLayout.CENTER);
        // Add row3 to top panel
        top.add(row3);
        // We'll place controls and output into a vertical split for better balance

        // Set up output area with styling
        setupOutputArea();
        // Create scroll pane for output area
        JScrollPane outScroll = new JScrollPane(outputArea);
        // Set viewport background color
        outScroll.getViewport().setBackground(new Color(15, 15, 15));
        // Create titled border for output area
        TitledBorder outputTitle = new TitledBorder(new LineBorder(new Color(138, 43, 226), 1), "Program Output",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12), new Color(200, 150, 255));
        // Set border with title and padding
        outScroll.setBorder(new CompoundBorder(outputTitle, new EmptyBorder(6, 6, 6, 6)));
        // Create vertical split pane for controls and output
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, outScroll);
        // Set resize weight (35% for controls, 65% for output)
        rightSplit.setResizeWeight(0.35); // allocate ~35% for controls/input, 65% for output
        // Set divider size
        rightSplit.setDividerSize(8);
        // Set border
        rightSplit.setBorder(new LineBorder(new Color(75, 0, 130), 1));
        // Add right split pane to right panel
        right.add(rightSplit, BorderLayout.CENTER);

        // Add status bar to bottom of frame
        add(createStatusBar(), BorderLayout.SOUTH);

        // Add action listener for test case selection
        tests.addActionListener(e -> {
            // Apply selected test case
            applySelection();
            // Update delete button state
            updateDeleteButtonState();
        });
        // Add action listener for new file button
        newFileBtn.addActionListener(this::createNewFile);
        // Add action listener for upload button
        uploadBtn.addActionListener(this::uploadFile);
        // Add action listener for save button
        saveBtn.addActionListener(this::saveCurrentFile);
        // Add action listener for delete button
        deleteBtn.addActionListener(this::deleteCurrentFile);
        // Add action listener for run button
        runBtn.addActionListener(this::runProgram);
        // Add action listener for clear button - clears output area
        clearBtn.addActionListener(e -> { outputArea.setText(""); updateStatus("Output cleared"); });

        // Show PL1 by default
        applySelection();
        // Update delete button state based on selection
        updateDeleteButtonState();
    }

    // Method to apply dark purple theme to UI components
    private void applyDarkTheme() {
        // Apply purple dark theme using ThemeUtil
        ThemeUtil.applyPurpleDarkTheme();
        // Set background color of content pane
        getContentPane().setBackground(new Color(20, 20, 20));
    }

    // Method to initialize the file dropdown list
    private void initializeFileList() {
        // Remove all existing elements
        fileModel.removeAllElements();
        // Add default PL1 test case
        fileModel.addElement("C++ (PL1)");
        // Add default PL2 test case
        fileModel.addElement("C++ (PL2)");
        // Add default PL3 test case
        fileModel.addElement("C++ (PL3)");
        
        // Load saved files
        // Check if saved files directory exists
        if (savedFilesDir.exists() && savedFilesDir.isDirectory()) {
            // Get all .cpp files in saved directory
            File[] savedFiles = savedFilesDir.listFiles((dir, name) -> name.endsWith(".cpp"));
            // If files exist
            if (savedFiles != null) {
                // Sort files by name
                Arrays.sort(savedFiles, Comparator.comparing(File::getName));
                // Add each file to dropdown
                for (File file : savedFiles) {
                    fileModel.addElement("Saved: " + file.getName());
                }
            }
        }
    }

    // Method to create a new file
    private void createNewFile(ActionEvent e) {
        // Show dialog to get file name from user
        String fileName = JOptionPane.showInputDialog(this, "Enter file name (without extension):", "New File", JOptionPane.PLAIN_MESSAGE);
        // If user entered a name
        if (fileName != null && !fileName.trim().isEmpty()) {
            // Trim whitespace
            fileName = fileName.trim();
            // Add .cpp extension if not present
            if (!fileName.endsWith(".cpp")) {
                fileName += ".cpp";
            }
            
            // Check if file already exists
            File newFile = new File(savedFilesDir, fileName);
            // If file exists
            if (newFile.exists()) {
                // Ask user if they want to overwrite
                int result = JOptionPane.showConfirmDialog(this, 
                    "File already exists. Overwrite?", 
                    "File Exists", 
                    JOptionPane.YES_NO_OPTION);
                // If user doesn't want to overwrite, return
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            // Clear code area and set new file
            codeArea.setText("");
            // Set current file name
            currentFileName = fileName;
            // Update status message
            updateStatus("New file: " + fileName);
            
            // Add to dropdown if not already there
            String displayName = "Saved: " + fileName;
            // Check if display name is not in dropdown
            if (!fileModelContains(displayName)) {
                // Add to dropdown
                fileModel.addElement(displayName);
                // Select the new file
                tests.setSelectedItem(displayName);
            } else {
                // Select existing file
                tests.setSelectedItem(displayName);
            }
            
            // Hide input panel
            inputPanel.setVisible(false);
            // Set output message
            outputArea.setText("New file created. Start coding!");
        }
    }

    // Method to run the lexer on code
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
            // StringBuilder to store output
            StringBuilder output = new StringBuilder();
            // Variable to store each line
            String line;
            // Read all lines from lexer output
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
            // Print stack trace for debugging
            ex.printStackTrace();
            // Return error message
            return "Error: Lexer execution failed.";
        }
    }
    

    // Method to check if file model contains a specific item
    private boolean fileModelContains(String item) {
        // Loop through all elements in file model
        for (int i = 0; i < fileModel.getSize(); i++) {
            // Check if current element equals item
            if (fileModel.getElementAt(i).equals(item)) {
                return true;
            }
        }
        // Item not found
        return false;
    }

    // Method to save the current file
    private void saveCurrentFile(ActionEvent e) {
        // Variable to store file name
        String fileName;
        
        // If no current file name and no uploaded file
        if (currentFileName == null && currentUploadedFile == null) {
            // Prompt for file name
            fileName = JOptionPane.showInputDialog(this, "Enter file name (without extension):", "Save File", JOptionPane.PLAIN_MESSAGE);
            // If user cancelled or entered empty name, return
            if (fileName == null || fileName.trim().isEmpty()) {
                return;
            }
            // Trim whitespace
            fileName = fileName.trim();
            // Add .cpp extension if not present
            if (!fileName.endsWith(".cpp")) {
                fileName += ".cpp";
            }
            // Set current file name
            currentFileName = fileName;
        } else if (currentUploadedFile != null) {
            // If it's an uploaded file, copy it to saved directory
            String uploadedFileName = currentUploadedFile.getName();
            // Use uploaded file name
            fileName = uploadedFileName;
            // Set current file name
            currentFileName = fileName;
            // Clear uploaded file reference
            currentUploadedFile = null; // Clear uploaded file reference
        } else {
            // Use current file name
            fileName = currentFileName;
        }
        
        // Create file object for saving
        File fileToSave = new File(savedFilesDir, fileName);
        // Try to write file
        try (FileWriter writer = new FileWriter(fileToSave)) {
            // Write code area text to file
            writer.write(codeArea.getText());
            // Update status message
            updateStatus("File saved: " + fileName);
            
            // Add to dropdown if not already there
            String displayName = "Saved: " + fileName;
            // Check if not in dropdown
            if (!fileModelContains(displayName)) {
                // Add to dropdown
                fileModel.addElement(displayName);
            }
            // Select the saved file
            tests.setSelectedItem(displayName);
            // Update current file name
            currentFileName = fileName;
            // Clear uploaded file reference
            currentUploadedFile = null;
            // Update delete button state
            updateDeleteButtonState();
            
            // Show success message
            JOptionPane.showMessageDialog(this, "File saved successfully!", "Save", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            // Show error message if save fails
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            // Update status message
            updateStatus("Error saving file");
        }
    }

    // Method to upload a file from user's computer
    private void uploadFile(ActionEvent e) {
        // Create file chooser dialog
        JFileChooser fileChooser = new JFileChooser();
        // Set dialog title
        fileChooser.setDialogTitle("Select C++ File to Upload");
        // Set file filter for C++ files
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            // Accept directories and C++ files
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".cpp") || 
                       f.getName().toLowerCase().endsWith(".c") || 
                       f.getName().toLowerCase().endsWith(".cc");
            }
            
            // Description for file filter
            @Override
            public String getDescription() {
                return "C++ Files (*.cpp, *.c, *.cc)";
            }
        });
        
        // Apply dark theme to file chooser
        applyFileChooserTheme(fileChooser);
        
        // Show open dialog and get result
        int result = fileChooser.showOpenDialog(this);
        // If user approved file selection
        if (result == JFileChooser.APPROVE_OPTION) {
            // Get selected file
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // StringBuilder to store file content
                StringBuilder content = new StringBuilder();
                // Read file content
                try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                    // Variable to store each line
                    String line;
                    // Read all lines
                    while ((line = br.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                
                // Set code area with file content
                codeArea.setText(content.toString());
                // Set current uploaded file
                currentUploadedFile = selectedFile;
                // Clear saved file name
                currentFileName = null; // Clear saved file name
                
                // Ask if user wants to save it to the saved directory
                int saveOption = JOptionPane.showConfirmDialog(this,
                    "File loaded successfully!\n\nDo you want to add it to your saved files?",
                    "File Uploaded",
                    JOptionPane.YES_NO_OPTION);
                
                // If user wants to save
                if (saveOption == JOptionPane.YES_OPTION) {
                    // Get file name
                    String fileName = selectedFile.getName();
                    // Create file object for saving
                    File fileToSave = new File(savedFilesDir, fileName);
                    
                    // Check if file already exists in saved directory
                    if (fileToSave.exists()) {
                        // Ask if user wants to overwrite
                        int overwrite = JOptionPane.showConfirmDialog(this,
                            "File already exists in saved files. Overwrite?",
                            "File Exists",
                            JOptionPane.YES_NO_OPTION);
                        // If user doesn't want to overwrite
                        if (overwrite != JOptionPane.YES_OPTION) {
                            // Don't save, but keep the file loaded
                            outputArea.setText("File loaded: " + fileName + "\nYou can edit and run it, or save it with a different name.");
                            // Update status
                            updateStatus("File loaded: " + fileName);
                            // Update delete button state
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
                    // Check if not in dropdown
                    if (!fileModelContains(displayName)) {
                        // Add to dropdown
                        fileModel.addElement(displayName);
                    }
                    // Select the saved file
                    tests.setSelectedItem(displayName);
                    // Set current file name
                    currentFileName = fileName;
                    // Clear uploaded file reference
                    currentUploadedFile = null;
                    
                    // Show success message
                    JOptionPane.showMessageDialog(this, "File saved and added to your files!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    // Update status
                    updateStatus("File uploaded and saved: " + fileName);
                } else {
                    // File loaded but not saved
                    outputArea.setText("File loaded: " + selectedFile.getName() + "\nYou can edit and run it. Use Save to add it to your saved files.");
                    // Update status
                    updateStatus("File loaded: " + selectedFile.getName());
                }
                
                // Hide input panel
                inputPanel.setVisible(false);
                // Update delete button state
                updateDeleteButtonState();
            } catch (IOException ex) {
                // Show error message if file read fails
                JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                // Update status
                updateStatus("Error loading file");
            }
        }
    }

    // Method to delete the current file
    private void deleteCurrentFile(ActionEvent e) {
        // Get selected item from dropdown
        String selected = (String) tests.getSelectedItem();
        // If no selection or not a saved file, return
        if (selected == null || !selected.startsWith("Saved: ")) {
            return;
        }
        
        // Extract file name from display name
        String fileName = selected.substring(7); // Remove "Saved: " prefix
        // Create file object for deletion
        File fileToDelete = new File(savedFilesDir, fileName);
        
        // If file doesn't exist
        if (!fileToDelete.exists()) {
            // Show error message
            JOptionPane.showMessageDialog(this, "File not found: " + fileName, "Error", JOptionPane.ERROR_MESSAGE);
            // Remove from dropdown if file doesn't exist
            fileModel.removeElement(selected);
            // Update delete button state
            updateDeleteButtonState();
            return;
        }
        
        // Ask for confirmation before deleting
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete:\n" + fileName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        // If user confirmed deletion
        if (confirm == JOptionPane.YES_OPTION) {
            // Try to delete file
            if (fileToDelete.delete()) {
                // Remove from dropdown
                fileModel.removeElement(selected);
                // Update status
                updateStatus("File deleted: " + fileName);
                
                // If this was the current file, clear the editor
                if (fileName.equals(currentFileName)) {
                    // Clear code area
                    codeArea.setText("");
                    // Clear current file name
                    currentFileName = null;
                    // Set output message
                    outputArea.setText("File deleted. Select another file or create a new one.");
                }
                
                // Select first item in dropdown
                if (fileModel.getSize() > 0) {
                    // Select first item
                    tests.setSelectedIndex(0);
                    // Apply selection
                    applySelection();
                }
                
                // Update delete button state
                updateDeleteButtonState();
                // Show success message
                JOptionPane.showMessageDialog(this, "File deleted successfully!", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // Show error message if deletion fails
                JOptionPane.showMessageDialog(this, "Failed to delete file: " + fileName, "Error", JOptionPane.ERROR_MESSAGE);
                // Update status
                updateStatus("Error deleting file");
            }
        }
    }

    // Method to enable/disable delete button based on selection
    private void updateDeleteButtonState() {
        // Get selected item
        String selected = (String) tests.getSelectedItem();
        // Enable delete button only if a saved file is selected
        deleteBtn.setEnabled(selected != null && selected.startsWith("Saved: "));
    }

    // Method to apply dark theme to file chooser
    private void applyFileChooserTheme(JFileChooser fileChooser) {
        // Try to apply dark theme to file chooser components
        try {
            // Set background color
            UIManager.put("FileChooser.background", new Color(25, 25, 25));
            // Set foreground color
            UIManager.put("FileChooser.foreground", new Color(220, 220, 220));
            // Update component UI
            SwingUtilities.updateComponentTreeUI(fileChooser);
        } catch (Exception ex) {
            // Ignore if theme application fails
        }
    }

    // Method to set up code editor styling and behavior
    private void setupCodeEditor() {
        // Set font for code area
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        // Set background color
        codeArea.setBackground(new Color(15, 15, 15));
        // Set text color
        codeArea.setForeground(new Color(220, 220, 220));
        // Set cursor color
        codeArea.setCaretColor(new Color(200, 150, 255));
        // Set selection color
        codeArea.setSelectionColor(new Color(138, 43, 226));
        // Set border with padding
        codeArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        // Disable line wrapping
        codeArea.setLineWrap(false);
        // Set tab size to 4 spaces
        codeArea.setTabSize(4);

        // Set input panel background
        inputPanel.setBackground(new Color(20, 20, 20));
        // Set input panel border
        inputPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        // Create label for input field
        JLabel inputLbl = new JLabel("Input: ");
        // Set label color
        inputLbl.setForeground(new Color(200, 150, 255));
        // Add label to left of input panel
        inputPanel.add(inputLbl, BorderLayout.WEST);
        // Set input field character limit (allow all input)
        inputField.setDocument(new JTextFieldLimit(20, false)); // allow all input
        // Set input field background
        inputField.setBackground(new Color(25, 25, 25));
        // Set input field text color
        inputField.setForeground(new Color(230, 230, 230));
        // Set input field cursor color
        inputField.setCaretColor(new Color(200, 150, 255));
        // Set input field border
        inputField.setBorder(new CompoundBorder(new LineBorder(new Color(138, 43, 226), 1), new EmptyBorder(4, 6, 4, 6)));
        // Add input field to center of input panel
        inputPanel.add(inputField, BorderLayout.CENTER);
        // Hide input panel initially
        inputPanel.setVisible(false);

        // Map F5 key to "run" action
        codeArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "run");
        // Define "run" action to execute runProgram method
        codeArea.getActionMap().put("run", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { runProgram(e); }
        });
        // Map Ctrl+A to "selectAll" action
        codeArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), "selectAll");
        // Define "selectAll" action to select all text
        codeArea.getActionMap().put("selectAll", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { codeArea.selectAll(); }
        });
    }

    // Method to set up output area styling
    private void setupOutputArea() {
        // Make output area non-editable
        outputArea.setEditable(false);
        // Set font for output area
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        // Set background color
        outputArea.setBackground(new Color(15, 15, 15));
        // Set text color
        outputArea.setForeground(new Color(220, 220, 220));
        // Set cursor color
        outputArea.setCaretColor(new Color(200, 150, 255));
        // Set border with padding
        outputArea.setBorder(new EmptyBorder(6, 6, 6, 6));
    }

    // Method to create a styled button with custom colors
    private JButton createStyledButton(String text, Color color) {
        // Create button with text
        JButton b = new JButton(text);
        // Set button font
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        // Set button background color
        b.setBackground(color);
        // Set button text color
        b.setForeground(Color.WHITE);
        // Disable focus painting
        b.setFocusPainted(false);
        // Set button border with darker edge and padding
        b.setBorder(new CompoundBorder(new LineBorder(color.darker(), 1, true), new EmptyBorder(6, 12, 6, 12)));
        // Make button opaque
        b.setOpaque(true);
        // Set preferred button size
        b.setPreferredSize(new Dimension(96, 32));
        // Add mouse listener for hover effects
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            // Darken button when mouse enters
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(color.darker()); }
            // Restore original color when mouse exits
            public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(color); }
        });
        // Return styled button
        return b;
    }

    // Method to create menu bar (currently removes it)
    private void createMenuBar() {
        // Remove menu bar as requested
        setJMenuBar(null);
    }

    // Method to create toolbar at top of window
    private void createToolbar() {
        // Create toolbar
        JToolBar tb = new JToolBar();
        // Make toolbar non-floatable
        tb.setFloatable(false);
        // Set toolbar border
        tb.setBorder(new CompoundBorder(new LineBorder(new Color(75, 0, 130), 1), new EmptyBorder(2, 6, 2, 6)));
        // Set toolbar background color
        tb.setBackground(new Color(20, 20, 20));
        // Create title label
        JLabel title = new JLabel("C++ Mini Compiler");
        // Set title color
        title.setForeground(new Color(200, 150, 255));
        // Set title font
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        // Set toolbar layout
        tb.setLayout(new BorderLayout());
        // Add title to left side of toolbar
        tb.add(title, BorderLayout.WEST);
        // Add toolbar to top of frame
        add(tb, BorderLayout.NORTH);
    }

    // Method to create status bar at bottom of window
    private JPanel createStatusBar() {
        // Create panel for status bar
        JPanel sb = new JPanel(new BorderLayout());
        // Set status bar border with padding
        sb.setBorder(new CompoundBorder(new EmptyBorder(6, 12, 6, 12), new LineBorder(new Color(75, 0, 130), 1)));
        // Set status bar background color
        sb.setBackground(new Color(20, 20, 20));
        // Ensure minimum height for larger text
        sb.setMinimumSize(new Dimension(0, 36)); // Ensure minimum height for larger text
        // Set progress bar preferred size
        progressBar.setPreferredSize(new Dimension(220, 28));
        // Set progress bar minimum size
        progressBar.setMinimumSize(new Dimension(220, 28));
        // Show text on progress bar
        progressBar.setStringPainted(true);
        // Set initial progress bar text
        progressBar.setString("Ready");
        // Set progress bar foreground color
        progressBar.setForeground(new Color(138, 43, 226));
        // Set progress bar background color
        progressBar.setBackground(new Color(30, 30, 30));
        // Set progress bar font (larger)
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Larger font
        // Set status label text color
        statusLabel.setForeground(new Color(200, 150, 255));
        // Set status label font (14pt)
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14)); // Larger font for status text (14pt)
        // Add right padding to status label
        statusLabel.setBorder(new EmptyBorder(0, 0, 0, 15)); // Add right padding
        // Add status label to left of status bar
        sb.add(statusLabel, BorderLayout.WEST);
        // Add progress bar to right of status bar
        sb.add(progressBar, BorderLayout.EAST);
        // Return status bar panel
        return sb;
    }

    // Method to update status message and progress bar text
    private void updateStatus(String msg) {
        // Run on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> { statusLabel.setText(msg); progressBar.setString(msg); });
    }

    // Method to show "About" dialog
    private void showAbout(ActionEvent e) {
        // Display information dialog
        JOptionPane.showMessageDialog(this, "Mini Compiler IDE v2.0\n\nBuilt-in evaluator for PL1/PL2/PL3.",
                "About Mini Compiler IDE", JOptionPane.INFORMATION_MESSAGE);
    }

    // Method to show keyboard shortcuts dialog
    private void showShortcuts(ActionEvent e) {
        // Display shortcuts information
        JOptionPane.showMessageDialog(this, "F5 - Run Program\nCtrl+A - Select All",
                "Keyboard Shortcuts", JOptionPane.INFORMATION_MESSAGE);
    }

    // Simple file loader method
    private String loadFileContent(String filename) {
        try {
            // Variable to store file object
            File file;
            // Check if it's a saved file
            if (filename.startsWith("Saved: ")) {
                // Remove "Saved: " prefix to get actual file name
                String actualFileName = filename.substring(7); // Remove "Saved: " prefix
                // Create file object in saved directory
                file = new File(savedFilesDir, actualFileName);
            } else {
                // Create file object with given filename
                file = new File(filename);
            }
            
            // If file doesn't exist
            if (!file.exists()) {
                // Get default content based on filename
                String defaultContent = switch (filename) {
                    case "PL1.cpp" -> "cin >> x;\ncout << x;";
                    case "PL2.cpp" -> "x = 3;\ny = 7;\ncout<< x + y;";
                    case "PL3.cpp" -> "cout<< \"Hello, World!\";";
                    default -> "";
                };
                // If not a saved file, create it with default content
                if (!filename.startsWith("Saved: ")) {
                    try (FileWriter w = new FileWriter(file)) { w.write(defaultContent); }
                }
                // Return default content
                return defaultContent;
            }
            // StringBuilder to accumulate file content
            StringBuilder sb = new StringBuilder();
            // Read file line by line
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line; while ((line = br.readLine()) != null) sb.append(line).append('\n');
            }
            // Return file content
            return sb.toString();
        } catch (IOException e) {
            // Return error message if file can't be loaded
            return "// Error: Could not load file.";
        }
    }

    // Selection logic for test cases
    private void applySelection() {
        // Get selected item from dropdown
        String selected = (String) tests.getSelectedItem();
        // If nothing selected, return
        if (selected == null) return;
        
        // If PL1 is selected
        if (selected.equals("C++ (PL1)")) {
            // Load PL1 content
            codeArea.setText(loadFileContent("PL1.cpp"));
            // Set output message
            outputArea.setText("PL1: Enter input and click Run");
            // Show input panel for user input
            inputPanel.setVisible(true);
            // Enable input field
            inputField.setEnabled(true);
            // Focus on input field
            inputField.requestFocusInWindow();
            // Update status
            updateStatus("PL1 ready");
            // Clear current file name
            currentFileName = null;
            // Clear uploaded file reference
            currentUploadedFile = null;
        } else if (selected.equals("C++ (PL2)")) {
            // Load PL2 content
            codeArea.setText(loadFileContent("PL2.cpp"));
            // Set output message
            outputArea.setText("PL2: Click Run");
            // Hide input panel (no input needed)
            inputPanel.setVisible(false);
            // Update status
            updateStatus("PL2 ready");
            // Clear current file name
            currentFileName = null;
            // Clear uploaded file reference
            currentUploadedFile = null;
        } else if (selected.equals("C++ (PL3)")) {
            // Load PL3 content
            codeArea.setText(loadFileContent("PL3.cpp"));
            // Set output message
            outputArea.setText("PL3: Click Run");
            // Hide input panel (no input needed)
            inputPanel.setVisible(false);
            // Update status
            updateStatus("PL3 ready");
            // Clear current file name
            currentFileName = null;
            // Clear uploaded file reference
            currentUploadedFile = null;
        } else if (selected.startsWith("Saved: ")) {
            // Extract file name from display name
            String fileName = selected.substring(7); // Remove "Saved: " prefix
            // Load saved file content
            codeArea.setText(loadFileContent(selected));
            // Set output message
            outputArea.setText("Loaded: " + fileName + " - Click Run to execute");
            // Hide input panel
            inputPanel.setVisible(false);
            // Set current file name
            currentFileName = fileName;
            // Clear uploaded file reference
            currentUploadedFile = null;
            // Update status
            updateStatus("Loaded: " + fileName);
        }
    }

    // --- Simple evaluator for PL1/PL2/PL3 ---
    private String interpretMini(String code, int mode) throws Exception {
        // Remove comments and normalize whitespace
        String normalized = code.replaceAll("//.*", "").replace("\r", "").trim();
        // If mode is 1 (PL1 - input/output)
        if (mode == 1) {
            // Get input from input field
            String in = inputField.getText().trim();
            // Return input or error message
            return in.isEmpty() ? "No input provided" : in;
        }

        // Map to store variables
        Map<String, Integer> vars = new HashMap<>();
        // Pattern to match variable assignments
        Pattern assign = Pattern.compile("\\b([a-zA-Z_]\\w*)\\s*=\\s*(-?\\d+)\\s*;");
        // Pattern to match cout with numbers or variables
        Pattern coutNum = Pattern.compile("cout\\s*<<\\s*([a-zA-Z_]\\w*|\\d+)\\s*(?:\\+\\s*([a-zA-Z_]\\w*|\\d+))?\\s*;?");
        // Pattern to match cout with strings
        Pattern coutStr = Pattern.compile("cout\\s*<<\\s*\"(.*?)\"\\s*;?");
        // Split code into lines
        String[] lines = normalized.split("\\n+");
        // Variable to store last output
        String lastOutput = null;

        // Process each line
        for (String raw : lines) {
            // Trim whitespace
            String line = raw.trim();
            // Skip empty lines
            if (line.isEmpty()) continue;

            // Try to match assignment pattern
            Matcher mAssign = assign.matcher(line);
            // Try to match string cout pattern
            Matcher mCoutStr = coutStr.matcher(line);
            // Try to match number cout pattern
            Matcher mCoutNum = coutNum.matcher(line);

            // If line is an assignment
            if (mAssign.matches()) {
                // Store variable and its value
                vars.put(mAssign.group(1), Integer.parseInt(mAssign.group(2)));
            } else if (mCoutStr.matches()) {
                // If line is cout with string, store string as output
                lastOutput = mCoutStr.group(1);
            } else if (mCoutNum.matches()) {
                // If line is cout with number/variable
                // Resolve first value
                int a = resolveValue(mCoutNum.group(1), vars);
                // Get second token (for addition)
                String bTok = mCoutNum.group(2);
                // Calculate final value (with or without addition)
                int val = (bTok == null) ? a : a + resolveValue(bTok, vars);
                // Store result as output
                lastOutput = String.valueOf(val);
            }
        }
        // If no output was generated, throw exception
        if (lastOutput == null) throw new Exception("No output from program");
        // Return last output
        return lastOutput;
    }

    // Method to resolve variable or literal value
    private int resolveValue(String token, Map<String, Integer> vars) throws Exception {
        // If token is null, return 0
        if (token == null) return 0;
        // Trim whitespace
        token = token.trim();
        // If token is a number, parse and return it
        if (token.matches("-?\\d+")) return Integer.parseInt(token);
        // Try to get variable value
        Integer v = vars.get(token);
        // If variable not found, throw exception
        if (v == null) throw new Exception("Undefined variable: " + token);
        // Return variable value
        return v;
    }

    // Method to run the program
    private void runProgram(ActionEvent e) {
        // Prevent running if already running
        if (isRunning) return; // Prevent running if already running
        // Set running flag
        isRunning = true;
        // Show indeterminate progress
        progressBar.setIndeterminate(true);
        // Update status message
        updateStatus("Running program...");
    
        // Get code from code area
        final String code = codeArea.getText().trim();
        // If code is empty
        if (code.isEmpty()) {
            // Display error message
            outputArea.setText("Error: No code to execute");
            // Update status
            updateStatus("Error: No code to execute");
            // Reset running flag
            isRunning = false;
            // Stop progress bar animation
            progressBar.setIndeterminate(false);
            return;
        }
    
        // Step 1: Pass code to lexer
        String lexerOutput = runLexer(code);
        // Check if lexer returned an error
        if (lexerOutput.contains("Error")) {
            // Display lexer error
            outputArea.setText(lexerOutput);
            // Update status
            updateStatus("Lexer error");
            // Reset running flag
            isRunning = false;
            // Stop progress bar animation
            progressBar.setIndeterminate(false);
            return;
        }
    
        // Step 2: Pass lexer output (tokens) to parser
        String parserOutput = runParser(lexerOutput);
        // Check if parser returned an error
        if (parserOutput.contains("Error")) {
            // Display parser error
            outputArea.setText(parserOutput);
            // Update status
            updateStatus("Parser error");
            // Reset running flag
            isRunning = false;
            // Stop progress bar animation
            progressBar.setIndeterminate(false);
            return;
        }
    
        // Step 3: Display the result from the parser
        outputArea.setText("Program Output:\n" + parserOutput);
        // Scroll to end of output
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
        // Update status with success message
        updateStatus("Program executed successfully");
        // Reset running flag
        isRunning = false;
        // Stop progress bar animation
        progressBar.setIndeterminate(false);
    }

    // Method to run parser on lexer output
    private String runParser(String lexerOutput) {
        // Step 1: Assuming lexerOutput is a string of tokens
        // Step 2: You may need to process these tokens and pass them to a parser
        // If the lexer output is a file, you can write it to a temp file just like in runLexer()
    
        // Example: Parsing the tokens (simplified)
        try {
            // Execute parser with lexer output as argument
            Process parserProcess = Runtime.getRuntime().exec("./parser " + lexerOutput);
            // Create reader for parser output
            BufferedReader reader = new BufferedReader(new InputStreamReader(parserProcess.getInputStream()));
            // StringBuilder to accumulate output
            StringBuilder output = new StringBuilder();
            // Variable to store each line
            String line;
            // Read all lines from parser output
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            // Wait for parser process to complete
            parserProcess.waitFor();
            // Return parser output
            return output.toString();
        } catch (Exception ex) {
            // Print stack trace for debugging
            ex.printStackTrace();
            // Return error message
            return "Error: Parser execution failed.";
        }
    }
    
    
    // Method to run both lexer and parser (alternative approach)
    private String runLexerAndParser(String code) {
        try {
            // Write the code to a temporary file
            File tempFile = File.createTempFile("input", ".cpp");
            // Write code to temp file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(code);  // Write the code entered by the user in the codeArea
            }
    
            // Run the lexer and parser on the temp file (assuming your lexer/parser are compiled as executables)
            Process process = Runtime.getRuntime().exec("./lexer_parser " + tempFile.getAbsolutePath());
    
            // Read the output from the lexer and parser
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            // StringBuilder to accumulate output
            StringBuilder output = new StringBuilder();
            // Variable to store each line
            String line;
            // Read all output lines
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
    
            // Wait for the process to finish
            process.waitFor();
    
            // Return combined lexer/parser output
            return output.toString();
        } catch (Exception ex) {
            // Print stack trace for debugging
            ex.printStackTrace();
            // Return error message
            return "Error: Could not run lexer/parser.";
        }
    }
        
    // Main method to launch the application
    public static void main(String[] args) {
        // Run on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new MiniCompilerGUI().setVisible(true));
    }
}

// --- Theming helpers ---
class ThemeUtil {
    // Method to apply purple dark theme to UI
    static void applyPurpleDarkTheme() {
        // Core Nimbus palette overrides with purple theme
        // Set control background color
        UIManager.put("control", new Color(25, 25, 25));
        // Set info background color
        UIManager.put("info", new Color(30, 30, 30));
        // Set base color
        UIManager.put("nimbusBase", new Color(10, 10, 10));
        // Set blue-grey color
        UIManager.put("nimbusBlueGrey", new Color(50, 50, 50));
        // Set light background color
        UIManager.put("nimbusLightBackground", new Color(20, 20, 20));
        // Set text color
        UIManager.put("text", new Color(220, 220, 220));
        // Set selection background color
        UIManager.put("nimbusSelectionBackground", new Color(138, 43, 226));
        // Set focus color
        UIManager.put("nimbusFocus", new Color(200, 150, 255));

        // General components
        // Set menu background color
        UIManager.put("Menu.background", new Color(20, 20, 20));
        // Set menu item background color
        UIManager.put("MenuItem.background", new Color(20, 20, 20));
        // Set menu foreground color
        UIManager.put("Menu.foreground", new Color(200, 150, 255));
        // Set menu item foreground color
        UIManager.put("MenuItem.foreground", new Color(200, 150, 255));
        // Set panel background color
        UIManager.put("Panel.background", new Color(20, 20, 20));
        // Set scroll pane background color
        UIManager.put("ScrollPane.background", new Color(20, 20, 20));
        // Set toolbar background color
        UIManager.put("ToolBar.background", new Color(20, 20, 20));
        // Set table background color
        UIManager.put("Table.background", new Color(25, 25, 25));
        // Set table foreground color
        UIManager.put("Table.foreground", new Color(220, 220, 220));
        // Set label foreground color
        UIManager.put("Label.foreground", new Color(200, 150, 255));
        // Set combo box background color
        UIManager.put("ComboBox.background", new Color(25, 25, 25));
        // Set combo box foreground color
        UIManager.put("ComboBox.foreground", new Color(220, 220, 220));
        // Set text field background color
        UIManager.put("TextField.background", new Color(25, 25, 25));
        // Set text field foreground color
        UIManager.put("TextField.foreground", new Color(220, 220, 220));

        // Font: apply a saner default for a professional look
        // Create base font
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
        // Loop through all UI defaults
        for (Object key : UIManager.getDefaults().keySet()) {
            // Get value for key
            Object val = UIManager.get(key);
            // If value is a Font
            if (val instanceof Font) {
                // Set font to base font
                UIManager.put(key, base);
            }
        }
    }
}

// --- Line number gutter for code editor ---
class LineNumberView extends JComponent {
    // Margin for line numbers
    private static final int MARGIN = 6;
    // Reference to text area
    private final JTextArea textArea;
    // Font for line numbers
    private final Font numberFont = new Font("Segoe UI", Font.PLAIN, 11);

    // Constructor to initialize line number view
    LineNumberView(JTextArea textArea) {
        // Store text area reference
        this.textArea = textArea;
        // Set foreground color for line numbers
        setForeground(new Color(150, 100, 200));
        // Set background color for gutter
        setBackground(new Color(10, 10, 10));
        // Make component opaque
        setOpaque(true);

        // Add document listener to repaint when text changes
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            // Repaint when text is inserted
            public void insertUpdate(javax.swing.event.DocumentEvent e) { repaint(); revalidate(); }
            // Repaint when text is removed
            public void removeUpdate(javax.swing.event.DocumentEvent e) { repaint(); revalidate(); }
            // Repaint when text is changed
            public void changedUpdate(javax.swing.event.DocumentEvent e) { repaint(); revalidate(); }
        });
        // Add caret listener to repaint when cursor moves
        textArea.addCaretListener(e -> repaint());
    }

    // Override to calculate preferred size based on line count
    @Override
    public Dimension getPreferredSize() {
        // Get line count (at least 1)
        int lineCount = Math.max(1, textArea.getLineCount());
        // Calculate number of digits needed
        int digits = String.valueOf(lineCount).length();
        // Get font metrics
        FontMetrics fm = getFontMetrics(numberFont);
        // Calculate width based on digits and margin
        int width = MARGIN * 2 + fm.charWidth('0') * digits;
        // Return preferred dimension
        return new Dimension(width, textArea.getHeight());
    }

    // Override to paint line numbers
    @Override
    protected void paintComponent(Graphics g) {
        // Call superclass paint method
        super.paintComponent(g);
        // Get clip bounds
        Rectangle clip = g.getClipBounds();
        // Set background color
        g.setColor(getBackground());
        // Fill background
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        // Set font for line numbers
        g.setFont(numberFont);
        // Set text color
        g.setColor(getForeground());

        // Get start offset for visible area
        int startOffset = textArea.viewToModel(new Point(0, clip.y));
        // Get end offset for visible area
        int endOffset = textArea.viewToModel(new Point(0, clip.y + clip.height));

        // Loop through visible lines
        while (startOffset <= endOffset) {
            try {
                // Get line number for offset
                int line = textArea.getLineOfOffset(startOffset);
                // Get rectangle for line
                Rectangle r = textArea.modelToView(startOffset);
                // If rectangle is null, break
                if (r == null) break;
                // Convert line number to string (1-based)
                String lineNumber = String.valueOf(line + 1);
                // Calculate x position (right-aligned)
                int x = getPreferredSize().width - MARGIN - g.getFontMetrics().stringWidth(lineNumber);
                // Calculate y position (baseline)
                int y = r.y + r.height - g.getFontMetrics().getDescent();
                // Draw line number
                g.drawString(lineNumber, x, y);
                // Move to next line
                startOffset = textArea.getLineEndOffset(line) + 1;
            } catch (Exception ex) {
                // Break on any exception
                break;
            }
        }

        // Right separator line
        // Set separator color
        g.setColor(new Color(138, 43, 226));
        // Draw vertical line on right edge
        g.drawLine(getWidth() - 1, clip.y, getWidth() - 1, clip.y + clip.height);
    }
}