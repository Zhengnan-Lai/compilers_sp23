package zc246_zl345_co232_mw756.assembly;

import java.util.Map;

public class AACompUnit {
    private final String name;
    private Map<String, Function> functions;
    private final Map<String, Data> dataMap;

    public AACompUnit(String name, Map<String, Function> functions, Map<String, Data> dataMap) {
        this.name = name;
        this.functions = functions;
        this.dataMap = dataMap;
    }

    public String getName() {
        return name;
    }

    public Map<String, Function> getFunctions() {
        return functions;
    }

    public Map<String, Data> getDataMap() {
        return dataMap;
    }

    public void setFunctions(Map<String, Function> functions) {
        this.functions = functions;
    }

    public String printer(){
        StringBuilder ret = new StringBuilder();
        Directives dfile = new Directives(Directives.DirectivesType.file, name, null);
        ret.append("\t").append(dfile);
        ret.append("\t").append(Directives.INTEL_DIRECTIVE);
        for (Map.Entry<String, Function> entry: functions.entrySet()){
            ret.append(entry.getValue().toString()).append("\n");
        }
        for (Map.Entry<String, Data> entry: dataMap.entrySet()){
            ret.append(entry.getValue().toString()).append("\n");
        }
        return ret.toString();
    }
}
