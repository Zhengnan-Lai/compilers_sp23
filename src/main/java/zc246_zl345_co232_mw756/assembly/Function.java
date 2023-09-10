package zc246_zl345_co232_mw756.assembly;

import java.util.List;

public class Function {
    private final String name;
    private final List<Assembly> body;

    public Function(String name, List<Assembly> body) {
        this.name = name;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t").append(Directives.TEXT);
        sb.append("\t").append(new Directives(Directives.DirectivesType.globl, name, null));
        sb.append("\t").append(new Directives(Directives.DirectivesType.type, name, "@function"));
        sb.append(name).append(":\n");
        for (Assembly a : body) {
            if (a instanceof Label) {
                sb.append(a).append("\n");
            } else if (a instanceof Comment) {
                sb.append("\n\t").append(a);
            } else {
                sb.append("\t").append(a).append("\n");
            }
        }
        return sb.toString();
    }

    public List<Assembly> getBody() {
        return body;
    }

    public String getName() {
        return name;
    }
}
