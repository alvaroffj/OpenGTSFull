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
//  Template data packet 'business' logic.
//  This module is an *example* of how client data packets can be parsed and
//  inserted into the EventData table.  Since every device protocol is different,
//  significant changes will likely be necessary to support the protocol used by
//  your chosen device.
// ----------------------------------------------------------------------------
// Notes:
// - See the OpenGTS_Config.pdf document for additional information regarding the
//   implementation of a device communication server.
// - Implementing a device communication server for your chosen device may take a
//   signigicant and substantial amount of programming work to accomplish, depending
//   on the device protocol.  To implement a server, you will likely need an in-depth
//   understanding of TCP/UDP based communication, and a good understanding of Java
//   programming techniques, including socket communication and multi-threading.
// - The first and most important step when starting to implement a device
//   communication server for your chosen device is to obtain and fully understand
//   the protocol documentation from the manufacturer of the device.  Attempting to
//   reverse-engineer a raw-socket base protocol can prove extremely difficult, if
//   not impossible, without proper protocol documentation.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// Change History:
//  2006/06/30  Martin D. Flynn
//     -Initial release
//  2007/07/27  Martin D. Flynn
//     -Moved constant information to 'Constants.java'
//  2007/08/09  Martin D. Flynn
//     -Added additional help/comments.
//     -Now uses "imei_" as the primary IMEI prefix for the unique-id when
//      looking up the Device record (for data format example #1)
//     -Added a second data format example (#2) which includes the parsing of a
//      standard $GPRMC NMEA-0183 record.
//  2008/02/17  Martin D. Flynn
//     -Added additional help/comments.
//  2008/03/12  Martin D. Flynn
//     -Added ability to compute a GPS-based odometer value
//     -Added ability to update the Device record with IP-address and last connect time.
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/06/20  Martin D. Flynn
//     -Added some additional comments regarding the use of 'terminate' and 'terminateSession()'.
//  2008/12/01  Martin D. Flynn
//     -Added entry point for parsing GPS packet data store in a flat file.
//  2009/04/02  Martin D. Flynn
//     -Changed default for 'INSERT_EVENT' to true
//  2009/05/27  Martin D. Flynn
//     -Added changes for estimated odometer calculations, and simulated geozones
//  2009/06/01  Martin D. Flynn
//     -Updated to utilize Device gerozone checks
//  2009/08/07  Martin D. Flynn
//     -Updated to use "DCServerConfig" and "GPSEvent"
//  2009/10/02  Martin D. Flynn
//     -Modified to describe how to return ACK packets back to the device.
//     -Added parser for RTProperties String (format #3)
// ----------------------------------------------------------------------------
package org.opengts.servers.GTX;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.text.SimpleDateFormat;
import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.servers.*;

public class TrackClientPacketHandler
        extends AbstractClientPacketHandler {

    // ------------------------------------------------------------------------
    // This data parsing template contains *examples* of 2 different ASCII data formats:
    //
    // Example #1: (see "parseInsertRecord_ASCII_1")
    //   123456789012345,2006/09/05,07:47:26,35.3640,-141.2958,27.0,224.8
    //
    // Example #2: (see "parseInsertRecord_ASCII_2")
    //   account/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,0.094824,108.52,200505,,*12
    //
    // Example #3: (see "parseInsertRecord_RTProps")
    //   mid=123456789012345 ts=1254100914 code=0xF100 gps=39.1234/-142.1234 kph=45.6 dir=123 odom=1234.5
    //
    // These are only *examples* of an ASCII encoded data protocol.  Since this 'template'
    // cannot anticipate every possible ASCII/Binary protocol that may be encounted, this
    // module should only be used as an *example* of how a device communication server might
    // be implemented.  The implementation of a device communication server for your chosen
    // device may take a signigicant and substantial amount of programming work to accomplish,
    // depending on the device protocol.
    public static int DATA_FORMAT_OPTION = 1;
    // ------------------------------------------------------------------------

    /* estimate GPS-based odometer */
    // (enable to include estimated GPS-based odometer values on EventData records)
    // Note:
    //  - Enabling this feature may cause an extra query to the EventData table to obtain
    //    the previous EventData record, from which it will calculate the distance between
    //    this prior point and the current point.  This means that the GPS "dithering"
    //    thich can occur when a vehicle is stopped will cause the calculated odometer
    //    value to increase even when the vehicle is not moving.  You may wish to add some
    //    additional logic to mitigate this particular behavior.
    //  - The accuracy of a GPS-based odometer calculation varies greatly depending on
    //    factors such as the accuracy of the GPS receiver (ie. WAAS, DGPS, etc), the time
    //    interval between generated "in-motion" events, and how straight or curved the
    //    road is.  Typically, a GPS-based odometer tends to under-estimate the actual
    //    vehicle value.
    public static boolean ESTIMATE_ODOMETER = false;
    /* simulate geozone arrival/departure */
    // (enable to insert simulated Geozone arrival/departure EventData records)
    public static boolean SIMEVENT_GEOZONES = false;
    /* simulate digital input changes */
    public static long SIMEVENT_DIGITAL_INPUTS = 0x0000L; // 0xFFFFL;

    /* flag indicating whether data should be inserted into the DB */
    // should be set to 'true' for production.
    private static boolean DFT_INSERT_EVENT = true;
    private static boolean INSERT_EVENT = DFT_INSERT_EVENT;

    /* update Device record */
    // (enable to update Device record with current IP address and last connect time)
    private static boolean UPDATE_DEVICE = false;
    /* minimum acceptable speed value */
    // Speeds below this value should be considered 'stopped'
    public static double MINIMUM_SPEED_KPH = 0.0;
    // ------------------------------------------------------------------------

    /* Ingore $GPRMC checksum? */
    // (only applicable for data formats that include NMEA-0183 formatted event records)
    private static boolean IGNORE_NMEA_CHECKSUM = false;
    // ------------------------------------------------------------------------

    /* GMT/UTC timezone */
    private static final TimeZone gmtTimezone = DateTime.getGMTTimeZone();
    // ------------------------------------------------------------------------

    /* GTS status codes for Input-On events */
    private static final int InputStatusCodes_ON[] = new int[]{
        StatusCodes.STATUS_INPUT_ON_00,
        StatusCodes.STATUS_INPUT_ON_01,
        StatusCodes.STATUS_INPUT_ON_02,
        StatusCodes.STATUS_INPUT_ON_03,
        StatusCodes.STATUS_INPUT_ON_04,
        StatusCodes.STATUS_INPUT_ON_05,
        StatusCodes.STATUS_INPUT_ON_06,
        StatusCodes.STATUS_INPUT_ON_07,
        StatusCodes.STATUS_INPUT_ON_08,
        StatusCodes.STATUS_INPUT_ON_09,
        StatusCodes.STATUS_INPUT_ON_10,
        StatusCodes.STATUS_INPUT_ON_11,
        StatusCodes.STATUS_INPUT_ON_12,
        StatusCodes.STATUS_INPUT_ON_13,
        StatusCodes.STATUS_INPUT_ON_14,
        StatusCodes.STATUS_INPUT_ON_15
    };

    /* GTS status codes for Input-Off events */
    private static final int InputStatusCodes_OFF[] = new int[]{
        StatusCodes.STATUS_INPUT_OFF_00,
        StatusCodes.STATUS_INPUT_OFF_01,
        StatusCodes.STATUS_INPUT_OFF_02,
        StatusCodes.STATUS_INPUT_OFF_03,
        StatusCodes.STATUS_INPUT_OFF_04,
        StatusCodes.STATUS_INPUT_OFF_05,
        StatusCodes.STATUS_INPUT_OFF_06,
        StatusCodes.STATUS_INPUT_OFF_07,
        StatusCodes.STATUS_INPUT_OFF_08,
        StatusCodes.STATUS_INPUT_OFF_09,
        StatusCodes.STATUS_INPUT_OFF_10,
        StatusCodes.STATUS_INPUT_OFF_11,
        StatusCodes.STATUS_INPUT_OFF_12,
        StatusCodes.STATUS_INPUT_OFF_13,
        StatusCodes.STATUS_INPUT_OFF_14,
        StatusCodes.STATUS_INPUT_OFF_15
    };
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Session 'terminate' indicator */
    // This value should be set to 'true' when this server has determined that the
    // session should be terminated.  For instance, if this server finishes communication
    // with the device or if parser finds a fatal error in the incoming data stream
    // (ie. invalid account/device, or unrecognizable data).
    private boolean terminate = false;

    /* duplex/simplex */
    // This value will be set for you by the incoming session to indicate whether
    // the session is TCP (duplex) or UDP (simplex).
    private boolean isDuplex = true;

    /* session IP address */
    // These values will be set for you by the incoming session to indicate the
    // originating IP address.
    private InetAddress inetAddress = null;
    private String ipAddress = null;
    private int clientPort = 0;

    /* packet handler constructor */
    public TrackClientPacketHandler() {
        super();
    }

    // ------------------------------------------------------------------------

    /* callback when session is starting */
    // this method is called at the beginning of a communication session
    public void sessionStarted(InetAddress inetAddr, boolean isTCP, boolean isText) {
        super.sessionStarted(inetAddr, isTCP, isText);

        /* init */
        this.inetAddress = inetAddr;
        this.ipAddress = (inetAddr != null) ? inetAddr.getHostAddress() : null;
        this.clientPort = this.getSessionInfo().getRemotePort();
        this.isDuplex = isTCP;

        /* debug message */
        if (this.isDuplex) {
//            Print.logInfo("Begin TCP communication: " + this.ipAddress + " [" + new DateTime() + "]");
        } else {
//            Print.logInfo("Begin UDP communication: " + this.ipAddress + " [" + new DateTime() + "]");
        }

    }

    /* callback when session is terminating */
    // this method is called at the end of a communication session
    public void sessionTerminated(Throwable err, long readCount, long writeCount) {

        // called before the socket is closed
        if (this.isDuplex()) {
            Print.logInfo("End TCP communication: " + this.ipAddress);
            // short pause to 'help' make sure the pending outbound data is transmitted
            try {
                Thread.sleep(50L);
            } catch (Throwable t) {
            }
        } else {
            Print.logInfo("End UDP communication: " + this.ipAddress);
        }

    }

    // ------------------------------------------------------------------------

    /* returns true if this session is duplex (ie TCP), false if simplex (ie UDP) */
    public boolean isDuplex() {
        return this.isDuplex;
    }

    // ------------------------------------------------------------------------

    /* based on the supplied packet data, return the remaining bytes to read in the packet */
    public int getActualPacketLength(byte packet[], int packetLen) {
        // (This method is only called if "Constants.ASCII_PACKETS" is false!)
        //
        // This method is possibly the most important part of a server protocol implementation.
        // The length of the incoming client packet must be correctly identified in order to
        // know how many incoming packet bytes should be read.
        //
        // 'packetLen' will be the value specified by Constants.MIN_PACKET_LENGTH, and should
        // be the minimum number of bytes required (but not more) to accurately determine what
        // the total length of the incoming client packet will be.  After analyzing the initial
        // bytes of the packet, this method should return what it beleives to be the full length
        // of the client data packet, including the length of these initial bytes.
        //
        // For example:
        //   Assume that all client packets have the following binary format:
        //      Byte  0    - packet type
        //      Byte  1    - payload length (ie packet data)
        //      Bytes 2..X - payload data (as determined by the payload length byte)
        // In this case 'Constants.ASCII_PACKETS' should be set to 'false', and
        // 'Constants.MIN_PACKET_LENGTH' should be set to '2' (the minimum number of bytes
        // required to determine the actual packet length).  This method should then return
        // the following:
        //      return 2 + ((int)packet[1] & 0xFF);
        // Which is the packet header length (2 bytes) plus the remaining length of the data
        // payload found in the second byte of the packet header.
        //
        // Note that the integer cast and 0xFF mask is very important.  'byte' values in
        // Java are signed, thus the byte 0xFF actually represents a '-1'.  So if the packet
        // payload length is 128, then without the (int) cast and mask, the returned value
        // would end up being -126.
        // IE:
        //    byte b = (byte)128;  // this is actually a signed '-128'
        //    System.out.println("1: " + (2+b)); // this casts -128 to an int, and adds 2
        //    System.out.println("2: " + (2+((int)b&0xFF)));
        // The above would print the following:
        //    1: -126
        //    2: 130
        //
        // Once the full client packet is read, it will be delivered to the 'getHandlePacket'
        // method below.
        //
        // WARNING: If a packet length value is returned here that is greater than what the
        // client device will actually be sending, then the server will receive a read timeout,
        // and this error may cause the socket connection to be closed.  If you happen to see
        // read timeouts occuring during testing/debugging, then it is likely that this method
        // needs to be adjusted to properly identify the client packet length.
        //
        if (Constants.ASCII_PACKETS) {
            // (this actually won't be called if 'Constants.ASCII_PACKETS' is true).
            // ASCII packets - look for line terminator [see Constants.ASCII_LINE_TERMINATOR)]
            return ServerSocketThread.PACKET_LEN_ASCII_LINE_TERMINATOR;  // read until line termination character
            //return ServerSocketThread.PACKET_LEN_END_OF_STREAM;  // read until end of stream, or maxlen
        } else {
            // BINARY packet - need to analyze 'packet[]' and determine actual packet length
            return ServerSocketThread.PACKET_LEN_ASCII_LINE_TERMINATOR; // <-- change this for binary packets
        }

    }

    // ------------------------------------------------------------------------

    /* set session terminate after next packet handling */
    private void setTerminate() {
        this.terminate = true;
    }

    /* indicate that the session should terminate */
    // This method is called after each return from "getHandlePacket" to check to see
    // the current session should be closed.
    public boolean terminateSession() {
        return this.terminate;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return the initial packet sent to the device after session is open */
    public byte[] getInitialPacket()
            throws Exception {
        // At this point a connection from the client to the server has just been
        // initiated, and we have not yet received any data from the client.
        // If the client is expecting to receive an initial packet from the server at
        // the time that the client connects, then this is where the server can return
        // a byte array that will be transmitted to the client device.
        return null;
        // Note: any returned response for "getInitialPacket()" is ignored for simplex/udp connections.
        // Returned UDP packets may be sent from "getHandlePacket" or "getFinalPacket".
    }

    /* workhorse of the packet handler */
    public byte[] getHandlePacket(byte pktBytes[]) {

        // After determining the length of a client packet (see method 'getActualPacketLength'),
        // this method is called with the single packet which has been read from the client.
        // It is the responsibility of this method to determine what type of packet was received
        // from the client, parse/insert any event data into the tables, and return any expected
        // response that the client may be expected in the form of a byte array.
        if ((pktBytes != null) && (pktBytes.length > 0)) {

            /* (debug message) display received data packet */
//            Print.logInfo("Recv[HEX]: " + StringTools.toHexString(pktBytes));
            String s = StringTools.toStringValue(pktBytes).trim(); // remove leading/trailing spaces
//            Print.logInfo("Recv[TXT]: " + s); // debug message

            /* parse/insert event */
            byte rtn[] = null;
            switch (DATA_FORMAT_OPTION) {
                case 1:
                    rtn = this.parseInsertRecord_ASCII_1(s);
                    break;
                case 2:
                    rtn = this.parseInsertRecord_ASCII_2(s);
                    break;
                case 3:
                    rtn = this.parseInsertRecord_RTProps(s);
                    break;
                default:
                    Print.logError("Unspecified data format");
                    break;
            }
            // Note:
            // The above examples assume ASCII data.  If the data arrives as a binary data packet,
            // the utility class "org.opengts.util.Payload" can be used to parse the binary data:
            // For example:
            //   Assume 'pktBytes' contains the following binary hex data:
            //      01 02 03 04 05 06 07 08 09 0A 0B
            //   One way to parse this binary data would be as follows:
            //      Payload p = new Payload(pktBytes);
            //      int fld_1 = (int)p.readLong(3,0L);   // parse 0x010203   into 'fld_1'
            //      int fld_2 = (int)p.readLong(4,0L);   // parse 0x04050607 into 'fld_2'
            //      int fld_3 = (int)p.readLong(2,0L);   // parse 0x00809    into 'fld_2'
            //      int fld_4 = (int)p.readLong(2,0L);   // parse 0x0A0B     into 'fld_2'

            /* return response */
            // If the client is expecting to receive a response from the server (such as an
            // acknowledgement), this is where the server should compose a returned response
            // in the form of an array of bytes which should be returned here.  This byte array
            // will then be transmitted back to the client.
            return rtn; // no return packets are expected

        } else {

            /* no packet date received */
            Print.logInfo("Empty packet received ...");
            return null; // no return packets are expected

        }

        // when this method returns, the server framework then starts the process over again
        // attempting to read another packet from the client device (see method 'getActualPacketLength').
        // If this server determines that communicqtion with the client device has completed, then
        // the above "terminateSession" method should return true [the method "setTerminate()" is
        // provided to facilitate session termination - see "setTerminate" above].

    }

    /* final packet sent to device before session is closed */
    public byte[] getFinalPacket(boolean hasError)
            throws Exception {
        // If the server wishes to send a final packet to the client just before the connection
        // is closed, then this is where the server should compose the final packet, and return
        // this packet in the form of a byte array.  This byte array will then be transmitted
        // to the client device before the session is closed.
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private byte[] parseInsertRecord_ASCII_1(String s) {
        // This is an example showing how the server might parse one type of ASCII encoded data.
        // Since every device utilizes a different data format, this will likely not match the
        // format coming from your chosen device and may need some significant changes to support
        // the format provided by your device (assuming that the format is even ASCII).

        // This parsing method assumes the data format appears as follows:
        //      0             1        2       3         4       5     6
        // <IMEI number>,2006/09/05,07:47:26,35.3640,-141.2958,27.0,224.8
        //   0 - IMEI unique ID
        //   1 - Date [GMT]
        //   2 - Time [GMT]
        //   3 - Latitude
        //   4 - Longitude
        //   5 - Speed (kph)
        //   6 - Heading (degrees)
		/*
        0										1										2	3	4		5				6			7		8			9	10	  11	12	 13
        $$B0353358019462410|AA$GPRMC,102156.000,A,2232.4690,N,11403.6847,E,0.00,,180909,,*15|02.0|01.2|01.6|000000001010|20090918102156|14181353|00000000|279311AA|0000|0.7614|0080|D2B5

        /*

         */

       Print.logInfo("Parsear: " + s);
       Print.logInfo("Largo cadena :" + s.length());
    //    String a = StringTools.parseHex(s);
        /* pre-validate */
        if (s == null) {
            Print.logError("Cadena nula");
            return null;
        }

        /* parse to fields */
        // String fld[] = StringTools.parseString(s, '|');
        String ini = s.substring(0, 2);
        Print.logInfo("Ini :" + ini);
        String l = s.substring(2, 4);
        l = StringTools.parseHex(l, null);
        Print.logInfo("l :" + l);
        String id = s.substring(4, 11);
        id = StringTools.parseHex(id, null);
        Print.logInfo("id :" + id);
        String com = s.substring(11, 13);
        com = StringTools.parseHex(com, null);
        Print.logInfo("com :" + com);

//         if ((fld == null) || (fld.length < 14)) {
//             Print.logWarn("Numero invalido de campos: ");
//             return null;
//         }
//         /* parsear campo 0 , $$, tamanno e IMEI*/
//         //String   campo0		= fld[0].toLowerCase();
//         //String   accountID  = fld[0].toLowerCase();
//         String IMEI = fld[0].substring(4, fld[0].length());
//         /* parse individual fields */
//         Print.logInfo("Largo cadena :" + s.length());
//         /* parse individual fields */
// //        Print.logWarn("s: " + s);
// //        Print.logWarn("lala: " + fld[1]);
//         String iniGPS = fld[1].substring(2, 3);
// //        Print.logWarn("iniGPS: " + iniGPS);
//         long fixtime;
//         int statusCode;
//         double latitude;
//         double longitude;
//         double speedKPH;
//         double heading;
//         double altitudeM = 0.0;  //
//         String gprmcparcial;
//         String gprmccortado[];
//         String horautc;
//         String fechautc;
//         if (iniGPS.compareTo("$") == 0) {
//             Nmea0183 gprmc = new Nmea0183(fld[1].substring(2, fld[1].length() - 1), true);
//             fixtime = gprmc.getFixtime();
//             statusCode = StatusCodes.STATUS_LOCATION;
//             latitude = gprmc.getLatitude();
//             longitude = gprmc.getLongitude();
//             speedKPH = gprmc.getSpeedKPH();
//             heading = gprmc.getHeading();
//             gprmcparcial = fld[1].substring(2, fld[1].length() - 1);
//             gprmccortado = StringTools.parseString(gprmcparcial, ',');
//             horautc = gprmccortado[1];
//             fechautc = gprmccortado[9];
//         } else {
//             Print.logWarn("Sin senal GPS");
//             fixtime = 0x0;
//             latitude = 0.0;
//             longitude = 0.0;
//             speedKPH = 0.0;
//             heading = 0.0;
//             gprmcparcial = "";
//             gprmccortado = null;
//             horautc = "";
//             fechautc = "";
//         }
//             //       Nmea0183 gprmc      = new Nmea0183(fld[1].substring(2,fld[1].length()-1), IGNORE_NMEA_CHECKSUM);
//
//         String aa = fld[1].substring(0, 2);
//         String mileage = fld[11];
//         String temp = fld[10];
//         String sa = fld[5];
//         String sd = fld[8];
//
//
//         /* no deviceID? */
//         if (StringTools.isBlank(IMEI)) {
//             Print.logWarn("DeviceID not specified!");
//             return null;
//         }
//
//         String url = "http://localhost/backend/alerta.php?"
//                 + "IMEI=" + IMEI + ""
//                 + "&statusCode=61472&"
//                 + "fixtime=" + fixtime + ""
//                 + "&horautc=" + horautc + ""
//                 + "&fechautc=" + fechautc + ""
//                 + "&fechafullutc=" + fld[6] + ""
//                 + "&latitude=" + latitude + ""
//                 + "&longitude=" + longitude + ""
//                 + "&gpsAge=0"
//                 + "&speedKPH=" + speedKPH + ""
//                 + "&heading=" + heading + ""
//                 + "&altitude=" + altitudeM + ""
//                 + "&distanceKM=0"
//                 + "&odometerKM=" + mileage + ""
//                 + "&aa=" + aa + ""
//                 + "&temp=" + temp + ""
//                 + "&sa=" + sa + ""
//                 + "&sd=" + sd + ""
//                 + "&rawData=" + s;
//
//         Print.logInfo("Enviando :" + url);
//         try {
//             lanzar(url);
//         } catch (MalformedURLException e) {
//             Print.logInfo(e.getMessage());
//         } catch (IOException e) {
//             Print.logInfo(e.getMessage());
//         }
        return null;

    }

    private void lanzar(String url1) throws MalformedURLException, IOException {

        URL url = new URL(url1);
        URLConnection con = url.openConnection();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String linea;
        while ((linea = in.readLine()) != null) {
            System.out.println(linea);
        }
    }

    /* parse the specified date into unix 'epoch' time */
    private long _parseDate(String ymd, String hms) {
        // "YYYY/MM/DD", "hh:mm:ss"
        String d[] = StringTools.parseString(ymd, "/");
        String t[] = StringTools.parseString(hms, ":");
        if ((d.length != 3) && (t.length != 3)) {
            //Print.logError("Invalid date: " + ymd + ", " + hms);
            return 0L;
        } else {
            int YY = StringTools.parseInt(d[0], 0); // 07 year
            int MM = StringTools.parseInt(d[1], 0); // 04 month
            int DD = StringTools.parseInt(d[2], 0); // 18 day
            int hh = StringTools.parseInt(t[0], 0); // 01 hour
            int mm = StringTools.parseInt(t[1], 0); // 48 minute
            int ss = StringTools.parseInt(t[2], 0); // 04 second
            if (YY < 100) {
                YY += 2000;
            }
            DateTime dt = new DateTime(gmtTimezone, YY, MM, DD, hh, mm, ss);
            return dt.getTimeSec();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record (common) */
    private boolean parseInsertRecord_Common(GPSEvent gpsEvent) {
        long fixtime = gpsEvent.getTimestamp();
        int statusCode = gpsEvent.getStatusCode();

        /* invalid date? */
        if (fixtime <= 0L) {
            Print.logWarn("Invalid date/time");
            fixtime = DateTime.getCurrentTimeSec(); // default to now
            gpsEvent.setTimestamp(fixtime);
        }

        /* valid lat/lon? */
        if (!gpsEvent.isValidGeoPoint()) {
            Print.logWarn("Invalid GPRMC lat/lon: " + gpsEvent.getLatitude() + "/" + gpsEvent.getLongitude());
            gpsEvent.setLatitude(0.0);
            gpsEvent.setLongitude(0.0);
        }
        GeoPoint geoPoint = gpsEvent.getGeoPoint();

        /* minimum speed */
        if (gpsEvent.getSpeedKPH() < MINIMUM_SPEED_KPH) {
            gpsEvent.setSpeedKPH(0.0);
            gpsEvent.setHeading(0.0);
        }

        /* estimate GPS-based odometer */
        Device device = gpsEvent.getDevice();
        double odomKM = 0.0; // set to available odometer from event record
        if (odomKM <= 0.0) {
            odomKM = (ESTIMATE_ODOMETER && geoPoint.isValid())
                    ? device.getNextOdometerKM(geoPoint)
                    : device.getLastOdometerKM();
        } else {
            odomKM = device.adjustOdometerKM(odomKM);
        }
        gpsEvent.setOdometerKM(odomKM);

        /* simulate Geozone arrival/departure */
        if (SIMEVENT_GEOZONES && geoPoint.isValid()) {
            java.util.List<Device.GeozoneTransition> zone = device.checkGeozoneTransitions(fixtime, geoPoint);
            if (zone != null) {
                for (Device.GeozoneTransition z : zone) {
                    gpsEvent.insertEventData(z.getTimestamp(), z.getStatusCode());
                    Print.logInfo("Geozone    : " + z);
                }
            }
        }

        /* digital input change events */
        if (gpsEvent.hasInputMask() && (gpsEvent.getInputMask() >= 0L)) {
            long gpioInput = gpsEvent.getInputMask();
            if (SIMEVENT_DIGITAL_INPUTS > 0L) {
                // The current input state is compared to the last value stored in the Device record.
                // Changes in the input state will generate a synthesized event.
                long chgMask = (device.getLastInputState() ^ gpioInput) & SIMEVENT_DIGITAL_INPUTS;
                if (chgMask != 0L) {
                    // an input state has changed
                    for (int b = 0; b <= 15; b++) {
                        long m = 1L << b;
                        if ((chgMask & m) != 0L) {
                            // this bit changed
                            int inpCode = ((gpioInput & m) != 0L) ? InputStatusCodes_ON[b] : InputStatusCodes_OFF[b];
                            long inpTime = fixtime;
                            gpsEvent.insertEventData(inpTime, inpCode);
                            Print.logInfo("GPIO : " + StatusCodes.GetDescription(inpCode, null));
                        }
                    }
                }
            }
            device.setLastInputState(gpioInput & 0xFFFFL); // FLD_lastInputState
        }

        /* create/insert standard event */
        gpsEvent.insertEventData(fixtime, statusCode);

        /* save device changes */
        gpsEvent.updateDevice();

        /* return success */
        return true;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse and insert data record */
    private byte[] parseInsertRecord_ASCII_2(String s) {
        // This is an example showing how the server might parse one type of ASCII encoded data.
        // Since every device utilizes a different data format, this will likely not match the
        // format coming from your chosen device and may need some significant changes to support
        // the format provided by your device (assuming that the format is even ASCII).

        // This parsing method assumes the data format appears as follows:
        //      0          1      2 ...
        // <AccountID>/<DeviceID>/$GPRMC,025423.494,A,3709.0642,N,11907.8315,W,0.094824,108.52,200505,,*12
        //   0 - Account ID
        //   1 - Device ID
        //   2 - $GPRMC record ...
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (s == null) {
            Print.logError("String is null");
            return null;
        }

        /* parse to fields */
        String fld[] = StringTools.parseString(s, '/');
        if ((fld == null) || (fld.length < 3)) {
            Print.logWarn("Invalid number of fields");
            return null;
        }

        /* parse individual fields */
        String accountID = fld[0].toLowerCase();
        String deviceID = fld[1].toLowerCase();
        Nmea0183 gprmc = new Nmea0183(fld[2], IGNORE_NMEA_CHECKSUM);
        long fixtime = gprmc.getFixtime();
        int statusCode = StatusCodes.STATUS_LOCATION;
        double latitude = gprmc.getLatitude();
        double longitude = gprmc.getLongitude();
        double speedKPH = gprmc.getSpeedKPH();
        double heading = gprmc.getHeading();
        double altitudeM = 0.0;  //

        /* no deviceID? */
        if (StringTools.isBlank(deviceID)) {
            Print.logWarn("DeviceID not specified!");
            return null;
        }

        /* GPS Event */
        GPSEvent gpsEvent = new GPSEvent(Main.getServerConfig(),
                this.ipAddress, this.clientPort, accountID, deviceID);
        Device device = gpsEvent.getDevice();
        if (device == null) {
            // errors already displayed
            return null;
        }
        gpsEvent.setTimestamp(fixtime);
        gpsEvent.setStatusCode(statusCode);
        gpsEvent.setLatitude(latitude);
        gpsEvent.setLongitude(longitude);
        gpsEvent.setSpeedKPH(speedKPH);
        gpsEvent.setHeading(heading);
        gpsEvent.setAltitude(altitudeM);

        /* insert/return */
        if (this.parseInsertRecord_Common(gpsEvent)) {
            // change this to return any required acknowledgement (ACK) packets back to the device
            return null;
        } else {
            return null;
        }

    }
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    private static String RTP_ACCOUNT[] = new String[]{"acct", "accountid"};
    private static String RTP_DEVICE[] = new String[]{"dev", "deviceid"};
    private static String RTP_MODEMID[] = new String[]{"mid", "modemid", "uniqueid", "imei"};
    private static String RTP_TIMESTAMP[] = new String[]{"ts", "timestamp", "time"};
    private static String RTP_STATUSCODE[] = new String[]{"code", "statusCode"};
    private static String RTP_GEOPOINT[] = new String[]{"gps", "geopoint"};
    private static String RTP_GPSAGE[] = new String[]{"age", "gpsAge"};
    private static String RTP_SATCOUNT[] = new String[]{"sats", "satCount"};
    private static String RTP_SPEED[] = new String[]{"kph", "speed", "speedKph"};
    private static String RTP_HEADING[] = new String[]{"dir", "heading"};
    private static String RTP_ALTITUDE[] = new String[]{"alt", "altm", "altitude"};
    private static String RTP_ODOMETER[] = new String[]{"odom", "odometer"};
    private static String RTP_INPUTMASK[] = new String[]{"gpio", "inputMask"};
    private static String RTP_SERVERID[] = new String[]{"dcs", "serverid"};
    private static String RTP_ACK[] = new String[]{"ack"};
    private static String RTP_NAK[] = new String[]{"nak"};

    /* parse and insert data record */
    private byte[] parseInsertRecord_RTProps(String s) {
        // This is an example showing how another parsing server might transfer data to this
        // server, using the following simple (and extensible) format:
        //   mid=123456789012345 ts=1254100914 code=0xF020 gps=39.1234/-142.1234 kph=45.6 dir=123 alt=1234 odom=1234.5

        // The following data field are supported:
        //   mid   = Mobile-ID (typically the IMEI#)
        //   ts    = Timestamp (in Unix Epoch format)
        //   code  = The status code
        //   gps   = the latitude/logitude
        //   age   = age of GPS fix in seconds
        //   sats  = number of satellites
        //   kph   = Vehicle speed in km/h
        //   dir   = Vehicle heading in degrees
        //   alt   = Altitude in meters
        //   odom  = Vehicle odometer (if available)
        //   gpio  = Input mask
        //   ack   = Acknowledgement to return to the device on successful parsing
        //   nak   = Negative-acknowledgement to return to the device on error
        Print.logInfo("Parsing: " + s);

        /* pre-validate */
        if (StringTools.isBlank(s)) {
            Print.logError("Packet string is blank/null");
            return null;
        }

        /* parse */
        RTProperties rtp = new RTProperties(s);
        String accountID = rtp.getString(RTP_ACCOUNT, null);
        String deviceID = rtp.getString(RTP_DEVICE, null);
        String mobileID = rtp.getString(RTP_MODEMID, null);
        long fixtime = rtp.getLong(RTP_TIMESTAMP, 0L);
        int statusCode = rtp.getInt(RTP_STATUSCODE, StatusCodes.STATUS_LOCATION);
        String gpsStr = rtp.getString(RTP_GEOPOINT, null);
        long gpsAge = rtp.getLong(RTP_GPSAGE, 0L);
        int satCount = rtp.getInt(RTP_SATCOUNT, 0);
        double speedKPH = rtp.getDouble(RTP_SPEED, 0.0);
        double heading = rtp.getDouble(RTP_HEADING, 0.0);
        double altitudeM = rtp.getDouble(RTP_ALTITUDE, 0.0);
        double odomKM = rtp.getDouble(RTP_ODOMETER, 0.0);
        long gpioInput = rtp.getLong(RTP_INPUTMASK, -1L);
        String dcsid = rtp.getString(RTP_SERVERID, null);
        String ack = rtp.getString(RTP_ACK, null);
        String nak = rtp.getString(RTP_NAK, null);
        GeoPoint geoPoint = new GeoPoint(gpsStr);

        /* no mobileID? */
        if (StringTools.isBlank(mobileID)) {
            Print.logError("UniqueID/ModemID not specified!");
            return (nak != null) ? (nak + "\n").getBytes() : null;
        }

        /* DCServer */
        String dcsName = !StringTools.isBlank(dcsid) ? dcsid : Main.getServerName();
        DCServerConfig dcserver = DCServerFactory.getServerConfig(dcsName);
        if (dcserver == null) {
            Print.logWarn("DCServer name not registered: " + dcsName);
        }

        /* validate IDs */
        boolean hasAcctDevID = false;
        if (!StringTools.isBlank(accountID)) {
            if (StringTools.isBlank(deviceID)) {
                Print.logError("'deviceid' required if 'accountid' specified");
                return (nak != null) ? (nak + "\n").getBytes() : null;
            } else if (!StringTools.isBlank(mobileID)) {
                Print.logError("'mobileID' not allowed if 'accountid' specified");
                return (nak != null) ? (nak + "\n").getBytes() : null;
            }
            hasAcctDevID = true;
        } else if (!StringTools.isBlank(deviceID)) {
            Print.logError("'accountid' required if 'deviceid' specified");
            return (nak != null) ? (nak + "\n").getBytes() : null;
        } else if (StringTools.isBlank(mobileID)) {
            Print.logError("'mobileID' not specified");
            return (nak != null) ? (nak + "\n").getBytes() : null;
        }

        /* GPS Event */
        GPSEvent gpsEvent = hasAcctDevID
                ? new GPSEvent(dcserver, this.ipAddress, this.clientPort, accountID, deviceID)
                : new GPSEvent(dcserver, this.ipAddress, this.clientPort, mobileID);
        Device device = gpsEvent.getDevice();
        if (device == null) {
            // errors already displayed
            return (nak != null) ? (nak + "\n").getBytes() : null;
        }
        gpsEvent.setTimestamp(fixtime);
        gpsEvent.setStatusCode(statusCode);
        gpsEvent.setGeoPoint(geoPoint);
        gpsEvent.setGpsAge(gpsAge);
        gpsEvent.setSatelliteCount(satCount);
        gpsEvent.setSpeedKPH(speedKPH);
        gpsEvent.setHeading(heading);
        gpsEvent.setAltitude(altitudeM);
        gpsEvent.setOdometerKM(odomKM);
        if (gpioInput >= 0L) {
            gpsEvent.setInputMask(gpioInput);
        }

        /* insert/return */
        if (this.parseInsertRecord_Common(gpsEvent)) {
            return (ack != null) ? (ack + "\n").getBytes() : null;
        } else {
            return (nak != null) ? (nak + "\n").getBytes() : null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* initialize runtime config */
    public static void configInit() {
        DCServerConfig dcsc = Main.getServerConfig();
        if (dcsc == null) {
            Print.logWarn("DCServer not found: " + Main.getServerName());
            return;
        }

        /* custom */
        DATA_FORMAT_OPTION = dcsc.getIntProperty(Main.ARG_FORMAT, DATA_FORMAT_OPTION);

        /* common */
        MINIMUM_SPEED_KPH = dcsc.getMinimumSpeedKPH(MINIMUM_SPEED_KPH);
        ESTIMATE_ODOMETER = dcsc.getEstimateOdometer(ESTIMATE_ODOMETER);
        SIMEVENT_GEOZONES = dcsc.getSimulateGeozones(SIMEVENT_GEOZONES);
        SIMEVENT_DIGITAL_INPUTS = dcsc.getSimulateDigitalInputs(SIMEVENT_DIGITAL_INPUTS) & 0xFFFFL;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Once you have modified this example 'template' server to parse your particular
    // device packets, you can also use this source module to load GPS data packets
    // which have been saved in a file.  To run this module to load your save GPS data
    // packets, start this command as follows:
    //   java -cp <classpath> org.opengts.servers.template.TrackClientPacketHandler {options}
    // Where your options are one or more of
    //   -insert=[true|false]    Insert parse records into EventData
    //   -format=[1|2]           Data format
    //   -debug                  Parse internal sample data
    //   -parseFile=<file>       Parse data from specified file
    private static int _usage() {
        String cn = StringTools.className(TrackClientPacketHandler.class);
        Print.sysPrintln("Test/Load Device Communication Server");
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  $JAVA_HOME/bin/java -classpath <classpath> %s {options}", cn);
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -insert=[true|false]    Insert parsed records into EventData");
        Print.sysPrintln("  -format=[1|2]           Data format");
        Print.sysPrintln("  -debug                  Parse internal sample/debug data (if any)");
        Print.sysPrintln("  -parseFile=<file>       Parse data from specified file");
        return 1;
    }

    /* debug entry point (does not return) */
    public static int _main(boolean fromMain) {

        /* default options */
        INSERT_EVENT = RTConfig.getBoolean(Main.ARG_INSERT, DFT_INSERT_EVENT);
        if (!INSERT_EVENT) {
            Print.sysPrintln("Warning: Data will NOT be inserted into the database");
        }

        /* create client packet handler */
        TrackClientPacketHandler tcph = new TrackClientPacketHandler();

        /* DEBUG sample data */
        if (RTConfig.getBoolean(Main.ARG_DEBUG, false)) {
            String data[] = null;
            switch (DATA_FORMAT_OPTION) {
                case 1:
                    data = new String[]{
                        "123456789012345,2006/09/05,07:47:26,35.3640,-142.2958,27.0,224.8",};
                    break;
                case 2:
                    data = new String[]{
                        "account/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,12.09,108.52,200505,,*2E",
                        "/device/$GPRMC,025423.494,A,3709.0642,N,14207.8315,W,12.09,108.52,200505,,*2E",};
                    break;
                case 3:
                    data = new String[]{
                        "mid=123456789012345 lat=39.12345 lon=-1421.2345 kph=123.0"
                    };
                    break;
                default:
                    Print.sysPrintln("Unrecognized Data Format: %d", DATA_FORMAT_OPTION);
                    return _usage();
            }
            for (int i = 0; i < data.length; i++) {
                tcph.getHandlePacket(data[i].getBytes());
            }
            return 0;
        }

        /* 'parseFile' specified? */
        if (RTConfig.hasProperty(Main.ARG_PARSEFILE)) {

            /* get input file */
            File parseFile = RTConfig.getFile(Main.ARG_PARSEFILE, null);
            if ((parseFile == null) || !parseFile.isFile()) {
                Print.sysPrintln("Data source file not specified, or does not exist.");
                return _usage();
            }

            /* open file */
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(parseFile);
            } catch (IOException ioe) {
                Print.logException("Error openning input file: " + parseFile, ioe);
                return 2;
            }

            /* loop through file */
            try {
                // records are assumed to be terminated by CR/NL
                for (;;) {
                    String data = FileTools.readLine(fis);
                    if (!StringTools.isBlank(data)) {
                        tcph.getHandlePacket(data.getBytes());
                    }
                }
            } catch (EOFException eof) {
                Print.sysPrintln("");
                Print.sysPrintln("***** End-Of-File *****");
            } catch (IOException ioe) {
                Print.logException("Error reaading input file: " + parseFile, ioe);
            } finally {
                try {
                    fis.close();
                } catch (Throwable th) {/* ignore */

                }
            }

            /* done */
            return 0;

        }

        /* no options? */
        return _usage();

    }

    /* debug entry point */
    public static void main(String argv[]) {
        DBConfig.cmdLineInit(argv, false);
        TrackClientPacketHandler.configInit();
        System.exit(TrackClientPacketHandler._main(false));
    }
}
