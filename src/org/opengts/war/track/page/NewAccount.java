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
//  2007/01/25  Martin D. Flynn
//     -Initial release
//  2007/02/25  Martin D. Flynn
//     -Included in standard OpenGTS release
//  2007/05/06  Martin D. Flynn
//     -Added note about leaving the userID blank.
//  2007/06/03  Martin D. Flynn
//     -Added I18N support
//  2007/06/13  Martin D. Flynn
//     -Added support for browsers with disabled cookies
//  2007/07/27  Martin D. Flynn
//     -Added 'getNavigationTab(...)'
//  2007/12/13  Martin D. Flynn
//     -The current "<PrivateLabel>.getDomainName()" name is now used to set the 
//      temporary Account 'privateLabelName' field (previously it was left blank).
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class NewAccount
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------

    /* set this value to 'true' to always display the "New accounts are offline" message */
    public  static      boolean OFFLINE                 = false;

    // ------------------------------------------------------------------------
    
    public  static final String COMMAND_EMAIL_SUBMIT    = "e_submit";

    public  static final String PARM_EMAIL_SUBMIT       = "e_submit";

    public  static final String PARM_CONTACT_EMAIL      = "e_addr";
    public  static final String PARM_CONTACT_NAME       = "e_name";

    public  static final String CSS_NEW_ACCOUNT[]       = new String[] { "newAccountTable", "newAccountCell" };

    // ------------------------------------------------------------------------

    public NewAccount()
    {
        this.setBaseURI(Track.BASE_URI());
        this.setPageName(PAGE_ACCOUNT_NEW);
        this.setPageNavigation(new String[] { PAGE_LOGIN });
        this.setLoginRequired(false);
    }

    // ------------------------------------------------------------------------

    public String getMenuName(RequestProperties reqState)
    {
        return "";
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getMenuDescription(reqState,"");
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        //PrivateLabel privLabel = reqState.getPrivateLabel();
        //I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getMenuHelp(reqState,"");
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getNavigationDescription(reqState,i18n.getString("NewAccount.navDesc","New Account"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(NewAccount.class);
        return super._getNavigationTab(reqState,i18n.getString("NewAccount.navTab","New Account"));
    }

    // ------------------------------------------------------------------------
    
    private void offline(
        final RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(NewAccount.class);

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                WebPageAdaptor.writeCssLink(out, reqState, "NewAccount.css", null);
            }
        };

        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_NEW_ACCOUNT, pageMsg) {
            public void write(PrintWriter out) throws IOException {
                out.println("<span style='font-size:11pt'>"+i18n.getString("NewAccount.newTempAccountOffline","New Temporary Account")+"</span>");
                out.println("<hr>");
                out.println("<span style='font-size:9pt'>"+i18n.getString("NewAccount.offline1","New account registration is temporarily offline,")+"<br>"+i18n.getString("NewAccount.offline2","Please check back soon.")+"</span>");
                out.println("<hr>");
                String baseURL = Track.GetBaseURL(reqState); // EncodeMakeURL(reqState,Track.BASE_URI());
                out.println("<a href='"+baseURL+"'>Back</a>");
            }
        };

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            null,null,                      // onLoad/onUnload
            HTML_CSS,                       // Style sheets
            HTMLOutput.NOOP,                // JavaScript
            null,                           // Navigation
            HTML_CONTENT);                  // Content

    }

    // ------------------------------------------------------------------------

    private void newAccount(
        String contactEmail,
        String contactName,
        RequestProperties reqState, 
        final String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(NewAccount.class);
        
        Print.logInfo("EMail address submitted: '" + contactName + "' : " + contactEmail);
        Account account = null;
        Device device = null;
        try {
            
            /* already assigned? */
            String existingAcct[] = Account.getAccountsForContactEmail(contactEmail);
            if ((existingAcct != null) && (existingAcct.length > 0)) {
                Track.writeErrorResponse(reqState, i18n.getString("NewAccount.alreadyAccount","An account is already assigned to this Contact EMail Address."));
                return;
            }

            /* create account */
            String aliasName = privLabel.getDomainName();
            account = Account.createTemporaryAccount(contactName, contactEmail, aliasName);
            if (account == null) {
                // unable to assign an account within the 3 attempts
                this.offline(reqState, pageMsg);
                return;
            }
            
            /* create device */
            String deviceID = "mobile";
            device = Device.getDevice(account, deviceID, true);
            device.setIsActive(true);
            device.setDescription("Mobile Device");
            device.save();
            
        } catch (DBException dbe) {
            Print.logException("Creating Account", dbe);
            Track.writeErrorResponse(reqState, i18n.getString("NewAccount.accountError","Internal error occurred while creating account.  Try again later."));
            return;
        }

        /* send new account info */
        // Hello Mr User,
        //
        // Your new temporary account has been created.
        // Your access information is as follows:
        //    AccountID: T999999
        //    Password : vJcKFSbM
        //    DeviceID : mobile
        //
        // Please note that this is a temporary account to be used only for testing and
        // debug purposes.   This account is due to expire on 2006/06/28 08:50:15 GMT,
        // after which time the account and data will no longer be available.   Also note
        // that this free service may become unavailable from time to time and may be
        // discontinued at any time without advance notice.
        // 
        // You must login within the next 6 hours to confirm your new account registration.
        // You will then be able to change your password, and other account information.
        // 
        // Thank you.
        //
        String expd = reqState.formatDateTime(account.getExpirationTime());
        if (StringTools.isBlank(expd)) { expd = "n/a"; }

        String subj = i18n.getString("NewAccount.newAccount", "New Account");
        String body = i18n.getString("NewAccount.emailBody",
            "Hello {0},\n" +
            "\n" +
            "Your new temporary account has been created.\n" +
            "Your access information is as follows:\n" +
            "   AccountID: {1}\n" +
            "   UserID   : (leave blank)\n" +
            "   Password : {2}\n" +
            "   DeviceID : {3}\n" +
            "\n" +
            "Please note that this is a temporary account to be used only for testing and\n" +
            "debug purposes.   This account is due to expire on {4},\n" +
            "after which time the account and data will no longer be available.   Also note\n" +
            "that this free service may become unavailable from time to time and may be\n" +
            "discontinued at any time without advance notice.\n" +
            "\n" +
            "You must login within the next 6 hours to confirm your new account registration.\n" +
            "You will then be able to change your password, and other account information.\n" +
            "\n" +
            "Thank you.\n",
            new Object[] {contactName,account.getAccountID(),Account.decodePassword(account.getPassword()),device.getDeviceID(),expd});
        //Print.logInfo("EMail body:\n" + body);
        String from = privLabel.getEMailAddress(PrivateLabel.EMAIL_TYPE_ACCOUNTS);
        String to   = account.getContactEmail();
        if ((from != null) && !from.equals("") && (to != null) && !to.equals("")) {
            String cc   = null;
            String bcc  = null;
            EMail.send(from, to, cc, bcc, subj, body);
            Track.writeMessageResponse(reqState, 
                i18n.getString("NewAccount.emailSent","An email was sent to the specified email address with your new account information."));
        } else {
            Track.writeMessageResponse(reqState, 
                i18n.getString("NewAccount.emailError","Due to an internal error, we were unable to email your new account information."));
        }

    }
    
    // ------------------------------------------------------------------------
        
    public void writePage(
        final RequestProperties reqState, 
        String pageMsg)
        throws IOException
    {
        final PrivateLabel privLabel = reqState.getPrivateLabel();
        final I18N i18n = privLabel.getI18N(NewAccount.class);
        String m = pageMsg;
        
        /* offline */
        if (OFFLINE) {
            this.offline(reqState, pageMsg);
            return;
        }

        /* submitted? */
        String email = "";
        String name  = "";
        if (reqState.getCommandName().equals(COMMAND_EMAIL_SUBMIT)) {
            HttpServletRequest request = reqState.getHttpServletRequest();
            String submitSend = AttributeTools.getRequestString(request, PARM_EMAIL_SUBMIT, "");
            if (SubmitMatch(submitSend,i18n.getString("NewAccount.submit","Submit"))) {
                name  = AttributeTools.getRequestString(request, PARM_CONTACT_NAME ,"").trim();
                email = AttributeTools.getRequestString(request, PARM_CONTACT_EMAIL,"").trim();
                if (name.equals("")) {
                    m = i18n.getString("NewAccount.pleaseEnterName","Please enter a valid name");
                    name = "";
                } else
                if (email.equals("") || !EMail.validateAddress(email)) {
                    m = i18n.getString("NewAccount.pleaseEnterEMail","Please enter a valid email address");
                    email = "";
                } else {
                    this.newAccount(EMail.getEMailAddress(email), name, reqState, pageMsg);
                    return;
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = NewAccount.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "NewAccount.css", cssDir);
            }
        };

        /* write frame */
        final String cn  = name;
        final String ce  = email;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CSS_NEW_ACCOUNT, m) {
            public void write(PrintWriter out) throws IOException {
              //String menuURL    = EncodeMakeURL(reqState, Track.BASE_URI(), PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String emailURL   = NewAccount.this.encodePageURL(reqState, COMMAND_EMAIL_SUBMIT);
                String expireDate = reqState.formatDateTime(DateTime.getCurrentTimeSec() + Account.MAX_EXPIRATION_SEC);
                if (StringTools.isBlank(expireDate)) { expireDate = "n/a"; }
                out.println("<form name='AccountInfo' method='post' action='"+emailURL+"' target='_self'>"); // target='_top'
                out.println("  <span style='font-size:11pt'>"+i18n.getString("NewAccount.newTempAccount","New Temporary Account")+"</span>");
                out.println("  <hr>");
                out.println("  <span style='font-size:9pt'>");
                out.println(i18n.getString("NewAccount.instructions","Create a free temporary account to test your GPS tracking device.  Enter your email address below and we will create a new temporary account for you and send you details regarding how to login."));
                out.println("  </span>");
                out.println("  <hr>");
                out.println("  <span style='font-size:9pt'>"+i18n.getString("NewAccount.enterYourName","Enter Your Name:")+"</span><br>");
                out.println("  <input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_CONTACT_NAME +"' value='"+cn+"' maxlength='30' size='30'><br>");
                out.println("  <span style='font-size:9pt'>"+i18n.getString("NewAccount.enterYourEMail","Enter Your Email Address:")+"</span><br>");
                out.println("  <input class='"+CommonServlet.CSS_TEXT_INPUT+"' type='text' name='"+PARM_CONTACT_EMAIL+"' value='"+ce+"' maxlength='36' size='36'><br>");
                out.println("  <input type='submit' name='"+PARM_EMAIL_SUBMIT+"' value='"+i18n.getString("NewAccount.submit","Submit")+"'><br>");
                out.println("</form>");
                out.println("<hr>");
                out.println("<span style='font-size:8pt'>"+i18n.getString("NewAccount.willExpire","Temporary accounts are in fact temporary, and do have an expiry date.")+"<br>");
                out.println(i18n.getString("NewAccount.expireOnDate","Accounts created now will expire {0}",expireDate)+"</span>");
                out.println("<hr>");
                out.println("<a href='"+menuURL+"'>Back</a>");
            }
        };

        /* write frame */
        CommonServlet.writePageFrame(
            reqState,
            null,null,                  // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTMLOutput.NOOP,            // JavaScript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }
    
}
