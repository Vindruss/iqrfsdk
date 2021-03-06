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

package com.microrisc.cdc;

/**
 * Send command exceptions for J_CDCImpl class. This exceptions occur when
 * some error has occured during command send to USB device.
 * <p>
 * Peer class for CDCSendException class.
 * 
 * @version     1.0
 */
public class J_CDCSendException extends J_CDCImplException {
    /**
     * Creates exception object.
     * @param msg desctiption of exception
     */
    public J_CDCSendException(String msg) {
       super(msg); 
    }
}
