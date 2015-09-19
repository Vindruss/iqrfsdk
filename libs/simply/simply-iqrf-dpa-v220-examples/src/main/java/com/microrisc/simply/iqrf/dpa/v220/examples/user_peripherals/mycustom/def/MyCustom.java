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

package com.microrisc.simply.iqrf.dpa.v220.examples.user_peripherals.mycustom.def;

import com.microrisc.simply.DeviceInterface;
import com.microrisc.simply.DeviceInterfaceMethodId;
import com.microrisc.simply.di_services.GenericAsyncCallable;
import com.microrisc.simply.di_services.MethodIdTransformer;
import com.microrisc.simply.iqrf.dpa.di_services.DPA_StandardServices;

/**
 *  MyDallas18B20 Device Interface.
 * 
 * @author Martin Strouhal
 */
@DeviceInterface
public interface MyCustom 
extends DPA_StandardServices, GenericAsyncCallable, MethodIdTransformer {

    public final int USER_PERIPHERAL_ID = 0x20;
    
    /**
     * Identifiers of this Device Interface's methods.
     */
    enum MethodID implements DeviceInterfaceMethodId {
        SEND
    }

    /**
     * Send custom data to user periheral.
     *
     * @param cmdId id of command
     * @param data to send
     * @return response data <br> 
     *         {@code null} if an error has occurred during processing
     */
    Short[] send(short cmdId, short[] data);
}