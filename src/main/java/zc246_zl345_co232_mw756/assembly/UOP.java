package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class UOP extends Assembly {
    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        r = r.replaceReg(old, newReg);
    }

    public enum Type {
        MUL, IMUL, DIV, IDIV
    }

    private final Type t;
    private Register r;

    public UOP(Type t, Register r) {
        this.t = t;
        this.r = r;
    }

    public Type getType() {
        return this.t;
    }

    public Register getRegister() {
        return this.r;
    }

    @Override
    public String toString() {
        return String.format("%s %s", t.toString().toLowerCase(), r);
    }
}
