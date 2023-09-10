package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import edu.cornell.cs.cs4120.xic.ir.interpret.IRSimulator;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

public class Value extends Expr {
    public enum Op {
        PLUS("+") {
            @Override
            public String toString() {
                return "+";
            }
        },
        MINUS("-") {
            @Override
            public String toString() {
                return "-";
            }
        },
        TIMES("*") {
            @Override
            public String toString() {
                return "*";
            }
        },
        DIVIDE("/") {
            @Override
            public String toString() {
                return "/";
            }
        },
        MODULO("%") {
            @Override
            public String toString() {
                return "%";
            }
        },
        HIGHMUL("*>>") {
            @Override
            public String toString() {
                return "*>>";
            }
        },
        OR("|") {
            @Override
            public String toString() {
                return "|";
            }
        },
        AND("&") {
            @Override
            public String toString() {
                return "&";
            }
        },
        LT("<") {
            @Override
            public String toString() {
                return "<";
            }
        },
        LEQ("<=") {
            @Override
            public String toString() {
                return "<=";
            }
        },
        GT(">") {
            @Override
            public String toString() {
                return ">";
            }
        },
        GEQ(">=") {
            @Override
            public String toString() {
                return ">=";
            }
        },
        EQUAL("==") {
            @Override
            public String toString() {
                return "==";
            }
        },
        NOT_EQUAL("!=") {
            @Override
            public String toString() {
                return "!=";
            }
        },
        NOT_LOG("!") {
            @Override
            public String toString() {
                return "!";
            }
        },
        ARRAY_ACCESS("[]") {
            @Override
            public String toString() {
                return "[]";
            }
        },
        LENGTH("length") {
            @Override
            public String toString() {
                return "length";
            }
        };

        Op(String rep) {
        }
    }

    public Op op;
    public List<Expr> children;

    public Value(Op op, Expr child1, Expr child2) {
        this.op = op;
        this.children = new ArrayList<>();
        this.children.add(child1);
        this.children.add(child2);
    }

    public Value(Op op, Expr child1) {
        this.op = op;
        this.children = new ArrayList<>();
        this.children.add(child1);
    }


    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom(op.toString());
        for (Node child : children) {
            child.printNode(printer);
        }
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitValue(this);
    }

    @Override
    public Node constFold() {
        if(op==Op.MINUS){
            if(children.size()==1){
                if(children.get(0) instanceof Value && ((Value) children.get(0)).op==Op.MINUS && ((Value) children.get(0)).children.size()==1){
                    return ((Value) children.get(0)).children.get(0).constFold();
                }
            }
        }else if(op==Op.NOT_LOG){
            if(children.get(0) instanceof Value && ((Value) children.get(0)).op==Op.NOT_LOG && ((Value) children.get(0)).children.size()==1){
                return ((Value) children.get(0)).children.get(0).constFold();
            }
        }
        children.replaceAll(e -> (Expr) e.constFold());
        if(children.size()==1){
            Expr e = children.get(0);
            switch(op){
                case MINUS:
                    if(e instanceof Char){
                        Char c = (Char) e;
                        return new Int(c.lineNumber, c.columnNumber, -c.value).setType(this.type);
                    } else if(e instanceof Int){
                        Int i = (Int) e;
                        return new Int(i.lineNumber, i.columnNumber, -i.value).setType(this.type);
                    }
                    break;
                case NOT_LOG:
                    if(e instanceof Bool){
                        Bool b = (Bool) e;
                        return new Bool(b.lineNumber, b.columnNumber, 1L ^ b.value).setType(this.type);
                    }
                    break;
                case LENGTH:
                    if(e instanceof ArrayLiteral){
                        ArrayLiteral a = (ArrayLiteral) e;
                        return new Int(a.lineNumber, a.columnNumber, a.values.size()).setType(this.type);
                    }else if(e instanceof Str){
                        Str s = (Str) e;
                        return new Int(s.lineNumber, s.columnNumber, s.value.length).setType(this.type);
                    }
            }
        }else{
            Expr e1 = children.get(0);
            Expr e2 = children.get(1);
            if(isLiteral()) {
                long l1, l2, b1, b2;
                switch (op) {
                    case PLUS:
                        List<Expr> values = new ArrayList<>();
                        if(e1 instanceof ArrayLiteral|| e1 instanceof Str){
                            if(e1 instanceof ArrayLiteral)
                                values.addAll(((ArrayLiteral) e1).values);
                            else
                                values.addAll(ArrayLiteral.fromStr((Str) e1).values);
                            if(e2 instanceof ArrayLiteral)
                                values.addAll(((ArrayLiteral) e2).values);
                            else
                                values.addAll(ArrayLiteral.fromStr((Str) e2).values);
                            return new ArrayLiteral(e1.lineNumber, e1.columnNumber, values).setType(this.type);
                        }else{
                            l1 = getLong(e1);
                            l2 = getLong(e2);
                            return new Int(e1.lineNumber, e1.columnNumber, l1 + l2).setType(this.type);
                        }
                    case MINUS:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Int(e1.lineNumber, e1.columnNumber, l1 - l2).setType(this.type);
                    case TIMES:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Int(e1.lineNumber, e1.columnNumber, l1 * l2).setType(this.type);
                    case DIVIDE:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        if(l2==0) throw new Error("Divide by zero");
                        return new Int(e1.lineNumber, e1.columnNumber, l1 / l2).setType(this.type);
                    case MODULO:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        if(l2==0) throw new Error("Modulo by zero");
                        return new Int(e1.lineNumber, e1.columnNumber, l1 % l2).setType(this.type);
                    case HIGHMUL:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Int(e1.lineNumber, e1.columnNumber, Math.multiplyHigh(l1, l2)).setType(this.type);
                    case OR:
                        b1 = getBool(e1);
                        b2 = getBool(e2);
                        return new Bool(e1.lineNumber, e1.columnNumber, b1 | b2).setType(this.type);
                    case AND:
                        b1 = getBool(e1);
                        b2 = getBool(e2);
                        return new Bool(e1.lineNumber, e1.columnNumber, b1 & b2).setType(this.type);
                    case LT:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Bool(e1.lineNumber, e1.columnNumber, l1 < l2?1:0).setType(this.type);
                    case LEQ:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Bool(e1.lineNumber, e1.columnNumber, l1 <= l2?1:0).setType(this.type);
                    case GT:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Bool(e1.lineNumber, e1.columnNumber, l1 > l2?1:0).setType(this.type);
                    case GEQ:
                        l1 = getLong(e1);
                        l2 = getLong(e2);
                        return new Bool(e1.lineNumber, e1.columnNumber, l1 >= l2?1:0).setType(this.type);
                    case EQUAL:
                        try{
                            l1 = getLong(e1);
                            l2 = getLong(e2);
                            return new Bool(e1.lineNumber, e1.columnNumber, l1 == l2?1:0).setType(this.type);
                        }catch(Error e){
                            try{
                                b1 = getBool(e1);
                                b2 = getBool(e2);
                                return new Bool(e1.lineNumber, e1.columnNumber, b1 == b2?1:0).setType(this.type);
                            }catch (Error ee){
                                // both are arrays
                                return new Bool(-1,-1, 0).setType(this.type);
                            }
                        }
                    case NOT_EQUAL:
                        try{
                            l1 = getLong(e1);
                            l2 = getLong(e2);
                            return new Bool(e1.lineNumber, e1.columnNumber, l1 != l2?1:0).setType(this.type);
                        }catch(Error e){
                            try {
                                b1 = getBool(e1);
                                b2 = getBool(e2);
                                return new Bool(e1.lineNumber, e1.columnNumber, b1 != b2?1:0).setType(this.type);
                            }catch (Error ee){
                                // both are arrays
                                return new Bool(-1,-1, 0).setType(this.type);
                            }
                        }
                    default:
                        //[]
                        long index = getLong(e2);
                        if(e1 instanceof ArrayLiteral){
                            ArrayLiteral a = (ArrayLiteral) e1;
                            if(index<0 || index>=a.values.size())
                                throw new IRSimulator.OutOfBoundTrap("Out of bounds!");
                            return a.values.get((int) index).setType(this.type);
                        }else{
                            Str s = (Str) e1;
                            if(index<0 || index>=s.value.length)
                                throw new IRSimulator.OutOfBoundTrap("Out of bounds!");
                            return new Char(s.lineNumber, s.columnNumber, s.value[(int) index]).setType(this.type);
                        }
                }
            }
        }
        return this;
    }

    private boolean isLiteral(){
        boolean is = true;
        for(Expr e: children){
            is = is && (e instanceof Int || e instanceof Char || e instanceof Bool || e instanceof Str || e instanceof ArrayLiteral);
        }
        return is;
    }

    private long getLong(Expr e){
        if(e instanceof Int){
            return ((Int) e).value;
        }else if(e instanceof Char){
            return ((Char) e).value;
        }
        throw new Error("Unexpected type");
    }

    private long getBool(Expr e){
        if(e instanceof Bool){
            return ((Bool) e).value;
        }
        throw new Error("Unexpected type");
    }
}
