package zc246_zl345_co232_mw756.assembly;

import edu.cornell.cs.cs4120.xic.ir.IRTemp;

import java.util.*;

public class Register extends Expr {

    public static final Register RAX = new Register("rax");
    public static final Register AL = new Register("al");
    public static final Register RBX = new Register("rbx");
    public static final Register BL = new Register("bl");
    public static final Register RCX = new Register("rcx");
    public static final Register CL = new Register("cl");
    public static final Register RDX = new Register("rdx");
    public static final Register DL = new Register("dl");
    public static final Register RSI = new Register("rsi");
    public static final Register SIL = new Register("sil");
    public static final Register RDI = new Register("rdi");
    public static final Register DIL = new Register("dil");
    public static final Register RBP = new Register("rbp");
    public static final Register BPL = new Register("bpl");
    public static final Register RSP = new Register("rsp");
    public static final Register SPL = new Register("spl");
    public static final Register R8 = new Register("r8");
    public static final Register R8B = new Register("r8b");
    public static final Register R9 = new Register("r9");
    public static final Register R9B = new Register("r9b");
    public static final Register R10 = new Register("r10");
    public static final Register R10B = new Register("r10b");
    public static final Register R11 = new Register("r11");
    public static final Register R11B = new Register("r11b");
    public static final Register R12 = new Register("r12");
    public static final Register R12B = new Register("r12b");
    public static final Register R13 = new Register("r13");
    public static final Register R13B = new Register("r13b");
    public static final Register R14 = new Register("r14");
    public static final Register R14B = new Register("r14b");
    public static final Register R15 = new Register("r15");
    public static final Register R15B = new Register("r15b");
    public static final java.util.Set<Register> generalRegisters = java.util.Set.of(RAX, RBX, RCX, RDX, RSI, RDI, R8, R9, R10, R11, R12, R13, R14, R15);
    public static final Map<Register, Register> to8Bits = Map.ofEntries(Map.entry(RAX, AL), Map.entry(RBX, BL),
            Map.entry(RCX, CL), Map.entry(RDX, DL), Map.entry(RSI, SIL), Map.entry(RDI, DIL), Map.entry(RSP, SPL),
            Map.entry(RBP, BPL), Map.entry(R8, R8B), Map.entry(R9, R9B), Map.entry(R10, R10B), Map.entry(R11, R11B),
            Map.entry(R12, R12B), Map.entry(R13, R13B), Map.entry(R14, R14B), Map.entry(R15, R15B));
    public static final ArrayList<Register> ARGUMENTS = new ArrayList<>(List.of(RDI, RSI, RDX, RCX, R8, R9));
    private String name;
    private static int count = 0;

    public Register(String name) {
        this.name = name;
    }

    public static Register getReg() {
        return new Register("_reg" + count++);
    }

    public String getName() {
        return name;
    }

    public IRTemp getTemp() {
        return new IRTemp(name);
    }

    public static Register getReg(IRTemp temp) {
        count++;
        return new Register(temp.name());
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Register register = (Register) o;
        return Objects.equals(name, register.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public Register replaceReg(List<Register> old, List<Register> newReg) {
        if(old.contains(this)){
            int index = old.indexOf(this);
            return newReg.get(index);
        }
        return this;
    }
}
