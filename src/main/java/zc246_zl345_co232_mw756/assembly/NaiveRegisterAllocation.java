package zc246_zl345_co232_mw756.assembly;

import java.util.Set;
import java.util.*;

// [TrivialRegAlloc] is a trivial register allocator
public class NaiveRegisterAllocation {
    private static final Set<String> genPurposeRegs = Set.of("rax", "rbx", "rcx", "rdx", "rsi", "rdi", "rsp", "rbp", "r8",
            "r9", "r10", "r11", "r12", "r13", "r14", "r15");
    // hashmap from register to address
    private final HashMap<String, Mem> regToAddr;
    private int allocateRegs;

    public NaiveRegisterAllocation() {
        regToAddr = new HashMap<>();
        allocateRegs = 0;
    }

    public AACompUnit alloc(AACompUnit compUnit) {
        Map<String, Function> functions = new HashMap<>();
        for (Map.Entry<String, Function> entry : compUnit.getFunctions().entrySet()) {
            allocateRegs = 0;
            regToAddr.clear();
            functions.put(entry.getKey(), new Function(entry.getKey(), allocFunc(entry.getValue().getBody())));
        }
        return new AACompUnit(compUnit.getName(), functions, compUnit.getDataMap());
    }

    // [alloc] allocates registers
    private List<Assembly> allocFunc(List<Assembly> assembly) {
        // [allocateRegs] is the number of registers that need to be allocated on the stack
        List<Assembly> toAssembly = new ArrayList<>();
        for (Assembly a : assembly) {
            if (a instanceof Push) {
                // push src, [src] can be register, immediate, or memory address
                Push push = (Push) a;
                Expr pushReg = push.getReg();
                if (pushReg instanceof Register) {
                    toAssembly.add(new Push(searchRegisters((Register) pushReg)));
                } else if (pushReg instanceof Const || pushReg instanceof Mem) {
                    toAssembly.add(a);
                } else {
                    throw new Error("[src] of instruction [push src] is not a register, immediate, or memory address.");
                }
            } else if (a instanceof Pop) {
                // pop dest, [dest] can be register or memory location
                Expr popReg = ((Pop) a).getReg();
                if (popReg instanceof Register) {
                    Register reg = (Register) popReg;
                    if (containsRegister(reg) == -1) {
                        allocateRegister(reg.getName());
                    }
                    toAssembly.add(new Pop(searchRegisters(reg)));
                } else if (popReg instanceof Mem) {
                    toAssembly.add(a);
                } else {
                    throw new Error("[dest] of [push dest] is not a register or memory location.");
                }
            } else if (a instanceof BOP) {
                BOP bop = (BOP) a;
                if (bop.getDest() instanceof Const) {
                    throw new Error("Cannot have a constant as a destination.");
                } else if (bop.getDest() instanceof Mem && bop.getSrc() instanceof Mem) {
                    throw new Error("Cannot have two memory accesses in one instruction.");
                } else if (bop.getDest() instanceof Mem) {
                    Mem memDest = (Mem) bop.getDest();
                    List<Assembly> memAlloc = naiveMemAllocation(memDest);
                    for (int i = 0; i < memAlloc.size() - 1; i++) {
                        toAssembly.add(memAlloc.get(i));
                    }
                    boolean useR9 = false;
                    if (bop.getSrc() instanceof Register) {
                        Register srcReg = (Register) bop.getSrc();
                        if (regToAddr.containsKey(srcReg.getName())) {
                            toAssembly.add(new Mov(Register.R10, regToAddr.get(srcReg.getName())));
                            useR9 = true;
                        }
                    }
                    toAssembly.add(new Mov(((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc(), useR9 ? Register.R10 : bop.getDest()));
                } else if (bop.getDest() instanceof Register && bop.getSrc() instanceof Register && bop.getDest().equals(bop.getSrc())) {
                    Register bopDest = (Register) bop.getDest();
                    boolean useR9 = false;
                    if (containsRegister(bopDest) < 1) {
                        if (containsRegister(bopDest) == -1) {
                            allocateRegister(((Register) bop.getDest()).getName());
                        }
                        useR9 = true;
                        toAssembly.add(new Mov(Register.R10, searchRegisters(bopDest)));
                    }
                    toAssembly.add(new BOP(bop.getBOPType(), useR9 ? Register.R10 : bop.getDest(), useR9 ? Register.R10 : bop.getSrc()));
                    if (useR9) {
                        toAssembly.add(new Mov(searchRegisters(bopDest), Register.R10));
                    }
                } else if (bop.getDest() instanceof Register) {
                    Register bopDest = (Register) bop.getDest();
                    boolean useR9 = false;
                    if (containsRegister(bopDest) < 1) {
                        if (containsRegister(bopDest) == -1) {
                            allocateRegister(bopDest.getName());
                        }
                        toAssembly.add(new Mov(Register.R10, searchRegisters(bopDest)));
                        useR9 = true;
                    }

                    if (bop.getSrc() instanceof Register) {
                        Register srcReg = (Register) bop.getSrc();
                        boolean useR10 = false;
                        if (containsRegister(srcReg) < 1) {
                            if (containsRegister(srcReg) == -1) {
                                allocateRegister(srcReg.getName());
                            }
                            toAssembly.add(new Mov(Register.R11, searchRegisters((Register) bop.getSrc())));
                            useR10 = true;
                        }
                        toAssembly.add(new BOP(bop.getBOPType(), useR9 ? Register.R10 : bop.getDest(), useR10 ? Register.R11 : bop.getSrc()));
                    } else if (bop.getSrc() instanceof Mem) {
                        Mem memSrc = (Mem) bop.getSrc();
                        List<Assembly> memAlloc = naiveMemAllocation(memSrc);
                        for (int i = 0; i < memAlloc.size() - 1; i++) {
                            toAssembly.add(memAlloc.get(i));
                        }
                        toAssembly.add(new Mov(useR9 ? Register.R10 : bop.getDest(), ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc()));
                    } else if (bop.getSrc() instanceof Const) {
                        toAssembly.add(new BOP(bop.getBOPType(), useR9 ? Register.R10 : bop.getDest(), bop.getSrc()));
                    }
                    if (useR9) {
                        toAssembly.add(new Mov(searchRegisters(bopDest), Register.R10));
                    }
                }
            } else if (a instanceof Mov) {
                Mov mov = (Mov) a;
                Expr movDest = mov.getDest();
                Expr movSrc = mov.getSrc();
                if (movDest instanceof Register) {
                    boolean useR9 = false;
                    Register destRegister = (Register) movDest;
                    if (containsRegister(destRegister) < 1) {
                        if (containsRegister(destRegister) == -1) {
                            allocateRegister(destRegister.getName());
                        }
                        useR9 = true;
                    }
                    if (movSrc instanceof Register && containsRegister((Register) movSrc) < 1) {
                        Register srcRegister = (Register) movSrc;
                        if (containsRegister(srcRegister) == -1) {
                            allocateRegister(srcRegister.getName());
                        }
                        toAssembly.add(new Mov(Register.R11, searchRegisters(srcRegister)));
                        toAssembly.add(new Mov(useR9 ? Register.R10 : movDest, Register.R11));
                    } else if (movSrc instanceof Mem) {
                        // JUMP HERE
                        Mem memSrc = (Mem) movSrc;
                        List<Assembly> memAlloc = naiveMemAllocation(memSrc);
                        for (int i = 0; i < memAlloc.size() - 1; i++) {
                            toAssembly.add(memAlloc.get(i));
                        }
                        toAssembly.add(new Mov(useR9 ? Register.R10 : movDest, ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc()));
                    } else { // [movSrc] is memory location or immediate
                        toAssembly.add(new Mov(useR9 ? Register.R10 : movDest, movSrc));
                    }
                    if (useR9) {
                        toAssembly.add(new Mov(regToAddr.get(destRegister.getName()), Register.R10));
                    }
                } else if (movDest instanceof Mem) {
                    Mem memDest = (Mem) movDest;
                    List<Assembly> memAlloc = naiveMemAllocation(memDest);
                    for (int i = 0; i < memAlloc.size() - 1; i++) {
                        toAssembly.add(memAlloc.get(i));
                    }

                    if (movSrc instanceof Register) {
                        Register srcRegister = (Register) movSrc;
                        if (containsRegister(srcRegister) < 1) {
                            if (containsRegister(srcRegister) == -1) {
                                allocateRegister(srcRegister.getName());
                            }
                            toAssembly.add(new Mov(Register.R10, searchRegisters(srcRegister)));
                            toAssembly.add(new Mov(((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc(), Register.R10));
                        } else {
                            toAssembly.add(new Mov(((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc(), movSrc));
                        }
                    } else {
                        toAssembly.add(new Mov(((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc(), movSrc));
                    }
                } else {
                    //Offset
                    if (movSrc instanceof Register && containsRegister((Register) movSrc) < 1) {
                        Register srcRegister = (Register) movSrc;
                        if (containsRegister(srcRegister) == -1) {
                            allocateRegister(srcRegister.getName());
                        }
                        toAssembly.add(new Mov(Register.R10, searchRegisters(srcRegister)));
                        toAssembly.add(new Mov(movDest, Register.R10));
                    } else {
                        toAssembly.add(a);
                    }
                }
            } else if (a instanceof Inc) {
                // For a spilled register, can directly use memory location of register
                Inc inc = (Inc) a;
                if (inc.getReg() instanceof Register) {
                    Register regName = (Register) inc.getReg();
                    toAssembly.add(new Inc(searchRegisters(regName)));
                } else {
                    toAssembly.add(a);
                }
            } else if (a instanceof Dec) {
                Dec dec = (Dec) a;
                if (dec.getReg() instanceof Register) {
                    Register regName = (Register) dec.getReg();
                    toAssembly.add(new Dec(searchRegisters(regName)));
                } else {
                    toAssembly.add(a);
                }
            } else if (a instanceof Lea) {
                Lea lea = (Lea) a;
                // Naive solution of using r9-r11
                Register leaDest = lea.getDest();
                Expr addr = lea.getExpr();
                boolean useR9 = false;
                if (containsRegister(leaDest) < 1) {
                    if (containsRegister(leaDest) == -1) {
                        allocateRegister(leaDest.getName());
                    }
                    useR9 = true;
                }
                if (addr instanceof Mem) {
                    Mem leaMem = (Mem) addr;
                    List<Assembly> memAlloc = naiveMemAllocation(leaMem);
                    for (int i = 0; i < memAlloc.size() - 1; i++) {
                        toAssembly.add(memAlloc.get(i));
                    }
                    toAssembly.add(new Lea(useR9 ? Register.R10 : lea.getDest(), ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc()));
                } else {
                    //instanceof Offset
                    toAssembly.add(new Lea(useR9 ? Register.R10 : lea.getDest(), addr));
                }
                if (useR9) {
                    toAssembly.add(new Mov(regToAddr.get(leaDest.getName()), Register.R10));
                }
            } else if (a instanceof zc246_zl345_co232_mw756.assembly.Set) {
                zc246_zl345_co232_mw756.assembly.Set set = (zc246_zl345_co232_mw756.assembly.Set) a;
                Expr setExpr = set.getExpr();
                if (setExpr instanceof Register && regToAddr.containsKey(((Register) setExpr).getName())) {
                    Register reg = (Register) setExpr;
                    toAssembly.add(new Mov(Register.R10, searchRegisters(reg)));
                    toAssembly.add(new zc246_zl345_co232_mw756.assembly.Set(Register.R10B, set.getType()));
                    toAssembly.add(new Movsx(Register.R10, Register.R10B));
                    toAssembly.add(new Mov(regToAddr.get(reg.getName()), Register.R10));
                } else {
                    toAssembly.add(a);
                }
            } else if (a instanceof Test) {
                Test test = (Test) a;
                if (test.getR1() instanceof Register) {
                    Register r1 = (Register) test.getR1();
                    if (regToAddr.containsKey(r1.getName()) && test.getR2() instanceof Mem) {
                        toAssembly.add(new Mov(Register.R10, searchRegisters(r1)));
                        toAssembly.add(new Test(Register.R10, test.getR2()));
                    } else {
                        toAssembly.add(a);
                    }
                } else if (test.getR2() instanceof Register) {
                    Register r2 = (Register) test.getR2();
                    if (regToAddr.containsKey(r2.getName()) && test.getR1() instanceof Mem) {
                        toAssembly.add(new Mov(Register.R10, searchRegisters(r2)));
                        toAssembly.add(new Test(test.getR1(), Register.R10));
                    } else {
                        toAssembly.add(a);
                    }
                } else {
                    toAssembly.add(a);
                }
            } else if (a instanceof UOP) {
                UOP uop = (UOP) a;
                if (regToAddr.containsKey(uop.getRegister().getName())) {
                    toAssembly.add(new Mov(Register.R10, searchRegisters(uop.getRegister())));
                    toAssembly.add(new UOP(uop.getType(), Register.R10));
                    toAssembly.add(new Mov(regToAddr.get(uop.getRegister().getName()), Register.R10));
                } else {
                    toAssembly.add(a);
                }
            } else if (a instanceof Cmp) {
                Cmp cmp = (Cmp) a;
                if (cmp.getR1() instanceof Register) {
                    Register cmpR1 = (Register) cmp.getR1();
                    boolean useR9 = false;
                    if (containsRegister(cmpR1) < 1) {
                        if (containsRegister(cmpR1) == -1) {
                            allocateRegister(cmpR1.getName());
                        }
                        toAssembly.add(new Mov(Register.R10, searchRegisters(cmpR1)));
                        useR9 = true;
                    }
                    if (cmp.getR2() instanceof Register && containsRegister((Register) cmp.getR2()) < 1) {
                        Register cmpR2 = (Register) cmp.getR2();
                        if (containsRegister(cmpR2) == -1) {
                            allocateRegister(cmpR2.getName());
                        }
                        toAssembly.add(new Mov(Register.R11, regToAddr.get(cmpR2.getName())));
                        toAssembly.add(new Cmp(useR9 ? Register.R10 : cmp.getR1(), Register.R11));
                    } else if (cmp.getR2() instanceof Mem) {
                        Mem memR2 = (Mem) cmp.getR2();
                        List<Assembly> memAlloc = naiveMemAllocation(memR2);
                        for (int i = 0; i < memAlloc.size() - 1; i++) {
                            toAssembly.add(memAlloc.get(i));
                        }
                        toAssembly.add(new Cmp(useR9 ? Register.R10 : cmp.getR1(), ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc()));
                    } else {
                        toAssembly.add(new Cmp(useR9 ? Register.R10 : cmp.getR1(), cmp.getR2()));
                    }
                } else if (cmp.getR2() instanceof Register) {
                    Register cmpR2 = (Register) cmp.getR2();
                    boolean useMem = false;
                    List<Assembly> memAlloc = null;
                    if (cmp.getR1() instanceof Mem) {
                        Mem memR1 = (Mem) cmp.getR1();
                        memAlloc = naiveMemAllocation(memR1);
                        for (int i = 0; i < memAlloc.size() - 1; i++) {
                            toAssembly.add(memAlloc.get(i));
                        }
                        useMem = true;
                    }
                    if (regToAddr.containsKey(cmpR2.getName())) {
                        toAssembly.add(new Mov(Register.R10, regToAddr.get(cmpR2.getName())));
                        toAssembly.add(new Cmp(useMem ? ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc() : cmp.getR1(), Register.R10));
                    } else {
                        toAssembly.add(new Cmp(useMem ? ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc() : cmp.getR1(), cmp.getR2()));
                    }
                } else if (cmp.getR1() instanceof Mem) {
                    Mem memR1 = (Mem) cmp.getR1();
                    List<Assembly> memAlloc = naiveMemAllocation(memR1);
                    for (int i = 0; i < memAlloc.size() - 1; i++) {
                        toAssembly.add(memAlloc.get(i));
                    }
                    toAssembly.add(new Cmp(((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc(), cmp.getR2()));
                } else if (cmp.getR2() instanceof Mem) {
                    Mem memR2 = (Mem) cmp.getR2();
                    List<Assembly> memAlloc = naiveMemAllocation(memR2);
                    for (int i = 0; i < memAlloc.size() - 1; i++) {
                        toAssembly.add(memAlloc.get(i));
                    }
                    toAssembly.add(new Cmp(cmp.getR1(), ((Mov) memAlloc.get(memAlloc.size() - 1)).getSrc()));
                } else {
                    toAssembly.add(a);
                }
            }else if(a instanceof Leave){
                toAssembly.add(new Mov(Register.R12, new Mem(Register.RBP, null, new Const(1), new Const(-8))));
                toAssembly.add(a);
            } else {
                // instruction does not require trivial register allocation
                toAssembly.add(a);
            }
        }
        // update stack pointer
        toAssembly.add(0, new Mov(new Mem(Register.RBP, null, new Const(1), new Const(-8L)), Register.R12));
        toAssembly.add(0, new Enter(new Const(allocateRegs * 8L+8L)));
        return toAssembly;
    }

    // search [genPurposeRegs] and [regToAddr] for register
    private Expr searchRegisters(Register register) {
        if (register == null) return null;
        String registerName = register.getName();
        if (genPurposeRegs.contains(registerName)) {
            return register;
        } else if (regToAddr.containsKey(registerName)) {
            return regToAddr.get(registerName);
        } else {
            throw new Error("Register " + register.getName() + " has not been allocated.");
        }
    }

    // check if [register] in either [genPurposeRegs] or [regToAddr]
    private int containsRegister(Register register) {
        if (regToAddr.containsKey(register.getName())) return 0;
        else if (genPurposeRegs.contains(register.getName())) return 1;
        else return -1;
    }

    private void allocateRegister(String registerName) {
        Mem allocMem = new Mem(Register.RBP, null, new Const(1), new Const(-8 * (allocateRegs + 2L)));
        allocateRegs++;
        regToAddr.put(registerName, allocMem);
    }

    // returns a boolean array of form [base, index] of registers that need trivial register allocation
    private List<Assembly> naiveMemAllocation(Mem mem) {
        List<Assembly> assembly = new ArrayList<>();
        Register memBase = mem.getBase();
        Register memIndex = mem.getIndex();
        boolean[] spilledRegs = {false, false};

        if (memBase != null && containsRegister(memBase) < 1) {
            if (containsRegister(memBase) == -1) {
                allocateRegister(memBase.getName());
            }
            assembly.add(new Mov(Register.R11, searchRegisters(memBase)));
            spilledRegs[0] = true;
        }
        if (memIndex != null && containsRegister(memIndex) < 1) {
            if (containsRegister(memIndex) == -1) {
                allocateRegister(memIndex.getName());
            }
            assembly.add(new Mov(Register.R12, searchRegisters(memIndex)));
            spilledRegs[1] = true;
        }
        assembly.add(new Mov(new Register("dummy_reg"), new Mem(spilledRegs[0] ? Register.R11 : memBase,
                spilledRegs[1] ? Register.R12 : memIndex, mem.getScale(), mem.getDisplacement())));

        return assembly;
    }
}
