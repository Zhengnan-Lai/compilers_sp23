package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.Objects;
//thomas
public class Identifier extends Expr {
    public String id;

    public static final Node LOG_SEP = new Identifier(-1, -1, "_LOG_SEP");

    public Identifier(int lineNumber, int columnNumber, String id) {
        super(lineNumber, columnNumber);
        this.id = id;
    }

    public String toString() {
        return id;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.printAtom(id);
    }

    @Override
    public void accept(Visitor v) {
        v.visitIdentifier(this);
    }

    @Override
    public Node constFold() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier that = (Identifier) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
