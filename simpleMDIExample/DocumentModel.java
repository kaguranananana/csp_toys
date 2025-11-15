
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 文档数据模型：负责维护文档内容、文件路径以及字体等状态
 */
public class DocumentModel {
    /** 文档内容 */
    private String content = "";
    /** 当前绑定的文件 */
    private File file;
    /** 是否为新建文档 */
    private boolean newDocument = true;
    /** 文档是否被修改 */
    private boolean dirty;
    /** 当前字体 */
    private Font font = new Font("SansSerif", Font.PLAIN, 14);
    /** 是否启用下划线 */
    private boolean underline;

    /**
     * 从文件加载文本内容
     */
    public void loadFromFile(File target) throws IOException {
        if (target == null) {
            throw new IllegalArgumentException("文件不能为空");
        }
        byte[] bytes = Files.readAllBytes(target.toPath());
        this.content = new String(bytes, StandardCharsets.UTF_8);
        this.file = target;
        this.newDocument = false;
        this.dirty = false;
    }

    /**
     * 将内容保存到文件
     */
    public void saveToFile(File target) throws IOException {
        File destination = target != null ? target : this.file;
        if (destination == null) {
            throw new IllegalStateException("尚未指定保存文件");
        }
        Files.write(destination.toPath(), content.getBytes(StandardCharsets.UTF_8));
        this.file = destination;
        this.newDocument = false;
        this.dirty = false;
    }

    public String getContent() {
        return content;
    }

    /**
     * 设置文档内容，若内容有变则标记为 dirty
     */
    public void setContent(String newContent) {
        String safeText = newContent == null ? "" : newContent;
        if (!safeText.equals(this.content)) {
            this.content = safeText;
            this.dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isNewDocument() {
        return newDocument;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    /**
     * 设置字体及下划线状态
     */
    public void setFontStyle(Font font, boolean underline) {
        this.font = font == null ? new Font("SansSerif", Font.PLAIN, 14) : font;
        this.underline = underline;
        this.dirty = true;
    }

    public Font getFontStyle() {
        return font;
    }

    public boolean isUnderline() {
        return underline;
    }
}
