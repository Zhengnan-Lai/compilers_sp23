package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Lea extends Assembly {

    private Register dest;
    private Expr e;

    public Lea(Register dest, Expr e) {
        this.dest = dest;
        this.e = e;
    }

    public Register getDest() {
        return this.dest;
    }

    public Expr getExpr() {
        return this.e;
    }

    @Override
    public String toString() {
        return String.format("lea %s, %s", dest, e);
    }

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        dest = dest.replaceReg(old, newReg);
        e = e.replaceReg(old, newReg);
    }
}
