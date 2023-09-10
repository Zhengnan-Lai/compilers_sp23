package edu.cornell.cs.cs4120.xic.ir.visit;

import edu.cornell.cs.cs4120.xic.ir.*;

import java.util.ArrayList;
import java.util.List;

//Invariant: after leaving a node, there is no IRESeq in the subtree of the node
public class LIR extends IRVisitor {
    public LIR(IRNodeFactory inf) {
        super(inf);
    }

    //flatten IRESeq
    private IRExpr flatten(IRExpr n, List<IRStmt> stmts){
        if (n instanceof IRESeq){
            IRESeq ireseq = (IRESeq) n;
            if (ireseq.stmt() instanceof IRSeq)
            {
                IRSeq seq = (IRSeq) ireseq.stmt();
                stmts.addAll(seq.stmts());
            }
            else {
                stmts.add(ireseq.stmt());
            }
            return ireseq.expr();
        }
        else {
            return n;
        }
    }

    private boolean classInv(IRNode n2){
        if(n2 instanceof IRBinOp){
            IRBinOp binOp = (IRBinOp)n2;
            return !(binOp.left() instanceof IRESeq) && !(binOp.right() instanceof IRESeq);
        }else if(n2 instanceof IRCall){
            IRCall call = (IRCall)n2;
            boolean ret = true;
            for(IRExpr e: call.args()){
                ret = ret && !(e instanceof IRESeq);
            }
            return ret;
        }else if(n2 instanceof IRCallStmt){
            IRCallStmt callStmt = (IRCallStmt)n2;
            boolean ret = true;
            for(IRExpr e: callStmt.args()){
                ret = ret && !(e instanceof IRESeq);
            }
            return ret;
        }else if(n2 instanceof IRESeq){
            IRESeq eSeq = (IRESeq)n2;
            return !(eSeq.expr() instanceof IRESeq);
        }else if(n2 instanceof IRExp){
            IRExp exp = (IRExp)n2;
            return !(exp.expr() instanceof IRESeq);
        }else if(n2 instanceof IRMem){
            IRMem mem = (IRMem)n2;
            return !(mem.expr() instanceof IRESeq);
        }else if(n2 instanceof IRMove){
            IRMove move = (IRMove)n2;
            return !(move.target() instanceof IRESeq) && !(move.source() instanceof IRESeq);
        }else if(n2 instanceof IRReturn){
            IRReturn ret = (IRReturn)n2;
            boolean b = true;
            for(IRExpr e: ret.rets()){
                b = b && !(e instanceof IRESeq);
            }
            return b;
        }else {
            return true;
        }
    }

    @Override
    protected IRNode leave(IRNode parent, IRNode n, IRNode n2, IRVisitor v2) {
        IRNode node;
        if(n2 instanceof IRBinOp){
            node =  leaveIRBinOp(parent, n2);
        }else if(n2 instanceof IRCall){
            node = leaveIRCall(parent, n2);
        }else if(n2 instanceof IRCallStmt){
            node = leaveIRCallStmt(parent, n2);
        }else if(n2 instanceof IRCJump){
            node = leaveIRCJump(parent, n2);
        }else if(n2 instanceof IRCompUnit){
            node = leaveIRCompUnit(parent, n2);
        }else if(n2 instanceof IRConst){
            node = leaveIRConst(parent, n2);
        }else if(n2 instanceof IRESeq){
            node =leaveIRESeq(parent, n2);
        }else if(n2 instanceof IRExp){
            node = leaveIRExp(parent, n2);
        }else if(n2 instanceof IRFuncDecl){
            node = leaveIRFuncDecl(parent, n2);
        }else if(n2 instanceof IRJump){
            node = leaveIRJump(parent, n2);
        }else if(n2 instanceof IRLabel){
            node = leaveIRLabel(parent, n2);
        }else if(n2 instanceof IRMem){
            node = leaveIRMem(parent, n2);
        }else if(n2 instanceof IRMove){
            node = leaveIRMove(parent, n2);
        }else if(n2 instanceof IRName){
            node = leaveIRName(parent, n2);
        }else if(n2 instanceof IRReturn){
            node = leaveIRReturn(parent, n2);
        }else if(n2 instanceof IRSeq){
            node = leaveIRSeq(parent, n2);
        }else{
            //IRTemp
            node = leaveIRTemp(parent, n2);
        }
        assert classInv(node) : "LIR invariant violated for node "+ n2.getClass().getName() +"!";
        return node;
    }

    //chuhan
    private IRNode leaveIRBinOp(IRNode parent, IRNode n){
        IRBinOp binOpNode = (IRBinOp) n;
        ArrayList<IRStmt> s1 = new ArrayList<>();
        ArrayList<IRStmt> s2 = new ArrayList<>();
        IRExpr e1 = flatten(binOpNode.left(), s1);
        IRExpr e2 = flatten(binOpNode.right(), s2);

        boolean b = e1 instanceof IRMem || e1 instanceof IRCall || e2 instanceof IRMem || e2 instanceof IRCall;
        ArrayList<IRStmt> res = new ArrayList<>(s1);
        if(b){
            IRTemp t = IRTemp.getTemp();
            res.add(new IRMove(t, e1));
            res.addAll(s2);
            return new IRESeq(new IRSeq(res), new IRBinOp(binOpNode.opType(), t, e2));
        }else{
            res.addAll(s2);
            return new IRESeq(new IRSeq(res), new IRBinOp(binOpNode.opType(), e1, e2));
        }
    }

    //santiago
    private IRNode leaveIRCall(IRNode parent, IRNode n){
        throw new Error("leaveIRCall should not be called");
    }

    //santiago
    private IRNode leaveIRCallStmt(IRNode parent, IRNode n){
        IRCallStmt callStmt = (IRCallStmt) n;
        List<IRStmt> stmts = new ArrayList<>();
        List<IRExpr> args = new ArrayList<>();
        for(IRExpr e: ((IRCallStmt) n).args()){
            IRExpr expr = flatten(e, stmts);
            IRTemp t = IRTemp.getTemp();
            stmts.add(new IRMove(t, expr));
            args.add(t);
        }
        stmts.add(new IRCallStmt(callStmt.target(), callStmt.n_returns(), args));
        return new IRSeq(stmts);
    }

    //michael
    private IRNode leaveIRCJump(IRNode parent, IRNode n){
        IRCJump jumpNode = (IRCJump) n;
        List<IRStmt> stmts = new ArrayList<>();
        IRExpr expr = flatten(jumpNode.cond(), stmts);
        if(expr == jumpNode.cond()){
            return n;
        }else{
            stmts.add(new IRCJump(expr, jumpNode.trueLabel(), jumpNode.falseLabel()));
            return new IRSeq(stmts);
        }
    }

    private IRNode leaveIRCompUnit(IRNode parent, IRNode n){
        return n;
    }

    private IRNode leaveIRConst(IRNode parent, IRNode n){
        return n;
    }

    private IRNode leaveIRESeq(IRNode parent, IRNode n){
        IRESeq eseq = (IRESeq) n;
        ArrayList<IRStmt> res = new ArrayList<>();
        IRExpr exp = eseq.expr();
        res.add(eseq.stmt());

        // flatten exp
        IRExpr expr = flatten(exp, res);
        if(expr == exp){
            return n;
        }else{
            return new IRESeq(new IRSeq(res), expr);
        }
    }

    private IRNode leaveIRExp(IRNode parent, IRNode n){
        IRExp expNode = (IRExp) n;
        List<IRStmt> stmts = new ArrayList<>();
        IRExpr expr = flatten(expNode.expr(), stmts);
        if(expr == expNode.expr()){
            return n;
        }else{
            return new IRSeq(stmts);
        }
    }

    private IRNode leaveIRFuncDecl(IRNode parent, IRNode n){
        return n;
    }

    private IRNode leaveIRJump(IRNode parent, IRNode n){
        IRJump j = (IRJump) n;
        List<IRStmt> stmts = new ArrayList<>();
        IRExpr expr = flatten(j.target(), stmts);
        if(expr == j.target()){
            return n;
        }else{
            stmts.add(new IRJump(expr));
            return new IRSeq(stmts);
        }
    }

    private IRNode leaveIRLabel(IRNode parent, IRNode n){
        return n;
    }

    private IRNode leaveIRMem(IRNode parent, IRNode n){
        IRMem mem = (IRMem) n;
        List<IRStmt> stmts = new ArrayList<>();
        IRExpr expr = flatten(mem.expr(), stmts);
        if(expr == mem.expr()){
            return n;
        }else{
            return new IRESeq(new IRSeq(stmts), new IRMem(expr));
        }
    }

    private IRNode leaveIRMove(IRNode parent, IRNode n) {
        IRMove mov = (IRMove) n;
        List<IRStmt> s1 = new ArrayList<>();
        List<IRStmt> s2 = new ArrayList<>();
        IRExpr e1 = flatten(mov.source(), s1);
        IRExpr e2 = flatten(mov.target(), s2);

        // assume commutativity for temp
        if (e2 instanceof IRTemp) {
            if(e1==mov.source() && e2==mov.target()){
                return n;
            }else{
                s1.addAll(s2);
                s1.add(new IRMove(e2, e1));
                return new IRSeq(s1);
            }
        }

        // assume no commutativity for mem
        else{
            if(e1 instanceof IRTemp || e1 instanceof IRName || e1 instanceof IRConst){
                s1.addAll(s2);
                s1.add(new IRMove(e2, e1));
                return new IRSeq(s1);
            }
            IRMem memTarget = (IRMem) e2;
            IRTemp tmp = IRTemp.getTemp();
            s1.add(new IRMove(tmp, memTarget.expr()));
            s1.addAll(s2);
            s1.add(new IRMove(new IRMem(tmp), e1));
            return new IRSeq(s1);
        }
    }

    private IRNode leaveIRName(IRNode parent, IRNode n){
        return n;
    }

    private IRNode leaveIRReturn(IRNode parent, IRNode n){
        List<IRStmt> stmts = new ArrayList<>();
        List<IRExpr> exprs = new ArrayList<>();
        for(IRExpr expr: ((IRReturn) n).rets()){
            IRTemp t = IRTemp.getTemp();
            IRExpr e = flatten(expr, stmts);
            stmts.add(new IRMove(t, e));
            exprs.add(t);
        }
        stmts.add(new IRReturn(exprs));
        return new IRSeq(stmts);
    }

    private IRNode leaveIRSeq(IRNode parent, IRNode n){
        return n;
    }


    private IRNode leaveIRTemp(IRNode parent, IRNode n){
        return n;
    }
}
