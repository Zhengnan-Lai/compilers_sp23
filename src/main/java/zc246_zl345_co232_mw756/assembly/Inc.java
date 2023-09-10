package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Inc extends Assembly {
    private Expr reg;

    public Inc(Expr reg) {
        this.reg = reg;
    }

    public Expr getReg() {
        return this.reg;
    }

    @Override
    public String toString() {
        return String.format("inc %s", reg);
    }

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        reg = reg.replaceReg(old, newReg);
    }
}
