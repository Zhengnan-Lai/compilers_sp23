package zc246_zl345_co232_mw756;

import zc246_zl345_co232_mw756.ast.*;
import zc246_zl345_co232_mw756.errors.SyntaxError;

import java.util.List;

public class Parser {
    public static void assertProgram(Node n) {
        Program p = (Program) n;
        List<Definition> defs = p.defs;
        for(Definition d : defs) {
            if (d instanceof Function) {
                Function f = (Function) d;
                if (f.body == null) {
                    int line = f.id.lineNumber;
                    int col = f.id.columnNumber;
                    throw new SyntaxError(line, col, "Function body cannot be empty.");
                }
            }
        }
    }

    public static void assertInterface(Node n) {
        Program p = (Program) n;
        List<Definition> defs = p.defs;
        for(Definition d : defs) {
            if (d instanceof Function) {
                Function f = (Function) d;
                if (f.body != null) {
                    int line = f.id.lineNumber;
                    int col = f.id.columnNumber;
                    throw new SyntaxError(line, col, "Cannot have function body in interfaces.");
                }
            }else if(d instanceof Global) {
                Global g = (Global) d;
                int line = g.id.lineNumber;
                int col = g.id.columnNumber;
                throw new SyntaxError(line, col, "Cannot have global variable in interfaces.");
            }
        }
    }
}
