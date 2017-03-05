package com.gmail.br45entei.swt.dialog;

import java.util.Collection;

/** @author Brian_Entei */
public abstract class DialogOptions {
	
	/** @param collection The collection of Strings to combine, delimiting with
	 *            '\n'
	 * @return The resulting string */
	public static final String stringCollectionToString(Collection<String> collection) {
		String list = "";
		for(String line : collection) {
			list += line + "\n";
		}
		return list;
	}
	
	/** Default constructor */
	public DialogOptions() {
	}
	
	/** @param copy The DialogOptions object to copy */
	public DialogOptions(DialogOptions copy) {
		this.setFrom(copy);
	}
	
	/** @param c The DialogOptions object to copy */
	public abstract void setFrom(Object c);
	
	@Override
	public abstract int hashCode();
	
	@Override
	public abstract boolean equals(Object obj);
	
}
