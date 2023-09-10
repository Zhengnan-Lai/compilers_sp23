package zc246_zl345_co232_mw756.opt;

import edu.cornell.cs.cs4120.xic.ir.*;
import edu.cornell.cs.cs4120.xic.ir.visit.ConstFolding;

import java.util.*;


public class ConstProp extends DataFlow<IRStmt, HashMap<IRTemp, Object>> {
    // Null for top element; long for some constant value of variable; BOTTOM for bottom element
    enum Bot {BOTTOM}
    private final ConstFolding constFold = new ConstFolding(new IRNodeFactory_c());

    public ConstProp(CFG<IRStmt> cfg) {
        super(0, cfg);
    }

    public HashMap<IRTemp, Object> transfer(Node<IRStmt> node, HashMap<IRTemp, Object> value) {
        HashMap<IRTemp, Object> ret = new HashMap<>(value);
        if (node.stmt instanceof IRMove && ((IRMove) node.stmt).target() instanceof IRTemp) {
            IRTemp dest = (IRTemp) ((IRMove) node.stmt).target();
            if (((IRMove) node.stmt).source() instanceof IRTemp
                    && (((IRTemp) ((IRMove) node.stmt).source()).name().startsWith("_RV")
                    || ((IRTemp) ((IRMove) node.stmt).source()).name().startsWith("_ARG")))
                ret.put(dest, Bot.BOTTOM);
            else{
                Object v = evaluate(((IRMove) node.stmt).source(), value);
                if (v == null) ret.remove(dest);
                else if (v instanceof Bot) ret.put(dest, Bot.BOTTOM);
                else ret.put(dest, v);
            }
        }
        return ret;
    }

    public HashMap<IRTemp, Object> top() {
        return new HashMap<>();
    }

    public HashMap<IRTemp, Object> meet(HashMap<IRTemp, Object> v1, HashMap<IRTemp, Object> v2) {
        HashMap<IRTemp, Object> ret = new HashMap<>();
        Set<IRTemp> keys = new HashSet<>(v1.keySet());
        keys.addAll(v2.keySet());
        for (IRTemp t : keys) {
            Object o1 = v1.get(t);
            Object o2 = v2.get(t);
            if (o1 instanceof Bot || o2 instanceof Bot) ret.put(t, Bot.BOTTOM);
            else if (o1 instanceof Long && o2 instanceof Long) {
                if (o1.equals(o2)) ret.put(t, o1);
                else ret.put(t, Bot.BOTTOM);
            } else if (o1 instanceof Long) ret.put(t, o1);
            else if (o2 instanceof Long) ret.put(t, o2);
        }
        return ret;
    }

    protected IRStmt convert(Node<IRStmt> node) {
        IRStmt stmt = node.stmt;
        HashMap<IRTemp, Object> value = in.get(node);
        if (stmt instanceof IRMove) {
            if (((IRMove) stmt).target() instanceof IRTemp)
                stmt = new IRMove(((IRMove) stmt).target(), substitute(((IRMove) stmt).source(), value));
            else stmt = new IRMove(substitute(((IRMove) stmt).target(), value), substitute(((IRMove) stmt).source(), value));
        } else if (stmt instanceof IRCallStmt) {
            List<IRExpr> args = new ArrayList<>();
            for (IRExpr e : ((IRCallStmt) stmt).args()) {
                args.add(substitute(e, value));
            }
            stmt = new IRCallStmt(substitute(((IRCallStmt) stmt).target(), value), ((IRCallStmt) stmt).n_returns(), args);
        } else if (stmt instanceof IRJump) {
            stmt = new IRJump(substitute(((IRJump) stmt).target(), value));
        } else if (stmt instanceof IRCJump) {
            IRExpr sub = substitute(((IRCJump) stmt).cond(), value);
            if(sub.equals(new IRConst(0L))){
                if(node.next.get(0).stmt instanceof IRLabel && ((IRLabel)node.next.get(0).stmt).name().equals(((IRCJump) stmt).trueLabel())){
                    node.next.remove(0);
                }else{
                    node.next.remove(1);
                }
                return null;
            }else if(sub.equals(new IRConst(1L))){
                if(node.next.get(0).stmt instanceof IRLabel && ((IRLabel)node.next.get(0).stmt).name().equals(((IRCJump) stmt).trueLabel())){
                    node.next.remove(1);
                }else{
                    node.next.remove(0);
                }
                stmt = new IRJump(new IRName(((IRCJump) stmt).trueLabel()));
            }
            else stmt = new IRCJump(sub, ((IRCJump) stmt).trueLabel(), ((IRCJump) stmt).falseLabel());
        } else if (stmt instanceof IRReturn) {
            List<IRExpr> rets = new ArrayList<>();
            for (IRExpr e : ((IRReturn) stmt).rets()) {
                rets.add(substitute(e, value));
            }
            stmt = new IRReturn(rets);
        }
        stmt = (IRStmt) constFold.visit(stmt);
        return stmt;
    }

    private IRExpr substitute(IRExpr expr, HashMap<IRTemp, Object> value) {
        if (expr instanceof IRBinOp) {
            return new IRBinOp(((IRBinOp) expr).opType(), substitute(((IRBinOp) expr).left(), value), substitute(((IRBinOp) expr).right(), value));
        } else if (expr instanceof IRMem) {
            return new IRMem(substitute(((IRMem) expr).expr(), value), ((IRMem) expr).memType());
        } else if (expr instanceof IRTemp) {
            Object c = value.get((IRTemp) expr);
            converged = converged && !(c instanceof Long);
            return c instanceof Long ? new IRConst((Long) c) : expr;
        }
        // IRName, IRConst: no change
        return expr;
    }

    /**
     * Evaluate an IRExpr to constant if possible
     */
    private Object evaluate(IRExpr expr, HashMap<IRTemp, Object> value) {
        if (expr instanceof IRBinOp) {
            Object v1 = evaluate(((IRBinOp) expr).left(), value);
            Object v2 = evaluate(((IRBinOp) expr).right(), value);
            if (v1 instanceof Bot || v2 instanceof Bot) return Bot.BOTTOM;
            if (v1 == null || v2 == null) return null;
            long n1 = (Long) v1, n2 = (Long) v2;
            switch (((IRBinOp) expr).opType()) {
                case EQ:
                    return n1 == n2 ? 1L : 0L;
                case NEQ:
                    return n1 != n2 ? 1L : 0L;
                case GT:
                    return n1 > n2 ? 1L : 0L;
                case GEQ:
                    return n1 >= n2 ? 1L : 0L;
                case LT:
                    return n1 < n2 ? 1L : 0L;
                case LEQ:
                    return n1 <= n2 ? 1L : 0L;
                case ADD:
                    return n1 + n2;
                case SUB:
                    return n1 - n2;
                case MUL:
                    return n1 * n2;
                case DIV:
                    if (n2 == 0L) throw new Error("Divide by zero");
                    return n1 / n2;
                case MOD:
                    if (n2 == 0L) throw new Error("Modulo by zero");
                    return n1 % n2;
                case AND:
                    return n1 & n2;
                case OR:
                    return n1 | n2;
                case XOR:
                    return n1 ^ n2;
                case LSHIFT:
                    return n1 << n2;
                case RSHIFT:
                    return n1 >>> n2;
                case ARSHIFT:
                    return n1 >> n2;
                case ULT:
                    return Long.compareUnsigned(n1, n2);
                case HMUL:
                    return Math.multiplyHigh(n1, n2);
                default:
            }
        } else if (expr instanceof IRTemp) {
            if (((IRTemp) expr).name().contains("_RV") || ((IRTemp) expr).name().contains("_ARG") ) return Bot.BOTTOM;
            return value.get((IRTemp) expr);
        } else if (expr instanceof IRConst) {
            return (((IRConst) expr).value());
        }
        // IRMem
        return Bot.BOTTOM;
    }
}
