package org.opengts.servers.GP6000;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.opengts.util.DateTime;
import org.opengts.util.Print;

/**
 *
 * @author Alvaro
 */
public class Command {
    public String hex;
    public String[] aux;
    public int length;
    public String imei;
    public String pVer;
    public String commandType;
    public String commandSN;
    public String[] parameters;
    public String[] data;
    public String fecha;
    public String date;
    public String time;
    public Long fixtime;
    public double latitude;
    public double longitude;

    public Command(String hex) {
        this.hex = hex;
        this.parseString();
    }
    
    public final void parseString() {
        int[] largos = new int[14];
        largos[0] = 20;
        largos[1] = 2;
        largos[2] = 6;
        largos[3] = 2;
        largos[4] = 2;
        largos[5] = 2;
        largos[6] = 2;
        largos[7] = 0;
        largos[8] = 2;
        largos[9] = 24;
        largos[10] = 2;
        largos[11] = 20;
        largos[12] = 2;
        largos[13] = 18;
        int n = 14;
        int acum = 0;
        this.parameters = new String[4];
        this.aux = new String[14];
        this.hex = this.hex.substring(2, this.hex.length()-2);
        for(int i=0; i<n; i++) {
            if(i!=7) {
                this.aux[i] = this.hex.substring(acum, acum+largos[i]);
                this.aux[i] = this.hexToString(this.aux[i]);
            } else {
                largos[i] = Integer.parseInt(aux[i-1])*2;
                this.aux[i] = this.hex.substring(acum, acum+largos[i]);
            }
            acum += largos[i]+2;
        }
        this.imei = this.aux[0];
        this.pVer = this.aux[1];
        this.commandType = this.aux[2];
        this.commandSN = this.aux[3];
        this.parameters[0] = this.aux[4];
        this.parameters[1] = this.aux[5];
        this.parameters[2] = this.aux[6];
        this.parameters[3] = this.aux[7];
        int dataL = Integer.parseInt(this.parameters[2]);
        this.data = new String[dataL];
        for(int i=0; i<dataL; i++) {
            this.data[i] = this.parameters[3].substring(i*2, (i+1)*2);
        }
        this.fecha = this.aux[9];
        this.date = this.fecha.substring(0, 6);
        this.time = this.fecha.substring(6);
        this.fixtime = this.getFixtime(Long.parseLong(this.date), Long.parseLong(this.time));
        this.latitude = this.parseLatitude(this.aux[13], this.aux[12]);
        this.longitude = this.parseLongitude(this.aux[11], this.aux[10]);
    }
    
    public void save() {
        String datos;
        String sensor = "&sd=";
        String base = "http://localhost/backend/alerta.php?"
                + "IMEI=" + this.imei + ""
                + "&statusCode=61472"
                + "&tipo_dato=2";

//        datos = "&fixtime=" + this.fixtime + ""
//                + "&horautc=" + this.time + ""
//                + "&fechautc=" + this.date + ""
//                + "&latitude=" + this.latitude + ""
//                + "&longitude=" + this.longitude + ""
//                + "&rawData=" + this.hex;
        for(int i=0; i<this.data.length; i++) {
            sensor += this.data[i];
        }
        try {
            lanzar(base+sensor);
        } catch (MalformedURLException e) {
                Print.logInfo(e.getMessage());
        } catch (IOException e) {
                Print.logInfo(e.getMessage());
        }
    }

    private void lanzar(String url1) throws MalformedURLException, IOException {
        URL url = new URL(url1);
        URLConnection con = url.openConnection();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String linea;
        Print.logInfo("Enviando : " + url1);
        while ((linea = in.readLine()) != null) {
            Print.logInfo("Recibido : " + linea);
        }
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
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName());
        result.append(" Object {");
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = this.getClass().getDeclaredFields();

        //print field names paired with their values
        for (Field field : fields) {
            result.append("  ");
            try {
                result.append(field.getName());
                result.append(": ");
                //requires access to private field:
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
        double _lat = Double.parseDouble(s);
        if (_lat < 99999.0) {
            double lat = (double)((long)_lat / 100L); // _lat is always positive here
            lat += (_lat - (lat * 100.0)) / 60.0;
            return d.equals("S")? -lat : lat;
        } else {
            return 90.0; // invalid latitude
        }
    }
    
    private double parseLongitude(String s, String d) {
        double _lon = Double.parseDouble(s);
        if (_lon < 99999.0) {
            double lon = (double)((long)_lon / 100L); // _lon is always positive here
            lon += (_lon - (lon * 100.0)) / 60.0;
            return d.equals("W")? -lon : lon;
        } else {
            return 180.0; // invalid longitude
        }
    }
}
