package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Test extends Assembly {
    private Expr r1;
    private Expr r2;

    public Test(Expr r1, Expr r2) {
        this.r1 = r1;
        this.r2 = r2;
    }

    public Expr getR1() {
        return this.r1;
    }

    public Expr getR2() {
        return this.r2;
    }

    @Override
    public String toString() {
        return String.format("test %s, %s", r1, r2);
    }

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        r1 = r1.replaceReg(old, newReg);
        r2 = r2.replaceReg(old, newReg);
    }
}
