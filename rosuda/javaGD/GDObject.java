//
//  GDObject.java
//  Java graphics device
//
//  Created by Simon Urbanek on Thu Aug 05 2004.
//  Copyright (c) 2004 Simon Urbanek. All rights reserved.
//
//  $Id$

package org.rosuda.javaGD;

import java.awt.*;

/** GDObject is an arbitrary object that can be painted */
abstract class GDObject {
    public abstract void paint(Component c, GDState gs, Graphics g);
}

/** object storing the current graphics state */
class GDState {
    public Color col;
    public Color fill;
    public Font f;
}

class GDLine extends GDObject {
    double x1,y1,x2,y2;
    public GDLine(double x1, double y1, double x2, double y2) {
        this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2;
    }

    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.col!=null)
            g.drawLine((int)(x1+0.5),(int)(y1+0.5),(int)(x2+0.5),(int)(y2+0.5));
    }
}

class GDRect extends GDObject {
    double x1,y1,x2,y2;
    public GDRect(double x1, double y1, double x2, double y2) {
        double tmp;
        if (x1>x2) { tmp=x1; x1=x2; x2=tmp; }
        if (y1>y2) { tmp=y1; y1=y2; y2=tmp; }
        this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2;
        //System.out.println(">> RECT "+x1+":"+y1+" "+x2+":"+y2);
    }

    public void paint(Component c, GDState gs, Graphics g) {
        //System.out.println(" paint> rect: "+x1+":"+y1+" "+x2+":"+y2);
        int x=(int)(x1+0.5);
        int y=(int)(y1+0.5);
        int w=(int)(x2+0.5)-x;
        int h=(int)(y2+0.5)-y;
        if (gs.fill!=null) {
            g.setColor(gs.fill);
            g.fillRect(x,y,w+1,h+1);
            if (gs.col!=null) g.setColor(gs.col);
        }
        if (gs.col!=null)
            g.drawRect(x,y,w,h);
    }
}

class GDClip extends GDObject {
    double x1,y1,x2,y2;
    public GDClip(double x1, double y1, double x2, double y2) {
        double tmp;
        if (x1>x2) { tmp=x1; x1=x2; x2=tmp; }
        if (y1>y2) { tmp=y1; y1=y2; y2=tmp; }
        this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2;
    }

    public void paint(Component c, GDState gs, Graphics g) {
        g.setClip((int)(x1+0.5),(int)(y1+0.5),(int)(x2-x1+1.7),(int)(y2-y1+1.7));
    }
}

class GDCircle extends GDObject {
    double x,y,r;
    public GDCircle(double x, double y, double r) {
        this.x=x; this.y=y; this.r=r;
    }

    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.fill!=null) {
            g.setColor(gs.fill);
            g.fillOval((int)(x-r+0.5),(int)(y-r+0.5),(int)(r+r+0.5),(int)(r+r+0.5));
            if (gs.col!=null) g.setColor(gs.col);
        }
        if (gs.col!=null)
            g.drawOval((int)(x-r+0.5),(int)(y-r+0.5),(int)(r+r+0.5),(int)(r+r+0.5));
    }
}

class GDText extends GDObject {
    double x,y,r,h;
    String txt;
    public GDText(double x, double y, double r, double h, String txt) {
        this.x=x; this.y=y; this.r=r; this.h=h; this.txt=txt;
    }

    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.col!=null) {
            double rx=x, ry=y;
            double hc=0d;
            if (h!=0d) {
                FontMetrics fm=g.getFontMetrics();
                int w=fm.stringWidth(txt);
                hc=((double)w)*h;
                rx=x-(((double)w)*h);
            }
            int ix=(int)(rx+0.5), iy=(int)(ry+0.5);

            if (r!=0d) {
                Graphics2D g2d=(Graphics2D) g;
                g2d.translate(x,y);
                double rr=-r/180d*Math.PI;
                g2d.rotate(rr);
                if (hc!=0d)
                    g2d.translate(-hc,0d);
                g2d.drawString(txt,0,0);
                if (hc!=0d)
                    g2d.translate(hc,0d);
                g2d.rotate(-rr);
                g2d.translate(-x,-y);
            } else
                g.drawString(txt,ix,iy);
        }
    }
}


class GDFont extends GDObject {
    double cex,ps,lineheight;
    int face;
    String family;

    Font font;

    public GDFont(double cex, double ps, double lineheight, int face, String family) {
        //System.out.println(">> FONT(cex="+cex+",ps="+ps+",lh="+lineheight+",face="+face+",\""+family+"\")");
        this.cex=cex; this.ps=ps; this.lineheight=lineheight; this.face=face; this.family=family;
        int jFT=Font.PLAIN;
        if (face==2) jFT=Font.BOLD;
        if (face==3) jFT=Font.ITALIC;
        if (face==4) jFT=Font.BOLD|Font.ITALIC;
        font=new Font(family.equals("")?null:family, jFT, (int)(cex*ps+0.5));
    }

    public Font getFont() { return font; }
    
    public void paint(Component c, GDState gs, Graphics g) {
        g.setFont(font);
        gs.f=font;
    }
}

class GDPolygon extends GDObject {
    int n;
    double x[],y[];
    int xi[], yi[];
    boolean isPolyline;
    public GDPolygon(int n, double[] x, double[] y, boolean isPolyline) {
        this.x=x; this.y=y; this.n=n; this.isPolyline=isPolyline;
        int i=0;
        xi=new int[n]; yi=new int[n];
        while (i<n) {
            xi[i]=(int)(x[i]+0.5);
            yi[i]=(int)(y[i]+0.5);
            i++;
        }
    }

    public void paint(Component c, GDState gs, Graphics g) {
        if (gs.fill!=null && !isPolyline) {
            g.setColor(gs.fill);
            g.fillPolygon(xi, yi, n);
            if (gs.col!=null) g.setColor(gs.col);
        }
        if (gs.col!=null) {
            if (isPolyline)
                g.drawPolyline(xi, yi, n);
            else
                g.drawPolygon(xi, yi, n);
        }
    }
}

class GDColor extends GDObject {
    int col;
    Color gc;
    public GDColor(int col) {
        this.col=col;
        //System.out.println(">> COLOR: "+Integer.toString(col,16));
        if (col==-1 || col==0x80000000) gc=null;
        else
            gc=new Color(((float)(col&255))/255f,
                         ((float)((col>>8)&255))/255f,
                         ((float)((col>>16)&255))/255f,
                         1f-((float)((col>>24)&255))/255f);
        //System.out.println("          "+gc);
    }

    public void paint(Component c, GDState gs, Graphics g) {
        gs.col=gc;
        System.out.println(" paint > color> (col="+col+") "+gc);
        if (gc!=null) g.setColor(gc);
    }
}

class GDFill extends GDObject {
    int col;
    Color gc;
    public GDFill(int col) {
        this.col=col;
        //System.out.println(">> FILL COLOR: "+Integer.toString(col,16));
        if (col==-1 || col==0x80000000)
            gc=null;
        else
            gc=new Color(((float)(col&255))/255f,
                         ((float)((col>>8)&255))/255f,
                         ((float)((col>>16)&255))/255f,
                         1f-((float)((col>>24)&255))/255f);
        //System.out.println("          "+gc);
    }

    public void paint(Component c, GDState gs, Graphics g) {
        gs.fill=gc;
    }
}

class GDLinePar extends GDObject {
    double lwd;
    int lty;
    BasicStroke bs;

    public GDLinePar(double lwd, int lty) {
        this.lwd=lwd; this.lty=lty;
        //System.out.println(">> LINE TYPE: width="+lwd+", type="+Integer.toString(lty,16));
        bs=null;
        if (lty==0)
            bs=new BasicStroke((float)lwd);
        else if (lty==-1)
            bs=new BasicStroke(0f);
        else {
            int l=0;
            int dt=lty;
            while (dt>0) {
                dt>>=4;
                l++;
            }
            float[] dash=new float[l];
            dt=lty;
            l=0;
            while (dt>0) {
                int rl=dt&15;
                dash[l++]=(float)rl;
                dt>>=4;
            }
            bs=new BasicStroke((float)lwd, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3f, dash, 0f);
        }
    }

    public void paint(Component c, GDState gs, Graphics g) {
        if (bs!=null)
            ((Graphics2D)g).setStroke(bs);
    }
}
