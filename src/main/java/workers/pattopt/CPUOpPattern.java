/*
 * Author: Santiago Ontañón Villar (Brain Games)
 */
package workers.pattopt;

import java.util.ArrayList;
import java.util.List;

import cl.MDLConfig;
import code.CPUOp;
import code.CodeBase;
import code.Expression;
import code.SourceFile;
import code.CodeStatement;
import java.util.HashMap;
import parser.SourceLine;

/**
 *
 * @author santi
 */
public class CPUOpPattern {
    public static final String WILDCARD = "*";
    
    int ID;
    boolean wildcard = false;   // if this is true, "opName" and "args" will be "*" and empty.
    String repetitionVariable = null;   // if this is not null, it means we can match this line many times in a row (its number will be matched to "repetitionVariable")
    String opName;
    List<Expression> args = new ArrayList<>();


    @Override
    public String toString()
    {
        String str = opName;
        boolean first = true;
        for(Expression arg:args) {
            if (first) {
                str += " " + arg;
                first = false;
            } else {
                str += ", " + arg;
            }
        }
        return str;
    }

    
    public boolean isWildcard()
    {
        return opName.equals(WILDCARD);
    }
    
    
    public Expression instantiateExpression(Expression exp, HashMap<String, Expression> variables)
    {
        if (exp.type == Expression.EXPRESSION_SYMBOL) {
            if (variables.containsKey(exp.symbolName)) {
                return variables.get(exp.symbolName).clone();
            } else {
                return exp;
            }
        } else if (exp.args != null) {
            List<Expression> newArgs = new ArrayList<>();
            boolean replacement = false;
            for(Expression arg:exp.args) {
                Expression newArg = instantiateExpression(arg, variables);
                if (newArg != arg) {
                    replacement = true;
                }
                newArgs.add(newArg);
            }
            if (replacement) {
                exp = exp.clone();
                exp.args = newArgs;
                return exp;
            } else {
                return exp;
            }
        } else {
            return exp;        
        }
    }
    

    public CPUOp instantiate(PatternMatch match, Pattern pattern, MDLConfig config)
    {
        // replace variables by the matched values:
        CodeBase code = new CodeBase(config);
        SourceFile f = new SourceFile("", null, null, code, config);
        CodeStatement s = new CodeStatement(CodeStatement.STATEMENT_CPUOP, new SourceLine("", f, 0), f, config);
        List<Expression> instantiatedArgs = new ArrayList<>();
        for(Expression arg:args) {
            Expression exp = instantiateExpression(arg, match.variables);
            if (exp != arg) {
                if (exp.type != Expression.EXPRESSION_INTEGER_CONSTANT && 
                    exp.evaluatesToIntegerConstant()) {
                    Integer value = exp.evaluateToInteger(s, code, true);
                    if (value != null) {
                        exp = Expression.constantExpression(value, config);
                    }
                }
            }
            
            instantiatedArgs.add(exp);
        }
        String instantiatedOpName = opName;
        if (opName.startsWith("?op") && match.variables.containsKey(opName)) {
            instantiatedOpName = match.variables.get(opName).toString();
        }

        List<CPUOp> op_l =  config.opParser.parseOp(instantiatedOpName, instantiatedArgs, s, null, code);
        if (op_l == null) {
            config.error("Cannot parse: " + opName + " " + instantiatedArgs);
            return null;
        }
        if (op_l.size() != 1) {
            config.error("Cannot parse (it's a fake instruction!): " + opName + " " + instantiatedArgs);
        }
        return op_l.get(0);
    }


    public static CPUOpPattern parse(String line, CodeBase code, MDLConfig config)
    {
        List<String> tokens = config.tokenizer.tokenize(line);
        CPUOpPattern pat = new CPUOpPattern();
        pat.ID = Integer.parseInt(tokens.remove(0));
        if (!tokens.remove(0).equals(":")) {
            config.error("Cannot parse CPUOpPattern: " + line);
            return null;
        }
        String token = tokens.remove(0);
        if (token.equals("[")) {
            // it's a repetition pattern:
            token = tokens.remove(0);
            if (!token.equals("?")) {
                config.error("Cannot parse CPUOpPattern: " + line);
                return null;                
            }
            pat.repetitionVariable = "?" + tokens.remove(0);
            if (!tokens.remove(0).equals("]")) {
                config.error("Cannot parse CPUOpPattern: " + line);
                return null;                
            }
            token = tokens.remove(0);
        }
        pat.opName = token;
        if (pat.opName.equals(WILDCARD)) return pat;
        if (pat.opName.equals("?")) {
            pat.opName = "?" + tokens.remove(0);
        }
        
        while(!tokens.isEmpty()) {
            if (config.tokenizer.isSingleLineComment(tokens.get(0))) break;
            Expression exp = config.expressionParser.parse(tokens, null, null ,code);
            if (exp == null) {
                config.error("Cannot parse CPUOpPattern: " + line);
                return null;
            } else {
                pat.args.add(exp);
            }
            if (!tokens.isEmpty() && tokens.get(0).equals(",")) {
                tokens.remove(0);
            } else {
                break;
            }
        }
        return pat;
    }
}
