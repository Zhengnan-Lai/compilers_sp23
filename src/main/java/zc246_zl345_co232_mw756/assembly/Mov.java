package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Mov extends Assembly {
    private Expr dest;
    private Expr src;

    public Mov(Expr dest, Expr src) {
        assert dest instanceof Register || dest instanceof Mem;
        assert !(src instanceof Mem && dest instanceof Mem);
        this.dest = dest;
        this.src = src;
    }

    public Expr getDest() {
        return dest;
    }
    public Expr getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return String.format("mov %s, %s", dest, src);
    }

    @Override
    public void replaceReg(List<Register> old, List<Register> newReg) {
        dest = dest.replaceReg(old, newReg);
        src = src.replaceReg(old, newReg);
    }
}
