package org.rosuda.deducer.widgets.param;

import org.rosuda.deducer.Deducer;

public class ParamAny extends Param{
	
	protected String value;
	protected String defaultValue;			//default	
	
	final public static String VIEW_ENTER = "enter";
	final public static String VIEW_ENTER_LONG = "enter long";
	
	public ParamAny(){
		name = "";
		title = "";
		value = "";
		defaultValue = "";
		view = VIEW_ENTER_LONG;
	}
	
	public ParamAny(String nm){
		name = nm;
		title = nm;
		value = "";
		defaultValue = "";
		view = VIEW_ENTER_LONG;
	}
	
	public ParamAny(String theName, String theTitle, String theView,
			String theValue,String theDefaultValue){
		name = theName;
		title = theTitle;
		view = theView;
		value = theValue;
		defaultValue = theDefaultValue;
		view = VIEW_ENTER_LONG;
	}
	
	public ParamWidget getView(){
		return new ParamTextFieldWidget(this);
	}
	
	public Object clone(){
		ParamAny p = new ParamAny();
		p.setName(this.name);
		p.setTitle(this.title);
		p.setValue(this.value);
		p.setDefaultValue(this.defaultValue);
		p.setViewType(this.getViewType());
		return p;
	}
	
	public String[] getParamCalls(){
		String[] calls;
		if(value!=null && !value.equals(defaultValue)){
			String val = "";
			if(value.toString().length()>0){
				try{
					Double.parseDouble(value.toString());
					if(!defaultValue.toString().equals(value.toString()))
						val = value.toString();
					else
						val = "";
				}catch(Exception e){
					val = "'" + Deducer.addSlashes(value.toString()) + "'";
				}
			}
			if(val.length()>0)
				calls = new String[]{name + " = "+val};
			else
				calls = new String[]{};
			
		}else
			calls = new String[]{};
		return calls;
	}

	public void setDefaultValue(Object defaultValue) {
		if(defaultValue instanceof String || value==null)
			this.defaultValue = (String) defaultValue;
		else
			System.out.println("ParamAny: invalid setDefaultValue");
	}
	public Object getDefaultValue() {
		return defaultValue;
	}
	public void setValue(Object value) {
		if(value instanceof String || value==null)
			this.value = (String) value;
		else
			System.out.println("ParamAny: invalid setValue");
	}
	public Object getValue() {
		return value;
	}
	
}
