package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.Objects;

public class Type extends Node {


    public String name; // {"int", "bool", "_array", f"{record_name}"}
    public Expr len;
    public Type next;


    public Type(int line, int col, String name, Expr len, Type next) {
        super(line, col);
        this.name = name;
        this.len = len;
        this.next = next;
    }

    public Type(int line, int col, String name, Expr len) {
        super(line, col);
        this.name = name;
        this.len = len;
    }

    public void setNext(Type next) {
        Type p = this;
        for (; p.next != null; p = p.next) {
        }
        p.next = next;
    }

    public static Type INT(int line, int col) {
        return new Type(line, col, "int", null);
    }

    public static Type BOOL(int line, int col) {
        return new Type(line, col, "bool", null);
    }

    @Override
    public void printNode(SExpPrinter printer) {
        if (next == null) {
            printer.printAtom(name);
        } else {
            next.printNode(printer, name);
        }
    }

    @Override
    public void accept(Visitor v) {
        v.visitType(this);
    }

    @Override
    public Node constFold() {
        if(len!=null) len = (Expr) len.constFold();
        if(next!=null) next = (Type) next.constFold();
        return this;
    }

    private void printNode(SExpPrinter printer, String type) {
        printer.startUnifiedList();
        printer.printAtom("[]");
        if (next == null) printer.printAtom(type);
        else next.printNode(printer, type);
        if (len != null) len.printNode(printer);
        printer.endList();
    }

    public boolean isInt(){
        return Objects.equals(name, "int") && len == null;
    }

    public boolean isBool(){
        return Objects.equals(name, "bool") && len == null;
    }
}
