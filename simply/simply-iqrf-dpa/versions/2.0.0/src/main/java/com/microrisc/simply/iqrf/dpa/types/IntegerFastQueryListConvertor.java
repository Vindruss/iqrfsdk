
package com.microrisc.simply.iqrf.dpa.types;

import com.microrisc.simply.types.AbstractConvertor;
import com.microrisc.simply.types.ValueConversionException;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functionality for conversion from IQRF DPA bit array 
 * to {@code IntegerFastQueryList} values. 
 * 
 * @author Michal Konopa
 */
public final class IntegerFastQueryListConvertor extends AbstractConvertor {
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(IntegerFastQueryListConvertor.class);
    
    /** Singleton. */
    private static final IntegerFastQueryListConvertor instance = new IntegerFastQueryListConvertor();
    
    private IntegerFastQueryListConvertor() {}
    
    /**
     * @return IntegerFastQueryListConvertor instance 
     */
    static public IntegerFastQueryListConvertor getInstance() {
        return instance;
    }
    
    
    @Override
    public short[] toProtoValue(Object value) throws ValueConversionException {
        throw new UnsupportedOperationException("Currently not supported");
    }

    @Override
    public Object toObject(short[] protoValue) throws ValueConversionException {
        logger.debug("toObject - start: protoValue={}", protoValue);
        
        List<Integer> membersList = new LinkedList<Integer>();
        for (int byteId = 0; byteId < protoValue.length; byteId++) {
            if (protoValue[byteId] == 0) {
                continue;
            }
            
            int bitComp = 1;
            for (int bitId = 0; bitId < 8; bitId++) {
                if ((protoValue[byteId] & bitComp) == bitComp) {
                    membersList.add(byteId * 8 + bitId);
                }
                bitComp *= 2;
            }
        }
        IntegerFastQueryList intQueryList = new IntegerFastQueryList(membersList);
        
        logger.debug("toObject - end: {}", intQueryList);
        return intQueryList;
    }
}
