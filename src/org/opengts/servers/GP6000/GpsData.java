package org.opengts.servers.GP6000;

import java.lang.reflect.Field;
import org.opengts.util.DateTime;

/**
 *
 * @author Alvaro
 */
public class GpsData {
    public String hex;
    public String date;
    public String time;
    public Long fixtime;
    public String locating;
    public double latitude;
    public double longitude;
    public float speed;
    public int heading;
    public int fuelLevelH;
    public int fuelLevelL;
    public int fuelLevel;
    public String[] status;
    public int mileage;
    public int sn;

    public GpsData(String hex) {
        this.hex = hex;
        this.parseString();
    }
    
    public void parseString() {
        this.date = this.hex.substring(0, 6);
        this.time = this.hex.substring(6, 12);
        this.fixtime = this.getFixtime(Long.parseLong(this.date), Long.parseLong(this.time));
        String auxLat = this.hex.substring(12, 20);
        String auxLon = this.hex.substring(20, 29);
        this.locating = this.hexToBin(this.hex.substring(29, 30), 4);
        this.latitude = this.parseLatitude(auxLat, (this.locating.substring(2, 3).equals("1"))?"N":"S");
        this.longitude = this.parseLongitude(auxLon, (this.locating.substring(1, 2).equals("1"))?"E":"W");
        this.speed = (float) (Integer.parseInt(this.hex.substring(30, 32), 16)*1.85);
        this.heading = Integer.parseInt(this.hex.substring(32, 34), 16);
        this.fuelLevelH = Integer.parseInt(this.hex.substring(34, 36), 16);
        if(this.hex.length()>54)
            this.fuelLevelL = Integer.parseInt(this.hex.substring(52, 54), 16);
        else this.fuelLevelL = 0;
        this.fuelLevel = this.fuelLevelH*256 + this.fuelLevelL;
        this.status = this.parseStatus(this.hex.substring(36, 44));
        this.mileage = Integer.parseInt(this.hex.substring(44, 52), 16);
        if(this.hex.length()>56)
            this.sn = Integer.parseInt(this.hex.substring(54, 56), 16);
    }
    
    public long getFixtime(Long ddmmyy, Long hhmmss) {
        return this._getUTCSeconds(ddmmyy, hhmmss);
    }
    
    private long _getUTCSeconds(long dmy, long hms) {
        int    HH  = (int)((hms / 10000L) % 100L);
        int    MM  = (int)((hms / 100L) % 100L);
        int    SS  = (int)(hms % 100L);
        long   TOD = (HH * 3600L) + (MM * 60L) + SS;
    
        long DAY;
        if (dmy > 0L) {
            int    yy  = (int)(dmy % 100L) + 2000;
            int    mm  = (int)((dmy / 100L) % 100L);
            int    dd  = (int)((dmy / 10000L) % 100L);
            long   yr  = ((long)yy * 1000L) + (long)(((mm - 3) * 1000) / 12);
            DAY        = ((367L * yr + 625L) / 1000L) - (2L * (yr / 1000L))
                         + (yr / 4000L) - (yr / 100000L) + (yr / 400000L)
                         + (long)dd - 719469L;
        } else {
            long   utc = DateTime.getCurrentTimeSec();
            long   tod = utc % DateTime.DaySeconds(1);
            DAY        = utc / DateTime.DaySeconds(1);
            long   dif = (tod >= TOD)? (tod - TOD) : (TOD - tod); // difference should be small (ie. < 1 hour)
            if (dif > DateTime.HourSeconds(12)) { // 12 to 18 hours
                if (tod > TOD) {
                    DAY++;
                } else {
                    DAY--;
                }
            }
        }
        long sec = DateTime.DaySeconds(DAY) + TOD;
        return sec;
    }
    
    public String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);
            sb.append((char) decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }
    
    public String[] parseStatus(String hex) {
        String[] r = new String[4];
        for(int i=0; i<4; i++) {
            r[i] = this.hexToBin(hex.substring(2*(4-i-1), 2*(4-i)), 8);
        }
        return r;
    }
    
    public float hexToFloat(String hex) {
        Long i = Long.parseLong(hex, 16);
        Float f = Float.intBitsToFloat(i.intValue());
        return f;
    }
    
    public String hexToBin(String hex, int largo) {
        int i = Integer.parseInt(hex, 16);
        String s = Integer.toBinaryString(i);
        int largoAux = s.length(); 
        if(largoAux<largo) {
            String com = "";
            for(int j=0; j<largo-largoAux; j++) {
                com += "0";
            }
            s = com+s;
        }
        return s;
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName());
        result.append(" Object {");
        result.append(newLine);

        Field[] fields = this.getClass().getDeclaredFields();

        for (Field field : fields) {
            result.append("  ");
            try {
                result.append(field.getName());
                result.append(": ");
                result.append(field.get(this));
            } catch (IllegalAccessException ex) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");

        return result.toString();
    }

    private double parseLatitude(String s, String d) {
        double _lat = Double.parseDouble(s)/10000;
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equals("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
    }
    
    private double parseLongitude(String s, String d) {
        double _lon = Double.parseDouble(s)/10000;
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equals("W")? -lon : lon;
        } else {
            return 180.0; // invalid longitude
        }
    }
}
