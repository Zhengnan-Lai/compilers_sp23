package zc246_zl345_co232_mw756.opt;

import edu.cornell.cs.cs4120.xic.ir.*;

import java.util.*;
import java.util.stream.Collectors;

public class CopyProp extends DataFlow<IRStmt, Map<IRTemp, IRTemp>>{

    public static final HashMap<IRTemp, IRTemp> TOP = null;

    public CopyProp(CFG<IRStmt> cfg) {
        super(0, cfg);
    }

    @Override
    public Map<IRTemp, IRTemp> transfer(Node<IRStmt> node, Map<IRTemp, IRTemp> value) {
        IRStmt s = node.stmt;
        if(node == cfg.start){
            if(value == TOP) return new HashMap<>();
            else return value;
        }
        if(s instanceof IRCallStmt){
            if (value == TOP) return TOP;
            Map<IRTemp, IRTemp> newVal = new HashMap<>(value);
            newVal.entrySet().removeIf(entry -> entry.getKey().name().startsWith("_RV") || entry.getValue().name().startsWith("_RV"));
            return newVal;
        }else if(s instanceof IRCJump){
            return value;
        }else if(s instanceof IRJump){
            return value;
        }else if(s instanceof IRLabel){
            return value;
        }else if(s instanceof IRMove){
            IRMove m = (IRMove) s;
            if (!(m.target() instanceof IRMem)) {
                if (value == TOP) return TOP;
                Map<IRTemp, IRTemp> newVal = new HashMap<>(value);
                newVal.entrySet().removeIf(entry -> entry.getKey().equals(m.target()) || entry.getValue().equals(m.target()));
                if (m.source() instanceof IRTemp) {
                    newVal.put((IRTemp) m.target(), (IRTemp) m.source());
                }
                return newVal;
            }
            return value;
        }else if(s instanceof IRReturn){
            return value;
        }else{
            throw new Error("Unexpected IRStmt type: " + s.getClass().getName());
        }
    }

    @Override
    public HashMap<IRTemp, IRTemp> top() {
        return TOP;
    }

    @Override
    public Map<IRTemp, IRTemp> meet(Map<IRTemp, IRTemp> v1, Map<IRTemp, IRTemp> v2) {
        if(v1 == TOP) return v2;
        if(v2 == TOP) return v1;
        Set<Map.Entry<IRTemp, IRTemp>> ret = new HashSet<>(v1.entrySet());
        ret.retainAll(v2.entrySet());
        return setToMap(ret);
    }

    @Override
    protected IRStmt convert(Node<IRStmt> node) {
        IRStmt s = node.stmt;
        if(s instanceof IRCallStmt){
            IRCallStmt c = (IRCallStmt) s;
            List<IRExpr> args = new ArrayList<>();
            for(IRExpr e : c.args()){
                args.add(convertExpr(node, e));
            }
            return new IRCallStmt(c.target(), c.n_returns(), args);
        }else if(s instanceof IRCJump){
            return new IRCJump(convertExpr(node, ((IRCJump) s).cond()), ((IRCJump) s).trueLabel());
        }else if(s instanceof IRJump){
            return s;
        }else if(s instanceof IRLabel){
            return s;
        }else if(s instanceof IRMove){
            IRMove m = (IRMove) s;
            IRExpr target = (m.target() instanceof IRTemp)? m.target():convertExpr(node, m.target());
            IRExpr src = convertExpr(node, m.source());
            if(target == m.target() && src == m.source()) return s;
            else return new IRMove(target, src);
        }else if(s instanceof IRReturn){
            List<IRExpr> rets = new ArrayList<>();
            for(IRExpr e : ((IRReturn) s).rets()){
                rets.add(convertExpr(node, e));
            }
            return new IRReturn(rets);
        }else{
            throw new Error("Unexpected IRStmt type: " + s.getClass().getName());
        }
    }

    private IRExpr convertExpr(Node<IRStmt> node, IRExpr subexpr){
        if(subexpr instanceof IRTemp){
            Map<IRTemp, IRTemp> ins = in.get(node);
            IRTemp t = (IRTemp) subexpr;
            while(ins.containsKey(t)){
                t = ins.get(t);
            }
            converged = converged && t == subexpr;
            return t;
        }else if(subexpr instanceof IRName){
            return subexpr;
        }else if(subexpr instanceof IRConst){
            return subexpr;
        }else if(subexpr instanceof IRBinOp){
            IRBinOp b = (IRBinOp) subexpr;
            IRExpr left = convertExpr(node, b.left());
            IRExpr right = convertExpr(node, b.right());
            if(left == b.left() && right == b.right()) return subexpr;
            else return new IRBinOp(b.opType(), left, right);
        }else if(subexpr instanceof IRMem) {
            IRMem m = (IRMem) subexpr;
            IRExpr e = convertExpr(node, m.expr());
            if (e == m.expr()) return subexpr;
            else return new IRMem(e);
        }else{
            throw new Error("Unexpected IRExpr type: " + subexpr.getClass().getName());
        }
    }

    private Map<IRTemp, IRTemp> setToMap(Set<Map.Entry<IRTemp, IRTemp>> set){
        return set.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
