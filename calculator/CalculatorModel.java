package calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Encapsulates the calculator state and all arithmetic rules.
 * The controller manipulates this model through high level methods so that the view
 * stays unaware of how numbers are stored or formatted.
 */
public class CalculatorModel {

    /**
     * Describes unary operations the controller can trigger.
     */
    public enum UnaryOperation {
        SQUARE_ROOT,
        RECIPROCAL,
        PERCENT,
        NEGATE
    }

    private static final MathContext DEFAULT_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final StringBuilder currentInput = new StringBuilder("0");
    private BigDecimal accumulator = BigDecimal.ZERO;
    private String pendingOperator;
    private boolean resetInputOnNextDigit = true;
    private String historyDisplay = "";
    private String errorMessage;

    /**
     * Appends a digit (0-9) to the current input buffer.
     */
    public void inputDigit(int digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("Digit must be between 0 and 9");
        }
        if (isErrorState()) {
            return;
        }
        if (resetInputOnNextDigit) {
            overwriteInput("0");
            resetInputOnNextDigit = false;
        }
        if (currentInput.length() == 1 && currentInput.charAt(0) == '0') {
            currentInput.setLength(0);
        }
        currentInput.append((char) ('0' + digit));
    }

    /**
     * Adds a decimal separator if not present yet.
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
     * Applies the requested binary operator (+, -, *, /). Uses immediate execution rules.
     */
    public void applyBinaryOperator(String operator) {
        if (isErrorState()) {
            return;
        }
        if (isBlank(operator)) {
            throw new IllegalArgumentException("Operator cannot be blank");
        }
        if (pendingOperator != null && resetInputOnNextDigit) {
            pendingOperator = operator;
            historyDisplay = formatBigDecimal(accumulator) + " " + operator;
            return;
        }
        BigDecimal inputValue = getCurrentInputValue();
        try {
            if (pendingOperator == null) {
                accumulator = inputValue;
            } else {
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
     * Calculates the result if an operator is pending.
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
     * Applies unary operations like square root, reciprocal, percent and toggle sign.
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
                        percentResult = value.divide(ONE_HUNDRED, DEFAULT_CONTEXT);
                    } else {
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
     * Clears the current input (CE behavior).
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
     * Clears everything including pending operations.
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
     * Removes the last typed character.
     */
    public void backspace() {
        if (isErrorState()) {
            return;
        }
        if (resetInputOnNextDigit) {
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

    public boolean isErrorState() {
        return errorMessage != null;
    }

    public String getCurrentDisplay() {
        if (isErrorState()) {
            return errorMessage;
        }
        return currentInput.toString();
    }

    public String getHistoryDisplay() {
        if (isErrorState()) {
            return "";
        }
        return historyDisplay;
    }

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

    private void overwriteInput(BigDecimal value) {
        overwriteInput(formatBigDecimal(value));
    }

    private void overwriteInput(String value) {
        currentInput.setLength(0);
        currentInput.append(value);
    }

    private void setErrorMessage(String message) {
        errorMessage = message;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String formatBigDecimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0);
        }
        return normalized.toPlainString();
    }

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
