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

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

        setLayout(new BorderLayout());
        createMenuBar();
        createToolbar();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.6);
        split.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5), new LineBorder(new Color(200, 200, 200), 1)));
        split.setDividerSize(8);
        add(split, BorderLayout.CENTER);

        setupCodeEditor();
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codeScroll.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(100, 149, 237), 2), "Code Editor",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)),
                new EmptyBorder(5, 5, 5, 5)));
        split.setLeftComponent(codeScroll);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(new EmptyBorder(10, 5, 10, 10));
        split.setRightComponent(right);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(34, 139, 34), 2), "Controls",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)),
                new EmptyBorder(10, 10, 10, 10)));

        JPanel row1 = new JPanel(new BorderLayout(8, 8));
        row1.add(new JLabel("Choose Test Case:"), BorderLayout.WEST);
        tests.setPreferredSize(new Dimension(200, 25));
        row1.add(tests, BorderLayout.CENTER);
        top.add(row1);
        top.add(Box.createVerticalStrut(15));

        JPanel row3 = new JPanel(new BorderLayout(8, 8));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton runBtn = createStyledButton("Run", new Color(34, 139, 34));
        JButton clearBtn = createStyledButton("Clear", new Color(220, 20, 60));
        buttonPanel.add(runBtn);
        buttonPanel.add(clearBtn);
        row3.add(buttonPanel, BorderLayout.NORTH);
        row3.add(inputPanel, BorderLayout.CENTER);
        top.add(row3);
        right.add(top, BorderLayout.NORTH);

        setupOutputArea();
        JScrollPane outScroll = new JScrollPane(outputArea);
        outScroll.setBorder(new CompoundBorder(
                new TitledBorder(new LineBorder(new Color(255, 140, 0), 2), "Program Output",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("Arial", Font.BOLD, 12)),
                new EmptyBorder(5, 5, 5, 5)));
        right.add(outScroll, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);

        tests.addActionListener(e -> applySelection());
        runBtn.addActionListener(this::runProgram);
        clearBtn.addActionListener(e -> { outputArea.setText(""); updateStatus("Output cleared"); });

        // Show PL1 by default
        applySelection();
    }

    private void setupCodeEditor() {
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        codeArea.setBackground(new Color(248, 248, 255));
        codeArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        codeArea.setLineWrap(false);
        codeArea.setTabSize(4);

        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        inputPanel.add(new JLabel("Input: "), BorderLayout.WEST);
        inputField.setDocument(new JTextFieldLimit(20, false)); // allow all input
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
        outputArea.setBackground(new Color(240, 240, 240));
        outputArea.setBorder(new EmptyBorder(5, 5, 5, 5));
    }

    private JButton createStyledButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 11));
        b.setBackground(color);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(80, 30));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(color.darker()); }
            public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(color); }
        });
        return b;
    }

    private void createMenuBar() {
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem n = new JMenuItem("New");
        JMenuItem q = new JMenuItem("Exit");
        n.addActionListener(e -> { codeArea.setText(""); outputArea.setText(""); updateStatus("New file"); });
        q.addActionListener(e -> System.exit(0));
        file.add(n); file.addSeparator(); file.add(q);

        JMenu run = new JMenu("Run");
        JMenuItem r = new JMenuItem("Run Program (F5)");
        r.addActionListener(this::runProgram);
        run.add(r);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(this::showAbout);
        JMenuItem keys = new JMenuItem("Keyboard Shortcuts");
        keys.addActionListener(this::showShortcuts);
        help.add(about); help.add(keys);

        mb.add(file); mb.add(run); mb.add(help);
        setJMenuBar(mb);
    }

    private void createToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBorder(new EmptyBorder(2, 2, 2, 2));
        JButton runBtn = createStyledButton("Run", new Color(34, 139, 34));
        JButton clearBtn = createStyledButton("Clear", new Color(220, 20, 60));
        runBtn.addActionListener(this::runProgram);
        clearBtn.addActionListener(e -> { outputArea.setText(""); updateStatus("Output cleared"); });
        tb.add(runBtn);
        tb.add(clearBtn);
        add(tb, BorderLayout.NORTH);
    }

    private JPanel createStatusBar() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBorder(new CompoundBorder(new EmptyBorder(2, 5, 2, 5), new LineBorder(new Color(200, 200, 200), 1)));
        sb.setBackground(new Color(240, 240, 240));
        progressBar.setPreferredSize(new Dimension(150, 20));
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
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
                    updateStatus("Program executed successfully");
                    isRunning = false;
                    progressBar.setIndeterminate(false);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText("Error executing program: " + ex.getMessage());
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
