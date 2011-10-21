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

import com.dsi.ant.exception.AntInterfaceException;
import com.dsi.ant.exception.AntRemoteException;
import com.dsi.ant.exception.AntServiceNotConnectedException;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.Arrays;

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

    /** Name of the ANT Radio shared library */
    private static final String ANT_LIBRARY_NAME = "com.dsi.ant.antradio_library";

    /** Inter-process communication with the ANT Radio Proxy Service. */
    private static IAnt_6 sAntReceiver = null;

    /** Singleton instance of this class. */
    private static AntInterface INSTANCE;

    /** Used when obtaining a reference to the singleton instance. */
    private static Object INSTANCE_LOCK = new Object();

    /** The context to use. */
    private Context sContext = null;

    /** Listens to changes to service connection status. */
    private ServiceListener sServiceListener;

    /** Is the ANT Radio Proxy Service connected. */
    private static boolean sServiceConnected = false;

    /** The version code (eg. 1) of ANTLib used by the ANT application service */
    private static int mServiceLibraryVersionCode = 0;

    /** Has support for ANT already been checked */
    private static boolean mCheckedAntSupported = false;

    /** Is ANT supported on this device */
    private static boolean mAntSupported = false;

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
     * Only the initial request for an instance will have context and listener set to the requested objects.
     *
     * @param context the context used to bind to the remote service.
     * @param listener the listener to be notified of status changes.
     * @return the AntInterface instance.
     * @since 1.0
     */
    public static AntInterface getInstance(Context context,ServiceListener listener)
    {
        if(DEBUG)   Log.d(TAG, "getInstance");

        synchronized (INSTANCE_LOCK)
        {
            if(!hasAntSupport(context))
            {
                if(DEBUG) Log.d(TAG, "getInstance: ANT not supported");

                return null;
            }

            if (INSTANCE == null)
            {
                if(DEBUG)   Log.d(TAG, "getInstance: Creating new instance");

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
    private final ServiceConnection sIAntConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName pClassName, IBinder pService) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            if(DEBUG)   Log.d(TAG, "sIAntConnection onServiceConnected()");
            sAntReceiver = IAnt_6.Stub.asInterface(pService);

            sServiceConnected = true;

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
            mServiceLibraryVersionCode = 0;

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
     * Binds this activity to the ANT service.
     *
     * @return true, if successful
     */
    private boolean initService() {
        if(DEBUG)   Log.d(TAG, "initService() entered");

        boolean ret = false;

        if(!sServiceConnected)
        {
            ret = sContext.bindService(new Intent(IAnt_6.class.getName()), sIAntConnection, Context.BIND_AUTO_CREATE);
            if(DEBUG) Log.i(TAG, "initService(): Bound with ANT service: " + ret);
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

        // TODO Make sure can handle multiple calls to onDestroy
        if(sServiceConnected)
        {
            sContext.unbindService(sIAntConnection);
            sServiceConnected = false;
        }

        if(DEBUG)   Log.d(TAG, "releaseService() unbound.");
    }

    /**
     * True if this activity can communicate with the ANT service.
     *
     * @return true, if service is connected
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


    ////-------------------------------------------------

    /**
     * Enable.
     *
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void enable() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.enable())
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Disable.
     *
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void disable() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.disable())
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public boolean isEnabled() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.isEnabled();
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT tx message.
     *
     * @param message the message
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTTxMessage(byte[] message) throws AntInterfaceException
    {
        if(DEBUG) Log.d(TAG, "ANTTxMessage");

        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTTxMessage(message))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT reset system.
     *
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTResetSystem() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTResetSystem())
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT unassign channel.
     *
     * @param channelNumber the channel number
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTUnassignChannel(byte channelNumber) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTUnassignChannel(channelNumber))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT assign channel.
     *
     * @param channelNumber the channel number
     * @param channelType the channel type
     * @param networkNumber the network number
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTAssignChannel(byte channelNumber, byte channelType, byte networkNumber) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTAssignChannel(channelNumber, channelType, networkNumber))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set channel id.
     *
     * @param channelNumber the channel number
     * @param deviceNumber the device number
     * @param deviceType the device type
     * @param txType the tx type
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetChannelId(byte channelNumber, short deviceNumber, byte deviceType, byte txType) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetChannelId(channelNumber, deviceNumber, deviceType, txType))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set channel period.
     *
     * @param channelNumber the channel number
     * @param channelPeriod the channel period
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetChannelPeriod(byte channelNumber, short channelPeriod) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetChannelPeriod(channelNumber, channelPeriod))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set channel rf freq.
     *
     * @param channelNumber the channel number
     * @param radioFrequency the radio frequency
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetChannelRFFreq(byte channelNumber, byte radioFrequency) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetChannelRFFreq(channelNumber, radioFrequency))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set channel search timeout.
     *
     * @param channelNumber the channel number
     * @param searchTimeout the search timeout
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetChannelSearchTimeout(byte channelNumber, byte searchTimeout) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetChannelSearchTimeout(channelNumber, searchTimeout))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set low priority channel search timeout.
     *
     * @param channelNumber the channel number
     * @param searchTimeout the search timeout
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetLowPriorityChannelSearchTimeout(byte channelNumber, byte searchTimeout) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetLowPriorityChannelSearchTimeout(channelNumber, searchTimeout))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set proximity search.
     *
     * @param channelNumber the channel number
     * @param searchThreshold the search threshold
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetProximitySearch(byte channelNumber, byte searchThreshold) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetProximitySearch(channelNumber, searchThreshold))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT set channel transmit power
     * @param channelNumber the channel number
     * @param txPower the transmit power level
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSetChannelTxPower(byte channelNumber, byte txPower) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSetChannelTxPower(channelNumber, txPower))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT add channel id.
     *
     * @param channelNumber the channel number
     * @param deviceNumber the device number
     * @param deviceType the device type
     * @param txType the tx type
     * @param listIndex the list index
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTAddChannelId(byte channelNumber, short deviceNumber, byte deviceType, byte txType, byte listIndex) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTAddChannelId(channelNumber, deviceNumber, deviceType, txType, listIndex))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT config list.
     *
     * @param channelNumber the channel number
     * @param listSize the list size
     * @param exclude the exclude
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTConfigList(byte channelNumber, byte listSize, byte exclude) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTConfigList(channelNumber, listSize, exclude))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT config event buffering.
     *
     * @param screenOnFlushTimerInterval the screen on flush timer interval
     * @param screenOnFlushBufferThreshold the screen on flush buffer threshold
     * @param screenOffFlushTimerInterval the screen off flush timer interval
     * @param screenOffFlushBufferThreshold the screen off flush buffer threshold
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.3
     */
    public void ANTConfigEventBuffering(short screenOnFlushTimerInterval, short screenOnFlushBufferThreshold, short screenOffFlushTimerInterval, short screenOffFlushBufferThreshold) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTConfigEventBuffering((int)screenOnFlushTimerInterval, (int)screenOnFlushBufferThreshold, (int)screenOffFlushTimerInterval, (int)screenOffFlushBufferThreshold))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT disable event buffering.
     *
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.1
     */
    public void ANTDisableEventBuffering() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTDisableEventBuffering())
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT open channel.
     *
     * @param channelNumber the channel number
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTOpenChannel(byte channelNumber) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTOpenChannel(channelNumber))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT close channel.
     *
     * @param channelNumber the channel number
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTCloseChannel(byte channelNumber) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTCloseChannel(channelNumber))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT request message.
     *
     * @param channelNumber the channel number
     * @param messageID the message id
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTRequestMessage(byte channelNumber, byte messageID) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTRequestMessage(channelNumber, messageID))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT send broadcast data.
     *
     * @param channelNumber the channel number
     * @param txBuffer the tx buffer
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSendBroadcastData(byte channelNumber, byte[] txBuffer) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSendBroadcastData(channelNumber, txBuffer))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT send acknowledged data.
     *
     * @param channelNumber the channel number
     * @param txBuffer the tx buffer
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSendAcknowledgedData(byte channelNumber, byte[] txBuffer) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSendAcknowledgedData(channelNumber, txBuffer))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT send burst transfer packet.
     *
     * @param control the control
     * @param txBuffer the tx buffer
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public void ANTSendBurstTransferPacket(byte control, byte[] txBuffer) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            if(!sAntReceiver.ANTSendBurstTransferPacket(control, txBuffer))
            {
                throw new AntInterfaceException();
            }
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Transmits the given data on channelNumber as part of a burst message.
     *
     * @param channelNumber Which channel to transmit on.
     * @param txBuffer The data to send.
     * @param initialPacket Which packet in the burst sequence does the data begin in, 1 is the first.
     * @param containsEndOfBurst Is this the last of the data to be sent in burst.
     * @return The number of bytes still to be sent (approximately).  0 if success.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     */
    public int ANTSendBurstTransfer(byte channelNumber, byte[] txBuffer) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.ANTSendBurstTransfer(channelNumber, txBuffer);
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * ANT send partial burst.
     *
     * @param channelNumber the channel number
     * @param txBuffer the tx buffer
     * @param initialPacket the initial packet
     * @param containsEndOfBurst the contains end of burst
     * @return The number of bytes still to be sent (approximately).  0 if success.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.0
     */
    public int ANTSendPartialBurst(byte channelNumber, byte[] txBuffer, int initialPacket, boolean containsEndOfBurst) throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.ANTTransmitBurst(channelNumber, txBuffer, initialPacket, containsEndOfBurst);
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Returns the version code (eg. 1) of ANTLib used by the ANT application service
     *
     * @return the service library version code, or 0 on error.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.2
     */
    public int getServiceLibraryVersionCode()  throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        if(mServiceLibraryVersionCode == 0)
        {
            try
            {
                mServiceLibraryVersionCode = sAntReceiver.getServiceLibraryVersionCode();
            }
            catch(RemoteException e)
            {
                throw new AntRemoteException(e);
            }
        }

        return mServiceLibraryVersionCode;
    }

    /**
     * Returns the version name (eg "1.0") of ANTLib used by the ANT application service
     *
     * @return the service library version name, or null on error.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.2
     */
    public String getServiceLibraryVersionName()  throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.getServiceLibraryVersionName();
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Take control of the ANT Radio.
     *
     * @return True if control has been granted, false if another application has control or the request failed.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.5
     */
    public boolean claimInterface() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.claimInterface();
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Give up control of the ANT Radio.
     *
     * @return True if control has been given up, false if this application did not have control.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.5
     */
    public boolean releaseInterface() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.releaseInterface();
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Claims the interface if it is available.  If not the user will be prompted (on the notification bar) if a force claim should be done.
     * If the ANT Interface is claimed, an AntInterfaceIntent.ANT_INTERFACE_CLAIMED_ACTION intent will be sent, with the current applications pid.
     *
     * @param String appName The name if this application, to show to the user.
     * @returns false if a claim interface request notification already exists.
     * @throws IllegalArgumentException
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.5
     */
    public boolean requestForceClaimInterface(String appName) throws AntInterfaceException
    {
        if((null == appName) || ("".equals(appName)))
        {
            throw new IllegalArgumentException();
        }

        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.requestForceClaimInterface(appName);
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Clears the notification asking the user if they would like to seize control of the ANT Radio.
     *
     * @returns false if this process is not requesting to claim the interface.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.5
     */
    public boolean stopRequestForceClaimInterface() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.stopRequestForceClaimInterface();
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Check if the calling application has control of the ANT Radio.
     *
     * @return True if control is currently granted.
     * @throws AntInterfaceException
     * @throws AntServiceNotConnectedException
     * @throws AntRemoteException
     * @since 1.5
     */
    public boolean hasClaimedInterface() throws AntInterfaceException
    {
        if(!sServiceConnected)
        {
            throw new AntServiceNotConnectedException();
        }

        try
        {
            return sAntReceiver.hasClaimedInterface();
        }
        catch(RemoteException e)
        {
            throw new AntRemoteException(e);
        }
    }

    /**
     * Check if this device has support for ANT.
     *
     * @return True if the device supports ANT (may still require ANT Radio Service be installed).
     * @since 1.5
     */
    public static boolean hasAntSupport(Context pContext)
    {
        if(!mCheckedAntSupported)
        {
            mAntSupported = Arrays.asList(pContext.getPackageManager().getSystemSharedLibraryNames()).contains(ANT_LIBRARY_NAME);
            mCheckedAntSupported = true;
        }

        return mAntSupported;
    }
}
