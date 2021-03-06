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

package com.microrisc.simply.iqrf.dpa.v21x.examples.std_per.coordinator;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.Simply;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v21x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v21x.devices.Coordinator;
import com.microrisc.simply.iqrf.dpa.v21x.types.DiscoveryParams;
import com.microrisc.simply.iqrf.dpa.v21x.types.DiscoveryResult;
import java.io.File;

/**
 * Examples of using Coordinator peripheral - synchronous version.
 * 
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public class RunDiscovery {
    // reference to Simply
    private static Simply simply = null;
    
    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        if ( simply != null) {
            simply.destroy();
        }
        System.exit(1);
    }
    
    public static void main(String[] args) {
        // creating Simply instance
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "Simply.properties");
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }
        
        // getting network 1
        Network network1 = simply.getNetwork("1", Network.class);
        if ( network1 == null ) {
            printMessageAndExit("Network 1 doesn't exist");
        }
        
        // getting a master node
        Node master = network1.getNode("0");
        if ( master == null ) {
            printMessageAndExit("Master doesn't exist");
        }
        
        // getting Coordinator interface
        Coordinator coordinator = master.getDeviceObject(Coordinator.class);
        if ( coordinator == null ) {
            printMessageAndExit("Coordinator doesn't exist or is not enabled");
        }
        
        // Setting DO waiting timeout to 11s 
        coordinator.setDefaultWaitingTimeout(11000);
       
        // setting params for discovery process
        // 6 as txPower, recomended is max-1
        // 0 zones means number of bonded devices
        DiscoveryParams discoParams = new DiscoveryParams(6, 0);
        DiscoveryResult discoResult = coordinator.runDiscovery(discoParams);
        if ( discoResult == null ) {            
            CallRequestProcessingState procState = coordinator.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                CallRequestProcessingError error = coordinator.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Discovery result failed: " + error);
            } else {
                printMessageAndExit("Discovery result hasn't been processed yet: " + procState);
            }
        }
        
        // number of discovered devices
        System.out.println("Discovery result: " + discoResult.getDiscoveredNodesNum());
        
        // end working with Simply
        simply.destroy();
    }
}
