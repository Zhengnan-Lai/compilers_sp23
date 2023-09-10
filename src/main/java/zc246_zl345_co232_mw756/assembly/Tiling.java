package zc246_zl345_co232_mw756.assembly;

import edu.cornell.cs.cs4120.xic.ir.*;
import edu.cornell.cs.cs4120.xic.ir.visit.UsedArgs;

import static zc246_zl345_co232_mw756.assembly.Register.ARGUMENTS;

import java.util.*;

public class Tiling {
    private int n_ret;
    private final boolean debug;
    private static final Register RV3_addr = new Register("_RV3_addr");
    private static final IRConst ONE = new IRConst(1);
    private static final IRConst ZERO = new IRConst(0);

    public Tiling() {
        n_ret = 0;
        debug = false;
    }

    public Tiling(boolean debug) {
        n_ret = 0;
        this.debug = debug;
    }

    /**
     * @param n
     * @param dest If the node is an IRExpr, put the result in dest. dest CAN be null!
     * @return
     */
    public List<Assembly> tile(IRNode n, Register dest) {
        if (n instanceof IRBinOp) {
            return tileBinOp((IRBinOp) n, dest);
        } else if (n instanceof IRCall) {
            return tileCall((IRCall) n, dest);
        } else if (n instanceof IRCallStmt) {
            return tileCallStmt((IRCallStmt) n);
        } else if (n instanceof IRCJump) {
            return tileCJump((IRCJump) n);
        } else if (n instanceof IRConst) {
            return tileConst((IRConst) n, dest);
        } else if (n instanceof IRESeq) {
            return tileESeq((IRESeq) n);
        } else if (n instanceof IRExp) {
            return tileExp((IRExp) n);
        } else if (n instanceof IRFuncDecl) {
            return tileFuncDecl((IRFuncDecl) n);
        } else if (n instanceof IRJump) {
            return tileJump((IRJump) n);
        } else if (n instanceof IRLabel) {
            return tileLabel((IRLabel) n);
        } else if (n instanceof IRMem) {
            return tileMem((IRMem) n, dest);
        } else if (n instanceof IRMove) {
            return tileMove((IRMove) n);
        } else if (n instanceof IRName) {
            return tileName((IRName) n, dest);
        } else if (n instanceof IRReturn) {
            return tileReturn((IRReturn) n);
        } else if (n instanceof IRSeq) {
            return tileSeq((IRSeq) n);
        } else {
            //IRTemp
            return tileTemp((IRTemp) n, dest);
        }
    }

    private List<Assembly> tileBinOp(IRBinOp binOp, Register dest) {
        List<Assembly> ret = new ArrayList<>();
        if (binOp.opType() == IRBinOp.OpType.ADD) {
            IRExpr left = binOp.left();
            IRExpr right = binOp.right();
            if ((left instanceof IRTemp || left instanceof IRMem) &&
                    (right instanceof IRConst || right instanceof IRTemp || right instanceof IRMem)) {
                tileBOP(binOp, ret, BOP.BOPType.ADD, dest);
            } else {
                ret.add(new Lea(dest, new Mem(mem(ret, binOp))));
            }
        } else if (binOp.opType() == IRBinOp.OpType.SUB) {
            tileBOP(binOp, ret, BOP.BOPType.SUB, dest);
        } else if (binOp.opType() == IRBinOp.OpType.MUL) {
            tileBOP(binOp, ret, BOP.BOPType.IMUL, dest);
        } else if (binOp.opType() == IRBinOp.OpType.DIV) {
            Register left = Register.getReg();
            Register right = Register.getReg();
            ret.addAll(tile(binOp.left(), left));
            ret.addAll(tile(binOp.right(), right));
            ret.add(new Mov(Register.RAX, left));
            ret.add(new CQO());
            ret.add(new UOP(UOP.Type.IDIV, right));
            ret.add(new Mov(dest != null ? dest : left, Register.RAX));
        } else if (binOp.opType() == IRBinOp.OpType.MOD) {
            Register left = Register.getReg();
            Register right = Register.getReg();
            ret.addAll(tile(binOp.left(), left));
            ret.addAll(tile(binOp.right(), right));
            ret.add(new Mov(Register.RAX, left));
            ret.add(new CQO());
            ret.add(new UOP(UOP.Type.IDIV, right));
            ret.add(new Mov(dest != null ? dest : left, Register.RDX));
        } else if (binOp.opType() == IRBinOp.OpType.HMUL) {
            Register left = Register.getReg();
            Register right = Register.getReg();
            ret.addAll(tile(binOp.left(), left));
            ret.addAll(tile(binOp.right(), right));
            ret.add(new Mov(Register.RAX, left));
            ret.add(new UOP(UOP.Type.IMUL, right));
            ret.add(new Mov(dest != null ? dest : left, Register.RDX));
        } else if (binOp.opType() == IRBinOp.OpType.AND) {
            tileBOP(binOp, ret, BOP.BOPType.AND, dest);
        } else if (binOp.opType() == IRBinOp.OpType.OR) {
            tileBOP(binOp, ret, BOP.BOPType.OR, dest);
        } else if (binOp.opType() == IRBinOp.OpType.XOR) {
            tileBOP(binOp, ret, BOP.BOPType.XOR, dest);
        } else if (binOp.opType() == IRBinOp.OpType.LSHIFT) {
            tileBOP(binOp, ret, BOP.BOPType.SHL, dest);
        } else if (binOp.opType() == IRBinOp.OpType.RSHIFT) {
            tileBOP(binOp, ret, BOP.BOPType.SHR, dest);
        } else if (binOp.opType() == IRBinOp.OpType.ARSHIFT) {
            tileBOP(binOp, ret, BOP.BOPType.SAR, dest);
        } else if (binOp.opType() == IRBinOp.OpType.EQ) {
            tileCompare(binOp, ret, Set.Type.SETZ, dest);
        } else if (binOp.opType() == IRBinOp.OpType.NEQ) {
            tileCompare(binOp, ret, Set.Type.SETNZ, dest);
        } else if (binOp.opType() == IRBinOp.OpType.LT) {
            tileCompare(binOp, ret, Set.Type.SETL, dest);
        } else if (binOp.opType() == IRBinOp.OpType.GT) {
            tileCompare(binOp, ret, Set.Type.SETG, dest);
        } else if (binOp.opType() == IRBinOp.OpType.LEQ) {
            tileCompare(binOp, ret, Set.Type.SETLE, dest);
        } else if (binOp.opType() == IRBinOp.OpType.GEQ) {
            tileCompare(binOp, ret, Set.Type.SETGE, dest);
        } else if (binOp.opType() == IRBinOp.OpType.ULT) {
            tileCompare(binOp, ret, Set.Type.SETB, dest);
        } else {
            throw new Error("Unknown binop type: " + binOp.opType());
        }
        return ret;
    }

    private void tileCompare(IRBinOp binOp, List<Assembly> ret, Set.Type op, Register dest) {
        Register left = dest == null ? Register.getReg() : dest;
        Expr right = Register.getReg();
        ret.addAll(tile(binOp.left(), left));
        if (binOp.right() instanceof IRConst) {
            IRConst br = (IRConst) binOp.right();
            if(br.value()<= Integer.MAX_VALUE && br.value() >= Integer.MIN_VALUE){
                right = new Const((IRConst) binOp.right());
            }else{
                ret.add(new Mov(right, new Const((IRConst) binOp.right())));
            }
        } else if (binOp.right() instanceof IRTemp) {
            right = Register.getReg((IRTemp) binOp.right());
        } else {
            ret.addAll(tile(binOp.right(), (Register) right));
        }
        ret.add(new Cmp(left, right));
        ret.add(new Set(left, op));
    }

    private void tileBOP(IRBinOp binOp, List<Assembly> ret, BOP.BOPType op, Register dest) {
        Register left = dest == null ? Register.getReg() : dest;
        Expr right = Register.getReg();
        boolean move = false;
        if (binOp.right() instanceof IRTemp && dest != null && dest.getName().equals(((IRTemp) binOp.right()).name())) {
            left = Register.getReg();
            ret.addAll(tile(binOp.left(), left));
            move = true;
        }
        ret.addAll(tile(binOp.left(), left));
        if (binOp.right() instanceof IRConst) {
            IRConst br = (IRConst) binOp.right();
            if(br.value()<= Integer.MAX_VALUE && br.value() >= Integer.MIN_VALUE){
                right = new Const((IRConst) binOp.right());
            }else{
                ret.add(new Mov(right, new Const((IRConst) binOp.right())));
            }
        } else if (binOp.right() instanceof IRTemp) {
            right = Register.getReg((IRTemp) binOp.right());
        } else {
            ret.addAll(tile(binOp.right(), (Register) right));
        }
        ret.add(new BOP(op, left, right));
        if (move) {
            ret.add(new Mov(dest, left));
        }
    }

    //[base, index, scale, displacement]
    //[0], [1] are IRTemps, [2], [3] are IRConsts
    //[0], [1] can be null, [2], [3] can never be null
    private IRExpr[] mem(List<Assembly> codes, IRExpr expr) {
        if (expr instanceof IRConst) {
            IRConst constExpr = (IRConst) expr;
            if(constExpr.value()<= Integer.MAX_VALUE && constExpr.value() >= Integer.MIN_VALUE){
                return new IRExpr[]{null, null, ONE, constExpr};
            }else{
                Register temp = Register.getReg();
                codes.add(new Mov(temp, new Const(constExpr)));
                return new IRExpr[]{temp.getTemp(), null, ONE, ZERO};
            }
        } else if (expr instanceof IRBinOp) {
            IRBinOp binOp = (IRBinOp) expr;
            IRExpr left = binOp.left();
            IRExpr right = binOp.right();
            if (binOp.opType() == IRBinOp.OpType.ADD) {
                IRExpr[] leftResult = mem(codes, left);
                IRExpr[] rightResult = mem(codes, right);
                if (leftResult[0] != null && rightResult[0] != null) {
                    if (leftResult[1] != null || rightResult[1] != null) {
                        //can't merge, add code
                        return merge(leftResult, rightResult, BOP.BOPType.ADD, codes);
                    } else {
                        return new IRExpr[]{leftResult[0], rightResult[0], ONE, new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    }
                } else if (leftResult[1] != null && rightResult[1] != null) {
                    if ((!leftResult[2].equals(ONE) && !rightResult[2].equals(ONE) && !leftResult[2].equals(rightResult[2])) || (leftResult[0] != null || rightResult[0] != null)) {
                        return merge(leftResult, rightResult, BOP.BOPType.ADD, codes);
                    } else if (!leftResult[2].equals(ONE) && leftResult[2].equals(rightResult[2])) {
                        codes.add(new BOP(BOP.BOPType.ADD, Register.getReg((IRTemp) leftResult[1]), Register.getReg((IRTemp) rightResult[1])));
                        return new IRExpr[]{null, leftResult[1], leftResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    } else if (leftResult[2].equals(ONE)) {
                        return new IRExpr[]{leftResult[1], rightResult[1], rightResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    } else {
                        return new IRExpr[]{rightResult[1], leftResult[1], leftResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    }
                } else {
                    if (leftResult[0] == null && leftResult[1] == null) {
                        return new IRExpr[]{rightResult[0], rightResult[1], rightResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    } else if (rightResult[0] == null && rightResult[1] == null) {
                        return new IRExpr[]{leftResult[0], leftResult[1], leftResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    } else if (leftResult[0] != null) {
                        return new IRExpr[]{leftResult[0], rightResult[1], rightResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    } else {
                        return new IRExpr[]{rightResult[0], leftResult[1], leftResult[2], new IRConst(((IRConst) leftResult[3]).value() + ((IRConst) rightResult[3]).value())};
                    }
                }
            } else if (binOp.opType() == IRBinOp.OpType.MUL) {
                if (left instanceof IRConst && right instanceof IRConst) {
                    return new IRExpr[]{null, null, ONE, new IRConst(((IRConst) left).value() * ((IRConst) right).value())};
                } else if (isScale(left)) {
                    IRExpr[] rightResult = mem(codes, right);
                    if (right instanceof IRTemp) {
                        return new IRExpr[]{null, right, left, ZERO};
                    } else {
                        Register r = Register.getReg();
                        codes.add(new Lea(r, new Mem(rightResult)));
                        return new IRExpr[]{null, r.getTemp(), left, ZERO};
                    }
                } else if (isScale(right)) {
                    IRExpr[] leftResult = mem(codes, left);
                    if (left instanceof IRTemp) {
                        return new IRExpr[]{null, left, right, ZERO};
                    } else {
                        Register r = Register.getReg();
                        codes.add(new Lea(r, new Mem(leftResult)));
                        return new IRExpr[]{null, r.getTemp(), right, ZERO};
                    }
                }
            } else if (binOp.opType() == IRBinOp.OpType.SUB) {
                IRExpr[] leftResult = mem(codes, left);
                IRExpr[] rightResult = mem(codes, right);
                if (binOp.right() instanceof IRConst) {
                    return new IRExpr[]{leftResult[0], leftResult[1], leftResult[2], new IRConst(((IRConst) leftResult[3]).value() - ((IRConst) rightResult[3]).value())};
                } else {
                    return merge(leftResult, rightResult, BOP.BOPType.SUB, codes);
                }
            }
        } else if (expr instanceof IRTemp) {
            return new IRExpr[]{expr, null, ONE, ZERO};
        }
        Register r = Register.getReg();
        codes.addAll(tile(expr, r));
        return new IRExpr[]{r.getTemp(), null, ONE, ZERO};
    }

    private IRExpr[] merge(IRExpr[] left, IRExpr[] right, BOP.BOPType type, List<Assembly> codes) {
        Register l;
        if (left[0] != null && left[1] == null && left[2].equals(ONE) && left[3].equals(ZERO)) {
            l = Register.getReg((IRTemp) left[0]);
        } else if (left[0] == null && left[1] == null && left[2].equals(ONE)) {
            l = Register.getReg();
            codes.add(new Mov(l, new Const(((IRConst) left[3]))));
        } else {
            l = Register.getReg();
            codes.add(new Lea(l, new Mem(left)));
        }
        Expr r;
        if (right[0] != null && right[1] == null && left[2].equals(ONE) && left[3].equals(ZERO)) {
            r = Register.getReg((IRTemp) right[0]);
        } else if (right[0] != null && right[1] == null && left[2].equals(ONE)) {
            r = new Const(((IRConst) left[3]));
        } else {
            r = Register.getReg();
            codes.add(new Lea((Register) r, new Mem(right)));
        }
        codes.add(new BOP(type, l, r));
        return new IRExpr[]{l.getTemp(), null, ONE, ZERO};
    }

    private boolean isScale(IRExpr e) {
        if (e instanceof IRConst) {
            long value = ((IRConst) e).value();
            return Arrays.asList(1L, 2L, 4L, 8L).contains(value);
        }
        return false;
    }

    private List<Assembly> tileCall(IRCall call, Register dest) {
        throw new Error("tileCall should not be called");
    }

    //santiago
    private List<Assembly> tileCallStmt(IRCallStmt callStmt) {
        List<Assembly> assembly = new ArrayList<>();
        List<Register> regs = new ArrayList<>(6);
        Register[] params = ARGUMENTS.toArray(new Register[6]);
        Register[] rets = {Register.RAX, Register.RDX};
        int argSize = callStmt.args().size();
        Long retSize = callStmt.n_returns();

        long onStack = Math.max(0, retSize - 2);
        onStack = (onStack == 0 ? Math.max(argSize - 6, 0) : Math.max(argSize - 5, 0)) + onStack;
        if (onStack % 2 != 0) {
            Register tmp = Register.getReg();
            assembly.add(new Mov(tmp, new Const(8L)));
            assembly.add(new BOP(BOP.BOPType.AND, tmp, Register.RSP));
            assembly.add(new Set(tmp, Set.Type.SETZ));
            assembly.add(new BOP(BOP.BOPType.SHL, tmp, new Const(3L)));
            assembly.add(new BOP(BOP.BOPType.SUB, Register.RSP, tmp));
        } else {
            assembly.add(new BOP(BOP.BOPType.AND, Register.RSP, new Const(-16L)));
        }
        if (retSize <= 2) {
            for (int i = 0; i < Math.min(6, argSize); i++) {
                IRExpr expr = callStmt.args().get(i);
                Register reg = Register.getReg();
                regs.add(reg);
                assembly.addAll(tile(expr, reg));
            }
            for (int i = argSize - 1; i >= 6; i--) {
                IRExpr expr = callStmt.args().get(i);
                Register reg = Register.getReg();
                assembly.addAll(tile(expr, reg));
                assembly.add(new Push(reg));
            }
            for (int i = Math.min(6, argSize) - 1; i >= 0; i--) {
                assembly.add(new Mov(params[i], regs.get(i)));
            }
            assembly.add(new Call(((IRName) callStmt.target()).name(), argSize, n_ret));
            for (int i = 0; i < retSize; i++) {
                assembly.add(new Mov(new Register("_RV" + (i + 1)), rets[i]));
            }
            if (argSize > 6) {
                assembly.add(new BOP(BOP.BOPType.ADD, Register.RSP, new Const(8L * (argSize - 6))));
            }
        } else {
            for (int i = 0; i < Math.min(5, argSize); i++) {
                IRExpr expr = callStmt.args().get(i);
                Register reg = Register.getReg();
                regs.add(reg);
                assembly.addAll(tile(expr, reg));
            }
            assembly.add(new BOP(BOP.BOPType.SUB, Register.RSP, new Const(8 * (retSize - 2))));
            assembly.add(new Mov(Register.RDI, Register.RSP));
            for (int i = argSize - 1; i >= 5; i--) {
                IRExpr expr = callStmt.args().get(i);
                if(expr instanceof IRConst && ((IRConst) expr).value()<= Integer.MAX_VALUE && ((IRConst) expr).value()>= Integer.MIN_VALUE){
                    assembly.add(new Push(new Const((IRConst) expr)));
                }else {
                    Register reg = Register.getReg();
                    assembly.addAll(tile(expr, reg));
                    assembly.add(new Push(reg));
                }
            }
            for (int i = Math.min(5, argSize) - 1; i >= 0; i--) {
                assembly.add(new Mov(params[i + 1], regs.get(i)));
            }
            assembly.add(new Call(((IRName) callStmt.target()).name(), argSize, n_ret));
            if (argSize > 5) {
                assembly.add(new BOP(BOP.BOPType.ADD, Register.RSP, new Const(8L * (argSize - 5))));
            }
            assembly.add(new Mov(new Register("_RV1"), rets[0]));
            assembly.add(new Mov(new Register("_RV2"), rets[1]));
            for (int i = 2; i < retSize; i++) {
                assembly.add(new Pop(new Register("_RV" + (i + 1))));
            }
        }
        return assembly;
    }

    private List<Assembly> tileCJump(IRCJump cJump) {
        List<Assembly> codes = new ArrayList<>();
        Register r;
        if (cJump.cond() instanceof IRTemp) {
            r = Register.getReg((IRTemp) cJump.cond());
            codes.add(new Cmp(r, new Const(1)));
            codes.add(new Jump(Jump.JumpType.JE, cJump.trueLabel()));
        } else if (cJump.cond() instanceof IRMem) {
            codes.add(new Cmp(new Mem(mem(codes, ((IRMem) cJump.cond()).expr())), new Const(1)));
            codes.add(new Jump(Jump.JumpType.JE, cJump.trueLabel()));
        } else if (cJump.cond() instanceof IRConst){
            if(((IRConst) cJump.cond()).value() == 1) {
                codes.add(new Jump(Jump.JumpType.JMP, cJump.trueLabel()));
            }
        }else {
            // IRBinOp
            IRBinOp binOp = (IRBinOp) cJump.cond();
            if (binOp.opType() == IRBinOp.OpType.EQ || binOp.opType() == IRBinOp.OpType.NEQ ||
                    binOp.opType() == IRBinOp.OpType.LT || binOp.opType() == IRBinOp.OpType.LEQ ||
                    binOp.opType() == IRBinOp.OpType.GT || binOp.opType() == IRBinOp.OpType.GEQ ||
                    binOp.opType() == IRBinOp.OpType.ULT) {
                Register left = Register.getReg();
                Expr right = Register.getReg();
                if (binOp.left() instanceof IRConst) {
                    codes.add(new Mov(left, new Const((IRConst) binOp.left())));
                } else if (binOp.left() instanceof IRTemp) {
                    left = Register.getReg((IRTemp) binOp.left());
                } else {
                    codes.addAll(tile(binOp.left(), left));
                }
                if (binOp.right() instanceof IRConst) {
                    IRConst rightConst = (IRConst) binOp.right();
                    if(rightConst.value()>= Integer.MIN_VALUE && rightConst.value()<= Integer.MAX_VALUE){
                        right = new Const(rightConst);
                    }else {
                        codes.add(new Mov(right, new Const(rightConst)));
                    }
                } else if (binOp.right() instanceof IRTemp) {
                    right = Register.getReg((IRTemp) binOp.right());
                } else if (binOp.right() instanceof IRMem) {
                    right = new Mem(mem(codes, ((IRMem) binOp.right()).expr()));
                } else {
                    codes.addAll(tile(binOp.right(), (Register) right));
                }
                codes.add(new Cmp(left, right));
                switch (binOp.opType()) {
                    case EQ:
                        codes.add(new Jump(Jump.JumpType.JZ, cJump.trueLabel()));
                        break;
                    case NEQ:
                        codes.add(new Jump(Jump.JumpType.JNZ, cJump.trueLabel()));
                        break;
                    case LT:
                        codes.add(new Jump(Jump.JumpType.JL, cJump.trueLabel()));
                        break;
                    case GT:
                        codes.add(new Jump(Jump.JumpType.JG, cJump.trueLabel()));
                        break;
                    case LEQ:
                        codes.add(new Jump(Jump.JumpType.JLE, cJump.trueLabel()));
                        break;
                    case GEQ:
                        codes.add(new Jump(Jump.JumpType.JGE, cJump.trueLabel()));
                        break;
                    case ULT:
                        codes.add(new Jump(Jump.JumpType.JB, cJump.trueLabel()));
                        break;
                    default:
                        throw new Error("Unexpected binop type");
                }
            } else {
                codes.addAll(tile(binOp, null));
                codes.add(new Jump(Jump.JumpType.JNZ, cJump.trueLabel()));
            }
        }
        return codes;
    }

    //chuhan
    public AACompUnit tileCompUnit(IRCompUnit compUnit) {

        Map<String, Function> functions = new LinkedHashMap<>();
        for (Map.Entry<String, IRFuncDecl> function : compUnit.functions().entrySet()) {
            functions.put(function.getKey(), new Function(function.getKey(), tileFuncDecl(function.getValue())));
        }

        Map<String, Data> dataMap = new LinkedHashMap<>();
        for (Map.Entry<String, IRData> data : compUnit.dataMap().entrySet()) {
            String name = data.getKey();
            dataMap.put(name, new Data(name, data.getValue().data()));
        }
        return new AACompUnit(compUnit.name(), functions, dataMap);
    }

    private List<Assembly> tileConst(IRConst constant, Register dest) {
        if (dest == null) throw new Error("tileConst: dest should not be null");
        return List.of(new Mov(dest, new Const(constant)));
    }

    private List<Assembly> tileESeq(IRESeq eSeq) {
        throw new Error("tileESeq should not be called");
    }

    private List<Assembly> tileExp(IRExp exp) {
        throw new Error("tileExp should not be called");
    }

    private List<Assembly> tileFuncDecl(IRFuncDecl funcDecl) {
        List<Assembly> assembly = new ArrayList<>(tile(funcDecl.body(), null));
        UsedArgs ua = new UsedArgs();
        java.util.Set<Integer> ints = ua.visit(funcDecl);

        List<Assembly> ret = new ArrayList<>();
        Register[] params = ARGUMENTS.toArray(new Register[6]);
        if (n_ret <= 2) {
            for(int i:ints){
                if(i<=6){
                    ret.add(new Mov(new Register("_ARG" + i), params[i-1]));
                }else{
                    ret.add(new Mov(new Register("_ARG" + i),
                            new Mem(Register.RBP, null, new Const(1), new Const(8 * (i - 5L)))));
                }
            }
        } else {
            ret.add(new Mov(RV3_addr, Register.RDI));
            for(int i:ints){
                if(i<=5){
                    ret.add(new Mov(new Register("_ARG" + i), params[i]));
                }else{
                    ret.add(new Mov(new Register("_ARG" + i),
                            new Mem(Register.RBP, null, new Const(1), new Const(8 * (i - 4L)))));
                }
            }
        }
        ret.addAll(assembly);
        return ret;
    }

    private List<Assembly> tileJump(IRJump jump) {
        List<Assembly> assembly = new ArrayList<>();
        assembly.add(new Jump(Jump.JumpType.JMP, ((IRName) jump.target()).name()));
        return assembly;
    }

    private List<Assembly> tileLabel(IRLabel label) {
        List<Assembly> assembly = new ArrayList<>();
        assembly.add(new Label(label.name()));
        return assembly;
    }

    private List<Assembly> tileMem(IRMem mem, Register dest) {
        List<Assembly> assembly = new ArrayList<>();
        dest = dest == null ? Register.getReg() : dest;
        assembly.add(new Mov(dest, new Mem(mem(assembly, mem.expr()))));
        return assembly;
    }

    private List<Assembly> tileMove(IRMove move) {
        List<Assembly> assembly = new ArrayList<>();
        if (move.target() instanceof IRTemp) {
            if (move.source() instanceof IRMem && ((IRMem) move.source()).expr() instanceof IRName) {
                String label = ((IRName) ((IRMem) move.source()).expr()).name();
                assembly.add(new Mov(Register.getReg((IRTemp) move.target()), new Offset(label)));
            } else if (move.source() instanceof IRBinOp
                    && (((IRBinOp) move.source()).left().equals(move.target()) && ((IRBinOp) move.source()).right().equals(new IRConst(1))
                    || ((IRBinOp) move.source()).right().equals(move.target()) && ((IRBinOp) move.source()).left().equals(new IRConst(1)))
                    && ((IRBinOp) move.source()).opType() == IRBinOp.OpType.ADD) {
                assembly.add(new Inc(Register.getReg((IRTemp) move.target())));
            } else if (move.source() instanceof IRBinOp && ((IRBinOp) move.source()).left() instanceof IRTemp
                    && ((IRBinOp) move.source()).left().equals(move.target())
                    && ((IRBinOp) move.source()).right().equals(new IRConst(1))
                    && ((IRBinOp) move.source()).opType() == IRBinOp.OpType.SUB) {
                assembly.add(new Dec(Register.getReg((IRTemp) move.target())));
            } else {
                assembly.addAll(tile(move.source(), Register.getReg((IRTemp) move.target())));
            }
        } else {
            // target is mem
            IRMem tar = (IRMem) move.target();
            if (tar.expr() instanceof IRName) {
                String label = ((IRName) tar.expr()).name();
                if (move.source() instanceof IRTemp) {
                    assembly.add(new Mov(new Offset(label), Register.getReg((IRTemp) move.source())));
                } else if (move.source() instanceof IRConst && ((IRConst) move.source()).value() <= Integer.MAX_VALUE && ((IRConst) move.source()).value() >= Integer.MIN_VALUE) {
                    assembly.add(new Mov(new Offset(label), new Const((IRConst) move.source())));
                } else {
                    Register r = Register.getReg();
                    assembly.addAll(tile(move.source(), r));
                    assembly.add(new Mov(new Offset(label), r));
                }

            } else {
                Expr target = new Mem(mem(assembly, ((IRMem) move.target()).expr()));
                if (move.source() instanceof IRConst && ((IRConst) move.source()).value() <= Integer.MAX_VALUE && ((IRConst) move.source()).value() >= Integer.MIN_VALUE) {
                    assembly.add(new Mov(target, new Const((IRConst) move.source())));
                } else if (move.source() instanceof IRTemp) {
                    assembly.add(new Mov(target, Register.getReg((IRTemp) move.source())));
                } else {
                    Register r = Register.getReg();
                    assembly.addAll(tile(move.source(), r));
                    assembly.add(new Mov(target, r));
                }
            }
        }
        return assembly;
    }

    private List<Assembly> tileName(IRName name, Register dest) {
        List<Assembly> assembly = new ArrayList<>();
        dest = dest == null ? Register.getReg() : dest;
        String n = name.name();
        assembly.add(new Lea(dest, new Offset(n)));
        return assembly;
    }

    //michael
    private List<Assembly> tileReturn(IRReturn ret) {
        List<Assembly> assembly = new ArrayList<>();
        List<Register> regs = new ArrayList<>();

        int retSize = ret.rets().size();
        n_ret = retSize;
        for (IRExpr expr : ret.rets()) {
            Register reg = Register.getReg();
            regs.add(reg);
            assembly.addAll(tile(expr, reg));
        }

        if (retSize > 0) assembly.add(new Mov(Register.RAX, regs.get(0)));
        if (retSize > 1) assembly.add(new Mov(Register.RDX, regs.get(1)));
        if (retSize > 2) {
            assembly.add(new Mov(Register.RDI, RV3_addr));
            for (int i = 2; i < retSize; i++) {
                assembly.add(new Mov(new Mem(Register.RDI, null, new Const(1), new Const((i - 2L) * 8)), regs.get(i)));
            }
        }

        assembly.add(new Leave());
        assembly.add(new Ret(n_ret));

        return assembly;
    }

    private List<Assembly> tileSeq(IRSeq seq) {
        List<Assembly> assembly = new ArrayList<>();
        for (IRStmt stmt : seq.stmts()) {
            if (debug)
                assembly.add(new Comment(stmt.toString().substring(0, stmt.toString().length() - 1)));
            assembly.addAll(tile(stmt, null));
        }
        return assembly;
    }

    private List<Assembly> tileTemp(IRTemp temp, Register dest) {
        if (dest != null && temp.name().equals(dest.getName())) {
            return new ArrayList<>();
        } else {
            Register d = dest == null ? Register.getReg() : dest;
            return List.of(new Mov(d, Register.getReg(temp)));
        }
    }
}