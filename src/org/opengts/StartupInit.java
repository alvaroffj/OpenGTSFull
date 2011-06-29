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
//  2008/06/20  Martin D. Flynn
//     -Initial release
//  2009/05/24  Martin D. Flynn
//     -Changed "addDBFields" method 'alwaysAdd' to 'defaultAdd' to only add fields 
//      if not explicitly specified in the runtime conf file.
//  2010/09/09  Martin D. Flynn
//     -Added unit conversion for "getOptionalEventField".
// ----------------------------------------------------------------------------
package org.opengts;

import java.util.Locale;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.dbtypes.*;
import org.opengts.geocoder.*;

import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.db.dmtp.*;

/**
*** Provides startup initialization.<br>
*** This class is loaded by <code>DBConfig.java</code> at startup initialization time, and 
*** various methods are called within this class to allow custom DB initialization.<br>
*** The actual class loaded and executed by <code>DBConfig</code> can be overridden by placing 
*** the following line in the system 'default.conf' and 'webapp.conf' files:
*** <pre>
***   startup.initClass=org.opengts.StartupInit
*** </pre>
*** Where 'org.opengts.opt.StartupInit' is the name of the class you wish to have loaded in
*** place of this class file.
**/

public class StartupInit
    // standard/parent StartupInit class
    implements DBConfig.DBInitialization, DBFactory.CustomFactoryHandler
{

    // ------------------------------------------------------------------------
    
    private boolean     didInitRuleFactory      = false;
    private RuleFactory ruleFactoryInstance     = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor.<br>
    *** (Created with the DBConfig db startup initialization)
    **/
    public StartupInit()
    {
        super(); // <-- Object

        /* set a default "User-Agent" in the config file properties (if not already present) */
        RTProperties cfgFileProps = RTConfig.getConfigFileProperties();
        String userAgent = cfgFileProps.getString(RTKey.HTTP_USER_AGENT, null, false);
        if (StringTools.isBlank(userAgent)) {
            // no default "http.userAgent" defined in the config-file properties
            cfgFileProps.setString(RTKey.HTTP_USER_AGENT, "OpenGTS/" + org.opengts.Version.getVersion());
        }
        //Print.logInfo("HTTP User-Agent set to '%s'", HTMLTools.getHttpUserAgent());

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBConfig.DBInitialization interface

    /**
    *** Pre-DBInitialization.<br>
    *** This method is called just before the standard database factory classes are initialized/added.
    **/
    public void preInitialization()
    {
        if (RTConfig.isWebApp()) {
            OSTools.printMemoryUsage();
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Opportunity to add custom DBFactory classes.<br>
    *** This method is called just after all standard database factory classes have been intialized/added.
    *** Additional database factories that are needed for the custom installation may be added here.
    **/
    public void addTableFactories()
    {

        /* MUST add standard DBFactories */
        DBConfig.addTableFactories();

        /* add custom DBFactories here */
        //DBAdmin.addTableFactory("com.example.db.tables.MyCustomTable", true);

        /* add custom RuleFactory */
        // See "RuleFactoryExample.java" for more information
        if (!Device.hasRuleFactory()) {
            // To add the RuleFactoryExample module:
            //   Device.setRuleFactory(new RuleFactoryExample());
            // To add a different customized RuleFactory implementation:
            //   Device.setRuleFactory(new org.opengts.extra.rule.RuleFactoryLite());
            RuleFactory rf = this._getRuleFactoryInstance();
            if (rf != null) {
                Device.setRuleFactory(rf);
                Print.logInfo("RuleFactory installed: " + StringTools.className(rf));
            }
        }

        /* add custom map event data handler */
        EventUtil.OptionalEventFields optEvFlds = this.createOptionalEventFieldsHandler();
        EventUtil.setOptionalEventFieldHandler(optEvFlds);
        Print.logDebug("Installed OptionalEventFieldHandler: " + StringTools.className(optEvFlds));

    }
    
    private RuleFactory _getRuleFactoryInstance()
    {
        
        /* already initialized? */
        if (this.ruleFactoryInstance != null) {
            return this.ruleFactoryInstance;
        } else
        if (this.didInitRuleFactory) {
            return null;
        }
        this.didInitRuleFactory = true;

        /* get RuleFactory class */
        Class rfClass      = null;
        String rfClassName = RTConfig.getString("RuleFactory.class",null);
        try {
            String rfcName = !StringTools.isBlank(rfClassName)?
                rfClassName : 
                (DBConfig.PACKAGE_EXTRA_ + "rule.RuleFactoryLite");
            rfClass     = Class.forName(rfcName);
            rfClassName = rfcName;
        } catch (Throwable th) {
            if (!StringTools.isBlank(rfClassName)) {
                Print.logException("Unable to locate RuleFactory class: " + rfClassName, th);
            }
            return null;
        }

        /* instantiate RuleFactory */
        try {
            this.ruleFactoryInstance = (RuleFactory)rfClass.newInstance();
            return this.ruleFactoryInstance;
        } catch (Throwable th) {
            Print.logException("Unable to instantiate RuleFactory: " + rfClassName, th);
            return null;
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Creates a generic custom EventUtil.OptionalEventFields instance
    *** @return An EventUtil.OptionalEventFields instance
    **/
    protected EventUtil.OptionalEventFields createOptionalEventFieldsHandler()
    {

        /* Device fields */
        String devFieldNames[] = null;
        final DBField optDevFields[] = this.parseFields(Device.getFactory()   ,RTConfig.getStringArray("OptionalEventFields.Device"   ,devFieldNames));

        /* get Device/Event fields */
        String evtFieldNames[] = null;
        final DBField optEvtFields[] = this.parseFields(EventData.getFactory(),RTConfig.getStringArray("OptionalEventFields.EventData",evtFieldNames));

        /* return OptionalEventFields instance */
        return new EventUtil.OptionalEventFields() {

            // return number of 'optional' fields
            public int getOptionalEventFieldCount(boolean isFleet) {
                if (isFleet) {
                    // "Fleet" map
                    if (optDevFields != null) {
                        return optDevFields.length;
                    } else {
                        // Set this to '1' to display the Device 'linkURL' in the info balloon
                        return 0;
                    }
                } else {
                    // "Device" map
                    if (optEvtFields != null) {
                        return optEvtFields.length;
                    } else {
                        // Set this to '1' to display the Device digital input in info balloon
                        return 0;
                    }
                }
            }

            // return the title for a specific 'optional' field
            public String getOptionalEventFieldTitle(int ndx, boolean isFleet, Locale locale) {
                if (ndx < 0) { 
                    return ""; 
                } else
                if (isFleet) {
                    // "Fleet" map
                    if (optDevFields != null) {
                        if (ndx < optDevFields.length) {
                            return optDevFields[ndx].getTitle(locale);
                        }
                    } else {
                        I18N i18n = I18N.getI18N(StartupInit.class, locale);
                        switch (ndx) {
                            case 0: return i18n.getString("StartupInit.info.link", "Link");
                        }
                    }
                } else {
                    // "Device" map
                    if (optEvtFields != null) {
                        if (ndx < optEvtFields.length) {
                            return optEvtFields[ndx].getTitle(locale);
                        }
                    } else {
                        I18N i18n = I18N.getI18N(StartupInit.class, locale);
                        switch (ndx) {
                            case 0: return i18n.getString("StartupInit.info.digInput", "Digital Input");
                        }
                    }
                }
                return "";
            }

            // return the value for a specific 'optional' field
            public String getOptionalEventField(int ndx, boolean isFleet, EventDataProvider edp) {
                // The specified 'EventDataProvider' is likely the 'EventData' record.
                if (ndx < 0) { 
                    return ""; 
                } else
                if (isFleet) {
                    // "Fleet" map
                    if (optDevFields != null) {
                        // extract field value from Device record
                        if ((ndx < optDevFields.length) && (edp instanceof EventData)) {
                            Device device = ((EventData)edp).getDevice();
                            if (device != null) {
                                DBField fld = optDevFields[ndx];
                                Object  val = fld.getFieldValue(device);
                                Account account = ((EventData)edp).getAccount();
                                if (account != null) {
                                    val = account.convertFieldUnits(fld, val, true/*inclUnits*/, null/*TODO:Locale*/);
                                }
                                return StringTools.trim(val);
                            }
                        }
                    } else {
                        // return custom field value
                        switch (ndx) {
                            case 0: {
                                if (edp instanceof EventData) {
                                    Device device = ((EventData)edp).getDevice();
                                    if ((device != null) && device.hasLink()) {
                                        // NOTE! Enabling 'getLinkURL' and 'getLinkDescrption' requires that the
                                        // following property be specified in "common.conf" (or another .conf file):
                                        //   startupInit.Device.LinkFieldInfo=true
                                        // (also, "bin/dbAdmin.pl -tables=ca" must be run to add these Device columns)
                                        String url = device.getLinkURL();
                                        String dsc = device.getLinkDescription();
                                        if (StringTools.isBlank(dsc)) { dsc = "link"; }
                                        return "<a href='"+url+"' target='_blank'>"+dsc+"</a>";
                                    }
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // "Device" map
                    if (optEvtFields != null) {
                        // extract field value from EventData record
                        if ((ndx < optEvtFields.length) && (edp instanceof EventData)) {
                            DBField fld = optEvtFields[ndx];
                            Object  val = fld.getFieldValue((EventData)edp);
                            Account account = ((EventData)edp).getAccount();
                            if (account != null) {
                                val = account.convertFieldUnits(fld, val, true/*inclUnits*/, null/*TODO:Locale*/);
                            }
                            return StringTools.trim(val);
                        }
                    } else {
                        // return custom field value
                        switch (ndx) {
                            case 0: {
                                if (edp instanceof EventData) {
                                    EventData ed = (EventData)edp;
                                    return StringTools.toBinaryString(edp.getInputMask(),8,null).toString();
                                }
                                break;
                            }
                        }
                    }
                }
                return "";
            }

        };
        
    }
        
    protected DBField[] parseFields(DBFactory factory, String flda[])
    {
        if (factory == null) {
            return null;
        } else
        if (ListTools.isEmpty(flda)) {
            // no defined field names, return nothing
            return null;
        } else {
            return factory.getFields(flda); 
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Post-DBInitialization.<br>
    *** This method is called after all startup initialization has completed.
    **/
    public void postInitialization()
    {

        /* init StatusCode descriptions */
        StatusCodes.initStatusCodes(null); // include all status codes
        /* //The following specifies the list of specific status codes to include:
        StatusCodes.initStatusCodes(new int[] {
            StatusCodes.STATUS_LOCATION,
            StatusCodes.STATUS_MOTION_START,
            StatusCodes.STATUS_MOTION_IN_MOTION,
            StatusCodes.STATUS_MOTION_STOP,
            StatusCodes.STATUS_MOTION_DORMANT,
            ... include other StatusCodes here ...
        });
        */

        /* This sets the description for all accounts, all 'private.xml' domains, and all Localizations. */
        //StatusCodes.SetDescription(StatusCodes.STATUS_LOCATION      , "Marker");
        //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_START  , "Start Point");
        //StatusCodes.SetDescription(StatusCodes.STATUS_MOTION_STOP   , "Stop Point");
        
        /* Install custom PasswordHandler */
        String phClassName = RTConfig.getString("PasswordHandler.class",null);
        if (StringTools.isBlank(phClassName)) {
            // ignore
        } else
        if (phClassName.equalsIgnoreCase("md5")) {
            Account.setPasswordHandler(Account.MD5PasswordHandler);
        } else
        if (phClassName.equalsIgnoreCase("default")) {
            Account.setPasswordHandler(Account.DefaultPasswordHandler);
        } else {
            try {
                Class phClass = Class.forName(phClassName);
                PasswordHandler ph = (PasswordHandler)phClass.newInstance();
                Account.setPasswordHandler(ph);
            } catch (Throwable th) { // ClassCastException, ClassNotFoundException, ...
                Print.logException("Unable to instantiate PasswordHandler: " + phClassName, th);
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // DBFactory.CustomFactoryHandler interface

    /**
    *** Create a DBFactory instance.  The DBFactory initialization process will call this method
    *** when creating a DBFactory for a given table, allowing this class to override/customize
    *** any specific table attributes.  If this method returns null, the default table DBFactory
    *** will be created.
    *** @param tableName  The name of the table
    *** @param field      The DBFields in the table
    *** @param keyType    The table key type
    *** @param rcdClass   The DBRecord subclass representing the table
    *** @param keyClass   The DBRecordKey subclass representing the table key
    *** @param editable   True if this table should be editable, false otherwise.  
    ***                   This value is used by the GTSAdmin application.
    *** @param viewable   True if this table should be viewable, false otherwise.  
    ***                   An 'editable' table is automatically considered viewable.
    ***                   This value is used by the GTSAdmin application.
    *** @return The DBFactory instance (or null to indicate that the default DBFactory should be created).
    ***/
    public <T extends DBRecord<T>> DBFactory<T> createDBFactory(
        String tableName, 
        DBField field[], 
        DBFactory.KeyType keyType, 
        Class<T> rcdClass, 
        Class<? extends DBRecordKey<T>> keyClass, 
        boolean editable, boolean viewable)
    {
        //Print.logInfo("Intercept creation of DBFactory: %s", tableName);
        return null; // returning null indicates default behavior
    }

    /**
    *** Augment DBFactory fields.  This method is called before fields have been added to any
    *** given DBFactory.  This method may alter the list of DBFields by adding new fields, or 
    *** altering/deleting existing fields.  However, deleting/altering fields that have other
    *** significant systems dependencies may cause unpredictable behavior.
    *** @param factory  The DBFactory
    *** @param fields   The list of fields scheduled to be added to the DBFactory
    *** @return The list of fields which will be added to the DBFactory
    **/
    public java.util.List<DBField> selectFields(DBFactory factory, java.util.List<DBField> fields)
    {
        String tblName = factory.getTableName();
        // These additional fields can be enabled by placing the appropriate/specified 
        // property key=value in a 'custom.conf' file.

        /* Account */
        if (tblName.equalsIgnoreCase(Account.TABLE_NAME())) {
            // startupInit.Account.AddressFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Account.AddressFieldInfo"       , false, Account.AddressFieldInfo);
            // startupInit.Account.MapLegendFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Account.MapLegendFieldInfo"     , false, Account.MapLegendFieldInfo);
            return fields;
        }

        /* User */
        if (tblName.equalsIgnoreCase(User.TABLE_NAME())) {
            // startupInit.User.AddressFieldInfo=true
            addDBFields(tblName, fields, "startupInit.User.AddressFieldInfo"          , false, User.AddressFieldInfo);
            return fields;
        }

        /* Device */
        if (tblName.equalsIgnoreCase(Device.TABLE_NAME())) {
            // startupInit.Device.GeoCorridorFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Device.GeoCorridorFieldInfo"    , false, Device.GeoCorridorFieldInfo);
            // startupInit.Device.FixedLocationFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Device.FixedLocationFieldInfo"  , false, Device.FixedLocationFieldInfo);
            // startupInit.Device.LinkFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Device.LinkFieldInfo"           , false, Device.LinkFieldInfo);
            // startupInit.Device.BorderCrossingFieldInfo=true
            boolean devBC = Account.SupportsBorderCrossing();
            addDBFields(tblName, fields, "startupInit.Device.BorderCrossingFieldInfo" , devBC, Device.BorderCrossingFieldInfo);
            // startupInit.Device.MaintOdometerFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Device.MaintOdometerFieldInfo"  , false, Device.MaintOdometerFieldInfo);
            // startupInit.Device.NotificationFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Device.NotificationFieldInfo"   , false, Device.NotificationFieldInfo);
            // startupInit.Device.MiscFieldInfo=true
            addDBFields(tblName, fields, "startupInit.Device.MiscFieldInfo"           , false, Device.MiscFieldInfo);
            return fields;
        }

        /* EventData */
        if (tblName.equalsIgnoreCase(EventData.TABLE_NAME())) {
            // startupInit.EventData.AutoIncrementIndex=true
            addDBFields(tblName, fields, "startupInit.EventData.AutoIncrementIndex"   , false, EventData.AutoIncrementIndex);
            // startupInit.EventData.AddressFieldInfo=true
            addDBFields(tblName, fields, "startupInit.EventData.AddressFieldInfo"     , false, EventData.AddressFieldInfo);
            // startupInit.EventData.GPSFieldInfo=true
            addDBFields(tblName, fields, "startupInit.EventData.GPSFieldInfo"         , false, EventData.GPSFieldInfo);
            // startupInit.EventData.CustomFieldInfo=true
            addDBFields(tblName, fields, "startupInit.EventData.CustomFieldInfo"      , false, EventData.CustomFieldInfo);
            // startupInit.EventData.J1708FieldInfo=true
            addDBFields(tblName, fields, "startupInit.EventData.J1708FieldInfo"       , false, EventData.J1708FieldInfo);
            // startupInit.EventData.AtmosphereFieldInfo=true
            addDBFields(tblName, fields, "startupInit.EventData.AtmosphereFieldInfo"  , false, EventData.AtmosphereFieldInfo);
            // startupInit.EventData.ThermoFieldInfo=true
            addDBFields(tblName, fields, "startupInit.EventData.ThermoFieldInfo"      , false, EventData.ThermoFieldInfo, 4);
            // startupInit.EventData.EndOfDaySummary=true
            addDBFields(tblName, fields, "startupInit.EventData.EndOfDaySummary"      , false, EventData.EndOfDaySummary);
            // startupInit.EventData.ServingCellTowerData=true
            addDBFields(tblName, fields, "startupInit.EventData.ServingCellTowerData" , false, EventData.ServingCellTowerData);
            // startupInit.EventData.NeighborCellTowerData=true
            addDBFields(tblName, fields, "startupInit.EventData.NeighborCellTowerData", false, EventData.NeighborCellTowerData);
            // startupInit.EventData.WorkZoneGridData=true
            addDBFields(tblName, fields, "startupInit.EventData.WorkZoneGridData"     , false, EventData.WorkZoneGridData);
            return fields;
        }

        /* Geozone */
        if (tblName.equalsIgnoreCase(Geozone.TABLE_NAME())) {
            // startupInit.Geozone.PriorityFieldInfo
            addDBFields(tblName, fields, "startupInit.Geozone.PriorityFieldInfo"      , false, Geozone.PriorityFieldInfo);
            return fields;
        }

        /* leave as-is */
        return fields;

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Add the specified fields to the table
    *** @param tblName      The table name
    *** @param tblFields    The list of table fields
    *** @param key          The boolean key used to check for permission to add these fields
    *** @param defaultAdd   The default if the property is not explicitly specfified
    *** @param customFields The fields to add, assuming the boolean key returns true.
    **/
    protected void addDBFields(String tblName, java.util.List<DBField> tblFields, String key, boolean defaultAdd, DBField customFields[])
    {
        this.addDBFields(tblName, tblFields, key, defaultAdd, customFields, -1);
    }

    /**
    *** Add the specified fields to the table
    *** @param tblName      The table name
    *** @param tblFields    The list of table fields
    *** @param key          The boolean key used to check for permission to add these fields
    *** @param defaultAdd   The default if the property is not explicitly specfified
    *** @param customFields The fields to add, assuming the boolean key returns true.
    *** @param maxCount     The maximum number of fields to add from the customFields array
    **/
    protected void addDBFields(String tblName, java.util.List<DBField> tblFields, String key, boolean defaultAdd, DBField customFields[], int maxCount)
    {
        if (StringTools.isBlank(key) || RTConfig.getBoolean(key,defaultAdd)) {
            int cnt = ((maxCount >= 0) && (maxCount <= customFields.length))? maxCount : customFields.length;
            for (int i = 0; i < cnt; i++) {
                tblFields.add(customFields[i]);
            }
        }
    }

    // ------------------------------------------------------------------------

}
