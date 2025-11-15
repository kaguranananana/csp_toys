import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Controller 层：负责连接 Model 与 View，处理菜单动作与拖拽逻辑。
 */
public class PuzzleController {
    private final PuzzleModel model;
    private final PuzzleView view;
    private final Random random = new Random();

    private Timer challengeTimer;
    private boolean challengeRunning;
    private int elapsedSeconds;
    private File lastDirectory;
    private final MouseAdapter boardMouseHandler = new BoardMouseHandler();

    public PuzzleController(PuzzleModel model, PuzzleView view) {
        this.model = Objects.requireNonNull(model);
        this.view = Objects.requireNonNull(view);
        attachMenuListeners();
        attachBoardListeners();
        loadInitialImage();
        updateDifficultyLabel();
        resetTimerLabel();
    }

    private void attachMenuListeners() {
        view.getOpenImageItem().addActionListener(e -> openImageFromDisk());
        view.getViewOriginalItem().addActionListener(e -> previewOriginalImage());
        view.getRandomImageItem().addActionListener(e -> {
            if (!loadRandomSampleImage()) {
                shuffleCurrentImage();
            }
        });

        view.getDiff3Item().addActionListener(e -> changeDifficulty(3));
        view.getDiff4Item().addActionListener(e -> changeDifficulty(4));
        view.getDiff5Item().addActionListener(e -> changeDifficulty(5));

        view.getChallengeModeItem().addActionListener(e -> toggleChallengeMode());
    }

    private void attachBoardListeners() {
        view.addBoardMouseListener(boardMouseHandler);
        view.addBoardMouseMotionListener(boardMouseHandler);
    }

    /**
     * 初始化时尝试加载示例图片，不存在则生成彩条占位图。
     */
    private void loadInitialImage() {
        if (loadRandomSampleImage()) {
            return;
        }
        model.loadImage(createFallbackImage());
        model.shufflePieces();
        refreshBoard();
    }

    private void openImageFromDisk() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "bmp", "gif"));
        int result = chooser.showOpenDialog(view);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            try {
                model.loadImage(file);
                refreshBoard();
                updateDifficultyLabel();
                stopChallengeTimer();
            } catch (IOException ex) {
                view.showInfoDialog("加载图片失败：\n" + ex.getMessage());
            }
        }
    }

    private void previewOriginalImage() {
        BufferedImage image = model.getOriginalImage();
        if (image == null) {
            view.showInfoDialog("请先加载一张图片再查看原图。");
            return;
        }
        view.showOriginalImage(image);
    }

    private void changeDifficulty(int gridSize) {
        model.setGridSize(gridSize);
        model.shufflePieces();
        refreshBoard();
        updateDifficultyLabel();
        stopChallengeTimer();
    }

    private void shuffleCurrentImage() {
        model.shufflePieces();
        refreshBoard();
        stopChallengeTimer();
    }

    private void toggleChallengeMode() {
        if (challengeRunning) {
            stopChallengeTimer();
        } else {
            startChallengeTimer();
        }
    }

    private void startChallengeTimer() {
        if (challengeTimer == null) {
            challengeTimer = new Timer(1000, new ChallengeTimerListener());
        }
        elapsedSeconds = 0;
        challengeRunning = true;
        view.getChallengeModeItem().setText("停止计时");
        resetTimerLabel();
        challengeTimer.start();
    }

    private void stopChallengeTimer() {
        if (challengeTimer != null) {
            challengeTimer.stop();
        }
        challengeRunning = false;
        elapsedSeconds = 0;
        view.getChallengeModeItem().setText("开始计时");
        resetTimerLabel();
    }

    private void resetTimerLabel() {
        view.updateTimerLabel(formatElapsed(elapsedSeconds));
    }

    private String formatElapsed(int seconds) {
        int minutes = seconds / 60;
        int remain = seconds % 60;
        return String.format(Locale.getDefault(), "计时：%02d:%02d", minutes, remain);
    }

    private void refreshBoard() {
        view.renderPieces(model.getPieces(), model.getGridSize());
        view.getBoardPanel().repaint();
    }

    private void updateDifficultyLabel() {
        int grid = model.getGridSize();
        view.updateDifficultyLabel("难度：" + grid + "x" + grid);
    }

    private boolean loadRandomSampleImage() {
        List<File> samples = discoverSampleImages();
        if (samples.isEmpty()) {
            return false;
        }
        File target = samples.get(random.nextInt(samples.size()));
        try {
            model.loadImage(target);
            refreshBoard();
            updateDifficultyLabel();
            stopChallengeTimer();
            return true;
        } catch (IOException ex) {
            view.showInfoDialog("读取示例图片失败：\n" + ex.getMessage());
            return false;
        }
    }

    private List<File> discoverSampleImages() {
        File dir = new File("resources/images");
        if (!dir.exists() || !dir.isDirectory()) {
            return new ArrayList<>();
        }
        File[] files = dir.listFiles(new ImageFileFilter());
        List<File> result = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    private BufferedImage createFallbackImage() {
        int width = PuzzleModel.DEFAULT_BOARD_WIDTH;
        int height = PuzzleModel.DEFAULT_BOARD_HEIGHT;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < height; i++) {
            float hue = i / (float) height;
            g2d.setColor(ColorUtil.hsbColor(hue));
            g2d.drawLine(0, i, width, i);
        }
        g2d.dispose();
        return image;
    }

    private void checkCompletion() {
        if (model.isFinished()) {
            view.showInfoDialog("恭喜拼图完成！");
            stopChallengeTimer();
        }
    }

    private int locatePieceIndex(int x, int y) {
        JPanel boardPanel = view.getBoardPanel();
        int grid = model.getGridSize();
        if (grid <= 0 || model.getPieces().isEmpty()) {
            return -1;
        }
        int tileWidth = boardPanel.getWidth() / grid;
        int tileHeight = boardPanel.getHeight() / grid;
        if (tileWidth <= 0 || tileHeight <= 0) {
            return -1;
        }
        if (x < 0 || y < 0 || x >= tileWidth * grid || y >= tileHeight * grid) {
            return -1;
        }
        int col = x / tileWidth;
        int row = y / tileHeight;
        int index = row * grid + col;
        return index >= model.getPieces().size() ? -1 : index;
    }

    /**
     * 鼠标拖拽处理：按下记录起点、释放后交换。
     */
    private class BoardMouseHandler extends MouseAdapter {
        private int startIndex = -1;

        @Override
        public void mousePressed(MouseEvent e) {
            startIndex = locatePieceIndex(e.getX(), e.getY());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int targetIndex = locatePieceIndex(e.getX(), e.getY());
            if (startIndex >= 0 && targetIndex >= 0) {
                model.swapPieces(startIndex, targetIndex);
                refreshBoard();
                checkCompletion();
            }
            startIndex = -1;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // 拖动过程中暂不做视觉反馈，释放时完成交换。
        }
    }

    private class ChallengeTimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!challengeRunning) {
                return;
            }
            elapsedSeconds++;
            view.updateTimerLabel(formatElapsed(elapsedSeconds));
        }
    }

    /**
     * 图片文件过滤器，用于示例图片目录扫描。
     */
    private static class ImageFileFilter implements FileFilter {
        private final String[] extensions = {".png", ".jpg", ".jpeg", ".bmp", ".gif"};

        @Override
        public boolean accept(File pathname) {
            String name = pathname.getName().toLowerCase(Locale.ROOT);
            for (String ext : extensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 简易颜色生成工具：根据 hue 创建彩虹色。
     */
    private static class ColorUtil {
        static java.awt.Color hsbColor(float hue) {
            return java.awt.Color.getHSBColor(hue, 0.5f, 0.9f);
        }
    }
}
