/*
 * Copyright 2015 MICRORISC s.r.o.
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
package com.microrisc.simply.iqrf.dpa.v210.init;

import com.microrisc.simply.iqrf.RF_Mode;

/**
 * Netwrok configuration depending on specific network.
 * 
 * @author Martin Strouhal
 */
public final class SimpleDeterminetedNetworkConfig implements DeterminetedNetworkConfig{

    /** RF_Mode used in network */
    private final RF_Mode rfMode;

    private static RF_Mode checkRF_Mode(RF_Mode rfMode) {
        if ( rfMode == null ) {
            throw new IllegalArgumentException("RF mode must be defined.");
        }
        
        if ( rfMode == RF_Mode.XLP ) {
            throw new IllegalArgumentException("XLP mode is not currently supported.");
        }
        
        return rfMode;
    }
    
    public SimpleDeterminetedNetworkConfig(RF_Mode rfMode) {
        this.rfMode = checkRF_Mode(rfMode);
    }

    @Override
    public RF_Mode getRFMode() {
        return rfMode;
    }    

    @Override
    public String toString() {
        return "SimpleDeterminetedNetworkConfig{" + "rfMode=" + rfMode + '}';
    }
}
