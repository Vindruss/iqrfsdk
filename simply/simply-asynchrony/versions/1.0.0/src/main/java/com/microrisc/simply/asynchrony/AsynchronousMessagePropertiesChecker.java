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

package com.microrisc.simply.asynchrony;

/**
 * Checker of asynchronous message properties.
 * 
 * @param <T> type of asynchronous message
 * @param <V> type of required properties of asynchronous messages
 * 
 * @author Michal Konopa
 */
public interface AsynchronousMessagePropertiesChecker
<T extends BaseAsynchronousMessage, V extends AsynchronousMessageProperties>
{
    /**
     * Returns {@code true} if the specified message has specified properties.
     * Otherwise returns {@code false}.
     * @param message message to check for specified properties
     * @param reqProps required properties of the message
     * @return {@code true} if the specified message has specified properties <br>
     *         {@code false}, otherwise 
     */
    boolean messageHasRequiredProperties(T message, V reqProps);
}
