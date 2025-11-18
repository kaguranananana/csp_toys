package calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * 计算器的“数据与业务大脑”：集中管理当前输入、累加器、历史显示以及所有算术规则。
 * 控制器只能通过这里暴露的高层方法操作状态，视图不会直接触碰 BigDecimal 或格式化逻辑，
 * 从而实现 MVC 中视图与模型的完全解耦。
 */
public class CalculatorModel {

    /**
     * 支持的一元运算类型，由控制器传入以便统一处理。
     */
    public enum UnaryOperation {
        SQUARE_ROOT,
        RECIPROCAL,
        PERCENT,
        NEGATE
    }

    // 所有 BigDecimal 运算共用的精度与舍入方式，确保乘除/倒数统一。
    private static final MathContext DEFAULT_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    // currentInput 保存显示屏上的“正在输入”部分，使用 StringBuilder 便于退格。
    private final StringBuilder currentInput = new StringBuilder("0");
    // accumulator 存储立即执行链路中已确认的左操作数。
    private BigDecimal accumulator = BigDecimal.ZERO;
    // pendingOperator 记录等待执行的二元运算符（+、-、×、÷）。
    private String pendingOperator;
    // 立即执行完成后需要在下一个数字输入时清空 currentInput。
    private boolean resetInputOnNextDigit = true;
    // historyDisplay 对应界面上方的小字历史记录，如 “12 +”.
    private String historyDisplay = "";
    // errorMessage 非空即表示出现错误，此时显示屏上只显示错误内容。
    private String errorMessage;

    /**
     * 输入一位数字（0-9）。
     * 负责处理覆盖输入、去掉前导 0、错误状态屏蔽等细节。
     */
    public void inputDigit(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("Digit must be between 0 and 9");
        }
        if (isErrorState()) {
            return;
        }
        if (resetInputOnNextDigit) {
            // 刚执行完一次运算，需要从 0 开始新的输入。
            overwriteInput("0");
            resetInputOnNextDigit = false;
        }
        if (currentInput.length() == 1 && currentInput.charAt(0) == '0') {
            // 去掉单独的 0，避免形成 0123。
            currentInput.setLength(0);
        }
        currentInput.append((char) ('0' + digit));
    }

    /**
     * 输入小数点：若当前值没有小数点则追加，若刚执行完运算则补 0.。
     */
    public void inputDecimalPoint() {
        if (isErrorState()) {
            return;
        }
        if (resetInputOnNextDigit) {
            overwriteInput("0");
            resetInputOnNextDigit = false;
        }
        if (currentInput.indexOf(".") == -1) {
            currentInput.append('.');
        }
    }

    /**
     * 应用二元运算符（+、-、×、÷），遵循“立即执行”规则：
     * - 第一次按运算符时把当前输入搬到 accumulator；
     * - 再次按运算符会先计算 accumulator (op) currentInput；
     * - 如果刚按完运算符又换一个运算符，只替换 pendingOperator。
     */
    public void applyBinaryOperator(String operator) {
        if (isErrorState()) {
            return;
        }
        if (isBlank(operator)) {
            throw new IllegalArgumentException("Operator cannot be blank");
        }
        if (pendingOperator != null && resetInputOnNextDigit) {
            // 用户在没有输入新数字前重复按运算符，只需改历史和待执行运算符。
            pendingOperator = operator;
            historyDisplay = formatBigDecimal(accumulator) + " " + operator;
            return;
        }
        BigDecimal inputValue = getCurrentInputValue();
        try {
            if (pendingOperator == null) {
                // 链路的第一步：把当前输入记为左操作数。
                accumulator = inputValue;
            } else {
                // 立即执行：把之前的运算符派上用场，并把结果继续放入 accumulator。
                accumulator = computeBinaryOperation(accumulator, inputValue, pendingOperator);
            }
        } catch (ArithmeticException ex) {
            setErrorMessage(ex.getMessage() == null ? "Math error" : ex.getMessage());
            return;
        }
        pendingOperator = operator;
        historyDisplay = formatBigDecimal(accumulator) + " " + operator;
        resetInputOnNextDigit = true;
    }

    /**
     * 按下等号：仅当有待执行的运算符时才执行运算，并把结果显示在主显示区。
     */
    public void evaluate() {
        if (isErrorState()) {
            return;
        }
        if (pendingOperator == null) {
            historyDisplay = "";
            return;
        }
        BigDecimal leftOperand = accumulator;
        BigDecimal rightOperand = getCurrentInputValue();
        try {
            BigDecimal result = computeBinaryOperation(leftOperand, rightOperand, pendingOperator);
            historyDisplay = formatBigDecimal(leftOperand) + " " + pendingOperator + " "
                    + formatBigDecimal(rightOperand) + " =";
            overwriteInput(result);
            resetInputOnNextDigit = true;
            accumulator = result;
            pendingOperator = null;
        } catch (ArithmeticException ex) {
            setErrorMessage(ex.getMessage() == null ? "Math error" : ex.getMessage());
        }
    }

    /**
     * 执行一元运算：平方根、倒数、百分号、取反号。
     * 这些运算通常在当前输入数字上直接生效，部分情况下需要更新 historyDisplay。
     */
    public void applyUnaryOperation(UnaryOperation operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }
        if (isErrorState()) {
            return;
        }
        BigDecimal value = getCurrentInputValue();
        try {
            switch (operation) {
                case SQUARE_ROOT:
                    if (value.compareTo(BigDecimal.ZERO) < 0) {
                        // 负数的平方根不可用，直接进入错误态。
                        setErrorMessage("Invalid input");
                        return;
                    }
                    BigDecimal sqrtValue = sqrt(value);
                    historyDisplay = "\u221a(" + formatBigDecimal(value) + ")";
                    overwriteInput(sqrtValue);
                    resetInputOnNextDigit = true;
                    break;
                case RECIPROCAL:
                    if (value.compareTo(BigDecimal.ZERO) == 0) {
                        setErrorMessage("Cannot divide by zero");
                        return;
                    }
                    BigDecimal reciprocal = BigDecimal.ONE.divide(value, DEFAULT_CONTEXT);
                    historyDisplay = "1/(" + formatBigDecimal(value) + ")";
                    overwriteInput(reciprocal);
                    resetInputOnNextDigit = true;
                    break;
                case PERCENT:
                    BigDecimal percentResult;
                    if (pendingOperator == null) {
                        // 没有待执行运算符，则将当前值直接除以 100。
                        percentResult = value.divide(ONE_HUNDRED, DEFAULT_CONTEXT);
                    } else {
                        // 存在运算符时采用 Windows 计算器规则：accumulator * current / 100。
                        percentResult = accumulator.multiply(value, DEFAULT_CONTEXT)
                                .divide(ONE_HUNDRED, DEFAULT_CONTEXT);
                    }
                    overwriteInput(percentResult);
                    resetInputOnNextDigit = true;
                    if (pendingOperator != null) {
                        historyDisplay = formatBigDecimal(accumulator) + " " + pendingOperator + " "
                                + formatBigDecimal(percentResult);
                    }
                    break;
                case NEGATE:
                    if (value.compareTo(BigDecimal.ZERO) == 0 && currentInput.indexOf(".") == -1) {
                        // 对纯 0 取负没有意义，避免出现 -0。
                        return;
                    }
                    overwriteInput(value.negate(DEFAULT_CONTEXT));
                    resetInputOnNextDigit = false;
                    break;
                default:
                    throw new IllegalStateException("Unsupported operation: " + operation);
            }
        } catch (ArithmeticException ex) {
            setErrorMessage(ex.getMessage() == null ? "Math error" : ex.getMessage());
        }
    }

    /**
     * CE 行为：仅清空当前输入；若处于错误状态则退化为 C。
     */
    public void clearEntry() {
        if (isErrorState()) {
            clearAll();
            return;
        }
        overwriteInput("0");
        resetInputOnNextDigit = false;
    }

    /**
     * C 行为：清空当前输入、累加器、历史与错误信息。
     */
    public void clearAll() {
        overwriteInput("0");
        accumulator = BigDecimal.ZERO;
        pendingOperator = null;
        historyDisplay = "";
        errorMessage = null;
        resetInputOnNextDigit = true;
    }

    /**
     * 退格：删除当前输入的最后一个字符；如果刚执行完一次运算，则把显示重置为 0。
     */
    public void backspace() {
        if (isErrorState()) {
            return;
        }
        if (resetInputOnNextDigit) {
            // 运算完立即退格相当于开始新的数字，直接回到 0。
            overwriteInput("0");
            resetInputOnNextDigit = false;
            return;
        }
        if (currentInput.length() <= 1 || (currentInput.length() == 2 && currentInput.charAt(0) == '-')) {
            overwriteInput("0");
        } else {
            currentInput.deleteCharAt(currentInput.length() - 1);
        }
    }

    /**
     * 只要 errorMessage 非空就说明模型处于错误态。
     */
    public boolean isErrorState() {
        return errorMessage != null;
    }

    /**
     * 返回主显示区内容：错误优先，其次是 currentInput。
     */
    public String getCurrentDisplay() {
        if (isErrorState()) {
            return errorMessage;
        }
        return currentInput.toString();
    }

    /**
     * 返回历史显示区内容：错误时隐藏历史，避免出现“错误 +”的组合。
     */
    public String getHistoryDisplay() {
        if (isErrorState()) {
            return "";
        }
        return historyDisplay;
    }

    /**
     * 根据运算符执行加减乘除，所有逻辑都围绕 BigDecimal 展开。
     * 之所以接受字符串，是因为控制器直接把按钮上的字符转进来。
     */
    private BigDecimal computeBinaryOperation(BigDecimal left, BigDecimal right, String operator) {
        switch (operator) {
            case "+":
                return left.add(right, DEFAULT_CONTEXT);
            case "-":
                return left.subtract(right, DEFAULT_CONTEXT);
            case "\u00d7":
            case "x":
            case "X":
            case "*":
                return left.multiply(right, DEFAULT_CONTEXT);
            case "\u00f7":
            case "/":
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ArithmeticException("Cannot divide by zero");
                }
                return left.divide(right, DEFAULT_CONTEXT);
            default:
                throw new IllegalArgumentException("Unknown operator " + operator);
        }
    }

    /**
     * 将 currentInput 转成 BigDecimal：
     * - 单独的 "-" 被视为 0；
     * - 末尾的 "." 会补上 0 以通过 BigDecimal 构造器。
     */
    private BigDecimal getCurrentInputValue() {
        String value = currentInput.toString();
        if (value.equals("-")) {
            return BigDecimal.ZERO;
        }
        if (value.endsWith(".")) {
            value = value + "0";
        }
        return new BigDecimal(value);
    }

    /**
     * 用格式化后的 BigDecimal 替换 currentInput。
     */
    private void overwriteInput(BigDecimal value) {
        overwriteInput(formatBigDecimal(value));
    }

    /**
     * 直接重写 currentInput 的字符串版本。
     */
    private void overwriteInput(String value) {
        currentInput.setLength(0);
        currentInput.append(value);
    }

    /**
     * 进入错误状态，只需设置错误消息；视图会在下一次刷新时读到它。
     */
    private void setErrorMessage(String message) {
        errorMessage = message;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 统一的 BigDecimal 文本表示：去掉末尾多余 0，并在必要时降低 scale，
     * 以模拟 Windows 计算器的输出风格。
     */
    private static String formatBigDecimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

    /**
     * BigDecimal 没有内置 sqrt，这里用牛顿迭代：
     * 从 double 精度的近似值开始，不断迭代直到两次结果相等。
     */
    private BigDecimal sqrt(BigDecimal value) {
        if (value.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        BigDecimal guess = new BigDecimal(Math.sqrt(value.doubleValue()), DEFAULT_CONTEXT);
        BigDecimal previous;
        do {
            previous = guess;
            guess = guess.add(value.divide(guess, DEFAULT_CONTEXT))
                    .divide(BigDecimal.valueOf(2), DEFAULT_CONTEXT);
        } while (!guess.equals(previous));
        return guess;
    }
}
