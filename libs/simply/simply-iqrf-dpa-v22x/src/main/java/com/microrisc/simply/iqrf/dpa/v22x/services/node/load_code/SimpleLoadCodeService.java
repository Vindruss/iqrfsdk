/*
 * Copyright 2016 MICRORISC s.r.o.
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
package com.microrisc.simply.iqrf.dpa.v22x.services.node.load_code;

import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.devices.EEEPROM;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.di_services.DPA_StandardServices;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_Request;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingCodeProperties;
import com.microrisc.simply.iqrf.dpa.v22x.types.LoadingResult;
import com.microrisc.simply.iqrf.types.VoidType;
import com.microrisc.simply.services.BaseServiceResult;
import com.microrisc.simply.services.ServiceParameters;
import com.microrisc.simply.services.ServiceResult;
import com.microrisc.simply.services.node.BaseService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Simple Load Code Service implementation.
 * 
 * @author Michal Konopa
 */
public final class SimpleLoadCodeService 
extends BaseService implements LoadCodeService {

   private CodeBlock findHandlerBlock(IntelHex file) {
      Collections.sort(file.getCodeBlocks());

      List<CodeBlock> blocks = file.getCodeBlocks();

      for (CodeBlock block : blocks) {
         if (block != null && (file.getData().getShort((int) block.
                 getAddressStart()) == 0x6400)) {
            return block;
         }
      }

      terminateWithFail("Selected .hex files does not include Custom DPA "
              + "handler section or the code does not start with clrwdt() marker.");
      return null;
   }

   private int calculateChecksum(IntelHex file, CodeBlock handlerBlock,
           int checkSumInitialValue, int length) {
      int dataChecksum = checkSumInitialValue;
      // checksum for data
      for (long address = handlerBlock.getAddressStart();
              address < handlerBlock.getAddressStart() + length;
              address++) {
         int oneByte = file.getData().get((int) address) & 0xFF;
         if(handlerBlock.getAddressEnd() - address < 0){
            oneByte = 0x34FF;
         }
         
         // One’s Complement Fletcher Checksum
         int tempL = dataChecksum & 0xff;
         tempL += oneByte;
         if ((tempL & 0x100) != 0) {
            tempL++;
         }

         int tempH = dataChecksum >> 8;
         tempH += tempL & 0xff;
         if ((tempH & 0x100) != 0) {
            tempH++;
         }

         dataChecksum = (tempL & 0xff) | (tempH & 0xff) << 8;
      }
      return dataChecksum;
   }

   private ServiceResult<LoadingResult, LoadCodeProcessingInfo> 
        terminateWithFail(String failMsg){
      return new BaseServiceResult<LoadingResult, LoadCodeProcessingInfo>(
              ServiceResult.Status.ERROR,
              new LoadingResult(false), new LoadCodeProcessingInfo(failMsg));
   }
   
   private ServiceResult<LoadingResult, LoadCodeProcessingInfo> 
        terminateSuccessfully(LoadingResult result){
      return new BaseServiceResult<LoadingResult, LoadCodeProcessingInfo>(
              ServiceResult.Status.SUCCESSFULLY_COMPLETED,
              result, new LoadCodeProcessingInfo(result.toString()));
   }
        
   @Deprecated
   private String writeDataToMemory(CodeBlock handlerBlock, IntelHex file,
           LoadCodeServiceParameters params){
      EEEPROM eeeprom = getDeviceObject(EEEPROM.class);
      if (eeeprom == null) {
         return "EEEPROM doesn't exist or is not enabled";
      }
      
      // specify length of block, which will be written to EEEPROM
      int blockSize = 32;
      int blockStartAddress = params.getStartAddress();
      for (long address = handlerBlock.getAddressStart();
              address < handlerBlock.getAddressEnd();
              address += blockSize) 
      {
         // preparing code to write into EEEPROM
         short[] block = new short[blockSize];
         for (int i = 0; i < blockSize; i++) {
            block[i] = (short) ((file.getData().get((int) address + i)) & 0xFF);
            if (address + i > handlerBlock.getAddressEnd()) {
               block[i] = 0x34FF;
            }
         }
         System.out.println(Arrays.toString(block));

         // writing code to EEEPROM
         VoidType voidResult = eeeprom.extendedWrite(blockStartAddress, block);
         if (voidResult == null) {
            return "Writing to EEEPROM failed: Writing data hasn't been processed yet";
         }
         blockStartAddress += blockSize;
      }
      return null;
   }
   
   private void writeDataToMemory(int startAddress, short[][] data) {
      EEEPROM eeeprom = getDeviceObject(EEEPROM.class);
      if (eeeprom == null) {
         throw new RuntimeException("EEEPROM doesn't exist or is not enabled");
      }
      OS os = getDeviceObject(OS.class);
      if (os == null) {
         throw new RuntimeException("OS doesn't exist or is not enabled");
      }
      
      
      int actualAddress = startAddress;
      int index = 0;
      while(index < data.length){
         if(data[index].length == 48 && data[index+1].length == 16 ||
                 data[index].length == 16 && data[index+1].length == 48){
          
            DPA_Request firstReq = new DPA_Request(EEEPROM.class,
                    EEEPROM.MethodID.EXTENDED_WRITE,
                    new Object[]{actualAddress, data[index]},
                    DPA_ProtocolProperties.HWPID_Properties.DO_NOT_CHECK);
            actualAddress += data[index].length;

            DPA_Request secondReq = new DPA_Request(EEEPROM.class,
                    EEEPROM.MethodID.EXTENDED_WRITE,
                    new Object[]{actualAddress+data[index].length, data[index+1]},
                    DPA_ProtocolProperties.HWPID_Properties.DO_NOT_CHECK);
            actualAddress += data[index+1].length;
            
            VoidType result = os.batch(new DPA_Request[]{firstReq, secondReq});
            System.out.println("batch");
            checkResult(os, result);
            
            index+=2;
         }else{
            VoidType result = eeeprom.extendedWrite(actualAddress, data[index]);
            System.out.println("direct write extended");
            checkResult(eeeprom, result);
            actualAddress+=data[index].length;
            index++;
         }
      }
      
   }
   
   private void checkResult(DPA_StandardServices peripheral, VoidType result){
      if(result == null){
         CallRequestProcessingState procState = peripheral.getCallRequestProcessingStateOfLastCall();
        if ( procState == CallRequestProcessingState.ERROR ) {
            CallRequestProcessingError error = peripheral.getCallRequestProcessingErrorOfLastCall();
            throw new RuntimeException("Exception while writing data to memory " + error);
        } else {
            throw new RuntimeException("Exception while writing data to memory " + procState);
        }
      }
   }
   
    /**
     * Creates new Load Code Service object.
     * @param deviceObjects Device Objects to use
     */
    public SimpleLoadCodeService(Map<Class, DeviceObject> deviceObjects) {
        super(deviceObjects);
    }
    
    @Override
    public ServiceResult<LoadingResult, LoadCodeProcessingInfo> 
        loadCode(LoadCodeServiceParameters params) 
    {
      IntelHex file = new IntelHex(0xFFFFFF);
      try {
         /*file.parseIntelHex("D:\\Plocha\\IQRF_OS307_7xD\\Examples\\DPA\\"
                 + "CustomDpaHandlerExamples\\hex\\"
                 + "CustomDpaHandler-FRC-Minimalistic-7xD-V224-151201.hex");*/
         file.parseIntelHex("D:\\Plocha\\IQRF_OS308_7xD\\Examples\\DPA\\"
                 + "CustomDpaHandlerExamples\\hex\\"
                 + "CustomDpaHandler-UserPeripheral-ADC-7xD-V226-160303.hex");
      } catch (IOException ex) {
         return terminateWithFail(ex.getMessage());
      }

      // separating code block with custom DPA handler block
      CodeBlock handlerBlock = findHandlerBlock(file);      
      System.out.println("Handler block starts at " + handlerBlock.getAddressStart() 
              + " and ends at " + handlerBlock.getAddressEnd());

      // calcualting rounded length of handler in memory
      final int length = (int) ((handlerBlock.getLength() + (64 - 1)) & ~(64 - 1));

      // calculating checksum with initial value 1 (defined for DPA handler)
      int dataChecksum = calculateChecksum(file, handlerBlock, 1, length);
      System.out.println("Checksum of data is " + Integer.toHexString(dataChecksum));


      short[][] dataToWrite = new DataPreparer(handlerBlock, file, params).prepare();
      
       try{
         writeDataToMemory(params.getStartAddress(), dataToWrite);
       }catch(RuntimeException re){
          return terminateWithFail(re.getMessage());
       }
      
      // get access to OS peripheral
      OS os = getDeviceObject(OS.class);
      if (os == null) {
         return terminateWithFail("OS doesn't exist or is not enabled");
      }
      
      // set longer waiting timeout
      os.setDefaultWaitingTimeout(30000);            
      
      // loading code
      LoadingResult result = os.loadCode(new LoadingCodeProperties(
              params.getLoadingAction(),
              LoadingCodeProperties.LoadingContent.CustomDPAHandler,
              params.getStartAddress(), length, dataChecksum));

      if (result == null) {
         return terminateWithFail("Getting OS info failed: "
                 + "Getting OS info hasn't been processed yet");
      } else {
         System.out.println("Result of operation with Custom DPA handler: " + result);
         return terminateSuccessfully(result);
      }
    }

    @Override
    public void setServiceParameters(ServiceParameters params) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
