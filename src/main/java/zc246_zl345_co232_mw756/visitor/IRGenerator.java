package zc246_zl345_co232_mw756.visitor;

import edu.cornell.cs.cs4120.xic.ir.*;
import edu.cornell.cs.cs4120.xic.ir.interpret.Configuration;
import zc246_zl345_co232_mw756.ast.Record;
import zc246_zl345_co232_mw756.ast.Type;
import zc246_zl345_co232_mw756.ast.*;
import zc246_zl345_co232_mw756.context.*;

import java.util.*;
import java.util.stream.Collectors;

public class IRGenerator {

    private Map<String, IRData> dataMap;
    private Map<String, IRFuncDecl> functions;
    private Stack<String> loopEnd;
    private int stringCount;

    public IRGenerator() {
        dataMap = new LinkedHashMap<>();
        functions = new LinkedHashMap<>();
        loopEnd = new Stack<>();
        stringCount = 0;
    }

    public IRNode toIR(Node node) {
        if (node instanceof ArrayLiteral) {
            return visitArrayLiteral((ArrayLiteral) node);
        } else if (node instanceof Assignment) {
            return visitAssignment((Assignment) node);
        } else if (node instanceof Block) {
            return visitBlock((Block) node);
        } else if (node instanceof Bool) {
            return visitBool((Bool) node);
        } else if (node instanceof Break) {
            return visitBreak((Break) node);
        } else if (node instanceof Char) {
            return visitChar((Char) node);
        } else if (node instanceof Declaration) {
            return visitDeclaration((Declaration) node);
        } else if (node instanceof FieldAccess) {
            return visitFieldAccess((FieldAccess) node);
        } else if (node instanceof FuncCall) {
            return visitFuncCall((FuncCall) node);
        } else if (node instanceof Function) {
            return visitFunction((Function) node);
        } else if (node instanceof Global) {
            return visitGlobal((Global) node);
        } else if (node instanceof Identifier) {
            return visitIdentifier((Identifier) node);
        } else if (node instanceof If) {
            return visitIf((If) node);
        } else if (node instanceof Int) {
            return visitInt((Int) node);
        } else if (node instanceof Null) {
            return visitNull((Null) node);
        } else if (node instanceof ProcedureCall) {
            return visitProcedureCall((ProcedureCall) node);
        } else if (node instanceof Record) {
            return visitRecord((Record) node);
        } else if (node instanceof Return) {
            return visitReturn((Return) node);
        } else if (node instanceof Str) {
            return visitStr((Str) node);
        } else if (node instanceof Type) {
            return visitType((Type) node);
        } else if (node instanceof Use) {
            return visitUse((Use) node);
        } else if (node instanceof Value) {
            return visitValue((Value) node);
        } else if (node instanceof While) {
            return visitWhile((While) node);
        } else {
            throw new Error("IRGenerator: unknown node type");
        }
    }

    private IRNode visitArrayLiteral(ArrayLiteral node) {
        List<IRStmt> stmts = new ArrayList<>();
        IRTemp array = IRTemp.getTemp();
        stmts.add(
                new IRCallStmt(
                        new IRName("_eta_alloc"), 1L,
                        new IRBinOp(
                                IRBinOp.OpType.ADD,
                                new IRConst(8),
                                new IRBinOp(
                                        IRBinOp.OpType.MUL,
                                        new IRConst(node.values.size()),
                                        new IRConst(8)))));
        stmts.add(new IRMove(array, new IRTemp("_RV1")));
        stmts.add(new IRMove(new IRMem(array), new IRConst(node.values.size())));
        for (int i = 1; i <= node.values.size(); i++) {
            stmts.add(new IRMove(
                    new IRMem(
                            new IRBinOp(
                                    IRBinOp.OpType.ADD,
                                    array,
                                    new IRBinOp(
                                            IRBinOp.OpType.MUL,
                                            new IRConst(i),
                                            new IRConst(8)))),
                    (IRExpr) toIR(node.values.get(i - 1)))
            );
        }
        return new IRESeq(new IRSeq(stmts), new IRBinOp(IRBinOp.OpType.ADD, array, new IRConst(8)));
    }

    private IRNode visitAssignment(Assignment node) {
        List<Expr> vals = node.vals;
        List<IRStmt> stmts = new ArrayList<>();
        List<IRTemp> temps = new ArrayList<>();

        for(Expr val:vals){
            if(val instanceof FuncCall){
                FuncCall fc = (FuncCall) val;
                List<IRExpr> args = new ArrayList<>();
                for (Expr e : fc.args) {
                    IRTemp t = IRTemp.getTemp();
                    stmts.add(new IRMove(t, (IRExpr) toIR(e)));
                    args.add(t);
                }
                long nRets = ((Tuple) fc.type).tuple.length;
                stmts.add(new IRCallStmt(new IRName(fName(fc.id.id, (FunctionType) fc.id.type)), nRets, args));
                for (int i = 1; i <= nRets; i++) {
                    IRTemp tmp = IRTemp.getTemp();
                    stmts.add(new IRMove(tmp, new IRTemp("_RV" + i)));
                    temps.add(tmp);
                }
            } else {
                IRExpr expr = (IRExpr) toIR(val);
                IRTemp t = IRTemp.getTemp();
                stmts.add(new IRMove(t, expr));
                temps.add(t);
            }
        }

        for (int i = 0; i < node.args.size(); i++) {
            Node arg = node.args.get(i);
            if (arg instanceof Identifier && ((Identifier) arg).id.equals("_")) {
                continue;
            } else if (arg instanceof Identifier) {
                if (dataMap.containsKey("_t" + ((Identifier) arg).id.replaceAll("'", "_"))) {
                    stmts.add(new IRMove(new IRMem(new IRName("_t" + ((Identifier) arg).id.replaceAll("'", "_"))), temps.get(i)));
                } else {
                    stmts.add(new IRMove(new IRTemp(((Identifier) arg).id), temps.get(i)));
                }
            } else if (arg instanceof Declaration) {
                Declaration d = (Declaration) arg;
                stmts.add(new IRMove(new IRTemp(d.id.id), temps.get(i)));
            } else if (arg instanceof Value){
                //array assign
                stmts.add(getSeq((Value) arg, temps.get(i)));
            } else{
                //field access
                FieldAccess fa = (FieldAccess) arg;
                long offset = fa.record.offsets.get(fa.field);
                stmts.add(new IRMove(new IRMem(
                        new IRBinOp(IRBinOp.OpType.ADD, (IRExpr) toIR(fa.expr), new IRConst(offset))), temps.get(i)));
            }
        }
        return new IRSeq(stmts);
    }

    private IRStmt getSeq(Value arg, IRExpr val) {
        // helper function for turning array access assign into a sequence
        Expr e1 = arg.children.get(0);
        Expr e2 = (arg.children.size() == 1) ? null : arg.children.get(1);
        IRTemp ta = IRTemp.getTemp();
        IRTemp ti = IRTemp.getTemp();
        IRLabel ok = IRLabel.getLabel();
        IRLabel error = IRLabel.getLabel();
        List<IRStmt> stmts = new ArrayList<>();
        stmts.add(new IRMove(ta, (IRExpr) toIR(e1)));
        stmts.add(new IRMove(ti, (IRExpr) toIR(e2)));
        stmts.add(new IRCJump(new IRBinOp(IRBinOp.OpType.ULT, ti,
                new IRMem(new IRBinOp(IRBinOp.OpType.SUB,
                        ta, new IRConst(8)))), ok.name(), error.name()));
        stmts.add(error);
        stmts.add(new IRCallStmt(new IRName("_eta_out_of_bounds"), 0L, new ArrayList<>()));
        stmts.add(ok);
        IRTemp t;
        if (val instanceof IRTemp) {
            t = (IRTemp) val;
        } else {
            t = IRTemp.getTemp();
            stmts.add(new IRMove(t, val));
        }
        stmts.add(new IRMove(new IRMem(new IRBinOp(IRBinOp.OpType.ADD, ta,
                new IRBinOp(IRBinOp.OpType.MUL, ti, new IRConst(8)))), t));
        return new IRSeq(stmts);
    }

    private IRNode visitBlock(Block node) {
        List<IRStmt> stmts = new ArrayList<>();
        for (Statement s : node.statements) {
            stmts.add((IRStmt) toIR(s));
        }
        return new IRSeq(stmts);
    }

    private IRNode visitBool(Bool node) {
        return new IRConst(node.value);
    }

    private IRNode visitBreak(Break node) {
        return new IRJump(new IRName(loopEnd.peek()));
    }

    private IRNode visitChar(Char node) {
        return new IRConst(node.value);
    }

    private IRNode visitDeclaration(Declaration node) {
        Type varType = node.varType;
        Identifier id = node.id;
        IRTemp var = new IRTemp(id.id);
        if (varType.next != null && varType.next.len != null) {
            List<IRExpr> counts = new ArrayList<>();
            List<IRStmt> stmts = new ArrayList<>();
            for (Type t = varType.next; t != null && t.len != null; t = t.next) {
                IRExpr count = (IRExpr) toIR(t.len);
                IRTemp tmp;
                if (count instanceof IRTemp) {
                    tmp = (IRTemp) count;
                } else {
                    tmp = IRTemp.getTemp();
                    stmts.add(new IRMove(tmp, count));
                }
                counts.add(tmp);
            }
            stmts.add(new IRMove(var, createArray(varType.next, counts, 0)));
            return new IRSeq(stmts);
        } else {
            return new IRMove(var, new IRConst(0));
        }
    }

    private static IRExpr createArray(Type varType, List<IRExpr> counts, int index) {
        List<IRStmt> stmts = new ArrayList<>();
        IRExpr count = counts.get(index);
        IRExpr array = malloc_array(count);
        IRTemp t = IRTemp.getTemp();
        stmts.add(new IRMove(t, array));
        IRTemp tmp = IRTemp.getTemp();
        IRLabel lh = IRLabel.getLabel();
        IRLabel l1 = IRLabel.getLabel();
        IRLabel le = IRLabel.getLabel();
        stmts.add(new IRMove(tmp, new IRConst(0)));
        stmts.add(lh);
        stmts.add(new IRCJump(new IRBinOp(IRBinOp.OpType.LT, tmp, count), l1.name(), le.name()));
        stmts.add(l1);
        IRExpr maybeArray;
        if (varType.next != null && varType.next.len != null) {
            maybeArray = createArray(varType.next, counts, index + 1);
        } else {
            maybeArray = new IRConst(0L);
        }
        stmts.add(new IRMove(new IRMem(
                new IRBinOp(IRBinOp.OpType.ADD, t,
                        new IRBinOp(IRBinOp.OpType.MUL, tmp, new IRConst(8)))), maybeArray));
        stmts.add(new IRMove(tmp, new IRBinOp(IRBinOp.OpType.ADD, tmp, new IRConst(1))));
        stmts.add(new IRJump(new IRName(lh.name())));
        stmts.add(le);
        return new IRESeq(new IRSeq(stmts), t);
    }

    private IRNode visitFieldAccess(FieldAccess node) {
        IRTemp ret = IRTemp.getTemp();
        List<IRStmt> stmts = new ArrayList<>();
        stmts.add(new IRMove(ret, new IRMem(new IRBinOp(IRBinOp.OpType.ADD, (IRExpr) toIR(node.expr),
                new IRConst(node.record.offsets.get(node.field))))));
        return new IRESeq(new IRSeq(stmts), ret);
    }

    private IRNode visitFuncCall(FuncCall node) {
        // assumes only return 1 value, multiassigncall implemented separately
        List<IRStmt> stmts = new ArrayList<>();
        List<IRExpr> args = prepareArgs(node.args, stmts);
        IRTemp tmp = IRTemp.getTemp();
        stmts.add(new IRCallStmt(new IRName(fName(node.id.id, (FunctionType) node.id.type)), 1L, args));
        stmts.add(new IRMove(tmp, new IRTemp(Configuration.ABSTRACT_RET_PREFIX + 1)));
        return new IRESeq(new IRSeq(stmts), tmp);
    }

    private List<IRExpr> prepareArgs(List<Expr> args, List<IRStmt> stmts) {
        List<IRExpr> irArgs = new ArrayList<>();
        for (Expr e : args) {
            IRTemp tmp = IRTemp.getTemp();
            stmts.add(new IRMove(tmp, (IRExpr) toIR(e)));
            irArgs.add(tmp);
        }
        return irArgs;
    }

    private IRNode visitFunction(Function node) {
        Identifier id = node.id;
        List<Declaration> params = node.params;
        List<Type> return_types = node.return_types;
        Block body = node.body;
        List<IRStmt> stmts = new ArrayList<>();
        stmts.add(new IRLabel(id.id));
        for (int i = 0; i < params.size(); i++) {
            Declaration dec = params.get(i);
            stmts.add(new IRMove(new IRTemp(dec.id.id), new IRTemp(Configuration.ABSTRACT_ARG_PREFIX + (i + 1))));
        }
        stmts.add((IRStmt) toIR(body));
        if (body.statements.size() == 0 || (return_types.size() == 0 && body.statements.get(body.statements.size() - 1).type == StatementType.UNIT)) {
            stmts.add(new IRReturn());
        }
        IRFuncDecl func = new IRFuncDecl(fName(id.id, (FunctionType) node.type), new IRSeq(stmts));
        functions.put(id.id, func);
        return null;
    }

    private String fName(String id, FunctionType type) {
        PrimitiveType[] rets = type.rets;
        StringBuilder sb = new StringBuilder("_I");
        sb.append(id.replaceAll("_", "__")).append("_");
        // add return types to function name
        if (rets.length == 0) {
            sb.append("p");
        } else {
            if (rets.length > 1) {
                sb.append("t").append(rets.length);
            }
            for (PrimitiveType ret : rets) {
                getName(sb, ret);
            }
        }
        for (PrimitiveType param : type.args) {
            getName(sb, param);
        }
        return sb.toString();
    }

    private void getName(StringBuilder sb, PrimitiveType t) {
        String primitiveType;
        switch (t.primitive) {
            case INT:
                primitiveType = "i";
                break;
            case BOOL:
                primitiveType = "b";
                break;
            default:
                RecordType r = (RecordType) t;
                String name = r.record.name.id;
                primitiveType = "r" + name.length() + name;
                break;
        }
        sb.append("a".repeat(t.arrayDepth)).append(primitiveType);
    }

    private IRNode visitGlobal(Global node) {
        long[] gData = new long[1];
        if (node.value instanceof Bool) {
            gData[0] = ((Bool) node.value).value;
        } else if (node.value instanceof Int) {
            gData[0] = ((Int) node.value).value;
        }
        IRData globalVar = new IRData(node.id.id.replaceAll("'", "_"), gData);
        dataMap.put("_t" + node.id.id.replaceAll("'", "_"), globalVar);
        return null;
    }

    private IRNode visitIdentifier(Identifier node) {
        String id = node.id;
        if (dataMap.containsKey("_t" + id.replaceAll("'", "_")))
            return new IRMem(new IRName("_t" + id.replaceAll("'", "_")));
        return new IRTemp(id);
    }

    private IRNode visitIf(If node) {
        Expr guard = node.guard;
        Statement body = node.body;
        Statement elseBody = node.elseBody;
        List<IRStmt> stmts = new ArrayList<>();
        IRLabel l1 = IRLabel.getLabel();
        IRLabel l2 = IRLabel.getLabel();
        IRLabel l3 = IRLabel.getLabel();
        stmts.add(control(guard, l1, l2));
        stmts.add(l1);
        stmts.add((IRStmt) toIR(body));
        stmts.add(new IRJump(new IRName(l3.name())));
        stmts.add(l2);
        if (elseBody != null) {
            stmts.add((IRStmt) toIR(elseBody));
        }
        stmts.add(l3);
        return new IRSeq(stmts);
    }

    private IRStmt control(Expr e, IRLabel t, IRLabel f) {
        if (e instanceof Bool) {
            if (((Bool) e).value == 1) {
                return new IRJump(new IRName(t.name()));
            } else {
                return new IRJump(new IRName(f.name()));
            }
        } else if (e instanceof Value) {
            Value v = (Value) e;
            if (v.op == Value.Op.NOT_LOG) {
                return control(v.children.get(0), f, t);
            } else if (v.op == Value.Op.AND) {
                Expr c1 = v.children.get(0);
                Expr c2 = v.children.get(1);
                List<IRStmt> stmts = new ArrayList<>();
                IRLabel l1 = IRLabel.getLabel();
                stmts.add(control(c1, l1, f));
                stmts.add(l1);
                stmts.add(control(c2, t, f));
                return new IRSeq(stmts);
            } else if (v.op == Value.Op.OR) {
                Expr c1 = v.children.get(0);
                Expr c2 = v.children.get(1);
                List<IRStmt> stmts = new ArrayList<>();
                IRLabel l1 = IRLabel.getLabel();
                stmts.add(control(c1, t, l1));
                stmts.add(l1);
                stmts.add(control(c2, t, f));
                return new IRSeq(stmts);
            }
        }
        return new IRCJump((IRExpr) toIR(e), t.name(), f.name());
    }

    private IRNode visitInt(Int node) {
        return new IRConst(node.value);
    }

    private IRNode visitNull(Null node) {
        return new IRConst(0L);
    }

    private IRNode visitProcedureCall(ProcedureCall node) {
        Identifier id = node.id;
        List<IRStmt> stmts = new ArrayList<>();
        List<IRExpr> args = prepareArgs(node.args, stmts);
        stmts.add(new IRCallStmt(new IRName(fName(id.id, (FunctionType) id.type)), 0L, args));
        return new IRSeq(stmts);
    }

    public IRCompUnit visitProgram(Program node, String name) {
        // ignore [uses]
        List<Definition> defs = node.defs;
        for (Definition d : defs) {
            if(d instanceof Global){
                toIR(d);
            }
        }
        for (Definition d : defs) {
            if(!(d instanceof Global)){
                toIR(d);
            }
        }
        return new IRCompUnit(name, functions, new ArrayList<>(), dataMap);
    }

    private IRNode visitRecord(Record node) {
        List<Declaration> fields = node.fields;
        List<IRStmt> stmts = new ArrayList<>();
        stmts.add(new IRLabel(node.name.id));
        IRTemp loc = IRTemp.getTemp();
        stmts.add(new IRMove(loc, malloc(new IRConst(fields.size()))));
        for (int i = 0; i < fields.size(); i++) {
            Declaration dec = fields.get(i);
            stmts.add(new IRMove(new IRMem(new IRBinOp(IRBinOp.OpType.ADD, loc,
                    new IRBinOp(IRBinOp.OpType.MUL, new IRConst(8), new IRConst(i)))),
                    new IRTemp(Configuration.ABSTRACT_ARG_PREFIX + (i + 1))));
        }
        stmts.add(new IRReturn(new ArrayList<>(List.of(loc))));
        functions.put(node.name.id, new IRFuncDecl(fName(node.name.id, node.fType), new IRSeq(stmts)));
        return null;
    }

    private IRNode visitReturn(Return node) {
        List<IRExpr> rets = node.values.stream().map(e -> (IRExpr) toIR(e)).collect(Collectors.toList());
        return new IRReturn(new ArrayList<>(rets));
    }

    private IRNode visitStr(Str node) {
        Long[] value = node.value;
        long[] s = new long[value.length + 1];
        for (int i = 0; i < value.length; i++) {
            s[i + 1] = value[i];
        }
        s[0] = value.length;
        String name = getStringName();
        IRData d = new IRData(name, s);
        dataMap.put(name, d);
        IRTemp t1 = IRTemp.getTemp();
        List<IRStmt> stmts = new ArrayList<>();
        stmts.add(new IRMove(t1, malloc_array(new IRConst(value.length))));
        stmts.add(copy(t1,
                new IRBinOp(IRBinOp.OpType.ADD, new IRName(name), new IRConst(8)),
                new IRConst(value.length)));
        return new IRESeq(new IRSeq(stmts), t1);
    }

    private static IRExpr malloc(IRExpr numElem) {
        List<IRStmt> stmts = new ArrayList<>();
        IRTemp loc = IRTemp.getTemp();
        stmts.add(
                new IRCallStmt(
                        new IRName("_eta_alloc"), 1L,
                        new IRBinOp(IRBinOp.OpType.MUL, numElem, new IRConst(8))));
        stmts.add(new IRMove(loc, new IRTemp("_RV1")));
        return new IRESeq(new IRSeq(stmts), loc);
    }

    private static IRExpr malloc_array(IRExpr numElem) {
        List<IRStmt> stmts = new ArrayList<>();
        IRTemp array = IRTemp.getTemp();
        stmts.add(new IRMove(array, malloc(new IRBinOp(IRBinOp.OpType.ADD, numElem, new IRConst(1L)))));
        stmts.add(new IRMove(new IRMem(array), numElem));
        return new IRESeq(new IRSeq(stmts), new IRBinOp(IRBinOp.OpType.ADD, array, new IRConst(8)));
    }

    private String getStringName() {
        return "_string_const" + ++stringCount;
    }

    private IRNode visitType(Type node) {
        throw new Error("IRGenerator: visitType not implemented");
    }

    private IRNode visitUse(Use node) {
        throw new Error("IRGenerator: visitUse not implemented");
    }

    private IRNode visitValue(Value node) {
        List<Expr> children = node.children;
        Value.Op op = node.op;
        Expr e1 = children.get(0);
        Expr e2 = (children.size() == 1) ? null : children.get(1);
        switch (op) {
            case PLUS:
                if (e1.type instanceof PrimitiveType && ((PrimitiveType) e1.type).arrayDepth == 0
                        || e1.type instanceof Tuple && ((Tuple) e1.type).tuple[0].arrayDepth == 0)
                    return new IRBinOp(IRBinOp.OpType.ADD, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
                else
                    return arrayConcat((IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case MINUS:
                if (children.size() == 1)
                    return new IRBinOp(IRBinOp.OpType.SUB, new IRConst(0), (IRExpr) toIR(e1));
                else
                    return new IRBinOp(IRBinOp.OpType.SUB, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case TIMES:
                return new IRBinOp(IRBinOp.OpType.MUL, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case DIVIDE:
                return new IRBinOp(IRBinOp.OpType.DIV, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case MODULO:
                return new IRBinOp(IRBinOp.OpType.MOD, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case HIGHMUL:
                return new IRBinOp(IRBinOp.OpType.HMUL, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case OR:
                IRTemp tmp = IRTemp.getTemp();
                IRLabel t = IRLabel.getLabel();
                IRLabel f = IRLabel.getLabel();
                return new IRESeq(new IRSeq(
                        new IRMove(tmp, new IRConst(0)),
                        control(node, t, f),
                        t,
                        new IRMove(tmp, new IRConst(1)),
                        f
                ), tmp);
            case AND:
                tmp = IRTemp.getTemp();
                t = IRLabel.getLabel();
                f = IRLabel.getLabel();
                return new IRESeq(new IRSeq(
                        new IRMove(tmp, new IRConst(0)),
                        control(node, t, f),
                        t,
                        new IRMove(tmp, new IRConst(1)),
                        f
                ), tmp);
            case LT:
                return new IRBinOp(IRBinOp.OpType.LT, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case LEQ:
                return new IRBinOp(IRBinOp.OpType.LEQ, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case GT:
                return new IRBinOp(IRBinOp.OpType.GT, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case GEQ:
                return new IRBinOp(IRBinOp.OpType.GEQ, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case EQUAL:
                return new IRBinOp(IRBinOp.OpType.EQ, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case NOT_EQUAL:
                return new IRBinOp(IRBinOp.OpType.NEQ, (IRExpr) toIR(e1), (IRExpr) toIR(e2));
            case NOT_LOG:
                return new IRBinOp(IRBinOp.OpType.XOR, (IRExpr) toIR(e1), new IRConst(1));
            case ARRAY_ACCESS:
                IRTemp ta = IRTemp.getTemp();
                IRTemp ti = IRTemp.getTemp();
                IRLabel ok = IRLabel.getLabel();
                IRLabel error = IRLabel.getLabel();
                return new IRESeq(new IRSeq(
                        new IRMove(ta, (IRExpr) toIR(e1)),
                        new IRMove(ti, (IRExpr) toIR(e2)),
                        new IRCJump(new IRBinOp(IRBinOp.OpType.ULT, ti, new IRMem(new IRBinOp(IRBinOp.OpType.SUB, ta, new IRConst(8)))), ok.name(), error.name()),
                        error,
                        new IRCallStmt(new IRName("_eta_out_of_bounds"), 0L, new ArrayList<>()),
                        ok
                ), new IRMem(new IRBinOp(IRBinOp.OpType.ADD, ta, new IRBinOp(IRBinOp.OpType.MUL, ti, new IRConst(8)))));
            default:
                //length
                return new IRMem(new IRBinOp(IRBinOp.OpType.SUB, (IRExpr) toIR(e1), new IRConst(8)));
        }
    }

    private IRExpr arrayConcat(IRExpr e1, IRExpr e2) {
        IRTemp a1 = IRTemp.getTemp();
        IRTemp a2 = IRTemp.getTemp();
        List<IRStmt> stmts = new ArrayList<>();
        stmts.add(new IRMove(a1, e1));
        stmts.add(new IRMove(a2, e2));
        IRTemp size1 = IRTemp.getTemp();
        stmts.add(new IRMove(size1, new IRMem(new IRBinOp(IRBinOp.OpType.SUB, a1, new IRConst(8)))));
        IRTemp size2 = IRTemp.getTemp();
        stmts.add(new IRMove(size2, new IRMem(new IRBinOp(IRBinOp.OpType.SUB, a2, new IRConst(8)))));
        IRExpr size = new IRBinOp(IRBinOp.OpType.ADD, size1, size2);
        IRTemp array = IRTemp.getTemp();
        stmts.add(new IRMove(array, malloc_array(size)));
        stmts.add(copy(array, a1, size1));
        stmts.add(copy(new IRBinOp(IRBinOp.OpType.ADD, array,
                new IRBinOp(IRBinOp.OpType.MUL, size1, new IRConst(8))), a2, size2));
        return new IRESeq(new IRSeq(stmts), array);
    }

    private static IRStmt copy(IRExpr dest, IRExpr src, IRExpr size) {
        List<IRStmt> stmts = new ArrayList<>();
        IRTemp counter = IRTemp.getTemp();
        stmts.add(new IRMove(counter, new IRConst(0)));
        IRLabel lh = IRLabel.getLabel();
        IRLabel l1 = IRLabel.getLabel();
        IRLabel le = IRLabel.getLabel();
        stmts.add(lh);
        stmts.add(new IRCJump(new IRBinOp(IRBinOp.OpType.LT, counter, size), l1.name(), le.name()));
        stmts.add(l1);
        IRTemp addr = IRTemp.getTemp();
        stmts.add(new IRMove(addr, new IRBinOp(IRBinOp.OpType.MUL, counter, new IRConst(8))));
        IRTemp s = IRTemp.getTemp();
        stmts.add(new IRMove(s, new IRMem(new IRBinOp(IRBinOp.OpType.ADD, src, addr))));
        stmts.add(new IRMove(
                new IRMem(new IRBinOp(IRBinOp.OpType.ADD, dest, addr)),
                s));
        stmts.add(new IRMove(counter, new IRBinOp(IRBinOp.OpType.ADD, counter, new IRConst(1))));
        stmts.add(new IRJump(new IRName(lh.name())));
        stmts.add(le);
        return new IRSeq(stmts);
    }

    private IRNode visitWhile(While node) {
        Expr guard = node.guard;
        Statement body = node.body;
        List<IRStmt> stmts = new ArrayList<>();
        IRLabel lh = IRLabel.getLabel();
        IRLabel l1 = IRLabel.getLabel();
        IRLabel le = IRLabel.getLabel();
        stmts.add(lh);
        stmts.add(control(guard, l1, le));
        stmts.add(l1);
        loopEnd.push(le.name());
        stmts.add((IRStmt) toIR(body));
        loopEnd.pop();
        stmts.add(new IRJump(new IRName(lh.name())));
        stmts.add(le);
        return new IRSeq(stmts);
    }
}
