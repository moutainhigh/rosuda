import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

class TestException extends Exception {
    public TestException(String msg) { super(msg); }
}

public class test {
    public static void main(String[] args) {
	try {
	    RConnection c = new RConnection();

	    System.out.println(">>"+c.eval("R.version$version.string").asString()+"<<");
		
	    {
			System.out.println("Test assigning of lists and vectors ...");
			RList l = new RList();
			l.put("a",new REXPInteger(new int[] { 0,1,2,3}));
			l.put("b",new REXPDouble(new double[] { 0.5,1.2,2.3,3.0}));
			System.out.println("assign x=pairlist, y=vector, z=data.frame");
			c.assign("x", new REXPList(l));
			c.assign("y", new REXPGenericVector(l));
			c.assign("z", REXP.createDataFrame(l));
			System.out.println("pull all three back to Java");
			REXP x = c.parseAndEval("x");
			System.out.println("x = "+x);
			x = c.eval("y");
			System.out.println("y = "+x);
			x = c.eval("z");
			System.out.println("z = "+x);
	    }

            { // factors
                System.out.println("test support of factors");
                REXP f = c.eval("factor(paste('F',as.integer(runif(20)*5),sep=''))");
                System.out.println("isFactor: "+f.isFactor()+"\nasFactor: "+f.asFactor());
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor test failed");
                System.out.println("singe-level factor used to degenerate:");
                f = c.eval("factor('foo')");
                System.out.println("isFactor: "+f.isFactor()+"\nasFactor: "+f.asFactor());
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("single factor test failed");
            }


	    {
			System.out.println("lowess test");
			double x[] = c.eval("rnorm(100)").asDoubles();
			double y[] = c.eval("rnorm(100)").asDoubles();
			c.assign("x", x);
			c.assign("y", y);
			RList l = c.parseAndEval("lowess(x,y)").asList();
			System.out.println(l);
			x = l.at("x").asDoubles();
			y = l.at("y").asDoubles();
		}
		
		{
            System.out.println("matrix: create a matrix");
            int m=100, n=100;
            double[] mat=new double[m*n];
            int i=0;
            while (i<m*n) mat[i++]=i/100;
            System.out.println("matrix: assign a matrix");
            c.assign("m", mat);
            c.voidEval("m<-matrix(m,"+m+","+n+")");
            System.out.println("matrix: cross-product");
            double[][] mr=c.parseAndEval("crossprod(m,m)").asDoubleMatrix();
		}
		
		{
			System.out.println("test serialization and raw vectors");
			byte[] b = c.eval("serialize(ls, NULL, ascii=FALSE)").asBytes();
			System.out.println("serialized ls is "+b.length+" bytes long");
			c.assign("r", new REXPRaw(b));
			String[] s = c.eval("unserialize(r)()").asStrings();
			System.out.println("we have "+s.length+" items in the workspace");
		}
		
		} catch (RserveException rse) {
	    System.out.println(rse);
	} catch (REXPMismatchException mme) {
	    System.out.println(mme);
	    mme.printStackTrace();
        } catch(TestException te) {
            System.err.println("** Test failed: "+te.getMessage());
            te.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}