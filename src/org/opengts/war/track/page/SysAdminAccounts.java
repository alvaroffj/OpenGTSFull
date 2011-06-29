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
//  2009/01/28  Martin D. Flynn
//     -Added "Logged-In" list column
//  2009/08/23  Martin D. Flynn
//     -Added ability to log-in to selected account (this feature controlled by
//      property "sysAdminAccounts.allowAccountLogin" - default is "false").
//     -Convert new entered IDs to lowercase
//  2009/09/23  Martin D. Flynn
//     -Added "TemporaryProperties" field.
//  2010/09/09  Martin D. Flynn
//     -Moved to "org.opengts.war.track.page"
// ----------------------------------------------------------------------------
package org.opengts.war.track.page;

import java.util.*;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.AclEntry.AccessLevel;
import org.opengts.db.tables.*;

import org.opengts.war.tools.*;
import org.opengts.war.track.*;

public class SysAdminAccounts
    extends WebPageAdaptor
    implements Constants
{

    // ------------------------------------------------------------------------
    // Parameters

    // forms 
    public  static final String FORM_ACCOUNT_SELECT             = "SysAdminSelect";
    public  static final String FORM_ACCOUNT_EDIT               = "SysAdminEdit";
    public  static final String FORM_ACCOUNT_NEW                = "SysAdminNew";

    // commands
    public  static final String COMMAND_INFO_UPDATE             = "update";
    public  static final String COMMAND_INFO_SELECT             = "select";
    public  static final String COMMAND_INFO_NEW                = "new";

    // submit
    public  static final String PARM_SUBMIT_EDIT                = "a_subedit";
    public  static final String PARM_SUBMIT_VIEW                = "a_subview";
    public  static final String PARM_SUBMIT_CHG                 = "a_subchg";
    public  static final String PARM_SUBMIT_DEL                 = "a_subdel";
    public  static final String PARM_SUBMIT_NEW                 = "a_subnew";
    public  static final String PARM_SUBMIT_LOGIN               = "a_sublogin";

    // buttons
    public  static final String PARM_BUTTON_CANCEL              = "d_btncan";
    public  static final String PARM_BUTTON_BACK                = "d_btnbak";

    // parameters
    public  static final String PARM_NEW_NAME                   = "s_newname";
    public  static final String PARM_ACCOUNT_SELECT             = "s_account";
    
    public  static final String PARM_ACCT_ID                    = "a_id";
    public  static final String PARM_ACCT_DESC                  = "a_desc";
    public  static final String PARM_ACCT_PASSWORD              = "a_pass";
    public  static final String PARM_ACCT_ACTIVE                = "a_active";
    public  static final String PARM_ACCT_PRIVLABEL             = "a_privlbl";
    public  static final String PARM_ACCT_EXPIRE                = "a_expire";
    public  static final String PARM_ACCT_TEMP_PROPS            = "a_tmpProps";

    // ------------------------------------------------------------------------

    public  static final String PROP_privateLabelAdminAccounts  = "privateLabelAdminAccounts";

    // ------------------------------------------------------------------------

    // password holder/indicator
    private static final String PASSWORD_HOLDER                 = "**********";
    private static final char   PASSWORD_INVALID_CHAR           = '*'; // password can't have all '*'

    // ------------------------------------------------------------------------
    // WebPage interface
    
    public SysAdminAccounts()
    {
        this.setBaseURI(Track.BASE_URI());
        this.setPageName(PAGE_SYSADMIN_ACCOUNTS);
        this.setPageNavigation(new String[] { PAGE_LOGIN, PAGE_MENU_TOP });
        this.setLoginRequired(true);
        //this.setCssDirectory("extra/css");
    }

    // ------------------------------------------------------------------------

    //public void setCssDirectory(String cssDir)
    //{
    //    super.setCssDirectory(cssDir);
    //    Print.logStackTrace("CSS Dir: " + cssDir);
    //}

    // ------------------------------------------------------------------------
   
    public String getMenuName(RequestProperties reqState)
    {
        return MenuBar.MENU_ADMIN;
    }

    public String getMenuDescription(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return super._getMenuDescription(reqState,i18n.getString("SysAdminAccounts.editMenuDesc","System Accounts"));
    }
   
    public String getMenuHelp(RequestProperties reqState, String parentMenuName)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return super._getMenuHelp(reqState,i18n.getString("SysAdminAccounts.editMenuHelp","Create/Delete/Edit/View System Accounts"));
    }

    // ------------------------------------------------------------------------

    public String getNavigationDescription(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return super._getNavigationDescription(reqState,i18n.getString("SysAdminAccounts.navDesc","System Accounts"));
    }

    public String getNavigationTab(RequestProperties reqState)
    {
        PrivateLabel privLabel = reqState.getPrivateLabel();
        I18N i18n = privLabel.getI18N(SysAdminAccounts.class);
        return i18n.getString("SysAdminAccounts.navTab","System Accounts");
    }

    // ------------------------------------------------------------------------

    /* true if this page iis for the system admin only */
    public boolean systemAdminOnly()
    {
        return true;
    }

    // ------------------------------------------------------------------------

    private static String filter(String s)
    {
        return StringTools.isBlank(s)? "&nbsp;" : StringTools.htmlFilterText(s);
    }
    
    private boolean isValidPassword(String pwd)
    {
        if (StringTools.isBlank(pwd)) {
            return true; // user is not allowed to log-in
        } else
        if (pwd.equals(PASSWORD_HOLDER)) {
            return false;
        } else {
            for (int i = 0; i < pwd.length(); i++) {
                if (pwd.charAt(i) != PASSWORD_INVALID_CHAR) {
                    return true;
                }
            }
            return false; // all '*'
        }
    }

    private Map<String,java.util.List<String>> getLoggedInAccounts(RequestProperties reqState)
    {
        final Map<String,java.util.List<String>> acctLoginMap = new HashMap<String,java.util.List<String>>();
        HttpSession session = AttributeTools.getSession(reqState.getHttpServletRequest());
        if (session != null) {
            int count = RTConfigContextListener.GetSessionCount(session.getServletContext(),
                new RTConfigContextListener.HttpSessionFilter() {
                    public boolean countSession(HttpSession session) {
                        String acctID = (String)AttributeTools.getSessionAttribute(session,Constants.PARM_ACCOUNT,null);
                        if (!StringTools.isBlank(acctID)) {
                            java.util.List<String> userList = acctLoginMap.get(acctID);
                            if (userList == null) {
                                userList = new Vector<String>();
                                acctLoginMap.put(acctID,userList);
                            }
                            String userID = (String)AttributeTools.getSessionAttribute(session,Constants.PARM_USER,null);
                            if (!StringTools.isBlank(userID)) {
                                userList.add(userID);
                            } else {
                                userID = "?";
                            }
                            Print.logInfo("Logged-in User: %s,%s", acctID, userID);
                            return true;
                        }
                        return false;
                    }
                }
            );
        }
        return acctLoginMap;
    }

    // ------------------------------------------------------------------------

    public void writePage(
        final RequestProperties reqState,
        String pageMsg)
        throws IOException
    {
        final HttpServletRequest request = reqState.getHttpServletRequest();
        final PrivateLabel privLabel = reqState.getPrivateLabel(); // never null
        final String   dtFormat      = privLabel.getDateFormat() + " " + privLabel.getTimeFormat();
        final I18N     i18n          = privLabel.getI18N(SysAdminAccounts.class);
        final Locale   locale        = reqState.getLocale();
        final Account  currAcct      = reqState.getCurrentAccount(); // never null
        final String   currAcctID    = reqState.getCurrentAccountID();
        final String   currAcctTZID  = currAcct.getTimeZone();
        final TimeZone currAcctTZ    = currAcct.getTimeZone(null);
        final boolean  isSysAdmin    = Account.isSystemAdmin(currAcct); // all access
        final String   pageName      = this.getPageName();
        final boolean  accountProps  = privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_accountProperties,false);
        String  m     = pageMsg;
        boolean error = false;

        /* "PrivateLabel Admin" handling */
        final String currPrivateLabel = currAcct.getPrivateLabelName();
        BasicPrivateLabel adminBPL = !isSysAdmin? BasicPrivateLabelLoader.getPrivateLabelForSysAdminAccountID(currAcctID) : null;
        final boolean isPrivateAdmin = (adminBPL != null) && adminBPL.getDomainName().equals(currPrivateLabel);
        final boolean isAdmin = isSysAdmin || isPrivateAdmin; // must be 'true', otherwise no access

        /* invalid user? */
        if (!isAdmin) {
            Print.logWarn("Current user is neither a SysAdmin, nor PrivateLabelAdmin! ==> " + currAcctID);
            // access will be restricted below
        }

        /* list of accounts */
        Collection<String> accountList = null;
        if (isAdmin) {
            try {
                accountList = Account.getAuthorizedAccounts(currAcct);
            } catch (DBException dbe) {
                Print.logError("Error reading authorized Accounts");
            }
        }
        if (accountList == null) {
            accountList = new Vector<String>();
            accountList.add(currAcctID);
        }
        // 'accountList' has at least one element in it.

        /* selected account-id */
        String selAccountID = AttributeTools.getRequestString(reqState.getHttpServletRequest(), PARM_ACCOUNT_SELECT, "");
        if (StringTools.isBlank(selAccountID)) {
            selAccountID = ListTools.itemAt(accountList, 0, "");
        }
        if (!isAdmin && !ListTools.contains(accountList,selAccountID)) {
            // not an admin account, and selected Account was not found in the list of allowed accounts
            selAccountID = currAcctID;
        }
        final boolean isCurrentAccountSelected = selAccountID.equals(currAcctID);

        /* account db */
        Account selAccount = null;
        try {
            selAccount = !StringTools.isBlank(selAccountID)? Account.getAccount(selAccountID) : null; // may still be null
        } catch (DBException dbe) {
            // ignore
        }

        /* command */
        String  accountCmd    = reqState.getCommandName();
        boolean listAccounts  = false;
        boolean updateAccount = accountCmd.equals(COMMAND_INFO_UPDATE);
        boolean selectAccount = accountCmd.equals(COMMAND_INFO_SELECT);
        boolean newAccount    = accountCmd.equals(COMMAND_INFO_NEW);
        boolean deleteAccount = false;
        boolean editAccount   = false;
        boolean viewAccount   = false;
        boolean loginAccount  = false;

        /* submit buttons */
        String  submitEdit    = AttributeTools.getRequestString(request, PARM_SUBMIT_EDIT , "");
        String  submitView    = AttributeTools.getRequestString(request, PARM_SUBMIT_VIEW , "");
        String  submitChange  = AttributeTools.getRequestString(request, PARM_SUBMIT_CHG  , "");
        String  submitNew     = AttributeTools.getRequestString(request, PARM_SUBMIT_NEW  , "");
        String  submitDelete  = AttributeTools.getRequestString(request, PARM_SUBMIT_DEL  , "");
        String  submitLogin   = AttributeTools.getRequestString(request, PARM_SUBMIT_LOGIN, "");

        /* CACHE_ACL: ACL allow edit/view */
        boolean allowNew     = isAdmin;
        boolean allowDelete  = allowNew;  // 'delete' allowed if 'new' allowed
        boolean allowEdit    = isAdmin;
        boolean allowView    = true;
        boolean allowLogin   = allowEdit && privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_allowAccountLogin,false);

        /* sub-command */
        String newAccountID = null;
        if (newAccount) {
            if (!allowNew) {
                newAccount = false; // not authorized
            } else {
                HttpServletRequest httpReq = reqState.getHttpServletRequest();
                newAccountID = AttributeTools.getRequestString(httpReq,PARM_NEW_NAME,"").trim();
                newAccountID = newAccountID.toLowerCase();
                if (StringTools.isBlank(newAccountID)) {
                    m = i18n.getString("SysAdminAccounts.enterNewAccount","Please enter a new Account name.");
                    error = true;
                    newAccount = false;
                } else
                if (!WebPageAdaptor.isValidID(reqState, PrivateLabel.PROP_SysAdminAccounts_validateNewIDs, newAccountID)) {
                    m = i18n.getString("SysAdminAccounts.invalidIDChar","ID contains invalid characters");
                    error = true;
                    newAccount = false;
                }
            }
        } else
        if (updateAccount) {
            if (!allowEdit) {
                // not authorized to update users
                updateAccount = false;
            } else
            if (!SubmitMatch(submitChange,i18n.getString("SysAdminAccounts.change","Change"))) {
                updateAccount = false;
            }
        } else
        if (selectAccount) {
            if (SubmitMatch(submitLogin,i18n.getString("SysAdminAccounts.login","Login"))) {
                if (allowLogin) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account");
                        error = true;
                        listAccounts = true;
                    } else
                    if (isCurrentAccountSelected) {
                        m = i18n.getString("SysAdminAccounts.alreadyLoggedInToAccount","Already Logged-In to this Account");
                        error = true;
                        listAccounts = true;
                    } else {
                        loginAccount = true;
                    }
                }
            } else
            if (SubmitMatch(submitDelete,i18n.getString("SysAdminAccounts.delete","Delete"))) {
                if (allowDelete) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account");
                        error = true;
                        listAccounts = true;
                    } else
                    if (isCurrentAccountSelected) {
                        m = i18n.getString("SysAdminAccounts.cannotDeleteCurrentAccount","Cannot delete current logged-in Account");
                        error = true;
                        listAccounts = true;
                    } else {
                        deleteAccount = true;
                    }
                }
            } else
            if (SubmitMatch(submitEdit,i18n.getString("SysAdminAccounts.edit","Edit"))) {
                if (allowEdit) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account");
                        error = true;
                        listAccounts = true;
                    } else {
                        editAccount = !isCurrentAccountSelected;
                        viewAccount = true;
                    }
                }
            } else
            if (SubmitMatch(submitView,i18n.getString("SysAdminAccounts.view","View"))) {
                if (allowView) {
                    if (selAccount == null) {
                        m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account");
                        error = true;
                        listAccounts = true;
                    } else {
                        viewAccount = true;
                    }
                }
            } else {
                listAccounts = true;
            }
        } else {
            listAccounts = true;
        }
        
        /* login to account? */
        if (loginAccount) {
            if (selAccount == null) {
                m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account");
                error = true;
            } else {
                try {
                    String loginAcctID = selAccount.getAccountID();
                    String loginUserID = User.getAdminUserID();
                    User   loginUser   = User.getUser(selAccount, loginUserID);
                    String loginPasswd = Account.decodePassword((loginUser != null)? loginUser.getPassword() : selAccount.getPassword());
                    String reloginKey  = privLabel.getStringProperty(PrivateLabel.PROP_SysAdminAccounts_reloginPasscode,"");
                    URIArg url = new URIArg(reqState.getBaseURI());
                    url.addArg(Constants.PARM_ACCOUNT , loginAcctID);
                    url.addArg(Constants.PARM_USER    , loginUserID);
                    url.addArg(Constants.PARM_PASSWORD, loginPasswd);
                    url.addArg(CommonServlet.PARM_PAGE, Constants.PAGE_MENU_TOP);
                    url.addArg(Constants.PARM_SYSADMIN_RELOGIN, reloginKey);
                    Print.logInfo("ReLogin URL: " + url);
                    AttributeTools.clearSessionAttributes(request); // invalidate/logout
                    HttpServletResponse response = reqState.getHttpServletResponse();
                    RequestDispatcher rd = request.getRequestDispatcher(url.toString());
                    rd.forward(request, response);
                    return;
                } catch (Throwable th) {
                    m = i18n.getString("SysAdminAccounts.errorDuringLoginDispatch","Error ocurred during dispatch to login");
                    error = true;
                }
            }
            listAccounts = true;
        }

        /* delete account? */
        if (deleteAccount) {
            if (selAccount == null) {
                m = i18n.getString("SysAdminAccounts.pleaseSelectAccount","Please select an Account");
                error = true;
            } else {
                try {
                    Account.Key accountKey = (Account.Key)selAccount.getRecordKey();
                    Print.logWarn("Deleting Account: " + accountKey);
                    accountKey.delete(true); // will also delete dependencies
                    accountList = Account.getAuthorizedAccounts(currAcct);
                    selAccountID = ListTools.itemAt(accountList, 0, "");
                    try {
                        selAccount = !selAccountID.equals("")? Account.getAccount(selAccountID) : null; // may still be null
                    } catch (DBException dbe) {
                        selAccount = null;
                    }
                } catch (DBException dbe) {
                    m = i18n.getString("SysAdminAccounts.errorDelete","Internal error deleting Account");
                    error = true;
                }
            }
            listAccounts = true;
        }

        /* new account? */
        if (newAccount) {
            boolean createAccountOK = true;
            try {
                if (Account.exists(newAccountID)) {
                    m = i18n.getString("SysAdminAccounts.alreadyExists","This Account already exists");
                    error = true;
                    createAccountOK = false;
                }
            } catch (DBException dbe) {
                m = i18n.getString("SysAdminAccounts.accountError","Error checking account");
                error = true;
                createAccountOK = false;
            }
            if (createAccountOK) {
                try {
                    String password = null;
                    String pLblName = isPrivateAdmin? currPrivateLabel : null;
                    Account account = Account.createNewAccount(newAccountID, password, pLblName); // already saved
                    accountList     = Account.getAuthorizedAccounts(currAcct);
                    selAccount      = account;
                    selAccountID    = account.getAccountID();
                    m = i18n.getString("SysAdminAccounts.createdAccount","New Account has been created");
                } catch (DBAlreadyExistsException dbaee) {
                    m = i18n.getString("SysAdminAccounts.alreadyExists","This Account already exists");
                    error = true;
                } catch (DBException dbe) {
                    m = i18n.getString("SysAdminAccounts.errorCreate","Internal error creating Account");
                    error = true;
                }
            }
            listAccounts = true;
        }

        /* change/update the account info? */
        if (updateAccount) {
            if (selAccount == null) {
                m = i18n.getString("SysAdminAccounts.noAccounts","There are currently no defined Accounts.");
            } else {
                String acctDesc      = AttributeTools.getRequestString(request, PARM_ACCT_DESC      , "");
                String acctActive    = AttributeTools.getRequestString(request, PARM_ACCT_ACTIVE    , "");
                String acctPassword  = AttributeTools.getRequestString(request, PARM_ACCT_PASSWORD  , "");
                String acctPrivLabel = AttributeTools.getRequestString(request, PARM_ACCT_PRIVLABEL , "<n/a>");
                User   adminUser     = null;
                listAccounts = true;
                // update
                try {
                    boolean saveOK = true;
                    // active
                    if (isCurrentAccountSelected) {
                        if (!selAccount.getIsActive()) {
                            selAccount.setIsActive(true);
                        }
                    } else {
                        boolean acctActv = ComboOption.parseYesNoText(locale, acctActive, true);
                        if (selAccount.getIsActive() != acctActv) { 
                            selAccount.setIsActive(acctActv); 
                        }
                    }
                    // password
                    if (!isCurrentAccountSelected) {
                        if (acctPassword.equals(PASSWORD_HOLDER)) {
                            // password not entered
                        } else
                        if (this.isValidPassword(acctPassword)) {
                            selAccount.setPassword(acctPassword);
                            try {
                                adminUser = User.getUser(selAccount, User.getAdminUserID());
                                if (adminUser != null) {
                                    adminUser.setPassword(acctPassword);
                                }
                            } catch (DBException dbe) {
                                // ignore
                            }
                        } else {
                            m = i18n.getString("SysAdminAccounts.pleaseEnterValidPassword","Please enter a valid password");
                            error = true;
                            saveOK = false;
                            editAccount  = true;
                            listAccounts = false;
                        }
                    }
                    // description
                    if (!acctDesc.equals("")) {
                        selAccount.setDescription(acctDesc);
                    }
                    // private label name
                    if (!acctPrivLabel.equals("<n/a>")) {
                        selAccount.setPrivateLabelName(acctPrivLabel);
                    }
                    // save
                    if (saveOK) {
                        if (adminUser != null) {
                            try {
                                adminUser.update(User.FLD_password);
                            } catch (DBException dbe) {
                                Print.logError("Error saving 'admin' User password", dbe);
                            }
                        }
                        selAccount.save();
                        if (accountProps) {
                            String acctTempProps = AttributeTools.getRequestString(request, PARM_ACCT_TEMP_PROPS, "");
                            try {
                                acctTempProps = (new RTProperties(acctTempProps.replace('\n',' '))).toString();
                                Resource resource = Resource.getResource(selAccount, Resource.RESID_TemporaryProperties);
                                if (StringTools.isBlank(acctTempProps)) {
                                    if ((resource != null) && !StringTools.isBlank(resource.getProperties())) {
                                        resource.setProperties("");
                                        resource.update(Resource.FLD_properties);
                                    } else {
                                        // no change
                                    }
                                } else {
                                    if (resource != null) {
                                        if (!acctTempProps.equals(resource.getProperties())) {
                                            resource.setProperties(acctTempProps);
                                            resource.update(Resource.FLD_properties);
                                        } else {
                                            // no change
                                        }
                                    } else {
                                        resource = Resource.getResource(selAccount, Resource.RESID_TemporaryProperties, true);
                                        resource.setType(Resource.TYPE_RTPROPS);
                                        resource.setProperties(acctTempProps);
                                        resource.save();
                                    }
                                }
                            } catch (DBException dbe) {
                                Print.logException("Unable to save Resource: " + selAccount.getAccountID(), dbe);
                            }
                        } // accountProps
                        m = i18n.getString("SysAdminAccounts.accountUpdated","Account information updated");
                    } else {
                        // should stay on this page
                        editAccount  = !isCurrentAccountSelected;
                        listAccounts = false;
                    }
                } catch (Throwable t) {
                    m = i18n.getString("SysAdminAccounts.errorUpdate","Internal error updating Account");
                    error = true;
                }
            }
        }

        /* Style */
        HTMLOutput HTML_CSS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                String cssDir = SysAdminAccounts.this.getCssDirectory();
                WebPageAdaptor.writeCssLink(out, reqState, "SysAdminAccounts.css", cssDir);
            }
        };

        /* JavaScript */
        HTMLOutput HTML_JS = new HTMLOutput() {
            public void write(PrintWriter out) throws IOException {
                MenuBar.writeJavaScript(out, pageName, reqState);
                JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef(SORTTABLE_JS));
            }
        };

        /* Content */
        final Collection<String> _accountList = accountList;
        final String  _selAccountID  = selAccountID;
        final Account _selAccount    = selAccount;
        final boolean _allowEdit     = allowEdit;
        final boolean _allowView     = allowView;
        final boolean _allowNew      = allowNew;
        final boolean _allowDelete   = allowDelete;
        final boolean _allowLogin    = allowLogin;
        final boolean _editAccount   = _allowEdit && editAccount;
        final boolean _viewAccount   = _editAccount || viewAccount;
        final boolean _listAccounts  = listAccounts;
        HTMLOutput HTML_CONTENT = new HTMLOutput(CommonServlet.CSS_CONTENT_FRAME, m) {
            public void write(PrintWriter out) throws IOException {
                String pageName = SysAdminAccounts.this.getPageName();

                // frame header
              //String menuURL    = EncodeMakeURL(reqState,Track.BASE_URI(),PAGE_MENU_TOP);
                String menuURL    = privLabel.getWebPageURL(reqState, PAGE_MENU_TOP);
                String editURL    = SysAdminAccounts.this.encodePageURL(reqState);//,Track.BASE_URI());
                String selectURL  = SysAdminAccounts.this.encodePageURL(reqState);//,Track.BASE_URI());
                String newURL     = SysAdminAccounts.this.encodePageURL(reqState);//,Track.BASE_URI());
                String frameTitle = _allowNew? 
                    i18n.getString("SysAdminAccounts.createDeleteAccounts","Create/Delete/Edit Accounts") : 
                    i18n.getString("SysAdminAccounts.viewEditAccounts","View/Edit Accounts");
                out.write("<span class='"+CommonServlet.CSS_MENU_TITLE+"'>"+frameTitle+"</span><br/>\n");
                out.write("<hr>\n");

                // account selection table (Select, Account ID, Account Description)
                if (_listAccounts) {

                    // account selection table (Select, Account ID, Account Description)
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("SysAdminAccounts.selectAccount","Select an Account")+":</h1>\n");
                    out.write("<div style='margin-left:25px;'>\n");
                    out.write("<form name='"+FORM_ACCOUNT_SELECT+"' method='post' action='"+selectURL+"' target='_top'>");
                    out.write("<input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_SELECT+"'/>");
                    out.write("<table class='"+CommonServlet.CSS_ADMIN_SELECT_TABLE+"' cellspacing=0 cellpadding=0 border=0>\n");
                    out.write(" <thead>\n");
                    out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_ROW+"'>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL_SEL+"' nowrap>"+filter(i18n.getString("SysAdminAccounts.select","Select"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.accountID","Account ID"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.accountName","Account Description"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.active","Active"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.deviceCount","Device\nCount"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.privateLabel","PrivateLabel\nName"))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.creationDate","Created\n{0}",currAcctTZID))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.lastLogin","Last Login\n{0}",currAcctTZID))+"</th>\n");
                    out.write("   <th class='"+CommonServlet.CSS_ADMIN_TABLE_HEADER_COL    +"' nowrap>"+filter(i18n.getString("SysAdminAccounts.loggedIn","Logged\nIn Now"))+"</th>\n");
                    out.write("  </tr>\n");
                    out.write(" </thead>\n");
                    out.write(" <tbody>\n");
                    Map<String,java.util.List<String>> loggedInAccounts = SysAdminAccounts.this.getLoggedInAccounts(reqState);
                    for (int u = 0; u < ListTools.size(_accountList); u++) {
                        // get Account
                        Account acct = null;
                        try {
                            acct = Account.getAccount(ListTools.itemAt(_accountList,u,""));
                        } catch (DBException dbe) {
                            // 
                        }
                        if (acct == null) {
                            continue;
                        }
                        String acctID       = acct.getAccountID();
                        String acctDesc     = acct.getDescription();
                        String prvLabelName = acct.getPrivateLabelName();
                        //if (!prvLabelName.equals("*")) { continue; } // <-- debug/testing
                        // odd/even row
                        boolean oddRow = ((u & 1) == 0); // odd row index starts at '0'
                        if (oddRow) {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_ODD+"'>\n");
                        } else {
                            out.write("  <tr class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_ROW_EVEN+"'>\n");
                        }
                        // display account info
                        String active       = ComboOption.getYesNoText(locale,acct.isActive());
                        long creationTime   = acct.getCreationTime();
                        String creationStr  = (creationTime > 0L)? new DateTime(creationTime,currAcctTZ).format(dtFormat) : i18n.getString("SysAdminAccounts.unknown","unknown");
                        long lastLoginTime  = acct.getLastLoginTime();
                        long deltaTimeSec   = DateTime.getCurrentTimeSec() - lastLoginTime;
                        String lastLoginStr = (lastLoginTime > 0L)? new DateTime(lastLoginTime,currAcctTZ).format(dtFormat) : i18n.getString("SysAdminAccounts.never","never");
                        String lastLoginCls = oddRow? "normalLoginDate_odd" : "normalLoginDate_even";
                        if (deltaTimeSec <= DateTime.DaySeconds(1)) {
                            // has logged i within the last 24 hours (green)
                            lastLoginCls = oddRow? "recentLoginDate_odd" : "recentLoginDate_even";
                        } else
                        if (deltaTimeSec <= DateTime.DaySeconds(7)) {
                            // has logged i within the last week (black)
                            lastLoginCls = oddRow? "normalLoginDate_odd" : "normalLoginDate_even";
                        } else
                        if (deltaTimeSec <= DateTime.DaySeconds(21)) {
                            // has logged i within the last 3 weeks (yellow)
                            lastLoginCls = oddRow? "oldLoginDate_odd" : "oldLoginDate_even";
                        } else {
                            // logged in more than 3 weeks ago (red)
                            lastLoginCls = oddRow? "veryOldLoginDate_odd" : "veryOldLoginDate_even"; // (196, 54, 54)
                        }
                        String deviceCountS = String.valueOf(acct.getDeviceCount());
                        int    loginCount   = 0;
                        String loginCountS  = "--"; // ComboOption.getYesNoText(locale,false);
                        if (loggedInAccounts.containsKey(acctID)) {
                            java.util.List<String> userList = loggedInAccounts.get(acctID);
                            loginCount  = userList.size();
                            loginCountS = "(" + loginCount + ")";
                        }
                        //if (prvLabelName.equals("*")) { prvLabelName = "default"; }
                        String checked      = _selAccountID.equals(acctID)? " checked" : "";
                        String viewStyle    = currAcctID.equals(acctID)? "background-color:#E5E5E5;" : "background-color:#FFFFFF;";
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL_SEL+"' "+SORTTABLE_SORTKEY+"='"+u+"' style='"+viewStyle+"'><input type='radio' name='"+PARM_ACCOUNT_SELECT+"' id='"+acctID+"' value='"+acctID+"' "+checked+"></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap><label for='"+acctID+"'>"+filter(acctID)+"</label></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(acctDesc)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(active)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(deviceCountS)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' nowrap>"+filter(prvLabelName)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+creationTime +"' nowrap>"+filter(creationStr)+"</td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+lastLoginTime+"' nowrap><span class='"+lastLoginCls+"'>"+filter(lastLoginStr)+"</span></td>\n");
                        out.write("   <td class='"+CommonServlet.CSS_ADMIN_TABLE_BODY_COL    +"' "+SORTTABLE_SORTKEY+"='"+loginCount   +"' nowrap>"+filter(loginCountS)+"</td>\n");
                        // end of table row
                        out.write("  </tr>\n");
                    }
                    out.write(" </tbody>\n");
                    out.write("</table>\n");
                    out.write("<table cellpadding='0' cellspacing='0' border='0' style='width:95%; margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<tr>\n");
                    if (_allowView) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_VIEW+"' value='"+i18n.getString("SysAdminAccounts.view","View")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowEdit) { 
                        out.write("<td style='padding-left:5px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_EDIT+"' value='"+i18n.getString("SysAdminAccounts.edit","Edit")+"'>");
                        out.write("</td>\n"); 
                    }
                    if (_allowLogin) {
                        out.write("<td style='padding-left:30px;'>");
                        out.write("<input type='submit' name='"+PARM_SUBMIT_LOGIN+"' value='"+i18n.getString("SysAdminAccounts.login","Login")+"' "+Onclick_ConfirmLogin(locale)+">");
                        out.write("</td>\n"); 
                    }
                    out.write("<td style='width:100%; text-align:right; padding-right:10px;'>");
                    if (_allowDelete) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_DEL+"' value='"+i18n.getString("SysAdminAccounts.delete","Delete")+"' "+Onclick_ConfirmDelete(locale)+">");
                    } else {
                        out.write("&nbsp;"); 
                    }
                    out.write("</td>\n"); 
                    out.write("</tr>\n");
                    out.write("</table>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");

                    /* new Account */
                    if (_allowNew) {
                    out.write("<h1 class='"+CommonServlet.CSS_ADMIN_SELECT_TITLE+"'>"+i18n.getString("SysAdminAccounts.createNewAccount","Create a new Account")+":</h1>\n");
                    out.write("<div style='margin-top:5px; margin-left:5px; margin-bottom:5px;'>\n");
                    out.write("<form name='"+FORM_ACCOUNT_NEW+"' method='post' action='"+newURL+"' target='_top'>");
                    out.write(" <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_NEW+"'/>");
                    out.write(i18n.getString("SysAdminAccounts.accountID","Account ID")+": <input type='text' name='"+PARM_NEW_NAME+"' value='' size='32' maxlength='32'><br>\n");
                    out.write(" <input type='submit' name='"+PARM_SUBMIT_NEW+"' value='"+i18n.getString("SysAdminAccounts.new","New")+"' style='margin-top:5px; margin-left:10px;'>\n");
                    out.write("</form>\n");
                    out.write("</div>\n");
                    out.write("<hr>\n");
                    }

                } else {
                    // user view/edit form

                    /* start of form */
                    out.write("<form name='"+FORM_ACCOUNT_EDIT+"' method='post' action='"+editURL+"' target='_top'>\n");
                    out.write("  <input type='hidden' name='"+PARM_COMMAND+"' value='"+COMMAND_INFO_UPDATE+"'/>\n");

                    /* password */
                    String password = PASSWORD_HOLDER;
                    boolean showPass = privLabel.getBooleanProperty(PrivateLabel.PROP_SysAdminAccounts_showPasswords,false);
                    if (showPass && (_selAccount != null)) {
                        try {
                            User adminUser = User.getUser(_selAccount, User.getAdminUserID());
                            if (adminUser != null) {
                                password = Account.decodePassword(adminUser.getPassword());
                            } else {
                                password = Account.decodePassword(_selAccount.getPassword());
                            }
                        } catch (DBException dbe) {
                            password = Account.decodePassword(_selAccount.getPassword());
                        }
                    }
                    password = StringTools.htmlFilterValue(password);

                    /* Account fields */
                    ComboOption acctActive = ComboOption.getYesNoOption(locale, ((_selAccount != null) && _selAccount.isActive()));
                    String acctDesc      = (_selAccount!=null)?_selAccount.getDescription() :"";
                    String acctPrivLbl   = (_selAccount!=null)?_selAccount.getPrivateLabelName() : "";
                    boolean editPrvLabel = _editAccount && isSysAdmin; // only editable if sys-admin
                    ComboMap privLblList = null;
                    if (editPrvLabel) {
                        // editable ComboMap (isPrivateAdmin vs. isSysAdmin)
                        privLblList = isSysAdmin? 
                            new ComboMap(BasicPrivateLabelLoader.getPrivateLabelNames(true)) :
                            new ComboMap();
                        if (!ListTools.containsKey(privLblList, acctPrivLbl)) {
                            privLblList.insert(acctPrivLbl);
                        }
                        if (isSysAdmin) {
                            if (!ListTools.containsKey(privLblList, "")) {
                                privLblList.insert("");
                            }
                            if (!ListTools.containsKey(privLblList, "*")) {
                                privLblList.insert("*");
                            }
                        }
                    } else {
                        // non-editable ComboMap
                        privLblList = new ComboMap();
                        privLblList.insert(acctPrivLbl);
                    }
                    out.println("<table class='"+CommonServlet.CSS_ADMIN_VIEW_TABLE+"' cellspacing='0' callpadding='0' border='0'>");
                    out.println(FormRow_TextField(PARM_ACCOUNT_SELECT , false        , i18n.getString("SysAdminAccounts.accountID","Account ID")+":"                 , _selAccountID, 40, 40));
                    out.println(FormRow_TextField(PARM_ACCT_DESC      , _editAccount , i18n.getString("SysAdminAccounts.accountDesc","Account Description")+":"      , acctDesc, 40, 40));
                    out.println(FormRow_ComboBox (PARM_ACCT_ACTIVE    , _editAccount , i18n.getString("SysAdminAccounts.active","Active")+":"                        , acctActive, ComboMap.getYesNoMap(locale), "", -1));
                    out.println(FormRow_TextField(PARM_ACCT_PASSWORD  , _editAccount , i18n.getString("SysAdminAccounts.password","Password")+":"                    , password, 20, 20));
                    out.println(FormRow_ComboBox (PARM_ACCT_PRIVLABEL , editPrvLabel , i18n.getString("SysAdminAccounts.privateLabelName","PrivateLabel Name")+":"   , acctPrivLbl, privLblList, "", -1));
                    if (accountProps) {
                        try {
                            String acctTempProps = "";
                            Resource resource = (_selAccount != null)? Resource.getResource(_selAccount, Resource.RESID_TemporaryProperties) : null;
                            if (resource != null) {
                                RTProperties resRtp = resource.getRTProperties();
                                acctTempProps = resRtp.toString(null, null, "");
                            }
                            out.println(FormRow_TextArea(PARM_ACCT_TEMP_PROPS, _editAccount , i18n.getString("SysAdminAccounts.accountProperties" ,"Account Properties")+":", acctTempProps, 7, 75));
                        } catch (DBException dbe) {
                            Print.logError("Unable to read Account Resource: " + dbe);
                        }
                    }
                    out.println("</table>");

                    /* end of form */
                    out.write("<hr>\n");
                    out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                    if (_editAccount) {
                        out.write("<input type='submit' name='"+PARM_SUBMIT_CHG+"' value='"+i18n.getString("SysAdminAccounts.change","Change")+"'>\n");
                        out.write("<span style='padding-left:10px'>&nbsp;</span>\n");
                        out.write("<input type='button' name='"+PARM_BUTTON_CANCEL+"' value='"+i18n.getString("SysAdminAccounts.cancel","Cancel")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    } else {
                        out.write("<input type='button' name='"+PARM_BUTTON_BACK+"' value='"+i18n.getString("SysAdminAccounts.back","Back")+"' onclick=\"javascript:openURL('"+editURL+"','_top');\">\n");
                    }
                    out.write("</form>\n");

                }

            }
        };

        /* write frame */
        String onload = error? JS_alert(true,m) : null;
        CommonServlet.writePageFrame(
            reqState,
            onload,null,                // onLoad/onUnload
            HTML_CSS,                   // Style sheets
            HTML_JS,                    // Javascript
            null,                       // Navigation
            HTML_CONTENT);              // Content

    }

    protected String Onclick_ConfirmLogin(Locale locale)
    {
        I18N i18n = I18N.getI18N(SysAdminAccounts.class, locale);
        String confirmLogin = i18n.getString("SysAdminAccounts.confirmLogin",
            "Are you sure you want to login to the selected Account?");
        return "onclick=\"return confirm('"+confirmLogin+"');\"";
    }

    // ------------------------------------------------------------------------
}
