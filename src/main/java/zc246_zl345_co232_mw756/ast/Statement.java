package zc246_zl345_co232_mw756.ast;

import zc246_zl345_co232_mw756.context.PrimitiveType;

public abstract class Statement extends Node {
    public Statement(int lineNumber, int columnNumber) {
        super(lineNumber, columnNumber);
    }

    public Statement() {
    }

    public void checkReturn(PrimitiveType[] rets) {
    }
}
