package edu.cornell.cs.cs4120.xic.ir.visit;

import edu.cornell.cs.cs4120.xic.ir.IRBinOp;
import edu.cornell.cs.cs4120.xic.ir.IRConst;
import edu.cornell.cs.cs4120.xic.ir.IRNode;
import edu.cornell.cs.cs4120.xic.ir.IRNodeFactory;

public class ConstFolding extends IRVisitor{

    public ConstFolding(IRNodeFactory inf) {
        super(inf);
    }

    @Override
    public IRNode leave(IRNode parent, IRNode n2, IRNode n, IRVisitor v2) {
        if(n instanceof IRBinOp){
            IRBinOp binOp = (IRBinOp) n;
            if(binOp.isConstant()){
                switch (binOp.opType()){
                    case EQ:
                        return binOp.left().constant() == binOp.right().constant() ? new IRConst(1) : new IRConst(0);
                    case NEQ:
                        return binOp.left().constant() != binOp.right().constant() ? new IRConst(1) : new IRConst(0);
                    case LT:
                        return binOp.left().constant() < binOp.right().constant() ? new IRConst(1) : new IRConst(0);
                    case GT:
                        return binOp.left().constant() > binOp.right().constant() ? new IRConst(1) : new IRConst(0);
                    case LEQ:
                        return binOp.left().constant() <= binOp.right().constant() ? new IRConst(1) : new IRConst(0);
                    case GEQ:
                        return binOp.left().constant() >= binOp.right().constant() ? new IRConst(1) : new IRConst(0);
                    case ULT:
                        return Long.compareUnsigned(binOp.left().constant(), binOp.right().constant()) < 0 ? new IRConst(1) : new IRConst(0);
                    case ADD:
                        return new IRConst(binOp.left().constant() + binOp.right().constant());
                    case SUB:
                        return new IRConst(binOp.left().constant() - binOp.right().constant());
                    case MUL:
                        return new IRConst(binOp.left().constant() * binOp.right().constant());
                    case DIV:
                        return new IRConst(binOp.left().constant() / binOp.right().constant());
                    case MOD:
                        return new IRConst(binOp.left().constant() % binOp.right().constant());
                    case HMUL:
                        return new IRConst(Math.multiplyHigh(binOp.left().constant(), binOp.right().constant()));
                    case AND:
                        return new IRConst(binOp.left().constant() & binOp.right().constant());
                    case OR:
                        return new IRConst(binOp.left().constant() | binOp.right().constant());
                    case XOR:
                        return new IRConst(binOp.left().constant() ^ binOp.right().constant());
                    case LSHIFT:
                        return new IRConst(binOp.left().constant() << binOp.right().constant());
                    case RSHIFT:
                        return new IRConst(binOp.left().constant() >> binOp.right().constant());
                    case ARSHIFT:
                        return new IRConst(binOp.left().constant() >>> binOp.right().constant());
                }
            } else if(binOp.left().isConstant()){
                switch (binOp.opType()){
                    case ADD:
                        if(binOp.left().constant() == 0L) return binOp.right();
                        break;
                    case MUL:
                        if(binOp.left().constant() == 0L) return new IRConst(0);
                        if(binOp.left().constant() == 1L) return binOp.right();
                        break;
                    case AND:
                        if(binOp.left().constant() == 0L) return new IRConst(0);
                        if(binOp.left().constant() == 1L) return binOp.right();
                        break;
                    case OR:
                        if(binOp.left().constant() == 0L) return binOp.right();
                        if(binOp.left().constant() == 1L) return new IRConst(1);
                        break;
                }
            } else if(binOp.right().isConstant()){
                switch (binOp.opType()){
                    case ADD:
                    case SUB:
                    case LSHIFT:
                    case RSHIFT:
                    case ARSHIFT:
                        if(binOp.right().constant() == 0L) return binOp.left();
                        break;
                    case MUL:
                        if(binOp.right().constant() == 0L) return new IRConst(0);
                        if(binOp.right().constant() == 1L) return binOp.left();
                        break;
                    case DIV:
                        if(binOp.right().constant() == 1L) return binOp.left();
                        break;
                    case MOD:
                        if(binOp.right().constant() == 1L) return new IRConst(0);
                        break;
                    case AND:
                        if(binOp.right().constant() == 0L) return new IRConst(0);
                        if(binOp.right().constant() == 1L) return binOp.left();
                        break;
                    case OR:
                        if(binOp.right().constant() == 0L) return binOp.left();
                        if(binOp.right().constant() == 1L) return new IRConst(1);
                        break;
                }
            }
        }
        return n;
    }
}
