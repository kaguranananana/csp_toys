package calculator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * 控制器：作为 View 与 Model 的粘合层，负责监听按钮/键盘事件并把命令转交给模型。
 * 同时根据模型返回的显示内容刷新视图，保证两者之间不过度耦合。
 */
public class CalculatorController implements ActionListener {

    private final CalculatorModel model;
    private final CalculatorView view;

    /**
     * 构造时即完成事件绑定与显示初始化。
     */
    public CalculatorController(CalculatorModel model, CalculatorView view) {
        this.model = model;
        this.view = view;
        this.view.addButtonListener(this);
        this.view.setDisplays(model.getHistoryDisplay(), model.getCurrentDisplay());
        registerKeyboardShortcuts();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        handleCommand(e.getActionCommand());
    }

    /**
     * 根据按钮/键盘发来的 action command 分派模型方法。
     * 命令字符串与按钮标签保持一致，降低额外映射成本。
     */
    private void handleCommand(String command) {
        if (command == null || command.isEmpty()) {
            return;
        }
        switch (command) {
            case "0": case "1": case "2": case "3": case "4":
            case "5": case "6": case "7": case "8": case "9":
                model.inputDigit(command.charAt(0) - '0');
                break;
            case ".":
                model.inputDecimalPoint();
                break;
            case "+":
            case "-":
            case "\u00d7":
            case "\u00f7":
                model.applyBinaryOperator(command);
                break;
            case "=":
                model.evaluate();
                break;
            case "%":
                model.applyUnaryOperation(CalculatorModel.UnaryOperation.PERCENT);
                break;
            case "1/x":
                model.applyUnaryOperation(CalculatorModel.UnaryOperation.RECIPROCAL);
                break;
            case "\u221a":
                model.applyUnaryOperation(CalculatorModel.UnaryOperation.SQUARE_ROOT);
                break;
            case "+/-":
                model.applyUnaryOperation(CalculatorModel.UnaryOperation.NEGATE);
                break;
            case "CE":
                model.clearEntry();
                break;
            case "C":
                model.clearAll();
                break;
            case "\u2190":
                model.backspace();
                break;
            default:
                break;
        }
        // 每次模型状态改变后立即刷新两个显示标签。
        view.setDisplays(model.getHistoryDisplay(), model.getCurrentDisplay());
    }

    /**
     * 把常用按键映射到按钮命令，保证键盘与鼠标交互一致。
     */
    private void registerKeyboardShortcuts() {
        for (char digit = '0'; digit <= '9'; digit++) {
            registerKeyAction("DIGIT_" + digit, KeyStroke.getKeyStroke(digit), String.valueOf(digit));
        }
        registerKeyAction("DECIMAL_POINT", KeyStroke.getKeyStroke('.'), ".");
        registerKeyAction("DECIMAL_NUMPAD", KeyStroke.getKeyStroke(KeyEvent.VK_DECIMAL, 0), ".");

        registerKeyAction("ADD_KEY", KeyStroke.getKeyStroke('+'), "+");
        registerKeyAction("ADD_NUMPAD", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "+");
        registerKeyAction("SUBTRACT_KEY", KeyStroke.getKeyStroke('-'), "-");
        registerKeyAction("SUBTRACT_NUMPAD", KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "-");
        registerKeyAction("MULTIPLY_KEY", KeyStroke.getKeyStroke('*'), "\u00d7");
        registerKeyAction("MULTIPLY_NUMPAD", KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, 0), "\u00d7");
        registerKeyAction("DIVIDE_KEY", KeyStroke.getKeyStroke('/'), "\u00f7");
        registerKeyAction("DIVIDE_NUMPAD", KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0), "\u00f7");

        registerKeyAction("ENTER", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "=");
        registerKeyAction("EQUALS", KeyStroke.getKeyStroke('='), "=");

        registerKeyAction("BACKSPACE", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "\u2190");
        registerKeyAction("ESCAPE", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "C");
        registerKeyAction("DELETE", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "CE");
    }

    /**
     * 注册单个快捷键：
     * - name: 供 InputMap/ActionMap 使用的逻辑名称；
     * - stroke: 触发该命令的按键组合；
     * - command: 复用按钮命令，保证键盘事件走同一条流程。
     */
    private void registerKeyAction(String name, KeyStroke stroke, String command) {
        view.registerKeyAction(name, stroke, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCommand(command);
            }
        });
    }
}
