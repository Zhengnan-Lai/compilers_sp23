package zc246_zl345_co232_mw756.assembly;

public class Comment extends Assembly {
    private final String message;

    public Comment(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "// " + message;
    }
}
