package zc246_zl345_co232_mw756.errors;

public class SyntaxError extends CompilerError {
    public SyntaxError(int line, int column, String message) {
        super(line, column, message);
    }

    @Override
    public String getMessage(String file) {
        return getMessage("Syntax", file);
    }
}
