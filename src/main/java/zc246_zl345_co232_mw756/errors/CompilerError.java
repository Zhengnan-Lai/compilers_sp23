package zc246_zl345_co232_mw756.errors;

public abstract class CompilerError extends Error {
    int line;
    int column;
    String message;

    public CompilerError(int line, int column, String message) {
        this.line = line;
        this.column = column;
        this.message = message;
    }

    @Override
    public String getMessage() {
        return line + ":" + column + " error:" + this.message;
    }

    protected String getMessage(String type, String file) {
        return type + " error beginning at " + file + ":" + line + ":" + column + ": " + this.message;
    }

    public abstract String getMessage(String file);
}
