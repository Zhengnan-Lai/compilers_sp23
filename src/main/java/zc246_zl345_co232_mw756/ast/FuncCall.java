package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.List;

//thomas
public class FuncCall extends Expr {
    public Identifier id;
    public List<Expr> args;

    public FuncCall(Identifier id, List<Expr> args) {
        super(id.lineNumber, id.columnNumber);
        this.id = id;
        this.args = args;
    }

    public Identifier getId() {
        return id;
    }

    public List<Expr> getArgs() {
        return args;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        id.printNode(printer);
        for (Expr e : args) {
            e.printNode(printer);
        }
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitFuncCall(this);
    }

    @Override
    public Node constFold() {
        args.replaceAll(expr -> (Expr) expr.constFold());
        return this;
    }

}

