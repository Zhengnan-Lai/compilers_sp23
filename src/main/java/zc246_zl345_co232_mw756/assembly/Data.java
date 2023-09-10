package zc246_zl345_co232_mw756.assembly;

public class Data {
    private final String name;
    private final long[] value;

    private static final int DEFAULT_SIZE = 8;
    private static final int SECONDARY_SIZE = 16;
    private static final int TERNARY_SIZE = 32;

    public Data(String name, long[] value) {
        this.name = name;
        this.value = value;
    }

    public int getAlignment(){
        int originalSize = value.length * DEFAULT_SIZE;
        if (originalSize <= 8){
            return DEFAULT_SIZE;
        }
        else if (originalSize <= 16){
            return SECONDARY_SIZE;
        }
        else{
            return TERNARY_SIZE;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Directives globld = new Directives(Directives.DirectivesType.globl, name, null);
        sb.append("\t").append(globld);
        if (value.length == 0){
            sb.append("\t").append(Directives.BSS);
            Directives alignd = new Directives(Directives.DirectivesType.align,
                    String.valueOf(getAlignment()), null);
            sb.append("\t").append(alignd);
            Directives typed = new Directives(Directives.DirectivesType.type, name, "@object");
            sb.append("\t").append(typed);
            Directives sized = new Directives(Directives.DirectivesType.size, name, String.valueOf(DEFAULT_SIZE));
            sb.append("\t").append(sized);
            sb.append(name + ":\n");
            sb.append("\t").append(Directives.ZERO);
        }
        // non zero
        else{
            sb.append("\t").append(Directives.DATA);
            Directives alignd = new Directives(Directives.DirectivesType.align,
                    String.valueOf(getAlignment()), null);
            sb.append("\t").append(alignd);
            Directives typed = new Directives(Directives.DirectivesType.type, name, "@object");
            sb.append("\t").append(typed);
            Directives sized = new Directives(Directives.DirectivesType.size, name,
                    String.valueOf(DEFAULT_SIZE * value.length));
            sb.append("\t").append(sized);
            sb.append(name + ":\n");
            for (int i = 0; i < value.length; i++){
                Directives d = new Directives(Directives.DirectivesType.quad, String.valueOf(value[i]), null);
                sb.append("\t").append(d);
            }
        }
        return sb.toString();
    }
}