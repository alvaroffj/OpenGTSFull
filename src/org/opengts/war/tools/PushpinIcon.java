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
//  2008/07/27  Martin D. Flynn
//     -Initial release
//  2009/10/02  Martin D. Flynn
//     -Added "TextIcon" to allow combining a pushpin with embedded text.
//  2009/11/10  Martin D. Flynn
//     -Added PushpinChooser support
//  2009/12/16  Martin D. Flynn
//     -Added separate error message for TextIcon text/font errors
// ----------------------------------------------------------------------------
package org.opengts.war.tools;

import java.util.*;
import java.io.*;

import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import javax.swing.ImageIcon;
import javax.imageio.ImageIO;

import org.opengts.util.*;
    
public class PushpinIcon
{

    // ------------------------------------------------------------------------
    // References:
    // Google marker/pushpins:
    //   http://gmapicons.googlepages.com/home
    //      - http://www.google.com/mapfiles/marker.png
    //      - http://www.google.com/mapfiles/dd-start.png
    //      - http://www.google.com/mapfiles/dd-end.png
    //      - http://www.google.com/mapfiles/marker[A..Z].png
    //      - http://www.google.com/mapfiles/shadow50.png
    //      - http://maps.google.com/mapfiles/arrow.png
    //      - http://maps.google.com/mapfiles/arrowshadow.png
    // Other possible images:
    //   http://www.greendmedia.com/img_project/googleMapIcons/
    //   http://www1.arcwebservices.com/v2006/help/index_Left.htm#StartTopic=support/arcwebicons.htm#|SkinName=ArcWeb
    //   http://www.gpsdrive.de/development/map-icons/overview.en.shtml
    //   http://mapki.com/index.php?title=Icon_Image_Sets
    // Create your own map marker/pushpins:
    //   http://www.gmaplive.com/marker_maker.php
    //   http://www.cartosoft.com/mapicons
    //   http://gmaps-utility-library.googlecode.com/svn/trunk/mapiconmaker/1.0/examples/markericonoptions-wizard.html
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Borrowed from Google Maps.  Change these to your own desired push-pins
    // (The dimension of these images is 12x20)

    private static final String G_PUSHPIN_URL           = "http://labs.google.com/ridefinder/images/";

    private static final String G_PUSHPIN_BLACK         = G_PUSHPIN_URL + "mm_20_black.png";
    private static final String G_PUSHPIN_BROWN         = G_PUSHPIN_URL + "mm_20_brown.png";
    private static final String G_PUSHPIN_RED           = G_PUSHPIN_URL + "mm_20_red.png";
    private static final String G_PUSHPIN_ORANGE        = G_PUSHPIN_URL + "mm_20_orange.png";
    private static final String G_PUSHPIN_YELLOW        = G_PUSHPIN_URL + "mm_20_yellow.png";
    private static final String G_PUSHPIN_GREEN         = G_PUSHPIN_URL + "mm_20_green.png";
    private static final String G_PUSHPIN_BLUE          = G_PUSHPIN_URL + "mm_20_blue.png";
    private static final String G_PUSHPIN_PURPLE        = G_PUSHPIN_URL + "mm_20_purple.png";
    private static final String G_PUSHPIN_GRAY          = G_PUSHPIN_URL + "mm_20_gray.png";
    private static final String G_PUSHPIN_WHITE         = G_PUSHPIN_URL + "mm_20_white.png";
    
    private static final int    G_ICON_SIZE[]           = new int[] { 12, 20 };
    private static final int    G_ICON_OFFSET[]         = new int[] {  6, 20 };

    private static final String G_PUSHPIN_SHADOW        = G_PUSHPIN_URL + "mm_20_shadow.png";
    
    private static final int    G_SHADOW_SIZE[]         = new int[] { 22, 20 };

    // ------------------------------------------------------------------------

    public  static       String DEFAULT_TEXT_FONT       = "SanSerif";
    
    // ------------------------------------------------------------------------
    // Default icon images included with OpenGTS
    // (created using the "Marker Maker" tool at "http://www.gmaplive.com/marker_maker.php")

    private static final String PUSHPIN_URL             = "images/pp/";

    private static final String PUSHPIN_BLACK           = PUSHPIN_URL + "pin30_black.png";
    private static final String PUSHPIN_BROWN           = PUSHPIN_URL + "pin30_brown.png";
    private static final String PUSHPIN_RED             = PUSHPIN_URL + "pin30_red.png";
    private static final String PUSHPIN_ORANGE          = PUSHPIN_URL + "pin30_orange.png";
    private static final String PUSHPIN_YELLOW          = PUSHPIN_URL + "pin30_yellow.png";
    private static final String PUSHPIN_GREEN           = PUSHPIN_URL + "pin30_green.png";
    private static final String PUSHPIN_BLUE            = PUSHPIN_URL + "pin30_blue.png";
    private static final String PUSHPIN_PURPLE          = PUSHPIN_URL + "pin30_purple.png";
    private static final String PUSHPIN_GRAY            = PUSHPIN_URL + "pin30_gray.png";
    private static final String PUSHPIN_WHITE           = PUSHPIN_URL + "pin30_white.png";
    
    private static final int    ICON_SIZE[]             = new int[] { 18, 30 };
    private static final int    ICON_OFFSET[]           = new int[] {  9, 30 };

    private static final String PUSHPIN_SHADOW          = PUSHPIN_URL + "pin30_shadow.png";
    
    private static final int    SHADOW_SIZE[]           = new int[] { 30, 30 };
    
    // ------------------------------------------------------------------------

    private static OrderedMap<String,PushpinIcon> DefaultPushpinIconMap = null;

    /**
    *** Returns the PushpinIcon map
    *** @param reqState  The RequestProperties state from the current session
    *** @return The PushpinIcon map
    **/
    private static OrderedMap<String,PushpinIcon> _DefaultPushpinIconMap()
    {
        if (DefaultPushpinIconMap == null) {
            DefaultPushpinIconMap = new OrderedMap<String,PushpinIcon>();
            DefaultPushpinIconMap.put("black" , new PushpinIcon("black" ,PUSHPIN_BLACK , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("brown" , new PushpinIcon("brown" ,PUSHPIN_BROWN , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("red"   , new PushpinIcon("red"   ,PUSHPIN_RED   , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("orange", new PushpinIcon("orange",PUSHPIN_ORANGE, ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("yellow", new PushpinIcon("yellow",PUSHPIN_YELLOW, ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("green" , new PushpinIcon("green" ,PUSHPIN_GREEN , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("blue"  , new PushpinIcon("blue"  ,PUSHPIN_BLUE  , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("purple", new PushpinIcon("purple",PUSHPIN_PURPLE, ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("gray"  , new PushpinIcon("gray"  ,PUSHPIN_GRAY  , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
            DefaultPushpinIconMap.put("white" , new PushpinIcon("white" ,PUSHPIN_WHITE , ICON_SIZE, ICON_OFFSET, PUSHPIN_SHADOW, SHADOW_SIZE,null));
        }
        return DefaultPushpinIconMap;
    }

    /**
    *** Returns a shalow copy of the PushpinIcon map
    *** @return The PushpinIcon map
    **/
    public static OrderedMap<String,PushpinIcon> newDefaultPushpinIconMap()
    {
        return new OrderedMap<String,PushpinIcon>(_DefaultPushpinIconMap()); // shadow copy
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final String MARKER_ARG_ICON          = "icon";
    public static final String MARKER_ARG_FRAME         = "fr";
    public static final String MARKER_ARG_FILL_COLOR    = "fill";
    public static final String MARKER_ARG_BORDER_COLOR  = "border";
    public static final String MARKER_ARG_COLOR         = "color";
    public static final String MARKER_ARG_TEXT          = "text";
    public static final String MARKER_ARG_ARROW         = "arrow";

    public static class TextIcon
    {
        private File        iconFile        = null;
        private int         frameOffset_x   = 0;
        private int         frameOffset_y   = 0;
        private int         frameWidth      = 0;
        private int         frameHeight     = 0;
        private ImageIcon   imageIcon       = null;
        private Color       fillColor       = null;
        private Color       bordColor       = null;
        private Color       foreColor       = null;
        private String      fontName        = null;
        private double      fontSize        = 0.0;
        public TextIcon(File file, int xOfs, int yOfs, int xSiz, int ySiz) {
            this(file, xOfs, yOfs, xSiz, ySiz, 0, null);
        }
        public TextIcon(File file, int xOfs, int yOfs, int xSiz, int ySiz, int fontPt) {
            this(file, xOfs, yOfs, xSiz, ySiz, fontPt, null);
        }
        public TextIcon(File file, int xOfs, int yOfs, int xSiz, int ySiz, int fontPt, String fontName) {
            this.iconFile       = ((file != null) && file.isFile())? file : null;
            this.frameOffset_x  = xOfs;
            this.frameOffset_y  = yOfs;
            this.frameWidth     = xSiz;
            this.frameHeight    = ySiz;
            this.fontSize       = (fontPt > 0)? (double)fontPt : 8.0;
            this.fontName       = StringTools.blankDefault(fontName, DEFAULT_TEXT_FONT);
            try {
                this.imageIcon  = (this.iconFile != null)? new ImageIcon(this.iconFile.toString()) : null;
            } catch (Throwable th) { // X11 Window error
                Print.logError("Unable to create ImageIcon: " + th);
                this.imageIcon  = null;
            }
            //Print.logInfo("File=%s, X/Y=%d/%d,  W/H=%d/%d", file, xOfs, yOfs, xSiz, ySiz);
        }
        public void setFillColor(Color color) {
            this.fillColor = color;
        }
        public void setBorderColor(Color color) {
            this.bordColor = color;
        }
        public int getFrameOffset_X() {
            return this.frameOffset_x;
        }
        public void setForegroundColor(Color color) {
            this.foreColor = color;
        }
        public int getFrameOffset_Y() {
            return this.frameOffset_y;
        }
        public int getFrameWidth() {
            return this.frameWidth;
        }
        public int getFrameHeight() {
            return this.frameHeight;
        }
        public String getFontName() {
            return !StringTools.isBlank(this.fontName)? this.fontName : DEFAULT_TEXT_FONT;
        }
        public double getFontSize() {
            return (this.fontSize > 0.0)? this.fontSize : 8.0;
        }
        public boolean hasImageIcon() {
            return (this.imageIcon != null) && (this.imageIcon.getImageLoadStatus() == MediaTracker.COMPLETE);
        }
        public ImageIcon getImageIcon() {
            return this.hasImageIcon()? this.imageIcon : null;
        }
        public RenderedImage createImage(String text, double arrow) {
            ImageIcon icon = this.getImageIcon();
            if (icon == null) {
                return null;
            }
            int xOfs = this.getFrameOffset_X();
            int yOfs = this.getFrameOffset_Y();
            int xSiz = this.getFrameWidth();
            int ySiz = this.getFrameHeight();
            // get image
            BufferedImage image = null;
            Graphics2D    g2D   = null;
            try {
                int W = icon.getIconWidth();
                int H = icon.getIconHeight();
                image = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                g2D = image.createGraphics();
                /* draw template icon */
                g2D.drawImage(icon.getImage(),0,0,W,H,new ImageObserver() {
                    public boolean imageUpdate(Image img, int infoflags, int x, int y, int W, int H) {
                        return false;
                    }
                });
                /* fill rectangular region with color */
                if (this.fillColor != null) {
                    g2D.setColor(this.fillColor);
                    g2D.fillRect(xOfs, yOfs, xSiz, ySiz);
                }
                if (this.bordColor != null) {
                    g2D.setColor(this.bordColor);
                    g2D.drawRect(xOfs, yOfs, xSiz-1, ySiz-1);
                }
            } catch (Throwable th) {
                Print.logError("Unable to create TextIcon image: " + th);
                return null; // fatal error
            }
            // draw text
            try {
                text = StringTools.trim(text);
                int txLen = text.length();
                if (txLen > 0) {
                    double pt = this.getFontSize();
                    g2D.setFont(new Font(this.getFontName(), Font.PLAIN, (int)Math.round(pt)));
                    g2D.setColor((this.foreColor != null)? this.foreColor : Color.black);
                    double txPx = g2D.getFontMetrics().stringWidth(text); // (pt * 0.59375) * (double)txLen;
                    double dx = ((double)xSiz - txPx) / 2.0;
                    double dy = (((double)ySiz - pt) / 2.0) + pt - 1.0; // reasonable guess at height
                    if (dx < 0) { dx = 0; }
                    g2D.drawString(text, (float)((double)xOfs + dx), (float)((double)yOfs + dy));
                }
            } catch (Throwable th) {
                Print.logError("Unable to draw text into Icon: " + th);
                // continuing without drawn text
            }
            // draw arrow
            try {
                if (arrow >= 0.0) {
                    // draw arrow
                    double L        = this.getFontSize() + 2.0;
                    Polygon poly    = new Polygon();
                    double thetaR   = (360.0 - ((arrow - 20.0) % 360.0)) * GeoPoint.RADIANS;
                    double thetaC   = (360.0 - ((arrow       ) % 360.0)) * GeoPoint.RADIANS;
                    double thetaL   = (360.0 - ((arrow + 20.0) % 360.0)) * GeoPoint.RADIANS;
                    double dx       = Math.sin(thetaC) * L;
                    double dy       = Math.cos(thetaC) * L;
                    int    axOfs    = xOfs + (xSiz/2) - (int)Math.round(dx/2) + 1;
                    int    ayOfs    = yOfs + (ySiz/2) - (int)Math.round(dy/2) + 1;
                    poly.addPoint(0, 0);
                    poly.addPoint((int)(Math.sin(thetaR)*L)      , (int)(Math.cos(thetaR)*L      ));
                    poly.addPoint((int)(Math.sin(thetaC)*(L-2.0)), (int)(Math.cos(thetaC)*(L-2.0)));
                    poly.addPoint((int)(Math.sin(thetaL)*L)      , (int)(Math.cos(thetaL)*L      ));
                    poly.addPoint(0, 0);
                    poly.translate(axOfs, ayOfs);
                    g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2D.setColor((this.foreColor != null)? this.foreColor : Color.black);
                  //g2D.draw(poly); // outline
                    g2D.fill(poly);
                }
            } catch (Throwable th) {
                Print.logError("Unable to draw arrow into Icon: " + th);
                // continuing without drawn text
            }
            // return image
            try {
                g2D.dispose(); // release graphics
            } catch (Throwable th) {
                Print.logError("Error disposing image graphics: " + th);
            }
            return image;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    
    public static void writePushpinChooserJS(PrintWriter out, RequestProperties reqState, boolean inclBlank)
        throws IOException
    {
        Locale  locale = reqState.getLocale();
        MapProvider mp = reqState.getMapProvider();
        OrderedMap<String,PushpinIcon> iconMap = (mp != null)? mp.getPushpinIconMap(reqState) : null;
        JavaScriptTools.writeStartJavaScript(out);
        PushpinIcon.writePushpinImageArray(out, iconMap, locale, inclBlank);
        JavaScriptTools.writeEndJavaScript(out);
        JavaScriptTools.writeJSInclude(out, JavaScriptTools.qualifyJSFileRef("PushpinChooser.js"));
    }

    public static void writePushpinImageArray(PrintWriter out, OrderedMap<String,PushpinIcon> pushpinMap, Locale locale, boolean inclBlank)
        throws IOException
    {
        int count = 0;
        StringBuffer sb = new StringBuffer();
        sb.append("var ppcPushpinChooserList = new Array(\n");
        if (inclBlank) {
            I18N   i18n = I18N.getI18N(PushpinIcon.class, locale);
            String desc = i18n.getString("PushpinIcon.default","default");
            sb.append("   { name:\"\", desc:\""+desc+"\", isEval:false, image:\"\", width:24, height:0, index:-1 }");
            count++;
        }
        if (pushpinMap != null) {
            int ppNdx = 0;
            for (Iterator<PushpinIcon> p = pushpinMap.values().iterator(); p.hasNext();) {
                PushpinIcon ppi = p.next();
                if (count++ > 0) { sb.append(",\n"); }
                String desc = ppi.getName();
                sb.append("   {");
                sb.append(" name:\"").append(ppi.getName()).append("\",");
                sb.append(" desc:\"").append(desc).append("\",");
                sb.append(" isEval:").append(ppi.getIconEval()).append(",");
                sb.append(" image:\"").append(StringTools.blankDefault(ppi.getImageURL(),"?")).append("\",");
                sb.append(" width:").append(ppi.getIconWidth()).append(",");
                sb.append(" height:").append(ppi.getIconHeight()).append(",");
                sb.append(" index:").append(ppNdx++);
                sb.append(" }");
            }
            sb.append("\n");
        }
        sb.append("   );\n");
        out.write(sb.toString());
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String          name         = "";
    
    private String          iconURL      = null;
    private boolean         iconEval     = false;
    private int             iconWidth    = 12;
    private int             iconHeight   = 20;
    private int             iconOffsetX  = 6;
    private int             iconOffsetY  = 20;
    
    private String          shadowURL    = null;
    private int             shadowWidth  = 22;
    private int             shadowHeight = 20;
    
    private String          imageURL     = null;

    // ------------------------------------------------------------------------

    /**
    *** Constructor 
    *** @param name         This PushPin name
    *** @param icon         The icon image url
    *** @param iconEval     True if 'icon' contains a JavaScript function to be evaluated
    *** @param iconWidth    The icon image width (in pixels)
    *** @param iconHeight   The icon image height (in pixels)
    *** @param iconOffsetX  The X offset to the icon image 'hotspot' (in pixels)
    *** @param iconOffsetY  The Y offset to the icon image 'hotspot' (in pixels)
    *** @param shadow       The icon shadow image url
    *** @param shadowWidth  The shadow image width (in pixels)
    *** @param shadowHeight The shadow image height (in pixels)
    *** @param imageURL     The image used to represent the icon for user selection
    **/
    public PushpinIcon(
        String name  ,
        String icon  , boolean iconEval, int iconWidth, int iconHeight, int iconOffsetX, int iconOffsetY, 
        String shadow, int shadowWidth, int shadowHeight,
        String imageURL) 
    {
        
        /* name */
        this.name         = StringTools.trim(name);

        /* icon image */
        this.iconURL      = StringTools.trim(icon);
        this.iconEval     = iconEval;
        this.iconWidth    = iconWidth;
        this.iconHeight   = iconHeight;
        this.iconOffsetX  = iconOffsetX;
        this.iconOffsetY  = iconOffsetY;
        
        /* icon shadow */
        this.shadowURL    = shadow;
        this.shadowWidth  = shadowWidth;
        this.shadowHeight = shadowHeight;
        
        /* image representation */
        this.imageURL     = StringTools.trim(imageURL);
        
    }

    /**
    *** Constructor 
    *** @param name         This PushPin name
    *** @param icon         The icon image URL, for Javascript command which returns the icon URL
    *** @param iconEval     "false" indicates that 'icon' specifies a fixed URL, "true" indicates that 'icon'
    ***                     is a Javascript line that when evaluated, will return the icon URL.
    *** @param iconSize     2 element array containing the icon image width/height (in pixels)
    *** @param iconOffset   2 element array specifying the icon image X/Y 'hotspot' (in pixels)
    *** @param shadow       The icon shadow image url
    *** @param shadowSize   2 element array containing the icon shadow width/height (in pixels)
    *** @param imageURL     The image used to represent the icon for user selection
    **/
    public PushpinIcon(
        String name  ,
        String icon  , boolean iconEval, int iconSize[], int iconOffset[],
        String shadow, int shadowSize[],
        String imageURL) 
    {
        this(name ,
            icon  , iconEval, iconSize[0],iconSize[1], iconOffset[0],iconOffset[1],
            shadow, shadowSize[0],shadowSize[1],
            imageURL);
    }

    /**
    *** Constructor 
    *** @param name         This PushPin name
    *** @param icon         The icon image URL
    *** @param iconSize     2 element array containing the icon image width/height (in pixels)
    *** @param iconOffset   2 element array specifying the icon image X/Y 'hotspot' (in pixels)
    *** @param shadow       The icon shadow image url
    *** @param shadowSize   2 element array containing the icon shadow width/height (in pixels)
    *** @param imageURL     The image used to represent the icon for user selection
    **/
    public PushpinIcon(
        String name  ,
        String icon  , int iconSize[], int iconOffset[],
        String shadow, int shadowSize[],
        String imageURL) 
    {
        this(name ,
            icon  , false, iconSize[0],iconSize[1], iconOffset[0],iconOffset[1],
            shadow, shadowSize[0],shadowSize[1],
            imageURL);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the name of this PushPin
    *** @return The name of this PushPin
    **/
    public String getName()
    {
        return this.name;
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon image URL
    *** @return The icon image url
    **/
    public String getIconURL()
    {
        return this.iconURL;
    }

    /**
    *** Gets the Pushpin Icon image URL
    *** @param keyValMap  The StringTools.KeyValueMap interface for replacing any 'keys' found in the URL
    *** @return The icon image url
    **/
    /* not currently used
    public String getIconURL(StringTools.KeyValueMap keyValMap)
    {
        // This method allows handling pushpin URLs of the form:
        //  - http://pp.example.com/image/pushpin.png?heading=${heading}
        //  - http://pp.example.com/image/pushpin.png?R=${R}&G=${G}&B=${B}
        //  - http://pp.example.com/image/pushpin.png?color=${color}
        //  - http://pp.example.com/image/pushpin.png?index=${index}
        //  - etc.
        return StringTools.replaceKeys(this.iconURL, keyValMap);
    }
    */
    
    // ------------------------------------------------------------------------

    /**
    *** Returns true if the icon URL should be run through "eval(...)"
    *** @return True if the icon URL should be run through "eval(...)"
    **/
    public boolean getIconEval()
    {
        return this.iconEval;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon image width (in pixels)
    *** @return The icon image width
    **/
    public int getIconWidth() 
    {
        return this.iconWidth;
    }
    
    /**
    *** Gets the Pushpin Icon image height (in pixels)
    *** @return The icon image height
    **/
    public int getIconHeight() 
    {
        return this.iconHeight;
    }
    
    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon image 'hotspot' X-offset (in pixels)
    *** @return The icon image 'hotspot' X-offset
    **/
    public int getIconOffsetX() 
    {
        return this.iconOffsetX;
    }
    
    /**
    *** Gets the Pushpin Icon image 'hotspot' Y-offset (in pixels)
    *** @return The icon image 'hotspot' Y-offset
    **/
    public int getIconOffsetY() 
    {
        return this.iconOffsetY;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon shadow image URL
    *** @return The icon shadow image url
    **/
    public String getShadowURL() 
    {
        return this.shadowURL;
    }

    // ------------------------------------------------------------------------

    /**
    *** Gets the Pushpin Icon shadow image width (in pixels)
    *** @return The icon shadow image width
    **/
    public int getShadowWidth() 
    {
        return this.shadowWidth;
    }
    
    /**
    *** Gets the Pushpin Icon shadow image height (in pixels)
    *** @return The icon shadow image height
    **/
    public int getShadowHeight() 
    {
        return this.shadowHeight;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Gets the representation image URL for this Pushpin Icon for user selection
    *** @return The representation image url
    **/
    public String getImageURL()
    {
        if (!StringTools.isBlank(this.imageURL)) {
            return this.imageURL;
        } else
        if (!this.getIconEval()) {
            return this.getIconURL();
        } else {
            return "";
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the String representation of this Pushpin object
    *** @return The String representation of this Pushpin object
    **/
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Icon:'"  ).append(this.getIconURL()).append("', ");
        sb.append("IconSize:[").append(this.getIconWidth()).append(",").append(this.getIconHeight()).append("], ");
        sb.append("IconOffset:[").append(this.getIconOffsetX()).append(",").append(this.getIconOffsetY()).append("], ");
        sb.append("Shadow:'").append(this.getShadowURL()).append("', ");
        sb.append("ShadowSize:[").append(this.getShadowWidth()).append(",").append(this.getShadowHeight()).append("]");
        return sb.toString();
    }
    
}
