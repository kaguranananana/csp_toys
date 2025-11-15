import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * View 层：负责窗口、菜单、拼图区以及状态栏展示。
 */
public class PuzzleView extends JFrame {
    private final JMenuItem openImageItem;
    private final JMenuItem viewOriginalItem;
    private final JMenuItem randomImageItem;
    private final JRadioButtonMenuItem diff3Item;
    private final JRadioButtonMenuItem diff4Item;
    private final JRadioButtonMenuItem diff5Item;
    private final JMenuItem challengeModeItem;

    private final PuzzleBoardPanel boardPanel;
    private final JLabel statusLabel;
    private final JLabel timerLabel;

    public PuzzleView() {
        super("myPuzzle 拼图游戏");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(800, 700));

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu imageMenu = new JMenu("图片");
        openImageItem = new JMenuItem("打开图片...");
        viewOriginalItem = new JMenuItem("查看原图");
        randomImageItem = new JMenuItem("随机切换图片");
        imageMenu.add(openImageItem);
        imageMenu.add(viewOriginalItem);
        imageMenu.add(randomImageItem);
        menuBar.add(imageMenu);

        JMenu difficultyMenu = new JMenu("难度设置");
        ButtonGroup difficultyGroup = new ButtonGroup();
        diff3Item = new JRadioButtonMenuItem("3x3", true);
        diff4Item = new JRadioButtonMenuItem("4x4");
        diff5Item = new JRadioButtonMenuItem("5x5");
        difficultyGroup.add(diff3Item);
        difficultyGroup.add(diff4Item);
        difficultyGroup.add(diff5Item);
        difficultyMenu.add(diff3Item);
        difficultyMenu.add(diff4Item);
        difficultyMenu.add(diff5Item);
        menuBar.add(difficultyMenu);

        JMenu challengeMenu = new JMenu("挑战模式");
        challengeModeItem = new JMenuItem("开始计时");
        challengeMenu.add(challengeModeItem);
        menuBar.add(challengeMenu);

        boardPanel = new PuzzleBoardPanel();
        add(boardPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout(10, 0));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusLabel = new JLabel("难度：3x3");
        timerLabel = new JLabel("计时：00:00", SwingConstants.RIGHT);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(timerLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    // ===== 菜单项 Getter，方便 Controller 绑定事件 =====
    public JMenuItem getOpenImageItem() {
        return openImageItem;
    }

    public JMenuItem getViewOriginalItem() {
        return viewOriginalItem;
    }

    public JMenuItem getRandomImageItem() {
        return randomImageItem;
    }

    public JRadioButtonMenuItem getDiff3Item() {
        return diff3Item;
    }

    public JRadioButtonMenuItem getDiff4Item() {
        return diff4Item;
    }

    public JRadioButtonMenuItem getDiff5Item() {
        return diff5Item;
    }

    public JMenuItem getChallengeModeItem() {
        return challengeModeItem;
    }

    /**
     * 暴露拼图区，便于 Controller 注册鼠标拖拽监听器。
     */
    public JPanel getBoardPanel() {
        return boardPanel;
    }

    /**
     * 更新拼图区显示的拼图块。
     */
    public void renderPieces(List<PuzzleModel.PuzzlePiece> pieces, int gridSize) {
        boardPanel.setPieces(pieces, gridSize);
    }

    /**
     * 设置状态栏中的难度提示。
     */
    public void updateDifficultyLabel(String text) {
        statusLabel.setText(text);
    }

    /**
     * 更新状态栏中的计时/挑战提示。
     */
    public void updateTimerLabel(String text) {
        timerLabel.setText(text);
    }

    /**
     * 弹出窗口显示原图，供用户对照。
     */
    public void showOriginalImage(BufferedImage image) {
        Objects.requireNonNull(image, "原图不能为空");
        ImageIcon icon = new ImageIcon(image);
        JLabel imageLabel = new JLabel(icon);
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        scrollPane.setPreferredSize(new Dimension(600, 600));

        JDialog dialog = new JDialog(this, "原图预览", true);
        dialog.getContentPane().add(scrollPane);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 弹出简单对话框，用于提示挑战模式或完成状态。
     */
    public void showInfoDialog(String message) {
        JDialog dialog = new JDialog(this, "提示", true);
        dialog.setLayout(new BorderLayout(10, 10));
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        JButton okButton = new JButton("确定");
        okButton.addActionListener(e -> dialog.dispose());
        dialog.add(label, BorderLayout.CENTER);
        dialog.add(okButton, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 在拼图区注册鼠标监听器（Controller 会调用）。
     */
    public void addBoardMouseListener(MouseListener listener) {
        boardPanel.addMouseListener(listener);
    }

    public void addBoardMouseMotionListener(MouseMotionListener listener) {
        boardPanel.addMouseMotionListener(listener);
    }

    /**
     * 自定义面板，负责绘制当前拼图块。
     */
    private static class PuzzleBoardPanel extends JPanel {
        private List<PuzzleModel.PuzzlePiece> pieces = Collections.emptyList();
        private int gridSize = PuzzleModel.DEFAULT_GRID_SIZE;

        PuzzleBoardPanel() {
            setBackground(Color.DARK_GRAY);
            setPreferredSize(new Dimension(
                    PuzzleModel.DEFAULT_BOARD_WIDTH,
                    PuzzleModel.DEFAULT_BOARD_HEIGHT));
        }

        void setPieces(List<PuzzleModel.PuzzlePiece> newPieces, int newGridSize) {
            pieces = newPieces == null ? Collections.emptyList() : newPieces;
            gridSize = Math.max(2, newGridSize);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (pieces.isEmpty()) {
                drawPlaceholder(g);
                return;
            }

            int tileWidth = getWidth() / gridSize;
            int tileHeight = getHeight() / gridSize;

            for (int i = 0; i < pieces.size(); i++) {
                PuzzleModel.PuzzlePiece piece = pieces.get(i);
                int row = i / gridSize;
                int col = i % gridSize;
                int x = col * tileWidth;
                int y = row * tileHeight;
                Image pieceImage = piece.getImage();
                g.drawImage(pieceImage, x, y, tileWidth, tileHeight, this);
                g.setColor(Color.BLACK);
                g.drawRect(x, y, tileWidth, tileHeight);
            }
        }

        private void drawPlaceholder(Graphics g) {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.DARK_GRAY);
            String text = "请通过菜单加载图片";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(text)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, x, y);
        }
    }
}
