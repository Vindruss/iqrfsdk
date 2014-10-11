
package com.microrisc.simply.iqrf.dpa.types;

import com.microrisc.simply.types.ValueConversionException;
import java.util.HashMap;
import java.util.Map;

/**
 * FRC_AcknowledgedBroadcastBytes command. 
 * 
 * @author Michal Konopa
 */
public final class FRC_AcknowledgedBroadcastBytes extends AbstractFRC_Command {
    private static final int id = 0x81;
    
    /** 
     * Provides acces to parsed FRC data comming from IQRF. 
     * Note to {@code getByte()} method: <br>
     * Returned byte equals normally to the same temperature value 
     * as FRC_TemperatureFRC command, but if this FRC command is caught 
     * by FrcValue event and a nonzero value is stored at responseFRCvalue 
     * then this value instead of temperature. DPA data also stores DPA request 
     * to execute after data bytes are collected in the same way as 
     * FRC_AcknowledgedBroadcastBits FRC command does.
     */
    public static interface Result extends FRC_CollectedBytes {
    }
    
    /** Parsed FRC data comming from IQRF. */
    private static class ResultImpl implements Result {
        private final short byteValue;
        
        public ResultImpl(short byteValue) {
            this.byteValue = byteValue;
        }
        
        @Override
        public short getByte() {
            return byteValue;
        }
    }
    
    
    private static short[] checkFrcData(short[] frcData) {
        if ( frcData == null ) {
            throw new IllegalArgumentException("FRC data to parse cannot be null");
        }
        
        if ( frcData.length != 64 ) {
            throw new IllegalArgumentException(
                    "Invalid length of FRC data. Expected: 64, got: " + frcData.length 
            );
        }
        return frcData;
    }
    
    private static DPA_Request checkDpaRequest(DPA_Request dpaRequest) {
        if ( dpaRequest == null ) {
            throw new IllegalArgumentException("DPA request cannot be null");
        }
        return dpaRequest;
    }
    
    /**
     * Creates new object of {@code FRC_AcknowledgedBroadcastBytes} with specified user data.
     * @param dpaRequest DPA request to take as a user data
     * @throws IllegalArgumentException if an error has occured during conversion 
     *         of specified DPA request into the series of bytes of user data
     */
    public FRC_AcknowledgedBroadcastBytes(DPA_Request dpaRequest) {
        super();
        try {
            this.userData = DPA_RequestConvertor.getInstance().toProtoValue(checkDpaRequest(dpaRequest));
        } catch ( ValueConversionException e ) {
            throw new IllegalArgumentException("Conversion of DPA request failed: " + e);
        }
    }
    
    /**
     * Creates new object of {@code FRC_AcknowledgedBroadcastBytes} with specified user data.
     * @param userData user data
     * @throws IllegalArgumentException if {@code userData} is invalid. See the
     * {@link AbstractFRC_Command#AbstractFRC_Command(short[]) AbstractFRC_Command}
     * constructor.
     */
    public FRC_AcknowledgedBroadcastBytes(short[] userData) {
        super(userData);
    }
    
    /**
     * Creates new object of {@code FRC_AcknowledgedBroadcastBytes} with default user data.
     * See the
     * {@link AbstractFRC_Command#AbstractFRC_Command() AbstractFRC_Command}
     * constructor.
     */
    public FRC_AcknowledgedBroadcastBytes() {
    }
    
    @Override
    public int getId() {
        return id;
    }

    @Override
    public short[] getUserData() {
        return userData;
    }
    
    /**
     * Parses specified FRC data comming from IQRF.
     * @param frcData FRC data to parse
     * @return map of results for each node. Identifiers of nodes are used as a
     *         keys of the returned map.
     * @throws IllegalArgumentException if specified FRC data are not in correct format
     * @throws Exception if parsing failed
     */
    public static Map<String, Result> parse(short[] frcData) throws Exception {
        checkFrcData(frcData);
        Map<String, ResultImpl> resultImplMap = null;
        try {
            resultImplMap = FRC_ResultParser.parseAsCollectedBytes(frcData, ResultImpl.class);
        } catch ( Exception ex ) {
            throw new Exception("Parsing failed: " + ex);
        }
        Map<String, Result> resultMap = new HashMap<>();
        for ( Map.Entry<String, ResultImpl> resImplEntry : resultImplMap.entrySet() ) {
            resultMap.put(resImplEntry.getKey(), resImplEntry.getValue());
        }
        return resultMap;
    }
}
