package dataStructure;

import java.sql.*;

public class textOutline {
	
	public textOutline() {}
	
	public textOutline(String text) {
		set_text(text);
		this._time = Time.valueOf("00:00:00");
	}
	
	public textOutline(String text, int hierarchy) {
		set_text(text);
		set_hierarchy(hierarchy);
		this._time = Time.valueOf("00:00:00");
	}
	
	public textOutline(String text, int hierarchy, int child) {
		set_text(text);
		set_hierarchy(hierarchy);
		set_child(child);
		this._time = Time.valueOf("00:00:00");
	}
	
	public textOutline(String text, int hierarchy, int child, int childEnd) {
		set_text(text);
		set_hierarchy(hierarchy);
		set_child(child);
		set_childEnd(childEnd);
		this._time = Time.valueOf("00:00:00");
	}
	
	
	private String _text = "";
	private int _hierarchy = -1;
	private int _child = -1;
	private int _childEnd = -1;
	private Time _time = new Time(0);
	
	public String get_text() {
		return _text;
	}

	public void set_text(String _text) {
		if(_text.length() <= 0)
		{
			this._text = "";
			return;
		}
		//remove possible space at end when setting text
		this._text = _text;
		if(this._text.charAt(this._text.length() - 1) == ' ')
			this._text = this._text.substring(0, this._text.length() - 1);
	}

	public int get_hierarchy() {
		return _hierarchy;
	}

	public void set_hierarchy(int _hierarchy) {
		this._hierarchy = _hierarchy;
	}
	
	public int get_child() {
		return _child;
	}

	public void set_child(int _child) {
		this._child = _child;
	}

	public int get_childEnd() {
		return _childEnd;
	}

	public void set_childEnd(int _childEnd) {
		this._childEnd = _childEnd;
	}

	public Time get_time() {
		return _time;
	}

	public void set_time(Time _time) {
		this._time = _time;
	}

	public void add_text(String extra) {
		this._text = this._text + " " + extra;
		if(this._text.charAt(this._text.length() - 1) == '-')
			this._text = this._text.substring(0, this._text.length() - 1);
	}


}
