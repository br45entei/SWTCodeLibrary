package com.gmail.br45entei.swt.dialog;

/** @author Brian_Entei */
@SuppressWarnings("javadoc")
public class PopupDialogMessage {
	
	public final String title;
	public final String message;
	
	public PopupDialogMessage(String title, String message) {
		this.title = title;
		this.message = message;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.message == null) ? 0 : this.message.hashCode());
		result = prime * result + ((this.title == null) ? 0 : this.title.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(!(obj instanceof PopupDialogMessage)) {
			return false;
		}
		PopupDialogMessage other = (PopupDialogMessage) obj;
		if(this.message == null) {
			if(other.message != null) {
				return false;
			}
		} else if(!this.message.equals(other.message)) {
			return false;
		}
		if(this.title == null) {
			if(other.title != null) {
				return false;
			}
		} else if(!this.title.equals(other.title)) {
			return false;
		}
		return true;
	}
	
}
