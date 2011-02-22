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
 * The Android Ant API is not finalized, and *will* change. Use at your 
 * own risk.
 *
 * Public API for controlling the Ant Service. 
 * AntDefines contains definitions commonly used in ANT messaging.
 * 
 * @hide
 */
public class AntDefine {
	
    //////////////////////////////////////////////
    // Valid Configuration Values
    //////////////////////////////////////////////
    public static final byte MIN_BIN = 0;
    public static final byte MAX_BIN = 10;
    
    public static final short MIN_DEVICE_ID = 0;
    public static final short MAX_DEVICE_ID = (short)65535;
    
    public static final short MIN_BUFFER_THRESHOLD = 0;
    public static final short MAX_BUFFER_THRESHOLD = 996;
    
	//////////////////////////////////////////////
	// ANT Message Payload Size
	//////////////////////////////////////////////
	public static final byte ANT_STANDARD_DATA_PAYLOAD_SIZE             =((byte)8);

	//////////////////////////////////////////////
	// ANT LIBRARY Extended Data Message Fields
	// NOTE: You must check the extended message
	// bitfield first to find out which fields
	// are present before accessing them!
	//////////////////////////////////////////////
	public static final byte ANT_EXT_MESG_DEVICE_ID_FIELD_SIZE          =((byte)4);
	//public static final byte ANT_EXT_STRING_SIZE                        =((byte)19);             // this is the additional buffer space required required for setting USB Descriptor strings
	public static final byte ANT_EXT_STRING_SIZE                        =((byte)0);              // changed to 0 as we will not be dealing with ANT USB parts.	

	//////////////////////////////////////////////
	// ANT Extended Data Message Bifield Definitions
	//////////////////////////////////////////////
	public static final byte ANT_EXT_MESG_BITFIELD_DEVICE_ID            =((byte)0x80);           // first field after bitfield	
	public static final byte ANT_EXT_MESG_BITFIELD_RSSI                 =((byte)0x40);           // next field after ID, if there is one
	public static final byte ANT_EXT_MESG_BITFIELD_TIME_STAMP           =((byte)0x20);           // next field after RSSI, if there is one	

	// 4 bits free reserved set to 0
	public static final byte ANT_EXT_MESG_BIFIELD_EXTENSION             =((byte)0x01);

	// extended message input bitfield defines
	public static final byte ANT_EXT_MESG_BITFIELD_OVERWRITE_SHARED_ADR =((byte)0x10);
	public static final byte ANT_EXT_MESG_BITFIELD_TRANSMISSION_TYPE    =((byte)0x08);	

	//////////////////////////////////////////////
	// ID Definitions
	//////////////////////////////////////////////
	public static final byte ANT_ID_SIZE                                =((byte)4);
	public static final byte ANT_ID_TRANS_TYPE_OFFSET                   =((byte)3);
	public static final byte ANT_ID_DEVICE_TYPE_OFFSET                  =((byte)2);
	public static final byte ANT_ID_DEVICE_NUMBER_HIGH_OFFSET           =((byte)1);
	public static final byte ANT_ID_DEVICE_NUMBER_LOW_OFFSET            =((byte)0);
	public static final byte ANT_ID_DEVICE_TYPE_PAIRING_FLAG            =((byte)0x80);

	public static final byte ANT_TRANS_TYPE_SHARED_ADDR_MASK            =((byte)0x03);
	public static final byte ANT_TRANS_TYPE_1_BYTE_SHARED_ADDRESS       =((byte)0x02);
	public static final byte ANT_TRANS_TYPE_2_BYTE_SHARED_ADDRESS       =((byte)0x03);	

	//////////////////////////////////////////////
	// Assign Channel Parameters
	//////////////////////////////////////////////
	public static final byte PARAMETER_RX_NOT_TX                        =((byte)0x00);
	public static final byte PARAMETER_TX_NOT_RX                        =((byte)0x10);
	public static final byte PARAMETER_SHARED_CHANNEL                   =((byte)0x20);
	public static final byte PARAMETER_NO_TX_GUARD_BAND                 =((byte)0x40);
	public static final byte PARAMETER_ALWAYS_RX_WILD_CARD_SEARCH_ID    =((byte)0x40);                 //Pre-AP2
	public static final byte PARAMETER_RX_ONLY                          =((byte)0x40);	

	//////////////////////////////////////////////
	// Ext. Assign Channel Parameters
	//////////////////////////////////////////////
	public static final byte EXT_PARAM_ALWAYS_SEARCH                    =((byte)0x01);	
	public static final byte EXT_PARAM_FREQUENCY_AGILITY                =((byte)0x04);	

	//////////////////////////////////////////////
	// Radio TX Power Definitions
	//////////////////////////////////////////////
	public static final byte RADIO_TX_POWER_LVL_MASK                    =((byte)0x03);

	public static final byte RADIO_TX_POWER_LVL_0                       =((byte)0x00);                //(formerly: RADIO_TX_POWER_MINUS20DB); lowest
	public static final byte RADIO_TX_POWER_LVL_1                       =((byte)0x01);                //(formerly: RADIO_TX_POWER_MINUS10DB);
	public static final byte RADIO_TX_POWER_LVL_2                       =((byte)0x02);                //(formerly: RADIO_TX_POWER_MINUS5DB);
	public static final byte RADIO_TX_POWER_LVL_3                       =((byte)0x03);                //(formerly: RADIO_TX_POWER_0DB); highest

	//////////////////////////////////////////////
	// Channel Status
	//////////////////////////////////////////////
	public static final byte STATUS_CHANNEL_STATE_MASK                  =((byte)0x03);
	public static final byte STATUS_UNASSIGNED_CHANNEL                  =((byte)0x00);
	public static final byte STATUS_ASSIGNED_CHANNEL                    =((byte)0x01);
	public static final byte STATUS_SEARCHING_CHANNEL                   =((byte)0x02);
	public static final byte STATUS_TRACKING_CHANNEL                    =((byte)0x03);

	//////////////////////////////////////////////
	// Standard capabilities defines
	//////////////////////////////////////////////
	public static final byte CAPABILITIES_NO_RX_CHANNELS                =((byte)0x01);
	public static final byte CAPABILITIES_NO_TX_CHANNELS                =((byte)0x02);
	public static final byte CAPABILITIES_NO_RX_MESSAGES                =((byte)0x04);
	public static final byte CAPABILITIES_NO_TX_MESSAGES                =((byte)0x08);
	public static final byte CAPABILITIES_NO_ACKD_MESSAGES              =((byte)0x10);
	public static final byte CAPABILITIES_NO_BURST_TRANSFER             =((byte)0x20);

	//////////////////////////////////////////////
	// Advanced capabilities defines
	//////////////////////////////////////////////
	public static final byte CAPABILITIES_OVERUN_UNDERRUN               =((byte)0x01);     // Support for this functionality has been dropped
	public static final byte CAPABILITIES_NETWORK_ENABLED               =((byte)0x02);
	public static final byte CAPABILITIES_AP1_VERSION_2                 =((byte)0x04);     // This Version of the AP1 does not support transmit and only had a limited release
	public static final byte CAPABILITIES_SERIAL_NUMBER_ENABLED         =((byte)0x08);
	public static final byte CAPABILITIES_PER_CHANNEL_TX_POWER_ENABLED  =((byte)0x10);
	public static final byte CAPABILITIES_LOW_PRIORITY_SEARCH_ENABLED   =((byte)0x20);
	public static final byte CAPABILITIES_SCRIPT_ENABLED                =((byte)0x40);
	public static final byte CAPABILITIES_SEARCH_LIST_ENABLED           =((byte)0x80);

	//////////////////////////////////////////////
	// Advanced capabilities 2 defines
	//////////////////////////////////////////////
	public static final byte CAPABILITIES_LED_ENABLED                   =((byte)0x01);
	public static final byte CAPABILITIES_EXT_MESSAGE_ENABLED           =((byte)0x02);
	public static final byte CAPABILITIES_SCAN_MODE_ENABLED             =((byte)0x04);
	public static final byte CAPABILITIES_RESERVED                      =((byte)0x08);
	public static final byte CAPABILITIES_PROX_SEARCH_ENABLED           =((byte)0x10);
	public static final byte CAPABILITIES_EXT_ASSIGN_ENABLED            =((byte)0x20);
	public static final byte CAPABILITIES_FREE_1                        =((byte)0x40);
	public static final byte CAPABILITIES_FIT1_ENABLED                  =((byte)0x80);

	//////////////////////////////////////////////
	// Advanced capabilities 3 defines
	//////////////////////////////////////////////
	public static final byte CAPABILITIES_SENSRCORE_ENABLED             =((byte)0x01);
	public static final byte CAPABILITIES_RESERVED_1                    =((byte)0x02);
	public static final byte CAPABILITIES_RESERVED_2                    =((byte)0x04);
	public static final byte CAPABILITIES_RESERVED_3                    =((byte)0x08);


	//////////////////////////////////////////////
	// Burst Message Sequence
	//////////////////////////////////////////////
	public static final byte CHANNEL_NUMBER_MASK                        =((byte)0x1F);
	public static final byte SEQUENCE_NUMBER_MASK                       =((byte)0xE0);
	public static final byte SEQUENCE_NUMBER_ROLLOVER                   =((byte)0x60);
	public static final byte SEQUENCE_FIRST_MESSAGE                     =((byte)0x00);
	public static final byte SEQUENCE_LAST_MESSAGE                      =((byte)0x80);
	public static final byte SEQUENCE_NUMBER_INC                        =((byte)0x20);

	//////////////////////////////////////////////
	// Control Message Flags
	//////////////////////////////////////////////
	public static final byte BROADCAST_CONTROL_BYTE                     =((byte)0x00);
	public static final byte ACKNOWLEDGED_CONTROL_BYTE                  =((byte)0xA0);

	//////////////////////////////////////////////
	// Response / Event Codes
	//////////////////////////////////////////////
	public static final byte RESPONSE_NO_ERROR                          =((byte)0x00);
	public static final byte NO_EVENT                                   =((byte)0x00);

	public static final byte EVENT_RX_SEARCH_TIMEOUT                    =((byte)0x01);
	public static final byte EVENT_RX_FAIL                              =((byte)0x02);
	public static final byte EVENT_TX                                   =((byte)0x03);
	public static final byte EVENT_TRANSFER_RX_FAILED                   =((byte)0x04);
	public static final byte EVENT_TRANSFER_TX_COMPLETED                =((byte)0x05);
	public static final byte EVENT_TRANSFER_TX_FAILED                   =((byte)0x06);
	public static final byte EVENT_CHANNEL_CLOSED                       =((byte)0x07);
	public static final byte EVENT_RX_FAIL_GO_TO_SEARCH                 =((byte)0x08);
	public static final byte EVENT_CHANNEL_COLLISION                    =((byte)0x09);
	public static final byte EVENT_TRANSFER_TX_START                    =((byte)0x0A);           // a pending transmit transfer has begun

	public static final byte EVENT_CHANNEL_ACTIVE                       =((byte)0x0F);
	
	public static final byte EVENT_TRANSFER_TX_NEXT_MESSAGE             =((byte)0x11);           // only enabled in FIT1

	public static final byte CHANNEL_IN_WRONG_STATE                     =((byte)0x15);           // returned on attempt to perform an action from the wrong channel state
	public static final byte CHANNEL_NOT_OPENED                         =((byte)0x16);           // returned on attempt to communicate on a channel that is not open
	public static final byte CHANNEL_ID_NOT_SET                         =((byte)0x18);           // returned on attempt to open a channel without setting the channel ID
	public static final byte CLOSE_ALL_CHANNELS                         =((byte)0x19);           // returned when attempting to start scanning mode, when channels are still open

	public static final byte TRANSFER_IN_PROGRESS                       =((byte)0x1F);           // returned on attempt to communicate on a channel with a TX transfer in progress
	public static final byte TRANSFER_SEQUENCE_NUMBER_ERROR             =((byte)0x20);           // returned when sequence number is out of order on a Burst transfer
	public static final byte TRANSFER_IN_ERROR                          =((byte)0x21);
	public static final byte TRANSFER_BUSY                              =((byte)0x22);

	public static final byte INVALID_MESSAGE_CRC                        =((byte)0x26);           // returned if there is a framing error on an incomming message
	public static final byte MESSAGE_SIZE_EXCEEDS_LIMIT                 =((byte)0x27);           // returned if a data message is provided that is too large
	public static final byte INVALID_MESSAGE                            =((byte)0x28);           // returned when the message has an invalid parameter
	public static final byte INVALID_NETWORK_NUMBER                     =((byte)0x29);           // returned when an invalid network number is provided
	public static final byte INVALID_LIST_ID                            =((byte)0x30);           // returned when the provided list ID or size exceeds the limit
	public static final byte INVALID_SCAN_TX_CHANNEL                    =((byte)0x31);           // returned when attempting to transmit on channel 0 when in scan mode
	
	public static final byte INVALID_PARAMETER_PROVIDED                 =((byte)0x33);           // returned when an invalid parameter is specified in a configuration message

	public static final byte EVENT_SERIAL_QUE_OVERFLOW                  =((byte)0x34);
	public static final byte EVENT_QUE_OVERFLOW                         =((byte)0x35);           // ANT event que has overflowed and lost 1 or more events

	public static final byte EVENT_CLK_ERROR                            =((byte)0x36);           // debug event for XOSC16M on LE1

	public static final byte SCRIPT_FULL_ERROR                          =((byte)0x40);           // error writing to script, memory is full
	public static final byte SCRIPT_WRITE_ERROR                         =((byte)0x41);           // error writing to script, bytes not written correctly
	public static final byte SCRIPT_INVALID_PAGE_ERROR                  =((byte)0x42);           // error accessing script page
	public static final byte SCRIPT_LOCKED_ERROR                        =((byte)0x43);           // the scripts are locked and can't be dumped

	public static final byte NO_RESPONSE_MESSAGE                        =((byte)0x50);           // returned to the Command_SerialMessageProcess function, so no reply message is generated
	public static final byte RETURN_TO_MFG                              =((byte)0x51);           // default return to any mesg when the module determines that the mfg procedure has not been fully completed

	public static final byte FIT_ACTIVE_SEARCH_TIMEOUT                  =((byte)0x60);           // Fit1 only event added for timeout of the pairing state after the Fit module becomes active
	public static final byte FIT_WATCH_PAIR                             =((byte)0x61);           // Fit1 only
	public static final byte FIT_WATCH_UNPAIR                           =((byte)0x62);           // Fit1 only

	public static final byte USB_STRING_WRITE_FAIL                      =((byte)0x70);

	// Internal only events below this point
	public static final byte INTERNAL_ONLY_EVENTS                       =((byte)0x80);
	public static final byte EVENT_RX                                   =((byte)0x80);           // INTERNAL: Event for a receive message
	public static final byte EVENT_NEW_CHANNEL                          =((byte)0x81);           // INTERNAL: EVENT for a new active channel
	public static final byte EVENT_PASS_THRU                            =((byte)0x82);           // INTERNAL: Event to allow an upper stack events to pass through lower stacks

	public static final byte EVENT_BLOCKED                              =((byte)0xFF);           // INTERNAL: Event to replace any event we do not wish to go out, will also zero the size of the Tx message

	///////////////////////////////////////////////////////
	// Script Command Codes
	///////////////////////////////////////////////////////
	public static final byte SCRIPT_CMD_FORMAT                          =((byte)0x00);
	public static final byte SCRIPT_CMD_DUMP                            =((byte)0x01);
	public static final byte SCRIPT_CMD_SET_DEFAULT_SECTOR              =((byte)0x02);
	public static final byte SCRIPT_CMD_END_SECTOR                      =((byte)0x03);
	public static final byte SCRIPT_CMD_END_DUMP                        =((byte)0x04);
	public static final byte SCRIPT_CMD_LOCK                            =((byte)0x05);

	///////////////////////////////////////////////////////
	// Reset Mesg Codes
	///////////////////////////////////////////////////////
	public static final byte RESET_FLAGS_MASK                           =((byte)0xE0);
	public static final byte RESET_SUSPEND                              =((byte)0x80);              // this must follow bitfield def
	public static final byte RESET_SYNC                                 =((byte)0x40);              // this must follow bitfield def
	public static final byte RESET_CMD                                  =((byte)0x20);              // this must follow bitfield def
	public static final byte RESET_WDT                                  =((byte)0x02);
	public static final byte RESET_RST                                  =((byte)0x01);
	public static final byte RESET_POR                                  =((byte)0x00);
	
}