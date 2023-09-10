package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.List;

//santiago
public class Function extends Definition {
    public Identifier id;
    public List<Declaration> params;
    public List<Type> return_types;
    public Block body;

    public Function(Identifier id, List<Declaration> params, List<Type> return_types, Block body) {
        super(id.lineNumber, id.columnNumber);
        this.id = id;
        this.params = params;
        this.return_types = return_types;
        this.body = body;
    }


    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        id.printNode(printer);
        printer.startUnifiedList();
        for (Declaration d : params) {
            d.printNode(printer);
        }
        printer.endList();
        printer.startUnifiedList();
        for (Type t : return_types) {
            t.printNode(printer);
        }
        printer.endList();
        if (body != null) body.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitFunction(this);
    }

    @Override
    public Node constFold() {
        body = (Block) body.constFold();
        return this;
    }
}
