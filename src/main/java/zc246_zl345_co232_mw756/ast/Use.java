package zc246_zl345_co232_mw756.ast;


import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

//santiago
public class Use extends Node {
    public Identifier id;

    public Use(Identifier id) {
        this.id = id;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom("use");
        id.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitUse(this);
    }

    @Override
    public Node constFold() {
        return this;
    }
}
