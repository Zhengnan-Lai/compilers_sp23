package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.context.FunctionType;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.HashMap;
import java.util.List;

public class Record extends Definition{
    public Identifier name;
    public List<Declaration> fields;
    public HashMap<Identifier, Long> offsets;
    public FunctionType fType;

    public Record(Identifier name, List<Declaration> fields) {
        super(name.lineNumber, name.columnNumber);
        this.name = name;
        this.fields = fields;
        this.offsets = new HashMap<>();
        for(int i=0;i<fields.size();i++){
            offsets.put(fields.get(i).id, i*8L);
        }
    }

    public Identifier recordIdentifier(){
        return new Identifier(name.lineNumber, name.columnNumber, "r"+name.id);
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        name.printNode(printer);
        printer.startUnifiedList();
        for (Declaration d : fields) {
            d.printNode(printer);
        }
        printer.endList();
        printer.endList();
    }

    @Override
    public String toString() {
         return name.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){
            return true;
        }
        if (!(obj instanceof Record)){
            return false;
        }
        return name == ((Record) obj).name && fields == ((Record) obj).fields;
    }

    @Override
    public void accept(Visitor v) {
        v.visitRecord(this);
    }

    @Override
    public Node constFold() {
        return this;
    }
}
