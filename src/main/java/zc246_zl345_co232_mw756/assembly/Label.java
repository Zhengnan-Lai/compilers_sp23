package zc246_zl345_co232_mw756.assembly;

public class Label extends Assembly {
    private final String label;

    public Label(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format(".%s:", label.substring(1));
    }
}
