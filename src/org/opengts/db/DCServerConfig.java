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
//  Device Communication Server configuration (central registry for port usage)
// ----------------------------------------------------------------------------
// Change History:
//  2009/04/02  Martin D. Flynn
//     -Initial release
//  2009/07/01  Martin D. Flynn
//     -Added support for sending commands to the appropriate DCS.
//  2009/08/23  Martin D. Flynn
//     -Added several additional common runtime property methods.
//  2009/09/23  Martin D. Flynn
//     -Changed 'getSimulateDigitalInputs' to return a mask
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

public class DCServerConfig
    implements Comparable
{

    // ------------------------------------------------------------------------
    // flags
    
    // Boolean Properties
    public static final String  P_NONE                          = "none";
    public static final String  P_HAS_INPUTS                    = "hasInputs";
    public static final String  P_HAS_OUTPUTS                   = "hasOutputs";
    public static final String  P_COMMAND_SMS                   = "commandSms";
    public static final String  P_COMMAND_UDP                   = "commandUdp";
    public static final String  P_COMMAND_TCP                   = "commandTcp";
    public static final String  P_XMIT_TCP                      = "transmitTcp";
    public static final String  P_XMIT_UDP                      = "transmitUdp";
    public static final String  P_XMIT_SMS                      = "transmitSms";
    public static final String  P_XMIT_SAT                      = "transmitSat";
    public static final String  P_JAR_OPTIONAL                  = "jarOptional";

    public static final long    F_NONE                          = 0x00000000L;
    public static final long    F_HAS_INPUTS                    = 0x00000002L; // hasInputs
    public static final long    F_HAS_OUTPUTS                   = 0x00000004L; // hasOutputs
    public static final long    F_COMMAND_TCP                   = 0x00000100L; // commandTcp
    public static final long    F_COMMAND_UDP                   = 0x00000200L; // commandUdp
    public static final long    F_COMMAND_SMS                   = 0x00000400L; // commandSms
    public static final long    F_XMIT_TCP                      = 0x00001000L; // transmitTcp
    public static final long    F_XMIT_UDP                      = 0x00002000L; // transmitUdp
    public static final long    F_XMIT_SMS                      = 0x00004000L; // transmitSms
    public static final long    F_XMIT_SAT                      = 0x00008000L; // transmitSat
    public static final long    F_JAR_OPTIONAL                  = 0x00010000L; // jarOptional
    
    public static final long    F_STD_VEHICLE                   = F_HAS_INPUTS | F_HAS_OUTPUTS | F_XMIT_TCP | F_XMIT_UDP;
    public static final long    F_STD_PERSONAL                  = F_XMIT_TCP | F_XMIT_UDP;
    
    public static long GetAttributeFlags(RTProperties rtp)
    {
        long flags = 0L;
        if (rtp.getBoolean(P_HAS_INPUTS  ,false)) { flags |= F_HAS_INPUTS  ; }
        if (rtp.getBoolean(P_HAS_OUTPUTS ,false)) { flags |= F_HAS_OUTPUTS ; }
        if (rtp.getBoolean(P_COMMAND_SMS ,false)) { flags |= F_COMMAND_SMS ; }
        if (rtp.getBoolean(P_COMMAND_UDP ,false)) { flags |= F_COMMAND_UDP ; }
        if (rtp.getBoolean(P_COMMAND_TCP ,false)) { flags |= F_COMMAND_TCP ; }
        if (rtp.getBoolean(P_XMIT_TCP    ,false)) { flags |= F_XMIT_TCP    ; }
        if (rtp.getBoolean(P_XMIT_UDP    ,false)) { flags |= F_XMIT_UDP    ; }
        if (rtp.getBoolean(P_XMIT_SMS    ,false)) { flags |= F_XMIT_SMS    ; }
        if (rtp.getBoolean(P_XMIT_SAT    ,false)) { flags |= F_XMIT_SAT    ; }
        if (rtp.getBoolean(P_JAR_OPTIONAL,false)) { flags |= F_JAR_OPTIONAL; }
        return flags;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public enum CommandProtocol implements EnumTools.IsDefault, EnumTools.IntValue {
        UDP(0,"udp"),
        TCP(1,"tcp"),
        SMS(2,"sms");
        // ---
        private int         vv = 0;
        private String      ss = "";
        CommandProtocol(int v, String s) { vv = v; ss = s; }
        public int getIntValue()   { return vv; }
        public String toString()   { return ss; }
        public boolean isDefault() { return this.equals(UDP); }
    };

    /* get the CommandProtocol Enum valud, based on the value of the specified String */
    public static CommandProtocol getCommandProtocol(String v)
    {
        // returns 'null' if protocol value is invalid
        return EnumTools.getValueOf(CommandProtocol.class, v);
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Device event code to status code translation
    **/
    public static class EventCode
    {
        private int    oldCode     = 0;
        private int    statusCode  = StatusCodes.STATUS_NONE;
        private String dataString  = null;
        private long   dataLong    = Long.MIN_VALUE;
        public EventCode(int oldCode, int statusCode, String data) {
            this.oldCode    = oldCode;
            this.statusCode = statusCode;
            this.dataString = data;
            this.dataLong   = StringTools.parseLong(data, Long.MIN_VALUE);
        }
        public int getCode() {
            return this.oldCode;
        }
        public int getStatusCode() {
            return this.statusCode;
        }
        public String getDataString(String dft) {
            return !StringTools.isBlank(this.dataString)? this.dataString : dft;
        }
        public long getDataLong(long dft) {
            return (this.dataLong != Long.MIN_VALUE)? this.dataLong : dft;
        }
        public String toString() {
            return StringTools.toHexString(this.getCode(),16) + " ==> 0x" + StringTools.toHexString(this.getStatusCode(),16);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* Perl 'psjava' command (relative to GTS_HOME) */
    private static final String PSJAVA_PERL  = File.separator + "bin" + File.separator + "psjava";

    /**
    *** Returns the "psjava" command relative to GTS_HOME
    *** @return The "psjava" command relative to GTS_HOME
    **/
    public static String getPSJavaCommand()
    {
        File psjava = FileTools.toFile(DBConfig.get_GTS_HOME(), new String[] {"bin","psjava"});
        if (psjava != null) {
            return psjava.toString();
        } else {
            return null;
        }
    }

    /**
    *** Returns the "psjava" command relative to GTS_HOME, and returning the 
    *** specified information for the named jar file
    *** @param name    The DCServerConfig name
    *** @param display The type of information to return ("pid", "name", "user")
    *** @return The returned 'display' information for the specified DCServerConfig
    **/
    public static String getPSJavaCommand_jar(String name, String display)
    {
        String psjava = DCServerConfig.getPSJavaCommand();
        if (!StringTools.isBlank(psjava)) {
            StringBuffer sb = new StringBuffer();
            sb.append(psjava);
            if (OSTools.isWindows()) {
                sb.append(" \"-jar=").append(name).append(".jar\"");
                if (!StringTools.isBlank(display)) {
                    sb.append(" \"-display="+display+"\"");
                }
            } else {
                sb.append(" -jar=").append(name).append(".jar");
                if (!StringTools.isBlank(display)) {
                    sb.append(" -display="+display+"");
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }
    
    /**
    *** Returns the file path for the named running DCServerConfig jar files.<br>
    *** This method will return 'null' if no DCServerConfig jar files with the specified
    *** name are currently running.<br>
    *** All matching running DCServerConfig entries will be returned.
    *** @param name  The DCServerConfig name
    *** @return The matching running jar file paths, or null if no matching server enteries are running.
    **/
    public static File[] getRunningJarPath(String name)
    {
        if (OSTools.isLinux() || OSTools.isMacOSX()) {
            try {
                String cmd = DCServerConfig.getPSJavaCommand_jar(name,"name");
                Process process = (cmd != null)? Runtime.getRuntime().exec(cmd) : null;
                if (process != null) {
                    BufferedReader procReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    for (;;) {
                        String line = procReader.readLine();
                        if (line == null) { break; }
                        sb.append(StringTools.trim(line));
                    }
                    process.waitFor();
                    procReader.close();
                    int exitVal = process.exitValue();
                    if (exitVal == 0) {
                        String jpath[] = StringTools.split(sb.toString(), '\n');
                        java.util.List<File> jpl = new Vector<File>();
                        for (int i = 0; i < jpath.length; i++) {
                            if (StringTools.isBlank(jpath[i])) { continue; }
                            File jarPath = new File(sb.toString());
                            try {
                                jpl.add(jarPath.getCanonicalFile());
                            } catch (Throwable th) {
                                jpl.add(jarPath);
                            }
                        }
                        if (!ListTools.isEmpty(jpl)) {
                            return jpl.toArray(new File[jpl.size()]);
                        }
                    }
                } else {
                    if (StringTools.isBlank(cmd)) {
                        Print.logWarn("Unable to create 'psjava' command for '"+name+"'");
                    } else {
                        Print.logError("Unable to execute command: " + cmd);
                    }
                }
            } catch (Throwable th) {
                Print.logException("Unable to determine if Tomcat is running:", th);
            }
            return null;
        } else {
            // not supported on Windows
            return null;
        }
    }

    /**
    *** Return log file paths from jar file paths
    **/
    public static File getLogFilePath(File jarPath)
    {
        if (jarPath != null) {
            // "/usr/local/GTS_1.2.3/build/lib/enfora.jar"
            String jarName = jarPath.getName();
            if (jarName.endsWith(".jar")) {
                String name = jarName.substring(0,jarName.length()-4);
                File logDir = new File(jarPath.getParentFile().getParentFile().getParentFile(), "logs");
                if (logDir.isDirectory()) {
                    return new File(logDir, name + ".log");
                }
            }
        }
        return null;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static class EventDataAnalogField
    {
        private int         index       = 0;
        private double      gain        = 1.0 / (double)(1L << 10);  // default 10-bit analog
        private double      offset      = 0.0;
        private DBField     dbField     = null;
        public EventDataAnalogField(int ndx, double gain, double offset) {
            this(ndx, gain, offset, null);
        }
        public EventDataAnalogField(int ndx, double gain, double offset, String fieldN) {
            this.index  = ndx;
            this.gain   = gain;
            this.offset = offset;
            this.setFieldName(fieldN);
            Print.logInfo("New AnalogField["+this.index+"]: " + this);
        }
        public EventDataAnalogField(int ndx, String gof, double dftGain, double dftOffset) {
            this.index  = ndx;
            String  v[] = StringTools.split(gof,',');
            this.gain   = (v.length >= 1)? StringTools.parseDouble(v[0],dftGain  ) : dftGain;
            this.offset = (v.length >= 2)? StringTools.parseDouble(v[1],dftOffset) : dftOffset;
            this.setFieldName((v.length >= 3)? StringTools.blankDefault(v[2],null) : null);
            Print.logInfo("New AnalogField["+this.index+"]: " + this);
        }
        public int getIndex() {
            return this.index;
        }
        public double getGain() {
            return this.gain;
        }
        public double getOffset() {
            return this.offset;
        }
        public double convert(double value) {
            return (value * this.gain) + this.offset;
        }
        public void setFieldName(String fieldN) {
            this.dbField = null;
            if (!StringTools.isBlank(fieldN)) {
                DBField dbf = EventData.getFactory().getField(fieldN);
                if (dbf == null) {
                    Print.logError("**** EventData analog field does not exist: " + fieldN);
                } else {
                    this.dbField = dbf;
                    Object val = this.getValueObject(0.0);
                    if (val == null) {
                        Print.logError("**** EventData field type not supported: " + fieldN);
                        this.dbField = null;
                    }
                }
            }
        }
        public DBField getDBField() {
            return this.dbField;
        }
        public String getFieldName() {
            return (this.dbField != null)? this.dbField.getName() : null;
        }
        public Object getValueObject(double value) {
            // DBField
            DBField dbf = this.getDBField();
            if (dbf == null) {
                return null;
            }
            // convert to type
            Class dbfc = dbf.getTypeClass();
            if (dbfc == String.class) {
                return String.valueOf(value);
            } else
            if ((dbfc == Integer.class) || (dbfc == Integer.TYPE)) {
                return new Integer((int)value);
            } else
            if ((dbfc == Long.class)    || (dbfc == Long.TYPE   )) {
                return new Long((long)value);
            } else
            if ((dbfc == Float.class)   || (dbfc == Float.TYPE  )) {
                return new Float((float)value);
            } else
            if ((dbfc == Double.class)  || (dbfc == Double.TYPE )) {
                return new Double(value);
            } else
            if ((dbfc == Boolean.class) || (dbfc == Boolean.TYPE)) {
                return new Boolean(value != 0.0);
            }
            return null;
        }
        public boolean setEventDataFieldValue(EventData evdb, long value) {
            return this.setEventDataFieldValue(evdb, (double)value);
        }
        public boolean setEventDataFieldValue(EventData evdb, double value) {
            if (evdb != null) {
                Object objVal = this.getValueObject(this.convert(value)); // null if no dbField
                if (objVal != null) {
                    String fn = this.getFieldName();
                    boolean ok = evdb.setFieldValue(fn, objVal);
                    Print.logInfo("Set AnalogField["+this.getIndex()+"]: "+fn+" ==> " + (ok?evdb.getFieldValue(fn):"n/a"));
                    return ok;
                }
            }
            return false;
        }
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("index="  ).append(this.getIndex());
            sb.append(" gain="  ).append(this.getGain());
            sb.append(" offset=").append(this.getOffset());
            sb.append(" field=" ).append(this.getFieldName());
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String  COMMAND_CONFIG                  = "config";     // arg=deviceCommandString
    public static final String  COMMAND_PING                    = "ping";       // arg=commandID
  //public static final String  COMMAND_OUTPUT                  = "output";     // arg=gpioOutputState
    public static final String  COMMAND_GEOZONE                 = "geozone";    // arg=""
    
    public static final String  DEFAULT_ARG_NAME                = "arg";

    public class Command
    {
        private String                  name        = null;
        private String                  desc        = null;
        private String                  types[]     = null;
        private String                  aclName     = "";
        private AclEntry.AccessLevel    aclDft      = AclEntry.AccessLevel.WRITE;
        private String                  cmdStr      = "";
        private boolean                 hasArgs     = false;
        private String                  cmdProto    = null;
        private String                  protoHandlr = null;
        private boolean                 expectAck   = false;
        private int                     cmdStCode   = StatusCodes.STATUS_NONE;
        private OrderedMap<String,CommandArg> argMap = null;
        public Command(
            String name, String desc, 
            String types[], String aclName, AclEntry.AccessLevel aclDft,
            String cmdStr, boolean hasArgs, Collection<CommandArg> cmdArgs,
            String cmdProtoH, boolean expectAck,
            int cmdStCode) {
            this.name      = StringTools.trim(name);
            this.desc      = StringTools.trim(desc);
            this.types     = (types != null)? types : new String[0];
            this.aclName   = StringTools.trim(aclName);
            this.aclDft    = (aclDft != null)? aclDft : AclEntry.AccessLevel.WRITE;
            this.cmdStr    = (cmdStr != null)? cmdStr : "";
            this.hasArgs   = hasArgs || (this.cmdStr.indexOf("${") >= 0);
            cmdProtoH  = StringTools.trim(cmdProtoH);
            if (cmdProtoH.indexOf(":") >= 0) {
                int p = cmdProtoH.indexOf(":");
                this.cmdProto    = StringTools.trim(cmdProtoH.substring(0,p));
                this.protoHandlr = StringTools.trim(cmdProtoH.substring(p+1));
            } else {
                this.cmdProto    = cmdProtoH;
                this.protoHandlr = null;
            }
            this.expectAck = expectAck;
            this.cmdStCode = (cmdStCode > 0)? cmdStCode : StatusCodes.STATUS_NONE;
            if (!ListTools.isEmpty(cmdArgs) && this.hasArgs) {
                this.argMap = new OrderedMap<String,CommandArg>();
                for (CommandArg arg : cmdArgs) {
                    arg.setCommand(this);
                    this.argMap.put(arg.getName(),arg);
                }
            }
        }
        public DCServerConfig getDCServerConfig() {
            return DCServerConfig.this;
        }
        public String getName() {
            return this.name; // not null
        }
        public String getDescription() {
            return this.desc; // not null
        }
        public String[] getTypes() {
            return this.types;
        }
        public boolean isType(String type) {
            return ListTools.contains(this.types, type);
        }
        public String getAclName() {
            return this.aclName; // not null
        }
        public AclEntry.AccessLevel getAclAccessLevelDefault() {
            return this.aclDft; // not null
        }
        public String getCommandString() {
            return this.cmdStr; // not null
        }
        public String getCommandString(String cmdArgs[]) {
            String cs = this.getCommandString();
            if (this.hasCommandArgs()) {
                final String args[] = (cmdArgs != null)? cmdArgs : new String[0];
                return StringTools.replaceKeys(cs, new StringTools.KeyValueMap() {
                    public String getKeyValue(String key, String notUsed) {
                        int argNdx = (Command.this.argMap != null)? Command.this.argMap.indexOfKey(key) : -1;
                        if ((argNdx >= 0) && (argNdx < args.length)) {
                            return (args[argNdx] != null)? args[argNdx] : "";
                        } else
                        if (key.equals(DEFAULT_ARG_NAME)) { 
                            return ((args.length > 0) && (args[0] != null))? args[0] : ""; 
                        } else {
                            for (int i = 0; i < args.length; i++) {
                                if (key.equals(DEFAULT_ARG_NAME+i)) { // "arg0", "arg1", ...
                                    return (args[i] != null)? args[i] : ""; 
                                }
                            }
                        }
                        return "";
                    }
                });
            }
            return cs;
        }
        public boolean hasCommandArgs() {
            //return (this.cmdStr != null)? (this.cmdStr.indexOf("${arg") >= 0) : false;
            return this.hasArgs;
        }
        public int getArgCount() {
            if (!this.hasArgs) {
                return 0;
            } else
            if (this.argMap != null) {
                return this.argMap.size();
            } else {
                return 1;
            }
        }
        public CommandArg getCommandArg(int argNdx) {
            if (this.hasArgs && (this.argMap != null)) {
                return this.argMap.getValue(argNdx);
            } else {
                return null;
            }
        }
        public CommandProtocol getCommandProtocol() {
            return this.getCommandProtocol(null);
        }
        public CommandProtocol getCommandProtocol(CommandProtocol dftProto) {
            if (!StringTools.isBlank(this.cmdProto)) {
                // may return null if cmdProto is not one of the valid values
                return DCServerConfig.getCommandProtocol(this.cmdProto);
            } else {
                return dftProto;
            }
        }
        public String getCommandProtocolHandler() {
            return this.protoHandlr; // may be null
        }
        public boolean getExpectAck() {
            return this.expectAck;
        }
        public int getStatusCode() {
            return this.cmdStCode;
        }
    }

    public static class CommandArg
    { // static because the 'Args' are initialized before the 'Command' is
        private Command command   = null;   // The command that owns this arg
        private String  name      = null;   // This argument name
        private String  desc      = null;   // This argument description
        private boolean readOnly  = false;
        private String  resKey    = null;
        private String  dftVal    = "";
        private int     lenDisp   = 70;
        private int     lenMax    = 500;
        public CommandArg(String name, String desc, boolean readOnly, String resKey, String dftVal) {
            this.name      = StringTools.trim(name);
            this.desc      = StringTools.trim(desc);
            this.readOnly  = readOnly;
            this.resKey    = !StringTools.isBlank(resKey)? resKey : null;
            this.dftVal    = StringTools.trim(dftVal);
        }
        public String getName() {
            return this.name;
        }
        public String getDescription() {
            return this.desc;
        }
        public boolean isReadOnly() {
            return this.readOnly;
        }
        public void setCommand(Command cmd) {
            this.command = cmd;
        }
        public Command getCommand() {
            return this.command;
        }
        public String getResourceName() {
            return this.resKey;
            // ie. "DCServerConfig.enfora.DriverMessage.arg"
            /*
            StringBuffer sb = new StringBuffer();
            sb.append("DCServerConfig.");
            sb.append(cmd.getDCServerConfig().getName());
            sb.append(".");
            sb.append(cmd.getName());
            sb.append(".");
            sb.append(this.getName());
            return sb.toString();
            */
        }
        public String getDefaultValue() {
            return this.dftVal;
        }
        public void setLength(int dispLen, int maxLen) {
            this.lenDisp = (dispLen > 0)? dispLen : 70;
            this.lenMax  = (maxLen  > 0)? maxLen  : (this.lenDisp * 2);
        }
        public int getDisplayLength() {
            return this.lenDisp;
        }
        public int getMaximumLength() {
            return this.lenMax;
        }
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Send SMS command to device
    *** @param handlerName  The name of the SMS gateway handler
    *** @param device       The device to which the SMS command is sent
    *** @param commandStr   The SMS command which is sent
    *** @return The ResultCode
    **/
    public static DCServerFactory.ResultCode SendSMSCommand(String handlerName, Device device, String commandStr)
    {

        /* default handler name */
        String smsHandler = StringTools.blankDefault(handlerName,"body");

        /* get handler */
        SMSOutboundGateway smsgw = SMSOutboundGateway.GetSMSGateway(smsHandler);
        if (smsgw == null) {
            Print.logError("SMS gateway handler not found: " + smsHandler);
            return DCServerFactory.ResultCode.INVALID_PROTO;
        }

        /* send */
        return smsgw.sendSMSCommand(device, commandStr);

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String                          dcName                  = "";
    private String                          dcDesc                  = "";

    private String                          uniquePrefix[]          = null;

    private OrderedMap<Integer,InetAddress> tcpPortMap              = null;
    private OrderedMap<Integer,InetAddress> udpPortMap              = null;

    private boolean                         customCodeEnabled       = true;
    private Map<Integer,EventCode>          customCodeMap           = new HashMap<Integer,EventCode>();

    private String                          commandHost             = null;
    private int                             commandPort             = 0;
    private CommandProtocol                 commandProtocol         = null;

    private long                            attrFlags               = F_NONE;

    private RTProperties                    rtProps                 = new RTProperties();
    
    private String                          commandsAclName         = null;
    private AclEntry.AccessLevel            commandsAccessLevelDft  = null;

    private OrderedMap<String,Command>      commandMap              = null;
    
    /**
    *** Blank Constructor
    **/
    public DCServerConfig()
    {
        this.setName("unregistered");
        this.setDescription("Unregistered DCS");
        this.setAttributeFlags(F_NONE);
        this.setTcpPorts(null, null, false);
        this.setUdpPorts(null, null, false);
        this.setCommandDispatcherPort(0);
        this.setUniquePrefix(null);
        this._postInit();
    }

    /**
    *** Constructor
    **/
    public DCServerConfig(String name, String desc, int tcpPorts[], int udpPorts[], int commandPort, long flags, String... uniqPfx)
    {
        this.setName(name);
        this.setDescription(desc);
        this.setAttributeFlags(flags);
        this.setTcpPorts(null, tcpPorts, true);
        this.setUdpPorts(null, udpPorts, true);
        this.setCommandDispatcherPort(commandPort, true);
        this.setUniquePrefix(uniqPfx);
        this._postInit();
    }

    private void _postInit()
    {
        //
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Sets the server name/id
    **/
    protected void setName(String n)
    {
        this.dcName = StringTools.trim(n);
    }

    /**
    *** Gets the server name/id
    **/
    public String getName()
    {
        return this.dcName;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server description
    **/
    public void setDescription(String d)
    {
        this.dcDesc = StringTools.trim(d);
    }

    /**
    *** Gets the server description
    **/
    public String getDescription()
    {
        return this.dcDesc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the server attribute flags
    **/
    public void setAttributeFlags(long f)
    {
        this.attrFlags = f;
    }

    /**
    *** Gets the server attribute flags
    **/
    public long getAttributeFlags()
    {
        return this.attrFlags;
    }

    /**
    *** Returns true if the indicate mask is non-zero
    **/
    public boolean isAttributeFlag(long mask)
    {
        return ((this.getAttributeFlags() & mask) != 0L);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets an array of server ports from the specified runtime keys.
    *** (first check command-line, then config file, then default) 
    *** @param name The server name
    *** @param argKey  The runtime key names
    *** @param dft  The default array of server ports if not defined otherwise
    *** @return The array of server ports
    **/
    private int[] getServerPorts(String argKey[], int dft[])
    {
        String name = this.getName();
        String ak[] = this.normalizeKeys(argKey);

        /* no port override? */
        if (!this.hasProperty(ak,false)) { // exclude 'defaults'
            return dft;
        }

        /* get overridden ports */
        String portStr[] = this.getStringArrayProperty(ak, null);
        if (ListTools.isEmpty(portStr)) {
            // ports explicitly removed
            //Print.logInfo(name + ": Returning 'null' ports");
            return null;
        }

        /* parse/return port numbers */
        int p = 0;
        int srvPorts[] = new int[portStr.length];
        for (int i = 0; i < portStr.length; i++) {
            int port = StringTools.parseInt(portStr[i], 0);
            if (ServerSocketThread.isValidPort(port)) {
                srvPorts[p++] = port;
            }
        }
        if (p < srvPorts.length) {
            // list contains invalid port numbers
            int newPorts[] = new int[p];
            System.arraycopy(srvPorts, 0, newPorts, 0, p);
            srvPorts = newPorts;
        }
        if (!ListTools.isEmpty(srvPorts)) {
            //Print.logInfo(name + ": Returning server ports: " + StringTools.join(srvPorts,","));
            return srvPorts;
        } else {
            //Print.logInfo(name + ": Returning 'null' ports");
            return null;
        }

    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default TCP port for this server
    **/
    public void setTcpPorts(InetAddress bindAddr, int tcp[], boolean checkRTP)
    {
        if (checkRTP) {
            tcp = this.getServerPorts(DCServerFactory.CONFIG_TCP_PORT(this.getName()), tcp);
        }
        if (!ListTools.isEmpty(tcp)) {
            if (this.tcpPortMap == null) { this.tcpPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < tcp.length; i++) {
                Integer p = new Integer(tcp[i]);
                if (this.tcpPortMap.containsKey(p)) {
                    Print.logWarn("TCP port already defined ["+this.getName()+"]: " + p);
                }
                this.tcpPortMap.put(p, bindAddr);
            }
        }
    }
    
    /**
    *** Get TCP Port bind address
    **/
    public InetAddress getTcpPortBindAddress(int port)
    {
        InetAddress bind = (this.tcpPortMap != null)? this.tcpPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default TCP port for this server
    **/
    public int[] getTcpPorts()
    {
        if (ListTools.isEmpty(this.tcpPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.tcpPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.tcpPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create TCP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_TCP(int port)
        throws SocketException, IOException
    {
        InetAddress bindAddr = this.getUdpPortBindAddress(port);
        return new ServerSocketThread(bindAddr, port);
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the default UDP port for this server
    **/
    public void setUdpPorts(InetAddress bindAddr, int udp[], boolean checkRTP)
    {
        if (checkRTP) {
            udp = this.getServerPorts(DCServerFactory.CONFIG_UDP_PORT(this.getName()), udp);
        }
        if (!ListTools.isEmpty(udp)) {
            if (this.udpPortMap == null) { this.udpPortMap = new OrderedMap<Integer,InetAddress>(); }
            for (int i = 0; i < udp.length; i++) {
                Integer p = new Integer(udp[i]);
                if (this.udpPortMap.containsKey(p)) {
                    Print.logWarn("UDP port already defined ["+this.getName()+"]: " + p);
                }
                this.udpPortMap.put(p, bindAddr);
                //Print.logInfo("Setting UDP listener at " + StringTools.blankDefault(bindAddr,"<ALL>") + " : " + p);
            }
        }
    }
    
    /**
    *** Get UDP Port bind address
    **/
    public InetAddress getUdpPortBindAddress(int port)
    {
        InetAddress bind = (this.udpPortMap != null)? this.udpPortMap.get(new Integer(port)) : null;
        return (bind != null)? bind : ServerSocketThread.getDefaultBindAddress();
    }

    /**
    *** Gets the default UDP port for this server
    **/
    public int[] getUdpPorts()
    {
        if (ListTools.isEmpty(this.udpPortMap)) {
            return null;
        } else {
            int ports[] = new int[this.udpPortMap.size()];
            for (int i = 0; i < ports.length; i++) {
                ports[i] = this.udpPortMap.getKey(i).intValue();
            }
            return ports;
        }
    }

    /**
    *** Create UDP ServerSocketThread
    **/
    public ServerSocketThread createServerSocketThread_UDP(int port)
        throws SocketException, IOException
    {
        InetAddress bindAddr = this.getUdpPortBindAddress(port);
        return new ServerSocketThread(ServerSocketThread.createDatagramSocket(bindAddr, port));
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of all TCP/UDP 'listen' ports
    *** @return An array of all TCP/UDP 'listen' ports
    **/
    public int[] getListenPorts()
    {
        if (ListTools.isEmpty(this.tcpPortMap)) {
            return this.getUdpPorts(); // may still be null/empty
        } else
        if (ListTools.isEmpty(this.udpPortMap)) {
            return this.getTcpPorts(); // may still be null/empty
        } else {
            java.util.List<Integer> portList = new Vector<Integer>();
            int tcpPorts[] = this.getTcpPorts();
            for (int t = 0; t < tcpPorts.length; t++) {
                Integer tcp = new Integer(tcpPorts[t]);
                portList.add(tcp); 
            }
            int udpPorts[] = this.getUdpPorts();
            for (int u = 0; u < udpPorts.length; u++) {
                Integer udp = new Integer(udpPorts[u]);
                if (!portList.contains(udp)) {
                    portList.add(udp);
                }
            }
            int ports[] = new int[portList.size()];
            for (int p = 0; p < portList.size(); p++) {
                ports[p] = portList.get(p).intValue();
            }
            return ports;
        }
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Load device record from unique-id (not yet used/tested)
    *** @param modemID The unique modem ID (IMEI, ESN, etc)
    *** @return The Device record
    **/
    public Device loadDeviceUniqueID(String modemID)
    {
        return DCServerFactory.loadDeviceUniqueID(this.getUniquePrefix(), modemID);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the named server is defined
    **/
    public boolean serverJarExists()
    {
        return DCServerFactory.serverJarExists(this.getName());
    }
    
    /**
    *** Returns true if this DCS requires a Jar file
    **/
    public boolean isJarOptional()
    {
        return this.isAttributeFlag(F_JAR_OPTIONAL);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the command protocol to use when communicating with remote devices
    *** @param proto  The CommandProtocol
    **/
    public void setCommandProtocol(String proto)
    {
        this.commandProtocol = DCServerConfig.getCommandProtocol(proto);
    }

    /**
    *** Sets the command protocol to use when communicating with remote devices
    *** @param proto  The CommandProtocol
    **/
    public void setCommandProtocol(CommandProtocol proto)
    {
        this.commandProtocol = proto;
    }

    /**
    *** Gets the command protocol to use when communicating with remote devices
    *** @return The Command Protocol
    **/
    public CommandProtocol getCommandProtocol()
    {
        return (this.commandProtocol != null)? this.commandProtocol : CommandProtocol.UDP;
    }

    /**
    *** Gets the "Client Command Port" 
    *** @param dft  The default Client Command Port
    *** @return The Client Command Port
    **/
    public int getClientCommandPort_udp(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_CLIENT_COMMAND_PORT_UDP(this.getName()), dft);
    }

    /**
    *** Gets the "Client Command Port" 
    *** @param dft  The default Client Command Port
    *** @return The Client Command Port
    **/
    public int getClientCommandPort_tcp(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_CLIENT_COMMAND_PORT_TCP(this.getName()), dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the "ACK Response Port" 
    *** @param dft  The ACK response port
    *** @return The ack response port
    **/
    public int getAckResponsePort(int dft)
    {
        return this.getIntProperty(DCServerFactory.CONFIG_ACK_RESPONSE_PORT(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "TCP idle timeout" 
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpIdleTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_TCP_IDLE_TIMEOUT(this.getName()), dft);
    }

    /**
    *** Gets the "TCP packet timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpPacketTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_TCP_PACKET_TIMEOUT(this.getName()), dft);
    }

    /**
    *** Gets the "TCP session timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getTcpSessionTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_TCP_SESSION_TIMEOUT(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "UDP idle timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpIdleTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_UDP_IDLE_TIMEOUT(this.getName()), dft);
    }

    /**
    *** Gets the "UDP packet timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpPacketTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_UDP_PACKET_TIMEOUT(this.getName()), dft);
    }

    /**
    *** Gets the "UDP session timeout"
    *** @param dft  The default timeout value
    *** @return The default timeout value
    **/
    public long getUdpSessionTimeoutMS(long dft)
    {
        return this.getLongProperty(DCServerFactory.CONFIG_UDP_SESSION_TIMEOUT(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the array of allowed UniqueID prefixes
    *** @return The array of allowed UniqueID prefixes
    **/
    public String[] getUniquePrefix()
    {
        if (ListTools.isEmpty(this.uniquePrefix)) {
            this.uniquePrefix = new String[] { "" };
        }
        return this.uniquePrefix;
    }

    /**
    *** Sets the array of allowed UniqueID prefixes
    *** @param pfx  The default UniqueID prefixes
    **/
    public void setUniquePrefix(String pfx[])
    {
        if (!ListTools.isEmpty(pfx)) {
            for (int i = 0; i < pfx.length; i++) {
                String p = pfx[i].trim();
                if (p.equals("<blank>") || p.equals("*")) { 
                    p = ""; 
                } else
                if (p.endsWith("*")) {
                    p = p.substring(0, p.length() - 1);
                }
                pfx[i] = p;
            }
            this.uniquePrefix = pfx;
        } else {
            this.uniquePrefix = new String[] { "" };;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Minimum Moved Meters"
    *** @param dft  The default minimum distance
    *** @return The Minimum Moved Meters
    **/
    public double getMinimumMovedMeters(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_MIN_MOVED_METERS(this.getName()), dft);
    }

    /**
    *** Gets the "Minimum Speed KPH"
    *** @param dft  The default minimum speed
    *** @return The Minimum Speed KPH
    **/
    public double getMinimumSpeedKPH(double dft)
    {
        return this.getDoubleProperty(DCServerFactory.CONFIG_MIN_SPEED_KPH(this.getName()), dft);
    }

    /**
    *** Gets the "Minimum Speed KPH"
    *** @param dft  The default minimum speed
    *** @return The Minimum Speed KPH
    **/
    public boolean getEstimateOdometer(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_ESTIMATE_ODOM(this.getName()), dft);
    }

    /**
    *** Gets the "Simulate Geozones"
    *** @param dft  The default Simulate Geozones state
    *** @return The Simulate Geozones
    **/
    public boolean getSimulateGeozones(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_SIMEVENT_GEOZONE(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Save Raw Data Packet" config
    *** @param dft  The default "Save Raw Data Packet" state
    *** @return The "Save Raw Data Packet" state
    **/
    public boolean getSaveRawDataPackets(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_SAVE_RAW_DATA_PACKETS(this.getName()), dft);
    }

    /**
    *** Gets the "Start/Stop StatusCode supported" config
    *** @param dft  The default "Start/Stop StatusCode supported" state
    *** @return The "Start/Stop StatusCode supported" state
    **/
    public boolean getStartStopSupported(boolean dft)
    {
        String n = this.getName();
        if (n.equals(DCServerFactory.OPENDMTP_NAME)) {
            return true;
        } else {
            return this.getBooleanProperty(DCServerFactory.CONFIG_START_STOP_SUPPORTED(this.getName()), dft);
        }
    }

    /**
    *** Gets the "Status Location/InMotion Translation" config
    *** @param dft  The default "Status Location/InMotion Translation" state
    *** @return The "Status Location/InMotion Translation" state
    **/
    public boolean getStatusLocationInMotion(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_STATUS_LOCATION_IN_MOTION(this.getName()), dft);
    }

    /**
    *** Gets the "Use Last Valid GPS Location" config
    *** @param dft  The default "Use Last Valid GPS Location" state
    *** @return The "Use Last Valid GPS Location" state
    **/
    public boolean getUseLastValidGPSLocation(boolean dft)
    {
        return this.getBooleanProperty(DCServerFactory.CONFIG_USE_LAST_VALID_GPS(this.getName()), dft);
    }

    // ------------------------------------------------------------------------

    /**
    *** Convenience for converting the initial/final packet to a byte array.
    *** If the string begins with "0x" the the remain string is assumed to be hex
    *** @return The byte array
    **/
    public static byte[] convertToBytes(String s)
    {
        if (s == null) {
            return null;
        } else
        if (s.startsWith("0x")) {
            byte b[] = StringTools.parseHex(s,null);
            if (b != null) {
                return b;
            } else {
                return null;
            }
        } else {
            return StringTools.getBytes(s);
        }
    }

    /**
    *** Gets the "Initial Packet" byte array
    *** @param dft  The default "Initial Packet" byte array
    *** @return The "Initial Packet" byte array
    **/
    public String getInitialPacket(String dft)
    {
        String s = this.getStringProperty(DCServerFactory.CONFIG_INITIAL_PACKET(this.getName()), null);
        return (s != null)? s : dft;
    }

    /**
    *** Gets the "Final Packet" byte array
    *** @param dft  The default "Final Packet" byte array
    *** @return The "Final Packet" byte array
    **/
    public String getFinalPacket(String dft)
    {
        String s = this.getStringProperty(DCServerFactory.CONFIG_FINAL_PACKET(this.getName()), null);
        return (s != null)? s : dft;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the "Simulate Geozones" mask
    *** @param dft  The default Simulate Geozones mask
    *** @return The Simulate Geozones mask
    **/
    public long getSimulateDigitalInputs(long dft)
    {
        String maskStr = this.getStringProperty(DCServerFactory.CONFIG_SIMEVENT_DIGITAL_INPUT(this.getName()), null);
        if (StringTools.isBlank(maskStr)) {
            // not specified (or blank)
            return dft;
        } else
        if (maskStr.equalsIgnoreCase("default")) {
            // explicit "default"
            return dft;
        } else
        if (maskStr.equalsIgnoreCase("true")) {
            // explicit "true"
            return 0xFFFFFFFFL;
        } else
        if (maskStr.equalsIgnoreCase("false")) {
            // explicit "false"
            return 0x00000000L;
        } else {
            // mask specified
            long mask = StringTools.parseLong(maskStr, -1L);
            return (mask >= 0L)? mask : dft;
        }
    }

    /**
    *** Returns true if this device supports digital inputs
    *** @return True if this device supports digital inputs
    **/
    public boolean hasDigitalInputs()
    {
        return this.isAttributeFlag(F_HAS_INPUTS);
    }

    /**
    *** Returns true if this device supports digital outputs
    *** @return True if this device supports digital outputs
    **/
    public boolean hasDigitalOutputs()
    {
        return this.isAttributeFlag(F_HAS_OUTPUTS);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the "Event Code Map Enable" config
    *** @param enabled  The "Event Code Map Enable" state
    **/
    public void setEventCodeEnabled(boolean enabled)
    {
        this.customCodeEnabled = enabled;
        //Print.logDebug("[" + this.getName() + "] EventCode translation enabled=" + this.customCodeEnabled);
    }

    /**
    *** Gets the "Event Code Map Enable" config
    *** @return The "Event Code Map Enable" state
    **/
    public boolean getEventCodeEnabled()
    {
        return this.customCodeEnabled;
    }
    
    /**
    **/
    public void setEventCodeMap(Map<Integer,EventCode> codeMap)
    {
        this.customCodeMap = (codeMap != null)? codeMap : new HashMap<Integer,EventCode>();
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(int code)
    {
        return this.customCodeEnabled? this.customCodeMap.get(new Integer(code)) : null;
    }

    /**
    *** Returns the EventCode instance for the specified code
    *** @param code  The code
    *** @return The EventCode
    **/
    public EventCode getEventCode(long code)
    {
        return this.customCodeEnabled? this.customCodeMap.get(new Integer((int)code)) : null;
    }

    /**
    *** Translates the specified device status code into a GTS status code
    *** @param code           The code to translate
    *** @param dftStatusCode  The default code returned if no translation is defined
    *** @return The translated GTS status code
    **/
    public int translateStatusCode(int code, int dftStatusCode)
    {
        EventCode sci = this.getEventCode(code);
        return (sci != null)? sci.getStatusCode() : dftStatusCode;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Sets the device command listen host (may be null to use default bind-address)
    *** @param cmdHost The device command listen host
    **/
    public void setCommandDispatcherHost(String cmdHost)
    {
        this.commandHost = cmdHost;
    }

    /**
    *** Gets the device command listen host
    *** @return The device command listen host
    **/
    public String getCommandDispatcherHost()
    {
        if (!StringTools.isBlank(this.commandHost)) {
            return this.commandHost;
        } else
        if (!StringTools.isBlank(DCServerFactory.BIND_ADDRESS)) {
            return DCServerFactory.BIND_ADDRESS;
        } else {
            String bindAddr = this.getStringProperty(DBConfig.PROP_dcs_bindInterface,null);
            return !StringTools.isBlank(bindAddr)? bindAddr : "localhost";
        }
    }

    /**
    *** Sets the device command listen port
    *** @param cmdPort  The device command listen port
    *** @param checkRTP True to allow the RTConfig propertiesto override this value
    **/
    public void setCommandDispatcherPort(int cmdPort, boolean checkRTP)
    {
        if (checkRTP) {
            cmdPort = this.getIntProperty(DCServerFactory.CONFIG_COMMAND_PORT(this.getName()), cmdPort);
        }
        this.commandPort = cmdPort;
    }

    /**
    *** Sets the device command listen port
    *** @param cmdPort  The device command listen port
    **/
    public void setCommandDispatcherPort(int cmdPort)
    {
        this.setCommandDispatcherPort(cmdPort,false);
    }

    /**
    *** Gets the device command listen port (returns '0' if not supported)
    *** @return The device command listen port
    **/
    public int getCommandDispatcherPort()
    {
        return this.commandPort;
    }

    // ------------------------------------------------------------------------

    /**
    *** Sets the Commands Acl name
    *** @param aclName The Commands Acl name
    **/
    public void setCommandsAclName(String aclName, AclEntry.AccessLevel dft)
    {
        this.commandsAclName        = StringTools.trim(aclName);
        this.commandsAccessLevelDft = dft;
    }

    /**
    *** Gets the Commands Acl name
    *** @return The Commands Acl name
    **/
    public String getCommandsAclName()
    {
        return this.commandsAclName;
    }

    /**
    *** Gets the Commands Acl AccessLevel default
    *** @return The Commands Acl AccessLevel default
    **/
    public AclEntry.AccessLevel getCommandsAccessLevelDefault()
    {
        return this.commandsAccessLevelDft;
    }

    /**
    *** Returns True if the specified user has access to the named command
    **/
    public boolean userHasAccessToCommand(BasicPrivateLabel privLabel, User user, String commandName)
    {

        /* BasicPrivateLabel must be specified */
        if (privLabel == null) {
            return false;
        }

        /* get command */
        Command command = this.getCommand(commandName);
        if (command == null) {
            return false;
        }

        /* has access to commands */
        if (privLabel.hasWriteAccess(user, this.getCommandsAclName())) {
            return false;
        }

        /* has access to specific command? */
        if (privLabel.hasWriteAccess(user, command.getAclName())) {
            return false;
        }

        /* access granted */
        return true;

    }
    
    // ------------------------------------------------------------------------
    
    public void addCommand(
        String cmdName, String cmdDesc, 
        String cmdTypes[], 
        String cmdAclName, AclEntry.AccessLevel cmdAclDft,
        String cmdString, boolean hasArgs, Collection<DCServerConfig.CommandArg> cmdArgList,
        String cmdProto, boolean expectAck,
        int cmdSCode)
    {
        if (StringTools.isBlank(cmdName)) {
            Print.logError("Ignoreing blank command name");
        } else
        if ((this.commandMap != null) && this.commandMap.containsKey(cmdName)) {
            Print.logError("Command already defined: " + cmdName);
        } else {
            Command cmd = new Command(
                cmdName, cmdDesc, 
                cmdTypes, 
                cmdAclName, cmdAclDft,
                cmdString, hasArgs, cmdArgList,
                cmdProto, expectAck,
                cmdSCode);
            if (this.commandMap == null) {
                this.commandMap = new OrderedMap<String,Command>();
            }
            this.commandMap.put(cmdName, cmd);
        }
    }

    public Command getCommand(String name)
    {
        return (this.commandMap != null)? this.commandMap.get(name) : null;
    }

    /**
    *** Gets the "Command List"
    *** @return The "Command List"
    **/
    public String[] getCommandList()
    {
        if (ListTools.isEmpty(this.commandMap)) {
            return null;
        } else {
            return this.commandMap.keyArray(String.class);
        }
    }

    /**
    *** Gets the "Command Description" for the specified command
    *** @param dft  The default "Command Description"
    *** @return The "Command Description" for the specified command
    **/
    public String getCommandDescription(String cmdName, String dft)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getDescription() : dft;
    }

    /**
    *** Gets the "Command String" for the specified command
    *** @param dft  The default "Command String"
    *** @return The "Command String" for the specified command
    **/
    public String getCommandString(String cmdName, String dft)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getCommandString() : dft;
    }

    /**
    *** Gets the status-code for the specified command.  An event with this 
    *** status code will be inserted into the EventData table when this command
    *** is sent to the device.
    *** @param code  The default status-code
    *** @return The status-code for the specified command
    **/
    public int getCommandStatusCode(String cmdName, int code)
    {
        Command cmd = this.getCommand(cmdName);
        return (cmd != null)? cmd.getStatusCode() : code;
    }

    /**
    *** Gets the command's (name,description) map
    *** @param type The description type 
    *** @return The command's (name,description) map
    **/
    public Map<String,Command> getCommandMap(BasicPrivateLabel privLabel, User user, String type)
    {
        boolean inclReplCmds = true; // for now, include all commands
        String cmdList[] = this.getCommandList();
        if (!ListTools.isEmpty(cmdList)) {
            Map<String,Command> cmdMap = new OrderedMap<String,Command>();
            for (Command cmd : this.commandMap.values()) {
                if (!DCServerFactory.isCommandTypeAll(type) && !cmd.isType(type)) {
                    // ignore this command
                    //Print.logInfo("Command '%s' is not property type '%s'", cmd.getName(), type);
                } else
                if ((privLabel != null) && !privLabel.hasWriteAccess(user,cmd.getAclName())) {
                    // user does not have access to this command
                    //Print.logInfo("User does not have access to command '%s'", cmd.getName());
                } else {
                    String key  = cmd.getName();
                    String desc = cmd.getDescription();
                    String cstr = cmd.getCommandString();
                    if (StringTools.isBlank(desc) && StringTools.isBlank(cstr)) {
                        // skip commands with blank description and commands
                        Print.logInfo("Command does not have a descripton, or command is blank");
                        continue;
                    } else
                    if (!inclReplCmds) {
                        if (cstr.indexOf("${") >= 0) { //}
                            // should not occur ('type' should not include commands that require parameters)
                            // found "${text}"
                            continue;
                        }
                    }
                    cmdMap.put(key,cmd);
                }
            }
            return cmdMap;
        } else {
            return null;
        }
    }

    /**
    *** Gets the command's (name,description) map
    *** @param type The description type 
    *** @return The command's (name,description) map
    **/
    public Map<String,String> getCommandDescriptionMap(BasicPrivateLabel privLabel, User user, String type)
    {
        Map<String,Command> cmdMap = this.getCommandMap(privLabel, user, type);
        if (!ListTools.isEmpty(cmdMap)) {
            Map<String,String> cmdDescMap = new OrderedMap<String,String>();
            for (Command cmd : cmdMap.values()) {
                String key  = cmd.getName();
                String desc = cmd.getDescription();
                cmdDescMap.put(key,desc); // Commands are pre-qualified
            }
            return cmdDescMap;
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public RTProperties getProperties()
    {
        return this.rtProps;
    }

    // ------------------------------------------------------------------------

    public String normalizeKey(String key)
    {
        if (StringTools.isBlank(key)) {
            return "";
        } else {
            String normKey = this.getName() + ".";
            if (key.startsWith(normKey)) {
                return key;
            } else {
                return normKey + key;
            }
        }
    }

    public String[] normalizeKeys(String key[])
    {
        if (!ListTools.isEmpty(key)) {
            for (int i = 0; i < key.length; i++) {
                key[i] = this.normalizeKey(key[i]);
            }
        }
        return key;
    }

    // ------------------------------------------------------------------------

    public boolean hasProperty(String key[], boolean inclDft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return true;
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return true;
            } else {
                return RTConfig.hasProperty(k, inclDft);
            }
        }
    }

    public Set<String> getPropertyKeys(String prefix)
    {
        RTProperties rtp = this.getProperties();
        Set<String> propKeys = new HashSet<String>();
        
        /* regualr keys */
        propKeys.addAll(rtp.getPropertyKeys(prefix));
        propKeys.addAll(RTConfig.getPropertyKeys(prefix));

        /* normalized keys */
        String pfx = this.normalizeKey(prefix);
        propKeys.addAll(rtp.getPropertyKeys(pfx));
        propKeys.addAll(RTConfig.getPropertyKeys(pfx));

        return propKeys;
    }

    // ------------------------------------------------------------------------

    public String[] getStringArrayProperty(String key, String dft[])
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getStringArray(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getStringArray(k, dft);
            } else {
                return RTConfig.getStringArray(k, dft);
            }
        }
    }

    public String[] getStringArrayProperty(String key[], String dft[])
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getStringArray(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getStringArray(k, dft);
            } else {
                return RTConfig.getStringArray(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public String getStringProperty(String key, String dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getString(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getString(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getString(k, dft);
            } else {
                // global original key
                return RTConfig.getString(key, dft);
            }
        }
    }

    public String getStringProperty(String key[], String dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getString(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                // local normalized key
                return rtp.getString(k, dft);
            } else
            if (RTConfig.hasProperty(k)) {
                // global normalized key
                return RTConfig.getString(k, dft);
            } else {
                // global original key
                return RTConfig.getString(key, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public int getIntProperty(String key, int dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getInt(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getInt(k, dft);
            } else {
                return RTConfig.getInt(k, dft);
            }
        }
    }

    public int getIntProperty(String key[], int dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getInt(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getInt(k, dft);
            } else {
                return RTConfig.getInt(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public long getLongProperty(String key, long dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getLong(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getLong(k, dft);
            } else {
                return RTConfig.getLong(k, dft);
            }
        }
    }

    public long getLongProperty(String key[], long dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getLong(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getLong(k, dft);
            } else {
                return RTConfig.getLong(k, dft);
            }
        }
    }
    
    // ------------------------------------------------------------------------

    public double getDoubleProperty(String key, double dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getDouble(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getDouble(k, dft);
            } else {
                return RTConfig.getDouble(k, dft);
            }
        }
    }

    public double getDoubleProperty(String key[], double dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getDouble(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getDouble(k, dft);
            } else {
                return RTConfig.getDouble(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------

    public boolean getBooleanProperty(String key, boolean dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getBoolean(key, dft);
        } else {
            String k = this.normalizeKey(key);
            if (rtp.hasProperty(k)) {
                return rtp.getBoolean(k, dft);
            } else {
                return RTConfig.getBoolean(k, dft);
            }
        }
    }

    public boolean getBooleanProperty(String key[], boolean dft)
    {
        RTProperties rtp = this.getProperties();
        if (rtp.hasProperty(key)) {
            return rtp.getBoolean(key, dft);
        } else {
            String k[] = this.normalizeKeys(key);
            if (rtp.hasProperty(k)) {
                return rtp.getBoolean(k, dft);
            } else {
                return RTConfig.getBoolean(k, dft);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    /**
    *** Returns the state of the indicated bit within the mask for this device type.
    *** @param mask  The input mask from the device
    *** @param bit   The bit to test
    **/
    public boolean getDigitalInputState(long mask, int bit)
    {
        int ofs = this.getIntProperty(DCServerFactory.PROP_Attribute_InputOffset, -1);
        int b   = (ofs > 0)? (ofs - bit) : bit;
        return (b >= 0)? ((mask & (1L << b)) != 0L) : false;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the 'other' DCServerCOnfig is equal to this DCServerConfig
    *** based on the name.
    *** @param other  The other DCServerConfig instance.
    *** @return True if the other DCServerConfig as the same name as this DCServerConfig
    **/
    public boolean equals(Object other)
    {
        if (other instanceof DCServerConfig) {
            String thisName  = this.getName();
            String otherName = ((DCServerConfig)other).getName();
            return thisName.equals(otherName);
        } else {
            return false;
        }
    }

    /**
    *** Compares another DCServerConfig instance to this instance.
    *** @param other  The other DCServerConfig instance.
    *** @return 'compareTo' operator on DCServerConfig names.
    **/
    public int compareTo(Object other)
    {
        if (other instanceof DCServerConfig) {
            String thisName  = this.getName();
            String otherName = ((DCServerConfig)other).getName();
            return thisName.compareTo(otherName);
        } else {
            return -1;
        }
    }

    /**
    *** Return hashCode based on the DCServerConfig name
    *** @return this.getNmae().hashCoe()
    **/
    public int hashCode()
    {
        return this.getName().hashCode();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a String representation of this instance
    *** @return A String representation
    **/
    public String toString()
    {
        return this.toString(true);
    }

    /**
    *** Returns a String representation of this instance
    *** @param inclName True to include the name in the returnsed String representation
    *** @return A String representation
    **/
    public String toString(boolean inclName)
    {
        // "(opendmtp) OpenDMTP Server [TCP=31000 UDP=31000 CMD=30050]
        StringBuffer sb = new StringBuffer();

        /* name/description */
        if (inclName) {
            sb.append("(").append(this.getName()).append(") ");
        }
        sb.append(this.getDescription()).append(" ");

        /* ports */
        sb.append("[");
        this.getPortsString(sb);
        sb.append("]");
        
        /* String representation */
        return sb.toString();

    }
    
    public StringBuffer getPortsString(StringBuffer sb)
    {
        if (sb == null) { sb = new StringBuffer(); }
        int p = 0;
        int tcp[] = this.getTcpPorts();
        if (!ListTools.isEmpty(tcp)) {
            if (p > 0) { sb.append(" "); }
            sb.append("TCP=" + StringTools.join(tcp,","));
            p++;
        }
        int udp[] = this.getUdpPorts();
        if (!ListTools.isEmpty(udp)) {
            if (p > 0) { sb.append(" "); }
            sb.append("UDP=" + StringTools.join(udp,","));
            p++;
        }
        int cmd = this.getCommandDispatcherPort();
        if (cmd > 0) {
            if (p > 0) { sb.append(" "); }
            sb.append("CMD=" + cmd);
            p++;
        }
        if (p == 0) {
            sb.append("no-ports");
        }
        return sb;
    }
    
    public String getPortsString()
    {
        return this.getPortsString(null).toString();
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Return running jar file path
    **/
    public File[] getRunningJarPath()
    {
        return DCServerConfig.getRunningJarPath(this.getName());
    }

}
