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
//  2006/04/09  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//  2007/03/16  Martin D. Flynn
//     -Added XML output
//  2007/06/30  Martin D. Flynn
//     -Included additional EventData fields in KML output.
//  2007/07/14  Martin D. Flynn
//     -Fixed closing tag name (changed from "</Record>" to "</Event>")
//  2007/09/16  Martin D. Flynn
//     -Added additional XML field support
//  2007/11/28  Martin D. Flynn
//     -Added EventData geozone update option
//     -XML output for Geozone now correctly checks 'getGeozoneID()'
//     -Added City/PostalCode to XML output
//     -Made EventUtil a singleton
//  2008/03/12  Martin D. Flynn
//     -Tool date ranges can now be specified in "YYYY/MM/DD" format
//  2008/12/01  Martin D. Flynn
//     -Added support for optional map event data fields.
//  2009/05/27  Martin D. Flynn
//     -Added speed 'limit' to XML output.
//  2009/07/01  Martin D. Flynn
//     -Map points wrapped in XML "MapData"/"DataSet" tags
//  2009/09/23  Martin D. Flynn, Clifton Flynn
//     -Changed to support SOAP xml encoding
//  2009/10/02  Martin D. Flynn
//     -Modified "getParseMapEventJS" and "formatMapEvent" to include the device
//      vehicle ID in the dataset sent to the client browser.
//  2010/09/09  Martin D. Flynn
//     -Added "DeviceID" column to CSV event output format
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.Version;
import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;
import org.opengts.db.tables.*;

public class EventUtil
{

    // ------------------------------------------------------------------------

    public  static final long   MAX_LIMIT           = 1000L;
    
    private static final long   DFT_CSV_LIMIT       = 30L;

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final int    OPT_MAX_FIELDS      = 3;
    
    public interface OptionalEventFields
    {
        public int    getOptionalEventFieldCount(boolean isFleet);
        public String getOptionalEventFieldTitle(int ndx, boolean isFleet, Locale locale);
        public String getOptionalEventField(int ndx, boolean isFleet, EventDataProvider edp);
    }
    
    private static OptionalEventFields optionalEventFieldHandler = null;
    
    public static void setOptionalEventFieldHandler(OptionalEventFields oef)
    {
        EventUtil.optionalEventFieldHandler = oef;
    }
    
    public static OptionalEventFields getOptionalEventFieldHandler()
    {
        return EventUtil.optionalEventFieldHandler;
    }
    
    public static boolean hasOptionalEventFieldHandler()
    {
        return (EventUtil.optionalEventFieldHandler != null);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public  static final int    FORMAT_UNKNOWN      = 0;
    public  static final int    FORMAT_CSV          = 1;
    public  static final int    FORMAT_KML          = 2;
    public  static final int    FORMAT_XML          = 3;
    public  static final int    FORMAT_TXT          = 4;
    public  static final int    FORMAT_GPX          = 5;
    public static  final int    FORMAT_BML          = 6;

    public static int parseOutputFormat(String fmt, int dftFmt)
    {
        if (fmt == null) {
            return dftFmt;
        } else
        if (fmt.equalsIgnoreCase("csv")) {
            return FORMAT_CSV;
        } else
        if (fmt.equalsIgnoreCase("kml")) {
            return FORMAT_KML;
        } else
        if (fmt.equalsIgnoreCase("xml")) {
            return FORMAT_XML;
        } else
        if (fmt.equalsIgnoreCase("txt")) {
            return FORMAT_TXT;
        } else
        if (fmt.equalsIgnoreCase("gpx")) {
            return FORMAT_GPX;
        } else
        if (fmt.equalsIgnoreCase("bml")) {
            return FORMAT_BML;
        } else {
            return dftFmt;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static EventUtil instance = null;
    public static EventUtil getInstance()
    {
        if (EventUtil.instance == null) {
            EventUtil.instance = new EventUtil();
        }
        return EventUtil.instance;
    }

    static {
        EventUtil.getInstance();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Constructor
    **/
    private EventUtil() 
    {
        super();
    }

    // ------------------------------------------------------------------------

    /**
    *** Write output to specified PrintWriter
    *** @param out  The PrintWriter
    *** @param s    The String to write
    **/
    private void write(PrintWriter pwout, String s)
        throws IOException
    {
        if (s != null) {
            if (pwout != null) {
                pwout.write(s);
                //Print.logInfo(s);
            } else {
                Print.sysPrint(s);
            }
        }
    }

    /**
    *** Flushes the specified PrintStream
    *** @param out  The PrintStream
    **/
    private void flush(PrintWriter pwout)
    {
        if (pwout != null) {
            pwout.flush();
        }
    }

    /**
    *** Print output to specified PrintStream
    *** @param psout  The PrintStream
    *** @param s      The String to print
    **/
    private void print(PrintStream psout, String s)
    {
        if (s != null) {
            if (psout != null) {
                psout.print(s);
            } else {
                Print.sysPrint(s);
            }
        }
    }

    /**
    *** Print output to specified PrintStream, appending a newline after printing the String
    *** @param psout  The PrintStream
    *** @param s      The String to print
    **/
    private void println(PrintStream psout, String s)
    {
        if (s != null) {
            if (psout != null) {
                psout.println(s);
            } else {
                Print.sysPrintln(s);
            }
        }
    }

    // ------------------------------------------------------------------------

    public boolean writeEvents_CSV(PrintWriter pwout, EventData evdata[], BasicPrivateLabel privLabel)
        throws IOException
    {
        boolean  allTags    = false;
        TimeZone tmzone     = null;
        char     csvSep     = ',';
        boolean  inclHeader = true;
        return writeEvents_CSV(pwout, evdata, allTags, tmzone, csvSep, inclHeader, privLabel);
    }
    
    public boolean writeEvents_CSV(PrintWriter pwout, EventData evdata[], boolean allTags, TimeZone tz, 
        char csvSep, boolean inclHeader, BasicPrivateLabel privLabel)
        throws IOException
    {

        /* fields to place in CSV format */
        String evFields[] = null;
        if (allTags) {
            evFields = new String[] {
                EventData.FLD_deviceID,
                // --
                EventData.FLD_timestamp,
                EventData.FLD_statusCode,
                EventData.FLD_latitude,
                EventData.FLD_longitude,
                EventData.FLD_speedKPH,
                EventData.FLD_heading,
                EventData.FLD_altitude,
                EventData.FLD_address,
                // --
                EventData.FLD_gpsAge,
                EventData.FLD_satelliteCount,
                EventData.FLD_inputMask,
                EventData.FLD_odometerKM,
                EventData.FLD_geozoneID,
                EventData.FLD_driverID,
                EventData.FLD_driverMessage,
                // --
                EventData.FLD_fuelTotal,
                EventData.FLD_engineRpm,
                EventData.FLD_engineHours,
                EventData.FLD_vBatteryVolts,
                EventData.FLD_coolantLevel,
                EventData.FLD_coolantTemp,
            };
        } else {
            evFields = new String[] {
                EventData.FLD_deviceID,
                // --
                EventData.FLD_timestamp,
                EventData.FLD_statusCode,
                EventData.FLD_latitude,
                EventData.FLD_longitude,
                EventData.FLD_speedKPH,
                EventData.FLD_heading,
                EventData.FLD_altitude,
                EventData.FLD_address,
            };
        }

        /* write events */
        return this.writeEvents_CSV(pwout, evdata, tz, evFields, csvSep, inclHeader, privLabel);

    }
    
    public boolean writeEvents_CSV(PrintWriter pwout, EventData evdata[], TimeZone tz, 
        String evFields[], char csvSep, boolean inclHeader, BasicPrivateLabel privLabel)
        throws IOException
    {
        // Note: If all of the specified EventData records do not belong to the 
        // same 'deviceID', then 'evFields' should contain the 'deviceID'.

        /* print header */
        if (inclHeader) {
            String hdr = this.formatHeader_CSV(evFields,csvSep) + "\n";
            this.write(pwout, hdr);
        }

        /* date/time format */
        Account acct = (evdata.length > 0)? evdata[0].getAccount() : null;
        String dateFmt = (acct != null)? acct.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (acct != null)? acct.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();

        /* print events */
        for (int i = 0; i < evdata.length; i++) {
            evdata[i].setAccount(acct);
            String rcd = this.formatEventData_CSV(evdata[i], evFields, tz, dateFmt, timeFmt, csvSep) + "\n";
            this.write(pwout, rcd);
        }

        /* flush (output may not occur until the PrintWriter is flushed) */
        this.flush(pwout);
        return true;

    }

    private String formatHeader_CSV(String f[], char csvSep)
    {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < f.length; i++) {
            if (sb.length() > 0) { sb.append(csvSep); }
            if (f[i].equals(EventData.PFLD_deviceDesc)) { // <-- pseudo field
                sb.append("DeviceDesc");
            } else
            if (f[i].equals(EventData.FLD_deviceID)) {
                sb.append("DeviceID");
            } else
            if (f[i].equals(EventData.FLD_timestamp)) {
                // 'timestamp' is separated into "date,time"
                sb.append("Date").append(csvSep).append("Time");
            } else
            if (f[i].equals(EventData.FLD_statusCode)) {
                sb.append("Code");
            } else
            if (f[i].equals(EventData.FLD_latitude)) {
                sb.append("Latitude");
            } else
            if (f[i].equals(EventData.FLD_longitude)) {
                sb.append("Longitude");
            } else
            if (f[i].equals(EventData.FLD_speedKPH)) {
                sb.append("Speed");
            } else
            if (f[i].equals(EventData.FLD_heading)) {
                sb.append("Heading");
            } else
            if (f[i].equals(EventData.FLD_altitude)) {
                sb.append("Altitude");
            } else
            if (f[i].equals(EventData.FLD_address)) {
                sb.append("Address");
            } else
            if (EventData.getFactory().hasField(f[i])) {
                sb.append(f[i]); // field name
            }
        }
        return sb.toString();
    }
    
    public String formatEventData_CSV(EventData evdata, String fields[], 
        TimeZone tz, String dateFmt, String timeFmt, char csvSep)
    {
        StringBuffer sb = new StringBuffer();
        if ((evdata != null) && (fields != null)) {
            Account account = evdata.getAccount();
            BasicPrivateLabel privLabel = account.getPrivateLabel();
            for (int i = 0; i < fields.length; i++) {

                // Pseudo fields here (if any)
                if (fields[i].startsWith(DBRecord.PSEUDO_FIELD_CHAR)) {
                    if (fields[i].equals(EventData.PFLD_deviceDesc)) {
                        if (sb.length() > 0) { sb.append(csvSep); }
                        sb.append(evdata.getDeviceDescription());
                    }
                    continue;
                }

                // DB field
                DBField dbFld = evdata.getRecordKey().getField(fields[i]); // TODO: could be optimized
                Object val = (dbFld != null)? evdata.getFieldValue(fields[i]) : null;
                if (val != null) {
                    // field exists
                    if (sb.length() > 0) { sb.append(csvSep); }
                    Class typeClass = dbFld.getTypeClass();
                    if (fields[i].equals(EventData.FLD_timestamp)) {
                        // format timestamp
                        long time = ((Long)val).longValue();
                        DateTime dt = new DateTime(time);
                        String fmt = dateFmt + csvSep + timeFmt;
                        if (tz == null) {
                            sb.append(dt.gmtFormat(fmt));
                        } else {
                            sb.append(dt.format(fmt,tz));
                        }
                    } else
                    if (fields[i].equals(EventData.FLD_statusCode)) {
                        // return statusCode description
                        //int code = ((Integer)val).intValue();
                        //sb.append(StatusCodes.GetDescription(code));
                        String scd = evdata.getStatusCodeDescription(privLabel);
                        sb.append(scd);
                    } else
                    if ((typeClass == Float.class) || (typeClass == Float.TYPE)) {
                        // generic Float type
                        float d = ((Float)val).floatValue();
                        String fmt = dbFld.getFormat();
                        if ((fmt != null) && !fmt.equals("")) {
                            sb.append(StringTools.format(d,fmt));
                        } else {
                            sb.append(String.valueOf(d));
                        }
                    } else
                    if ((typeClass == Double.class) || (typeClass == Double.TYPE)) {
                        // generic Double type
                        double d = ((Double)val).doubleValue();
                        String fmt = dbFld.getFormat();
                        if ((fmt != null) && !fmt.equals("")) {
                            sb.append(StringTools.format(d,fmt));
                        } else {
                            sb.append(String.valueOf(d));
                        }
                    } else
                    if ((typeClass == Long.class) || (typeClass == Long.TYPE)) {
                        // generic Long type
                        sb.append(val.toString());
                    } else
                    if ((typeClass == Integer.class) || (typeClass == Integer.TYPE)) {
                        // generic Integer type
                        sb.append(val.toString());
                    } else
                    if (fields[i].equals(EventData.FLD_address)) {
                        // format Address
                        String v = val.toString().replace(csvSep,' '); // remove csv separators
                        sb.append(StringTools.quoteString(v)); // always quote address
                    } else {
                        // everything else
                        String v = val.toString().replace(csvSep,' '); // remove csv separators
                        if ((v.indexOf(" ") >= 0) || (v.indexOf('\"') >= 0)) {
                            sb.append(StringTools.quoteString(v));
                        } else {
                            sb.append(v);
                        }
                    }
                }
                
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------------
    // This section specifically handles writing CSV-like encoded data to the map browser
    
    public  static final boolean SEPARATE_DATASET_PER_DEVICE    = false;
    
    public  static final String  TAG_MapData                    = "MapData";
    public  static final String  TAG_Latest                     = "Latest";
    public  static final String  TAG_LastEvent                  = "LastEvent";
    public  static final String  TAG_Time                       = "Time";
    public  static final String  TAG_DataColumns                = "DataColumns";
    public  static final String  TAG_DataSet                    = "DataSet";
    public  static final String  TAG_Point                      = "P";
    public  static final String  TAG_Shape                      = "Shape"; // MapShape

    public  static final String  ATTR_isFleet                   = "isFleet";
    public  static final String  ATTR_type                      = "type";
    public  static final String  ATTR_id                        = "id";
    public  static final String  ATTR_route                     = "route";
    public  static final String  ATTR_routeColor                = "routeColor";
    public  static final String  ATTR_textColor                 = "textColor";
    public  static final String  ATTR_timestamp                 = "timestamp";
    public  static final String  ATTR_timezone                  = "timezone";
    public  static final String  ATTR_device                    = "device";
    public  static final String  ATTR_year                      = "year";
    public  static final String  ATTR_month                     = "month";
    public  static final String  ATTR_day                       = "day";
    public  static final String  ATTR_color                     = "color";
    public  static final String  ATTR_radius                    = "radius";
    public  static final String  ATTR_battery                   = "battery";
    public  static final String  ATTR_signal                    = "signal";

    public  static final String  DSTYPE_device                  = "device";
    public  static final String  DSTYPE_group                   = "group";
    public  static final String  DSTYPE_poi                     = "poi";

  //public  static final String  CSV_SEPARATOR                  = "|";
    public  static final char    CSV_SEPARATOR_CHAR             = '|';

    /* return JavaScript for parsing the formatted CSV EventDataProvider record */
    public String getParseMapEventJS(boolean isFleet, Locale locale)
    {
        return this.getParseMapEventJS(isFleet, locale, CSV_SEPARATOR_CHAR);
    }

    /* return JavaScript for parsing the formatted CSV EventDataProvider record */
    // NOTE: The format parsed here must match the formatter 'formatMapEvent' below
    public String getParseMapEventJS(boolean isFleet, Locale locale, char csvSep)
    {
        StringBuffer js = new StringBuffer();

        js.append("// (generated by 'EventUtil.getParseMapEventJS')\n");
        js.append("function MapEventRecord(csvRcd) {\n");
        js.append("    var fld        = csvRcd.split('" + csvSep + "');\n");
        js.append("    this.index     = 0;\n"); // will be set later
        js.append("    this.valid     = (fld.length > 8);\n"); // must include at least up to latitude/longitude
        js.append("    this.devVIN    = (fld.length > 0)? fld[0] : '';\n");
        js.append("    this.device    = (fld.length > 1)? fld[1] : '';\n");
        js.append("    this.timestamp = (fld.length > 2)? parseInt(fld[2]) : 0;\n");    // epoch
        js.append("    this.dateFmt   = (fld.length > 3)? fld[3] : '';\n");             // date in 'account' format (in selected timezone)
        js.append("    this.timeFmt   = (fld.length > 4)? fld[4] : '';\n");             // time in 'account' format (in selected timezone)
        js.append("    this.timeZone  = (fld.length > 5)? fld[5] : '';\n");             // short timezone name
        js.append("    this.code      = (fld.length > 6)? fld[6] : '';\n");             // status code
        js.append("    this.iconNdx   = (fld.length > 7)? fld[7] : '';\n");             // display icon index
        js.append("    this.latitude  = numParseFloat(((fld.length >  8)? fld[ 8] : '0'), 0);\n");
        js.append("    this.longitude = numParseFloat(((fld.length >  9)? fld[ 9] : '0'), 0);\n");
        js.append("    this.validGPS  = ((this.latitude != 0) || (this.longitude != 0))? true : false;\n");
        js.append("    this.satCount  = (fld.length > 10)? fld[10] : '0';\n");          // satellite count
        js.append("    this.speedKPH  = numParseFloat(((fld.length > 11)? fld[11] : '0'), 0);\n");
        js.append("    this.speedMPH  = this.speedKPH * " + GeoPoint.MILES_PER_KILOMETER + ";\n");
        js.append("    this.heading   = numParseFloat(((fld.length > 12)? fld[12] : '0'), 0);\n");
        js.append("    this.compass   = HEADING[Math.round(this.heading / 45.0) % 8];\n");
        js.append("    this.altitude  = numParseFloat(((fld.length > 13)? fld[13] : '0'), 0); // meters\n");
        js.append("    this.odomKM    = numParseFloat(((fld.length > 14)? fld[14] : '0'), 0); // kilometers\n");
        js.append("    this.address   = (fld.length > 15)? fld[15].trim() : '';\n");
        js.append("    if (this.address.startsWith('\\\"')) { this.address = this.address.substring(1); }\n");
        js.append("    if (this.address.endsWith('\\\"')  ) { this.address = this.address.substring(0, this.address.length - 1); }\n");
      //js.append("    //if (this.address == '') { this.address = '&nbsp;'; }\n"); // fill with space, so field isn't blank
        js.append("    if (fld.length > 16) {\n");
        js.append("        this.optDesc = new Array();\n");
        js.append("        for (var i = 16; i < fld.length; i++) {\n");
        js.append("            this.optDesc.push(fld[i]);\n");
        js.append("        }\n");
        js.append("    }\n");
        js.append("};\n");

        js.append("function OptionalEventFieldCount() {\n");
        js.append("    return "+((EventUtil.optionalEventFieldHandler!=null)?EventUtil.optionalEventFieldHandler.getOptionalEventFieldCount(isFleet):0)+";\n");
        js.append("};\n");

        js.append("function OptionalEventFieldTitle(ndx) {\n");
        if (EventUtil.optionalEventFieldHandler != null) {
            int optFieldCount = EventUtil.optionalEventFieldHandler.getOptionalEventFieldCount(isFleet);
            if (optFieldCount > 0) {
                js.append("    switch (ndx) {\n");
                for (int i = 0; i < optFieldCount; i++) {
                    String t = EventUtil.optionalEventFieldHandler.getOptionalEventFieldTitle(i, isFleet, null);
                    js.append("        case "+i+": return \""+t+"\";\n");
                }
                js.append("    }\n");
            }
        }
        js.append("    return '';\n");
        js.append("};\n");

        return js.toString();
    }

    /* write encoded map event data to the specified PrintWriter */
    public boolean writeMapEvents(
        int indentLevel, PrintWriter pwout, boolean isSoapRequest,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[], boolean includeShapes,
        String iconSelector, OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, 
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM)
        throws IOException
    {
        return this.writeMapEvents(
            indentLevel, pwout, isSoapRequest,
            privLabel,
            edp, includeShapes,
            iconSelector, iconKeys,
            isFleet, fleetRoute, selID,
            tmz,
            acct,
            latestTime, lastBattery, lastSignal,
            minProximityM,
            CSV_SEPARATOR_CHAR);
    }

    /* write encoded map event data to the specified PrintWriter */
    public boolean writeMapEvents(
        int indentLevel, PrintWriter pwout, boolean isSoapRequest,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[],  boolean includeShapes,
        String iconSelector, OrderedSet<String>iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID,
        TimeZone tmz, 
        Account acct, 
        DateTime latestTime, double lastBattery, double lastSignal,
        double minProximityM,
        char csvSep)
        throws IOException
    {
        // <MapData>
        //   <Time timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD">YYYY/MM/DD|hh:mm:ss</Time>
        //   <LastEvent device="DEVICE" timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD">YYYY/MM/DD|hh:mm:ss</LastEvent>
        //   <DataSet type="poi">
        //     <P>POIDesc|||0|Latitude|Longitude|0.0|0.0|0.0|Address</P>
        //   </DataSet>
        //   <DataSet type="device" id="deviceid" route="true">
        //     <P>DeviceDesc|Data|Time|StatusCode|Latitude|Longitude|SpeedKPH|Heading|Altitude|Address</P>
        //   </DataSet>
        // </MapData>

        /* date/time format */
        String dateFmt = (acct != null)? acct.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (acct != null)? acct.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();
        
        /* TimeZone */
        if ((acct != null) && (tmz == null)) { 
            tmz = acct.getTimeZone(null); 
        }
        String tmzStr = null;
        //tmzStr = (tmz != null)? tmz.getID() : null;
        //tmzStr = (tmz != null)? tmz.getDisplayName(true,TimeZone.SHORT) : null;

        /* XML header */
        String PFX1 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest, indentLevel   *3) : "";
        String PFX2 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest,(indentLevel+1)*3) : "";
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_MapData,
            XMLTools.ATTR(ATTR_isFleet,isFleet),
            false,true));

        /* today time */
        // <Time timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD">YYYY/MM/DD|hh:mm:ss</Time>
        DateTime today = new DateTime(tmz);
        String   todayTmzFmt = (tmzStr != null)? tmzStr : today.format("zzz",tmz);
        this.write(pwout, PFX2);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Time,
            XMLTools.ATTR(ATTR_timestamp, today.getTimeSec()) +
            XMLTools.ATTR(ATTR_timezone , todayTmzFmt) +
            XMLTools.ATTR(ATTR_year     , today.getYear(tmz)) +
            XMLTools.ATTR(ATTR_month    , today.getMonth1(tmz)) +
            XMLTools.ATTR(ATTR_day      , today.getDayOfMonth(tmz)),
            false,false));
        this.write(pwout, today.format(dateFmt,tmz) + csvSep + today.format(timeFmt,tmz));
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Time,true));

        /* latest event? */
        // <LastEvent device="DEVICE" timestamp="EPOCH" timezone="TMZ" year="YYYY" month="MM" day="DD" battery="0.82" signal="0.45">YYYY/MM/DD|hh:mm:ss</LastEvent>
        if (!isFleet && (latestTime != null)) {
            String lastTmzFmt = (tmzStr != null)? tmzStr : latestTime.format("zzz",tmz);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_LastEvent,
                XMLTools.ATTR(ATTR_device   , selID) +
                XMLTools.ATTR(ATTR_timestamp, latestTime.getTimeSec()) +
                XMLTools.ATTR(ATTR_timezone , lastTmzFmt) +
                XMLTools.ATTR(ATTR_year     , latestTime.getYear(tmz)) +
                XMLTools.ATTR(ATTR_month    , latestTime.getMonth1(tmz)) +
                XMLTools.ATTR(ATTR_day      , latestTime.getDayOfMonth(tmz)) +
                XMLTools.ATTR(ATTR_battery  , lastBattery) +
                XMLTools.ATTR(ATTR_signal   , lastSignal),
                false,false));
            this.write(pwout, 
                latestTime.format(dateFmt,tmz) + csvSep + 
                latestTime.format(timeFmt,tmz));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_LastEvent,true));
        }
        
        /* map shapes (EXPERIMENTAL) [MapShape] */
        // <Shape type="circle" radius="0" color="#FF0000"><![CDATA[ <lat>/<lon>,... ]]></Shape>
        if (includeShapes && (edp != null) && (acct != null)) {
            Set<String> zoneShapes = null;
            for (EventDataProvider e : edp) {
                String zid = e.getGeozoneID();
                if (!zid.equals("")) {
                    if (zoneShapes == null) { zoneShapes = new HashSet<String>(); }
                    if (!zoneShapes.contains(zid)) {
                        Geozone zone[] = null;
                        try {
                            zone = Geozone.getGeozone(acct, zid);
                        } catch (DBException dbe) {
                            zone = null;
                        }
                        zoneShapes.add(zid); // add even if 'zone' is null
                        if (zone != null) {
                            for (Geozone z : zone) {
                                String type = "circle";
                                switch (Geozone.getGeozoneType(z)) {
                                    case POINT_RADIUS: type = "circle";    break;
                                    case BOUNDED_RECT: type = "rectangle"; break;
                                    case POLYGON     : type = "polygon";   break;
                                    default          : continue; // not supported
                                }
                                int radiusM   = z.getRadius();
                                String color  = z.getShapeColor("#00FF00");
                                GeoPoint gp[] = z.getGeoPoints();
                                //Print.logInfo("Writing Shape: type="+type + " ["+StringTools.join(gp,",")+"]");
                                this.write(pwout, PFX2);
                                this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Shape,
                                    XMLTools.ATTR(ATTR_type     , type) +
                                    XMLTools.ATTR(ATTR_radius   , radiusM) +
                                    XMLTools.ATTR(ATTR_color    , color),
                                    false,false));
                                this.write(pwout, XMLTools.CDATA(isSoapRequest,StringTools.join(gp,",")));
                                this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Shape,true));
                            }
                        }
                    }
                }
            }
        }

        /* column headers */
        this.write(pwout, PFX2);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DataColumns,"",false,false));
        this.write(pwout, XMLTools.CDATA(isSoapRequest,"Desc|Epoch|Date|Time|Tmz|Stat|Icon|Lat|Lon|#Sats|kph|Heading|Alt|Addr"));
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataColumns,true));
        
        // <DataSet type="poi">
        this._writeMapPoi(
            (indentLevel>0)?(indentLevel+1):0, pwout, isSoapRequest,
            privLabel,
            this._getPOI(((acct != null)? acct.getAccountID() : null), privLabel), 
            iconKeys, 
            csvSep);

        // <DataSet type="device" id="deviceid" route="true">
        boolean rtn = this._writeMapEvents(
            (indentLevel>0)?(indentLevel+1):0, pwout, isSoapRequest,
            privLabel,
            edp, 
            iconSelector, iconKeys, 
            isFleet, fleetRoute, selID,
            tmz, dateFmt, timeFmt, 
            csvSep,
            minProximityM);

        /* XML footer */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_MapData,true));

        /* flush (output may not occur until the PrintWriter is flushed) */
        this.flush(pwout);
        return rtn;

    }

    /* write encoded map points-of-interest to the specified PrintWriter */
    private boolean _writeMapPoi(
        int indentLevel, PrintWriter pwout, boolean isSoapRequest,
        BasicPrivateLabel privLabel,
        PoiProvider poip[], 
        OrderedSet<String> iconKeys, 
        char csvSep)
        throws IOException
    {

        /* valid EventDataProvider? */
        if (ListTools.isEmpty(poip)) {
            //Print.logInfo("No PointsOfInterest ...");
            return false;
        }

        /* indent */
        String PFX1 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest, indentLevel   *3) : "";
        String PFX2 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest,(indentLevel+1)*3) : "";

        /* header */
        String type = DSTYPE_poi;
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DataSet,
            XMLTools.ATTR(ATTR_type     , type ) +
            XMLTools.ATTR(ATTR_route    , false),
            false,true));

        /* points of interest */
        for (int i = 0; i < poip.length; i++) {
            final PoiProvider pp = poip[i];
            EventDataProvider edp = new EventDataProviderAdapter() {
                public String getAccountID()         { return pp.getAccountID(); }
                public String getDeviceID()          { return pp.getPoiID(); }
                public String getDeviceDescription() { return pp.getPoiDescription(); }
                public double getLatitude()          { return pp.getLatitude(); }
                public double getLongitude()         { return pp.getLongitude(); }
                public String getAddress()           { return pp.getAddress(); }
                public int    getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys, boolean isFleet, BasicPrivateLabel bpl) { return pp.getPushpinIconIndex(iconKeys,bpl); }
            };
            String rcd = this.formatMapEvent(privLabel, edp,
                null, iconKeys, false,
                null, null, null, csvSep);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Point,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,rcd));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Point,true));
        }

        /* footer */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataSet,true));

        /* flush (output may not occur until the PrintWriter is flushed) */
        this.flush(pwout);
        return true;

    }

    /* write encoded map event data to the specified PrintWriter */
    private boolean _writeMapEvents(
        int indentLevel, PrintWriter pwout, boolean isSoapRequest,
        BasicPrivateLabel privLabel,
        EventDataProvider edp[], 
        String iconSelector, OrderedSet<String> iconKeys, 
        boolean isFleet, boolean fleetRoute, String selID, // "selID" is either a DeviceID or GroupID
        TimeZone tmz, 
        String dateFmt, String timeFmt, 
        char csvSep,
        double minProximityM)
        throws IOException
    {

        /* valid EventDataProvider? */
        if (ListTools.isEmpty(edp)) {
            return false;
        }

        /* use custom Device 'displayColor' for routeLine color? */
        boolean useRouteDisplayColor = (privLabel != null)?
            privLabel.getBooleanProperty(BasicPrivateLabel.PROP_TrackMap_useRouteDisplayColor, true) :
            true;

        /* indent */
        String PFX1 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest, indentLevel   *3) : "";
        String PFX2 = (indentLevel > 0)? XMLTools.PREFIX(isSoapRequest,(indentLevel+1)*3) : "";

        /* print events */
        boolean  isDeviceData = !isFleet;
        boolean  didStartSet  = false;
        GeoPoint lastGP       = null;
        String   lastDevID    = "";
        String   routeColor   = "";
        String   textColor    = "";
        for (int i = 0; i < edp.length; i++) {
            String thisDevID = edp[i].getDeviceID();
            //Print.logInfo(i + ") Event: " + thisDevID);

            /* first device? */
            if (!thisDevID.equals(lastDevID)) {
                if (isFleet /*&& fleetRoute*/) {
                    if (didStartSet) {
                        // close previous dataset
                        this.write(pwout, PFX1);
                        this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataSet,true));
                        didStartSet = false;
                    }
                    isDeviceData = true;
                    selID        = thisDevID;
                }
                lastDevID    = thisDevID;
                lastGP       = null;
                textColor    = "";
                routeColor   = "";
                if ((edp[i] instanceof EventData) && (isFleet || useRouteDisplayColor)) {
                    Device dev = ((EventData)edp[i]).getDevice();
                    if ((dev != null) && dev.hasDisplayColor()) {
                        if (isFleet) {
                            textColor = dev.getDisplayColor();
                        }
                        if (useRouteDisplayColor) {
                            routeColor = dev.getDisplayColor();
                        }
                    }
                }
            }

            /* last device? */
            if ((i + 1) >= edp.length) {
                // last event in list
                edp[i].setIsLastEvent(true);
            } else {
                String nextDevID = edp[i + 1].getDeviceID();
                if (!thisDevID.equals(nextDevID)) {
                    // DeviceID will change on next iteration
                    edp[i].setIsLastEvent(true);
                }
            }

            /* trim events in close proximity */
            if (minProximityM > 0.0) {
                // check proximity to established target location (range 20-200 metera)
                double lat = edp[i].getLatitude();
                double lon = edp[i].getLongitude();
                if (GeoPoint.isValid(lat,lon)) {
                    // guarantee that this point is valid
                    GeoPoint thisGP = new GeoPoint(lat,lon);
                    if (lastGP == null) {
                        // the first 'last' event, set target location and continue
                        lastGP = thisGP;
                    } else
                    if (thisGP.metersToPoint(lastGP) >= minProximityM) {
                        // outside tolerance zone, set new target location and continue
                        lastGP = thisGP;
                    } else {
                        // inside tolerance zone, skip this event
                        //Print.logError(i + ") Skipping this record!");
                        continue;
                    }
                }
            }

            /* start "DataSet" (if not already started) */
            if (!didStartSet) {
                String type = isDeviceData? DSTYPE_device : DSTYPE_group; // "poi"
                this.write(pwout, PFX1);
                this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_DataSet,
                    XMLTools.ATTR(ATTR_type      , type        ) +
                    XMLTools.ATTR(ATTR_id        , selID       ) +
                    XMLTools.ATTR(ATTR_route     , isDeviceData) +
                    XMLTools.ATTR(ATTR_routeColor, routeColor  ) +
                    XMLTools.ATTR(ATTR_textColor , textColor   ),
                    false,true));
                didStartSet = true;
                //Print.logWarn(i + ") New DataSet: " + selID);
            }

            /* fleet icon */
            boolean showFleetIcon;
            if (!isFleet) {
                // not a 'fleet' map, do not show fleet icon
                showFleetIcon = false;
            } else {
                String sfi = privLabel.getStringProperty(BasicPrivateLabel.PROP_TrackMap_showFleetMapDevicePushpin,"");
                if (StringTools.isBlank(sfi) || sfi.equalsIgnoreCase("default")) {
                    if (!fleetRoute) {
                        // fleet map, single point, show fleet icon
                        showFleetIcon = true;
                    } else {
                        // fleet map, multiple points, show fleet icon if last event
                        showFleetIcon = edp[i].getIsLastEvent();
                    }
                } else {
                    // 'true' will display all device pushpins
                    // 'false' will display the default pushpins
                    showFleetIcon = StringTools.parseBoolean(sfi,false);
                }
            }

            /* format and print event */
            //Print.logInfo("["+thisDevID+"] iconSelector='"+iconSelector+"' showFleetIcon=" + showFleetIcon);
            String rcd = this.formatMapEvent(privLabel, edp[i],
                iconSelector, iconKeys, showFleetIcon,
                tmz, dateFmt, timeFmt, csvSep);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,TAG_Point,"",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,rcd));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_Point,true));
            //Print.logWarn(i + ") Write Event: " + selID);

        } // looping through events

        /* Dataset footer */
        if (didStartSet) {
            this.write(pwout, PFX1);
            this.write(pwout, XMLTools.endTAG(isSoapRequest,TAG_DataSet,true));
        }

        return true;

    }

    /* encode a single map event record */
    public String formatMapEvent(
        BasicPrivateLabel privLabel,
        EventDataProvider edp,
        String iconSelector, OrderedSet<String>iconKeys, boolean isFleet,
        TimeZone tmz, String dateFmt, String timeFmt)
    {
        char cvsSep = CSV_SEPARATOR_CHAR;
        return this.formatMapEvent(privLabel, edp, 
            iconSelector, iconKeys, isFleet, 
            tmz, dateFmt, timeFmt, cvsSep);
    }

    /* encode a single map event record */
    // NOTE: The format encoded here must match the parser 'getParseMapEventJS' above
    public String formatMapEvent(
        BasicPrivateLabel privLabel, 
        EventDataProvider edp,
        String iconSelector, OrderedSet<String> iconKeys, boolean isFleet,
        TimeZone tmz, String dateFmt, String timeFmt, char csvSep)
    {
        // ID|DeviceDesc|Epoch|Date|Time|Timezone|StatusCode|Icon|Latitude|Longitude|Satellites|SpeedKPH|Heading|Altitude|Address

        /* valid EventDataProvider? */
        if (edp == null) {
            return "";
        }

        /* start record assembly */
        StringBuffer sb = new StringBuffer();

        /* [ 0] VehicleID */
        sb.append(edp.getDeviceVIN());
        sb.append(csvSep);

        /* [ 1] DeviceDesc */
        sb.append(edp.getDeviceDescription());
        sb.append(csvSep);
        
        /* [ 2] Timestamp */
        long time = edp.getTimestamp();
        sb.append(time);
        sb.append(csvSep);

        /* [3,4] Date|Time */
        //Print.logInfo("Specified TimeZone: " + tmz);
        //Print.logInfo("Actual    TimeZone: " + dt.getTimeZone());
        DateTime dt = null;
        if (dateFmt != null) {
            dt = new DateTime(time,tmz);
            String dtfmt = dateFmt + csvSep + timeFmt;
            sb.append(dt.format(dtfmt));
        } else {
            sb.append(csvSep);
        }
        sb.append(csvSep);

        /* [ 5] TimeZone (short name) */
        if (dt != null) {
            //sb.append(dt.getTimeZoneShortName());
            sb.append(dt.format("zzz",tmz)); // PDT
        }
        sb.append(csvSep);

        /* [ 6] StatusCode */
        sb.append(edp.getStatusCodeDescription(privLabel));
        sb.append(csvSep);

        /* [ 7] Icon */
        sb.append(edp.getPushpinIconIndex(iconSelector, iconKeys, isFleet, privLabel));
        sb.append(csvSep);

        /* [ 8] Latitude (6 decimal places) */
        sb.append(StringTools.format(edp.getLatitude(),"0.000000"));
        sb.append(csvSep);

        /* [ 9] Longitude (6 decimal places) */
        sb.append(StringTools.format(edp.getLongitude(),"0.000000"));
        sb.append(csvSep);

        /* [10] Satellite Count */
        sb.append(String.valueOf(edp.getSatelliteCount()));
        sb.append(csvSep);

        /* [11] SpeedKPH */
        sb.append(StringTools.format(edp.getSpeedKPH(),"0.0"));
        sb.append(csvSep);

        /* [12] Heading */
        sb.append(StringTools.format(edp.getHeading(),"0.0"));
        sb.append(csvSep);

        /* [13] Altitude (meters) */
        sb.append(StringTools.format(edp.getAltitude(),"0"));
        sb.append(csvSep);

        /* [14] Odometer (kilometers) */
        sb.append(StringTools.format(edp.getOdometerKM(),"0.0"));
        sb.append(csvSep);

        /* [15] Address */
        sb.append(StringTools.quoteString(edp.getAddress().replace(csvSep,' ')));

        /* [16+] other fields? */
        if (EventUtil.optionalEventFieldHandler != null) {
            int optFieldCount = EventUtil.optionalEventFieldHandler.getOptionalEventFieldCount(isFleet);
            for (int i = 0; i < optFieldCount; i++) {
                String v = StringTools.trim(EventUtil.optionalEventFieldHandler.getOptionalEventField(i,isFleet,edp));
                sb.append(csvSep);
                sb.append(v.replace(csvSep,' '));
            }
        }

        /* return CSV record */
        return sb.toString();

    }
    
    // ------------------------------------------------------------------------

    private PoiProvider[] _getPOI(String accountID, BasicPrivateLabel privLabel)
    {
        java.util.List<PoiProvider> poiList = privLabel.getPointsOfInterest();
        if (!ListTools.isEmpty(poiList)) {
            //Print.logInfo("POI Count: %d", poiList.size());
            return poiList.toArray(new PoiProvider[poiList.size()]);
        } else {
            //Print.logInfo("POI Count: none");
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // <?xml version=\"1.0\" encoding=\"UTF-8\"?>
    // <EventData account="account" timezone="US/Pacific">
    //    <Event device="device">
    //       <Timestamp epoch="1183397093">yyyy/MM/dd HH:mm:ss</Timestamp>
    //       <StatusCode code="0xF112">IN-MOTION</StatusCode>
    //       <Entity>entity</Entity>
    //       <GPSPoint age="5">35.12345/-135.12345</GPSPoint>
    //       <Speed units="kph">113.0</Speed>
    //       <Heading degrees="21.0">N</Heading>
    //       <Altitude units="meters">567</Altitude>
    //       <Odometer units="Km">123456.0</Odometer>
    //       <Sensor type="low">0xAA112233</Sensor>
    //       <Sensor type="high">0xAA112233</Sensor>
    //    </Event>
    // </EventData>

    public void writeEvents_XML_Event(PrintWriter pwout, EventData ev, int indent, boolean allTags, BasicPrivateLabel privLabel)
        throws IOException
    {
        boolean isSoapRequest = false;
        Account account = ev.getAccount();
        Device  device  = ev.getDevice();
        Locale  locale  = privLabel.getLocale(); // should be "reqState.getLocale();"
        String  PFX1    = XMLTools.PREFIX(isSoapRequest, indent);
        String  PFX2    = XMLTools.PREFIX(isSoapRequest, 2 * indent);

        /* Event tag start */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,"Event",
            XMLTools.ATTR("device",ev.getDeviceID()),
            false,true));

        // Timestamp
        long timestamp = ev.getTimestamp();
        if (allTags || (timestamp > 0L)) {
            TimeZone tz = account.getTimeZone(null);
            DateTime ts = new DateTime(timestamp); // 'tz' used below
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Timestamp",
                XMLTools.ATTR("epoch",timestamp),
                false,false));
            this.write(pwout, ts.format("yyyy/MM/dd HH:mm:ss zzz",tz));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Timestamp",true));
        }
        
        // StatusCode
        int statusCode = ev.getStatusCode();
        String code = "0x" + StringTools.toHexString(statusCode, 16);
        String desc = ev.getStatusCodeDescription(privLabel);
        this.write(pwout, PFX2);
        this.write(pwout, XMLTools.startTAG(isSoapRequest,"StatusCode",
            XMLTools.ATTR("code",code),
            false,false));
        this.write(pwout, XMLTools.CDATA(isSoapRequest,desc));
        this.write(pwout, XMLTools.endTAG(isSoapRequest,"StatusCode",true));

        // GPSPoint
        GeoPoint geoPoint = ev.getGeoPoint();
        if (allTags || geoPoint.isValid()) {
            long age = ev.getGpsAge();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"GPSPoint",
                ((allTags || (age > 0))?XMLTools.ATTR("age",age):""),
                false,false));
            // satellite count?
            this.write(pwout, geoPoint.toString(','));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"GPSPoint",true));
        }
        
        // SpeedKPH
        double speedKPH = ev.getSpeedKPH();
        if (allTags || (speedKPH >= 0.0)) {
            double speed = Account.getSpeedUnits(account).convertFromKPH(speedKPH);
            String units = Account.getSpeedUnits(account).toString(locale);
            double speedLimKPH = ev.getSpeedLimitKPH();
            double limit = (speedLimKPH > 0.0)? Account.getSpeedUnits(account).convertFromKPH(speedLimKPH) : 0.0;
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Speed",
                XMLTools.ATTR("units",units) +
                ((limit>0.0)?XMLTools.ATTR("limit",StringTools.format(limit,"0.0")):""),
                false,false));
            this.write(pwout, StringTools.format(speed,"0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Speed",true));
        }
        
        // Heading (only if speed is > 0)
        if (allTags || (speedKPH > 0.0)) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Heading",
                XMLTools.ATTR("degrees",StringTools.format(ev.getHeading(),"0.0")),
                false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,GeoPoint.GetHeadingString(ev.getHeading(),locale)));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Heading",true));
        }
        
        // Altitude
        double altitudeM = ev.getAltitude();
        if (allTags || (altitudeM > 0.0)) {
            double altitude = altitudeM;
            String units = "meters";
            int distUnits = account.getDistanceUnits();
            if (Account.getDistanceUnits(account).isMiles()) {
                altitude = altitudeM * GeoPoint.FEET_PER_METER;
                units = "feet";
            }
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Altitude",
                XMLTools.ATTR("units",units),
                false,false));
            this.write(pwout, StringTools.format(altitude,"#0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Altitude",true));
        }

        // Odometer
        double odomKM = ev.getOdometerKM() + device.getOdometerOffsetKM(); // ok
        if (allTags || (odomKM > 0.0)) {
            double odometer = Account.getDistanceUnits(account).convertFromKM(odomKM);
            String units    = Account.getDistanceUnits(account).toString(locale);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Odometer",
                XMLTools.ATTR("units",units),
                false,false));
            this.write(pwout, StringTools.format(odometer,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Odometer",true));
        }

        // Geozone
        String geozoneID = ev.getGeozoneID();
        long geozoneNdx  = ev.getGeozoneIndex();
        if (allTags || !geozoneID.equals("") || (geozoneNdx > 0L)) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Geozone",
                XMLTools.ATTR("index",geozoneNdx),
                false,false));
            this.write(pwout, geozoneID);
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Geozone",true));
        }

        // Address
        String address = ev.getAddress();
        if (allTags || !address.equals("")) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"Address","",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,address));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"Address",true));
        }

        // City
        String city = ev.getCity();
        if (allTags || !city.equals("")) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"City","",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,city));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"City",true));
        }

        // PostalCode
        String postalCode = ev.getPostalCode();
        if (allTags || !postalCode.equals("")) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"PostalCode","",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,postalCode));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"PostalCode",true));
        }

        // DigitalImputMask
        long inputMask = ev.getInputMask();
        if (allTags || (inputMask != 0L)) {
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"DigitalImputMask","",false,false));
            this.write(pwout, StringTools.toHexString(inputMask));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"DigitalImputMask",true));
        }

        // [allTags] DriverID
        if (allTags && EventData.getFactory().hasField(EventData.FLD_driverID)) {
            String driverID = ev.getDriverID();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"DriverID","",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,driverID));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"DriverID",true));
        }

        // [allTags] DriverMessage
        if (allTags && EventData.getFactory().hasField(EventData.FLD_driverMessage)) {
            String driverMsg = ev.getDriverMessage();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"DriverMessage","",false,false));
            this.write(pwout, XMLTools.CDATA(isSoapRequest,driverMsg));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"DriverMessage",true));
        }

        // [allTags] EngineRPM
        if (allTags && EventData.getFactory().hasField(EventData.FLD_engineRpm)) {
            long engineRpm = ev.getEngineRpm();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"EngineRPM","",false,false));
            this.write(pwout, String.valueOf(engineRpm));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"EngineRPM",true));
        }

        // [allTags] EngineHours
        if (allTags && EventData.getFactory().hasField(EventData.FLD_engineHours)) {
            double engineHours = ev.getEngineHours();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"EngineHours","",false,false));
            this.write(pwout, StringTools.format(engineHours,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"EngineHours",true));
        }

        // [allTags] VehicleBatteryVolts
        if (allTags && EventData.getFactory().hasField(EventData.FLD_vBatteryVolts)) {
            double battVolts = ev.getVBatteryVolts();
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"VehicleBatteryVolts","",false,false));
            this.write(pwout, StringTools.format(battVolts,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"VehicleBatteryVolts",true));
        }

        // [allTags] EngineCoolantLevel
        if (allTags && EventData.getFactory().hasField(EventData.FLD_coolantLevel)) {
            double pct100 = ev.getCoolantLevel() * 100.0;
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"EngineCoolantLevel",
                XMLTools.ATTR("units","percent"),
                false,false));
            this.write(pwout, StringTools.format(pct100,"#0.0"));
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"EngineCoolantLevel",true));
        }

        // [allTags] EngineCoolantTemperature
        if (allTags && EventData.getFactory().hasField(EventData.FLD_coolantTemp)) {
            Account.TemperatureUnits tempUnits = Account.getTemperatureUnits(account);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"EngineCoolantTemperature",
                XMLTools.ATTR("units",tempUnits.toString()),
                false,false));
            double tempC = ev.getCoolantTemp();
            if (tempC > 0.0) {
                double temp = tempUnits.convertFromC(tempC);
                this.write(pwout, StringTools.format(temp,"#0.0"));
            }
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"EngineCoolantTemperature",true));
        }

        // [allTags] EngineFuelUsed
        if (allTags && EventData.getFactory().hasField(EventData.FLD_fuelTotal)) {
            Account.VolumeUnits volUnits = Account.getVolumeUnits(account);
            this.write(pwout, PFX2);
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"EngineFuelUsed",
                XMLTools.ATTR("units",volUnits.toString()),
                false,false));
            double fuelL = ev.getFuelTotal();
            if (fuelL > 0.0) {
                double fuel = volUnits.convertFromLiters(fuelL);
                this.write(pwout, StringTools.format(fuel,"#0.0"));
            }
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"EngineFuelUsed",true));
        }

        /* Event tag end */
        this.write(pwout, PFX1);
        this.write(pwout, XMLTools.endTAG(isSoapRequest,"Event",true));

    }

    public void writeEvents_XML_EventData(PrintWriter pwout, String accountID, String tz, boolean startTag)
        throws IOException
    {
        boolean isSoapRequest = false;
        if (startTag) {
            this.write(pwout, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            this.write(pwout, XMLTools.startTAG(isSoapRequest,"EventData",
                XMLTools.ATTR("account",accountID) + 
                XMLTools.ATTR("timezone",tz),
                false,true));
        } else {
            this.write(pwout, XMLTools.endTAG(isSoapRequest,"EventData",true));
        }
    }

    public boolean writeEvents_XML(PrintWriter pwout, EventData evdata[], boolean allTags, BasicPrivateLabel privLabel)
        throws IOException
    {
        // This does assume that all events belong to the same "Account"

        /* account info */
        Account account = ((evdata != null) && (evdata.length > 0))? evdata[0].getAccount() : null;
        if (account == null) {
            return false;
        }
        String accountID = account.getAccountID();
        String tzStr = account.getTimeZone();
        if ((tzStr == null) || tzStr.equals("")) {
            tzStr = DateTime.GMT_TIMEZONE;
        }

        /* header */
        this.writeEvents_XML_EventData(pwout, accountID, tzStr, true);

        /* event data */
        if (evdata != null) {
            for (int i = 0; i < evdata.length; i++) {
                if (evdata[i].getAccountID().equals(accountID)) {
                    evdata[i].setAccount(account);
                    this.writeEvents_XML_Event(pwout, evdata[i], 2, allTags, privLabel);
                }
            }
        }

        /* trailer */
        this.writeEvents_XML_EventData(pwout, null, null, false);
        this.flush(pwout); // flush (output may not occur this the PrintWriter is flushed)
        return true;

    }

    // ------------------------------------------------------------------------
    // <?xml version="1.0" encoding="UTF-8"?>
    // <gpx version="1.0"
    //      creator="OpenGTS - http://www.opengts.org"
    //      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    //      xmlns="http://www.topografix.com/GPX/1/0"
    //      xsi:schemaLocation="http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd">
    //    <time>2009-05-30T12:48:43Z</time>
    //    <wpt lat="39.4431641" lon="-142.7295456">
    //      <name>Device</name>
    //      <ele>1234.5</ele>
    //    </wpt>
    //    <trk>
    //      <name>Device</name>
    //      <src>GPS Tracking Device</src>
    //      <trkseg>
    //        <trkpt lat="39.4431641" lon="-142.7295456">
    //          <time>2009-05-30T12:48:43Z</time>
    //          <ele>1234.5</ele>
    //        </trkpt>
    //        <trkpt lat="39.4431641" lon="-142.7295456">
    //          <time>2009-05-30T12:48:43Z</time>
    //          <ele>1234.5</ele>
    //        </trkpt>
    //      </trkseg>
    //    </trk>
    // </gpx>

    public boolean writeEvents_GPX(PrintWriter pwout, EventData evdata[], BasicPrivateLabel privLabel)
        throws IOException
    {
        String dateFmt = "yyyy-MM-dd'T'HH:mm:ss'Z'";

        /* account info */
        Account account = !ListTools.isEmpty(evdata)? evdata[0].getAccount() : null;
        if (account == null) {
            return false;
        }
        TimeZone tz = DateTime.getGMTTimeZone();

        /* header */
        this.write(pwout, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        this.write(pwout, "<gpx version=\"1.0\"\n");
        this.write(pwout, "    creator=\"OpenGTS "+Version.getVersion()+" - http://www.opengts.org\"\n");
        this.write(pwout, "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        this.write(pwout, "    xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">\n");
        this.write(pwout, "  <time>" + (new DateTime(tz)).format(dateFmt) + "</time>\n");
        
        /* track body */
        Device lastDev   = null;
        String lastDevID = null;
        for (int i = 0; i < evdata.length; i++) {
            String thisDevID = evdata[i].getDeviceID();
            // Device change
            if (!thisDevID.equals(lastDevID)) {
                if (lastDevID != null) {
                    this.write(pwout, "  </trkseg>\n");
                    this.write(pwout, "  </trk>\n");
                    // we're done with the last device
                }
                lastDevID = thisDevID;
                lastDev   = evdata[i].getDevice();
                this.write(pwout, "  <trk>\n");
                this.write(pwout, "  <name><![CDATA["+lastDevID+"]]></name>\n");
                this.write(pwout, "  <desc><![CDATA["+((lastDev!=null)?lastDev.getDescription():"?")+"]]></desc>\n");
                this.write(pwout, "  <trkseg>\n");
            }
            // Data
            this.write(pwout, "    <trkpt lat=\"" + evdata[i].getLatitude() + "\" lon=\"" + evdata[i].getLongitude() + "\">\n");
            this.write(pwout, "      <time>" + (new DateTime(evdata[i].getTimestamp(),tz)).format(dateFmt) + "</time>\n");
            this.write(pwout, "      <ele>"+evdata[i].getAltitude()+"</ele>\n"); // meters
            this.write(pwout, "    </trkpt>\n");
        }
        if (lastDevID != null) {
            this.write(pwout, "  </trkseg>\n");
            this.write(pwout, "  </trk>\n");
        }

        /* footer */
        this.write(pwout, "</gpx>\n");

        return false;
    }

    // ------------------------------------------------------------------------

    public boolean writeEvents_BML(PrintWriter pwout, EventData evdata[], BasicPrivateLabel privLabel)
        throws IOException
    {
        pwout.write("<lbs>\n");
        for (EventData ev : evdata) {
            pwout.write("<location lon=\""+ev.getLongitude()+"\" lat=\""+ev.getLatitude()+"\"");
            pwout.write(" label=\""+ev.getDeviceID()+"\"");
            pwout.write(" description=\""+ev.getAddress()+"\"");
            // other options available as well
            pwout.write("/>\n");
        }
        pwout.write("</lbs>\n");
        return false;
    }

    // ------------------------------------------------------------------------

    public boolean writeEvents(OutputStream out, 
        EventData evdata[], 
        int formatEnum, boolean allTags, BasicPrivateLabel privLabel)
        throws IOException
    {
        PrintWriter pwout = (out != null)? new PrintWriter(out) : null;
        return this.writeEvents(pwout, evdata, formatEnum, allTags, privLabel);
    }
    
    public boolean writeEvents(PrintWriter pwout, 
        EventData evdata[], 
        int formatEnum, boolean allTags, BasicPrivateLabel privLabel)
        throws IOException
    {
        if (evdata != null) {
            switch (formatEnum) {
                case FORMAT_TXT:
                case FORMAT_CSV:
                    return this.writeEvents_CSV(pwout, evdata, allTags, null/*TimeZone*/, ',', true/*inclHeader*/, privLabel);
                case FORMAT_KML:
                    return GoogleKML.getInstance().writeEvents(pwout, evdata, privLabel);
                case FORMAT_XML:
                    return this.writeEvents_XML(pwout, evdata, allTags, privLabel);
                case FORMAT_GPX:
                    return this.writeEvents_GPX(pwout, evdata, privLabel);
                case FORMAT_BML:
                    return this.writeEvents_BML(pwout, evdata, privLabel);
                default:
                    Print.logError("Unrecognized data format: " + formatEnum);
                    return false;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------

    public static OutputStream openFileOutputStream(String outFile)
    {
        try {
            if (StringTools.isBlank(outFile) || outFile.equalsIgnoreCase("stdout")) {
                return System.out;
            } else
            if (outFile.equalsIgnoreCase("stderr")) {
                return System.err;
            } else {
                return new FileOutputStream(outFile, false/*no-append*/);
            }
        } catch (IOException ioe) {
            Print.logException("Unable to open output file: " + outFile, ioe);
            return null;
        }
    }
    
    public static void closeOutputStream(OutputStream out)
    {
        if ((out != null) && (out != System.out) && (out != System.err)) {
            try { out.close(); } catch (Throwable t) {/*ignore*/}
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse date range (format: "YYYY/MM/DD,YYYY/MM/DD[,LIMIT]" */
    private static long[] parseArgDateRange(String range, TimeZone tz)
    {
        String rangeFlds[] = StringTools.parseString(range, "|,");
        
        /* Start time */
        long startTime = -1L;            
        if (rangeFlds.length >= 1) {
            if (rangeFlds[0].indexOf("/") >= 0) {
                try {
                    DateTime startDT = DateTime.parseArgumentDate(rangeFlds[0], tz, false);
                    Print.logInfo("Start Date: " + startDT);
                    startTime = (startDT != null)? startDT.getTimeSec() : -1L;
                } catch (DateTime.DateParseException dtpe) {
                    Print.logError("Invalid Start Date: " + rangeFlds[0] + " [" + dtpe.getMessage() + "]");
                    startTime = -1L;
                }
            } else {
                startTime = StringTools.parseLong(rangeFlds[0], -1L);
            }
        }
        
        /* End time */
        long endTime = -1L;
        if (rangeFlds.length >= 2) {
            if (rangeFlds[1].indexOf("/") >= 0) {
                try {
                    DateTime endDT = DateTime.parseArgumentDate(rangeFlds[1], tz, true);
                    Print.logInfo("End Date: " + endDT);
                    endTime = (endDT != null)? endDT.getTimeSec() : -1L;
                } catch (DateTime.DateParseException dtpe) {
                    Print.logError("Invalid End Date: " + rangeFlds[1] + " [" + dtpe.getMessage() + "]");
                    endTime = -1L;
                }
            } else {
                endTime = StringTools.parseLong(rangeFlds[1], -1L);
            }
        }
        
        /* limit */
        long limit = -1L;
        if (rangeFlds.length >= 3) {
            limit = StringTools.parseLong(rangeFlds[2], -1L);
        }
        
        /* return start/end times */
        if ((startTime <= 0L) && (endTime <= 0L)) {
            return null;
        } else {
            return new long[] { startTime, endTime, limit };
        }
        
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[] = new String[] { "acct"   , "account" };
    private static final String ARG_DEVICE[]  = new String[] { "dev"    , "device"  };
    private static final String ARG_EVENTS[]  = new String[] { "events"             };
    private static final String ARG_OUTPUT[]  = new String[] { "out"    , "output"  };
    private static final String ARG_FORMAT[]  = new String[] { "fmt"    , "format"  };
    private static final String ARG_GEOZONE[] = new String[] { "geozone"            };
    private static final String ARG_GEOCODE[] = new String[] { "rg"     , "geocode" };
    private static final String ARG_UPDATE[]  = new String[] { "update" , "upd"     };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + EventUtil.class.getName() + " {options}");
        Print.logInfo("Options:");
        Print.logInfo("  -account=<id>                  Acount ID which owns Device");
        Print.logInfo("  -device=<id>                   Device ID to create/edit");
        Print.logInfo("  -events=<count>                Write last <count> events to output file");
        Print.logInfo("  -events=<from>,<to>[,<limit>]  Write events in specified range to output file");
        Print.logInfo("  -format=[csv|kml]              Event output format");
        Print.logInfo("  -output=<file>                 Event output file");
        Print.logInfo("  -geozone=<from>,<to>           Look for matching geozones for account/device");
        Print.logInfo("  -geocode=<from>,<to> [-update] Apply reverse-geocode to addresses");
        Print.logInfo("  -update                        Update matching geozone/address");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        String acctID  = RTConfig.getString(ARG_ACCOUNT, "");
        String devID   = RTConfig.getString(ARG_DEVICE, "");

        /* account/device specified? */
        if ((acctID == null) || acctID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        } else
        if ((devID == null) || devID.equals("")) {
            Print.logError("Device-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(acctID);
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + acctID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logError("Error loading Account: " + acctID);
            dbe.printException();
            System.exit(99);
        }
        TimeZone timeZone = DateTime.getTimeZone(acct.getTimeZone()); // will be GMT if invalid
        BasicPrivateLabel privLabel = acct.getPrivateLabel();
        boolean allTags = true;

        /* get device(s) */
        Device devList[] = null;
        try {
            if (devID.equals("*") || devID.equals("ALL")) {
                OrderedSet<String> devIdList = Device.getDeviceIDsForAccount(acctID, null, true);
                if (devIdList.size() <= 0) {
                    Print.logError("Account does not contain any Devices: " + acctID);
                    usage();
                }
                devList = new Device[devIdList.size()];
                for (int i = 0; i < devIdList.size(); i++) {
                    Device dev = Device.getDevice(acct, devIdList.get(i));
                    if (dev == null) {
                        Print.logError("Device-ID does not exist: " + acctID + "," + devIdList.get(i));
                        usage();
                    }
                    devList[i] = dev;
                }
            } else {
                Device dev = Device.getDevice(acct, devID);
                if (dev == null) {
                    Print.logError("Device-ID does not exist: " + acctID + "," + devID);
                    usage();
                }
                devList = new Device[] { dev };
            }
        } catch (DBException dbe) {
            Print.logError("Error loading Device: " + acctID + "," + devID);
            dbe.printException();
            System.exit(99);
        }

        /* events */
        // -events=17345678|17364636|40
        // -events=YYYY/MM/DD|YYYY/MM/DD|40
        if (RTConfig.hasProperty(ARG_EVENTS)) {

            /* get requested date range */
            long startTime = -1L;            
            long endTime   = -1L;
            long limit     = DFT_CSV_LIMIT;
            long rangeTime[] = EventUtil.parseArgDateRange(RTConfig.getString(ARG_EVENTS,""), timeZone);
            if (rangeTime != null) {
                startTime = rangeTime[0];            
                endTime   = rangeTime[1];
                limit     = (rangeTime[2] > 0L)? rangeTime[2] : DFT_CSV_LIMIT;
            }

            /* open output file */
            String evFile = RTConfig.getString(ARG_OUTPUT, "");
            OutputStream fos = EventUtil.openFileOutputStream(evFile);
            if (fos == null) {
                System.exit(1);
            }

            /* extract records */
            // this assumes that the number of returned records is reasonable and fits in memory
            EventData evdata[] = null;
            try {
                if ((startTime <= 0L) && (endTime <= 0L)) {
                    evdata = devList[0].getLatestEvents(limit, false);
                } else {
                    evdata = devList[0].getRangeEvents(startTime, endTime, false, EventData.LimitType.FIRST, limit);
                }
                if (evdata == null) { evdata = EventData.EMPTY_ARRAY; }
            } catch (DBException dbe) {
                dbe.printException();
                System.exit(99);
            }

            /* output records */
            int outFmt = EventUtil.parseOutputFormat(RTConfig.getString(ARG_FORMAT,null),FORMAT_CSV);
            EventUtil evUtil = new EventUtil();
            try {
                evUtil.writeEvents(fos, evdata, outFmt, allTags, privLabel);
            } catch (IOException t) {
                Print.logException("Error writing events", t);
                System.exit(1);
            }

            /* close output file */
            EventUtil.closeOutputStream(fos);

            /* done */
            System.exit(0);
            
        }

        /* geozone */
        // -geozone=17345678|17364636
        // -geozone=YYYY/MM/DD|YYYY/MM/DD
        if (RTConfig.hasProperty(ARG_GEOZONE)) {

            /* get requested date range */
            long rangeTime[] = EventUtil.parseArgDateRange(RTConfig.getString(ARG_GEOZONE,""), timeZone);
            if (rangeTime == null) {
                Print.logError("Date range not specified...");
                System.exit(99);
            }
            long startTime = rangeTime[0];            
            long endTime   = rangeTime[1];

            /* traverse records */
            try {
                final Account rhAccount = acct;
                final boolean rhUpdate  = RTConfig.getBoolean(ARG_UPDATE,false);
                final String rhUpdateFields[] = { 
                    EventData.FLD_geozoneID, 
                    EventData.FLD_address 
                };
                EventData.getRangeEvents(
                    devList[0].getAccountID(), devList[0].getDeviceID(),
                    startTime, endTime,
                    null/*statusCodes[]*/,
                    true/*validGPS*/,
                    EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
                    null/*additionalSelect*/,
                    new DBRecordHandler<EventData>() {
                        public int handleDBRecord(EventData rcd) throws DBException {
                            EventData ev = rcd;
                            GeoPoint  gp = ev.getGeoPoint();
                            Geozone   gz = Geozone.getGeozone(rhAccount, null, gp, true);
                            if (gz != null) {
                                if (rhUpdate) {
                                    Print.logInfo("Updating Geozone: [" + gz.getGeozoneID() + "] " + gz.getDescription());
                                    //if (gzone.getClientUpload() && (ev.getGeozoneIndex() == 0L)) {
                                    //    ev.setGeozoneIndex(gzone.getClientID());
                                    //}
                                    ev.setGeozoneID(gz.getGeozoneID());
                                    ev.setAddress(gz.getDescription());
                                    ev.update(rhUpdateFields);
                                } else {
                                    Print.logInfo("Found Geozone: [" + gz.getGeozoneID() + "] " + gz.getDescription());
                                }
                            }
                            return DBRH_SKIP;
                        }
                    }
                );
            } catch (DBException dbe) {
                dbe.printException();
                System.exit(99);
            }

            /* done */
            System.exit(0);

        }

        /* reverse-geocode */
        // -geocode=17345678|17364636
        // -geocode=YYYY/MM/DD|YYYY/MM/DD
        if (RTConfig.hasProperty(ARG_GEOCODE)) {

            /* get requested date range */
            long rangeTime[] = EventUtil.parseArgDateRange(RTConfig.getString(ARG_GEOCODE,""), timeZone);
            if (rangeTime == null) {
                Print.logError("Date range not specified...");
                System.exit(99);
            }
            long startTime = rangeTime[0];            
            long endTime   = rangeTime[1];
            Print.sysPrintln("Reverse-geocoding events in range:");
            Print.sysPrintln("   Start - " + new DateTime(startTime));
            Print.sysPrintln("   End   - " + new DateTime(endTime  ));

            /* get geocoder mode */
            Account.GeocoderMode geocoderMode = Account.getGeocoderMode(acct);
            if (geocoderMode.isNone()) {
                // no geocoding is performed for this account
                Print.logError("GeocoderMode is set to NONE for this account: " + acct.getAccountID());
                System.exit(99);
            }

            /* check for reverse-geocoder */
            if (geocoderMode.okPartial()) {
                ReverseGeocodeProvider rgp = acct.getPrivateLabel().getReverseGeocodeProvider();
                if (rgp == null) {
                    // no ReverseGeocodeProvider, no reverse-geocoding
                    Print.logError("No ReverseGeocodeProvider for this account: " + acct.getAccountID());
                    System.exit(99);
                }
            }

            /* traverse records (for each device) */
            for (int d = 0; d < devList.length; d++) {
                Print.sysPrintln("");
                Print.sysPrintln("--- Reverse-Gecoding Events: " + devList[d].getAccountID() + "," + devList[d].getDeviceID());
                try {
                    final Account rhAccount = acct;
                    final Device  rhDevice  = devList[d];
                    final boolean rhUpdate  = RTConfig.getBoolean(ARG_UPDATE,false);
                    final boolean rhForceUpdate = false;
                    EventData.getRangeEvents(
                        devList[d].getAccountID(), devList[d].getDeviceID(),
                        startTime, endTime,
                        null/*statusCodes[]*/,
                        true/*validGPS*/,
                        EventData.LimitType.FIRST, -1L/*limit*/, true/*ascending*/,
                        null/*additionalSelect*/,
                        new DBRecordHandler<EventData>() {
                            public int handleDBRecord(EventData rcd) throws DBException {
                                EventData ev = rcd;
                                ev.setAccount(rhAccount);
                                ev.setDevice(rhDevice);
                                //Print.logInfo("Checking event: " + ev.getGeoPoint());
                                try {
                                    Set<String> updf = ev.updateAddress(false,rhForceUpdate);
                                    if (!ListTools.isEmpty(updf)) {
                                        if (rhUpdate) {
                                            Print.logInfo("Updating Address: " + ev.getAddress());
                                            ev.update(updf);
                                        } else {
                                            Print.logWarn("Found Address: " + ev.getAddress() + " [NOT UPDATED]");
                                        }
                                    }
                                } catch (SlowOperationException soe) {
                                    // will not occur
                                }
                                return DBRH_SKIP;
                            }
                        }
                    );
                } catch (DBException dbe) {
                    dbe.printException();
                    System.exit(99);
                }
            }

            /* done */
            System.exit(0);

        }

        /* usage */
        usage();

    }

}
