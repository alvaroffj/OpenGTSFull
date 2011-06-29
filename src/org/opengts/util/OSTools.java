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
//  General OS specific tools
// ----------------------------------------------------------------------------
// Change History:
//  2006/03/26  Martin D. Flynn
//     -Initial release
//  2006/06/30  Martin D. Flynn
//     -Repackaged
//  2008/06/20  Martin D. Flynn
//     -Added method 'getProcessID()'
//  2010/05/24  Martin D. Flynn
//     -Added "getMemoryUsage", "printMemoryUsage"
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.lang.management.*;
import java.util.*;
import java.io.*;

public class OSTools
{

    // ------------------------------------------------------------------------

    private static final Object LockObject          = new Object();

    // ------------------------------------------------------------------------
    // OS and JVM specific tools
    // ------------------------------------------------------------------------

    private static final int OS_INITIALIZE          = -1;
    public  static final int OS_TYPE_MASK           = 0x00FF00;
    public  static final int OS_SUBTYPE_MASK        = 0x0000FF;

    public  static final int OS_UNKNOWN             = 0x000000;
    public  static final int OS_LINUX               = 0x000100;
    public  static final int OS_MACOSX              = 0x000200;
    public  static final int OS_WINDOWS             = 0x000300;
    public  static final int OS_WINDOWS_XP          = 0x000001;
    public  static final int OS_WINDOWS_9X          = 0x000002;
    public  static final int OS_WINDOWS_CYGWIN      = 0x000010;

    private static       int OSType                 = OS_INITIALIZE;

    /**
    *** Returns the known OS type as an integer bitmask
    *** @return The OS type
    **/
    public static int getOSType()
    {
        if (OSType == OS_INITIALIZE) {
            String osName = System.getProperty("os.name").toLowerCase();
            //Print.logInfo("OS: " + osName);
            if (osName.startsWith("windows")) {
                OSType = OS_WINDOWS;
                if (osName.startsWith("windows xp")) {
                    OSType |= OS_WINDOWS_XP;
                } else
                if (osName.startsWith("windows 9") || osName.startsWith("windows m")) {
                    OSType |= OS_WINDOWS_9X;
                }
            } else
            if (osName.startsWith("mac")) {
                // "Max OS X"
                OSType = OS_MACOSX;
            } else
            if (File.separatorChar == '/') {
                // "Linux"
                OSType = OS_LINUX;
            } else {
                OSType = OS_UNKNOWN;
            }
        }
        return OSType;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the OS is unknown
    *** @return True if the OS is unknown
    **/
    public static boolean isUnknown()
    {
        return (getOSType() == OS_UNKNOWN);
    }

    /**
    *** Returns true if the OS is the specified type
    *** @return True if the OS is the specified type
    **/
    public static boolean isOSType(int type)
    {
        int osType = getOSType();
        return ((osType & OS_TYPE_MASK) == type);
    }

    /**
    *** Returns true if the OS is the specified type
    *** @return True if the OS is the specified type
    **/
    public static boolean isOSType(int type, int subType)
    {
        int osType = getOSType();
        if ((osType & OS_TYPE_MASK) != type) {
            return false;
        } else {
            return ((osType & OS_SUBTYPE_MASK & subType) != 0);
        }
    }

    /**
    *** Returns true if the OS is a flavor of Windows
    *** @return True if the OS is a flavor of Windows
    **/
    public static boolean isWindows()
    {
        return isOSType(OS_WINDOWS);
    }

    /**
    *** Returns true if the OS is Windows XP
    *** @return True if the OS is Windows XP
    **/
    public static boolean isWindowsXP()
    {
        return isOSType(OS_WINDOWS, OS_WINDOWS_XP);
    }

    /**
    *** Returns true if the OS is Windows 95/98
    *** @return True if the OS is Windows 95/98
    **/
    public static boolean isWindows9X()
    {
        return isOSType(OS_WINDOWS, OS_WINDOWS_9X);
    }

    /**
    *** Returns true if the OS is Unix/Linux
    *** @return True if the OS is Unix/Linux
    **/
    public static boolean isLinux()
    {
        return isOSType(OS_LINUX);
    }

    /**
    *** Returns true if the OS is Apple Mac OS X
    *** @return True if the OS is Apple Mac OS X
    **/
    public static boolean isMacOSX()
    {
        return isOSType(OS_MACOSX);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Get the current memory usage (in number of bytes)
    *** @param L  The long array where the memory values will be placed.  If 'null',
    ***           is specified, or if the array has fewer than 3 elements, a new
    ***           long array will be returned.
    ***           then the array must be 
    *** @return The current memory usage as an array of 3 long values indicating
    ***         { MaxMemory, TotalMemory, FreeMemory } (in that order).
    **/
    public static long[] getMemoryUsage(long L[])
    {
        long mem[] = ((L != null) && (L.length >= 3))? L : new long[3];
        Runtime rt = Runtime.getRuntime();
        synchronized (OSTools.LockObject) {
            mem[0] = rt.maxMemory();
            mem[1] = rt.totalMemory();
            mem[2] = rt.freeMemory();
        }
        return mem;
    }
    
    /**
    *** Prints the current memory usage to the log file
    **/
    public static void printMemoryUsage()
    {
        long mem[] = OSTools.getMemoryUsage(null);
        long maxK  = mem[0] / 1024L;
        long totK  = mem[1] / 1024L;
        long freK  = mem[2] / 1024L;
        Print.logInfo("Memory-K: max=%d, total=%d, free=%d, used=%d", maxK, totK, freK, (totK - freK));
        //OSTools.printMemoryUsageMXBean();
    }
    
    /**
    *** Prints the current memory usage to the log file
    **/
    public static void printMemoryUsageMXBean()
    {
        
        /* Heap/Non-Heap */
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage    = memory.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memory.getNonHeapMemoryUsage();
        Print.logInfo("Heap Memory Usage    : " + formatMemoryUsage(heapUsage   ));
        Print.logInfo("Non-Heap Memory Usage: " + formatMemoryUsage(nonHeapUsage)); 

        /* Pools */
        java.util.List<MemoryPoolMXBean> memPool = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean mp : memPool) {
            String      name      = mp.getName();
            MemoryType  type      = mp.getType();
            MemoryUsage estUsage  = mp.getUsage();
            MemoryUsage peakUsage = mp.getPeakUsage();
            MemoryUsage collUsage = mp.getCollectionUsage();
            Print.logInfo("Pool Usage: " + name + " [" + type + "]");
            Print.logInfo("  Estimate  : "  + formatMemoryUsage(estUsage ));
            Print.logInfo("  Peak      : "  + formatMemoryUsage(peakUsage));
            Print.logInfo("  Collection: "  + formatMemoryUsage(collUsage));
        }

    }
    
    /**
    *** Formats a MemoryUsage instance
    **/
    private static String formatMemoryUsage(MemoryUsage u)
    {
        if (u != null) {
            long comm = u.getCommitted() / 1024L;
            long init = u.getInit()      / 1024L;
            long max  = u.getMax()       / 1024L;
            long used = u.getUsed()      / 1024L;
            StringBuffer sb = new StringBuffer();
            sb.append("[K]");
            sb.append(" Committed=").append(comm);
            sb.append(" Init=").append(init);
            sb.append(" Max=").append(max);
            sb.append(" Used=").append(used);
            return sb.toString();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private static Object  memoryCheckLock         = new Object();
    private static long    firstMem_maxB           = 0L;
    private static long    firstMem_usedB          = 0L;
    private static long    firstMem_time           = 0L;
    private static long    memoryCheckCount        = 0L;
    private static long    averMem_usedB           = 0L;
    private static long    lastMem_usedB           = 0L;

    /**
    *** Analyzes/Prints the current memory usage.<br>
    *** (This method only analyzes/prints memory usage if the current usage is less than 
    *** the previous usage, implying that a garbage collection has recently occured)<br>
    *** Useful for determining <b>IF</b> there are memory leaks, and how much it is leaking, 
    *** but useless for determining <b>WHERE</b> the leak is occurring.
    *** @param reset  True to reset the memory growth-rate checks.
    **/
    public static void checkMemoryUsage(boolean reset)
    {
        // http://olex.openlogic.com/wazi/2009/how-to-fix-memory-leaks-in-java/
        // http://java.sun.com/docs/hotspot/gc1.4.2/faq.html
        // http://java.dzone.com/articles/letting-garbage-collector-do-c

        /* memory check enabled? */
        if (!RTConfig.getBoolean(RTKey.OSTOOLS_MEMORY_CHECK_ENABLE)) {
            return;
        }

        /* get current memory usage */
        long nowTime = DateTime.getCurrentTimeSec();
        long maxB, usedB;
        long averUsedB = 0L, firstUsedB = 0L, firstTime = 0L;
        long count = 0L;
        Runtime rt = Runtime.getRuntime();
        synchronized (OSTools.memoryCheckLock) {
            // reset?
            if (reset) {
                // start over
                OSTools.firstMem_maxB    = 0L;
                OSTools.firstMem_usedB   = 0L;
                OSTools.firstMem_time    = 0L;
                OSTools.memoryCheckCount = 0L;
                OSTools.averMem_usedB    = 0L;
                OSTools.lastMem_usedB    = 0L;
            }
            // get memory usage
            maxB  = rt.maxMemory();
            usedB = rt.totalMemory() - rt.freeMemory();
            if (usedB <= 0L) {
                // unlikely, but we need to check anyway
                Print.logWarn("Memory usage <= 0? " + usedB + " bytes");
            } else {
                if (usedB < OSTools.lastMem_usedB) {
                    // garbage collection has occurred
                    if ((OSTools.firstMem_time <= 0L) || (usedB < OSTools.firstMem_usedB)) {
                        // store results after first garbage collection
                        OSTools.firstMem_maxB    = maxB;      // should never change
                        OSTools.firstMem_usedB   = usedB;
                        OSTools.firstMem_time    = nowTime;
                        OSTools.memoryCheckCount = 0L;
                        OSTools.averMem_usedB    = 0L;
                    }
                    firstUsedB = OSTools.firstMem_usedB; // cache for use outside synchronized section
                    firstTime  = OSTools.firstMem_time;  // cache for use outside synchronized section
                    // average "trend"
                    if (OSTools.averMem_usedB <= 0L) {
                        // initialize average
                        OSTools.averMem_usedB = usedB;
                    } else
                    if (usedB <= OSTools.averMem_usedB) {
                        // always reset to minimum used (ie. 100% downward trend)
                        OSTools.averMem_usedB = usedB;
                    } else {
                        // upward "trend" determined by weighting factor
                        double trendWeight = RTConfig.getDouble(RTKey.OSTOOLS_MEMORY_TREND_WEIGHT);
                        OSTools.averMem_usedB = OSTools.averMem_usedB + (long)((double)(usedB - OSTools.averMem_usedB) * trendWeight);
                    }
                    averUsedB = OSTools.averMem_usedB; // cache for use outside synchronized section
                    // count
                    count = ++OSTools.memoryCheckCount; // increment and cache count for use outside synchronized section
                }
                OSTools.lastMem_usedB = usedB; // save last used
            }
        } // synchronized

        /* return if a garbage collection has not just occurred */
        if (count <= 0L) {
            return;
        }

        /* analyze */
        double deltaHours = (double)(nowTime - firstTime) / 3600.0;
        long   deltaUsedB = averUsedB - firstUsedB; // could be <= 0
        long   grwBPH     = (deltaHours > 0.0)? (long)(deltaUsedB / deltaHours) : 0L; // bytes/hour
        long   grwBPC     = deltaUsedB / count; // bytes/hour

        /* message */
        long maxK  = maxB      / 1024;
        long usedK = usedB     / 1024;
        long averK = averUsedB / 1024;
        String s = "["+count+"] Memory-K max "+maxK+ ", used "+usedK+ " (trend "+averK+ " K "+grwBPH+" b/h "+ grwBPC+" b/c)";

        /* display */
        double maxPercent = RTConfig.getDouble(RTKey.OSTOOLS_MEMORY_USAGE_WARN);
        if (usedB >= (long)((double)maxB * maxPercent)) {
            Print.logWarn("**** More than "+(maxPercent*100.0)+"% of max memory has been used!! ****");
            Print.logWarn(s);
        } else {
            Print.logInfo(s);
        }

    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns true if this implementation has a broken 'toFront' Swing implementation.<br>
    *** (may only be applicable on Java v1.4.2)
    *** @return True if this implementation has a broken 'toFront' Swing implementation.
    **/
    public static boolean isBrokenToFront()
    {
        return isWindows();
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static final String PROPERTY_JAVA_HOME                   = "java.home";
    public static final String PROPERTY_JAVA_VENDOR                 = "java.vendor";
    public static final String PROPERTY_JAVA_SPECIFICATION_VERSION  = "java.specification.version";

    /**
    *** Returns true if executed from a Sun Microsystems JVM.
    *** @return True is executed from a Sun Microsystems JVM.
    **/
    public static boolean isSunJava()
    {
        String propVal = System.getProperty(PROPERTY_JAVA_VENDOR); // "Sun Microsystems Inc."
        if ((propVal == null) || (propVal.indexOf("Sun Microsystems") < 0)) {
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    @SuppressWarnings("proprietary")  // <-- does not work to supress the "Sun proprietary API" warning
    private static Class _getCallerClass(int frame)
        throws Throwable
    {
        return sun.reflect.Reflection.getCallerClass(frame + 1); // <== ignore any warnings
    }

    /**
    *** Gets the class of the caller at the specified frame index
    *** @param frame The frame index
    *** @return The calling class
    **/
    public static Class getCallerClass(int frame)
    {
        try {
            // sun.reflect.Reflection.getCallerClass(0) == sun.reflect.Reflection
            // sun.reflect.Reflection.getCallerClass(1) == OSTools
            Class clz = OSTools._getCallerClass(frame + 1);
            //Print._println("" + (frame + 1) + "] class " + StringTools.className(clz));
            return clz;
        } catch (Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java (or other non-Sun version).
            Print.logException("Sun Microsystems version of Java is not in use", th);
            return null;
        }
    }

    /**
    *** Returns true if 'sun.reflect.Reflection' is present in the runtime libraries.<br>
    *** (will return true when running with the Sun Microsystems version of Java)
    *** @return True if 'getCallerClass' is available.
    **/
    public static boolean hasGetCallerClass()
    {
        try {
            OSTools._getCallerClass(0);
            return true;
        } catch (Throwable th) {
            return false;
        }
    }

    /**
    *** Prints the class of the caller (debug purposes only)
    **/
    public static void printCallerClasses()
    {
        try {
            for (int i = 0;; i++) {
                Class clz = OSTools._getCallerClass(i);
                Print.logInfo("" + i + "] class " + StringTools.className(clz));
                if (clz == null) { break; }
            }
        } catch (Throwable th) { // ClassNotFoundException
            // This can occur when the code has been compiled with the Sun Microsystems version
            // of Java, but is executed with the GNU version of Java.
            Print.logException("Sun Microsystems version of Java is not in use", th);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Process-ID of this JVM invocation.<br>
    *** IMPORTANT: This implementation relies on a "convention", rather that a documented method
    *** of obtaining the process-id of this JVM within the OS.  <b>Caveat Emptor!</b><br>
    *** (On Windows, this returns the 'WINPID' which is probably useless anyway)
    *** @return The Process-ID
    **/
    public static int getProcessID()
    {
        // References:
        //  - http://blog.igorminar.com/2007/03/how-java-application-can-discover-its.html
        if (OSTools.isSunJava()) {
            try {
                // by convention, returns "<PID>@<host>" (until something changes, and it doesn't)
                String n = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                int pid = StringTools.parseInt(n,-1); // parse PID
                return pid;
            } catch (Throwable th) {
                Print.logException("Unable to obtain Process ID", th);
                return -1;
            }
        } else {
            return -1;
        }
    }

    /* this does not work on Windows (and seems to return the wrong parent PID on Linux) */
    private static int _getProcessID()
    {
        try {
            String cmd[] = new String[] { "bash", "-c", "echo $PPID" };
            Process ppidExec = Runtime.getRuntime().exec(cmd);
            BufferedReader ppidReader = new BufferedReader(new InputStreamReader(ppidExec.getInputStream()));
            StringBuffer sb = new StringBuffer();
            for (;;) {
                String line = ppidReader.readLine();
                if (line == null) { break; }
                sb.append(StringTools.trim(line));
            }
            int pid = StringTools.parseInt(sb.toString(),-1);
            int exitVal = ppidExec.waitFor();
            Print.logInfo("Exit value: %d [%s]", exitVal, sb.toString());
            ppidReader.close();
            return pid;
        } catch (Throwable th) {
            Print.logException("Unable to obtain PID", th);
            return -1;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns a Java command set up to be executed by Runtime.getRuntime().exec(...)
    *** @param classpath The classpath 
    *** @param className The main Java class name
    *** @param args The command line arguments
    *** @return A command to call and it's arguments
    **/
    public static String[] createJavaCommand(String classpath[], String className, String args[])
    {
        java.util.List<String> execCmd = new Vector<String>();
        execCmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        execCmd.add("-classpath");
        if (ListTools.isEmpty(classpath)) {
            execCmd.add(System.getProperty("java.class.path"));
        } else {
            StringBuffer sb = new StringBuffer();
            for (String p : classpath) {
                if (sb.length() > 0) { sb.append(File.pathSeparator); }
                sb.append(p);
            }
            execCmd.add(sb.toString());
        }
        execCmd.add(className);
        if (!ListTools.isEmpty(args)) {
            for (String a : args) {
                execCmd.add(a);
            }
        }
        return execCmd.toArray(new String[execCmd.size()]);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Main entry point for testing/debugging
    *** @param argv Comand-line arguments
    **/
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        System.out.println("OS Type ...");
        Print.sysPrintln("Is Windows  : " + isWindows());
        Print.sysPrintln("Is Windows9X: " + isWindows9X());
        Print.sysPrintln("Is WindowsXP: " + isWindowsXP());
        Print.sysPrintln("Is Linux    : " + isLinux());
        Print.sysPrintln("Is MacOSX   : " + isMacOSX());
        Print.sysPrintln("PID #1      : " + getProcessID());
        Print.sysPrintln("PID #2      : " + _getProcessID());
        Runtime rt = Runtime.getRuntime();
        Print.sysPrintln("Total Mem   : " + rt.totalMemory()/(1024.0*1024.0));
        Print.sysPrintln("Max Mem     : " + rt.maxMemory()/(1024.0*1024.0));
        Print.sysPrintln("Free Mem    : " + rt.freeMemory()/(1024.0*1024.0));
    }

}
