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
// This module provides generic support for a vide variety of HTTP-based device 
// communication protocols.   This includes devices that send the NMEA-0183 $GPRMC 
// record in the requres URL.
//
// Here are the configurable properties that may be set in 'webapp.conf' to customize
// for a specific device type:
//    gprmc.logName           - Name used in logging output [default "gprmc"]
//    gprmc.uniquePrefix      - Prefix used on uniqueID when lookup up Device [defaults to 'gprmc.logName']
//    gprmc.defaultAccountID  - Default account id [default "gprmc"]
//    gprmc.minimumSpeedKPH   - Minimum acceptable speed
//    gprmc.dateFormat        - Date format for 'date' parameter (NONE|EPOCH|YMD|DMY|MDY) [default "YMD"]
//    gprmc.response.ok       - Response on successful data [default ""]
//    gprmc.response.error    - Response on error data [default ""]
//    gprmc.parm.unique       - Unique-ID parameter key [default "id"]
//    gprmc.parm.account      - Account-ID parameter key [default "acct"]
//    gprmc.parm.device       - Device-ID parameter key [default "dev"]
//    gprmc.parm.auth         - Auth/Password parameter key (not used)
//    gprmc.parm.status       - StatusCode parameter key [default "code"]
//    gprmc.parm.gprmc        - $GPRMC parameter key [default "gprmc"]
//    gprmc.parm.date         - Date parameter key (ignored if 'gprmc' is used) [default "date"]
//    gprmc.parm.time         - Time parameter key (ignored if 'gprmc' is used) [default "time"]
//    gprmc.parm.latitude     - Latitude parameter key (ignored if 'gprmc' is used) [default "lat"]
//    gprmc.parm.longitude    - Longitude parameter key (ignored if 'gprmc' is used) [default "lon"]
//    gprmc.parm.speed"       - Speed(kph) parameter key (ignored if 'gprmc' is used) [default "speed"]
//    gprmc.parm.heading      - Heading(degrees) parameter key (ignored if 'gprmc' is used) [default "head"]
//    gprmc.parm.altitude     - Altitude(meters) parameter key [default "alt"]
//    gprmc.parm.odometer     - Odometer(kilometers) parameter key [default "odom"]
//    gprmc.parm.address      - Reverse-Geocode parameter key [default "addr"]
//    gprmc.parm.driver       - DriverID parameter key [default "drv"]
//    gprmc.parm.message      - Message parameter key [default "msg"]
//
// Note: Do not rely on the property defaults always remaining the same as they are
// currently in this module.  This module is still under development and is subject to
// change, which includes the default values.
//
// Default sample Data:
//   http://track.example.com/gprmc/Data?
//      acct=myaccount&
//      dev=mydevice&
//      gprmc=$GPRMC,065954,V,3244.2749,N,14209.9369,W,21.6,0.0,211202,11.8,E,S*07
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=gprmc
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.parm.account=acct
//      gprmc.parm.device=dev
//      gprmc.parm.gprmc=gprmc
//
// NetGPS configuration: [http://www.gpsvehiclenavigation.com/GPS/netgps.php]
//   http://track.example.com/gprmc/Data?
//      un=deviceid&
//      cds=$GPRMC,140159.435,V,3244.2749,N,14209.9369,W,,,200807,,*13&
//      pw=anypass
//   'webapp.conf' properties:
//      gprmc.logName=netgps
//      gprmc.defaultAccountID=netgps
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.parm.account=acct
//      gprmc.parm.device=un
//      gprmc.parm.auth=pw
//      gprmc.parm.gprmc=cds
//      gprmc.response.ok=GPSOK
//      gprmc.response.error=GPSERROR:
//
// GC-101 configuration:
//   http://track.example.com/gprmc/Data?
//      imei=471923002250245&
//      rmc=$GPRMC,023000.000,A,3130.0577,N,14271.7421,W,0.53,208.37,210507,,*19,AUTO
//   'webapp.conf' properties:
//      gprmc.logName=gc101
//      gprmc.uniquePrefix=gc101
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=NONE
//      gprmc.parm.unique=imei
//      gprmc.parm.gprmc=rmc
//
// Mologogo configuration:
//   http://track.example.com/gprmc/Data?
//      id=dad&
//      lat=39.251811&
//      lon=-137.132341&
//      accuracy=35949&
//      direction=-1&
//      speed=0&
//      speedUncertainty=0&
//      altitude=519&
//      altitudeUncertainty=49390&
//      pointType=GPS
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=mologogo
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=NONE
//      gprmc.parm.account=acct
//      gprmc.parm.device=id
//      gprmc.parm.gprmc=
//      gprmc.parm.latitude=lat
//      gprmc.parm.longitude=lon
//      gprmc.parm.speed=speed
//      gprmc.parm.heading=direction
//      gprmc.parm.status=pointType
//
// GPSGate configuration:
//   http://track.example.com/gprmc/Data?
//      longitude=113.38063&
//      latitude=22.53922&
//      altitude=22.4&
//      speed=0.0&
//      heading=0.0&
//      date=20100526&
//      time=142252.000&
//      username=123123456789&
//      pw=0111111
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=YMD
//      gprmc.parm.unique=username
//      gprmc.parm.account=
//      gprmc.parm.device=
//      gprmc.parm.gprmc=
//      gprmc.parm.latitude=latitude
//      gprmc.parm.longitude=longitude
//      gprmc.parm.date=date
//      gprmc.parm.time=time
//      gprmc.parm.speed=speed
//      gprmc.parm.heading=heading
//      gprmc.parm.altitude=altitude
//      gprmc.parm.status=code
//
// Another example configuration:
//   http://track.example.com/gprmc/Data?
//      acct=myacct&
//      dev=mydev&
//      lon=32.1234&
//      lat=-142.1234&
//      date=20070819&
//      time=225446&
//      speed=45.4&
//      code=1
//   'webapp.conf' properties:
//      gprmc.defaultAccountID=undefined
//      gprmc.minimumSpeedKPH=4.0
//      gprmc.dateFormat=YMD
//      gprmc.parm.account=acct
//      gprmc.parm.device=dev
//      gprmc.parm.gprmc=
//      gprmc.parm.latitude=lat
//      gprmc.parm.longitude=lon
//      gprmc.parm.date=date
//      gprmc.parm.time=time
//      gprmc.parm.speed=speed
//      gprmc.parm.heading=heading
//      gprmc.parm.status=code
//
// ----------------------------------------------------------------------------
// Change History:
//  2007/08/09  Martin D. Flynn
//     -Initial release. 
//     -Note: this module is new for this release and has not yet been fully tested.
//  2007/09/16  Martin D. Flynn
//     -Additional optional parameters to allow for more flexibility in defining data
//      format types.  This module should now be able to be configured for a wide variety
//      of HTTP base communication protocols from various types of remote devices.
//     -Note: this module has still not yet been fully tested.
//  2007/11/28  Martin D. Flynn
//     -Added 'gprmc.uniquePrefix' property
//  2008/02/10  Martin D. Flynn
//     -Added additional logging messages when lat/lon is invalid
//  2008/05/14  Martin D. Flynn
//     -Integrated Device DataTransport interface
//  2008/08/15  Martin D. Flynn
//     -Make sure 'isValid' is set for non-GPRMC parsed records.
//  2010/04/11  Martin D. Flynn
//     -Various changes
// ----------------------------------------------------------------------------
package org.opengts.war.gprmc;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.dbtypes.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public class Data 
    extends CommonServlet
{

    // ------------------------------------------------------------------------

    /* name used to tag logged messaged */
    public static String LOG_NAME                           = "gprmc";

    /* name used to tag logged messaged */
    public static String VERSION                            = "1.2.1";

    // ------------------------------------------------------------------------

    /* unique id prefix */
    public static String UniqueIDPrefix                     = LOG_NAME;

    /* default account id */
    // The URL variable "?un=<id>" is used to allow this module the ability to uniquely
    // identify a reporting phone.  The reported "<id>" provided by the phone is used
    // as the "DeviceID" for the following AccountID.  This value can be overridden in
    // the Servlet 'webapp.conf' file.
    public static String  DefaultAccountID                  = "gprmc";

    /* minimum required speed */
    // GPS receivers have a tendency to appear 'moving', even when sitting stationary
    // on your desk.  This filter is a 'speed' threshold which is used to force the 
    // reported speed to '0' if it falls below this value.  This value can be overridden
    // in the Servlet 'webapp.conf' file.
    public static double  MinimumReqSpeedKPH                = 4.0;

    /* estimate GPS-based odometer */
    public  static boolean  ESTIMATE_ODOMETER               = false;

    /* Default time zone */
    private static TimeZone gmtTimeZone                     = DateTime.getGMTTimeZone();
   
    /* Maximum timestamp */
    private static long     MaxTimestamp                    = DateTime.getMaxDate().getTimeSec();

    /* Status code map */
    private static Map<String,Integer> StatusCodeMap        = null;

    // ------------------------------------------------------------------------

    /* date format constants */
    public static final int DATE_FORMAT_NONE                = 0; // Current time will be used
    public static final int DATE_FORMAT_EPOCH               = 1; // <epochTime>
    public static final int DATE_FORMAT_YMD                 = 2; // "YYYYMMDD" or "YYMMDD"
    public static final int DATE_FORMAT_MDY                 = 3; // "MMDDYYYY" or "MMDDYY"
    public static final int DATE_FORMAT_DMY                 = 4; // "DDMMYYYY" or "DDMMYY"
    
    /* date format */
    // The date format must be specified here
    public static int     DateFormat                        = DATE_FORMAT_YMD;

    public static String GetDateFormatString()
    {
        switch (DateFormat) {
            case DATE_FORMAT_NONE :  return "NONE";
            case DATE_FORMAT_EPOCH:  return "EPOCH";
            case DATE_FORMAT_YMD  :  return "YMD";
            case DATE_FORMAT_MDY  :  return "MDY";
            case DATE_FORMAT_DMY  :  return "DMY";
            default               :  return "???";
        }
    }
    
    // ------------------------------------------------------------------------
    
    // common parameter keys (lookups are case insensitive) */
    private static String PARM_UNIQUE                       = "id";         // UniqueID
    private static String PARM_ACCOUNT                      = "acct";       // AccountID
    private static String PARM_DEVICE                       = "dev";        // DeviceID
    private static String PARM_AUTH                         = "pass";       // authorization/password
    private static String PARM_STATUS                       = "code";       // status code
    private static String PARM_ALTITUDE                     = "alt";        // altitude (meters)
    private static String PARM_ODOMETER                     = "odom";       // odometer (kilometers)
    private static String PARM_ADDRESS                      = "addr";       // reverse-geocoded address
    private static String PARM_DRIVER                       = "drv";        // driver
    private static String PARM_MESSAGE                      = "msg";        // message
    private static String PARM_EMAIL                        = "email";      // email

    // $GPRMC field key
    private static String PARM_GPRMC                        = "gprmc";      // $GPRMC data

    // these are ignored if PARM_GPRMC is defined
    private static String PARM_DATE                         = "date";       // date (YYYYMMDD)
    private static String PARM_TIME                         = "time";       // time (HHMMSS)
    private static String PARM_LATITUDE                     = "lat";        // latitude
    private static String PARM_LONGITUDE                    = "lon";        // longitude
    private static String PARM_SPEED                        = "speed";      // speed (kph)
    private static String PARM_HEADING                      = "head";       // heading (degrees)

    /* returned response */
    private static String RESPONSE_OK                       = "OK";
    private static String RESPONSE_ERROR                    = "";

    // ------------------------------------------------------------------------

    /* configuration name (TODO: update based on specific servlet configuration) */
    public static final String  CONFIG_NAME                 = "gprmc";
    
    /* runtime config */
    public static final String  CONFIG_LOG_NAME             = CONFIG_NAME + ".logName";
    public static final String  CONFIG_UNIQUE_PREFIX        = CONFIG_NAME + ".uniquePrefix";
    public static final String  CONFIG_DFT_ACCOUNT          = CONFIG_NAME + ".defaultAccountID";
    public static final String  CONFIG_MIN_SPEED            = CONFIG_NAME + ".minimumSpeedKPH";
    public static final String  CONFIG_ESTIMATE_ODOMETER    = CONFIG_NAME + ".estimateOdometer";
    public static final String  CONFIG_DATE_FORMAT          = CONFIG_NAME + ".dateFormat";       // "YMD", "DMY", "MDY"
    public static final String  CONFIG_RESPONSE_OK          = CONFIG_NAME + ".response.ok";
    public static final String  CONFIG_RESPONSE_ERROR       = CONFIG_NAME + ".response.error";

    public static final String  CONFIG_PARM_UNIQUE          = CONFIG_NAME + ".parm.unique";
    public static final String  CONFIG_PARM_ACCOUNT         = CONFIG_NAME + ".parm.account";
    public static final String  CONFIG_PARM_DEVICE          = CONFIG_NAME + ".parm.device";
    public static final String  CONFIG_PARM_AUTH            = CONFIG_NAME + ".parm.auth";
    public static final String  CONFIG_PARM_STATUS          = CONFIG_NAME + ".parm.status";
    
    public static final String  CONFIG_PARM_GPRMC           = CONFIG_NAME + ".parm.gprmc";       // $GPRMC
    
    public static final String  CONFIG_PARM_DATE            = CONFIG_NAME + ".parm.date";        // epoch, YYYYMMDD, DDMMYYYY, MMDDYYYY
    public static final String  CONFIG_PARM_TIME            = CONFIG_NAME + ".parm.time";        // HHMMSS
    public static final String  CONFIG_PARM_LATITUDE        = CONFIG_NAME + ".parm.latitude";
    public static final String  CONFIG_PARM_LONGITUDE       = CONFIG_NAME + ".parm.longitude";
    public static final String  CONFIG_PARM_SPEED           = CONFIG_NAME + ".parm.speed";       // kph
    public static final String  CONFIG_PARM_HEADING         = CONFIG_NAME + ".parm.heading";     // degrees
    public static final String  CONFIG_PARM_ALTITUDE        = CONFIG_NAME + ".parm.altitude";    // meters
    public static final String  CONFIG_PARM_ODOMETER        = CONFIG_NAME + ".parm.odometer";    // kilometers
    public static final String  CONFIG_PARM_ADDRESS         = CONFIG_NAME + ".parm.address";     // reverse-geocode
    public static final String  CONFIG_PARM_DRIVER          = CONFIG_NAME + ".parm.driver";      // driverId
    public static final String  CONFIG_PARM_MESSAGE         = CONFIG_NAME + ".parm.message";     // message
    public static final String  CONFIG_PARM_EMAIL           = CONFIG_NAME + ".parm.email";       // email address

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* static initializer */
    // Only initialized once (per JVM)
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

        /* set configuration */
        LOG_NAME           = RTConfig.getString( CONFIG_LOG_NAME         , LOG_NAME);
        UniqueIDPrefix     = RTConfig.getString( CONFIG_UNIQUE_PREFIX    , LOG_NAME);
        DefaultAccountID   = RTConfig.getString( CONFIG_DFT_ACCOUNT      , DefaultAccountID);
        MinimumReqSpeedKPH = RTConfig.getDouble( CONFIG_MIN_SPEED        , MinimumReqSpeedKPH);
        ESTIMATE_ODOMETER  = RTConfig.getBoolean(CONFIG_ESTIMATE_ODOMETER, ESTIMATE_ODOMETER);
        String dateFmt     = RTConfig.getString( CONFIG_DATE_FORMAT      , "");
        if (dateFmt.equals("") || dateFmt.equalsIgnoreCase("NONE")) {
            DateFormat = DATE_FORMAT_NONE;
        } else
        if (dateFmt.equalsIgnoreCase("EPOCH")) {
            DateFormat = DATE_FORMAT_EPOCH;
        } else
        if (dateFmt.equalsIgnoreCase("YMD")) {
            DateFormat = DATE_FORMAT_YMD;
        } else
        if (dateFmt.equalsIgnoreCase("MDY")) {
            DateFormat = DATE_FORMAT_MDY;
        } else
        if (dateFmt.equalsIgnoreCase("DMY")) {
            DateFormat = DATE_FORMAT_DMY;
        } else {
            DateFormat = DATE_FORMAT_YMD;
            Data.logError(null, "Invalid date format: " + dateFmt);
        }

        /* parameters */
        PARM_UNIQUE        = RTConfig.getString(CONFIG_PARM_UNIQUE   , PARM_UNIQUE)     .trim();
        PARM_ACCOUNT       = RTConfig.getString(CONFIG_PARM_ACCOUNT  , PARM_ACCOUNT)    .trim();
        PARM_DEVICE        = RTConfig.getString(CONFIG_PARM_DEVICE   , PARM_DEVICE)     .trim();
        PARM_AUTH          = RTConfig.getString(CONFIG_PARM_AUTH     , PARM_AUTH)       .trim();
        PARM_STATUS        = RTConfig.getString(CONFIG_PARM_STATUS   , PARM_STATUS)     .trim();
        PARM_GPRMC         = RTConfig.getString(CONFIG_PARM_GPRMC    , PARM_GPRMC)      .trim();
        PARM_DATE          = RTConfig.getString(CONFIG_PARM_DATE     , PARM_DATE)       .trim();
        PARM_TIME          = RTConfig.getString(CONFIG_PARM_TIME     , PARM_TIME)       .trim();
        PARM_LATITUDE      = RTConfig.getString(CONFIG_PARM_LATITUDE , PARM_LATITUDE)   .trim();
        PARM_LONGITUDE     = RTConfig.getString(CONFIG_PARM_LONGITUDE, PARM_LONGITUDE)  .trim();
        PARM_SPEED         = RTConfig.getString(CONFIG_PARM_SPEED    , PARM_SPEED)      .trim();
        PARM_HEADING       = RTConfig.getString(CONFIG_PARM_HEADING  , PARM_HEADING)    .trim();
        PARM_ALTITUDE      = RTConfig.getString(CONFIG_PARM_ALTITUDE , PARM_ALTITUDE)   .trim();
        PARM_ODOMETER      = RTConfig.getString(CONFIG_PARM_ODOMETER , PARM_ODOMETER)   .trim();
        PARM_ADDRESS       = RTConfig.getString(CONFIG_PARM_ADDRESS  , PARM_ADDRESS)    .trim();
        PARM_DRIVER        = RTConfig.getString(CONFIG_PARM_DRIVER   , PARM_DRIVER)     .trim();
        PARM_MESSAGE       = RTConfig.getString(CONFIG_PARM_MESSAGE  , PARM_MESSAGE)    .trim();
        PARM_EMAIL         = RTConfig.getString(CONFIG_PARM_EMAIL    , PARM_EMAIL)      .trim();

        /* return errors */
        RESPONSE_OK        = RTConfig.getString(CONFIG_RESPONSE_OK   , RESPONSE_OK);
        RESPONSE_ERROR     = RTConfig.getString(CONFIG_RESPONSE_ERROR, RESPONSE_ERROR);

        /* header */
        Data.logInfo("Version            : " + VERSION);
        Data.logInfo("Default AccountID  : " + DefaultAccountID);
        Data.logInfo("Minimum speed      : " + MinimumReqSpeedKPH + " kph");
        Data.logInfo("Date Format        : " + GetDateFormatString());
        Data.logInfo("UniqueID parameter : &" + PARM_UNIQUE + "=");
        Data.logInfo("Account parameter  : &" + PARM_ACCOUNT + "=");
        Data.logInfo("Device parameter   : &" + PARM_DEVICE + "=");
        Data.logInfo("Status parameter   : &" + PARM_STATUS + "=");
        if (!StringTools.isBlank(PARM_GPRMC)) {
        Data.logInfo("$GPRMC parameter   : &" + PARM_GPRMC + "=");
        } else {
        Data.logInfo("Date parameter     : &" + PARM_DATE + "=");
        Data.logInfo("Time parameter     : &" + PARM_TIME + "=");
        Data.logInfo("Latitude parameter : &" + PARM_LATITUDE + "=");
        Data.logInfo("Longitude parameter: &" + PARM_LONGITUDE + "=");
        Data.logInfo("SpeedKPH parameter : &" + PARM_SPEED + "=");
        Data.logInfo("Heading parameter  : &" + PARM_HEADING + "=");
        }
        Data.logInfo("Altitude parameter : &" + PARM_ALTITUDE + "=");
        Data.logInfo("Odometer parameter : &" + PARM_ODOMETER + "=");
        Data.logInfo("Address parameter  : &" + PARM_ADDRESS + "=");
        Data.logInfo("Driver parameter   : &" + PARM_DRIVER + "=");
        Data.logInfo("Message parameter  : &" + PARM_MESSAGE + "=");
        
        /* status code map */
        StatusCodeMap = new HashMap<String,Integer>();
        StatusCodeMap.put("GPS"      , new Integer(StatusCodes.STATUS_LOCATION));
        StatusCodeMap.put("PANIC"    , new Integer(StatusCodes.STATUS_PANIC_ON));
        StatusCodeMap.put("SOS"      , new Integer(StatusCodes.STATUS_PANIC_ON));
        StatusCodeMap.put("ASSIST"   , new Integer(StatusCodes.STATUS_ASSIST_ON));
        StatusCodeMap.put("HELP"     , new Integer(StatusCodes.STATUS_ASSIST_ON));
        StatusCodeMap.put("NOTIFY"   , new Integer(StatusCodes.STATUS_NOTIFY));
        StatusCodeMap.put("MEDICAL"  , new Integer(StatusCodes.STATUS_MEDICAL_ON));
        StatusCodeMap.put("NURSE"    , new Integer(StatusCodes.STATUS_MEDICAL_ON));
        StatusCodeMap.put("DOCTOR"   , new Integer(StatusCodes.STATUS_MEDICAL_ON));
        StatusCodeMap.put("IMPACT"   , new Integer(StatusCodes.STATUS_IMPACT));
        StatusCodeMap.put("ACCIDENT" , new Integer(StatusCodes.STATUS_IMPACT));
        StatusCodeMap.put("WAYMARK"  , new Integer(StatusCodes.STATUS_WAYMARK_0));
        StatusCodeMap.put("JOBSTART" , new Integer(StatusCodes.STATUS_JOB_ARRIVE));
        StatusCodeMap.put("JOBARRIVE", new Integer(StatusCodes.STATUS_JOB_ARRIVE));
        StatusCodeMap.put("JOBEND"   , new Integer(StatusCodes.STATUS_JOB_DEPART));
        StatusCodeMap.put("JOBDEPART", new Integer(StatusCodes.STATUS_JOB_DEPART));
        StatusCodeMap.put("LOGIN"    , new Integer(StatusCodes.STATUS_LOGIN));
        StatusCodeMap.put("LOGOUT"   , new Integer(StatusCodes.STATUS_LOGOUT));

    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* translate status code from device */
    private static int TranslateStatusCode(String statusCodeStr)
    {

        /* blank status */
        if (StringTools.isBlank(statusCodeStr)) {
            // no status code specified
            return StatusCodes.STATUS_LOCATION;
        }
        String sc = statusCodeStr.toUpperCase();

        /* check status code name */
        StatusCodes.Code code = StatusCodes.GetCode(sc,null);
        if (code != null) {
            return code.getCode();
        }

        /* status code number? */
        if (StringTools.isInt(statusCodeStr,true)) {
            return StringTools.parseInt(statusCodeStr, StatusCodes.STATUS_NONE);
        }

        /* check default codes */
        Integer sci = StatusCodeMap.get(sc);
        if (sci != null) {
            return sci.intValue();
        }

        // TODO: Status code translations are device dependent, thus this section will 
        //       needs to be customized to the specific device using this server.
        //
        // For instance, the Mologogo would use the following code translation:
        //    "GPS"    => StatusCodes.STATUS_LOCATION
        //    "CELL"   => StatusCodes.STATUS_LOCATION
        //    "MANUAL" => StatusCodes.STATUS_WAYMARK_0
        //
        // For now, just return the generic StatusCodes.STATUS_LOCATION
        return StatusCodes.STATUS_LOCATION;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this._doWork(true, request, response);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this._doWork(false, request, response);
    }
    
    private void _doWork(boolean isPost, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String  ipAddr     = request.getRemoteAddr();
        String  uniqueID   = AttributeTools.getRequestString(request, PARM_UNIQUE    , null);
        String  accountID  = AttributeTools.getRequestString(request, PARM_ACCOUNT   , null);
        String  deviceID   = AttributeTools.getRequestString(request, PARM_DEVICE    , "");
        String  authCode   = AttributeTools.getRequestString(request, PARM_AUTH      , "");  // not currently used
        String  driverID   = AttributeTools.getRequestString(request, PARM_DRIVER    , "");
        Device  device     = null;

        /* URL */
        StringBuffer reqURL = request.getRequestURL();
        String queryStr = StringTools.blankDefault(request.getQueryString(),"(n/a)");
        if (isPost) {
            // 'queryStr' is likely not available
            StringBuffer postSB = new StringBuffer();
            for (java.util.Enumeration ae = request.getParameterNames(); ae.hasMoreElements();) {
                if (postSB.length() > 0) { postSB.append("&"); }
                String ak = (String)ae.nextElement();
                String av = request.getParameter(ak);
                postSB.append(ak + "=" + av);
            }
            Data.logDebug("[" + ipAddr + "] POST: " + reqURL + " " + queryStr + " [" + postSB + "]");
        } else {
            Data.logDebug("[" + ipAddr + "] GET: "  + reqURL + " " + queryStr);
        }

        /* unique id? */
        if (!StringTools.isBlank(uniqueID)) {
            
            /* get Device by UniqueID */
            String uid = UniqueIDPrefix + "_" + uniqueID; // ie: "gprmc_123456789012345"
            try {
                device = Transport.loadDeviceByUniqueID(uid);
                if (device == null) {
                    Data.logWarn("Unique ID not found!: " + uid); // <== display main key
                    Print.sysPrintln("NotFound: now=%d id=%s", DateTime.getCurrentTimeSec(), uid);
                    DCServerFactory.addUnassignedDevice(LOG_NAME, uid, ipAddr, true, null);
                    this.plainTextResponse(response, RESPONSE_ERROR);
                    return;
                }
            } catch (DBException dbe) {
                Data.logError(null, "Exception getting Device: " + uniqueID + " [" + dbe + "]");
                this.plainTextResponse(response, RESPONSE_ERROR);
                return;
            }
            
        } else {

            /* account id? */
            if (StringTools.isBlank(accountID)) {
                accountID = DefaultAccountID;
                if ((accountID == null) || accountID.equals("")) {
                    Data.logError(null, "Unable to identify Account");
                    Data.logError(null, "(has '" + PARM_ACCOUNT + "' been properly configured in 'webapp.conf'?)");
                    this.plainTextResponse(response, RESPONSE_ERROR);
                    return;
                }
            }

            /* device id? */
            if (StringTools.isBlank(deviceID)) {
                Data.logError(null, "Unable to identify Device");
                Data.logError(null, "(has '" + PARM_DEVICE + "' been properly configured in 'webapp.conf'?)");
                this.plainTextResponse(response, RESPONSE_ERROR);
                return;
            }

            /* read the device */
            try {
                device = Transport.loadDeviceByTransportID(Account.getAccount(accountID), deviceID);
                if (device == null) {
                    // Device was not found
                    String uid = accountID + "/" + deviceID;
                    Data.logError(null, "Device not found AccountID/DeviceID: " + uid);
                    Print.sysPrintln("NotFound: now=%d id=%s", DateTime.getCurrentTimeSec(), uid);
                    DCServerFactory.addUnassignedDevice(LOG_NAME, uid, ipAddr, true, null);
                    this.plainTextResponse(response, RESPONSE_ERROR);
                    return;
                }
            } catch (DBException dbe) {
                // Error while reading Device
                Data.logException(null, "Error reading Device", dbe);
                this.plainTextResponse(response, RESPONSE_ERROR);
                return;
            }
            
        }

        /* update actual device/account ids */
        accountID = device.getAccountID();
        deviceID  = device.getDeviceID();
        uniqueID  = device.getUniqueID();

        /* account */
        Account account = device.getAccount();
        if (account == null) {
            Data.logError(null, "Account record not found!");
            this.plainTextResponse(response, RESPONSE_ERROR);
            return;
        }

        /* validate source IP address */
        // This may be used to prevent rogue hackers from spoofing data coming from the phone
        if (!device.getDataTransport().isValidIPAddress(ipAddr)) {
            // 'ipAddr' does not match allowable device IP addresses
            Data.logError(null, "Invalid IP Address for device");
            this.plainTextResponse(response, RESPONSE_ERROR);
            return;
        }

        // ---------------------------------------------------------------------------------
        // --- Parse data below --- 
        
        /* message */
        String message     = AttributeTools.getRequestString(request, PARM_MESSAGE   , "");
        String emailAddr   = AttributeTools.getRequestString(request, PARM_EMAIL     , "");

        /* fields */
        String _statusStr  = AttributeTools.getRequestString(request, PARM_STATUS    , null);
        String _address    = AttributeTools.getRequestString(request, PARM_ADDRESS   , null);
        String _altitudeM  = AttributeTools.getRequestString(request, PARM_ALTITUDE  , null);
        String _odometerKM = AttributeTools.getRequestString(request, PARM_ODOMETER  , null);
        String _gprmcStr   = AttributeTools.getRequestString(request, PARM_GPRMC     , null);
        String _dateStr    = AttributeTools.getRequestString(request, PARM_DATE      , null);
        String _timeStr    = AttributeTools.getRequestString(request, PARM_TIME      , null);
        String _latitude   = AttributeTools.getRequestString(request, PARM_LATITUDE  , null);
        String _longitude  = AttributeTools.getRequestString(request, PARM_LONGITUDE , null);
        String _speedKPH   = AttributeTools.getRequestString(request, PARM_SPEED     , null);
        String _headingDeg = AttributeTools.getRequestString(request, PARM_HEADING   , null);
        
        /* parse fields */
        String statusStr   = _statusStr;
        String address     = _address;
        double altitudeM   = StringTools.parseDouble(_altitudeM , 0.0);     // meters
        double odometerKM  = StringTools.parseDouble(_odometerKM, 0.0);     // meters
        String gprmcStr    = _gprmcStr;
        String dateStr     = _dateStr;
        String timeStr     = _timeStr;
        double latitude    = StringTools.parseDouble(_latitude  , -999.0);
        double longitude   = StringTools.parseDouble(_longitude , -999.0);
        double speedKPH    = StringTools.parseDouble(_speedKPH  , 0.0);     // kph
        double headingDeg  = StringTools.parseDouble(_headingDeg, 0.0);     // degrees

        /* status code translation */
        int statusCode = Data.TranslateStatusCode(statusStr);

        /* latitude, longitude, speed, heading, ... */
        boolean isValid = false;
        long    fixtime = 0L;
        if (!StringTools.isBlank(gprmcStr)) {
            if (!gprmcStr.startsWith("$GPRMC")) {
                Data.logError(null, "Missing/Invalid $GPRMC: " + gprmcStr);
                Data.logError(null, "(is '" + PARM_GPRMC + "' properly configured in 'webapp.conf'?)");
                this.plainTextResponse(response, RESPONSE_ERROR);
                return;
            }
            Nmea0183 gprmc = new Nmea0183(gprmcStr);
            fixtime    = gprmc.getFixtime();
            isValid    = gprmc.isValidGPS();
            latitude   = isValid? gprmc.getLatitude()  : 0.0;
            longitude  = isValid? gprmc.getLongitude() : 0.0;
            speedKPH   = isValid? gprmc.getSpeedKPH()  : 0.0;
            headingDeg = isValid? gprmc.getHeading()   : 0.0;
            if (!isValid) {
                Data.logWarn("Invalid latitude/longitude");
            }
        } else {
            fixtime    = this._parseFixtime(dateStr, timeStr);
            if ((latitude == -999.0) || (longitude == -999.0)) {
                Data.logError(null, "Missing/Invalid latitude/longitude");
                Data.logError(null, "(is '"+PARM_LATITUDE+"'/'"+PARM_LONGITUDE+"' properly configured in 'webapp.conf'?)");
                isValid   = false;
                latitude  = 0.0;
                longitude = 0.0;
            } else
            if ((latitude  >=  90.0) || (latitude  <=  -90.0) ||
                (longitude >= 180.0) || (longitude <= -180.0) ||
                ((latitude == 0.0) && (longitude == 0.0)    )   ) {
                Data.logWarn("Invalid latitude/longitude: " + latitude + "/" + longitude);
                isValid   = false;
                latitude  = 0.0;
                longitude = 0.0;
            } else {
                isValid   = true;
            }
        }
                
        /* adjustments to speed/heading */
        if (!isValid || (speedKPH < MinimumReqSpeedKPH)) {
            // Say we're not moving if the value is <= our desired threshold
            speedKPH = 0.0;
        }
        if ((speedKPH <= 0.0) || (headingDeg < 0.0)) {
            // We're either not moving, or the GPS receiver doesn't know the heading
            headingDeg = 0.0; // to be consistent, set the heading to North
        }
    
        /* estimate GPS-based odometer */
        if (odometerKM <= 0.0) {
            // calculate odometer
            odometerKM = (ESTIMATE_ODOMETER && isValid)? 
                device.getNextOdometerKM(new GeoPoint(latitude,longitude)) : 
                device.getLastOdometerKM();
        } else {
            // bounds-check odometer
            odometerKM = device.adjustOdometerKM(odometerKM);
        }

        /* debug */
        Data.logDebug("Fixtime  : ["+fixtime+"] " + (new DateTime(fixtime)));
        Data.logDebug("Status   : ["+StatusCodes.GetHex(statusCode)+"] " + StatusCodes.GetDescription(statusCode,null));
        Data.logDebug("Account  : " + accountID);
        Data.logDebug("Device   : " + deviceID);
        Data.logDebug("UniqueID : " + uniqueID);
        Data.logDebug("GeoPoint : " + latitude + "/" + longitude);
        Data.logDebug("SpeedKPH : " + speedKPH + " km/h  [" + headingDeg + "]");
        Data.logDebug("Altitude : " + altitudeM + " meters");
        Data.logDebug("Odometer : " + odometerKM + " km");

        /* reject invalid GPS fixes? */
        if (!isValid && (statusCode == StatusCodes.STATUS_LOCATION)) {
            // ignore invalid GPS fixes that have a simple 'STATUS_LOCATION' status code
            Data.logWarn("Ignoring event with invalid latitude/longitude");
            this.plainTextResponse(response, "");
            return;
        }
    
        /* create/insert new event record */
        EventData.Key evKey = new EventData.Key(accountID, deviceID, fixtime, statusCode);
        EventData evdb = evKey.getDBRecord();
        evdb.setLatitude(latitude);
        evdb.setLongitude(longitude);
        evdb.setSpeedKPH(speedKPH);
        evdb.setHeading(headingDeg);
        evdb.setAltitude(altitudeM);
        evdb.setOdometerKM(odometerKM);
        evdb.setAddress(address);
        evdb.setDriverID(driverID);
        evdb.setDriverMessage(message);
        //evdb.setEmailRecipient(emailAddr);
        // this will display an error if it was unable to store the event
        if (device.insertEventData(evdb)) {
            Data.logDebug("Event inserted: "+accountID+"/"+deviceID +
                " - " + evdb.getGeoPoint());
        }

        /* write success response */
        this.plainTextResponse(response, RESPONSE_OK);

    }

    private long _parseFixtime(String dateStr, String timeStr)
    {
        // Examples:
        // 0) if (DateFormat == DATE_FORMAT_NONE):
        //      return current time
        // 1) if (DateFormat == DATE_FORMAT_EPOCH):
        //      &date=1187809084
        // 2) if (DateFormat == DATE_FORMAT_YMD):
        //      &date=2007/08/21&time=17:59:23
        //      &date=20070821&time=175923
        //      &date=070821&time=175923
        // 3) if (DateFormat == DATE_FORMAT_MDY):
        //      &date=08/21/2007&time=17:59:23
        //      &date=08212007&time=175923
        //      &date=082107&time=175923
        // 4) if (DateFormat == DATE_FORMAT_DMY):
        //      &date=21/08/2007&time=17:59:23
        //      &date=21082007&time=175923
        //      &date=210807&time=175923

        /* no date/time specification? */
        if (DateFormat == DATE_FORMAT_NONE) {
            return DateTime.getCurrentTimeSec();
        }

        /* unix 'Epoch' time? */
        if (DateFormat == DATE_FORMAT_EPOCH) {
            String epochStr = !dateStr.equals("")? dateStr : timeStr;
            long timestamp = StringTools.parseLong(epochStr, 0L);
			if (timestamp > MaxTimestamp) { // (long)Integer.MAX_VALUE * 2)
				timestamp /= 1000L; // timestamp was specified in milliseconds
			}
            return (timestamp > 0L)? timestamp : DateTime.getCurrentTimeSec();
        }

        /* time */
        if (timeStr.indexOf(":") >= 0) {
            // Convert "HH:MM:SS" to "HHMMSS"
            timeStr = StringTools.stripChars(timeStr,':');
        }
        if (timeStr.length() < 6) {
            // invalid time length, expecting at least "HHMMSS"
            return DateTime.getCurrentTimeSec();
        }
        // timeStr may be "HHMMSS" or "HHMMSS.000"

        /* date */
        if (dateStr.indexOf("/") >= 0) {
            // Convert "YYYY/MM/DD" to "YYYYMMDD"
            dateStr = StringTools.stripChars(dateStr,'/');
        }
        int dateLen = dateStr.length();
        if ((dateLen != 8) && (dateLen != 6)) {
            // invalid date length
            return DateTime.getCurrentTimeSec();
        }

        /* parse date */
        int YYYY = 0;
        int MM   = 0;
        int DD   = 0;
        if (DateFormat == DATE_FORMAT_YMD) {
            if (dateLen == 8) {
                YYYY = StringTools.parseInt(dateStr.substring(0,4), 0);
                MM   = StringTools.parseInt(dateStr.substring(4,6), 0);
                DD   = StringTools.parseInt(dateStr.substring(6,8), 0);
            } else { // datalen == 6
                YYYY = StringTools.parseInt(dateStr.substring(0,2), 0) + 2000;
                MM   = StringTools.parseInt(dateStr.substring(2,4), 0);
                DD   = StringTools.parseInt(dateStr.substring(4,5), 0);
            }
        } else
        if (DateFormat == DATE_FORMAT_MDY) {
            if (dateLen == 8) {
                MM   = StringTools.parseInt(dateStr.substring(0,2), 0);
                DD   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,8), 0);
            } else { // datalen == 6
                MM   = StringTools.parseInt(dateStr.substring(0,2), 0);
                DD   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,6), 0) + 2000;
            }
        } else
        if (DateFormat == DATE_FORMAT_DMY) {
            if (dateLen == 8) {
                DD   = StringTools.parseInt(dateStr.substring(0,2), 0);
                MM   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,8), 0);
            } else { // datalen == 6
                DD   = StringTools.parseInt(dateStr.substring(0,2), 0);
                MM   = StringTools.parseInt(dateStr.substring(2,4), 0);
                YYYY = StringTools.parseInt(dateStr.substring(4,6), 0) + 2000;
            }
        } else {
            // invalid date format specification
            return DateTime.getCurrentTimeSec();
        }

        /* parse time */
        int hh = StringTools.parseInt(timeStr.substring(0,2), 0);
        int mm = StringTools.parseInt(timeStr.substring(2,4), 0);
        int ss = StringTools.parseInt(timeStr.substring(4,6), 0);
        
        /* return epoch time */
        DateTime dt = new DateTime(gmtTimeZone, YYYY, MM, DD, hh, mm, ss);
        return dt.getTimeSec();

    }
    
    // ------------------------------------------------------------------------

    /* send plain text response */
    private void plainTextResponse(HttpServletResponse response, String errMsg)
        throws ServletException, IOException
    {
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
        PrintWriter out = response.getWriter();
        out.println(errMsg);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // message logging
   
    private static void logDebug(String msg)
    {
        Print.logDebug(LOG_NAME + ": " + msg);
    }

    private static void logInfo(String msg)
    {
        Print.logInfo(LOG_NAME + ": " + msg);
    }

    private static void logWarn(String msg)
    {
        Print.logWarn(LOG_NAME + ": " + msg);
    }

    private static void logError(String URL, String msg)
    {
        if (URL != null) {
            Print.logError(LOG_NAME + ": " + URL);
        }
        Print.logError(LOG_NAME + ": " + msg);
    }

    private static void logException(String URL, String msg, Throwable th)
    {
        if (URL != null) {
            Print.logError(LOG_NAME + ": " + URL);
        }
        Print.logException(LOG_NAME + ": " + msg, th);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

}
