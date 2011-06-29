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
//  2009/07/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;

import org.opengts.util.*;

public class ReportURL
{

    // ------------------------------------------------------------------------
    
    public  static final String RPTARG_ACCOUNT      = "account";
    public  static final String RPTARG_USER         = "user";
    public  static final String RPTARG_ENCPASS      = "encpass";

    public  static final String RPTARG_DEVICE       = "device";
    public  static final String RPTARG_GROUP        = "group";
    
    public  static final String RPTARG_DATE_FR      = "date_fr";
    public  static final String RPTARG_DATE_TO      = "date_to";
    public  static final String RPTARG_DATE_TZ      = "date_tz";
    
    public  static final String RPTARG_REPORT       = "r_report";
    public  static final String RPTARG_LIMIT        = "r_limit";
    public  static final String RPTARG_LIMIT_TYPE   = "r_limType";
    
    public  static final String RPTARG_FORMAT       = "r_format";
    
    public  static final String URLARG_RTP          = "rtp_";

    // ------------------------------------------------------------------------
    // account=<account> user=<user>
    // r_report=<report> 
    // device=<device> | group=<group>
    // date_fr=<ts> date_to=<ts> date_tz=<tz>
    // r_limit=<limit> r_limType=last|first
    // format=html|csv|xml

    public static URIArg createReportURL(URIArg rptURL, boolean rtpEncode)
    {
        if (rptURL == null) { return null; }
        String  baseURL   = rptURL.getURI();
        RTProperties rtp  = rptURL.getArgProperties();
        String  account   = rtp.getString(RPTARG_ACCOUNT     , "");
        String  user      = rtp.getString(RPTARG_USER        , "");
        String  encpass   = rtp.getString(RPTARG_ENCPASS     , "");
        String  device    = rtp.getString(RPTARG_DEVICE      , "");
        String  group     = rtp.getString(RPTARG_GROUP       , "");
        long    date_fr   = rtp.getLong  (RPTARG_DATE_FR     , -1L);
        long    date_to   = rtp.getLong  (RPTARG_DATE_TO     , -1L);
        String  date_tz   = rtp.getString(RPTARG_DATE_TZ     , "");
        String  r_report  = rtp.getString(RPTARG_REPORT      , "");
        long    r_limit   = rtp.getLong  (RPTARG_LIMIT       , -1L);
        String  r_limType = rtp.getString(RPTARG_LIMIT_TYPE  , "");
        String  r_format  = rtp.getString(RPTARG_FORMAT      , "");
        return  ReportURL.createReportURL(
            baseURL, rtpEncode,
            account, user, encpass,
            device, group,
            date_fr, date_to, date_tz,
            r_report,
            r_limit, r_limType,
            r_format);
    }

    public static URIArg createReportURL(
        String baseURL, boolean rtpEncode,
        String account, String user, String encPass,
        String device, String group,
        long date_fr, long date_to, String date_tz,
        String r_report,
        long r_limit, String r_limType,
        String r_format)
    {

        /* create RTP */
        RTProperties rtp = new RTProperties();
        rtp.setString("page", "report.show");
        if (!StringTools.isBlank(device)) {
            rtp.setString(RPTARG_DEVICE, device);
        }
        if (!StringTools.isBlank(group)) {
            rtp.setString(RPTARG_GROUP, group);
        }
        if (date_fr > 0L) {
            rtp.setLong(RPTARG_DATE_FR, date_fr);
        }
        if (date_to > 0L) {
            rtp.setLong(RPTARG_DATE_TO, date_to);
        }
        if (!StringTools.isBlank(date_tz)) {
            rtp.setString(RPTARG_DATE_TZ, date_tz);
        }
        if (!StringTools.isBlank(r_report)) {
            rtp.setString(RPTARG_REPORT, r_report);
        }
        if (r_limit > 0L) {
            rtp.setLong(RPTARG_LIMIT, r_limit);
        }
        if (!StringTools.isBlank(r_limType)) {
            rtp.setString(RPTARG_LIMIT_TYPE, r_limType);
        }
        if (!StringTools.isBlank(r_format)) {
            rtp.setString(RPTARG_FORMAT, r_format);
        }

        /* URL */
        URIArg url = new URIArg(baseURL);
        if (!StringTools.isBlank(account)) {
            url.addArg(RPTARG_ACCOUNT, account);
        }
        if (!StringTools.isBlank(user)) {
            url.addArg(RPTARG_USER, user);
        }
        if (encPass != null) { // blank is allowed
            url.addArg(RPTARG_ENCPASS, encPass);
        }
        
        /* remaining arguments */
        if (rtpEncode) {
            url.addArg(URLARG_RTP, rtp);
        } else {
            Map<Object,Object> props = rtp.getProperties();
            for (Object rtk : props.keySet()) {
                Object rtv = props.get(rtk);
                url.addArg((String)rtk, StringTools.trim(rtv));
            }
        }
        
        /* URL */
        return url;

    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String args[])
    {
        RTConfig.setCommandLineArgs(args);
        
        if (RTConfig.hasProperty("urld")) {
            String urld = RTConfig.getString("urld","");
            URIArg rtpUrl = new URIArg(urld);
            URIArg decUrl = rtpUrl.rtpDecode(URLARG_RTP);
            Print.sysPrintln("URL: " + decUrl.toString());
            System.exit(0);
        }
        
        if (RTConfig.hasProperty("urle")) {
            String urle = RTConfig.getString("urle","");
            URIArg decUrl = new URIArg(urle);
            URIArg rtpUrl = decUrl.rtpEncode(URLARG_RTP, RPTARG_ACCOUNT, RPTARG_USER);
            Print.sysPrintln("URL: " + rtpUrl.toString());
            System.exit(0);
        }
        
        String url       = RTConfig.getString("url"             ,"");
        String account   = RTConfig.getString(RPTARG_ACCOUNT    ,"");
        String user      = RTConfig.getString(RPTARG_USER       ,"");
        String encpass   = RTConfig.getString(RPTARG_ENCPASS    ,"");
        String device    = RTConfig.getString(RPTARG_DEVICE     ,"");
        String group     = RTConfig.getString(RPTARG_GROUP      ,"");
        String date_fr_  = RTConfig.getString(RPTARG_DATE_FR    ,"");
        String date_to_  = RTConfig.getString(RPTARG_DATE_TO    ,"");
        String date_tz   = RTConfig.getString(RPTARG_DATE_TZ    ,"");
        String r_report  = RTConfig.getString(RPTARG_REPORT     ,"");
        String r_limit_  = RTConfig.getString(RPTARG_LIMIT      ,"");
        String r_limType = RTConfig.getString(RPTARG_LIMIT_TYPE ,"");
        String format    = RTConfig.getString(RPTARG_FORMAT     ,"");
        
        /* from/to dates */
        TimeZone tmz = DateTime.getTimeZone(date_tz);
        long date_fr = -1L;
        try {
            date_fr = DateTime.parseDateTime(date_fr_, tmz, DateTime.DefaultParsedTime.DayStart).getEpochTime();
        } catch (DateTime.DateParseException dpe) {
            date_fr = -1L;
        }
        long date_to = -1L;
        try {
            date_to = DateTime.parseDateTime(date_to_, tmz, DateTime.DefaultParsedTime.DayEnd).getEpochTime();
        } catch (DateTime.DateParseException dpe) {
            date_to = -1L;
        }
        
        /* report limit */
        int r_limit = StringTools.parseInt(r_limit_, -1);

        /* URL */
        URIArg rptURL = createReportURL(
            url, false,
            account, user, "",
            device, group,
            date_fr, date_to, date_tz,
            r_report, r_limit, r_limType,
            format);
        Print.logInfo("URL: " + rptURL);

    }

}

