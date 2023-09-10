package edu.cornell.cs.cs4120.xic.ir;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.visit.InsnMapsBuilder;

/** An intermediate representation for naming a memory address */
public class IRLabel extends IRStmt {
    private final String name;
    private static int count = 0;

    /** @param name name of this memory address */
    public IRLabel(String name) {
        this.name = name;
        count++;
    }

    public String name() {
        return name;
    }

    @Override
    public String label() {
        return "LABEL(" + name + ")";
    }

    @Override
    public InsnMapsBuilder buildInsnMapsEnter(InsnMapsBuilder v) {
        v.addNameToCurrentIndex(name);
        return v;
    }

    @Override
    public void printSExp(SExpPrinter p) {
        p.startList();
        p.printAtom("LABEL");
        p.printAtom(name);
        p.endList();
    }

    public static IRLabel getLabel(){
        count++;
        return new IRLabel("_L" + (count-1));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IRLabel) {
            IRLabel other = (IRLabel) obj;
            return name.equals(other.name);
        }
        return false;
    }
}
