package edu.cornell.cs.cs4120.xic.ir.visit;

import edu.cornell.cs.cs4120.xic.ir.IRFuncDecl;
import edu.cornell.cs.cs4120.xic.ir.IRNode;
import edu.cornell.cs.cs4120.xic.ir.IRSeq;
import edu.cornell.cs.cs4120.xic.ir.IRStmt;

import java.util.ArrayList;
import java.util.List;

public class SeqRemover extends AggregateVisitor<List<IRStmt>> {

    @Override
    protected List<IRStmt> leave(IRNode parent, IRNode n, List<IRStmt> r, AggregateVisitor<List<IRStmt>> v_) {
        if(n instanceof IRSeq){
            return r;
        }else if(n instanceof IRStmt){
            r.add((IRStmt)n);
            return r;
        }else if(n instanceof IRFuncDecl){
            IRFuncDecl func = (IRFuncDecl) n;
            func.setBody(new IRSeq(r));
        }
        return unit();
    }

    @Override
    public List<IRStmt> unit() {
        return new ArrayList<>();
    }

    @Override
    public List<IRStmt> bind(List<IRStmt> r1, List<IRStmt> r2) {
        List<IRStmt> stmts = new ArrayList<>(r1);
        stmts.addAll(r2);
        return stmts;
    }
}

