//
//  KlimtSplash.java
//  Klimt
//
//  Created by Simon Urbanek on Wed Jul 30 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

package org.rosuda.klimt;

import org.rosuda.ibase.*;
import org.rosuda.ibase.toolkit.*;
import org.rosuda.util.*;

public class KlimtSplash extends SplashScreen {
    public KlimtSplash() {
        super("Klimt v"+Common.Version+" (release "+Common.Release+")");
    }

    /* we need to re-define this since we're KlimtSplash */
    public static void runMainAsAbout() {
        if (main==null) main=new KlimtSplash();
        main.runAsAbout();
    }
    
    public Object run(Object o, String cmd) {
        super.run(o,cmd); // let SplashScreen handle the defaults
        String openFn=null;
        
        if (cmd=="prefs") {
            Platform.getPlatform().handlePrefs();
        }

        if (cmd.startsWith("recent:")) {
            openFn=cmd.substring(7);
            cmd="openData";
        }
        
        if (cmd=="openData") {
            SVarSet tvs=new SVarSet();
            DataRoot dr=Klimt.addData(tvs);
            SNode t=Klimt.openTreeFile(this,openFn,dr);
            if (t==null && tvs.count()<1) {
            } else {
                if (t!=null) {
                    TFrame f=new TFrame("Tree "+tvs.getName(),TFrame.clsTree);
                    Klimt.newTreeDisplay(t,f,0,0,Common.screenRes.width-160,(Common.screenRes.height>600)?600:Common.screenRes.height-20);
                }
                VarFrame vf=Klimt.newVarDisplay(dr,Common.screenRes.width-150,0,140,(Common.screenRes.height>600)?600:Common.screenRes.height-30);
                setVisible(false);
            }
        }

        return null;
    }
    
}
