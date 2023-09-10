package zc246_zl345_co232_mw756.context;

import zc246_zl345_co232_mw756.ast.Record;

import java.util.Arrays;

public class RecordType extends PrimitiveType{

    public String[] fieldNames;
    public PrimitiveType[] fieldTypes;

    public Record record; // used to retrieve record based on the record type


    public RecordType(String[] fieldNames, PrimitiveType[] fieldTypes, Record record, int arrayDepth) {
        super(Primitive.RECORD);
        this.fieldNames = fieldNames;
        this.fieldTypes = fieldTypes;
        this.record = record;
        this.arrayDepth = arrayDepth;
    }

    @Override
    public String toString() {
        return record.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PrimitiveType)){
            return false;
        }
        if (o instanceof RecordType){
            RecordType other = (RecordType) o;
            return Arrays.equals(this.fieldNames, other.fieldNames) && arrayDepth == other.arrayDepth && record.equals(other.record);
        }
        // allow record type to be compared with null
        return ((PrimitiveType) o).primitive == Primitive.ANY;
    }

    public boolean equalPrefix (Object o) {
        // return true if o is a prefix of this
        if (o == this) {
            return true;
        }
        if (!(o instanceof RecordType)) {
            return false;
        }
        if(arrayDepth != ((RecordType) o).arrayDepth){
            return false;
        }
        if(arrayDepth > 0) return true;
        if (((RecordType) o).fieldNames.length > fieldNames.length){
            return false;
        }
        for (int i = 0; i < ((RecordType) o).fieldNames.length; i++){
            if (!fieldNames[i].equals(((RecordType) o).fieldNames[i])){
                return false;
            }
            if (!((fieldTypes[i] != null) && (((RecordType) o).fieldTypes[i] != null) &&
                    fieldTypes[i].equals(((RecordType) o).fieldTypes[i]))){
                if (fieldTypes[i] instanceof RecordType){
                    if (!(((RecordType) o).fieldTypes[i] instanceof RecordType)){
                        return false;
                    }
                    if (((RecordType) fieldTypes[i]).equalPrefix(((RecordType) o).fieldTypes[i])){
                        break;
                    }
                }
                return false;
            }

        }
        return arrayDepth == ((RecordType) o).arrayDepth && record.name.equals(((RecordType) o).record.name);
    }
}
