package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Movsx extends Assembly {
    private Register dest;
    private Expr src;

    public Movsx(Register dest, Expr src) {
        this.dest = dest;
        this.src = src;
    }

    public Register getDest() {
        return dest;
    }

    public Expr getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return String.format("movsx %s, %s", dest, src);
    }

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        dest = dest.replaceReg(old, newReg);
        src = src.replaceReg(old, newReg);
    }
}
