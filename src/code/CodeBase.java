/*
 * author: Santiago Ontañón Villar (Brain Games)
 */
package code;

import cl.MDLConfig;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

public class CodeBase {    
    public static final String CURRENT_ADDRESS = "$";

    MDLConfig config;
    
    SourceFile main;
    LinkedHashMap<String, SourceFile> sources = new LinkedHashMap<>();
    LinkedHashMap<String, SourceConstant> symbols = new LinkedHashMap<>();
    LinkedHashMap<String, SourceMacro> macros = new LinkedHashMap<>();
    
    int current_address = 0;
    
    
    public CodeBase(MDLConfig a_config)
    {
        config = a_config;        
    }
    
    
    public boolean isRegister(String name)
    {
        String registers[] = {"a", "b", "c", "d", "e", "h","l",
                              "af", "bc", "de", "hl",
                              "sp", "ix", "iy", "pc", 
                              "ixl", "ixh", "iyl", "iyh",
                              "af'", 
                              "i", "r"};
        for(String reg:registers) {
            if (name.equalsIgnoreCase(reg)) return true;
        }
        
        return false;
    }


    public boolean isCondition(String name)
    {
        String conditions[] = {"c", "m", "nc", "nz", "p", "pe", "po", "z"};
        for(String c:conditions) {
            if (name.equalsIgnoreCase(c)) return true;
        }
        
        return false;
    }
    
    
    public SourceConstant getSymbol(String name)
    {
        if (symbols.containsKey(name)) {
            return symbols.get(name);
        }
        return null;
    }
    
    
    public Set<String> getSymbols()
    {
        return symbols.keySet();
    }


    public Integer getSymbolValue(String name, boolean silent)
    {
        if (symbols.containsKey(name)) {
            return symbols.get(name).getValue(this, silent);
        }
        return null;
    }
    
    
    public void addSymbol(String name, SourceConstant sc)
    {
        symbols.put(name, sc);
    }
    
    
    public Collection<SourceFile> getSourceFiles()
    {
        return sources.values();
    }
    
    
    public SourceFile getSourceFile(String fileName)
    {
        if (sources.containsKey(fileName)) return sources.get(fileName);
        return null;
    }
    
    
    public void addSourceFile(SourceFile s)
    {
        sources.put(s.fileName, s);
    }
        
    
    public SourceMacro getMacro(String name)
    {
        if (macros.containsKey(name)) return macros.get(name);
        return null;
    }
    
    
    public void addMacro(SourceMacro m)
    {
        macros.put(m.name, m);
    }
    
    
    public void setAddress(int a_address)
    {
        current_address = a_address;
    }
    
    
    public int getAddress()
    {
        return current_address;
    }
    
    
    public void resetAddresses()
    {
        for(SourceFile f:sources.values()) {
            f.resetAddresses();
        }
    }
    
    
    public void setMain(SourceFile s)
    {
        main = s;
    }
    
    
    public SourceFile getMain()
    {
        return main;
    }
                
}
