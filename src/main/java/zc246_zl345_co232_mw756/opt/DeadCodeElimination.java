package zc246_zl345_co232_mw756.opt;

import edu.cornell.cs.cs4120.xic.ir.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeadCodeElimination extends DataFlow<IRStmt, Set<IRTemp>> {
    public static final Set<IRTemp> TOP = new HashSet<IRTemp>();
    private Map<String, IRData> dataMap;

    public DeadCodeElimination(CFG<IRStmt> cfg, Map<String, IRData> dataMap) {
        super(1, cfg);
        this.dataMap = dataMap;
    }

    public Set<IRTemp> transfer(Node<IRStmt> node, Set<IRTemp> value) {
        IRStmt s = node.stmt;
        Set<IRTemp> newVals = new HashSet<>(value);
        Set<IRTemp> uses = new HashSet<>();
        Set<IRTemp> defs = new HashSet<>();
        if (node == cfg.start) {
            if (value == TOP) {
                return new HashSet<>();
            } else {
                return value;
            }
        }
        if (s instanceof IRCallStmt) {
            // target(IRExpr), args (List<IRExpr>)
            IRCallStmt c = (IRCallStmt) s;
            uses.addAll(getTemps(c.target()));
            uses.addAll(getAllTemps(c.args()));
            for (int i = 1; i <= c.n_returns(); i++) {
                defs.add(new IRTemp("_RV" + i));
            }
        } else if (s instanceof IRCJump) {
            // cond (IRExpr)
            uses.addAll(getTemps(((IRCJump) s).cond()));
        } else if (s instanceof IRJump) {
            // target(IRExpr)
            uses.addAll(getTemps(((IRJump) s).target()));
        } else if (s instanceof IRLabel) {
            return value;
        } else if (s instanceof IRMove) {
            // target (IRExpr), src (IRExpr)
            IRMove m = (IRMove) s;
            uses.addAll(getTemps(((IRMove) s).source()));
            if ((m.target() instanceof IRMem)) {
                // expr (IRExpr)
                IRMem mem = (IRMem) m.target();
                // new addition
                uses.addAll(getTemps(mem.expr()));
            } else if ((m.target()) instanceof IRTemp) {
                defs.add((IRTemp) m.target());
            }
        } else if (s instanceof IRReturn) {
            // rets List<IRExpr>name
            uses.addAll(getAllTemps(((IRReturn) s).rets()));
        } else {
            throw new Error("Unexpected IRStmt type: " + s.getClass().getName());
        }
        // in[n] = use[n] + out[n] - def[n]
        newVals.removeIf(defs::contains);
        newVals.addAll(uses);
        return newVals;
    }

    public Set<IRTemp> top() {
        return TOP;
    }

    public Set<IRTemp> meet(Set<IRTemp> v1, Set<IRTemp> v2) {
        return union(v1, v2);
    }

    public Set<IRTemp> union(Set<IRTemp> v1, Set<IRTemp> v2) {
        Set<IRTemp> union = new HashSet<>(v1);
        union.addAll(v2);
        return union;
    }

    public Set<IRTemp> getAllTemps(List<IRExpr> args) {
        Set<IRTemp> res = new HashSet<>();
        for (IRExpr expr : args) {
            res.addAll(getTemps(expr));
        }
        return res;
    }

    public Set<IRTemp> getTemps(IRExpr expr) {
        Set<IRTemp> res = new HashSet<>();
        if (expr instanceof IRTemp) {
            res.add((IRTemp) expr);
            return res;
        } else if (expr instanceof IRName) {
            return res;
        } else if (expr instanceof IRConst) {
            return res;
        } else if (expr instanceof IRBinOp) {
            IRBinOp b = (IRBinOp) expr;
            Set<IRTemp> left = getTemps(b.left());
            Set<IRTemp> right = getTemps(b.right());
            return union(left, right);
        } else if (expr instanceof IRMem) {
            // expr (IRExpr)
            IRMem mem = (IRMem) expr;
            return getTemps(mem.expr());
        } else {
            throw new Error("Unexpected IRExpr type: " + expr.getClass().getName());
        }
    }

    @Override
    protected IRStmt convert(Node<IRStmt> node) {
        IRStmt s = node.stmt;
        if (s instanceof IRMove) {
            IRMove m = (IRMove) s;
            if ((m.target()) instanceof IRTemp) {
                IRTemp def = (IRTemp) m.target();
                if (!(out.get(node).contains(def))) {
                    // if the def is not in live out, delete node
                    converged = false;
                    return null;
                }
            }
        } else if (s instanceof IRLabel && node.prev.size() == 1 &&
                (!(node.prev.get(0).stmt instanceof IRCJump) || node.prev.get(0).next.get(0).equals(node.prev.get(0).next.get(1)))) {
            converged = false;
            return null;
        } else if (s instanceof IRJump && (node.next.get(0).prev.size() == 1
                || !(node.next.get(0).stmt instanceof IRLabel))) {
            converged = false;
            return null;
        } else {
            return s;
        }
        return s;
    }
}