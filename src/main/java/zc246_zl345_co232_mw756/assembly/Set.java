package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Set extends Assembly {

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        r = r.replaceReg(old, newReg);
    }

    public enum Type {
        SETZ, SETNZ, SETL, SETLE, SETG, SETGE, SETB
    }

    private Expr r;
    private final Type t;

    public Set(Expr r, Type t) {
        this.r = r;
        this.t = t;
    }

    public Expr getExpr() {
        return this.r;
    }

    public Type getType() {
        return this.t;
    }

    public void setExpr(Expr r) {
        this.r = r;
    }

    @Override
    public String toString() {
        return String.format("%s %s", t.toString().toLowerCase(), r);
    }
}