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
//  2007/03/11  Martin D. Flynn
//     -Initial release
//  2007/03/25  Martin D. Flynn
//     -Added CSV output format
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2008/02/11  Martin D. Flynn
//     -Added support for displaying a map of locations on a report.
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;
import org.opengts.war.track.Calendar;
import org.opengts.war.track.*;

public class ReportDisplay
    extends WebPageAdaptor
    implements Constants
{
    
    // ------------------------------------------------------------------------
    
    /* DEBUG: save emailed report output to "/tmp/" (should be false for production) */
    private static final boolean SAVE_EHTML_TO_TMP          = false;
    
    // ------------------------------------------------------------------------

    public  static final String CSS_REPORT_DISPLAY[]        = new String[] { "reportDisplayTable", "reportDisplayCell" };

    // ------------------------------------------------------------------------
    
    public  static final String COMMAND_REPORT_SELECT       = ReportMenu.COMMAND_REPORT_SELECT;

    public  static final String PARM_DEVICE_ID              = ReportMenu.PARM_DEVICE_ID;
    public  static final String PARM_GROUP_ID               = ReportMenu.PARM_GROUP_ID;
    public  static final String PARM_REPORT                 = ReportMenu.PARM_REPORT;
    public  static final String PARM_REPORT_OPT             = ReportMenu.PARM_REPORT_OPT;
    public  static final String PARM_REPORT_TEXT            = ReportMenu.PARM_REPORT_TEXT;
    public  static final String PARM_LIMIT                  = ReportMenu.PARM_LIMIT;
    public  static final String PARM_LIMIT_TYPE             = ReportMenu.PARM_LIMIT_TYPE; // not used
    public  static final String PARM_REPORT_SUBMIT          = ReportMenu.PARM_REPORT_SUBMIT;
    public  static final String PARM_FORMAT                 = ReportMenu.PARM_FORMAT;
    public  static final String PARM_MENU                   = ReportMenu.PARM_MENU;
    public  static final String PARM_EMAIL_ADDR             = ReportMenu.PARM_EMAIL_ADDR;

    // ------------------------------------------------------------------------

    public ReportDisplay()
    {
        this.setBaseURI(Track.BASE_URI());
        this.setPageName(PAGE_REPORT_SHOW);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP, PAGE_MENU_REPORT });
        this.setLoginRequired(true);
    }

    // ------------------------------------------------------------------------

    public String getPageNavigationHTML(RequestProperties reqState)
    {
        HttpServletRequest request = reqState.getHttpServletRequest();
        String rptMenu = AttributeTools.getRequestString(request, PARM_MENU, PAGE_MENU_REPORT);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP, rptMenu });
        return super.getPageNavigationHTML(reqState,true);
    }
    
    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getMenuDescription(reqState,"");
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getMenuHelp(reqState,"");
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getNavigationDescription(reqState,"");
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(ReportDisplay.class);
        return super._getNavigationTab(reqState,"");
    }

    // ------------------------------------------------------------------------

    private String _formatMapEvent(EventDataProvider edp, RequestProperties reqState, ReportData report)
    {
        MapProvider mapProvider = reqState.getMapProvider(); // not null
        PrivateLabel privLabel = reqState.getPrivateLabel();

        /* date/time format */
        Account acct = reqState.getCurrentAccount();
        String dateFmt = (acct != null)? acct.getDateFormat() : BasicPrivateLabel.getDefaultDateFormat();
        String timeFmt = (acct != null)? acct.getTimeFormat() : BasicPrivateLabel.getDefaultTimeFormat();

        /* format */
        boolean isFleet = reqState.isFleet();
        TimeZone tmz    = reqState.getTimeZone();
        String iconSel  = report.getMapIconSelector(); // may be null
        OrderedSet<String> iconKeys = (OrderedSet<String>)mapProvider.getPushpinIconMap(reqState).keySet();
        return EventUtil.getInstance().formatMapEvent(privLabel, edp, iconSel, iconKeys, isFleet, tmz, dateFmt, timeFmt);

    }

    private void _writeReportMap(HttpServletResponse response, final RequestProperties reqState, final ReportData report, final I18N i18n)
        throws ReportException, IOException
    {

        /* map provider */
        MapProvider mapProvider = reqState.getMapProvider();
        if (mapProvider == null) {
            throw new ReportException(i18n.getString("ReportDisplay.noMapProvider","No Map Provider defined for this URL"));
        }

        /* write frame */
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
        PrintWriter out = response.getWriter();
        PrivateLabel privLabel = reqState.getPrivateLabel();
        
        /* map dataset type */
        final boolean isFleet = reqState.isFleet();
        final String type = isFleet? EventUtil.DSTYPE_group : EventUtil.DSTYPE_device; // "poi"

        // HTML start
        out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
        out.write("<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>\n");

        // HTML head
        out.write("\n");
        out.write("<head>\n");
        out.write("  <meta http-equiv='content-type' content='text/html; charset=UTF-8'/>\n");
        out.write("  <meta http-equiv='cache-control' content='no-cache'/>\n");
        out.write("  <meta http-equiv='expires' content='0'/>\n"); // expires 'now'
        out.write("  <meta name='copyright' content='"+privLabel.getCopyright()+"'/>\n");
        out.write("  <meta name='robots' content='none'/>\n");
        out.write("  <title>" + privLabel.getPageTitle() + "</title>\n");

        // JavaScript tools
        JavaScriptTools.writeUtilsJS(out);
        mapProvider.writeJavaScript(out, reqState);

        // JavaScript map points
        JavaScriptTools.writeStartJavaScript(out);
        out.write("function trackMapOnLoad() {\n");
        out.write("   var mapPts =\n");
        TimeZone tz = reqState.getTimeZone();
        out.write("\"<"+EventUtil.TAG_MapData+">\\n\" +\n");
        out.write("\"<"+EventUtil.TAG_DataSet+" type=\\\""+type+"\\\" route=\\\""+!isFleet+"\\\">\\n\" +\n");
        for (DBDataIterator dbi = report.getBodyDataIterator(); dbi.hasNext();) {
            Object ev = dbi.next().getRowObject();
            if (ev instanceof EventDataProvider) {
                EventDataProvider edp = (EventDataProvider)ev;
                if (!dbi.hasNext()) { edp.setIsLastEvent(true); }
                String rcd = StringTools.replace(this._formatMapEvent(edp,reqState,report),"\"","\\\"");
                out.write("\"<"+EventUtil.TAG_Point+"><![CDATA[" + rcd + "]]></"+EventUtil.TAG_Point+">\\n\" +\n");
                //out.write("\"" + rcd + "\\n\" +\n");
            } else {
                Print.logWarn("Not an EventDataProvider: " + StringTools.className(ev));
            }
        }
        out.write("\"</"+EventUtil.TAG_DataSet+">\\n\" +\n");
        out.write("\"</"+EventUtil.TAG_MapData+">\\n\" +\n");
        out.write("\"\";\n");
        out.write("   mapProviderParseXML(mapPts);\n");
        out.write("}\n");
        out.write("function trackMapOnUnload() {\n");
        out.write("   mapProviderUnload();\n");
        out.write("}\n");
        JavaScriptTools.writeEndJavaScript(out);

        out.write("</head>\n");
        out.write("\n");
        
        // HTML Body
        out.write("<body onload=\"javascript:trackMapOnLoad();\" onunload=\"javascript:trackMapOnUnload();\">\n"); 
        //out.write(" leftmargin='0' rightmargin='0' topmargin='0' bottommargin='0'>\n");
        out.write("<div>\n"); //  style='align:center; width:99%; height:99%;'>\n");
        mapProvider.writeMapCell(out, reqState, new MapDimension());
        out.write("</div>\n");
        out.write("</body>\n");
        
        // HTML end
        out.write("</html>\n");
        out.close();

    }

    // ------------------------------------------------------------------------

    private void _writeReportKML(HttpServletResponse response, RequestProperties reqState, ReportData report)
        throws ReportException, IOException
    {
        PrintWriter out = response.getWriter();
        PrivateLabel privLabel = reqState.getPrivateLabel();

        /* events */
        java.util.List<EventData> edList = new Vector<EventData>();
        for (DBDataIterator dbi = report.getBodyDataIterator(); dbi.hasNext();) {
            Object ev = dbi.next().getRowObject();
            if (ev instanceof EventData) {
                EventData ed = (EventData)ev;
                if (!dbi.hasNext()) { ed.setIsLastEvent(true); }
                edList.add(ed);
            } else {
                Print.logWarn("Not an EventDataProvider: " + StringTools.className(ev));
            }
        }
        
        /* KML output */
        GoogleKML.getInstance().writeEvents(out, edList.toArray(new EventData[edList.size()]), privLabel);

    }

    // ------------------------------------------------------------------------

    private void _writeReportGraph(HttpServletResponse response, final RequestProperties reqState, final ReportData report, final I18N i18n)
        throws ReportException, IOException
    {
        Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.graphError","Error generating map: {0}",report.getReportName()));

        /* write frame */
        CommonServlet.setResponseContentType(response, HTMLTools.MIME_HTML());
        PrintWriter out = response.getWriter();
        PrivateLabel privLabel = reqState.getPrivateLabel();
        
        // HTML start
        out.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
        out.write("<html xmlns='http://www.w3.org/1999/xhtml' xmlns:v='urn:schemas-microsoft-com:vml'>\n");

        // HTML head
        out.write("\n");
        out.write("<head>\n");
        out.write("  <meta http-equiv='content-type' content='text/html; charset=UTF-8'/>\n");
        out.write("  <meta http-equiv='cache-control' content='no-cache'/>\n");
        out.write("  <meta http-equiv='expires' content='0'/>\n"); // expires 'now'
        out.write("  <meta name='copyright' content='"+privLabel.getCopyright()+"'/>\n");
        out.write("  <meta name='robots' content='none'/>\n");
        out.write("  <title>" + privLabel.getPageTitle() + "</title>\n");
        out.write("</head>\n");
        out.write("\n");
        
        // HTML body
        out.write("<body>\n"); 
        String graphURL = EncodeURL(reqState, report.getGraphURL());
        if (!StringTools.isBlank(graphURL)) {
            out.write("<img src='"+graphURL+"'/>\n");
        }
        out.write("</body>\n");
        
        // HTML end
        out.write("</html>\n");
        out.close();

    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel  privLabel = reqState.getPrivateLabel();
        final I18N          i18n      = privLabel.getI18N(ReportDisplay.class);
        final Locale        locale    = reqState.getLocale();
        HttpServletRequest  request   = reqState.getHttpServletRequest();
        HttpServletResponse response  = reqState.getHttpServletResponse();
        String m = pageMsg;
        boolean error = false;

        /* report constraints */
        // account=<account> user=<user>
        // r_report=<report> 
        // device=<device> | group=<group>
        // date_fr=<ts> date_to=<ts> date_tz=<tz>
        // r_limit=<limit> r_limType=last|first
        // format=html|csv|xml
        String reportID   = (String)AttributeTools.getRequestAttribute(request, PARM_REPORT     , "?");
        String rptOption  = (String)AttributeTools.getRequestAttribute(request, PARM_REPORT_OPT , "");
        String rptText    = (String)AttributeTools.getRequestAttribute(request, PARM_REPORT_TEXT, ""); // not used
        String deviceID   = (String)AttributeTools.getRequestAttribute(request, PARM_DEVICE_ID  , "");
        String groupID    = (String)AttributeTools.getRequestAttribute(request, PARM_GROUP_ID   , ""); 
        String rangeFr    = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_RANGE_FR, "");
        String rangeTo    = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_RANGE_TO, "");
        String tzStr      = (String)AttributeTools.getRequestAttribute(request, Calendar.PARM_TIMEZONE, "");
        String limitStr   = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT      , "");
        String limTypStr  = (String)AttributeTools.getRequestAttribute(request, PARM_LIMIT_TYPE , ""); // not used
        String emailAddr  = (String)AttributeTools.getRequestAttribute(request, PARM_EMAIL_ADDR , ""); // not used

        /* report format */
        String rptFormat  = (String)AttributeTools.getRequestAttribute(request, PARM_FORMAT, ReportPresentation.FORMAT_HTML);

        /* report menu */
        String rptMenu    = AttributeTools.getRequestString(request, PARM_MENU, PAGE_MENU_REPORT);
      //String rptMenuURL = EncodeMakeURL(reqState, Track.BASE_URI(), rptMenu);
        String rptMenuURL = privLabel.getWebPageURL(reqState, rptMenu);
        //Print.logDebug("ReportMenu: %s => %s", rptMenu, rptMenuURL);

        /* get report */
        //String cmdName = reqState.getCommandName(); // should be COMMAND_REPORT_SELECT (but ignored)
        if (StringTools.isBlank(reportID)) {
            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.noReport","No report requested"));
            return;
        }
        ReportEntry reportEntry = privLabel.getReportEntry(reportID);
        if (reportEntry == null) {
            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.notFound","Report not found: {0}",reportID));
            return;
        }
        ReportFactory reportFactory = reportEntry.getReportFactory();

        /* set "fleet" request type */
        boolean isGroup = reportFactory.getReportTypeIsGroup();
        reqState.setFleet(isGroup);
        reqState.setReport(true);

        /* create report */
        final Account    account = reqState.getCurrentAccount();
        final User       user    = reqState.getCurrentUser(); // may be null
        final ReportData report;
        try {
            if (isGroup) {
                // Group/Summary report
                //Print.logInfo("Group/Summary report ...");
                if (!StringTools.isBlank(groupID)) {
                    if (groupID.equals(DeviceGroup.DEVICE_GROUP_ALL)) {
                        ReportDeviceList rdl = new ReportDeviceList(account, user);
                        rdl.addAllAuthorizedDevices();
                        report = reportFactory.createReport(reportEntry, rptOption, reqState, rdl);
                    } else {
                        DeviceGroup group = DeviceGroup.getDeviceGroup(account, groupID);
                        if (group == null) {
                            Print.logError("Group does not exist: "  + groupID);
                            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.groupNotFound","Report: {0}\\nGroup ''{1}'' not found.", reportID, groupID));
                            return;
                        }
                        report = reportFactory.createReport(reportEntry, rptOption, reqState, group);
                    }
                } else {
                    Print.logError("Group not specified: "  + reportID);
                    Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.noGroupSelection","Report: {0}\\nNo Group selected.", reportID));
                    return;
                }
            } else {
                // Device/Detail report
                //Print.logInfo("Device/Detail report ...");
                if (!StringTools.isBlank(deviceID)) {
                    Device device = Device.getDevice(account, deviceID);
                    if (device == null) {
                        Print.logError("Device does not exist: "  + deviceID);
                        Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.deviceNotFound","Report: {0}\\nDevice ''{1}'' not found.", reportID, deviceID));
                        return;
                    }
                    report = reportFactory.createReport(reportEntry, rptOption, reqState, device);
                } else {
                    Print.logError("Device not specified: "  + reportID);
                    Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.noDeviceSelection","Report: {0}\\nNo Device selected.", reportID));
                    return;
                }
            }
        } catch (Throwable t) {
            Print.logException("Error generating report: "  + reportID, t);
            Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.reportError","Report: {0}\\nError generating report", reportID));
            return;
        }
        final ReportLayout reportLayout = report.getReportLayout();
        ReportConstraints rc = report.getReportConstraints();

        /* set refresh/map URL (FORMAT_HTML only) */
        if (rptFormat.equalsIgnoreCase(ReportPresentation.FORMAT_HTML)) {
            // - refresh URL
            URIArg refreshURL = reqState.getHttpServletRequestURIArg(false);
            if (refreshURL != null) {
                report.setRefreshURL(refreshURL.removeBlankValues());
            }
            // - automatic report URL
            URIArg autoReportURL = reqState.getHttpServletRequestURIArg(false);
            if (autoReportURL != null) {
                // timezone
                TimeZone tz      = DateTime.getTimeZone(autoReportURL.getArgValue(Calendar.PARM_TIMEZONE)); // will be GMT if invalid
                // now
                DateTime dtm_NW  = new DateTime(tz);
                long     day_NW  = DateTime.getDayNumberFromDate(dtm_NW);
                // convert "from" date (ie. "-1d,00:00")
                String   arg_FR  = URIArg.decodeArg(null,autoReportURL.getArgValue(Calendar.PARM_RANGE_FR)).toString();
                DateTime dtm_FR  = Calendar.parseDate(arg_FR, tz, false);
                long     day_FR  = DateTime.getDayNumberFromDate(dtm_FR);
                long     delt_FR = day_FR - day_NW;
                String   new_FR  = ((delt_FR >= 0)? ("+"+delt_FR) : delt_FR) + "d," + dtm_FR.getHour24() + ":" + dtm_FR.getMinute();
                autoReportURL.setArgValue(Calendar.PARM_RANGE_FR, new_FR);
                // convert "to" date (ie. +0d,23:59")
                String   arg_TO  = URIArg.decodeArg(null,autoReportURL.getArgValue(Calendar.PARM_RANGE_TO)).toString();
                DateTime dtm_TO  = Calendar.parseDate(arg_TO, tz, true );
                long     day_TO  = DateTime.getDayNumberFromDate(dtm_TO);
                long     delt_TO = day_TO - day_NW;
                String   new_TO  = ((delt_TO >= 0)? ("+"+delt_TO) : delt_TO) + "d," + dtm_TO.getHour24() + ":" + dtm_TO.getMinute();
                autoReportURL.setArgValue(Calendar.PARM_RANGE_TO, new_TO);
                // save
                //Print.logInfo("Auto-Report URL: " + autoReportURL);
                report.setAutoReportURL(autoReportURL);
            }
            // - graph URL
            URIArg graphURL = reqState.getHttpServletRequestURIArg(false);
            if (graphURL != null) {
                graphURL.setArgValue(PARM_FORMAT   , ReportData.FORMAT_GRAPH);
                //graphURL.setArgValue(PARM_DEVICE_ID, deviceID);
                report.setGraphURL(graphURL);
            }
            // - map URL
            URIArg mapURL = reqState.getHttpServletRequestURIArg(false);
            if (mapURL != null) {
                mapURL.setArgValue(PARM_FORMAT, ReportData.FORMAT_MAP);
                //graphURL.setArgValue(PARM_DEVICE_ID, deviceID);
                report.setMapURL(mapURL);
            }
            // - kml URL
            URIArg kmlURL = reqState.getHttpServletRequestURIArg(false);
            if (kmlURL != null) {
                kmlURL.addExtension(".kml");
                kmlURL.setArgValue(PARM_FORMAT, ReportData.FORMAT_KML);
                //graphURL.setArgValue(PARM_DEVICE_ID, deviceID);
                report.setKmlURL(kmlURL);
            }
        }

        /* TimeZone */
        if (StringTools.isBlank(tzStr)) {
            if (user != null) {
                // try User timezone
                tzStr = user.getTimeZone(); // may be blank
                if (StringTools.isBlank(tzStr) || tzStr.equals(User.DEFAULT_TIMEZONE)) {
                    // override with Account timezone
                    tzStr = account.getTimeZone();
                }
            } else {
                // get Account timezone
                tzStr = account.getTimeZone();
            }
            if (StringTools.isBlank(tzStr)) {
                // make sure we have a timezone 
                // (unecessary, since Account/User will return a timezone)
                tzStr = Account.DEFAULT_TIMEZONE;
            }
        }
        TimeZone tz = DateTime.getTimeZone(tzStr); // will be GMT if invalid
        AttributeTools.setSessionAttribute(request, Calendar.PARM_TIMEZONE, tzStr);
        reqState.setTimeZone(tz, tzStr);

        /* Event date range */
        DateTime dateFr = Calendar.parseDate(rangeFr,tz,false);
        DateTime dateTo = Calendar.parseDate(rangeTo,tz,true );
        if (dateFr == null) { dateFr = Calendar.getCurrentDayStart(tz); }
        if (dateTo == null) { dateTo = Calendar.getCurrentDayEnd(tz); }
        reqState.setEventDateFrom(dateFr);
        reqState.setEventDateTo(  dateTo);
        long timeStart  = (dateFr != null)? dateFr.getTimeSec() : -1L;
        long timeEnd    = (dateTo != null)? dateTo.getTimeSec() : -1L;
        rc.setTimeRange(timeStart, timeEnd);
        AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_FR , Calendar.formatArgDateTime(dateFr));
        AttributeTools.setSessionAttribute(request, Calendar.PARM_RANGE_TO , Calendar.formatArgDateTime(dateTo));

        /* limit */
        //if (!StringTools.isBlank(limitStr)) {
        //    long limit = StringTools.parseLong(limitStr, -1L);
        //    if (limit > 0L) {
        //        if (limit > MAX_LIMIT) { limit = MAX_LIMIT; }
        //        rc.setSelectionLimit(EventData.LimitType.LAST, limit);
        //    }
        //} else {
        //    if (!rc.hasSelectionLimit()) {
        //        // set a limit if no limit has been set
        //        rc.setSelectionLimit(EventData.LimitType.LAST, MAX_LIMIT);
        //    }
        //}

        /* valid gps? */
        //rc.setValidGPSRequired(false);
        
        /* store vars as session attributes */
        AttributeTools.setSessionAttribute(request, PARM_REPORT    , reportID);
        AttributeTools.setSessionAttribute(request, PARM_DEVICE_ID , deviceID);
        AttributeTools.setSessionAttribute(request, PARM_GROUP_ID  , groupID);
        AttributeTools.setSessionAttribute(request, PARM_LIMIT     , limitStr);
        AttributeTools.setSessionAttribute(request, PARM_LIMIT_TYPE, limTypStr); // not used

        /* report post initialization */
        // After all external configuration and constraints have been set
        report.postInitialize();

        /* XML output? */
        // output as XML to browser
        if (rptFormat.equalsIgnoreCase(ReportPresentation.FORMAT_XML)) {
            try {
                //CommonServlet.setResponseContentType(response, HTMLTools.MIME_XML());
                // (See "org.opengts.war.report.ReportTable:writeXML")
                int count = report.writeReport(ReportPresentation.FORMAT_XML, response.getWriter());
                if (count > 0) {
                    // all is ok (XML report written)
                    return;
                } else {
                    Print.logWarn("XML Date/Time range contains no data: "  + reportID);
                    m = i18n.getString("ReportDisplay.xmlNoData","The selected Date/Time range contains no data.\\nReport: {0}",reportFactory.getReportTitle(locale));
                    //Track.writeErrorResponse(reqState, m);
                    error = true;
                    // TODO: what we really want to do here is redisplay the ReportMenu with the appropriate alert dialog
                    WebPage rptPage = privLabel.getWebPage(rptMenu);
                    if (rptPage != null) {
                        rptPage.writePage(reqState, m);
                    }
                    return;
                }
            } catch (ReportException re) {
                Print.logException("Error generating XML: " + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.xmlError","Error generating XML: {0}",reportID));
            }
            return;
        }

        /* CSV output? */
        // output as CSV/TXT to browser
        if (rptFormat.equalsIgnoreCase(ReportPresentation.FORMAT_CSV) ||
            rptFormat.equalsIgnoreCase(ReportPresentation.FORMAT_TXT)) {
            try {
                //CommonServlet.setResponseContentType(response, HTMLTools.MIME_CSV());
                // (See "org.opengts.war.report.ReportTable:writeCSV")
                int count = report.writeReport(rptFormat.toUpperCase(), response.getWriter());
                if (count > 0) {
                    // all is ok
                    return;
                } else {
                    Print.logWarn("CSV Date/Time range contains no data: "  + reportID);
                    m = i18n.getString("ReportDisplay.csvNoData","The selected Date/Time range contains no data.\\nReport: {0}",reportFactory.getReportTitle(locale));
                    //Track.writeErrorResponse(reqState, m);
                    error = true;
                    // TODO: what we really want to do here is redisplay the ReportMenu with the appropriate alert dialog
                    WebPage rptPage = privLabel.getWebPage(rptMenu);
                    if (rptPage != null) {
                        rptPage.writePage(reqState, m);
                    }
                    return;
                }
            } catch (ReportException re) {
                Print.logException("Error generating CSV: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.csvError","Error generating CSV: {0}",reportID));
            }
            return;
        }

        /* graph? */
        // output as graph image to browser
        if (rptFormat.equalsIgnoreCase(ReportData.FORMAT_GRAPH)) {
            try {
                this._writeReportGraph(response, reqState, report, i18n);
            } catch (ReportException re) {
                Print.logException("Error generating Map: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.mapError","Error generating map: {0}",reportID));
            }
            return;
        }

        /* map? */
        // output as map to browser
        if (rptFormat.equalsIgnoreCase(ReportData.FORMAT_MAP)) {
            try {
                this._writeReportMap(response, reqState, report, i18n);
            } catch (ReportException re) {
                Print.logException("Error generating Map: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.mapError","Error generating map: {0}",reportID));
            }
            return;
        }

        /* KML? */
        // output as KML to browser
        if (rptFormat.equalsIgnoreCase(ReportData.FORMAT_KML)) {
            try {
                this._writeReportKML(response, reqState, report);
            } catch (ReportException re) {
                Print.logException("Error generating KML: "  + reportID, re);
                Track.writeErrorResponse(reqState, i18n.getString("ReportDisplay.kmlError","Error generating KML: {0}",reportID));
            }
            return;
        }

        /* style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                try {
                    out.write("\n");
                    out.write("<!-- Begin Report Style -->\n");
                    String cssDir = ReportDisplay.this.getCssDirectory(); 
                    WebPageAdaptor.writeCssLink(out, reqState, "ReportDisplay.css", cssDir);
                    if (reportLayout.hasCSSFiles()) {
                        for (String file : reportLayout.getCSSFiles(true)) {
                            WebPageAdaptor.writeCssLink(out, reqState, file, cssDir);
                        }
                    }
                    report.writeReportStyle(ReportPresentation.FORMAT_HTML, out);
                    out.write("<!-- End Report Style -->\n");
                    out.write("\n");
                } catch (ReportException re) {
                    throw new IOException(re.getMessage());
                }
            }
        };

        /* JavaScript */
        final boolean isTableSortable = reportFactory.isTableSortable();
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                if (isTableSortable) {
                    JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(ReportPresentation.SORTTABLE_JS));
                }
            }
        };

        /* report */
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_REPORT_DISPLAY, m) {
            public void write(PrintWriter out) throws IOException {
                try {
                    report.writeReport(ReportPresentation.FORMAT_HTML, out);
                } catch (ReportException re) {
                    throw new IOException(re.getMessage());
                }
            }
        };

        /* Tag as periodic report */
        // save report paramters
        if (rptFormat.equalsIgnoreCase(ReportPresentation.FORMAT_SCHEDULE)) {
            URIArg reportURL = reqState.getHttpServletRequestURIArg(false);
            reportURL.removeArg(Constants.PARM_ENCPASS); // remove password, if present
            TimeZone _tz    = DateTime.getTimeZone(reportURL.getArgValue(Calendar.PARM_TIMEZONE));
            DateTime _dtNow = new DateTime(_tz);
            long     _dyNow = DateTime.getDayNumberFromDate(_dtNow);
            DateTime _dtFr  = Calendar.parseDate(reportURL.getArgValue(Calendar.PARM_RANGE_FR),_tz,false);
            long     _dyFr  = DateTime.getDayNumberFromDate(_dtFr);
            DateTime _dtTo  = Calendar.parseDate(reportURL.getArgValue(Calendar.PARM_RANGE_TO),_tz,true );
            long     _dyTo  = DateTime.getDayNumberFromDate(_dtTo);
            String   _ranFr = ((_dyFr<_dyNow)?"":"+") + String.valueOf(_dyFr-_dyNow) + "d," + _dtFr.format("HH:mm:ss");
            String   _ranTo = ((_dyTo<_dyNow)?"":"+") + String.valueOf(_dyTo-_dyNow) + "d," + _dtTo.format("HH:mm:ss");
            reportURL.addArg(Calendar.PARM_RANGE_FR, _ranFr);
            reportURL.addArg(Calendar.PARM_RANGE_TO, _ranTo);
            // save this URL "?.....&date_fr=-3d,00:00:00&date_to=+0,23:59:59&..."
            Print.logInfo("Report URL: " + reportURL);
            // redisplay the Report Menu
            WebPage rptPage = privLabel.getWebPage(rptMenu);
            if (rptPage != null) {
                rptPage.writePage(reqState, m);
            }
            return;
        }

        /* EMail report */
        // output as EMail to SMTP server
        if (rptFormat.equalsIgnoreCase(ReportPresentation.FORMAT_EHTML)) {
            // set Report JSP
            String uri = privLabel.getJSPFile("emailReport", false);
            //Print.logInfo("Embedded Report JSP: " + uri);
            reqState.setWebPageURI(uri);
            reqState.setEncodeEMailHTML(true);
            URIArg emailURL = reqState.getHttpServletRequestURIArg(true);
            RTProperties emailLinkProps = null;
            if (emailURL != null) {
                emailURL.addArg(ReportURL.RPTARG_FORMAT,ReportPresentation.FORMAT_HTML);
                if (privLabel.hasDefaultBaseURL()) {
                    emailURL.setURI(privLabel.getDefaultBaseURL());
                }
                Print.logInfo("EMail URL(1): " + emailURL);
                String rtpVal = URIArg.encodeRTP(emailURL.getArgProperties());
                emailURL = new URIArg(emailURL.getURI(),true);
                emailURL.addArg(AttributeTools.ATTR_RTP, rtpVal);
                //Print.logInfo("EMail URL(2): " + emailURL);
                emailLinkProps = new RTProperties();
                emailLinkProps.setString("EMailReport.url" , emailURL.toString());
                emailLinkProps.setString("EMailReport.desc", i18n.getString("ReportDisplay.webBrowserLink", "Web Link"));
            }
            // write report byte array
            //Print.logInfo("Report JSP: " + reqState.getJspURI());
            HttpServletResponse httpResp = reqState.getHttpServletResponse();
            BufferedHttpServletResponse bhsp = new BufferedHttpServletResponse(httpResp);
            reqState.setHttpServletResponse(bhsp);
            try {
                if (emailLinkProps != null) {
                    RTConfig.pushTemporaryProperties(emailLinkProps);
                }
                CommonServlet.writePageFrame(
                    reqState,
                    null,null,                  // onLoad/onUnload
                    HTML_CSS,                   // Style sheets
                    HTML_JS,                    // JavaScript
                    null,                       // Navigation
                    HTML_CONTENT);              // Content
            } finally {
                if (emailLinkProps != null) {
                    RTConfig.popTemporaryProperties(emailLinkProps);
                }
            }
            String s = bhsp.toString();
            //Print.logInfo("Report HTML:\n" + s);
            // email report
            int logLevel = Print.getLogLevel();
            Print.setLogLevel(Print.LOG_ALL);
            try {
                String frEmail = (privLabel != null)? privLabel.getEventNotificationFrom() : null;
                String toEmail = emailAddr; // account.getReportEmail(user);
                if (StringTools.isBlank(frEmail)) {
                    m = i18n.getString("ReportDisplay.missingFromEmail","The 'From' email address has not been configured");
                } else
                if (StringTools.isBlank(toEmail)) {
                    m = i18n.getString("ReportDisplay.missingToEmail","No recipient email address has been specified");
                } else {
                    String subj = i18n.getString("ReportDisplay.reportTitle","Report") + ": " + 
                        report.getReportTitle();
                    String body = subj; //  + "\n" + StringTools.trim(emailURL.toString());
                    byte rptAttach[] = bhsp.toByteArray();
                    SendMail.Attachment attach = new SendMail.Attachment(
                        rptAttach, 
                        reportID + ".html", 
                        HTMLTools.MIME_HTML());
                    SendMail.send(frEmail, toEmail, subj, body, attach);
                    m = i18n.getString("ReportDisplay.reportEmailed","The selected report has been emailed");
                    if (SAVE_EHTML_TO_TMP) {
                        // debug purposes only
                        File rptFile = new File("/tmp/" + reportID + ".html");
                        FileTools.writeFile(rptAttach, rptFile);
                    }
                }
            } catch (Throwable t) { // NoClassDefFoundException, ClassNotFoundException
                // this will fail if JavaMail support for SendMail is not available.
                Print.logWarn("SendMail error: " + t);
                m = i18n.getString("ReportDisplay.sendMailError","An error occurred while attempting to send email");
            } finally {
                Print.setLogLevel(logLevel);
            }
            // redisplay the Report Menu
            reqState.setHttpServletResponse(httpResp);
            reqState.setWebPageURI(null);
            reqState.setEncodeEMailHTML(false);
            WebPage rptPage = privLabel.getWebPage(rptMenu);
            if (rptPage != null) {
                rptPage.writePage(reqState, m);
            }
            return;
        }
        
        /* write report to client browser output stream */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    // ------------------------------------------------------------------------

}
