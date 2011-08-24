package org.opengts.servers.GP6000;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import org.opengts.util.Print;

/**
 *
 * @author Alvaro
 */
public class Gps {

    public String hex;
    public String head;
    public String imei;
    public String pVer;
    public int dataType;
    public int dataLength;
    public List<GpsData> paquete;
    public int fin;

    public Gps(String hex) {
//        Print.logInfo(hex);
        this.paquete = new LinkedList();
        this.hex = hex;
        this.parseString();
    }

    public void parseString() {
        this.head = this.hex.substring(0, 2);
        this.imei = this.hex.substring(2, 12);
        this.pVer = this.hex.substring(12, 13);
        this.dataType = Integer.parseInt(this.hex.substring(13, 14), 16);
        this.dataLength = Integer.parseInt(this.hex.substring(14, 18), 16);
        GpsData d;
        if (this.dataType == 1 || this.dataType == 2) {
            if(hex.length() >= 18 + this.dataLength * 2) {
                d = new GpsData(this.hex.substring(18, 18 + this.dataLength * 2));
                this.fin = 18 + this.dataLength * 2;
            } else {
                d = new GpsData(this.hex.substring(18));
                this.fin = this.hex.length();
            }
            this.paquete.add(d);
            this.hex = this.hex.substring(0, this.fin);
        } else {
            this.fin = 18;
            int n = this.dataLength / 27;
            String auxHex = "";
            for (int i = 0; i < n; i++) {
                if(this.hex.length() <= this.fin) {
                    break;
                } else {
                    if(this.hex.length() < this.fin + 54) {
                        auxHex = this.hex.substring(this.fin);
                        this.fin = this.hex.length();
//                        System.out.println(" ->* "+this.fin);
                    } else {
                        auxHex = this.hex.substring(this.fin, this.fin + 54);
                        this.fin += 54;
//                        System.out.println(" -> "+this.fin);
                    }
                    d = new GpsData(auxHex);
                    this.paquete.add(d);
                }
            }
            this.hex = this.hex.substring(0, this.fin);
        }
    }

    public void save() {
        String datos;
        String base = "http://localhost/backend/alerta.php?"
                + "IMEI=" + this.imei + ""
                + "&statusCode=61472";

        int n=this.paquete.size();
        for(int i=0; i<n; i++) {
            GpsData aux = this.paquete.get(i);
//            Print.logInfo(aux.toString());
            datos = "&fixtime=" + aux.fixtime + ""
                    + "&horautc=" + aux.time + ""
                    + "&fechautc=" + aux.date + ""
                    + "&latitude=" + aux.latitude + ""
                    + "&longitude=" + aux.longitude + ""
                    + "&gpsAge=0"
                    + "&speedKPH=" + aux.speed + ""
                    + "&heading=" + aux.heading + ""
                    + "&altitude=" + 0 + ""
                    + "&distanceKM=0"
                    + "&odometerKM=" + aux.mileage + ""
                    + "&sa="+aux.status[0]+""+aux.status[1]+""+aux.status[2]+""+aux.status[3]+""
                    + "&rawData=" + this.hex;
            try {
                lanzar(base+datos);
            } catch (MalformedURLException e) {
                Print.logInfo(e.getMessage());
            } catch (IOException e) {
                Print.logInfo(e.getMessage());
            }
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
}
