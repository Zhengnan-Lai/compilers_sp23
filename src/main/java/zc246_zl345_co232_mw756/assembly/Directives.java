package zc246_zl345_co232_mw756.assembly;

public class Directives {

    public enum DirectivesType{
        align, file, globl, type, size, quad
    }

    public static final String INTEL_DIRECTIVE = ".intel_syntax noprefix\n";
    public static final String TEXT = ".text\n";
    public static final String DATA = ".data\n";
    public static final String BSS = ".bss\n";
    public static final String ZERO = ".zero\t8\n";

    private final DirectivesType dir;
    private final String arg1;
    private final String arg2;

    public DirectivesType getDir(){return dir;}

    public String getArg1() {
        return arg1;
    }

    public String getArg2(){
        return arg2;
    }

    public Directives(DirectivesType dir, String arg1, String arg2) {
        this.dir = dir;
        if(dir == DirectivesType.file){
            arg1 = String.format("\"%s\"", arg1);
        }
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public String toString(){
        String res;
        if (arg2 != null){
            res = String.format(".%s\t%s, %s\n", dir.toString(), arg1, arg2);
        }
        else{
            res = String.format(".%s\t%s\n", dir.toString(), arg1);
        }
        return res;
    }
}
