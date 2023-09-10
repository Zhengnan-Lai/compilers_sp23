package zc246_zl345_co232_mw756.context;

import java.util.StringJoiner;

public class Tuple implements Type {
    public PrimitiveType[] tuple;

    public Tuple(PrimitiveType[] tuple) {
        this.tuple = tuple;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ");
        for (PrimitiveType p : tuple) {
            sj.add(p.toString());
        }
        return sj.toString();
    }
}
