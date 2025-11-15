package calculator;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Coordinates interactions between the view and the model.
 */
public class CalculatorController implements ActionListener {

    private final CalculatorModel model;
    private final CalculatorView view;

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
        view.setDisplays(model.getHistoryDisplay(), model.getCurrentDisplay());
    }

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

    private void registerKeyAction(String name, KeyStroke stroke, String command) {
        view.registerKeyAction(name, stroke, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCommand(command);
            }
        });
    }
}
