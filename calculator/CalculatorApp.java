package calculator;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 应用入口类：
 * 1. 保证所有 GUI 初始化都发生在 Swing 事件派发线程（EDT）；
 * 2. 尝试套用操作系统的外观，让窗口更“原生”；
 * 3. 在 main 方法里装配 MVC 三层对象。
 */
public class CalculatorApp {

    public static void main(String[] args) {
        // Swing 是单线程 UI 框架：所有组件必须由 EDT 创建/更新，否则会抛异常或出现渲染问题。
        // 因此把整个启动过程包装在 invokeLater 回调里，确保 main 线程只负责安排任务。
        SwingUtilities.invokeLater(() -> {
            try {
                // 尝试设置系统默认的 LookAndFeel（Windows 使用 Fluent、macOS 使用 Aqua 等）。
                // 如果平台不支持或者类名不存在，catch 块会吞掉异常并继续使用默认 LAF。
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 忽略所有异常：UIManager 本身会回退到 Metal 默认主题。
            }

            // MVC 装配：模型负责计算和状态存储，视图负责布局与渲染，
            // 控制器监听用户操作并把事件转成模型调用。  
            CalculatorModel model = new CalculatorModel();
            CalculatorView view = new CalculatorView();
            new CalculatorController(model, view);

            // 最后显示主窗口；此时控制器已把按钮监听器和键盘映射都注册好了。
            view.setVisible(true);
        });
    }
}
