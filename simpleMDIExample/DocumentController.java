import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 控制器：负责处理菜单/工具栏动作、文档读写以及窗口管理
 */
public class DocumentController {
    private final MainFrame view;
    /** 管理每个内部窗口对应的模型 */
    private final Map<MainFrame.DocumentInternalFrame, DocumentModel> documents = new HashMap<>();
    /** 未命名文档计数 */
    private int untitledCounter = 1;
    /** 用于避免在程序更新文本时重复触发 listener */
    private boolean updatingTextArea;

    public DocumentController(MainFrame view) {
        this.view = view;
        bindActions();
    }

    /**
     * 为菜单与工具栏绑定统一的动作
     */
    private void bindActions() {
        // 文件菜单
        ActionListener newListener = e -> createNewDocument();
        view.getNewItem().addActionListener(newListener);
        view.getNewButton().addActionListener(newListener);

        ActionListener openListener = e -> openDocument();
        view.getOpenItem().addActionListener(openListener);
        view.getOpenButton().addActionListener(openListener);

        ActionListener saveListener = e -> saveDocument(false);
        view.getSaveItem().addActionListener(saveListener);
        view.getSaveButton().addActionListener(saveListener);

        view.getSaveAsItem().addActionListener(e -> saveDocument(true));
        view.getExitItem().addActionListener(e -> exitApplication());

        // 格式菜单
        ActionListener fontListener = e -> chooseFont();
        view.getFontItem().addActionListener(fontListener);
        view.getFontButton().addActionListener(fontListener);

        ActionListener boldToggle = e -> toggleStyle(Font.BOLD);
        ActionListener italicToggle = e -> toggleStyle(Font.ITALIC);
        ActionListener underlineToggle = e -> toggleUnderline();

        view.getBoldItem().addActionListener(boldToggle);
        view.getBoldButton().addActionListener(boldToggle);
        view.getItalicItem().addActionListener(italicToggle);
        view.getItalicButton().addActionListener(italicToggle);
        view.getUnderlineItem().addActionListener(underlineToggle);
        view.getUnderlineButton().addActionListener(underlineToggle);

        // 窗口菜单
        view.getCascadeItem().addActionListener(e -> view.cascadeFrames());
        view.getTileHorizontalItem().addActionListener(e -> view.tileHorizontally());
        view.getTileVerticalItem().addActionListener(e -> view.tileVertically());
    }

    /**
     * 新建文档
     */
    public void createNewDocument() {
        String title = "未命名" + untitledCounter++;
        MainFrame.DocumentInternalFrame frame = view.createDocumentFrame(title);
        setupFrame(frame, new DocumentModel());
    }

    /**
     * 打开 txt 文档
     */
    public void openDocument() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(view);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        DocumentModel model = new DocumentModel();
        try {
            model.loadFromFile(file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(view, "读取文件失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        MainFrame.DocumentInternalFrame frame = view.createDocumentFrame(file.getName());
        setupFrame(frame, model);
        updateTextAreaFromModel(frame, model);
    }

    /**
     * 保存当前文档
     */
    public void saveDocument(boolean saveAs) {
        MainFrame.DocumentInternalFrame frame = view.getSelectedDocumentFrame();
        if (frame == null) {
            JOptionPane.showMessageDialog(view, "没有选中文档", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DocumentModel model = documents.get(frame);
        if (model == null) {
            return;
        }
        if (saveFrame(frame, model, saveAs)) {
            updateFrameTitle(frame, model);
        }
    }

    /**
     * 退出前检查所有文档
     */
    private void exitApplication() {
        for (JInternalFrame frame : view.getDesktopPane().getAllFrames()) {
            if (!(frame instanceof MainFrame.DocumentInternalFrame)) {
                continue;
            }
            if (!confirmClose((MainFrame.DocumentInternalFrame) frame)) {
                return;
            }
        }
        view.dispose();
    }

    /**
     * 绑定文本修改监听、窗口关闭监听
     */
    private void setupFrame(MainFrame.DocumentInternalFrame frame, DocumentModel model) {
        documents.put(frame, model);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                if (confirmClose(frame)) {
                    documents.remove(frame);
                    frame.dispose();
                }
            }

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                syncFontControls(model);
            }
        });
        frame.getTextArea().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateModelContent();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateModelContent();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateModelContent();
            }

            private void updateModelContent() {
                if (updatingTextArea) {
                    return;
                }
                model.setContent(frame.getTextArea().getText());
                updateFrameTitle(frame, model);
            }
        });
        syncFontControls(model);
        applyFontToTextArea(frame, model);
        updateFrameTitle(frame, model);
    }

    /**
     * 关闭文档前根据 dirty 状态提示保存
     */
    private boolean confirmClose(MainFrame.DocumentInternalFrame frame) {
        DocumentModel model = documents.get(frame);
        if (model == null || !model.isDirty()) {
            return true;
        }
        int option = JOptionPane.showConfirmDialog(view, "文档已修改，是否保存？", "提示", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) {
            return false;
        }
        if (option == JOptionPane.YES_OPTION) {
            return saveFrame(frame, model, false);
        }
        return true;
    }

    /**
     * 实际保存逻辑，可复用于“另存为”或关闭提示
     */
    private boolean saveFrame(MainFrame.DocumentInternalFrame frame, DocumentModel model, boolean saveAs) {
        File target = model.getFile();
        if (saveAs || target == null || model.isNewDocument()) {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(view);
            if (result != JFileChooser.APPROVE_OPTION) {
                return false;
            }
            target = chooser.getSelectedFile();
            model.setFile(target);
        }
        try {
            model.setContent(frame.getTextArea().getText());
            model.saveToFile(target);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(view, "保存失败：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        updateFrameTitle(frame, model);
        return true;
    }

    /**
     * 根据模型内容刷新文本区域
     */
    private void updateTextAreaFromModel(MainFrame.DocumentInternalFrame frame, DocumentModel model) {
        updatingTextArea = true;
        frame.getTextArea().setText(model.getContent());
        updatingTextArea = false;
        frame.getTextArea().setCaretPosition(0);
        applyFontToTextArea(frame, model);
        updateFrameTitle(frame, model);
        syncFontControls(model);
    }

    /**
     * 切换字体对话框
     */
    private void chooseFont() {
        MainFrame.DocumentInternalFrame frame = view.getSelectedDocumentFrame();
        if (frame == null) {
            JOptionPane.showMessageDialog(view, "没有选中文档", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DocumentModel model = documents.get(frame);
        if (model == null) {
            return;
        }
        Font current = model.getFontStyle();
        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String selectedFamily = (String) JOptionPane.showInputDialog(view, "选择字体", "字体", JOptionPane.PLAIN_MESSAGE, null, fontFamilies, current.getFamily());
        if (selectedFamily == null) {
            return;
        }
        String sizeText = JOptionPane.showInputDialog(view, "字体大小：", current.getSize());
        if (sizeText == null) {
            return;
        }
        int size;
        try {
            size = Integer.parseInt(sizeText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(view, "字体大小必须为数字", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int style = current.getStyle();
        Font newFont = new Font(selectedFamily, style, size);
        model.setFontStyle(newFont, model.isUnderline());
        applyFontToTextArea(frame, model);
        syncFontControls(model);
    }

    /**
     * 切换粗体/斜体
     */
    private void toggleStyle(int styleFlag) {
        MainFrame.DocumentInternalFrame frame = view.getSelectedDocumentFrame();
        if (frame == null) {
            return;
        }
        DocumentModel model = documents.get(frame);
        if (model == null) {
            return;
        }
        Font current = model.getFontStyle();
        boolean hasFlag = (current.getStyle() & styleFlag) == styleFlag;
        int newStyle = hasFlag ? current.getStyle() & ~styleFlag : current.getStyle() | styleFlag;
        Font updated = new Font(current.getFamily(), newStyle, current.getSize());
        model.setFontStyle(updated, model.isUnderline());
        applyFontToTextArea(frame, model);
        syncFontControls(model);
    }

    /**
     * 切换下划线
     */
    private void toggleUnderline() {
        MainFrame.DocumentInternalFrame frame = view.getSelectedDocumentFrame();
        if (frame == null) {
            return;
        }
        DocumentModel model = documents.get(frame);
        if (model == null) {
            return;
        }
        model.setFontStyle(model.getFontStyle(), !model.isUnderline());
        applyFontToTextArea(frame, model);
        syncFontControls(model);
    }

    /**
     * 将模型中的字体同步到 JTextArea
     */
    private void applyFontToTextArea(MainFrame.DocumentInternalFrame frame, DocumentModel model) {
        Font base = model.getFontStyle();
        Font fontWithUnderline = applyUnderline(base, model.isUnderline());
        frame.getTextArea().setFont(fontWithUnderline);
    }

    /**
     * 根据下划线状态生成新字体
     */
    private Font applyUnderline(Font base, boolean underline) {
        if (!underline) {
            return base;
        }
        Map<TextAttribute, Object> attributes = new HashMap<>(base.getAttributes());
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        return base.deriveFont(attributes);
    }

    /**
     * 根据模型更新菜单/工具栏勾选状态
     */
    private void syncFontControls(DocumentModel model) {
        boolean bold = (model.getFontStyle().getStyle() & Font.BOLD) == Font.BOLD;
        boolean italic = (model.getFontStyle().getStyle() & Font.ITALIC) == Font.ITALIC;
        boolean underline = model.isUnderline();

        view.getBoldItem().setSelected(bold);
        view.getBoldButton().setSelected(bold);
        view.getItalicItem().setSelected(italic);
        view.getItalicButton().setSelected(italic);
        view.getUnderlineItem().setSelected(underline);
        view.getUnderlineButton().setSelected(underline);
    }

    /**
     * 根据 dirty 状态刷新标题
     */
    private void updateFrameTitle(MainFrame.DocumentInternalFrame frame, DocumentModel model) {
        String baseTitle;
        if (model.getFile() != null) {
            baseTitle = model.getFile().getName();
        } else {
            baseTitle = frame.getTitle().replace("*", "");
        }
        if (model.isDirty()) {
            frame.setTitle(baseTitle + "*");
        } else {
            frame.setTitle(baseTitle);
        }
    }
}
