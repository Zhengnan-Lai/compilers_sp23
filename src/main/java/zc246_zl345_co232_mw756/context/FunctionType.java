package zc246_zl345_co232_mw756.context;

import java.util.Arrays;

public class FunctionType implements Type {
    public PrimitiveType[] args;
    public PrimitiveType[] rets;

    public FunctionType(PrimitiveType[] args, PrimitiveType[] rets) {
        this.args = args;
        this.rets = rets;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FunctionType)) {
            return false;
        }
        FunctionType other = (FunctionType) o;
        return Arrays.equals(this.args, other.args) && Arrays.equals(this.rets, other.rets);
    }
}
