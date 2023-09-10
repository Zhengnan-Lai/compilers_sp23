package zc246_zl345_co232_mw756.assembly;

public class Enter extends Assembly {
    private final Const size;

    public Enter(Const size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return String.format("enter %s, 0", size);
    }
}
