package org.rosuda.JGR.toolkit;

/**
*  JGRPrefs
 *
 * 	preferences like fonts colors ....
 *
 *	@author Markus Helbig
 *
 * 	RoSuDA 2003 - 2004
 */

import java.awt.*;
import java.io.*;
import java.util.prefs.*;
import javax.swing.text.*;

import org.rosuda.JGR.*;

import java.util.prefs.Preferences;

public class JGRPrefs {


    public static final int DEBUG = 0;
    public static boolean isMac = false;

    /** DefaultFontName */
    public static String FontName = "Dialog";
    /** DefaultFontStyle */
    public static int FontStyle = Font.PLAIN;
    /** DefaultFontSize */
    public static int  FontSize = 12;

    public static final int MINFONTSIZE = 18;
    /** DefaultFont */
    public static Font DefaultFont;
    /** DefaultHighLightColor */
    public static Color HighLightColor = Color.green;
    /** DefaultCommandColor */
    public static Color CMDColor = Color.red;
    /** DefaultResultColor */
    public static Color RESULTColor = Color.blue;
    /** DefaultErrorColor */
    public static Color ERRORColor = Color.red;
    /** DefaultBracketHighLightColor */
    public static Color BRACKETHighLight = new Color(200, 255, 255);
    /** DefaultFontSet */
    public static MutableAttributeSet DEFAULTFONT = new SimpleAttributeSet();
    /** DefaultSizeSet */
    public static MutableAttributeSet SIZE = new SimpleAttributeSet();
    /** DefaultCMDSet */
    public static MutableAttributeSet CMD = new SimpleAttributeSet();
    /** DefaultResultSet */
    public static MutableAttributeSet RESULT = new SimpleAttributeSet();
    /** DefaultSet */
    public static MutableAttributeSet NORMAL = new SimpleAttributeSet();
    /** DefaultNumberSet */
    public static MutableAttributeSet NUMBER = new SimpleAttributeSet();
    /** DefaultNumberColor*/
    public static Color NUMBERColor = Color.red;
    /** DefaultKEYWORDSet */
    public static MutableAttributeSet KEYWORD = new SimpleAttributeSet();
    /** DefaultKeyWordColor */
    public static Color KEYWORDColor = new Color(0,0,140);
    /** DefaultKEYWORDOBJECTSet */
    public static MutableAttributeSet OBJECT = new SimpleAttributeSet();
    /** DefaultKeyWordObjectColor */
    public static Color OBJECTColor = new Color(50,0,140);
    /** DefaultCommentSet */
    public static MutableAttributeSet COMMENT = new SimpleAttributeSet();
    /** DefaultCommentColor */
    public static Color COMMENTColor = new Color(0,120,0);
    /** DefaultQuoteSet */
    public static MutableAttributeSet QUOTE = new SimpleAttributeSet();
    /** DefaultQuoteColor */
    public static Color QUOTEColor = Color.blue;


    public static int maxHelpTabs = 10;

    public static boolean useHelpAgent = true;

    public static boolean useEmacsKeyBindings = false;



    public static void apply() {
        JGRPrefs.refresh();
        FontTracker.current.applyFont();
    }

    public static void initialize() {
        readPrefs();
        DefaultFont = new Font(FontName,FontStyle,FontSize);
        StyleConstants.setFontSize(SIZE,FontSize);
        StyleConstants.setFontSize(DEFAULTFONT,FontSize);
        StyleConstants.setFontFamily(DEFAULTFONT,FontName);
        StyleConstants.setForeground(CMD,CMDColor);
        StyleConstants.setForeground(RESULT,RESULTColor);
        StyleConstants.setForeground(NORMAL, Color.black);
        StyleConstants.setFontSize(NORMAL,FontSize);
        StyleConstants.setForeground(NUMBER, NUMBERColor);
        StyleConstants.setForeground(COMMENT, COMMENTColor);
        StyleConstants.setForeground(KEYWORD, KEYWORDColor);
        StyleConstants.setBold(KEYWORD, true);
        StyleConstants.setForeground(OBJECT, OBJECTColor);
        StyleConstants.setItalic(OBJECT, true);
        StyleConstants.setForeground(QUOTE, QUOTEColor);
    }

    public static void refresh() {
        DefaultFont = new Font(FontName,FontStyle,FontSize);
        StyleConstants.setFontSize(SIZE,FontSize);
        StyleConstants.setFontSize(DEFAULTFONT,FontSize);
        StyleConstants.setFontFamily(DEFAULTFONT,FontName);
        StyleConstants.setForeground(CMD,CMDColor);
        StyleConstants.setForeground(RESULT,RESULTColor);
        StyleConstants.setForeground(NORMAL, Color.black);
        StyleConstants.setFontSize(NORMAL,FontSize);
        StyleConstants.setForeground(NUMBER, NUMBERColor);
        StyleConstants.setForeground(COMMENT, COMMENTColor);
        StyleConstants.setForeground(KEYWORD, KEYWORDColor);
        StyleConstants.setBold(KEYWORD, true);
        StyleConstants.setForeground(OBJECT, OBJECTColor);
        StyleConstants.setItalic(OBJECT, true);
        StyleConstants.setForeground(QUOTE, QUOTEColor);
    }

    public static void readPrefs() {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(System.getProperty("user.home")+File.separator+".JGRprefsrc"));
        } catch (FileNotFoundException e) {
        }

        try {
            Preferences.importPreferences(is);
        } catch (InvalidPreferencesFormatException e) {
        } catch (IOException e) {
        }

        Preferences prefs = Preferences.userNodeForPackage(String.class);
        FontName = prefs.get("FontName","Dialog");
        FontSize = prefs.getInt("FontSize",12);
        maxHelpTabs = prefs.getInt("MaxHelpTabs",10);
        useHelpAgent = prefs.getBoolean("UseHelpAgent", true);
        // it is safe to use emacs bindings on Macs since that's the default in Coca widgets. on win/unix it's not safe since ctrl may be the sc modifier
        useEmacsKeyBindings = prefs.getBoolean("UseEmacsKeyBindings", org.rosuda.util.Platform.isMac);
    }

    public static void writePrefs() {
        Preferences prefs = Preferences.userNodeForPackage(String.class);

        prefs.put("FontName", FontName);        // String
        prefs.putInt("FontSize", FontSize);               // int
        prefs.putInt("MaxHelpTabs",maxHelpTabs);
        prefs.putBoolean("UseHelpAgent", useHelpAgent);
        prefs.putBoolean("UseEmacsKeyBindings", useEmacsKeyBindings);
        if (JGRPackageManager.defaultPackages != null && JGRPackageManager.defaultPackages.length > 0) {
            String packages = JGRPackageManager.defaultPackages[JGRPackageManager.defaultPackages.length-1].toString();
            for (int i = JGRPackageManager.defaultPackages.length-2; i >= 0; i--)
                packages += ", "+JGRPackageManager.defaultPackages[i];
            prefs.put("DefaultPackages", packages);
        }
        if (JGR.RLIBS != null && JGR.RLIBS.length > 0) {
            String libpaths = JGR.RLIBS[0].toString();
            for (int i = 1; i < JGR.RLIBS.length; i++) 
				libpaths +=  (isMac?":":";")+JGR.RLIBS[i];
            prefs.put("InitialRLibraryPath", libpaths);
        }
        try {
            prefs.exportNode(new FileOutputStream(System.getProperty("user.home")+File.separator+".JGRprefsrc"));
        } catch (IOException e) {
        } catch (BackingStoreException e) {
        }
    }
}
