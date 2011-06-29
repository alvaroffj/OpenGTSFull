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
package org.opengts.war.report;

import java.io.*;

import org.opengts.util.EnumTools;

public interface ReportPresentation
{

    // ------------------------------------------------------------------------
    // Sortable table constants (used by 'sorttable.js')

    public static final String SORTTABLE_SORTKEY            = "sorttable_customkey";
    public static final String SORTTABLE_JS                 = "sorttable/sorttable.js";
    public static final String SORTTABLE_CSS_CLASS          = "sortable";
    public static final String SORTTABLE_CSS_NOSORT         = "nosort"; // MDF modified, was "sorttable_nosort";

    // ------------------------------------------------------------------------

    public static final int    INDENT                       = 3;

    // ------------------------------------------------------------------------

    public static final String FORMAT_HTML                  = "html";
    public static final String FORMAT_XML                   = "xml";
    public static final String FORMAT_CSV                   = "csv";
    public static final String FORMAT_TXT                   = "txt";
    public static final String FORMAT_SOAPXML               = "soapxml";
    public static final String FORMAT_EHTML                 = "ehtml";      // embedded HTML (no external links)
    public static final String FORMAT_CUSTOM                = "custom";
    public static final String FORMAT_SCHEDULE              = "sched";

    public enum Format implements EnumTools.IntValue, EnumTools.StringValue {
        HTML     ( 0, FORMAT_HTML     ),    // MIME: "text/html" (default)
        XML      ( 1, FORMAT_XML      ),    // MIME: "text/xml"
        CSV      ( 2, FORMAT_CSV      ),    // MIME: "text/csv"
        TXT      ( 3, FORMAT_TXT      ),    // MIME: "text/plain" (csv format)
        SOAP     ( 4, FORMAT_SOAPXML  ),    // 
        EHTML    ( 5, FORMAT_EHTML    ),    // 
        CUSTOM   ( 6, FORMAT_CUSTOM   ),    // 
        SCHEDULE ( 7, FORMAT_SCHEDULE );    // 
        // ---
        private int      vv = 0;
        private String   aa = null;
        Format(int v, String a)         { vv = v; aa = a; }
        public int     getIntValue()    { return vv; }
        public String  getStringValue() { return this.toString(); }
        public String  toString()       { return aa; }
    }

    // ------------------------------------------------------------------------

    public int writeReport(String format, ReportData rd, PrintWriter out, int indentLevel)
        throws ReportException;

    public int writeReport(Format format, ReportData rd, PrintWriter out, int indentLevel)
        throws ReportException;

    // ------------------------------------------------------------------------

}
