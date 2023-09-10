package zc246_zl345_co232_mw756.opt;

import polyglot.util.Pair;
import zc246_zl345_co232_mw756.assembly.*;

import java.util.Set;
import java.util.*;

import static java.util.Map.entry;

public class RegisterAllocation {

    // Node work lists, sets, and stacks
    protected static final Set<Register> precolored = Set.of(Register.RAX, Register.RBX, Register.RCX, Register.RDX,
            Register.RSI, Register.RSP, Register.RBP, Register.RDI, Register.R8, Register.R9, Register.R10,
            Register.R11, Register.R12, Register.R13, Register.R14, Register.R15);
    private Set<Register> initial;
    private Set<Register> simplifyWorklist;
    private Set<Register> freezeWorklist;
    private Set<Register> spillWorklist;
    private Set<Register> spilledNodes;
    private Set<Register> coalescedNodes;
    private Set<Register> coloredNodes;
    private Stack<Register> selectStack;

    // Move sets
    private Set<Node<Assembly>> coalescedMoves;
    private Set<Node<Assembly>> constrainedMoves;
    private Set<Node<Assembly>> frozenMoves;
    private Set<Node<Assembly>> worklistMoves;
    private Set<Node<Assembly>> activeMoves;

    // Other
    private Set<Pair<Register, Register>> adjSet;
    private Map<String, HashSet<Register>> adjList;
    private Map<Register, Integer> degree;
    private Map<Register, HashSet<Node<Assembly>>> moveList;
    private Map<Register, Register> alias;
    private Map<Register, Integer> color;
    private static final int K = 16;
    private static final Set<Integer> availableColors = new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15));
    private final Random rand = new Random();

    private Map<Register, Integer> spilledAddr;
    private CFG<Assembly> cfg;
    private LiveVariable lv;
    private static final Map<Integer, Register> colorToReg = Map.ofEntries(
            entry(0, Register.RAX), entry(1, Register.RBX), entry(2, Register.RCX),
            entry(3, Register.RDX), entry(4, Register.RSI), entry(5, Register.RSP),
            entry(6, Register.RBP), entry(7, Register.RDI), entry(8, Register.R8), entry(9, Register.R9),
            entry(10, Register.R10), entry(11, Register.R11), entry(12, Register.R12),
            entry(13, Register.R13), entry(14, Register.R14), entry(15, Register.R15)
    );
    private List<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> blocks;

    public RegisterAllocation(CFG<Assembly> cfg) {
        this.cfg = cfg;
        // Node work lists, sets, and stacks
        simplifyWorklist = new HashSet<>();
        freezeWorklist = new HashSet<>();
        spillWorklist = new HashSet<>();
        spilledNodes = new HashSet<>();
        coalescedNodes = new HashSet<>();
        coloredNodes = new HashSet<>();
        selectStack = new Stack<>();
        // Move sets
        coalescedMoves = new HashSet<>();
        constrainedMoves = new HashSet<>();
        frozenMoves = new HashSet<>();
        worklistMoves = new HashSet<>();
        activeMoves = new HashSet<>();
        // Other
        adjSet = new HashSet<>();
        adjList = new HashMap<>();
        degree = new HashMap<>();
        moveList = new HashMap<>();
        alias = new HashMap<>();
        color = new HashMap<>();
        spilledAddr = new HashMap<>();
        initializeColor();
    }

    public Const Main() {
        // Liveness Analysis
        lv = new LiveVariable(cfg);
        lv.run();
        zc246_zl345_co232_mw756.assembly.BasicBlock bb = new zc246_zl345_co232_mw756.assembly.BasicBlock();
        zc246_zl345_co232_mw756.assembly.BasicBlock.Block b = bb.makeBasicBlock(cfg);
        blocks = getBlocks(b);
        initial = new HashSet<>(lv.initial);

        //alloc
        Build();
        MakeWorklist();
        while (!(simplifyWorklist.size() == 0 && worklistMoves.size() == 0 && freezeWorklist.size() == 0 && spillWorklist.size() == 0)) {
            if (simplifyWorklist.size() > 0) {
                Simplify();
            } else if (worklistMoves.size() > 0) {
                Coalesce();
            } else if (freezeWorklist.size() > 0) {
                Freeze();
            } else if (spillWorklist.size() > 0) {
                SelectSpill();
            }
        }
        AssignColors();

        if (spilledNodes.size() > 0) {
            // Rewrite program
            RewriteProgram();
            return Main();
        }else{
            RecolorInstruction();
            return new Const(spilledAddr.size()*8L);
        }
    }

    private static List<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> getBlocks(zc246_zl345_co232_mw756.assembly.BasicBlock.Block startBlock) {
        Queue<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> queue = new LinkedList<>(Arrays.asList(startBlock));
        Set<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> visited = new LinkedHashSet<>();
        while (queue.size() > 0) {
            zc246_zl345_co232_mw756.assembly.BasicBlock.Block head = queue.poll();
            if (visited.contains(head)) {
                continue;
            }
            visited.add(head);
            queue.addAll(head.getNext());
        }
        ArrayList<zc246_zl345_co232_mw756.assembly.BasicBlock.Block> blocks = new ArrayList<>();
        for (zc246_zl345_co232_mw756.assembly.BasicBlock.Block b : visited) {
            if (b.getBlock().size() > 0)
                blocks.add(b);
        }
        return blocks;
    }

    // [registerMove] checks that both the source and destination of a move are registers
    private boolean registerMove(Assembly stmt) {
        if (stmt instanceof Mov) {
            Mov mov = (Mov) stmt;
            return mov.getSrc() instanceof Register && mov.getDest() instanceof Register;
        }
        return false;
    }

    private void Build() {
        for (zc246_zl345_co232_mw756.assembly.BasicBlock.Block block : blocks) {
            List<Node<Assembly>> blockList = block.getBlock();
            Collections.reverse(blockList);
            Set<Register> live = new HashSet<>(lv.out.get(blockList.get(0)));
            for (Node<Assembly> node : blockList) {
                if (registerMove(node.stmt)) {
                    Mov mov = (Mov) node.stmt;
                    Register movSrc = (Register) mov.getSrc();
                    Register movDest = (Register) mov.getDest();
                    live.remove(movSrc);
                    Pair<Register, Register> movePair = new Pair<>(movSrc, movDest);
                    // fix moveList instruction below
                    moveList.putIfAbsent(movSrc, new HashSet<>());
                    moveList.putIfAbsent(movDest, new HashSet<>());
                    moveList.get(movSrc).add(node);
                    moveList.get(movDest).add(node);
                    worklistMoves.add(node);
                }
                live.addAll(lv.nodeDefs.get(node));
                for (Register d : lv.nodeDefs.get(node)) {
                    for (Register l : live) {
                        AddEdge(l, d);
                    }
                }
                live.removeAll(lv.nodeDefs.get(node));
                live.addAll(lv.nodeUses.get(node));
            }
        }

        HashSet<Node<Assembly>> precoloredWorkListMoves = new LinkedHashSet<>();
        HashSet<Node<Assembly>> tempWorkListMoves = new HashSet<>();
        for (Node<Assembly> node : worklistMoves) {
//            Node<Assembly> node = (Node<Assembly>) n;
            Mov mov = (Mov) node.stmt;
            Register src = (Register) mov.getSrc();
            Register dest = (Register) mov.getDest();
            if (precolored.contains(src) || precolored.contains(dest)) {
                precoloredWorkListMoves.add(node);
            } else {
                tempWorkListMoves.add(node);
            }
        }
        precoloredWorkListMoves.addAll(tempWorkListMoves);
        worklistMoves = precoloredWorkListMoves;
    }

    private void SelectSpill() {
        Register m = spillWorklist.iterator().next();
        spillWorklist.remove(m);
        simplifyWorklist.add(m);
        FreezeMoves(m);
    }

    private void AssignColors() {
        while (selectStack.size() > 0) {
            Register n = selectStack.pop();
            HashSet<Integer> okColors = new HashSet<>(availableColors);
            if (precolored.contains(GetAlias(n)) || coloredNodes.contains(GetAlias(n))) {
                continue;
            }
            for (Register w : adjList.getOrDefault(n.getName(), new HashSet<>())) {
                if (coloredNodes.contains(GetAlias(w)) || precolored.contains(GetAlias(w))) {
                    okColors.remove(color.get(GetAlias(w)));
                }
            }
            if (okColors.size() == 0) {
                spilledNodes.add(n);
            } else {
                coloredNodes.add(n);
                color.put(n, okColors.iterator().next());
            }
        }
        for (Register n : coalescedNodes) {
            color.put(n, color.get(GetAlias(n)));
        }
    }


    private void AddEdge(Register u, Register v) {
        if (!adjSet.contains(new Pair<>(u, v)) && !u.equals(v)) {
            adjSet.addAll(Arrays.asList(new Pair<>(u, v), new Pair<>(v, u)));
            if (!precolored.contains(u)) {
                adjList.putIfAbsent(u.getName(), new HashSet<>());
                adjList.get(u.getName()).add(v);
                degree.put(u, degree.getOrDefault(u, 0) + 1);
            }
            if (!precolored.contains(v)) {
                adjList.putIfAbsent(v.getName(), new HashSet<>());
                adjList.get(v.getName()).add(u);
                degree.put(v, degree.getOrDefault(v, 0) + 1);
            }
        }
    }

    private void MakeWorklist() {
        Set<Register> initialCopy = new HashSet<>(initial);
        for (Register n : initialCopy) {
            initial.remove(n);
            if (degree.get(n) >= K) {
                spillWorklist.add(n);
            } else if (MoveRelated(n)) {
                freezeWorklist.add(n);
            } else {
                simplifyWorklist.add(n);
            }
        }
    }

    private HashSet<Register> Adjacent(Register n) {
        HashSet<Register> adjacent = new HashSet<>(adjList.getOrDefault(n.getName(), new HashSet<>()));
        selectStack.forEach(adjacent::remove);
        adjacent.removeAll(coalescedNodes);
        return adjacent;
    }

    private Set<Node<Assembly>> NodeMoves(Register n) {
        Set<Node<Assembly>> nodeMoves = new HashSet<>(activeMoves);
        // activeMoves union worklistMoves
        nodeMoves.addAll(worklistMoves);
        // moveList[n] intersection nodeMoves
        nodeMoves.retainAll(moveList.getOrDefault(n, new HashSet<>()));
        return nodeMoves;
    }

    private boolean MoveRelated(Register n) {
        return NodeMoves(n).size() > 0;
    }

    private void Simplify() {
        assert simplifyWorklist.size() > 0;
//        Register n = (Register) simplifyWorklist.toArray()[0];
        Register n = simplifyWorklist.iterator().next();
        simplifyWorklist.remove(n);
        selectStack.push(n);
        for (Register m : Adjacent(n)) {
            DecrementDegree(m);
        }
    }

    private void DecrementDegree(Register m) {
        int d = degree.get(m);
        assert d > 0;
        degree.put(m, d - 1);
        if (d == K) {
            HashSet<Register> nodes = new HashSet<>(Adjacent(m));
            nodes.add(m);
            EnableMoves(nodes);
            spillWorklist.remove(m);
            if (MoveRelated(m)) {
                freezeWorklist.add(m);
            } else {
                simplifyWorklist.add(m);
            }
        }
    }

    private void EnableMoves(Set<Register> nodes) {
        for (Register n : nodes) {
            for (Node<Assembly> m : NodeMoves(n)) {
                if (activeMoves.contains(m)) {
                    activeMoves.remove(m);
                    worklistMoves.add(m);
                }
            }
        }
    }

    private void AddWorkList(Register u) {
        if (!precolored.contains(u) && !MoveRelated(u) && degree.get(u) < K) {
            freezeWorklist.remove(u);
            simplifyWorklist.add(u);
        }
    }

    private boolean OK(Register t, Register r) {
        return degree.getOrDefault(t, 0) < K || precolored.contains(t) || adjSet.contains(new Pair<>(t, r));
    }


    private boolean Conservative(Set<Register> nodes) {
        int k = 0;
        for (Register n : nodes) {
            if (degree.getOrDefault(n, 0) >= K) {
                k++;
            }
        }
        return k < K;
    }

    private void Coalesce() {
        assert worklistMoves.size() > 0;
        Node<Assembly> mov = worklistMoves.iterator().next();
        Mov m = (Mov) mov.stmt;
        Register x = GetAlias((Register) m.getDest());
        Register y = GetAlias((Register) m.getSrc());
        Register u, v;
        if (precolored.contains(y)) {
            u = y;
            v = x;
        } else {
            u = x;
            v = y;
        }
        worklistMoves.remove(mov);

        Set<Register> adjUV = new HashSet<>(Adjacent(u));
        adjUV.addAll(Adjacent(v));

        if (u.equals(v)) {
            coalescedMoves.add(mov);
            AddWorkList(u);
        } else if (precolored.contains(v) || adjSet.contains(new Pair<>(u, v))) {
            constrainedMoves.add(mov);
            AddWorkList(u);
            AddWorkList(v);
        } else if ((precolored.contains(u) && Adjacent(v).stream().allMatch(t -> OK(t, u))) || (!precolored.contains(u) && Conservative(adjUV))) {
            coalescedMoves.add(mov);
            Combine(u, v);
            AddWorkList(u);
        } else {
            activeMoves.add(mov);
        }
    }

    private void Combine(Register u, Register v) {
        if (freezeWorklist.contains(v)) {
            freezeWorklist.remove(v);
        } else {
            spillWorklist.remove(v);
        }
        coalescedNodes.add(v);
        alias.put(v, u);
        moveList.putIfAbsent(u, new HashSet<>());
        moveList.get(u).addAll(moveList.getOrDefault(v, new HashSet<>()));
        EnableMoves(new HashSet<>(Collections.singletonList(v)));
        for (Register t : Adjacent(v)) {
            AddEdge(t, u);
            DecrementDegree(t);
        }
        if (degree.getOrDefault(u, 0) >= K && freezeWorklist.contains(u)) {
            freezeWorklist.remove(u);
            spillWorklist.add(u);
        }
    }

    private Register GetAlias(Register n) {
        if (coalescedNodes.contains(n)) {
            return GetAlias(alias.get(n));
        } else {
            return n;
        }
    }

    private void Freeze() {
        assert freezeWorklist.size() > 0;
        Register u = freezeWorklist.iterator().next();
        freezeWorklist.remove(u);
        simplifyWorklist.add(u);
        FreezeMoves(u);
    }

    private void FreezeMoves(Register u) {
        for (Node<Assembly> mov : NodeMoves(u)) {
            Mov m = (Mov) mov.stmt;
            Register x = (Register) m.getDest();
            Register y = (Register) m.getSrc();
            Register v;
            if (GetAlias(y).equals(GetAlias(u))) {
                v = GetAlias(x);
            } else {
                v = GetAlias(y);
            }
            activeMoves.remove(mov);
            frozenMoves.add(mov);
            if (freezeWorklist.contains(v) && NodeMoves(v).isEmpty()) {
                freezeWorklist.remove(v);
                simplifyWorklist.add(v);
            }
        }
    }

    private void RewriteProgram() {
//        spilledAddr.clear();
//        int i = 8;
        for (Register r:spilledNodes){
            spilledAddr.put(r, spilledAddr.size() * 8 + 8);
        }

        Map<Register, Set<Register>> newTemps = new HashMap<>();
        Set<Node<Assembly>> visited = new HashSet<>();
        Queue<Node<Assembly>> frontier = new ArrayDeque<>();
        frontier.add(this.cfg.start);
        while(!frontier.isEmpty()){
            Node<Assembly> n = frontier.poll();
            if (visited.contains(n)) continue;
            visited.add(n);
            for(Node<Assembly> s:n.next){
                frontier.offer(s);
            }
            Set<Register> defs = new HashSet<>(lv.nodeDefs.get(n));
            defs.retainAll(spilledNodes);
            Set<Register> uses = new HashSet<>(lv.nodeUses.get(n));
            uses.retainAll(spilledNodes);
            List<Node<Assembly>> addBefore = new ArrayList<>();
            List<Node<Assembly>> addAfter = new ArrayList<>();
            List<Register> oldRegs = new ArrayList<>();
            List<Register> replacements = new ArrayList<>();
            Set<Register> intersect = new HashSet<>(defs);
            intersect.retainAll(uses);
            defs.removeAll(intersect);
            uses.removeAll(intersect);
            for(Register r:intersect){
                newTemps.putIfAbsent(r, new HashSet<>());
                Register newTemp = Register.getReg();
                oldRegs.add(r);
                replacements.add(newTemp);
                newTemps.get(r).add(newTemp);
                addBefore.add(new Node<>(new Mov(newTemp, new Mem(Register.RBP, null, new Const(1L), new Const(-spilledAddr.get(r))))));
                addAfter.add(new Node<>(new Mov(new Mem(Register.RBP, null, new Const(1L), new Const(-spilledAddr.get(r))), newTemp)));
            }
            for(Register r: defs){
                newTemps.putIfAbsent(r, new HashSet<>());
                Register newTemp = Register.getReg();
                oldRegs.add(r);
                replacements.add(newTemp);
                newTemps.get(r).add(newTemp);
                addAfter.add(new Node<>(new Mov(new Mem(Register.RBP, null, new Const(1L), new Const(-spilledAddr.get(r))), newTemp)));
            }
            for(Register r: uses){
                newTemps.putIfAbsent(r, new HashSet<>());
                Register newTemp = Register.getReg();
                oldRegs.add(r);
                replacements.add(newTemp);
                newTemps.get(r).add(newTemp);
                addBefore.add(new Node<>(new Mov(newTemp, new Mem(Register.RBP, null, new Const(1L), new Const(-spilledAddr.get(r))))));
            }
            if(n.stmt!=null) n.stmt.replaceReg(oldRegs, replacements);
            if(!addBefore.isEmpty()){
                assembleList(addBefore);
                for(Node<Assembly> p: n.prev){
                    p.next.remove(n);
                    p.next.add(addBefore.get(0));
                    addBefore.get(0).prev.add(p);
                }
                n.prev.clear();
                addBefore.get(addBefore.size()-1).next.add(n);
                n.prev.add(addBefore.get(addBefore.size()-1));
            }
            if(!addAfter.isEmpty()){
                assembleList(addAfter);
                for(Node<Assembly> s: n.next){
                    s.prev.remove(n);
                    s.prev.add(addAfter.get(addAfter.size()-1));
                    addAfter.get(addAfter.size()-1).next.add(s);
                }
                n.next.clear();
                addAfter.get(0).prev.add(n);
                n.next.add(addAfter.get(0));
            }
            cfg.nodes.addAll(addBefore);
            cfg.nodes.addAll(addAfter);
        }
        
        spilledNodes.clear();
        initial.clear();
        initial.addAll(coloredNodes);
        initial.addAll(coalescedNodes);
        for(Set<Register> regs : newTemps.values()){
            initial.addAll(regs);
        }
        coloredNodes.clear();
        coalescedNodes.clear();
    }

    private void assembleList(List<Node<Assembly>> nodes){
        Node<Assembly> head = null;
        Node<Assembly> tail = null;
        for(Node<Assembly> n: nodes){
            if (head == null){
                head = n;
                tail = n;
            } else {
                tail.next.add(n);
                n.prev.add(tail);
                tail = n;
            }
        }
    }

    private void RecolorInstruction() {
        for (Node<Assembly> n : this.cfg.nodes) {
            Assembly stmt = n.stmt;
            if (stmt instanceof BOP) {
                BOP bop = (BOP) stmt;
                Expr bopDest = recolorExpr(bop.getDest());
                Expr bopSrc = recolorExpr(bop.getSrc());
                n.stmt = new BOP(bop.getBOPType(), bopDest, bopSrc);
            } else if (stmt instanceof Mov) {
                Mov mov = (Mov) stmt;
                Expr movDest = recolorExpr(mov.getDest());
                Expr movSrc = recolorExpr(mov.getSrc());
                if (movDest.equals(movSrc)) {
                    for (Node<Assembly> prev : n.prev) {
                        prev.next.remove(n);
                        for (Node<Assembly> next : n.next) {
                            next.prev.add(prev);
                        }
                    }
                    for (Node<Assembly> next : n.next) {
                        next.prev.remove(n);
                        for (Node<Assembly> prev : n.prev) {
                            prev.next.add(next);
                        }
                    }
                }
                n.stmt = new Mov(movDest, movSrc);
            } else if (stmt instanceof Cmp) {
                Cmp cmp = (Cmp) stmt;
                Expr cmpR1 = recolorExpr(cmp.getR1());
                Expr cmpR2 = recolorExpr(cmp.getR2());
                n.stmt = new Cmp(cmpR1, cmpR2);
            } else if (stmt instanceof Dec) {
                Dec dec = (Dec) stmt;
                Expr decReg = recolorExpr(dec.getReg());
                n.stmt = new Dec(decReg);
            } else if (stmt instanceof Inc) {
                Inc inc = (Inc) stmt;
                Expr incReg = recolorExpr(inc.getReg());
                n.stmt = new Inc(incReg);
            } else if (stmt instanceof Lea) {
                Lea lea = (Lea) stmt;
                Register leaDest = colorToReg.get(color.get(lea.getDest()));
                Expr leaExpr = recolorExpr(lea.getExpr());
                n.stmt = new Lea(leaDest, leaExpr);
            } else if (stmt instanceof Pop) {
                Pop pop = (Pop) stmt;
                Expr popReg = recolorExpr(pop.getReg());
                n.stmt = new Pop(popReg);
            } else if (stmt instanceof Push) {
                Push push = (Push) stmt;
                Expr pushReg = recolorExpr(push.getReg());
                n.stmt = new Push(pushReg);
            } else if (stmt instanceof zc246_zl345_co232_mw756.assembly.Set) {
                zc246_zl345_co232_mw756.assembly.Set set = (zc246_zl345_co232_mw756.assembly.Set) stmt;
                Register b64 = (Register) recolorExpr(set.getExpr());
                Register b8 = Register.to8Bits.get(b64);
                Node<Assembly> next = new Node<>(new Movsx(b64, b8));
                n.stmt = new zc246_zl345_co232_mw756.assembly.Set(b8, set.getType());
                next.next = new ArrayList<>(n.next);
                next.prev = new ArrayList<>(List.of(n));
                for(Node<Assembly> nextNode: n.next){
                    nextNode.prev.remove(n);
                    nextNode.prev.add(next);
                }
                n.next.clear();
                n.next.add(next);
            } else if (stmt instanceof Test) {
                Test test = (Test) stmt;
                Expr testR1 = recolorExpr(test.getR1());
                Expr testR2 = recolorExpr(test.getR2());
                n.stmt = new Test(testR1, testR2);
            } else if (stmt instanceof UOP) {
                UOP uop = (UOP) stmt;
                Register uopReg = colorToReg.get(color.get(uop.getRegister()));
                n.stmt = new UOP(uop.getType(), uopReg);
            } else {
                // do nothing
                // Call, Comment, Const, CQO, Data, Directives, Expr, Function, Jump, Label, Mem, Movsx, Offset, Register, Ret, Leave, Enter
            }
        }
    }

    private Expr recolorExpr(Expr e) {
        if (e instanceof Register && color.containsKey((Register) e)) {
            return colorToReg.get(color.get((Register) e));
        } else if (e instanceof Mem) {
            Mem mem = (Mem) e;
            Register memBase = mem.getBase();
            Register memIndex = mem.getIndex();
            if (memBase != null) {
                memBase = colorToReg.get(color.get(memBase));
            }
            if (memIndex != null) {
                memIndex = colorToReg.get(color.get(memIndex));
            }
            return new Mem(memBase, memIndex, mem.getScale(), mem.getDisplacement());
        } else {
            return e;
        }
    }

    public CFG<Assembly> getCFG() {
        return this.cfg;
    }

    private void initializeColor() {
        color.put(Register.RAX, 0);
        color.put(Register.RBX, 1);
        color.put(Register.RCX, 2);
        color.put(Register.RDX, 3);
        color.put(Register.RSI, 4);
        color.put(Register.RSP, 5);
        color.put(Register.RBP, 6);
        color.put(Register.RDI, 7);
        color.put(Register.R8, 8);
        color.put(Register.R9, 9);
        color.put(Register.R10, 10);
        color.put(Register.R11, 11);
        color.put(Register.R12, 12);
        color.put(Register.R13, 13);
        color.put(Register.R14, 14);
        color.put(Register.R15, 15);
        for (Register reg : precolored) {
            degree.put(reg, Integer.MAX_VALUE);
        }
    }
}
