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
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/03/31  Martin D. Flynn
//     -Added new status codes:
//      STATUS_INITIALIZED, STATUS_WAYMARK
//  2006/06/17  Martin D. Flynn
//      Copied from the OpenDMTP Server package.
//      OpenDMTP Protocol Definition v0.1.1 Conformance: These status-code values 
//      conform to the definition specified by the OpenDMTP protocol and must 
//      remain as specified here.  When extending the status code values to
//      encompass other purposes, it is recommended that values in the following
//      range be used: 0x0001 to 0xDFFF
//  2007/01/25  Martin D. Flynn
//     -Added new status codes:
//      STATUS_QUERY, STATUS_LOW_BATTERY, STATUS_OBC_FAULT, STATUS_OBC_RANGE,
//      STATUS_OBC_RPM_RANGE, STATUS_OBC_FUEL_RANGE, STATUS_OBC_OIL_RANGE,
//      STATUS_OBC_TEMP_RANGE, STATUS_MOTION_MOVING
//     -Changed "Code" descriptions to start their indexing at '1' (instead of '0') 
//      since this string value is used to display to the user on various reports.
//  2007/03/11  Martin D. Flynn
//     -'GetCodeDescription' defaults to hex value of status code (or status code
//      name) if code is not found in the table.
//  2007/03/30  Martin D. Flynn
//     -Added new status code: STATUS_POWER_FAILURE
//  2007/04/15  Martin D. Flynn
//     -Added new status codes: STATUS_GEOBOUNDS_ENTER, STATUS_GEOBOUNDS_EXIT
//  2007/11/28  Martin D. Flynn
//     -Added new status codes: STATUS_EXCESS_BRAKING
//  2008/04/11  Martin D. Flynn
//     -Added "IsDigitalInput..." methods
//  2008/07/27  Martin D. Flynn
//     -Changed 'description' of digital inputs/outputs to start at '0' (instead of '1')
//     -Added "GetDigitalInputIndex".
//  2008/09/01  Martin D. Flynn
//     -Changed status 'description' index's to start at '0' to match the status 'name'.
//  2008/10/16  Martin D. Flynn
//     -Added the following status codes: STATUS_MOTION_IDLE, STATUS_POWER_RESTORED,
//      STATUS_WAYMARK_3,  STATUS_INPUT_ON_08/09, STATUS_INPUT_OFF_08/09, 
//      STATUS_OUTPUT_ON_08/09, STATUS_OUTPUT_OFF_08/09
//  2009/01/01  Martin D. Flynn
//     -Internationalized StatusCode descriptions.
//  2009/05/01  Martin D. Flynn
//     -Added STATUS_GPS_EXPIRED, STATUS_GPS_FAILURE, STATUS_CONNECTION_FAILURE
//     -Added STATUS_IGNITION_ON, STATUS_IGNITION_OFF
//  2009/12/16  Martin D. Flynn
//     -Added Garmin GFMI status codes.
//  2010/04/11  Martin D. Flynn
//     -Added STATUS_WAYMARK_4..8, STATUS_NOTIFY, STATUS_IMPACT, STATUS_PANIC_*
//      STATUS_ASSIST_*, STATUS_MEDICAL_*, STATUS_OBC_INFO_#, STATUS_CONFIG_RESET, ...
//  2010/05/24  Martin D. Flynn
//     -Added STATUS_DAY_SUMMARY
//  2010/07/04  Martin D. Flynn
//     -Added STATUS_BATTERY_LEVEL
//  2010/09/09  Martin D. Flynn
//     -Added STATUS_PARKED, STATUS_SHUTDOWN, STATUS_SUSPEND, STATUS_RESUME,
//      STATUS_LAST_LOCATION
//  2010/10/25  Martin D. Flynn
//     -Added STATUS_SAMPLE_#, STATUS_TOWING_START, STATUS_TOWING_STOP
// ----------------------------------------------------------------------------
package org.opengts.db;

import java.util.*;

import org.opengts.util.*;

public class StatusCodes
{
    
    /* Digital Input index for explicit STATUS_IGNITION_ON/STATUS_IGNITION_OFF codes */
    public static final int IGNITION_INPUT_INDEX        = 99;

// ----------------------------------------------------------------------------
// Reserved status codes: [E0-00 through FF-FF]
// Groups:
//      0xF0..  - Generic
//      0xF1..  - Motion
//      0xF2..  - Geofence
//      0xF4..  - Digital input/output
//      0xF6..  - Sensor input
//      0xF7..  - Temperature input
//      0xF9..  - OBC/J1708
//      0xFD..  - Device status
// ----------------------------------------------------------------------------

    public static final int STATUS_IGNORE               = -1;

// ----------------------------------------------------------------------------
// Reserved: 0x0000 to 0x0FFF
// No status code: 0x0000

    public static final int STATUS_NONE                 = 0x0000;

// ----------------------------------------------------------------------------
// Available: 0x1000 to 0xCFFF
    public static final int STATUS_1000                 = 0x1000;
    // ...
    public static final int STATUS_CFFF                 = 0xCFFF;

// ----------------------------------------------------------------------------
// Reserved: 0xD000 to 0xEFFF

    // Garmin GFMI interface [0xE100 - 0xE1FF]
    public static final int STATUS_GFMI_CMD_03          = 0xE103;   // 57603 send non-ack message
    public static final int STATUS_GFMI_CMD_04          = 0xE104;   // 57604 send ack message
    public static final int STATUS_GFMI_CMD_05          = 0xE105;   // 57605 send answerable message
    public static final int STATUS_GFMI_CMD_06          = 0xE106;   // 57606 send stop location
    public static final int STATUS_GFMI_CMD_08          = 0xE108;   // 57608 request stop ETA
    public static final int STATUS_GFMI_CMD_09          = 0xE109;   // 57609 set auto arrival criteria
    public static final int STATUS_GFMI_LINK_OFF        = 0xE110;   // 57616 GFMI Link lost
    public static final int STATUS_GFMI_LINK_ON         = 0xE111;   // 57617 GFMI Link established
    public static final int STATUS_GFMI_ACK             = 0xE1A0;   // 57760 received ACK
    public static final int STATUS_GFMI_MESSAGE         = 0xE1B1;   // 57777 received message
    public static final int STATUS_GFMI_MESSAGE_ACK     = 0xE1B2;   // 57778 received message ACK

// ----------------------------------------------------------------------------
// Generic codes: 0xF000 to 0xF0FF

    public static final int STATUS_INITIALIZED          = 0xF010;   // 61456
    // Description:
    //      General Status/Location information (event generated by some
    //      initialization function performed by the device).

    public static final int STATUS_LOCATION             = 0xF020;   // 61472
    // Description:
    //      General Status/Location information.  This status code indicates
    //      no more than just the location of the device at a particular time.

    public static final int STATUS_LAST_LOCATION        = 0xF025;   // 61477
    // Description:
    //      General Status/Location information.  This status code indicates
    //      the last known location of the device (GPS may not be current)

    public static final int STATUS_WAYMARK_0            = 0xF030;   // 61488
    public static final int STATUS_WAYMARK_1            = 0xF031;   // 61489
    public static final int STATUS_WAYMARK_2            = 0xF032;   // 61490
    public static final int STATUS_WAYMARK_3            = 0xF033;   // 61491
    public static final int STATUS_WAYMARK_4            = 0xF034;   // 61492
    public static final int STATUS_WAYMARK_5            = 0xF035;   // 61493
    public static final int STATUS_WAYMARK_6            = 0xF036;   // 61494
    public static final int STATUS_WAYMARK_7            = 0xF037;   // 61495
    public static final int STATUS_WAYMARK_8            = 0xF038;   // 61496
    // Description:
    //      General Status/Location information (event generated by manual user
    //      intervention at the device. ie. By pressing a 'Waymark' button).
    // Notes:
    //      - This status code can also be used to indicate a "PANIC" situation
    //      by redefining the description for the chosen code (this can be done
    //      in the "StatusCodes" table).

    public static final int STATUS_QUERY                = 0xF040;   // 61504
    // Description:
    //      General Status/Location information (event generated by 'query'
    //      from the server).
    // Notes:
    //      - This contents of the payload must at least contain the current
    //      timestamp, latitude, and longitude.

    public static final int STATUS_NOTIFY               = 0xF044;   // 61508
    // Description:
    //      General notification triggered by device operator

    public static final int STATUS_SAMPLE_0             = 0xF050;   // 61520
    public static final int STATUS_SAMPLE_1             = 0xF051;   // 61521
    public static final int STATUS_SAMPLE_2             = 0xF052;   // 61522
    // Description:
    //      General sample triggered by device operator

// ----------------------------------------------------------------------------
// Motion codes: 0xF100 to 0xF1FF
// (motion related status codes - while ignition is on)

    public static final int STATUS_MOTION_START         = 0xF111;   // 61713
    // Description:
    //      Device start of motion
    // Notes:
    //      The definition of motion-start is provided by property PROP_MOTION_START

    public static final int STATUS_MOTION_IN_MOTION     = 0xF112;   // 61714
    // Description:
    //      Device in-motion interval
    // Notes:
    //      The in-motion interval is provided by property PROP_MOTION_IN_MOTION

    public static final int STATUS_MOTION_STOP          = 0xF113;   // 61715
    // Description:
    //      Device stopped motion
    // Notes:
    //      The definition of motion-stop is provided by property PROP_MOTION_STOP

    public static final int STATUS_MOTION_DORMANT       = 0xF114;   // 61716
    // Description:
    //      Device dormant interval (ie. not moving)
    // Notes:
    //      The dormant interval is provided by property PROP_MOTION_DORMANT

    public static final int STATUS_MOTION_IDLE          = 0xF116;   // 61718
    // Description:
    //      Device idle interval (ie. not moving, but engine may still be on)

    public static final int STATUS_MOTION_EXCESS_SPEED  = 0xF11A;   // 61722
    // Description:
    //      Device exceeded preset speed limit
    // Notes:
    //      The excess-speed threshold is provided by property PROP_MOTION_EXCESS_SPEED

    public static final int STATUS_MOTION_MOVING        = 0xF11C;   // 61724
    // Description:
    //      Device is moving
    // Notes:
    //      - This status code may be used to indicating that the device was moving
    //      at the time the event was generated. It is typically not associated
    //      with the status codes STATUS_MOTION_START, STATUS_MOTION_STOP, and  
    //      STATUS_MOTION_IN_MOTION, and may be used independently of these codes.
    //      - This status code is typically used for devices that need to periodically
    //      report that they are moving, apart from the standard start/stop/in-motion
    //      events.

    public static final int STATUS_ODOM_0               = 0xF130;   // 61744
    public static final int STATUS_ODOM_1               = 0xF131;
    public static final int STATUS_ODOM_2               = 0xF132;
    public static final int STATUS_ODOM_3               = 0xF133;
    public static final int STATUS_ODOM_4               = 0xF134;
    public static final int STATUS_ODOM_5               = 0xF135;
    public static final int STATUS_ODOM_6               = 0xF136;
    public static final int STATUS_ODOM_7               = 0xF137;   // 61751
    // Description:
    //      Odometer value
    // Notes:
    //      The odometer limit is provided by property PROP_ODOMETER_#_LIMIT

    public static final int STATUS_ODOM_LIMIT_0         = 0xF140;   // 61760
    public static final int STATUS_ODOM_LIMIT_1         = 0xF141;
    public static final int STATUS_ODOM_LIMIT_2         = 0xF142;
    public static final int STATUS_ODOM_LIMIT_3         = 0xF143;
    public static final int STATUS_ODOM_LIMIT_4         = 0xF144;
    public static final int STATUS_ODOM_LIMIT_5         = 0xF145;
    public static final int STATUS_ODOM_LIMIT_6         = 0xF146;
    public static final int STATUS_ODOM_LIMIT_7         = 0xF147;   // 61767
    // Description:
    //      Odometer has exceeded a set limit
    // Notes:
    //      The odometer limit is provided by property PROP_ODOMETER_#_LIMIT

// ----------------------------------------------------------------------------
// Geofence: 0xF200 to 0xF2FF

    public static final int STATUS_GEOFENCE_ARRIVE      = 0xF210;   // 61968
    // Description:
    //      Device arrived at geofence
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_JOB_ARRIVE           = 0xF215;   // 61973
    // Description:
    //      Device arrived at job-site (typically driver entered)
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_GEOFENCE_DEPART      = 0xF230;   // 62000
    // Description:
    //      Device departed geofence
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_JOB_DEPART           = 0xF235;   // 62005
    // Description:
    //      Device departed job-site (typically driver entered)
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_GEOFENCE_VIOLATION   = 0xF250;   // 62032
    // Description:
    //      Geofence violation
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_CORRIDOR_VIOLATION   = 0xF258;   // 62040
    // Description:
    //      GeoCorridor violation

    public static final int STATUS_GEOFENCE_ACTIVE      = 0xF270;   // 62064
    // Description:
    //      Geofence now active
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_CORRIDOR_ACTIVE      = 0xF278;   // 62072
    // Description:
    //      GeoCorridor now active

    public static final int STATUS_GEOFENCE_INACTIVE    = 0xF280;   // 62080
    // Description:
    //      Geofence now inactive
    // Notes:
    //      - Client may wish to include FIELD_GEOFENCE_ID in the event packet.

    public static final int STATUS_CORRIDOR_INACTIVE    = 0xF288;   // 62088
    // Description:
    //      Geofence now inactive

    public static final int STATUS_GEOBOUNDS_ENTER      = 0xF2A0;   // 62112
    // Description:
    //      Device has entered a state

    public static final int STATUS_GEOBOUNDS_EXIT       = 0xF2B0;   // 62128
    // Description:
    //      Device has exited a state

    public static final int STATUS_PARKED               = 0xF2C0;   // 62144
    // Description:
    //      Device has parked

// ----------------------------------------------------------------------------
// Digital input/output (state change): 0xF400 to 0xF4FF

    public static final int STATUS_INPUT_STATE          = 0xF400;   // 62464
    // Description:
    //      Current input ON state (bitmask)
    // Notes:
    //      - Client should include FIELD_INPUT_STATE in the event packet,
    //      otherwise this status code would have no meaning.

    public static final int STATUS_IGNITION_ON          = 0xF401;   // 62465
    // Description:
    //      Ignition turned ON
    // Notes:
    //      - This status code may be used to indicate that the ignition input
    //      turned ON.

    public static final int STATUS_INPUT_ON             = 0xF402;   // 62466
    // Description:
    //      Input turned ON
    // Notes:
    //      - Client should include FIELD_INPUT_ID in the event packet,
    //      otherwise this status code would have no meaning.
    //      - This status code may be used to indicate that an arbitrary input
    //      'thing' turned ON, and the 'thing' can be identified by the 'Input ID'.
    //      This 'ID' can also represent the index of a digital input.

    public static final int STATUS_IGNITION_OFF         = 0xF403;   // 62467
    // Description:
    //      Ignition turned OFF
    // Notes:
    //      - This status code may be used to indicate that the ignition input
    //      turned OFF.

    public static final int STATUS_INPUT_OFF            = 0xF404;   // 62468
    // Description:
    //      Input turned OFF
    // Notes:
    //      - Client should include FIELD_INPUT_ID in the event packet,
    //      otherwise this status code would have no meaning.
    //      - This status code may be used to indicate that an arbitrary input
    //      'thing' turned OFF, and the 'thing' can be identified by the 'Input ID'.
    //      This 'ID' can also represent the index of a digital input.

    public static final int STATUS_OUTPUT_STATE         = 0xF406;   // 62470
    // Description:
    //      Current output ON state (bitmask)
    // Notes:
    //      - Client should include FIELD_OUTPUT_STATE in the event packet,
    //      otherwise this status code would have no meaning.

    public static final int STATUS_OUTPUT_ON            = 0xF408;   // 62472
    // Description:
    //      Output turned ON
    // Notes:
    //      - Client should include FIELD_OUTPUT_ID in the event packet,
    //      otherwise this status code would have no meaning.
    //      - This status code may be used to indicate that an arbitrary output
    //      'thing' turned ON, and the 'thing' can be identified by the 'Output ID'.
    //      This 'ID' can also represent the index of a digital output.

    public static final int STATUS_OUTPUT_OFF           = 0xF40A;   // 62474
    // Description:
    //      Output turned OFF
    // Notes:
    //      - Client should include FIELD_OUTPUT_ID in the event packet,
    //      otherwise this status code would have no meaning.
    //      - This status code may be used to indicate that an arbitrary output
    //      'thing' turned OFF, and the 'thing' can be identified by the 'Output ID'.
    //      This 'ID' can also represent the index of a digital output.

    public static final int STATUS_INPUT_ON_00          = 0xF420;   // 62496
    public static final int STATUS_INPUT_ON_01          = 0xF421;   // 62497
    public static final int STATUS_INPUT_ON_02          = 0xF422;   // 62498
    public static final int STATUS_INPUT_ON_03          = 0xF423;   // 62499
    public static final int STATUS_INPUT_ON_04          = 0xF424;   // 62500
    public static final int STATUS_INPUT_ON_05          = 0xF425;   // 62501
    public static final int STATUS_INPUT_ON_06          = 0xF426;   // 62502
    public static final int STATUS_INPUT_ON_07          = 0xF427;   // 62503
    public static final int STATUS_INPUT_ON_08          = 0xF428;   // 62504
    public static final int STATUS_INPUT_ON_09          = 0xF429;   // 62505
    public static final int STATUS_INPUT_ON_10          = 0xF42A;   // 62406
    public static final int STATUS_INPUT_ON_11          = 0xF42B;   // 62407
    public static final int STATUS_INPUT_ON_12          = 0xF42C;   // 62408
    public static final int STATUS_INPUT_ON_13          = 0xF42D;   // 62409
    public static final int STATUS_INPUT_ON_14          = 0xF42E;   // 62510
    public static final int STATUS_INPUT_ON_15          = 0xF42F;   // 62511
    // Description:
    //      Digital input state changed to ON
    //      0xFA28 through 0xFA2F reserved

    public static final int STATUS_INPUT_OFF_00         = 0xF440;   // 62528
    public static final int STATUS_INPUT_OFF_01         = 0xF441;   // 62529
    public static final int STATUS_INPUT_OFF_02         = 0xF442;   // 62530
    public static final int STATUS_INPUT_OFF_03         = 0xF443;   // 62531
    public static final int STATUS_INPUT_OFF_04         = 0xF444;   // 62532
    public static final int STATUS_INPUT_OFF_05         = 0xF445;   // 62533
    public static final int STATUS_INPUT_OFF_06         = 0xF446;   // 62534
    public static final int STATUS_INPUT_OFF_07         = 0xF447;   // 62535
    public static final int STATUS_INPUT_OFF_08         = 0xF448;   // 62536
    public static final int STATUS_INPUT_OFF_09         = 0xF449;   // 62537
    public static final int STATUS_INPUT_OFF_10         = 0xF44A;   // 62538
    public static final int STATUS_INPUT_OFF_11         = 0xF44B;   // 62539
    public static final int STATUS_INPUT_OFF_12         = 0xF44C;   // 62540
    public static final int STATUS_INPUT_OFF_13         = 0xF44D;   // 62541
    public static final int STATUS_INPUT_OFF_14         = 0xF44E;   // 62542
    public static final int STATUS_INPUT_OFF_15         = 0xF44F;   // 62543
    // Description:
    //      Digital input state changed to OFF
    //      0xFA48 through 0xFA4F reserved

    public static final int STATUS_OUTPUT_ON_00         = 0xF460;   // 62560
    public static final int STATUS_OUTPUT_ON_01         = 0xF461;
    public static final int STATUS_OUTPUT_ON_02         = 0xF462;
    public static final int STATUS_OUTPUT_ON_03         = 0xF463;
    public static final int STATUS_OUTPUT_ON_04         = 0xF464;
    public static final int STATUS_OUTPUT_ON_05         = 0xF465;
    public static final int STATUS_OUTPUT_ON_06         = 0xF466;
    public static final int STATUS_OUTPUT_ON_07         = 0xF467;
    public static final int STATUS_OUTPUT_ON_08         = 0xF468;
    public static final int STATUS_OUTPUT_ON_09         = 0xF469;   // 62569
    // Description:
    //      Digital output state set to ON
    //      0xFA68 through 0xFA6F reserved

    public static final int STATUS_OUTPUT_OFF_00        = 0xF480;   // 62592
    public static final int STATUS_OUTPUT_OFF_01        = 0xF481;
    public static final int STATUS_OUTPUT_OFF_02        = 0xF482;
    public static final int STATUS_OUTPUT_OFF_03        = 0xF483;
    public static final int STATUS_OUTPUT_OFF_04        = 0xF484;
    public static final int STATUS_OUTPUT_OFF_05        = 0xF485;
    public static final int STATUS_OUTPUT_OFF_06        = 0xF486;
    public static final int STATUS_OUTPUT_OFF_07        = 0xF487;
    public static final int STATUS_OUTPUT_OFF_08        = 0xF488;
    public static final int STATUS_OUTPUT_OFF_09        = 0xF489;   // 62601
    // Description:
    //      Digital output state set to OFF
    //      0xFA88 through 0xFA8F reserved

    public static final int STATUS_ELAPSED_00           = 0xF4A0;   // 62624
    public static final int STATUS_ELAPSED_01           = 0xF4A1;
    public static final int STATUS_ELAPSED_02           = 0xF4A2;
    public static final int STATUS_ELAPSED_03           = 0xF4A3;
    public static final int STATUS_ELAPSED_04           = 0xF4A4;
    public static final int STATUS_ELAPSED_05           = 0xF4A5;
    public static final int STATUS_ELAPSED_06           = 0xF4A6;
    public static final int STATUS_ELAPSED_07           = 0xF4A7;   // 62631
    // Description:
    //      Elapsed time
    //      0xFAA8 through 0xFAAF reserved
    // Notes:
    //      - Client should include FIELD_ELAPSED_TIME in the event packet,
    //      otherwise this status code would have no meaning.

    public static final int STATUS_ELAPSED_LIMIT_00     = 0xF4B0;   // 62640
    public static final int STATUS_ELAPSED_LIMIT_01     = 0xF4B1;   // 62641
    public static final int STATUS_ELAPSED_LIMIT_02     = 0xF4B2;   // 62642
    public static final int STATUS_ELAPSED_LIMIT_03     = 0xF4B3;   // 62643
    public static final int STATUS_ELAPSED_LIMIT_04     = 0xF4B4;   // 62644
    public static final int STATUS_ELAPSED_LIMIT_05     = 0xF4B5;   // 62645
    public static final int STATUS_ELAPSED_LIMIT_06     = 0xF4B6;   // 62646
    public static final int STATUS_ELAPSED_LIMIT_07     = 0xF4B7;   // 62647
    // Description:
    //      Elapsed timer has exceeded a set limit
    //      0xFAB8 through 0xFABF reserved
    // Notes:
    //      - Client should include FIELD_ELAPSED_TIME in the event packet,
    //      otherwise this status code would have no meaning.

// ----------------------------------------------------------------------------
// Analog/etc sensor values (extra data): 0xF600 to 0xF6FF

    public static final int STATUS_SENSOR32_0           = 0xF600;   // 62976
    public static final int STATUS_SENSOR32_1           = 0xF601;
    public static final int STATUS_SENSOR32_2           = 0xF602;
    public static final int STATUS_SENSOR32_3           = 0xF603;
    public static final int STATUS_SENSOR32_4           = 0xF604;
    public static final int STATUS_SENSOR32_5           = 0xF605;
    public static final int STATUS_SENSOR32_6           = 0xF606;
    public static final int STATUS_SENSOR32_7           = 0xF607;   // 62983
    // Description:
    //      32-bit unsigned sensor value
    // Notes:
    //      - Client should include FIELD_SENSOR32 in the event packet,
    //      otherwise this status code would have no meaning.
    //      - The server must be able to convert this 32-bit value to something
    //      meaningful to the user.  This can be done using the following formula:
    //         Actual_Value = ((double)Sensor32_Value * <Gain>) + <Offset>;
    //      Where <Gain> & <Offset> are user configurable values provided at setup.
    //      For instance: Assume Sensor32-0 contains a temperature value that can
    //      have a range of -40.0C to +125.0C.  The client would encode -14.7C
    //      by adding 40.0 and multiplying by 10.0.  The resulting value would be
    //      253.  The server would then be configured to know how to convert this
    //      value back into the proper temperature using the above formula by
    //      substituting 0.1 for <Gain>, and -40.0 for <Offset>: eg.
    //          -14.7 = ((double)253 * 0.1) + (-40.0);

    public static final int STATUS_SENSOR32_RANGE_0     = 0xF620;   // 63008
    public static final int STATUS_SENSOR32_RANGE_1     = 0xF621;
    public static final int STATUS_SENSOR32_RANGE_2     = 0xF622;
    public static final int STATUS_SENSOR32_RANGE_3     = 0xF623;
    public static final int STATUS_SENSOR32_RANGE_4     = 0xF624;
    public static final int STATUS_SENSOR32_RANGE_5     = 0xF625;
    public static final int STATUS_SENSOR32_RANGE_6     = 0xF626;
    public static final int STATUS_SENSOR32_RANGE_7     = 0xF627;   // 63015
    // Description:
    //      32-bit unsigned sensor value out-of-range violation
    // Notes:
    //      - Client should include FIELD_SENSOR32 in the event packet,
    //      otherwise this status code would have no meaning.

// ----------------------------------------------------------------------------
// Temperature sensor values (extra data): 0xF700 to 0xF7FF

    public static final int STATUS_TEMPERATURE_0        = 0xF710;   // 63248
    public static final int STATUS_TEMPERATURE_1        = 0xF711;
    public static final int STATUS_TEMPERATURE_2        = 0xF712;
    public static final int STATUS_TEMPERATURE_3        = 0xF713;
    public static final int STATUS_TEMPERATURE_4        = 0xF714;
    public static final int STATUS_TEMPERATURE_5        = 0xF715;
    public static final int STATUS_TEMPERATURE_6        = 0xF716;
    public static final int STATUS_TEMPERATURE_7        = 0xF717;   // 63255
    // Description:
    //      Temperature value
    // Notes:
    //      - Client should include at least the field FIELD_TEMP_AVER in the 
    //      event packet, and may also wish to include FIELD_TEMP_LOW and
    //      FIELD_TEMP_HIGH.

    public static final int STATUS_TEMPERATURE_RANGE_0  = 0xF730;   // 63280
    public static final int STATUS_TEMPERATURE_RANGE_1  = 0xF731;
    public static final int STATUS_TEMPERATURE_RANGE_2  = 0xF732;
    public static final int STATUS_TEMPERATURE_RANGE_3  = 0xF733;
    public static final int STATUS_TEMPERATURE_RANGE_4  = 0xF734;
    public static final int STATUS_TEMPERATURE_RANGE_5  = 0xF735;
    public static final int STATUS_TEMPERATURE_RANGE_6  = 0xF736;
    public static final int STATUS_TEMPERATURE_RANGE_7  = 0xF737;   // 63287
    // Description:
    //      Temperature value out-of-range [low/high/average]
    // Notes:
    //      - Client should include at least one of the fields FIELD_TEMP_AVER,
    //      FIELD_TEMP_LOW, or FIELD_TEMP_HIGH.

    public static final int STATUS_TEMPERATURE          = 0xF7F1;   // 63473
    // Description:
    //      All temperature averages [aver/aver/aver/...]

// ----------------------------------------------------------------------------
// Miscellaneous

    public static final int STATUS_LOGIN                = 0xF811;   // 63505
    // Description:
    //      Generic 'login'

    public static final int STATUS_LOGOUT               = 0xF812;   // 63506
    // Description:
    //      Generic 'logout'

    public static final int STATUS_CONNECT              = 0xF821;   // 63521
    // Description:
    //      Connect/Hook/On

    public static final int STATUS_DISCONNECT           = 0xF822;   // 63522
    // Description:
    //      Disconnect/Drop/Off

    public static final int STATUS_ACK                  = 0xF831;   // 63537
    // Description:
    //      Acknowledge

    public static final int STATUS_NAK                  = 0xF832;   // 
    // Description:
    //      Negative Acknowledge

    public static final int STATUS_PANIC_ON             = 0xF841;   // 
    // Description:
    //      Panic condition activated

    public static final int STATUS_PANIC_OFF            = 0xF842;   // 
    // Description:
    //      Panic condition deactivated (may not be supported by the device)

    public static final int STATUS_ASSIST_ON            = 0xF851;   // 
    // Description:
    //      Assist condition activated

    public static final int STATUS_ASSIST_OFF           = 0xF852;   // 
    // Description:
    //      Assist condition deactivated

    public static final int STATUS_MEDICAL_ON           = 0xF861;   // 
    // Description:
    //      Medical Call condition activated

    public static final int STATUS_MEDICAL_OFF          = 0xF862;   // 
    // Description:
    //      Medical Call condition deactivated

    public static final int STATUS_TOWING_START         = 0xF871;   // 
    // Description:
    //      Vehicle started to be towed

    public static final int STATUS_TOWING_STOP          = 0xF872;   // 
    // Description:
    //      Vehicle stopped being towed

// ----------------------------------------------------------------------------
// OBC/J1708 status: 0xF900 to 0xF9FF

    public static final int STATUS_OBC_INFO_0           = 0xF900;   // 
    public static final int STATUS_OBC_INFO_1           = 0xF901;
    public static final int STATUS_OBC_INFO_2           = 0xF902;
    public static final int STATUS_OBC_INFO_3           = 0xF903;
    public static final int STATUS_OBC_INFO_4           = 0xF904;   // 
    // Description:
    //      OBC/J1708 information packet

    public static final int STATUS_OBC_FAULT            = 0xF911;   // 
    // Description:
    //      OBC/J1708 fault code occurred.
    // Notes:
    //      - Client should include the field FIELD_OBC_J1708_FAULT

    public static final int STATUS_OBC_RANGE            = 0xF920;   // 
    // Description:
    //      Generic OBC/J1708 value out-of-range
    // Notes:
    //      - Client should include at least one of the FIELD_OBC_xxxxx fields.

    public static final int STATUS_OBC_RPM_RANGE        = 0xF922;   // 
    // Description:
    //      OBC/J1708 RPM out-of-range
    // Notes:
    //      - Client should include the field FIELD_OBC_ENGINE_RPM.

    public static final int STATUS_OBC_FUEL_RANGE       = 0xF924;   // 
    // Description:
    //      OBC/J1708 Fuel level out-of-range (ie. to low)
    // Notes:
    //      - Client should include the field FIELD_OBC_FUEL_LEVEL.
    //      - This code can also be used to indicate possible fuel theft.

    public static final int STATUS_OBC_OIL_RANGE        = 0xF926;   // 
    // Description:
    //      OBC/J1708 Oil level out-of-range (ie. to low)

    public static final int STATUS_OBC_TEMP_RANGE       = 0xF928;   // 
    // Description:
    //      OBC/J1708 Temperature out-of-range
    // Notes:
    //      - Client should include at least one of the FIELD_OBC_xxxxx fields
    //      which indicates an OBC temperature out-of-range.

    public static final int STATUS_EXCESS_BRAKING       = 0xF930;   // 
    // Description:
    //      Excessive acceleration/deceleration detected

    public static final int STATUS_IMPACT               = 0xF941;   // 
    // Description:
    //      Excessive acceleration/deceleration detected

// ----------------------------------------------------------------------------
// Internal device status

    public static final int STATUS_DAY_SUMMARY          = 0xFA00;   // 64000
    // Description:
    //      End-Of-Day Summary

// ----------------------------------------------------------------------------
// Internal device status

    public static final int STATUS_LOW_BATTERY          = 0xFD10;   // 64784
    // Description:
    //      Low battery indicator

    public static final int STATUS_BATTERY_LEVEL        = 0xFD11;   // 64785
    // Description:
    //      Battery indicator

    public static final int STATUS_POWER_FAILURE        = 0xFD13;   // 64787
    // Description:
    //      Power failure indicator (or running on internal battery)

    public static final int STATUS_POWER_RESTORED       = 0xFD15;   // 64789
    // Description:
    //      Power restored (after previous failure)

    public static final int STATUS_GPS_EXPIRED          = 0xFD21;   // 64801
    // Description:
    //      GPS fix expiration detected

    public static final int STATUS_GPS_FAILURE          = 0xFD22;   // 64802
    // Description:
    //      GPS receiver failure detected

    public static final int STATUS_DIAGNOSTIC           = 0xFD30;   // 64816
    // Description:
    //      General Diagnostic message

    public static final int STATUS_CONNECTION_FAILURE   = 0xFD31;   // 64817
    // Description:
    //      Connection failure detected

    public static final int STATUS_MODEM_FAILURE        = 0xFD33;   // 64819
    // Description:
    //      Modem failure detected

    public static final int STATUS_INTERNAL_FAILURE     = 0xFD35;   // 64821
    // Description:
    //      Internal failure detected

    public static final int STATUS_CONFIG_RESET         = 0xFD41;   // 64833
    // Description:
    //      Configuration reset

    public static final int STATUS_SHUTDOWN             = 0xFD45;   // 64837
    // Description:
    //      device shutdown
    
    public static final int STATUS_SUSPEND              = 0xFD48;   // 64840
    // Description:
    //      device sleep/suspend
    
    public static final int STATUS_RESUME               = 0xFD4A;   // 64842
    // Description:
    //      device resume

// ----------------------------------------------------------------------------
// Reserved 0xFEXX to 0xFFFF

// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------

    private static final String CODE_PREFIX = ""; // "DMT.";

    public static class Code
    {
        private int         code        = 0;
        private String      name        = "";
        private String      desc        = null;
        private I18N.Text   text        = null;
        private boolean     rtChecked   = false;
        private String      rtDesc      = null;
        private boolean     hiPri       = false;
        private String      iconName    = null;
        public Code(int code, String name, I18N.Text text) {
            this(code, name, text, false);
        }
        public Code(int code, String name, I18N.Text text, boolean higherPri) {
            this.code   = code;
            this.name   = CODE_PREFIX + name;
            this.text   = text;
            this.hiPri  = higherPri;
        }
        public Code(int code, String name, String desc) {
            this(code, name, desc, false);
        }
        public Code(int code, String name, String desc, boolean higherPri) {
            this.code   = code;
            this.name   = CODE_PREFIX + name;
            this.desc   = desc;
            this.hiPri  = higherPri;
        }
        public int getCode() {
            return this.code;
        }
        public String getName() {
            return this.name;
        }
        public void setDescription(String desc) {
            this.desc = !StringTools.isBlank(desc)? desc : null;
        }
        public String getDescription(Locale locale) {
            // check runtime config override
            if (!this.rtChecked) {
                synchronized (this) {
                    if (!this.rtChecked) {
                        this.rtDesc = (this.text != null)? RTConfig.getString(this.text.getKey(),null) : null;
                        this.rtChecked = true;
                    }
                }
            }
            // return description
            if (this.rtDesc != null) {
                return this.rtDesc;
            } else
            if (this.desc != null) {
                return this.desc;
            } else {
                return this.text.toString(locale);
            }
        }
        public void setHighPriority(boolean hipri) {
            this.hiPri = hipri;
        }
        public boolean isHighPriority() {
            return this.hiPri;
        }
        public void setIconName(String name) {
            this.iconName = StringTools.trim(name);
        }
        public String getIconName() {
            return this.iconName;
        }
        public String toString() {
            return this.toString(null);
        }
        public String toString(Locale locale) {
            StringBuffer sb = new StringBuffer();
            sb.append("0x").append(StringTools.toHexString(this.getCode(),16));
            sb.append(" (").append(this.getCode()).append(") ");
            sb.append(this.getDescription(locale));
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------

    private static Code _codeArray[] = new Code[] {

        //       16-bit Code                 Name             Description                                                                     HiPri
        new Code(STATUS_NONE               , "NONE"         , I18N.getString(StatusCodes.class,"StatusCodes.none"          ,"None"          )       ), // always first

        new Code(STATUS_GFMI_CMD_03        , "GMFI.03"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_03"       ,"GFMI_SendMsg3" )       ), // send non-ack message
        new Code(STATUS_GFMI_CMD_04        , "GMFI.04"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_04"       ,"GFMI_SendMsg4" )       ), // send ack message
        new Code(STATUS_GFMI_CMD_05        , "GMFI.05"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_05"       ,"GFMI_SendMsg5" )       ), // send answerable message
        new Code(STATUS_GFMI_CMD_06        , "GMFI.06"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_06"       ,"GFMI_StopLoc6" )       ), // send stop location
        new Code(STATUS_GFMI_CMD_08        , "GMFI.08"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_08"       ,"GFMI_ReqETA8"  )       ), // request stop ETA
        new Code(STATUS_GFMI_CMD_09        , "GMFI.09"      , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_09"       ,"GFMI_AutoArr9" )       ), // set auto arrival criteria
        new Code(STATUS_GFMI_LINK_OFF      , "GFMI.LINK.0"  , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_link_0"   ,"GFMI_LinkOff"  )       ), // GFMI link lost
        new Code(STATUS_GFMI_LINK_ON       , "GFMI.LINK.1"  , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_link_1"   ,"GFMI_LinkOn"   )       ), // GFMI link established
        new Code(STATUS_GFMI_ACK           , "GFMI.ACK"     , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_ack"      ,"GFMI_ACK"      )       ), // received ACK
        new Code(STATUS_GFMI_MESSAGE       , "GFMI.MESSAGE" , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_message"  ,"GFMI_Message"  )       ), // received message
        new Code(STATUS_GFMI_MESSAGE_ACK   , "GFMI.MSG.ACK" , I18N.getString(StatusCodes.class,"StatusCodes.gfmi_msg_ack"  ,"GFMI_MsgACK"   )       ), // received message ack

        new Code(STATUS_INITIALIZED        , "INITIALIZED"  , I18N.getString(StatusCodes.class,"StatusCodes.initialized"   ,"Initialized"   )       ),
        new Code(STATUS_LOCATION           , "LOCATION"     , I18N.getString(StatusCodes.class,"StatusCodes.location"      ,"Location"      )       ),
        new Code(STATUS_LAST_LOCATION      , "LOC.LAST"     , I18N.getString(StatusCodes.class,"StatusCodes.lastLocation"  ,"Last_Location" )       ),
        new Code(STATUS_WAYMARK_0          , "WAYMARK.0"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_0"     ,"Waymark_0"     ), true ),
        new Code(STATUS_WAYMARK_1          , "WAYMARK.1"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_1"     ,"Waymark_1"     ), true ),
        new Code(STATUS_WAYMARK_2          , "WAYMARK.2"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_2"     ,"Waymark_2"     ), true ),
        new Code(STATUS_WAYMARK_3          , "WAYMARK.3"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_3"     ,"Waymark_3"     ), true ),
        new Code(STATUS_WAYMARK_4          , "WAYMARK.4"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_4"     ,"Waymark_4"     ), true ),
        new Code(STATUS_WAYMARK_5          , "WAYMARK.5"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_5"     ,"Waymark_5"     ), true ),
        new Code(STATUS_WAYMARK_6          , "WAYMARK.6"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_6"     ,"Waymark_6"     ), true ),
        new Code(STATUS_WAYMARK_7          , "WAYMARK.7"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_7"     ,"Waymark_7"     ), true ),
        new Code(STATUS_WAYMARK_8          , "WAYMARK.8"    , I18N.getString(StatusCodes.class,"StatusCodes.waymark_8"     ,"Waymark_8"     ), true ),
        new Code(STATUS_QUERY              , "QUERY"        , I18N.getString(StatusCodes.class,"StatusCodes.query"         ,"Query"         ), true ),
        new Code(STATUS_NOTIFY             , "NOTIFY"       , I18N.getString(StatusCodes.class,"StatusCodes.notify"        ,"Notify"        ), true ),
        new Code(STATUS_SAMPLE_0           , "SAMPLE.0"     , I18N.getString(StatusCodes.class,"StatusCodes.sample_0"      ,"Sample_0"      )       ),
        new Code(STATUS_SAMPLE_1           , "SAMPLE.1"     , I18N.getString(StatusCodes.class,"StatusCodes.sample_1"      ,"Sample_1"      )       ),
        new Code(STATUS_SAMPLE_2           , "SAMPLE.2"     , I18N.getString(StatusCodes.class,"StatusCodes.sample_2"      ,"Sample_2"      )       ),

        new Code(STATUS_MOTION_START       , "MOT.START"    , I18N.getString(StatusCodes.class,"StatusCodes.start"         ,"Start"         ), true ),
        new Code(STATUS_MOTION_IN_MOTION   , "MOT.INMOTION" , I18N.getString(StatusCodes.class,"StatusCodes.inMotion"      ,"InMotion"      )       ),
        new Code(STATUS_MOTION_STOP        , "MOT.STOP"     , I18N.getString(StatusCodes.class,"StatusCodes.stop"          ,"Stop"          ), true ),
        new Code(STATUS_MOTION_DORMANT     , "MOT.DORMANT"  , I18N.getString(StatusCodes.class,"StatusCodes.dormant"       ,"Dormant"       )       ),
        new Code(STATUS_MOTION_IDLE        , "MOT.IDLE"     , I18N.getString(StatusCodes.class,"StatusCodes.idle"          ,"Idle"          )       ),
        new Code(STATUS_MOTION_EXCESS_SPEED, "MOT.SPEED"    , I18N.getString(StatusCodes.class,"StatusCodes.speeding"      ,"Speeding"      )       ),
        new Code(STATUS_MOTION_MOVING      , "MOT.MOVING"   , I18N.getString(StatusCodes.class,"StatusCodes.moving"        ,"Moving"        )       ),

        new Code(STATUS_ODOM_0             , "ODO.0"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_0"    ,"Odometer_0"    )       ),
        new Code(STATUS_ODOM_1             , "ODO.1"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_1"    ,"Odometer_1"    )       ),
        new Code(STATUS_ODOM_2             , "ODO.2"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_2"    ,"Odometer_2"    )       ),
        new Code(STATUS_ODOM_3             , "ODO.3"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_3"    ,"Odometer_3"    )       ),
        new Code(STATUS_ODOM_4             , "ODO.4"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_4"    ,"Odometer_4"    )       ),
        new Code(STATUS_ODOM_5             , "ODO.5"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_5"    ,"Odometer_5"    )       ),
        new Code(STATUS_ODOM_6             , "ODO.6"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_6"    ,"Odometer_6"    )       ),
        new Code(STATUS_ODOM_7             , "ODO.7"        , I18N.getString(StatusCodes.class,"StatusCodes.odometer_7"    ,"Odometer_7"    )       ),
        new Code(STATUS_ODOM_LIMIT_0       , "ODO.LIM.0"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_0"    ,"OdoLimit_0"    ), true ),
        new Code(STATUS_ODOM_LIMIT_1       , "ODO.LIM.1"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_1"    ,"OdoLimit_1"    ), true ),
        new Code(STATUS_ODOM_LIMIT_2       , "ODO.LIM.2"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_2"    ,"OdoLimit_2"    ), true ),
        new Code(STATUS_ODOM_LIMIT_3       , "ODO.LIM.3"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_3"    ,"OdoLimit_3"    ), true ),
        new Code(STATUS_ODOM_LIMIT_4       , "ODO.LIM.4"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_4"    ,"OdoLimit_4"    ), true ),
        new Code(STATUS_ODOM_LIMIT_5       , "ODO.LIM.5"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_5"    ,"OdoLimit_5"    ), true ),
        new Code(STATUS_ODOM_LIMIT_6       , "ODO.LIM.6"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_6"    ,"OdoLimit_6"    ), true ),
        new Code(STATUS_ODOM_LIMIT_7       , "ODO.LIM.7"    , I18N.getString(StatusCodes.class,"StatusCodes.odoLimit_7"    ,"OdoLimit_7"    ), true ),

        new Code(STATUS_GEOFENCE_ARRIVE    , "GEO.ARR"      , I18N.getString(StatusCodes.class,"StatusCodes.arrive"        ,"Arrive"        ), true ),
        new Code(STATUS_JOB_ARRIVE         , "JOB.ARR"      , I18N.getString(StatusCodes.class,"StatusCodes.jobArrive"     ,"JobArrive"     ), true ),
        new Code(STATUS_GEOFENCE_DEPART    , "GEO.DEP"      , I18N.getString(StatusCodes.class,"StatusCodes.separt"        ,"Depart"        ), true ),
        new Code(STATUS_JOB_DEPART         , "JOB.DEP"      , I18N.getString(StatusCodes.class,"StatusCodes.jobDepart"     ,"JobDepart"     ), true ),
        new Code(STATUS_GEOFENCE_VIOLATION , "GEO.VIO"      , I18N.getString(StatusCodes.class,"StatusCodes.geofence"      ,"Geofence"      ), true ),
        new Code(STATUS_CORRIDOR_VIOLATION , "COR.VIO"      , I18N.getString(StatusCodes.class,"StatusCodes.corridor"      ,"GeoCorridor"   ), true ),
        new Code(STATUS_GEOFENCE_ACTIVE    , "GEO.ACT"      , I18N.getString(StatusCodes.class,"StatusCodes.geofActive"    ,"GeofActive"    )       ),
        new Code(STATUS_CORRIDOR_ACTIVE    , "COR.ACT"      , I18N.getString(StatusCodes.class,"StatusCodes.corrActive"    ,"CorrActive"    )       ),
        new Code(STATUS_GEOFENCE_INACTIVE  , "GEO.INA"      , I18N.getString(StatusCodes.class,"StatusCodes.geofInactive"  ,"GeofInactive"  )       ),
        new Code(STATUS_CORRIDOR_INACTIVE  , "COR.INA"      , I18N.getString(StatusCodes.class,"StatusCodes.corrInactive"  ,"CorrInactive"  )       ),
        new Code(STATUS_GEOBOUNDS_ENTER    , "STA.ENTR"     , I18N.getString(StatusCodes.class,"StatusCodes.stateEnter"    ,"StateEnter"    )       ),
        new Code(STATUS_GEOBOUNDS_EXIT     , "STA.EXIT"     , I18N.getString(StatusCodes.class,"StatusCodes.stateExit"     ,"StateExit"     )       ),
        new Code(STATUS_PARKED             , "GEO.PARK"     , I18N.getString(StatusCodes.class,"StatusCodes.parked"        ,"Parked"        )       ),

        new Code(STATUS_INPUT_STATE        , "INP.STA"      , I18N.getString(StatusCodes.class,"StatusCodes.inputs"        ,"Inputs"        )       ),
        new Code(STATUS_IGNITION_ON        , "IGN.ON"       , I18N.getString(StatusCodes.class,"StatusCodes.ignitionOn"    ,"IgnitionOn"    )       ),
        new Code(STATUS_INPUT_ON           , "INP.ON"       , I18N.getString(StatusCodes.class,"StatusCodes.inputOn"       ,"InputOn"       )       ),
        new Code(STATUS_IGNITION_OFF       , "IGN.OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.ignitionOff"   ,"IgnitionOff"   )       ),
        new Code(STATUS_INPUT_OFF          , "INP.OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.inputOff"      ,"InputOff"      )       ),

        new Code(STATUS_OUTPUT_STATE       , "OUT.ST"       , I18N.getString(StatusCodes.class,"StatusCodes.outputs"       ,"Outputs"       )       ),
        new Code(STATUS_OUTPUT_ON          , "OUT.ON"       , I18N.getString(StatusCodes.class,"StatusCodes.outputOn"      ,"OutputOn"      )       ),
        new Code(STATUS_OUTPUT_OFF         , "OUT.OFF"      , I18N.getString(StatusCodes.class,"StatusCodes.outputOff"     ,"OutputOff"     )       ),

        new Code(STATUS_INPUT_ON_00        , "INP.ON.0"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_0"     ,"InputOn_0"     )       ),
        new Code(STATUS_INPUT_ON_01        , "INP.ON.1"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_1"     ,"InputOn_1"     )       ),
        new Code(STATUS_INPUT_ON_02        , "INP.ON.2"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_2"     ,"InputOn_2"     )       ),
        new Code(STATUS_INPUT_ON_03        , "INP.ON.3"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_3"     ,"InputOn_3"     )       ),
        new Code(STATUS_INPUT_ON_04        , "INP.ON.4"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_4"     ,"InputOn_4"     )       ),
        new Code(STATUS_INPUT_ON_05        , "INP.ON.5"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_5"     ,"InputOn_5"     )       ),
        new Code(STATUS_INPUT_ON_06        , "INP.ON.6"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_6"     ,"InputOn_6"     )       ),
        new Code(STATUS_INPUT_ON_07        , "INP.ON.7"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_7"     ,"InputOn_7"     )       ),
        new Code(STATUS_INPUT_ON_08        , "INP.ON.8"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_8"     ,"InputOn_8"     )       ),
        new Code(STATUS_INPUT_ON_09        , "INP.ON.9"     , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_9"     ,"InputOn_9"     )       ),
        new Code(STATUS_INPUT_ON_10        , "INP.ON.10"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_10"    ,"InputOn_10"    )       ),
        new Code(STATUS_INPUT_ON_11        , "INP.ON.11"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_11"    ,"InputOn_11"    )       ),
        new Code(STATUS_INPUT_ON_12        , "INP.ON.12"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_12"    ,"InputOn_12"    )       ),
        new Code(STATUS_INPUT_ON_13        , "INP.ON.13"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_13"    ,"InputOn_13"    )       ),
        new Code(STATUS_INPUT_ON_14        , "INP.ON.14"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_14"    ,"InputOn_14"    )       ),
        new Code(STATUS_INPUT_ON_15        , "INP.ON.15"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOn_15"    ,"InputOn_15"    )       ),
        new Code(STATUS_INPUT_OFF_00       , "INP.OFF.0"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_0"    ,"InputOff_0"    )       ),
        new Code(STATUS_INPUT_OFF_01       , "INP.OFF.1"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_1"    ,"InputOff_1"    )       ),
        new Code(STATUS_INPUT_OFF_02       , "INP.OFF.2"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_2"    ,"InputOff_2"    )       ),
        new Code(STATUS_INPUT_OFF_03       , "INP.OFF.3"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_3"    ,"InputOff_3"    )       ),
        new Code(STATUS_INPUT_OFF_04       , "INP.OFF.4"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_4"    ,"InputOff_4"    )       ),
        new Code(STATUS_INPUT_OFF_05       , "INP.OFF.5"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_5"    ,"InputOff_5"    )       ),
        new Code(STATUS_INPUT_OFF_06       , "INP.OFF.6"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_6"    ,"InputOff_6"    )       ),
        new Code(STATUS_INPUT_OFF_07       , "INP.OFF.7"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_7"    ,"InputOff_7"    )       ),
        new Code(STATUS_INPUT_OFF_08       , "INP.OFF.8"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_8"    ,"InputOff_8"    )       ),
        new Code(STATUS_INPUT_OFF_09       , "INP.OFF.9"    , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_9"    ,"InputOff_9"    )       ),
        new Code(STATUS_INPUT_OFF_10       , "INP.OFF.10"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_10"   ,"InputOff_10"   )       ),
        new Code(STATUS_INPUT_OFF_11       , "INP.OFF.11"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_11"   ,"InputOff_11"   )       ),
        new Code(STATUS_INPUT_OFF_12       , "INP.OFF.12"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_12"   ,"InputOff_12"   )       ),
        new Code(STATUS_INPUT_OFF_13       , "INP.OFF.13"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_13"   ,"InputOff_13"   )       ),
        new Code(STATUS_INPUT_OFF_14       , "INP.OFF.14"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_14"   ,"InputOff_14"   )       ),
        new Code(STATUS_INPUT_OFF_15       , "INP.OFF.15"   , I18N.getString(StatusCodes.class,"StatusCodes.inputOff_15"   ,"InputOff_15"   )       ),

        new Code(STATUS_OUTPUT_ON_00       , "OUT.ON.0"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_0"    ,"OutputOn_0"    )       ),
        new Code(STATUS_OUTPUT_ON_01       , "OUT.ON.1"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_1"    ,"OutputOn_1"    )       ),
        new Code(STATUS_OUTPUT_ON_02       , "OUT.ON.2"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_2"    ,"OutputOn_2"    )       ),
        new Code(STATUS_OUTPUT_ON_03       , "OUT.ON.3"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_3"    ,"OutputOn_3"    )       ),
        new Code(STATUS_OUTPUT_ON_04       , "OUT.ON.4"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_4"    ,"OutputOn_4"    )       ),
        new Code(STATUS_OUTPUT_ON_05       , "OUT.ON.5"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_5"    ,"OutputOn_5"    )       ),
        new Code(STATUS_OUTPUT_ON_06       , "OUT.ON.6"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_6"    ,"OutputOn_6"    )       ),
        new Code(STATUS_OUTPUT_ON_07       , "OUT.ON.7"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_7"    ,"OutputOn_7"    )       ),
        new Code(STATUS_OUTPUT_ON_08       , "OUT.ON.8"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_8"    ,"OutputOn_8"    )       ),
        new Code(STATUS_OUTPUT_ON_09       , "OUT.ON.9"     , I18N.getString(StatusCodes.class,"StatusCodes.outputOn_9"    ,"OutputOn_9"    )       ),
        new Code(STATUS_OUTPUT_OFF_00      , "OUT.OFF.0"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_0"   ,"OutputOff_0"   )       ),
        new Code(STATUS_OUTPUT_OFF_01      , "OUT.OFF.1"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_1"   ,"OutputOff_1"   )       ),
        new Code(STATUS_OUTPUT_OFF_02      , "OUT.OFF.2"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_2"   ,"OutputOff_2"   )       ),
        new Code(STATUS_OUTPUT_OFF_03      , "OUT.OFF.3"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_3"   ,"OutputOff_3"   )       ),
        new Code(STATUS_OUTPUT_OFF_04      , "OUT.OFF.4"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_4"   ,"OutputOff_4"   )       ),
        new Code(STATUS_OUTPUT_OFF_05      , "OUT.OFF.5"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_5"   ,"OutputOff_5"   )       ),
        new Code(STATUS_OUTPUT_OFF_06      , "OUT.OFF.6"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_6"   ,"OutputOff_6"   )       ),
        new Code(STATUS_OUTPUT_OFF_07      , "OUT.OFF.7"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_7"   ,"OutputOff_7"   )       ),
        new Code(STATUS_OUTPUT_OFF_08      , "OUT.OFF.8"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_8"   ,"OutputOff_8"   )       ),
        new Code(STATUS_OUTPUT_OFF_09      , "OUT.OFF.9"    , I18N.getString(StatusCodes.class,"StatusCodes.outputOff_9"   ,"OutputOff_9"   )       ),

        new Code(STATUS_ELAPSED_00         , "ELA.0"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_0"      ,"Elapse_0"      )       ),
        new Code(STATUS_ELAPSED_01         , "ELA.1"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_1"      ,"Elapse_1"      )       ),
        new Code(STATUS_ELAPSED_02         , "ELA.2"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_2"      ,"Elapse_2"      )       ),
        new Code(STATUS_ELAPSED_03         , "ELA.3"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_3"      ,"Elapse_3"      )       ),
        new Code(STATUS_ELAPSED_04         , "ELA.4"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_4"      ,"Elapse_4"      )       ),
        new Code(STATUS_ELAPSED_05         , "ELA.5"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_5"      ,"Elapse_5"      )       ),
        new Code(STATUS_ELAPSED_06         , "ELA.6"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_6"      ,"Elapse_6"      )       ),
        new Code(STATUS_ELAPSED_07         , "ELA.7"        , I18N.getString(StatusCodes.class,"StatusCodes.elapse_7"      ,"Elapse_7"      )       ),
        new Code(STATUS_ELAPSED_LIMIT_00   , "ELA.LIM.0"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_0"    ,"ElaLimit_0"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_01   , "ELA.LIM.1"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_1"    ,"ElaLimit_1"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_02   , "ELA.LIM.2"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_2"    ,"ElaLimit_2"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_03   , "ELA.LIM.3"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_3"    ,"ElaLimit_3"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_04   , "ELA.LIM.4"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_4"    ,"ElaLimit_4"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_05   , "ELA.LIM.5"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_5"    ,"ElaLimit_5"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_06   , "ELA.LIM.6"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_6"    ,"ElaLimit_6"    ), true ),
        new Code(STATUS_ELAPSED_LIMIT_07   , "ELA.LIM.7"    , I18N.getString(StatusCodes.class,"StatusCodes.elaLimit_7"    ,"ElaLimit_7"    ), true ),

        new Code(STATUS_SENSOR32_0         , "SEN32.0"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_0"    ,"Sensor32_0"    )       ),
        new Code(STATUS_SENSOR32_1         , "SEN32.1"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_1"    ,"Sensor32_1"    )       ),
        new Code(STATUS_SENSOR32_2         , "SEN32.2"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_2"    ,"Sensor32_2"    )       ),
        new Code(STATUS_SENSOR32_3         , "SEN32.3"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_3"    ,"Sensor32_3"    )       ),
        new Code(STATUS_SENSOR32_3         , "SEN32.4"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_4"    ,"Sensor32_4"    )       ),
        new Code(STATUS_SENSOR32_4         , "SEN32.5"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_5"    ,"Sensor32_5"    )       ),
        new Code(STATUS_SENSOR32_5         , "SEN32.6"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_6"    ,"Sensor32_6"    )       ),
        new Code(STATUS_SENSOR32_7         , "SEN32.7"      , I18N.getString(StatusCodes.class,"StatusCodes.sensor32_7"    ,"Sensor32_7"    )       ),
        new Code(STATUS_SENSOR32_RANGE_0   , "SEN32.LIM.0"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_0"  ,"Sen32Range_0"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_1   , "SEN32.LIM.1"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_1"  ,"Sen32Range_1"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_2   , "SEN32.LIM.2"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_2"  ,"Sen32Range_2"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_3   , "SEN32.LIM.3"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_3"  ,"Sen32Range_3"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_4   , "SEN32.LIM.4"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_4"  ,"Sen32Range_4"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_5   , "SEN32.LIM.5"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_5"  ,"Sen32Range_5"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_6   , "SEN32.LIM.6"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_6"  ,"Sen32Range_6"  ), true ),
        new Code(STATUS_SENSOR32_RANGE_7   , "SEN32.LIM.7"  , I18N.getString(StatusCodes.class,"StatusCodes.sen32Range_7"  ,"Sen32Range_7"  ), true ),

        new Code(STATUS_TEMPERATURE_0      , "TMP.0"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_0"        ,"Temp_0"        )       ),
        new Code(STATUS_TEMPERATURE_1      , "TMP.1"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_1"        ,"Temp_1"        )       ),
        new Code(STATUS_TEMPERATURE_2      , "TMP.2"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_2"        ,"Temp_2"        )       ),
        new Code(STATUS_TEMPERATURE_3      , "TMP.3"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_3"        ,"Temp_3"        )       ),
        new Code(STATUS_TEMPERATURE_4      , "TMP.4"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_4"        ,"Temp_4"        )       ),
        new Code(STATUS_TEMPERATURE_5      , "TMP.5"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_5"        ,"Temp_5"        )       ),
        new Code(STATUS_TEMPERATURE_6      , "TMP.6"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_6"        ,"Temp_6"        )       ),
        new Code(STATUS_TEMPERATURE_7      , "TMP.7"        , I18N.getString(StatusCodes.class,"StatusCodes.temp_7"        ,"Temp_7"        )       ),
        new Code(STATUS_TEMPERATURE_RANGE_0, "TMP.LIM.0"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_0"   ,"TempRange_0"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_1, "TMP.LIM.1"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_1"   ,"TempRange_1"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_2, "TMP.LIM.2"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_2"   ,"TempRange_2"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_3, "TMP.LIM.3"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_3"   ,"TempRange_3"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_4, "TMP.LIM.4"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_4"   ,"TempRange_4"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_5, "TMP.LIM.5"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_5"   ,"TempRange_5"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_6, "TMP.LIM.6"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_6"   ,"TempRange_6"   ), true ),
        new Code(STATUS_TEMPERATURE_RANGE_7, "TMP.LIM.7"    , I18N.getString(StatusCodes.class,"StatusCodes.tempRange_7"   ,"TempRange_7"   ), true ),
        new Code(STATUS_TEMPERATURE        , "TMP.ALL"      , I18N.getString(StatusCodes.class,"StatusCodes.temp_All"      ,"Temp_All"      )       ),

        new Code(STATUS_LOGIN              , "LOGIN"        , I18N.getString(StatusCodes.class,"StatusCodes.login"         ,"Login"         ), true ),
        new Code(STATUS_LOGOUT             , "LOGOUT"       , I18N.getString(StatusCodes.class,"StatusCodes.logout"        ,"Logout"        )       ),
        new Code(STATUS_CONNECT            , "CONNECT"      , I18N.getString(StatusCodes.class,"StatusCodes.connect"       ,"Connect"       )       ),
        new Code(STATUS_DISCONNECT         , "DISCONNECT"   , I18N.getString(StatusCodes.class,"StatusCodes.disconnect"    ,"Disconnect"    )       ),
        new Code(STATUS_ACK                , "ACK"          , I18N.getString(StatusCodes.class,"StatusCodes.ack"           ,"Ack"           )       ),
        new Code(STATUS_NAK                , "NAK"          , I18N.getString(StatusCodes.class,"StatusCodes.nak"           ,"Nak"           )       ),
        new Code(STATUS_PANIC_ON           , "PANIC.ON"     , I18N.getString(StatusCodes.class,"StatusCodes.panicOn"       ,"Panic_On"      ), true ),
        new Code(STATUS_PANIC_OFF          , "PANIC.OFF"    , I18N.getString(StatusCodes.class,"StatusCodes.panicOff"      ,"Panic_Off"     )       ),
        new Code(STATUS_ASSIST_ON          , "ASSIST.ON"    , I18N.getString(StatusCodes.class,"StatusCodes.assistOn"      ,"Assist_On"     ), true ),
        new Code(STATUS_ASSIST_OFF         , "ASSIST.OFF"   , I18N.getString(StatusCodes.class,"StatusCodes.assistOff"     ,"Assist_Off"    )       ),
        new Code(STATUS_MEDICAL_ON         , "MEDICAL.ON"   , I18N.getString(StatusCodes.class,"StatusCodes.medicalOn"     ,"Medical_On"    ), true ),
        new Code(STATUS_MEDICAL_OFF        , "MEDICAL.OFF"  , I18N.getString(StatusCodes.class,"StatusCodes.medicalOff"    ,"Medical_Off"   )       ),
        new Code(STATUS_TOWING_START       , "TOW.START"    , I18N.getString(StatusCodes.class,"StatusCodes.towStart"      ,"Tow_Start"     ), true ),
        new Code(STATUS_TOWING_STOP        , "TOW.STOP"     , I18N.getString(StatusCodes.class,"StatusCodes.towStop"       ,"Tow_Stop"      ), true ),

        new Code(STATUS_OBC_INFO_0         , "OBC.INFO.0"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_0"    ,"OBC_Info_0"    )       ),
        new Code(STATUS_OBC_INFO_1         , "OBC.INFO.1"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_1"    ,"OBC_Info_1"    )       ),
        new Code(STATUS_OBC_INFO_2         , "OBC.INFO.2"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_2"    ,"OBC_Info_2"    )       ),
        new Code(STATUS_OBC_INFO_3         , "OBC.INFO.3"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_3"    ,"OBC_Info_3"    )       ),
        new Code(STATUS_OBC_INFO_4         , "OBC.INFO.4"   , I18N.getString(StatusCodes.class,"StatusCodes.obc_Info_4"    ,"OBC_Info_4"    )       ),
        new Code(STATUS_OBC_FAULT          , "OBC.FAULT"    , I18N.getString(StatusCodes.class,"StatusCodes.obc_Fault"     ,"OBC_Fault"     ), true ),
        new Code(STATUS_OBC_RANGE          , "OBC.RANGE"    , I18N.getString(StatusCodes.class,"StatusCodes.obc_Range"     ,"OBC_Range"     )       ),
        new Code(STATUS_OBC_RPM_RANGE      , "OBC.RPM"      , I18N.getString(StatusCodes.class,"StatusCodes.obc_Rpm"       ,"OBC_Rpm"       )       ),
        new Code(STATUS_OBC_FUEL_RANGE     , "OBC.FUEL"     , I18N.getString(StatusCodes.class,"StatusCodes.obc_Fuel"      ,"OBC_Fuel"      )       ),
        new Code(STATUS_OBC_OIL_RANGE      , "OBC.OIL"      , I18N.getString(StatusCodes.class,"StatusCodes.obc_Oil"       ,"OBC_Oil"       )       ),
        new Code(STATUS_OBC_TEMP_RANGE     , "OBC.TEMP"     , I18N.getString(StatusCodes.class,"StatusCodes.obc_Temp"      ,"OBC_Temp"      )       ),
        new Code(STATUS_EXCESS_BRAKING     , "OBC.BRAKE"    , I18N.getString(StatusCodes.class,"StatusCodes.braking"       ,"Braking"       )       ),
        new Code(STATUS_IMPACT             , "OBC.IMPACT"   , I18N.getString(StatusCodes.class,"StatusCodes.impact"        ,"Impact"        ), true ),

        new Code(STATUS_DAY_SUMMARY        , "SUMMARY.DAY"  , I18N.getString(StatusCodes.class,"StatusCodes.daySummary"    ,"Day_Summary"   )       ),

        new Code(STATUS_LOW_BATTERY        , "BATTERY.LOW"  , I18N.getString(StatusCodes.class,"StatusCodes.lowBattery"    ,"LowBattery"    ), true ),
        new Code(STATUS_BATTERY_LEVEL      , "BATTERY.LEVEL", I18N.getString(StatusCodes.class,"StatusCodes.batteryLevel"  ,"BatteryLevel"  )       ),
        new Code(STATUS_POWER_FAILURE      , "POWERFAIL"    , I18N.getString(StatusCodes.class,"StatusCodes.powerFail"     ,"PowerFail"     ), true ),
        new Code(STATUS_POWER_RESTORED     , "POWERRESTORE" , I18N.getString(StatusCodes.class,"StatusCodes.powerRestore"  ,"PowerRestore"  )       ),
        new Code(STATUS_GPS_EXPIRED        , "GPS.EXPIRE"   , I18N.getString(StatusCodes.class,"StatusCodes.gpsExpired"    ,"GPSExpired"    )       ),
        new Code(STATUS_GPS_FAILURE        , "GPS.FAILURE"  , I18N.getString(StatusCodes.class,"StatusCodes.gpsFailure"    ,"GPSFailure"    )       ),
        new Code(STATUS_DIAGNOSTIC         , "DIAGNOSTIC"   , I18N.getString(StatusCodes.class,"StatusCodes.diagnostic"    ,"Diagnostic"    )       ),
        new Code(STATUS_CONNECTION_FAILURE , "CONN.FAILURE" , I18N.getString(StatusCodes.class,"StatusCodes.connectFailure","ConnectFailure")       ),
        new Code(STATUS_MODEM_FAILURE      , "MODEM.FAILURE", I18N.getString(StatusCodes.class,"StatusCodes.modemFailure"  ,"ModemFailure"  ), true ),
        new Code(STATUS_INTERNAL_FAILURE   , "INTRN.FAILURE", I18N.getString(StatusCodes.class,"StatusCodes.internFailure" ,"InternFailure" ), true ),
        new Code(STATUS_CONFIG_RESET       , "CFG.RESET"    , I18N.getString(StatusCodes.class,"StatusCodes.configReset"   ,"ConfigReset"   )       ),
        new Code(STATUS_SHUTDOWN           , "SHURDOWN"     , I18N.getString(StatusCodes.class,"StatusCodes.shutdown"      ,"Shutdown"      )       ),
        new Code(STATUS_SUSPEND            , "SUSPEND"      , I18N.getString(StatusCodes.class,"StatusCodes.suspend"       ,"Suspend"       )       ),
        new Code(STATUS_RESUME             , "RESUME"       , I18N.getString(StatusCodes.class,"StatusCodes.resume"        ,"Resume"        )       ),

    };

    /* static status code maps */
    private static volatile Map<Integer,Code> statusCodeMap = null;
    private static volatile Map<String ,Code> statusNameMap = null;
    private static volatile boolean           statusMapInit = false;

    /* init StatusCode map (must be synchronized on "_codeArray" */
    private static void _initStatusCodeMap(int inclCodes[])
    {
        Map<Integer,Code> scMap  = new OrderedMap<Integer,Code>();
        Map<String ,Code> snMap  = new OrderedMap<String ,Code>();

        /* always add STATUS_NONE first (OrderedMap) */
        if ((inclCodes != null) && (inclCodes.length > 0)) {
            Print.logDebug("Initializing specific StatusCodes ...");
            scMap.put(new Integer(_codeArray[0].getCode()), _codeArray[0]);
            snMap.put(_codeArray[0].getName()             , _codeArray[0]);
        }

        /* add StatusCodes */
        for (int c = 0; c < _codeArray.length; c++) {
            Code    code = _codeArray[c];
            int     sc   = code.getCode();
            Integer sci  = new Integer(sc);
            String  scn  = code.getName();
            if ((inclCodes == null) || (inclCodes.length == 0)) {
                scMap.put(sci, code); // add all codes
                snMap.put(scn, code); // add all codes
            } else {
                for (int i = 0; (i < inclCodes.length); i++) {
                    if ((inclCodes[i] != STATUS_NONE) && (inclCodes[i] == sc)) {
                        //Print.logDebug("  ==> " + code);
                        scMap.put(sci, code); // add specific code
                        snMap.put(scn, code); // add specific code
                        break;
                    }
                }
            }
        }

        /* set status code map */
        statusCodeMap = scMap;
        statusNameMap = snMap;
        statusMapInit = true;

    }

    /* internal status code to 'Code' map */
    private static void _initStatusCodes()
    {
        if (!statusMapInit) { // only iff not yet initialized
            // Calling "StatusCodes.initStatusCodeMap(...)" at startup is preferred.
            Print.logInfo("StatusCodes late initialization ...");
            synchronized (_codeArray) {
                if (!statusMapInit) { // check again after lock
                    _initStatusCodeMap(null);
                }
            }
        }
    }
    
    /* internal status code to 'Code' map */
    private static Map<Integer,Code> _GetStatusCodeMap()
    {
        if (!statusMapInit) { // only iff not yet initialized
            _initStatusCodes();
        }
        return statusCodeMap;
    }

    /* internal status code to 'Name' map */
    private static Map<String,Code> _GetStatusNameMap()
    {
        if (!statusMapInit) { // only iff not yet initialized
            _initStatusCodes();
        }
        return statusNameMap;
    }

    /**
    *** (Re)Initialize status code descriptions
    *** @param inclCodes  An array of codes to include in the status code list, null to include all codes
    **/
    public static void initStatusCodes(int inclCodes[])
    {
        synchronized (_codeArray) {
            if (statusMapInit) { // already initialized?
                Print.logWarn("Re-initializing StatusCode map");
            }
            _initStatusCodeMap(inclCodes);
        }
    }

    // ------------------------------------------------------------------------

    /* add code to map */
    public static void AddCode(Code code)
    {
        if (code != null) {
            Map<Integer,Code> map = _GetStatusCodeMap();
            synchronized (_codeArray) {
                map.put(new Integer(code.getCode()), code);
            }
        }
    }

    /* add codes to map */
    public static void AddCodes(Code codeList[])
    {
        if (codeList != null) {
            for (int c = 0; c < codeList.length; c++) {
                AddCode(codeList[c]);
            }
        }
    }

    /* remove code from map */
    public static Code RemoveCode(int sc)
    {
        Map<Integer,Code> map = _GetStatusCodeMap();
        Integer sci  = new Integer(sc);
        Code    code = map.get(sci);
        if (code != null) {
            synchronized (_codeArray) {
                map.remove(sci);
            }
        }
        return code;
    }

    /* remove list of code from map */
    public static void RemoveCodes(int cList[])
    {
        if (cList != null) {
            for (int c = 0; c < cList.length; c++) {
                RemoveCode(cList[c]);
            }
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* description to status code map */
    public static Map<Integer,String> GetDescriptionMap(Locale locale)
    {
        Map<Integer,String> descMap = new OrderedMap<Integer,String>();
        Map<Integer,Code>   codeMap = _GetStatusCodeMap();
        for (Integer sci : codeMap.keySet()) {
            descMap.put(sci, codeMap.get(sci).getDescription(locale));
        }
        return descMap;
    }

    // ------------------------------------------------------------------------

    /* return specific code (from statusCode) */
    public static Code GetCode(int code, BasicPrivateLabel bpl)
    {
        Integer ic = new Integer(code);
        if (bpl != null) {
            Code sc = bpl.getStatusCode(ic);
            if (sc != null) {
                return sc;
            }
        }
        return _GetStatusCodeMap().get(ic);
    }

    /* return specific code (from statusCode) */
    public static Code GetCode(String name, BasicPrivateLabel bpl)
    {
        if (StringTools.isBlank(name)) {
            return null;
        } else {
            return _GetStatusNameMap().get(name.toUpperCase());
        }
    }

    // ------------------------------------------------------------------------

    /* parse/return specific code */
    public static int ParseCode(String codeStr, BasicPrivateLabel bpl, int dftCode)
    {
        
        /* null/empty code name */
        if (StringTools.isBlank(codeStr)) {
            return dftCode;
        }

        /* lookup name */
        StatusCodes.Code code = StatusCodes.GetCode(codeStr, bpl);
        if (code != null) {
            return code.getCode();
        }

        /* status code number? */
        if (StringTools.isInt(codeStr,true)) {
            return StringTools.parseInt(codeStr, dftCode);
        }

        /* unknown */
        return dftCode;

    }

    // ------------------------------------------------------------------------

    public static String GetName(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        if (sc != null) {
            return sc.getName();
        } else {
            return "0x" + StringTools.toHexString((long)code,16);
        }
    }

    // ------------------------------------------------------------------------

    public static boolean SetDescription(int code, String desc)
    {
        Code sc = StatusCodes.GetCode(code, null);
        if (sc != null) {
            sc.setDescription(desc);
            return true;
        } else {
            return false;
        }
    }

    public static String GetDescription(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        if (sc != null) {
            Locale locale = (pl != null)? pl.getLocale() : null; // should be "reqState.getLocale()"
            return sc.getDescription(locale);
        } else {
            String codeDesc = "0x" + StringTools.toHexString((long)code,16);
            //Print.logWarn("Code not found: " + codeDesc);
            return codeDesc;
        }
    }
 
    // ------------------------------------------------------------------------

    public static boolean SetIconName(int code, String iconName)
    {
        Code sc = StatusCodes.GetCode(code, null);
        if (sc != null) {
            sc.setIconName(iconName);
            return true;
        } else {
            return false;
        }
    }

    public static String GetIconName(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        if (sc != null) {
            return sc.getIconName();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String GetHex(int code)
    {
        return StatusCodes.GetHex((long)code);
    }

    public static String GetHex(long code)
    {
        if (code < 0L) {
            return "0x" + StringTools.toHexString(code, 32);
        } else
        if ((code & ~0xFFFFL) != 0) {
            return "0x" + StringTools.toHexString((code & 0x7FFFFFFFL), 32);
        } else {
            return "0x" + StringTools.toHexString((code & 0xFFFFL), 16);
        }
    }

    // ------------------------------------------------------------------------

    public static boolean IsReserved(int code)
    {
        return ((code >= 0xE000) && (code <= 0xFFFF));
    }

    public static boolean IsValid(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        return (sc != null);
    }

    public static boolean IsHighPriority(int code, BasicPrivateLabel pl)
    {
        Code sc = StatusCodes.GetCode(code, pl);
        return (sc != null)? sc.isHighPriority() : false;
    }

    // ------------------------------------------------------------------------

    public static int GetDigitalInputIndex(int code)
    {
        switch (code) {
            case STATUS_INPUT_ON_00          :
            case STATUS_INPUT_OFF_00         :
                return 0;
            case STATUS_INPUT_ON_01          :
            case STATUS_INPUT_OFF_01         :
                return 1;
            case STATUS_INPUT_ON_02          :
            case STATUS_INPUT_OFF_02         :
                return 2;
            case STATUS_INPUT_ON_03          :
            case STATUS_INPUT_OFF_03         :
                return 3;
            case STATUS_INPUT_ON_04          :
            case STATUS_INPUT_OFF_04         :
                return 4;
            case STATUS_INPUT_ON_05          :
            case STATUS_INPUT_OFF_05         :
                return 5;
            case STATUS_INPUT_ON_06          :
            case STATUS_INPUT_OFF_06         :
                return 6;
            case STATUS_INPUT_ON_07          :
            case STATUS_INPUT_OFF_07         :
                return 7;
            case STATUS_INPUT_ON_08          :
            case STATUS_INPUT_OFF_08         :
                return 8;
            case STATUS_INPUT_ON_09          :
            case STATUS_INPUT_OFF_09         :
                return 9;
            case STATUS_INPUT_ON_10          :
            case STATUS_INPUT_OFF_10         :
                return 10;
            case STATUS_INPUT_ON_11          :
            case STATUS_INPUT_OFF_11         :
                return 11;
            case STATUS_INPUT_ON_12          :
            case STATUS_INPUT_OFF_12         :
                return 12;
            case STATUS_INPUT_ON_13          :
            case STATUS_INPUT_OFF_13         :
                return 13;
            case STATUS_INPUT_ON_14          :
            case STATUS_INPUT_OFF_14         :
                return 14;
            case STATUS_INPUT_ON_15          :
            case STATUS_INPUT_OFF_15         :
                return 15;
            case STATUS_IGNITION_ON          :
            case STATUS_IGNITION_OFF         :
                return IGNITION_INPUT_INDEX;
        }
        return -1;
    }

    public static boolean IsIgnition(int code)
    {
        switch (code) {
            case STATUS_IGNITION_ON          :
            case STATUS_IGNITION_OFF         :
                return true;
        }
        return false;
    }

    public static boolean IsDigitalInputOn(int code)
    {
        return IsDigitalInputOn(code, true);
    }

    public static boolean IsDigitalInputOn(int code, boolean inclIgn)
    {
        
        /* omit ignition codes? */
        if (!inclIgn && IsIgnition(code)) {
            return false;
        }
        
        /* check code */
        switch (code) {
            case STATUS_INPUT_STATE          :
                return true; // always true (since we don't know the actual state)
            case STATUS_IGNITION_ON          :
            case STATUS_INPUT_ON             :
            case STATUS_INPUT_ON_00          :
            case STATUS_INPUT_ON_01          :
            case STATUS_INPUT_ON_02          :
            case STATUS_INPUT_ON_03          :
            case STATUS_INPUT_ON_04          :
            case STATUS_INPUT_ON_05          :
            case STATUS_INPUT_ON_06          :
            case STATUS_INPUT_ON_07          :
            case STATUS_INPUT_ON_08          :
            case STATUS_INPUT_ON_09          :
            case STATUS_INPUT_ON_10          :
            case STATUS_INPUT_ON_11          :
            case STATUS_INPUT_ON_12          :
            case STATUS_INPUT_ON_13          :
            case STATUS_INPUT_ON_14          :
            case STATUS_INPUT_ON_15          :
                return true;
        }
        
        /* not a digital input */
        return false;
        
    }

    public static boolean IsDigitalInputOff(int code)
    {
        switch (code) {
            case STATUS_INPUT_STATE          :
                return true; // always true (since we don't know the actual state)
            case STATUS_IGNITION_OFF         :
            case STATUS_INPUT_OFF            :
            case STATUS_INPUT_OFF_00         :
            case STATUS_INPUT_OFF_01         :
            case STATUS_INPUT_OFF_02         :
            case STATUS_INPUT_OFF_03         :
            case STATUS_INPUT_OFF_04         :
            case STATUS_INPUT_OFF_05         :
            case STATUS_INPUT_OFF_06         :
            case STATUS_INPUT_OFF_07         :
            case STATUS_INPUT_OFF_08         :
            case STATUS_INPUT_OFF_09         :
            case STATUS_INPUT_OFF_10         :
            case STATUS_INPUT_OFF_11         :
            case STATUS_INPUT_OFF_12         :
            case STATUS_INPUT_OFF_13         :
            case STATUS_INPUT_OFF_14         :
            case STATUS_INPUT_OFF_15         :
                return true;
        }
        return false;
    }

    public static boolean IsDigitalInput(int code)
    {
        return IsDigitalInputOn(code) || IsDigitalInputOff(code);
    }

    public static boolean IsDigitalInput(int code, boolean state)
    {
        if (state) {
            return IsDigitalInputOn(code);
        } else {
            return IsDigitalInputOff(code);
        }
    }

    public static int GetDigitalInputStatusCode(int ndx, boolean state)
    {
        if (ndx < 0) {
            return STATUS_NONE;
        } else
        if (ndx == IGNITION_INPUT_INDEX) {
            return state? STATUS_IGNITION_ON : STATUS_IGNITION_OFF;
        }
        switch (ndx) {
            case   0: return state? STATUS_INPUT_ON_00 : STATUS_INPUT_OFF_00;
            case   1: return state? STATUS_INPUT_ON_01 : STATUS_INPUT_OFF_01;
            case   2: return state? STATUS_INPUT_ON_02 : STATUS_INPUT_OFF_02;
            case   3: return state? STATUS_INPUT_ON_03 : STATUS_INPUT_OFF_03;
            case   4: return state? STATUS_INPUT_ON_04 : STATUS_INPUT_OFF_04;
            case   5: return state? STATUS_INPUT_ON_05 : STATUS_INPUT_OFF_05;
            case   6: return state? STATUS_INPUT_ON_06 : STATUS_INPUT_OFF_06;
            case   7: return state? STATUS_INPUT_ON_07 : STATUS_INPUT_OFF_07;
            case   8: return state? STATUS_INPUT_ON_08 : STATUS_INPUT_OFF_08;
            case   9: return state? STATUS_INPUT_ON_09 : STATUS_INPUT_OFF_09;
            case  10: return state? STATUS_INPUT_ON_10 : STATUS_INPUT_OFF_10;
            case  11: return state? STATUS_INPUT_ON_11 : STATUS_INPUT_OFF_11;
            case  12: return state? STATUS_INPUT_ON_12 : STATUS_INPUT_OFF_12;
            case  13: return state? STATUS_INPUT_ON_13 : STATUS_INPUT_OFF_13;
            case  14: return state? STATUS_INPUT_ON_14 : STATUS_INPUT_OFF_14;
            case  15: return state? STATUS_INPUT_ON_15 : STATUS_INPUT_OFF_15;
        }
        return STATUS_NONE;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_CODE[]      = new String[] { "code" };
    private static final String ARG_LIST[]      = new String[] { "list" };

    private static void usage()
    {
        Print.logInfo("Usage:");
        Print.logInfo("  java ... " + StatusCodes.class.getName() + " {options}");
        Print.logInfo("Common Options:");
        Print.logInfo("  -code=<code>    Display StatusCode description");
        Print.logInfo("  -list           List active StatusCodes");
        System.exit(1);
    }

    public static void main(String argv[])
    {
        DBConfig.cmdLineInit(argv,true);  // main

        /* display code description */
        if (RTConfig.hasProperty(ARG_CODE)) {
            int code = RTConfig.getInt(ARG_CODE,0);
            Print.sysPrintln("Code Description: " + StatusCodes.GetDescription(code,null));
            System.exit(0);
        }

        /* list codes */
        if (RTConfig.getBoolean(ARG_LIST,false)) {
            Map<Integer,Code> map = _GetStatusCodeMap();
            for (Integer sci : map.keySet()) {
                Print.sysPrintln(map.get(sci).toString());
            }
            System.exit(0);
        }

        /* usage */
        usage();

    }

}

