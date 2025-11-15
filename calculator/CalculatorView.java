package calculator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the calculator window and exposes hooks for controller updates.
 */
public class CalculatorView extends JFrame {

    private final JLabel historyLabel = buildHistoryLabel();
    private final JLabel mainDisplayLabel = buildMainDisplayLabel();
    private final Map<String, JButton> buttonMap = new LinkedHashMap<>();

    public CalculatorView() {
        super("Windows Style Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        setResizable(true);
        add(createDisplayPanel(), BorderLayout.NORTH);
        add(createButtonPanel(), BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public void setDisplays(String history, String current) {
        historyLabel.setText(history == null ? "" : history);
        mainDisplayLabel.setText(current == null ? "0" : current);
    }

    public void addButtonListener(ActionListener listener) {
        buttonMap.values().forEach(button -> button.addActionListener(listener));
    }

    public JButton getButton(String command) {
        return buttonMap.get(command);
    }

    public Map<String, JButton> getButtonMap() {
        return Collections.unmodifiableMap(buttonMap);
    }

    public void registerKeyAction(String actionName, KeyStroke keyStroke,
                                  javax.swing.Action action) {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(keyStroke, actionName);
        root.getActionMap().put(actionName, action);
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(historyLabel, gbc);

        gbc.gridy = 1;
        panel.add(mainDisplayLabel, gbc);
        panel.setBackground(new Color(0xf2f2f2));
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        String[][] layout = {
                {"%", "CE", "C", "\u2190"},
                {"1/x", "\u221a", "+/-", "\u00f7"},
                {"7", "8", "9", "\u00d7"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "+"},
                {"0", ".", "="}
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.weightx = 1;
        gbc.weighty = 1;

        for (int row = 0; row < layout.length; row++) {
            int gridX = 0;
            for (String label : layout[row]) {
                if (label == null || label.isEmpty()) {
                    continue;
                }
                JButton button = buildButton(label);
                buttonMap.put(label, button);
                gbc.gridx = gridX;
                gbc.gridy = row;
                boolean spanTwo = "0".equals(label) && row == layout.length - 1;
                gbc.gridwidth = spanTwo ? 2 : 1;
                panel.add(button, gbc);
                gridX += gbc.gridwidth;
                gbc.gridwidth = 1;
            }
            while (gridX < 4) {
                gridX++;
            }
        }
        return panel;
    }

    private JLabel buildHistoryLabel() {
        JLabel label = new JLabel("", SwingConstants.RIGHT);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        label.setForeground(Color.DARK_GRAY);
        return label;
    }

    private JLabel buildMainDisplayLabel() {
        JLabel label = new JLabel("0", SwingConstants.RIGHT);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setForeground(Color.BLACK);
        return label;
    }

    private JButton buildButton(String label) {
        JButton button = new JButton(label);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        button.setBackground(UIManager.getColor("Button.background"));
        return button;
    }
}
