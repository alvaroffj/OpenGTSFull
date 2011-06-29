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
//  2010/05/24  Martin D. Flynn
//      - Initial release
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;

/**
*** A Container for Cell-Tower information
**/

public class CellTower
{

    // ------------------------------------------------------------------------

    public static final String ARG_MCC      = "mcc";
    public static final String ARG_MNC      = "mnc";
    public static final String ARG_TAV      = "tav";
    public static final String ARG_CID      = "cid";
    public static final String ARG_LAC      = "lac";
    public static final String ARG_ARFCN    = "arfcn";
    public static final String ARG_RXLEV    = "rxlev";

    // ------------------------------------------------------------------------

    private String      cellTowerName       = null;
    private GeoPoint    cellTowerGPS        = null;

    private int         cellTowerID         = -1;
    private int         mobileCountryCode   = -1;
    private int         mobileNetworkCode   = -1;
    private int         locationAreaCode    = -1;
    private int         arfChannelNumber    = -1;
    private int         receptionLevel      = -1;
    private int         timingAdvance       = -1;

    /**
    *** Constructor
    **/
    public CellTower()
    {
        super();
        this._init(-1, -1, -1, -1, -1, -1, -1);
    }

    /**
    *** Constructor
    **/
    public CellTower(int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(mcc, mnc, tav, cid, lac, arfcn, rxlev);
    }

    /**
    *** Constructor
    **/
    public CellTower(int cid, int lac, int arfcn, int rxlev)
    {
        super();
        this._init(-1, -1, -1, cid, lac, arfcn, rxlev);
    }

    /**
    *** Constructor
    **/
    public CellTower(RTProperties cidp)
    {
        super();
        if (cidp != null) {
            int mcc   = cidp.getInt(ARG_MCC  , -1);
            int mnc   = cidp.getInt(ARG_MNC  , -1);
            int tav   = cidp.getInt(ARG_TAV  , -1);
            int cid   = cidp.getInt(ARG_CID  , -1);
            int lac   = cidp.getInt(ARG_LAC  , -1);
            int arfcn = cidp.getInt(ARG_ARFCN, -1);
            int rxlev = cidp.getInt(ARG_RXLEV, -1);
            this._init(mcc, mnc, tav, cid, lac, arfcn, rxlev);
        }
    }

    /**
    *** private init
    **/
    private void _init(int mcc, int mnc, int tav, int cid, int lac, int arfcn, int rxlev)
    {
        // --
        this.mobileCountryCode  = mcc;
        this.mobileNetworkCode  = mnc;
        this.timingAdvance      = tav;
        // --
        this.cellTowerID        = cid;
        this.locationAreaCode   = lac;
        this.arfChannelNumber   = arfcn;
        this.receptionLevel     = rxlev;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if this CellTower instance contains valid information
    **/
    public boolean isValid()
    {
        if (this.getCellTowerID() <= 0) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the Cell Tower Name (if available)
    *** @return The Cell Tower name (or null if not available)
    **/
    public String getCellTowerName()
    {
        return this.cellTowerName;
    }

    /** 
    *** Sets the Cell Tower Name (if any)
    *** @param name The Cell Tower name
    **/
    public void setCellTowerName(String name)
    {
        this.cellTowerName = !StringTools.isBlank(name)? name.trim() : null;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the Cell Tower Latitude/Longitude (if available)
    *** @return The Cell Tower Latitude/Longitude (or null if not available)
    **/
    public GeoPoint getCellTowerLocation()
    {
        return this.cellTowerGPS;
    }

    /** 
    *** Sets the Cell Tower Name (if any)
    *** @param name The Cell Tower name
    **/
    public void setCellTowerLocation(GeoPoint gps)
    {
        this.cellTowerGPS = gps;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Mobile Country Code
    *** @return The Mobile Country Code
    **/
    public int getMobileCountryCode()
    {
        return this.mobileCountryCode;
    }

    /**
    *** Sets the Mobile Country Code
    *** @param mcc The Mobile Country Code
    **/
    public void setMobileCountryCode(int mcc)
    {
        this.mobileCountryCode = mcc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Mobile Network Code
    *** @return The Mobile Network Code
    **/
    public int getMobileNetworkCode()
    {
        return this.mobileNetworkCode;
    }

    /**
    *** Sets the Mobile Network Code
    *** @param mnc The Mobile Network Code
    **/
    public void setMobileNetworkCode(int mnc)
    {
        this.mobileNetworkCode = mnc;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Timing Advance
    *** @return The Timing Advance
    **/
    public int getTimingAdvance()
    {
        return this.timingAdvance;
    }

    /**
    *** Sets the Timing Advance
    *** @param tav The Timing Advance
    **/
    public void setTimingAdvance(int tav)
    {
        this.timingAdvance = tav;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Cell Tower ID
    *** @return The Cell Tower ID
    **/
    public int getCellTowerID()
    {
        return this.cellTowerID;
    }

    /**
    *** Sets the Cell Tower ID
    *** @param cid The Cell Tower ID
    **/
    public void setCellTowerID(int cid)
    {
        this.cellTowerID = cid;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Location Area Code
    *** @return The Location Area Code
    **/
    public int getLocationAreaCode()
    {
        return this.locationAreaCode;
    }

    /**
    *** Sets the Location Area Code
    *** @param lac  The Location Area Code
    **/
    public void setLocationAreaCode(int lac)
    {
        this.locationAreaCode = lac;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Absolute Radio Frequency Channel Number
    *** @return The Absolute Radio Frequency Channel Number
    **/
    public int getAbsoluteRadioFrequencyChannelNumber()
    {
        return this.arfChannelNumber;
    }

    /**
    *** Sets the Absolute Radio Frequency Channel Number
    *** @param arfcn The Absolute Radio Frequency Channel Number
    **/
    public void setAbsoluteRadioFrequencyChannelNumber(int arfcn)
    {
        this.arfChannelNumber = arfcn;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Reception Level
    *** @return The Reception Level
    **/
    public int getReceptionLevel()
    {
        return this.receptionLevel;
    }

    /**
    *** Sets the Reception Level
    *** @param rxlev The Reception Level
    **/
    public void setReceptionLevel(int rxlev)
    {
        this.receptionLevel = rxlev;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a RTProperties representation of this instance
    *** @return A RTProperties representation of this instance
    **/
    public RTProperties getRTProperties()
    {
        RTProperties cidp = new RTProperties();
        if (this.mobileCountryCode >= 0) {
            cidp.setInt(ARG_MCC  , this.mobileCountryCode);
        }
        if (this.mobileNetworkCode >= 0) {
            cidp.setInt(ARG_MNC  , this.mobileNetworkCode);
        }
        if (this.timingAdvance >= 0) {
            cidp.setInt(ARG_TAV  , this.timingAdvance);
        }
        if (this.cellTowerID >= 0) {
            cidp.setInt(ARG_CID  , this.cellTowerID);
        }
        if (this.locationAreaCode >= 0) {
            cidp.setInt(ARG_LAC  , this.locationAreaCode);
        }
        if (this.arfChannelNumber >= 0) {
            cidp.setInt(ARG_ARFCN, this.arfChannelNumber);
        }
        if (this.receptionLevel >= 0) {
            cidp.setInt(ARG_RXLEV, this.receptionLevel);
        }
        return cidp;
    }

    /**
    *** Returns the String representation of this instance
    *** @return String representation of this instance
    **/
    public String toString()
    {
        return this.getRTProperties().toString();
    }

    // ------------------------------------------------------------------------

}
