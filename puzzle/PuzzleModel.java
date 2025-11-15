import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * Model 层：负责图片的加载、缩放、切割以及当前拼图状态的维护。
 */
public class PuzzleModel {
    /** 默认拼图网格大小（3x3）。 */
    public static final int DEFAULT_GRID_SIZE = 3;
    /** 默认拼图区目标宽度。 */
    public static final int DEFAULT_BOARD_WIDTH = 600;
    /** 默认拼图区目标高度。 */
    public static final int DEFAULT_BOARD_HEIGHT = 600;

    private final List<PuzzlePiece> pieces = new ArrayList<>();
    private final Dimension boardSize = new Dimension(DEFAULT_BOARD_WIDTH, DEFAULT_BOARD_HEIGHT);
    private final Random random = new Random();

    private BufferedImage originalImage;
    private BufferedImage scaledImage;
    private int gridSize = DEFAULT_GRID_SIZE;

    /**
     * 从文件系统加载图片，并触发缩放与切割。
     *
     * @param imageFile 待加载的文件
     * @throws IOException 图片无法读取时抛出
     */
    public void loadImage(File imageFile) throws IOException {
        Objects.requireNonNull(imageFile, "imageFile 不能为空");
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("无法读取图片：" + imageFile.getAbsolutePath());
        }
        loadImage(image);
    }

    /**
     * 直接使用内存中的 BufferedImage，便于后续复用或测试。
     */
    public void loadImage(BufferedImage image) {
        Objects.requireNonNull(image, "image 不能为空");
        originalImage = image;
        scaledImage = scaleToFit(image, boardSize.width, boardSize.height);
        sliceImage();
    }

    /**
     * 修改目标网格大小（如 3/4/5），会重新切割已有图片。
     */
    public void setGridSize(int newGridSize) {
        if (newGridSize < 2) {
            throw new IllegalArgumentException("gridSize 至少为 2");
        }
        if (this.gridSize != newGridSize) {
            this.gridSize = newGridSize;
            if (scaledImage != null) {
                sliceImage();
            }
        }
    }

    public int getGridSize() {
        return gridSize;
    }

    /**
     * 修改目标绘制尺寸，便于在不同分辨率窗口中复用。
     */
    public void setBoardSize(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("boardSize 必须为正值");
        }
        boardSize.setSize(width, height);
        if (originalImage != null) {
            scaledImage = scaleToFit(originalImage, boardSize.width, boardSize.height);
            sliceImage();
        }
    }

    public Dimension getBoardSize() {
        return new Dimension(boardSize);
    }

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public BufferedImage getScaledImage() {
        return scaledImage;
    }

    /**
     * 当前拼图块的浅拷贝视图，供 View 渲染。
     */
    public List<PuzzlePiece> getPieces() {
        return Collections.unmodifiableList(pieces);
    }

    /**
     * 将两个索引位置的拼图块交换位置，并更新各自的 currentIndex/GridPos。
     */
    public void swapPieces(int firstIndex, int secondIndex) {
        if (firstIndex == secondIndex) {
            return;
        }
        validatePieceIndex(firstIndex);
        validatePieceIndex(secondIndex);
        Collections.swap(pieces, firstIndex, secondIndex);
        updatePiecePlacement(firstIndex);
        updatePiecePlacement(secondIndex);
    }

    /**
     * 随机打乱拼图，保证不会停留在完成状态。
     */
    public void shufflePieces() {
        if (pieces.isEmpty()) {
            return;
        }
        Collections.shuffle(pieces, random);
        for (int i = 0; i < pieces.size(); i++) {
            updatePiecePlacement(i);
        }
        if (isFinished() && pieces.size() > 1) {
            swapPieces(0, 1);
        }
    }

    /**
     * 判断当前是否已经拼回原始顺序。
     */
    public boolean isFinished() {
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).getOriginalIndex() != i) {
                return false;
            }
        }
        return true;
    }

    /**
     * 切割缩放后的图片，并初始化拼图块列表。
     */
    private void sliceImage() {
        pieces.clear();
        if (scaledImage == null) {
            return;
        }
        double tileWidth = scaledImage.getWidth() / (double) gridSize;
        double tileHeight = scaledImage.getHeight() / (double) gridSize;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int x1 = (int) Math.round(col * tileWidth);
                int y1 = (int) Math.round(row * tileHeight);
                int x2 = (int) Math.round((col + 1) * tileWidth);
                int y2 = (int) Math.round((row + 1) * tileHeight);
                int width = Math.max(1, x2 - x1);
                int height = Math.max(1, y2 - y1);
                BufferedImage subImage = scaledImage.getSubimage(x1, y1, width, height);
                int originalIndex = row * gridSize + col;
                PuzzlePiece piece = new PuzzlePiece(subImage, originalIndex, originalIndex,
                        new Point(col, row));
                pieces.add(piece);
            }
        }

        shufflePieces();
    }

    private void updatePiecePlacement(int index) {
        PuzzlePiece piece = pieces.get(index);
        piece.setCurrentIndex(index);
        piece.setCurrentGridPosition(indexToGridPoint(index));
    }

    private Point indexToGridPoint(int index) {
        int row = index / gridSize;
        int col = index % gridSize;
        return new Point(col, row);
    }

    private void validatePieceIndex(int index) {
        if (index < 0 || index >= pieces.size()) {
            throw new IndexOutOfBoundsException("拼图索引超出范围：" + index);
        }
    }

    /**
     * 将图片按比例缩放至指定区域内。
     */
    private BufferedImage scaleToFit(BufferedImage source, int targetWidth, int targetHeight) {
        double scale = Math.min(
                targetWidth / (double) source.getWidth(),
                targetHeight / (double) source.getHeight());
        int newWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int newHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(source, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return resized;
    }

    /**
     * 描述单个拼图块的结构体。
     */
    public static class PuzzlePiece {
        private final BufferedImage image;
        private final int originalIndex;
        private final Point originalGridPosition;

        private int currentIndex;
        private Point currentGridPosition;

        private PuzzlePiece(BufferedImage image, int originalIndex, int currentIndex, Point originalGridPosition) {
            this.image = image;
            this.originalIndex = originalIndex;
            this.currentIndex = currentIndex;
            this.originalGridPosition = originalGridPosition;
            this.currentGridPosition = new Point(originalGridPosition);
        }

        public BufferedImage getImage() {
            return image;
        }

        public int getOriginalIndex() {
            return originalIndex;
        }

        public int getCurrentIndex() {
            return currentIndex;
        }

        public Point getOriginalGridPosition() {
            return new Point(originalGridPosition);
        }

        public Point getCurrentGridPosition() {
            return new Point(currentGridPosition);
        }

        private void setCurrentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        private void setCurrentGridPosition(Point point) {
            this.currentGridPosition = new Point(point);
        }
    }
}
