package zc246_zl345_co232_mw756.assembly;

import edu.cornell.cs.cs4120.xic.ir.IRConst;

public class Const extends Expr{
    private final long val;

    public Const(long val) {
        this.val = val;
    }

    public Const(IRConst c){
        this.val = c.value();
    }

    public long val() {
        return val;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Const aConst = (Const) o;
        return val == aConst.val;
    }

    @Override
    public String toString() {
        return Long.toString(val);
    }
}
