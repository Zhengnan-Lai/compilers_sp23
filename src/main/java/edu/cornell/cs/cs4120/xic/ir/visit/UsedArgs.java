package edu.cornell.cs.cs4120.xic.ir.visit;

import edu.cornell.cs.cs4120.xic.ir.IRNode;
import edu.cornell.cs.cs4120.xic.ir.IRTemp;

import java.util.HashSet;
import java.util.Set;

public class UsedArgs extends AggregateVisitor<Set<Integer>>{
    @Override
    public Set<Integer> unit() {
        return new HashSet<>();
    }

    @Override
    public Set<Integer> bind(Set<Integer> r1, Set<Integer> r2) {
        Set<Integer> result = new HashSet<>(r1);
        result.addAll(r2);
        return result;
    }

    @Override
    public Set<Integer> leave(IRNode parent, IRNode n, Set<Integer> r, AggregateVisitor<Set<Integer>> v_){
        if(n instanceof IRTemp){
            //IRTemp
            IRTemp temp = (IRTemp) n;
            if(temp.name().startsWith("_ARG")){
                r.add(Integer.parseInt(temp.name().substring(4)));
            }
        }
        return r;
    }
}
