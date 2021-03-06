/* 
 * Copyright 2014 MICRORISC s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microrisc.simply.iqrf.dpa.v21x.devices.impl;

import com.microrisc.simply.CallRequestProcessingInfoContainer;
import com.microrisc.simply.ConnectorService;
import com.microrisc.simply.di_services.MethodArgumentsChecker;
import com.microrisc.simply.iqrf.dpa.v21x.DPA_DeviceObject;
import com.microrisc.simply.iqrf.dpa.v21x.devices.PWM;
import com.microrisc.simply.iqrf.dpa.v21x.di_services.method_id_transformers.PWMStandardTransformer;
import com.microrisc.simply.iqrf.dpa.v21x.types.PWM_Parameters;
import com.microrisc.simply.iqrf.types.VoidType;
import java.util.UUID;

/**
 * Simple PWM implementation.
 * 
 * @author Michal Konopa
 */
public final class SimplePWM 
extends DPA_DeviceObject implements PWM {
    
    public SimplePWM(String networkId, String nodeId, ConnectorService connector, 
            CallRequestProcessingInfoContainer resultsContainer
    ) {
        super(networkId, nodeId, connector, resultsContainer);
    }
    
    @Override
    public UUID call(Object methodId, Object[] args) {
        String methodIdStr = transform((PWM.MethodID) methodId);
        if ( methodIdStr == null ) {
            return null;
        }
        
        switch ( (PWM.MethodID)methodId ) {
            case SET:
                MethodArgumentsChecker.checkArgumentTypes(args, new Class[] { PWM_Parameters.class } );
                return dispatchCall(
                        methodIdStr, 
                        new Object[] { getRequestHwProfile(), (PWM_Parameters)args[0] },
                        getDefaultWaitingTimeout()
                );
            default:
                throw new IllegalArgumentException("Unsupported command: " + methodId);
        }
    }
    
    @Override
    public String transform(Object methodId) {
        return PWMStandardTransformer.getInstance().transform(methodId);
    }
    
    
    
    // ASYNCHRONOUS METHODS IMPLEMENTATIONS
    
    @Override
    public UUID async_set(PWM_Parameters param) {
        return dispatchCall(
                "1", new Object[] { getRequestHwProfile(), param }, getDefaultWaitingTimeout() 
        );
    }
    
    
    // SYNCHRONOUS WRAPPERS IMPLEMENTATIONS
    
    @Override
    public VoidType set(PWM_Parameters param) {
        UUID uid = dispatchCall(
                "1", new Object[] { getRequestHwProfile(), param }, getDefaultWaitingTimeout() 
        );
        if ( uid == null ) {
            return null;
        }
        return getCallResult(uid, VoidType.class, getDefaultWaitingTimeout() );
    }
}
