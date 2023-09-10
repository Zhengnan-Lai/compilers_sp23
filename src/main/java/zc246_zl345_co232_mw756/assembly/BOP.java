package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class BOP extends Assembly {
    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        dest = dest.replaceReg(old, newReg);
        src = src.replaceReg(old, newReg);
    }

    public enum BOPType {
        ADD, SUB, IMUL, AND, OR, XOR, NOT, SHL, SHR, SAR
    }

    private Expr dest;
    private Expr src;
    private final BOPType op;

    public BOPType getBOPType() {
        return op;
    }

    public Expr getDest() {
        return dest;
    }

    public Expr getSrc() {
        return src;
    }

    public BOP(BOPType op, Expr dest, Expr src) {
        assert dest instanceof Register || dest instanceof Mem;
        assert !(src instanceof Mem && dest instanceof Mem);
        this.dest = dest;
        this.src = src;
        this.op = op;
    }

    @Override
    public String toString() {
        return String.format("%s %s, %s", op.toString().toLowerCase(), dest, src);
    }
}
