package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.context.Type;
import zc246_zl345_co232_mw756.visitor.Visitor;

public abstract class Node {

    public Type type;
    public int lineNumber;
    public int columnNumber;

    public Node(int lineNumber, int columnNumber) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public Node() {
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    public abstract void printNode(SExpPrinter printer);

    public abstract void accept(Visitor v);

    public abstract Node constFold();

    public Node setType(zc246_zl345_co232_mw756.context.Type type) {
        this.type = type;
        return this;
    }
}
