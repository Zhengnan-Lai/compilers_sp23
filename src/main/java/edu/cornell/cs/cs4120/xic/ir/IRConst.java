package edu.cornell.cs.cs4120.xic.ir;

import edu.cornell.cs.cs4120.util.SExpPrinter;

import java.util.Objects;

/** An intermediate representation for a 64-bit integer constant. CONST(n) */
public class IRConst extends IRExpr_c {
    private final long value;

    /** @param value value of this constant */
    public IRConst(long value) {
        this.value = value;
    }

    public long value() {
        return value;
    }

    @Override
    public String label() {
        return "CONST(" + value + ")";
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public long constant() {
        return value;
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("CONST");
        p.printAtom(String.valueOf(value));
        p.endList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IRConst irConst = (IRConst) o;
        return value == irConst.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
