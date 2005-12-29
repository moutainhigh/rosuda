//
//  PPrimPolygon.java
//  Klimt
//
//  Created by Simon Urbanek on Wed Oct 29 2003.
//  Copyright (c) 2003 __MyCompanyName__. All rights reserved.
//

package org.rosuda.ibase.toolkit;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import org.rosuda.ibase.*;
import org.rosuda.pograss.*;
import org.rosuda.util.*;

/** Plot primitive based on {@link PPrimBase},  using a list of IDs and implementing polygons.
 * @version $Id$
 */
public class PPrimPolygon extends PPrimBase {
    public Polygon pg;
    
    public boolean drawBorder=true;
    public boolean useSelAlpha=true;
    public boolean closed=true;
    public boolean fill=true;
    public boolean selectByCorners=false;
    public boolean drawCorners=false;
    public float[] lineWidth;
    public boolean[] invisibleLines;
    public boolean showInvisibleLines=false;
    public boolean[] gapDots;
    public boolean[] noDotsAt;
    public boolean showGapDots=true;
    
    private int nodeSize=2;
    
    /** checks whether the PlotPrimitive contains the given point.*/
    public boolean contains(int x, int y) {
        if(pg==null) return false;
        if(selectByCorners){
            for(int i=0; i<pg.npoints; i++)
                if(x==pg.xpoints[i] && y==pg.ypoints[i])
                    return true;
            return false;
        } else return pg.contains(x,y);
    }
    
    /** checks whether the PlotPrimitive intersects (or is contained) in the given rectangle. */
    public boolean intersects(Rectangle rt) {
        if(pg==null) return false;
        if(selectByCorners){
            for(int i=0; i<pg.npoints; i++)
                if(rt.contains(pg.xpoints[i], pg.ypoints[i]))
                    return true;
            return false;
        } else return pg.intersects(rt);
    }
    
    /** paint the primitive */
    public void paint(PoGraSS g, int orientation) {
        if (pg==null) return;
        g.defineColor("red",255,0,0);
        if(fill){
            if (col!=null)
                g.setColor(col.getRed(),col.getGreen(),col.getBlue());
            else
                g.setColor("object");
            g.fillPolygon(pg.xpoints,pg.ypoints,pg.npoints);
        }
        if (drawBorder) {
            g.setColor("outline");
            //g.drawPolygon(pg.xpoints,pg.ypoints,pg.npoints,closed);
            for(int i=1; i<pg.npoints; i++){
                if(!invisibleLines[i-1]){
                    if(lineWidth!=null) g.setLineWidth(lineWidth[i-1]);
                    g.drawLine(pg.xpoints[i-1], pg.ypoints[i-1], pg.xpoints[i], pg.ypoints[i]);
                }
            }
            if(closed){
                if(lineWidth!=null) g.setLineWidth(lineWidth[pg.npoints-1]);
                g.drawLine(pg.xpoints[pg.npoints-1], pg.ypoints[pg.npoints-1], pg.xpoints[0], pg.ypoints[0]);
            }
            if(showInvisibleLines && invisibleLines!=null){
                g.setColor("red");
                for(int i=0; i<invisibleLines.length; i++){
                    if(invisibleLines[i]){
                        g.drawLine(pg.xpoints[i],pg.ypoints[i],pg.xpoints[i+1],pg.ypoints[i+1]);
                    }
                }
            }
        }
        if(drawCorners){
            g.setColor("outline");
            for(int i=0; i<pg.npoints; i++){
                if(noDotsAt==null || !noDotsAt[i])
                    g.fillOval(pg.xpoints[i]-nodeSize, pg.ypoints[i]-nodeSize, 2*nodeSize+1,2*nodeSize+1);
            }
        }
        if(showGapDots && gapDots!=null){
            g.setColor("outline");
            for(int i=0; i<gapDots.length; i++)
                if(gapDots[i])
                    g.fillOval(pg.xpoints[i]-nodeSize, pg.ypoints[i]-nodeSize, 2*nodeSize+1,2*nodeSize+1);
        }
    }
    
    public void paintSelected(PoGraSS g, int orientation, SMarker m) {
        if (pg==null) return;
        g.defineColor("red",255,0,0);
        double sa=getMarkedProportion(m,-1);
        //System.out.println("pp["+i+"] sa="+sa+" "+pp);
        if (sa>0d) {
            if(fill){
                if (useSelAlpha && sa<1.0)
                    g.setColor(((float)Common.selectColor.getRed())/255.0F,
                            ((float)Common.selectColor.getGreen())/255.0F,
                            ((float)Common.selectColor.getBlue())/255.0F,(float)sa);
                else
                    g.setColor("marked");
                g.fillPolygon(pg.xpoints,pg.ypoints,pg.npoints);
            }
            if (drawBorder) {
                if(!fill){
                    if (useSelAlpha && sa<1.0)
                        g.setColor(((float)Common.selectColor.getRed())/255.0F,
                                ((float)Common.selectColor.getGreen())/255.0F,
                                ((float)Common.selectColor.getBlue())/255.0F,(float)sa);
                    else
                        g.setColor("marked");
                } else g.setColor("outline");
                //g.drawPolygon(pg.xpoints,pg.ypoints,pg.npoints,closed);
                for(int i=1; i<pg.npoints; i++){
                    if(!invisibleLines[i-1]){
                        if(lineWidth!=null) g.setLineWidth(lineWidth[i-1]);
                        g.drawLine(pg.xpoints[i-1], pg.ypoints[i-1], pg.xpoints[i], pg.ypoints[i]);
                    }
                }
                if(closed){
                    if(lineWidth!=null) g.setLineWidth(lineWidth[pg.npoints-1]);
                    g.drawLine(pg.xpoints[pg.npoints-1], pg.ypoints[pg.npoints-1], pg.xpoints[0], pg.ypoints[0]);
                }
            }
            if(drawCorners){
                g.setColor("marked");
                for(int i=0; i<pg.npoints; i++){
                    if(noDotsAt==null || !noDotsAt[i])
                        g.fillOval(pg.xpoints[i]-nodeSize, pg.ypoints[i]-nodeSize, 2*nodeSize+1,2*nodeSize+1);
                }
            }
            if(showGapDots && gapDots!=null){
                g.setColor("marked");
                for(int i=0; i<gapDots.length; i++)
                    if(gapDots[i])
                        g.fillOval(pg.xpoints[i]-nodeSize, pg.ypoints[i]-nodeSize, 2*nodeSize+1,2*nodeSize+1);
            }
        }
    }
    
    public String toString() {
        return "PPrimPolygon("+((pg==null)?"<null polygon>":(""+pg.npoints+" points"))+", drawBorder="+drawBorder+", useSelAlpha="+useSelAlpha+")";
    }
    
    public int getNodeSize() {
        return nodeSize;
    }
    
    public void setNodeSize(int nodeSize) {
        if(nodeSize>0)
            this.nodeSize = nodeSize;
    }
}
