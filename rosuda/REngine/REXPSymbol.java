package org.rosuda.REngine;

public class REXPSymbol extends REXP {
	private String name;
	
	public REXPSymbol(String name) {
		super();
		name=(name==null)?"":name;
	}
	
	public boolean isSymbol() { return true; }
	
	public String asString() { return name; }

	public String[] asStringArray() {
		return new String[] { name };
	}
	
	public String toString() {
		return getClass().getName()+"["+name+"]";
	}
}
