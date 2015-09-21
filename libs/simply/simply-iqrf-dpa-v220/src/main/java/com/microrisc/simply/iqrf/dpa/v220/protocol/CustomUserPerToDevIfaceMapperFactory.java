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
package com.microrisc.simply.iqrf.dpa.v220.protocol;

import com.microrisc.simply.iqrf.dpa.v220.devices.MyCustom;
import com.microrisc.simply.iqrf.dpa.protocol.PeripheralToDevIfaceMapper;
import com.microrisc.simply.iqrf.dpa.protocol.PeripheralToDevIfaceMapperFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.configuration.Configuration;

/**
 * User peripheral to Device Interfaces mapper.
 * <p>
 * @author Michal Konopa
 * @author Martin Strouhal
 */
// September 2015 - for Custom peripheral implemented advanced mapping
// of multiple interfaces
public class CustomUserPerToDevIfaceMapperFactory
        implements PeripheralToDevIfaceMapperFactory {

    /**
     * Holds mapping between my peripherals and Device Interfaces.
     */
    private class UserPerToDevIfaceMapper implements PeripheralToDevIfaceMapper {

        private final Map<Integer, Class> peripheralToIface;
        private final Map<Class, Integer> ifaceToPeripheral;

        private void createMappings(Configuration config) {
            //TODO zpracovat config
            
            for (int i = DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START;
                    i <= DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_END; i++) {
                peripheralToIface.put(i, MyCustom.class);
            }

            // creating transposition
            for (Map.Entry<Integer, Class> entry : peripheralToIface.entrySet()) {
                ifaceToPeripheral.put(entry.getValue(), entry.getKey());
            }
        }

        /**
         * Create a new instance of {@link UserPerToDevIfaceMapper} with
         * peripherals used in specified config.
         * <p>
         * @param config must contain used perpiherals which will be mapped
         */
        public UserPerToDevIfaceMapper(Configuration config) {
            peripheralToIface = new HashMap<>();
            ifaceToPeripheral = new HashMap<>();
            createMappings(config);
        }

        @Override
        public Set<Class> getMappedDeviceInterfaces() {
            return ifaceToPeripheral.keySet();
        }

        @Override
        public Class getDeviceInterface(int perId) {
            return peripheralToIface.get(perId);
        }

        @Override
        public Integer getPeripheralId(Class devInterface) {
            return ifaceToPeripheral.get(devInterface);
        }

        @Override
        public Set<Integer> getMappedPeripherals() {
            return peripheralToIface.keySet();
        }
    }

    private Configuration config;

    public void setInitConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public PeripheralToDevIfaceMapper createPeripheralToDevIfaceMapper() throws Exception {
        if (config == null) {
            throw new Exception("In implementation CustomPerToDevIfaceMapperFactory must be called setInitConfig method first!");
        }
        return new UserPerToDevIfaceMapper(config);
    }
}