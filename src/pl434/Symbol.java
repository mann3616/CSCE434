package pl434;

import types.*;

public class Symbol {

    private String name;
    // TODO: Add other parameters like type
 
    private Type type;
    int address;
    int regno;

    public Symbol (String name, int address) {
        this.name = name;
        this.address = address;
    }
    public String name () {
        return name;
    }
    public String functionString() {
        FuncType t = (FuncType) type;
        String ret = "";
        ret += name();
        ret += ":(";
        String param = "";
        for(Type tt : t.params()){
            param += ", " + typeToString(tt);
        }
        ret += param.substring(2) + ")->" + typeToString(t.returnType());
        return ret;
    }
    public String getTypeAsString(){
        return typeToString(type);
    }
    public static String typeToString(Type tt){
        String param = "";
        if(tt.getClass().equals(FloatType.class)){
            param += "float";
        } else if(tt.getClass().equals(BoolType.class)){
            param += "bool";
        } else if(tt.getClass().equals(IntType.class)){
            param += "int";
        }
        return param;
    }
}
