package zc246_zl345_co232_mw756.visitor;

import zc246_zl345_co232_mw756.ast.*;
import zc246_zl345_co232_mw756.ast.Record;

public abstract class Visitor {
    public abstract void visitArrayLiteral(ArrayLiteral arrayLiteral);

    public abstract void visitAssignment(Assignment assignment);

    public abstract void visitBlock(Block block);

    public abstract void visitBool(Bool bool);

    public abstract void visitBreak(Break breakNode);

    public abstract void visitChar(Char c);

    public abstract void visitDeclaration(Declaration declaration);

    public abstract void visitFieldAccess(FieldAccess fieldAccess);

    public abstract void visitFuncCall(FuncCall funcCall);

    public abstract void visitFunction(Function function);

    public abstract void visitGlobal(Global global);

    public abstract void visitIdentifier(Identifier identifier);

    public abstract void visitIf(If ifNode);

    public abstract void visitInt(Int i);

    public abstract void visitNull(Null nullNode);

    public abstract void visitProcedureCall(ProcedureCall procedureCall);

    public abstract void visitProgram(Program program);

    public abstract void visitRecord(Record record);

    public abstract void visitReturn(Return returnNode);

    public abstract void visitStr(Str str);

    public abstract void visitType(Type type);

    public abstract void visitUse(Use use);

    public abstract void visitValue(Value value);

    public abstract void visitWhile(While whileNode);

}
