/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers.searchopt;

import code.CPUOpDependency;
import code.CodeBase;
import code.SourceConstant;
import java.util.ArrayList;
import java.util.List;
import util.microprocessor.IMemory;
import util.microprocessor.Z80.Z80Core;

/**
 *
 * @author santi
 */
public class Specification {
    int codeStartAddress = 0x4000;
    int maxSimulationTime = 256;
    int maxSizeInBytes = 256;
    int maxOps = 4;
    int searchType = SearchBasedOptimizer.SEARCH_ID_OPS;
    
    // instruction set:
    List<String> allowedOps = new ArrayList<>();
    List<String> allowedRegisters = new ArrayList<>();
    boolean allowRamUse = false;
    boolean allowGhostRegisters = false;
    
    public List<Integer> allowed8bitConstants = new ArrayList<>();
    public List<Integer> allowed16bitConstants = new ArrayList<>();
    public List<Integer> allowedOffsetConstants = new ArrayList<>();
    
    public List<InputParameter> parameters = new ArrayList<>();
    public List<SpecificationExpression> startState = new ArrayList<>();
    public List<SpecificationExpression> goalState = new ArrayList<>();
    
    
    public Specification()
    {
        // By default, we only allow "0" as a constant:
        allowed8bitConstants.add(0);
        allowed16bitConstants.add(0);
        allowedOffsetConstants.add(0);
        
        allowedOps.add("and");
        allowedOps.add("or");
        allowedOps.add("xor");
        
        allowedOps.add("inc");
        allowedOps.add("dec");
        
        allowedOps.add("add");
        allowedOps.add("adc");
        allowedOps.add("sub");
        allowedOps.add("sbc");
        
        allowedOps.add("ld");
        
        allowedOps.add("rlc");
        allowedOps.add("rl");
        allowedOps.add("rrc");
        allowedOps.add("rr");
        allowedOps.add("rlca");
        allowedOps.add("rla");
        allowedOps.add("rrca");
        allowedOps.add("rra");
        
        allowedOps.add("sla");
        allowedOps.add("sra");
        allowedOps.add("srl");
        allowedOps.add("sli");

        allowedOps.add("cpl");
        allowedOps.add("neg");

        allowedOps.add("bit");
        allowedOps.add("set");
        allowedOps.add("res");
        
        allowedRegisters.add("a");
        allowedRegisters.add("af");
        allowedRegisters.add("b");
        allowedRegisters.add("c");
        allowedRegisters.add("bc");
        allowedRegisters.add("d");
        allowedRegisters.add("e");
        allowedRegisters.add("de");
        allowedRegisters.add("h");
        allowedRegisters.add("l");
        allowedRegisters.add("hl");
        allowedRegisters.add("ixh");
        allowedRegisters.add("ixl");
        allowedRegisters.add("ix");
        allowedRegisters.add("iyh");
        allowedRegisters.add("iyl");
        allowedRegisters.add("iy");
    }
    
    
    public void clearOpGroups()
    {
        allowedOps.clear();
    }


    public void clearAllowedRegisters()
    {
        allowedRegisters.clear();
    }
    
    
    public void addParameter(SourceConstant symbol)
    {
        for(InputParameter p:parameters) {
            if (p.symbol.name.equals(symbol.name)) return;
        }
        parameters.add(new InputParameter(symbol));
    }
    
    
    public InputParameter getParameter(String name) 
    {
        for(InputParameter p:parameters) {
            if (p.symbol.name.equals(name)) return p;
        }
        return null;
    }
    
    
    public boolean[] getInitialDependencies(List<CPUOpDependency> allDependencies)
    {
        boolean dependencies[] = new boolean[allDependencies.size()];
        for(int i = 0;i<dependencies.length;i++) dependencies[i] = false;
        
        for(SpecificationExpression exp:startState) {
            CPUOpDependency dep = new CPUOpDependency(exp.leftRegisterName.toUpperCase(), null, null, null, null);
            for(int i = 0;i<dependencies.length;i++) {
                if (dep.match(allDependencies.get(i))) dependencies[i] = true;
            }
        }
        
        return dependencies;
    }


    public boolean[] getGoalDependencies(List<CPUOpDependency> allDependencies)
    {
        boolean dependencies[] = new boolean[allDependencies.size()];
        for(int i = 0;i<dependencies.length;i++) dependencies[i] = false;
        
        for(SpecificationExpression exp:goalState) {
            CPUOpDependency dep = new CPUOpDependency(exp.leftRegisterName.toUpperCase(), null, null, null, null);
            for(int i = 0;i<dependencies.length;i++) {
                if (dep.match(allDependencies.get(i))) dependencies[i] = true;
            }
        }
        
        return dependencies;
    }
    
    
    public boolean initCPU(Z80Core z80, CodeBase code)
    {
        for(SpecificationExpression exp:startState) {
            Integer value = exp.right.evaluateToInteger(null, code, true);
            if (value == null) return false;
            z80.setRegisterValue(exp.leftRegister, value);
        }
        return true;
    }
    
    public boolean checkGoalState(Z80Core z80, IMemory z80memory, CodeBase code)
    {
        for(SpecificationExpression exp:goalState) {
            if (!exp.check(z80, z80memory, code)) return false;
        }
        return true;
    }
    
    
    @Override
    public String toString() {
        String tmp = "start_state:\n";
        for(SpecificationExpression exp:startState) {
            tmp += exp + "\n";
        }
        tmp += "goal_state:\n";
        for(SpecificationExpression exp:goalState) {
            tmp += exp + "\n";
        }
        return tmp;
    }            
}
