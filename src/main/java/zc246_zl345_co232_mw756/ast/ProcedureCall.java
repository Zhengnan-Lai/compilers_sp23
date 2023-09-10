package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.List;

//santiago
public class ProcedureCall extends Statement {
    public Identifier id;
    public List<Expr> args;

    public ProcedureCall(Identifier id, List<Expr> args) {
        this.id = id;
        this.args = args;
    }

    public static ProcedureCall fromFuncCall(FuncCall fc) {
        return new ProcedureCall(fc.getId(), fc.getArgs());
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
        v.visitProcedureCall(this);
    }

    @Override
    public Node constFold() {
        args.replaceAll(expr -> (Expr) expr.constFold());
        return this;
    }
}
