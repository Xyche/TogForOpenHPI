package dataStructure;

import java.sql.*;
import java.util.List;

public class textOutline {

	public textOutline() {
		this._time = Time.valueOf("00:00:00");
	}

	public textOutline(String text) {
		this();
		set_text(text);
	}

	public textOutline(String text, int hierarchy) {
		this(text);
		set_hierarchy(hierarchy);
	}

	public textOutline(String text, int hierarchy, int child) {
		this(text, hierarchy);
		set_child(child);
	}

	public textOutline(String text, int hierarchy, int child, int childEnd) {
		this(text, hierarchy, child);
		set_childEnd(childEnd);
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
		if (_text.length() <= 0) {
			this._text = "";
			return;
		}
		// remove possible space at end when setting text
		this._text = _text;
		if (this._text.charAt(this._text.length() - 1) == ' ')
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
		if (this._text.charAt(this._text.length() - 1) == '-')
			this._text = this._text.substring(0, this._text.length() - 1);
	}

	public static counts count(List<textOutline> values) {
		counts c = new counts();
		for (textOutline text : values) {
			if (text.get_child() == 0)
				continue;

			if (text.get_child() == 1)
				c.count1++;
			else
				c.count2++;

			char firstChar = text.get_text().charAt(0), secondChar = text.get_text().charAt(1),
					thirdChar = text.get_text().charAt(2);

			if (Character.isLowerCase(firstChar))
				if (text.get_text().length() > 3 && Character.isWhitespace(secondChar)
						&& !Character.isUpperCase(thirdChar))
					if (text.get_child() == 1)
						c.count1low++;
					else
						c.count2low++;

			if (text.get_text().length() > 3 && Character.isWhitespace(secondChar))
				if (text.get_child() == 1)
					c.count1dot++;
				else
					c.count2dot++;
		}

		return c;
	}

	public static class counts {
		public int count1 = 1, count2 = 1, count1low = 0, count2low = 0, count1dot = 0, count2dot = 0;
		
		private double ratio(double a, double b){
			return b == 0 ? 0 : 100 * a / b;
		}

		public double lev1TopicCaseStartRatio(){
			return ratio(count1low, count1);
		}

		public double lev2TopicCaseStartRatio(){
			return ratio(count2low, count2);
		}
		
		public double topicCaseStartRatio(){
			return ratio(countLowSum(), countSum());
		}
		
		public double topicWithDotRatio(){
			return ratio(countDotSum(), countSum());
		}
		
		public double lev1WithDotRatio(){
			return ratio(count1dot, count1);
		}
		
		public double lev2WithDotRatio(){
			return ratio(count2dot, count2);
		}
		
		public int countSum() {
			return count1 + count2;
		}
		public int countLowSum() {
			return count1low + count2low;
		}
		public int countDotSum() {
			return count1dot + count2dot;
		}
	}

}
