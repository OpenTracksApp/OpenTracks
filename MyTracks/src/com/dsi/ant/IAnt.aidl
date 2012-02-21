/*
 * Copyright 2010 Dynastream Innovations Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.dsi.ant;

/**
 * The Android Ant API is not finalized, and *will* be updated and expanded.
 * This is the base level ANT messaging API and gives any application full
 * control over the ANT radio HW.  Caution should be exercised when using
 * this interface.
 *
 * Public API for controlling the Ant Service.
 *
 * {@hide}
 */
interface IAnt {
// Since version 1 (1.0):
    boolean enable();
    boolean disable();
    boolean isEnabled();    

    boolean ANTTxMessage(in byte[] message);

    boolean ANTResetSystem();
    boolean ANTUnassignChannel(byte channelNumber);
    boolean ANTAssignChannel(byte channelNumber, byte channelType, byte networkNumber);
    boolean ANTSetChannelId(byte channelNumber, int deviceNumber, byte deviceType, byte txType); 
    boolean ANTSetChannelPeriod(byte channelNumber, int channelPeriod);
    boolean ANTSetChannelRFFreq(byte channelNumber, byte radioFrequency);
    boolean ANTSetChannelSearchTimeout(byte channelNumber, byte searchTimeout);
    boolean ANTSetLowPriorityChannelSearchTimeout(byte channelNumber, byte searchTimeout); 
    boolean ANTSetProximitySearch(byte channelNumber, byte searchThreshold);
    boolean ANTSetChannelTxPower(byte channelNumber, byte txPower);
    boolean ANTAddChannelId(byte channelNumber, int deviceNumber, byte deviceType, byte txType, byte listIndex); 
    boolean ANTConfigList(byte channelNumber, byte listSize, byte exclude);
    
    boolean ANTOpenChannel(byte channelNumber);
    boolean ANTCloseChannel(byte channelNumber);
    
    boolean ANTRequestMessage(byte channelNumber, byte messageID);
    boolean ANTSendBroadcastData(byte channelNumber, in byte[] txBuffer);
    boolean ANTSendAcknowledgedData(byte channelNumber, in byte[] txBuffer); 

    boolean ANTSendBurstTransferPacket(byte control, in byte[] txBuffer);

    int ANTSendBurstTransfer(byte channelNumber, in byte[] txBuffer);
    int ANTTransmitBurst(byte channelNumber, in byte[] txBuffer, int initialPacket, boolean containsEndOfBurst);

// Since version 4 (1.3):
    boolean ANTConfigEventBuffering(int screenOnFlushTimerInterval, int screenOnFlushBufferThreshold, int screenOffFlushTimerInterval, int screenOffFlushBufferThreshold);
    
// Since version 2 (1.1):
    boolean ANTDisableEventBuffering();
    
// Since version 3 (1.2):
    int getServiceLibraryVersionCode();
    String getServiceLibraryVersionName();
}
