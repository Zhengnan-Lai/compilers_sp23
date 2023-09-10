package edu.cornell.cs.cs4120.xic.ir;

import edu.cornell.cs.cs4120.util.SExpPrinter;

import java.util.Objects;

/** An intermediate representation for a temporary register TEMP(name) */
public class IRTemp extends IRExpr_c {
    private final String name;
    private static int count;

    /** @param name name of this temporary register */
    public IRTemp(String name) {
        if(!name.startsWith("_")) name = "_t" + name.replaceAll("'", "_");
        this.name=name;
        count++;
    }

    public String name() {
        return name;
    }

    @Override
    public String label() {
        return "TEMP(" + name + ")";
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("TEMP");
        p.printAtom(name);
        p.endList();
    }

    public static IRTemp getTemp(){
        return new IRTemp("_t" + count++);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IRTemp irTemp = (IRTemp) o;
        return Objects.equals(name, irTemp.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
