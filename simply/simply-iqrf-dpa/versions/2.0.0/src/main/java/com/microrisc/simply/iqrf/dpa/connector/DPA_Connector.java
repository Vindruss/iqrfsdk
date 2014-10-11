

package com.microrisc.simply.iqrf.dpa.connector;

import com.microrisc.simply.AbstractMessage;
import com.microrisc.simply.BaseCallResponse;
import com.microrisc.simply.CallRequest;
import com.microrisc.simply.CallRequestProcessingInfo;
import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import static com.microrisc.simply.CallRequestProcessingState.RESULT_ARRIVED;
import static com.microrisc.simply.CallRequestProcessingState.WAITING_FOR_PROCESSING;
import static com.microrisc.simply.CallRequestProcessingState.WAITING_FOR_RESULT;
import com.microrisc.simply.CallResult;
import com.microrisc.simply.ConnectedDeviceObject;
import com.microrisc.simply.ConnectorListener;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.ProtocolLayerService;
import com.microrisc.simply.asynchrony.BaseAsynchronousMessage;
import com.microrisc.simply.asynchrony.AsynchronousMessagesGenerator;
import com.microrisc.simply.asynchrony.AsynchronousMessagesGeneratorListener;
import com.microrisc.simply.connector.AbstractConnector;
import com.microrisc.simply.connector.CallResultsSender;
import com.microrisc.simply.connector.response_waiting.ResponseWaitingConnector;
import com.microrisc.simply.errors.DispatchingRequestToProtocolLayerError;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.broadcasting.BroadcastRequest;
import com.microrisc.simply.iqrf.dpa.broadcasting.BroadcastingConnectorService;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple response waiting connector supporting DPA.
 * 
 * @author Michal Konopa
 */
public final class DPA_Connector
extends AbstractConnector 
implements 
        ResponseWaitingConnector, 
        BroadcastingConnectorService,
        AsynchronousMessagesGenerator<DPA_AsynchronousMessage>
{
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPA_Connector.class);
    
    
    /**
     * Call request associated with information needed to process that call request.
     */
    private class CallRequestToProcess {
        CallRequest callRequest;
        long maxProcTime;
        
        public CallRequestToProcess(CallRequest callRequest, long maxProcTime) {
            this.callRequest = callRequest;
            this.maxProcTime = maxProcTime;
        }
    }
    
    private class IdleRequest {
        CallRequestToProcess reqToProc;
        long startIdleTime;
        
        public IdleRequest(CallRequestToProcess reqToProc, long startIdleTime) {
            this.reqToProc = reqToProc;
            this.startIdleTime = startIdleTime;
        }
    }
    
    private CallResultsSender callResultsSender = null;
    
    
    // implementation of some aspects of asynchronous messages generator
    private class AsyncMsgGeneratorImpl {
        /** Registered listeners. */
        private final List<AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage>> regListeners;
        
        /** Synchronization for registered listeners. */
        private final Object synchroRegListeners = new Object();
        
        
        public AsyncMsgGeneratorImpl() {
            this.regListeners = new LinkedList<>();
        }
        
        public void registerListener(
                AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage> listener
        ) {
            synchronized ( synchroRegListeners ) {
                for ( AsynchronousMessagesGeneratorListener regListener : regListeners ) {
                    if ( listener == regListener ) {
                        return;
                    }
                }
                regListeners.add(listener);
            }
        }
        
        public void unregisterListener(
                AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage> listener
        ) {
            synchronized ( synchroRegListeners ) {
                Iterator<AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage>> listenerIter 
                        = regListeners.iterator();
                while ( listenerIter.hasNext() ) {
                    if ( listener == listenerIter.next() ) {
                        listenerIter.remove();
                        return;
                    }
                }
            }
        }
        
        public List<AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage>> getListeners() 
        {
            List<AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage>> listToReturn 
                    = new LinkedList<>();
            synchronized ( synchroRegListeners ) {
                listToReturn.addAll(regListeners);
            }
            return listToReturn;
        }
    }
    
    AsyncMsgGeneratorImpl asyncMsgGenerator = null;
    
    
    /**
     * Responsible for sending requests to underlaying network and receiving 
     * messages (responses) from that network.
     */
    private class WorkerThread extends Thread {
        /** Time, when last request was sended. */
        private long lastSendTime = 0;
        
        /** Call request, which was lastly sent to protocol layer. */
        private CallRequestToProcess lastRequestToProc = null;
        
        /** 
         * Returns value of sleep time before sending next request to
         * protocol layer.
         * @param sendAttempt send attempt
         */
        private long getSleepTimeBeforeSend(int sendAttempt) {
            long waitedTime = System.currentTimeMillis() - lastSendTime;
            if ( sendAttempt > 0 ) {
                if ( attemptPause > betweenSendPause ) {
                    return ( attemptPause - waitedTime );
                } else {
                    return ( betweenSendPause - waitedTime );
                }

            }
            return ( betweenSendPause - waitedTime );
        }   
        
        /**
         * Sends specified DO method call request to protocol layer. <br>
         * SOME TIMEOUT IS MANDATORY, OTHERWISE THE CONNECTION DEVICE MAY
         * CRASH !!!
         * @param request DO method call-request
         */
        private void sendRequestToProtocolLayer(CallRequest request) 
                throws SimplyException, InterruptedException 
        {
            logger.debug("sendRequestToProtocolLayer - start: request={}", request);
            
            int sendAttempt = 0;
            boolean sentOK = false;
            while ( (sendAttempt < maxSendAttempts && ( sentOK == false )) ) { 
                long sleepTime = getSleepTimeBeforeSend(sendAttempt);
                
                // pause between sending request to protocol layer
                if ( sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                
                protocolLayerService.sendRequest(request);
                sentOK = true;
               
                lastSendTime = System.currentTimeMillis();                
                sendAttempt++;
            }
            
            logger.debug("sendRequestToProtocolLayer - end:");
        }
        
        /**
         * Processes specified response from underlaying network.
         * @param response response to process
         */
        private CallRequestProcessingInfo createCallRequestProcessingInfo( 
                BaseCallResponse response 
        ) {
            logger.debug("createCallRequestProcessingInfo - start: {}", response);
            
            CallResult callResult = new CallResult( response.getMainData(), 
                    response.getAdditionalData()
            );
            
            CallRequestProcessingInfo procInfo = null;
            CallRequestProcessingError procError = response.getProcessingError();
            if  ( procError != null ) {
                procInfo = new CallRequestProcessingInfo(
                    currProcRequestInfo.getRequestId(), ERROR, callResult, procError
                );
            } else {
                procInfo = new CallRequestProcessingInfo(
                    currProcRequestInfo.getRequestId(), RESULT_ARRIVED, callResult
                );
            }
            
            logger.debug("createCallRequestProcessingInfo - end: {}", procInfo);
            return procInfo;
        }
        
        private boolean responseArrivedForLastRequest() {
            if ( msgFromProtoLayer.isEmpty() ) {
                return false;
            }
            
            AbstractMessage message = msgFromProtoLayer.element();
            if ( message instanceof BaseCallResponse ) {
                BaseCallResponse response = (BaseCallResponse) message;
                if ( response.getRequestId().equals(currProcRequestInfo.getRequestId()) ) {
                    return true;
                }
            }
            return false;
        }
        
        // Processes next message in the input queue from protocol layer.
        private void processNextIncommingMessage() {
            logger.debug("processNextIncommingMessage - start:");
            
            AbstractMessage message = msgFromProtoLayer.poll();
            logger.info("Processed message: {}", message);
            
            if ( message instanceof BaseCallResponse ) {
                BaseCallResponse response = (BaseCallResponse) message;
                if ( response.getRequestId().equals(currProcRequestInfo.getRequestId()) ) {
                    logger.info("Response found");
                    CallRequestProcessingInfo procInfo = createCallRequestProcessingInfo( response );
                    callResultsSender.addCallRequestProcessingInfo(procInfo);
                } else {
                    logger.warn("Response not matching to the last request. "
                            + "Response will be discarded."
                    );
                }     
            } else {
                logger.info("Non response type of message found: {}", message);
            }
            
            logger.debug("processNextIncommingMessage - end");
        }
        
        // processes all messages incomming from protocol layer
        private void processAllIncommingMessages() {
            while ( !msgFromProtoLayer.isEmpty() ) {
                processNextIncommingMessage();
            }
        }
        
        // processes all asynchronous messages incomming from protocol layer
        private void processAllIncommingAsynchronousMessages() {
            logger.debug("processAllIncommingAsynchronousMessages - start:");
            
            while ( !asyncMsgFromProtoLayer.isEmpty() ) {
                DPA_AsynchronousMessage asyncMsg = asyncMsgFromProtoLayer.poll();
                for ( AsynchronousMessagesGeneratorListener regListener : 
                        asyncMsgGenerator.getListeners()
                ) {
                    regListener.onAsynchronousMessage(asyncMsg);
                }
            }
            
            logger.debug("processAllIncommingAsynchronousMessages - end");
        }
        
        // variable variant for CallRequestProcessingInfo
        private class VariableCallRequestProcessingInfo {
            private UUID requestId = null;
            private CallRequestProcessingState state = null;
            private CallResult callResult = null;
            private CallRequestProcessingError error = null;

            
            public VariableCallRequestProcessingInfo() {
            }
            
            public VariableCallRequestProcessingInfo( UUID requestId,
                    CallRequestProcessingState state, CallResult callResult, 
                    CallRequestProcessingError error 
            ) {
                this.requestId = requestId;
                this.state = state;
                this.callResult = callResult;
                this.error = error;
            }

            public synchronized void setRequestId(UUID requestId) {
                this.requestId = requestId;
            }
            
            public UUID getRequestId() {
                return requestId;
            }
            
            public synchronized void setState(CallRequestProcessingState state) {
                this.state = state;
            }
            
            public synchronized void setCallResult(CallResult callResult) {
                this.callResult = callResult;
            }

            public synchronized void setError(CallRequestProcessingError error) {
                this.error = error;
            }
            
            public synchronized void setAll( 
                    UUID requestId, CallRequestProcessingState state, 
                    CallResult callResult, CallRequestProcessingError error 
            ) {
                this.requestId = requestId;
                this.state = state;
                this.callResult = callResult;
                this.error = error;
            }
            
            public synchronized CallRequestProcessingInfo getCallRequestProcessingInfo() {
                return new CallRequestProcessingInfo(requestId, state, callResult, error);
            }
        }
        
        // info about currently processed request 
        private VariableCallRequestProcessingInfo currProcRequestInfo = 
                new VariableCallRequestProcessingInfo();
        private final Object syncCurrProcRequestInfo = new Object(); 
        
        
        // sets maximal processing time for specified request 
        public void setCallRequestProcessingTime(UUID reqId, long maxProcTime) {
            synchronized ( syncRequestsToProcess ) {
                for ( CallRequestToProcess reqToProc : requestsToProcess ) {
                    if ( reqToProc.callRequest.getId().equals(reqId) ) {
                        reqToProc.maxProcTime = maxProcTime;
                        return;
                    }
                }
            }
            
            synchronized ( syncIdleRequests ) {
                boolean found = false;
                Iterator<IdleRequest> requestIt = idleRequests.iterator();
                while ( requestIt.hasNext() ) {
                    IdleRequest idleRequest = requestIt.next();
                    long idleTime = System.currentTimeMillis() - idleRequest.startIdleTime;
                    if ( idleTime > maxCallRequestIdleTime ) {
                        requestIt.remove();
                        continue;
                    }
                    
                    if ( found ) {
                        continue;
                    }
                    
                    if ( idleRequest.reqToProc.callRequest.getId().equals(reqId) ) {
                        idleRequest.startIdleTime = System.currentTimeMillis();
                        idleRequest.reqToProc.maxProcTime = maxProcTime;
                        found = true;
                    }
                }
            }
        } 
        
        /**
         * Returns processing info of specified request or null, if no such 
         * request was found.
         */
        public CallRequestProcessingInfo getCallRequestProcessingInfo(UUID reqId) {
            // if the request is waiting in input requests queue
            synchronized( syncRequestsToProcess ) {
                for ( CallRequestToProcess reqToProcess : requestsToProcess ) {
                    if ( reqToProcess.callRequest.getId().equals(reqId) ) {
                        return new CallRequestProcessingInfo( reqId, WAITING_FOR_PROCESSING );
                    }
                }
            }
            
            synchronized ( syncCurrProcRequestInfo ) {
                if ( currProcRequestInfo.getRequestId().equals(reqId) ) {
                    return currProcRequestInfo.getCallRequestProcessingInfo();
                }
            }
            
            CallRequestProcessingInfo procInfo = null; 
            
            synchronized ( syncIdleRequests ) {
                boolean found = false;
                Iterator<IdleRequest> requestIt = idleRequests.iterator();
                while ( requestIt.hasNext() ) {
                    IdleRequest idleRequest = requestIt.next();
                    long idleTime = System.currentTimeMillis() - idleRequest.startIdleTime;
                    if ( idleTime > maxCallRequestIdleTime ) {
                        requestIt.remove();
                        continue;
                    }
                    
                    if ( found ) {
                        continue;
                    }
                    
                    if ( idleRequest.reqToProc.callRequest.getId().equals(reqId) ) {
                        procInfo = new CallRequestProcessingInfo(reqId, WAITING_FOR_PROCESSING );
                        found = true;
                    }
                }
            }
            
            // could be NULL
            return procInfo;
        }
        
        /**
         * Cancels processing of specified request.
         * @param reqId ID of request to cancel
         */
        public void cancelCallRequest(UUID reqId) {
            synchronized ( syncRequestsToProcess ) {
                Iterator<CallRequestToProcess> requestIt = requestsToProcess.iterator();
                while ( requestIt.hasNext() ) {
                    CallRequestToProcess reqToProc = requestIt.next();
                    if ( reqToProc.callRequest.getId().equals(reqId) ) {
                        requestIt.remove();
                    }
                }
            }
            
            synchronized ( syncIdleRequests ) {
                boolean found = false;
                Iterator<IdleRequest> requestIt = idleRequests.iterator();
                while ( requestIt.hasNext() ) {
                    IdleRequest idleRequest = requestIt.next();
                    long idleTime = System.currentTimeMillis() - idleRequest.startIdleTime;
                    if ( idleTime > maxCallRequestIdleTime ) {
                        requestIt.remove();
                        continue;
                    }
                    
                    if ( found ) {
                        continue;
                    }
                    
                    if ( idleRequest.reqToProc.callRequest.getId().equals(reqId) ) {
                        requestIt.remove();
                        found = true;
                    }
                }
            }
        }
        
        // removes idle requests, which exceeded max idle time period
        private void removeMaxTimeExceededIdleRequests() {
            synchronized ( syncIdleRequests ) {
                Iterator<IdleRequest> requestIt = idleRequests.iterator();
                while ( requestIt.hasNext() ) {
                    IdleRequest idleRequest = requestIt.next();
                    long idleTime = System.currentTimeMillis() - idleRequest.startIdleTime;
                    if ( idleTime > maxCallRequestIdleTime ) {
                        requestIt.remove();
                    }
                }
            }
        }
        
        
        @Override
        public void run() {
            while ( true ) {
                if ( this.isInterrupted() ) {
                    logger.info("Worker thread end");
                    return;
                }
                
                // waiting for the situation, when there is some request or
                // some message from the protocol layer
                synchronized( syncRequestOrMessage ) {
                    while ( requestsToProcess.isEmpty() && asyncMsgFromProtoLayer.isEmpty() ) {
                        try {
                            syncRequestOrMessage.wait();
                        } catch ( InterruptedException e ) {
                            logger.warn(
                                    "Worker thread interrupted while waiting on requests"
                                            + " and messages", e
                            );
                            return;
                        }
                    }
                }
                
                // processing of asynchronous messages
                synchronized ( syncAsyncMsgfromProtoLayer ) {
                    if ( !asyncMsgFromProtoLayer.isEmpty() ) {
                        processAllIncommingAsynchronousMessages();
                    }
                }
                
                // checking, if there are some new requests to process
                synchronized( syncRequestsToProcess ) {
                    if ( !requestsToProcess.isEmpty() ) {
                        lastRequestToProc = requestsToProcess.poll();
                    } else {
                        lastRequestToProc = null;
                    }
                }
                
                // if no new requests, go to waiting
                if ( lastRequestToProc == null ) {
                    continue;
                }
                
                // processing of new requests
                currProcRequestInfo.setAll( 
                        lastRequestToProc.callRequest.getId(), 
                        WAITING_FOR_PROCESSING, null, null
                );
                
                boolean respArrivedForLastRequest = false;
                boolean requestSentOk = false;
                
                // sending last request to protocol layer
                try {
                    sendRequestToProtocolLayer( lastRequestToProc.callRequest );
                    requestSentOk = true;
                } catch ( Exception ex ) {
                    // dispatching error
                    logger.error("Send request to protocol layer error", ex);
                    currProcRequestInfo.setAll( lastRequestToProc.callRequest.getId(), ERROR, 
                            null, new DispatchingRequestToProtocolLayerError(ex)
                    );
                    callResultsSender.addCallRequestProcessingInfo(
                            currProcRequestInfo.getCallRequestProcessingInfo() 
                    );
                } 
                
                if ( !requestSentOk ) {
                    removeMaxTimeExceededIdleRequests();
                    continue;
                }
                
                // waiting for a response of the sent request
                currProcRequestInfo.setState( WAITING_FOR_RESULT );
                synchronized( syncMsgfromProtoLayer ) {
                    // while-cycle is needed because other responses may
                    // arrive to the connector
                    long totalWaitingTime = 0;
                    respArrivedForLastRequest = responseArrivedForLastRequest();
                    
                    while ( !respArrivedForLastRequest ) {
                        try {
                            long beforeWaitTime = System.currentTimeMillis();
                            syncMsgfromProtoLayer.wait( lastRequestToProc.maxProcTime );
                            totalWaitingTime += System.currentTimeMillis() - beforeWaitTime;

                            respArrivedForLastRequest = responseArrivedForLastRequest();
                            if ( totalWaitingTime >= lastRequestToProc.maxProcTime ) {
                                break;
                            }
                        } catch ( InterruptedException e ) {
                            logger.warn("Worker thread interrupted while "
                                    + "waiting on response", e);
                            return;
                        }
                    }
                }

                // process arrived messages
                if ( !msgFromProtoLayer.isEmpty() ) {
                    processAllIncommingMessages();
                    if ( !respArrivedForLastRequest ) {
                        // there wasn't a response for a last request
                        idleRequests.add( new IdleRequest(lastRequestToProc, System.currentTimeMillis()) );
                        logger.warn("No messages arrived for last request.");
                    }
                } else {
                    // no messages arrived in the timeout
                    idleRequests.add( new IdleRequest(lastRequestToProc, System.currentTimeMillis()) );
                    logger.warn("No messages arrived at timeout");
                }
                
                // removes idle requests, which exceeded max idle time period
                removeMaxTimeExceededIdleRequests();
            }
        }
    }
    
    /** 
     * Worker thread: sending call requests to protocol layer and receiving 
     * messages from protocol layer to connector. 
     */
    private WorkerThread workerThread = null;
    
    /** 
     * Queue of incomming call requests to process.
     */
    private Queue<CallRequestToProcess> requestsToProcess = new ConcurrentLinkedQueue<>();
    
    /**
     * Synchronization object for {@code requestsToProcess}. 
     */
    private final Object syncRequestsToProcess = new Object();
    
    
    /**
     * Idle requests.
     */
    private Queue<IdleRequest> idleRequests = new ConcurrentLinkedQueue<>();
    
    /**
     * Synchronization object for {@code idleRequests}. 
     */
    private final Object syncIdleRequests = new Object();
    
    
    
    /** 
     * Queue of messages received from protocol layer.  
     */  
    private Queue<AbstractMessage> msgFromProtoLayer = new ConcurrentLinkedQueue<>(); 
    
    /**
     * Synchronization object for messages incomming from protocol layer.
     */
    private final Object syncMsgfromProtoLayer = new Object();
   
    /** 
     * Synchronization object for situation, when there is some waiting request 
     * or some message from protocol layer.
     */
    private final Object syncRequestOrMessage = new Object();
    
    
    
    /** 
     * Queue of asynchronous messages received from protocol layer.  
     */  
    private Queue<DPA_AsynchronousMessage> asyncMsgFromProtoLayer = 
            new ConcurrentLinkedQueue<>();
    
    /**
     * Synchronization object for asynchronous messages incomming from protocol layer.
     */
    private final Object syncAsyncMsgfromProtoLayer = new Object(); 
    
    
    private static long MAX_CALL_REQUEST_IDLE_TIME_DEFAULT = 30000;
    private volatile long maxCallRequestIdleTime = MAX_CALL_REQUEST_IDLE_TIME_DEFAULT;
    
    
    /**
     * Default pause between subsequent attempts to send request to underlaying
     * network [in miliseconds].
     */
    private static long ATTEMPT_PAUSE_DEFAULT = 1000;
    
    /** Default minimal pause between sending requests [in miliseconds]. */
    private static long BETWEEN_SEND_PAUSE_DEFAULT = 1000;
    
    /**
     * Default number of maximal attempts of sending request to underlaying
     * network.
     */
    private static int MAX_SEND_ATTEMPTS_DEFAULT = 3;
    
    /**
     * Default timeout for waiting for a response from underlaying network
     * [in miliseconds].
     */
    private static long RESP_TIMEOUT_DEFAULT = 25000;
    
    
    /** Number of maximal attempts of sending request to underlaying network. */
    private volatile int maxSendAttempts = MAX_SEND_ATTEMPTS_DEFAULT;
    
    /** 
     * Pause between subsequent attempts to send request to underlaying network
     * [in miliseconds]. 
     */
    private volatile long attemptPause = ATTEMPT_PAUSE_DEFAULT;
    
    /** 
     * Timeout for waiting for a response from underlaying network 
     * [in miliseconds]. 
     */
    private volatile long responseTimeout = RESP_TIMEOUT_DEFAULT;
    
    /** Minimal pause between sending requests [in miliseconds]. */
    private volatile long betweenSendPause = BETWEEN_SEND_PAUSE_DEFAULT;
    
    
    private static long checkMaxProcessingTime( long maxProcTime ) {
        if ( maxProcTime <= 0 ) {
            throw new IllegalArgumentException("Maximal processing time must be greather then 0");
        }
        return maxProcTime;
    }
    
    
    
    /**
     * Creates new response-waiting connector.
     * @param protocolLayerService protocol layer service to use
     */
    public DPA_Connector(ProtocolLayerService protocolLayerService) {
       super( protocolLayerService );
       this.callResultsSender = new CallResultsSender();
       this.workerThread = new WorkerThread();
       this.asyncMsgGenerator = new AsyncMsgGeneratorImpl();
    }
    
    /**
     * @throws IllegalArgumentException if {@code timeout} is less than or 
     *         equal to 0
     */
    @Override
    public UUID callMethod( ConnectedDeviceObject devObject, Class deviceIface, 
            String methodId, Object[] args, long maxProcTime
    ) {
        Object[] logArgs = new Object[5];
        logArgs[0] = devObject.getNodeId();
        logArgs[1] = deviceIface.getName();
        logArgs[2] = methodId;
        logArgs[3] = args;
        logArgs[4] = maxProcTime;
        logger.debug("callMethod - start: devObject={}, devIface={}, methodId={}, "
                + "args={}, timeout={}", logArgs
        );
        
        // checking maximal processing time
        maxProcTime = checkMaxProcessingTime( maxProcTime );
        
        UUID callId = UUID.randomUUID();
        CallRequest request = new CallRequest(
                callId, devObject.getNetworkId(), devObject.getNodeId(), deviceIface, 
                methodId, args
        );
        
        // associate request ID with addressee of its result
        callResultsSender.associateCallRequestWithAddressee(callId, devObject);
        
        CallRequestToProcess requestToProcess = new CallRequestToProcess(request, maxProcTime);
        synchronized ( syncRequestOrMessage ) {
            synchronized( syncRequestsToProcess ) {
                requestsToProcess.offer( requestToProcess );
                syncRequestsToProcess.notifyAll();
            }
            syncRequestOrMessage.notifyAll();
        }
        
        logger.info("New call request created: {}", request);
        logger.debug("callMethod - end: {}", callId);
        return callId;
    }
    
    @Override
    public UUID callMethod(ConnectedDeviceObject deviceObject, Class deviceIface, 
            String methodId, Object[] args
    ) {
        return callMethod(deviceObject, deviceIface, methodId, args, responseTimeout);
    }
    
    @Override
    public void setCallRequestProcessingTime(UUID requestId, long maxProcTime) {
        workerThread.setCallRequestProcessingTime(requestId, maxProcTime);
    }
    
    @Override
    public CallRequestProcessingInfo getCallRequestProcessingInfo(UUID callId) {
        CallRequestProcessingInfo procInfo = workerThread.getCallRequestProcessingInfo(callId);
        if ( procInfo != null ) {
            return procInfo;
        }
        
        // if procInfo == null, then workerThread hasn't any information about
        // specified request - so it is neccessary to query listener thread
        procInfo = callResultsSender.getCallRequestProcessingInfo(callId);
        return procInfo;
    }

    @Override
    public void cancelCallRequest(UUID callId) {
        workerThread.cancelCallRequest(callId);
    }
    
    @Override
    public UUID broadcastCallMethod(
            ConnectorListener connListener,
            String networkId,
            Class deviceIface, 
            String methodId, 
            Object[] args,
            long maxProcTime
    ) {
        Object[] logArgs = new Object[6];
        logArgs[0] = connListener;
        logArgs[1] = networkId;
        logArgs[2] = deviceIface;
        logArgs[3] = methodId;
        logArgs[4] = args;
        logArgs[5] = maxProcTime;
        logger.debug("broadcastCallMethod - start: connListener={}, networkId={}, "
                + "devIface={}, methodId={}, args={}, maxProcTime={}", logArgs
        );
        
        // checking maximal processing time
        maxProcTime = checkMaxProcessingTime( maxProcTime );
        
        UUID requestId = UUID.randomUUID();
        BroadcastRequest request = new BroadcastRequest(
                requestId, networkId, deviceIface, methodId, args
        );
        
        // associate request ID with addressee of its result
        callResultsSender.associateCallRequestWithAddressee(requestId, connListener);
        
        CallRequestToProcess requestToProcess = new CallRequestToProcess(request, maxProcTime);
        synchronized ( syncRequestOrMessage ) {
            synchronized( syncRequestsToProcess ) {
                requestsToProcess.offer( requestToProcess );
                syncRequestsToProcess.notifyAll();
            }
            syncRequestOrMessage.notifyAll();
        }
        
        logger.info("New broadcast call request created: {}", request);
        logger.debug("broadcastCallMethod - end: {}", requestId);
        return requestId;
    }
    
    @Override
    public UUID broadcastCallMethod(
            ConnectorListener connListener,
            String networkId,
            Class deviceIface, 
            String methodId, 
            Object[] args
    ) {
        return broadcastCallMethod(connListener, networkId, deviceIface, 
                methodId, args, responseTimeout
        );
    }
    
    
    // ASYNCHRONOUS MESSAGES GENERATOR INTERFACE
    
    @Override
    public void registerListener(
            AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage> listener
    ) {
        asyncMsgGenerator.registerListener(listener);
    }

    @Override
    public void unregisterListener(
            AsynchronousMessagesGeneratorListener<DPA_AsynchronousMessage> listener
    ) {
        asyncMsgGenerator.unregisterListener(listener); 
    }
    
    
    @Override
    public void start() throws SimplyException {
        logger.debug("startMessaging - start:");
        
        callResultsSender.start();
        workerThread.start();
        
        // register this connector as a listener of messages from protocol layer 
        this.protocolLayerService.registerListener(this);
        
        logger.info("Messaging started");
        logger.debug("startMessaging - end");
    }
    
     /**
     * Terminates worker thread.
     */
    private void terminateWorkedThread() {
        logger.debug("stopMessaging - start:");
        
        // termination signal to worker thread
        workerThread.interrupt();
        
        // Waiting for threads to terminate. Cancelling worker threads has higher 
        // priority than main thread interruption. 
        while ( workerThread.isAlive() ) {
            try {
                if ( workerThread.isAlive() ) {
                    workerThread.join();
                }
            } catch ( InterruptedException e ) {
                // restoring interrupt status
                Thread.currentThread().interrupt();
                logger.warn("Stop messaging - connector interrupted");
            }
        } 
        
        logger.info("Messaging stopped.");
        logger.debug("stopMessaging - end");
    }
    
    /**
     * Destroys this connector and frees all used resources.
     */
    @Override
    public void destroy() {
        logger.debug("destroy - start:");
        
        protocolLayerService.unregisterListener();
        terminateWorkedThread();
        callResultsSender.destroy();
        protocolLayerService = null;
        asyncMsgGenerator = null;
        
        logger.info("Destroyed.");
        logger.debug("destroy - end");
    }
    
    @Override
    public void onGetMessage(AbstractMessage message) {
        logger.debug("onGetMessage - start: data={}", message);
        
        synchronized ( syncRequestOrMessage ) {
            if ( message instanceof BaseAsynchronousMessage ) {
                if ( message instanceof DPA_AsynchronousMessage ) {
                    synchronized( syncAsyncMsgfromProtoLayer ) {
                        asyncMsgFromProtoLayer.offer((DPA_AsynchronousMessage) message);
                        syncAsyncMsgfromProtoLayer.notifyAll();
                    }
                } else {
                    logger.warn(
                        "Incomming asynchronous message is not of DPA_AsynchronousMessage "
                        + "type. It will be discared", message
                    );
                }
            } else {
                synchronized( syncMsgfromProtoLayer ) {
                    msgFromProtoLayer.offer(message);
                    syncMsgfromProtoLayer.notifyAll();
                }   
            }
            syncRequestOrMessage.notifyAll();
        }
        
        logger.info("New message from protocol layer get: {}", message);
        logger.debug("onGetMessage - end");
     }
    
    
    
    private static long checkMaxCallRequestIdleTime(long idleTime) {
        if ( idleTime < 0 ) {
            throw new IllegalArgumentException(
                    "Maximal call request idle time must be nonnegative"
            );
        }
        return idleTime;
    }
    
    /**
     * @param idleTime maximal time period, during which requests can be idle
     * @throws IllegalArgumentException if {@code idleTime} is less than or 
     *         equal to 0
     */
    @Override
    public void setMaxCallRequestIdleTime(long idleTime) {
        maxCallRequestIdleTime = checkMaxCallRequestIdleTime(idleTime);
    }
    
    @Override
    public long getMaxCallRequestIdleTime() {
        return maxCallRequestIdleTime;
    }
    
    
    /**
     * Returns number of maximal attempts of sending request to underlaying 
     * network.
     * @return number of maximal attempts of sending request to underlaying network.
     */
    @Override
    public int getMaxSendAttempts() {
        return maxSendAttempts;
    }
    
    
    private static int checkMaxSendAttempts(int maxSendAttempts) {
        if ( maxSendAttempts <= 0 ) {
            throw new IllegalArgumentException(
                    "Number of maximal attempts of sending request to underlaying "
                            + "network must be greather then 0"
            );
        }
        return maxSendAttempts;
    }
    
    /**
     * Sets number of maximal attempts of sending request to underlaying network.
     * @param maxSendAttempts number of maximal attempts of sending request to 
     *                        underlaying network. Must be greater than 0.
     * @throws IllegalArgumentException if {@code maxSendAttempts} is less than or 
     *         equal to 0
     */
    @Override
    public void setMaxSendAttempts(int maxSendAttempts) {
        this.maxSendAttempts = checkMaxSendAttempts(maxSendAttempts);
    }

    /**
     * Returns pause between subsequent attempts to send request to underlaying 
     * network.
     * @return Pause between subsequent attempts to send request to underlaying 
     *         network [in miliseconds].
     */
    @Override
    public long getAttemptPause() {
        return attemptPause;
    }

    
    private static long checkAttemptPause(long attemptPause) {
        if ( attemptPause <= 0 ) {
            throw new IllegalArgumentException(
                    "Pause between subsequent attempts to send request to underlaying "
                            + "network must be greather then 0"
            );
        }
        return attemptPause;
    }
    
    /**
     * Sets pause between subsequent attempts to send request to underlaying 
     * network.
     * @param attemptPause pause between subsequent attempts to send request to 
     *                underlaying network [in miliseconds]. Must be greater than 0.
     * @throws IllegalArgumentException if {@code attemptPause} is less than or 
     *         equal to 0
     */
    @Override
    public void setAttemptPause(long attemptPause) {
        this.attemptPause = checkAttemptPause(attemptPause);
    }

    /**
     * Returns timeout for waiting for a response from underlaying network.
     * @return timeout for waiting for a response from underlaying network 
     *         [in miliseconds].
     */
    @Override
    public long getResponseTimeout() {
        return responseTimeout;
    }
    
    private static long checkResponseTimeout(long responseTimeout) {
        if ( responseTimeout <= 0 ) {
            throw new IllegalArgumentException("Reponse timeout must be greather then 0");
        }
        return responseTimeout;
    }
    
    /**
     * Sets timeout for waiting for a response from underlaying network.
     * @param responseTimeout timeout for waiting for a response from underlaying 
     *                        network [in miliseconds]. Must be greater than 0
     * @throws IllegalArgumentException if {@code responseTimeout} is less than or 
     *         equal to 0
     */
    @Override
    public void setResponseTimeout(long responseTimeout) {
        this.responseTimeout = checkResponseTimeout(responseTimeout);
    }
    
    /**
     * Returns minimal pause between sending requests.
     * @return Minimal pause between sending requests [in miliseconds].
     */
    @Override
    public long getBetweenSendPause() {
        return betweenSendPause;
    }
    
    
    private static long checkBetweenSendPause(long betweenSendPause) {
        if ( betweenSendPause <= 0 ) {
            throw new IllegalArgumentException(
                    "Minimal pause between sending requests to underlaying "
                            + "network must be greather then 0"
            );
        }
        return betweenSendPause;
    }
    
    /**
     * Sets minimal pause between sending requests.
     * @param betweenSendPause minimal pause between sending requests [in miliseconds].
     *        Must be greater than 0.
     * @throws IllegalArgumentException if {@code betweenSendPause} is less than or 
     *         equal to 0
     */
    @Override
    public void setBetweenSendPause(long betweenSendPause) {
        this.betweenSendPause = checkBetweenSendPause(betweenSendPause);
    }
}
