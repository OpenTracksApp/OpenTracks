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
 * Manages the ANT Interface 
 *
 * @hide
 */
public interface AntInterfaceIntent {
	
	public static final String STATUS                    = "com.dsi.ant.rx.intent.STATUS";	
	public static final String ANT_MESSAGE               = "com.dsi.ant.intent.ANT_MESSAGE";
	public static final String ANT_INTERFACE_CLAIMED_PID = "com.dsi.ant.intent.ANT_INTERFACE_CLAIMED_PID";

	public static final String ANT_ENABLED_ACTION    = "com.dsi.ant.intent.action.ANT_ENABLED";
	public static final String ANT_DISABLED_ACTION   = "com.dsi.ant.intent.action.ANT_DISABLED";
	public static final String ANT_RESET_ACTION      = "com.dsi.ant.intent.action.ANT_RESET";
	
	public static final String ANT_INTERFACE_CLAIMED_ACTION = "com.dsi.ant.intent.action.ANT_INTERFACE_CLAIMED_ACTION";
	
	public static final String ANT_RX_MESSAGE_ACTION = "com.dsi.ant.intent.action.ANT_RX_MESSAGE_ACTION";

}
