package zc246_zl345_co232_mw756.ast;


import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

//michael
public class Declaration extends Statement {
    public Identifier id;
    public Type varType;

    public Declaration(Identifier id, Type varType) {
        this.id = id;
        this.varType = varType;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        id.printNode(printer);
        varType.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitDeclaration(this);
    }

    @Override
    public Node constFold() {
        return this;
    }

    public Declaration another(Identifier id){
        return new Declaration(id, varType);
    }
}
