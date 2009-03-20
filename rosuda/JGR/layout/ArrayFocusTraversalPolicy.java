package org.rosuda.JGR.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;

/**
 * This class allows the focus-traversal order (or tab-order) to be set
  * for a Container in a simple fashion using an array of Components.
  * This class is generated by Cloudgarden's Jigloo GUI builder. It can be
  * used freely without any restriction whatever.
 */
public class ArrayFocusTraversalPolicy extends FocusTraversalPolicy {

    private Component[] list;
    
    public ArrayFocusTraversalPolicy(Component[] list) {
        this.list = list;
    }
    
    public Component getDefaultComponent(Container focusCycleRoot) {
        if(list == null || list.length < 1)
            return null;
        return list[0];
    }

    public Component getFirstComponent(Container focusCycleRoot) {
        if(list == null || list.length < 1)
            return null;
        return list[0];
    }

    public Component getLastComponent(Container focusCycleRoot) {
        if(list == null || list.length < 1)
            return null;
        return list[list.length - 1];
    }

    public Component getComponentAfter(Container focusCycleRoot, Component comp) {
        if(list == null || list.length < 1)
            return null;
        for (int j = 0; j < list.length; j++) {
            if(list[j].equals(comp)) {
                if(j == list.length-1)
                    return list[0];
                return list[j+1];
            }
        }
        return list[0];
    }

    public Component getComponentBefore(Container focusCycleRoot, Component comp) {
        if(list == null || list.length < 1)
            return null;
        for (int j = 0; j < list.length; j++) {
            if(list[j].equals(comp)) {
                if(j == 0)
                    return list[list.length-1];
                return list[j-1];
            }
        }
        return list[0];
    }

    /**
     * @return
     */
    public Component[] getComponentArray() {
        return list;
    }

}