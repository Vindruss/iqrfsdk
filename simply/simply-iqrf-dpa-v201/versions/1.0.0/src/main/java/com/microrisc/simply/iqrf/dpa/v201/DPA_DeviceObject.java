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

package com.microrisc.simply.iqrf.dpa.v201;

import com.microrisc.simply.StandardServicesDeviceObject;
import com.microrisc.simply.ConnectorService;
import com.microrisc.simply.CallRequestProcessingInfoContainer;
import com.microrisc.simply.iqrf.dpa.v201.di_services.DPA_StandardServices;
import com.microrisc.simply.iqrf.dpa.v201.types.DPA_AdditionalInfo;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device Object for DPA specific needs.
 * 
 * @author Michal Konopa
 */
public class DPA_DeviceObject 
extends StandardServicesDeviceObject 
implements DPA_StandardServices {
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPA_DeviceObject.class);
    
    /** Default HW profile. */
    public static int DEFAULT_HW_PROFILE = 0xFFFF;
    
    /** HW profile. */
    private int hwProfile = DEFAULT_HW_PROFILE;
    
    
    private int checkHwProfile( int hwProfile ) {
        if ( (hwProfile < 0x0000) || (hwProfile > 0xFFFF) ) {
            throw new IllegalArgumentException("Invalid value of HW profile: " + hwProfile);
        }
        return hwProfile;
    }
    
    
    public DPA_DeviceObject(String networkId, String nodeId, ConnectorService connector, 
            CallRequestProcessingInfoContainer resultsContainer
    ) {
        super(networkId, nodeId, connector, resultsContainer);
    }
    
    @Override
    public void setRequestHwProfile(int hwProfile) {
        this.hwProfile = checkHwProfile( hwProfile );
    }
    
    @Override
    public int getRequestHwProfile() {
        return hwProfile;
    }
    
    
    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfo(UUID callId) {
        logger.debug("{}getDPA_AdditionalInfo - start: callId={}", logPrefix, callId);
        
        Object addInfo = getCallResultAdditionalInfo(callId);
        if ( addInfo == null ) {
            logger.debug("{}getDPA_AdditionalInfo - end: null", logPrefix);
            return null;
        }
        
        if ( !(addInfo instanceof DPA_AdditionalInfo) ) {
            throw new IllegalStateException("Wrong additional info type.");
        }
        
        DPA_AdditionalInfo dpaAddInfo = (DPA_AdditionalInfo) addInfo;
        
        logger.debug("{}getDPA_AdditionalInfo - end: {}", logPrefix, dpaAddInfo);
        return dpaAddInfo;
    }

    @Override
    public DPA_AdditionalInfo getDPA_AdditionalInfoOfLastCall() {
        logger.debug("{}getDPA_AdditionalInfoOfLastCall - start: ", logPrefix);
        
        Object addInfo = getCallResultAdditionalInfoOfLastCall();
        if ( addInfo == null ) {
            logger.debug("{}getDPA_AdditionalInfoOfLastCall - end: null", logPrefix);
            return null;
        }
        
        if ( !(addInfo instanceof DPA_AdditionalInfo) ) {
            throw new IllegalStateException("Wrong additional info type.");
        }
        
        DPA_AdditionalInfo dpaAddInfo = (DPA_AdditionalInfo) addInfo;
        
        logger.debug("{}getDPA_AdditionalInfoOfLastCall - end: {}", logPrefix, dpaAddInfo);
        return dpaAddInfo;
    }

}
