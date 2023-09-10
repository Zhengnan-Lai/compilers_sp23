package zc246_zl345_co232_mw756.assembly;

import edu.cornell.cs.cs4120.xic.ir.IRConst;
import edu.cornell.cs.cs4120.xic.ir.IRExpr;
import edu.cornell.cs.cs4120.xic.ir.IRTemp;

import java.util.List;

public class Mem extends Expr{
    // [base \pm index * scale + displacement]
    // index can be null, others cannot
    private final Register base;
    private final Register index;
    private final Const scale;
    private final Const displacement;

    public Register getBase() {
        return base;
    }
    public Register getIndex() {
        return index;
    }
    public Const getScale() {
        return scale;
    }
    public Const getDisplacement() {
        return displacement;
    }

    public Mem(Register base, Register index, Const scale, Const displacement) {
        assert scale.val() == 1 || scale.val() == 2 || scale.val() == 4 || scale.val() == 8;
        this.base = base;
        this.index = index;
        this.scale = scale;
        this.displacement = displacement;
    }

    public Mem(IRExpr[] exprs){
        this.base = (exprs[0] != null) ? new Register(((IRTemp) exprs[0]).name()):null;
        this.index = (exprs[1] != null) ? new Register(((IRTemp) exprs[1]).name()):null;
        this.scale = new Const((IRConst) exprs[2]);
        this.displacement = new Const((IRConst) exprs[3]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QWORD PTR [");
        if(base != null) {
            sb.append(base);
        }
        if(base != null && index != null){
            sb.append(" + ");
        }
        if (index != null){
            if(Math.abs(scale.val())==1){
                sb.append(index.toString());
            }else{
                sb.append(index.toString()).append(" * ").append(Math.abs(scale.val()));
            }
        }
        if(displacement.val() != 0L){
            sb.append(displacement.val() < 0L ? " - " : " + ");
            sb.append(Math.abs(displacement.val()));
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Expr replaceReg(List<Register> old, List<Register> newReg) {
        Register newBase = base;
        Register newIndex = index;
        if(base != null) {
            newBase = base.replaceReg(old, newReg);
        }
        if(index != null) {
            newIndex = index.replaceReg(old, newReg);
        }
        if(newBase == base && newIndex == index){
            return this;
        }else{
            return new Mem(newBase, newIndex, scale, displacement);
        }
    }
}
