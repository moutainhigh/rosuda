/*
 * ClassifierManager.java
 *
 * Created on 25. September 2005, 16:25
 *
 */

package org.rosuda.JClaR;

import java.util.Vector;

/**
 *
 * @author tobias
 */
public class ClassifierManager {
    
    private static Vector classifiers;
    private static DataClassifierListenerIF listener;
    
    /** Creates a new instance of ClassifierManager */
    public ClassifierManager() {
    }
    
    public static Vector getClassifiers(){
        if(classifiers==null) classifiers=new Vector();
        return classifiers;
    }
    
    public static void addClassifier(Classifier newClassifier){
        if(classifiers==null) classifiers=new Vector();
        classifiers.add(newClassifier);
        if(listener!=null) listener.classifiersChanged();
    }
    
    public static void setListener(DataClassifierListenerIF aListener) {
        listener = aListener;
    }
    
}