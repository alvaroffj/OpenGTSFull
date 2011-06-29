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
//  2007/09/16  Martin D. Flynn
//     -Extracted from 'PrivateLabel.java'.
//     -General (ie not-WAR specific) properties moved to 'org.opengts.db.BasicLabelLoader'
//  2008/04/11  Martin D. Flynn
//     -Added 'Property' tag parse to parent 'MapProvider' tag
//  2008/07/27  Martin D. Flynn
//     -Added 'Pushpins' tag parsing to parent 'MapProvider' tag
//  2008/08/15  Martin D. Flynn
//     -Added a warning if a pushpin Javascript evaluated icon reference may instead be an 
//      image reference.
//     -Added checks for invalid 'aclName' references.
//  2009/08/23  Martin D. Flynn
//     -Added MapProvider "Legend" and "IconSelector" tags.
//     -Ignore certain tags if not loading within a 'track.war' environment.
//  2009/09/23  Martin D. Flynn
//     -Added "NavigationTab"/"Property" sub-tag to "Page"
//  2009/10/02  Martin D. Flynn
//     -Fixed Legend decoding (always decode to HTML, not back to XML).
// ----------------------------------------------------------------------------
package org.opengts.war.tools; // see also BasicPrivateLabelLoader.CLASS_PrivateLabelLoader

import java.util.*;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.Color;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import org.opengts.util.*;
import org.opengts.db.*;
import org.opengts.db.tables.*;
import org.opengts.geocoder.*;

import org.opengts.war.report.ReportException;
import org.opengts.war.report.ReportFactory;
import org.opengts.war.report.ReportEntry;
import org.opengts.war.report.ReportOption;

public class PrivateLabelLoader
    extends BasicPrivateLabelLoader
{
    
    // ------------------------------------------------------------------------

    public PrivateLabelLoader()
    {
        super();
    }
    
    protected BasicPrivateLabel createPrivateLabel(File xmlFile, String hostName)
    {
        return new PrivateLabel(hostName);
    }

    // ------------------------------------------------------------------------

    /* override to automatically reload 'reports.xml' as well */
    protected int _resetLoadDefaultXML()
    {
        int count = 0;
        if (isTrackServlet()) {
            ReportFactory.loadReportDefinitionXML();
            count = super._resetLoadDefaultXML();
            if (ReportFactory.hasParsingErrors()) {
                this._setHasParsingErrors(null);
            }
        } else {
            count = super._resetLoadDefaultXML();
        }
        return count;
    }

    // ------------------------------------------------------------------------

    /* parse TAG_MapProvider tag ('isTrackServlet' only) */
    protected void parseTag_MapProvider(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element mapProvElem,
        OrderedMap<String,Object> dftPushpinMap,
        OrderedMap<String,String> dftLegend)
    {
        PrivateLabel pl = (PrivateLabel)bpl;

        /* name */
        String mapName = XMLTools.getAttribute(mapProvElem, ATTR_name, null, false);
        if (StringTools.isBlank(mapName)) {
            printError("MapProvider 'name' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* active? */
        String active = XMLTools.getAttribute(mapProvElem, ATTR_active, null, true);
        if (!this._isAttributeActive(active,mapName)) {
            // inactive, ignore
            Print.logDebug("Ignoring inactive MapProvider: " + mapName);
            return;
        }

        /* MapProvider class name */
        String mapClass = XMLTools.getAttribute(mapProvElem, ATTR_class, null, false);
        if (StringTools.isBlank(mapClass)) {
            printError("MapProvider 'class' not specified.");
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* attributes */
        String mapKey = XMLTools.getAttribute(mapProvElem, ATTR_key, null, true);
        String keyPrefix = XMLTools.getAttribute(mapProvElem, ATTR_rtPropPrefix, null, true);

        /* create instance of MapProvider */
        MapProvider mp = null;
        MapProviderAdapter mpa = null;
        try {
            Class providerClass = Class.forName(mapClass);  // ClassNotFoundException
            MethodAction ma = new MethodAction(mapClass, null, String.class, String.class);
            mp = (MapProvider)ma.invoke(mapName, mapKey);
            if (pl.getMapProviderCount() > 0) {
                MapProvider firstMP = pl.getMapProvider();
                printError("More than one MapProvider defined: " + firstMP.getName() + ", " + mapName);
                this._setHasParsingErrors(xmlFile);
            }
            Print.logDebug("Adding MapProvider: " + mapName);
            pl.addMapProvider(mp);
            if (mp instanceof MapProviderAdapter) {
                mpa = (MapProviderAdapter)mp;
            }
        } catch (ClassNotFoundException cnfe) {
            printError("MapProvider class not found: " + mapClass);
            this._setHasParsingErrors(xmlFile);
            return;
        } catch (Throwable t) { // ClassNotFoundException, ClassCastException, etc.
            printError("MapProvider creation error: " + mapClass + " [" + t);
            this._setHasParsingErrors(xmlFile);
            return;
        }

        /* default pushpins */
        if (!ListTools.isEmpty(dftPushpinMap)) {
            OrderedMap<String,PushpinIcon> pushpinMap = mp.getPushpinIconMap(null); // TODO: null
            for (String ppKey : dftPushpinMap.keySet()) {
                PushpinIcon ppi = (PushpinIcon)dftPushpinMap.get(ppKey);
                pushpinMap.put(ppKey, ppi);
            }
        }

        /* parse sub-nodes */
        NodeList attrList = mapProvElem.getChildNodes();
        for (int c = 0; c < attrList.getLength(); c++) {

            /* get Node (only interested in 'Element's) */
            Node attrNode = attrList.item(c);
            if (!(attrNode instanceof Element)) {
                continue;
            }
                
            /* parse node */
            String attrName = attrNode.getNodeName();
            Element attrElem = (Element)attrNode;
            if (attrName.equalsIgnoreCase(TAG_Property)) {
                String key   = XMLTools.getAttribute(attrElem, ATTR_key, null, false);
                String rtKey = StringTools.blankDefault(XMLTools.getAttribute(attrElem,ATTR_rtKey,null,false),key);
                if (!StringTools.isBlank(key)) {
                    String val = XMLTools.getNodeText(attrElem, "\n", true);
                    if (!StringTools.isBlank(keyPrefix)) {
                        String v = RTConfig.getString(keyPrefix + rtKey, null);
                        if (v != null) {
                            val = v;
                        }
                        //Print.logInfo("[%s] Property '%s' ==> '%s'", mapName, key, val);
                    }
                    mp.getProperties().setProperty(key, val);
                } else {
                    printWarn("Undefined property key ignored.");
                    this._setHasParsingWarnings(xmlFile);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_Pushpins)) {
                OrderedMap<String,Object> ppMap = this.parseTAG_Pushpins(xmlFile, pl, attrElem, dftPushpinMap);
                if (ppMap != null) {
                    OrderedMap<String,PushpinIcon> pushpinMap = mp.getPushpinIconMap(null); // TODO: null
                    for (String ppKey : ppMap.keySet()) {
                        PushpinIcon ppi = (PushpinIcon)ppMap.get(ppKey);
                        pushpinMap.put(ppKey, ppi);
                    }
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_IconSelector)) {
                String rfName = XMLTools.getAttribute(attrElem,ATTR_ruleFactoryName,"",false);
                RuleFactory rf = Device.getRuleFactory();
                if (rf == null) {
                    // ignore
                    // No Device RuleFactory installed
                } else
                if (StringTools.isBlank(rfName) || rfName.equalsIgnoreCase(rf.getName())) {
                    boolean isFleet = XMLTools.getAttribute(attrElem,ATTR_type,"",false).equalsIgnoreCase("fleet");
                    String  iconSel = XMLTools.getNodeText(attrElem," ",false).trim();
                    if (!rf.checkSelectorSyntax(iconSel)) {
                        Print.logError("["+xmlFile+"] Invalid IconSelector syntax: " + iconSel + " [" + rf.getName() + "]");
                        this._setHasParsingErrors(xmlFile);
                    } else
                    if (mpa != null) {
                        mpa.setIconSelector(isFleet, iconSel);
                    } else {
                        String key = isFleet? MapProvider.PROP_iconSel_fleet[0] : MapProvider.PROP_iconSelector[0];
                        mp.getProperties().setProperty(key, iconSel);
                        //Print.logInfo("[%s] IconSelector[%s] '%s' ==>\n %s", mapName, type, val);
                    }
                } else {
                    // ignore
                    // Installed Device RuleFactory does not match
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_Legend)) {
                //Print.logInfo("Legend: \n" + XMLTools.nodeToString(attrElem));
                String  legType    = XMLTools.getAttribute(attrElem, ATTR_type, "", false);
                boolean isFleet    = legType.equalsIgnoreCase("fleet");
                String  legend     = StringTools.replace(XMLTools.getNodeText(attrElem,"\n",true),"\\n","\n").trim();
                boolean useDefault = XMLTools.getAttributeBoolean(attrElem, ATTR_includeDefault, false, false);
                if (useDefault && (dftLegend != null) && dftLegend.containsKey(legType)) {
                    legend = dftLegend.get(legType);
                    //Print.logInfo("Default Legend: \n" + legend);
                } else
                if (StringTools.isBlank(legend) && (mpa != null)) {
                    // MapProviderAdapter will continue to parse the contents of the "<Legend>" tag
                    //  TAG_Title
                    //  TAG_Icon
                    //  ...
                    String refName = xmlFile.getName() + ":" + mpa.getName();
                    Locale locale  = bpl.getLocale();
                    OrderedMap<String,PushpinIcon> pushpinMap = mpa.getPushpinIconMap(null/*RequestProperties*/);
                    legend = MapProviderAdapter.GetIconLegendHtml(refName, locale, pushpinMap, legType, attrElem, true);
                    //legend = mpa._getIconLegendHtml(xmlFile, attrElem, null/*RequestProperties*/, true); //false);
                    //Print.logInfo("Legend: \n" + legend);
                }
                if (mpa != null) {
                    mpa.setIconSelectorLegend(isFleet, legend);
                } else {
                    String key = isFleet? MapProvider.PROP_iconSel_fleet_legend[0] : MapProvider.PROP_iconSelector_legend[0];
                    mp.getProperties().setProperty(key, legend);
                    //Print.logInfo("[%s] Legend[%s] '%s' ==>\n %s", mapName, type, val);
                }
            } else
            if (attrName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, attrElem);
            } else {
                // unrecognized tag
            }

        }

    }
    
    /**
    *** Parse HTML Legend
    **/
    @SuppressWarnings("unchecked")
    protected String parseLegendHTML(String refName, Locale locale, 
        OrderedMap pushpins, 
        String legendType, Element legendElem)
    {
        OrderedMap<String,PushpinIcon> pushpinMap = (OrderedMap<String,PushpinIcon>)pushpins;
        return MapProviderAdapter.GetIconLegendHtml(refName, locale, pushpinMap, legendType, legendElem, true);
    }

    /**
    *** Parse the specified dimension String, specified as "Width,Height" and return the parsed
    *** dimension in a 2-element 'int' array.
    *** @param dim  The dimension String in the format "Width,Height".
    *** @return The 2-element 'int' array containing the parse dimension values.
    **/
    private int[] _parseDim(String dim)
    {
        String d[] = StringTools.split(dim, ',');
        return new int[] {
            ((d.length > 0)? StringTools.parseInt(d[0],0) : 0),
            ((d.length > 1)? StringTools.parseInt(d[1],0) : 0)
        };
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Parse 'PushPins' tag.
    *** @param xmlFile       The current XML file being parsed
    *** @param pl            The BasicPrivateLabel instance for the current 'Domain'
    *** @param attrElem      The 'MapProvider' node
    *** @param dftPushpinMap The default pushpin map
    *** @return The default pushpin map updated with the pushpins contained within this parsed tag sectoin
    **/
    protected OrderedMap<String,Object> parseTAG_Pushpins(File xmlFile, BasicPrivateLabel pl, Element attrElem,
        OrderedMap<String,Object> dftPushpinMap)
    {
        String baseURL = XMLTools.getAttribute(attrElem, ATTR_baseURL, "", false);
        OrderedMap<String,Object> pushpinMap = new OrderedMap<String,Object>();
        
        /* include default pushpins */
        boolean inclDefault = XMLTools.getAttributeBoolean(attrElem, ATTR_includeDefault, false, false);
        if (inclDefault) {
            if (!ListTools.isEmpty(dftPushpinMap)) {
                pushpinMap.putAll(dftPushpinMap);
            } else {
                printWarn("Pushpins 'includeDefault' specified, but no default pushpins defined!");
                this._setHasParsingWarnings(xmlFile);
            }
        }

        /* parse Pushpin tags */
        NodeList iconList = attrElem.getChildNodes();
        for (int i = 0; i < iconList.getLength(); i++) {
            Node iconNode = iconList.item(i);
            if (!(iconNode instanceof Element)) {
                continue;
            }
            String iconTagName = iconNode.getNodeName();
            Element iconElem = (Element)iconNode;
            if (iconTagName.equalsIgnoreCase(TAG_Pushpin)) {
                String key = XMLTools.getAttribute(iconElem, ATTR_key, null, false);
                if (!StringTools.isBlank(key)) {
                    // icon URL
                    boolean iconEval = false;
                    String  iconJS   = XMLTools.getAttribute(iconElem, ATTR_eval , "", false).trim();
                    String  iconURL  = XMLTools.getAttribute(iconElem, ATTR_icon , "", false).trim();
                    String  alias    = XMLTools.getAttribute(iconElem, ATTR_alias, "", false).trim();
                    if (!alias.equals("")) {
                        // define another alias name for a previously defined color
                        if (!iconJS.equals("") || !iconURL.equals("")) {
                            printWarn("Pushpin 'evel'/'icon' may not be specified with 'alias'");
                            this._setHasParsingWarnings(xmlFile);
                        } else {
                            PushpinIcon ppi = (PushpinIcon)pushpinMap.get(alias);
                            if ((ppi == null) && !inclDefault && !ListTools.isEmpty(dftPushpinMap)) {
                                // allow reading 'alias'ed pushpin from default pushpin map
                                //ppi = (PushpinIcon)dftPushpinMap.get(alias);
                            }
                            if (ppi != null) {
                                pushpinMap.put(key, ppi);
                            } else {
                                printWarn("Pushpin icon 'alias' not defined: key=%s, alias=%s", key, alias);
                                this._setHasParsingWarnings(xmlFile);
                            }
                        }
                    } else {
                        if (!iconJS.equals("") && !iconURL.equals("")) {
                            printWarn("Pushpin 'eval' and 'icon' are mutally exclusive, Pushpin ignored");
                            this._setHasParsingWarnings(xmlFile);
                            iconURL = "";
                        } else
                        if (iconJS.equalsIgnoreCase("DELETE") || iconURL.equalsIgnoreCase("DELETE")) {
                            pushpinMap.remove(key);
                            iconURL = "";
                        } else
                        if (!iconJS.equals("")) {
                            // URL is to be evaluated via Javascript "eval(...)"
                            iconEval = true;
                            iconURL  = iconJS;
                            if (StringTools.endsWithIgnoreCase(iconURL,new String[]{".png",".gif",".jpg",".jpeg"})) {
                                printWarn("JavaScript evaluated String may be an image file reference: " + iconURL);
                                this._setHasParsingWarnings(xmlFile);
                            }
                        } else
                        if (!iconURL.equals("")) {
                            // URL is a static reference
                            iconEval = false;
                            iconURL  = baseURL + iconURL;
                        } else {
                            printWarn("One of Pushpin 'eval' or 'icon' must be specified");
                            this._setHasParsingWarnings(xmlFile);
                            iconURL = "";
                        }
                        if (!iconURL.equals("")) {
                            int iconSize[]   = this._parseDim(XMLTools.getAttribute(iconElem,ATTR_iconSize  ,"12,20",false));
                            int iconOffset[] = this._parseDim(XMLTools.getAttribute(iconElem,ATTR_iconOffset,"-1,-1",false));
                            if (iconOffset[0] < 0) { iconOffset[0] = iconSize[0] / 2; }
                            if (iconOffset[1] < 0) { iconOffset[1] = iconSize[1]; }
                            // image URL
                            String imageURL = XMLTools.getAttribute(iconElem,ATTR_image,"",false).trim();
                            if (!StringTools.isBlank(imageURL)) {
                                imageURL = baseURL + imageURL;
                            }
                            // icon shadow
                            String shadowURL = XMLTools.getAttribute(iconElem,ATTR_shadow,"",false).trim();
                            if (!StringTools.isBlank(shadowURL)) { shadowURL = baseURL + shadowURL; }
                            int shadowSize[] = this._parseDim(XMLTools.getAttribute(iconElem,ATTR_shadowSize,"22,20",false));
                            // Pushpin
                            PushpinIcon ppi = new PushpinIcon(
                                key,
                                iconURL  , iconEval, iconSize, iconOffset, 
                                shadowURL, shadowSize,
                                imageURL);
                            //printInfo("PushPin - " + ppi);
                            pushpinMap.put(key, ppi);
                        }
                    }
                } else {
                    printWarn("Pushpin missing 'key' attribute");
                    this._setHasParsingWarnings(xmlFile);
                }
            } else
            if (iconTagName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, iconElem);
            } else {
                // unrecognized tag
            }
        }
        return pushpinMap;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse the TAG_JSPEntries element ('isTrackServlet' only) */
    protected void parseTag_JSPEntries(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element jspFiles)
    {
        String keyPrefix = XMLTools.getAttribute(jspFiles, ATTR_rtPropPrefix, null, true);
        Map<String,PrivateLabel.JSPEntry> jspMap = new HashMap<String,PrivateLabel.JSPEntry>();
        PrivateLabel pl  = (PrivateLabel)bpl;
        boolean foundDefault = false;
        
        /* parse JSP nodes */
        NodeList jspList = jspFiles.getChildNodes();
        for (int j = 0; j < jspList.getLength(); j++) {

            /* get Node (only interested in 'Element's) */
            Node jspNode = jspList.item(j);
            if (!(jspNode instanceof Element)) {
                continue;
            }
            Element jspElem     = (Element)jspNode;
            String  jspElemName = jspElem.getNodeName();
            
            /* parse "JSP" */
            if (jspElemName.equalsIgnoreCase(TAG_JSP)) {
                String name  = XMLTools.getAttribute(jspElem,ATTR_name ,null,false);
                String rtKey = StringTools.blankDefault(XMLTools.getAttribute(jspElem,ATTR_rtKey,null,false),name);
                String file  = XMLTools.getAttribute(jspElem,ATTR_file ,null,false);
                if (StringTools.isBlank(name)) {
                    // name not specified
                    Print.logError("["+xmlFile+"] Blank JSP Entry name");
                    this._setHasParsingErrors(xmlFile);
                } else {
                    // check property override
                    if (!StringTools.isBlank(keyPrefix)) {
                        String v = RTConfig.getString(keyPrefix + rtKey, null);
                        if (v != null) {
                            //Print.logInfo("Found override ["+name+"] " + keyPrefix + rtKey + " ==> " + v);
                            file = v;
                        }
                    }
                    // save JSP entry
                    if (StringTools.isBlank(file)) {
                        Print.logError("["+xmlFile+"] Blank JSP Entry file: " + name);
                        this._setHasParsingErrors(xmlFile);
                    } else {
                        PrivateLabel.JSPEntry jsp = new PrivateLabel.JSPEntry(name, file);
                        jspMap.put(name, jsp);
                        if (!foundDefault && name.equals(PrivateLabel.JSPENTRY_DEFAULT)) {
                            foundDefault = true;
                        }
                    }
                }
            } else
            if (jspElemName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, jspElem);
            } else {
                // unrecognized tag
            }

        }
        pl.setJSPMap(jspMap);
        
        /* default found? */
        if (!foundDefault) {
            Print.logError("["+xmlFile+"] \""+PrivateLabel.JSPENTRY_DEFAULT+"\" JSP page not found.");
            this._setHasParsingErrors(xmlFile);
        }

    }

    // ------------------------------------------------------------------------

    /* parse the TAG_WebPages element ('isTrackServlet' only) */
    protected void parseTag_WebPages(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element webPages)
    {
        Map<String,WebPage> pageMap         = new OrderedMap<String,WebPage>();
        Map<String,MenuGroup> menuGroupMap  = new OrderedMap<String,MenuGroup>();
        PrivateLabel pl       = (PrivateLabel)bpl;
        String   dftJSP       = XMLTools.getAttribute(webPages,ATTR_jsp         ,null,false);
        String   dftCssDir    = XMLTools.getAttribute(webPages,ATTR_cssDir      ,null,false);
        String   dftIconDir   = XMLTools.getAttribute(webPages,ATTR_iconDir     ,null,false);
        String   dftButtonDir = XMLTools.getAttribute(webPages,ATTR_buttonDir   ,null,false);
        String   keyPrefix    = XMLTools.getAttribute(webPages,ATTR_rtPropPrefix,null,true);
        if (!StringTools.isBlank(dftJSP)   ) { pl.setWebPageJSP(dftJSP); }
        if (!StringTools.isBlank(dftCssDir)) { pl.setCssDirectory(dftCssDir); }
        this._parseTag_WebPages(xmlFile, i18nPkgName, pl, 
            dftJSP, dftCssDir, dftIconDir, dftButtonDir, keyPrefix,
            webPages, pageMap, 
            null, menuGroupMap);
        pl.setWebPageMap(pageMap);
        pl.setMenuGroupMap(menuGroupMap);
    }
    
    /* parse TAG_WebPages/TAG_MenuGroup element */
    private void _parseTag_WebPages(File xmlFile, String i18nPkgName, PrivateLabel pl, 
        String dftJSP, String dftCssDir, String dftIconDir, String dftButtonDir, String keyPrefix,
        Element webPages, // may be a "MenuGroup" tag
        Map<String,WebPage> pageMap, MenuGroup menuGroup, Map<String,MenuGroup> menuGroupMap)
    {
        NodeList webPageList = webPages.getChildNodes();
        for (int p = 0; p < webPageList.getLength(); p++) {

            /* get Node (only interested in 'Element's) */
            Node wpNode = webPageList.item(p);
            if (!(wpNode instanceof Element)) {
                continue;
            }
            Element wpElem     = (Element)wpNode;
            String  wpElemName = wpElem.getNodeName();
            
            /* parse "MenuGroup" */
            if (wpElemName.equalsIgnoreCase(TAG_MenuGroup)) {
                if (menuGroup == null) {
                    String mgName   = XMLTools.getAttribute(wpElem,ATTR_name,null,false);
                    boolean menuBar = XMLTools.getAttributeBoolean(wpElem,ATTR_menuBar,true,false);
                    boolean topMenu = XMLTools.getAttributeBoolean(wpElem,ATTR_topMenu,true,false);
                    MenuGroup mg    = new MenuGroup(mgName);
                    mg.setShowInMenuBar(menuBar);
                    mg.setShowInTopMenu(topMenu);
                    menuGroupMap.put(mgName, mg);
                    this._parseTag_WebPages(xmlFile, i18nPkgName, pl, 
                        dftJSP, dftCssDir, dftIconDir, dftButtonDir, keyPrefix,
                        wpElem, pageMap, 
                        mg, menuGroupMap);
                } else {
                    printError("Recursive 'MenuGroup' tags not allowed");
                    this._setHasParsingErrors(xmlFile);
                }
                continue;
            }

            /* parse "Title" */
            if (wpElemName.equalsIgnoreCase(TAG_Title)) {
                if (menuGroup != null) {
                    String i18nKey  = XMLTools.getAttribute(wpElem,ATTR_i18n,null,false);
                    String titleDft = XMLTools.getNodeText(wpElem,"\\n",false);
                    menuGroup.setTitle(BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,titleDft));
                } else {
                    printError("'MenuGroup' not defined");
                    this._setHasParsingErrors(xmlFile);
                }
                continue; //
            }

            /* parse "Description" */
            if (wpElemName.equalsIgnoreCase(TAG_Description)) {
                if (menuGroup != null) {
                    String i18nKey = XMLTools.getAttribute(wpElem,ATTR_i18n,null,false);
                    String descDft = XMLTools.getNodeText(wpElem,"\\n",false);
                    menuGroup.setDescription(BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,descDft));
                } else {
                    printError("'MenuGroup' not defined");
                    this._setHasParsingErrors(xmlFile);
                }
                continue; //
            }

            /* parse "Page" */
            if (wpElemName.equalsIgnoreCase(TAG_Page)) {
                Element page      = wpElem;
                String  pageName  = XMLTools.getAttribute(page,ATTR_name,null,false);
                String  rtKey     = StringTools.blankDefault(XMLTools.getAttribute(page,ATTR_rtKey,null,false),pageName);
                if (!StringTools.isBlank(keyPrefix)) {
                    String pk = keyPrefix + rtKey;
                    if (!RTConfig.getBoolean(pk,true)) {
                        Print.logDebug("Ignoring Page per property: " + pk);
                        continue;
                    }
                }
                // other attributes
                String  pageAlias = XMLTools.getAttribute(page,ATTR_alias      ,null,false);
                String  classname = XMLTools.getAttribute(page,ATTR_class      ,null,false);
                boolean optional  = XMLTools.getAttributeBoolean(page,ATTR_optional,false,false);
                String  aclName   = XMLTools.getAttribute(page,ATTR_aclName    ,null,false);
                String  jspURI    = XMLTools.getAttribute(page,ATTR_jsp        ,null,false);
                String  cssDir    = XMLTools.getAttribute(page,ATTR_cssDir     ,null,false);
                String  iconURI   = XMLTools.getAttribute(page,ATTR_icon       ,null,false);
                String  buttonURI = XMLTools.getAttribute(page,ATTR_button     ,null,false);
                String  buttonALT = XMLTools.getAttribute(page,ATTR_altButton  ,null,false);
                // I18N Strings
                I18N.Text navDesc  = null;
                I18N.Text navTab   = null;
                I18N.Text menuDesc = null;
                I18N.Text menuHelp = null;
                // Runtime properties
                RTProperties rtProps = null;
                // child nodes 
                NodeList chList = page.getChildNodes();
                for (int c = 0; c < chList.getLength(); c++) {
                    Node chNode = chList.item(c);
                    if (!(chNode instanceof Element)) { continue; }
                    String childName = chNode.getNodeName();
                    Element childElem = (Element)chNode;
                    if (childName.equalsIgnoreCase(TAG_NavigationDescription)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        navDesc = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_NavigationTab)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        navTab = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_MenuDescription)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        menuDesc = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_MenuHelp)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        menuHelp = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_IconImage)) {
                        iconURI = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_ButtonImage)) {
                        buttonURI = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_ButtonImageAlt)) {
                        buttonALT = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_AclName)) {
                        aclName = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_Property)) {
                        String key = XMLTools.getAttribute(childElem,ATTR_key,null,false);
                        if (!StringTools.isBlank(key)) {
                            if (rtProps == null) { rtProps = new RTProperties(); }
                            String val = XMLTools.getNodeText(childElem,"\n",true);
                            rtProps.setProperty(key, val);
                        } else {
                            printWarn("Undefined property key ignored.");
                            this._setHasParsingWarnings(xmlFile);
                        }
                    } else
                    if (childName.equalsIgnoreCase(TAG_LogMessage)) {
                        this.parseTag_LogMessage(xmlFile, pl, childElem);
                    } else {
                        printWarn("Unrecognized tag name: " + childName);
                        this._setHasParsingWarnings(xmlFile);
                    }
                }
                // adjust icon image URL
                if (!StringTools.isBlank(dftIconDir)) {
                    if (!StringTools.isBlank(iconURI)) {
                        if (iconURI.startsWith("/") || dftIconDir.endsWith("/")) {
                            iconURI = dftIconDir + iconURI; 
                        } else {
                            iconURI = dftIconDir + "/" + iconURI; 
                        }
                    }
                }
                // adjust button image URL
                if (!StringTools.isBlank(dftButtonDir)) {
                    if (!StringTools.isBlank(buttonURI)) {
                        if (buttonURI.startsWith("/") || dftButtonDir.endsWith("/")) {
                            buttonURI = dftButtonDir + buttonURI; 
                        } else {
                            buttonURI = dftButtonDir + "/" + buttonURI; 
                        }
                    }
                    if (!StringTools.isBlank(buttonALT)) {
                        if (buttonALT.startsWith("/") || dftButtonDir.endsWith("/")) {
                            buttonALT = dftButtonDir + buttonALT; 
                        } else {
                            buttonALT = dftButtonDir + "/" + buttonALT; 
                        }
                    }
                }
                // make sure ACL exists
                if (!StringTools.isBlank(aclName) && !pl.hasAclEntry(aclName)) {
                    printError("Domain '%s' Page class='%s': Undefined ACL key '%s'", pl.getName(), classname, aclName);
                    this._setHasParsingErrors(xmlFile);
                }
                // create WebPage
                try {
                    Class pageClass = Class.forName(classname);
                    if (WebPage.class.isAssignableFrom(pageClass)) {
                        WebPage wp = (WebPage)pageClass.newInstance();
                        // override page name
                        if (!StringTools.isBlank(pageName)) {
                            // this WebPage may reject this assignment and use it's own name
                            wp.setPageName(pageName); 
                        }
                        // save in menu group
                        if (menuGroup != null) {
                            menuGroup.addWebPage(wp);
                        }
                        // special settings
                        if (wp instanceof WebPageAdaptor) {
                            WebPageAdaptor wpa = (WebPageAdaptor)wp;
                            wpa.setJspURI(!StringTools.isBlank(jspURI)? jspURI : dftJSP);
                            wpa.setCssDirectory(!StringTools.isBlank(cssDir)? cssDir : dftCssDir);
                            wpa.setPrivateLabel(pl);
                            wpa.setMenuGroup(menuGroup);
                            wpa.setAclName(aclName);
                            wpa.setNavigationDescription(navDesc);
                            wpa.setNavigationTab(navTab);
                            wpa.setMenuDescription(menuDesc);
                            wpa.setMenuHelp(menuHelp);
                            wpa.setMenuIconImage(iconURI);
                            wpa.setMenuButtonImage(buttonURI);
                            wpa.setMenuButtonAltImage(buttonALT);
                            wpa.setProperties(rtProps);
                            String subAclList[] = wpa.getChildAclList();
                            if (!ListTools.isEmpty(subAclList)) {
                                if (StringTools.isBlank(aclName)) {
                                    printError("Domain '%s' Page class='%s': ACL key not specified");
                                    this._setHasParsingErrors(xmlFile);
                                } else {
                                    for (String subAcl : subAclList) {
                                        String subName = wpa.getAclName(subAcl);
                                        if (!pl.hasAclEntry(subName)) {
                                            printError("Domain '%s' Page class='%s': Undefined ACL key '%s'", pl.getName(), classname, subName);
                                            this._setHasParsingErrors(xmlFile);
                                        }
                                    }
                                }
                            }
                            wpa.postInit(); // post initialization
                        }
                        if (wp.getIsEnabled()) {
                            String pn = wp.getPageName();
                            pageMap.put(pn, wp);
                            if (!StringTools.isBlank(pageAlias) && !pageAlias.equals(pn)) {
                                pageMap.put(pageAlias, wp);
                            }
                        }
                    } else {
                        printError(classname + " does not implement interface WebPage");
                        this._setHasParsingErrors(xmlFile);
                    }
                } catch (ClassNotFoundException cnfe) {
                    if (optional) {
                        //printInfo("Domain '"+pl.getName()+"', WebPage class not found: "+classname);
                    } else {
                        printError("Domain '"+pl.getName()+"', WebPage class not found: "+classname);
                        this._setHasParsingErrors(xmlFile);
                    }
                } catch (InstantiationException ie) {
                    if (optional) {
                        printWarn("Domain '"+pl.getName()+"', Unable to instantiate WebPage: "+classname);
                        this._setHasParsingWarnings(xmlFile);
                    } else {
                        printError("Domain '"+pl.getName()+"', Unable to instantiate WebPage: "+classname);
                        Print.logException("Domain '"+pl.getName()+"', Unable to instantiate WebPage: "+classname, ie);
                        this._setHasParsingErrors(xmlFile);
                    }
                } catch (NoClassDefFoundError ncde) {
                    // likely reason is that Tomcat libraries were not found
                    String ncdm = ncde.getMessage();
                    if ((ncdm.indexOf("javax.servlet") >= 0) || (ncdm.indexOf("javax/servlet") >= 0)) {
                        printError("Missing Servlet Classes: " + ncdm + " (check 'CATALALINA_HOME')");
                    } else {
                        printError(ncde.toString());
                    }
                    if (optional) {
                        printWarn("Domain '"+pl.getName()+"', Unable to load optional WebPage: "+classname);
                        this._setHasParsingWarnings(xmlFile);
                    } else {
                        printError("Domain '"+pl.getName()+"', Unable to load WebPage: "+classname);
                        this._setHasParsingErrors(xmlFile);
                    }
                } catch (Throwable t) { // NullPointerException, InvocationException
                    if (optional) {
                        printWarn("Domain '"+pl.getName()+"', Unable to load optional WebPage: "+classname);
                        this._setHasParsingWarnings(xmlFile);
                    } else {
                        printError("Domain '"+pl.getName()+"', Unable to load WebPage: "+classname);
                        Print.logException("Domain '"+pl.getName()+"', Unable to load WebPage: "+classname, t);
                        this._setHasParsingErrors(xmlFile);
                    }
                }
                continue;
            }

            /* parse "Link" */
            if (wpElemName.equalsIgnoreCase(TAG_Link)) {
                Element link      = wpElem;
                String  urlLink   = XMLTools.getAttribute(link,ATTR_url        ,null,false);
                String  target    = XMLTools.getAttribute(link,ATTR_target     ,null,false);
                String  aclName   = XMLTools.getAttribute(link,ATTR_aclName    ,null,false);
                String  iconURI   = XMLTools.getAttribute(link,ATTR_icon       ,null,false);
                String  buttonURI = XMLTools.getAttribute(link,ATTR_button     ,null,false);
                String  buttonALT = XMLTools.getAttribute(link,ATTR_altButton  ,null,false);
                // I18N Strings
                I18N.Text navDesc  = null;
                I18N.Text navTab   = null;
                I18N.Text menuDesc = null;
                I18N.Text menuHelp = null;
                // child nodes 
                NodeList chList = link.getChildNodes();
                for (int c = 0; c < chList.getLength(); c++) {
                    Node chNode = chList.item(c);
                    if (!(chNode instanceof Element)) { continue; }
                    String childName = chNode.getNodeName();
                    Element childElem = (Element)chNode;
                    if (childName.equalsIgnoreCase(TAG_NavigationDescription)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        navDesc = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_NavigationTab)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        navTab = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_MenuDescription)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        menuDesc = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_MenuHelp)) {
                        String i18nKey = XMLTools.getAttribute(childElem,ATTR_i18n,null,false);
                        String text    = XMLTools.getNodeText(childElem," ",false);
                        menuHelp = BasicPrivateLabelLoader.parseI18N(xmlFile,i18nPkgName,i18nKey,text,false);
                    } else
                    if (childName.equalsIgnoreCase(TAG_IconImage)) {
                        iconURI = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_ButtonImage)) {
                        buttonURI = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_ButtonImageAlt)) {
                        buttonALT = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_AclName)) {
                        aclName = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                    } else
                    if (childName.equalsIgnoreCase(TAG_LogMessage)) {
                        this.parseTag_LogMessage(xmlFile, pl, childElem);
                    } else {
                        printWarn("Unrecognized tag name: " + childName);
                        this._setHasParsingWarnings(xmlFile);
                    }
                }
                // adjust icon image URL
                if (!StringTools.isBlank(dftIconDir)) {
                    if (!StringTools.isBlank(iconURI)) {
                        if (iconURI.startsWith("/") || dftIconDir.endsWith("/")) {
                            iconURI = dftIconDir + iconURI; 
                        } else {
                            iconURI = dftIconDir + "/" + iconURI; 
                        }
                    }
                }
                // adjust button image URL
                if (!StringTools.isBlank(dftButtonDir)) {
                    if (!StringTools.isBlank(buttonURI)) {
                        if (buttonURI.startsWith("/") || dftButtonDir.endsWith("/")) {
                            buttonURI = dftButtonDir + buttonURI; 
                        } else {
                            buttonURI = dftButtonDir + "/" + buttonURI; 
                        }
                    }
                    if (!StringTools.isBlank(buttonALT)) {
                        if (buttonALT.startsWith("/") || dftButtonDir.endsWith("/")) {
                            buttonALT = dftButtonDir + buttonALT; 
                        } else {
                            buttonALT = dftButtonDir + "/" + buttonALT; 
                        }
                    }
                }
                // make sure ACL exists
                if (!StringTools.isBlank(aclName) && !pl.hasAclEntry(aclName)) {
                    printError("Domain '%s' Link url='%s': Undefined ACL key '%s'", pl.getName(), urlLink, aclName);
                    this._setHasParsingErrors(xmlFile);
                }
                // create WebPageURL
                WebPageURL wpu = new WebPageURL();
                if (menuGroup != null) {
                    menuGroup.addWebPage(wpu);
                }
                wpu.setURL(urlLink);
                wpu.setTarget(target);
                wpu.setPrivateLabel(pl);
                wpu.setMenuGroup(menuGroup);
                wpu.setAclName(aclName);
                wpu.setNavigationDescription(navDesc);
                wpu.setNavigationTab(navTab);
                wpu.setMenuDescription(menuDesc);
                wpu.setMenuHelp(menuHelp);
                wpu.setMenuIconImage(iconURI);
                wpu.setMenuButtonImage(buttonURI);
                wpu.setMenuButtonAltImage(buttonALT);
                if (wpu.getIsEnabled()) {
                    pageMap.put(wpu.getPageName(), wpu);
                }
                continue;
            }
            
            /* Log Message */
            if (wpElemName.equalsIgnoreCase(TAG_LogMessage)) {
                this.parseTag_LogMessage(xmlFile, pl, wpElem);
                continue;
            }

        } // chile nodes
    }

    // ------------------------------------------------------------------------

    /* parse the TAG_Reports element ('isTrackServlet' only) */
    protected void parseTag_Reports(File xmlFile, String i18nPkgName, BasicPrivateLabel bpl, Element reports)
    {
        PrivateLabel pl = (PrivateLabel)bpl;
        String keyPrefix = XMLTools.getAttribute(reports,ATTR_rtPropPrefix,null,true);
        Map<String,ReportEntry> reportMap = new OrderedMap<String,ReportEntry>();
        NodeList reportList = XMLTools.getChildElements(reports,TAG_Report);
        for (int r = 0; r < reportList.getLength(); r++) {
            Element report     = (Element)reportList.item(r);
            String  reportName = XMLTools.getAttribute(report,ATTR_name,null,false);
            String  rtKey      = StringTools.blankDefault(XMLTools.getAttribute(report,ATTR_rtKey,null,false),reportName);
            String  aclName    = XMLTools.getAttribute(report,ATTR_aclName,null,false);
            boolean optional   = XMLTools.getAttributeBoolean(report,ATTR_optional,false,false);
            String  sysAdmin   = XMLTools.getAttribute(report,ATTR_sysAdminOnly,null,false);
            //OrderedMap<String,ReportOption> rptOptMap = null;
            
            // ignore Report?
            if (!StringTools.isBlank(keyPrefix)) {
                String rk = keyPrefix + rtKey;
                if (!RTConfig.getBoolean(rk,true)) {
                    Print.logDebug("Ignoring report per property: " + rk);
                    continue;
                }
            }
            
            // 'Report' child nodes 
            NodeList chList = report.getChildNodes();
            for (int c = 0; c < chList.getLength(); c++) {
                Node chNode = chList.item(c);
                if (!(chNode instanceof Element)) { continue; }
                String childName = chNode.getNodeName();
                Element childElem = (Element)chNode;
                if (childName.equalsIgnoreCase(TAG_AclName)) {
                    aclName = StringTools.trim(XMLTools.getNodeText(childElem,"",false));
                //} else
                //if (childName.equalsIgnoreCase(TAG_Options)) {
                //    NodeList optList = childElem.getChildNodes();
                //    rptOptMap = new OrderedMap<String,ReportOption>();
                //    for (int p = 0; p < optList.getLength(); p++) {
                //        Node optNode = optList.item(p);
                //        if (!(optNode instanceof Element)) { continue; }
                //        String optName = optNode.getNodeName();
                //        Element optElem = (Element)optNode;
                //        if (optName.equalsIgnoreCase(TAG_Select)) {
                //            String selName  = XMLTools.getAttribute(optElem,ATTR_key  ,"",false);
                //            String selValue = XMLTools.getAttribute(optElem,ATTR_value,"",false);
                //            String selDesc  = StringTools.trim(XMLTools.getNodeText(optElem,"",false)); // TODO: i18n?
                //            ReportOption rptOpt = new ReportOption(selName, selValue, selDesc);
                //            rptOptMap.put(selName,rptOpt);
                //        } else
                //        if (optName.equalsIgnoreCase(TAG_LogMessage)) {
                //            this.parseTag_LogMessage(xmlFile, pl, optElem);
                //        } else {
                //            printWarn("Unrecognized tag name: " + optName);
                //            this._setHasParsingWarnings(xmlFile);
                //        }
                //    }
                } else
                if (childName.equalsIgnoreCase(TAG_LogMessage)) {
                    this.parseTag_LogMessage(xmlFile, pl, childElem);
                } else {
                    printWarn("["+xmlFile+"] Unrecognized tag name: " + childName);
                    this._setHasParsingWarnings(xmlFile);
                }
            }

            // make sure ACL exists
            if (!StringTools.isBlank(aclName) && !pl.hasAclEntry(aclName)) {
                printError("["+xmlFile+"] Domain '%s' Report name='%s': Undefined ACL key '%s'", pl.getName(), reportName, aclName);
                this._setHasParsingErrors(xmlFile);
            }

            // create ReportFactory
            try {
                ReportFactory rf = ReportFactory.getReportFactory(reportName,optional); // throws ReportException if name not found
                if (rf != null) {
                    if (StringTools.isBoolean(sysAdmin,true)) {
                        rf.setSysAdminOnly(StringTools.parseBoolean(sysAdmin,true));
                    }
                    rf.getReportClass(); // throws ReportException if class not found
                    ReportEntry re = new ReportEntry(rf, aclName);
                    reportMap.put(reportName, re);
                }
            } catch (ReportException re) {
                printError("["+xmlFile+"] Unable to add report: " + reportName + " [" + re.getMessage() + "]");
                this._setHasParsingErrors(xmlFile);
            } catch (Throwable t) {
                printError("["+xmlFile+"] Unable to add report: " + reportName);
                Print.logException("["+xmlFile+"] Unable to add report: " + reportName, t);
                this._setHasParsingErrors(xmlFile);
            }

        }
        pl.setReportMap(reportMap);
    }

    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------

    /* add a BasicPrivateLabel instance to the host map */
    protected void _addPrivateLabel(File xmlFile, BasicPrivateLabel privLabel, boolean ignoreDuplicates)
    {
        PrivateLabel pl = (PrivateLabel)privLabel;
        if (pl.hasUserPageDecorations()) {
            pl.getUserPageDecorations().setDefaultPageDecorations(pl.getDefaultPageDecorations());
        }
        super._addPrivateLabel(xmlFile, pl, ignoreDuplicates);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* debug: test loading PrivateLabel XML file */
    public static void main(String argv[])
    {
        RTConfig.setCommandLineArgs(argv);
        RTConfig.setDebugMode(true);
        Print.setLogLevel(Print.LOG_ALL);
        Print.setLogHeaderLevel(Print.LOG_ALL);
        PrivateLabelLoader pll = new PrivateLabelLoader();
        File xmlFile = RTConfig.getFile("xml",null);
        if (xmlFile != null) {
            if (isTrackServlet()) {
                ReportFactory.loadReportDefinitionXML();
                pll._resetLoadXML(xmlFile);
                if (ReportFactory.hasParsingErrors()) {
                    pll._setHasParsingErrors(xmlFile);
                }
            } else {
                pll._resetLoadXML(xmlFile);
            }
        } else {
            pll._resetLoadDefaultXML();
        }
    }

}
