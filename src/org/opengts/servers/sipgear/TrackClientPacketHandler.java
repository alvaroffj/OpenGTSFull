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
// Change History:
//  2010/05/24  ZhongShan SIPGEAR Technology Co, Ltd. (updated by Martin D. Flynn)
//     -Initial release
//  2010/06/17  Martin D. Flynn 
//     -Attempt to updated to handle TCP (as well as the previous UDP)
// ----------------------------------------------------------------------------
// Note:
//  - This device communication server module has been provided by the company
//    ZhongShan SIPGEAR Technology Co, Ltd.  It has been updated to support some 
//    of the latested features of OpenGTS.
//  - Geotelematic Solutions, Inc. and OpenGTS are not affiliated with ZhongShan 
//    SIPGEAR Technology Co, Ltd.
// ----------------------------------------------------------------------------
package org.opengts.servers.sipgear;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class TrackClientPacketHandler
    extends AbstractClientPacketHandler
{

    // ------------------------------------------------------------------------

    public static       String  UNIQUEID_PREFIX[]           = null;
    public static       double  MINIMUM_SPEED_KPH           = Constants.MINIMUM_SPEED_KPH;
    public static       boolean ESTIMATE_ODOMETER           = true;
    public static       boolean SIMEVENT_GEOZONES           = false;
    public static       double  MINIMUM_MOVED_METERS        = 0.0;

    // ------------------------------------------------------------------------

    /* convenience for converting knots to kilometers */
    public static final double KILOMETERS_PER_KNOT          = 1.85200000;

    // ------------------------------------------------------------------------

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone               = DateTime.getGMTTimeZone();

    // ------------------------------------------------------------------------

    /* Session 'terminate' indicator */
    private boolean         terminate                       = false;
    
    /* duplex/simplex */
    private boolean         isDuplex                        = true;
    
    /* session IP address */
    private InetAddress     inetAddress                     = null;
    private String          ipAddress                       = null;
    private int             clientPort                      = 0;

    /* session start time */
    private long            sessionStartTime                = 0L;

    /* count the number of events we've parsed during this session */
    private int             eventTotalCount                 = 0;

    /* packet handler constructor */
    public TrackClientPacketHandler() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText)
    {
        super.sessionStarted(inetAddr, isTCP, isText);

        /* init */
        this.sessionStartTime = DateTime.getCurrentTimeSec();
        this.inetAddress      = inetAddr;
        this.ipAddress        = (inetAddr != null)? inetAddr.getHostAddress() : null;
        this.clientPort       = this.getSessionInfo().getRemotePort();
        this.isDuplex         = isTCP;
        this.eventTotalCount  = 0;

        /* debug message */
        if (this.isDuplex) {
            Print.logInfo("Begin TCP communication: " + this.ipAddress);
        } else {
            Print.logInfo("Begin UDP communication: " + this.ipAddress);
        }

    }
    
    /* callback when session is terminating */
    public void sessionTerminated(Throwable err, long readCount, long writeCount)
    {

        // called before the socket is closed
        if (this.isDuplex()) {
            Print.logInfo("End TCP communication: " + this.ipAddress);
            // short pause to 'help' make sure the pending outbound data is transmitted
            try { Thread.sleep(50L); } catch (Throwable t) {}
        } else {
            Print.logInfo("End UDP communication: " + this.ipAddress);
        }

    }

    // ------------------------------------------------------------------------

    /* return true if this session is duplex (ie TCP) */
    public boolean isDuplex()
    {
        return this.isDuplex;
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen)
    {
        // minimum packet length is 12, so 'packetLen' should be 12 here
        return ServerSocketThread.PACKET_LEN_ASCII_LINE_TERMINATOR; // Remainder is an ASCII packet
      //return ServerSocketThread.PACKET_LEN_END_OF_STREAM;
    }

    // ------------------------------------------------------------------------

    /* indicate that the session should terminate */
    public boolean terminateSession()
    {
        return this.terminate;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) 
    {
        if (pktBytes == null) {
            Print.logError("Packet is null");
        } else
        if (pktBytes.length < 11) {
            Print.logError("Unexpected packet length: " + pktBytes.length);
        } else {
            // skip the first 11 bytes of this packet and parse the rest
            int ofs = 11;
            int len = pktBytes.length - ofs;
            String s = StringTools.toStringValue(pktBytes,ofs,len).trim();
            Print.logInfo("Recv: " + s); // debug message
            this.parseInsertRecord(s);
            this.eventTotalCount++;
            // the remainder of the data stream probably can be flushed
            // this.terminate = true;
        }
        return null; // no return packets are expected
    }

    // ------------------------------------------------------------------------

    /**
    *** Computes seconds in UTC time given values from GPS device.
    *** @param dmy    Date received from GPS in DDMMYY format, where DD is day, MM is month,
    ***               YY is year.
    *** @param hms    Time received from GPS in HHMMSS format, where HH is hour, MM is minute,
    ***               and SS is second.
    *** @return Time in UTC seconds.
    ***/
    private long _getUTCSeconds(long dmy, long hms)
    {
    
        /* time of day [TOD] */
        int    HH  = (int)((hms / 10000L) % 100L);
        int    MM  = (int)((hms / 100L) % 100L);
        int    SS  = (int)(hms % 100L);
        long   TOD = (HH * 3600L) + (MM * 60L) + SS;
    
        /* current UTC day */
        long DAY;
        if (dmy > 0L) {
            int    yy  = (int)(dmy % 100L) + 2000;
            int    mm  = (int)((dmy / 100L) % 100L);
            int    dd  = (int)((dmy / 10000L) % 100L);
            long   yr  = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY        = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
        } else {
            // we don't have the day, so we need to figure out as close as we can what it should be.
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= TOD)? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                // > 12 hour difference, assume we've crossed a day boundary
                if (tod > TOD) {
                    // tod > TOD likely represents the next day
                    DAY++;
                } else {
                    // tod < TOD likely represents the previous day
                    DAY--;
                }
            }
        }
        
        /* return UTC seconds */
        long sec = DateTime.DaySeconds(DAY) + TOD;
        return sec;
        
    }

    /**
    *** Parses latitude given values from GPS device.
    *** @param  s  Latitude String from GPS device in ddmm.mm format.
    *** @param  d  Latitude hemisphere, "N" for northern, "S" for southern.
    *** @return Latitude parsed from GPS data, with appropriate sign based on hemisphere or
    ***         90.0 if invalid latitude provided.
    **/
    private double _parseLatitude(String s, String d)
    {
        double _lat = StringTools.parseDouble(s, 99999.0);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equals("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
    }

    /**
    *** Parses longitude given values from GPS device.
    *** @param s Longitude String from GPS device in ddmm.mm format.
    *** @param d Longitude hemisphere, "E" for eastern, "W" for western.
    *** @return Longitude parsed from GPS data, with appropriate sign based on hemisphere or
    *** 180.0 if invalid longitude provided.
    **/
    private double _parseLongitude(String s, String d)
    {
        double _lon = StringTools.parseDouble(s, 99999.0);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equals("W")? -lon : lon;
        } else {
            return 180.0; // invalid longitude
        }
    }

    /* parse and insert data record */
    private boolean parseInsertRecord(String s)
    {
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return false;
        }

        /* parse to fields */
        String fld[] = StringTools.parseString(s, ',');
        if ((fld == null) || (fld.length < 11)) {
            Print.logWarn("Invalid number of fields");
            return false;
        }

        /* find "imei:" */
        String mobileID = null;
        for (int f = 0; f < fld.length; f++) {
            if (fld[f].startsWith("imei:")) {
                StringBuffer midSB = new StringBuffer();
                for (int c = 0; c < fld[f].length(); c++) {
                    char ch = fld[f].charAt(c);
                    if (!Character.isLetterOrDigit(ch)) { break; }
                    midSB.append(ch);
                }
                mobileID = midSB.toString();
                break;
            }
        }
        if (StringTools.isBlank(mobileID)) {
            Print.logError("'imei:' is missing");
            return false;
        }

        /* find "GPRMC" */
        int gpx = 0;
        for (; (gpx < fld.length) && !fld[gpx].equals("GPRMC"); gpx++);
        if (gpx >= fld.length) {
            Print.logError("'GPRMC' not found");
            return false;
        }
        long    hms         = StringTools.parseLong(fld[gpx + 1], 0L);
        long    dmy         = StringTools.parseLong(fld[gpx + 9], 0L);
        long    fixtime     = this._getUTCSeconds(dmy, hms);
        boolean validGPS    = fld[gpx + 2].equalsIgnoreCase("A");
        double  latitude    = validGPS? this._parseLatitude( fld[gpx + 3], fld[gpx + 4])  : 0.0;
        double  longitude   = validGPS? this._parseLongitude(fld[gpx + 5], fld[gpx + 6])  : 0.0;
        double  knots       = validGPS? StringTools.parseDouble(fld[gpx + 7], -1.0) : 0.0;
        double  headingDeg  = validGPS? StringTools.parseDouble(fld[gpx + 8], -1.0) : 0.0;
        double  speedKPH    = (knots >= 0.0)? (knots * KILOMETERS_PER_KNOT)   : -1.0;
        double  altitudeM   = 0.0;
        double  odomKM      = 0.0;
        int     statusCode  = StatusCodes.STATUS_LOCATION;

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date: " + fld[gpx + 9] + "/" + fld[gpx + 1]);
            fixtime = DateTime.getCurrentTimeSec(); // default to now
        }

        /* valid lat/lon? */
        if (validGPS &&
            ((latitude  >=  90.0) || (latitude  <=  -90.0) ||
             (longitude >= 180.0) || (longitude <= -180.0)   )) {
            Print.logWarn("Invalid GPRMC lat/lon: " + latitude + "/" + longitude);
            latitude  = 0.0;
            longitude = 0.0;
            validGPS  = false;
        }
        GeoPoint geoPoint = new GeoPoint(latitude, longitude);

        /* adjustments to received values */
        if (speedKPH < MINIMUM_SPEED_KPH) {
            speedKPH   = 0.0;
            headingDeg = 0.0;
        } else
        if (headingDeg < 0.0) {
            headingDeg = 0.0;
        }

        /* parsed data */
        Print.logInfo("IMEI     : " + mobileID);
        Print.logInfo("Timestamp: " + fixtime + " [" + new DateTime(fixtime) + "]");
        Print.logInfo("GPS      : " + geoPoint);
        Print.logInfo("Speed    : " + StringTools.format(speedKPH,"#0.0") + " kph " + headingDeg);

        /* find Device */
        Device device = DCServerFactory.loadDeviceUniqueID(UNIQUEID_PREFIX, mobileID);
        if (device == null) {
            return false; // errors already displayed
        }
        String accountID = device.getAccountID();
        String deviceID  = device.getDeviceID();
        String uniqueID  = device.getUniqueID();
        Print.logInfo("UniqueID  : " + uniqueID);
        Print.logInfo("DeviceID  : " + accountID + "/" + deviceID);

        /* check IP address */
        DataTransport dataXPort = device.getDataTransport();
        if ((this.ipAddress != null) && !dataXPort.isValidIPAddress(this.ipAddress)) {
            DTIPAddrList validIPAddr = dataXPort.getIpAddressValid(); // may be null
            Print.logError("Invalid IP Address from device: " + this.ipAddress + " [expecting " + validIPAddr + "]");
            return false;
        }
        dataXPort.setIpAddressCurrent(this.ipAddress);    // FLD_ipAddressCurrent
        dataXPort.setRemotePortCurrent(this.clientPort);  // FLD_remotePortCurrent
        dataXPort.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime
        if (!dataXPort.getDeviceCode().equalsIgnoreCase(Main.getServerName())) {
            dataXPort.setDeviceCode(Main.getServerName()); // FLD_deviceCode
        }

        /* estimate GPS-based odometer */
        if (odomKM <= 0.0) {
            // calculate odometer
            odomKM = (ESTIMATE_ODOMETER && validGPS)? 
                device.getNextOdometerKM(geoPoint) : 
                device.getLastOdometerKM();
        } else {
            // bounds-check odometer
            odomKM = device.adjustOdometerKM(odomKM);
        }
        Print.logInfo("OdometerKM: " + odomKM);

        /* create event */
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb = evKey.getDBRecord();
        evdb.setLatitude(latitude);
        evdb.setLongitude(longitude);
        evdb.setHeading(headingDeg);
        evdb.setSpeedKPH(speedKPH);
        evdb.setAltitude(altitudeM);
        evdb.setOdometerKM(odomKM);

        /* insert event */
        // this will display an error if it was unable to store the event
        device.insertEventData(evdb);

        /* save device changes */
        try {
            //DBConnection.pushShowExecutedSQL();
            device.updateChangedEventFields();
        } catch (DBException dbe) {
            Print.logException("Unable to update Device: " + accountID + "/" + deviceID, dbe);
        } finally {
            //DBConnection.popShowExecutedSQL();
        }

        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void configInit() 
    {

        // common
        DCServerConfig dcsc     = Main.getServerConfig();
        UNIQUEID_PREFIX         = dcsc.getUniquePrefix();
        MINIMUM_SPEED_KPH       = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
        ESTIMATE_ODOMETER       = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
        SIMEVENT_GEOZONES       = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
        MINIMUM_MOVED_METERS    = dcsc.getMinimumMovedMeters(MINIMUM_MOVED_METERS);

    }

}
