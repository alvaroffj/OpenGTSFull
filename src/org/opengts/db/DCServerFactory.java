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
// Description:
//  Device Communication Server configuration factory
// ----------------------------------------------------------------------------
// Change History:
//  2009/09/23  Martin D. Flynn
//     -Initial release
//  2009/11/01  Martin D. Flynn
//     - Discard duplicate server names (first entry encountered is saved)
//  2010/01/29  Martin D. Flynn
//     - "DCServer" and "Include" tags are now processed sequentially
//     - Added ability to query RTKey.MAIN_CLASS for a specific DCServer to load.
//  2010/05/24  Martin D. Flynn
//     -Added check for memory usage ("checkMemoryUsage")
//  2010/06/17  Martin D. Flynn
//     -Added "getServerConfigDescription"
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;

import org.opengts.db.tables.*;

public class DCServerFactory
{

    // ------------------------------------------------------------------------

    public  static final String  DCSERVERS_DIR                  = "dcservers";
    public  static final String  DCSERVER_XML                   = "dcservers.xml";
    
    private static       File    LoadedDCServerXMLFile          = null;

    // ------------------------------------------------------------------------
    
    public  static       boolean DEFAULT_WARN_PORT_CONFLICT     = true;

    // ------------------------------------------------------------------------
    // Attribute properties
    
    public  static final String  PROP_Attribute_                = "Attribute.";
    public  static final String  PROP_Attribute_InputOffset     = PROP_Attribute_ + "inputOffset";

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Command types:
    
    public  static final String CMDTYPE_ALL                     = "all";
    public  static final String CMDTYPE_MAP                     = "map";
    public  static final String CMDTYPE_ADMIN                   = "admin";
    public  static final String CMDTYPE_GARMIN                  = "garmin";
    public  static final String CMDTYPE_SYSADMIN                = "sysadmin";
    
    public static boolean isCommandTypeAll(String type)
    {
        if (StringTools.isBlank(type)) {
            return true;
        } else {
            return type.equalsIgnoreCase(CMDTYPE_ALL);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Registry of Device Communication Server IDs 
    // Device communication servers listed here must also have a corresponding
    // "<server>.jar" file in the OpenGTS "build/lib/" directory in order to
    // be supported.  If this "<server>.jar" file does not exist, then the
    // corresponding device communication server port entry is not used.

    public static final String  NONE_NAME                       = "none";           // reserved for "No Server"
    
    /* OpenGTS */
    public static final String  OPENDMTP_NAME                   = "gtsdmtp";        // [31000,3x100,3x200] OpenDMTP
    public static final String  TEMPLATE_NAME                   = "template";       // [31200]
    public static final String  GP6000_NAME                     = "GP6000";       // [31200]
    public static final String  ICARE_NAME                      = "icare";          // [31260] GX3300
    public static final String  ASPICORE_NAME                   = "aspicore";       // [31265] 3x265
    public static final String  SIPGEAR_NAME                    = "sipgear";        // [31270] 3x270
    public static final String  TAIP_NAME                       = "taip";           // [31275] 3x275

    /* GTS Enterprise */
    public static final String  CALAMP_NAME                     = "calamp";         // [20500] (31050) LMU-1000, LMU-1500
    public static final String  SANAV_NAME                      = "sanav";          // [31220] GC-101, GX-101
    public static final String  FALCOM_NAME                     = "falcom";         // [3x230]
    public static final String  WONDE_NAME                      = "wonde";          // [3x240]
    public static final String  LAIPAC_NAME                     = "laipac";         // [3x250] S-911, StarFinder
    public static final String  INTELLITRAC_NAME                = "intellitrac";    // [30300] X1, X8
    public static final String  K611_NAME                       = "k611";           // [3x380] 
    public static final String  ENFORA_NAME                     = "enfora";         // [3x400] (31400) MT-Gu, MT-uL, Mini-MT
    public static final String  MEITRACK_NAME                   = "meitrack";       // [3x480] VT310/CT03
    public static final String  MEITRACK2_NAME                  = "meitrack2";      // [3x482] MVT88/MVT100/MVT600
    public static final String  ONER_NAME                       = "oner";           // [3x485] CT04
    public static final String  ELOC_NAME                       = "eloc";           // [3x490] (31490) eLOC GL100, Enduro
    public static final String  TZAVL05_NAME                    = "tzavl05";        // [3x510] TZone AVL05
    public static final String  TZAVL08_NAME                    = "tzavl08";        // [3x513] TZone AVL08
    public static final String  ANTARES_NAME                    = "antares";        // [3x520] 
    public static final String  ATRACK_NAME                     = "atrack";         // [3x525] 
    public static final String  WEBTECH_NAME                    = "webtech";        // [3x530] 
    public static final String  TELTONIKA_NAME                  = "teltonika";      // [3x540]
    public static final String  TRIMTRAC_NAME                   = "trimtrac";       // [3x680] Pro
    public static final String  XIRGO_NAME                      = "xirgo";          // [3x690] 
    
    public static final String  GLOBALSAT_NAME                  = "globalsat";      // [xxxxx]
    public static final String  STARSNAV_NAME                   = "starsnav";       // [xxxxx]
    public static final String  BLUETREE_NAME                   = "bluetree";       // [xxxxx]
    public static final String  MAJID_NAME                      = "majid";          // [xxxxx]
    public static final String  MOREY_NAME                      = "morey";          // [xxxxx]
    public static final String  BIGWATCHER_NAME                 = "bigwatcher";     // [xxxxx]
    
    public static final String  GTSGEN1_NAME                    = "gtsgen_1";       // [3x111]
    public static final String  GTSGEN2_NAME                    = "gtsgen_2";       // [3x112]

    /* servlet-based DCS */
    public static final String  GSSPOT_NAME                     = "w-gsspot";       // Servlet
    public static final String  AXONN_NAME                      = "w-axonn";        // Servlet

    /* non-DCS services */
    public static final String  UPLOAD_NAME                     = "upload";         // [3x000]

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // This DCServerFactory will check the class returned from "RTConfig.getProperty(RTKey.MAIN_CLASS,null)"
    // to see if it reaponds to the method "DCServerFactory_LoadName".  If it does, it calls this method
    // to see if there is a specific DCServer which it is to load exclusively.  Compliant DCServer
    // implementations should include a static method like the following:
    //   public static String DCServerFactory_LoadName() { return DCServerFactory.SERVER_NAME; }
    // In the absense of this method, or if this method return null/blank, then all available DCServers
    // will be loaded.
    
    private static final String StaticMethod_LoadName           = "DCServerFactory_LoadName";
    
    // ------------------------------------------------------------------------

    private static       String LoadServerNameOnly              = null;

    /**
    *** Gets the specific DCServerConfig name, or null if there is no specific DCServerConfig.
    *** @return  The name of the specific DCServerConfig entry
    **/
    public static String GetSpecificDCServerName()
    {
        return LoadServerNameOnly; // may be null/blank
    }
   
    /**
    *** Returns true if a specific DCServerConfig name is defined.
    *** @return True if a specific DCServerConfig name is defined.
    **/
    public static boolean HasSpecificDCServerName()
    {
        return !StringTools.isBlank(LoadServerNameOnly);
    }

    /**
    *** Sets the specific DCServerConfig name
    *** @param name  The name of the DCServerConfig entry
    **/
    public static void SetSpecificDCServerName(String name)
    {
        String n = StringTools.trim(name);
        LoadServerNameOnly = (StringTools.isBlank(n) || n.equals("*"))? null : n;
    }

    /**
    *** Initialize the specific DCServerConfig name
    **/
    public static void InitSpecificDCServerName()
    {

        /* already initialized? */
        if (DCServerFactory.HasSpecificDCServerName()) {
            return;
        }

        /* check runtime property */
        String dcsName = RTConfig.getString(DBConfig.PROP_dcs_name,null);
        if (!StringTools.isBlank(dcsName)) {
            DCServerFactory.SetSpecificDCServerName(dcsName);
            return;
        }

        /* check main class for specific name */
        Object mainClass = RTConfig.getProperty(RTKey.MAIN_CLASS, null);
        if (mainClass instanceof Class) {
            //Print.logInfo("Checking main class for DCServer: " + StringTools.className(mainClass));
            try {
                MethodAction ma = new MethodAction((Class)mainClass, StaticMethod_LoadName);
                Object rtn = ma.invoke();
                if (rtn == null) {
                    // skip
                } else
                if (rtn instanceof String[]) {
                    String dcsn[] = (String[])rtn;
                    if (!ListTools.isEmpty(dcsn)) {
                        DCServerFactory.SetSpecificDCServerName(dcsn[0]);
                        return;
                    }
                } else
                if (rtn instanceof String) {
                    DCServerFactory.SetSpecificDCServerName((String)rtn);
                    return;
                } else {
                    Print.logInfo("Unrecognized return type: " + StringTools.className(rtn));
                }
            } catch (Throwable th) {
                // skip
            }
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified port is valid
    *** @param port The port to validate
    *** @return True if the specified port is valid
    **/
    public static boolean isValidPort(int port)
    {
        return ((port > 0) && (port <= 65535));
    }

    /**
    *** Returns true if the specified port array is valid
    *** @param port The ports to validate
    *** @return True if the specified port array is valid
    **/
    public static boolean isValidPort(int port[])
    {
        if (ListTools.isEmpty(port)) {
            return false;
        } else {
            for (int i = 0; i < port.length; i++) {
                if (!DCServerFactory.isValidPort(port[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the named server is defined
    **/
    public static boolean serverJarExists(String name)
    {
        try {
            String gtsHome  = !StringTools.isBlank(GTS_HOME)? GTS_HOME : ".";
            String jarPath  = gtsHome + File.separator + "build" + File.separator + "lib" + File.separator;
            File serverJar  = new File(jarPath + name + ".jar");
            if (serverJar.isFile()) {
                //Print.logDebug("Found " + serverJar);
                return true;
            } else {
                if (StringTools.isBlank(GTS_HOME)) {
                    //Print.logWarn("GTS_HOME not defined (unable to find "+serverJar+")");
                } else {
                    //Print.logDebug("Missing " + serverJar);
                }
                return false;
            }
        } catch (Throwable th) {
            Print.logException("Error checking server jar existance: " + name, th);
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  CMDARG_ACCOUNT                  = "account";
    public static final String  CMDARG_DEVICE                   = "device";
    public static final String  CMDARG_CMDTYPE                  = "cmdtype";
    public static final String  CMDARG_CMDNAME                  = "cmdname";
    public static final String  CMDARG_ARG                      = DCServerConfig.DEFAULT_ARG_NAME;
    public static final String  CMDARG_SERVER                   = "server";

    public static final String  RESPONSE_SERVER                 = CMDARG_SERVER;
    public static final String  RESPONSE_RESULT                 = "result";
    public static final String  RESPONSE_MESSAGE                = "message";

    /**
    *** ResultCode enumeration for server command responses
    **/
    public enum ResultCode implements EnumTools.StringLocale, EnumTools.StringValue {
        SUCCESS         ("OK000",I18N.getString(DCServerFactory.class,"DCServerFactory.result.successful"     ,"Successful")),
        INVALID_ACCOUNT ("AC001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidAccount" ,"Invalid Account")),
        INVALID_DEVICE  ("DV001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidDevice"  ,"Invalid Device")),
        INVALID_SERVER  ("SR001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidServer"  ,"Invalid Server")),
        NOT_AUTHORIZED  ("AU001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.notAuthorized"  ,"Not Authorized")),
        OVER_LIMIT      ("AU002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.overLimit"      ,"Over Limit")),
        INVALID_COMMAND ("CM001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidCommand" ,"Invalid command")),
        INVALID_ARG     ("CM002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidArgument","Invalid command/argument")),
        INVALID_TYPE    ("CM003",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidType"    ,"Invalid command type")),
        EMPTY_REQUEST   ("CM004",I18N.getString(DCServerFactory.class,"DCServerFactory.result.emptyRequest"   ,"Imvalid/Empty request")),
        NOT_SUPPORTED   ("CM005",I18N.getString(DCServerFactory.class,"DCServerFactory.result.notSupported"   ,"Not Supported by Device")),
        UNKNOWN_HOST    ("HP001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidHost"    ,"Invalid host")),
        TRANSMIT_FAIL   ("TX001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.transmitFail"   ,"Transmit failure")),
        INVALID_PROTO   ("PR001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidProtocol","Invalid Protocol")),
        INVALID_SMS     ("PR002",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidSmsSpec" ,"Invalid SMS specification")),
        INVALID_PACKET  ("PK001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.invalidPacket"  ,"Invalid Packet")),
        INTERNAL_ERROR  ("XX001",I18N.getString(DCServerFactory.class,"DCServerFactory.result.internalError"  ,"Internal Error"));
        // ---
        private String      cc = null;
        private I18N.Text   aa = null;
        ResultCode(String c, I18N.Text a)           { cc = c; aa = a; }
        public String  getStringValue()             { return cc; }
        public String  getCode()                    { return cc; }
        public String  getMessage()                 { return aa.toString(); }
        public String  getMessage(Locale loc)       { return aa.toString(loc); }
        public String  toString()                   { return aa.toString(); }
        public String  toString(Locale loc)         { return aa.toString(loc); }
        public boolean isDefault()                  { return this.equals(SUCCESS); }
    };

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    private static final String TAG_DCServerConfig      = "DCServerConfig";
    private static final String TAG_DCServer            = "DCServer";
    private static final String TAG_Description         = "Description";
    private static final String TAG_ModelNames          = "ModelNames";
    private static final String TAG_Attributes          = "Attributes";
    private static final String TAG_UniqueIDPrefix      = "UniqueIDPrefix";
    private static final String TAG_GlobalProperties    = "GlobalProperties";
    private static final String TAG_Properties          = "Properties";
    private static final String TAG_Property            = "Property";
    private static final String TAG_EventCodeMap        = "EventCodeMap";
    private static final String TAG_Code                = "Code";
    private static final String TAG_Commands            = "Commands";
    private static final String TAG_Command             = "Command";
    private static final String TAG_Type                = "Type";
    private static final String TAG_AclName             = "AclName";
    private static final String TAG_String              = "String";
    private static final String TAG_StatusCode          = "StatusCode";
    private static final String TAG_ListenPorts         = "ListenPorts";
    private static final String TAG_Include             = "Include";
    private static final String TAG_Arg                 = "Arg";

    private static final String ATTR_bindAddress        = "bindAddress";
    private static final String ATTR_backlog            = "backlog";
    private static final String ATTR_portOffset         = "portOffset";
    private static final String ATTR_name               = "name";
    private static final String ATTR_save               = "save";
    private static final String ATTR_readOnly           = "readOnly";
    private static final String ATTR_key                = "key";
    private static final String ATTR_enabled            = "enabled";
    private static final String ATTR_dispatchHost       = "dispatchHost";
    private static final String ATTR_dispatchPort       = "dispatchPort";
    private static final String ATTR_tcpPort            = "tcpPort";
    private static final String ATTR_udpPort            = "udpPort";
    private static final String ATTR_active             = "active";
    private static final String ATTR_dir                = "dir";
    private static final String ATTR_file               = "file";
    private static final String ATTR_optional           = "optional";
    private static final String ATTR_data               = "data";
    private static final String ATTR_protocol           = "protocol";
    private static final String ATTR_arg                = "arg";
    private static final String ATTR_expectAck          = "expectAck";
    private static final String ATTR_acl                = "acl";
    private static final String ATTR_hasArgs            = "hasArgs";
    private static final String ATTR_includeDir         = "includeDir";
    private static final String ATTR_sessionVar         = "sessionVar";
    private static final String ATTR_defaultValue       = "defaultValue";
    private static final String ATTR_length             = "length";
    private static final String ATTR_warnPortConflict   = "warnPortConflict";
    private static final String ATTR_default            = "default";

    private static File _getDCServerXMLFile()
    {
        File cfgFile = RTConfig.getLoadedConfigFile();
        if (cfgFile != null) {
            return new File(cfgFile.getParentFile(), DCSERVER_XML);
        } else {
            return null;
        }
    }

    /* return an XML Document for the 'dcserver.xml' config file */
    private static Document _getDocument(File xmlFile)
    {

        /* valid file specified? */
        if (xmlFile == null) {
            Print.logError("DCServer XML file not specified: " + xmlFile);
            return null;
        } else
        if (!xmlFile.exists()) {
            Print.logError("DCServer XML file does not exist: " + xmlFile);
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(xmlFile);
        } catch (ParserConfigurationException pce) {
            Print.logException("Parse error: ", pce);
        } catch (SAXException se) {
            Print.logException("Parse error: ", se);
        } catch (IOException ioe) {
            Print.logException("Parse error: ", ioe);
        }
        
        /* return */
        return doc;
        
    }

    /* load the 'dcserver.xml' file */
    private static RTProperties                 GlobalProperties    = null;
    private static Map<Integer,DCServerConfig>  TCPPortMap          = null;
    private static Map<Integer,DCServerConfig>  UDPPortMap          = null;
    public static void loadDCServerXML(File xmlFile)
    {

        /* Global properties */
        GlobalProperties = new RTProperties();

        /* port conflict check */
        TCPPortMap = new OrderedMap<Integer,DCServerConfig>();
        UDPPortMap = new OrderedMap<Integer,DCServerConfig>();

        /* load XML */
        try {
            OrderedSet<DCServerConfig> dcsSet = DCServerFactory._loadDCServerXML(xmlFile, 0);
            if (!ListTools.isEmpty(dcsSet)) {
                for (DCServerConfig dcs : dcsSet) {
                    DCServerFactory.addDCS(dcs);
                }
            }
        } catch (Throwable t) {
            Print.logException("Unable to load DCServerFactory XML", t);
        }

        /* global properties */
        if (GlobalProperties != null) {
            //GlobalProperties.printProperties("Setting Global Properties:");
            RTConfig.setProperties(GlobalProperties);
        }

        /* list ports */
        /*
        for (Integer p : TCPPortMap.keySet()) {
            DCServerConfig dcs = TCPPortMap.get(p);
            Print.logInfo("TCP Port " + p + " ==> " + dcs.getName());
        }
        for (Integer p : UDPPortMap.keySet()) {
            DCServerConfig dcs = UDPPortMap.get(p);
            Print.logInfo("UDP Port " + p + " ==> " + dcs.getName());
        }
        */

        /* clear for garbage collection (no longer needed) */
        GlobalProperties = null;
        TCPPortMap = null;
        UDPPortMap = null;

    }

    /* load the 'private.xml' file */
    private static OrderedSet<DCServerConfig> _loadDCServerXML(File xmlFile, int recurseLvl)
    {

        /* xml file specified? */
        if (xmlFile == null) {
            // get default file
            xmlFile = DCServerFactory._getDCServerXMLFile(); 
            // 'xmlFile' may still be null
        }

        /* save top-level loaded file name */
        if (recurseLvl == 0) {
            DCServerFactory.LoadedDCServerXMLFile = xmlFile;
        }

        /* look for specific DCServer name */
        if ((xmlFile == null) || !xmlFile.isFile()) {
            File origFile = xmlFile;

            /* look for the specific DCS name (if applicable) */
            File cfgDir = RTConfig.getLoadedConfigDir();
            String dcsName = DCServerFactory.GetSpecificDCServerName();
            if (!StringTools.isBlank(dcsName) && (cfgDir != null)) {
                String dcsFileName = "dcserver_" + dcsName + ".xml";
                xmlFile = FileTools.findFile(cfgDir, new String[] {
                    dcsFileName,
                    DCSERVERS_DIR + "/" + dcsFileName
                }); // may still return null if not found
                if (xmlFile == null) {
                    Print.logWarn("DCServerConfig XML file not found: " + dcsFileName);
                }
            }

            /* if still not found, then display message and return */
            if ((xmlFile == null) || !xmlFile.isFile()) {
                if (RTConfig.isWebApp() && !BasicPrivateLabel.isTrackServlet()) {
                    Print.logInfo("DCServerConfig XML file not loaded: " + origFile);
                } else {
                    Print.logError("DCServerConfig XML file does not exist: " + origFile);
                }
                return null;
            }

        }

        /* get XML document */
        //Print.logDebug("Loading " + xmlFile);
        Document xmlDoc = DCServerFactory._getDocument(xmlFile);
        if (xmlDoc == null) {
            Print.logError("Unable to create DCServerConfig XML 'Document' (syntax errors?)");
            return null;
        }

        /* get top-level tag */
        Element dcsDef = xmlDoc.getDocumentElement();
        if (!dcsDef.getTagName().equalsIgnoreCase(TAG_DCServerConfig)) {
            Print.logError("["+xmlFile+"] Invalid root tag ID: " + dcsDef.getTagName());
            return null;
        }
        
        /* top-level attributes */
        if (recurseLvl == 0) {
            BIND_ADDRESS   = XMLTools.getAttribute(   dcsDef, ATTR_bindAddress, BIND_ADDRESS  , true);
            BIND_ADDRESS   = StringTools.blankDefault(RTConfig.getString(ATTR_bindAddress,null),BIND_ADDRESS);
            LISTEN_BACKLOG = XMLTools.getAttributeInt(dcsDef, ATTR_backlog    , LISTEN_BACKLOG, true);
            PORT_OFFSET    = XMLTools.getAttributeInt(dcsDef, ATTR_portOffset , PORT_OFFSET   , true);
            INCLUDE_DIR    = XMLTools.getAttribute(   dcsDef, ATTR_includeDir , INCLUDE_DIR   , true);
        }

        /* prepare for include: XML parent dir */
        File parentDir = xmlFile.getParentFile();
        if (parentDir != null) {
            try {
                File dir = parentDir.getCanonicalFile();
                parentDir = dir;
            } catch (Throwable th) {
                // 
            }
        }

        /* parse DCServerConfig tags */
        OrderedSet<DCServerConfig> dcserverSet = new OrderedSet<DCServerConfig>(true);
        Set<String> dcserverNames = new HashSet<String>();
        NodeList dcsChildNodes = dcsDef.getChildNodes();
        for (int dcsn = 0; dcsn < dcsChildNodes.getLength(); dcsn++) {

            /* get Node (only interested in 'Element's) */
            Node dcsChildNode = dcsChildNodes.item(dcsn);
            if (!(dcsChildNode instanceof Element)) {
                continue;
            }

            /* parse node */
            String dcsNodeName = dcsChildNode.getNodeName();
            Element dcsChildElem = (Element)dcsChildNode;
            if (dcsNodeName.equalsIgnoreCase(TAG_DCServer)) {
                Element dcsTag   = dcsChildElem;
                String  dcsName  = XMLTools.getAttribute(dcsTag,ATTR_name    ,null,false);
                String  dcsProto = XMLTools.getAttribute(dcsTag,ATTR_protocol,null,false);

                /* load specific DCServer only? */
                boolean setGlobalProperties = false;
                String specDCN = DCServerFactory.GetSpecificDCServerName();
                if (!StringTools.isBlank(specDCN)) {
                    // a specific DCServerConfig name has been specified
                    if (!specDCN.equals(dcsName)) {
                        // current DCServerConfig does NOT match name
                        //Print.logDebug("Skipping this DCServer: " + dcsName);
                        continue;
                    }
                    // current DCServerConfig DOES not match name
                    setGlobalProperties = true;
                }

                /* already added */
                if (dcserverNames.contains(dcsName)) {
                    Print.logInfo("Ignoring duplicate DCServer ["+recurseLvl+"]: " + dcsName);
                    continue;
                }
                dcserverNames.add(dcsName);

                /* active? */
                boolean active = XMLTools.getAttributeBoolean(dcsTag, ATTR_active, true, false);
                if (!active) {
                    Print.logDebug("Inactive DCServer ["+recurseLvl+"]: " + dcsName);
                    continue;
                }
                Print.logDebug("Parsing DCServer ["+recurseLvl+"]: " + dcsName);

                /* create new DCServerConfig */
                DCServerConfig dcs = new DCServerConfig();
                dcs.setName(dcsName);
                dcs.setCommandProtocol(dcsProto);
                boolean warnPortConflict = DEFAULT_WARN_PORT_CONFLICT;
                NodeList childNodes = dcsTag.getChildNodes();
                for (int c = 0; c < childNodes.getLength(); c++) {
    
                    /* get Node (only interested in 'Element's) */
                    Node dcsNode = childNodes.item(c);
                    if (!(dcsNode instanceof Element)) {
                        continue;
                    }
    
                    /* parse node */
                    String nodeName = dcsNode.getNodeName();
                    Element dcsElem = (Element)dcsNode;
                    if (nodeName.equalsIgnoreCase(TAG_Description)) {
                        String desc = XMLTools.getNodeText(dcsElem," ",false);
                        //Print.logInfo("Old Description: " + dcsName + " - " + desc);
                        desc = RTConfig.getString("DCServer." + dcsName + ".description", desc);
                        //Print.logInfo("New Description: " + dcsName + " - " + desc);
                        dcs.setDescription(desc);
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_ModelNames)) {
                        String modelNames[] = StringTools.split(XMLTools.getNodeText(dcsElem,"\n",false),'\n');
                        // TODO:
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_Attributes)) {
                        RTProperties attr = new RTProperties(XMLTools.getNodeText(dcsElem," ",false));
                        long flagAttr = DCServerConfig.GetAttributeFlags(attr);
                        dcs.setAttributeFlags(flagAttr);
                        RTProperties rtProps = dcs.getProperties();
                        for (Object rtk : attr.getPropertyKeys()) {
                            Object rtv = attr.getProperty(rtk,null);
                            rtProps.setProperty(PROP_Attribute_ + rtk, rtv);
                        }
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_UniqueIDPrefix)) {
                        String uidPfx = XMLTools.getNodeText(dcsElem,",",false);
                        dcs.setUniquePrefix(StringTools.parseString(uidPfx,','));
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_ListenPorts)) {
                        warnPortConflict = XMLTools.getAttributeBoolean(dcsElem,ATTR_warnPortConflict,DEFAULT_WARN_PORT_CONFLICT,false);
                        InetAddress bindAddr = null;
                        String bindAddrName = XMLTools.getAttribute(dcsElem,ATTR_bindAddress,null,false);
                        try {
                            if (!StringTools.isBlank(bindAddrName)) {
                                bindAddr = InetAddress.getByName(bindAddrName);
                            }
                        } catch (Throwable th) {
                            Print.logException("Unable to locate Bind Address: " + bindAddrName, th);
                            bindAddr = null;
                        }
                        dcs.setTcpPorts(bindAddr,DCServerFactory.parsePorts(XMLTools.getAttribute(dcsElem,ATTR_tcpPort,null,false)),true);
                        dcs.setUdpPorts(bindAddr,DCServerFactory.parsePorts(XMLTools.getAttribute(dcsElem,ATTR_udpPort,null,false)),true);
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_Properties)) {
                        RTProperties rtProps = dcs.getProperties();
                        NodeList propList = XMLTools.getChildElements(dcsElem,TAG_Property);
                        for (int p = 0; p < propList.getLength(); p++) {
                            Node propNode = propList.item(p);
                            if (propNode instanceof Element) {
                                Element propElem = (Element)propNode;
                                String key = XMLTools.getAttribute(propElem,ATTR_key,null,false);
                                if (!StringTools.isBlank(key)) {
                                    if (!key.startsWith(dcsName+".")) { key = dcsName + "." + key; }
                                    String val = StringTools.trim(XMLTools.getNodeText(propElem," ",false));
                                    //Print.logInfo("Setting property " + key + " ==> " + val);
                                    rtProps.setProperty(key, val);
                                } else {
                                    Print.logWarn("Undefined Property key ignored.");
                                }
                            }
                        }
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_GlobalProperties)) {
                        if (setGlobalProperties && (GlobalProperties != null)) {
                            RTProperties rtProps = GlobalProperties;
                            NodeList propList = XMLTools.getChildElements(dcsElem,TAG_Property);
                            for (int p = 0; p < propList.getLength(); p++) {
                                Node propNode = propList.item(p);
                                if (propNode instanceof Element) {
                                    Element propElem = (Element)propNode;
                                    String key = XMLTools.getAttribute(propElem,ATTR_key,null,false);
                                    if (!StringTools.isBlank(key)) {
                                        String val = StringTools.trim(XMLTools.getNodeText(propElem," ",false));
                                        //Print.logInfo("Setting global property " + key + " ==> " + val);
                                        rtProps.setProperty(key, val);
                                    } else {
                                        Print.logWarn("Undefined Property key ignored.");
                                    }
                                }
                            }
                        }
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_EventCodeMap)) {
                        boolean ecEnabled = XMLTools.getAttributeBoolean(dcsElem,ATTR_enabled,true,false); // property key
                        Map<Integer,DCServerConfig.EventCode> codeMap = new HashMap<Integer,DCServerConfig.EventCode>();
                        NodeList codeList = XMLTools.getChildElements(dcsElem,TAG_Code);
                        for (int eci = 0; eci < codeList.getLength(); eci++) {
                            Node codeNode = codeList.item(eci);
                            if (codeNode instanceof Element) {
                                Element codeElem = (Element)codeNode;
                                String dataStr = XMLTools.getAttribute(codeElem,ATTR_data,null,false); // property key
                                String keyStr  = XMLTools.getAttribute(codeElem,ATTR_key ,null,false); // property key
                                int    keyInt  = StringTools.parseInt(keyStr,-1);
                                if (keyInt >= 0) {
                                    int sc = StatusCodes.STATUS_IGNORE;
                                    String valStr  = XMLTools.getNodeText(codeElem," ",false);
                                    if (StringTools.isBlank(valStr) || valStr.equalsIgnoreCase("ignore")) {
                                        sc = StatusCodes.STATUS_IGNORE;
                                    } else
                                    if (valStr.equalsIgnoreCase("default") || valStr.equalsIgnoreCase("none")) {
                                        sc = StatusCodes.STATUS_NONE;
                                    } else {
                                        int sci = StringTools.parseInt(valStr, StatusCodes.STATUS_IGNORE);
                                        if (sci < 0) {
                                            sc = StatusCodes.STATUS_IGNORE; // ignore
                                        } else
                                        if (sci == 0) {
                                            sc = StatusCodes.STATUS_NONE;
                                        } else {
                                            sc = sci;
                                        }
                                    }
                                    //Print.logDebug("Code translate " + StatusCodes.GetHex(keyInt) + " ==> " + StatusCodes.GetHex(sc));
                                    codeMap.put(new Integer(keyInt), new DCServerConfig.EventCode(keyInt,sc,dataStr));
                                } else {
                                    Print.logWarn("Invalid Code key ignored: " + keyStr);
                                }
                            }
                        }
                        dcs.setEventCodeEnabled(ecEnabled);
                        dcs.setEventCodeMap(codeMap);
                    } else
                    if (nodeName.equalsIgnoreCase(TAG_Commands)) {
                        // parse "Command"
                        String cmdHost = XMLTools.getAttribute(   dcsElem, ATTR_dispatchHost,null,false);
                        int    cmdPort = XMLTools.getAttributeInt(dcsElem, ATTR_dispatchPort,-1  ,false);
                        dcs.setCommandDispatcherHost(cmdHost);
                        dcs.setCommandDispatcherPort(cmdPort,true);
                        NodeList commandsNodes = dcsElem.getChildNodes();
                        for (int cmi = 0; cmi < commandsNodes.getLength(); cmi++) {
                            Node cmdNode = commandsNodes.item(cmi);
                            if (!(cmdNode instanceof Element)) { continue; }
                            Element cmdElem = (Element)cmdNode;
                            String cmdTagName = cmdNode.getNodeName();
                            if (cmdTagName.equalsIgnoreCase(TAG_AclName)) {
                                String aclName = XMLTools.getNodeText(cmdElem,"");
                                String aclDftStr = XMLTools.getAttribute(cmdElem,ATTR_default,null,false);
                                AclEntry.AccessLevel aclDft = AclEntry.parseAccessLevel(aclDftStr, AclEntry.AccessLevel.WRITE);
                                dcs.setCommandsAclName(aclName, aclDft);
                            } else
                            if (cmdTagName.equalsIgnoreCase(TAG_Command)) {
                                boolean cmdEnable = XMLTools.getAttributeBoolean(cmdElem,ATTR_enabled,true,false);
                                if (cmdEnable) {
                                    String  cmdName    = XMLTools.getAttribute(cmdElem,ATTR_name,null,false);
                                    boolean hasArgs    = XMLTools.getAttributeBoolean(cmdElem,ATTR_hasArgs,false,false);
                                    boolean expectAck  = XMLTools.getAttributeBoolean(cmdElem,ATTR_expectAck,false,false);
                                    String  aclName    = XMLTools.getAttribute(cmdElem,ATTR_acl,null,false);
                                    String  cmdTypes[] = null;
                                    String  cmdDesc    = null;
                                    String  cmdAclName = !StringTools.isBlank(aclName)? aclName : AclEntry.CreateAclName(dcs.getCommandsAclName(),cmdName);
                                    AclEntry.AccessLevel cmdAclDft = AclEntry.AccessLevel.WRITE;
                                    String  cmdString  = null;
                                    String  cmdProto   = null;
                                    int     cmdSCode   = StatusCodes.STATUS_NONE;
                                    java.util.List<DCServerConfig.CommandArg> cmdArgList = new Vector<DCServerConfig.CommandArg>();
                                    NodeList cmdSubNodes = cmdElem.getChildNodes();
                                    for (int s = 0; s < cmdSubNodes.getLength(); s++) {
                                        Node cmdSubNode = cmdSubNodes.item(s);
                                        if (!(cmdSubNode instanceof Element)) { continue; }
                                        String cmdNodeName = cmdSubNode.getNodeName();
                                        Element cmdSubElem = (Element)cmdSubNode;
                                        if (cmdNodeName.equalsIgnoreCase(TAG_Type)) {
                                            String typ = XMLTools.getNodeText(cmdSubElem,",");
                                            cmdTypes = StringTools.parseString(typ,',');
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_Description)) {
                                            cmdDesc = XMLTools.getNodeText(cmdSubElem," ");
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_AclName)) {
                                            cmdAclName = XMLTools.getNodeText(cmdSubElem,"");
                                            String aclDftStr = XMLTools.getAttribute(cmdSubElem,ATTR_default,null,false);
                                            cmdAclDft = AclEntry.parseAccessLevel(aclDftStr, AclEntry.AccessLevel.WRITE);
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_String)) {
                                            cmdProto    = XMLTools.getAttribute(cmdSubElem,ATTR_protocol,null,false);
                                            //cmdProtoArg = XMLTools.getAttribute(cmdSubElem,ATTR_arg     ,null,false);
                                            cmdString   = XMLTools.getNodeText(cmdSubElem,"");
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_StatusCode)) {
                                            String scStr = XMLTools.getNodeText(cmdSubElem,"");
                                            int sc = StringTools.parseInt(scStr,-1);
                                            cmdSCode = (sc > 0)? sc : StatusCodes.STATUS_NONE;
                                        } else
                                        if (cmdNodeName.equalsIgnoreCase(TAG_Arg)) {
                                            String  argName   = XMLTools.getAttribute(cmdSubElem,ATTR_name,null,false);
                                            String  argVar    = XMLTools.getAttribute(cmdSubElem,ATTR_sessionVar,null,false);
                                            String  argDftVal = XMLTools.getAttribute(cmdSubElem,ATTR_defaultValue,null,false);
                                            boolean argROnly  = XMLTools.getAttributeBoolean(cmdSubElem,ATTR_readOnly,false,false);
                                            String  argDesc   = StringTools.trim(XMLTools.getNodeText(cmdSubElem," "));
                                            DCServerConfig.CommandArg cmdArg;
                                            cmdArg = new DCServerConfig.CommandArg(argName,argDesc,argROnly,argVar,argDftVal);
                                            String  argLen    = XMLTools.getAttribute(cmdSubElem,ATTR_length,null,false);
                                            if (!StringTools.isBlank(argLen)) {
                                                int len[] = StringTools.parseInt(StringTools.split(argLen,','),0);
                                                int dispLen = ((len.length > 0) && (len[0] > 0))? len[0] : 70;
                                                int maxLen  = ((len.length > 1) && (len[1] > 0))? len[1] : dispLen;
                                                if (maxLen < dispLen) { maxLen = dispLen; }
                                                cmdArg.setLength(dispLen, maxLen);
                                            }
                                            cmdArgList.add(cmdArg);
                                            hasArgs = true;
                                        } else {
                                            // unrecognized tag
                                        }
                                    }
                                    dcs.addCommand(
                                        cmdName, cmdDesc, 
                                        cmdTypes, 
                                        cmdAclName, cmdAclDft,
                                        cmdString, hasArgs, cmdArgList,
                                        cmdProto, expectAck,
                                        cmdSCode);
                                }
                            } else {
                                // unrecognized tag
                            }
                        }
                    } else {
                        Print.logError("["+xmlFile+"] Unrecognized tag name: " + nodeName);
                    }
    
                }

                /* TCP check ports */
                if (TCPPortMap != null) {
                    int tcpPorts[] = dcs.getTcpPorts();
                    if (!ListTools.isEmpty(tcpPorts)) {
                        for (int p = 0; p < tcpPorts.length; p++) {
                            Integer port = new Integer(tcpPorts[p]);
                            if (TCPPortMap.containsKey(port)) {
                                DCServerConfig s = TCPPortMap.get(port);
                                if (warnPortConflict && !s.getName().equals(dcs.getName())) {
                                    Print.logWarn("["+xmlFile.getName()+":"+dcs.getName()+"] TCP Port Conflict: "+port+" (see '"+s.getName()+"')");
                                }
                            } else {
                                TCPPortMap.put(port,dcs);
                            }
                        }
                    }
                } else {
                    Print.logWarn("TCPPortMap is null!");
                }
    
                /* UDP check ports */
                if (UDPPortMap != null) {
                    int udpPorts[] = dcs.getUdpPorts();
                    if (!ListTools.isEmpty(udpPorts)) {
                        for (int p = 0; p < udpPorts.length; p++) {
                            Integer port = new Integer(udpPorts[p]);
                            if (UDPPortMap.containsKey(port)) {
                                DCServerConfig s = UDPPortMap.get(port);
                                if (warnPortConflict && !s.getName().equals(dcs.getName())) {
                                    Print.logWarn("["+xmlFile.getName()+":"+dcs.getName()+"] UDP Port Conflict: "+port+" (see '"+s.getName()+"')");
                                }
                            } else {
                                UDPPortMap.put(port,dcs);
                            }
                        }
                    }
                } else {
                    Print.logWarn("UDPPortMap is null!");
                }
    
                /* save dcs */
                //Print.logInfo("Saving " + dcs.getName());
                dcserverSet.add(dcs);

            } else
            if (dcsNodeName.equalsIgnoreCase(TAG_Include)) {
                Element inclTag = dcsChildElem;
                int     portOffset = XMLTools.getAttributeInt(    inclTag, ATTR_portOffset,     0, false);
                boolean optional   = XMLTools.getAttributeBoolean(inclTag, ATTR_optional  , false, false);

                /* check file */
                String inclFileStr = inclTag.getAttribute(ATTR_file);
                if (StringTools.isBlank(inclFileStr)) {
                    Print.logError("Invalid 'Include' (blank file)");
                    continue;
                }
                
                /* directory override */
                String inclDirStr = inclTag.getAttribute(ATTR_dir);
                File inclDir = null;
                if (!StringTools.isBlank(inclDirStr)) {
                    inclDir = new File(inclDirStr);
                } else
                if (!StringTools.isBlank(INCLUDE_DIR)) {
                    inclDir = new File(INCLUDE_DIR);
                }
        
                /* locate file */
                File inclFile = null;
                if ((inclDir != null) && inclDir.isAbsolute()) {
                    // absolute include directory location
                    File file = new File(inclDir, inclFileStr);
                    inclFile  = file.isFile()? file : null;
                    // do not continue looking for the include file
                } else
                if (parentDir != null) {
                    // relative parent/include directory
                    if ((inclFile == null) && (inclDir != null)) {
                        File dir  = new File(parentDir, inclDir.toString());
                        File file = new File(dir, inclFileStr);
                        inclFile  = file.isFile()? file : null;
                    }
                    // relative parent directory
                    if (inclFile == null) {
                        File dir  = parentDir;
                        File file = new File(dir, inclFileStr);
                        inclFile  = file.isFile()? file : null;
                    }
                }

                /* include */
                //Print.logInfo("Including["+recurseLvl+"]: " + inclFile + "  [optional="+optional+"]");
                if ((inclFile != null) && inclFile.isFile()) {
                    try {
                        if (inclFile.getCanonicalPath().equals(xmlFile.getCanonicalPath())) {
                            Print.logWarn("Recursive Include ignored: " + inclFile.getCanonicalPath());
                        } else {
                            OrderedSet<DCServerConfig> inclDscSet = DCServerFactory._loadDCServerXML(inclFile, recurseLvl+1);
                            if (!ListTools.isEmpty(inclDscSet)) {
                                for (DCServerConfig dcs : inclDscSet) {
                                    if (!dcserverSet.contains(dcs)) {
                                        //Print.logDebug("Adding: " + dcs);
                                        dcserverSet.add(dcs);
                                    }
                                }
                                //if (recurseLvl==0){for(DCServerConfig dcs:inclDscSet){Print.logInfo("Included["+recurseLvl+"]: "+dcs);}}
                            } else {
                                //Print.logInfo("Nothing included ...");
                            }
                        }
                    } catch (Throwable th) {
                        Print.logException("Error while including file: " + inclFile, th);
                    }
                } else
                if (!optional) {
                    Print.logError("Include file not found: " + inclFileStr);
                } else {
                    // optional include ignored
                }

            }

        }

        /* success */
        return dcserverSet;

    }
    
    private static int[] parsePorts(String portStr)
    {
        if (!StringTools.isBlank(portStr)) {
            String portArr[] = StringTools.split(portStr,',');
            int    portInt[] = StringTools.parseInt(portArr,-1);
            if (!ListTools.isEmpty(portInt)) {
                for (int i = 0; i < portInt.length; i++) { 
                    if (portInt[i] > 0) { 
                        portInt[i] += PORT_OFFSET; 
                    } else {
                        Print.logError("Invalid port specification: " + portStr);
                        return null;
                    }
                }
                return portInt;
            } else {
                Print.logError("Invalid port specification: " + portStr);
                return null;
            }
        }
        return null;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // bind address
    // (may ne necessary is this system has multiple IP addresses)
    public static       String  BIND_ADDRESS                = null;

    // listen backlog
    // (how many pending connections are allowed before they start being rejected)
    public static       int     LISTEN_BACKLOG              = -1; // default

    // port offset (used by 'getPort' only)
    // (used to change to general location of all server ports as a group)
    public static       int     PORT_OFFSET                 = 1000;

    // include directory
    // (directory where included files may be placed)
    public static       String  INCLUDE_DIR                 = null;

    // 'GTS_HOME' environment variable
    public static       String  GTS_HOME                    = null;

    // DCServerConfig map
    private static HashMap<String,DCServerConfig> dcServerMap; // do not pre-initialize

    // "missing server" list
    private static      Vector<String> missingServerList    = null;

    static {
        // should not be initialized here!
        //DCServerFactory._startupInit();
    }

    private static HashMap<String,DCServerConfig> _DCServerMap(boolean dispError)
    {
        if (DCServerFactory.dcServerMap == null) {
            if (dispError && (BasicPrivateLabel.isTrackServlet() || RTConfig.isCommandLine())) {
                Print.logStackTrace("DCServerConfig not initialized!");
            }
            DCServerFactory.dcServerMap = new HashMap<String,DCServerConfig>();
        }
        return DCServerFactory.dcServerMap;
    }

    private static boolean _didInit = false;
    public static void init()
    {
        if (!_didInit) {
            DCServerFactory._startupInit();
            SMSOutboundGateway._startupInit();
        }
    }

    private static void _startupInit()
    {
        Print.logDebug("DCServerFactory initializing ...");
        _didInit = true;

        // This must be called _after_ the command-line runtime initialization has occurred.
        if (!RTConfig.isInitialized()) {
            Print.logError("**** DCServerFactory init: Runtime configuration has not been properly initialized ***");
        }

        /* 'GTS_HOME' */
        GTS_HOME = System.getenv(DBConfig.env_GTS_HOME);
        if (StringTools.isBlank(GTS_HOME)) {
            Print.logWarn("Environment variable 'GTS_HOME' not defined");
        }

        /* load default values */
        BIND_ADDRESS   = RTConfig.getString(DBConfig.PROP_dcs_bindInterface, BIND_ADDRESS);
        LISTEN_BACKLOG = RTConfig.getInt(   DBConfig.PROP_dcs_listenBacklog, LISTEN_BACKLOG);
        PORT_OFFSET    = RTConfig.getInt(   DBConfig.PROP_dcs_portOffset   , PORT_OFFSET);
        
        /* specific DCServer? */
        DCServerFactory.InitSpecificDCServerName();
        if (DCServerFactory.HasSpecificDCServerName()) {
            Print.logInfo("Loading only DCServer name: " + DCServerFactory.GetSpecificDCServerName());
        }

        /* load 'dcserver.xml' */
        DCServerFactory.loadDCServerXML(null);

        /* ServerSocketThread bind interface */
        if (StringTools.isBlank(BIND_ADDRESS)) {
            // bind to any/all available address(es)
            ServerSocketThread.setBindAddress(null);
        } else {
            // bind to a specific IP address
            try {
                InetAddress localBA = InetAddress.getByName(BIND_ADDRESS);
                Print.logDebug("ServerSocketThread Local Bind Address: " + localBA);
                ServerSocketThread.setBindAddress(localBA);
            } catch (UnknownHostException uhe) {
                //Print.logException("Setting local bind interface", uhe);
                Print.logError("Local Bind Address Unknown Host: " + BIND_ADDRESS);
                ServerSocketThread.setBindAddress(null);
            }
        }

        /* ServerSocketThread listen backlog */
        if (LISTEN_BACKLOG > 0) {
            Print.logDebug("ServerSocketThread Listen Backlog: " + LISTEN_BACKLOG);
            ServerSocketThread.setListenBacklog(LISTEN_BACKLOG);
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Adds the named server to the list of registered servers
    **/
    public static DCServerConfig addDCS(
        String name, String desc, 
        int tcpPorts[], int udpPorts[], 
        int commandPort, 
        long flags, 
        String... uniqPfx)
    {

        // ignore if no name
        if (StringTools.isBlank(name)) {
            // quietly ignore
            return null;
        }

        // already added?
        if (DCServerFactory._DCServerMap(false).containsKey(name)) {
            // ignore duplicate entries
            Print.logDebug("Ignoring duplicate DCServer: " + name);
            return null;
        }

        // port keys
        if (!DCServerFactory.serverJarExists(name)) {
            Set<String> serverKeys = RTConfig.getPropertyKeys(name + ".", false);
            if (!ListTools.isEmpty(serverKeys)) {
                if (DCServerFactory.missingServerList == null) {
                    DCServerFactory.missingServerList = new Vector<String>();
                }
                DCServerFactory.missingServerList.add(name);
            }
            Print.logDebug("Server jar not found: %s", name);
            if (!RTConfig.isWebApp()) {
                // skip this server if running from command-line
                return null;
            }
        }

        // add configuration
        return DCServerFactory.addDCS(new DCServerConfig(name, desc, tcpPorts, udpPorts, commandPort, flags, uniqPfx));

    }

    /**
    *** Adds the named server to the list of registered servers
    **/
    private static DCServerConfig addDCS(DCServerConfig dcs)
    {

        /* null server? */
        if (dcs == null) {
            // quietly ignore
            //Print.logDebug("Ignoring null DCServer ...");
            return null;
        }

        /* DCS name */
        String name = dcs.getName();
        if (StringTools.isBlank(name)) {
            // quietly ignore
            //Print.logDebug("Ignoring DCServer with blank name ...");
            return null;
        }

        /* already added? */
        if (DCServerFactory._DCServerMap(false).containsKey(name)) {
            // ignore duplicate entries
            Print.logDebug("Ignoring duplicate DCServer: " + name);
            return null;
        }

        /* add */
        //Print.logInfo("Adding: " + dcs);
        DCServerFactory._DCServerMap(false).put(name, dcs);
        return dcs;

    }
    
    /**
    *** Returns the DCServerConfig instance for the specified device communication server name
    *** @param name  The name of the device communication server
    *** @return The DCServerConfig instance
    **/
    public static DCServerConfig _getServerConfig(String name)
    {
        return DCServerFactory._DCServerMap(true).get(name);
    }

    /**
    *** Returns the DCServerConfig instance for the specified device communication server name
    *** @param name  The name of the device communication server
    *** @return The DCServerConfig instance
    **/
    public static DCServerConfig getServerConfig(String name)
    {
        if (StringTools.isBlank(name)) {
            //Print.logError("DCServerConfig name is blank");
            return null;
        } else {
            DCServerConfig dcs = DCServerFactory._getServerConfig(name);
            if (dcs == null) {
                String m = "DCServer not found: " + name;
                if (DCServerFactory.LoadedDCServerXMLFile != null) {
                    m += " [Not defined in " + DCServerFactory.LoadedDCServerXMLFile + "?]";
                }
                Print.logError(m);
                return null;
            } else {
                return dcs;
            }
        }
    }

    /**
    *** Gets the server config description
    *** @param serverName The server name
    *** @return The server config description
    **/
    public static String getServerConfigDescription(String serverName)
    {
        if (!StringTools.isBlank(serverName)) {
            DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
            return (dcs != null)? dcs.getDescription() : ("("+serverName+")");
        } else {
            return "";
        }
    }

    /**
    *** Returns True if the named DCServerConfig has been registered
    *** @param name  The name of the device communication server
    *** @return True if the named DCServerConfig has been registered
    **/
    public static boolean hasServerConfig(String name)
    {
        if (StringTools.isBlank(name)) {
            return false;
        } else {
            //return (DCServerFactory._DCServerMap(true).get(name) != null);
            return DCServerFactory._DCServerMap(false).containsKey(name);
        }
    }

    /**
    *** Returns a list of all DCServerConfig instances
    *** @return A list of DCServerConfig instances
    **/
    public static java.util.List<DCServerConfig> getServerConfigList(boolean inclAll)
    {
        java.util.List<DCServerConfig> list = new Vector<DCServerConfig>();
        for (DCServerConfig dcs : DCServerFactory._DCServerMap(true).values()) {
            String n = dcs.getName();
            if (inclAll || DCServerFactory.serverJarExists(n)) {
                list.add(dcs);
            }
        }
        return list;
    }

    /**
    *** (used by CheckInstall) Return the number of refererenced servers which are undefined
    **/
    public static boolean hasUndefinedServers()
    {
        return !ListTools.isEmpty(DCServerFactory.missingServerList);
    }

    /**
    *** (used by CheckInstall) Returns the list of referenced, but undefined, servers
    **/
    public static java.util.List<String> getUndefinedServerList()
    {
        return DCServerFactory.missingServerList;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the named server supports a command port
    **/
    public static boolean supportsCommandDispatcher(String serverName)
    {
        return (DCServerFactory.getCommandDispatcherPort(serverName) > 0);
    }

    /**
    *** Returns true if the server for the specified Device supports a command port
    **/
    public static boolean supportsCommandDispatcher(Device device)
    {
        if (device == null) {
            return false;
        } else {
            return DCServerFactory.supportsCommandDispatcher(device.getDeviceCode());
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this device supports digital inputs
    *** @return True if this device supports digital inputs
    **/
    public static boolean hasDigitalInputs(String serverName)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        return (dcs != null)? dcs.hasDigitalInputs() : false;
    }

    /**
    *** Returns true if this device supports digital outputs
    *** @return True if this device supports digital outputs
    **/
    public static boolean hasDigitalOutputs(String serverName)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        return (dcs != null)? dcs.hasDigitalOutputs() : false;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the server port (with constant offset applied)
    *** @param port  server port (without offset applied)
    *** @return The server port
    **/
    public static int getPort(int port)
    {
        return (port > 0)? (port + PORT_OFFSET) : 0;
    }

    /**
    *** Returns an array of server ports (with constant offset applied)
    *** @param ports  array of server ports (without offset applied)
    *** @return The server port array
    **/
    public static int[] getPorts(int... ports)
    {
        if (!ListTools.isEmpty(ports)) {
            int newPorts[] = new int[ports.length];
            for (int i = 0; i < ports.length; i++) {
                newPorts[i] = getPort(ports[i]);
            }
            return newPorts;
        } else {
            return new int[0];
        }
    }

    /**
    *** Private abbreviation for 'getPort'
    *** @param port  server port (without offset applied)
    *** @return The server port array
    **/
    private static int GP(int port)
    {
        return getPort(port);
    }

    /**
    *** Private abbreviation for 'getPorts'
    *** @param ports  array of server ports (without offset applied)
    *** @return The server port array
    **/
    private static int[] GP(int... ports)
    {
        return getPorts(ports);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Command ResultCode ID
    **/
    public static String getCommandResultID(RTProperties r)
    {
        return (r != null)? r.getString(RESPONSE_RESULT,"") : null;
    }

    /**
    *** Return true if the ResultCode represents a successful request/operation
    **/
    public static boolean isCommandResultOK(RTProperties r)
    {
        if (r != null) {
            String rid = DCServerFactory.getCommandResultID(r);
            return StringTools.isBlank(rid) || ResultCode.SUCCESS.getCode().equals(rid);
        } else {
            return false;
        }
    }

    /**
    *** Gets the Command ResultCode Message
    **/
    public static String getCommandResultMessage(RTProperties r)
    {
        return (r != null)? r.getString(RESPONSE_MESSAGE,"") : null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the server 'command' port
    *** @param serverName The server name
    *** @return The server command port, or '0' if not supported
    **/
    public static int getCommandDispatcherPort(String serverName)
    {
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        return (dcs != null)? dcs.getCommandDispatcherPort() : 0;
    }
    
    /**
    *** Send a command request to the command port for the specified server
    **/
    private static RTProperties _sendServerCommand(String serverName,
        String accountID, String deviceID,
        String cmdType, String cmdName, String cmdArgs[])
    {

        /* blank account/device */
        if (StringTools.isBlank(accountID) || StringTools.isBlank(deviceID)) {
            Print.logError("Account/Device is blank");
            return null;
        }

        /* DCS */
        DCServerConfig dcs = DCServerFactory.getServerConfig(serverName);
        if (dcs == null) {
            Print.logError("DCServerConfig not found: %s", serverName);
            return null;
        }

        /* get port */
        String cmdHost = dcs.getCommandDispatcherHost();
        int    cmdPort = dcs.getCommandDispatcherPort();
        if (cmdPort <= 0) {
            Print.logError("[%s] Command port not supported", serverName);
            return null;
        }
        Print.logInfo("[%s] Sending command to '%s:%d'", serverName, cmdHost, cmdPort);

        /* command */
        RTProperties rtCmd = DCServerFactory.createRTProperties(accountID, deviceID, cmdType, cmdName, cmdArgs);
        String   cmdStr    = rtCmd.toString();
        byte     cmdData[] = (cmdStr + "\n").getBytes();
        Print.logInfo("[%s] Command String: %s", serverName, cmdStr);

        /* send */
        RTProperties response = null;
        ClientSocketThread cst = new ClientSocketThread(cmdHost, cmdPort);
        try {
            cst.openSocket();
            cst.socketWriteBytes(cmdData);
            String resp = cst.socketReadLine();
            Print.logInfo("[%s] Command Response: %s", serverName, resp);
            response = new RTProperties(resp);
        } catch (ConnectException ce) { // "Connection refused"
            Print.logError("[" + serverName + "] Unable to connect to server: " + ce.getMessage());
        } catch (Throwable t) {
            Print.logException("[" + serverName + "] Server Command Error", t);
        } finally {
            cst.closeSocket();
        }
        
        /* return response */
        return response;

    }
    
    /**
    *** Send a command request to the server command port for the specified Device
    **/
    public static RTProperties sendServerCommand(Device device,
        String cmdType, String cmdName, String cmdArgs[])
    {

        /* quick checks */
        if (device == null) {
            Print.logWarn("Device is null");
            return null;
        } 
        String acctID = device.getAccountID();
        String devID  = device.getDeviceID();

        /* DCServerConfig */
        String server = device.getDeviceCode();
        if (StringTools.isBlank(server)) {
            Print.logWarn("DeviceCode is null/blank");
            ResultCode result = ResultCode.INVALID_SERVER;
            RTProperties resp = createRTProperties(acctID, devID, cmdType, cmdName, cmdArgs);
            resp.setString(DCServerFactory.RESPONSE_RESULT , result.getCode() );
            resp.setString(DCServerFactory.RESPONSE_MESSAGE, result.toString());
            return resp;
        }
        
        /* exceed max 'ping' */
        int totPings = device.getTotalPingCount();
        int maxPings = device.getMaxPingCount();
        if ((maxPings > 0) && (totPings >= maxPings)) {
            Print.logWarn("Device exceeded maximum pings: %d >= %d", totPings, maxPings);
            ResultCode result = ResultCode.OVER_LIMIT;
            RTProperties resp = createRTProperties(acctID, devID, cmdType, cmdName, cmdArgs);
            resp.setString(DCServerFactory.RESPONSE_RESULT , result.getCode() );
            resp.setString(DCServerFactory.RESPONSE_MESSAGE, result.toString());
            return resp;
        }

        /* send */
        RTProperties resp = DCServerFactory._sendServerCommand(server, acctID, devID, cmdType, cmdName, cmdArgs);

        /* increment 'ping' fields in Device record */
        if (DCServerFactory.isCommandResultOK(resp)) {
            device.incrementPingCount(DateTime.getCurrentTimeSec(), true/*update*/);
        }

        /* return response */
        return resp;

    }

    public static RTProperties createRTProperties(String accountID, String deviceID, 
        String cmdType, String cmdName, String cmdArgs[])
    {
        RTProperties rtCmd = new RTProperties();
        rtCmd.setString(DCServerFactory.CMDARG_ACCOUNT, accountID);
        rtCmd.setString(DCServerFactory.CMDARG_DEVICE , deviceID);
        rtCmd.setString(DCServerFactory.CMDARG_CMDTYPE, cmdType);
        rtCmd.setString(DCServerFactory.CMDARG_CMDNAME, cmdName);
        if (!ListTools.isEmpty(cmdArgs)) {
            for (int i = 0; (i < cmdArgs.length) && (cmdArgs[i] != null); i++) {
                String argKey = CMDARG_ARG + i;
                rtCmd.setString(argKey, cmdArgs[i]);
            }
        }
        return rtCmd;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Calculates/returns the next odometer value 
    **/
    public static double calculateOdometerKM(EventData prevEvent, GeoPoint toPoint)
    {
        if ((prevEvent != null) && prevEvent.isValidGeoPoint() && GeoPoint.isValid(toPoint)) {
            double deltaKM = toPoint.kilometersToPoint(prevEvent.getGeoPoint());
            return prevEvent.getOdometerKM() + deltaKM;
        } else {
            return 0.0;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static void addUnassignedDevice(
        String dcName, String mobID, 
        GeoPoint geoPoint)
    {
        DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, geoPoint, null/*data*/);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static void addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        GeoPoint geoPoint)
    {
        DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, geoPoint, null/*data*/);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static void addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        GeoPoint geoPoint,
        String data)
    {
        double lat = (geoPoint != null)? geoPoint.getLatitude()  : 0.0;
        double lon = (geoPoint != null)? geoPoint.getLongitude() : 0.0;
        DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, lat, lon, data);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static void addUnassignedDevice(
        String dcName, String mobID, 
        double lat, double lon)
    {
        DCServerFactory.addUnassignedDevice(dcName, mobID, null, true, lat, lon, null/*data*/);
    }

    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static void addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        double lat, double lon)
    {
        DCServerFactory.addUnassignedDevice(dcName, mobID, ipAddr, isDuplex, lat, lon, null/*data*/);
    }
    
    /**
    *** Add device-id to UnassignedDevice table
    **/
    public static void addUnassignedDevice(
        String dcName, String mobID, 
        String ipAddr, boolean isDuplex,
        double lat, double lon,
        String data)
    {
        try {
            //org.opengts.extra.tables.UnassignedDevices.add(dcName, mobID, lat, lon); 
            MethodAction unasDev = new MethodAction(
                (DBConfig.PACKAGE_EXTRA_TABLES_ + "UnassignedDevices"),
                "add",
                String.class, String.class, String.class, Boolean.TYPE, Double.TYPE, Double.TYPE, String.class);
            unasDev.invoke(dcName, mobID, ipAddr, isDuplex, lat, lon, data);
        } catch (Throwable th) {
            // necessary because UnassignedDevices is in the 'extra' library
            //Print.logException("UnassignedDevices", th);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the EventData record previous to the specified fixtime
    *** @param device  The Device record handle
    *** @param fixtime The current event fixtime
    *** @return The previous event, or null if there is no previous event
    **/
    public static EventData getPreviousEventData(Device device, long fixtime)
    {
        if (device != null) {
            try {
                long startTime = -1L;
                long endTime   = fixtime - 1L;
                EventData ed[] = EventData.getRangeEvents(
                    device.getAccountID(), device.getDeviceID(),
                    startTime, endTime,
                    null/*statusCodes*/,
                    true/*validGPS*/,
                    EventData.LimitType.LAST, 1, true,
                    null/*additionalSelect*/);
                if ((ed != null) && (ed.length > 0)) {
                    return ed[0];
                } else {
                    return null;
                }
            } catch (DBException dbe) {
                return null;
            }
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Analyzes/Prints the current memory usage.
    *** (see OSTools.checkMemoryUsage) 
    **/
    public static void checkMemoryUsage()
    {
        OSTools.checkMemoryUsage(false/*reset*/);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Load device record from unique-id (not yet used/tested)
    *** @param prefix     An array of unique-id prefixes
    *** @param modemID    The unique modem ID (IMEI, ESN, etc)
    *** @return The Device record
    **/
    public static Device loadDeviceUniqueID(String prefix[], String modemID)
    {
        return DCServerFactory.loadDeviceUniqueID(prefix, modemID, 
            false, null, null, true, null);
    }

    /**
    *** Load device record from unique-id (not yet used/tested)
    *** @param prefix           An array of unique-id prefixes
    *** @param modemID          The unique modem ID (IMEI, ESN, etc)
    *** @param serverID         The server-id (also 'device code'), used for UnassignedDevice entries
    *** @param ipAddress        The inbound IP address, used for UnassignedDevice entries
    *** @param isDuplex         True if duplex, false if simplex, used for UnassignedDevice entries
    *** @param geoPoint         The GPS location of the device, used for UnassignedDevice entries
    *** @return The Device record
    **/
    public static Device loadDeviceUniqueID(String prefix[], String modemID, 
        String serverID, String ipAddress, boolean isDuplex, GeoPoint geoPoint)
    {
        return DCServerFactory.loadDeviceUniqueID(prefix, modemID, 
            true, serverID, ipAddress, isDuplex, geoPoint);
    }

    /**
    *** Load device record from unique-id
    *** @param prefix           An array of unique-id prefixes
    *** @param modemID          The unique modem ID (IMEI, ESN, etc)
    *** @param saveUnassigned   True to save Device to UnassignedDevices, if device is not found
    *** @param serverID         The server-id (also 'device code'), used for UnassignedDevice entries
    *** @param ipAddress        The inbound IP address, used for UnassignedDevice entries
    *** @param isDuplex         True if duplex, false if simplex, used for UnassignedDevice entries
    *** @param geoPoint         The GPS location of the device, used for UnassignedDevice entries
    *** @return The Device record
    **/
    public static Device loadDeviceUniqueID(String prefix[], String modemID, 
        boolean saveUnassigned, String serverID, String ipAddress, boolean isDuplex, GeoPoint geoPoint)
    {
        Device device = null;

        /* find Device */
        String uniqueID = "";
        try {

            /* load device record */
            if (ListTools.isEmpty(prefix)) {
                uniqueID = modemID;
                //Print.logDebug("Looking for UniqueID: " + uniqueID);
                device = Transport.loadDeviceByUniqueID(uniqueID);
            } else {
                uniqueID = prefix[0] + modemID;
                for (int u = 0; u < prefix.length; u++) {
                    String pfxid = prefix[u] + modemID;
                    //Print.logDebug("Looking for UniqueID: " + pfxid);
                    device = Transport.loadDeviceByUniqueID(pfxid);
                    if (device != null) {
                        uniqueID = pfxid;
                        break;
                    }
                }
            }

            /* not found? */
            if (device == null) {
                Print.logWarn("!!!UniqueID not found!: " + uniqueID);
                if (saveUnassigned) {
                    DCServerFactory.addUnassignedDevice(serverID, modemID, ipAddress, isDuplex, geoPoint, null/*data*/);
                }
                return null;
            }

            /* inactive? */
            if (!device.getAccount().isActive() || !device.isActive()) {
                String a = device.getAccountID();
                String d = device.getDeviceID();
                Print.logWarn("Account/Device is inactive: " + a + "/" + d + " [" + uniqueID + "]");
                return null;
            }

            /* return device */
            device.setModemID(modemID);
            return device;

        } catch (Throwable dbe) { // DBException
            Print.logError("Exception getting Device: " + uniqueID + " [" + dbe + "]");
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Lookup the specified mobile ID in the Transport/Device tables using
    *** the specified DC server prefixes.
    **/
    private static void _lookupUniqueID(java.util.List<Device> devList, 
        DCServerConfig dcs,
        String uniqueID, Set<String> uidTried)
    {
        String uidPfx[] = (dcs != null)? dcs.getUniquePrefix() : new String[] { "" };
        for (String pfx : uidPfx) {
            String uid = pfx + uniqueID;
            if ((uidTried == null) || !uidTried.contains(uid)) {
                try {
                    //Print.logInfo("Checking: " + uid);
                    uidTried.add(uid);
                    Device device = Transport.loadDeviceByUniqueID(uid);
                    if (device != null) {
                        devList.add(device);
                    }
                } catch (DBException dbe) {
                    Print.logException("Error retrieving Device by UniqueID", dbe);
                }
            }
        }
    }
    
    /**
    *** Lookup the specified mobile-id in the Transport/Device tables, use
    *** all available DC servers unique-id prefixes.
    *** @param mobileID  The mobile ID to search for
    *** @return An array of matching Devices, or null if no Device was found
    **/
    public static Device[] lookupUniqueID(String mobileID)
    {
        
        /* ignore blank mobile-id */
        if (StringTools.isBlank(mobileID)) {
            return new Device[0];
        }
        
        /* list of found devices */
        java.util.List<Device> devList = new Vector<Device>();

        /* set of all unique id's we've tried (for optimization) */
        Set<String> uidTried = new HashSet<String>();

        /* try blank prefix first */
        DCServerFactory._lookupUniqueID(devList, null, mobileID, uidTried);

        /* scan all loaded DCServerConfig instances */
        HashMap<String,DCServerConfig> dcsMap = DCServerFactory._DCServerMap(false);
        for (String dcsName : dcsMap.keySet()) {
            DCServerFactory._lookupUniqueID(devList, dcsMap.get(dcsName), mobileID, uidTried);
        }

        /* return array of matching Devices */
        return devList.toArray(new Device[devList.size()]);

    }

    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of the unique-id prefix array
    *** @param pfx  The UniqueID prefixes
    *** @return A String representation of the unique-id prefix array
    **/
    public static String getUniquePrefixString(String pfx[])
    {
        if (ListTools.isEmpty(pfx)) {
            return "<blank>";
        } else {
            StringBuffer sb = new StringBuffer();
            String list[] = new String[pfx.length];
            for (int i = 0; i < pfx.length; i++) {
                if (sb.length() > 0) { sb.append(", "); }
                if (StringTools.isBlank(pfx[i]) || pfx[i].equals("*")) {
                    sb.append("<blank>");
                } else {
                    sb.append("\"").append(pfx[i]).append("\"");
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // Runtime property name suffixes
 
    public static final String  CFG_uniquePrefix                = ".uniquePrefix";               // String array
    public static final String  CFG_uniqueIdPrefix              = ".uniqueIdPrefix";             // String array
    public static final String  CFG_tcpPort                     = ".tcpPort";                    // int
    public static final String  CFG_udpPort                     = ".udpPort";                    // int
    public static final String  CFG_port                        = ".port";                       // int
    public static final String  CFG_commandPort                 = ".commandPort";                // int
    public static final String  CFG_commandProtocol             = ".commandProtocol";            // String [udp|tcp|sms]
    public static final String  CFG_ackResponsePort             = ".ackResponsePort";            // int
    public static final String  CFG_clientCommandPort           = ".clientCommandPort";          // int
    public static final String  CFG_clientCommandPort_udp       = ".clientCommandPort.udp";      // int
    public static final String  CFG_clientCommandPort_tcp       = ".clientCommandPort.tcp";      // int
    public static final String  CFG_tcpIdleTimeoutMS            = ".tcpIdleTimeoutMS";           // int/long
    public static final String  CFG_tcpPacketTimeoutMS          = ".tcpPacketTimeoutMS";         // int/long
    public static final String  CFG_tcpSessionTimeoutMS         = ".tcpSessionTimeoutMS";        // int/long
    public static final String  CFG_udpIdleTimeoutMS            = ".udpIdleTimeoutMS";           // int/long
    public static final String  CFG_udpPacketTimeoutMS          = ".udpPacketTimeoutMS";         // int/long
    public static final String  CFG_udpSessionTimeoutMS         = ".udpSessionTimeoutMS";        // int/long
    public static final String  CFG_minimumSpeedKPH             = ".minimumSpeedKPH";            // double
    public static final String  CFG_estimateOdometer            = ".estimateOdometer";           // boolean
    public static final String  CFG_simulateGeozones            = ".simulateGeozones";           // boolean
    public static final String  CFG_simulateDigitalInputs       = ".simulateDigitalInputs";      // boolean
    public static final String  CFG_minimumMovedMeters          = ".minimumMovedMeters";         // double
    public static final String  CFG_saveRawDataPackets          = ".saveRawDataPackets";         // boolean
    public static final String  CFG_startStopSupported          = ".startStopSupported";         // boolean
    public static final String  CFG_statusLocationInMotion      = ".statusLocationInMotion";     // Translate Location to InMotion [Boolean]
    public static final String  CFG_useLastValidGPSLocation     = ".useLastValidGPSLocation";    // boolean
    public static final String  CFG_initialPacket               = ".initialPacket";              // String/Bytes
    public static final String  CFG_finalPacket                 = ".finalPacket";                // String/Bytes

    /**
    *** Return an array of "TCP port" property names
    *** @param name  The server name
    *** @return An array of "TCP port" property names
    **/
    public static String[] CONFIG_TCP_PORT(String name)
    {
        return new String[] { name + CFG_tcpPort, name + CFG_port };
    }

    /**
    *** Return an array of "UDP port" property names
    *** @param name  The server name
    *** @return An array of "UDP port" property names
    **/
    public static String[] CONFIG_UDP_PORT(String name)
    {
        return new String[] { name + CFG_udpPort, name + CFG_port };
    }

    /**
    *** Return an array of "Command port" property names
    *** @param name  The server name
    *** @return An array of "Command port" property names
    **/
    public static String[] CONFIG_COMMAND_PORT(String name)
    {
        return new String[] { name + CFG_commandPort };
    }

    /**
    *** Return an array of "Command Protocol" property names
    *** Return command protocol to used when communicating with remote devices
    *** @param name  The server name
    *** @return An array of "Command Protocol" property names
    **/
    public static String[] CONFIG_COMMAND_PROTOCOL(String name)
    {
        return new String[] { name + CFG_commandProtocol };
    }

    /**
    *** Return an array of "Client Command port" property names
    *** @param name  The server name
    *** @return An array of "Client Command port" property names
    **/
    public static String[] CONFIG_CLIENT_COMMAND_PORT_UDP(String name)
    {
        return new String[] { name + CFG_clientCommandPort_udp, name + CFG_clientCommandPort };
    }

    /**
    *** Return an array of "Client Command port" property names
    *** @param name  The server name
    *** @return An array of "Client Command port" property names
    **/
    public static String[] CONFIG_CLIENT_COMMAND_PORT_TCP(String name)
    {
        return new String[] { name + CFG_clientCommandPort_tcp, name + CFG_clientCommandPort };
    }

    /**
    *** Return an array of "ACK Response port" property names
    *** @param name  The server name
    *** @return An array of "ACK Response port" property names
    **/
    public static String[] CONFIG_ACK_RESPONSE_PORT(String name)
    {
        return new String[] { name + CFG_ackResponsePort };
    }

    /**
    *** Return an array of "TCP idle timeout" property names
    *** @param name  The server name
    *** @return An array of "TCP idle timeout" property names
    **/
    public static String[] CONFIG_TCP_IDLE_TIMEOUT(String name)
    {
        return new String[] { name + CFG_tcpIdleTimeoutMS };  // int
    }

    /**
    *** Return an array of "TCP packet timeout" property names
    *** @param name  The server name
    *** @return An array of "TCP packet timeout" property names
    **/
    public static String[] CONFIG_TCP_PACKET_TIMEOUT(String name)
    {
        return new String[] { name + CFG_tcpPacketTimeoutMS };
    }

    /**
    *** Return an array of "TCP session timeout" property names
    *** @param name  The server name
    *** @return An array of "TCP session timeout" property names
    **/
    public static String[] CONFIG_TCP_SESSION_TIMEOUT(String name)
    {
        return new String[] { name + CFG_tcpSessionTimeoutMS };
    }
    /**
    *** Return an array of "UDP idle timeout" property names
    *** @param name  The server name
    *** @return An array of "UDP idle timeout" property names
    **/
    public static String[] CONFIG_UDP_IDLE_TIMEOUT(String name)
    {
        return new String[] { name + CFG_udpIdleTimeoutMS };  // int
    }

    /**
    *** Return an array of "UDP packet timeout" property names
    *** @param name  The server name
    *** @return An array of "UDP packet timeout" property names
    **/
    public static String[] CONFIG_UDP_PACKET_TIMEOUT(String name)
    {
        return new String[] { name + CFG_udpPacketTimeoutMS };  // int
    }

    /**
    *** Return an array of "UDP session timeout" property names
    *** @param name  The server name
    *** @return An array of "UDP session timeout" property names
    **/
    public static String[] CONFIG_UDP_SESSION_TIMEOUT(String name)
    {
        return new String[] { name + CFG_udpSessionTimeoutMS };  // int
    }

    /**
    *** Return an array of UniquID prefix property names
    *** @param name  The server name
    *** @return An array of UniquID prefix property names
    **/
    public static String[] CONFIG_UNIQUE_PREFIX(String name)
    {
        return new String[] { name + CFG_uniquePrefix, name + CFG_uniqueIdPrefix };
    }

    /**
    *** Return an array of "Minimum Moved Meters" property names
    *** @param name  The server name
    *** @return An array of "Minimum Moved Meters" names
    **/
    public static String[] CONFIG_MIN_MOVED_METERS(String name)
    {
        return new String[] { name + CFG_minimumMovedMeters };
    }

    /**
    *** Return an array of "Minimum SpeedKPH" property names
    *** @param name  The server name
    *** @return An array of "Minimum SpeedKPH" names
    **/
    public static String[] CONFIG_MIN_SPEED_KPH(String name)
    {
        return new String[] { name + CFG_minimumSpeedKPH };
    }

    /**
    *** Return an array of "Estimate Odometer" property names
    *** @param name  The server name
    *** @return An array of "Estimate Odometer" names
    **/
    public static String[] CONFIG_ESTIMATE_ODOM(String name)
    {
        return new String[] { name + CFG_estimateOdometer };
    }

    /**
    *** Return an array of "Simulate Geozone Arrival/Departure" property names
    *** @param name  The server name
    *** @return An array of "Simulate Geozone Arrival/Departure" names
    **/
    public static String[] CONFIG_SIMEVENT_GEOZONE(String name)
    {
        return new String[] { name + CFG_simulateGeozones };
    }

    /**
    *** Return an array of "Simulate Digital Inputs" property names
    *** @param name  The server name
    *** @return An array of "Simulate Digital Inputs" names
    **/
    public static String[] CONFIG_SIMEVENT_DIGITAL_INPUT(String name)
    {
        return new String[] { name + CFG_simulateDigitalInputs };
    }
    /**
    *** Return an array of "Start/Stop StatusCode supported" property names
    *** @param name  The server name
    *** @return An array of "Start/Stop StatusCode supported" property names
    **/
    public static String[] CONFIG_START_STOP_SUPPORTED(String name)
    {
        return new String[] { name + CFG_startStopSupported };
    }

    /**
    *** Return an array of "Save Raw Data Packet" property names
    *** @param name  The server name
    *** @return An array of "Save Raw Data Packet" property names
    **/
    public static String[] CONFIG_SAVE_RAW_DATA_PACKETS(String name)
    {
        return new String[] { name + CFG_saveRawDataPackets };
    }

    /**
    *** Return an array of "Status Location/InMotion Translation" property names
    *** @param name  The server name
    *** @return An array of "Status Location/InMotion Translation" property names
    **/
    public static String[] CONFIG_STATUS_LOCATION_IN_MOTION(String name)
    {
        return new String[] { name + CFG_statusLocationInMotion };
    }

    /**
    *** Return an array of "se Last Valid GPS Location" property names
    *** @param name  The server name
    *** @return An array of "se Last Valid GPS Location" property names
    **/
    public static String[] CONFIG_USE_LAST_VALID_GPS(String name)
    {
        return new String[] { name + CFG_useLastValidGPSLocation };
    }

    /**
    *** Return an array of "Initial Packet" property names
    *** @param name  The server name
    *** @return An array of "Initial Packet" property names
    **/
    public static String[] CONFIG_INITIAL_PACKET(String name)
    {
        return new String[] { name + CFG_initialPacket };
    }

    /**
    *** Return an array of "Final Packet" property names
    *** @param name  The server name
    *** @return An array of "Final Packet" property names
    **/
    public static String[] CONFIG_FINAL_PACKET(String name)
    {
        return new String[] { name + CFG_finalPacket };
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // test/debug entry point

    private static final String ARG_LIST[]          = new String[] { "list"             };
    private static final String ARG_LOOKUP[]        = new String[] { "lookup"  , "find" };
    private static final String ARG_SERVER[]        = new String[] { "server"  , "dcs"  };
    private static final String ARG_ACCOUNT[]       = new String[] { "account" , "acct" };
    private static final String ARG_DEVICE[]        = new String[] { "device"  , "dev"  };
    private static final String ARG_CMDTYPE[]       = new String[] { "cmdType" , "ct"   };
    private static final String ARG_CMDNAME[]       = new String[] { "cmdName" , "cn"   };
    private static final String ARG_ARG[]           = new String[] { "arg"     , "a"    };

    /**
    *** Command-Line usage
    **/
    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + DCServerFactory.class.getName() + " {options}");
        Print.logInfo("'Lookup' Options:");
        Print.logInfo("  -lookup=<mobileID>      A device mobile-id");
        Print.logInfo("'Send' Options:");
        Print.logInfo("  -server=<serverID>      The DCS id");
        Print.logInfo("  -account=<accountID>    The account id");
        Print.logInfo("  -device=<deviceID>      The device id");
        Print.logInfo("  -cmdType=<command>      The command [send|query|ping|output|config]");
        Print.logInfo("  -cmdName=<name>         The command name (if any)");
        Print.logInfo("  -arg=<arg>              The command argument");
        System.exit(1);
    }
    
    /**
    *** Command-line main entry point
    **/
    public static void main(String args[])
    {
        DBConfig.cmdLineInit(args, true);
        String server    = RTConfig.getString(ARG_SERVER , "");
        String accountID = RTConfig.getString(ARG_ACCOUNT, "");
        String deviceID  = RTConfig.getString(ARG_DEVICE , "");
        String cmdType   = RTConfig.getString(ARG_CMDTYPE, "");
        String cmdName   = RTConfig.getString(ARG_CMDNAME, "");
        String cmdArg0   = RTConfig.getString(ARG_ARG    , "");
        
        /* list */
        if (RTConfig.hasProperty(ARG_LIST)) {
            java.util.List<DCServerConfig> list = getServerConfigList(true);
            for (DCServerConfig dcs : list) {
                Print.sysPrintln(dcs.getName() + ":");
                File jarPath[] = dcs.getRunningJarPath(); 
                if (!ListTools.isEmpty(jarPath)) {
                    if (jarPath.length == 1) {
                        Print.sysPrintln("  Jar: " + jarPath[0]);
                        File logPath = DCServerConfig.getLogFilePath(jarPath[0]);
                        Print.sysPrintln("  Log: " + logPath + (logPath.isFile()?"  [exists]":"  [not-found]"));
                    } else {
                        for (int i = 0; i < jarPath.length; i++) {
                            Print.sysPrintln("  "+(i+1)+") Jar: " + jarPath[i]);
                            File logPath = DCServerConfig.getLogFilePath(jarPath[i]);
                            Print.sysPrintln("     Log: " + logPath + (logPath.isFile()?"  [exists]":"  [not-found]"));
                        }
                    }
                } else {
                    Print.sysPrintln("  not running");
                }
            }
            System.exit(0);
        }
        
        /* lookup IMEI# */
        if (RTConfig.hasProperty(ARG_LOOKUP)) {
            String uid = RTConfig.getString(ARG_LOOKUP,null);
            if (!StringTools.isBlank(uid)) {
                Device dev[] = DCServerFactory.lookupUniqueID(uid);
                Print.sysPrintln("");
                Print.sysPrintln("Device UniqueID Lookup:");
                if (!ListTools.isEmpty(dev)) {
                    for (Device d : dev) {
                        Print.sysPrintln("   Found: ["+d.getUniqueID()+"] " + d.getAccountID() + "/" + d.getDeviceID());
                    }
                } else {
                    Print.sysPrintln("   None Found");
                }
                Print.sysPrintln("");
                System.exit(0);
            } else {
                Print.sysPrintln("Missing IMEI/Mobile ID");
                usage();
            }
        }

        /* server blank? */
        if (StringTools.isBlank(server)) {
            Print.sysPrintln("ERROR: server missing");
            usage();
        }

        /* blank account/device? */
        if (StringTools.isBlank(accountID) || StringTools.isBlank(deviceID)) {
            Print.sysPrintln("ERROR: account/device missing");
            usage();
        }
        
        /* command blank? */
        if (StringTools.isBlank(cmdType)) {
            Print.sysPrintln("ERROR: command type missing");
            usage();
        }
        
        /* pre-check DCS existance */
        Print.logDebug("Checking DCServerConfig existance: %s", server);
        DCServerConfig dcs = DCServerFactory.getServerConfig(server);
        if (server == null) {
            Print.sysPrintln("ERROR: Invalid server id: %s", server);
            usage();
        }
        
        /* send */
        String cmdArgs[] = new String[] { cmdArg0 };
        RTProperties resp = DCServerFactory._sendServerCommand(server, accountID, deviceID, cmdType, cmdName, cmdArgs);
        if (resp == null) {
            Print.sysPrintln("Unable to send command");
            System.exit(2);
        }
        Print.sysPrintln("Command Response: " + resp);
        
    }
    // ------------------------------------------------------------------------

}
    
