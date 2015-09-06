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
package com.microrisc.simply.iqrf.dpa.v220.examples.user_peripherals.mycustom;

import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.Simply;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.iqrf.dpa.v220.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v220.examples.user_peripherals.mycustom.def.MyCustom;
import java.io.File;
import java.util.Arrays;

/**
 * Example of MyCustom peripheral. Id of custom peripheral is defined in
 * {@link MyCustom.USER_PERIPHERAL_ID}.
 * <p>
 * For using MyCustom peripheral is need specify peripheral number (same as in MyCustom) in Peripheral
 * distribution file and add next lines to Simply configuration:
 * dpa.perToDevIfaceMapper.factory.class = com.microrisc.simply.iqrf.dpa.v220.examples.user_peripherals.mycustom.def.UserPerToDevIfaceMapperFactory
 * protocolLayer.protocolMapping.factory.class = com.microrisc.simply.iqrf.dpa.v220.examples.user_peripherals.mycustom.def.UserProtocolMappingFactory
 * <p>
 * <p>
 * @author Martin Strouhal
 */
public class MyCustomExample {

    // reference to Simply
    private static Simply simply = null;

    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        if (simply != null) {
            simply.destroy();
        }
        System.exit(1);
    }

    public static void main(String[] args) {
        // get simply
        try {
            simply = DPA_SimplyFactory.getSimply(
                    "config" + File.separator + "Simply.properties"
            );
        } catch (SimplyException ex) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }

        // get network "1"
        Network network1 = simply.getNetwork("1", Network.class);
        if (network1 == null) {
            printMessageAndExit("Network 1 doesn't exist");
        }

        // get node "1"
        Node node = network1.getNode("1");
        if (node == null) {
            printMessageAndExit("Node 1 doesn't exist");
        }

        // getting MyCustom device
        MyCustom custom = node.getDeviceObject(MyCustom.class);
        if (custom == null) {
            printMessageAndExit("Error when MyCustom was getting from node.");
        }

        // sending command and getting result
        Short[] result = custom.send((short) 0x00, new short[]{});

        // printing result
        System.out.println(Arrays.toString(result));

        simply.destroy();
    }
}
