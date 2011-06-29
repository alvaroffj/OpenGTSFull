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
//  2008/07/08  Martin D. Flynn
//     -Initial release
//  2008/08/08  Martin D. Flynn
//     -Added support for Geozones
//  2008/08/17  Martin D. Flynn
//     -Distance now always displayed, even if value falls to '0'.
//  2008/09/19  Martin D. Flynn
//     -Added warning message when MAX_PUSH_PINS has been reached.
//  2008/12/01  Martin D. Flynn
//     -Added support for optional event data fields
//     -Added JS code to highlite the details report row when the info balloon
//      is displayed for a specific pushpin.
//  2009/01/01  Martin D. Flynn
//     -Added option for displaying altitude in info-bubble.
//  2009/07/01  Martin D. Flynn
//     -Map points wrapped in XML Data/Set tags
//  2009/08/07  Martin D. Flynn
//     -Changed "Time" and "LastEvent" tag sections
//  2009/11/01  Martin D. Flynn
//     -Display all device detail records when showing multiple events per device
//      on group map.  Line breaks separate devices.
//  2010/07/04  Martin D. Flynn
//     -Added support for collapsible map controls
// ----------------------------------------------------------------------------
// External funtions:
//   new JSMap(String mapID)
//   JSClearLayers()
//   JSSetCenter(JSMapPoint center)
//   JSDrawPushpins(JSMapPushpin pushPin[], int recenterMode, int replay)
//   JSDrawPOI(JSMapPushpin pushPin[])
//   JSDrawRoute(JSMapPoint points[], String color)
//   JSDrawGeozone(int type, double radius, JSMapPoint points[], String color, int primaryIndex)
//   JSShowPushpin(JSMapPushpin pushPin, boolean center)
//   JSPauseReplay(int replay)
//   JSUnload()
// ----------------------------------------------------------------------------

/* these must match the data response values in "Track.java" */
var DATA_RESPONSE_LOGOUT        = "LOGOUT";
var DATA_RESPONSE_ERROR         = "ERROR";
var DATA_RESPONSE_PING_OK       = "PING:OK";
var DATA_RESPONSE_PING_ERROR    = "PING:ERROR";

/* these must match the class definitions in "Controls.css" */
var CLASS_DETAILS_DIV           = "trackMapDetailLocation";
var CLASS_DETAILS_HEADER_ROW    = "mapDetailsHeaderRow";
var CLASS_DETAILS_HEADER_COL    = "mapDetailsHeaderColumn";
var CLASS_DETAILS_ROW_HILITE    = "mapDetailsDataRowHiLite";
var CLASS_DETAILS_ROW_ODD       = "mapDetailsDataRowOdd";
var CLASS_DETAILS_ROW_EVEN      = "mapDetailsDataRowEven";
var CLASS_DETAILS_INDEX_COL     = "mapDetailsIndexColumn";
var CLASS_DETAILS_DATA_COL_NEW  = "mapDetailsDataColumn_new";
var CLASS_DETAILS_DATA_COL      = "mapDetailsDataColumn";

var ID_DETAIL_ROW_              = "detailRow_";

var DETAILS_WINDOW              = false; // not fully implemented
var jsvDetailsWindow            = null;
var jsvDetailsLastHilightedRow  = null;
var jsvUseDeviceBreaks          = false;

/* replay state */
var REPLAY_STOPPED              = 0;
var REPLAY_PAUSED               = 1;
var REPLAY_RUNNING              = 2;

/* recenter modes */
var RECENTER_NONE               = 0; // don't change current zoom
var RECENTER_LAST               = 1; // center/zoom on last point
var RECENTER_ZOOM               = 2; // normal center zoom on all points
var RECENTER_PAN                = 3; // pan to last point

/* Geozone types */
var ZONE_POINT_RADIUS           = 0;
var ZONE_BOUNDED_RECT           = 1; // not yet supported
var ZONE_SWEPT_POINT_RADIUS     = 2; // not supported
var ZONE_POLYGON                = 3; // not yet supported

/* MapData tags */
var TAG_MapData                 = "MapData";    // top-level tag
var TAG_Action                  = "Action";     // action to perform ("autoupdate", "alert", "gotourl", etc)
var TAG_Time                    = "Time";       // update time (server time)
var TAG_LastEvent               = "LastEvent";  // last event time for current device
var TAG_Geozone                 = "Geozone";    // Geozone [attr: type, radius]
var TAG_DataSet                 = "DataSet";    // map point datasets
var TAG_Point                   = "P";          // CSV data record
var TAG_Shape                   = "Shape";      // CSV data record

var ATTR_isFleet                = "isFleet";
var ATTR_type                   = "type";
var ATTR_routeColor             = "routeColor";
var ATTR_textColor              = "textColor";
var ATTR_color                  = "color";
var ATTR_id                     = "id";
var ATTR_route                  = "route";
var ATTR_timestamp              = "timestamp";
var ATTR_timezone               = "timezone";
var ATTR_year                   = "year";
var ATTR_month                  = "month";
var ATTR_day                    = "day";
var ATTR_command                = "command";
var ATTR_radius                 = "radius";
var ATTR_battery                = "battery";
var ATTR_signal                 = "signal";

/* partial data */
var jsvPartialData              = false;

/* jsmap image base dir */
var jsvImageBaseDir             = ".";

/* fixed zoom mode */
var jsvFixedZoom                = false;

/* Latitude/Longitude format Deg:Min decimal places. */
var LATLON_FORMAT_MIN_DEC       = 2; // 2=20'34.12",  3=20'34.123"

/* Device display color type (0=off, 1=foreground, 2=background) */
var DISPLAY_COLOR_TYPE          = 1;

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapPoint

/**
*** Constructor: Creates a GeoPoint
*** @param lat  The latitude  (decimal degrees)
*** @param lon  The longitude (decimal degrees)
**/
function JSMapPoint(lat, lon)
{
    this.lat = lat;
    this.lon = lon;
};

//JSMapPoint.prototype.isValid = funtion() 
//{
//    return ((this.lat != 0.0) || (this.lon != 0.0))? true : false;
//};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapShape

function JSMapShape(type, radiusM, points, color, zoomTo)
{
    this.type   = type;
    this.radius = radiusM;
    this.points = points;
    this.color  = color;
    this.zoomTo = zoomTo;
}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSBounds

/**
*** Constructor: Creates a Bounds object
**/
function JSBounds()
{
    this.maxLat =  -90.0;
    this.maxLon = -180.0;
    this.minLat =   90.0;
    this.minLon =  180.0;
};

/**
*** Extends the bound to include the specified JSMapPoint
*** @param pt  The JSMapPoint
**/
JSBounds.prototype.extend = function(pt)
{
    if (pt != null) {
        this.extendLat(pt.lat);
        this.extendLon(pt.lon);
    }
};

/**
*** Extends the bound to include the specified Latitude
*** @param lat  The Latitude
**/
JSBounds.prototype.extendLat = function(lat)
{
    if (lat > this.maxLat) { this.maxLat = lat; }
    if (lat < this.minLat) { this.minLat = lat; }
};

/**
*** Extends the bound to include the specified Longitude
*** @param lat  The Longitude
**/
JSBounds.prototype.extendLon = function(lon)
{
    if (lon > this.maxLon) { this.maxLon = lon; }
    if (lon < this.minLon) { this.minLon = lon; }
};

/**
*** Gets the center of the bounds
*** @return The center JSMapPoint
**/
JSBounds.prototype.getCenter = function()
{
    return new JSMapPoint((this.minLat + this.maxLat) / 2.0, (this.minLon + this.maxLon) / 2.0);
};

/**
*** Gets the width of the bounds
*** @return The bounds width (ie. delta longitude)
**/
JSBounds.prototype.getWidth = function()
{
    return this.maxLon - this.minLon;
};

/**
*** Gets the width of the bounds (in delta meters)
*** @return The bounds width (ie. delta meters)
**/
JSBounds.prototype.getWidthMeters = function()
{
    var lat = this.minLat;
    return geoDistanceMeters(lat, this.minLon, lat, this.maxLon);
};

/**
*** Gets the height of the bounds
*** @return The bounds height (ie. delta latitude)
**/
JSBounds.prototype.getHeight = function()
{
    return this.maxLat - this.minLat;
};

/**
*** Gets the height of the bounds (in delta meters)
*** @return The bounds height (ie. delta meters)
**/
JSBounds.prototype.getHeightMeters = function()
{
    var lon = this.minLon;
    return geoDistanceMeters(this.minLat, lon, this.maxLat, lon);
};

/**
*** Calculates the best zoom for this bounds (in meters per pixel)
*** @param viewWidth  The map width in pixels
*** @param viewHeight The map height in pixels
**/
JSBounds.prototype.calculateMetersPerPixel = function(viewWidth, viewHeight)
{
    var mppW = this.getWidthMeters()  / viewWidth;
    var mppH = this.getHeightMeters() / viewHeight;
    return (mppW > mppH)? mppW : mppH;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapPushpin

/**
*** Constructor: Creates a JSMapPushpin
**/
function JSMapPushpin(rcdNdx, dsNdx, ppNdx, evRcd, 
    lat,lon, label,html, iconUrl,iconSize,iconOffset, shadowUrl,shadowSize)
{

    /* detail record index */
    this.rcdNdx     = rcdNdx;
    this.dsNdx      = dsNdx;
    this.ppNdx      = ppNdx;
    this.evRcd      = evRcd;

    /* latitude/longitude */
    this.lat        = lat;
    this.lon        = lon;
    
    /* displayed information */
    this.label      = label;
    this.html       = html;
    
    /* icon attributes */
    this.iconUrl    = iconUrl;
    this.iconSize   = iconSize;
    this.iconOffset = iconOffset;
    this.shadowUrl  = shadowUrl;
    this.shadowSize = shadowSize;

    /* popup attributes */
    this.map        = null;
    this.marker     = null;
    this.hoverPopup = false;
    this.popup      = null;
    this.popupShown = false;
    
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSMapDataSet

/**
*** Constructor: Creates a JSMapDataSet
**/
function JSMapDataSet(pushPins, routePoints, routeColor, partial)
{
    this.pushPins    = pushPins;    // device pushpins
    this.routePoints = routePoints; // route line
    this.routeColor  = routeColor;
    this.partial     = partial;
}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- JSDetailPoint

/**
*** Constructor: Creates a JSDetailPoint
**/
function JSDetailPoint(rcdNdx, dsNdx, ppNdx, evRcd, textColor)
{
    this.device   = evRcd.device;
    this.latlon   = jsmFormatCoord(evRcd.latitude,true,4) + "/" + jsmFormatCoord(evRcd.longitude,false,4); // AA.AAAA/-NNN.NNNN
    this.satCount = evRcd.satCount;
    this.dsNdx    = dsNdx;                                              // dataset index
    this.ppNdx    = ppNdx;                                              // XX [1+, -1 if no pushpin]
    this.index    = rcdNdx;                                             // XX [1+]
    this.code     = evRcd.code;                                         // A...A
    this.dateTime = evRcd.dateFmt + ' ' + evRcd.timeFmt;                // YYYY/MM/DD HH:MM:SS
    this.timeZone = evRcd.timeZone; // tmz;                             // US/Pacific
    this.speed    = numFormatFloat(evRcd.speedKPH * SPEED_KPH_MULT, 1); // SS
    this.heading  = numFormatFloat(evRcd.heading, 0);                   // SS
    this.compass  = evRcd.compass;                                      // NE
    this.altitude = evRcd.altitude;
    this.address  = evRcd.address;
    this.optDesc  = evRcd.optDesc;
    this.color    = textColor;
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- map initialization
 
/**
*** Initializes the map
**/
function jsMapInit() 
{
    if (jsmap == null) {
        jsmapElem = document.getElementById(MAP_ID);
        if (jsmapElem != null) {
            try {
                jsmap = new JSMap(jsmapElem);
                if (jsmap) {
                    jsmap.JSClearLayers();
                    jsmap.JSDrawPushpins(null, RECENTER_ZOOM, 0);
                } else {
                    // seems to be necessary on IE (it doesn't throw an exception)
                    alert("Error [jsMapInit]:\nError occured while creating JSMap");
                }
            } catch (e) {
                alert("Error [jsMapInit]:\n" + e);
            }
        } else {
            alert("Error [jsMapInit]:\nDiv '" + MAP_ID + "' not found");
        }
    }
};

// ----------------------------------------------------------------------------
// --- unload

/**
*** Releases any map resources
**/
function jsmUnload()
{
    if (!jsmap) { return; }
    try {
        jsmap.JSUnload();
    } catch (e) {
        // quietly ignore "unload" errors
    }
};

// ----------------------------------------------------------------------------
// fixed zoom mode

function jsmSetFixedZoom(fixedZoom)
{
    jsvFixedZoom = fixedZoom; // true|false
};

function jsmRecenterZoomMode(mode)
{
    return jsvFixedZoom? RECENTER_NONE : mode;
};

// ----------------------------------------------------------------------------
// --- load/update

/**
*** Centers on the last available point
**/
function jsmSetCenter(lat, lon, zoom)
{
    if (!jsmap) { return; }
    var center = new JSMapPoint(lat, lon);
    jsmap.JSSetCenter(center, zoom);
}

/**
*** Centers on the last available point
**/
function jsmCenterOnLastPushpin(showLastPointOnly)
{
    if (!jsmap) { return; }

    /* remove old layers */
    jsmap.JSClearLayers();

    /* draw POI */
    if (jsvPoiPins && (jsvPoiPins.length > 0)) {
        jsmap.JSDrawPOI(jsvPoiPins);
    }

    /* data set */
    if (jsvDataSets && (jsvDataSets.length > 0)) {
        var jds = jsvDataSets[0];
        
        /* draw the route line first */
        if (jds.routePoints && (jds.routePoints.length >= 2)) {
            jsmap.JSDrawRoute(jds.routePoints, jds.routeColor);
        }
    
        /* draw the pushpins */
        var jpp = jds.pushPins;
        if (jpp && (jpp.length > 0)) {
            if (showLastPointOnly) {
                var lastPoint = [ jpp[jpp.length - 1] ];
                jsmap.JSDrawPushpins(lastPoint, jsmRecenterZoomMode(RECENTER_ZOOM), REPLAY_STOPPED);
            } else {
                jsmap.JSDrawPushpins(jpp, jsmRecenterZoomMode(RECENTER_ZOOM), REPLAY_STOPPED);
            }
        }
    
    }
    
    /* close the detail report */
    jsvDetailVisible = false;
    jsmShowDetailReport();

}

// ----------------------------------------------------------------------------

/**
*** Sets the points/attributes on the current map
**/
function _jsmSetMap(recenterMode, /*JSMapDataSet[]*/mapDataSets, poiPins, replay) 
{
    if (!jsmap) { return; }

    /* remove old layers */
    jsmap.JSClearLayers();

    /* draw POI */
    jsvPoiPins = poiPins;
    if (jsvPoiPins && (jsvPoiPins.length > 0)) {
        jsmap.JSDrawPOI(jsvPoiPins);
    }

    /* draw datasets */
    jsvDataSets = mapDataSets;
    if (jsvDataSets) {
        for (var i = 0; i < jsvDataSets.length; i++) {
            var jds = jsvDataSets[i]; // JSMapDataSet

            /* draw the route line first */
            if (jds.routePoints && (jds.routePoints.length >= 2)) { 
                jsmap.JSDrawRoute(jds.routePoints, jds.routeColor);
            }
    
            /* draw the pushpins */
            if (jds.pushPins && (jds.pushPins.length > 0)) {
                var rcm = ((i + 1) == jsvDataSets.length)? recenterMode : RECENTER_NONE;
                jsmap.JSDrawPushpins(jds.pushPins, jsmRecenterZoomMode(rcm), replay);
            }
            
            /* only one dataset if 'replay' active */
            if (replay > 0) {
                break;
            }

        }
    }

};

/**
*** Returns an AJAX request object
**/
function jsmGetXMLHttpRequest() 
{
    return getXMLHttpRequest();
};

/**
*** Parse the specified XML 
**/
function jsmParseXMLPoints(xmlText, recenterMode, replay) // tmz
{

    /* create XML doc */
    //alert("Point XML: " + xmlText);
    var xmlDoc = createXMLDocument(xmlText);
    if (xmlDoc == null) {
        // alert('No data points provided');
        return 0;
    }

    /* parse */
    var data = xmlDoc.getElementsByTagName(TAG_MapData);
    if (data.length <= 0) {
        return 0;
    }
    var dataElem = data[0];
    var dataAttr = dataElem.attributes;
    var isFleet  = getXMLNodeAttribute(dataAttr,ATTR_isFleet,false);

    /* last event */
    var latest    = dataElem.getElementsByTagName(TAG_LastEvent);
    var latestVal = (latest.length > 0)? latest[0].childNodes[0].nodeValue : null;
    if (latestVal != null) {
        var timeAttr          = latest[0].attributes;
        jsvLastEventEpoch     = getXMLNodeAttribute(timeAttr,ATTR_timestamp,0);
        jsvLastEventTmzFmt    = getXMLNodeAttribute(timeAttr,ATTR_timezone,"");
        var year              = getXMLNodeAttribute(timeAttr,ATTR_year,0);     // year (in selected timezone)
        var month1            = getXMLNodeAttribute(timeAttr,ATTR_month,0);    // month1 (in selected timezone)
        var day               = getXMLNodeAttribute(timeAttr,ATTR_day,0);      // date (in selected timezone)
        var battery           = getXMLNodeAttribute(timeAttr,ATTR_battery,0);  // battery level (%)
        var signal            = getXMLNodeAttribute(timeAttr,ATTR_signal,0);   // signal strength (%)
        jsvLastEventYMD       = { YYYY:year, MM:month1, DD:day };              // in selected timezone
        jsvLastBatteryLevel   = battery;
        jsvLastSignalStrength = signal;
        var fld = latestVal.split('|');
        if (fld.length > 0) {
            var dateFmt         = (fld.length > 0)? fld[0] : '';            // formatted date
            var timeFmt         = (fld.length > 1)? fld[1] : '';            // formatted time
            var battFmt         = (fld.length > 2)? fld[2] : '';            // formatted battery level
            var signFmt         = (fld.length > 3)? fld[3] : '';            // formatted signal strength
            jsvLastEventDateFmt = dateFmt;
            jsvLastEventTimeFmt = timeFmt;
        }
    } else {
        jsvLastBatteryLevel   = 0.0;
        jsvLastSignalStrength = 0.0;
    }
    
    /* time */
    var today    = dataElem.getElementsByTagName(TAG_Time);
    var todayVal = (today.length > 0)? today[0].childNodes[0].nodeValue : null;
    if (todayVal != null) {
        var timeAttr       = today[0].attributes;
        jsvTodayEpoch      = getXMLNodeAttribute(timeAttr,ATTR_timestamp,0);
        jsvTodayTmzFmt     = getXMLNodeAttribute(timeAttr,ATTR_timezone ,"");
        var year           = getXMLNodeAttribute(timeAttr,ATTR_year ,0);    // year (in selected timezone)
        var month1         = getXMLNodeAttribute(timeAttr,ATTR_month,0);    // month1 (in selected timezone)
        var day            = getXMLNodeAttribute(timeAttr,ATTR_day  ,0);    // date (in selected timezone)
        jsvTodayYMD        = { YYYY:year, MM:month1, DD:day };              // in selected timezone
        var fld = todayVal.split('|');
        if (fld.length > 0) {
            var dateFmt       = (fld.length > 0)? fld[0] : '';              // formatted date
            var timeFmt       = (fld.length > 1)? fld[1] : '';              // formatted time
            jsvTodayDateFmt   = dateFmt;
            jsvTodayTimeFmt   = timeFmt;
        }
    }

    /* detail report */
    //var detailList  = []; // detailed report table
    
    /* points of interest */
    var poiPinList  = []; // POI pushpins

    /* dataset */
    var dsNdx       = 0;
    var dsList      = []; // dataset list

    /* "Location Detail" report */
    var detailList  = []; // detailed report table

    /* parse Shape tags [MapShape] */
    var shapes      = [];
    var mapShapes   = dataElem.getElementsByTagName(TAG_Shape);
    for (var msi = 0; msi < mapShapes.length; msi++) {
        var ms      = mapShapes[msi];
        var msAttr  = ms.attributes;
        var type    = getXMLNodeAttribute(msAttr, ATTR_type  , "none"); // "circle", "rectangle", "polygon"
        var radiusM = getXMLNodeAttribute(msAttr, ATTR_radius, "0");
        var color   = getXMLNodeAttribute(msAttr, ATTR_color , "#0000FF");
        //alert("Parsed shape type=" + type + " radius=" + radiusM + " cdata=" + ms.childNodes[0].nodeValue);

        /* points "<lat>/<lon>,<lat>/<lon>,..." */
        var ptsStr = ms.childNodes[0].nodeValue;
        var ptFld  = ptsStr.split(',');
        var points = [];
        for (var i = 0; i < ptFld.length; i++) {
            var LL = ptFld[i].split('/');
            if (LL.length < 2) { continue; }
            var lat = numParseFloat(LL[0], 0);
            var lon = numParseFloat(LL[1], 0);
            if (((lat != 0) || (lon != 0))) {
                points.push(new JSMapPoint(lat,lon));
            }
        }

        /* draw shape */
        shapes.push(new JSMapShape(type, radiusM, points, color, false));

    }

    /* parse DataSet tags */
    var rcdNdx = 0;
    var maxDataSetPoints = 0;
    var dataSets = dataElem.getElementsByTagName(TAG_DataSet);
    for (var dsi = 0; dsi < dataSets.length; dsi++) {
        var ds = dataSets[dsi];
        var dsAttr = ds.attributes;

        /* dataset vars */
        var pushPinList = []; // device pushpins
        var routeList   = []; // route line
        var partial     = false;
        
        /* type */
        var type        = getXMLNodeAttribute(dsAttr, ATTR_type, "device"); // "group", "device", "poi"
        var isPOI       = (type == "poi")? true : false;

        /* route-line */
        var showRoute   = ROUTE_LINE_SHOW;
        var route       = (getXMLNodeAttribute(dsAttr, ATTR_route, "true") != "false")? true : false;
        var textColor   = getXMLNodeAttribute(dsAttr, ATTR_textColor, "");
        var routeColor  = getXMLNodeAttribute(dsAttr, ATTR_routeColor, "");
        if (routeColor == "") { routeColor = ROUTE_LINE_COLOR; }

        /* show route-line? */
        if (showRoute) {
            if (isPOI) {
                // route-line is already not shown for POI
            } else
            if (!route) {
                showRoute = false;
            } 
        }
        //alert("Route = "+route+"/"+showRoute+" [" + getXMLNodeAttribute(dsAttr,ATTR_route,"?"));

        /* points */
        var pts = ds.getElementsByTagName(TAG_Point);
        var startNdx = 0;
        if ((pts.length - startNdx) > MAX_PUSH_PINS) {
            startNdx = pts.length - MAX_PUSH_PINS;
            partial  = true;
        }

        /* parse points */
        var dsPtCount = 0;
        for (var p = startNdx; p < pts.length; p++) {
            var cvsRcd = pts[p].childNodes[0].nodeValue;

            /* parse point */
            var evRcd = new MapEventRecord(cvsRcd);
            if (!evRcd.valid) {
                continue; // skip invalid records
            }

            // Point Of Interest?
            if (isPOI) {
                if (evRcd.validGPS) {
                    var ndx = -1; // ++rcdNdx;
                    var ppNdx = poiPinList.length;
                    poiPinList.push(jsmCreatePushPin(ndx, -1, ppNdx, evRcd));
                }
                continue;
            }

            // save displayable point
            rcdNdx++;
            evRcd.index = rcdNdx;
            dsPtCount++;
            if (evRcd.validGPS) {
                if (showRoute) {
                    routeList.push(new JSMapPoint(evRcd.latitude, evRcd.longitude));
                }
                if (!SHOW_ADDR && (evRcd.address != null)) { SHOW_ADDR = true; }
                var ppNdx = pushPinList.length;
                pushPinList.push(jsmCreatePushPin(rcdNdx, dsNdx, ppNdx, evRcd));
                detailList.push(new JSDetailPoint(rcdNdx, dsNdx, ppNdx, evRcd, textColor));
            } else {
                detailList.push(new JSDetailPoint(rcdNdx,    -1,    -1, evRcd, textColor));
            }

            // save last reported event times
            if (!IS_FLEET && (evRcd.timestamp > jsvLastEventEpoch)) { 
                jsvLastEventEpoch   = evRcd.timestamp;
                jsvLastEventYMD     = { YYYY:evRcd.year, MM:evRcd.month1, DD:evRcd.day }; // in selected timezone
                jsvLastEventDateFmt = evRcd.dateFmt;
                jsvLastEventTimeFmt = evRcd.timeFmt;
                jsvLastEventTmzFmt  = evRcd.timeZone; // Timezone
            }

        }

        /* contimue if last was POI */
        if (isPOI) {
            continue;
        }

        /* save dataset */
        if (dsPtCount > maxDataSetPoints) { maxDataSetPoints = dsPtCount; }
        dsList.push(new JSMapDataSet(pushPinList,(showRoute?routeList:null),routeColor,partial));
        dsNdx++;

    } // parsing datasets
    
    /* device breaks? (more than one dataset and any single dataset has more than one point) */
    jsvUseDeviceBreaks = (dsList.length > 1) && (maxDataSetPoints > 1);
    
    /* save datasets */
    jsvDetailPoints = detailList; // JSDetailPoint[]

    /* update map */
    _jsmSetMap(recenterMode, dsList, poiPinList, replay);
    
    /* draw shapes */
    if (shapes && (shapes.length > 0)) {
        for (var i = 0; i < shapes.length; i++) {
            var s = shapes[i];
            jsmDrawShape(s.type, s.radius, s.points, s.color, s.zoomTo)
        }
    }
    
    /* update last event times */
    if (jsvLastEventDateFmt && jsvLastEventTimeFmt) {
        //alert("Updating Latest Event: " + jsvLastEventDateFmt + " " + jsvLastEventTimeFmt + " " + jsvLastEventTmzFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_DATE, jsvLastEventDateFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TIME, jsvLastEventTimeFmt);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TMZ , jsvLastEventTmzFmt );
    } else {
        jsmSetIDInnerHTML(ID_LATEST_EVENT_DATE, "");
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TIME, TEXT_UNAVAILABLE);
        jsmSetIDInnerHTML(ID_LATEST_EVENT_TMZ , jsvLastEventTmzFmt );
    }

    /* update battery */
    jsmSetIDInnerHTML(ID_LATEST_BATTERY, jsmBatteryLevelIMG(jsvLastBatteryLevel));

    /* reached maximum allowed pushpins? */
    jsvPartialData = partial;
    if (jsvPartialData) {
        //alert(TEXT_MAXPUSHPINS_ALERT);
        jsmSetIDInnerHTML(ID_MESSAGE_TEXT, TEXT_MAXPUSHPINS_MSG);
    } else {
        jsmSetIDInnerHTML(ID_MESSAGE_TEXT, "");
    }

    /* update detail report */
    jsmShowDetailReport();
    
    /* check for action */
    var actions = dataElem.getElementsByTagName(TAG_Action);
    for (var i = 0; i < actions.length; i++) {
        var act  = actions[i];
        var attr = act.attributes;
        var cmd  = getXMLNodeAttribute(attr,ATTR_command,""); // "autoupdate", "alert", "gotourl"
        var arg  = act.childNodes[0].nodeValue;
        if (cmd == "autoupdate") {
            try {
                if (arg == "true") {
                    // AutoInterval?
                    startAutoUpdateMapTimer();
                } else {
                    stopAutoUpdateMapTimer();
                }
            } catch (e) {
                // ignore
            }
        } else
        if (cmd == "alert") {
            alert(arg);
        } else
        if (cmd == "gotourl") {
            target = "_self";
            openURL(arg, target)
        }
    }

    /* return number of points parsed */
    return jsvDetailPoints.length;
        
};

/**
*** Load and display point from the specified URL and display them on the current map
**/
function jsmLoadPoints(mapURL, recenterMode, replay) 
{
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", mapURL, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var data = req.responseText;
                    if (data.trim().toUpperCase() == DATA_RESPONSE_LOGOUT) {
                        alert(TEXT_TIMEOUT);
                        jsmSetLoadingPointsState(0);
                    } else {
                        jsmParseXMLPoints(data, recenterMode, replay);
                        jsmSetLoadingPointsState(0);
                    }
                } else
                if (req.readyState == 1) {
                    // alert('Loading points from URL: [' + req.readyState + ']\n' + mapURL);
                } else {
                    // alert('Problem loading URL? [' + req.readyState + ']\n' + mapURL);
                }
            }
            jsmSetLoadingPointsState(1);
            req.send(null);
        } else {
            alert("Error [jsmLoadPoints]:\n" + mapURL);
        }
    } catch (e) {
        alert("Error [jsmLoadPoints]:\n" + e);
    }
};

/**
*** Call-back on load state change
***   -1 == finish error
***    0 == finish success
***    1 == start
**/
var jsmapLoadingView = null;
function jsmSetLoadingPointsState(state)
{

    /* change color of "Update" button */
    var elem = ID_MAP_UPDATE_BTN? document.getElementById(ID_MAP_UPDATE_BTN) : null;
    if (elem != null) {
        switch (state) {
            case 0 : elem.style.color = '#000000'; break;
            case 1 : elem.style.color = '#338833'; break;
            default: elem.style.color = '#FF0000'; break;
        }
    }

    /* show "Loading" message */
    if (TEXT_LOADING_MAP_POINTS) {
        if (state == 1) {
            // show
            if (jsmapLoadingView == null) {
                var absLoc = getElementPosition(jsmapElem);
                var absSiz = getElementSize(jsmapElem);
                var X = absLoc.left + (absSiz.width /2) - 70;
                var Y = absLoc.top  + (absSiz.height/2) - 40;
                jsmapLoadingView = createDivBox("mapLoadingView", X, Y, -1, -1);
                var html = "";
                html += "<table class='jsmapLoadingView' cellspacing='0' cellpadding='0' border='0'>\n";
                html += "<tbody>\n";
                html += "<tr class='jsmapLoadingRow'>";
                if (MAP_LOADING_IMAGE_URI) {
                    html += "<td nowrap class='jsmapLoadingImage' valign='center'>";
                    html += "<img src=\"" + MAP_LOADING_IMAGE_URI + "\">";
                    html += "</td>";
                }
                html += "<td nowrap class='jsmapLoadingText' valign='center'>";
                html += TEXT_LOADING_MAP_POINTS;
                html += "</td>";
                html += "</tr>\n";
                html += "</tbody>\n";
                html += "</table>\n";
                jsmapLoadingView.innerHTML = html;
            }
            document.body.appendChild(jsmapLoadingView);
        } else {
            // hide
            if (jsmapLoadingView != null) {
                document.body.removeChild(jsmapLoadingView);
            }
        }
    }

}

// ----------------------------------------------------------------------------

/**
*** Ping device
**/
function jsmDevicePing(pingURL) 
{
    try {
        var req = jsmGetXMLHttpRequest();
        if (req) {
            req.open("GET", pingURL, true);
            //req.setRequestHeader("CACHE-CONTROL", "NO-CACHE");
            //req.setRequestHeader("PRAGMA", "NO-CACHE");
            req.setRequestHeader("If-Modified-Since", "Sat, 1 Jan 2000 00:00:00 GMT");
            req.onreadystatechange = function() {
                if (req.readyState == 4) {
                    var data = req.responseText;
                    if (data.trim().toUpperCase() == DATA_RESPONSE_LOGOUT) {
                        alert(TEXT_TIMEOUT);
                    } else
                    if (data.trim().toUpperCase() == DATA_RESPONSE_PING_OK) {
                        alert(TEXT_PING_OK);
                    } else {
                        // TODO: extract a 'reason' from the error text
                        alert(TEXT_PING_ERROR);
                    }
                } else
                if (req.readyState == 1) {
                    // alert('Pinging device, URL: [' + req.readyState + ']\n' + pingURL);
                } else {
                    // alert('Problem sending URL? [' + req.readyState + ']\n' + pingURL);
                }
            }
            req.send(null);
        } else {
            alert("Error [jsmDevicePing]:\n" + pingURL);
        }
    } catch (e) {
        alert("Error [jsmDevicePing]:\n" + e);
    }
};

// ----------------------------------------------------------------------------
// --- shape

/**
*** Parse the specified csv Geozones and display them on the current map
*** @param type     The Geozone shape type
*** @param radiusM  The circle radius, in meters
*** @param points   An array of points (JSMapPoint[])
*** @param zoomTo   True to center/zoom to shape
*** @return True if shape was drawn, false otherwise
**/
function jsmDrawShape(type, radiusM, points, color, zoomTo)
{
    try {
        return jsmap? jsmap.JSDrawShape(type, radiusM, points, color, zoomTo) : false;
    } catch (e) {
        return false; // "JSDrawShape" not defined?
    }
}

// ----------------------------------------------------------------------------
// --- zone points

/**
*** Parse the specified csv Geozones and display them on the current map
*** @param zonePoints  Array of JSMapPoint's
**/
function jsmParseGeozones(zonePoints)
{
    if (!jsmap) { return; }
    // external var: jsvZoneRadiusMeters, jsvZoneEditable

    /* no zones? */
    if (zonePoints == null) {
        return 0;
    }

    /* parse zones */
    var points = [];
    var zoneIndex = zoneMapGetIndex();
    if ((zoneIndex >= 0) && (zoneIndex < zonePoints.length)) {
        // a single point
        var z = zoneIndex;
        if ((zonePoints[z].lat != 0.0) || (zonePoints[z].lon != 0.0)) {
            points.push(zonePoints[z]);
        }
    } else {
        // all points
        for (var z = 0; z < zonePoints.length; z++) {
            if ((zonePoints[z].lat != 0.0) || (zonePoints[z].lon != 0.0)) {
                points.push(zonePoints[z]);
            }
        }
    }

    /* draw Geozone */
    // jsvZoneType:
    //   0 - ZONE_POINT_RADIUS
    //   1 - ZONE_BOUNDED_RECT
    //   2 - ZONE_SWEPT_POINT_RADIUS
    //   3 - ZONE_POLYGON
    jsmap.JSDrawGeozone(jsvZoneType, jsvZoneRadiusMeters, zonePoints, jsvZoneColor, zoneIndex);
    return points.length;

}

// ----------------------------------------------------------------------------
// --- map points

function jsmFormatCoord(loc, isLat, dec)
{
    if ((LATLON_FORMAT == 1) || (LATLON_FORMAT == "DMS")) { // DD^MM'SS"
        var isPos = (loc >= 0.0)? true : false;
        loc = Math.abs(loc);
        var deg = parseInt(loc);
        loc = (loc - deg) * 60.0;
        var min = parseInt(loc);
        if (min <= 9) { min = '0' + min; }
        loc = (loc - min) * 60.0;
        var sec = parseInt(loc);
        if (sec <= 9) { sec = '0' + sec; }
        var quad = isLat? (isPos? HEADING[0] : HEADING[4]) : (isPos? HEADING[2] : HEADING[6]);
        return deg + "&deg;" + min + "'" + sec + "&quot;" + quad;
    } else
    if ((LATLON_FORMAT == 2) || (LATLON_FORMAT == "DM")) {  // DD^MM.mm'
        var isPos = (loc >= 0.0)? true : false;
        loc = Math.abs(loc);
        var deg = parseInt(loc);
        loc = (loc - deg) * 60.0;
        var min = numFormatFloat(loc, LATLON_FORMAT_MIN_DEC); // minutes decimal places
        if (min <= 9) { min = '0' + min; }
        var quad = isLat? (isPos? HEADING[0] : HEADING[4]) : (isPos? HEADING[2] : HEADING[6]);
        return deg + "&deg;" + min + "'" + quad;
    } else {
        return numFormatFloat(loc, dec);
    }
}

/**
*** Creates/Returns a JSMapPushpin object
**/
function jsmCreatePushPin(rcdNdx, dsNdx, ppNdx, evRcd)
{

    var dev    = evRcd.device;
    var dtime  = evRcd.dateFmt + ' ' + evRcd.timeFmt;
    var tmz    = evRcd.timeZone; // _tmz
    var lat    = evRcd.latitude;
    var lon    = evRcd.longitude;
    var flat   = jsmFormatCoord(lat,true ,5);
    var flon   = jsmFormatCoord(lon,false,5);
    var spdfmt = numFormatFloat(evRcd.speedKPH * SPEED_KPH_MULT, 1) + " " + SPEED_UNITS;
    var addr   = evRcd.address;
    var icon   = evRcd.iconNdx;
    var code   = evRcd.code;
    var sats   = evRcd.satCount;
    var satStr = (sats > 0)? (" [" + TEXT_INFO_SATS + " " + sats + "]") : "";

    /* balloon text */
    var html = "";
    html += "<table class='infoBoxTable' cellspacing='1' cellpadding='1' border='0'>";
    html += "<tr class='infoBoxRow'><td class='infoBoxCell'>[#"+rcdNdx+"] &nbsp; <b>"+dev+" : "+code+"</b></td></tr>";
    html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_DATE   +":</b> "+dtime+" ["+tmz+"]</td></tr>";
    html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_GPS    +":</b> "+flat+" / "+flon+" "+satStr+"</td></tr>";
    if (COMBINE_SPEED_HEAD) {
        if (evRcd.speedKPH > 0) {
            html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_SPEED  +":</b> "+spdfmt+" &nbsp;("+evRcd.compass+")</td></tr>";
        } else {
            html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_SPEED  +":</b> "+spdfmt+"</td></tr>";
        }
    } else {
        html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_SPEED  +":</b> "+spdfmt+"</td></tr>";
        if (evRcd.speedKPH > 0) {
            var head = numFormatFloat(evRcd.heading,0) + "&deg; &nbsp;(" + evRcd.compass + ")";
            html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_HEADING+":</b> "+head+"</td></tr>";
        }
    }
    if (SHOW_ALTITUDE) {
        var altfmt = numFormatFloat(evRcd.altitude * ALTITUDE_METERS_MULT,0) + " " + ALTITUDE_UNITS;
        html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_ALTITUDE +":</b> "+altfmt+"</td></tr>";
    }
    if (/*SHOW_ADDR && */((addr != "") || INCL_BLANK_ADDR)) {
        if (addr == "") { addr = '&nbsp;'; }
        html += "<tr class='infoBoxRow'><td class='infoBoxCell'><b>"+TEXT_INFO_ADDR+":</b> "+addr+"</td></tr>";
    }
    if (SHOW_OPT_FIELDS) {
        if (evRcd.optDesc && (evRcd.optDesc.length > 0)) {
            for (var i = 0; i < evRcd.optDesc.length; i++) {
                var v = evRcd.optDesc[i];
                if (v != "") {
                    var d = OptionalEventFieldTitle(i);
                    var r = (d && (d != ""))?  ("<b>"+d+":</b> "+v) : v;
                    html += "<tr class='infoBoxRow'><td class='infoBoxCell'>"+r+"</td></tr>";
                }
            }
        }
    }
    html += "</table>";
    html += "<script type='text/javascript'> jsmHighlightDetailRow("+rcdNdx+",true); </script>\n";

    /* return JSMapPushpin */
    var pp = jsmGetPushPinIcon(icon, evRcd);
    return new JSMapPushpin(
        rcdNdx, dsNdx, ppNdx, evRcd,
        lat, lon, 
        dev, html, 
        pp.iconURL, pp.iconSize, pp.iconOffset, 
        pp.shadow, pp.shadowSize);

};

/**
*** Returns the pushpin object
*** @param icon  Icon index
*** @param e     The current event
**/
function jsmGetPushPinIcon(icon, e)
{
    var pp = ((icon >= 0) && (icon < jsvPushpinIcon.length))?
        jsvPushpinIcon[icon] :
        jsvPushpinIcon[0]; // black
    if (pp.iconEval) {
        try {
            var url = eval(pp.iconEval); // 'e' may be used within this 'eval'
            pp.iconURL = url;
        } catch(err) {
            // Exceptions are possible, since we cannot control what the configuration has specified
            // for the evaluated string.  Also note that this Javascript 'eval' trusts the authority
            // of the configuration admin to not place rogue code into the 'iconSelector'
            if (!jsvPushpinIcon[0].iconEval) {
                pp.iconURL = jsvPushpinIcon[0].iconURL; // default to black icon
            } else {
                pp.iconURL = ""; // unknown icon (icons will show as broken images)
            }
        }
    }
    return pp;
}

// ----------------------------------------------------------------------------
// --- create circle

/**
*** Returns an array of JSMapPoints representing a circle polygon
*** @param center  The circle center (JSMapPoint)
*** @param radiusM The circle radius, in meters
*** @return An array of JSMapPoints
**/
function jsmCreateCircle(center, radiusM)
{
    var rLat = geoRadians(center.lat);  // radians
    var rLon = geoRadians(center.lon);  // radians
    var d    = radiusM / EARTH_RADIUS_METERS;
    var circlePoints = new Array();
    for (x = 0; x <= 360; x += 5) {         // 5 degrees (saves memory, & it still looks like a circle)
        var xrad = geoRadians(x);           // radians
        var tLat = Math.asin(Math.sin(rLat) * Math.cos(d) + Math.cos(rLat) * Math.sin(d) * Math.cos(xrad));
        var tLon = rLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(rLat), Math.cos(d)-Math.sin(rLat) * Math.sin(tLat));
        circlePoints.push(new JSMapPoint(geoDegrees(tLat),geoDegrees(tLon)));
    }
    return circlePoints; // (JSMapPoint[])
}

/**
*** Returns a point that is 'radiusM' from the specified lat/lon in the 'heading' direction
*** @param lat     The Latitude
*** @param lon     The Longitude
*** @param radiusM The radius, in meters
*** @param heading The compass heading
*** @return A JSMapPoint
**/
function jsmCalcRadiusPoint(lat, lon, radiusM, heading)
{
    var crLat = geoRadians(lat);          // radians
    var crLon = geoRadians(lon);          // radians
    var d     = radiusM / EARTH_RADIUS_METERS;
    var xrad  = geoRadians(heading);            // radians
    var rrLat = Math.asin(Math.sin(crLat) * Math.cos(d) + Math.cos(crLat) * Math.sin(d) * Math.cos(xrad));
    var rrLon = crLon + Math.atan2(Math.sin(xrad) * Math.sin(d) * Math.cos(crLat), Math.cos(d)-Math.sin(crLat) * Math.sin(rrLat));
    return new JSMapPoint(geoDegrees(rrLat), geoDegrees(rrLon));
};

// ----------------------------------------------------------------------------
// --- detail report

/**
*** Attempts to show the pushpin info bubble at the specified index
*** @param ppNdx  The pushpin index
**/
function jsmShowDetailPushpin(dsNdx, ppNdx)
{
    if (!jsmap) { return; }

    /* skip pushpin box? */
    if (!DETAIL_INFO_BOX || (dsNdx < 0) || (ppNdx < 0)) {
        // skip info box/bubble
        //alert("Skip show pushpin detail box ...");
        return;
    }

    /* no datasets? */
    if ((jsvDataSets == null) || (jsvDataSets.length <= 0)) {
        // skip info box/bubble
        //alert("No Datasets ... " + dsNdx);
        return;
    } else
    if (dsNdx >= jsvDataSets.length) {
        // skip info box/bubble
        //alert("Invalid Index ... " + dsNdx);
        return;
    }

    /* show pushpin */
    var jpp = jsvDataSets[dsNdx].pushPins;
    if (jpp && (ppNdx < jpp.length)) {
        jsmap.JSShowPushpin(jpp[ppNdx], jsvDetailCenterPushpin);
    } else {
        //alert("Invalid pushpin list or invalid index");
    }

}

/**
*** Shows the 'Location Details' table
**/
function jsmShowDetailReport() 
{

    /* clear details */
    var parentWin = null;
    var dpt = jsvDetailPoints; // JSDetailPoint[]
    if (!jsvDetailVisible || (dpt == null) || (dpt.length <= 0)) {
        /* destroy location detail report */
        if (DETAILS_WINDOW) {
            if (jsvDetailsWindow != null) {
                jsvDetailsWindow.close();
                jsvDetailsWindow = null;
            }
        } else {
            var tableDiv = document.getElementById(ID_DETAIL_TABLE);
            if (tableDiv != null) {
                tableDiv.innerHTML = "";
                tableDiv.style.display = "none";
                // TODO: need to refresh/redraw parent
            }
        }
        /* reset control to "Show Location Details" text */
        var detailControl = document.getElementById(ID_DETAIL_CONTROL);
        if (detailControl != null) {
            detailControl.innerHTML = TEXT_showLocationDetails;
        }
        return;
    }

    /* open element to contain report */
    var winVar = "window";
    var tableDiv = null;
    if (DETAILS_WINDOW) {
        winVar = "opener";
        if (jsvDetailsWindow == null) {
            var W = SHOW_ADDR? 600 : 500;
            var H = 300;
            var L = ((screen.width  - W) / 2);
            var T = ((screen.height - H) / 2);
            var attr = "resizable=yes,scrollbars=yes";
            attr += ",width=" + W + ",height=" + H;
            attr += ",screenX=" + L + ",screenY=" + T + ",left=" + L + ",top=" + T;
            jsvDetailsWindow = window.open("", "TrackLocationDetail", attr, true);
            if (jsvDetailsWindow != null) {
                jsvDetailsWindow.document.write("<html>\n");
                jsvDetailsWindow.document.write("<body onunload=\"opener.jsvDetailVisible=false;opener.jsvDetailsWindow=null;\">\n");
                jsvDetailsWindow.document.write("<center><div id='"+ID_DETAIL_TABLE+"'></div></center>");
                jsvDetailsWindow.document.write("</body>\n");
                jsvDetailsWindow.document.write("</html>\n");
                jsvDetailsWindow.moveTo(L,T);
                jsvDetailsWindow.focus();
                tableDiv = jsvDetailsWindow.document.getElementById(ID_DETAIL_TABLE);
            } else {
                // unable to create window?
                return;
            }
        } else {
            jsvDetailsWindow.focus();
        }
    } else {
        winVar = "window";
        tableDiv = document.getElementById(ID_DETAIL_TABLE);
    }
    if (tableDiv == null) {
        return;
    }
    tableDiv.innerHTML = "";

    /* generate HTML table eader */
    var html = "";
    html += "<table cellspacing='0' cellpadding='0' border='1'>\n";
    html += "<thead>\n";
    if (jsvPartialData) {
        var columns = 4 + (IS_FLEET?1:0) + (SHOW_ADDR?1:0) + (COMBINE_SPEED_HEAD?1:2);
        html += "<tr class='"+CLASS_DETAILS_HEADER_ROW+"'>";
        html += "<th class='"+CLASS_DETAILS_HEADER_COL+"' colSpan='"+columns+"' valign='center' style='color:red; border-bottom: 1px solid black;'>"+TEXT_MAXPUSHPINS_MSG+"</th>";
        html += "</tr>\n";
    }
    html += "<tr class='"+CLASS_DETAILS_HEADER_ROW+"'>";
    html += _jsmShowDetailReport_header( 25, "#");
    if (IS_FLEET) { html += _jsmShowDetailReport_header(-1, TEXT_DEVICE); }
    html += _jsmShowDetailReport_header( -1, TEXT_DATE);
    html += _jsmShowDetailReport_header( -1, TEXT_CODE);
    html += _jsmShowDetailReport_header( -1, TEXT_LATLON);
    if (SHOW_SAT_COUNT) {
        html += _jsmShowDetailReport_header( -1, TEXT_SATCOUNT);
    }
    if (COMBINE_SPEED_HEAD) { 
        html += _jsmShowDetailReport_header( -1, TEXT_SPEED);
    } else {
        html += _jsmShowDetailReport_header( -1, TEXT_SPEED);
        html += _jsmShowDetailReport_header( -1, TEXT_HEADING);
    }
    if (SHOW_ADDR) { 
        html += _jsmShowDetailReport_header( -1, TEXT_ADDR); 
    }
    if (SHOW_OPT_FIELDS) {
        for (var opti = 0; opti < OptionalEventFieldCount(); opti++) {
            var d = OptionalEventFieldTitle(opti);
            html += _jsmShowDetailReport_header( -1, d); 
        }
    }
    html += "</tr>\n";
    html += "</thead>\n";
    
    /* generate HTML table body */
    html += "<tbody>\n";
    var lastDevice = "";
    for (var i = 0; i < dpt.length; i++) {
        var pt = jsvDetailAscending? dpt[i] : dpt[dpt.length - i - 1]; // JSDetailPoint
        // new device?
        var isNew = jsvUseDeviceBreaks && (lastDevice != pt.device);
        var dataClass = isNew? CLASS_DETAILS_DATA_COL_NEW : CLASS_DETAILS_DATA_COL;
        lastDevice = pt.device;
        // class
        var isOdd = ((pt.index & 1) == 1);
        var rowClass = isOdd? CLASS_DETAILS_ROW_ODD : CLASS_DETAILS_ROW_EVEN;
        html += "<tr class='"+rowClass+"' id='"+(ID_DETAIL_ROW_+pt.index)+"'";
        if (pt.color != "") {
            var c = pt.color;
            if (DISPLAY_COLOR_TYPE == 1) {
                html += " style='color:"+c+";'";
            } else
            if (DISPLAY_COLOR_TYPE == 2) {
                if (isOdd) {
                    var RGB = rgbLighter(rgbVal(c),0.35);
                    c = "#" + rgbHex(RGB.R,RGB.G,RGB.B);
                }
                html += " style='background-color:"+c+";'";
            }
        }
        html += ">";
        // index
        if (DETAIL_INFO_BOX) {
            html += "<td nowrap class='"+CLASS_DETAILS_INDEX_COL+"' onclick=\"javascript:"+winVar+".jsmShowDetailPushpin("+pt.dsNdx+","+pt.ppNdx+")\">" + pt.index + "</td>";
        } else {
            html += "<td nowrap class='"+dataClass+"'>" + pt.index + "</td>";
        }
        // device id (fleet only)
        if (IS_FLEET) {
            html += "<td nowrap class='"+dataClass+"'>" + pt.device + "</td>"; 
        }
        // date/time
        html += "<td nowrap class='"+dataClass+"'>" + pt.dateTime + "</td>";
        // status code
        html += "<td nowrap class='"+dataClass+"'>" + pt.code     + "</td>";
        // latitude/longitude
        html += "<td nowrap class='"+dataClass+"'>" + pt.latlon   + "</td>";
        // # Sats
        if (SHOW_SAT_COUNT) {
            html += "<td nowrap class='"+dataClass+"'>" + pt.satCount + "</td>";
        }
        if (COMBINE_SPEED_HEAD) {
            // speed/compass
            var spdHead = (pt.speed > 0)? (pt.speed + " " + pt.compass) : pt.speed;
            html += "<td nowrap class='"+dataClass+"'>" + spdHead  + "</td>";
        } else {
            // speed
            html += "<td nowrap class='"+dataClass+"'>" + pt.speed  + "</td>";
            // heading
            html += "<td nowrap class='"+dataClass+"'>" + pt.heading + "&deg; " + pt.compass + "</td>";
        }
        // address
        if (SHOW_ADDR) { 
            html += "<td nowrap class='"+dataClass+"'>" + pt.address + "&nbsp;</td>"; 
        }
        // optional fields
        if (SHOW_OPT_FIELDS) {
            for (var opti = 0; opti < OptionalEventFieldCount() && (opti < 10); opti++) {
                var v = (pt.optDesc && (opti < pt.optDesc.length))? pt.optDesc[opti] : "";
                html += "<td nowrap class='"+dataClass+"'>" + v + "&nbsp;</td>"; 
            }
        }
        html += "</tr>\n";
    }
    html += "</tbody>\n";
    html += "</table>\n";

    /* write HTML into table DIV */
    if (DETAILS_WINDOW) {
        // separate window
        tableDiv.innerHTML = html;
    } else {
        // inline (under map)
        var tableHTML = "";
        tableHTML += "<div class='"+CLASS_DETAILS_DIV+"'>" + html + "</div>";
        tableDiv.innerHTML = tableHTML;
        tableDiv.style.display = "block";
    }

    /* set control to "Hide Location Details" */
    var detailControl = document.getElementById(ID_DETAIL_CONTROL);
    if (detailControl != null) {
        detailControl.innerHTML = TEXT_hideLocationDetails;
    }

};

function _jsmShowDetailReport_header(W, T)
{
    var TH = "<th class='"+CLASS_DETAILS_HEADER_COL+"' nowrap valign='center'"
    if (W > 0) { TH += " width='"+W+"'"; }
    TH += ">"+T+"</th>";
    return TH;
};

function _jsmHighlightDetailRow(rowNdx, highlight)
{
    var detailsWin = DETAILS_WINDOW? jsvDetailsWindow : window;
    if ((detailsWin != null) && (rowNdx >= 1)) {
        var row = detailsWin.document.getElementById(ID_DETAIL_ROW_ + rowNdx);
        if (row != null) {
            if (highlight) {
                row.className = CLASS_DETAILS_ROW_HILITE;
            } else {
                var isOdd = ((rowNdx & 1) == 1);
                row.className = isOdd? CLASS_DETAILS_ROW_ODD : CLASS_DETAILS_ROW_EVEN;
            }
        }
    }
}

function jsmHighlightDetailRow(rcdNdx, highlight)
{
    // remove old highlight
    if (jsvDetailsLastHilightedRow != null) {
        _jsmHighlightDetailRow(jsvDetailsLastHilightedRow.row, false);
        jsvDetailsLastHilightedRow = null;
    }
    // assign new highlight
    var rowNdx = parseInt(rcdNdx);
    if ((rowNdx >= 1) && highlight) {
        _jsmHighlightDetailRow(rowNdx, true);
        if (highlight) {
            jsvDetailsLastHilightedRow = { row:rowNdx };
        }
    }
}

// ----------------------------------------------------------------------------
// --- set replay state

function jsmSetReplayState(state)
{
    // REPLAY_STOPPED
    // REPLAY_PAUSED
    // REPLAY_RUNNING
    try {
        var btn = ID_MAP_REPLAY_BTN? document.getElementById(ID_MAP_REPLAY_BTN) : null;
        if (btn) {
            if (state == REPLAY_RUNNING) {
                // Replay is running, option is "Pause"
                btn.src = "images/Pause20.png";
            } else
            if (state == REPLAY_PAUSED) {
                // Replay if paused, option is "Continue"
                btn.src = "images/Continue20.png";
            } else {
                // Replay is stopped, option is "Play"
                btn.src = "images/Play20.png";
            }
        }
    } catch (e) {
        // may occur if ID_MAP_REPLAY_BTN is not defined
    }
}

// ----------------------------------------------------------------------------
// --- set Lat/Lon/Distance display

/**
*** Returns the distance display 'div' element (or null if not found)
**/
function jsmGetDistanceDisplayElement()
{
    return document.getElementById(ID_DISTANCE_DISPLAY);
};

/**
*** Displays the specified distance value
*** @param distM  The distance value, in meters
**/
function jsmSetDistanceDisplay(distM)
{
    var distItem = jsmGetDistanceDisplayElement();
    if (distItem != null) {
        //if (distM < 1.0) {
        //    distItem.innerHTML = '';
        //} else
        if (jsvGeozoneMode) {
            // always meters
            var d = numFormatFloat(distM,0) + ' ' + TEXT_METERS;
            distItem.innerHTML = d;
        } else {
            // distance units
            var dist = DISTANCE_KM_MULT * distM / 1000.0;
            var d = numFormatFloat(dist,2) + ' ' + TEXT_DISTANCE;
            distItem.innerHTML = d;
        }
    }
};

/**
*** Returns the Lat/Lon display 'div' element (or null if not found)
**/
function jsmGetLatLonDisplayElement()
{
    return document.getElementById(ID_LAT_LON_DISPLAY);
};

/**
*** Displays the specified Lat/Lon value
*** @param lat  The Latitude
*** @param lon  The Longitude
**/
function jsmSetLatLonDisplay(lat,lon)
{
    var latlonItem = jsmGetLatLonDisplayElement();
    if (latlonItem != null) {
        var dec = jsvGeozoneMode? 5 : 4;
        if ((lat == 0) && (lon == 0)) {
            // may want to handle this case differently
            var loc = jsmFormatCoord(0.0,true ,dec) + ', ' + jsmFormatCoord(0.0,false,dec)
            latlonItem.innerHTML = loc;
        } else {
            // display latitude/longitude
            var loc = jsmFormatCoord(lat,true ,dec) + ', ' + jsmFormatCoord(lon,false,dec)
            latlonItem.innerHTML = loc;
        }
    }
};

// ----------------------------------------------------------------------------
// --- set Zone values

/**
*** Sets/Displays the specified GeoZone value
*** @param lat      The Zone Latitude
*** @param lon      The Zone Longitude
*** @param radiusM  The Zone radius, in meters
**/
function jsmSetPointZoneValue(lat, lon, radiusM)
{
    var ndx = zoneMapGetIndex();
    _jsmSetPointZoneValue(ndx, lat, lon, radiusM);
};

/**
*** Sets/Displays the specified GeoZone value
*** @param lat      The Zone Latitude
*** @param lon      The Zone Longitude
*** @param radiusM  The Zone radius, in meters
**/
function _jsmSetPointZoneValue(ndx, lat, lon, radiusM)
{
    if ((ndx >= 0) && (ndx < jsvZoneList.length)) {

        /* set array values */
        jsvZoneList[ndx].lat = lat;
        jsvZoneList[ndx].lon = lon;

        /* set GeoZone display values */
        jsmSetIDValue(ID_ZONE_LATITUDE_  + ndx, numFormatFloat(lat,5));
        jsmSetIDValue(ID_ZONE_LONGITUDE_ + ndx, numFormatFloat(lon,5));
        jsmSetIDValue(ID_ZONE_RADIUS_M        , radiusM);

        /* display (remove for production?) */
        jsmSetLatLonDisplay(lat, lon);
        jsmSetDistanceDisplay(radiusM);
    
    }
};

// ----------------------------------------------------------------------------
// --- misc

/**
*** Gets the 'value' of the specified element
**/
function jsmGetElementValue(item)
{
    return (item != null)? item.value : null;
};

/**
*** Sets the 'value' of the specified element
**/
function jsmSetElementValue(item, value)
{
    if (item != null) {
        item.value = value; 
    }
};

/**
*** Gets the 'value' of the specified element ID
**/
function jsmGetIDValue(idName)
{
    return jsmGetElementValue(document.getElementById(idName));
};

/**
*** Sets the 'value' of the specified element ID
**/
function jsmSetIDValue(idName, value)
{
    jsmSetElementValue(document.getElementById(idName), value);
};

/**
*** Sets the 'innerHTML' of the specified element
**/
function jsmSetElementInnerHTML(item, html)
{
    if (item != null) { 
        item.innerHTML = html; 
    }
};

/**
*** Sets the 'innerHTML' of the specified element ID
**/
function jsmSetIDInnerHTML(idName, html)
{
    jsmSetElementInnerHTML(document.getElementById(idName), html);
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Creates and returns a custom map marker URL based on the Google Chart api.
*** Developer's Guide: http://code.google.com/apis/chart/
*** Usage Policy     : http://code.google.com/apis/chart/#usage
**/
function jsmCustomMapMarker(W, H, C)
{
    // The following assumptions are made regarding the generated map marker image:
    //  - The 'corner' color is the same as the 'fill' color
    //  - The border is always black
    //  - No transparency
    return "http://chart.apis.google.com/chart?cht=mm&ext=.png&chs="+W+"x"+H+"&chco="+C+"FF,"+C+"FF,000000FF";
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

var mapControlVisible = true;
function jsmControlToggleCollapse()
{
    var mapCtl    = document.getElementById(ID_MAP_CONTROL);
    var mapCtlBar = document.getElementById(ID_MAP_CONTROL_BAR);
    if (mapCtl != null) {
        if (mapControlVisible) {
            // is visible, make invisible
            mapCtl.style.display = "none";
            if (mapCtlBar) { mapCtlBar.className = CLASS_CONTROL_BAR[1]; }
            mapControlVisible = false;
        } else {
            // is invisible, make visible
            mapCtl.style.display = "";
            if (mapCtlBar) { mapCtlBar.className = CLASS_CONTROL_BAR[0]; }
            mapControlVisible = true;
        }
    }
}

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

/**
*** Calculate and return a color based on the event speed.
*** This algorithm performs the following:
***  - If the speed is greater than 70 mph, the returned color is green.
***  - If the speed is between 20 and 70 mph, the color will be a mix between yellow and green.
***  - If the speed is between  0 and 20 mph, the color will be a mix between red and yellow.
*** @param e  The 'MapEventRecord' object
**/
function evSpeedColor(e)
{
    var mph = e.speedMPH;
    var green  = [   0, 210, 0 ];
    var yellow = [ 240, 240, 0 ];
    var red    = [ 255,   0, 0 ];
    if (mph > 70.0) {
        // return green
        return rgbHex(green[0], green[1], green[2]);
    } else
    if (mph >= 20.0) {
        // fade from yellow to green
        var D = (mph - 20.0) / (70.0 - 20.0);
        var R = ((green[0] - yellow[0]) * D) + yellow[0];
        var G = ((green[1] - yellow[1]) * D) + yellow[1];
        var B = ((green[2] - yellow[2]) * D) + yellow[2];
        return rgbHex(R, G, B);
    } else
    if (mph >= 0.0) {
        // fade from red to yellow
        var D = (mph - 0.0) / (20.0 - 0.0);
        var R = ((yellow[0] - red[0]) * D) + red[0];
        var G = ((yellow[1] - red[1]) * D) + red[1];
        var B = ((yellow[2] - red[2]) * D) + red[2];
        return rgbHex(R, G, B);
    } else {
        // return black
        return "000000";
    }
        
};

/**
*** Returns an icon URL based on the event speed.
*** Analyzes the event 'speed' and creates an icon marker URL on the fly that fades from 
*** RED (stopped) to YELLOW (slow) to GREEN (fast).  It uses the Google Charts API to create the map 
*** marker icon [http://code.google.com/apis/chart/].  The first 2 arguments are the desired icon
*** width and height (required since it is dynamically creating this icon).
*** Example usage (in the Pushpins tag, within a MapProvider):
***   <Pushpin key="moving" eval="evSpeedMarkerURL(16,24,e)" iconSize="16,24" iconOffset="8,24"/>
*** @param W  The pushpin width
*** @param H  The pushpin height
*** @param e  The 'MapEventRecord' object
**/
function evSpeedMarkerURL(W,H,e)
{
    return jsmCustomMapMarker(W, H, evSpeedColor(e));
};

// ----------------------------------------------------------------------------

/**
*** Returns an icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL(e)
{
    if (e.speedKPH < 5.0) {
        // probably not moving
        return "images/pp/pin30_red_dot.png";
    } else
    if (e.speedKPH < 32.0) {
        // 5 <= X < 32
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_yellow_h"+x+".png";
    } else {
        // 32 <= X
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_green_h"+x+".png";
    }
};

/**
*** Returns an icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL_eu(e)
{
    var speedMPH = e.speedKPH * 1.609344;
    if (speedMPH < 5.0) {
        // probably not moving
        return "images/pp/pin30_red_dot.png";
    } else
    if (speedMPH < 50.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_yellow_h"+x+".png";
    } else
    if (speedMPH < 90.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_green_h"+x+".png";
    } else
    if (speedMPH < 110.0) {
        return "images/pp/pin30_gray.png";
    } else {
        return "images/pp/pin30_black.png";
    }
};

/**
*** Returns an icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingMarkerURL_ca(e)
{
    if (e.speedKPH < 1.0) {
        // probably not moving
        return "images/pp/pin30_red.png";
    } else
    if (e.speedKPH < 70.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_yellow_h"+x+".png";
    } else
    if (e.speedKPH < 100.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_green_h"+x+".png";
    } else
    if (e.speedKPH < 130.0) {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_blue_h"+x+".png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_gray_h"+x+".png";
    }
};

// ----------------------------------------------------------------------------

/**
*** Returns a 'yellow' icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingYellowURL(e)
{
    if (e.speedKPH < 1.0) {
        // probably not moving
        return "images/pp/pin30_yellow.png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_yellow_h"+x+".png";
    }
};

/**
*** Returns a 'green' icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function evHeadingGreenURL(e)
{
    if (e.speedKPH < 1.0) {
        // probably not moving
        return "images/pp/pin30_green_dot.png";
    } else {
        var x = Math.round(e.heading / 45.0) % 8;
        return "images/pp/pin30_green_h"+x+".png";
    }
};

// ----------------------------------------------------------------------------
// custom label markers

/**
*** Returns a pushpin icon URL which includes the event index
*** @param e  The 'MapEventRecord' object
**/
function evIndexedIconURL(e)
{
    // http://localhost:8080/track/Marker?icon=/images/pp/pin30_blue_fill.png&fr=3,4,11,7,9,Serif&color=880000&text=99
    var tx = e.index;
    return evTextLabelIconURL("/images/pp/pin30_blue_fill.png", "3,4,11,7,9,Serif", "", "", "880000", tx);
};

/**
*** Returns a label icon URL which includes the device short name
*** @param e  The 'MapEventRecord' object
**/
function evDeviceNameIconURL(e, fill, border)
{
    // http://localhost:8080/track/Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,9&text=Demo2&border=red&fill=yellow
    var tx = e.devVIN;
    var fc = fill?   fill   : "";
    var bc = border? border : "";
    return evTextLabelIconURL("/images/pp/label47_fill.png", "3,2,42,13,9", fc, bc, "", tx);
};

/**
*** Returns a label icon URL which includes the ID/Name at the specified index
*** @param e  The 'MapEventRecord' object
*** @param x  The 'optional' field index with the event object
**/
function evOptFieldIconURL(e,x)
{
    var text = (SHOW_OPT_FIELDS && e.optDesc && (x >= 0) && (x < e.optDesc.length))? e.optDesc[x] : "";
    return evTextLabelIconURL("/images/pp/label47_fill.png", "3,2,42,13,9", "", "yellow", "", text);
};

/**
*** Returns a label icon URL which includes the device short name
*** @param e    The 'MapEventRecord' object
*** @param icon The image URL
*** @param fr   The draw 'frame'
*** @param fill The fill-color
**/
function evLabelIconURL(e, icon, fr, fill)
{
    var text = e.devVIN;
    return evTextLabelIconURL(icon, fr, fill, "", "", text);
};

/**
*** Returns a label icon URL which includes the device short name
*** @param e    The 'MapEventRecord' object
*** @param icon The image URL
*** @param fr   The draw 'frame'
**/
function evArrowIconURL(e, icon, fr)
{
    var arrow = e.heading;
    return evArrowLabelIconURL(icon, fr, "", "", "000000", arrow);
};

/**
*** Returns a label icon URL which includes the device short name
**/
function evTextLabelIconURL(icon, fr, fill, border, color, text)
{
    // Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,8&text=Demo2&border=red&fill=yellow
    // Options:
    //   icon=<PathToImageIcon>                             - icon URI (relative to "/track/")
    //   fr=<XOffset>,<YOffset>,<Width>,<Height>,<FontPt>   - text frame definition
    //   fill=<FillColor>                                   - text frame fill color
    //   border=<BorderColor>                               - text frame border color
    //   color=<TextColor>                                  - text color
    //   text=<ShortText>                                   - text
    var url = 
        "Marker?"  + 
        "icon="    + icon + 
        "&fr="     + fr + 
        "&fill="   + fill + 
        "&border=" + border + 
        "&color="  + color + 
        "&text="   + strEncode(text);
    //alert("Marker URL: " + url);
    return url;
};

/**
*** Returns a label icon URL which includes an arrow pointing in the specified direction
**/
function evArrowLabelIconURL(icon, fr, fill, border, color, heading)
{
    // Marker?icon=/images/pp/label47_fill.png&fr=3,2,42,13,10&border=red&fill=yellow&arrow=120
    // Options:
    //   icon=<PathToImageIcon>                             - icon URI (relative to "/track/")
    //   fr=<XOffset>,<YOffset>,<Width>,<Height>,<FontPt>   - frame definition
    //   fill=<FillColor>                                   - frame fill color
    //   border=<BorderColor>                               - frame border color
    //   color=<TextColor>                                  - arrow color
    //   arrow=<Heading>                                    - arrow direction
    var url = 
        "Marker?"  + 
        "icon="    + icon + 
        "&fr="     + fr + 
        "&fill="   + fill + 
        "&border=" + border + 
        "&color="  + color + 
        "&arrow="  + heading;
    //alert("Marker URL: " + url);
    return url;
};

// ----------------------------------------------------------------------------

/**
*** Returns an icon URL based on the event heading
*** @param e  The 'MapEventRecord' object
**/
function jsmBatteryLevelIMG(lvl)
{
    if (lvl > 1.5) { lvl = lvl / 100.0; }
    var battLevelType = 1;
    try {
        battLevelType = BATTERY_LEVEL_TYPE;
    } catch (e) {
        battLevelType = 1;
    }
    var battIcon = "";
    if (battLevelType == 2) {
        // percent
        if (lvl <= 0.01) {
            battIcon = "images/Batt000.png";
        } else {
            var battLvl = numParseInt(((lvl * 100.0) + 0.5), 0);
            if (battLvl > 99) { battLvl = 99; }
            battIcon = "Marker?icon=/images/Batt000.png&fr=5,2,25,12,10&text="+battLvl+"%25";
        }
    } else {
        //icon
        if (lvl <= 0.01) {
            battIcon = "images/Batt000.png";
        } else
        if (lvl <= 0.25) {
            battIcon = "images/Batt025.png";
        } else
        if (lvl <= 0.50) {
            battIcon = "images/Batt050.png";
        } else
        if (lvl <= 0.70) {
            battIcon = "images/Batt070.png";
        } else
        if (lvl <= 0.90) {
            battIcon = "images/Batt090.png";
        } else {
            battIcon = "images/Batt100.png";
        }
    }
    return "<img src=\""+battIcon+"\"/>";
};

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// --- accessed by TrackMap/ZoneInfo/ReportDisplay

var recursiveRefresh = 0;

/**
*** Device ping
**/
function mapDevicePing(pingURL) 
{ // required function 
    jsmDevicePing(pingURL);
};

/**
*** Parse CSV points and display on map 
*** @param zonePoints  Array of JSMapPoint's
**/
function mapProviderParseZones(zonePoints) 
{
    jsMapInit();
    jsmParseGeozones(zonePoints);
};

/**
*** Parse XML points and display on map 
**/
function mapProviderParseXML(cvsPoints) 
{ // required function 
    jsMapInit();
    jsmParseXMLPoints(cvsPoints, jsmRecenterZoomMode(RECENTER_ZOOM), 0);
};

/**
*** Update map, and recenter if specified
**/
function mapProviderUpdateMap(mapURL, recenterMode, replay) 
{ // required function 
    if (recursiveRefresh != 0) { return; } // we're already in a 'update' 
    recursiveRefresh++;
    jsMapInit();
    jsmLoadPoints(mapURL, recenterMode, replay);
    recursiveRefresh--;
};

/**
*** Stop replay (if running)
**/
function mapProviderPauseReplay(replay) 
{
    return jsmap? jsmap.JSPauseReplay(replay) : REPLAY_STOPPED;
}

/**
*** Display "Location Details" report if it is hidden, else hide
**/
function mapProviderToggleDetails() 
{ // required function
    jsvDetailVisible = DETAILS_WINDOW? true : !jsvDetailVisible;
    jsmShowDetailReport();
};

/**
*** Unload any map resources
**/
function mapProviderUnload() 
{ // required function 
    jsmUnload();
};

// ----------------------------------------------------------------------------
