package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.context.PrimitiveType;
import zc246_zl345_co232_mw756.visitor.Visitor;

//chuhan
public class If extends Statement {
    public Expr guard;
    public Statement body;
    public Statement elseBody;

    public If(Expr guard, Statement body, Statement elseBody) {
        this.guard = guard;
        this.body = body;
        this.elseBody = elseBody;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom("if");
        guard.printNode(printer);
        body.printNode(printer);
        if (elseBody != null) elseBody.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitIf(this);
    }

    @Override
    public Node constFold() {
        guard = (Expr) guard.constFold();
        if(guard instanceof Bool){
            Bool b = (Bool) guard;
            if(b.value == 1){
                return body.constFold().setType(this.type);
            }else{
                if(elseBody != null) return elseBody.constFold();
                else return new Block().setType(this.type);
            }
        }
        body = (Statement) body.constFold();
        if (elseBody != null) elseBody = (Statement) elseBody.constFold();
        return this;
    }

    @Override
    public void checkReturn(PrimitiveType[] rets) {
        body.checkReturn(rets);
        if (elseBody != null) elseBody.checkReturn(rets);
    }
}

