/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workers;

import cl.MDLConfig;
import code.CodeBase;
import code.Expression;
import code.SourceFile;
import code.CodeStatement;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author santi
 */
public class BinaryGenerator implements MDLWorker {
    MDLConfig config = null;
    String outputFileName = null;

    public BinaryGenerator(MDLConfig a_config)
    {
        config = a_config;
    }

    
    @Override
    public String docString() {
        // This string has MD tags, so that I can easily generate the corresponding documentation in github with the 
        // hidden "-helpmd" flag:        
        return "- ```-bin <output file>```: (task) generates an assembled binary.\n";
    }

    
    @Override
    public boolean parseFlag(List<String> flags) {
        if (flags.get(0).equals("-bin") && flags.size()>=2) {
            flags.remove(0);
            outputFileName = flags.remove(0);
            return true;
        }
        return false;
    }

    
    @Override
    public boolean work(CodeBase code) {
        if (outputFileName != null) {
            config.debug("Executing "+this.getClass().getSimpleName()+" worker...");
            
            try (FileOutputStream os = new FileOutputStream(outputFileName)) {
                if (!writeBytes(code.getMain(), code, os)) return false;
                os.flush();
            } catch (Exception e) {
                config.error("Cannot write to file " + outputFileName + ": " + e);
                config.error(Arrays.toString(e.getStackTrace()));
                return false;
            }
        }
        return true;
    }
    
    
    public boolean writeBytes(SourceFile sf, CodeBase code, OutputStream os) throws Exception
    {
        for (CodeStatement ss:sf.getStatements()) {
            switch(ss.type) {
                case CodeStatement.STATEMENT_NONE:
                case CodeStatement.STATEMENT_ORG:
                case CodeStatement.STATEMENT_CONSTANT:
                case CodeStatement.STATEMENT_MACRO:
                case CodeStatement.STATEMENT_MACROCALL:
                    break;

                case CodeStatement.STATEMENT_INCLUDE:
                    if (!writeBytes(ss.include, code, os)) return false;
                    break;

                case CodeStatement.STATEMENT_INCBIN:
                {
                    int skip = 0;
                    int size = 0;
                    if (ss.incbinSkip != null) skip = ss.incbinSkip.evaluateToInteger(ss, code, false);
                    if (ss.incbinSize != null) size = ss.incbinSize.evaluateToInteger(ss, code, false);
                    try (InputStream is = new FileInputStream(ss.incbin)) {
                        while(is.available() != 0) {
                            int data = is.read();
                            if (skip > 0) {
                                skip --;
                                continue;
                            }
                            os.write(data);
                            size --;
                            if (size <= 0) break;
                        }
                    } catch(Exception e) {
                        config.error("Cannot expand incbin: " + ss.incbin);
                        return false;
                    }
                    break;
                }

                case CodeStatement.STATEMENT_DATA_BYTES:
                    for(Expression exp: ss.data) {        
                        if (!expressionToBytes(exp, ss, code, os)) return false;
                    }
                    break;

                case CodeStatement.STATEMENT_DATA_WORDS:
                    for(Expression exp: ss.data) {
                        if (exp.evaluatesToNumericConstant()) {
                            int v = exp.evaluateToInteger(ss, code, true);
                            os.write(v&0x00ff);
                            os.write((v>>8)&0x00ff);
                        } else {
                            config.error("Cannot evaluate expression " + exp + "when generating a binary.");
                            return false;
                        }
                    }
                    break;

                case CodeStatement.STATEMENT_DATA_DOUBLE_WORDS:
                    for(Expression exp: ss.data) {
                        if (exp.evaluatesToNumericConstant()) {
                            int v = exp.evaluateToInteger(ss, code, true);
                            os.write(v&0x00ff);
                            os.write((v>>8)&0x00ff);
                            os.write((v>>16)&0x00ff);
                            os.write((v>>24)&0x00ff);
                        } else {
                            config.error("Cannot evaluate expression " + exp + "when generating a binary.");
                            return false;
                        }
                    }
                    break;

                case CodeStatement.STATEMENT_DEFINE_SPACE:
                    if (ss.space_value != null) {
                        Integer value = ss.space_value.evaluateToInteger(ss, code, true);
                        Integer amount = ss.space.evaluateToInteger(ss, code, true);
                        if (value == null) {
                            config.error("Cannot evaluate " + ss.space_value + " in " + ss.sl);
                            return false;
                        }
                        if (amount == null) {
                            config.error("Cannot evaluate " + ss.space + " in " + ss.sl);
                            return false;
                        }
                        for(int i = 0;i<amount;i++) os.write(value);
                    }
                    break;

                case CodeStatement.STATEMENT_CPUOP:
                {
                    List<Integer> data = ss.op.assembleToBytes(ss, code, config);
                    if (data == null) {
                        config.error("Cannot convert op " + ss.op + " to bytes!");
                        return false;
                    }
                    for(Integer value: data) {
                        os.write(value);
                    }
                    break;
                }
            }
        }
        
        return true;
    }
    
    
    public boolean expressionToBytes(Expression exp, CodeStatement ss, CodeBase code, OutputStream os) throws Exception 
    {
        Object val = exp.evaluate(ss, code, true);
        if (val == null) {
            config.error("Cannot evaluate expression " + exp + "when generating a binary.");
            return false;
        }
        return valueToBytes(val, ss, code, os);
    }
    
    
    public boolean valueToBytes(Object val, CodeStatement ss, CodeBase code, OutputStream os) throws Exception 
    {
        if (val instanceof Integer) {
            int v = (Integer)val;
            os.write(v&0x00ff);
        } else if (val instanceof String) {
            String v = (String)val;
            for(int i = 0;i<v.length();i++) {
                os.write(v.charAt(i));
            }
        } else if (val instanceof List) {
            for(Object val2:(List)val) {
                if (!valueToBytes(val2, ss, code, os)) return false;
            }
        } else if (val instanceof Expression) {
            return expressionToBytes((Expression)val, ss, code, os);
        } else {
            config.error("Unsupported value " + val + "when generating a binary.");
            return false;
        }
        
        return true;
    }    
    
    
    @Override
    public boolean triggered() {
        return outputFileName != null;
    }

    
    @Override
    public MDLWorker cloneForExecutionQueue() {
        BinaryGenerator w = new BinaryGenerator(config);
        w.outputFileName = outputFileName;
        
        // reset state:
        outputFileName = null;
        
        return w;
    }    
}
