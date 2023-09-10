package edu.cornell.cs.cs4120.xic.ir.visit;

import edu.cornell.cs.cs4120.xic.ir.*;

import java.util.*;

public class BasicBlock{

    public class Block{
        private final List<IRStmt> stmts;
        private List<IRLabel> next;
        private final List<IRLabel> from;

        public Block(List<IRStmt> stmts, List<IRLabel> next){
            this.stmts = stmts;
            this.next = next;
            this.from = new ArrayList<>();
        }

        public Block(List<IRStmt> stmts, List<IRLabel> next, List<IRLabel> from){
            this.stmts = stmts;
            this.next = next;
            this.from = from;
        }

        public IRStmt lastStmt(){
            return stmts.get(stmts.size()-1);
        }

        public void removeLast(){
            stmts.remove(stmts.size()-1);
        }

        public void removeFirst(){
            stmts.remove(0);
        }

        public IRLabel label(){
            return (IRLabel) stmts.get(0);
        }
    }

    private Map<IRLabel, Block> blocks;
    // maximum blocks should always start with a label
    private IRLabel func_start;
    private Long count = 0L;
    private IRCompUnit cUnit;
    private Map<String, List<Block>> blockMap;

    public BasicBlock(IRCompUnit cUnit) {
        this.cUnit = cUnit;
        this.blockMap = new HashMap<>();
    }

    public IRCompUnit createBlocks(){
        for(Map.Entry<String, IRFuncDecl> entry:cUnit.functions().entrySet()){
            createBlocks(entry.getValue());
            List<Block> b = fixJump(greedyReorder());
            blockMap.put(entry.getKey(), b);
            List<IRStmt> stmts = toCode(b);
            cUnit.functions().put(entry.getKey(), new IRFuncDecl(entry.getValue().name(), new IRSeq(stmts)));
        }
        return cUnit;
    }

    public IRCompUnit valueNumbering(){
        for(Map.Entry<String, IRFuncDecl> entry:cUnit.functions().entrySet()){
            List<Block> b = blockMap.get(entry.getKey());
            List<IRStmt> stmts = toCode(valueNumbering(b));
            cUnit.functions().put(entry.getKey(), new IRFuncDecl(entry.getValue().name(), new IRSeq(stmts)));
        }
        return cUnit;
    }

    private void createBlocks(IRFuncDecl func){
        assert func.body() instanceof IRSeq;
        blocks = new HashMap<>();
        IRSeq body = (IRSeq) func.body();

        List<IRStmt> block = new ArrayList<>();
        List<IRLabel> next = new ArrayList<>();
        IRLabel first = (IRLabel) body.stmts().get(0);
        block.add(body.stmts().get(0));
        func_start = first;
        for(int i=1;i<body.stmts().size();i++){
            IRStmt stmt = body.stmts().get(i);
            if(stmt instanceof IRLabel){
                if(!(block.get(block.size()-1) instanceof IRReturn) && next.size()==0)
                    next.add((IRLabel) stmt);
                blocks.put(first, new Block(block, next));
                block = new ArrayList<>();
                next = new ArrayList<>();
                first = (IRLabel) stmt;
            }else if(stmt instanceof IRJump) {
                IRJump jump = (IRJump) stmt;
                IRLabel label = new IRLabel(((IRName) jump.target()).name());
                next.add(label);
            }else if (stmt instanceof IRCJump){
                IRCJump cjump = (IRCJump) stmt;
                next.add(new IRLabel(cjump.falseLabel()));
                next.add(new IRLabel(cjump.trueLabel()));
            }
            block.add(stmt);
        }
        if(!block.isEmpty()){
            blocks.put(first, new Block(block, next));
        }

        for(Map.Entry<IRLabel, Block> entry:blocks.entrySet()){
            for(IRLabel label:entry.getValue().next){
                if(blocks.get(label)!=null)
                    blocks.get(label).from.add(entry.getKey());
            }
        }
    }

    private List<Block> greedyReorder(){
        IRLabel root = func_start;
        Set<IRLabel> labels = new HashSet<>(blocks.keySet());
        List<Block> blocks = new ArrayList<>();
        while(!labels.isEmpty()){
            Block b = this.blocks.get(root);
            blocks.add(b);
            labels.remove(root);
            boolean done = !labels.isEmpty();
            for(IRLabel l:b.next){
                if(labels.contains(l)){
                    root = l;
                    done = false;
                    break;
                }
            }
            if(done){
                root = getNewRoot(labels, labels.iterator().next(), new HashSet<>());
            }
        }
        return blocks;
    }

    private List<Block> valueNumbering(List<Block> blocks){
        List<Block> ret = new ArrayList<>();
        for(Block block: blocks){
            List<IRStmt> stmts = new ArrayList<>();
            // Equivalent IRExpr's are mapped to same IRTemp _vInt
            HashMap<IRExpr, IRTemp> values = new HashMap<>();
            for(IRStmt stmt: block.stmts){
                if(stmt instanceof IRMove){
                    IRTemp t;
                    if(((IRMove) stmt).source() instanceof IRTemp && ((IRTemp) ((IRMove) stmt).source()).name().startsWith("_RV")){
                        t = (IRTemp) ((IRMove) stmt).source();
                    }
                    else{
                        t = find(((IRMove) stmt).source(), values, stmts, ((IRMove) stmt).target() instanceof IRTemp? (IRTemp) ((IRMove) stmt).target() : null);
                    }
                    stmts.add(new IRMove(((IRMove) stmt).target(), t));
                }
                else if(stmt instanceof IRCallStmt){
                    List<IRExpr> args = new ArrayList<>();
                    for(IRExpr expr: ((IRCallStmt) stmt).args()){
                        IRTemp t = find(expr, values, stmts, null);
                        args.add(t);
                    }
                    stmts.add(new IRCallStmt(((IRCallStmt) stmt).target(), ((IRCallStmt) stmt).n_returns(), args));
                }
                else if(stmt instanceof IRCJump){
                    if(((IRCJump) stmt).cond() instanceof IRBinOp){
                        IRBinOp binOp = (IRBinOp) ((IRCJump) stmt).cond();
                        IRTemp left = find(binOp.left(), values, stmts, null);
                        IRTemp right = find(binOp.right(), values, stmts, null);
                        stmts.add(new IRCJump(new IRBinOp(binOp.opType(), left, right), ((IRCJump) stmt).trueLabel(), ((IRCJump) stmt).falseLabel()));
                    }else{
                        IRTemp t = find(((IRCJump) stmt).cond(), values, stmts, null);
                        stmts.add(new IRCJump(t, ((IRCJump) stmt).trueLabel(), ((IRCJump) stmt).falseLabel()));
                    }
                }
                else if(stmt instanceof IRReturn){
                    List<IRExpr> rets = new ArrayList<>();
                    for(IRExpr expr: ((IRReturn) stmt).rets()){
                        IRTemp t = find(expr, values, stmts, null);
                        rets.add(t);
                    }
                    stmts.add(new IRReturn(rets));
                }
                else{
                    stmts.add(stmt);
                }
            }
            ret.add(new Block(stmts, block.next, block.from));
        }
        return ret;
    }

    private IRTemp getTemp(){
        count++;
        return new IRTemp("_v"+count);
    }

    private IRTemp find(IRExpr expr, HashMap<IRExpr, IRTemp> values, List<IRStmt> stmts, IRTemp target){
        if(expr instanceof IRBinOp){
            IRExpr left = ((IRBinOp) expr).left();
            IRExpr right = ((IRBinOp) expr).right();
            IRTemp t1 = find(left, values, stmts, null);
            IRTemp t2 = find(right, values, stmts, null);
            expr = new IRBinOp(((IRBinOp) expr).opType(), t1, t2);
            for(IRExpr key: values.keySet()){
                if(!(key instanceof IRBinOp)) continue;
                if(equiv(expr, key, values)){
                    values.put(expr, values.get(key));
                    if(target != null){
                        values.put(target, values.get(key));
                    }
                    return values.get(key);
                }
            }
        }
        if(values.get(expr) == null){
            IRTemp t = getTemp();
            values.put(expr, t);
            stmts.add(new IRMove(t, expr));
        }
        if(target != null){
            values.put(target, values.get(expr));
        }
        return values.get(expr);
    }

    private boolean equiv(IRExpr expr1, IRExpr expr2, HashMap<IRExpr, IRTemp> values){
        if(expr1 instanceof IRBinOp && expr2 instanceof IRBinOp){
            return  ((IRBinOp) expr1).opType().equals(((IRBinOp) expr2).opType()) &&
                    (equiv(((IRBinOp) expr1).left(), ((IRBinOp) expr2).left(), values) &&
                    equiv(((IRBinOp) expr1).right(), ((IRBinOp) expr2).right(), values)) ||
                    (equiv(((IRBinOp) expr1).left(), ((IRBinOp) expr2).right(), values) &&
                            equiv(((IRBinOp) expr1).right(), ((IRBinOp) expr2).left(), values));
        }
        else if(expr1 instanceof IRTemp && expr2 instanceof IRTemp){
//            if(values.get(expr1) == null && values.get(expr2) == null) return false;
//            else if(values.get(expr1) == null) return values.get(expr2).equals(expr1);
//            else if(values.get(expr2) == null) return values.get(expr1).equals(expr2);
//            else return values.get(expr1).equals(values.get(expr2));
            return expr1.equals(expr2);
        }
        else if(expr1 instanceof IRConst && expr2 instanceof IRConst){
            return ((IRConst) expr1).value() == ((IRConst) expr2).value();
        }
        return false;
    }

    private IRLabel getNewRoot(Set<IRLabel> labels, IRLabel l, Set<IRLabel> visited){
        visited.add(l);
        for(IRLabel label:blocks.get(l).from){
            if (labels.contains(label) && !visited.contains(label)) {
                return getNewRoot(labels, label, visited);
            }
        }
        return l;
    }

    private List<Block> fixJump(List<Block> blocks){
        for(int i=0;i<blocks.size();i++){
            Block b = blocks.get(i);
            IRStmt last = b.lastStmt();
            if(last instanceof IRCJump && i!=blocks.size()-1){
                IRLabel next = blocks.get(i+1).label();
                IRCJump cjump = (IRCJump) last;
                if(cjump.trueLabel().equals(next.name())){
                    cjump.invert();
                }
            }else if(last instanceof IRCJump){
                IRCJump cjump = (IRCJump) last;
                b.removeLast();
                b.stmts.add(new IRCJump(cjump.cond(), cjump.trueLabel()));
                b.stmts.add(new IRJump(new IRName(cjump.falseLabel())));
            }else if(last instanceof IRJump && i!=blocks.size()-1) {
                IRLabel next = blocks.get(i + 1).label();
                IRJump jump = (IRJump) last;
                if (jump.target() instanceof IRName) {
                    IRName name = (IRName) jump.target();
                    if (name.name().equals(next.name())) {
                        b.removeLast();
                    }
                }
            }else if(last instanceof IRJump){
            }else if(!(last instanceof IRReturn) && b.next.size()!=0){
                if(i == blocks.size()-1 || !b.next.get(0).equals(blocks.get(i+1).label())){
                    b.stmts.add(new IRJump(new IRName(b.next.get(0).name())));
                }
            }
        }
        Set<IRLabel> labels = new HashSet<>(this.blocks.keySet());
        for(Block b:blocks){
            IRStmt s = b.lastStmt();
            if(s instanceof IRCJump){
                IRCJump cjump = (IRCJump) s;
                b.removeLast();
                b.stmts.add(new IRCJump(cjump.cond(), cjump.trueLabel()));
                b.next = new ArrayList<>(List.of(new IRLabel(cjump.trueLabel())));
            }
            for(IRLabel l : b.next){
                labels.remove(l);
            }
        }
        for(IRLabel l:labels){
            this.blocks.get(l).removeFirst();
        }

        return blocks;
    }

    private List<IRStmt> toCode(List<Block> blocks){
        List<IRStmt> stmts = new ArrayList<>();
        for(Block b:blocks){
            stmts.addAll(b.stmts);
        }
        return stmts;
    }
}

