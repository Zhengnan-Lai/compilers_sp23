package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.context.PrimitiveType;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

public class Block extends Statement {
    public List<Statement> statements;

    public Block(int lineNumber, int columnNumber, List<Statement> statements) {
        super(lineNumber, columnNumber);
        this.statements = statements;
    }

    public Block(){
        this.statements = new ArrayList<>();
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        for (Statement s : statements) {
            s.printNode(printer);
        }
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitBlock(this);
    }

    @Override
    public void checkReturn(PrimitiveType[] rets) {
        for (Statement s : statements) {
            s.checkReturn(rets);
        }
    }

    @Override
    public Node constFold() {
        statements.replaceAll(stmt -> (Statement) stmt.constFold());
        return this;
    }

}
