package calculator;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 深色 Fluent 风格的计算器视图，封装显示与按钮布局。
 */
public class CalculatorView extends JFrame {

    private static final Color WINDOW_BACKGROUND = new Color(0x1f1f1f);
    private static final Color DISPLAY_BACKGROUND = new Color(0x181818);
    private static final Color DISPLAY_PRIMARY_TEXT = new Color(0xf5f5f5);
    private static final Color DISPLAY_SECONDARY_TEXT = new Color(0xbfbfbf);

    private static final Color DIGIT_COLOR = new Color(0x2d2d2d);
    private static final Color FUNCTION_COLOR = new Color(0x252525);
    private static final Color OPERATOR_COLOR = new Color(0x3c3c3c);
    private static final Color EQUALS_COLOR = new Color(0x2196f3);

    private static final Font PRIMARY_DISPLAY_FONT = new Font("Segoe UI", Font.BOLD, 44);
    private static final Font SECONDARY_DISPLAY_FONT = new Font("Segoe UI", Font.PLAIN, 18);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.PLAIN, 20);

    private static final Insets BUTTON_INSETS = new Insets(4, 4, 4, 4);
    private static final int BUTTON_CORNER_RADIUS = 12;

    private final JLabel historyLabel = buildHistoryLabel();
    private final JLabel mainDisplayLabel = buildMainDisplayLabel();
    private final Map<String, JButton> buttonMap = new LinkedHashMap<>();

    private enum ButtonCategory {
        DIGIT,
        FUNCTION,
        OPERATOR,
        EQUALS
    }

    public CalculatorView() {
        super("Windows Style Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(360, 520));

        JPanel content = new JPanel(new BorderLayout(16, 16));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        content.setBackground(WINDOW_BACKGROUND);
        content.add(createDisplayPanel(), BorderLayout.NORTH);
        content.add(createButtonPanel(), BorderLayout.CENTER);
        setContentPane(content);
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
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionName);
        root.getActionMap().put(actionName, action);
    }

    /**
     * 顶部显示区：模拟显示屏的深色背景与右对齐文本。
     */
    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(DISPLAY_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2a2a2a)),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        panel.add(historyLabel, BorderLayout.NORTH);
        panel.add(mainDisplayLabel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 主按钮区域：GridBagLayout + 圆角按钮，保证缩放时保持网格和间距。
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        addButton(panel, "%", ButtonCategory.FUNCTION, 0, 0, 1, 1);
        addButton(panel, "CE", ButtonCategory.FUNCTION, 1, 0, 1, 1);
        addButton(panel, "C", ButtonCategory.FUNCTION, 2, 0, 1, 1);
        addButton(panel, "\u2190", ButtonCategory.FUNCTION, 3, 0, 1, 1);

        addButton(panel, "1/x", ButtonCategory.FUNCTION, 0, 1, 1, 1);
        addButton(panel, "\u221a", ButtonCategory.FUNCTION, 1, 1, 1, 1);
        addButton(panel, "+/-", ButtonCategory.FUNCTION, 2, 1, 1, 1);
        addButton(panel, "\u00f7", ButtonCategory.OPERATOR, 3, 1, 1, 1);

        addButton(panel, "7", ButtonCategory.DIGIT, 0, 2, 1, 1);
        addButton(panel, "8", ButtonCategory.DIGIT, 1, 2, 1, 1);
        addButton(panel, "9", ButtonCategory.DIGIT, 2, 2, 1, 1);
        addButton(panel, "\u00d7", ButtonCategory.OPERATOR, 3, 2, 1, 1);

        addButton(panel, "4", ButtonCategory.DIGIT, 0, 3, 1, 1);
        addButton(panel, "5", ButtonCategory.DIGIT, 1, 3, 1, 1);
        addButton(panel, "6", ButtonCategory.DIGIT, 2, 3, 1, 1);
        addButton(panel, "-", ButtonCategory.OPERATOR, 3, 3, 1, 1);

        addButton(panel, "1", ButtonCategory.DIGIT, 0, 4, 1, 1);
        addButton(panel, "2", ButtonCategory.DIGIT, 1, 4, 1, 1);
        addButton(panel, "3", ButtonCategory.DIGIT, 2, 4, 1, 1);
        addButton(panel, "+", ButtonCategory.OPERATOR, 3, 4, 1, 1);

        addButton(panel, "0", ButtonCategory.DIGIT, 0, 5, 2, 1);
        addButton(panel, ".", ButtonCategory.DIGIT, 2, 5, 1, 1);
        addButton(panel, "=", ButtonCategory.EQUALS, 3, 5, 1, 2);

        return panel;
    }

    private JLabel buildHistoryLabel() {
        JLabel label = new JLabel("", SwingConstants.RIGHT);
        label.setForeground(DISPLAY_SECONDARY_TEXT);
        label.setFont(SECONDARY_DISPLAY_FONT);
        return label;
    }

    private JLabel buildMainDisplayLabel() {
        JLabel label = new JLabel("0", SwingConstants.RIGHT);
        label.setForeground(DISPLAY_PRIMARY_TEXT);
        label.setFont(PRIMARY_DISPLAY_FONT);
        return label;
    }

    private JButton addButton(JPanel panel, String label, ButtonCategory category,
                              int gridX, int gridY, int gridWidth, int gridHeight) {
        RoundedButton button = buildButton(label, category);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridX;
        gbc.gridy = gridY;
        gbc.gridwidth = gridWidth;
        gbc.gridheight = gridHeight;
        gbc.insets = BUTTON_INSETS;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = gridWidth;
        gbc.weighty = gridHeight;
        panel.add(button, gbc);
        button.setActionCommand(label);
        buttonMap.put(label, button);
        return button;
    }

    private RoundedButton buildButton(String label, ButtonCategory category) {
        RoundedButton button = new RoundedButton(label, BUTTON_CORNER_RADIUS);
        button.setFont(BUTTON_FONT);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setForeground(colorForText(category));

        Color base = colorForCategory(category);
        Color hover = adjustColor(base, 12);
        Color pressed = adjustColor(base, -20);
        if (category == ButtonCategory.EQUALS) {
            hover = adjustColor(base, 20);
            pressed = adjustColor(base, -30);
        }
        button.setColors(base, hover, pressed);
        return button;
    }

    private Color colorForCategory(ButtonCategory category) {
        switch (category) {
            case FUNCTION:
                return FUNCTION_COLOR;
            case OPERATOR:
                return OPERATOR_COLOR;
            case EQUALS:
                return EQUALS_COLOR;
            default:
                return DIGIT_COLOR;
        }
    }

    private Color colorForText(ButtonCategory category) {
        return category == ButtonCategory.FUNCTION
                ? DISPLAY_SECONDARY_TEXT
                : Color.WHITE;
    }

    private Color adjustColor(Color color, int delta) {
        int r = clamp(color.getRed() + delta);
        int g = clamp(color.getGreen() + delta);
        int b = clamp(color.getBlue() + delta);
        return new Color(r, g, b);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * 圆角按钮控件，使用透明背景、悬停/按下颜色。
     */
    private static class RoundedButton extends JButton {
        private final int radius;
        private Color baseColor;
        private Color hoverColor;
        private Color pressedColor;

        RoundedButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        }

        void setColors(Color base, Color hover, Color pressed) {
            this.baseColor = base;
            this.hoverColor = hover;
            this.pressedColor = pressed;
            ChangeListener listener = e -> repaint();
            getModel().addChangeListener(listener);
        }

        @Override
        protected void paintComponent(Graphics g) {
            ButtonModel model = getModel();
            Color fill = baseColor;
            if (model.isPressed()) {
                fill = pressedColor;
            } else if (model.isRollover()) {
                fill = hoverColor;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
