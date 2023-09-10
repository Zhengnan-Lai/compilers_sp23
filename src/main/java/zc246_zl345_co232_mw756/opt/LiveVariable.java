package zc246_zl345_co232_mw756.opt;

import zc246_zl345_co232_mw756.assembly.*;
import static zc246_zl345_co232_mw756.assembly.Register.ARGUMENTS;

import java.util.*;
import java.util.Set;

public class LiveVariable extends DataFlow<Assembly, Set<Register>> {
    protected HashMap<Node<Assembly>, Set<Register>> nodeDefs;
    protected HashMap<Node<Assembly>, Set<Register>> nodeUses;
    protected Set<Register> initial;

    public LiveVariable(CFG<Assembly> cfg) {
        super(1, cfg);
        nodeDefs = new HashMap<>();
        nodeUses = new HashMap<>();
        initial = new HashSet<>();
        setUseDef();
    }

    public Set<Register> transfer(Node<Assembly> node, Set<Register> value) {
        HashSet<Register> result = new HashSet<>(value);
        result.removeAll(nodeDefs.get(node));
        result.addAll(nodeUses.get(node));
        return result;
    }

    public Set<Register> top() {
        return new HashSet<>();
    }

    protected Assembly convert(Node<Assembly> node) {
        return node.stmt;
    }

    public Set<Register> meet(Set<Register> v1, Set<Register> v2) {
        Set<Register> union = new HashSet<>(v1);
        union.addAll(v2);
        return union;
    }

    private void setUseDef() {
        for (Node<Assembly> node : cfg.nodes) {
            setUseDefNode(node);
        }
    }

    private void setUseDefNode(Node<Assembly> node) {
        Assembly stmt = node.stmt;
        Set<Register> usedVars = new HashSet<>();
        Set<Register> defVars = new HashSet<>();
        if (stmt instanceof BOP) {
            BOP bop = (BOP) stmt;
            if (bop.getDest() instanceof Register) {
                Register bopDest = (Register) bop.getDest();
                defVars.add(bopDest);
                usedVars.add(bopDest);
            } else if (bop.getDest() instanceof Mem) {
                usedVars.addAll(usedExpr(bop.getDest()));
            }
            usedVars.addAll(usedExpr(bop.getSrc()));

            initialRegs(bop.getDest());
            initialRegs(bop.getSrc());
        } else if (stmt instanceof Mov) {
            Mov mov = (Mov) stmt;
            if (mov.getDest() instanceof Register) {
                defVars.add((Register) mov.getDest());
            } else if (mov.getDest() instanceof Mem) {
                usedVars.addAll(usedExpr(mov.getDest()));
            }
            usedVars.addAll(usedExpr(mov.getSrc()));

            initialRegs(mov.getDest());
            initialRegs(mov.getSrc());
        } else if (stmt instanceof Cmp) {
            Cmp cmp = (Cmp) stmt;
            usedVars.addAll(usedExpr(cmp.getR1()));
            usedVars.addAll(usedExpr(cmp.getR2()));

            initialRegs(cmp.getR1());
            initialRegs(cmp.getR2());
        } else if (stmt instanceof Dec) {
            Dec dec = (Dec) stmt;
            if (dec.getReg() instanceof Register) {
                defVars.add((Register) dec.getReg());
            } else if (dec.getReg() instanceof Mem) {
                usedVars.addAll(usedExpr(dec.getReg()));
            }
            usedVars.addAll(usedExpr(dec.getReg()));

            initialRegs(dec.getReg());
        } else if (stmt instanceof Inc) {
            Inc inc = (Inc) stmt;
            if (inc.getReg() instanceof Register) {
                defVars.add((Register) inc.getReg());
            } else if (inc.getReg() instanceof Mem) {
                usedVars.addAll(usedExpr(inc.getReg()));
            }
            usedVars.addAll(usedExpr(inc.getReg()));

            initialRegs(inc.getReg());
        } else if (stmt instanceof Lea) {
            Lea lea = (Lea) stmt;
            defVars.add(lea.getDest());
            usedVars.addAll(usedExpr(lea.getExpr()));

            initialRegs(lea.getDest());
            initialRegs(lea.getExpr());
        } else if (stmt instanceof Pop) {
            Pop pop = (Pop) stmt;
            if (pop.getReg() instanceof Register) {
                defVars.add((Register) pop.getReg());
            } else if (pop.getReg() instanceof Mem) {
                usedVars.addAll(usedExpr(pop.getReg()));
            }
            defVars.add(Register.RSP);
            usedVars.add(Register.RSP);

            initialRegs(pop.getReg());
        } else if (stmt instanceof Push) {
            Push push = (Push) stmt;
            if (push.getReg() instanceof Register || push.getReg() instanceof Mem) {
                usedVars.addAll(usedExpr(push.getReg()));
            }
            defVars.add(Register.RSP);
            usedVars.add(Register.RSP);

            initialRegs(push.getReg());
        } else if (stmt instanceof zc246_zl345_co232_mw756.assembly.Set) {
            zc246_zl345_co232_mw756.assembly.Set set = (zc246_zl345_co232_mw756.assembly.Set) stmt;
            if (set.getExpr() instanceof Register) {
                defVars.add((Register) set.getExpr());
            } else if (set.getExpr() instanceof Mem) {
                usedVars.addAll(usedExpr(set.getExpr()));
            }

            initialRegs(set.getExpr());
        } else if (stmt instanceof Test) {
            Test test = (Test) stmt;
            usedVars.addAll(usedExpr(test.getR1()));
            usedVars.addAll(usedExpr(test.getR2()));

            initialRegs(test.getR1());
            initialRegs(test.getR2());
        } else if (stmt instanceof UOP) {
            UOP uop = (UOP) stmt;
            UOP.Type type = uop.getType();
            defVars.add(uop.getRegister());
            usedVars.add(uop.getRegister());
            if (type == UOP.Type.IMUL || type == UOP.Type.MUL) {
                usedVars.add(Register.RAX);
                defVars.addAll(Arrays.asList(Register.RAX, Register.RDX));
            } else if (type == UOP.Type.IDIV || type == UOP.Type.DIV) {
                usedVars.addAll(Arrays.asList(Register.RAX, Register.RDX));
                defVars.addAll(Arrays.asList(Register.RAX, Register.RDX));
            }
            initialRegs(uop.getRegister());
        } else if (stmt instanceof Leave) {
            defVars.addAll(Arrays.asList(Register.RBP, Register.RSP));
            usedVars.add(Register.RBP);
        } else if (stmt instanceof Enter) {
            defVars.addAll(Arrays.asList(Register.RBP, Register.RSP));
            usedVars.addAll(Arrays.asList(Register.RBP, Register.RSP));
        } else if (stmt instanceof Call) {
            defVars.addAll(Arrays.asList(Register.RAX, Register.RCX, Register.RDX, Register.RSI, Register.RDI, Register.R8, Register.R9, Register.R10, Register.R11));
            Call c = (Call) stmt;
            usedVars.addAll(ARGUMENTS.subList(0, Math.min(c.n_args+(c.n_rets > 2?1:0), 6)));
        } else if (stmt instanceof Ret) {
            usedVars.addAll(Arrays.asList(Register.RSP, Register.RBX, Register.RBP, Register.R12, Register.R13, Register.R14, Register.R15));
            int retSize = ((Ret) stmt).retSize;
            if(retSize>0) usedVars.add(Register.RAX);
            if(retSize>1) usedVars.add(Register.RDX);
        } else if (stmt instanceof CQO) {
            usedVars.add(Register.RAX);
            defVars.addAll(Arrays.asList(Register.RDX, Register.RAX));
        } else if (node == cfg.start) {
            defVars.addAll(Arrays.asList(Register.RSP, Register.RBX, Register.RBP, Register.R12, Register.R13, Register.R14, Register.R15));
        } else {
            // do nothing
            // Comment, Const, Data, Directives, Expr, Function, Jump, Label, Mem, Movsx, Offset, Register
        }
        nodeDefs.put(node, defVars);
        nodeUses.put(node, usedVars);
    }

    private Set<Register> usedExpr(Expr expr) {
        Set<Register> usedRegisters = new HashSet<>();
        if (expr instanceof Register) {
            usedRegisters.add((Register) expr);
        } else if (expr instanceof Mem) {
            Mem mem = (Mem) expr;
            if (mem.getBase() != null) usedRegisters.add(mem.getBase());
            if (mem.getIndex() != null) usedRegisters.add(mem.getIndex());
        }
        return usedRegisters;
    }

    private void initialRegs(Expr expr) {
        if (expr instanceof Register) {
            Register reg = (Register) expr;
            if (!RegisterAllocation.precolored.contains(reg))
                initial.add(reg);
        } else if (expr instanceof Mem) {
            Mem mem = (Mem) expr;
            if (mem.getBase() != null) {
                Register base = mem.getBase();
                if (!RegisterAllocation.precolored.contains(base))
                    initial.add(base);
            }
            if (mem.getIndex() != null) {
                Register index = mem.getIndex();
                if (!RegisterAllocation.precolored.contains(index))
                    initial.add(index);
            }
        }
    }
}
