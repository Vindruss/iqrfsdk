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
package com.microrisc.simply.iqrf.dpa.v220.examples.user_peripherals.myadc.def;

import com.microrisc.simply.iqrf.dpa.v220.typeconvertors.DPA_AdditionalInfoConvertor;
import com.microrisc.simply.iqrf.typeconvertors.Uns16Convertor;
import com.microrisc.simply.protocol.mapping.CallRequestToPacketMapping;
import com.microrisc.simply.protocol.mapping.ConstValueToPacketMapping;
import com.microrisc.simply.protocol.mapping.InterfaceToPacketMapping;
import com.microrisc.simply.protocol.mapping.MethodToPacketMapping;
import com.microrisc.simply.protocol.mapping.PacketPositionValues;
import com.microrisc.simply.protocol.mapping.PacketToCallResponseMapping;
import com.microrisc.simply.protocol.mapping.PacketToInterfaceMapping;
import com.microrisc.simply.protocol.mapping.PacketToMethodMapping;
import com.microrisc.simply.protocol.mapping.PacketToValueMapping;
import com.microrisc.simply.protocol.mapping.ProtocolMapping;
import com.microrisc.simply.protocol.mapping.ProtocolMappingFactory;
import com.microrisc.simply.protocol.mapping.SimpleCallRequestToPacketMapping;
import com.microrisc.simply.protocol.mapping.SimplePacketToCallResponseMapping;
import com.microrisc.simply.protocol.mapping.SimpleProtocolMapping;
import com.microrisc.simply.protocol.mapping.ValueToPacketMapping;
import com.microrisc.simply.typeconvertors.StringToByteConvertor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Factory for protocol mapping of my periperals.
 * <br>
 * Warning note: This interface designed for CustomDpaHandler-UserPeripheral-ADC
 * has unsual style of using. One interface hasn't got few commands, but 2 id of
 * periheral for getting infomartion. This situation is fixed by switching
 * position of constants values.
 *
 * @author Michal Konopa
 * @author Martin Strouhal
 */
public class UserProtocolMappingFactory implements ProtocolMappingFactory {

    /**
     * Reference to protocol mapping.
     */
    private ProtocolMapping protocolMapping = null;

    // REQUEST MAPPING
    // returns currently empty list of mappings
    static private List<ConstValueToPacketMapping> createRequestConstMappings() {
        List<ConstValueToPacketMapping> mappings = new LinkedList<>();
        return mappings;
    }

    // returns empty list of mappings - more networks capability is not currently used
    static private List<ValueToPacketMapping> createRequestNetworkMappings() {
        List<ValueToPacketMapping> mappings = new LinkedList<>();
        return mappings;
    }

    static private List<ValueToPacketMapping> createRequestNodeMappings() {
        List<ValueToPacketMapping> mappings = new LinkedList<>();
        ValueToPacketMapping nodeMapping = new ValueToPacketMapping(0,
                StringToByteConvertor.getInstance()
        );
        mappings.add(nodeMapping);
        return mappings;
    }

    // ADC interface
    static private MethodToPacketMapping createGetValueMapping() {
        List<ConstValueToPacketMapping> constMapping = new LinkedList<>();
        // Warning note: See class doc
        constMapping.add(new ConstValueToPacketMapping(2, new short[]{0x20}));

        List<ValueToPacketMapping> argMapping = new LinkedList<>();
        argMapping.add(new ValueToPacketMapping(4, Uns16Convertor.getInstance()));

        return new MethodToPacketMapping(constMapping, argMapping);
    }

    static private MethodToPacketMapping createGet2ValueMapping() {
        List<ConstValueToPacketMapping> constMapping = new LinkedList<>();
        // Warning note: See class doc
        constMapping.add(new ConstValueToPacketMapping(2, new short[]{0x21}));

        List<ValueToPacketMapping> argMapping = new LinkedList<>();
        argMapping.add(new ValueToPacketMapping(4, Uns16Convertor.getInstance()));

        return new MethodToPacketMapping(constMapping, argMapping);
    }

    static private InterfaceToPacketMapping createRequestMyADCMapping() {
        List<ConstValueToPacketMapping> constMappings = new LinkedList<>();
        // Warning note: See class doc
        constMappings.add(new ConstValueToPacketMapping(3, new short[]{0x00}));

        Map<String, MethodToPacketMapping> methodMappings = new HashMap<>();

        methodMappings.put("0", createGetValueMapping());
        methodMappings.put("1", createGet2ValueMapping());

        return new InterfaceToPacketMapping(constMappings, methodMappings);
    }

    /**
     * Creates map of mapping of Device Interfaces into protocol packets.
     *
     * @return
     */
    static private Map<Class, InterfaceToPacketMapping> createRequestIfaceMappings() {
        Map<Class, InterfaceToPacketMapping> mappings = new HashMap<>();

        // creating interface mappings
        mappings.put(MyADC.class, createRequestMyADCMapping());
        return mappings;
    }

    static private CallRequestToPacketMapping createCallRequestToPacketMapping() {
        List<ConstValueToPacketMapping> constMappings = createRequestConstMappings();
        List<ValueToPacketMapping> networkMappings = createRequestNetworkMappings();
        List<ValueToPacketMapping> nodeMappings = createRequestNodeMappings();
        Map<Class, InterfaceToPacketMapping> ifaceMappings = createRequestIfaceMappings();

        return new SimpleCallRequestToPacketMapping(
                constMappings, networkMappings, nodeMappings, ifaceMappings
        );
    }

    // RESPONSES MAPPING
    static private PacketToValueMapping createResponseNetworkMapping() {
        return new PacketToValueMapping(0, 0, StringToByteConvertor.getInstance());
    }

    static private PacketToValueMapping createResponseNodeMapping() {
        return new PacketToValueMapping(0, 1, StringToByteConvertor.getInstance());
    }

    // ADC
    static private PacketToMethodMapping createResponseGetValue() {
        List<PacketPositionValues> packetValues = new LinkedList<>();
        // Warning note: See class doc
        packetValues.add(new PacketPositionValues(2, (short) 0x20));

        PacketToValueMapping resultMapping = new PacketToValueMapping(8, Uns16Convertor.getInstance());
        return new PacketToMethodMapping("0", packetValues, resultMapping);
    }

    static private PacketToMethodMapping createResponseGet2Value() {
        List<PacketPositionValues> packetValues = new LinkedList<>();
        // Warning note: See class doc
        packetValues.add(new PacketPositionValues(2, (short) 0x21));

        PacketToValueMapping resultMapping = new PacketToValueMapping(8, Uns16Convertor.getInstance());
        return new PacketToMethodMapping("1", packetValues, resultMapping);
    }

    static private PacketToInterfaceMapping createResponseMyADCMapping() {
        List<PacketPositionValues> packetValues = new LinkedList<>();
        // Warning note: See class doc
        packetValues.add(new PacketPositionValues(3, (short) 0x80));

        Map<String, PacketToMethodMapping> methodMappings = new HashMap<>();

        methodMappings.put("0", createResponseGetValue());
        methodMappings.put("1", createResponseGet2Value());

        return new PacketToInterfaceMapping(MyADC.class, packetValues, methodMappings);
    }

    // creating response mapping for Device Interfaces
    static private Map<Class, PacketToInterfaceMapping> createResponseIfaceMappings() {
        Map<Class, PacketToInterfaceMapping> mappings = new HashMap<>();

        // creating interface mappings
        mappings.put(MyADC.class, createResponseMyADCMapping());

        return mappings;
    }

    static private PacketToValueMapping createAdditionalDataMapping() {
        return new PacketToValueMapping(4, DPA_AdditionalInfoConvertor.getInstance());
    }

    static private PacketToCallResponseMapping createPacketToCallResponseMapping() {
        PacketToValueMapping networkMapping = createResponseNetworkMapping();
        PacketToValueMapping nodeMapping = createResponseNodeMapping();
        Map<Class, PacketToInterfaceMapping> ifaceMappings = createResponseIfaceMappings();
        PacketToValueMapping additionalDataMapping = createAdditionalDataMapping();

        return new SimplePacketToCallResponseMapping(
                networkMapping, nodeMapping, ifaceMappings, additionalDataMapping
        );
    }

    public UserProtocolMappingFactory() {
    }

    @Override
    public ProtocolMapping createProtocolMapping() throws Exception {
        if (protocolMapping != null) {
            return protocolMapping;
        }

        CallRequestToPacketMapping callRequestToPacketMapping
                = createCallRequestToPacketMapping();

        PacketToCallResponseMapping packetToCallResponseMapping
                = createPacketToCallResponseMapping();

        protocolMapping = new SimpleProtocolMapping(callRequestToPacketMapping,
                packetToCallResponseMapping
        );
        return protocolMapping;
    }
}
