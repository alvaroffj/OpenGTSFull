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
//  2008/12/01  Martin D. Flynn
//     -Initial release
// ----------------------------------------------------------------------------
package org.opengts.war.track;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.JspWriter;

import org.opengts.util.*;

import org.opengts.db.tables.*;
import org.opengts.war.tools.*;

public class IconMenu
{
    
    private static final String BLANK_IMAGE     = "images/blank.png";

    // ------------------------------------------------------------------------
    // write Style

    public static void writeStyle(JspWriter out, RequestProperties reqState)
        throws IOException
    {
        IconMenu.writeStyle(new PrintWriter(out, out.isAutoFlush()), reqState);
    }

    public static void writeStyle(PrintWriter out, RequestProperties reqState)
        throws IOException 
    {
        WebPageAdaptor.writeCssLink(out, reqState, "IconMenu.css", null);
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeJavaScript(JspWriter out, RequestProperties reqState)
        throws IOException
    {
        IconMenu.writeJavaScript(new PrintWriter(out, out.isAutoFlush()), reqState);
    }

    public static void writeJavaScript(PrintWriter out, RequestProperties reqState)
        throws IOException
    {
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("IconMenu.js"));
    }

    // ------------------------------------------------------------------------
    // write JavaScript

    public static void writeMenu(JspWriter out, RequestProperties reqState, String menuID, int maxIconsPerRow, boolean showIcon)
        throws IOException
    {
        IconMenu.writeMenu(new PrintWriter(out, out.isAutoFlush()), reqState, menuID, maxIconsPerRow, showIcon);
    }

    public static void writeMenu(PrintWriter out, RequestProperties reqState, String menuID, int maxIconsPerRow, boolean showIcon)
        throws IOException
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        Locale       locale    = reqState.getLocale();
        String  parentPageName = null;
        Account       account  = reqState.getCurrentAccount();

        /* sub style classes */
        String topMenuID         = "iconMenu";
        String menuTableClass    = "iconMenuTable";
        String groupTitleClass   = "menuGroupTitle";
        String groupIconsClass   = "menuGroupIcons";
        String menuItemTable     = "menuItemTable";
        String menuItemRow       = "menuItemRow";
        String menuItemImage_on  = "menuItemImage_on";
        String menuItemImage_off = "menuItemImage_off";
        String menuItemImage     = "menuItemImage";
        String menuItemText_on   = "menuItemText_on";
        String menuItemText_off  = "menuItemText_off";
        String menuItemText      = "menuItemText";
        String menuIconImage     = "menuIconImage";

        /* start menu */
        out.println("<table id='"+topMenuID+"' class='"+menuTableClass+"' cellpadding='0' cellspacing='0' border='0' width='100%'>");

        /* iterate through menu groups */
        Map<String,MenuGroup> menuMap = privLabel.getMenuGroupMap();
        for (String mgn : menuMap.keySet()) {
            MenuGroup mg = menuMap.get(mgn);
            if (!mg.showInTopMenu()) {
                continue; // skip this group
            }

            int rowIconCount = 0;
            boolean didDisplayGroup = false;
            for (WebPage wp : mg.getWebPageList(reqState)) {
                String menuName  = wp.getPageName();
                String iconImg   = showIcon? wp.getMenuIconImage() : null;
                String buttonImg = wp.getMenuButtonImage();
                String buttonAlt = wp.getMenuButtonAltImage();
                String url       = wp.encodePageURL(reqState);//, Track.BASE_URI());

                /* skip login page */
                if (menuName.equalsIgnoreCase(Constants.PAGE_LOGIN)) { 
                    //Print.logInfo("Skipping Login ...");
                    continue; // omit login
                }

                /* skip sysAdmin pages */
                if (wp.systemAdminOnly() && !Account.isSystemAdmin(account)) {
                    //Print.logInfo("Skipping SysAdmin item ...");
                    continue;
                }

                /* skip pages that are not ok to display */
                if (!wp.isOkToDisplay(reqState)) {
                    continue; 
                }

                /* start menu group */
                if (!didDisplayGroup) {
                    didDisplayGroup = true;
                    out.write("\n");
                    out.write("<!-- "+mg.getTitle(null)+" -->\n");
                    out.write("<tr class='"+groupTitleClass+"'><td class='"+groupTitleClass+"' width='100%'>"+mg.getTitle(locale)+"</td></tr>\n");
                    out.write("<tr class='"+groupIconsClass+"'><td class='"+groupIconsClass+"' width='100%'>\n");
                    out.write("<table class='"+menuItemTable+"' border='0'>\n"); //cellspacing='0' cellpadding='0' 
                    out.write("<tr class='"+menuItemRow+"'>\n");
                }

                /* wrap to next line? */
                if ((maxIconsPerRow > 0) && (rowIconCount >= maxIconsPerRow)) {
                    out.write("</tr>\n");
                    out.write("<tr class='"+menuItemRow+"'>\n");
                    rowIconCount = 0;
                }

                /* menu description */
                // replace all spaces with a newline "<BR>"
                String menuDesc = StringTools.trim(wp.getNavigationDescription(reqState)); // short
                menuDesc = StringTools.replace(menuDesc, " ", "<BR>");

                /* menu help */
                String menuHelp = StringTools.trim(wp.getMenuHelp(reqState, parentPageName));

                /* icon */
                String classOff = !StringTools.isBlank(buttonImg)? menuItemImage_off : menuItemText_off;
                String classOn  = !StringTools.isBlank(buttonImg)? menuItemImage_on  : menuItemText_on;
                String target   = StringTools.blankDefault(wp.getTarget(),"_self"); // (wp instanceof WebPageURL)? ((WebPageURL)wp).getTarget() : "_self";
                String onclick  = "javascript:openURL('"+url+"','"+target+"')";
                if (!target.startsWith("_")) {
                    PixelDimension pixDim = wp.getWindowDimension();
                    if (pixDim != null) {
                        int W = pixDim.getWidth();
                        int H = pixDim.getHeight();
                        onclick = "javascript:openFixedWindow('"+url+"','"+target+"',"+W+","+H+")";
                    }
                }
                out.write("<td class='"+classOff+"' title=\""+menuHelp+"\""+
                    " onmouseover=\"this.className='"+classOn+"'\""+
                    " onmouseout=\"this.className='"+classOff+"'\""+
                    " onclick=\""+onclick+"\""+
                    ">");
                if (StringTools.isBlank(buttonImg)) {
                    // draw text over background image
                    out.write("<span class='"+menuItemText+"'>"+menuDesc+"</span>");
                    if (!StringTools.isBlank(iconImg)) {
                        out.write("<br><img class='"+menuIconImage+"' border='0' src='"+iconImg+"'/>");
                    }
                } else {
                    // draw the image itself
                    out.write("<img class='"+menuItemImage+"' border='0' src='"+buttonImg+"'");
                    if (!StringTools.isBlank(buttonAlt)) {
                        out.write(" onmouseover=\"this.src='"+buttonAlt+"'\"");
                        out.write(" onmouseout=\"this.src='" +buttonImg+"'\"");
                    }
                    out.write("/>");
                }
                out.write("</td>\n");
                rowIconCount++;

            }
                
            /* end menu group */
            if (didDisplayGroup) {
                out.write("</tr>\n");
                out.write("</table>\n");
                out.write("</td></tr>\n");
                out.write("\n");
            }

        }
        
        /* end of menu */
        out.write("</table>\n");

    }

    // ------------------------------------------------------------------------

}
