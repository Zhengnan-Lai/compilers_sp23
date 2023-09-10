package zc246_zl345_co232_mw756.context;

import zc246_zl345_co232_mw756.ast.Function;
import zc246_zl345_co232_mw756.ast.Identifier;
import zc246_zl345_co232_mw756.ast.Node;
import zc246_zl345_co232_mw756.ast.Record;
import zc246_zl345_co232_mw756.errors.SemanticError;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;

import static zc246_zl345_co232_mw756.ast.Identifier.LOG_SEP;

public class Context {

    class Entry implements Map.Entry<Node, Type> {
        Node key;
        Type value;

        Entry(Node key, Type value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Node getKey() {
            return key;
        }

        @Override
        public Type getValue() {
            return value;
        }

        @Override
        public Type setValue(Type value) {
            throw new UnsupportedOperationException();
        }
    }

    private ArrayList<ArrayDeque<Entry>> table; //the hash table
    private BigInteger bucket; //number of buckets
    private long count; //number of elements
    private static final double threshold = 0.75;
    private final ArrayDeque<Node> log;

    /**
     * Create a new hash table.
     */
    public Context() {
        this.bucket = BigInteger.valueOf(53);
        this.table = new ArrayList<>(bucket.intValue());
        fill(this.table, bucket.intValue());
        this.count = 0;
        this.log = new ArrayDeque<>();
    }

    public int size() {
        return (int) Math.min(count, Integer.MAX_VALUE);
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean inDomain(Node key) {
        ArrayDeque<Entry> ll = table.get(hash(key));
        if (ll == null) return false;
        for (Entry e : ll) if (e.key.equals(key)) return true;
        return false;
    }

    public void newContext() {
        log.addFirst(LOG_SEP);
    }

    public void endContext() {
        while (!log.isEmpty()) {
            Node n = log.removeFirst();
            if (n == LOG_SEP) break;
            remove(n);
        }
    }

    public Type get(Node key) {
        ArrayDeque<Entry> ll = table.get(hash(key));
        if (ll != null)
            for (Entry e : ll)
                if (e.key.equals(key)) return e.value;
        throw new SemanticError(key.lineNumber, key.columnNumber,
                "Name " + ((Identifier) key).id + " cannot be resolved");
    }

    public void includeTopLevel(Function key, FunctionType value, int l, int c) {
        if (inDomain(key.id)) {
            FunctionType v = (FunctionType) get(key.id);
            if (!v.equals(value))
                throw new SemanticError(l, c,
                        "Function " + key.id.id + " already defined with different signature");
        } else {
            put(key.id, value);
        }
    }

    // return true if t1 is a subset of t2
    public static boolean prefix(RecordType t1, RecordType t2){
        if (t1.fieldNames.length > t2.fieldTypes.length){
            return false;
        }
        for (int i = 0; i < t1.fieldNames.length; i++){
            if (!(t1.fieldNames[i].equals(t2.fieldNames[i])) || (t1.fieldTypes[i] != null && t2.fieldTypes[i] != null
                    && !t1.fieldTypes[i].equals(t2.fieldTypes[i]))){
                return false;
            }
        }
        return true;
    }

    public void includeTopLevel(Record key, RecordType value, int l, int c){
        if (inDomain(key.recordIdentifier())) {
            RecordType original = (RecordType) get(key.recordIdentifier());
            // original -> defined by interface
            // value -> defined by program
            if (!prefix(original, value)) {
                boolean eq = prefix(original, value);
                throw new SemanticError(l, c,
                        "record " + key.name.id + " is already defined with a different signature");
                // put in new value with more information
            }
            remove(key.recordIdentifier());
        }
        put(key.recordIdentifier(), value);
        // default constructor
        put(key.name, defaultConstructor(value));
    }

    public static FunctionType defaultConstructor(RecordType value){
        PrimitiveType[] args = value.fieldTypes;
        PrimitiveType[] rets = new PrimitiveType[1];
        rets[0] = value;
        return new FunctionType(args, rets);
    }

    public void put(Node key, Type value) {
        if (inDomain(key)) {
            if (get(key) instanceof PrimitiveType) {
                    throw new SemanticError(key.lineNumber, key.columnNumber,
                            "Duplicate variable " + ((Identifier) key).id);
            }
        }
        int hash = hash(key);
        ArrayDeque<Entry> ll = table.get(hash);
        if (ll == null) {// no element in this linked list
            table.set(hash, new ArrayDeque<>());
            ll = table.get(hash);
        }
        ll.addFirst(new Entry(key, value));
        log.addFirst(key);
        count++;
        resize();
    }

    public void remove(Node key) {
        int hash = hash(key);
        ArrayDeque<Entry> ll = table.get(hash);
        if (ll == null) return;
        for (Entry e : ll) {
            if (e.key.equals(key)) {
                ll.remove(e);
                count--;
                return;
            }
        }
    }

    /**
     * Resize the current table to twice the original size when the load factor is greater than the threshold.
     * Also updates the hash function.
     */
    private void resize() {
        if ((((double) count) / bucket.intValue()) > threshold) {
            bucket = nextPrime(bucket.multiply(BigInteger.TWO));
            ArrayList<ArrayDeque<Entry>> newTab = new ArrayList<>(bucket.intValue());
            fill(newTab, bucket.intValue());
            for (ArrayDeque<Entry> ll : table) {
                if (ll != null) {
                    for (Entry e : ll) {
                        int hash = hash(e.key);
                        if (newTab.get(hash) == null) {
                            newTab.set(hash, new ArrayDeque<>());
                        }
                        newTab.get(hash).addLast(e);
                    }
                }
            }
            table = newTab;
        }
    }

    /**
     * The hash function to be used throughout this class
     *
     * @param o the object to be hashed
     * @return the hash of the object
     */
    private int hash(Node o) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(String.valueOf(o.hashCode()).getBytes(StandardCharsets.UTF_8));
            int h = digest[0] << 24 | digest[1] << 16 | digest[2] << 8 | digest[3];
            h = (h >>> 16) ^ (h & 0xFF);
            return h % bucket.intValue();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private BigInteger nextPrime(BigInteger b) {
        return b.nextProbablePrime();
    }

    private static void fill(ArrayList<ArrayDeque<Entry>> table, int amount) {
        for (int i = 0; i < amount; i++) {
            table.add(null);
        }
    }
}