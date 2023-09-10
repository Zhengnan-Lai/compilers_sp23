package zc246_zl345_co232_mw756.visitor;

import zc246_zl345_co232_mw756.ast.Record;
import zc246_zl345_co232_mw756.ast.Type;
import zc246_zl345_co232_mw756.ast.*;
import zc246_zl345_co232_mw756.context.*;
import zc246_zl345_co232_mw756.errors.SemanticError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static zc246_zl345_co232_mw756.context.Context.prefix;
import static zc246_zl345_co232_mw756.context.PrimitiveType.*;

public class TypeChecker extends Visitor {
    private final Context context;
    private final HashMap<String, Program> interfaces;
    private final HashMap<String, Record> records;
    private boolean enteredWhile;
    private boolean enteredInterface;
    private String progName;

    public TypeChecker(HashMap<String, Program> interfaces, String progName) {
        this.context = new Context();
        this.interfaces = interfaces;
        this.records = new HashMap<>();
        this.enteredWhile = false;
        this.enteredInterface = false;
        this.progName = progName;
    }

    public void visitArrayLiteral(ArrayLiteral arrayLiteral) {
        int s = arrayLiteral.getValues().size();
        if (arrayLiteral.getValues().size() == 0) {
            arrayLiteral.type = new PrimitiveType(ANY.primitive, 1);
            return;
        }
        zc246_zl345_co232_mw756.context.Type type;
        arrayLiteral.getValues().get(0).accept(this);
        if (arrayLiteral.getValues().get(0) instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) arrayLiteral.getValues().get(0).type).tuple;
            if (rets.length != 1)
                throw new SemanticError(arrayLiteral.getValues().get(0).lineNumber, arrayLiteral.getValues().get(0).columnNumber,
                        "Mismatched number of values");
            type = rets[0];
        }
        else type = arrayLiteral.getValues().get(0).type;
        for (int i = 1; i < s; i++) {
            arrayLiteral.getValues().get(i).accept(this);
            zc246_zl345_co232_mw756.context.Type new_type;
            if (arrayLiteral.getValues().get(i) instanceof FuncCall) {
                PrimitiveType[] rets = ((Tuple) arrayLiteral.getValues().get(i).type).tuple;
                if (rets.length != 1)
                    throw new SemanticError(arrayLiteral.getValues().get(i).lineNumber, arrayLiteral.getValues().get(i).columnNumber,
                            "Mismatched number of values");
                new_type = rets[0];
            }
            else new_type = arrayLiteral.getValues().get(0).type;
            if (!new_type.equals(type))
                throw new SemanticError(arrayLiteral.lineNumber, arrayLiteral.columnNumber, "Array literal has different types of elements.");
        }
        if (type instanceof RecordType){
            RecordType recordType = (RecordType) type;
            arrayLiteral.type = new RecordType(recordType.fieldNames, null, recordType.record, recordType.arrayDepth+1);
        }
        else{
            arrayLiteral.type = new PrimitiveType(((PrimitiveType) type).primitive, ((PrimitiveType) type).arrayDepth + 1);
        }
    }

    public void visitAssignment(Assignment assignment) {
        Node first = assignment.args.get(0);
        int lineNumber, columnNumber;
        if (first instanceof Declaration) {
            lineNumber = ((Declaration) first).id.lineNumber;
            columnNumber = ((Declaration) first).id.columnNumber;
        } else if (first instanceof Identifier) {
            lineNumber = first.lineNumber;
            columnNumber = first.columnNumber;
        } else if (first instanceof Value){
            first.accept(this);
            lineNumber = ((Value) first).children.get(0).lineNumber;
            columnNumber = ((Value) first).children.get(0).columnNumber;
        } else if (first instanceof FieldAccess){
            first.accept(this);
            lineNumber = first.lineNumber;
            columnNumber = first.columnNumber;
        } else{
            throw new SemanticError(assignment.args.get(0).lineNumber, assignment.args.get(0).columnNumber,
                    "Variable being assigned to is not a lvalue");
        }
        List<PrimitiveType> actual = new ArrayList<>();
        for (int i = 0; i < assignment.vals.size(); i++) {
            assignment.vals.get(i).accept(this);
            if (assignment.vals.get(i) instanceof FuncCall) {
                PrimitiveType[] rets = ((Tuple) assignment.vals.get(i).type).tuple;
                actual.addAll(Arrays.asList(rets));
            } else {
                actual.add((PrimitiveType) assignment.vals.get(i).type);
            }
        }
        List<PrimitiveType> args = new ArrayList<>();
        for (Node arg : assignment.args) {
            arg.accept(this);
            if (arg instanceof Declaration) args.add((PrimitiveType) ((Declaration) arg).varType.type);
            else args.add((PrimitiveType) arg.type);
        }
        if (args.size() != actual.size())
            throw new SemanticError(lineNumber, columnNumber, "Mismatched number of values");
        for (int i = 0; i < actual.size(); i++) {
            if (args.get(i) instanceof RecordType){
                if (!(actual.get(i) instanceof RecordType)){
                    throw new SemanticError(lineNumber, columnNumber,
                            "Cannot assign " + actual.get(i) + " to " + args.get(i));
                }
                if(!((RecordType) args.get(i)).equalPrefix(actual.get(i))){
                    throw new SemanticError(lineNumber, columnNumber,
                            "Cannot assign " + actual.get(i) + " to " + args.get(i));
                }
            }
            else if (!args.get(i).equals(actual.get(i))){
                throw new SemanticError(lineNumber, columnNumber,
                        "Cannot assign " + actual.get(i) + " to " + args.get(i));
            }
        }
        assignment.type = StatementType.UNIT;
    }

    public void visitBlock(Block block) {
        context.newContext();
        if (block.statements.size() > 0) {
            for (int i = 0; i < block.statements.size() - 1; i++) {
                Statement s = block.statements.get(i);
                s.accept(this);
                if (s.type != StatementType.UNIT)
                    throw new SemanticError(block.lineNumber, block.columnNumber, "Unreachable statement in block");
            }
            Statement s = block.statements.get(block.statements.size() - 1);
            s.accept(this);
            block.type = s.type;
        } else {
            block.type = StatementType.UNIT;
        }
        context.endContext();
    }

    public void visitBool(Bool bool) {
        bool.type = BOOL;
    }

    @Override
    public void visitBreak(Break breakNode) {
        if (!enteredWhile){
            throw new SemanticError(breakNode.lineNumber, breakNode.columnNumber,
                    "Break not inside a while statement");
        }
        breakNode.type = StatementType.VOID;
    }

    public void visitChar(Char c) {
        c.type = INT;
    }

    public void visitDeclaration(Declaration declaration) {
        declaration.varType.accept(this);
        zc246_zl345_co232_mw756.context.Type declaredType = declaration.varType.type;
        if (!declaration.id.id.equals("_"))
            context.put(declaration.id, declaredType);
        declaration.type = StatementType.UNIT;
    }

    @Override
    public void visitFieldAccess(FieldAccess fieldAccess) {
        fieldAccess.expr.accept(this);
        zc246_zl345_co232_mw756.context.Type actualType = fieldAccess.expr.type;
        if (!(actualType instanceof PrimitiveType)){
            throw new SemanticError(fieldAccess.expr.lineNumber, fieldAccess.field.columnNumber,
                    "Expected record but find: " + actualType);
        }
        int fieldIndex = -1;
        for (int i = 0; i < ((RecordType) actualType).fieldNames.length; i++) {
            if (((RecordType) actualType).fieldNames[i].equals(fieldAccess.field.id)) {
                fieldIndex = i;
            }
        }
        if (fieldIndex == -1){
            throw new SemanticError(fieldAccess.expr.lineNumber, fieldAccess.field.columnNumber,
                    "Field: " + fieldAccess.field.id + " is not valid in this record");
        }
        fieldAccess.type = ((RecordType) actualType).fieldTypes[fieldIndex];
        fieldAccess.record = ((RecordType) actualType).record;
    }

    public void visitFuncCall(FuncCall funcCall) {
        // check [funcCall] is in context
        zc246_zl345_co232_mw756.context.Type funcFromMap = context.get(funcCall.id);
        funcCall.id.type = funcFromMap;
        // cast to [FunctionType] and check number of arguments
        if(!(funcFromMap instanceof FunctionType))
            throw new SemanticError(funcCall.id.lineNumber, funcCall.id.columnNumber,
                    "Expected function but found variable");
        FunctionType actualFunc = (FunctionType) funcFromMap;
        // make an arraylist for getargs
        ArrayList<PrimitiveType> args = new ArrayList<>();
        for (int i = 0; i < funcCall.args.size(); i++) {
            funcCall.args.get(i).accept(this);
            if (funcCall.args.get(i) instanceof FuncCall) {
                PrimitiveType[] rets = ((Tuple) funcCall.args.get(i).type).tuple;
                args.addAll(Arrays.asList(rets));
            } else {
                args.add((PrimitiveType) funcCall.args.get(i).type);
            }
        }
        if (args.size() != actualFunc.args.length) {
            throw new SemanticError(funcCall.id.lineNumber, funcCall.id.columnNumber, "Mismatched number of values");
        }
        // check if the types of the arguments are the same as the types of the parameters
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i) instanceof RecordType){
                if (!(actualFunc.args[i] instanceof RecordType)){
                    throw new SemanticError(funcCall.id.lineNumber, funcCall.id.columnNumber,
                            "Expected " + actualFunc.args[i] + ", but found " + args.get(i));
                }
                if(!((RecordType) args.get(i)).equalPrefix((RecordType) actualFunc.args[i])){
                    boolean eq = ((RecordType) args.get(i)).equalPrefix((RecordType) actualFunc.args[i]);
                    throw new SemanticError(funcCall.id.lineNumber, funcCall.id.columnNumber,
                            "Expected " + actualFunc.args[i] + ", but found " + args.get(i));
                }
            }
            else if (!(args.get(i).equals(actualFunc.args[i]))) {
                throw new SemanticError(funcCall.id.lineNumber, funcCall.id.columnNumber,
                        "Expected " + actualFunc.args[i] + ", but found " + args.get(i));
            }
        }
        // check that funcCall is not a procedure
        if (actualFunc.rets.length == 0) {
            throw new SemanticError(funcCall.id.lineNumber, funcCall.id.columnNumber, "Function " +
                    funcCall.id.id + " is a procedure.");
        }
        funcCall.type = new Tuple(actualFunc.rets);
    }

    public void visitFunction(Function function) {
        context.newContext();
        for (Declaration d : function.params) {
            d.accept(this);
        }
        if (function.body != null) {
            function.body.accept(this);
            function.type = context.get(function.id);
            PrimitiveType[] rets = ((FunctionType) function.type).rets;
            if (rets.length != 0 && function.body.type == StatementType.UNIT)
                throw new SemanticError(function.body.lineNumber, function.body.columnNumber, "Missing return");
            function.body.checkReturn(rets);
        }
        context.endContext();
    }

    public void visitGlobal(Global global) {
        if (global.value != null) {
            global.value.accept(this);
            zc246_zl345_co232_mw756.context.Type declaredType = global.varType.type;
            zc246_zl345_co232_mw756.context.Type actualType = global.value.type;
            if (!declaredType.equals(actualType)) {
                throw new SemanticError(global.id.lineNumber, global.id.columnNumber,
                        "Expected " + declaredType + ", but found " + actualType);
            }
        }
    }

    public void visitIdentifier(Identifier identifier) {
        if (identifier.id.equals("_")) {
            identifier.type = ANY;
            return;
        }
        identifier.type = context.get(identifier);
    }

    public void visitIf(If ifNode) {
        context.newContext();
        ifNode.guard.accept(this);
        if (ifNode.guard instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) ifNode.guard.type).tuple;
            if (rets.length != 1 || !rets[0].equals(BOOL))
                throw new SemanticError(ifNode.guard.lineNumber, ifNode.guard.columnNumber, "Expected bool, but found " + rets[0]);
        } else if (!ifNode.guard.type.equals(BOOL))
            throw new SemanticError(ifNode.guard.lineNumber, ifNode.guard.columnNumber, "Expected bool, but found " + ifNode.guard.type);
        ifNode.body.accept(this);
        if (ifNode.elseBody != null) {
            ifNode.elseBody.accept(this);
            if (ifNode.body.type == StatementType.UNIT || ifNode.elseBody.type == StatementType.UNIT)
                ifNode.type = StatementType.UNIT;
            else
                ifNode.type = StatementType.VOID;
        } else {
            ifNode.type = StatementType.UNIT;
        }
        context.endContext();
    }

    public void visitInt(Int i) {
        i.type = same0D(INT);
    }


    @Override
    public void visitNull(Null nullNode) {
        nullNode.type = ANY;
    }

    public void visitProcedureCall(ProcedureCall procedureCall) {
        // check [funcCall] is in context
        zc246_zl345_co232_mw756.context.Type procFromMap = context.get(procedureCall.id);
        procedureCall.id.type = context.get(procedureCall.id);
        // cast to [FunctionType] and check number of arguments
        if(!(procFromMap instanceof FunctionType))
            throw new SemanticError(procedureCall.id.lineNumber, procedureCall.id.columnNumber,
                    "Expected procedure but found variable");
        PrimitiveType[] params = ((FunctionType) context.get(procedureCall.id)).args;
        ArrayList<PrimitiveType> args = new ArrayList<>();
        for (int i = 0; i < procedureCall.args.size(); i++) {
            procedureCall.args.get(i).accept(this);
            if (procedureCall.args.get(i) instanceof FuncCall) {
                PrimitiveType[] rets = ((Tuple) procedureCall.args.get(i).type).tuple;
                args.addAll(Arrays.asList(rets));
            } else {
                args.add((PrimitiveType) procedureCall.args.get(i).type);
            }
        }
        if (params.length != args.size())
            throw new SemanticError(procedureCall.id.lineNumber, procedureCall.id.columnNumber,
                    "Mismatched number of values");
        for (int i = 0; i < args.size(); i++) {
            PrimitiveType e = args.get(i);
            if (!e.equals(params[i])) {
                throw new SemanticError(procedureCall.id.lineNumber, procedureCall.id.columnNumber,
                        "Expected " + params[i] + ", but found " + e);
            }
        }
        PrimitiveType[] rets = ((FunctionType) context.get(procedureCall.id)).rets;
        if (rets.length != 0) {
            throw new SemanticError(procedureCall.id.lineNumber, procedureCall.id.columnNumber, "Expected procedure but found function.");
        }
        procedureCall.type = StatementType.UNIT;
    }

    public void visitProgram(Program program) {
        // first pass: add global, function, record to context
        for (Use use : program.uses) {
            use.accept(this);
        }

        for (Definition def : program.defs) {
            if (def instanceof Record){
                Record r = (Record) def;
                context.includeTopLevel(r, firstPassRecord(r, 0), r.name.lineNumber, r.name.columnNumber); // annotate nested fields
                records.put(r.name.id, r);
            }
        }

        for (Definition def : program.defs) {
            if(!(def instanceof Record)) {
                if (def instanceof Global) {
                    Global g = (Global) def;
                    g.varType.accept(this);
                    context.put(g.id, g.varType.type);
                } else if (def instanceof Function) {
                    Function f = (Function) def;
                    context.includeTopLevel(f, firstPassFunc(f), f.id.lineNumber, f.id.columnNumber);
                }
            }
        }

        // check .ri with the same name
        checkInterface(program);

        // second pass
        for (Definition def : program.defs) {
            def.accept(this);
        }

    }

    public void checkInterface(Program program){
        if (!enteredInterface){
            if (interfaces.containsKey(progName)){
                Program intf = interfaces.get(progName);
                //intf.accept(this);
                for (Definition def: intf.defs){
                    // each definition must be included in the original program's definition
                    if (def instanceof Function){
                        Function f = (Function) def;
                        if (!context.inDomain(f.id)){
                            throw new SemanticError(f.lineNumber, f.columnNumber,
                                    f + " is not defined in the original program");
                        }
                        FunctionType intfType = firstPassFunc(f);
                        if (!(context.get(f.id) instanceof FunctionType)){
                            throw new SemanticError(f.lineNumber, f.columnNumber,
                                    f + " is not defined as a function in the original program");
                        }
                        if (!(intfType.equals((FunctionType) context.get(f.id)))){
                            throw new SemanticError(f.lineNumber, f.columnNumber,
                                    f + " does not match the definition in the original program");
                        }
                    }
                    else if (def instanceof Record){
                        Record r = (Record) def;
                        RecordType intfType = firstPassRecord(r, 0);
//                        secondPassRecord(r);
                        boolean foundMatch = false;
                        for (Definition progDef: program.defs){
                            if (progDef instanceof Record){
                                RecordType progType = firstPassRecord((Record) progDef, 0);
//                                secondPassRecord((Record) progDef);
                                if (prefix(intfType, progType) && r.name.id.equals(((Record) progDef).name.id)){
                                    foundMatch = true;
                                    break;
                                }
                            }
                        }
                        if (!foundMatch){
                            throw new SemanticError(1,1,
                                    r + " is not defined in the original program");
                        }
                    }
                }
            }
        }
    }

    public void visitRecord(Record record) {
        // fields inside record should not be considered global
//        for (Declaration d: record.fields){
//            d.accept(this);
//        }
        record.type = context.get(record.recordIdentifier());
        record.fType = (FunctionType) context.get(record.name);
    }

    public void visitReturn(Return returnNode) {
        if(returnNode.getValues().size() == 0){
            returnNode.type = StatementType.VOID;
            returnNode.returnType = new PrimitiveType[0];
            return;
        }
        PrimitiveType[] actuals = new PrimitiveType[returnNode.getValues().size()];
        Expr e1 = returnNode.getValues().get(0);
        e1.accept(this);
        if (e1 instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) e1.type).tuple;
            if (rets.length != 1){
                if (returnNode.getValues().size() > 1)
                    throw new SemanticError(e1.lineNumber, e1.columnNumber,
                            "Mismatched number of values");
                returnNode.type = StatementType.VOID;
                actuals = rets;
            }
            else actuals[0] = rets[0];
        }
        else{
            actuals[0] = (PrimitiveType) e1.type;
        }

        for (int i = 1; i < returnNode.getValues().size(); i++) {
            Expr e = returnNode.getValues().get(i);
            e.accept(this);
            if (e instanceof FuncCall) {
                PrimitiveType[] rets = ((Tuple) e.type).tuple;
                if (rets.length != 1){
                    throw new SemanticError(e.lineNumber, e.columnNumber,
                            "Mismatched number of values");
                }
                else actuals[i] = rets[0];
            }
            else actuals[i] = (PrimitiveType) e.type;
        }
        returnNode.type = StatementType.VOID;
        returnNode.returnType = actuals;
    }

    public void visitStr(Str str) {
        str.type = new PrimitiveType(PrimitiveType.Primitive.INT, 1);
    }

    public void visitType(Type type) {
        if (type.next == null) {
            if (type.isInt()) {
                type.type = INT;
            } else if(type.isBool()){
                type.type = BOOL;
            } else{
                if (!records.containsKey(type.name)){
                    throw new SemanticError(type.lineNumber, type.columnNumber,
                            "record " + type.name + " is not defined");
                }
                type.type = context.get(records.get(type.name).recordIdentifier());
                //type.type = firstPassRecord(records.get(type.name), 0); // non-nested record fields
            }
        } else {
            int depth = -1;
            for (Type t = type; t != null; t = t.next) {
                if (t.len != null) {
                    t.len.accept(this);
                    if(t.len.type instanceof PrimitiveType && !t.len.type.equals(INT)
                            || (t.len.type instanceof Tuple &&
                            (((Tuple) (t.len.type)).tuple.length != 1 || !((Tuple) (t.len.type)).tuple[0].equals(INT)))){
                        throw new SemanticError(t.len.lineNumber, t.len.columnNumber, "Expected int, but found " + t.len.type);
                    }
                }
                depth++;
            }
            if (type.name.equals("int")){
                type.type = new PrimitiveType(PrimitiveType.Primitive.INT, depth);
            } else if (type.name.equals("bool")){
                type.type = new PrimitiveType(PrimitiveType.Primitive.BOOL, depth);
            } else{
                type.type = firstPassRecord(records.get(type.name), depth); // non-nested record fields
            }
        }
    }

    public void visitUse(Use use) {
        Program intf = interfaces.get(use.id.id);
        if (intf == null)
            if(use.id.id.equals(progName)) return;
            else throw new SemanticError(use.id.lineNumber, use.id.columnNumber,
                    "Name " + use.id.id + " cannot be resolved");
        boolean old = enteredInterface;
        enteredInterface = true;
        intf.accept(this);
        enteredInterface = old;
        for (Definition def : intf.defs) {
            if (def instanceof Function){
                Function f = (Function) def;
                f.accept(this);
                context.includeTopLevel(f, firstPassFunc(f), use.id.lineNumber, use.id.columnNumber);
            }
            else if (def instanceof Record){
                Record r = (Record) def;
                context.includeTopLevel(r, firstPassRecord(r, 0), r.name.lineNumber, r.name.columnNumber);
//                secondPassRecord(r); // annotate nested fields
                if (!records.containsKey(r.name.id)){
                    records.put(r.name.id, r);
                }
            }
        }
    }

    public void visitValue(Value value) {
        Expr v1, v2 = null;
        v1 = value.children.get(0);
        v1.accept(this);
        Value.Op op = value.op;
        switch (op) {
            case OR:
            case AND:
                v2 = value.children.get(1);
                v2.accept(this);
                checkIntBool(op, v1, BOOL);
                checkIntBool(op, v2, BOOL);
                value.type = BOOL;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case GT:
            case LT:
            case GEQ:
            case LEQ:
                v2 = value.children.get(1);
                v2.accept(this);
                checkIntBool(op, v1, INT);
                checkIntBool(op, v2, INT);
                value.type = BOOL;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case TIMES:
            case DIVIDE:
            case MODULO:
            case HIGHMUL:
                v2 = value.children.get(1);
                v2.accept(this);
                checkIntBool(op, v1, INT);
                checkIntBool(op, v2, INT);
                value.type = INT;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case PLUS:
                v2 = value.children.get(1);
                v2.accept(this);
                checkNotBool(op, v1);
                checkNotBool(op, v2);
                value.type = checkSameType(op, v1, v2);
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case EQUAL:
            case NOT_EQUAL:
                v2 = value.children.get(1);
                v2.accept(this);
                checkSameType(op, v1, v2);
                value.type = BOOL;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case NOT_LOG:
                checkIntBool(op, v1, BOOL);
                value.type = BOOL;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case MINUS:
                checkIntBool(op, v1, INT);
                if (value.children.size() == 2) {
                    v2 = value.children.get(1);
                    v2.accept(this);
                    checkIntBool(op, v2, INT);
                }
                value.type = INT;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case ARRAY_ACCESS:
                v2 = value.children.get(1);
                v2.accept(this);
                PrimitiveType temp = checkArray(op, v1);
                checkIntBool(op, v2, INT);
                value.type = temp;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
            case LENGTH:
                checkArray(op, v1);
                value.type = INT;
                value.lineNumber = v1.lineNumber;
                value.columnNumber = v1.columnNumber;
                break;
        }
    }

    public void visitWhile(While whileNode) {
        context.newContext();
        enteredWhile = true;
        whileNode.guard.accept(this);
        if (whileNode.guard instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) whileNode.guard.type).tuple;
            if (rets.length != 1 || !rets[0].equals(BOOL))
                throw new SemanticError(whileNode.guard.lineNumber,
                        whileNode.guard.columnNumber, "Guard of while loop must be boolean.");
        } else if (!whileNode.guard.type.equals(BOOL)) {
            throw new SemanticError(whileNode.guard.lineNumber, whileNode.guard.columnNumber,
                    "Guard of while loop must be boolean.");
        }
        whileNode.body.accept(this);
        whileNode.type = StatementType.UNIT;
        enteredWhile = false;
        context.endContext();
    }

    private void checkIntBool(Value.Op op, Expr expr, zc246_zl345_co232_mw756.context.PrimitiveType expected) {
        if (expr instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) expr.type).tuple;
            if (rets.length != 1 || !rets[0].equals(expected))
                throw new SemanticError(expr.lineNumber, expr.columnNumber, "Operands of " + op.toString() + " must be " + expected.toString());
        } else if (!expr.type.equals(expected)) {
            throw new SemanticError(expr.lineNumber, expr.columnNumber,
                    "Operands of " + op.toString() + " must be " + expected.toString());
        }
    }

    private void checkNotBool(Value.Op op, Expr expr) {
        if (expr instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) expr.type).tuple;
            if (rets.length != 1 || rets[0].equals(BOOL))
                throw new SemanticError(expr.lineNumber, expr.columnNumber, "Operands of " + op.toString() + " cannot be boolean");
        } else if (expr.type.equals(BOOL)) {
            throw new SemanticError(expr.lineNumber, expr.columnNumber,
                    "Operands of " + op.toString() + " cannot be boolean");
        }
    }

    private PrimitiveType checkArray(Value.Op op, Expr expr) {
        PrimitiveType t;
        if (expr instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) expr.type).tuple;
            if (rets.length != 1 || rets[0].arrayDepth <= 0)
                throw new SemanticError(expr.lineNumber, expr.columnNumber,
                        "Operands of " + op.toString() + " must be array");
            t = rets[0];
        } else if (((PrimitiveType) (expr.type)).arrayDepth <= 0) {
            throw new SemanticError(expr.lineNumber, expr.columnNumber,
                    "Operands of " + op.toString() + " must be array");
        } else {
            t = (PrimitiveType) expr.type;
        }
        if (t instanceof RecordType){
            RecordType rt = (RecordType) context.get(((RecordType) t).record.recordIdentifier());
            if(t.arrayDepth == 1) {
                return new RecordType(rt.fieldNames, rt.fieldTypes, rt.record, 0);
            }else{
                return new RecordType(rt.fieldNames, null, rt.record, 0);
            }
        }
        return new PrimitiveType(t.primitive, t.arrayDepth - 1);
    }

    private PrimitiveType checkSameType(Value.Op op, Expr expr1, Expr expr2) {
        PrimitiveType t1, t2;
        String message = "Operands of " + op.toString() + " must be both primitives or arrays";
        if (expr1 instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) expr1.type).tuple;
            if (rets.length != 1)
                throw new SemanticError(expr1.lineNumber, expr1.columnNumber, message);
            t1 = rets[0];
        } else {
            if (expr1.type instanceof FunctionType) {
                Identifier id = (Identifier) expr1;
                throw new SemanticError(id.lineNumber, id.columnNumber, message);
            }
            t1 = (PrimitiveType) expr1.type;
        }
        if (expr2 instanceof FuncCall) {
            PrimitiveType[] rets = ((Tuple) expr2.type).tuple;
            if (rets.length != 1)
                throw new SemanticError(expr2.lineNumber, expr2.columnNumber, message);
            t2 = rets[0];
        } else {
            if (expr2.type instanceof FunctionType) {
                Identifier id = (Identifier) expr2;
                throw new SemanticError(id.lineNumber, id.columnNumber, message);
            }
            t2 = (PrimitiveType) expr2.type;
        }
        if (!t1.equals(t2)) {
            throw new SemanticError(expr2.lineNumber, expr2.columnNumber,
                    "Operands of " + op + " must be same type");
        }
        return t1;
    }

    private RecordType deepCopyRecordType(RecordType r){
        PrimitiveType[] newFieldTypes = new PrimitiveType[r.fieldTypes.length];
        for (int i = 0; i < newFieldTypes.length; i++){
            if (r.fieldTypes[i] instanceof RecordType){
                newFieldTypes[i] = deepCopyRecordType((RecordType) r.fieldTypes[i]);
            }
            else{
                newFieldTypes[i] = r.fieldTypes[i];
            }
        }
        return new RecordType(r.fieldNames, newFieldTypes, r.record, r.arrayDepth);
    }

    private FunctionType firstPassFunc(Function f) {
        PrimitiveType[] paramTypes = new PrimitiveType[f.params.size()];
        for (int i = 0; i < f.params.size(); i++) {
            Type t = f.params.get(i).varType;
            t.accept(this);
            paramTypes[i] = (PrimitiveType) t.type;
        }
        PrimitiveType[] retTypes = new PrimitiveType[f.return_types.size()];
        for (int i = 0; i < f.return_types.size(); i++) {
            Type t = f.return_types.get(i);
            t.accept(this);
            retTypes[i] = (PrimitiveType) t.type;
        }
        return new FunctionType(paramTypes, retTypes);
    }

    private RecordType firstPassRecord(Record r, int arrayDepth){
        String[] fieldNames = new String[r.fields.size()];
        PrimitiveType[] fieldTypes = new PrimitiveType[r.fields.size()];
        for (int i = 0; i < r.fields.size(); i++){
            fieldNames[i] = r.fields.get(i).id.id;
            Type t = r.fields.get(i).varType;
            if (t.name.equals(r.name.id)){
                // put null temporarily for the first pass
                Type type = t.next;
                int depth = 0;
                while(type!=null){
                    depth++;
                    type = type.next;
                }
                if(depth == 0)
                    fieldTypes[i] = new RecordType(fieldNames, fieldTypes, r, depth);
                else fieldTypes[i] = new RecordType(fieldNames, null, r, depth);
            }
            else{
                t.accept(this);
                fieldTypes[i] = (PrimitiveType) t.type;
            }
        }
        return new RecordType(fieldNames, fieldTypes, r, arrayDepth);
    }
}