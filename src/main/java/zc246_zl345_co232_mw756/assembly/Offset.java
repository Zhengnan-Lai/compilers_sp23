package zc246_zl345_co232_mw756.assembly;

public class Offset extends Expr{
    // offset for data, gives the address of data - used by tiling IRName
    private final String label;

    public Offset(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format("QWORD PTR [%s]", label);
    }
}
