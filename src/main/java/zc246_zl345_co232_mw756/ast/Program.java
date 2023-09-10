package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.List;

//michael
public class Program extends Node {
    public List<Use> uses;
    public List<Definition> defs;

    public Program(List<Use> uses, List<Definition> defs) {
        this.uses = uses;
        this.defs = defs;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.startUnifiedList();
        for (Use u : uses) {
            u.printNode(printer);
        }
        printer.endList();
        printer.startUnifiedList();
        for (Definition d : defs) {
            d.printNode(printer);
        }
        printer.endList();
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitProgram(this);
    }

    @Override
    public Node constFold() {
        defs.replaceAll(def -> (Definition) def.constFold());
        return this;
    }
}
