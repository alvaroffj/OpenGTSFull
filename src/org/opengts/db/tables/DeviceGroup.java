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
//  2007/03/18  Martin D. Flynn
//     -Initial release
//  2007/06/13  Martin D. Flynn
//     -Moved to "org.opengts.db.tables"
//  2007/09/16  Martin D. Flynn
//     -Integrated DBSelect
//  2010/04/25  Martin D. Flynn
//     -Fix trimming of 'inactive' Devices
// ----------------------------------------------------------------------------
package org.opengts.db.tables;

import java.lang.*;
import java.util.*;
import java.math.*;
import java.io.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;

public class DeviceGroup
    extends GroupRecord<DeviceGroup>
{
    
    // ------------------------------------------------------------------------

    /* reserved name for the group containing "ALL" authorized devices */
    public static final String DEVICE_GROUP_ALL             = "all";
    public static final String DEVICE_GROUP_NONE            = "none";

    public static final OrderedSet<String> GROUP_LIST_ALL   = new OrderedSet<String>(new String[] { DEVICE_GROUP_ALL });
    public static final OrderedSet<String> GROUP_LIST_EMPTY = new OrderedSet<String>();

    // ------------------------------------------------------------------------

    /* "DeviceGroup" title (ie. "Group", "Fleet", etc) */
    public static String[] GetTitles(Locale loc) 
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return new String[] {
            i18n.getString("DeviceGroup.title.singular", "Group"),
            i18n.getString("DeviceGroup.title.plural"  , "Groups"),
        };
    }
    
    /* Group "All" description */
    public static String GetDeviceGroupAll(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return i18n.getString("DeviceGroup.allDescription", "All");
    }
        
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // SQL table definition below

    /* table name */
    public static final String _TABLE_NAME              = "DeviceGroup";
    public static String TABLE_NAME() { return DBProvider._translateTableName(_TABLE_NAME); }

    /* field definition */
    public static final String FLD_groupType            = "groupType";
    private static DBField FieldInfo[] = {
        // Group fields
        newField_accountID(true),
        newField_groupID(true),
      //new DBField(FLD_groupType       , Integer.TYPE  , DBField.TYPE_UINT16    , "Device Group Type", "edit=2"),
        // Home GeozoneID?
        // Common fields
        newField_displayName(),
        newField_description(),
        newField_notes(),
        newField_lastUpdateTime(),
        newField_creationTime(),
    };

    /* key class */
    public static class Key
        extends GroupKey<DeviceGroup>
    {
        public Key() {
            super();
        }
        public Key(String accountId, String groupId) {
            super.setFieldValue(FLD_accountID, ((accountId != null)? accountId.toLowerCase() : ""));
            super.setFieldValue(FLD_groupID  , ((groupId   != null)? groupId  .toLowerCase() : ""));
        }
        public DBFactory<DeviceGroup> getFactory() {
            return DeviceGroup.getFactory();
        }
    }
    
    /* factory constructor */
    private static DBFactory<DeviceGroup> factory = null;
    public static DBFactory<DeviceGroup> getFactory()
    {
        if (factory == null) {
            factory = DBFactory.createDBFactory(
                DeviceGroup.TABLE_NAME(), 
                DeviceGroup.FieldInfo, 
                DBFactory.KeyType.PRIMARY,
                DeviceGroup.class, 
                DeviceGroup.Key.class,
                true/*editable*/, true/*viewable*/);
            factory.addParentTable(Account.TABLE_NAME());
        }
        return factory;
    }

    /* Bean instance */
    public DeviceGroup()
    {
        super();
    }

    /* database record */
    public DeviceGroup(DeviceGroup.Key key)
    {
        super(key);
    }
    
    // ------------------------------------------------------------------------

    /* table description */
    public static String getTableDescription(Locale loc)
    {
        I18N i18n = I18N.getI18N(DeviceGroup.class, loc);
        return i18n.getString("DeviceGroup.description", 
            "This table defines " +
            "Account specific Device Groups."
            );
    }

    // SQL table definition above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Bean access fields below

    /* return the group type */
    public int getGroupType()
    {
        Integer v = (Integer)this.getFieldValue(FLD_groupType);
        return (v != null)? v.intValue() : 0;
    }

    /* set the group type */
    public void setGroupType(int v)
    {
        this.setFieldValue(FLD_groupType, v);
    }

    // ------------------------------------------------------------------------

    public void setMapLegend(String legend)
    {
        //
    }

    public String getMapLegend()
    {
        return "";
    }

    // Bean access fields above
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
 
    /* return string representation of instance */
    public String toString()
    {
        return this.getAccountID() + "/" + this.getGroupID();
    }
    
    // ------------------------------------------------------------------------

    /* overridden to set default values */
    public void setCreationDefaultValues()
    {
        this.setDescription("");
        //super.setRuntimeDefaultValues();
    }
    
    // ------------------------------------------------------------------------

    /* return true if the specified account/group/device exists */
    public boolean isDeviceInDeviceGroup(String deviceID)
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            return DeviceGroup.isDeviceInDeviceGroup(accountID, deviceID, groupID);
        } else {
            return false;
        }
    }

    /* return true if the specified account/group/device exists */
    public boolean isDeviceInDeviceGroup(Device device)
    {
        if (device != null) {
            return this.isDeviceInDeviceGroup(device.getDeviceID());
        } else {
            return false;
        }
    }
    
    /* add device to this group */
    public void addDeviceToDeviceGroup(String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, deviceID);
        }
    }

    /* add device to this group */
    public void addDeviceToDeviceGroup(Device device)
        throws DBException
    {
        if (device != null) {
            this.addDeviceToDeviceGroup(device.getDeviceID());
        }
    }
    
    /* remove device from this group */
    public void removeDeviceFromDeviceGroup(String deviceID)
        throws DBException
    {
        if (deviceID != null) {
            String accountID = this.getAccountID();
            String groupID   = this.getGroupID();
            DeviceGroup.removeDeviceFromDeviceGroup(accountID, deviceID, groupID);
        }
    }
    
    /* remove device from this group */
    public void removeDeviceFromDeviceGroup(Device device)
        throws DBException
    {
        if (device != null) {
            this.removeDeviceFromDeviceGroup(device.getDeviceID());
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* return true if specified account/group exists */
    public static boolean exists(String acctID, String groupID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID == null) || acctID.equals("")) {
            // invalid account
            return false;
        } else
        if ((groupID == null) || groupID.equals("")) {
            // invalid group
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            // 'all' always exists
            return true;
        } else {
            DeviceGroup.Key groupKey = new DeviceGroup.Key(acctID, groupID);
            return groupKey.exists();
        }
    }

    /* return true if specified account/group/device exists */
    public static boolean exists(String acctID, String groupID, String deviceID)
        throws DBException // if error occurs while testing existance
    {
        if ((acctID != null) && (groupID != null) && (deviceID != null)) {
            DeviceList.Key deviceListKey = new DeviceList.Key(acctID, groupID, deviceID);
            return deviceListKey.exists();
        }
        return false;
    }

    /* return true if the specified account/group/device exists */
    public static boolean isDeviceInDeviceGroup(String acctID, String groupID, String deviceID)
    {
        if ((acctID == null) || (groupID == null) || (deviceID == null)) {
            return false;
        } else
        if (groupID.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return true;
        } else {
            try {
                return DeviceGroup.exists(acctID, groupID, deviceID);
            } catch (DBException dbe) {
                return false;
            }
        }
    }

    // ------------------------------------------------------------------------

    /* add device to device group */
    public static void addDeviceToDeviceGroup(String accountID, String groupID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(accountID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }
        
        /* create/save record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        if (devListKey.exists()) {
            // already exists
        } else {
            DeviceList devListEntry = devListKey.getDBRecord();
            // not other data fields/columns required
            devListEntry.save();
        }

    }

    /* remove device from device group */
    public static void removeDeviceFromDeviceGroup(String accountID, String groupID, String deviceID)
        throws DBException
    {

        /* device exists? */
        if (!Device.exists(accountID,deviceID)) {
            throw new DBException("Device does not exist: " + accountID + "/" + deviceID);
        }

        /* delete record */
        DeviceList.Key devListKey = new DeviceList.Key(accountID, groupID, deviceID);
        devListKey.delete(false); // no dependencies
        
    }

    // ------------------------------------------------------------------------

    /* Return specified group, create if specified */
    public static DeviceGroup getDeviceGroup(Account account, String groupId)
        throws DBException
    {
        if (groupId == null) {
            return null;
        } else {
            return DeviceGroup.getDeviceGroup(account, groupId, false);
        }
    }

    /* Return specified group, create if specified */
    public static DeviceGroup getDeviceGroup(Account account, String groupId, boolean createOK)
        throws DBException
    {
        // does not return null, if 'createOK' is true

        /* account-id specified? */
        if (account == null) {
            throw new DBException("Account not specified.");
        }

        /* group-id specified? */
        if (StringTools.isBlank(groupId)) {
            throw new DBException("Device Group-ID not specified.");
        }

        /* get/create group */
        DeviceGroup.Key groupKey = new DeviceGroup.Key(account.getAccountID(), groupId);
        if (groupKey.exists()) { // may throw DBException
            DeviceGroup group = groupKey.getDBRecord(true);
            group.setAccount(account);
            return group;
        } else
        if (createOK) {
            DeviceGroup group = groupKey.getDBRecord();
            group.setAccount(account);
            group.setCreationDefaultValues();
            return group; // not yet saved!
        } else {
            // record doesn't exist, and caller doesn't want us to create it
            return null;
        }

    }

    /* create device group */
    public static DeviceGroup createNewDeviceGroup(Account account, String groupID)
        throws DBException
    {
        if ((account != null) && (groupID != null) && !groupID.equals("")) {
            DeviceGroup group = DeviceGroup.getDeviceGroup(account, groupID, true); // does not return null
            group.save();
            return group;
        } else {
            throw new DBException("Invalid Account/GroupID specified");
        }
    }

    // ------------------------------------------------------------------------

    /* return the DBSelect statement for the specified account/group */
    protected static DBSelect _getDeviceListSelect(String acctId, String groupId, long limit)
    {

        /* empty/null account */
        if (StringTools.isBlank(acctId)) {
            return null;
        }

        /* empty/null group */
        if (StringTools.isBlank(groupId)) {
            return null;
        }
        
        /* get select */
        // DBSelect: SELECT * FROM DeviceList WHERE ((accountID='acct') and (groupID='group')) ORDER BY deviceID
        DBSelect<DeviceList> dsel = new DBSelect<DeviceList>(DeviceList.getFactory());
        dsel.setSelectedFields(DeviceList.FLD_deviceID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(DeviceList.FLD_accountID,acctId ),
                    dwh.EQ(DeviceList.FLD_groupID  ,groupId)
                )
            )
        );
        dsel.setOrderByFields(DeviceList.FLD_deviceID);
        dsel.setLimit(limit);
        return dsel;

    }

    /* return the number of devices in this group */
    public long getDeviceCount()
    {
        
        /* get db selector */
        String acctId  = this.getAccountID();
        String groupId = this.getGroupID();
        DBSelect dsel = DeviceGroup._getDeviceListSelect(acctId, groupId, -1L);
        if (dsel == null) {
            return 0;
        }

        /* return count */
        try {
            //Print.logInfo("Retrieving count: " + dsel);
            return DBRecord.getRecordCount(DeviceList.getFactory(), dsel.getWhere());
        } catch (DBException dbe) {
            Print.logException("Unable to retrieve DeviceList count", dbe);
            return 0L;
        }
        
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public OrderedSet<String> getDevices(User userAuth, boolean inclInactv)
        throws DBException
    {
        String acctId  = this.getAccountID(); // TODO: matches "userAuth.getAccountID()"?
        String groupId = this.getGroupID();
        return DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static OrderedSet<String> getDeviceIDsForGroup(String acctId, String groupId, User userAuth, boolean inclInactv)
        throws DBException
    {
        return DeviceGroup.getDeviceIDsForGroup(acctId, groupId, userAuth, inclInactv, -1L);
    }

    /* return list of all Devices within the specified DeviceGroup (NOT SCALABLE BEYOND A FEW HUNDRED DEVICES) */
    public static OrderedSet<String> getDeviceIDsForGroup(String acctId, String groupId, User userAuth, boolean inclInactv, long limit)
        throws DBException
    {

        /* valid accountId/groupId? */
        if (StringTools.isBlank(acctId)) {
            return new OrderedSet<String>();
        } else
        if (StringTools.isBlank(groupId)) {
            return new OrderedSet<String>();
        }

        /* "All"? */
        if (groupId.equalsIgnoreCase(DeviceGroup.DEVICE_GROUP_ALL)) {
            return Device.getDeviceIDsForAccount(acctId, userAuth, inclInactv);
        }

        /* get db selector */
        DBSelect dsel = DeviceGroup._getDeviceListSelect(acctId, groupId, limit);
        if (dsel == null) {
            return new OrderedSet<String>();
        }

        /* read Account? */
        Account account = null;
        if (!inclInactv) {
            // We need the Account, to read the Devices, to determine if they are active/inactive
            // There is a chance that the User already has a handle to the Account
            account = (userAuth != null)? userAuth.getAccount() : Account.getAccount(acctId);
            if (account == null) {
                // account not found?
                Print.logWarn("Account not found? " + acctId);
                return new OrderedSet<String>();
            }
        }
        
        /* read devices for account */
        OrderedSet<String> devList = new OrderedSet<String>();
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(DeviceList.FLD_deviceID);
                // trim inactive?
                if (!inclInactv) {
                    Device device = (account != null)? Device.getDevice(account, devId) : null;
                    if ((device == null) || !device.isActive()) {
                        continue;
                    }
                }
                // trim unauthorized?
                if ((userAuth != null) && !userAuth.isAuthorizedDevice(devId)) {
                    continue;
                }
                // device ok
                devList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group DeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return devList;

    }

    // ------------------------------------------------------------------------

    /* return list of all DeviceGroups owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceGroupsForAccount(String acctId, boolean includeAll)
        throws DBException
    {

        /* select */
        // DBSelect: SELECT * FROM DeviceGroup WHERE (accountID='acct') ORDER BY groupID
        DBSelect<DeviceGroup> dsel = new DBSelect<DeviceGroup>(DeviceGroup.getFactory());
        dsel.setSelectedFields(DeviceGroup.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE(
                dwh.EQ(DeviceGroup.FLD_accountID,acctId)
            )
        );
        dsel.setOrderByFields(DeviceGroup.FLD_groupID);

        /* return list */
        return DeviceGroup.getDeviceGroups(dsel, includeAll);

    }

    /* return list of all DeviceGroups owned by the specified Account (NOT SCALABLE) */
    // does not return null
    public static OrderedSet<String> getDeviceGroups(DBSelect<DeviceGroup> dsel, boolean includeAll)
        throws DBException
    {

        /* group ID list, always add 'All' */
        OrderedSet<String> groupList = new OrderedSet<String>(true);
        
        /* include 'All'? */
        if (includeAll) {
            groupList.add(DeviceGroup.DEVICE_GROUP_ALL);
        }

        /* invalid account */
        if (dsel == null) {
            return groupList;
        }

        /* read device groups for account */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {

            /* get records */
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String groupId = rs.getString(DeviceGroup.FLD_groupID);
                //Print.logInfo("Adding DeviceGroup: " + groupId);
                groupList.add(groupId);
            }

        } catch (SQLException sqe) {
            throw new DBException("Getting Account DeviceGroup List", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return groupList;

    }

    // ------------------------------------------------------------------------

    /* return list of all DeviceGroups in which the specified device is a member */
    public static Collection<String> getDeviceGroupsForDevice(String acctId, String deviceId)
        throws DBException
    {

        /* valid Account/Device? */
        try {
            if ((acctId == null) || (deviceId == null) || !Device.exists(acctId,deviceId)) {
                return null;
            }
        } catch (DBException dbe) {
            // error attempting to text device existance
            return null;
        }

        /* group ids */
        java.util.List<String> groupList = new Vector<String>();
        groupList.add(DeviceGroup.DEVICE_GROUP_ALL);

        /* get select */
        // DBSelect: SELECT * FROM DeviceList WHERE ((accountID='acct') and (deviceID='dev')) ORDER BY groupID
        DBSelect<DeviceList> dsel = new DBSelect<DeviceList>(DeviceList.getFactory());
        dsel.setSelectedFields(DeviceList.FLD_groupID);
        DBWhere dwh = dsel.createDBWhere();
        dsel.setWhere(
            dwh.WHERE_(
                dwh.AND(
                    dwh.EQ(DeviceList.FLD_accountID,acctId  ),
                    dwh.EQ(DeviceList.FLD_deviceID ,deviceId)
                )
            )
        );
        dsel.setOrderByFields(DeviceList.FLD_groupID);

        /* read devices for account */
        DBConnection dbc = null;
        Statement   stmt = null;
        ResultSet     rs = null;
        try {
            dbc  = DBConnection.getDefaultConnection();
            stmt = dbc.execute(dsel.toString());
            rs   = stmt.getResultSet();
            while (rs.next()) {
                String devId = rs.getString(DeviceList.FLD_groupID);
                groupList.add(devId);
            }
        } catch (SQLException sqe) {
            throw new DBException("Get Group DeviceList", sqe);
        } finally {
            if (rs   != null) { try { rs.close();   } catch (Throwable t) {} }
            if (stmt != null) { try { stmt.close(); } catch (Throwable t) {} }
            DBConnection.release(dbc);
        }

        /* return list */
        return groupList;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_ACCOUNT[]   = new String[] { "account", "acct" };
    private static final String ARG_GROUP[]     = new String[] { "group"  , "grp"  };
    private static final String ARG_CREATE[]    = new String[] { "create" , "cr"   };
    private static final String ARG_EDIT[]      = new String[] { "edit"   , "ed"   };
    private static final String ARG_DELETE[]    = new String[] { "delete"          };
    private static final String ARG_ADD[]       = new String[] { "add"             };
    private static final String ARG_REMOVE[]    = new String[] { "remove"          };
    private static final String ARG_LIST[]      = new String[] { "list"            };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + DeviceGroup.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -account=<id>      Acount ID which owns DeviceGroup");
        Print.logInfo("  -group=<id>        Group ID to create/edit");
        Print.logInfo("  -create            Create a new DeviceGroup");
        Print.logInfo("  -edit              Edit an existing (or newly created) DeviceGroup");
        Print.logInfo("  -delete            Delete specified DeviceGroup");
        Print.logInfo("  -add=<deviceID>    Add deviceID to group");
        Print.logInfo("  -remove=<deviceID> Remove deviceID from group");
        Print.logInfo("  -list              List Devices in this Group");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        String groupID   = RTConfig.getString(ARG_GROUP  , "");
        boolean list     = RTConfig.getBoolean(ARG_LIST, false);

        /* option count */
        int opts = 0;

        /* account-id specified? */
        if ((accountID == null) || accountID.equals("")) {
            Print.logError("Account-ID not specified.");
            usage();
        }

        /* get account */
        Account acct = null;
        try {
            acct = Account.getAccount(accountID); // may throw DBException
            if (acct == null) {
                Print.logError("Account-ID does not exist: " + accountID);
                usage();
            }
        } catch (DBException dbe) {
            Print.logException("Error loading Account: " + accountID, dbe);
            //dbe.printException();
            System.exit(99);
        }

        /* group-id specified? */
        boolean hasGroupID = (groupID != null) && !groupID.equals("");

        /* group exists? */
        boolean groupExists = false;
        if (hasGroupID) {
            try {
                groupExists = DeviceGroup.exists(accountID, groupID);
            } catch (DBException dbe) {
                Print.logError("Error determining if DeviceGroup exists: " + accountID + "," + groupID);
                System.exit(99);
            }
        }

        /* delete */
        if (RTConfig.getBoolean(ARG_DELETE, false)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logWarn("DeviceGroup does not exist: " + accountID + "/" + groupID);
                Print.logWarn("Continuing with delete process ...");
            }
            try {
                DeviceGroup.Key groupKey = new DeviceGroup.Key(accountID, groupID);
                groupKey.delete(true); // also deletes dependencies
                Print.logInfo("DeviceGroup deleted: " + accountID + "/" + groupID);
            } catch (DBException dbe) {
                Print.logError("Error deleting DeviceGroup: " + accountID + "/" + groupID);
                dbe.printException();
                System.exit(99);
            }
            System.exit(0);
        }

        /* create */
        if (RTConfig.getBoolean(ARG_CREATE, false)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (groupExists) {
                Print.logWarn("DeviceGroup already exists: " + accountID + "/" + groupID);
            } else {
                try {
                    DeviceGroup.createNewDeviceGroup(acct, groupID);
                    Print.logInfo("Created DeviceGroup: " + accountID + "/" + groupID);
                } catch (DBException dbe) {
                    Print.logError("Error creating DeviceGroup: " + accountID + "/" + groupID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }

        /* edit */
        if (RTConfig.getBoolean(ARG_EDIT, false)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
            } else {
                try {
                    DeviceGroup group = DeviceGroup.getDeviceGroup(acct, groupID, false); // may throw DBException
                    DBEdit editor = new DBEdit(group);
                    editor.edit(); // may throw IOException
                } catch (IOException ioe) {
                    if (ioe instanceof EOFException) {
                        Print.logError("End of input");
                    } else {
                        Print.logError("IO Error");
                    }
                } catch (DBException dbe) {
                    Print.logError("Error editing DeviceGroup: " + accountID + "/" + groupID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* add */
        if (RTConfig.hasProperty(ARG_ADD)) {
            opts++;
            String deviceID = RTConfig.getString(ARG_ADD, "");
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
            } else {
                try {
                    DeviceGroup.addDeviceToDeviceGroup(accountID, groupID, deviceID);
                    Print.logInfo("DeviceList entry added: " + accountID + "/" + groupID + "/" + deviceID);
                } catch (DBException dbe) {
                    Print.logError("Error creating DeviceList entry: " + accountID + "/" + groupID + "/" + deviceID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* remove */
        if (RTConfig.hasProperty(ARG_REMOVE)) {
            opts++;
            if (!hasGroupID) {
                Print.logError("Group-ID not specified.");
                usage();
            } else
            if (!groupExists) {
                Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
            } else {
                String deviceID = RTConfig.getString(ARG_REMOVE, "");
                try {
                    DeviceGroup.removeDeviceFromDeviceGroup(accountID, groupID, deviceID);
                    Print.logInfo("DeviceList entry deleted: " + accountID + "/" + groupID + "/" + deviceID);
                } catch (DBException dbe) {
                    Print.logError("Error creating DeviceList entry: " + accountID + "/" + groupID + "/" + deviceID);
                    dbe.printException();
                    System.exit(99);
                }
            }
            System.exit(0);
        }

        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            opts++;
            if (hasGroupID) {
                if (!groupExists) {
                    Print.logError("DeviceGroup does not exist: " + accountID + "/" + groupID);
                } else {
                    Print.sysPrintln("");
                    Print.sysPrintln("DeviceGroup: " + accountID + "/" + groupID);
                    try {
                        OrderedSet<String> devList = DeviceGroup.getDeviceIDsForGroup(accountID, groupID, null, true/*inclInactv*/, -1L);
                        if (devList.size() <= 0) {
                            Print.sysPrintln("  No Devices");
                        } else {
                            for (int d = 0; d < devList.size(); d++) {
                                Print.sysPrintln("  Device: " + devList.get(d));
                            }
                        }
                    } catch (DBException dbe) {
                        Print.logError("Error listing Devices: " + accountID + "/" + groupID);
                        dbe.printException();
                        System.exit(99);
                    }
                }
            } else {
                try {
                    Print.sysPrintln("");
                    Print.logInfo("Account: " + accountID);
                    OrderedSet<String> groupList = DeviceGroup.getDeviceGroupsForAccount(accountID, true);
                    if (groupList.size() == 0) {
                        Print.sysPrintln("  No DeviceGroups");
                    } else {
                        for (String gid : groupList) {
                            Print.sysPrintln("  DeviceGroup: " + gid);
                        }
                    }
                } catch (DBException dbe) {
                    Print.logError("Error listing DeviceGroups: " + accountID);
                    dbe.printException();
                    System.exit(99);
                }
            }
        }
        

        /* no options specified */
        if (opts == 0) {
            Print.logWarn("Missing options ...");
            usage();
        }
        
    }
    
}
