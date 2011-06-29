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
// Descriptiuon:
//  Provides a web inteface/service for retrieving device/group events.
// Examples:
//  http://localhost:8080/events/dev.xml?a=demo&u=&p=&d=demo&l=1&at=true
//  http://localhost:8080/events/dev.xml?a=demo&u=&p=&g=all&l=1&at=true
//  http://localhost:8080/events/dev.xml?a=demo&u=demo&p=&g=all&l=1&at=true
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/04/23  Martin D. Flynn
//     -Integrated logging changes made to Print
//  2007/01/25  Martin D. Flynn
//     -Moved to "OpenGTS"
//     -Added timezone parameter field.
//     -Separated range from/to/limit into distinct fields
//  2007/07/14  Martin D. Flynn
//     -Added User specification
//     -Make sure User is authorized to access device
//  2007/08/09  Martin D. Flynn
//     -Added support for parameter name aliases.
//     -Added some additional EventData fields.
//  2007/09/16  Martin D. Flynn
//     -Added DB and BasicPrivateLabel initialization
//  2008/03/28  Martin D. Flynn
//     -Fixed case where events from multiple devices were being improperly  
//      retrieved (thanks to Artem Farafonov for finding this).
//  2008/04/11  Martin D. Flynn
//     -Use the account/user timezone when calculating the "default" date range.
//  2009/04/02  Martin D. Flynn
//     -Check user "preferred device", if device not specified in URL
//  2009/05/01  Martin D. Flynn
//     -Added URL parameter "validgps" to specify valid GPS fixes only.
//  2009/05/27  Martin D. Flynn
//     -Check to make sure that the URL matches a BasicPrivateLabel domain.
//  2010/09/09  Martin D. Flynn
//     -Added "DeviceID" column to CSV output format
//     -Added "&group=<group>" specification
// ----------------------------------------------------------------------------
package org.opengts.war.events;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;

public class Events 
    extends HttpServlet
{

    // ------------------------------------------------------------------------
    // http://example.com/events/Data.xml?a=account&u=user&p=password&d=device&tz=US/Pacific&rf=YYYY/MM/DD/hh:mm:ss&rt=YYYY/MM/DD/hh:mm:ss&l=200
    // http://example.com/events/Data.csv?a=account&u=user&p=password&d=device
    // http://localhost:8080/events/data.csv?a=demo&d=demo
    
    // abbreviated parameter keys (lookups are case insensitive)
    private static final String PARM_ACCOUNT[]      = new String[] { "account"    ,         "a"  };  // Constants.PARM_ACCOUNT;
    private static final String PARM_USER[]         = new String[] { "user"       ,         "u"  };  // Constants.PARM_USER;
    private static final String PARM_PASSWORD[]     = new String[] { "password"   ,         "p"  };  // Constants.PARM_PASSWORD;
    private static final String PARM_DEVICE[]       = new String[] { "device"     ,         "d"  };  // Constants.PARM_DEVICE;
    private static final String PARM_GROUP[]        = new String[] { "group"      ,         "g"  };  // Constants.PARM_GROUP;
    private static final String PARM_TIMEZONE[]     = new String[] { "timezone"   , "tmz" , "tz" };  // Constants.PARM_TIMEZONE;
    private static final String PARM_RANGE_FR[]     = new String[] { "rangefrom"  , "from", "rf" };  // Constants.PARM_RANGE_FR;    
    private static final String PARM_RANGE_TO[]     = new String[] { "rangeto"    , "to"  , "rt" };  // Constants.PARM_RANGE_TO;    
    private static final String PARM_LIMIT[]        = new String[] { "limit"      ,         "l"  };  // Constants.PARM_LIMIT;    
    private static final String PARM_VALID_GPS[]    = new String[] { "validgps"   , "gps" , "vg" };  // Constants.PARM_VALID_GPS;    
    private static final String PARM_ALL_TAGS[]     = new String[] { "alltags"    ,         "at" };  // Constants.PARM_ALL_TAGS;    

    // ------------------------------------------------------------------------

    /* limits */
    public  static final long   DFT_LIMIT           = 100L;
    public  static final long   MAX_LIMIT           = 1000L;
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* static initializer */
    static {

        /* initialize DBFactories */
        // should already have been called by 'RTConfigContextListener'
        DBConfig.servletInit(null);

    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* GET request */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this._doWork_wrapper(false, request, response);
    }

    /* POST request */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        this._doWork_wrapper(true, request, response);
    }

    private void _doWork_wrapper(boolean isPost, HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {

        /* get PrivateLabel instance for this URL */
        BasicPrivateLabel privLabel = null;
        URL         requestURL      = null;
        String      requestHostName = null;
        try {
            requestURL = new URL(request.getRequestURL().toString());
            requestHostName = requestURL.getHost();
            privLabel = BasicPrivateLabelLoader.getPrivateLabelForURL(requestURL);
        } catch (MalformedURLException mfue) {
            // invalid URL? (unlikely to occur)
            Print.logWarn("Invalid URL? " + request.getRequestURL());
            privLabel = BasicPrivateLabelLoader.getDefaultPrivateLabel();
        }

        /* PrivateLabel not found */
        if (privLabel == null) {
            Print.logError("PrivateLabel not defined or contains errors!");
            this.errorResponse(response, "Request not allowed for specified URL");
            return;
        }

        /* display PrivateLabel */
        RTProperties hostProps = Resource.getPrivateLabelPropertiesForHost(requestHostName);
        try {
            privLabel.pushRTProperties();
            RTConfig.pushTemporaryProperties(hostProps);
            this._doWork(isPost, request, response, privLabel);
        } finally {
            RTConfig.popTemporaryProperties(hostProps);
            privLabel.popRTProperties();
        }

    }

    /* handle POST/GET request */
    private void _doWork(boolean isPost, HttpServletRequest request, HttpServletResponse response, BasicPrivateLabel privLabel)
        throws ServletException, IOException
    {
        String  ipAddr     = request.getRemoteAddr();
        String  accountID  = Events.getRequestString (request, PARM_ACCOUNT  , "");
        String  userID     = Events.getRequestString (request, PARM_USER     , "");
        String  password   = Events.getRequestString (request, PARM_PASSWORD , "");
        String  deviceArgs = Events.getRequestString (request, PARM_DEVICE   , "");
        String  groupArgs  = Events.getRequestString (request, PARM_GROUP    , "");
        String  tzStr      = Events.getRequestString (request, PARM_TIMEZONE , "GMT");
        String  rangeFr    = Events.getRequestString (request, PARM_RANGE_FR , "");
        String  rangeTo    = Events.getRequestString (request, PARM_RANGE_TO , "");
        long    limit      = Events.getRequestLong   (request, PARM_LIMIT    , DFT_LIMIT);
        boolean validGPS   = Events.getRequestBoolean(request, PARM_VALID_GPS, true);
        boolean allTags    = Events.getRequestBoolean(request, PARM_ALL_TAGS , false);
        String  fmtStr     = null;

        /* default to 'admin' user */
        if (StringTools.isBlank(userID)) {
            userID = User.getAdminUserID();
        }

        /* output format */
        if (StringTools.isBlank(fmtStr)) {
            String uri = request.getRequestURI();
            fmtStr = FileTools.getExtension(uri);
        }
        int outFmt = EventUtil.parseOutputFormat(fmtStr, EventUtil.FORMAT_CSV);

        /* TimeZone */
        TimeZone tz  = DateTime.getTimeZone(tzStr); // will be GMT if invalid
        DateTime now = new DateTime(tz);

        /* range 'from' */
        // YYYY/MM[/DD[/hh[:mm[:ss]]]]  ie YYYY/MM/DD/hh:mm:ss  
        DateTime dateFr = null;
        String rangeFrFld[] = !rangeFr.equals("")? StringTools.parseString(rangeFr, "/:") : null;
        if (ListTools.isEmpty(rangeFrFld)) {
            // time not specified
            dateFr = null;
        } else
        if (rangeFrFld.length == 1) {
            // parse as 'Epoch' time
            long epoch = StringTools.parseLong(rangeFrFld[0], now.getTimeSec());
            dateFr = new DateTime(epoch, tz);
        } else {
            // (rangeFrFld.length >= 2)
            int YY = StringTools.parseInt(rangeFrFld[0], now.getYear());
            int MM = StringTools.parseInt(rangeFrFld[1], now.getMonth1());
            int DD;
            int hh = 0, mm = 0, ss = 0;    // default to beginning of day
            if (rangeFrFld.length >= 3) {
                // at least YYYY/MM/DD provided
                DD = StringTools.parseInt(rangeFrFld[2], now.getDayOfMonth());
                if (rangeFrFld.length >= 4) { hh = StringTools.parseInt(rangeFrFld[3], 0); }
                if (rangeFrFld.length >= 5) { mm = StringTools.parseInt(rangeFrFld[4], 0); }
                if (rangeFrFld.length >= 6) { ss = StringTools.parseInt(rangeFrFld[5], 0); }
            } else {
                // only YYYY/MM provided
                DD = 1; // first day of month
            }
            dateFr = new DateTime(tz, YY, MM, DD, hh, mm, ss);
        }
        long startTime = (dateFr != null)? dateFr.getTimeSec() : -1L;

        /* range 'to' */
        // YYYY/MM/DD/hh:mm:ss
        DateTime dateTo = null;
        String rangeToFld[] = !rangeTo.equals("")? StringTools.parseString(rangeTo, "/:") : null;
        if (ListTools.isEmpty(rangeToFld)) {
            // time not specified
            dateTo = null;
        } else
        if (rangeToFld.length == 1) {
            // parse as 'Epoch' time
            long epoch = StringTools.parseLong(rangeToFld[0], now.getTimeSec());
            dateTo = new DateTime(epoch, tz);
        } else {
            // (rangeToFld.length >= 2)
            int YY = StringTools.parseInt(rangeToFld[0], now.getYear());
            int MM = StringTools.parseInt(rangeToFld[1], now.getMonth1());
            int DD;
            int hh = 23, mm = 59, ss = 59;  // default to end of day
            if (rangeToFld.length >= 3) {
                // at least YYYY/MM/DD provided
                DD = StringTools.parseInt(rangeToFld[2], now.getDayOfMonth());
                if (rangeToFld.length >= 4) { hh = StringTools.parseInt(rangeToFld[3], 23); }
                if (rangeToFld.length >= 5) { mm = StringTools.parseInt(rangeToFld[4], 59); }
                if (rangeToFld.length >= 6) { ss = StringTools.parseInt(rangeToFld[5], 59); }
            } else {
                // only YYYY/MM provided
                DD = DateTime.getDaysInMonth(tz, MM, YY); // last day of month
            }
            dateTo = new DateTime(tz, YY, MM, DD, hh, mm, ss);
        }
        long endTime = (dateTo != null)? dateTo.getTimeSec() : -1L;

        /* limit */
        if (limit <= 0L) {
            limit = DFT_LIMIT;
        } else
        if (limit > MAX_LIMIT) {
            //limit = MAX_LIMIT;
        }

        /* get account */
        Account account = null;
        try {
            account = Account.getAccount(accountID);
            if (account == null) {
                Print.logError("Account not found: " + accountID);
                this.errorResponse(response, "Invalid account");
                return;
            }
        } catch (DBException dbe) {
            Print.logException("Error reading account", dbe);
            this.errorResponse(response, "Internal error (account)");
            return;
        }

        /* default to 'admin' user */
        if (StringTools.isBlank(userID)) {
            String dftUser = account.getDefaultUser();
            userID = !StringTools.isBlank(dftUser)? dftUser : User.getAdminUserID();
        }

        /* get user */
        User user = null;
        if (!StringTools.isBlank(userID)) {
            try {
                if (userID.indexOf("@") > 0) {
                    user = User.getUserForContactEmail(account.getAccountID(), userID);
                } else {
                    user = User.getUser(account, userID);
                }
            } catch (DBException dbe) {
                Print.logException("Error reading User", dbe);
                this.errorResponse(response, "Internal error (user)");
                return;
            }
            if ((user == null) && !User.isAdminUser(userID)) {
                Print.logError("User not found: " + userID);
                this.errorResponse(response, "Invalid user");
                return;
            }
        }

        /* validate password */
        if (user != null) {
            // check user password
            if (!user.checkPassword(password) && !account.checkPassword(password)) {
                Print.logError("Account/User password was invalid: " + accountID + "/" + userID);
                this.errorResponse(response, "Invalid account/user");
                return;
            }
        } else {
            // check account password
            if (!account.checkPassword(password)) {
                Print.logError("Account password was invalid: " + accountID);
                this.errorResponse(response, "Invalid account");
                return;
            }
        }

        /* device ID accumulator */
        OrderedSet<String> deviceIDSet = new OrderedSet<String>();

        /* device list */
        if (!StringTools.isBlank(deviceArgs)) {
            String devIDArray[] = StringTools.parseString(deviceArgs,",");
            for (String deviceID : devIDArray) {
                try {
                    // verify device existance
                    if (Device.exists(accountID,deviceID)) {
                        deviceIDSet.add(deviceID);
                    } else {
                        this.errorResponse(response, "Invalid device: " + deviceID);
                        return;
                    }
                } catch (DBException dbe) {
                    Print.logError("Unable to determine device existence: " + accountID + " => " + deviceID);
                    this.errorResponse(response, "Internal error (device existence)");
                    return;
                }
            }
        }

        /* group list */
        if (!StringTools.isBlank(groupArgs)) {
            String groupIDArray[] = StringTools.parseString(groupArgs,",");
            for (String groupID : groupIDArray) {
                try {
                    OrderedSet<String> devIDList = DeviceGroup.getDeviceIDsForGroup(accountID, groupID, user, false, -1);
                    if (!ListTools.isEmpty(devIDList)) {
                        deviceIDSet.addAll(devIDList);
                    }
                } catch (DBException dbe) {
                    Print.logError("Unable to retrieve device group: " + accountID + "/" + userID + " => " + groupID);
                    this.errorResponse(response, "Internal error (device group)");
                    return;
                }
            }
        }

        /* no devices specified? */
        if (ListTools.isEmpty(deviceIDSet)) {
            if ((user != null) && user.hasPreferredDeviceID()) {
                deviceIDSet.add(user.getPreferredDeviceID());
            } else {
                Print.logError("No devices specified: " + accountID);
                this.errorResponse(response, "No devices specified");
                return;
            }
        }

        /* user authorized to access device(s)? */
        if (user != null) {
            for (String deviceID : deviceIDSet) {
                try {
                    if (!user.isAuthorizedDevice(deviceID)) {
                        Print.logError("Account/User not authorized for device: " + accountID + "/" + userID + " => " + deviceID);
                        this.errorResponse(response, "Device(s) not authorized");
                        return;
                    }
                } catch (DBException dbe) {
                    Print.logError("Unable to determine if Account/User is authorized for device: " + accountID + "/" + userID + " => " + deviceID);
                    this.errorResponse(response, "Internal error (device auth)");
                    return;
                }
            }
        }

        /* extract records */
        // NOT CURRENTLY SCALABLE!
        // this version assumes that the number of returned records is reasonable and fits in memory
        EventData eventArray[] = null;
        try {
            // Note: 'dateFr' and/or 'dateTo' may be null
            //Print.logDebug("Event Date Range: " + dateFr + " ==> " + dateTo + " [limit=" + limit + "]");
            java.util.List<EventData> edList = new Vector<EventData>();
            for (String devID : deviceIDSet) {
                Device dev = Device.getDevice(account, devID);
                if (dev != null) {
                    EventData ed[] = this.getDeviceRangeEvents(dev, startTime, endTime, limit, validGPS);
                    ListTools.toList(ed, edList);
                } else {
                    Print.logWarn("Device not found: " + devID);
                }
            }
            int edLen = edList.size();
            eventArray = (edLen > 0)? edList.toArray(new EventData[edLen]) : EventData.EMPTY_ARRAY;
        } catch (DBException dbe) {
            dbe.printException();
            this.errorResponse(response, "Internal error (events)");
            return;
        }

        /* mime content type */
        switch (outFmt) {
            case EventUtil.FORMAT_TXT:
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                break;
            case EventUtil.FORMAT_CSV:
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_CSV());
                break;
            case EventUtil.FORMAT_KML:
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_KML());
                break;
            case EventUtil.FORMAT_XML:
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML());
                break;
            case EventUtil.FORMAT_GPX:
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML());
                break;
            default:
                CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
                break;
        }

        /* return events */
        try {
            PrintWriter out = response.getWriter();
            EventUtil.getInstance().writeEvents(out, eventArray, outFmt, allTags, privLabel);
        } catch (IOException ioe) {
            Print.logException("Error writing events", ioe);
            this.errorResponse(response, "Internal error (output)");
            return;
        }

    }

    // ------------------------------------------------------------------------

    private EventData[] getDeviceRangeEvents(Device device, long startTime, long endTime, long limit, boolean validGPS)
        throws DBException
    {
        EventData evdata[] = null;
        if (device != null) {
            if ((startTime <= 0L) && (endTime <= 0L)) {
                // date range not specified
                evdata = device.getLatestEvents(limit, validGPS);
            } else
            if (startTime <= 0L) {
                // start date range not specified
                evdata = device.getRangeEvents(startTime, endTime, validGPS, EventData.LimitType.LAST, limit);
            } else {
                // end date range MAY not have been specified
                evdata = device.getRangeEvents(startTime, endTime, validGPS, EventData.LimitType.FIRST, limit);
            }
        }
        return evdata;
    }

    // ------------------------------------------------------------------------

    protected void errorResponse(HttpServletResponse response, String msg)
        throws ServletException, IOException
    {
        CommonServlet.setResponseContentType(response, HTMLTools.CONTENT_TYPE_PLAIN);
        PrintWriter out = response.getWriter();
        out.println(msg);
    }

    // ------------------------------------------------------------------------
    // Search for the specified key in the following location(s):
    //  1) The URL Query string

    public static String getRequestString(ServletRequest req, String key[], String dft)
    {
        boolean ignoreCase = true;
        if ((req == null) || (key == null) || (key.length == 0)) {
            return dft;
        } else
        if (ignoreCase) {
            for (Enumeration e = req.getParameterNames(); e.hasMoreElements();) {
                String n = (String)e.nextElement();
                for (int i = 0; i < key.length; i++) {
                    if (n.equalsIgnoreCase(key[i])) {
                        String val = req.getParameter(n);
                        return (val != null)? val : dft;
                    }
                }
            }
            return dft;
        } else {
            for (int i = 0; i < key.length; i++) {
                String val = req.getParameter(key[i]);
                if (val != null) {
                    return val;
                }
            }
            return dft;
        }
    }
    
    public static long getRequestLong(ServletRequest req, String key[], long dft)
    {
        return StringTools.parseLong(getRequestString(req,key,null), dft);
    }
    
    public static boolean getRequestBoolean(ServletRequest req, String key[], boolean dft)
    {
        return StringTools.parseBoolean(getRequestString(req,key,null), dft);
    }

}
