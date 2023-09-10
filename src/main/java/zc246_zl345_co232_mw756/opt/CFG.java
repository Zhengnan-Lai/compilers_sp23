package zc246_zl345_co232_mw756.opt;

import edu.cornell.cs.cs4120.xic.ir.*;
import zc246_zl345_co232_mw756.assembly.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.*;

//T should be either IRStmt or Assembly
public class CFG<T> {
    public Node<T> start;
    public Set<Node<T>> nodes;

    private CFG(Node<T> start, Set<Node<T>> nodes) {
        this.start = start;
        this.nodes = nodes;
    }

    public static <T> CFG<T> makeCFG(List<T> statements) {
        Queue<T> stmts = new ArrayDeque<>(statements);
        if (stmts.size() == 0) return null;
        HashMap<String, Node<T>> labels = new HashMap<>();
        HashMap<Node<T>, String> waiting = new HashMap<>();
        Set<Node<T>> nodes = new LinkedHashSet<>();
        Node<T> n = new Node<>(stmts.poll());
        Node<T> start = new Node<>(null);
        start.next.add(n);
        n.prev.add(start);
        nodes.add(start);
        while (!stmts.isEmpty()) {
            nodes.add(n);
            if (action(n, labels, waiting)) {
                n = new Node<>(stmts.poll());
                continue;
            }
            Node<T> next = new Node<>(stmts.poll());
            n.next.add(next);
            next.prev.add(n);
            n = next;
        }
        nodes.add(n);
        action(n, labels, waiting);
        for (Node<T> wait : waiting.keySet()) {
            String label = waiting.get(wait);
            Node<T> target = labels.get(label);
            wait.next.add(target);
            target.prev.add(wait);
        }
        return new CFG<>(start, nodes);
    }

    private static <T> boolean action(Node<T> n, HashMap<String, Node<T>> labels, HashMap<Node<T>, String> waiting) {
        if (n.stmt instanceof IRStmt) {
            if (n.stmt instanceof IRCJump) {
                IRCJump c = (IRCJump) n.stmt;
                waiting.put(n, c.trueLabel());
            } else if (n.stmt instanceof IRLabel) {
                IRLabel l = (IRLabel) n.stmt;
                labels.put(l.name(), n);
            } else if (n.stmt instanceof IRReturn) {
                return true;
            } else if (n.stmt instanceof IRJump) {
                waiting.put(n, ((IRName) ((IRJump) n.stmt).target()).name());
                return true;
            }
        } else if (n.stmt instanceof Assembly) {
            Assembly a = (Assembly) n.stmt;
            if (a instanceof Jump) {
                Jump j = (Jump) a;
                waiting.put(n, j.getLabel());
                return j.getType() == Jump.JumpType.JMP;
            } else if (a instanceof Label) {
                Label l = (Label) a;
                labels.put(l.getLabel(), n);
            } else if (a instanceof Ret) {
                return true;
            }
        } else {
            throw new Error("Unexpected stmt type: " + n.stmt.getClass().getName());
        }
        return false;
    }

    public void toDot(String filename, String op, DataFlow df) throws IOException {
        HashSet<Node<T>> visited = new HashSet<>();
        Queue<Node<T>> frontier = new ArrayDeque<>();
        HashMap<Node<T>, Integer> count = new HashMap<>();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write("digraph G {\n");
        frontier.offer(start);
        count.put(start, 0);
        while (!frontier.isEmpty()) {
            Node<T> n = frontier.poll();
            if (visited.contains(n)) continue;
            visited.add(n);
            int c = getCount(n, count);
            if (op.equals("reg")) {
                writer.write("\t\"n" + c + "\"[shape=box, label=\"" + getCode(n) + "\n" + getInOut(n, (LiveVariable) df) + "\"];\n");
            } else {
                writer.write("\t\"n" + c + "\"[shape=box, label=\"" + getCode(n) + "\"];\n");
            }
            for (Node<T> next : n.next) {
                writer.write("\t\"n" + c + "\" -> \"n" + getCount(next, count) + "\";\n");
                frontier.offer(next);
            }
        }
        writer.write("}");
        writer.close();
    }

    public void toDot(String filename) throws IOException{
        HashSet<Node<T>> visited = new HashSet<>();
        Queue<Node<T>> frontier = new ArrayDeque<>();
        HashMap<Node<T>, Integer> count = new HashMap<>();
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write("digraph G {\n");
        frontier.offer(start);
        count.put(start, 0);
        while(!frontier.isEmpty()){
            Node<T> n = frontier.poll();
            if(visited.contains(n)) continue;
            visited.add(n);
            int c = getCount(n, count);
            writer.write("\t\"n" + c + "\"[shape=box, label=\"" + getCode(n) + "\"];\n");
            for(Node<T> next: n.next){
                writer.write("\t\"n" + c + "\" -> \"n" + getCount(next, count) + "\";\n");
                frontier.offer(next);
            }
        }
        writer.write("}");
        writer.close();
    }

    private static <T> String getCode(Node<T> n) {
        T stmt = n.stmt;
        if (stmt == null) return "start";
        String s = stmt.toString();
        return s.replaceAll("[\n\r\t]", "");
    }

    private static <T> String getInOut(Node<T> n, LiveVariable df) {
        Set<Register> in = df.in.get(n);
        Set<Register> out = df.out.get(n);
        Iterator<Register> inItr = in.iterator();
        StringBuilder inRegs = new StringBuilder("in: ");
        while (inItr.hasNext()) {
            inRegs.append(inItr.next()).append(", ");
        }
        if (inRegs.length() > 4) inRegs.delete(inRegs.length() - 2, inRegs.length());
        StringBuilder outRegs = new StringBuilder("out: ");
        for (Register register : out) {
            outRegs.append(register).append(", ");
        }
        if (outRegs.length() > 5) outRegs.delete(outRegs.length() - 2, outRegs.length());
        return inRegs + "\n" + outRegs;
    }

    private static <T> int getCount(Node<T> n, HashMap<Node<T>, Integer> count) {
        if (count.containsKey(n)) return count.get(n);
        int c = count.size();
        count.put(n, c);
        return c;
    }

    public List<T> toCode() {
        Stack<Node<T>> frontier = new Stack<>();
        Set<Node<T>> visited = new HashSet<>();
        List<T> code = new ArrayList<>();
        frontier.push(start);
        while (!frontier.isEmpty()) {
            Node<T> n = frontier.pop();
            if (visited.contains(n)) continue;
            visited.add(n);
            T after = null;
            if (n.stmt == null) {
                frontier.addAll(n.next);
                continue;
            } else if (n.stmt instanceof IRStmt) {
                if(n.next.size()!=0) {
                    Node<T> next = n.next.get(0);
                    if (n.stmt instanceof IRCJump) {
                        IRCJump c = (IRCJump) n.stmt;
                        IRStmt st = (IRStmt) n.next.get(0).stmt;
                        if (next.stmt instanceof IRLabel) {
                            IRLabel falseLabel = (IRLabel) st;
                            if (falseLabel.name().equals(c.trueLabel())) {
                                next = n.next.get(1);
                            }
                        }
                    }
                    if (next.stmt instanceof IRLabel && visited.contains(next) && !(n.stmt instanceof IRJump)) {
                       after = (T) new IRJump(new IRName(((IRLabel) next.stmt).name()));
                    }
                }
                if (n.stmt instanceof IRCJump) {
                    IRCJump c = (IRCJump) n.stmt;
                    IRStmt st = (IRStmt) n.next.get(0).stmt;
                    if (st instanceof IRLabel && ((IRLabel) st).name().equals(c.trueLabel())) {
                        frontier.push(n.next.get(0));
                        frontier.push(n.next.get(1));
                    }else{
                        frontier.push(n.next.get(1));
                        frontier.push(n.next.get(0));
                    }
                } else {
                    frontier.addAll(n.next);
                }
            } else {
                //Assembly
                Assembly a = (Assembly) n.stmt;
                if (a instanceof Jump && ((Jump) a).getType() != Jump.JumpType.JMP) {
                    Assembly st = (Assembly) n.next.get(0).stmt;
                    if (st instanceof Label && ((Label) st).getLabel().equals(((Jump) a).getLabel())) {
                        frontier.push(n.next.get(0));
                        frontier.push(n.next.get(1));
                    }else {
                        frontier.push(n.next.get(1));
                        frontier.push(n.next.get(0));
                    }
                } else {
                    frontier.addAll(n.next);
                }
            }
            code.add(n.stmt);
            if(after!=null) code.add(after);
        }
        return code;
    }

    public void simplify(){
        Queue<Node<T>> frontier = new ArrayDeque<>();
        Set<Node<T>> unreachable = new HashSet<>(nodes);
        frontier.offer(start);
        while(!frontier.isEmpty()){
            Node<T> n = frontier.poll();
            if(!unreachable.contains(n)) continue;
            unreachable.remove(n);
            for(Node<T> next: n.next){
                frontier.offer(next);
            }
        }
        nodes.removeAll(unreachable);
        for(Node<T> n:nodes){
            n.next.removeAll(unreachable);
            n.prev.removeAll(unreachable);
        }
    }
}
