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

package com.microrisc.simply.iqrf.dpa.v21x.types;

/**
 * Encapsulates information for putting device into sleep mode.
 * 
 * @author Michal Konopa
 */
public final class SleepInfo {
    /** 
     * Sleep time in 2.097s (i.e. 2048 * 1.024 ms) units. 0 specifies endless 
     * sleep (except Control.bit1 is set to run calibration process without 
     * performing sleep). Maximum sleep time is 38 hours 10 minutes 38.95
     * seconds. 
     */
    private final int time;
    
    /** Time lower bound. */
    public static final int TIME_LOWER_BOUND = 0x00;
    
    /** Time upper bound. */
    public static final int TIME_UPPER_BOUND = 0xFFFF;
    
    private static int checkTime(int time) {
        if ( (time < TIME_LOWER_BOUND) || (time > TIME_UPPER_BOUND) ) {
            throw new IllegalArgumentException("Time out of bounds");
        }
        return time;
    }
    
    /** 
     * Control	
     * bit 0 - wake up on PIN change.
     * bit 1 - runs calibration process before going to sleep.
     * bit 2 - if set, then when the device wakes up after the sleep period, 
     *          a green LED once shortly flashes.
     */
    private final int control;
    
    /** Control lower bound. */
    public static final int CONTROL_LOWER_BOUND = 0x00;
    
    /** Control upper bound. */
    public static final int CONTROL_UPPER_BOUND = 0xFF;
    
    private static int checkControl(int control) {
        if ( (control < CONTROL_LOWER_BOUND) || (control > CONTROL_UPPER_BOUND) ) {
            throw new IllegalArgumentException("Control out of bounds");
        }
        return control;
    }
    
    /**
     * Control bits.
     */
    public static enum ControlBits {
        WAKEUP_ON_PORTB4_NEGATIVE_EDGE_CHANGE   (0b0001),
        RUN_CALIBRATION_BEFORE_SLEEP            (0b0010),
        GREEN_LED_FLASH_AFTER_SLEEP             (0b0100),
        WAKEUP_ON_PORTB4_POSITIVE_EDGE_CHANGE   (0b1000);
        
        private final int bitsValue;
        
        ControlBits(int bitsValue) {
            this.bitsValue = bitsValue;
        }
        
        /**
         * @return integer value of control bits 
         */
        public int getIntValue() {
            return this.bitsValue;
        }
    }
    
    
    /**
     * Creates new {@code SleepInfo} object.
     * @param time Sleep time in 2.097s (i.e. 2048 * 1.024 ms) units. 
     *             0 specifies endless sleep (except Control.bit1 is set to run 
     *             calibration process without performing sleep). Maximum sleep 
     *             time is 38 hours 10 minutes 38.95 seconds.
     * @param control 
     *        bit 0 - Wake up on PORTB.4 pin negative edge change. See iqrfSleep() 
     *                method for more information. <br>
     *        bit 1 - Runs calibration process before going to sleep. Calibration 
     *                time takes approximately 132 ms and it is subtracted from the 
     *                requested sleep time. Calibration time deviation may produce 
     *                an absolute sleep time error at short sleep times. But it is worth 
     *                to run the calibration always before a longer sleep because the 
     *                calibration time deviation then accounts for a very small total 
     *                relative error. The calibration is always run before a first sleep 
     *                with nonzero Time after the module reset if calibration was not 
     *                already initiated by Time=0 and Control.bit1=1.<br>
     *        bit 2 - if set, then when the device wakes up after the sleep period, 
     *                a green LED once shortly flashes. It is useful for diagnostic 
     *                purposes. <br>
     *        bit 3 -  Wake up on PORTB.4 pin positive edge change. See iqrfSleep() 
     *                 method for more information.
     * @throws IllegalArgumentException if: <br> 
     *         specified sleep time is out of [{@code TIME_LOWER_BOUND}..{@code TIME_UPPER_BOUND}] interval <br> 
     *         specified control is out of [{@code CONTROL_LOWER_BOUND}..{@code CONTROL_UPPER_BOUND}] interval
     */
    public SleepInfo(int time, int control) {
        this.time = checkTime(time);
        this.control = checkControl(control);
    }

    /**
     * @return the time
     */
    public int getTime() {
        return time;
    }

    /**
     * @return the control
     */
    public int getControl() {
        return control;
    }
    
    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append(this.getClass().getSimpleName() + " { " + NEW_LINE);
        strBuilder.append(" Time: " + time + NEW_LINE);
        strBuilder.append(" Control bit: " + control + NEW_LINE);
        strBuilder.append("}");
        
        return strBuilder.toString();
    }
}
