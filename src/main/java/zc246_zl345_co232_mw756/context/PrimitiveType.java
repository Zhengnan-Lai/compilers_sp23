package zc246_zl345_co232_mw756.context;

public class PrimitiveType implements Type {

    public enum Primitive {
        INT, BOOL, ANY, RECORD
    }

    public static final PrimitiveType INT = new PrimitiveType(PrimitiveType.Primitive.INT, 0);
    public static final PrimitiveType BOOL = new PrimitiveType(PrimitiveType.Primitive.BOOL, 0);
    public static final PrimitiveType ANY = new PrimitiveType(PrimitiveType.Primitive.ANY, 0);
    public int arrayDepth;
    public Primitive primitive;

    public PrimitiveType(){

    }

    public PrimitiveType(Primitive primitive) {
        this.primitive = primitive;
        this.arrayDepth = 0;
    }

    public PrimitiveType(Primitive primitive, int d) {
        this.primitive = primitive;
        this.arrayDepth = d;
    }

    public void addDepth() {
        arrayDepth++;
    }

    public void setDepth(int depth) {
        this.arrayDepth = depth;
    }

    public static PrimitiveType same0D(Type t) {
        if (t instanceof PrimitiveType) {
            PrimitiveType p = (PrimitiveType) t;
            return new PrimitiveType(p.primitive);
        }
        throw new Error("Type is not primitive");
    }

    public static PrimitiveType same(Type t) {
        if (t instanceof PrimitiveType) {
            PrimitiveType p = (PrimitiveType) t;
            return new PrimitiveType(p.primitive, p.arrayDepth);
        }
        throw new Error("Type is not primitive");
    }

    @Override
    public String toString() {
        return primitive.toString().toLowerCase() + "[]".repeat(arrayDepth);
    }

    @Override
    public boolean equals(Object t) {
        if (!(t instanceof PrimitiveType)) return false;
        Primitive myPrimitive = primitive;
        Primitive otherPrimitive = ((PrimitiveType) t).primitive;
        if (myPrimitive == Primitive.ANY && otherPrimitive == Primitive.ANY) return true;
        else if (myPrimitive == Primitive.ANY) {
            return arrayDepth <= ((PrimitiveType) t).arrayDepth;
        } else if (otherPrimitive == Primitive.ANY) {
            return ((PrimitiveType) t).arrayDepth <= arrayDepth;
        } else {
            return myPrimitive == otherPrimitive && arrayDepth == ((PrimitiveType) t).arrayDepth;
        }
    }

}