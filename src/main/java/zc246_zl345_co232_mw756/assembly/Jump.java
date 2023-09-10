package zc246_zl345_co232_mw756.assembly;

public class Jump extends Assembly {

    public enum JumpType {
        JMP, JZ, JE, JNZ, JNE, JG, JGE, JL, JLE, JB, JBE, JA, JAE
    }

    private final JumpType type;
    private final String label;

    public Jump(JumpType type, String label) {
        this.type = type;
        this.label = label;
    }

    public JumpType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return String.format("%s .%s", type.toString().toLowerCase(), label.substring(1));
    }
}
