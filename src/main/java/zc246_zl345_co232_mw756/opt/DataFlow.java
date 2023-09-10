package zc246_zl345_co232_mw756.opt;

import java.util.*;

//S: IRStmt or Assembly
//T: type of value of this analysis
public abstract class DataFlow<S, T> {

    protected int direction; //0 for forward, 1 for backward
    protected CFG<S> cfg;
    protected HashMap<Node<S>, T> in;
    protected HashMap<Node<S>, T> out;
    public boolean converged;

    public DataFlow(int direction, CFG<S> cfg) {
        this.direction = direction;
        this.cfg = cfg;
        this.in = new HashMap<>();
        this.out = new HashMap<>();
        this.converged = true;
    }

    public abstract T transfer(Node<S> node, T value);

    public abstract T top();

    public abstract T meet(T v1, T v2);

    public T meet(List<T> values) {
        T result = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            result = meet(result, values.get(i));
        }
        return result;
    }

    public HashMap<Node<S>, T> run() {
        if (direction == 0) return forward_analysis();
        else return backward_analysis();
    }

    protected HashMap<Node<S>, T> forward_analysis() {
        //init worklist
        Queue<Node<S>> worklist = new ArrayDeque<>(cfg.nodes);

        //init out[n]
        for (Node<S> node : cfg.nodes) {
            out.put(node, top());
        }

        //while worklist not empty
        while (!worklist.isEmpty()) {
            Node<S> n = worklist.poll();
            //get in[n]
            T new_in = top();
            for (Node<S> prev : n.prev) {
                new_in = meet(new_in, out.get(prev));
            }
            in.put(n, new_in);
            T new_out = transfer(n, new_in);
            if (new_out == null || !new_out.equals(out.get(n))) {
                out.put(n, new_out);
                for (Node<S> next : n.next) {
                    worklist.offer(next);
                }
            }
        }
        return out;
    }

    protected HashMap<Node<S>, T> backward_analysis() {
        //init worklist
        Queue<Node<S>> worklist = new ArrayDeque<>(cfg.nodes);

        //init in[n]
        for (Node<S> node : cfg.nodes) {
            in.put(node, top());
        }

        //while worklist not empty
        while (!worklist.isEmpty()) {
            Node<S> n = worklist.poll();
            //get out[n]
            T new_out = top();
            for (Node<S> next : n.next) {
                new_out = meet(new_out, in.get(next));
            }
            out.put(n, new_out);
            T new_in = transfer(n, new_out);
            if (new_in == null || !new_in.equals(in.get(n))) {
                in.put(n, new_in);
                for (Node<S> prev : n.prev) {
                    worklist.offer(prev);
                }
            }
        }
        return in;
    }

    protected abstract S convert(Node<S> node);

    public CFG<S> finish() {
        HashSet<Node<S>> visited = new HashSet<>();
        Queue<Node<S>> frontier = new ArrayDeque<>();
        frontier.offer(cfg.start);
        while (!frontier.isEmpty()) {
            Node<S> n = frontier.poll();
            if (visited.contains(n)) continue;
            visited.add(n);
            for (Node<S> next : n.next) {
                frontier.offer(next);
            }
            if (n.stmt != null) {
                S stmt = convert(n);
                if (stmt != null) n.stmt = stmt;
                else {
                    n.prev.forEach(prev -> prev.next.remove(n));
                    n.next.forEach(next -> next.prev.remove(n));
                    n.prev.forEach(prev -> prev.next.addAll(n.next));
                    n.next.forEach(next -> next.prev.addAll(n.prev));
                }
            }
        }
        return cfg;
    }
}
