import java.util.*;

/** representation of R-eXpressions in Java

    @version $Id$
*/
public class REXP extends Object {
    /** xpression type: NULL */
    public static final int XT_NULL=0;
    /** xpression type: integer */
    public static final int XT_INT=1;
    /** xpression type: double */
    public static final int XT_DOUBLE=2;
    /** xpression type: String */
    public static final int XT_STR=3;
    /** xpression type: language construct (currently content is undefined) */
    public static final int XT_LANG=4;
    /** xpression type: symbol (content is symbol name: String) */
    public static final int XT_SYM=5;
    /** xpression type: RBool */    
    public static final int XT_BOOL=6;
    /** xpression type: Vector */
    public static final int XT_VECTOR=16;
    /** xpression type: RList */
    public static final int XT_LIST=17;
    /** xpression type: int[] */
    public static final int XT_ARRAY_INT=32;
    /** xpression type: double[] */
    public static final int XT_ARRAY_DOUBLE=33;
    /** xpression type: String[] (currently not used, Vector is used instead) */
    public static final int XT_ARRAY_STR=34;
    /** xpression type: RBool[] */
    public static final int XT_ARRAY_BOOL=35;
    /** xpression type: unknown; no assumptions can be made about the content */
    public static final int XT_UNKNOWN=48;

    /** xpression type: RFactor; this XT is internally generated (ergo is does not come from Rsrv.h) to support RFactor class which is built from XT_ARRAY_INT */
    public static final int XT_FACTOR=127; 

    /** xpression type */
    int Xt;
    /** attribute xpression or <code>null</code> if none */
    REXP attr;
    /** content of the xpression - its object type is dependent of {@link #Xt} */
    Object cont;

    /** construct a new, empty (NULL) expression w/o attribute */
    public REXP() { Xt=0; attr=null; cont=null; }

    /** construct a new xpression of type t and content o, but no attribute
	@param t xpression type (XT_...)
	@param o content */
    public REXP(int t, Object o) {
	Xt=t; cont=o; attr=null;
    }

    /** construct a new xpression of type t, content o and attribute at
	@param t xpression type
	@param o content
	@param at attribute */
    public REXP(int t, Object o, REXP at) {
	Xt=t; cont=o; attr=at;
    }

    /** get attribute of the REXP. In R every object can have attached attribute xpression. Some more complex structures such as classes are built that way.
        @return attribute xpression or <code>null</code> if there is none associated */
    public REXP getAttribute() {
        return attr;
    }

    /** get raw content. Use as... methods to retrieve contents of known type.
        @return content of the REXP */
    public Object getContent() {
        return cont;
    }

    /** get xpression type (see XT_.. constants) of the content. It defines the type of the content object.
        @return xpression type */
    public int getType() {
        return Xt;
    }
    
    /** parses byte buffer for binary representation of xpressions - read one xpression slot (descends recursively for aggregated xpressions such as lists, vectors etc.)
	@param x xpression object to store the parsed xpression in
	@param buf buffer containing the binary representation
	@param o offset in the buffer to start at
        @return position just behind the parsed xpression. Can be use for successive calls to {@link #parseREXP} if more than one expression is stored in the binary array. */
    public static int parseREXP(REXP x, byte[] buf, int o) {
	int xl=Rtalk.getLen(buf,o);
	boolean hasAtt=((buf[o]&128)!=0);
	int xt=(int)(buf[o]&127);
	int eox=o+4+xl;
	o+=4;
	
	x.Xt=xt; x.attr=null;
	if (hasAtt) o=parseREXP(x.attr=new REXP(),buf,o);
	if (xt==XT_NULL) {
	    x.cont=null; return o;
	};
	if (xt==XT_DOUBLE) {
	    long lr=Rtalk.getLong(buf,o);
	    x.cont=new Double(Double.longBitsToDouble(lr));
	    o+=8;
	    if (o!=eox) {
		System.out.println("Warning: double SEXP size mismatch\n");
		o=eox;
	    };
	    return o;
	}
	if (xt==XT_ARRAY_DOUBLE) {
	    int as=(eox-o)/8,i=0;
	    double[] d=new double[as];
	    while (o<eox) {
		d[i]=Double.longBitsToDouble(Rtalk.getLong(buf,o));
		o+=8;
		i++;
	    };
	    if (o!=eox) {
		System.out.println("Warning: double array SEXP size mismatch\n");
		o=eox;
	    };
	    x.cont=d;
	    return o;
	};
	if (xt==XT_BOOL) {
	    x.cont=new RBool(buf[o]); o++;
	    if (o!=eox) {
		System.out.println("Warning: bool SEXP size mismatch\n");
		o=eox;
	    };
	    return o;
	};
	if (xt==XT_ARRAY_BOOL) {
	    int as=(eox-o), i=0;
	    RBool[] d=new RBool[as];
	    while(o<eox) {
		d[i]=new RBool(buf[o]);
		i++; o++;
	    };
	    x.cont=d;
	    return o;
	};
	if (xt==XT_INT) {
	    x.cont=new Integer(Rtalk.getInt(buf,o));
	    o+=4;
	    if (o!=eox) {
		System.out.println("Warning: int SEXP size mismatch\n");
		o=eox;
	    };
	    return o;
	}
	if (xt==XT_ARRAY_INT) {
	    int as=(eox-o)/4,i=0;
	    int[] d=new int[as];
	    while (o<eox) {
		d[i]=Rtalk.getInt(buf,o);
		o+=4;
		i++;
	    };
	    if (o!=eox) {
		System.out.println("Warning: int array SEXP size mismatch\n");
		o=eox;
	    };
	    x.cont=d;
	    // hack for lists - special lists attached to int are factors
	    if (x.attr!=null && x.attr.Xt==XT_LIST && x.attr.cont!=null &&
		((RList)x.attr.cont).head!=null &&
		((RList)x.attr.cont).body!=null &&
		((RList)x.attr.cont).head.cont!=null &&
		((RList)x.attr.cont).body.cont!=null &&
		((RList)x.attr.cont).head.Xt==XT_VECTOR &&
		((RList)x.attr.cont).body.Xt==XT_LIST &&
		((RList)((RList)x.attr.cont).body.cont).head!=null &&
		((RList)((RList)x.attr.cont).body.cont).head.Xt==XT_STR &&
		((String)((RList)((RList)x.attr.cont).body.cont).head.cont).compareTo("factor")==0) {
		RFactor f=new RFactor(d,(Vector)((RList)x.attr.cont).head.cont);
		x.cont=f;
		x.Xt=XT_FACTOR;
		x.attr=null;
	    };
	    return o;
	};
	if (xt==XT_VECTOR) {
	    Vector v=new Vector();
	    while(o<eox) {
		REXP xx=new REXP();
		o=parseREXP(xx,buf,o);
		v.addElement(xx);
	    };
	    if (o!=eox) {
		System.out.println("Warning: int vector SEXP size mismatch\n");
		o=eox;
	    };
	    x.cont=v;
	    // fixup for lists since they're stored as attributes of vectors
	    if (x.attr!=null && x.attr.Xt==XT_LIST && x.attr.cont!=null) {
		RList l=new RList();
		l.head=((RList)x.attr.cont).head;
		l.body=new REXP(XT_VECTOR,v);
		x.cont=l;
		x.Xt=XT_LIST; x.attr=x.attr.attr;
		// one more hack: we're de-vectorizing strings if alone
		// so we should invert that in case of list heads
		if (l.head.Xt==XT_STR) {
		    Vector sv=new Vector();
		    sv.addElement(l.head);
		    l.head=new REXP(XT_VECTOR,sv,l.head.attr);
		    l.head.attr=null;
		};
	    };
	    return o;
	};
	if (xt==XT_STR) {
	    int i=o;
	    while (buf[i]!=0 && i<eox) i++;
	    try {
		x.cont=new String(buf,o,i-o,"UTF-8");
	    } catch(Exception e) {
		System.out.println("unable to convert string\n");
		x.cont=null;
	    };
	    o=eox;
	    return o;
	};
	if (xt==XT_LIST) {
	    RList rl=new RList();
	    rl.head=new REXP();
	    rl.body=new REXP();
	    o=parseREXP(rl.head,buf,o);
	    o=parseREXP(rl.body,buf,o);
	    if (o!=eox) {
		System.out.println("Warning: int array SEXP size mismatch\n");
		o=eox;
	    };
	    x.cont=rl;
	    return o;
	};
	
	x.cont=null;
	o=eox;
	System.out.println("unhandled type: "+xt);
	return o;
    }

    /** returns human-readable name of the xpression type as string
	@param xt xpression type
	@return name of the xpression type */
    public static String xtName(int xt) {
	if (xt==XT_NULL) return "NULL";
	if (xt==XT_INT) return "INT";
	if (xt==XT_STR) return "STRING";
	if (xt==XT_DOUBLE) return "REAL";
	if (xt==XT_BOOL) return "BOOL";
	if (xt==XT_ARRAY_INT) return "INT*";
	if (xt==XT_ARRAY_STR) return "STRING*";
	if (xt==XT_ARRAY_DOUBLE) return "REAL*";
	if (xt==XT_ARRAY_BOOL) return "BOOL*";
	if (xt==XT_SYM) return "SYMBOL";
	if (xt==XT_LANG) return "LANG";
	if (xt==XT_LIST) return "LIST";
	if (xt==XT_VECTOR) return "VECTOR";
	if (xt==XT_FACTOR) return "FACTOR";
	return "<unknown "+xt+">";
    }	

    /** get content of the REXP as string (if it is one)
        @return string content or <code>null</code> if the REXP is no string */
    public String asString() {
        return (Xt==XT_STR)?(String)cont:null;
    }

    /** get content of the REXP as int (if it is one)
        @return int content or 0 if the REXP is no integer */
    public int asInt() {
        return (Xt==XT_INT)?((Integer)cont).intValue():0;
    }

    /** get content of the REXP as double (if it is one)
        @return double content or 0.0 if the REXP is no double */
    public double asDouble() {
        return (Xt==XT_DOUBLE)?((Double)cont).doubleValue():0.0;
    }

    /** get content of the REXP as {@link Vector} (if it is one)
        @return Vector content or <code>null</code> if the REXP is no Vector */
    public Vector asVector() {
        return (Xt==XT_VECTOR)?(Vector)cont:null;
    }

    /** get content of the REXP as {@link RFactor} (if it is one)
        @return {@link RFactor} content or <code>null</code> if the REXP is no factor */
    public RFactor asFactor() {
        return (Xt==XT_FACTOR)?(RFactor)cont:null;
    }

    /** get content of the REXP as {@link RList} (if it is one)
        @return {@link RList} content or <code>null</code> if the REXP is no list */
    public RList asList() {
        return (Xt==XT_LIST)?(RList)cont:null;
    }

    /** get content of the REXP as {@link RBool} (if it is one)
        @return {@link RBool} content or <code>null</code> if the REXP is no logical value */
    public RBool asBool() {
        return (Xt==XT_LIST)?(RBool)cont:null;
    }

    /** displayable contents of the expression. The expression is traversed recursively if aggregation types are used (Vector, List, etc.)
        @return String descriptive representation of the xpression */
    public String toString() {
	StringBuffer sb=
	    new StringBuffer("["+xtName(Xt)+" ");
	if (attr!=null) sb.append("\nattr="+attr+"\n ");
	if (Xt==XT_DOUBLE) sb.append((Double)cont);
	if (Xt==XT_INT) sb.append((Integer)cont);
	if (Xt==XT_BOOL) sb.append((RBool)cont);
	if (Xt==XT_FACTOR) sb.append((RFactor)cont);
	if (Xt==XT_ARRAY_DOUBLE) {
	    double[] d=(double[])cont;
	    sb.append("(");
	    for(int i=0; i<d.length; i++) {
		sb.append(d[i]);
		if (i<d.length-1) sb.append(", ");
	    };
	    sb.append(")");
	};
	if (Xt==XT_ARRAY_INT) {
	    int[] d=(int[])cont;
	    sb.append("(");
	    for(int i=0; i<d.length; i++) {
		sb.append(d[i]);
		if (i<d.length-1) sb.append(", ");
	    };
	    sb.append(")");
	};
	if (Xt==XT_ARRAY_BOOL) {
	    RBool[] d=(RBool[])cont;
	    sb.append("(");
	    for(int i=0; i<d.length; i++) {
		sb.append(d[i]);
		if (i<d.length-1) sb.append(", ");
	    };
	    sb.append(")");
	};
	if (Xt==XT_VECTOR) {
	    Vector v=(Vector)cont;
	    sb.append("(");
	    for(int i=0; i<v.size(); i++) {
		sb.append(((REXP)v.elementAt(i)).toString());
		if (i<v.size()-1) sb.append(", ");
	    };
	    sb.append(")");
	};
	if (Xt==XT_STR) {
	    sb.append("\"");
	    sb.append((String)cont);
	    sb.append("\"");
	};
	if (Xt==XT_LIST) {
	    RList l=(RList)cont;
	    sb.append(l.head); sb.append(" <-> ");
	    sb.append(l.body);
	};
	sb.append("]");
	return sb.toString();
    };
}   
