import java.awt.*;               // 
import java.io.*;               // 


public class Selection {
  
  public static final int MODE_STANDARD = 0;
  public static final int MODE_AND      = 1;
  public static final int MODE_OR       = 2;
  public static final int MODE_XOR      = 3;
  public static final int MODE_NOT      = 4;

  public static final int VALID         = 0;
  public static final int KILLED        = 1;

  public Rectangle r;
  public Object o;
  public int step;
  public int mode;
  public int status = VALID;
  DragBox d;
  String modeString;
  public Query condition=new Query();		// Query to store the part of the WHERE clause generated  


  public Selection(Rectangle r, Object o, int step, int mode, DragBox d) {
    
    this.r = r;
    this.o = o;
    this.step = step;
    this.mode = mode;
    this.d = d;
    this.status = VALID;
  }

  public static String getModeString(int mode) {

      switch(mode) {
          case MODE_STANDARD:
              return "Replace";
          case MODE_AND:
              return "And";
          case MODE_OR:
              return "Or";
          case MODE_XOR:
              return "XOr";
          case MODE_NOT:
              return "Not";
      }
      return null;
  }

  public static String getSQLModeString(int mode) {

      switch(mode) {
          case MODE_STANDARD:
              return "Replace";
          case MODE_AND:
              return "AND";
          case MODE_OR:
              return "OR";
          case MODE_XOR:
              return "XOr";
          case MODE_NOT:
              return "AND NOT";
      }
      return null;
  }
}
