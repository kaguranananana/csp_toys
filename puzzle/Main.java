import javax.swing.SwingUtilities;

/**
 * 程序入口：初始化 MVC 并启动窗口。
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PuzzleModel model = new PuzzleModel();
            PuzzleView view = new PuzzleView();
            new PuzzleController(model, view);
            view.setVisible(true);
        });
    }
}
