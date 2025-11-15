import javax.swing.SwingUtilities;

/**
 * 应用入口：创建 MainFrame 并初始化控制器
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            new DocumentController(frame);
            frame.setVisible(true);
        });
    }
}
