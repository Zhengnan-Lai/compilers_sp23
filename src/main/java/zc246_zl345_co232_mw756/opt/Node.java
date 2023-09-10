package zc246_zl345_co232_mw756.opt;

import java.util.ArrayList;
import java.util.List;

public class Node<T>{
    public T stmt;
    public List<Node<T>> next;
    public List<Node<T>> prev;

    public Node(T stmt){
        this.stmt = stmt;
        this.next = new ArrayList<>();
        this.prev = new ArrayList<>();
    }

    @Override
    public String toString() {
        if(stmt == null)
            return "start";
        return stmt.toString();
    }
}
