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
//  This class provides many File based utilities
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//      Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/05/14  Martin D. Flynn
//     -Added method 'writeEscapedUnicode'
//  2009/04/02  Martin D. Flynn
//     -Added command-line "-strings=<len>" option for display string content 
//      from an input file specified with "-file=<file>"
//  2009/05/24  Martin D. Flynn
//     -Added "copyFile" method
//     -Changed command-line arguments.
//  2009/06/01  Martin D. Flynn
//     -Added 'toFile(URL)' method
//  2010/01/29  Martin D. Flynn
//     -Added additional 'toFile' methods
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;

/**
*** File handling tools
**/

public class FileTools
{

    // ------------------------------------------------------------------------

    /**
    *** Convert a File specification to a URL
    *** @param file  The file specification
    *** @return A URL representing the specified file
    *** @throws MalformedURLException
    ***     If a protocol handler for the URL could not be found,
    ***     or if some other error occurred while constructing the URL
    **/
    public static URL toURL(File file)
        throws MalformedURLException
    {
        if (file == null) {
            return null;
        } else {
            return file.toURI().toURL();
        }
    }

    /**
    *** Convert a URL specification to a File
    *** @param url  The URL specification
    *** @return A file representing the specified URL
    *** @throws URIsyntaxException if the protocol was not 'file' or URL could 
    ***     otherwise not be converted to a file
    **/
    public static File toFile(URL url)
        throws URISyntaxException
    {
        if (url == null) {
            return null;
        } else 
        if (!url.getProtocol().equalsIgnoreCase("file")) {
            throw new URISyntaxException(url.toString(), "Invalid protocol (expecting 'file')");
        } else {
            try {
                return new File(url.toURI());
            } catch (IllegalArgumentException iae) {
                return new File(HTMLTools.decodeParameter(url.getPath()));
            }
        }
    }

    /**
    *** Convert a URL specification to a File
    *** @param base  The base file specification
    *** @param path  The file path directories (last element may be a file name)
    *** @return A file representing the specified path
    **/
    public static File toFile(File base, String path[])
    {
        if (base == null) {
            return null;
        } else
        if (ListTools.isEmpty(path)) {
            return base;
        } else {
            File b = base;
            for (int i = 0; i < path.length; i++) {
                b = new File(b, path[i]);
            }
            return b;
        }
    }

    /**
    *** Convert a URL specification to a File
    *** @param path  The file path directories (last element may be a file name)
    *** @return A file representing the specified path
    **/
    public static File toFile(String path[])
    {
        if (ListTools.isEmpty(path)) {
            return null;
        } else {
            File b = new File(path[0]);
            for (int i = 1; i < path.length; i++) {
                b = new File(b, path[i]);
            }
            return b;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Copies an input URL to an output file
    *** @param inpURL  The URL from which data will be read
    *** @param outFile The File to whish data will be written
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, File outFile)
        throws IOException
    {
        return FileTools.copyFile(inpURL, outFile, false);
    }
    
    /**
    *** Copies an input URL to an output file
    *** @param inpURL  The URL from which data will be read
    *** @param outFile The File to whish data will be written
    *** @param progress Show progress
    *** @return The number of bytes copied
    **/
    public static int copyFile(URL inpURL, File outFile, boolean progress)
        throws IOException
    {
        if ((inpURL != null) && (outFile != null)) {
            int cnt = 0;
            InputStream  uis = null;
            OutputStream out = null;
            try {
                URLConnection urlConnect = inpURL.openConnection();
                urlConnect.setConnectTimeout(5000); 
                urlConnect.setReadTimeout(5000); 
                uis = urlConnect.getInputStream(); // openStream();
                int len = urlConnect.getContentLength();
                out = new FileOutputStream(outFile, false);
                cnt = FileTools.copyStreams(uis, out, null/*pattern*/, len, progress);
            } finally {
                closeStream(uis);
                closeStream(out);
            }
            return cnt;
        } else {
            return 0;
        }
    }

    // ------------------------------------------------------------------------
    
    private static  int COPY_STREAMS_BLOCK_SIZE = 200 * 1024;

    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @return The number of bytes copied
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output)
        throws IOException
    {
        return FileTools.copyStreams(input, output, null/*pattern*/, -1/*maxLen*/, false/*progress*/);
    }

    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @param maxLen The maximum number of bytes to copy
    *** @return The number of bytes copied
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output, 
        int maxLen)
        throws IOException
    {
        return FileTools.copyStreams(input, output, null/*pattern*/, maxLen, false/*progress*/);
    }
   
    /**
    *** Copies bytes from one stream to another
    *** @param input  The InputStream
    *** @param output The OutputStream
    *** @param maxLen The maximum number of bytes to copy
    *** @param progress Show progress
    *** @return The number of bytes copied
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output, 
        byte pattern[], int maxLen)
        throws IOException
    {
        return FileTools.copyStreams(input, output, pattern, maxLen, false/*progress*/);
    }
    
    /**
    *** Copies bytes from one stream to another.<br>
    *** If 'maxLen' is >= '0', then at most 'maxLen' bytes will be written to the output stream.
    *** If 'pattern' is specified, the stream will be scanned for the first occurance of the
    *** matching pattern, however, only 'maxLen' bytes will be written.
    *** to the output stream.
    *** @param input   The InputStream
    *** @param output  The OutputStream
    *** @param pattern The pattern to match, to terminate the copy 
    *** @param maxLen  The maximum number of bytes to copy
    *** @param progress Show progress (to stdout)
    *** @return The number of bytes copied
    *** @throws EOFException if the end of stream is reached before the pattern is found
    *** @throws IOException if an I/O error occurs
    **/
    public static int copyStreams(InputStream input, OutputStream output, 
        byte pattern[], int maxLen, 
        boolean progress)
        throws EOFException, IOException
    {

        /* copy nothing? */
        if ((input == null) || (output == null) || (maxLen == 0)) {
            return 0;
        }

        /* pattern? */
        boolean hasPattern = false;
        int pndx = 0, plen = 0;;
        if ((pattern != null) && (pattern.length > 0)) {
            hasPattern = true;
            plen = pattern.length;
            //Print.logInfo("Pattern: ["+plen+"] " + StringTools.toStringValue(pattern,'.'));
        }

        /* copy bytes */
        int  stopType  = 0; // 1=eof, 2=maxlen, 3=pattern
        long startMS   = DateTime.getCurrentTimeMillis();
        long lastMS    = 0L;
        int  length    = 0; // count of bytes copied
        byte tmpBuff[] = new byte[COPY_STREAMS_BLOCK_SIZE + plen]; // copy block size
        byte ch[]      = new byte[1];
        while (true) {

            /* read length */
            int readLen;
            if (!hasPattern && (maxLen > 0)) {
                readLen = maxLen - length;
                if (readLen <= 0) {
                    break; // done reading
                } else
                if (readLen > COPY_STREAMS_BLOCK_SIZE) {
                    readLen = COPY_STREAMS_BLOCK_SIZE; // max block size
                }
            } else {
                readLen = COPY_STREAMS_BLOCK_SIZE; // not '0'
            }
            // 'readLen' is > 0 here

            /* read input stream */
            int cnt;
            if (hasPattern) {
                // read until pattern (could probably be optimized)
                cnt = 0;
                for (;cnt < readLen;) {
                    int c = input.read(ch, 0, 1);
                    if (c < 0) {
                        // EOF
                        if (cnt <= 0) {
                            // if we've read nothing, set 'cnt' to EOF as well
                            cnt = c;
                        }
                        break;
                    } else 
                    if (c == 0) {
                        // 'cnt' contains the count we've read so far
                        break;
                    }
                    // normal length (c == 1)
                    if (ch[0] == pattern[pndx]) {
                        // pattern character matched
                        //Print.logInfo("Pattern byte matched: " + pndx + " 0x" + StringTools.toHexString(ch));
                        pndx++;
                        if (pndx >= plen) {
                            // full pattern matched
                            break;
                        }
                        // continue;
                    } else {
                        // start over with pattern matching
                        //Print.logInfo("Pattern byte reset: " + pndx + " 0x" + StringTools.toHexString(ch) + " != 0x" + StringTools.toHexString(pattern[pndx]));
                        if (pndx > 0) {
                            // copy current matched portion into buffer
                            System.arraycopy(pattern, 0, tmpBuff, cnt, pndx); 
                            cnt += pndx;
                            pndx = 0;
                        }
                        tmpBuff[cnt++] = ch[0];
                    }
                } // for (cnt < readLen)
            } else {
                // read entire block
                cnt = input.read(tmpBuff, 0, readLen);
            }

            /* copy to output stream */
            if (cnt < 0) {
                // EOF
                stopType = 1; // EOF
                if (hasPattern) {
                    throw new EOFException("Pattern not found");
                } else
                if (progress && (maxLen > 0) && (length != maxLen)) {
                    Print.logError("Copy size mismatch: " + maxLen + " --> " + length);
                }
                break;
            } else
            if (cnt > 0) {
                if (maxLen < 0) {
                    output.write(tmpBuff, 0, cnt);
                    length += cnt;
                } else
                if (length < maxLen) {
                    int wrtLen = ((length + cnt) > maxLen)? (maxLen - length) : cnt;
                    output.write(tmpBuff, 0, wrtLen);
                    length += wrtLen;
                    if (!hasPattern && (length >= maxLen)) {
                        stopType = 2; // maxlen
                        break; // per 'maxLen', done copying
                    }
                }
            } else {
                //Print.logDebug("Read 0 bytes ... continuing");
            }

            /* pattern matched? */
            if (hasPattern && (pndx >= plen)) {
                stopType = 3; // pattern
                break; // per 'pattern', done copying
            }

            /* show progress */
            if (progress) {
                // Copying XXXXX of XXXXX bytes
                long nowMS = DateTime.getCurrentTimeMillis();
                if ((nowMS - lastMS) >= 1000L) {
                    lastMS = nowMS;
                    double elapseMS = (double)(nowMS - startMS);
                    if (maxLen > 0) {
                        double p = (double)length / (double)maxLen;
                        double totalMS = elapseMS / p;
                        //double remainMS = totalMS - elapseMS;
                        Print.sysPrint("Copying - %7d/%d bytes (%2.0f/%.0f sec, %2.0f%%)\r", 
                            length, maxLen, (elapseMS/1000.0), (totalMS/1000.0), (p*100.0));
                    } else {
                        Print.sysPrint("Copying - %7d bytes (%2.0f sec)\r", 
                            length, (elapseMS/1000.0));
                    }
                }
            }

        } // while (true)
        output.flush();

        /* show final progress */
        if (progress) {
            // Copied XXXXX of XXXXX bytes
            double elapseMS = (double)(DateTime.getCurrentTimeMillis() - startMS);
            Print.sysPrintln("Copied - %7d bytes (%.0f sec)                                     ", 
                length, (elapseMS/1000.0));
        }

        /* return number of bytes copied */
        return length;
        
    }

    // ------------------------------------------------------------------------

    /**
    *** Opens the specified file for reading
    *** @param file  The path of the file to open
    *** @return The opened InputStream
    **/
    public static InputStream openInputFile(String file)
    {
        if ((file != null) && !file.equals("")) {
            return FileTools.openInputFile(new File(file));
        } else {
            return null;
        }
    }

    /**
    *** Opens the specified file for reading
    *** @param file  The file to open
    *** @return The opened InputStream
    **/
    public static InputStream openInputFile(File file)
    {
        try {
            return new FileInputStream(file);
        } catch (IOException ioe) {
            Print.logError("Unable to open file: " + file + " [" + ioe + "]");
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Closes the specified InputStream
    *** @param in  The InputStream to close
    **/
    public static void closeStream(InputStream in)
    {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ioe) {
                //Print.logError("Unable to close stream: " + ioe);
            }
        }
    }
    
    /**
    *** Closes the specified OutputStream
    *** @param out  The OutputStream to close
    **/
    public static void closeStream(OutputStream out)
    {
        if (out != null) {
            try {
                out.close();
            } catch (IOException ioe) {
                //Print.logError("Unable to close stream: " + ioe);
            }
        }
    }

    // ------------------------------------------------------------------------

    /** 
    *** Returns an array of bytes read from the specified InputStream
    *** @param input  The InputStream
    *** @return The array of bytes read from the InputStream
    *** @throws IOException if an I/O error occurs
    **/
    public static byte[] readStream(InputStream input)
        throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FileTools.copyStreams(input, output, null/*pattern*/, -1/*maxLen*/, false/*progress*/);
        return output.toByteArray();
    }

    // ------------------------------------------------------------------------

    /**
    *** Writes a String to the specified OutputStream
    *** @param output  The OutputStream 
    *** @param dataStr The String to write to the OutputStream
    *** @throws IOException if an I/O error occurs
    **/
    public static void writeStream(OutputStream output, String dataStr)
        throws IOException
    {
        byte data[] = dataStr.getBytes();
        output.write(data, 0, data.length);
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file  The file path from which the byte array is read
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(String file)
    {
        if ((file != null) && !file.equals("")) {
            return FileTools.readFile(new File(file));
        } else {
            return null;
        }
    }

    /**
    *** Returns an array of bytes read from the specified file
    *** @param file  The file from which the byte array is read
    *** @return The byte array read from the specified file
    **/
    public static byte[] readFile(File file)
    {
        if (file == null) {
            return null;
        } else
        if (!file.exists()) {
            Print.logError("File does not exist: " + file);
            return null;
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                return readStream(fis);
            } catch (IOException ioe) {
                Print.logError("Unable to read file: " + file + " [" + ioe + "]");
            } finally {
                if (fis != null) { try { fis.close(); } catch (IOException ioe) {/*ignore*/} }
            }
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Reads a single line of characters from the specified InputStream, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @param input  The InputStream
    *** @return The line read from the InputStream
    *** @throws EOFException if the end of the input stream is encountered
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLine(InputStream input)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch < 0) { // eof
                throw new EOFException("End of InputStream");
            } else
            if ((ch == '\r') || (ch == '\n')) {
                return sb.toString();
            }
            sb.append((char)ch);
        }
    }

    /**
    *** Reads a single line of characters from the specified InputStream, terminated by
    *** a newline only (\n).  Carriage-returns (\r) are ignored.
    *** @param input  The InputStream
    *** @return The line read from the InputStream
    *** @throws EOFException if the end of the input stream is encountered
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLineNL(InputStream input)
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch < 0) { // eof
                throw new EOFException("End of InputStream");
            } else
            if (ch == '\r') {
                continue;
            } else
            if (ch == '\n') {
                return sb.toString();
            }
            sb.append((char)ch);
        }
    }

    /**
    *** Reads a single line of characters from stdin, terminated by
    *** either a newline (\n) or carriage-return (\r)
    *** @return The line read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static String readLine_stdin()
        throws IOException
    {
        while (System.in.available() > 0) { System.in.read(); }
        return FileTools.readLine(System.in);
    }

    /**
    *** Prints a message, and reads a line of text from stdin
    *** @param msg  The message to print
    *** @param dft  The default String returned, if no text was entered
    *** @return The line of text read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static String readString_stdin(String msg, String dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [String: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin();
            if (line.equals("")) {
                if (dft != null) {
                    return dft;
                } else {
                    // if there is no default, a non-empty String is required
                    Print.sysPrint("String required, please re-enter] ");
                    continue;
                }
            }
            return line;
        }
    }

    /**
    *** Prints a message, and reads a boolean value from stdin
    *** @param msg  The message to print
    *** @param dft  The default boolean value returned, if no value was entered
    *** @return The boolean value read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static boolean readBoolean_stdin(String msg, boolean dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Boolean: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!StringTools.isBoolean(line,true)) {
                Print.sysPrint("Boolean required, please re-enter] ");
                continue;
            }
            return StringTools.parseBoolean(line, dft);
        }
    }

    /**
    *** Prints a message, and reads a long value from stdin
    *** @param msg  The message to print
    *** @param dft  The default long value returned, if no value was entered
    *** @return The long value read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static long readLong_stdin(String msg, long dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Long: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!Character.isDigit(line.charAt(0)) && (line.charAt(0) != '-')) {
                Print.sysPrint("Long required, please re-enter] ");
                continue;
            }
            return StringTools.parseLong(line, dft);
        }
    }

    /**
    *** Prints a message, and reads a double value from stdin
    *** @param msg  The message to print
    *** @param dft  The default double value returned, if no value was entered
    *** @return The double value read from stdin
    *** @throws IOException if an I/O error occurs
    **/
    public static double readDouble_stdin(String msg, double dft)
        throws IOException
    {
        if (msg == null) { msg = ""; }
        Print.sysPrintln(msg + "    [Double: default='" + dft + "'] ");
        for (;;) {
            Print.sysPrint("?");
            String line = FileTools.readLine_stdin().trim();
            if (line.equals("")) {
                return dft;
            } else
            if (!Character.isDigit(line.charAt(0)) && (line.charAt(0) != '-') && (line.charAt(0) != '.')) {
                Print.sysPrint("Double required, please re-enter] ");
                continue;
            }
            return StringTools.parseDouble(line, dft);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
    *** Writes a byte array to the specified file
    *** @param data  The byte array to write to the file
    *** @param file  The file to which the byte array is written
    *** @return True if the bytes were successfully written to the file
    *** @throws IOException if an I/O error occurs
    **/
    public static boolean writeFile(byte data[], File file)
        throws IOException
    {
        return FileTools.writeFile(data, file, false);
    }

    /**
    *** Writes a byte array to the specified file
    *** @param data  The byte array to write to the file
    *** @param file  The file to which the byte array is written
    *** @param append True to append the bytes to the file, false to overwrite.
    *** @return True if the bytes were successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeFile(byte data[], File file, boolean append)
        throws IOException
    {
        if ((data != null) && (file != null)) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file, append);
                fos.write(data, 0, data.length);
                return true;
            } finally {
                try { fos.close(); } catch (Throwable t) {/* ignore */}
            }
        } 
        return false;
    }

    /**
    *** Writes a String to the specified file in "ISO-8859-1" character encoding.<br>
    *** Unicode characters are escaped using the '\u0000' format.
    *** @param dataStr  The String to write to the file
    *** @param file     The file to which the byte array is written
    *** @return True if the String was successfully written to the file
    *** @throws IOException if an error occurred.
    **/
    public static boolean writeEscapedUnicode(String dataStr, File file)
        throws IOException
    {
        boolean append = false;
        if ((dataStr != null) && (file != null)) {
            FileOutputStream fos = new FileOutputStream(file, append);
            BufferedWriter fbw = null;
            try {
                fbw = new BufferedWriter(new OutputStreamWriter(fos, "8859_1"));
                int len = dataStr.length();
                for (int i = 0; i < len; i++) {
                    char ch = dataStr.charAt(i);
                    if ((ch == '\n') || (ch == '\r')) {
                        fbw.write(ch);
                    } else
                    if ((ch == '\t') || (ch == '\f')) {
                        fbw.write(ch);
                    } else
                    if ((ch < 0x0020) || (ch > 0x007e)) {
                        fbw.write('\\');
                        fbw.write('u');
                        fbw.write(StringTools.hexNybble((ch >> 12) & 0xF));
                        fbw.write(StringTools.hexNybble((ch >>  8) & 0xF));
                        fbw.write(StringTools.hexNybble((ch >>  4) & 0xF));
                        fbw.write(StringTools.hexNybble( ch        & 0xF));
                    } else {
                        fbw.write(ch);
                    }
                }
                return true;
            } finally {
                try { fbw.close(); } catch (Throwable t) {/* ignore */}
            }
        } 
        return false;
    }

    // ------------------------------------------------------------------------

    /** 
    *** Gets the extension characters from the specified file name
    *** @param filePath  The file name
    *** @return The extension characters
    **/
    public static String getExtension(String filePath) 
    {
        if (filePath != null) {
            return getExtension(new File(filePath));
        }
        return "";
    }

    /** 
    *** Gets the shortest possible extension characters from the specified file.
    *** IE. "file.aa.bb.cc" would return extension "cc".
    *** @param file  The file
    *** @return The extension characters
    **/
    public static String getExtension(File file) 
    {
        if (file != null) {
            String fileName = file.getName();
            int p = fileName.lastIndexOf(".");
            if ((p >= 0) && (p < (fileName.length() - 1))) {
                return fileName.substring(p + 1);
            }
        }
        return "";
    }

    /**
    *** Returns true if the specified file path has an extension which matches one of the
    *** extensions listed in the specified String array
    *** @param filePath  The file path/name
    *** @param extn      An array of file extensions
    *** @return True if the specified file path has a matching exention
    **/
    public static boolean hasExtension(String filePath, String extn[])
    {
        if (filePath != null) {
            return hasExtension(new File(filePath), extn);
        }
        return false;
    }

    /**
    *** Returns true if the specified file has an extension which matches one of the
    *** extensions listed in the specified String array
    *** @param file      The file
    *** @param extn      An array of file extensions
    *** @return True if the specified file has a matching exention
    **/
    public static boolean hasExtension(File file, String extn[])
    {
        if ((file != null) && (extn != null)) {
            String e = getExtension(file);
            for (int i = 0; i < extn.length; i++) {
                if (e.equalsIgnoreCase(extn[i])) { return true; }
            }
        }
        return false;
    }

    /**
    *** Removes the extension from the specified file path
    *** @param filePath  The file path from which the extension will be removed
    *** @return The file path with the extension removed
    **/
    public static String removeExtension(String filePath)
    {
        if (filePath != null) {
            return removeExtension(new File(filePath));
        }
        return filePath;
    }

    /**
    *** Removes the extension from the specified file
    *** @param file  The file from which the extension will be removed
    *** @return The file path with the extension removed
    **/
    public static String removeExtension(File file)
    {
        if (file != null) {
            String fileName = file.getName();
            int p = fileName.indexOf(".");
            if (p > 0) { // '.' in column 0 not allowed
                file = new File(file.getParentFile(), fileName.substring(0, p));
            }
            return file.getPath();
        } else {
            return null;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified character is a file separator
    *** @param ch  The character to test
    *** @return True if the specified character is a file separator
    **/
    public static boolean isFileSeparatorChar(char ch)
    {
        if (ch == File.separatorChar) {
            // simple test, matches Java's understanding of a file path separator
            return true;
        } else 
        if (OSTools.isWindows() && (ch == '/')) {
            // '/' can be used as a file path separator on Windows
            return true;
        } else {
            // not a file path separator character
            return false;
        }
    }

    /**
    *** Returns true if the specified String contains a file separator
    *** @param fn  The String file path
    *** @return True if the file String contains a file separator
    **/
    public static boolean hasFileSeparator(String fn)
    {
        if (fn == null) {
            // no string, no file separator
            return false;
        } else
        if (fn.indexOf(File.separator) >= 0) {
            // simple test, matches Java's understanding of a file path separator
            return true;
        } else
        if (OSTools.isWindows() && (fn.indexOf('/') >= 0)) {
            // '/' can be used as a file path separator on Windows
            return true;
        } else {
            // no file path separator found
            return false;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Resolves specified command relative to the environment "PATH" variable
    *** @param cmd  The command name.  If the specified command is an absolute path,
    ***             then the specified command path will me returned as-is.
    *** @return The resolved command path
    **/
    public static File resolveCommand(String cmd)
    {
        if (StringTools.isBlank(cmd)) {
            return null;
        } else {
            File cmdFile = new File(cmd);
            if (cmdFile.isAbsolute()) {
                return cmdFile;
            } else {
                String envPath = System.getenv("PATH");
                String path[] = StringTools.split(envPath, File.pathSeparatorChar);
                for (int i = 0; i < path.length; i++) {
                    File cmdf = new File(path[i], cmd);
                    if (cmdf.isFile()) {
                        Print.logInfo("Found: " + cmdf);
                        return cmdf;
                    }
                }
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------

    /* remove null bytes from data */
    private static byte[] deNullify(byte data[])
    {
        byte nonull[] = new byte[data.length];
        int n = 0;
        for (int d = 0; d < data.length; d++) {
            if (data[d] != 0) {
                nonull[n++] = data[d];
            }
        }
        if (n < data.length) {
            byte x[] = new byte[n];
            System.arraycopy(nonull,0, x,0, n);
            nonull = x;
        }
        return nonull;
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns true if the specified item is a file
    *** @param f  The item to check
    *** @return True if the specified item is a file
    **/
    public static boolean isFile(File f)
    {
        return (f != null) && f.isFile();
    }

    /**
    *** Returns true if the specified item is a file with the specified extension
    *** @param f    The item to check
    *** @param ext  The required file extension
    *** @return True if the specified item is a file with the specified extension
    **/
    public static boolean isFile(File f, String ext)
    {
        if ((f == null) || !f.isFile()) {
            return false;
        } else
        if (ext != null) {
            String fe = FileTools.getExtension(f);
            return ext.equalsIgnoreCase(fe);
        } else {
            return true;
        }
    }

    /**
    *** Returns true if the specified item is a directory
    *** @param d  The item to check
    *** @return True if the specified item is a directory
    **/
    public static boolean isDirectory(File d)
    {
        return (d != null) && d.isDirectory();
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the file path for the first file found in the specified list
    *** @param parentDir  The parent directory
    *** @param fileNames  The list of files to search for
    *** @return The path to the file, or null if not found
    **/
    public static File findFile(File parentDir, String fileNames[])
    {

        /* invalid parent directory? */
        if (!FileTools.isDirectory(parentDir)) {
            return null;
        }

        /* invalid file list */
        if ((fileNames == null) || (fileNames.length <= 0)) {
            return null;
        }

        /* search list */
        for (String fn : fileNames) {
            File file = new File(parentDir, fn);
            if (file.isFile()) {
                return file;
            }
        }

        /* not found */
        return null;

    }
    
    // ------------------------------------------------------------------------

    /**
    *** Return an array of all filesystem 'root' directories
    *** @return An array of all filesystem 'root' directories
    **/
    public static File[] getFilesystemRoots()
    {
        return File.listRoots();
    }

    /**
    *** Returns an array of sub-directories within the specified parent directory.
    *** Files are not included in the list.
    *** @param dir  The parent directory
    *** @return An array of sub-directories within the specified parent directory, or null
    ***         if the specified file is not a directory.
    **/
    public static File[] getDirectories(File dir)
    {
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        } else {
            return dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            });
        }
    }

    /**
    *** Returns an array of files within the specified parent directory.
    *** Directories are not included in the list.
    *** @param dir  The parent directory
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified file is not a directory.
    **/
    public static File[] getFiles(File dir)
    {
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        } else {
            return dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isFile();
                }
            });
        }
    }

    /**
    *** Returns an array of files within the specified parent directory which match 
    *** the specified extensions.
    *** Directories are not included in the list.
    *** @param dir  The parent directory
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified file is not a directory.
    **/
    public static File[] getFiles(File dir, final Set<String> extnSet)
    {
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        } else {
            return dir.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    if (!file.isFile()) {
                        return false;
                    } else
                    if (extnSet == null) {
                        return true;
                    } else {
                        String extn = FileTools.getExtension(file);
                        return extnSet.contains(extn);
                    }
                }
            });
        }
    }

    /**
    *** Returns an array of files within the specified parent directory which match 
    *** the specified extensions.
    *** Directories are not included in the list.
    *** @param dir  The parent directory
    *** @return An array of files within the specified parent directory, or null
    ***         if the specified directory is not a directory.
    **/
    public static File[] getFiles(File dir, String extnList[])
    {
        if ((dir == null) || !dir.isDirectory()) {
            return null;
        } else
        if (extnList == null) {
            return FileTools.getFiles(dir);
        } else {
            Set<String> extnSet = ListTools.toSet(extnList, new HashSet<String>());
            return FileTools.getFiles(dir, extnSet);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static final String ARG_WGET[]          = new String[] { "wget"     , "url"   };    // URL
    private static final String ARG_TODIR[]         = new String[] { "dir"      , "todir" };    // directory
    private static final String ARG_WHERE[]         = new String[] { "where"              };    // command name
    private static final String ARG_WIDTH[]         = new String[] { "width"    , "w"     };    // int
    private static final String ARG_DUMP[]          = new String[] { "dump"               };    // boolean
    private static final String ARG_STRINGS[]       = new String[] { "strings"            };    // boolean
    private static final String ARG_UNI_ENCODE[]    = new String[] { "uniencode", "ue"    };    // boolean
    private static final String ARG_UNI_DECODE[]    = new String[] { "unidecode", "ud"    };    // boolean

    /**
    *** Display usage
    **/
    private static void usage()
    {
        Print.sysPrintln("Usage:");
        Print.sysPrintln("  java ... " + FileTools.class.getName() + " {options}");
        Print.sysPrintln("Options:");
        Print.sysPrintln("  -dump=<file>                Print hex dump");
        Print.sysPrintln("  -strings=<file>             Display all contained strings");
        Print.sysPrintln("  -where=<cmd>                Find location of file in path");
        Print.sysPrintln("  -wget=<url> [-todir=<dir>]  Read file from URL and copy to current dir");

        System.exit(1);
    }

    /**
    *** Debug/Testing entry point
    *** @param argv  The Command-line args
    **/
    public static void main(String argv[])
        throws Throwable
    {
        RTConfig.setCommandLineArgs(argv);

        /* wget */
        if (RTConfig.hasProperty(ARG_WGET)) {
            Print.sysPrintln("");
            String urlStr = RTConfig.getString(ARG_WGET,"");
            int p = urlStr.lastIndexOf('/');
            if (p > 0) {
                String name = urlStr.substring(p+1);
                try {
                    File toDir   = RTConfig.getFile(ARG_TODIR,null);
                    if (toDir == null) { toDir = new File("."); }
                    URL  inpURL  = new URL(urlStr);
                    File outFile = new File(toDir,name);
                    if (outFile.exists()) {
                        throw new IOException("File already exists: " + outFile);
                    }
                    Print.sysPrintln("Copy from: " + inpURL);
                    Print.sysPrintln("Copy to  : " + outFile);
                    FileTools.copyFile(inpURL, outFile, true);
                    Print.sysPrintln("");
                    System.exit(0);
                } catch (FileNotFoundException fnfe) {
                    Print.sysPrintln("File does not exist: " + urlStr);
                    System.exit(99);
                } catch (SocketTimeoutException ste) {
                    Print.sysPrintln("Unable to read URL (timeout): " + urlStr);
                    System.exit(99);
                } catch (Throwable th) {
                    Print.logException("Copying URL", th);
                    System.exit(99);
                }
            }
            Print.sysPrintln("Invalid URL: " + urlStr);
            System.exit(1);
        }

        /* where */
        if (RTConfig.hasProperty(ARG_WHERE)) {
            File file = RTConfig.getFile(ARG_WHERE,null);
            Print.sysPrintln("Where: " + FileTools.resolveCommand(file.toString()));
            System.exit(0);
        }

        /* hex dump */
        if (RTConfig.hasProperty(ARG_DUMP)) {
            File file = RTConfig.getFile(ARG_DUMP,null);
            byte data[] = FileTools.readFile(file);
            System.out.println("Size " + ((data!=null)?data.length:-1));
            int width = RTConfig.getInt(ARG_WIDTH,16);
            System.out.println(StringTools.formatHexString(data,width));
            System.exit(0);
        }

        /* unicode encode */
        if (RTConfig.hasProperty(ARG_UNI_DECODE)) {
            File file = RTConfig.getFile(ARG_UNI_DECODE,null);
            byte data[] = FileTools.readFile(file);
            String dataStr = StringTools.unescapeUnicode(StringTools.toStringValue(data));
            Print.sysPrintln(dataStr);
            System.exit(0);
        }

        /* display strings > 4 chars */
        if (RTConfig.hasProperty(ARG_STRINGS)) {
            File file = RTConfig.getFile(ARG_STRINGS,null);
            byte data[] = FileTools.readFile(file);
            int len = RTConfig.getInt(ARG_WIDTH,4);
            if (len < 0) {
                len = -len;
                data = deNullify(data);
            }
            int s = -1, n = 0;
            for (int b = 0; b < data.length; b++) {
                if ((data[b] >= (byte)32) && (data[b] <= (byte)127)) {
                    if (s < 0) { s = b; }
                    n++;
                } else 
                if (s >= 0) {
                    if ((b - s) >= len) {
                        String x = StringTools.toStringValue(data, s, b - s);
                        Print.sysPrintln(x);
                    }
                    s = -1;
                }
            }
            if (s >= 0) {
                if ((data.length - s) >= len) {
                    String x = StringTools.toStringValue(data, s, data.length - s);
                    Print.sysPrintln(x);
                }
                s = -1;
            }
            System.exit(0);
        }

        /* pattern test */
        if (RTConfig.hasProperty("pattern")) {
            COPY_STREAMS_BLOCK_SIZE = RTConfig.getInt("pattern",COPY_STREAMS_BLOCK_SIZE);
            if (COPY_STREAMS_BLOCK_SIZE <= 0) { COPY_STREAMS_BLOCK_SIZE = 50 * 1024; }
            int maxLen = 75;
            Print.sysPrintln("Block Size: " + COPY_STREAMS_BLOCK_SIZE);
            byte pattern[] = "--pattrn--".getBytes();
            String file = 
                "Now is the time for all good ment to come to the aid of their country\n" +
                "--pattrn--\n" +
                "123456789+123456789+123456789+123456789+123456789+123456789+123456789\n" +
                "--pattrn--\n" +
                "Now is the time for all good ment to come to the aid of their country\n" +
                "123456789+123456789+123456789+123456789+123456789+123456789+123456789\n" +
                "Now is the time for all good ment to come to the aid of their country\n" +
                "123456789+123456789+123456789+123456789+123456789+123456789+123456789\n" +
                "";
            byte fileb[] = file.getBytes();
            Print.sysPrintln("String Len: " + fileb.length);
            ByteArrayInputStream  bais = new ByteArrayInputStream(fileb);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileTools.copyStreams(bais, baos, pattern, maxLen, false);
            byte inb[] = baos.toByteArray();
            Print.sysPrintln("Read Length: " + inb.length);
            Print.sysPrintln("Read String: \n" + StringTools.toStringValue(inb));
            System.exit(0);
        }

        /* done */
        usage();

    }

}
