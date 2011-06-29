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
//  2007/12/13  Martin D. Flynn
//     -Add indication of partial displayed data if the record limit was reached.
//  2008/03/12  Martin D. Flynn
//     -Changed the partial displayed data message to span 3 table columns.
//  2009/09/23  Clifton Flynn, Martin D. Flynn
//     -Added SOAP xml support
// ----------------------------------------------------------------------------
package org.opengts.war.report.presentation;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.db.*;

import org.opengts.war.tools.*;
import org.opengts.war.report.*;

public class ReportTable
    implements ReportPresentation
{

    // ------------------------------------------------------------------------

    public static final String CSS_CLASS_TABLE      = ReportLayout.CSS_CLASS_TABLE;
    public static final String CSS_CLASS_TABLE_SORT = ReportLayout.CSS_CLASS_TABLE_SORT;

    public static final int    INDENT               = ReportPresentation.INDENT;

    // ------------------------------------------------------------------------

    private ReportHeader rptHeader = null;
    private ReportBody   rptBody   = null;
    
    private Map<String,HeaderColumnTemplate> headerColumnMap = null;
    private Map<String,BodyColumnTemplate>   bodyColumnMap   = null;

    // ------------------------------------------------------------------------

    public ReportTable()
    {
        this(null, null);
    }
    
    protected ReportTable(ReportHeader rh, ReportBody rb)
    {
        this.rptHeader       = (rh != null)? rh : new ReportHeader(this);
        this.rptBody         = (rb != null)? rb : new ReportBody(this);
        this.headerColumnMap = new HashMap<String,HeaderColumnTemplate>();
        this.bodyColumnMap   = new HashMap<String,BodyColumnTemplate>();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public int writeReport(String format, ReportData rd, PrintWriter out, int indentLevel)
        throws ReportException
    {
        Format rptFmt = EnumTools.getValueOf(Format.class, format);
        return this.writeReport(rptFmt, rd, out, indentLevel);
    }

    public int writeReport(Format format, ReportData rd, PrintWriter out, int indentLevel)
        throws ReportException
    {
        if (format == null) {
            format = Format.HTML;
        }
        switch (format) {
            case XML :
            case SOAP :
                return this.writeXML( out, indentLevel, rd, false);
            case EHTML:
                return this.writeXML( out, indentLevel, rd, true);
            case CSV :
                return this.writeCSV( out, indentLevel, rd, true);  // csv
            case TXT :
                return this.writeCSV( out, indentLevel, rd, false); // text/plain
            case HTML:
            default:
                return this.writeHTML(out, indentLevel, rd);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeHTML(PrintWriter out, int level, ReportData rd) 
        throws ReportException
    {

        /* no ReportData */
        if (rd == null) {
            return 0;
        }

        /* simple report */
        if (!rd.isSingleDeviceOnly() || (rd.getDeviceCount() <= 1)) {
            return this._writeHTML(out, level, rd, -1);
        }

        /* multiple per-device reports */
        ReportDeviceList rdl = rd.getReportDeviceList();
        java.util.List<ReportDeviceList.DeviceHolder> dhList = rdl.getDeviceHolderList(true);
        rdl.clear();
        int rcdCount = 0;
        int devCount = dhList.size();
        for (int i = 0; i < devCount; i++) {
            if (i > 0) {
                out.print("<br>\n");
            }
            rdl.setDevice(null,dhList.get(i));
            rcdCount += this._writeHTML(out, level, rd, i);
        }
        return rcdCount;

    }

    private int _writeHTML(PrintWriter out, int level, ReportData rd, int ndx) 
        throws ReportException
    {
        RequestProperties reqState = rd.getRequestProperties();
        boolean isEMail = reqState.getEncodeEMailHTML();
        PrivateLabel privLabel = rd.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportTable.class);

        out.print("<center>\n");
        out.print("<table cellspacing='0' cellpadding='0' border='0'>\n");

        /* report title row */
        {
            String rptTitle = rd.getReportTitle();
            out.print("<tr><td colSpan='3'><H1 class=\"rptTitle\">" + FilterText(rptTitle) + "</H1></td></tr>\n");
        }
        
        /* report subtitle row */
        {
            out.print("<tr>\n");

            // "Refresh"
            out.print("<td>");
            StringBuffer linkSB_L = new StringBuffer();
            String refreshURL = (!isEMail && (ndx <= 0))? EncodeURL(reqState, rd.getRefreshURL()) : null;
            if (!StringTools.isBlank(refreshURL)) {
                String refreshDesc = i18n.getString("ReportTable.refreshReport","Refresh");
                if (linkSB_L.length() > 0) { linkSB_L.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_L.append("<a href='"+refreshURL+"' target='_self'>"+refreshDesc+"</a>"); // target='_top'
            }
            // ...
            if (linkSB_L.length() > 0) {
                out.print(linkSB_L.toString());
            } else {
                out.print("&nbsp;");
            }
            out.print("</td>\n");

            // Report subtitle
            out.print("<td width='100%'>");
            String rptSubtt = rd.getReportSubtitle();
            if (!StringTools.isBlank(rptSubtt)) {
                out.print("<H2 class=\"rptSubtitle\">" + FilterText(rptSubtt) + "</H2>");
            } else {
                out.print("&nbsp;");
            }
            out.print("</td>\n");

            // "Graph", "Map" links
            out.print("<td>");
            StringBuffer linkSB_R = new StringBuffer();
            String graphURL = (!isEMail && (ndx < 0) && rd.getSupportsGraphDisplay())? EncodeURL(reqState,rd.getGraphURL()) : null;
            if (!StringTools.isBlank(graphURL)) {
                MapDimension sz = rd.getGraphWindowSize();
                String desc = rd.getGraphLinkDescription();
                if (StringTools.isBlank(desc)) { desc = i18n.getString("ReportTable.displayGraph","Graph"); }
                if (linkSB_R.length() > 0) { linkSB_R.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_R.append("<span class='spanLink' onclick=\"javascript:openResizableWindow('"+graphURL+"','ReportGraph',"+sz.getWidth()+","+sz.getHeight()+");\">"+desc+"</span>");
            }
            String mapURL = (!isEMail && (ndx < 0) && rd.getSupportsMapDisplay())? EncodeURL(reqState,rd.getMapURL()) : null;
            if (!StringTools.isBlank(mapURL)) {
                MapDimension sz = rd.getMapWindowSize();
                String desc = rd.getMapLinkDescription();
                if (StringTools.isBlank(desc)) { desc = i18n.getString("ReportTable.displayMap","Map"); }
                if (linkSB_R.length() > 0) { linkSB_R.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_R.append("<span class='spanLink' onclick=\"javascript:openResizableWindow('"+mapURL+"','ReportMap',"+sz.getWidth()+","+sz.getHeight()+");\">"+desc+"</span>");
            }
            String kmlURL = (!isEMail && (ndx < 0) && rd.getSupportsKmlDisplay() && privLabel.getBooleanProperty(PrivateLabel.PROP_ReportDisplay_showGoogleKML,false))? EncodeURL(reqState,rd.getKmlURL()) : null;
            if (!StringTools.isBlank(kmlURL)) {
                String desc = rd.getKmlLinkDescription();
                if (StringTools.isBlank(desc)) { desc = i18n.getString("ReportTable.displayKML","KML"); }
                if (linkSB_R.length() > 0) { linkSB_R.append("&nbsp;&nbsp;"); } // add space between links
                linkSB_R.append("<a href='"+kmlURL+"' target='_blank'>"+desc+"</a>");
            }
            // ...
            if (linkSB_R.length() > 0) {
                out.print(linkSB_R.toString());
            } else {
                out.print("&nbsp;");
            }
            out.print("</td>\n");

            out.print("</tr>\n");
        }

        /* start report */
        out.print("<tr>\n");
        out.print("<td colSpan='3'>\n");
        String tableClass = rd.getReportFactory().isTableSortable()? CSS_CLASS_TABLE_SORT : CSS_CLASS_TABLE;
        out.print("<table class='"+tableClass+"' width='100%' cellspacing='0' cellpadding='0' border='0'>\n");
        out.print("<!-- Report Header -->\n");
        this.rptHeader.writeHTML(out, level+1, rd);
        out.print("<!-- Report Data -->\n");
        this.rptBody.writeHTML(out, level+1, rd);
        out.print("</table>\n");
        out.print("</td>\n");
        out.print("</tr>\n");

        /* no/partial data indication */
        if (this.rptBody.getRecordCount() <= 0) {
            out.print("<tr>\n");
            out.print("<td colSpan='3'><H2 class=\"rptNoData\">");
            String t = i18n.getString("ReportTable.noData","This report contains no data");
            out.print(FilterText(t));
            out.print("</H2></td>\n");
            out.print("</tr>\n");
        } else
        if (this.rptBody.isPartial()) {
            out.print("<tr>\n");
            out.print("<td colSpan='3'><H2 class=\"rptPartial\">");
            String t = i18n.getString("ReportTable.partialData","This report has reached it's record display limit and may only contain a portion of the possible data");
            out.print(FilterText(t));
            out.print("</H2></td>\n");
            out.print("</tr>\n");
        }

        out.print("</table>\n");
        out.print("</center>\n");
        return this.rptBody.getRecordCount();

    }

    // ------------------------------------------------------------------------
    
    public static String EncodeURL(RequestProperties reqState, URIArg url)
    {
        return WebPageAdaptor.EncodeURL(reqState, url);
    }

    public static String FilterText(String s)
    {
        return WebPageAdaptor.FilterText(s);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeXML(PrintWriter out, int level, ReportData rd, boolean urlOnly) 
        throws ReportException
    {
        int rcdCount = 0;
        rcdCount += this._writeXML(out, level, rd, urlOnly);
        return rcdCount;
    }
    
    private int _writeXML(PrintWriter out, int level, ReportData rd, boolean urlOnly) 
        throws ReportException
    {
        boolean isSoapRequest = rd.isSoapRequest();
        RequestProperties reqState = rd.getRequestProperties();
        PrivateLabel privLabel = rd.getPrivateLabel();
        I18N i18n = privLabel.getI18N(ReportTable.class);
        String PFX1 = XMLTools.PREFIX(isSoapRequest, level * ReportTable.INDENT);
        String PFX2 = XMLTools.PREFIX(isSoapRequest, (level + 1) * ReportTable.INDENT);

        /* begin */
        out.print(PFX1);
        out.print(XMLTools.startTAG(isSoapRequest,"Report", // TAG_Report
            XMLTools.ATTR("name",rd.getReportName()) +      // ATTR_name
            XMLTools.ATTR("type",rd.getReportType()),       // ATTR_type
            false,true));
        
        /* constraints */
        ReportConstraints rc = rd.getReportConstraints();
        String   dtFmt = DateTime.DEFAULT_DATE_FORMAT + "," + DateTime.DEFAULT_TIME_FORMAT;
        TimeZone tzone = rd.getTimeZone();
        String   tzStr = rd.getTimeZoneString();
        long     tmBeg = rc.getTimeStart();
        long     tmEnd = rc.getTimeEnd();
        DateTime dtStr = new DateTime(tmBeg,tzone);
        DateTime dtEnd = new DateTime(tmEnd,tzone);

        /* Account */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"Account","",false,false));           // TAG_Account
        out.print(XmlFilter(isSoapRequest,rd.getAccountID()));
        out.print(XMLTools.endTAG(isSoapRequest,"Account",true));                       // TAG_Account

        /* TimeFrom */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"TimeFrom",                           // TAG_TimeFrom
            XMLTools.ATTR("timestamp",String.valueOf(tmBeg)) +                          // ATTR_timestamp
            XMLTools.ATTR("timezone",tzStr),                                            // ATTR_timezone
            false,false));
        out.print((tmBeg>0L)? XmlFilter(isSoapRequest,dtStr.format(dtFmt)) : "");
        out.print(XMLTools.endTAG(isSoapRequest,"TimeFrom",true));                      // TAG_TimeFrom

        /* TimeTo */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"TimeTo",                             // TAG_TimeTo
            XMLTools.ATTR("timestamp",String.valueOf(tmEnd)) +                          // ATTR_timestamp
            XMLTools.ATTR("timezone",tzStr),                                            // ATTR_timezone
            false,false));
        out.print((tmEnd>0L)? XmlFilter(isSoapRequest,dtEnd.format(dtFmt)) : "");
        out.print(XMLTools.endTAG(isSoapRequest,"TimeTo",true));                        // TAG_TimeTo

        /* ValidGPSRequired */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"ValidGPSRequired","",false,false));  // TAG_ValidGPSRequired
        out.print(XmlFilter(isSoapRequest,rc.getValidGPSRequired()));
        out.print(XMLTools.endTAG(isSoapRequest,"ValidGPSRequired",true));              // TAG_ValidGPSRequired

        /* SelectionLimit */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"SelectionLimit",                     // TAG_SelectionLimit
            XMLTools.ATTR("type",rc.getSelectionLimitType()),
            false,false));
        out.print(XmlFilter(isSoapRequest,rc.getSelectionLimit()));                     
        out.print(XMLTools.endTAG(isSoapRequest,"SelectionLimit",true));                // TAG_SelectionLimit

        /* Ascending */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"Ascending","",false,false));         // TAG_Ascending
        out.print(XmlFilter(isSoapRequest,rc.getOrderAscending()));
        out.print(XMLTools.endTAG(isSoapRequest,"Ascending",true));                     // TAG_Ascending

        /* ReportLimit */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"ReportLimit","",false,false));       // TAG_ReportLimit
        out.print(XmlFilter(isSoapRequest,rc.getReportLimit()));
        out.print(XMLTools.endTAG(isSoapRequest,"ReportLimit",true));                   // TAG_ReportLimit

        /* Where */
        if (rc.hasWhere()) {
            out.print(PFX2);
            out.print(XMLTools.startTAG(isSoapRequest,"Where","",false,false));         // TAG_Where
            out.print(XmlFilter(isSoapRequest,rc.getWhere()));
            out.print(XMLTools.endTAG(isSoapRequest,"Where",true));                     // TAG_Where
        }
        
        /* RuleSelector */
        if (rc.hasRuleSelector()) {
            out.print(PFX2);
            out.print(XMLTools.startTAG(isSoapRequest,"RuleSelector","",false,false));  // TAG_RuleSelector
            out.print(XmlFilter(isSoapRequest,rc.getRuleSelector()));
            out.print(XMLTools.endTAG(isSoapRequest,"RuleSelector",true));              // TAG_RuleSelector
        }

        /* Title */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"Title","",false,false));             // TAG_Title
        out.print(XmlFilter(isSoapRequest,rd.getReportTitle()));
        out.print(XMLTools.endTAG(isSoapRequest,"Title",true));                         // TAG_Title

        /* Subtitle */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"Subtitle","",false,false));          // TAG_Subtitle
        out.print(XmlFilter(isSoapRequest,rd.getReportSubtitle()));
        out.print(XMLTools.endTAG(isSoapRequest,"Subtitle",true));                      // TAG_Subtitle
        
        /* URL */
        if (urlOnly) {
            // Web-URL only
            HttpServletRequest request = reqState.getHttpServletRequest();
            ReportDeviceList devList = rd.getReportDeviceList();
            String deviceID = devList.isDeviceGroup()? null : devList.getFirstDeviceID();
            String groupID  = devList.isDeviceGroup()? devList.getDeviceGroupID() : null;
            String baseURL  = privLabel.hasDefaultBaseURL()?
                privLabel.getDefaultBaseURL() :
                ((request != null)? request.getRequestURL().toString() : "");
            URIArg rptURL = ReportURL.createReportURL(
                baseURL, false,
                rd.getAccountID(), rd.getUserID(), "",
                deviceID, groupID,
                rc.getTimeStart(), rc.getTimeEnd(), rd.getTimeZoneString(),
                rd.getReportName(),
                rc.getReportLimit(), rc.getSelectionLimitType().toString(),
                ReportPresentation.FORMAT_HTML);
            out.print(PFX2);
            out.print(XMLTools.startTAG(isSoapRequest,"URL","",false,false));               // TAG_URL
            out.print(XmlFilter(isSoapRequest,rptURL.toString()));
            out.print(XMLTools.endTAG(isSoapRequest,"URL",true));                           // TAG_URL
        } else {
            // Report header/body
            this.rptHeader.writeXML(out, level+1, rd);
            this.rptBody  .writeXML(out, level+1, rd);
        }

        /* Partial */
        out.print(PFX2);
        out.print(XMLTools.startTAG(isSoapRequest,"Partial","",false,false));           // TAG_Partial
        out.print(XmlFilter(isSoapRequest,this.rptBody.isPartial()));
        out.print(XMLTools.endTAG(isSoapRequest,"Partial",true));                       // TAG_Partial

        /* end of report */
        out.print(PFX1);
        out.print(XMLTools.endTAG(isSoapRequest,"Report",true));                        // TAG_Report
        return this.rptBody.getRecordCount();

    }

    private static final char XML_CHARS[] = new char[] { '_', '-', '.', ',', '/', '+', ':', '|', '=', ' ' };
    public static String XmlFilter(boolean isSoapReq, String value)
    {
        if ((value == null) || value.equals("")) { // do not use StringTools.isBlank (spaces are significant)
            return "";
        } else
        if (StringTools.isAlphaNumeric(value,XML_CHARS)) {
            return value; // return all significant spaces
        } else {
            String v = StringTools.replace(value,"\n","\\n");
            return XMLTools.CDATA(isSoapReq, v);
        }
    }

    public static String XmlFilter(boolean isSoapReq, long value)
    {
        return String.valueOf(value);
    }

    public static String XmlFilter(boolean isSoapReq, int value)
    {
        return String.valueOf(value);
    }

    public static String XmlFilter(boolean isSoapReq, boolean value)
    {
        return String.valueOf(value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private int writeCSV(PrintWriter out, int level, ReportData rd, boolean mimeCSV) 
        throws ReportException
    {

        /* MIME type */
        // (See "org.opengts.war.track.page.ReportDisplay:writePage")
        HttpServletResponse response = rd.getRequestProperties().getHttpServletResponse();
        if (mimeCSV) {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_CSV());
        } else {
            CommonServlet.setResponseContentType(response, HTMLTools.MIME_PLAIN());
        }

        /* header row */
        this.rptHeader.writeCSV(out, level+1, rd);

        /* body */
        this.rptBody.writeCSV(out, level+1, rd);
        return this.rptBody.getRecordCount();

    }

    public static String csvFilter(String value)
    {
        return StringTools.quoteCSVString(value);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected HeaderColumnTemplate _createHeaderColumnTemplate(DataColumnTemplate dct)
    {
        return new HeaderColumnTemplate(dct);
    }
    
    public HeaderColumnTemplate getHeaderColumnTemplate(DataColumnTemplate dct)
    {
        if (dct != null) {
            String keyName = dct.getKeyName();
            if (this.headerColumnMap.containsKey(keyName)) {
                return this.headerColumnMap.get(keyName);
            } else {
                HeaderColumnTemplate hct = this._createHeaderColumnTemplate(dct);
                this.headerColumnMap.put(keyName, hct);
                return hct;
            }
        } else {
            Print.logStackTrace("DataColumnTemplate is null!");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    protected BodyColumnTemplate _createBodyColumnTemplate(DataColumnTemplate dct)
    {
        return new BodyColumnTemplate(dct.getKeyName());
    }

    public BodyColumnTemplate getBodyColumnTemplate(DataColumnTemplate dct)
    {
        if (dct != null) {
            String keyName = dct.getKeyName();
            if (this.bodyColumnMap.containsKey(keyName)) {
                return this.bodyColumnMap.get(keyName);
            } else {
                BodyColumnTemplate bct = this._createBodyColumnTemplate(dct);
                this.bodyColumnMap.put(keyName, bct);
                return bct;
            }
        } else {
            Print.logStackTrace("DataColumnTemplate is null!");
            return null;
        }
    }

    // ------------------------------------------------------------------------

}
