// ----------------------------------------------------------------------------
// Copyright 2006-2010, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Description:
//  Socket client packet handler
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.net.*;
import javax.net.*;

//import javax.net.ssl.*;

/**
*** Inteface for packet handling clients [CHECK]
**/

public interface ClientPacketHandler
{

    /**
    *** Called when new client session initiated
    *** @param inetAddr The host IP address
    *** @param isTCP True if the connection is TCP
    *** @param isText True if the connection is text
    **/
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText);
    
    /**
    *** Return initial response to the open session
    *** @return The initial response to be sent when the session opens
    **/
    public byte[] getInitialPacket() throws Exception;
    
    /**
    *** Return final response to the session before it closes
    *** @return the final response to be sent before the session closes
    **/
    public byte[] getFinalPacket(boolean hasError) throws Exception;

    /**
    *** Gets the minimum packet length
    *** @return  The minimum packet length
    **/
    public int getMinimumPacketLength();
    
    /**
    *** Gets the maximum packet length
    *** @return  The maximum packet length
    **/
    public int getMaximumPacketLength();

    /**
    *** Return actual packet length based on this partial packet
    **/
    public int getActualPacketLength(byte packet[], int packetLen); // non-text

    /**
    *** Process packet and return response
    *** @param cmd The packet
    *** @return The response
    **/
    public byte[] getHandlePacket(byte cmd[]) throws Exception;

    /**
    *** Return the port for UDP Datagram responses
    *** @return The port for UDP Datafram responses
    **/
    public int getResponsePort(); // may return '0' to default to "<ServerSocketThread>.getRemotePort()"

    /**
    *** Indicates if the session should terminate
    *** @return True if the session should terminate
    **/
    public boolean terminateSession();
    
    /**
    *** Called after client session terminated
    *** @param err 
    *** @param readCount
    *** @param writeCount 
    **/
    public void sessionTerminated(Throwable err, long readCount, long writeCount);
    
}
