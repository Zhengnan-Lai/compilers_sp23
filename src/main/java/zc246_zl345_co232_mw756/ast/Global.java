package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

public class Global extends Definition {
    public Identifier id;
    public Type varType;
    public Expr value;

    public Global(Identifier id, Type varType, Expr value) {
        super(id.lineNumber, id.columnNumber);
        this.id = id;
        this.varType = varType;
        this.value = value;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom(":global");
        id.printNode(printer);
        varType.printNode(printer);
        if (value != null) value.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitGlobal(this);
    }

    @Override
    public Node constFold() {
        if(value!=null) value = (Expr) value.constFold();
        return this;
    }

    public Global another(Identifier id){
        return new Global(id, varType, null);
    }
}
