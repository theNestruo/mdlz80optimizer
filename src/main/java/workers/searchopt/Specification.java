/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers.searchopt;

import cl.MDLConfig;
import code.CPUOpDependency;
import code.CodeBase;
import code.Expression;
import code.SourceConstant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import util.microprocessor.IMemory;
import util.microprocessor.Z80.CPUConstants;
import util.microprocessor.Z80.Z80Core;

/**
 *
 * @author santi
 */
public class Specification {
    
    public static class PrecomputedTestCase {
        CPUConstants.RegisterNames startRegisters[] = null;
        int startRegisterValues[] = null;
        CPUConstants.RegisterNames goalRegisters[] = null;
        int goalRegisterValues[] = null;

        public void initCPU(Z80Core z80)
        {
            for(int i = 0;i<startRegisters.length;i++) {
                z80.setRegisterValue(startRegisters[i], startRegisterValues[i]);
            }
        }


        public boolean checkGoalState(Z80Core z80)
        {
            for(int i = 0;i<goalRegisters.length;i++) {
                if (z80.getRegisterValue(goalRegisters[i]) != goalRegisterValues[i]) return false;
            }
            return true;
        }        
    }
    
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
    
    // precomputed, so that we don't waste time during search evaluating
    // expressions:
    public PrecomputedTestCase precomputedTestCases[] = null;
    
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
    
    /*
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
    */
    
    
    public boolean precomputeTestCases(int n, CodeBase code, MDLConfig config)
    {
        Random rand = new Random();
        precomputedTestCases = new PrecomputedTestCase[n];
        
        for(int i = 0;i<n;i++) {
            PrecomputedTestCase testCase = new PrecomputedTestCase();
            
            // randomize constants:
            for(InputParameter parameter:parameters) {
                int value = parameter.minValue + rand.nextInt((parameter.maxValue - parameter.minValue)+1);
                parameter.symbol.exp = Expression.constantExpression(value, config);
                parameter.symbol.clearCache();            
            }
            
            testCase.startRegisters = new CPUConstants.RegisterNames[startState.size()];
            testCase.startRegisterValues = new int[startState.size()];
            for(int j = 0;j<startState.size();j++) {
                SpecificationExpression exp = startState.get(j);
                Integer value = exp.right.evaluateToInteger(null, code, true);
                if (value == null) return false;
                testCase.startRegisters[j] = exp.leftRegister;
                testCase.startRegisterValues[j] = value;
            }
            testCase.goalRegisters = new CPUConstants.RegisterNames[goalState.size()];
            testCase.goalRegisterValues = new int[goalState.size()];
            for(int j = 0;j<goalState.size();j++) {
                SpecificationExpression exp = goalState.get(j);
                Integer value = exp.right.evaluateToInteger(null, code, true);
                if (value == null) return false;
                testCase.goalRegisters[j] = exp.leftRegister;
                switch(exp.leftRegister) {
                    case A:
                    case F:
                    case A_ALT:
                    case F_ALT:
                    case I:
                    case R:
                    case B:
                    case C:
                    case D:
                    case E:
                    case H:
                    case L:
                    case B_ALT:
                    case C_ALT:
                    case D_ALT:
                    case E_ALT:
                    case H_ALT:
                    case L_ALT:
                    case IXH:
                    case IXL:
                    case IYH:
                    case IYL:
                        testCase.goalRegisterValues[j] = (value & 0xff);
                        break;
                    default:
                        testCase.goalRegisterValues[j] = (value & 0xffff);
                }                
            }
            precomputedTestCases[i] = testCase;
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
