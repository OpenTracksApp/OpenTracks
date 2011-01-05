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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


// TODO: Auto-generated Javadoc

/**
 * Public API for controlling the Ant Service. AntInterface is a proxy
 * object for controlling the Ant Service via IPC. Creating a AntInterface
 * object will create a binding with the Ant service.
 * 
 * @hide
 */
public class AntInterface {

    /** The Log Tag. */
    public static final String TAG = "ANTInterface";
    
    /** Enable debug logging. */
    public static boolean DEBUG = false;

    /** Search string to find ANT Radio Proxy Service in the Android Marketplace */
    private static final String MARKET_SEARCH_TEXT_DEFAULT = "ANT Radio Service Dynastream Innovations Inc";
    
    /** Inter-process communication with the ANT Radio Proxy Service. */
    public static IAnt sAntReceiver = null;
    public static IServiceSettings sServiceSettingsReceiver = null;
    
    /** Singleton instance of this class. */
    private static AntInterface INSTANCE;
    
    /** Used when obtaining a reference to the singleton instance. */
    private static Object INSTANCE_LOCK = new Object();
    
    /** The context to use. */
    private static Context sContext = null;
    
    /** Listens to changes to service connection status. */
    private static ServiceListener sServiceListener;
    
    /** Is the ANT Radio Proxy Service connected. */
    private static boolean sServiceConnected = false;
    private static boolean sServiceSettingsConnected = false;

    private static int mServiceLibraryVersionCode = 0;
    
    /**
     * An interface for notifying AntInterface IPC clients when they have
     * been connected to the ANT service.
     *
     * @see ServiceEvent
     */
     public interface ServiceListener 
     {
         /**
          * Called to notify the client when this proxy object has been
          * connected to the ANT service. Clients must wait for
          * this callback before making IPC calls on the ANT
          * service.
          */
         public void onServiceConnected();

         /**
          * Called to notify the client that this proxy object has been
          * disconnected from the ANT service. Clients must not
          * make IPC calls on the ANT service after this callback.
          * This callback will currently only occur if the application hosting
          * the BluetoothAg service, but may be called more often in future.
          */
         public void onServiceDisconnected();
     }

     
  //Constructor
    /**
     * Instantiates a new ant interface.
     *
     * @param context the context
     * @param listener the listener
     * @since 1.0
     */
    private AntInterface(Context context, ServiceListener listener)
    {
        // This will be around as long as this process is
        sContext = context;
        sServiceListener = listener;
    }

    /**
     * Gets the single instance of AntInterface, creating it if it doesn't exist.
     *
     * @param context the context.
     * @param listener the listener to be notified of status changes.
     * @return the AntInterface instance.
     * @since 1.0
     */
    public static AntInterface getInstance(Context context,ServiceListener listener) 
    {
        if(DEBUG)   Log.d(TAG, "getInstance");

        synchronized (INSTANCE_LOCK) 
        {
            if (INSTANCE == null) 
            {
                if(DEBUG)   Log.d(TAG, "getInstance: Creating new instance");
                
                // TODO: rohan: bug, each new request for an instance will not have context and listener set to the requested objects
                INSTANCE = new AntInterface(context,listener);
            }
            else
            {
                if(DEBUG)   Log.d(TAG, "getInstance: Using existing instance");
            }

            if(!sServiceConnected)
            {
                if(DEBUG)   Log.d(TAG, "getInstance: No connection to proxy service, attempting connection");

                if(!INSTANCE.initService())
                {
                    Log.e(TAG, "getInstance: No connection to proxy service");
                    
                    INSTANCE.destroy();
                    INSTANCE = null;
                }
            }

            return INSTANCE;
        }
    }

    /**
     * Go to market.
     *
     * @param pContext the context
     * @param pSearchText the search text
     * @since 1.2
     */
    public static void goToMarket(Context pContext, String pSearchText)
    {
        if(null == pSearchText)
        {
            goToMarket(pContext);
        }
        else
        {
            if(DEBUG) Log.i(TAG, "goToMarket: Search text = "+ pSearchText);

            Intent goToMarket = null;
            goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse("http://market.android.com/search?q=" + pSearchText));
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            pContext.startActivity(goToMarket);
        }
    }

    /**
     * Go to market.
     *
     * @param pContext the context
     * @since 1.2
     */
    public static void goToMarket(Context pContext)
    {
        if(DEBUG) Log.d(TAG, "goToMarket");
        
        goToMarket(pContext, MARKET_SEARCH_TEXT_DEFAULT);
    }
    
    /**
     * Class for interacting with the ANT interface.
     */
    private static ServiceConnection sIAntConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName pClassName, IBinder pService) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            if(DEBUG)   Log.d(TAG, "sIAntConnection onServiceConnected()");
            sAntReceiver = IAnt.Stub.asInterface(pService);

            sServiceConnected = true;

            try
            {
                mServiceLibraryVersionCode = sAntReceiver.getServiceLibraryVersionCode();
            }
            catch(RemoteException e)
            {
                Log.e(TAG, "sIAntConnection onServiceConnected: Could not get service library version");
            }

            switch(mServiceLibraryVersionCode)
            {
                // Fall through from newer versions
                case 5:
                {
                    if(!sServiceSettingsConnected)
                    {
                        boolean boundServiceSettings = sContext.bindService(new Intent(IServiceSettings.class.getName()), sIServiceSettingsConnection, Context.BIND_AUTO_CREATE);
                        Log.i(TAG, "sIAntConnection onServiceConnected: Bound with ANT Service Settings: " + boundServiceSettings);
                    }
                    else
                    {
                        if(DEBUG)   Log.d(TAG, "sIAntConnection onServiceConnected: Already initialised Service Settings connection");
                    }
                }
            }
            
            // Notify the attached application if it is registered
            if (sServiceListener != null) 
            {
                sServiceListener.onServiceConnected();
            }
            else
            {
                if(DEBUG) Log.d(TAG, "sIAntConnection onServiceConnected: No service listener registered");
            }
        }

        public void onServiceDisconnected(ComponentName pClassName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            if(DEBUG)   Log.d(TAG, "sIAntConnection onServiceDisconnected()");
            sAntReceiver = null;

            sServiceConnected = false;

            // Notify the attached application if it is registered
            if (sServiceListener != null) 
            {
                sServiceListener.onServiceDisconnected();
            }
            else
            {
                if(DEBUG) Log.d(TAG, "sIAntConnection onServiceDisconnected: No service listener registered");
            }

            // Try and rebind to the service
            INSTANCE.releaseService();
            INSTANCE.initService();
        }
    };

    /**
     * Class for interacting with the ANT interface through the ANTLib version 5 functions.
     */
    private static ServiceConnection sIServiceSettingsConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName pClassName, IBinder pService) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            if(DEBUG)   Log.d(TAG, "sIServiceSettingsConnection onServiceConnected()");
            sServiceSettingsReceiver = IServiceSettings.Stub.asInterface(pService);

            sServiceSettingsConnected = true;
        }

        public void onServiceDisconnected(ComponentName pClassName) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            if(DEBUG)   Log.d(TAG, "sIServiceSettingsConnection onServiceDisconnected()");
            sServiceSettingsReceiver = null;

            sServiceSettingsConnected = false;

            // Try and rebind to the service
            INSTANCE.releaseService();
            INSTANCE.initService();
        }
    };
    
    
    /**
     * Binds this activity to the ANT service.
     *
     * @return true, if successful
     */
    private boolean initService() {
        if(DEBUG)   Log.d(TAG, "initService() entered");

        boolean ret = false;

        if(!sServiceConnected)
        {
            ret = sContext.bindService(new Intent(IAnt.class.getName()), sIAntConnection, Context.BIND_AUTO_CREATE);
            Log.i(TAG, "initService(): Bound with ANT service: " + ret);
        }
        else
        {
            if(DEBUG)   Log.d(TAG, "initService: already initialised service");
            ret = true;
        }
     
        return ret;
    }
    
    /** Unbinds this activity from the ANT service. */
    private void releaseService() {
      if(DEBUG)   Log.d(TAG, "releaseService() entered");
      
      if(sServiceConnected)
      {
          sContext.unbindService(sIAntConnection);
          sServiceConnected = false;
      }
      
      if(sServiceSettingsConnected)
      {
          sContext.unbindService(sIServiceSettingsConnection);
          sServiceSettingsConnected = false;
      }

      if(DEBUG)   Log.d(TAG, "releaseService() unbound.");
    }

    /**
     * True if this activity can communicate with the ANT service.
     *
     * @return true, if is service connected
     * @since 1.2
     */
    public boolean isServiceConnected()
    {
        return sServiceConnected;
    }

    /**
     * Destroy.
     *
     * @return true, if successful
     * @since 1.0
     */
    public boolean destroy()
    {
        if(DEBUG)   Log.d(TAG, "destroy");

        releaseService();

        INSTANCE = null;

        return true;
    }


    /**
     * Ant service connection lost.
     */
    private void antServiceConnectionLost()
    {
        Log.e(TAG, "Connection to ANT service lost");
    }
    
    /**
     * Ant service connection to settings binder lost.
     */
    private void antServiceSettingsConnectionLost()
    {
        Log.e(TAG, "Connection to ANT service settings lost");
    }
    
    
    ////-------------------------------------------------
    
    /**
     * Enable.
     *
     * @return true, if successful
     * @since 1.0
     */
    public boolean enable()
    {
        if(DEBUG)   Log.d(TAG, "enable");

        if(!sServiceConnected)
        {
            // Haven't received 'onConnected' notification yet
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.enable();
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * Disable.
     *
     * @return true, if successful
     * @since 1.0
     */
    public boolean disable()
    {
        if(DEBUG)   Log.d(TAG, "disable");

        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.disable();
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled
     * @since 1.0
     */
    public boolean isEnabled()
    {
        if(DEBUG)   Log.d(TAG, "isEnabled");

        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.isEnabled();
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }        

    /**
     * ANT tx message.
     *
     * @param message the message
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTTxMessage(byte[] message)
    {
        if(DEBUG)   Log.d(TAG, "ANTTxMessage");

        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTTxMessage(message);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }

    /**
     * ANT reset system.
     *
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTResetSystem()
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTResetSystem();
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT unassign channel.
     *
     * @param channelNumber the channel number
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTUnassignChannel(byte channelNumber)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTUnassignChannel(channelNumber);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT assign channel.
     *
     * @param channelNumber the channel number
     * @param channelType the channel type
     * @param networkNumber the network number
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTAssignChannel(byte channelNumber, byte channelType, byte networkNumber)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTAssignChannel(channelNumber, channelType, networkNumber);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT set channel id.
     *
     * @param channelNumber the channel number
     * @param deviceNumber the device number
     * @param deviceType the device type
     * @param txType the tx type
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetChannelId(byte channelNumber, short deviceNumber, byte deviceType, byte txType)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSetChannelId(channelNumber, deviceNumber, deviceType, txType);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    } 
    
    /**
     * ANT set channel period.
     *
     * @param channelNumber the channel number
     * @param channelPeriod the channel period
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetChannelPeriod(byte channelNumber, short channelPeriod)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSetChannelPeriod(channelNumber, channelPeriod);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT set channel rf freq.
     *
     * @param channelNumber the channel number
     * @param radioFrequency the radio frequency
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetChannelRFFreq(byte channelNumber, byte radioFrequency)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSetChannelRFFreq(channelNumber, radioFrequency);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT set channel search timeout.
     *
     * @param channelNumber the channel number
     * @param searchTimeout the search timeout
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetChannelSearchTimeout(byte channelNumber, byte searchTimeout)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSetChannelSearchTimeout(channelNumber, searchTimeout);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT set low priority channel search timeout.
     *
     * @param channelNumber the channel number
     * @param searchTimeout the search timeout
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetLowPriorityChannelSearchTimeout(byte channelNumber, byte searchTimeout)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSetLowPriorityChannelSearchTimeout(channelNumber, searchTimeout);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    } 
    
    /**
     * ANT set proximity search.
     *
     * @param channelNumber the channel number
     * @param searchThreshold the search threshold
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetProximitySearch(byte channelNumber, byte searchThreshold)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSetProximitySearch(channelNumber, searchThreshold);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
   
    /**
     * ANT set channel transmit power
     * @param channelNumber the channel number
     * @param txPower the transmit power level
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSetChannelTxPower(byte channelNumber, byte txPower)
    {
       if(!sServiceConnected)
       {
           return false;
       }
       
       boolean result = false;
       try {
           result = sAntReceiver.ANTSetChannelTxPower(channelNumber, txPower);
       } catch (RemoteException ex)
       {
           antServiceConnectionLost();
       }
       return result;
    }
    
    
    /**
     * ANT add channel id.
     *
     * @param channelNumber the channel number
     * @param deviceNumber the device number
     * @param deviceType the device type
     * @param txType the tx type
     * @param listIndex the list index
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTAddChannelId(byte channelNumber, short deviceNumber, byte deviceType, byte txType, byte listIndex)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTAddChannelId(channelNumber, deviceNumber, deviceType, txType, listIndex);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    } 
    
    /**
     * ANT config list.
     *
     * @param channelNumber the channel number
     * @param listSize the list size
     * @param exclude the exclude
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTConfigList(byte channelNumber, byte listSize, byte exclude)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTConfigList(channelNumber, listSize, exclude);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT config event buffering.
     *
     * @param screenOnFlushTimerInterval the screen on flush timer interval
     * @param screenOnFlushBufferThreshold the screen on flush buffer threshold
     * @param screenOffFlushTimerInterval the screen off flush timer interval
     * @param screenOffFlushBufferThreshold the screen off flush buffer threshold
     * @return true, if successful
     * @since 1.3
     */
    public boolean ANTConfigEventBuffering(short screenOnFlushTimerInterval, short screenOnFlushBufferThreshold, short screenOffFlushTimerInterval, short screenOffFlushBufferThreshold)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTConfigEventBuffering((int)screenOnFlushTimerInterval, (int)screenOnFlushBufferThreshold, (int)screenOffFlushTimerInterval, (int)screenOffFlushBufferThreshold);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT disable event buffering.
     *
     * @return true, if successful
     * @since 1.1
     */
    public boolean ANTDisableEventBuffering()
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTDisableEventBuffering();
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT open channel.
     *
     * @param channelNumber the channel number
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTOpenChannel(byte channelNumber)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTOpenChannel(channelNumber);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT close channel.
     *
     * @param channelNumber the channel number
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTCloseChannel(byte channelNumber)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTCloseChannel(channelNumber);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT request message.
     *
     * @param channelNumber the channel number
     * @param messageID the message id
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTRequestMessage(byte channelNumber, byte messageID)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTRequestMessage(channelNumber, messageID);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT send broadcast data.
     *
     * @param channelNumber the channel number
     * @param txBuffer the tx buffer
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSendBroadcastData(byte channelNumber, byte[] txBuffer)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSendBroadcastData(channelNumber, txBuffer);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    }
    
    /**
     * ANT send acknowledged data.
     *
     * @param channelNumber the channel number
     * @param txBuffer the tx buffer
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSendAcknowledgedData(byte channelNumber, byte[] txBuffer)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSendAcknowledgedData(channelNumber, txBuffer);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    } 

    /**
     * ANT send burst transfer packet.
     *
     * @param control the control
     * @param txBuffer the tx buffer
     * @return true, if successful
     * @since 1.0
     */
    public boolean ANTSendBurstTransferPacket(byte control, byte[] txBuffer)
    {
        if(!sServiceConnected)
        {
            return false;
        }

        boolean result = false;
        try {
            result = sAntReceiver.ANTSendBurstTransferPacket(control, txBuffer);
        } catch (RemoteException ex) {
            antServiceConnectionLost();
        }
        return result;
    } 

    /**
     * Transmits the given data on channelNumber as part of a burst message.
     * 
     * @param channelNumber Which channel to transmit on.
     * @param txBuffer The data to send.
     * @param initialPacket Which packet in the burst sequence does the data begin in, 1 is the first.
     * @param containsEndOfBurst Is this the last of the data to be sent in burst.
     * @return The number of bytes still to be sent (approximately).  0 if success.
     */
    public int ANTSendBurstTransfer(byte channelNumber, byte[] txBuffer)
    {
        int result = txBuffer.length;
        
        if(sServiceConnected)
        {
            try {
                result = sAntReceiver.ANTSendBurstTransfer(channelNumber, txBuffer);
            } catch (RemoteException ex) {
                antServiceConnectionLost();
            }
        }
        return result;
    }
    
    /**
     * ANT send partial burst.
     *
     * @param channelNumber the channel number
     * @param txBuffer the tx buffer
     * @param initialPacket the initial packet
     * @param containsEndOfBurst the contains end of burst
     * @return the int
     * @since 1.0
     */
    public int ANTSendPartialBurst(byte channelNumber, byte[] txBuffer, int initialPacket, boolean containsEndOfBurst)
    {
        int result = txBuffer.length;
        
        if(sServiceConnected)
        {
            try {
                result = sAntReceiver.ANTTransmitBurst(channelNumber, txBuffer, initialPacket, containsEndOfBurst);
            } catch (RemoteException ex) {
                antServiceConnectionLost();
            }
        }
        return result;
    }

    /**
     * Returns the version code (eg. 1) of ANTLib used by the ANT application service
     *
     * @return the service library version code
     * @throws RemoteException the remote exception
     * @since 1.2
     */
    public int getServiceLibraryVersionCode() throws RemoteException
    {
        if(mServiceLibraryVersionCode == 0)
        {
            mServiceLibraryVersionCode = sAntReceiver.getServiceLibraryVersionCode();
        }
        
        return mServiceLibraryVersionCode;
    }
    
    /**
     * Returns the version name (eg "1.0") of ANTLib used by the ANT application service
     *
     * @return the service library version name
     * @throws RemoteException the remote exception
     * @since 1.2
     */
    public String getServiceLibraryVersionName() throws RemoteException
    {
        return sAntReceiver.getServiceLibraryVersionName();
    }
    
    
    //
    //  -------------------  The below functions are for debugging/development and will likely disappear.
    //
    
    /**
     * Turn verbose logging on or off in the service.
     *
     * @param debug Whether debug logging should be enabled.
     * @return true, if successful
     * @since 1.4
     */
    public boolean debugLogging(boolean debug)
    {
        if(DEBUG)   Log.d(TAG, "debugLogging");

        if(!sServiceSettingsConnected)
        {
            // Haven't received 'onConnected' notification yet
            return false;
        }

        boolean result = false;
        try {
            sServiceSettingsReceiver.debugLogging(debug);
            result = true;
        } catch (RemoteException ex) {
            antServiceSettingsConnectionLost();
        }
        return result;
    }
    
    /**
     * Set how many ANT packets should be combined in one request to the hardware during a burst transfer.
     *
     * @param numPackets Combined packet count.
     * @return true, if successful
     * @since 1.4
     */
    boolean setNumCombinedBurstPackets(int numPackets)
    {
        if(DEBUG)   Log.d(TAG, "setNumCombinedBurstPackets");

        if(!sServiceSettingsConnected)
        {
            // Haven't received 'onConnected' notification yet
            return false;
        }

        boolean result = false;
        try {
            sServiceSettingsReceiver.setNumCombinedBurstPackets(numPackets);
            result = true;
        } catch (RemoteException ex) {
            antServiceSettingsConnectionLost();
        }
        
        return result;
    }
    
    /**
     * Get the maximum number of ANT packets which will be combined in one request to the hardware during a burst transfer.
     *
     * @return The number of combined packets, or -1 on failure.
     * @since 1.4
     */
    int getNumCombinedBurstPackets()
    {
    	if(DEBUG)   Log.d(TAG, "getNumCombinedBurstPackets");

        if(!sServiceSettingsConnected)
        {
            // Haven't received 'onConnected' notification yet
            return -1;
        }

        int result = -1;
        try {
            result = sServiceSettingsReceiver.getNumCombinedBurstPackets();
        } catch (RemoteException ex) {
            antServiceSettingsConnectionLost();
        }
        
        return result;
    }
}
