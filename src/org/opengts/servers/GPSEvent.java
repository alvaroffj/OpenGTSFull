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
//  2009/08/07  Martin D. Flynn
//     -Initial release
//  2009/08/23  Martin D. Flynn
//     -Fixed improper handling of speed/heading (thanks to Lyudmil Shoshorov 
//      for finding this issue).
// ----------------------------------------------------------------------------
package org.opengts.servers;

import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.sql.*;

import org.opengts.util.*;
import org.opengts.dbtools.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;

public class GPSEvent
    implements EventDataProvider, GeoPointProvider
{

    // ------------------------------------------------------------------------

    private DCServerConfig  server          = null;
    private RTProperties    fieldValues     = null;
    
    private Device          device          = null;
    private DataTransport   dataXPort       = null;
    
    private int             eventTotalCount = 0;

    /**
    *** Constructor 
    **/
    public GPSEvent(DCServerConfig server, String ipAddress, int clientPort, String modemID)
    {
        this.server      = server;
        this.fieldValues = new RTProperties();
        this._setDevice(this.loadDevice(modemID), ipAddress, clientPort);
    }

    /**
    *** Constructor 
    **/
    public GPSEvent(DCServerConfig server, String ipAddress, int clientPort, String acctID, String devID)
    {
        this.server      = server;
        this.fieldValues = new RTProperties();
        this._setDevice(this.loadDevice(acctID,devID), ipAddress, clientPort);
    }
    
    // ------------------------------------------------------------------------

    protected String getDeviceCode()
    {
        return (this.server != null)? this.server.getName() : "unknown";
    }
    
    protected Device loadDevice(String modemID)
    {
        if (this.server != null) {
            return this.server.loadDeviceUniqueID(modemID);
        } else {
            return DCServerFactory.loadDeviceUniqueID(null,modemID);
        }
    }

    protected Device loadDevice(String acctID, String devID)
    {
        if (StringTools.isBlank(acctID)) {
            return this.loadDevice(devID);
        } else {
            try {
                Account account = Account.getAccount(acctID);
                if (account == null) {
                    Print.logError("Account-ID not found: " + acctID);
                    return null;
                } else {
                    return Transport.loadDeviceByTransportID(account, devID);
                }
            } catch (DBException dbe) {
                Print.logError("Error getting Device: " + acctID + "/" + devID + " [" + dbe + "]");
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    protected boolean _setDevice(Device device, String ipAddress, int clientPort)
    {

        /* valid device? */
        if (device == null) {
            return false;
        }

        /* validate ID address */
        DataTransport dataXPort = device.getDataTransport();
        if ((ipAddress != null) && !dataXPort.isValidIPAddress(ipAddress)) {
            Print.logError("Invalid IPAddr: " + 
                device.getAccountID() + "/" + device.getDeviceID() + 
                " Found=" + ipAddress + 
                " Expect=" + dataXPort.getIpAddressValid());
            return false;
        }

        /* update device */
        this.device       = device;
        this.dataXPort    = dataXPort;
        this.dataXPort.setIpAddressCurrent(ipAddress);   // FLD_ipAddressCurrent
        this.dataXPort.setRemotePortCurrent(clientPort); // FLD_remotePortCurrent
        this.dataXPort.setDeviceCode(this.getDeviceCode()); // FLD_deviceCode
        this.device.setLastTotalConnectTime(DateTime.getCurrentTimeSec()); // FLD_lastTotalConnectTime

        /* ok */
        return true;

    }
    
    public boolean hasDevice()
    {
        return (this.device != null);
    }
    
    public Device getDevice()
    {
        return this.device;
    }

    public boolean updateDevice()
    {
        if (this.device != null) {
            /* save device changes */
            try {
                // TODO: check "this.device" vs "this.dataXPort"
                this.device.updateChangedEventFields();
                return true;
            } catch (DBException dbe) {
                Print.logException("Unable to update Device: " + 
                    this.getAccountID() + "/" + this.getDeviceID(), dbe);
            } finally {
                //
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public boolean insertEventData(long fixtime, int statusCode)
    {
        this.setTimestamp(fixtime);
        this.setStatusCode(statusCode);
        return this.insertEventData();
    }
    
    public boolean insertEventData()
    {

        /* valid device? */
        if (this.device == null) {
            return false;
        }
        
        /* debug message */
        if (RTConfig.isDebugMode()) {
            Print.logDebug("Inserting EventData ...\n" + this.toString());
        }

        /* EventData key */
        String acctID       = this.device.getAccountID();
        String devID        = this.device.getDeviceID();
        long   fixtime      = this.getTimestamp();
        int    statusCode   = this.getStatusCode();
        EventData.Key evKey = new EventData.Key(acctID, devID, fixtime, statusCode);
        EventData evdb      = evKey.getDBRecord();
        if (this.hasLatitude()      ) { evdb.setLatitude(      this.getLatitude());         }
        if (this.hasLongitude()     ) { evdb.setLongitude(     this.getLongitude());        }
        if (this.hasGpsAge()        ) { evdb.setGpsAge(        this.getGpsAge());           }
        if (this.hasSpeedKPH()      ) { evdb.setSpeedKPH(      this.getSpeedKPH());         }
        if (this.hasHeading()       ) { evdb.setHeading(       this.getHeading());          }
        if (this.hasAltitude()      ) { evdb.setAltitude(      this.getAltitude());         }
        if (this.hasInputMask()     ) { evdb.setInputMask(     this.getInputMask());        }
        if (this.hasBatteryLevel()  ) { evdb.setBatteryLevel(  this.getBatteryLevel());     }
        if (this.hasOdometerKM()    ) { evdb.setOdometerKM(    this.getOdometerKM());       }
        if (this.hasSatelliteCount()) { evdb.setSatelliteCount(this.getSatelliteCount());   }

        /* insert event */
        // this will display an error if it was unable to store the event
        Print.logInfo("Event     : [0x" + 
            StringTools.toHexString(statusCode,16) + "] " + 
            StatusCodes.GetDescription(statusCode,null));
        this.device.insertEventData(evdb); // FLD_lastValidLatitude,FLD_lastValidLongitude,FLD_lastGPSTimestamp,FLD_lastOdometerKM
        this.eventTotalCount++;
        return true;

     }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public String getUniqueID() 
    {
        return (this.device != null)? this.device.getUniqueID() : "";
    }

    public String getAccountID() 
    {
        return (this.device != null)? this.device.getAccountID() : "";
    }

    public String getDeviceID() 
    {
        return (this.device != null)? this.device.getDeviceID() : "";
    }

    // ------------------------------------------------------------------------

    public String getDeviceDescription() 
    {
        return (this.device != null)? this.device.getDescription() : "";
    }

    public String getDeviceVIN() 
    {
        return (this.device != null)? this.device.getDeviceVIN() : "";
    }

    // ------------------------------------------------------------------------

    public void setTimestamp(long timestamp) 
    {
        this.fieldValues.setLong(EventData.FLD_timestamp, timestamp);
    }
    
    public boolean hasTimestamp()
    {
        return this.fieldValues.hasProperty(EventData.FLD_timestamp);
    }

    public long getTimestamp() 
    {
        return this.fieldValues.getLong(EventData.FLD_timestamp, 0L);
    }

    // ------------------------------------------------------------------------

    public void setStatusCode(int code) 
    {
        this.fieldValues.setInt(EventData.FLD_statusCode, code);
    }
    
    public boolean hasStatusCode()
    {
        return this.fieldValues.hasProperty(EventData.FLD_statusCode);
    }

    public int getStatusCode() 
    {
        return this.fieldValues.getInt(EventData.FLD_statusCode, StatusCodes.STATUS_LOCATION);
    }

    // ------------------------------------------------------------------------

    public String getStatusCodeDescription(BasicPrivateLabel bpl) 
    {
        Device dev  = this.getDevice();
        int    code = this.getStatusCode();
        return StatusCode.getDescription(dev, code, bpl, null);
    }

    // ------------------------------------------------------------------------

    public int getPushpinIconIndex(String iconSelector, OrderedSet<String> iconKeys, 
        boolean isFleet, BasicPrivateLabel bpl) 
    {
        // not fully supported here
        return EventData.ICON_PUSHPIN_RED;
    }

    // ------------------------------------------------------------------------

    public void setLatitude(double lat) 
    {
        this.fieldValues.setDouble(EventData.FLD_latitude, lat);
    }
    
    public boolean hasLatitude()
    {
        return this.fieldValues.hasProperty(EventData.FLD_latitude);
    }

    public double getLatitude() 
    {
        return this.fieldValues.getDouble(EventData.FLD_latitude, 0.0);
    }

    public void setLongitude(double lon) 
    {
        this.fieldValues.setDouble(EventData.FLD_longitude, lon);
    }
    
    public boolean hasLongitude()
    {
        return this.fieldValues.hasProperty(EventData.FLD_longitude);
    }

    public double getLongitude() 
    {
        return this.fieldValues.getDouble(EventData.FLD_longitude, 0.0);
    }
    
    public void setGeoPoint(GeoPoint gp)
    {
        if ((gp == null) || !gp.isValid()) {
            this.setLatitude(0.0);
            this.setLongitude(0.0);
        } else {
            this.setLatitude(gp.getLatitude());
            this.setLongitude(gp.getLongitude());
        }
    }

    public GeoPoint getGeoPoint()
    {
        return new GeoPoint(this.getLatitude(), this.getLongitude());
    }
    
    public boolean isValidGeoPoint()
    {
        return GeoPoint.isValid(this.getLatitude(),this.getLongitude());
    }

    // ------------------------------------------------------------------------

    public void setGpsAge(long ageSec) 
    {
        this.fieldValues.setLong(EventData.FLD_gpsAge, ageSec);
    }
    
    public boolean hasGpsAge()
    {
        return this.fieldValues.hasProperty(EventData.FLD_gpsAge);
    }

    public long getGpsAge() 
    {
        return this.fieldValues.getLong(EventData.FLD_gpsAge, 0L);
    }

    // ------------------------------------------------------------------------

    public void setSatelliteCount(int count) 
    {
        this.fieldValues.setInt(EventData.FLD_satelliteCount, count);
    }
    
    public boolean hasSatelliteCount()
    {
        return this.fieldValues.hasProperty(EventData.FLD_satelliteCount);
    }

    public int getSatelliteCount() 
    {
        return this.fieldValues.getInt(EventData.FLD_satelliteCount, -1);
    }

    // ------------------------------------------------------------------------

    public void setBatteryLevel(double level) 
    {
        this.fieldValues.setDouble(EventData.FLD_batteryLevel, level);
    }
    
    public boolean hasBatteryLevel()
    {
        return this.fieldValues.hasProperty(EventData.FLD_batteryLevel);
    }

    public double getBatteryLevel() 
    {
        return this.fieldValues.getDouble(EventData.FLD_batteryLevel, 0.0);
    }

    // ------------------------------------------------------------------------

    public void setSpeedKPH(double kph) 
    {
        this.fieldValues.setDouble(EventData.FLD_speedKPH, kph);
    }
    
    public boolean hasSpeedKPH()
    {
        return this.fieldValues.hasProperty(EventData.FLD_speedKPH);
    }

    public double getSpeedKPH() 
    {
        return this.fieldValues.getDouble(EventData.FLD_speedKPH, 0.0);
    }

    // ------------------------------------------------------------------------

    public void setHeading(double heading) 
    {
        this.fieldValues.setDouble(EventData.FLD_heading, heading);
    }
    
    public boolean hasHeading()
    {
        return this.fieldValues.hasProperty(EventData.FLD_heading);
    }

    public double getHeading() 
    {
        return this.fieldValues.getDouble(EventData.FLD_heading, 0.0);
    }

    // ------------------------------------------------------------------------

    public void setAltitude(double altM) 
    {
        this.fieldValues.setDouble(EventData.FLD_altitude, altM);
    }
    
    public boolean hasAltitude()
    {
        return this.fieldValues.hasProperty(EventData.FLD_altitude);
    }

    public double getAltitude() 
    {
        return this.fieldValues.getDouble(EventData.FLD_altitude, 0.0);
    }

    // ------------------------------------------------------------------------

    public void setOdometerKM(double km) 
    {
        this.fieldValues.setDouble(EventData.FLD_odometerKM, km);
    }
    
    public boolean hasOdometerKM()
    {
        return this.fieldValues.hasProperty(EventData.FLD_odometerKM);
    }

    public double getOdometerKM() 
    {
        return this.fieldValues.getDouble(EventData.FLD_odometerKM, 0.0);
    }

    // ------------------------------------------------------------------------
    
    public boolean hasGeozoneID()
    {
        return this.fieldValues.hasProperty(EventData.FLD_geozoneID);
    }

    public String getGeozoneID() 
    {
        // not fully supported here
        return "";
    }

    // ------------------------------------------------------------------------
    
    public boolean hasAddress()
    {
        return this.fieldValues.hasProperty(EventData.FLD_address);
    }

    public String getAddress() 
    {
        // not fully supported here
        return "";
    }

    // ------------------------------------------------------------------------

    public void setInputMask(long mask) 
    {
        this.fieldValues.setLong(EventData.FLD_inputMask, mask);
    }
    
    public boolean hasInputMask()
    {
        return this.fieldValues.hasProperty(EventData.FLD_inputMask);
    }

    public long getInputMask() 
    {
        return this.fieldValues.getLong(EventData.FLD_inputMask, 0L);
    }

    // ------------------------------------------------------------------------

    /* icon selector properties */
    private boolean isLastEventInList = false;
    public void setIsLastEvent(boolean isLast)
    {
        this.isLastEventInList = isLast;
    }
    
    public boolean getIsLastEvent()
    {
        return this.isLastEventInList;
    }

    // ------------------------------------------------------------------------

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        /* standard event fields */
        int sc = this.getStatusCode();
        sb.append("Event values:\n");
        sb.append("  DeviceID  : " + this.getAccountID() + "/" + this.getDeviceID() + "\n");
        sb.append("  UniqueID  : " + this.getUniqueID() + "\n");
        sb.append("  Fixtime   : " + this.getTimestamp() + " [" + new DateTime(this.getTimestamp()) + "]\n");
        sb.append("  StatusCode: ["+ StatusCodes.GetHex(sc) + "] " + StatusCodes.GetDescription(sc,null));
        sb.append("  GPS       : " + this.getGeoPoint() + " [age "+this.getGpsAge()+" sec]\n");
        sb.append("  SpeedKPH  : " + StringTools.format(this.getSpeedKPH(),"0.0") + " [" + this.getHeading() + "]\n");

        /* remaining event fields */
        OrderedSet<?> fldn = new OrderedSet<Object>(this.fieldValues.getPropertyKeys());
        fldn.remove(EventData.FLD_timestamp);
        fldn.remove(EventData.FLD_statusCode);
        fldn.remove(EventData.FLD_latitude);
        fldn.remove(EventData.FLD_longitude);
        fldn.remove(EventData.FLD_gpsAge);
        fldn.remove(EventData.FLD_speedKPH);
        fldn.remove(EventData.FLD_heading);
        for (Object k : fldn) {
            Object v = this.fieldValues.getProperty(k,"?");
            sb.append("  ");
            sb.append(StringTools.leftAlign(k.toString(),10));
            sb.append(": ");
            sb.append(v.toString());
            sb.append("\n");
        }

        /* return string */
        return sb.toString();

    }
    
}

