package dataStructure;

import java.util.*;

import helper.enums.TextLineType;

import java.sql.*;
import sharedMethods.*;

public class textLine {
	
	@Override
	public String toString() {
		return String.format("%d: \"%s\" @ (%d : %d)|(%d x %d)", _slideID, _text, _top, _left, _width, _height);
	}
	
	public textLine(){}
	
	public textLine(int slideID, String text, TextLineType type, int top, int left, int width, int height, Time time){
		set_slideID(slideID);
		set_text(text);
		set_type(type);
		set_top(top);
		set_left(left);
		set_width(width);
		set_height(height);
		set_time(time);
		
		set_bottom(top + height);
		set_lastLineWidth(width);
		set_lastLineLeft(left);
	}
	
	
	/* 'bottom' will be calculated from other parameters, all others derive from DB
	 * 
	 * And for type, introduction below:
	 * -2 : too high text, maybe a combination of several lines, used only in helping locating the title candidates.
	 * -1 : text cannot be recognized, used only in locating title candidates and calculating the average height.
	 *  *** these 2 kind above will never be included in the text system, and will be definitely deleted.
	 *  0 : common text in OCR.
	 *  1 : 'Title' in OCR.
	 *  2 : 'Subtitle' in OCR.
	 *  3 : 'Footline' in OCR.
	 */
	
	private int _slideID;
	private String _text;
	private TextLineType _type;
	private int _top;
	private int _bottom;
	private int _left;
	private int _width;
	private int _lastLineWidth;
	private int _lastLineLeft;
	private int _height;
	private int _count = 1;
	private Time _time = new Time(0);

	
	//'set' and 'get' functions
	
	public int get_slideID() {
		return _slideID;
	}

	public void set_slideID(int _slideID) {
		this._slideID = _slideID;
	}

	public String get_text() {
		return _text;
	}

	public void set_text(String _text) {
		this._text = _text;
	}

	public TextLineType get_type() {
		return _type;
	}

	public void set_type(TextLineType _type) {
		this._type = _type;
	}

	public int get_top() {
		return _top;
	}

	public void set_top(int _top) {
		this._top = _top;
	}
	
	public int get_bottom() {
		return _bottom;
	}

	public void set_bottom(int _bottom) {
		this._bottom =_bottom;
	}

	public int get_left() {
		return _left;
	}

	public void set_left(int _left) {
		this._left = _left;
	}

	public int get_width() {
		return _width;
	}

	public void set_width(int _width) {
		this._width = _width;
	}

	public int get_height() {
		return _height;
	}

	public void set_height(int _height) {
		this._height = _height;
	}
	
	public int get_count() {
		return _count;
	}

	public void set_count(int _count) {
		this._count = _count;
	}
	
	//other functions below
	
	public Time get_time() {
		return _time;
	}

	public void set_time(Time _time) {
		this._time = _time;
	}

	public int get_lastLineWidth() {
		return _lastLineWidth;
	}

	public void set_lastLineWidth(int _lastLineWidth) {
		this._lastLineWidth = _lastLineWidth;
	}

	public int get_lastLineLeft() {
		return _lastLineLeft;
	}

	public void set_lastLineLeft(int _lastLineLeft) {
		this._lastLineLeft = _lastLineLeft;
	}

	public void repair() {
		/*use this function to improve the OCR result, some obviously meaningless text will be replaced or deleted
		
		the textline will be divided into words,
		and those short words(1 or 2 chars) will be sent into test
		if the neighboring words are also short, it will be deleted
		
		and for those short textline built by short words,
		if the average length of them is shorter than 2,
		it will be replaced by #NoUseString#, to be deleted in next step.. 
		
		#NoUseString# generated in this function will have a correlated 'Type' as -1 */
		
		if(this._text.length() > 0 && this._text.charAt(0) == ' ')
		{
			int count = 0;
			for(; count < this._text.length(); count++)
				if(this._text.charAt(count) != ' ')
					break;
			if(count != this._text.length())
			{
				int diff = (this.get_width() / (this._text.length() > 1 ? this._text.length() : 1) ) * count;
				this.set_width(this.get_width() - diff);
				this.set_left(this.get_left() + diff);
				this.set_lastLineLeft(this.get_left());
				this.set_text(_text.substring(count));
			}
		}
		
		//replace some symbols, 'shorts' contains those short word, while 'delete' contains those will be removed
		this._text = this._text.replace("  ", " ");
		this._text = this._text.replace(',', ' ');
		algorithmInterface ai = new algorithmInterface();
		for(int i = 1; i < this._text.length()-1; i++)
		{
			if(this._text.charAt(i) == ' ')
				if(this._text.charAt(i-1) >= '0' && this._text.charAt(i-1) <= '9' && this._text.charAt(i+1) == '%')
				{
					String a = this._text.substring(0, i);
					String b = this._text.substring(i+1);
					this._text = a + b;
					i--;
					continue;
				}
			
			if(this._text.charAt(i) == '.' && i > 1 && i < this._text.length()-2)
				if(this._text.charAt(i-1) == ' ' && this._text.charAt(i+1) == ' ')
					if(this._text.charAt(i-2) >= '0' && this._text.charAt(i-2) <= '9')
						if(this._text.charAt(i+2) >= '0' && this._text.charAt(i+2) <= '9')
						{
							String a = this._text.substring(0, i-1);
							String b = this._text.substring(i+2);
							this._text = a + "." + b;
							i--;
							continue;
						}
		}
		
		String[] words = this._text.split(" ");
		ArrayList<Integer> shorts = new ArrayList<Integer>();
		ArrayList<Integer> delete = new ArrayList<Integer>();
		int count = 0;
		int length = 0;
		String result = "";
				
		for(int i = 0; i < this._text.length(); i++ )
		{			
			if(ai.isSignal(this._text.charAt(i)))
				count++;
		}
		if(this._text.length() > 8 && count * 1.67 > this._text.length())
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		else if(this._text.length() > 3 && count+1 >= this._text.length())
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		count = 0;
		
		//calculate average length of words, the whole string will be further removed
		//if the average length is too short to seemed to be meaningful
		
		int count2 = 0;
		for (String w : words) {
			if(w.length() <= 2) 
				shorts.add(count);
			count++;
			length += w.length();
			if(ai.is_aA(w))
				count2++;				
		}
		
		if(count2 * 2 > words.length)
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		
		double averageLength = (double)length / (double)count ;
		
		if(averageLength < 2 && words.length > 2)
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		else if(averageLength < 2.1)
		{
			if(words.length == 1 && ai.isArabicNum(words[0]))
				return;
			else if(words.length == 2 && ai.isArabicNum(words[0]) && ai.isArabicNum(words[1]))
			{
				set_text(words[0] + '.' + words[1]);
				return;
			}
			else if(words.length == 3 && ai.isArabicNum(words[0]) && words[1].contentEquals(".") && ai.isArabicNum(words[2]))
			{
				set_text(words[0] + '.' + words[2]);
				return;
			}
			else
			{
				count2 = 0;
				for(String w : words) {
					if(isSpecial(w))
						count2 ++;
				}
				if(count2 < words.length / 2 || words.length == 1)
				{
					//set_text("#NoUseString#");
					set_type(TextLineType.CANNOT_RECOGNIZE);
					return;
				}				
			}
		}
		else if(averageLength < 2.51 && words.length > 5)
		{
			count2 = 0;
			for(String w : words) {
				if(isSpecial(w))
					count2 ++;
			}
			if(count2 < words.length / 3)
			{
				//set_text("#NoUseString#");
				set_type(TextLineType.CANNOT_RECOGNIZE);
				return;
			}
		}
		else if(averageLength > 10 && words.length > 3)
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		
		if(ai.containTooManySameLetter(this._text))
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		
		//deal with the short words
		for(int i = 0; i < shorts.size(); i++)
		{
			/* for 1-character word, if it locates in the very beginning,
			 * b~z will be signed as no-use, and for others, it depends 
			 * on the context (3 words in consideration), consistent  
			 * short words will be more likely to be deleted 
			 * and, some special short word, such as "is" will be treated differently */
			if(isSpecial(words[shorts.get(i)]) || (shorts.get(i) < words.length - 1 && isSpecial(words[shorts.get(i) + 1])))
				continue;
			else if(words[shorts.get(i)].length() <= 0)
				delete.add(shorts.get(i));
			else if(words[shorts.get(i)].length() <= 1)
			{
				if(shorts.get(i) == 0)
				{/*
					if(words.length >= 2 && words[1].length() <= 1)
						delete.add(shorts.get(i));
					else if(words.length >= 2 && words[shorts.get(i)].charAt(0) != 'A' && words[1].charAt(0) >= 'A' && words[1].charAt(0) <= 'Z')
						delete.add(shorts.get(i));*/
				}				
				else if(words.length >=3)
				{
					if(shorts.get(i) == words.length - 1)
					{
						if(words[shorts.get(i)-1].length() <= 1)
							delete.add(shorts.get(i));
					}
					else
					{
						if(words[shorts.get(i)+1].length() + words[shorts.get(i)-1].length() <= 4)
							delete.add(shorts.get(i));
					}
				}
			}
			/* for 2-character word, it depends on the context
			 * (5 words in consideration), consistent short words will be more likely 
			 * to be deleted */
			else
			{
				if(words.length >= 5)
				{
					if(words[shorts.get(i)].length() == 2)
					{
						if(shorts.get(i) == 0)
						{
							if(words[shorts.get(i)+1].length() + words[shorts.get(i)+2].length() <= 4)
								delete.add(shorts.get(i));
						}
						else if(shorts.get(i) == 1)
						{
							if(words[shorts.get(i)+1].length() + words[shorts.get(i)+2].length() + words[shorts.get(i)-1].length()<= 6)
								delete.add(shorts.get(i));
						}
						else if(shorts.get(i) == words.length - 1)
						{
							if(words[shorts.get(i)-1].length() + words[shorts.get(i)-2].length() <= 4)
								delete.add(shorts.get(i));
						}
						else if(shorts.get(i) == words.length - 2)
						{
							if(words[shorts.get(i)+1].length() + words[shorts.get(i)-2].length() + words[shorts.get(i)-1].length()<= 6)
								delete.add(shorts.get(i));
						}
						else
						{
							if(words[shorts.get(i)+1].length() + words[shorts.get(i)-1].length() + words[shorts.get(i)-2].length() + words[shorts.get(i)+2].length() <= 8)
								delete.add(shorts.get(i));
						}
					}
				}
			}
		}
		
		
		
		//remove those 'meaningless' short words from the String
		
		for(int j = 0; j < words.length; j++) 
		{
			boolean retain = true;
			for(int i = 0; i < delete.size(); i++)
			{
				if(delete.get(i) == j)
				{
					retain = false;
					//delete.remove(i);
					break;
				}
			}
			
			if(retain)
			{
				if(words[j].length() >= 30)
					retain = false;
				else if(words[j].length() > 15)
				{
					count = 0;
					for(int i = 0; i < words[j].length(); i++)
						if(ai.isSignal(words[j].charAt(i)))
							count++;
					if(count * 4 > words[j].length())
						retain = false;
				}
			}
			
			if(retain == true)
				result = result + words[j] + " ";
			//LoggerSingleton.info(result);
		}
		
		//remove potential space at final position in the String
		
		if(result.length() > 0 && this._text.charAt(0) == ' ')
		{
			count = 0;
			for(; count < this._text.length(); count++)
				if(this._text.charAt(count) != ' ')
					break;
			if(count != this._text.length())
				result = result.substring(count);
			else
			{
				//set_text("#NoUseString#");
				set_type(TextLineType.CANNOT_RECOGNIZE);
				return;
			}
		}
		
		if(result.length() > 0 && result.charAt(result.length() - 1) == ' ')
			result = result.substring(0, result.length() - 1);
		
		if(result.length() == 0)
		{
			//set_text("#NoUseString#");
			set_type(TextLineType.CANNOT_RECOGNIZE);
			return;
		}
		
		set_text(result);
	}
	
	public boolean isSpecial(String a) {
		
		String[] dic = {"is", "a", "on", "at", "in", "do", "to", "I"};		
		for(int i = 0; i < dic.length; i++)
			if(a.equalsIgnoreCase(dic[i]))
				return true;
		
		return false;
	}
	
	public boolean isSame(textLine that, int pageWidth, int pageHeight) {
		
		double wp = (double)pageWidth / 1024;
		double hp = (double)pageHeight / 768;
		
		if(this.get_left() + this.get_width() < 300*wp || this.get_left() > 750*wp || this.get_bottom() < 75*hp || this.get_top() > 600*hp)
			if(this.get_left() > that.get_left() - 5*wp && this.get_left() < that.get_left() + 5*wp)
				if(this.get_top() > that.get_top() - 5*hp && this.get_top() < that.get_top() + 5*hp)
					if(this.get_height() > that.get_height() - 10*hp && this.get_height() < that.get_height() + 10*hp)
						if(this.get_width() > that.get_width() - 10*wp && this.get_width() < that.get_width() + 10*wp)
						{
							algorithmInterface ai = new algorithmInterface();
							int longer = this.get_text().length() > that.get_text().length() ? this.get_text().length() : that.get_text().length();
							int le = ai.getLevenshteinDistance(this.get_text(), that.get_text());
							if(le * 1.5 < longer)
								return true;
						}
		
		
		return false;
	}
	
	public boolean isSamePosition(textLine that, int pageWidth, int pageHeight, boolean isStrict)
	{
		double wp = (double)pageWidth / 1024;
		double hp = (double)pageHeight / 768;
		
		if(isStrict)
		{
			if(this.get_left() > that.get_left() - 5*wp && this.get_left() < that.get_left() + 5*wp)
				if(this.get_top() > that.get_top() - 5*hp && this.get_top() < that.get_top() + 5*hp)
					if(this.get_height() > that.get_height() - 10*hp && this.get_height() < that.get_height() + 10*hp)
						if(this.get_width() > that.get_width() - 10*wp && this.get_width() < that.get_width() + 10*wp)
							return true;
		}
		else
		{
			int overlappHeight = this.get_height() + that.get_height() - (Math.max(this.get_bottom(), that.get_bottom()) - Math.min(this.get_top(), that.get_top()));
			int overlappWidth = this.get_width() + that.get_width() - (Math.max(this.get_left() + this.get_width(), that.get_left() + that.get_width()) - Math.min(this.get_left(), that.get_left()));
			if(overlappHeight > 0 && overlappWidth > 0)
				if(overlappHeight * overlappWidth > Math.min(this.get_height() * this.get_width(), that.get_height() * that.get_width()) * 0.75
				&& overlappHeight * overlappWidth > Math.max(this.get_height() * this.get_width(), that.get_height() * that.get_width()) * 0.25)
					return true;
		}
		
		
		if(this.get_left() - 2*wp <= that.get_left())
			if(this.get_left() + this.get_width() + 2*wp >= that.get_left() + that.get_width())
				if(this.get_top() - 2*hp <= that.get_top())
					if(this.get_top() + this.get_height() + 2*hp >= that.get_top() + that.get_height())
						return true;
		
		return false;
	}
	
	public boolean isInside(int[] area)
	{
		if(this.get_left() >= area[0])
			if(this.get_left() + this.get_width() <= area[1])
				if(this.get_top() >= area[2])
					if(this.get_top() + this.get_height() <= area[3])
						return true;
		
		return false;
	}
	
	public boolean isAwayFrom(int[] area)
	{
		if(this.get_left() + this.get_width() < area[0])
			return true;
		if(this.get_left() > area[1])
			return true;
		if(this.get_top() + this.get_height() < area[2])
			return true;
		if(this.get_top() > area[3])
			return true;
		
		return false;
	}

 	public boolean isSameTitlePosition(textLine that, int pageWidth, int pageHeight)
	{
		double wp = (double)pageWidth / 1024;
		double hp = (double)pageHeight / 768;
		
		if(this.get_left() > that.get_left() - 5*wp && this.get_left() < that.get_left() + 5*wp)
			if((this.get_top() > that.get_top() - 8*hp && this.get_top() < that.get_top() + 8*hp) || (this.get_bottom() > that.get_bottom() - 8*hp && this.get_bottom() < that.get_bottom() + 8*hp))
				if(this.get_height() > that.get_height() - 15*hp && this.get_height() < that.get_height() + 15*hp)
					return true;
		
		return false;
	}
 	
 	public boolean isSameTitlePosition(textLine area, int pageWidth, int pageHeight, boolean centered)
 	{
 		double wp = (double)pageWidth / 1024;
		double hp = (double)pageHeight / 768;
 		
		if(centered)
		{
			if(Math.abs(this.get_left() + this.get_width() / 2 - area.get_left() - area.get_width() / 2) < 5*wp)
				if(Math.abs(this.get_top() - area.get_top()) < 8*hp || Math.abs(this.get_bottom() - area.get_bottom()) < 8*hp)
					if(Math.abs(this.get_height() - area.get_height()) < 15*hp)
						return true;
		}
		else
		{
			if(Math.abs(this.get_left() - area.get_left()) < 5*wp)
				if(Math.abs(this.get_top() - area.get_top()) < 8*hp || Math.abs(this.get_bottom() - area.get_bottom()) < 8*hp)
					if(Math.abs(this.get_height() - area.get_height()) < 15*hp)
						return true;
		}
 		
 		return false;
 	}

	public boolean isInSameRow(textLine that)
	{
		if(this.get_height() > that.get_height() * 3 || that.get_height() > this.get_height() * 3)
			return false;
		
		if(this.get_top() <= that.get_top())
		{
			if(this.get_bottom() < that.get_top())
				return false;
			else
			{
				if(this.get_bottom() >= that.get_bottom())
					return true;
				else if(that.get_bottom() - this.get_bottom() < this.get_bottom() - that.get_top())
					return true;			
			}
		}
		else
		{
			if(this.get_top() > that.get_bottom())
				return false;
			else
			{
				if(this.get_bottom() < that.get_bottom())
					return true;
				else if(this.get_bottom() - that.get_bottom() < that.get_bottom() - this.get_top())
					return true;
			}
		}
		
		return false;
	}
	
	public boolean isInSameTableRow(textLine that)
	{
		if(this.get_height() >= that.get_height() * 1.5 || that.get_height() >= this.get_height() * 1.5)
			return false;
		
		if(this.get_top() <= that.get_top())
		{
			if(this.get_bottom() < that.get_top())
				return false;
			else
			{
				if(this.get_bottom() >= that.get_bottom())
					return true;
				else
				{
					if(this.get_bottom() - that.get_top() > (this.get_height() > that.get_height() ? that.get_height() : this.get_height()) * 0.9)
						return true;
				}
			}
		}
		else
		{
			if(this.get_top() > that.get_bottom())
				return false;
			else
			{
				if(this.get_bottom() < that.get_bottom())
					return true;
				else
				{
					if(that.get_bottom() - this.get_top() > (that.get_height() > this.get_height() ? this.get_height() : that.get_height()) * 0.9)
						return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean[] isInSameColumn(textLine that, boolean considerVerticalGap, boolean considerWidthDifference)
	{
		boolean[] aligns = {false, false, false};
		int threshold = 3;
		
		if(Math.abs(this.get_top() - that.get_top()) > Math.max(this.get_height(), that.get_height()) * 12)
			return aligns;
		
		if(considerVerticalGap)
		{
			if(Math.abs(this.get_top() - that.get_top()) > Math.max(this.get_height(), that.get_height()) * 10)
				return aligns;
			else if(Math.abs(this.get_top() - that.get_top()) > Math.max(this.get_height(), that.get_height()) * 7)
				threshold = 0;
			else if(Math.abs(this.get_top() - that.get_top()) > Math.max(this.get_height(), that.get_height()) * 5)
				threshold = 1;
			else if(Math.abs(this.get_top() - that.get_top()) > Math.max(this.get_height(), that.get_height()) * 3)
				threshold = 2;
			else if(Math.abs(this.get_top() - that.get_top()) < Math.max(this.get_height(), that.get_height()) * 2)
				threshold = 5;
		}
		
		if(considerWidthDifference)
		{
			if(Math.max(this.get_width(), that.get_width()) > Math.min(this.get_width(), that.get_width()) * 10)
				threshold = threshold - 3;
			else if(Math.max(this.get_width(), that.get_width()) > Math.min(this.get_width(), that.get_width()) * 7)
				threshold = threshold - 2;
			else if(Math.max(this.get_width(), that.get_width()) > Math.min(this.get_width(), that.get_width()) * 5)
				threshold = threshold - 1;
		}
		
		if(Math.abs(this.get_left() - that.get_left()) <= threshold)
			aligns[0] = true;
		if(Math.abs(this.get_left() + this.get_width() - that.get_left() - that.get_width()) <= threshold)
			aligns[1] = true;
		if(Math.abs(this.get_left() + this.get_width()/2 - that.get_left() - that.get_width()/2) <= threshold * 2)
			aligns[2] = true;
		
		return aligns;
	}

	public boolean isBlockInitials() {
		return 
			Character.isUpperCase(this.get_text().charAt(0)) && 
			this.get_text().length() > 1 && 
			Character.isUpperCase(this.get_text().charAt(1));
	}
}
