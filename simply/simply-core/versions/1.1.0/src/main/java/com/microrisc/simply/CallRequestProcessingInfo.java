

package com.microrisc.simply;

import com.microrisc.simply.errors.CallRequestProcessingError;
import java.util.UUID;

/**
 * Information about processing of a call request.
 * 
 * @author Michal Konopa
 */
public final class CallRequestProcessingInfo {
    /** ID of a call request, which processing information relates to. */
    private final UUID requestId;
    
    /** State of processing. */
    private final CallRequestProcessingState state;
    
    /** Result of a called method. */
    private final CallResult callResult;
    
    /** Information about processing errors. */
    private final CallRequestProcessingError error;
    
    
    /**
     * Creates object, which encapsulates information about processing of executed
     * call request.
     * @param requestId ID of a call request, which processing information relates to
     * @param state state of a call request processing
     * @param callResult result of called method
     * @param error error during processing of call request
     */
    public CallRequestProcessingInfo( UUID requestId,
            CallRequestProcessingState state, CallResult callResult, 
            CallRequestProcessingError error 
    ) {
        this.requestId = requestId;
        this.state = state;
        this.callResult = callResult;
        this.error = error;
    }
    
    /**
     * Creates object, which encapsulates information about processing of executed
     * call request. 
     * @param requestId ID of a call request, which processing information relates to
     * @param state state of a call request processing
     * @param callResult result of called method
     */
    public CallRequestProcessingInfo( UUID requestId, 
            CallRequestProcessingState state, CallResult callResult
    ) {
        this(requestId, state, callResult, null);
    }
    
    /**
     * Creates object, which encapsulates information about processing of executed
     * call request.
     * @param requestId ID of a call request, which processing information relates to
     * @param state state of a call request processing
     */
    public CallRequestProcessingInfo( UUID requestId, CallRequestProcessingState state) {
        this(requestId, state, null, null);
    }
    
    /**
     * @return ID of a call request, which processing information relates to 
     */
    public UUID getRequestId() {
        return requestId;
    }
    
    /**
     * @return state of a call request processing
     */
    public CallRequestProcessingState getState() {
        return state;
    }

    /**
     * @return result of called method
     */
    public CallResult getCallResult() {
        return callResult;
    }

    /**
     * @return error during processing of call request
     */
    public CallRequestProcessingError getError() {
        return error;
    }
    
    @Override
    public String toString() {
        return ("{ " +
                ", request ID=" + requestId +
                ", state=" + state + 
                ", call result=" + callResult +
                ", error=" + error +
                " }");
    }
    
}
