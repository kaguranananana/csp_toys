import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionListener;
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
                syncFontControls(frame);
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
        frame.getTextArea().addCaretListener(e -> syncFontControls(frame));
        syncFontControls(frame);
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
        syncFontControls(frame);
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
        model.setFontStyle(newFont);
        applyFontToTextArea(frame, model);
        syncFontControls(frame);
    }

    /**
     * 切换粗体/斜体（仅作用于当前选区；如果没有选区则影响后续输入）
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
        JTextPane textPane = frame.getTextArea();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        boolean isBoldFlag = styleFlag == Font.BOLD;
        if (start == end) {
            MutableAttributeSet inputAttrs = textPane.getInputAttributes();
            boolean currentState = isBoldFlag ? StyleConstants.isBold(inputAttrs) : StyleConstants.isItalic(inputAttrs);
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (isBoldFlag) {
                StyleConstants.setBold(attrs, !currentState);
                StyleConstants.setBold(inputAttrs, !currentState);
            } else {
                StyleConstants.setItalic(attrs, !currentState);
                StyleConstants.setItalic(inputAttrs, !currentState);
            }
            textPane.setCharacterAttributes(attrs, false);
        } else {
            StyledDocument doc = textPane.getStyledDocument();
            AttributeSet currentAttrs = doc.getCharacterElement(start).getAttributes();
            boolean currentState = isBoldFlag ? StyleConstants.isBold(currentAttrs) : StyleConstants.isItalic(currentAttrs);
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            if (isBoldFlag) {
                StyleConstants.setBold(attrs, !currentState);
            } else {
                StyleConstants.setItalic(attrs, !currentState);
            }
            doc.setCharacterAttributes(start, end - start, attrs, false);
        }
        syncFontControls(frame);
    }

    /**
     * 切换下划线（仅作用于当前选区；若无选区则影响输入属性）
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
        JTextPane textPane = frame.getTextArea();
        int start = textPane.getSelectionStart();
        int end = textPane.getSelectionEnd();
        if (start == end) {
            MutableAttributeSet inputAttrs = textPane.getInputAttributes();
            boolean currentState = StyleConstants.isUnderline(inputAttrs);
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setUnderline(attrs, !currentState);
            StyleConstants.setUnderline(inputAttrs, !currentState);
            textPane.setCharacterAttributes(attrs, false);
        } else {
            StyledDocument doc = textPane.getStyledDocument();
            AttributeSet currentAttrs = doc.getCharacterElement(start).getAttributes();
            boolean currentState = StyleConstants.isUnderline(currentAttrs);
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setUnderline(attrs, !currentState);
            doc.setCharacterAttributes(start, end - start, attrs, false);
        }
        syncFontControls(frame);
    }

    /**
     * 将模型中的默认字体同步到 JTextPane（默认字体影响全部文字）
     */
    private void applyFontToTextArea(MainFrame.DocumentInternalFrame frame, DocumentModel model) {
        Font base = model.getFontStyle();
        JTextPane textPane = frame.getTextArea();
        textPane.setFont(base);
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, base.getFamily());
        StyleConstants.setFontSize(attrs, base.getSize());
        StyleConstants.setBold(attrs, (base.getStyle() & Font.BOLD) == Font.BOLD);
        StyleConstants.setItalic(attrs, (base.getStyle() & Font.ITALIC) == Font.ITALIC);
        doc.setCharacterAttributes(0, doc.getLength(), attrs, false);
    }

    /**
     * 根据当前光标/选区更新菜单/工具栏勾选状态
     */
    private void syncFontControls(MainFrame.DocumentInternalFrame frame) {
        if (frame == null) {
            return;
        }
        JTextPane textPane = frame.getTextArea();
        AttributeSet attrs = textPane.getInputAttributes();
        boolean bold = StyleConstants.isBold(attrs);
        boolean italic = StyleConstants.isItalic(attrs);
        boolean underline = StyleConstants.isUnderline(attrs);

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
