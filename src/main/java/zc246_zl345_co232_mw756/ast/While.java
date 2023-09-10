package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.context.PrimitiveType;
import zc246_zl345_co232_mw756.visitor.Visitor;

//chuhan
public class While extends Statement {
    public Expr guard;
    public Statement body;

    public While(Expr guard, Statement body) {
        this.guard = guard;
        this.body = body;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom("while");
        guard.printNode(printer);
        body.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitWhile(this);
    }

    @Override
    public Node constFold() {
        guard = (Expr) guard.constFold();
        if(guard instanceof Bool){
            Bool b = (Bool) guard;
            if(b.value == 0){
                return new Block().setType(this.type);
            }
        }
        body = (Statement) body.constFold();
        return this;
    }

    @Override
    public void checkReturn(PrimitiveType[] rets) {
        body.checkReturn(rets);
    }
}
