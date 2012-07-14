package org.rosuda.deducer.data;


import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.swing.*;

import org.rosuda.JGR.util.ErrorMsg;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPFactor;
import org.rosuda.REngine.REXPInteger;
import org.rosuda.REngine.REXPLanguage;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPReference;
import org.rosuda.REngine.REXPString;
import org.rosuda.REngine.REXPVector;
import org.rosuda.REngine.REXPGenericVector;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.deducer.Deducer;

import sun.font.FontManager;


/**
 * Data Frame model
 * 
 * @author ifellows
 *
 */
public class RDataFrameModel extends ExDefaultTableModel {
	
	private static String guiEnv = Deducer.guiEnv;
	
	private static int numPageRows = 500;
	private static int numPageCols = 10;
	private static int maxPages = 10;
	private static String NA_STRING = "*$_$_$_$$NA$$_$_$_$*";
	
	private String rDataName=null;
	
	private String tempDataName=null;
	
	RowNamesModel rowNamesModel = new RowNamesModel();
	
	volatile ArrayList pages = new ArrayList();
	volatile ArrayList pageLocations = new ArrayList();
	volatile HashSet pendingPages = new HashSet();
	
	int ncol = 0;
	int nrow = 0;
	//0-numeric,1-integer,2-logical,3-factor,4-character,5-other
	int[] classes = new int[]{};
	
	
	public static final int numExtensionRows = 50;
	public static final int numExtensionColumns = 10; 
	
	public RDataFrameModel(){}
	
	public RDataFrameModel(String name){
		setDataName(name);	
	}
	
	public String getDataName(){return rDataName;}
	
	public void setDataName(String name){
		
		boolean envDefined = ((REXPLogical)Deducer.eval("'"+guiEnv+"' %in% .getOtherObjects()")).isTRUE()[0];
		
		if(!envDefined){
			Deducer.eval(guiEnv+"<-new.env(parent=emptyenv())");
		}
		if(tempDataName!=null)
			removeCachedData();
		rDataName = name;
		if(rDataName!=null){
			tempDataName = Deducer.getUniqueName(rDataName + Math.random(),guiEnv);
			try {
				Deducer.eval(guiEnv+"$"+tempDataName+"<-"+rDataName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			populateMetaData();
		}
		this.fireTableStructureChanged();
		this.fireTableDataChanged();
	}
	
	public void populateMetaData(){
		int[] dims;
		try {
			dims = Deducer.eval("dim("+rDataName+")").asIntegers();
			nrow = dims[0];
			ncol = dims[1];	
			
			String[] tmpClasses = null;
			try {
				tmpClasses = Deducer.eval(
						"sapply("+rDataName+",function(x){a <- class(x);return(a[length(a)])})").asStrings();
			} catch (Exception e1) {}
			if(tmpClasses==null){
				tmpClasses = new String[ncol];
				for(int i=0;i<tmpClasses.length;i++)
					tmpClasses[i] = "numeric";
			}
			classes = new int[ncol];
			for(int i=0;i<ncol;i++){
				if(tmpClasses[i].equals("numeric"))
					classes[i] = 0;
				else if(tmpClasses[i].equals("integer"))
					classes[i] = 1;
				else if(tmpClasses[i].equals("logical"))
					classes[i] = 2;
				else if(tmpClasses[i].equals("factor"))
					classes[i] = 3;
				else if(tmpClasses[i].equals("character"))
					classes[i] = 4;
				else 
					classes[i] = 5;

			}
			//System.out.println(classes.length);
		} catch (Exception e) {
			nrow=0;
			ncol=0;
			classes = new int[]{};
			e.printStackTrace();
		}	
	}
	
	public int getColumnCount( ){
		if(rDataName!=null){
			return ncol+numExtensionColumns;
		}else
			return 0;
	}
	
	public int getRealColumnCount( ){
		if(rDataName!=null){
			return ncol;
		}else
			return 0;
	}

	public int getRealRowCount(){
		if(rDataName!=null){
				return nrow;
		}else
			return 0;
	}
	
	public int getRowCount(){
		if(rDataName!=null){
			return nrow + numExtensionRows;
		}else
			return 0;
	}
	
	public void removeColumn(int colNumber){
		if(colNumber<getRealColumnCount()){
			Deducer.eval(rDataName+"<-"+rDataName+"[,-"+(colNumber+1)+"]");
			refresh();
		}
	}

	public void removeRow(int row){
		if((row+1)<=getRealRowCount()){
			Deducer.eval(rDataName + "<- "+rDataName + "[-"+(row+1)+",]");
			refresh();
		}
	}
	
	public void insertNewColumn(int col){
		if(col>getRealColumnCount()+1)
			return;
		if(col<1)
			Deducer.eval(rDataName+"<-data.frame(V=as.integer(NA),"+
					rDataName+"[,"+(col+1)+":"+getRealColumnCount()+",drop=FALSE])");
		else if(col>=getRealColumnCount())
			Deducer.eval(rDataName+"<-data.frame("+rDataName+",V=as.integer(NA))");
		else
			Deducer.eval(rDataName+"<-data.frame("+rDataName+"[,1:"+col+",drop=FALSE],V=as.integer(NA),"+
				rDataName+"[,"+(col+1)+":"+getRealColumnCount()+",drop=FALSE])");
		refresh();
	}
	
	public void insertNewRow(int row){
		int rowCount =getRealRowCount();
		setValueAt("NA",Math.max(rowCount,row),0);
		Deducer.eval("attr("+rDataName+",'row.names')["+(Math.max(rowCount,row)+1)+"]<-'New'");
		if(row<1)
			Deducer.eval(rDataName+"<-rbind("+rDataName+
					"["+(rowCount+1)+",],"+rDataName+"["+(row+1)+":"+rowCount+",,drop=FALSE])");
		else if(row<rowCount)
			Deducer.eval(rDataName+"<-rbind("+rDataName+"[1:"+row+",,drop=FALSE],"+rDataName+
					"["+(rowCount+1)+",],"+rDataName+"["+(row+1)+":"+rowCount+",,drop=FALSE])");
		Deducer.eval("rownames("+rDataName+")<-make.unique(rownames("+rDataName+"))");
		
	}
	
	public String getPageValue(int row,int col){
		for(int i=0;i<pageLocations.size();i++){
			int[] loc = (int[]) pageLocations.get(i);
			if(row>=loc[0] && row< (loc[0]+numPageRows) && col>=loc[1] && col<(loc[1]+numPageCols)){
				//System.out.println("found page");
				String[][] page = (String[][]) pages.get(i);
				return page[col - loc[1]][row - loc[0]];
			}
		}
		return null;
	}
	
	public boolean setPageValue(String value, int row,int col){
		for(int i=0;i<pageLocations.size();i++){
			int[] loc = (int[]) pageLocations.get(i);
			if(row>=loc[0] && row< (loc[0]+numPageRows) && col>=loc[1] && col<(loc[1]+numPageCols)){
				//System.out.println("found page");
				String[][] page = (String[][]) pages.get(i);
				page[col - loc[1]][row - loc[0]] = value;
				return true;
			}
		}
		return false;
	}
	
	public void lazyLoad(int row,int col){
		final int prow = (row/numPageRows)*numPageRows;
		final int pcol = (col/numPageCols)*numPageCols;
		
		if(!pending(prow,pcol)){
			//declarePending(prow,pcol);
			new Thread(new Runnable(){

				public void run() {
					loadPage(prow,pcol);
				}
				
			}).start();
		}
	}
	
	public boolean pending(int row,int col){
		int tmp = row * col * (row > col ? 1 : -1);
		return !pendingPages.add(new Integer(tmp));
	}
	
	public boolean isPending(int row,int col){
		int tmp = row * col * (row > col ? 1 : -1);
		return pendingPages.contains(new Integer(tmp));
	}
	
	public void declarePending(int row, int col){
		int tmp = row * col * (row > col ? 1 : -1);
		pendingPages.add(new Integer(tmp));
	}
	
	public void removePending(int row,int col){
		int tmp = row * col * (row > col ? 1 : -1);
		pendingPages.remove(new Integer(tmp));		
	}
	
	public void loadPage(int row,int col){
		//System.out.println("number of pages:" + pages.size());
		//System.out.println("Loading page:");
		//System.out.println(row);
		//System.out.println(col);
		String[][] page = new String[numPageCols][numPageRows];
		for(int j=0;j<numPageCols;j++){
			String[] column =null;
			if(j>=ncol-col || row>=nrow){
				column = new String[numPageRows];
				for(int i=0;i<column.length;i++)
					column[i]="";
			}else{
				try {
					column = Deducer.eval("format("+rDataName+"[("+
							(row+1)+"):("+Math.min(nrow,row+numPageRows+1)+"),"+
							(j+col+1)+"])").asStrings();
					boolean[] isNA = Deducer.eval(rDataName+"[("+
							(row+1)+"):("+Math.min(nrow,row+numPageRows+1)+"),"+
							(j+col+1)+"]").isNA();
					for(int i=0;i<isNA.length;i++){
						if(isNA[i])
							column[i]=NA_STRING;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if(column!=null){
				if(column.length<numPageRows){
					String[] tmp = new String[numPageRows];
					for(int i=0;i<numPageRows;i++){
						if(i<column.length)
							tmp[i]=column[i];
						else
							tmp[i]="";
					}
					column = tmp;
				}
				page[j] = column;
			}else{
				column = new String[numPageRows];
				for(int i=0;i<column.length;i++)
					column[i]="";
				page[j] = column;
			}
			//System.out.println(column.length);
		}
		pages.add(page);
		int[] dim = new int[]{row,col};
		pageLocations.add(dim);
		if(pages.size()>maxPages){
			pages.remove(0);
			pageLocations.remove(0);
		}
		removePending(row,col);
		//this.fireTableRowsUpdated(row, row+numPageRows-1);
		for(int i=row;i<row+numPageRows;i++){
			for(int j=col;j<col+numPageCols;j++){
				this.fireTableCellUpdated(i, j);
			}
		}
	}
	
	
	
	public Object getValueAt(int row, int col){
		String value = getPageValue(row,col);
		if(value!=null){
			return value;
		}
		
		lazyLoad(row,col);
		return "";
	}
	
	public Object getActualValueAt(int row, int col){
		String value = getPageValue(row,col);
		if(value!=null){
			return value;
		}
		try{
		return Deducer.eval("format("+rDataName+"[("+
				(row+1)+"),"+
				(col+1)+"])").asString();
		}catch(Exception e){
			e.printStackTrace();
			return "";
		}
	}
	
	public void setValueAt(Object value,int row, int col){
		if(!this.isCellEditable(row, col))
			return;		
		if(value==null)
			return;
		int numRealRows =getRealRowCount();
		int numRealCols =getRealColumnCount();		
		boolean colClassChange = false;
		String valueString = value.toString().trim();	
		boolean isDouble=true;
		try{
			Double.parseDouble(valueString);
		}catch(Exception e){
			isDouble=false;
		}		
		int colClass = 4;
		if(col<classes.length)
			colClass = classes[col];
		boolean isLogical = value.equals("TRUE") || value.equals("FALSE");
		boolean isNA = value.equals("NA");
		
		Deducer.eval("rm(\"" +tempDataName+"\",envir="+guiEnv+");");
		if(isNA){
			Deducer.eval(rDataName+"["+(row+1)+","+(col+1)+"]<- NA");
			setPageValue(NA_STRING,row,col);
		}else if(colClass==3){
			boolean isNewLevel=((REXPLogical)Deducer.eval("'"+value.toString()+"' %in% " +
					"levels(" +rDataName+"[,"+(col+1)+"])")).isFALSE()[0];

			if(isNewLevel){
				String addLevel = "levels(" +rDataName+"[,"+(col+1)+"])<-c("+
						"levels(" +rDataName+"[,"+(col+1)+"]),'"+value.toString()+"')";
				Deducer.eval(addLevel);
			}
			Deducer.eval(rDataName+"["+(row+1)+","+(col+1)+"]<-'"+value.toString()+"'");	
			setPageValue(value.toString(),row,col);
		}else if(isDouble || isLogical){
			Deducer.eval(rDataName+"["+(row+1)+","+(col+1)+"]<-" + value.toString());
			setPageValue(value.toString(),row,col);
		}else{
			Deducer.eval(rDataName+"["+(row+1)+","+(col+1)+"]<-'"+value.toString()+"'");
			setPageValue(value.toString(),row,col);
			if(colClass<3){
				classes[col] = 4;
				colClassChange=true;
			}
		}
		Deducer.eval(guiEnv+"$"+tempDataName+"<-"+rDataName);
		this.fireTableCellUpdated(row, col);			
		if(colClassChange)
			this.fireTableDataChanged();
		if((row+1)>numRealRows){
			Deducer.eval("rownames("+rDataName+")<-make.unique(rownames("+rDataName+"))");
			pageLocations.clear();
			pages.clear();
			pendingPages.clear();
			populateMetaData();			
			this.fireTableRowsInserted(numRealRows,row);			
			this.fireTableRowsUpdated(numRealRows,row);
			rowNamesModel.refresh();

			this.fireTableDataChanged();
		}
		if((col+1)>numRealCols){
			pageLocations.clear();
			pages.clear();
			pendingPages.clear();
			populateMetaData();
			this.fireTableStructureChanged();
			this.fireTableDataChanged();
		}
	
		

	}
	
	public Object[][] getRange(int row1, int row2, int col1, int col2){
		if(row1>=nrow || col1>=ncol)
			return new Object[0][0];
		row2 = Math.min(row2,nrow);
		col2 = Math.min(col2,ncol);
		Object[][] result = new Object[row2-row1][col2-col1];
		for(int j=col1;j<col2;j++){
			String[] column;
			try {
				column = Deducer.eval("format(" + rDataName+"[("+
						(row1+1)+"):("+Math.min(nrow,row2+1)+"),"+
						(j+1)+"])").asStrings();
				
				boolean[] isNA = Deducer.eval(rDataName+"[("+
						(row1+1)+"):("+Math.min(nrow,row2+1)+"),"+
						(j+1)+"]").isNA();
				for(int i=0;i<column.length;i++)
					if(isNA[i])
						column[i] = "NA";
			} catch (Exception e) {
				column = new String[row2-row1];
				e.printStackTrace();
			}
			for(int i=0;i<row2 - row1;i++){
				if(i-row1 < column.length)
					result[i][j-col1] = column[i];
				else
					result[i][j-col1] = null;
			}
		}
		return result;
	}
	
	public void setRange(Object[][] values,int row,int col){
		String temporary = "temporary" + (new Random()).nextDouble();
		REXP envir = Deducer.eval(guiEnv);
		
		if(values.length==0)
			return;
		int numRows = values.length;
		int numCols = values[0].length;
		for(int j=0;j<numCols;j++){
			boolean canParseDouble = true;
			for(int i=0;i<numRows;i++){
				try{
					if(values[i].length>j && values[i][j]!=null)
						Double.parseDouble(values[i][j].toString());
				}catch(Exception e){
					
					if(!values[i][j].toString().equals("NA")){
						canParseDouble=false;
						break;
					}
				}
			}
			boolean canParseInteger = canParseDouble;
			if(canParseDouble){
				for(int i=0;i<numRows;i++){
					try{
						if(values[i].length>j && values[i][j]!=null)
							Integer.parseInt(values[i][j].toString());
					}catch(Exception e){
						if(!values[i][j].toString().equals("NA")){
							canParseInteger=false;
							break;
						}
					}
				}
			}
			
			if(classes[col+j]==3){
				HashSet levels = new HashSet();
				for(int i=0;i<numRows;i++)
					if(values[i].length>j  && values[i][j]!=null && !values[i][j].toString().equals("NA")){
						levels.add(values[i][j].toString());
						if(levels.size()>10000){
							levels = new HashSet();
							break;
						}
					}
				Object[] tmp = levels.toArray();
				for(int i=0;i<tmp.length;i++){
					String value = (String) tmp[i];
					boolean isNewLevel=((REXPLogical)Deducer.eval("'"+value.toString()+"' %in% " +
							"levels(" +rDataName+"[,"+(col+j+1)+"])")).isFALSE()[0];
					if(isNewLevel){
						String addLevel = "levels(" +rDataName+"[,"+(col+j+1)+"])<-c("+
								"levels(" +rDataName+"[,"+(col+j+1)+"]),'"+value.toString()+"')";
						Deducer.eval(addLevel);
					}
				}
			}
			
			if(canParseInteger){
				int[] column = new int[numRows];
				for(int i=0;i<numRows;i++){
					if(values[i].length>j && values[i][j]!=null && !values[i][j].toString().equals("NA"))
						column[i] = Integer.parseInt(values[i][j].toString());
					else
						column[i] = REXPInteger.NA;
				}
				try {
					Deducer.getREngine().assign(temporary, new REXPInteger(column), envir);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else if(canParseDouble){
				double[] column = new double[numRows];
				for(int i=0;i<numRows;i++){
					if(values[i].length>j&& values[i][j]!=null && !values[i][j].toString().equals("NA"))
						column[i] = Double.parseDouble(values[i][j].toString());
					else
						column[i] = REXPDouble.NA;
				}
				try {
					Deducer.getREngine().assign(temporary, new REXPDouble(column), envir);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}else{
				String[] column = new String[numRows];
				for(int i=0;i<numRows;i++){
					if(values[i].length>j&& values[i][j]!=null && !values[i][j].toString().equals("NA"))
						column[i] = values[i][j].toString();
					else
						column[i] = null;
				}
				try {
					Deducer.getREngine().assign(temporary, new REXPString(column), envir);
				} catch (Exception e) {
					e.printStackTrace();
				}				
			}
			//Deducer.eval("print("+guiEnv+"$"+temporary+")");
			Deducer.eval(rDataName+"[("+
					(row+1)+"):("+(row+numRows)+"),"+
					(col+j+1)+"] <- " + guiEnv+"$"+temporary);
		}
		Deducer.eval("rm(\"" +temporary+"\",envir="+guiEnv+");");
		refresh();
	}
	
	public void eraseRange(int row1, int row2, int col1, int col2){
		if(row1>=nrow || col1>=ncol)
			return;
		row2 = Math.min(row2,nrow);
		col2 = Math.min(col2,ncol);		
		Deducer.eval("rm(\"" +tempDataName+"\",envir="+guiEnv+");");
		Deducer.eval(rDataName+"[("+
				(row1+1)+"):("+(row2)+"),"+
				(col1+1)+":"+(col2)+"] <- NA");	
		for(int i=row1;i<row2;i++)
			for(int j=col1;j<col2;j++){
				setPageValue(NA_STRING,i,j);
				this.fireTableCellUpdated(i, j);
			}
		Deducer.eval(guiEnv+"$"+tempDataName+"<-"+rDataName);
	}
	
	/**
	 * Notifies components about changes in the model
	 */
	public boolean refresh(){
		boolean changed = false;
		REXP exist = Deducer.eval("!inherits(try(eval(parse(text=\""+Deducer.addSlashes(rDataName)+
				"\")),silent=TRUE),'try-error')");
			//Deducer.eval("exists('"+rDataName+"')");
		if(exist!=null && ((REXPLogical)exist).isTRUE()[0]){
			REXP ident =Deducer.eval("identical("+rDataName+","+guiEnv+"$"+tempDataName+")"); 
			if(ident!=null && ((REXPLogical)ident).isFALSE()[0]){
				REXP strSame = Deducer.eval("all(dim("+rDataName+")==dim("+guiEnv+"$"+tempDataName+")) && " +
								"identical(colnames("+rDataName+"),colnames("+guiEnv+"$"+tempDataName+"))");
				Deducer.eval(guiEnv+"$"+tempDataName+"<-"+rDataName);
				pageLocations.clear();
				pages.clear();
				pendingPages.clear();
				populateMetaData();
				if(strSame!=null && !((REXPLogical)strSame).isTRUE()[0])
					this.fireTableStructureChanged();
				if(strSame!=null)
					this.fireTableDataChanged();			

				changed=true;
			}
		}
		return changed;
	}
	
	public String getColumnName(int col){
		if(col<getRealColumnCount()){
			REXP colName = Deducer.eval("colnames("+rDataName+")["+(col+1)+"]");
			if(colName!=null && colName.isString())
				try {
					return colName.asString();
				} catch (REXPMismatchException e) {
					return "?";
				}
			else
				return "?";
		}else{
			return "";
		}
	}
	
	public boolean isCellEditable(int rowIndex, int columnIndex){
		if(columnIndex<=getRealColumnCount())
			return true;
		else
			return false;
	}
	/**
	 * 		Deletes the cached data frame from the gui environment
	 */
	public void removeCachedData(){
		boolean envDefined = ((REXPLogical)Deducer.eval("'"+guiEnv+"' %in% .getOtherObjects()")).isTRUE()[0];
		
		if(!envDefined){
			Deducer.eval(guiEnv+"<-new.env(parent=emptyenv())");
		}
		boolean tempStillExists = false;
		REXP tmp = Deducer.eval("exists('"+tempDataName+"',where="+guiEnv+",inherits=FALSE)");
		if(tmp instanceof REXPLogical)
			tempStillExists = ((REXPLogical)tmp).isTRUE()[0];
		if(tempStillExists)
			Deducer.eval("rm("+tempDataName+",envir="+guiEnv+")");		
	}
	
	protected void finalize() throws Throwable {
		removeCachedData();
		super.finalize();
	}
	
	class RowNamesModel extends RowNamesListModel{
		
		private String[] rowNames = null;
		private int maxChar = -1;
		
		public int getSize() { 
			return getRowCount(); 
		}
		
		public Object getElementAt(int index) {
			if(rowNames == null){
				refresh();
			}
			if(index<getRealRowCount()){
				if(rowNames.length>index)
					return rowNames[index];
				else{
					return "?";
				}
			}else
				return new Integer(index+1).toString();
		}
		
		public void initHeaders(int n){}
		
		public int getMaxNumChar(){
			if(maxChar<0)
				refresh();
			return maxChar;
		}
		public void refresh(){
			if(rDataName==null){
				rowNames = new String[]{};
				super.refresh();
				return;
			}
			REXP exist = Deducer.eval("!inherits(try(eval(parse(text=\""+Deducer.addSlashes(rDataName)+
				"\")),silent=TRUE),'try-error')");
			if(exist==null || !((REXPLogical)exist).isTRUE()[0]){
				rowNames = new String[]{};
				super.refresh();
				return;				
			}
			try {
				rowNames = Deducer.eval("rownames("+rDataName+")").asStrings();
			} catch (REXPMismatchException e) {
				//new ErrorMsg(e);
				rowNames = new String[]{};
			}
			int max = 0;
			for(int i=0;i<rowNames.length;i++)
				max = Math.max(max,rowNames[i].length());
			maxChar = max;
			super.refresh();
		}
		
		public void setElementAt(int index,Object value){
			if(index >= getRealRowCount())
				return;
			String valueString = null;
			boolean isDouble = false;
			boolean isInteger =false;
			if(value==null)
				return;
			valueString = value.toString().trim();	
			isDouble=true;
			try{
				Double.parseDouble(valueString);
			}catch(Exception e){
				isDouble=false;
			}
			isInteger=true;
			try{
				Integer.parseInt(valueString);
			}catch(Exception ex){
				isInteger=false;
			}

			if(isInteger || isDouble)
				Deducer.eval("rownames("+rDataName+")["+ (index+1) +"] <- " + valueString);
			else
				Deducer.eval("rownames("+rDataName+")["+ (index+1) +"] <- '" + valueString +"'");
			refresh();
		}
	}
	
	public RowNamesListModel getRowNamesModel() { return rowNamesModel;}
	public void setRowNamesModel(RowNamesModel model){rowNamesModel = model;}
	
	
	/**
	 * 		Implements nice printing of NAs, as well as left alignment
	 * 		for strings and right alignment for numbers.
	 * 
	 * @author ifellows
	 *
	 */
	public class RCellRenderer extends ExCellRenderer
	{
		public final Font naFont = new Font("Dialog", Font.PLAIN, 6);
		
		public Component getTableCellRendererComponent(JTable table,
						Object value, boolean selected, boolean focused,
						int row, int column){
			super.getTableCellRendererComponent(table, value,
					selected, focused, row, column);

			if(row<getRealRowCount() || column<getRealColumnCount()){
				if(value.toString().equals(NA_STRING)){
					setHorizontalAlignment(RIGHT);
					setVerticalAlignment(BOTTOM);
					this.setFont(naFont);
					this.setText("NA");
				}else if(column<classes.length && classes[column]<3){
					setHorizontalAlignment(RIGHT);
					setVerticalAlignment(CENTER);						
				}else{
					setHorizontalAlignment(LEFT);	
					setVerticalAlignment(CENTER);					
				}
			}
			
			return this;
		}
	}
	


	
	
}

