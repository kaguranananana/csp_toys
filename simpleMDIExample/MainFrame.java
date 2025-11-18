import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;

/**
 * 主窗口视图：负责搭建菜单、工具栏以及 MDI 环境
 */
public class MainFrame extends JFrame {
    private final JDesktopPane desktopPane = new JDesktopPane();

    private final JMenuItem newItem;
    private final JMenuItem openItem;
    private final JMenuItem saveItem;
    private final JMenuItem saveAsItem;
    private final JMenuItem exitItem;

    private final JMenuItem fontItem;
    private final JCheckBoxMenuItem boldItem;
    private final JCheckBoxMenuItem italicItem;
    private final JCheckBoxMenuItem underlineItem;

    private final JMenuItem cascadeItem;
    private final JMenuItem tileHorizontalItem;
    private final JMenuItem tileVerticalItem;

    private final JButton newButton;
    private final JButton openButton;
    private final JButton saveButton;
    private final JButton fontButton;
    private final JToggleButton boldButton;
    private final JToggleButton italicButton;
    private final JToggleButton underlineButton;

    public MainFrame() {
        super("Simple MDI Example");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        desktopPane.setBackground(new Color(230, 230, 230));

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("文件");
        menuBar.add(fileMenu);
        newItem = new JMenuItem("新建");
        openItem = new JMenuItem("打开");
        saveItem = new JMenuItem("保存");
        saveAsItem = new JMenuItem("另存为");
        exitItem = new JMenuItem("退出");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu formatMenu = new JMenu("格式");
        menuBar.add(formatMenu);
        fontItem = new JMenuItem("字体");
        boldItem = new JCheckBoxMenuItem("粗体");
        italicItem = new JCheckBoxMenuItem("斜体");
        underlineItem = new JCheckBoxMenuItem("下划线");
        formatMenu.add(fontItem);
        formatMenu.addSeparator();
        formatMenu.add(boldItem);
        formatMenu.add(italicItem);
        formatMenu.add(underlineItem);

        JMenu windowMenu = new JMenu("窗口");
        menuBar.add(windowMenu);
        cascadeItem = new JMenuItem("层叠");
        tileHorizontalItem = new JMenuItem("水平平铺");
        tileVerticalItem = new JMenuItem("垂直平铺");
        windowMenu.add(cascadeItem);
        windowMenu.add(tileHorizontalItem);
        windowMenu.add(tileVerticalItem);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        newButton = new JButton("新建");
        openButton = new JButton("打开");
        saveButton = new JButton("保存");
        fontButton = new JButton("字体");
        boldButton = new JToggleButton("B");
        italicButton = new JToggleButton("I");
        underlineButton = new JToggleButton("U");

        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.addSeparator();
        toolBar.add(fontButton);
        toolBar.addSeparator();
        toolBar.add(boldButton);
        toolBar.add(italicButton);
        toolBar.add(underlineButton);

        add(toolBar, BorderLayout.NORTH);
        add(desktopPane, BorderLayout.CENTER);
    }

    /**
     * 创建一个包含 JTextArea 的内部文档窗口
     */
    public DocumentInternalFrame createDocumentFrame(String title) {
        DocumentInternalFrame frame = new DocumentInternalFrame(title);
        desktopPane.add(frame);
        Dimension size = desktopPane.getSize();
        if (size.width == 0 || size.height == 0) {
            size = getContentPane().getSize();
        }
        frame.setBounds(0, 0, size.width, size.height);
        frame.setVisible(true);
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException ignored) {
            // 视图层允许忽略该异常
        }
        return frame;
    }

    /**
     * 返回当前激活的内部窗口
     */
    public DocumentInternalFrame getSelectedDocumentFrame() {
        JInternalFrame frame = desktopPane.getSelectedFrame();
        if (frame instanceof DocumentInternalFrame) {
            return (DocumentInternalFrame) frame;
        }
        return null;
    }

    public JDesktopPane getDesktopPane() {
        return desktopPane;
    }

    public JMenuItem getNewItem() {
        return newItem;
    }

    public JMenuItem getOpenItem() {
        return openItem;
    }

    public JMenuItem getSaveItem() {
        return saveItem;
    }

    public JMenuItem getSaveAsItem() {
        return saveAsItem;
    }

    public JMenuItem getExitItem() {
        return exitItem;
    }

    public JMenuItem getFontItem() {
        return fontItem;
    }

    public JCheckBoxMenuItem getBoldItem() {
        return boldItem;
    }

    public JCheckBoxMenuItem getItalicItem() {
        return italicItem;
    }

    public JCheckBoxMenuItem getUnderlineItem() {
        return underlineItem;
    }

    public JMenuItem getCascadeItem() {
        return cascadeItem;
    }

    public JMenuItem getTileHorizontalItem() {
        return tileHorizontalItem;
    }

    public JMenuItem getTileVerticalItem() {
        return tileVerticalItem;
    }

    public JButton getNewButton() {
        return newButton;
    }

    public JButton getOpenButton() {
        return openButton;
    }

    public JButton getSaveButton() {
        return saveButton;
    }

    public JButton getFontButton() {
        return fontButton;
    }

    public JToggleButton getBoldButton() {
        return boldButton;
    }

    public JToggleButton getItalicButton() {
        return italicButton;
    }

    public JToggleButton getUnderlineButton() {
        return underlineButton;
    }

    /**
     * 视图层负责提供窗口排列方式
     */
    public void cascadeFrames() {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        int offset = 30;
        int x = 0;
        int y = 0;
        for (JInternalFrame frame : frames) {
            frame.setLocation(x, y);
            x += offset;
            y += offset;
        }
    }

    public void tileHorizontally() {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        if (frames.length == 0) {
            return;
        }
        int height = desktopPane.getHeight() / frames.length;
        int width = desktopPane.getWidth();
        for (int i = 0; i < frames.length; i++) {
            frames[i].setBounds(0, i * height, width, height);
        }
    }

    public void tileVertically() {
        JInternalFrame[] frames = desktopPane.getAllFrames();
        if (frames.length == 0) {
            return;
        }
        int width = desktopPane.getWidth() / frames.length;
        int height = desktopPane.getHeight();
        for (int i = 0; i < frames.length; i++) {
            frames[i].setBounds(i * width, 0, width, height);
        }
    }

    /**
     * 内部文档窗体：只负责可视化展示
     */
    public static class DocumentInternalFrame extends JInternalFrame {
        private final JTextPane textArea;

        public DocumentInternalFrame(String title) {
            super(title, true, true, true, true);
            textArea = new JTextPane();
            JScrollPane scrollPane = new JScrollPane(textArea);
            setSize(400, 300);
            setLayout(new BorderLayout());
            add(scrollPane, BorderLayout.CENTER);
        }

        public JTextPane getTextArea() {
            return textArea;
        }
    }
}
