package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.List;

public class Assignment extends Statement {
    public List<Node> args;
    public List<Expr> vals;

    public static Node getUnderscore(int line, int col) {
        return new Identifier(line, col, "_");
    }

    public Assignment(List<Node> args, List<Expr> vals) {
        this.args = args;
        this.vals = vals;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom("=");
        if (args.size() == 1) {
            args.get(0).printNode(printer);
        } else {
            printer.startUnifiedList();
            for (Node n : args) {
                n.printNode(printer);
            }
            printer.endList();
        }

        for (Expr e : vals) {
            e.printNode(printer);
        }

        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitAssignment(this);
    }

    @Override
    public Node constFold() {
        vals.replaceAll(expr -> (Expr) expr.constFold());
        return this;
    }
}
