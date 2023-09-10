package zc246_zl345_co232_mw756.assembly;

import zc246_zl345_co232_mw756.opt.CFG;
import zc246_zl345_co232_mw756.opt.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasicBlock {
    public static class Block {
        private List<Node<Assembly>> block;
        private List<Block> next;
        private List<Block> prev;

        public Block(List<Node<Assembly>> block) {
            this.block = block;
            this.next = new ArrayList<>();
            this.prev = new ArrayList<>();
        }

        public List<Block> getNext() {
            return next;
        }

        public List<Node<Assembly>> getBlock() {
            return block;
        }
    }

    private Set<Node<Assembly>> visited;

    public BasicBlock() {
        this.visited = new HashSet<>();
    }

    public Block makeBasicBlock(CFG<Assembly> cfg) {
        return makeBasicBlock(cfg.start, new ArrayList<>());
    }

    private Block makeBasicBlock(Node<Assembly> node, List<Node<Assembly>> acc) {
        visited.add(node);
        if (node.stmt == null) {
            return makeBasicBlock(node.next.get(0), acc);
        } else if (node.next.size() == 0) {
            //ret
            acc.add(node);
            return new Block(acc);
        } else if (node.next.size() == 1 && node.prev.size() == 1 && node.stmt instanceof Jump) {
            //jmp
            acc.add(node);
            Block b = new Block(acc);
            if (!visited.contains(node.next.get(0))) {
                Block next = makeBasicBlock(node.next.get(0), new ArrayList<>());
                b.next.add(next);
                next.prev.add(b);
            }
            return b;
        } else if (node.next.size() == 1 && node.prev.size() == 1) {
            //linear
            acc.add(node);
            return makeBasicBlock(node.next.get(0), acc);
        } else if (node.next.size() == 2) {
            //cjump
            acc.add(node);
            Block b = new Block(acc);
            for (Node<Assembly> n : node.next) {
                if (visited.contains(n)) continue;
                Block next = makeBasicBlock(n, new ArrayList<>());
                b.next.add(next);
                next.prev.add(b);
            }
            return b;
        } else if (node.prev.size() > 1) {
            //label
            Block b = new Block(acc);
            List<Node<Assembly>> newAcc = new ArrayList<>();
            newAcc.add(node);
            Block next = makeBasicBlock(node.next.get(0), newAcc);
            b.next.add(next);
            next.prev.add(b);
            return b;
        } else {
            throw new Error("assembly basic block");
        }
    }
}
