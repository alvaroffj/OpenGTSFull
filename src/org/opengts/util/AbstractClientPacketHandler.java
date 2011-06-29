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
//  Partial implementation of a ClientPacketHandler
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2009/04/02  Marti D. Flynn
//     -Added 'getMinimumPacketLength' and 'getMaximumPacketLength'
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.net.*;
import javax.net.*;

//import javax.net.ssl.*;

/**
*** An abstract implementation of the <code>ClientPacketHandler</code> interface
**/

public abstract class AbstractClientPacketHandler
    implements ClientPacketHandler
{
    
    // ------------------------------------------------------------------------
    
    public static final int     PACKET_LEN_ASCII_LINE_TERMINATOR = ServerSocketThread.PACKET_LEN_ASCII_LINE_TERMINATOR;
    public static final int     PACKET_LEN_END_OF_STREAM         = ServerSocketThread.PACKET_LEN_END_OF_STREAM;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private InetAddress inetAddr = null;
    private boolean isTCP = true;
    private boolean isTextPackets = false;
    
    /**
    *** Returns true if the packets are text
    *** @return True if the packets are text
    **/
    protected boolean isTextPackets() 
    {
        return this.isTextPackets;
    }
    
    // ------------------------------------------------------------------------

    private ServerSocketThread.SessionInfo sessionInfo = null;
    
    /**
    *** Sets the session info handler
    *** @param sessionInfo An implementation of the ServerSocketThread.SessionInfo interface
    **/
    public void setSessionInfo(ServerSocketThread.SessionInfo sessionInfo)
    {
        this.sessionInfo = sessionInfo;
    }
    
    /**
    *** Gets a reference to the ClientPacketHandler's session info implementation
    *** @return Reference to the session info object
    **/
    public ServerSocketThread.SessionInfo getSessionInfo()
    {
        return this.sessionInfo;
    }
    
    /**
    *** Gets the local port to which this socket is bound
    *** @return The local port to which this socket is bound
    **/
    public int getLocalPort()
    {
        return (this.sessionInfo != null)? this.sessionInfo.getLocalPort() : -1;
    }
    
    /**
    *** Gets the report port used by the client to send the received packet
    *** @return The client remote port
    **/
    public int getRemotePort()
    {
        return (this.sessionInfo != null)? this.sessionInfo.getRemotePort() : -1;
    }

    // ------------------------------------------------------------------------

    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText) 
    {
        this.inetAddr = inetAddr;
        this.isTCP = isTCP;
        this.isTextPackets = isText;
    }

    // ------------------------------------------------------------------------

    public byte[] getInitialPacket() 
        throws Exception
    {
        return null;
    }

    public byte[] getFinalPacket(boolean hasError) 
        throws Exception
    {
        return null;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the IP adress of the host
    *** @return The IP adress of the host
    **/
    public InetAddress getInetAddress()
    {
        return this.inetAddr;
    }

    /**
    *** Gets the IP adress of the host
    *** @return The IP adress of the host
    **/
    public String getHostAddress()
    {
        String ipAddr = (this.inetAddr != null)? this.inetAddr.getHostAddress() : null;
        return ipAddr;
    }

    // ------------------------------------------------------------------------

    public int getResponsePort()
    {
        return 0;
    }

    // ------------------------------------------------------------------------

    public int getMinimumPacketLength()
    {
        // '-1' indicates that 'ServerSocketThread' should be used
        return -1;
    }

    public int getMaximumPacketLength()
    {
        // '-1' indicates that 'ServerSocketThread' should be used
        return -1;
    }

    // ------------------------------------------------------------------------

    public int getActualPacketLength(byte packet[], int packetLen) 
    {
        return this.isTextPackets? PACKET_LEN_ASCII_LINE_TERMINATOR : packetLen;
    }

    // ------------------------------------------------------------------------

    public abstract byte[] getHandlePacket(byte cmd[]) 
        throws Exception;

    // ------------------------------------------------------------------------

    public boolean terminateSession() 
    {
        return true; // always terminate by default
    }

    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {
        // do nothing
    }

    // ------------------------------------------------------------------------

}
