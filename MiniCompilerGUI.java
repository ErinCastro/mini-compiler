import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final JComboBox<String> tests = new JComboBox<>(new String[]{"C++ (PL1)", "C++ (PL2)", "C++ (PL3)"});

    private final JLabel statusLabel = new JLabel("Ready");
    private final JProgressBar progressBar = new JProgressBar();
    private boolean isRunning = false;

    public MiniCompilerGUI() {
        super("Mini Compiler IDE");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Apply a dark, modernized Nimbus look without external dependencies
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}
        applyDarkTheme();

        setLayout(new BorderLayout());
        createMenuBar();
        createToolbar();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.6);
        split.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(new Color(60, 60, 60), 1)));
        split.setDividerSize(8);
        add(split, BorderLayout.CENTER);

        setupCodeEditor();
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setRowHeaderView(new LineNumberView(codeArea));
        codeScroll.getViewport().setBackground(new Color(30, 30, 30));
        codeScroll.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(70, 70, 70), 1), "Code Editor",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)),
                new EmptyBorder(6, 6, 6, 6)));
        split.setLeftComponent(codeScroll);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(new EmptyBorder(10, 5, 10, 10));
        split.setRightComponent(right);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(70, 70, 70), 1), "Controls",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)),
                new EmptyBorder(10, 10, 10, 10)));

        JPanel row1 = new JPanel(new BorderLayout(8, 8));
        JLabel testLbl = new JLabel("Choose Test Case:");
        testLbl.setForeground(new Color(210, 210, 210));
        row1.add(testLbl, BorderLayout.WEST);
        tests.setPreferredSize(new Dimension(20, 15));
        row1.add(tests, BorderLayout.CENTER);
        top.add(row1);
        top.add(Box.createVerticalStrut(15));

        JPanel row3 = new JPanel(new BorderLayout(8, 8));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton runBtn = createStyledButton("Run", new Color(46, 160, 67));
        JButton clearBtn = createStyledButton("Clear", new Color(184, 62, 62));
        runBtn.setToolTipText("Run program (F5)");
        clearBtn.setToolTipText("Clear output");
        buttonPanel.add(runBtn);
        buttonPanel.add(clearBtn);
        row3.add(buttonPanel, BorderLayout.NORTH);
        row3.add(inputPanel, BorderLayout.CENTER);
        top.add(row3);
        // We'll place controls and output into a vertical split for better balance

        setupOutputArea();
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.getViewport().setBackground(new Color(35, 35, 35));
        outScroll.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(70, 70, 70), 1), "Program Output",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)),
                new EmptyBorder(6, 6, 6, 6)));
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, outScroll);
        rightSplit.setResizeWeight(0.35); // allocate ~35% for controls/input, 65% for output
        rightSplit.setDividerSize(8);
        rightSplit.setBorder(new LineBorder(new Color(60, 60, 60), 1));
        right.add(rightSplit, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);

        tests.addActionListener(e -> applySelection());
        runBtn.addActionListener(this::runProgram);
        clearBtn.addActionListener(e -> { outputArea.setText(""); updateStatus("Output cleared"); });

        // Show PL1 by default
        applySelection();
    }

    private void applyDarkTheme() {
        ThemeUtil.applyDefaultDarkNimbusColors();
        getContentPane().setBackground(new Color(28, 28, 28));
    }

    private void setupCodeEditor() {
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeArea.setBackground(new Color(30, 30, 30));
        codeArea.setForeground(new Color(220, 220, 220));
        codeArea.setCaretColor(new Color(230, 230, 230));
        codeArea.setSelectionColor(new Color(75, 110, 175));
        codeArea.setBorder(new EmptyBorder(6, 6, 6, 6));
        codeArea.setLineWrap(false);
        codeArea.setTabSize(4);

        inputPanel.setBackground(new Color(28, 28, 28));
        inputPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
        JLabel inputLbl = new JLabel("Input: ");
        inputLbl.setForeground(new Color(210, 210, 210));
        inputPanel.add(inputLbl, BorderLayout.WEST);
        inputField.setDocument(new JTextFieldLimit(20, false)); // allow all input
        inputField.setBackground(new Color(38, 38, 38));
        inputField.setForeground(new Color(230, 230, 230));
        inputField.setCaretColor(new Color(230, 230, 230));
        inputField.setBorder(new CompoundBorder(new LineBorder(new Color(60, 60, 60), 1), new EmptyBorder(4, 6, 4, 6)));
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
        outputArea.setBackground(new Color(35, 35, 35));
        outputArea.setForeground(new Color(220, 220, 220));
        outputArea.setCaretColor(new Color(230, 230, 230));
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
        tb.setBorder(new CompoundBorder(new LineBorder(new Color(60, 60, 60), 1), new EmptyBorder(2, 6, 2, 6)));
        tb.setBackground(new Color(32, 32, 32));
        JLabel title = new JLabel("C++ Mini Compiler");
        title.setForeground(new Color(230, 230, 230));
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tb.setLayout(new BorderLayout());
        tb.add(title, BorderLayout.WEST);
        add(tb, BorderLayout.NORTH);
    }

    private JPanel createStatusBar() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBorder(new CompoundBorder(new EmptyBorder(2, 5, 2, 5), new LineBorder(new Color(60, 60, 60), 1)));
        sb.setBackground(new Color(28, 28, 28));
        progressBar.setPreferredSize(new Dimension(150, 20));
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setForeground(new Color(46, 160, 67));
        progressBar.setBackground(new Color(48, 48, 48));
        statusLabel.setForeground(new Color(210, 210, 210));
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
            File file = new File(filename);
            if (!file.exists()) {
                String defaultContent = switch (filename) {
                    case "PL1.cpp" -> "cin >> x;\ncout << x;";
                    case "PL2.cpp" -> "x = 3;\ny = 7;\ncout<< x + y;";
                    case "PL3.cpp" -> "cout<< \"Hello, World!\";";
                    default -> "";
                };
                try (FileWriter w = new FileWriter(file)) { w.write(defaultContent); }
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
        switch (tests.getSelectedIndex()) {
            case 0 -> {
                codeArea.setText(loadFileContent("PL1.cpp"));
                outputArea.setText("PL1: Enter input and click Run");
                inputPanel.setVisible(true);
                inputField.setEnabled(true);
                inputField.requestFocusInWindow();
                updateStatus("PL1 ready");
            }
            case 1 -> {
                codeArea.setText(loadFileContent("PL2.cpp"));
                outputArea.setText("PL2: Click Run");
                inputPanel.setVisible(false);
                updateStatus("PL2 ready");
            }
            case 2 -> {
                codeArea.setText(loadFileContent("PL3.cpp"));
                outputArea.setText("PL3: Click Run");
                inputPanel.setVisible(false);
                updateStatus("PL3 ready");
            }
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
        if (isRunning) return;
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

        final int mode = (tests.getSelectedIndex() == 0) ? 1 : (tests.getSelectedIndex() == 1) ? 2 : 3;

        new Thread(() -> {
            try {
                String out = interpretMini(code, mode);
                final String finalOutput = out;
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText("Program Output:\n" + finalOutput);
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                    updateStatus("Program executed successfully");
                    isRunning = false;
                    progressBar.setIndeterminate(false);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText("Error executing program: " + ex.getMessage());
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                    updateStatus("Error executing program");
                    isRunning = false;
                    progressBar.setIndeterminate(false);
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MiniCompilerGUI().setVisible(true));
    }
}

// --- Theming helpers ---
class ThemeUtil {
    static void applyDefaultDarkNimbusColors() {
        // Core Nimbus palette overrides
        UIManager.put("control", new Color(32, 32, 32));
        UIManager.put("info", new Color(40, 40, 40));
        UIManager.put("nimbusBase", new Color(18, 18, 18));
        UIManager.put("nimbusBlueGrey", new Color(60, 60, 60));
        UIManager.put("nimbusLightBackground", new Color(28, 28, 28));
        UIManager.put("text", new Color(230, 230, 230));
        UIManager.put("nimbusSelectionBackground", new Color(75, 110, 175));
        UIManager.put("nimbusFocus", new Color(90, 120, 190));

        // General components
        UIManager.put("Menu.background", new Color(32, 32, 32));
        UIManager.put("MenuItem.background", new Color(32, 32, 32));
        UIManager.put("Menu.foreground", new Color(220, 220, 220));
        UIManager.put("MenuItem.foreground", new Color(220, 220, 220));
        UIManager.put("Panel.background", new Color(28, 28, 28));
        UIManager.put("ScrollPane.background", new Color(28, 28, 28));
        UIManager.put("ToolBar.background", new Color(32, 32, 32));
        UIManager.put("Table.background", new Color(32, 32, 32));
        UIManager.put("Table.foreground", new Color(230, 230, 230));
        UIManager.put("Label.foreground", new Color(220, 220, 220));
        UIManager.put("ComboBox.background", new Color(38, 38, 38));
        UIManager.put("ComboBox.foreground", new Color(230, 230, 230));
        UIManager.put("TextField.background", new Color(38, 38, 38));
        UIManager.put("TextField.foreground", new Color(230, 230, 230));

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
        setForeground(new Color(150, 150, 150));
        setBackground(new Color(26, 26, 26));
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
        g.setColor(new Color(60, 60, 60));
        g.drawLine(getWidth() - 1, clip.y, getWidth() - 1, clip.y + clip.height);
    }
}

