package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Pop extends Assembly {
    private Expr reg;

    public Pop(Expr reg) {
        this.reg = reg;
    }

    public Expr getReg() {
    	return reg;
    }

    @Override
    public String toString() {
        return String.format("pop %s", reg);
    }

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        reg = reg.replaceReg(old, newReg);
    }
}