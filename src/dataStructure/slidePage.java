package dataStructure;

import java.util.*;

import helper.Constants;
import helper.FilterableList;
import helper.FilterableList.FilterFunc;
import helper.LoggerSingleton;
import helper.StaticMethods;
import helper.enums.SlidePageType;
import helper.enums.TextLineType;
import sharedMethods.algorithmInterface;

import java.io.*;
import java.sql.*;

public class slidePage {

	@Override
	public String toString() {
		return String.format("\"%s\" - %s", this._title, this._startTime.toString());
	}

	public slidePage() {
	}

	public slidePage(FilterableList<textLine> list, outlineGenerator og) throws IOException {
		if (og.isInitial())
			init(list, og.get_pageWidth(), og.get_pageHeight());
		else
			init(list, og.get_pageWidth(), og.get_pageHeight(), og.get_potentialTitleArea(),
					og.get_potentialHierarchicalGap(), og.is_beginWithLowCaseLetter(), og.is_haveSignBeforeSubtopic(),
					og.get_lectureID());
	}
	
	/**
	 * 'sign' useless texts: those cannot be recognized or with
	 * several lines binded(too high), and repair those partly useless
	 * texts. And all these signed textLines will be deleted: before create
	 * textOutlines and after seek a title.
	 *
	 * All the texts will be first recursively repaired until there's
	 * nothing to change and then the auto-combined text lines will also be
	 * ruled out Finally deleted all those #NoUseString# after set the page
	 * number
	 */
	private double signUselessText(ArrayList<textLine> textLines){
		double textHeightAverage = 0;
		for(textLine line: textLines){
			textHeightAverage += (double) line.get_height();
			String lastVersion = "";
			while (!lastVersion.contentEquals(line.get_text())) {
				lastVersion = line.get_text();
				line.repair();
			}
		}
		return textHeightAverage /= textLines.size();
	}
	
	private void init(FilterableList<textLine> textLines, int pageWidth, int pageHeight, ArrayList<int[]> potentialTitleArea,
			ArrayList<Integer> Gaps, boolean lowCaseStart, boolean extraSignStart, String lectureID) throws IOException{
		// This constructor will be used for a raw page: common use

		this.set_pageWidth(pageWidth);
		this.set_pageHeight(pageHeight);
		double wp = (double) pageWidth / Constants.DEFAULT_WIDTH;
		double hp = (double) pageHeight / Constants.DEFAULT_HEIGHT;
		this._titleLocation[0] = pageWidth;
		this._titleLocation[2] = pageHeight;

		if (textLines.isEmpty()) {
			LoggerSingleton.info("Empty List, cannot do the slidePage initialization...");
			return;
		}

		// Sort split textlines in same row

		for(textLine line: textLines){
			int j = textLines.indexOf(line) + 1;
			for(textLine otherLine: textLines.subList(j, textLines.size())){
				if (line.isInSameRow(otherLine))
					continue;
				else{
					j = textLines.indexOf(otherLine);
					break;
				}
			}
			for(textLine tl1: textLines.subList(textLines.indexOf(line), j))
				for(textLine tl2: textLines.subList(textLines.indexOf(tl1) + 1, j))
					if (tl2.get_left() < tl1.get_left())
						Collections.swap(textLines, textLines.indexOf(tl1), textLines.indexOf(tl2));
		}

		// In adaptive round, deleting potential extra signs before subtopics in
		// the beginning
		if (extraSignStart)
			for(textLine line: textLines)
				if (line.get_type().isInvalid())
					continue;
				else
					if (line.get_text().length() <= 3)
						continue;
					else {
						String lineText = line.get_text(); 
						Character first = lineText.charAt(0), second =lineText.charAt(1), third = lineText.charAt(2);
						if (first != ' ' && second == ' ' && third != ' ') {
							if ((first == 'a' || first == 'A') && !lowCaseStart && Character.isLowerCase(third)) continue;

							String temp = lineText.substring(2);
							int diff = 2 * line.get_width() / lineText.length();
							line.set_text(temp);
							line.set_left(line.get_left() + diff);
							line.set_width(line.get_width() - diff);
							line.set_lastLineLeft(line.get_left());
							line.set_count(-1);
						}
					}

		
		double textHeightAverage = signUselessText(textLines);

		// Second, set page number: all texts should be with one single same
		// number
		set_PageNum(textLines.get(0).get_slideID());
		set_startTime(textLines.get(0).get_time());

		// Here make a sign to those 'too large' or 'too small' text, but
		// temporarily retain them
		for(textLine line: textLines){
			int h = line.get_height();
			if (
			(h >= textHeightAverage * 2 && h >= 60 * hp) || 
			(h >= textHeightAverage * 4 && h >= 35 * hp) ||
			(h <= 6 * hp))
				line.set_type(TextLineType.TOO_HIGH);
			else if (h <= 8 * hp)
				line.set_type(TextLineType.INVALID);
		}

		// Next, find the title from texts and delete the textlines for title
		for (int i: seekTitleWithPTA(textLines, potentialTitleArea)) 
			textLines.set(i, null);

		textLines.notNullObjects(true);

		// Now, DELETE those #NoUseString# based on content
		textLines.filter(new FilterFunc<Boolean, textLine>() {
			public Boolean call() { return true; }
			public Boolean call(textLine line) { return line.get_type() != TextLineType.CANNOT_RECOGNIZE; }
		}, true);
		
		for(final int[] area: detectTable(textLines, wp, hp, lectureID))
			textLines.filter(new FilterFunc<Boolean, textLine>() {
				public Boolean call() { return true; }
				public Boolean call(textLine line) { return !line.isInside(area); }
			}, true);

		// now, DELETE those #NoUseString# based on size, remain those with
		// height 9 or 10 for middleline detection

		textLines.filter(new FilterFunc<Boolean, textLine>() {
			public Boolean call() { return true; }
			public Boolean call(textLine line) { return line.get_type() != TextLineType.TOO_HIGH; }
		}, true);
		
		// Next, load those text left
		loadTextInHierarchyAdaptively(textLines, Gaps, lowCaseStart, extraSignStart);

		// Finally, judge whether most of the texts inside the slide included in
		// the 3-level system.
		isSlideWellOrganized();
	}

	private void init(FilterableList<textLine> textLines, int pageWidth, int pageHeight) {

		// This constructor will be used for a raw page: common use

		this.set_pageWidth(pageWidth);
		this.set_pageHeight(pageHeight);
		// double wp = (double)pageWidth / Constants.DEFAULT_WIDTH;
		double hp = (double) pageHeight / Constants.DEFAULT_HEIGHT;
		this._titleLocation[0] = pageWidth;
		this._titleLocation[2] = pageHeight;

		if (textLines.isEmpty()) {
			LoggerSingleton.info("Empty List, cannot do the slidePage initialization...");
			return;
		}

		double textHeightAverage = signUselessText(textLines);

		for(textLine line: textLines){
			int h = line.get_height();
			if (
			(h >= textHeightAverage * 2 && h >= 60 * hp) ||
			(h >= textHeightAverage * 3 && h >= 35 * hp) ||
			(h <= 8 * hp))
				line.set_type(TextLineType.TOO_HIGH);
		}

		// Second, set page number: all texts should be with one single same
		// number
		set_PageNum(textLines.get(0).get_slideID());
		set_startTime(textLines.get(0).get_time());

		// Third, find the title from texts and delete the textlines for title
		for (int i: seekTitle(textLines)) 
			textLines.set(i, null);

		textLines.notNullObjects(true);

		// Then, DELETE those #NoUseString#
		textLines.filter(new FilterFunc<Boolean, textLine>() {
			public Boolean call() { return true; }
			public Boolean call(textLine line) { return line.get_type().isCommon(); }
		}, true);

		// Next, load those text left
		loadTextInHierarchy(textLines);

		// Finally, judge whether most of the texts inside the slide included in
		// the 3-level system.
		isSlideWellOrganized();
	}

	public slidePage(int pageNum, SlidePageType pageType, String title, int hierarchy, ArrayList<textOutline> list,
			int pageWidth, int pageHeight) {
		// This constructor will be used for a prepared page
		this.set_PageNum(pageNum);
		this.set_pageType(pageType);
		this.set_title(title);
		this.set_texts(list);
		this.set_hierarchy(hierarchy);
		this.set_pageWidth(pageWidth);
		this.set_pageHeight(pageHeight);
	}

	public slidePage(FilterableList<textLine> list, int pageWidth, int pageHeight, ArrayList<int[]> potentialTitleArea,
			ArrayList<Integer> Gaps, boolean lowCaseStart, boolean extraSignStart, String lectureID)
					throws IOException {
		init(list, pageWidth, pageHeight, potentialTitleArea, Gaps, lowCaseStart, extraSignStart, lectureID);
	}

	public slidePage(FilterableList<textLine> list, int pageWidth, int pageHeight) {
		init(list, pageWidth, pageHeight);
	}

	/*
	 * 'pageNum' is the unique symbol of a slide, maybe not continuous, but
	 * absolutely no repeat
	 *
	 * 'pageType' will be explained below: -3: Absolutely empty slide. -2:
	 * There's only a title or a unique text in this slide, maybe this is a
	 * picture slide. -1: Most of the texts in this slide haven't been
	 * organized. 0: Common well-organized slide 1: Index slide 2: Tag slide 3:
	 * Split slide
	 */

	private int _pageNum = -1;
	private SlidePageType _pageType = SlidePageType.MOST_TEXT_UNORGANIZED;
	private String _title = "";
	private int _hierarchy = 0;
	private ArrayList<textOutline> _texts = new ArrayList<textOutline>();
	private int _pageWidth = Constants.DEFAULT_WIDTH;
	private int _pageHeight = Constants.DEFAULT_HEIGHT;
	private Time _startTime = new Time(0);

	// 4 parameters in _titleLocation: left, right, top, bottom
	private int[] _titleLocation = { -1, -1, -1, -1 };
	private int[] _levelCoordinates = { -1, -1, -1 };
	private int _middleLine = -1;

	public int get_PageNum() {
		return _pageNum;
	}

	public void set_PageNum(int _pageNum) {
		this._pageNum = _pageNum;
	}

	public SlidePageType get_pageType() {
		return _pageType;
	}

	public void set_pageType(SlidePageType _pageType) {
		this._pageType = _pageType;
	}

	public int get_pageWidth() {
		return _pageWidth;
	}

	public void set_pageWidth(int _pageWidth) {
		this._pageWidth = _pageWidth;
	}

	public int get_pageHeight() {
		return _pageHeight;
	}

	public void set_pageHeight(int _pageHeight) {
		this._pageHeight = _pageHeight;
	}

	public String get_title() {
		return _title;
	}

	public void set_title(String _Title) {
		this._title = _Title;
	}

	public int get_hierarchy() {
		return _hierarchy;
	}

	public void set_hierarchy(int _hierarchy) {
		this._hierarchy = _hierarchy;
	}

	public ArrayList<textOutline> get_texts() {
		return _texts;
	}

	public void set_texts(ArrayList<textOutline> texts) {
		this._texts.clear();
		this._texts = texts;
	}

	public Time get_startTime() {
		return _startTime;
	}

	public void set_startTime(Time _time) {
		this._startTime = _time;
	}

	public int[] get_titleLocation() {
		return _titleLocation;
	}

	public void set_titleLocation(int[] _titleLocation) {
		this._titleLocation = _titleLocation;
	}

	public void set_titleLocation(int left, int top, int right, int bottom) {
		this._titleLocation[0] = left;
		this._titleLocation[1] = top;
		this._titleLocation[2] = right;
		this._titleLocation[3] = bottom;
	}

	public int get_middleLine() {
		return _middleLine;
	}

	public void set_middleLine(int _middleLine) {
		this._middleLine = _middleLine;
	}

	public int[] get_levelCoordinates() {
		return _levelCoordinates;
	}

	public void set_levelCoordinates(int[] _levelCoordinates) {
		this._levelCoordinates = _levelCoordinates;
	}

	// Real Functions

	public void contentCopy(slidePage s) {
		this.set_PageNum(s.get_PageNum());
		this.set_pageType(s.get_pageType());
		this.set_hierarchy(s.get_hierarchy());
		this.set_title(s.get_title());
		this.set_pageHeight(s.get_pageHeight());
		this.set_pageWidth(s.get_pageWidth());
		this.set_startTime(s.get_startTime());
		this.set_texts(new ArrayList<>(s.get_texts()));

	}

	private ArrayList<Integer> seekTitle(ArrayList<textLine> list) {
		/*
		 * In this function, up to 3 textLines will be picked out and construct
		 * the title of this slide. The decision covers the position, size and
		 * type of the textLines derived from the OCR result.
		 */

		String title = "";
		ArrayList<Integer> titleCandidates = new ArrayList<Integer>();

		double wp = (double) this.get_pageWidth() / 1024;
		double hp = (double) this.get_pageHeight() / 768;

		double averageHeight = 0;
		double biggest = 0;
		double shortest = this.get_pageHeight();
		int count = 0;

		/*
		 * Calculate the averageHeight of all the textLines, including those
		 * 'unrecognized' #NoUseString#, but without those 'binded' ones. The
		 * result will be further used as a decisive factor to judge whether a
		 * textLines is 'big' enough.
		 */
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get_type() == TextLineType.TOO_HIGH)
				continue;
			count++;
			averageHeight += (double) list.get(i).get_height();
			if (list.get(i).get_height() > biggest)
				biggest = list.get(i).get_height();
			if (list.get(i).get_height() < shortest)
				shortest = list.get(i).get_height();
		}

		if (count <= 0)
			return titleCandidates;
		else
			averageHeight = averageHeight / count;

		/*
		 * Firstly, search all the textLines with the type set as 1 (Title). It
		 * is a attribute derived directly from OCR process, based on how
		 * 'Block' the characters are, which in this project we will never know.
		 * Those textLines chosen should meet requirements below:
		 *
		 * 1. Shouldn't locate in the very top (10 as strict line and 20 as
		 * compromised line) 2. If it's not the first one selected, it shouldn't
		 * be vertically too far from the previous chosen one. It means: the
		 * distance from the previous chosen textLine should be smaller than the
		 * distance from the actual next text line( to avoid the situation that
		 * the next text is in the same vertical line). 3. The measurement 'far'
		 * will be different on occasion that whether 'the next text' is also
		 * set as 'title', if yes, the measurement will be easy; or else, it
		 * will be more strict. 4. If the current textLine is much bigger than
		 * the one in the next line, choose it.
		 */

		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get_type().isNotCommon())
				continue;

			if (list.get(i).get_type() == TextLineType.COMMON_TEXT && titleCandidates.size() <= 2
					&& (list.get(i).get_top() >= 20 * hp || (list.get(i).get_top() >= 10 * hp
							&& (list.get(i).get_height() >= (averageHeight + biggest) / 2
									|| list.get(i).get_height() >= 30 * hp)))) {
				if (titleCandidates.size() == 0)
					titleCandidates.add(i);
				else {
					int j = titleCandidates.get(titleCandidates.size() - 1);
					int aboveDistance = list.get(i).get_top() - list.get(j).get_bottom();
					int downDistance = aboveDistance;
					for (int k = 1; i + k < list.size(); k++) {
						if (list.get(i + k).get_top() - list.get(i).get_bottom() > 0) {
							downDistance = list.get(i + k).get_top() - list.get(i).get_bottom();
							j = i + k;
							break;
						}
					}
					if (aboveDistance < 0)
						titleCandidates.add(i);
					else if (aboveDistance <= 75) {
						if (list.get(j).get_type() == TextLineType.COMMON_TEXT) {
							if (aboveDistance < downDistance + 3 * hp)
								titleCandidates.add(i);
						} else {
							if (aboveDistance < downDistance * 0.5 || aboveDistance < downDistance - 20 * hp
									|| list.get(j).get_height() * 2 <= list.get(i).get_height()
									|| list.get(j).get_height() + 15 * hp <= list.get(i).get_height())
								titleCandidates.add(i);
						}
					}
				}
			}
		}
		String info = list.get(0).get_slideID() + ": ";
		for (int i = 0; i < titleCandidates.size(); i++)
			info += titleCandidates.get(i) + "-" + list.get(titleCandidates.get(i)).get_text() + " ";
		LoggerSingleton.info(info);

		/*
		 * Then, repair the choose.
		 *
		 * If some title candidates have already been selected, search all the
		 * text upper than the candidates, and change the candidates when an
		 * upper text has a big enough height and locates left or middle.
		 *
		 * If there's no candidate yet (mean no suitable 'title' text in the
		 * slide), search others from the beginning, find the first big, up and
		 * left one as the first title candidate.
		 *
		 * After this step, there's only 3 possibilities: 1. There are several
		 * candidates (up to 3) all with the type as 1(title). 2. There is only
		 * 1 single candidate with the type not to be 1(title). 3. No candidate
		 * -> in this case, title search is over and this slide will have no
		 * title.
		 */
		if (titleCandidates.size() > 0) {
			if (titleCandidates.get(0) > 0) {
				for (int i = 0; i < titleCandidates.get(0); i++) {
					if (list.get(i).get_type().isNotCommon())
						continue;

					if ((list.get(i).get_height() >= (averageHeight + biggest) / 2
							|| list.get(i).get_height() >= 30 * hp) && list.get(i).get_left() < 700 * wp
							&& list.get(i).get_top() >= 10 * hp) {
						titleCandidates.clear();
						if (i > 0 && list.get(i - 1).get_bottom() - list.get(i).get_top() >= 0) {
							if (list.get(i).get_left() < list.get(i - 1).get_left()) {
								if (list.get(i).get_left() + list.get(i).get_width() + 100 * wp > list.get(i - 1)
										.get_left())
									titleCandidates.add(i - 1);
								else
									titleCandidates.add(i);
							} else if (list.get(i - 1).get_left() < list.get(i).get_left()) {
								if (list.get(i - 1).get_left() + list.get(i - 1).get_width() + 100 * wp > list.get(i)
										.get_left() && list.get(i - 1).get_type().isCommon())
									titleCandidates.add(i - 1);
								else
									titleCandidates.add(i);
							}
						} else
							titleCandidates.add(i);
						break;
					}
				}
			}
		} else {
			for (int i = 0; i < list.size() && list.get(i).get_top() < 256 * hp; i++) {
				if (list.get(i).get_type().isNotCommon())
					continue;

				if ((list.get(i).get_height() >= (averageHeight + shortest) / 2 || list.get(i).get_height() > 25 * hp)
						&& list.get(i).get_left() < 700 * wp && list.get(i).get_top() >= 10 * hp) {
					titleCandidates.clear();
					if (i > 0 && list.get(i - 1).get_bottom() - list.get(i).get_top() < 0
							&& list.get(i - 1).get_type().isCommon())
						titleCandidates.add(i - 1);
					else
						titleCandidates.add(i);
					break;
				}
			}
		}
		info = list.get(0).get_slideID() + ": ";
		for (int i = 0; i < titleCandidates.size(); i++)
			info += titleCandidates.get(i) + "-" + list.get(titleCandidates.get(i)).get_text() + " ";
		LoggerSingleton.info(info);

		/*
		 * In this step, more text down to the previous title candidates will be
		 * considered. If the textLine is closed to the candidate and away from
		 * the next line of text enough, or the textLine is in the same line of
		 * the previous candidate and not separated so far horizontally, it will
		 * be adopted as the next candidate. Up to 3 candidates allowed.
		 */
		if (titleCandidates.size() > 0 && titleCandidates.size() < 3) {
			for (int i = titleCandidates.get(titleCandidates.size() - 1) + 1; i < list.size()
					&& titleCandidates.size() < 3; i++) {
				if (list.get(i).get_type().isNotCommon())
					continue;

				int j = titleCandidates.get(titleCandidates.size() - 1);
				int aboveDistance = list.get(i).get_top() - list.get(j).get_bottom();
				int downDistance = aboveDistance;
				for (int k = 1; i + k < list.size(); k++) {
					if (list.get(i + k).get_top() - list.get(i).get_bottom() > 0) {
						downDistance = list.get(i + k).get_top() - list.get(i).get_bottom();
						break;
					}
				}
				if (aboveDistance < 0 && list.get(i).get_left() < 700 * wp) {
					if (list.get(i).get_left() < list.get(j).get_left()) {
						if (list.get(i).get_left() + list.get(i).get_width() + 100 * wp > list.get(j).get_left())
							titleCandidates.add(i);
						else if (list.get(i).get_left() + list.get(i).get_width() + 200 * wp > list.get(j).get_left()
								&& list.get(i).get_height() >= (averageHeight + biggest) / 2)
							titleCandidates.add(i);
					} else if (list.get(j).get_left() < list.get(i).get_left()) {
						if (list.get(j).get_left() + list.get(j).get_width() + 100 * wp > list.get(i).get_left())
							titleCandidates.add(i);
						else if (list.get(j).get_left() + list.get(j).get_width() + 200 * wp > list.get(i).get_left()
								&& list.get(i).get_height() >= (averageHeight + biggest) / 2)
							titleCandidates.add(i);
					}
				} else if ((aboveDistance < downDistance * 0.5 || aboveDistance < downDistance - 20 * hp)
						&& aboveDistance < 40 * hp)
					if ((list.get(i).get_left() >= list.get(j).get_left()
							&& list.get(i).get_left() <= list.get(j).get_left() + list.get(j).get_width())
							|| (list.get(j).get_left() >= list.get(i).get_left()
									&& list.get(j).get_left() <= list.get(i).get_left() + list.get(i).get_width()))
						titleCandidates.add(i);
			}
		}

		info = list.get(0).get_slideID() + ": ";
		for (int i = 0; i < titleCandidates.size(); i++)
			info += titleCandidates.get(i) + "-" + list.get(titleCandidates.get(i)).get_text() + " ";
		LoggerSingleton.info(info);

		// bubble-reorder the candidates to make sure final title generated in
		// right order
		ArrayList<Integer> textForTitle = new ArrayList<Integer>();
		for (int i = 0; i < titleCandidates.size(); i++)
			textForTitle.add(titleCandidates.get(i));
		for (int i = 0; i < textForTitle.size(); i++) {
			for (int j = i + 1; j < textForTitle.size(); j++) {
				int xi = textForTitle.get(i);
				int xj = textForTitle.get(j);
				if (list.get(xi).get_top() - list.get(xj).get_bottom() >= 0) {
					textForTitle.set(i, xj);
					textForTitle.set(j, xi);
				} else {
					if (list.get(xi).get_bottom() - list.get(xj).get_top() >= 0
							&& list.get(xi).get_left() > list.get(xj).get_left() + list.get(xj).get_width()) {
						textForTitle.set(i, xj);
						textForTitle.set(j, xi);
					}
				}
			}
		}

		// generate the title and set the attribute of title_bottom
		for (int i = 0; i < textForTitle.size(); i++) {
			int j = textForTitle.get(i);
			title = title + list.get(j).get_text() + " ";
			if (list.get(j).get_left() < this._titleLocation[0])
				this._titleLocation[0] = list.get(j).get_left();
			if (list.get(j).get_left() + list.get(j).get_width() > this._titleLocation[1])
				this._titleLocation[1] = list.get(j).get_left() + list.get(j).get_width();
			if (list.get(j).get_top() < this._titleLocation[2])
				this._titleLocation[2] = list.get(j).get_top();
			if (list.get(j).get_bottom() > this._titleLocation[3])
				this._titleLocation[3] = list.get(j).get_bottom();
		}

		if (title != "" && title.charAt(title.length() - 1) == ' ')
			title = title.substring(0, title.length() - 1);

		set_title(title);

		return titleCandidates;
	}

	private ArrayList<Integer> seekTitleWithPTA(ArrayList<textLine> list, ArrayList<int[]> potentialTitleArea) {
		String title = "";
		ArrayList<Integer> titleCandidates = new ArrayList<Integer>();

		double wp = (double) this.get_pageWidth() / 1024;
		double hp = (double) this.get_pageHeight() / 768;

		int averageHeight = 0;
		int biggest = 0;
		int shortest = this.get_pageHeight();
		int count = 0;

		/*
		 * Calculate the averageHeight of all the textLines, including those
		 * 'unrecognized' #NoUseString#, but without those 'binded' ones. The
		 * result will be further used as a decisive factor to judge whether a
		 * textLines is 'big' enough.
		 */
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get_type() == TextLineType.TOO_HIGH)
				continue;
			count++;
			averageHeight += (double) list.get(i).get_height();
			if (list.get(i).get_height() > biggest)
				biggest = list.get(i).get_height();
			if (list.get(i).get_height() < shortest)
				shortest = list.get(i).get_height();
		}

		if (count <= 0)
			return titleCandidates;
		else
			averageHeight = averageHeight / count;

		/*
		 * Search from the beginning, find the first big, up and left one as the
		 * first title candidate. If there is/are potential title area(s)
		 * achieved from previous round, search this/these area(s) first!
		 *
		 * After this step, there's only 2 possibilities: 1. There is only 1
		 * single candidate with the type not to be 1(title). 2. No candidate ->
		 * in this case, title search is over and this slide will have no title.
		 */

		for (int i = 0; i < list.size() && list.get(i).get_top() < 256 * hp; i++) {
			if (list.get(i).get_type().isNotCommon())
				continue;

			if ((list.get(i).get_height() >= (averageHeight + shortest) / 2 || list.get(i).get_height() > 25 * hp)
					&& list.get(i).get_left() < 700 * wp && list.get(i).get_top() >= 10 * hp) {
				if (i > 0 && list.get(i - 1).get_bottom() - list.get(i).get_top() < 0
						&& list.get(i - 1).get_type().isCommon() && list.get(i - 1).get_left() < 700 * wp
						&& list.get(i - 1).get_top() >= 10 * hp)
					titleCandidates.add(i - 1);
				else
					titleCandidates.add(i);
				break;
			}
		}

		for (int i = 0; i < list.size() && list.get(i).get_top() < 256 * hp && potentialTitleArea.size() > 0; i++) {
			boolean done = false;
			for (int k = 0; k < potentialTitleArea.size(); k++) {
				int[] pta = potentialTitleArea.get(k);
				if (pta.length != 4)
					continue;
				textLine t = new textLine();
				t.set_top(pta[0]);
				t.set_height(pta[1]);
				t.set_bottom(pta[0] + pta[1]);
				t.set_type(TextLineType.fromInt(pta[2]));
				boolean centered = false;
				if (pta[2] == 0) {
					t.set_left(pta[3]);
					t.set_width(this._pageWidth / 3);
					centered = false;
				} else {
					t.set_left(pta[3] / 2);
					t.set_width(pta[3]);
					centered = true;
				}

				if (list.get(i).isSameTitlePosition(t, this._pageWidth, this._pageHeight, centered)) {
					if (titleCandidates.size() == 0) {
						titleCandidates.add(i);
						done = true;
						break;
					} else {
						textLine current = list.get(titleCandidates.get(0));
						if (Math.min(current.get_left() + current.get_width(),
								list.get(i).get_left() + list.get(i).get_width())
								- Math.max(current.get_left(), list.get(i).get_left()) < 0) {
							titleCandidates.clear();
							titleCandidates.add(i);
						}
						done = true;
						break;
					}
				}
			}
			if (done)
				break;
		}

		String info = list.get(0).get_slideID() + ": ";
		for (int i = 0; i < titleCandidates.size(); i++)
			info += titleCandidates.get(i) + "-" + list.get(titleCandidates.get(i)).get_text() + " ";
		LoggerSingleton.info(info);

		/*
		 * In this step, more text down to the previous title candidates will be
		 * considered. If the textLine is closed to the candidate and away from
		 * the next line of text enough, or the textLine is in the same line of
		 * the previous candidate and not separated so far horizontally, it will
		 * be adopted as the next candidate. Up to 3 candidates allowed.
		 */
		if (titleCandidates.size() > 0 && titleCandidates.size() < 3) {
			for (int i = titleCandidates.get(titleCandidates.size() - 1) + 1; i < list.size()
					&& titleCandidates.size() < 3; i++) {
				if (list.get(i).get_type().isNotCommon())
					continue;

				int j = titleCandidates.get(titleCandidates.size() - 1);
				int aboveDistance = list.get(i).get_top() - list.get(j).get_bottom();
				int downDistance = aboveDistance;
				for (int k = 1; i + k < list.size(); k++) {
					if (list.get(i + k).get_top() - list.get(i).get_bottom() > 0) {
						downDistance = list.get(i + k).get_top() - list.get(i).get_bottom();
						break;
					}
				}

				// Please note "inPTA" here means the current title is already
				// in title position, no further row needed.
				boolean inPTA = false;
				for (int k = 0; k < potentialTitleArea.size(); k++) {
					int[] pta = potentialTitleArea.get(k);
					if (pta.length != 4)
						continue;
					textLine t = new textLine();
					t.set_top(pta[0]);
					t.set_height(pta[1]);
					t.set_bottom(pta[0] + pta[1]);
					t.set_type(TextLineType.fromInt(pta[2]));
					boolean centered = false;
					if (pta[2] == 0) {
						t.set_left(pta[3]);
						t.set_width(this._pageWidth / 3);
						centered = false;
					} else {
						t.set_left(pta[3] / 2);
						t.set_width(pta[3]);
						centered = true;
					}

					for (int l = 0; l <= j; l++) {
						if (list.get(l).isSameTitlePosition(t, this._pageWidth, this._pageHeight, centered)) {
							if (list.get(i).get_top() > t.get_top() + t.get_height()) {
								inPTA = true;
								break;
							}
						}
					}
					if (inPTA)
						break;
				}
				if (inPTA)
					break;

				if (aboveDistance < 0 && list.get(i).get_left() < 768 * wp) {
					if (list.get(i).get_left() < list.get(j).get_left()) {
						if (list.get(i).get_left() + list.get(i).get_width() + 100 * wp > list.get(j).get_left())
							titleCandidates.add(i);
						else if (list.get(i).get_left() + list.get(i).get_width() + 200 * wp > list.get(j).get_left()
								&& list.get(i).get_height() >= (averageHeight + biggest) / 2)
							titleCandidates.add(i);
					} else if (list.get(j).get_left() < list.get(i).get_left()) {

						if (list.get(j).get_left() + list.get(j).get_width() + 100 * wp > list.get(i).get_left())
							titleCandidates.add(i);
						else if (list.get(j).get_left() + list.get(j).get_width() + 200 * wp > list.get(i).get_left()
								&& list.get(i).get_height() >= (averageHeight + biggest) / 2)
							titleCandidates.add(i);
					}
				} else if ((aboveDistance < downDistance * 0.5 || aboveDistance < downDistance - 20 * hp
						|| aboveDistance <= Math.min(list.get(i).get_height(), list.get(j).get_height()))
						&& aboveDistance < 40 * hp)
					if (Math.min(list.get(j).get_left() + list.get(j).get_width(),
							list.get(i).get_left() + list.get(i).get_width())
							- Math.max(list.get(j).get_left(), list.get(i).get_left()) > Math
									.min(list.get(j).get_width(), list.get(i).get_width()) * 0.66)
						titleCandidates.add(i);
			}
		}

		info = list.get(0).get_slideID() + ": ";
		for (int i = 0; i < titleCandidates.size(); i++)
			info += titleCandidates.get(i) + "-" + list.get(titleCandidates.get(i)).get_text() + " ";
		LoggerSingleton.info(info);

		// bubble-reorder the candidates to make sure final title generated in
		// right order
		ArrayList<Integer> textForTitle = new ArrayList<Integer>();
		for (int i = 0; i < titleCandidates.size(); i++)
			textForTitle.add(titleCandidates.get(i));
		for (int i = 0; i < textForTitle.size(); i++) {
			for (int j = i + 1; j < textForTitle.size(); j++) {
				int xi = textForTitle.get(i);
				int xj = textForTitle.get(j);
				if (list.get(xi).get_top() - list.get(xj).get_bottom() >= 0) {
					textForTitle.set(i, xj);
					textForTitle.set(j, xi);
				} else {
					if (list.get(xi).get_bottom() - list.get(xj).get_top() >= 0
							&& list.get(xi).get_left() > list.get(xj).get_left() + list.get(xj).get_width()) {
						textForTitle.set(i, xj);
						textForTitle.set(j, xi);
					}
				}
			}
		}

		// generate the title and set the attribute of title_bottom
		for (int i = 0; i < textForTitle.size(); i++) {
			int j = textForTitle.get(i);
			title = title + list.get(j).get_text() + " ";
			if (list.get(j).get_left() < this._titleLocation[0])
				this._titleLocation[0] = list.get(j).get_left();
			if (list.get(j).get_left() + list.get(j).get_width() > this._titleLocation[1])
				this._titleLocation[1] = list.get(j).get_left() + list.get(j).get_width();
			if (list.get(j).get_top() < this._titleLocation[2])
				this._titleLocation[2] = list.get(j).get_top();
			if (list.get(j).get_bottom() > this._titleLocation[3])
				this._titleLocation[3] = list.get(j).get_bottom();
		}

		if (title != "" && title.charAt(title.length() - 1) == ' ')
			title = title.substring(0, title.length() - 1);

		set_title(title);

		return titleCandidates;
	}

	private void loadTextInHierarchy(FilterableList<textLine> lines) {
		if (lines.isEmpty())
			return;

		double wp = (double) this.get_pageWidth() / Constants.DEFAULT_WIDTH;
		double hp = (double) this.get_pageHeight() / Constants.DEFAULT_HEIGHT;

		/*
		 * In this function, all the textLines left (after deleting the no-use
		 * and for-title) will be load into textOntlines. First those long texts
		 * occupied 2 vertical lines or more will be combined, and the (x, y) of
		 * combined textLined will be updated Then, do the loading process
		 */
		for(textLine line: lines.subList(1, lines.size())){
			if(line == null) continue;
			textLine previousLine = lines.get(lines.indexOf(line) - 1);
			if(previousLine == null) continue;

//		for (int i = 1; i < lines.size(); i++) {
			if (isTextContinued(lines, line)) {
				if ((line.get_top() < previousLine.get_top() + 7 * hp
						&& line.get_top() > previousLine.get_top() - 7 * hp)
						|| (line.get_bottom() < previousLine.get_bottom() + 7 * hp
								&& line.get_bottom() > previousLine.get_bottom() - 7 * hp)) {
					if (line.get_left() < previousLine.get_left())
						Collections.swap(lines, lines.indexOf(line), lines.indexOf(previousLine));
					
					previousLine
							.set_width(line.get_left() + line.get_width() - previousLine.get_left());
					previousLine.set_lastLineWidth(previousLine.get_width());
					int height = previousLine.get_height() > line.get_height() ? previousLine.get_height()
							: line.get_height();
					previousLine.set_height(height);

				} else {
					int right = previousLine.get_left() + previousLine.get_width() < line.get_left()
							+ line.get_width() ? line.get_left() + line.get_width()
									: previousLine.get_left() + previousLine.get_width();
					previousLine.set_width(right - previousLine.get_left());
					previousLine.set_lastLineWidth(line.get_lastLineWidth());
					int height = previousLine.get_height() > line.get_height() ? previousLine.get_height()
							: line.get_height();
					previousLine.set_height(height);
					previousLine.set_bottom(line.get_bottom());
				}

				previousLine.set_text(previousLine.get_text() + " " + line.get_text());
				lines.set(lines.indexOf(line), null);
			}
		}

		if (this.get_title().length() == 0) {
			if (lines.size() > 3) {
				for (int i = 0; i < lines.size(); i++) {
					if (lines.get(i).get_left() > 10 * wp
							&& lines.get(i).get_left() + lines.get(i).get_width() < 950 * wp) {
						this.set_title(lines.get(i).get_text());
						this._titleLocation[0] = lines.get(i).get_left();
						this._titleLocation[1] = lines.get(i).get_left() + lines.get(i).get_width();
						this._titleLocation[2] = lines.get(i).get_top();
						this._titleLocation[3] = lines.get(i).get_bottom();
						lines.remove(i);
						break;
					}
				}
			}
		}

		this.set_texts(new ArrayList<>(createTextOutlines(lines)));
	}

	private void loadTextInHierarchyAdaptively(FilterableList<textLine> list, ArrayList<Integer> Gaps, boolean lowCaseStart,
			boolean extraSignStart) {
		if (list.isEmpty())
			return;

		double wp = (double) this.get_pageWidth() / Constants.DEFAULT_WIDTH;

		// Do the text-lines combination in same row only
		this.connectContinuousTextlineInSameRowOnly(list, Gaps, lowCaseStart, extraSignStart);

		// Try to find potential middle axis, which represent 2-column slide
		// layout.
		this.set_middleLine(this.searchHorizontalMiddleLine(list, wp));
		LoggerSingleton.info("MiddleLine = " + this.get_middleLine());

		if (this.get_middleLine() < 0) {
			for (int i = 1; i < list.size() - 2; i++) {
				FilterableList<textLine> subList = list.slice(1, list.size() - 2);
				int subMiddleLine = searchHorizontalMiddleLine(subList, wp);
				if (subMiddleLine < this._pageWidth / 3 || subMiddleLine > this._pageWidth * 0.75)
					continue;
				else if (Math.abs(list.get(i).get_left() - list.get(i + 1).get_left()) <= 50)
					continue;
				else if (list.get(i + 1).get_top() > this._pageHeight * 0.75)
					continue;
				else {
					LoggerSingleton.info("$$$$ Above-Left-Right");
					set_texts(refinedTextOutlineLoading(list.slice(0, i + 1), -1, Gaps, lowCaseStart, extraSignStart));
					LoggerSingleton.info("Sub-Middleline = " + subMiddleLine);
					get_texts().addAll(refinedTextOutlineLoading(subList, subMiddleLine, Gaps, lowCaseStart, extraSignStart));
					return;
				}
			}

			for (textLine line: list.slice(2, list.size())){
				textLine prevLine = list.previous(line);
				FilterableList<textLine> subList = list.slice(0, list.indexOf(list));

				int subMiddleLine = searchHorizontalMiddleLine(subList, wp);
				if (subMiddleLine < this._pageWidth / 3 || subMiddleLine > this._pageWidth * 0.75)
					continue;
				else if (Math.abs(line.get_left() - prevLine.get_left()) <= 50)
					continue;
				else if (line.get_top() < this._pageHeight * 0.33)
					continue;
				else {
					LoggerSingleton.info("$$$$ Left-Right-Bottom");
					LoggerSingleton.info("Sub-Middleline = " + subMiddleLine);
					set_texts(refinedTextOutlineLoading(subList, subMiddleLine, Gaps, lowCaseStart, extraSignStart));
					get_texts().addAll(refinedTextOutlineLoading(list.slice(list.indexOf(line), list.size()), -1, Gaps, lowCaseStart, extraSignStart));
					return;
				}
			}

			for (textLine current : list.subList(0, list.size() - 1)) {
				textLine next = list.next(current);
				if (current == null || current.get_top() < this.get_titleLocation()[3] || next == null)
					continue;

				for (textLine line: list.slice(list.indexOf(next), list.size()).reversed()){
					textLine prevLine = list.get(list.indexOf(line) - 1);
					ArrayList<textLine> subList = list.slice(list.indexOf(next), list.indexOf(line));

					if (searchHorizontalMiddleLine(subList, wp) > 0)
						if (
							Math.abs(current.get_left() - next.get_left()) > 50 || 
							next.get_top() - current.get_bottom() > Math.min(current.get_height(), next.get_height()) * 2)
							if (
								Math.abs(line.get_left() - prevLine.get_left()) > 50 || 
								line.get_top() - prevLine.get_bottom() > Math.min(line.get_height(), prevLine.get_height()) * 2) {
								LoggerSingleton.info("Remove some diagram content. [ " + subList.get(0).get_top() + ", "
										+ subList.get(subList.size() - 1).get_bottom() + " ] " + subList.size()
										+ " items removed");
								list.setAll(list.indexOf(next), list.indexOf(line), null);
								break;
							}
				}
			}

			this.set_texts(refinedTextOutlineLoading(list.notNullObjects(true), this.get_middleLine(), Gaps, lowCaseStart, extraSignStart));
		} else
			this.set_texts(refinedTextOutlineLoading(list.notNullObjects(true), this.get_middleLine(), Gaps, lowCaseStart, extraSignStart));
	}

	private ArrayList<textOutline> adjustColumnTextSystem(ArrayList<textOutline> list) {
		/*
		 * This function is applied when a slide page has more than one
		 * sub-text-system. If a independent sub-text-system contain too many
		 * textlines in first level, make them as a whole with only the first
		 * textline remaining in first level. All others' hierarchy will be plus
		 * by 1.
		 */
		if (list.size() < 4)
			return list;

		int allLevel = 0, firstLevel = 0;
		for (textOutline outline: list)
			if (outline.get_hierarchy() > 0) {
				allLevel++;
				if (outline.get_hierarchy() == 1)
					firstLevel++;
			}
		

		if (firstLevel * 3 > allLevel && allLevel >= 4) {
			boolean first = false, second = false;
			for (textOutline outline: list){
				if (outline.get_hierarchy() == 1)
					if (first)
						second = true;
					else
						first = true;
				if (second) 
					if (outline.get_hierarchy() == 1)
						outline.set_hierarchy(2);
					else if (outline.get_hierarchy() == 2)
						outline.set_hierarchy(3);
					else if (outline.get_hierarchy() == 3)
						outline.set_hierarchy(0);
			}
		}

		return list;
	}

	private ArrayList<textLine> connectContinuousTextlineInSameRowOnly(FilterableList<textLine> list,
			ArrayList<Integer> Gaps, boolean lowCaseStart, boolean extraSignStart) {
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).isInSameRow(list.get(i - 1))) {
				if (isTextContinued(list, list.get(i), Gaps, lowCaseStart, extraSignStart)) {
					if (list.get(i).get_left() < list.get(i - 1).get_left()) {
						textLine t = list.get(i);
						list.set(i, list.get(i - 1));
						list.set(i - 1, t);
					}
					list.get(i - 1)
							.set_width(list.get(i).get_left() + list.get(i).get_width() - list.get(i - 1).get_left());
					list.get(i - 1).set_lastLineWidth(list.get(i - 1).get_width());
					int height = list.get(i - 1).get_height() > list.get(i).get_height() ? list.get(i - 1).get_height()
							: list.get(i).get_height();
					list.get(i - 1).set_height(height);

					list.get(i - 1).set_text(list.get(i - 1).get_text() + " " + list.get(i).get_text());
					list.remove(i);
					i--;
				}
			} else
				continue;
		}

		return list;
	}

	private FilterableList<textLine> connectContinuousTextLineMultiRowsOnly(FilterableList<textLine> list,
			ArrayList<Integer> Gaps, boolean lowCaseStart, boolean extraSignStart) {
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).isInSameRow(list.get(i - 1)))
				continue;
			else {
				if (isTextContinued(list, list.get(i), Gaps, lowCaseStart, extraSignStart)) {
					int right = list.get(i - 1).get_left() + list.get(i - 1).get_width() < list.get(i).get_left()
							+ list.get(i).get_width() ? list.get(i).get_left() + list.get(i).get_width()
									: list.get(i - 1).get_left() + list.get(i - 1).get_width();
					list.get(i - 1).set_width(right - list.get(i - 1).get_left());
					list.get(i - 1).set_lastLineWidth(list.get(i).get_lastLineWidth());
					list.get(i - 1).set_lastLineLeft(list.get(i).get_left());
					int height = list.get(i - 1).get_height() > list.get(i).get_height() ? list.get(i - 1).get_height()
							: list.get(i).get_height();
					list.get(i - 1).set_height(height);
					list.get(i - 1).set_bottom(list.get(i).get_bottom());

					list.get(i - 1).set_text(list.get(i - 1).get_text() + " " + list.get(i).get_text());
					list.remove(i);
					i--;
				}
			}
		}

		return list;
	}

	private boolean isTextContinued(ArrayList<textLine> list, textLine current) {
		double wp = (double) this.get_pageWidth() / Constants.DEFAULT_WIDTH;
		double hp = (double) this.get_pageHeight() / Constants.DEFAULT_HEIGHT;
		textLine prePrevious = null, previous = null, next = null; 
		int i = list.indexOf(current);
		if(i - 1 >= 0) previous = list.get(i - 1);
		if(i - 2 >= 0) prePrevious = list.get(i - 2);
		if(i + 1 < list.size()) next = list.get(i + 1);
		
		if(current == null || previous == null)
			return false;
		else {
			// footline or NoUseString will never be combined, while empty
			// string will definitely be.
			if (current.get_type() == TextLineType.FOOTLINE || current.get_type() == TextLineType.CANNOT_RECOGNIZE)
				return false;
			else if (current.get_text().isEmpty())
				return true;
			else {
				// if two textlines are in same vertical line and horizontally
				// not too far away, combine them.
				int 
					prev_top = previous.get_top(),
					curr_top = current.get_top(),
					
					prev_bot = previous.get_bottom(),
					curr_bot = current.get_bottom(),
					
					prev_hei = previous.get_height(),
					curr_hei = current.get_height(),
				
					prev_lef = previous.get_left(),
					curr_lef = current.get_left(),

					prev_wid = previous.get_width(),
					curr_wid = current.get_width();
				
				if ((curr_top < prev_top + 7 * hp && curr_top > prev_top - 7 * hp) || 
					(curr_bot < prev_bot + 7 * hp && curr_bot > prev_bot - 7 * hp) || 
					(curr_top - prev_top > 0 && curr_top - prev_top < prev_hei / 2) || 
					(prev_top - curr_top > 0 && prev_top - curr_top < curr_hei / 2))
					if (
						(prev_lef < current.get_left() && prev_lef + prev_wid + 100 * wp > curr_lef) || 
						(prev_lef >= current.get_left() && curr_lef + curr_wid + 100 * wp > prev_lef))
						return true;

				/*
				 * for others, four requirements: 1. Vertically near 2. Previous
				 * line begins with BLOCK-LETTER and current line not 3.
				 * Previous line should comparably longer in the context 4.
				 * Horizontally meet the demand
				 */
				int downDistance = next == null ? current.get_top() - previous.get_bottom() : (next.get_top() - current.get_bottom());
				int aboveDistance = current.get_top() - previous.get_bottom();
				if (downDistance < 0)
					for(textLine line: list.subList(Math.min(i + 2, list.size()), list.size()))
						if (line.get_top() - current.get_bottom() > 0){
							downDistance = line.get_top() - current.get_bottom();
							break;
						}

				boolean isAboveBlock, isCurrentBlock;
				if (current.get_text().charAt(0) >= 'A' && current.get_text().charAt(0) <= 'Z')
					isCurrentBlock = true;
				else if (current.get_text().length() > 3 && current.get_text().charAt(1) == ' '
						&& current.get_text().charAt(2) >= 'A' && current.get_text().charAt(2) <= 'Z')
					isCurrentBlock = true;
				else
					isCurrentBlock = false;

				if (previous.get_text().charAt(0) < 'a' || previous.get_text().charAt(0) > 'z')
					isAboveBlock = true;
				else if (previous.get_text().length() > 3 && previous.get_text().charAt(1) == ' '
						&& previous.get_text().charAt(2) >= 'A'
						&& previous.get_text().charAt(2) <= 'Z')
					isAboveBlock = true;
				else
					isAboveBlock = false;

				int count = 2;
				int averageRight = previous.get_lastLineWidth() + current.get_lastLineWidth();
				if (prePrevious != null && 
						prev_top  - prePrevious.get_bottom() < 30 * hp && 
						prev_lef < prePrevious.get_left() + 75 * wp && 
						prev_lef > prePrevious.get_left() - 75 * wp) {
					averageRight += prePrevious.get_lastLineWidth();
					count++;
				}
				if (next != null && 
						downDistance < 30 * hp && 
						next.get_left() < current.get_left() + 75 * wp && 
						next.get_left() > current.get_left() - 75 * wp) {
					averageRight += next.get_lastLineWidth();
					count++;
				}
				averageRight /= count;

				if (isAboveBlock && !isCurrentBlock && 
					aboveDistance < downDistance * 2 && 
					aboveDistance < current.get_height() * 2 && 
					(previous.get_lastLineWidth() >= averageRight - 100 * wp || previous.get_lastLineWidth() + previous.get_left() > 750 * wp) && 
					(
						(current.get_left() > previous.get_left() - 90 * wp && current.get_left() < previous.get_left() - 25 * wp) || 
						(current.get_left() > previous.get_left() + 25 * wp && current.get_left() < previous.get_left() + 50 * wp) ||
						(current.get_left() >= previous.get_left() - 15 * wp && current.get_left() <= previous.get_left() + 15 * wp)
					)) return true;
			}
		}
		return false;
	}

	private boolean isTextContinued(FilterableList<textLine> list, textLine current, ArrayList<Integer> Gaps, boolean lowCaseStart, boolean extraSignStart) {
		textLine previous = list.previous(current), prePrevious = list.previous(previous), next = list.next(current);
		double wp = (double) this.get_pageWidth() / 1024;

		if (list.indexOf(current) <= 0 || previous == null)
			return false;
		else if (current.get_type() == TextLineType.FOOTLINE || current.get_type() == TextLineType.CANNOT_RECOGNIZE)
			return false;
		else if (current.get_text().length() == 0 || current.get_text().contentEquals(" "))
			return true;
		if (current.isInSameRow(previous)) {
			int maxGap = (current.get_width() + previous.get_width()) / (current.get_text().length() + previous.get_text().length()) * 5;
			maxGap = maxGap < 100 * wp ? maxGap : (int) (100 * wp);
			textLine l1, l2;
			if (previous.get_left() < current.get_left()) {
				l1 = previous;
				l2 = current;
			} else {
				l1 = current;
				l2 = previous;
			}

			if (l1.get_left() + l1.get_width() < 512 * wp && l2.get_left() > 512 * wp)
				if (
					Character.isUpperCase(l2.get_text().charAt(0)) ||
					Math.abs(previous.get_width() - current.get_width()) < Math.min(previous.get_width(), current.get_width()) ||
					(previous.get_top() != current.get_top() && previous.get_bottom() != current.get_bottom()))
					return false;
			
			if (l1.get_left() + l1.get_width() + maxGap > l2.get_left())
				return true;
			
		} else {
			/*
			 * for others, 6 factors to consider, quantified to a value: 1.
			 * [-10, 0] : Hierarchical gap (VETO requirement) 2. [ -5, 2] :
			 * Potential extra subtopic sign 3. [ -5, 2] : BLOCK start & low
			 * case START 4. [-10, 2] : Vertical line space 5. [ -8, 2] :
			 * Horizontal start position difference 6. [ -8, 2] : Horizontal
			 * length
			 */
			int p1, p2, p3, p4, p5, p6;

			// 1. Hierarchical gap
			p1 = 0;
			for (int gap: Gaps)
				if (current.get_left() - previous.get_left() >= gap - 3 && current.get_left() - previous.get_left() <= gap + 3){
					p1 = -10;
					break;
				}

			/*
			 * 2. extra subtopic sign
			 *
			 * The 'count' parameter here is used to mark whether there was
			 * a extra subtopic sign removed in the beginning of adaptive
			 * round.
			 *
			 * And when there was one in the current textLine, we believe
			 * there is very rare change for a further combination.
			 *
			 * The only situation to recommend to combine is when above
			 * textLine had a extra sign while current textLine hadn't.
			 */
			p2 = 0;
			if (extraSignStart)
				if (current.get_count() == -1)
					p2 = -5;
				else if (previous.get_count() == -1)
					p2 = 2;
				else
					p2 = 0;
			else
				p2 = 0;

			/*
			 * 3. BLOCK & low case
			 *
			 * Based on the statistics in the last round, one set of slides
			 * can me marked by whether they prone to have low-case letters
			 * in the beginning of the subtopic, which saved in the boolean
			 * parameter 'lowCaseStart'
			 *
			 * Note that we have special treatment of ABBREVIATIONs. When
			 * lowCaseStart is true, we won't mark above textLine with a
			 * ABBREVIATION start as BLOCK to promote a combination. And
			 * whenever the current textLine beginning with ABBREVIATION
			 * will be considered as low case. As a result, only an
			 * ABBREVIATION start in above textLine while lowCaseStart is
			 * false, it can be marked as BLOCK
			 *
			 * Furthermore, if current subtopic looks like
			 * "1 First subtopic", "2 Second subtopic" which start with a
			 * number and followed by a BLOCK letter when lowCaseStart is
			 * false, it will also be considered as a BLOCK start to avoid
			 * being combined.
			 */
			p3 = 0;
			boolean 
				prevBlockInitials = previous.isBlockInitials(),
				currentBlockInitials = current.isBlockInitials();

			char 
				currentChar = StaticMethods.firstCharCase(current, lowCaseStart), 
				prevChar = StaticMethods.firstCharCase(previous, lowCaseStart);

			if (lowCaseStart && prevBlockInitials)
				prevChar = 'a';

			if (currentBlockInitials)
				currentChar = 'a';


			if (lowCaseStart && (Character.isLowerCase(prevChar) || Character.isDigit(prevChar)) && Character.isUpperCase(currentChar))
				p3 = -2;
			else if (lowCaseStart && (Character.isLowerCase(prevChar) || Character.isDigit(prevChar)) && !Character.isUpperCase(currentChar))
				p3 = -1;
			else if (lowCaseStart && !Character.isLowerCase(prevChar) && !Character.isDigit(prevChar) && Character.isUpperCase(currentChar))
				p3 = -2;
			else if (lowCaseStart && !Character.isLowerCase(prevChar) && !Character.isDigit(prevChar) && Character.isLowerCase(currentChar))
				p3 = 1;
			else if (lowCaseStart && !Character.isLowerCase(prevChar) && !Character.isDigit(prevChar) && Character.isDigit(currentChar))
				p3 = 0;
			else if (!lowCaseStart && Character.isUpperCase(prevChar) && Character.isLowerCase(currentChar))
				p3 = 2;
			else if (!lowCaseStart && Character.isUpperCase(prevChar) && Character.isDigit(currentChar))
				p3 = 1;
			else if (!lowCaseStart && Character.isUpperCase(prevChar) && Character.isUpperCase(currentChar))
				p3 = -4;
			else if (!lowCaseStart && Character.isDigit(prevChar) && Character.isLowerCase(currentChar))
				p3 = 1;
			else if (!lowCaseStart && Character.isDigit(prevChar) && Character.isDigit(currentChar))
				p3 = 0;
			else if (!lowCaseStart && Character.isDigit(prevChar) && Character.isUpperCase(currentChar))
				p3 = -4;
			else if (!lowCaseStart && Character.isLowerCase(prevChar) && Character.isLowerCase(currentChar))
				p3 = -1;
			else if (!lowCaseStart && Character.isLowerCase(prevChar) && Character.isDigit(currentChar))
				p3 = -2;
			else if (!lowCaseStart && Character.isLowerCase(prevChar) && Character.isUpperCase(currentChar))
				p3 = -5;



			// 4. Vertical line space
			/*
			 * Here we generally use line space (above and down) to evaluate
			 * how possible neighboring textLines should be combined.
			 *
			 * 1st. if aboveDistance is too large, 3 time of current
			 * textLine height, veto it.
			 *
			 * 2nd. if downDistance or above2Distance is too large, 8/3 of
			 * the height, ignore it because it means there is no
			 * correlation between current textLine and next one, or
			 * pre-previous one.
			 *
			 * 3rd. if aboveDistance is 2 times larger of the current
			 * height, marked as large line space, using a set of
			 * measurement not promoting combination. Or else, promoting it.
			 *
			 * 4th. More combining chance when downDistance is obviously
			 * larger than aboveDistance. While similar, promote it when
			 * above2Distance is larger than aboveDistance.
			 */
			p4 = 0;
			int charAveWidth = current.get_width() / current.get_text().length();
			int aboveDistance = current.get_top() - previous.get_bottom();
			int downDistance = next == null ? aboveDistance : (next.get_top() - current.get_bottom());
			if (downDistance < 0)
				for (textLine line: list.subList(Math.min(list.indexOf(next) + 1, list.size()), list.size()))
					if (line.get_top() - current.get_bottom() > 0) {
						downDistance = line.get_top() - current.get_bottom();
						break;
					}

			if (downDistance > current.get_height() * 2.66)
				downDistance = aboveDistance;
			else if (next != null && Math.abs(next.get_left() - current.get_left()) > 5 * charAveWidth)
				downDistance = aboveDistance;

			if (aboveDistance >= current.get_height() * 3)
				p4 = -10;
			else if (aboveDistance >= current.get_height() * 2.5)
				p4 = -5;
			else if (aboveDistance >= current.get_height() * 2) {
				if (aboveDistance >= downDistance * 2)
					p4 = -5;
				else if (aboveDistance >= downDistance * 1.5)
					p4 = -3;
				else if (downDistance >= aboveDistance * 2)
					p4 = 0;
				else if (downDistance >= aboveDistance * 1.5)
					p4 = -1;
				else {
					int above2Distance = prePrevious == null ? aboveDistance : previous.get_top() - prePrevious.get_top();
					if (above2Distance >= aboveDistance * 2)
						p4 = 0;
					else if (above2Distance >= aboveDistance * 1.5)
						p4 = -1;
					else
						p4 = -2;
				}
			} else {
				if (aboveDistance >= downDistance * 2)
					p4 = -2;
				else if (aboveDistance >= downDistance * 1.5)
					p4 = -1;
				else if (downDistance >= aboveDistance * 2)
					p4 = 2;
				else if (downDistance >= aboveDistance * 1.5)
					p4 = 1;
				else {
					int above2Distance = prePrevious == null ? aboveDistance : previous.get_top() - prePrevious.get_bottom();

					if (above2Distance > current.get_height() * 2.66)
						above2Distance = aboveDistance;
					else if (prePrevious != null && Math.abs(prePrevious.get_left() - previous.get_left()) > 5 * charAveWidth)
						above2Distance = aboveDistance;
					if (above2Distance >= aboveDistance * 2)
						p4 = 2;
					else if (above2Distance >= aboveDistance * 1.5)
						p4 = 1;
					else
						p4 = 0;
				}
			}

			// 5. Start Position
			// Pay attention that when lowCaseStart is true, drop this
			// measurement.
			p5 = 0;
			int startGap = current.get_left() - previous.get_lastLineLeft();

			if (startGap >= 0) {
				if (startGap > 15 * charAveWidth)
					p5 = -8;
				else if (startGap > 10 * charAveWidth)
					p5 = -5;
				else if (startGap > 7 * charAveWidth)
					p5 = -2;
				else if (startGap > 5 * charAveWidth)
					p5 = -1;
				else if (startGap > 3 * charAveWidth)
					p5 = 0;
				else if (startGap > 1 * charAveWidth)
					p5 = 1;
				else
					p5 = 2;
			} else {
				if (startGap < -15 * charAveWidth)
					p5 = -8;
				else if (startGap < -10 * charAveWidth)
					p5 = -5;
				else if (startGap < -7 * charAveWidth)
					p5 = -2;
				else if (startGap < -2 * charAveWidth)
					p5 = 0;
				else if (startGap < -1 * charAveWidth)
					p5 = 1;
				else
					p5 = 2;
			}

			if (lowCaseStart)
				p5 -= 2;

			/*
			 * 6. Length
			 *
			 * When current textLine is longer than above one, generally
			 * there's no chance for a combination especially when the
			 * difference is longer than the first word of current textLine.
			 *
			 * When above textLine is longer, it should be long enough
			 * through all the textLines with their left-edge locating
			 * similar to this above textLine. And the chance of combining
			 * increases when the difference gets larger.
			 *
			 * And when lowCaseStart is true, we prone to combine less even
			 * above textLine is longer.
			 */
			p6 = 0;

			int aboveRight = previous.get_lastLineLeft() + previous.get_lastLineWidth();
			int currentRight = current.get_left() + current.get_width();

			if (aboveRight - currentRight < 0) {
				String[] words = current.get_text().split(" ");
				if (currentRight - aboveRight > charAveWidth * (words[0].length() + 3))
					p6 = -8;
				else if (currentRight - aboveRight > charAveWidth * (words[0].length() + 1))
					p6 = -4;
				else if (currentRight - aboveRight > charAveWidth * (words[0].length() - 1))
					p6 = -2;
				else if (currentRight - aboveRight > charAveWidth * 5)
					p6 = -1;
				else
					p6 = 0;
			} else {
				String[] words = current.get_text().split(" ");
				int rightmostInThisLevel = 0;
				int threshold = lowCaseStart ? 8 : 3;
				for (textLine line: list)
					if (line.get_left() > previous.get_lastLineLeft() - charAveWidth * threshold && 
						line.get_left() < previous.get_lastLineLeft() + charAveWidth * threshold &&
						line.get_left() + line.get_width() > rightmostInThisLevel)
							rightmostInThisLevel = line.get_left() + line.get_width();

				if (rightmostInThisLevel - aboveRight < (words[0].length() + 3) * charAveWidth) {
					if (lowCaseStart) {
						if (aboveRight - currentRight > current.get_width() * 2)
							p6 = 2;
						else if (aboveRight - currentRight > current.get_width())
							p6 = 1;
						else
							p6 = 0;
					} else {
						if (aboveRight - currentRight > current.get_width())
							p6 = 2;
						else if (aboveRight - currentRight > words[0].length() * charAveWidth)
							p6 = 1;
						else
							p6 = 0;
					}
				} else {
					if (lowCaseStart)
						p6 = -5;
					else
						p6 = -2;
				}
			}

			if (p1 + p2 + p3 + p4 + p5 + p6 > 0)
				return true;
		}

		return false;
	}

	private ArrayList<textOutline> refinedTextOutlineLoading(FilterableList<textLine> list, int middleLine,
			ArrayList<Integer> Gaps, boolean lowCaseStart, boolean extraSignStart) {
		ArrayList<textOutline> result = new ArrayList<textOutline>();

		// When there's no middle axis found, directly construct the intra-slide
		// content tree.
		if (middleLine < 0) {
			// Remove too-short text-lines
			int countNum = 0, totalLength = 0, aveLength = 0;
			for (int i = 0; i < list.size(); i++) {
				textLine t = list.get(i);
				algorithmInterface ai = new algorithmInterface();
				totalLength += t.get_text().length();
				if (ai.isOrderNum(t.get_text()) || ai.isStatNum(t.get_text()) || ai.isOptionNum(t.get_text()))
					countNum++;
			}
			aveLength = totalLength / list.size();
			LoggerSingleton.info(
					"%%%% All: aveLength = " + aveLength + " totalItems = " + list.size() + " digitNum = " + countNum);

			if (aveLength < 5 && list.size() > 1) {
				for (int i = list.size() - 1; i >= 0; i--)
					if (list.get(i).get_text().length() < 5)
						list.remove(i);
			} else if (aveLength < 10 && list.size() > 1 && (countNum >= list.size() / 2 || aveLength < list.size())) {
				for (int i = list.size() - 1; i >= 0; i--)
					if (list.get(i).get_text().length() < 10)
						list.remove(i);
			}

			// remove all those textlines with height 9 or 10
			for (int i = list.size() - 1; i >= 0; i--) {
				if (list.get(i).get_type().isNotCommon())
					list.remove(i);
			}

			// Continuous text-lines connection
			list = this.connectContinuousTextLineMultiRowsOnly(list, Gaps, lowCaseStart, extraSignStart);

			// Remove numbers
			for (int i = list.size() - 1; i >= 0; i--) {
				textLine t = list.get(i);
				algorithmInterface ai = new algorithmInterface();
				if (ai.isOrderNum(t.get_text()) || ai.isStatNum(t.get_text()) || ai.isOptionNum(t.get_text()))
					list.remove(i);
			}

			// If no title found yet, try to find one here
			if (this.get_title().length() == 0) {
				if (list.size() > 3) {
					for (int i = 0; i < list.size(); i++) {
						if (list.get(i).get_left() > 10 && list.get(i).get_left() + list.get(i).get_width() < 950
								&& list.get(i).get_height() > 10) {
							this.set_title(list.get(i).get_text());
							this._titleLocation[0] = list.get(i).get_left();
							this._titleLocation[1] = list.get(i).get_left() + list.get(i).get_width();
							this._titleLocation[2] = list.get(i).get_top();
							this._titleLocation[3] = list.get(i).get_bottom();
							list.remove(i);
							break;
						}
					}
				}
			}

			/*
			 * If there are more than 5 text-lines in the slide, try 2 different
			 * content loading methods. And choose the better result (with more
			 * text-lines involved in the system).
			 *
			 * When not, use the static loading method only.
			 */
			if (list.size() >= 5) {
				ArrayList<textOutline> tl1 = new ArrayList<textOutline>();
				tl1 = createTextOutlines(list);
				int count1 = 0;
				for (int i = 0; i < tl1.size(); i++)
					if (tl1.get(i).get_hierarchy() > 0)
						count1++;

				ArrayList<textOutline> tl2 = new ArrayList<textOutline>();
				tl2 = createTextOutlinesBySelfStats(list);
				int count2 = 0;
				for (int i = 0; i < tl2.size(); i++)
					if (tl2.get(i).get_hierarchy() > 0)
						count2++;

				if (count2 > count1 && count2 > list.size() / 2)
					result = tl2;
				else
					result = tl1;

				LoggerSingleton.info("Stats: " + "static-" + count1 + " adaptive-" + count2);
			} else
				result = createTextOutlines(list);
		} else { // split all text-lines into 2 groups by the middle axis
			FilterableList<textLine> leftTexts = new FilterableList<textLine>();
			FilterableList<textLine> rightTexts = new FilterableList<textLine>();
			for (int i = 0; i < list.size(); i++) {
				textLine t = list.get(i);
				if (t.get_left() >= middleLine)
					rightTexts.add(t);
				else
					leftTexts.add(t);
			}

			leftTexts = halfThisColumnWhenNeeded(leftTexts, true);
			rightTexts = halfThisColumnWhenNeeded(rightTexts, false);

			// If the organizing of this column is bad, consider it as a part of
			// diagram and remove it.
			if (isThisPartChaos(leftTexts, middleLine, true))
				leftTexts.clear();

			if (isThisPartChaos(rightTexts, middleLine, false))
				rightTexts.clear();

			// remove all those textlines with height 9 or 10
			for (int i = leftTexts.size() - 1; i >= 0; i--)
				if (leftTexts.get(i).get_type().isNotCommon())
					leftTexts.remove(i);

			for (int i = rightTexts.size() - 1; i >= 0; i--)
				if (rightTexts.get(i).get_type().isNotCommon())
					rightTexts.remove(i);

			// Do the connection
			leftTexts = this.connectContinuousTextLineMultiRowsOnly(leftTexts, Gaps, lowCaseStart, extraSignStart);
			rightTexts = this.connectContinuousTextLineMultiRowsOnly(rightTexts, Gaps, lowCaseStart, extraSignStart);

			for (int i = leftTexts.size() - 1; i >= 0; i--) {
				textLine t = leftTexts.get(i);
				algorithmInterface ai = new algorithmInterface();
				if (ai.isOrderNum(t.get_text()) || ai.isStatNum(t.get_text()) || ai.isOptionNum(t.get_text()))
					leftTexts.remove(i);
			}
			for (int i = rightTexts.size() - 1; i >= 0; i--) {
				textLine t = rightTexts.get(i);
				algorithmInterface ai = new algorithmInterface();
				if (ai.isOrderNum(t.get_text()) || ai.isStatNum(t.get_text()) || ai.isOptionNum(t.get_text()))
					rightTexts.remove(i);
			}

			// Add title when possible from either column
			if (this.get_title().length() == 0) {
				if (leftTexts.size() > 3 && rightTexts.size() > 0
						&& leftTexts.get(0).get_top() <= rightTexts.get(0).get_top()) {
					for (int i = 0; i < leftTexts.size(); i++) {
						if (leftTexts.get(i).get_left() > 10
								&& leftTexts.get(i).get_left() + leftTexts.get(i).get_width() < 950
								&& leftTexts.get(i).get_height() > 10) {
							this.set_title(leftTexts.get(i).get_text());
							this._titleLocation[0] = leftTexts.get(i).get_left();
							this._titleLocation[1] = leftTexts.get(i).get_left() + leftTexts.get(i).get_width();
							this._titleLocation[2] = leftTexts.get(i).get_top();
							this._titleLocation[3] = leftTexts.get(i).get_bottom();
							leftTexts.remove(i);
							break;
						}
					}
				} else if (leftTexts.size() > 0 && rightTexts.size() > 3
						&& leftTexts.get(0).get_top() >= rightTexts.get(0).get_top()) {
					for (int i = 0; i < rightTexts.size(); i++) {
						if (rightTexts.get(i).get_left() > 10
								&& rightTexts.get(i).get_left() + rightTexts.get(i).get_width() < 950
								&& rightTexts.get(i).get_height() > 10) {
							this.set_title(rightTexts.get(i).get_text());
							this._titleLocation[0] = rightTexts.get(i).get_left();
							this._titleLocation[1] = rightTexts.get(i).get_left() + rightTexts.get(i).get_width();
							this._titleLocation[2] = rightTexts.get(i).get_top();
							this._titleLocation[3] = rightTexts.get(i).get_bottom();
							rightTexts.remove(i);
							break;
						}
					}
				}
			}

			// check whether two columns should be loaded
			boolean both = true;
			boolean onlyLeft = true;

			if (middleLine < 100) {
				both = false;
				onlyLeft = false;
			} else if (middleLine > 850) {
				both = false;
				onlyLeft = true;
			} else if (leftTexts.size() == rightTexts.size())
				both = true;
			else if (leftTexts.size() > rightTexts.size()) {
				if (leftTexts.size() >= rightTexts.size() * 4 && rightTexts.size() <= 2) {
					both = false;
					onlyLeft = true;
				} else
					both = true;
			} else {
				if (rightTexts.size() >= leftTexts.size() * 4 && leftTexts.size() <= 2) {
					both = false;
					onlyLeft = false;
				} else
					both = true;
			}

			/*
			 * Content loading: two columns respectively, and combine together
			 * one after another.
			 *
			 * If the right column is obviously contain more content than the
			 * left, make it ahead, otherwise, first left, then right.
			 *
			 * For each columns, if there are more than 8 text-lines, try static
			 * and adaptive loading method, otherwise, try static and centered
			 * loading method, then choose the better result.
			 *
			 * Please note when columns exist, they are very likely to be two
			 * natural first-level components, so if a column contains more than
			 * 4 elements, there would be only one first-level text-outline,
			 * which is the first one within this column.
			 */
			if (both) {
				int leftLength = 0, rightLength = 0;
				for (int i = 0; i < leftTexts.size(); i++)
					leftLength += leftTexts.get(i).get_text().length();
				for (int i = 0; i < rightTexts.size(); i++)
					rightLength += rightTexts.get(i).get_text().length();

				if (rightLength > leftLength * 5 && rightTexts.size() > leftTexts.size() * 2
						&& leftTexts.get(0).get_top() < rightTexts.get(0).get_bottom()) {
					ArrayList<textOutline> tl1 = new ArrayList<textOutline>();
					tl1 = createTextOutlines(rightTexts);
					int count1 = 0;
					for (int i = 0; i < tl1.size(); i++)
						if (tl1.get(i).get_hierarchy() > 0)
							count1++;

					ArrayList<textOutline> tl2 = new ArrayList<textOutline>();
					tl2 = (rightTexts.size() >= 8) ? createTextOutlinesBySelfStats(rightTexts)
							: createTextOutlinesByCenterAlignment(rightTexts);
					int count2 = 0;
					for (int i = 0; i < tl2.size(); i++)
						if (tl2.get(i).get_hierarchy() > 0)
							count2++;

					if (count2 > count1 && count2 > rightTexts.size() / 2)
						result = adjustColumnTextSystem(tl2);
					else
						result = adjustColumnTextSystem(tl1);

					LoggerSingleton.info("Right Column Stats: " + "static-" + count1
							+ (rightTexts.size() >= 8 ? " adaptive-" : " center-") + count2);

					result.addAll(adjustColumnTextSystem(createTextOutlines(leftTexts)));
				} else {
					ArrayList<textOutline> tl1 = new ArrayList<textOutline>();
					tl1 = createTextOutlines(leftTexts);
					int count1 = 0;
					for (int i = 0; i < tl1.size(); i++)
						if (tl1.get(i).get_hierarchy() > 0)
							count1++;

					ArrayList<textOutline> tl2 = new ArrayList<textOutline>();
					tl2 = (leftTexts.size() >= 8) ? createTextOutlinesBySelfStats(leftTexts)
							: createTextOutlinesByCenterAlignment(leftTexts);
					int count2 = 0;
					for (int i = 0; i < tl2.size(); i++)
						if (tl2.get(i).get_hierarchy() > 0)
							count2++;

					if (count2 >= count1 && count2 > leftTexts.size() / 2)
						result = adjustColumnTextSystem(tl2);
					else
						result = adjustColumnTextSystem(tl1);

					LoggerSingleton.info("Left Column Stats: " + "static-" + count1
							+ (leftTexts.size() >= 8 ? " adaptive-" : " center-") + count2);

					ArrayList<textOutline> tl_1 = new ArrayList<textOutline>();
					tl_1 = createTextOutlines(rightTexts);
					count1 = 0;
					for (int i = 0; i < tl_1.size(); i++)
						if (tl_1.get(i).get_hierarchy() > 0)
							count1++;

					ArrayList<textOutline> tl_2 = new ArrayList<textOutline>();
					tl_2 = (rightTexts.size() >= 8) ? createTextOutlinesBySelfStats(rightTexts)
							: createTextOutlinesByCenterAlignment(rightTexts);
					count2 = 0;
					for (int i = 0; i < tl_2.size(); i++)
						if (tl_2.get(i).get_hierarchy() > 0)
							count2++;

					if (count2 >= count1 && count2 > rightTexts.size() / 2)
						result.addAll(adjustColumnTextSystem(tl_2));
					else
						result.addAll(adjustColumnTextSystem(tl_1));

					LoggerSingleton.info("Right Column Stats: " + "static-" + count1
							+ (rightTexts.size() >= 8 ? " adaptive-" : " center-") + count2);
				}
			} else {
				if (onlyLeft) {
					ArrayList<textOutline> tl1 = new ArrayList<textOutline>();
					tl1 = createTextOutlines(leftTexts);
					int count1 = 0;
					for (int i = 0; i < tl1.size(); i++)
						if (tl1.get(i).get_hierarchy() > 0)
							count1++;

					ArrayList<textOutline> tl2 = new ArrayList<textOutline>();
					tl2 = (rightTexts.size() >= 8) ? createTextOutlinesBySelfStats(leftTexts)
							: createTextOutlinesByCenterAlignment(leftTexts);
					int count2 = 0;
					for (int i = 0; i < tl2.size(); i++)
						if (tl2.get(i).get_hierarchy() > 0)
							count2++;

					if (count2 > count1 && count2 > leftTexts.size() / 2)
						result = tl2;
					else
						result = tl1;

					LoggerSingleton.info("Left Column (only) Stats: " + "static-" + count1
							+ (leftTexts.size() >= 8 ? " adaptive-" : " center-") + count2);
				} else {
					ArrayList<textOutline> tl1 = new ArrayList<textOutline>();
					tl1 = createTextOutlines(rightTexts);
					int count1 = 0;
					for (int i = 0; i < tl1.size(); i++)
						if (tl1.get(i).get_hierarchy() > 0)
							count1++;

					ArrayList<textOutline> tl2 = new ArrayList<textOutline>();
					tl2 = (rightTexts.size() >= 8) ? createTextOutlinesBySelfStats(rightTexts)
							: createTextOutlinesByCenterAlignment(rightTexts);
					int count2 = 0;
					for (int i = 0; i < tl2.size(); i++)
						if (tl2.get(i).get_hierarchy() > 0)
							count2++;

					if (count2 > count1 && count2 > rightTexts.size() / 2)
						result = tl2;
					else
						result = tl1;

					LoggerSingleton.info("Right Column (only) Stats: " + "static-" + count1
							+ (rightTexts.size() >= 8 ? " adaptive-" : " center-") + count2);
				}
			}
		}

		return result;
	}

	private ArrayList<textOutline> createTextOutlines(FilterableList<textLine> list) {
		ArrayList<textOutline> result = new ArrayList<textOutline>();

		// remove all those textlines with height 9 or 10
		list.filter(new FilterFunc<Boolean, textLine>() {
			@Override
			public Boolean call(textLine line) { 
				return line != null && line.get_type().isCommon();
			}

			@Override
			public Boolean call() { return true; }
		}, true);
		
		if (list.isEmpty())
			return result;

		double wp = (double) this.get_pageWidth() / 1024;
		double hp = (double) this.get_pageHeight() / 768;

		/*
		 * Up to 3 levels differed. (can be less) First adaptively find the
		 * levels and then set them
		 */

		int firstLevelLeft = (int) (768 * wp);
		int secondLevelLeft = 0;
		int thirdLevelLeft = 0;

		double averageHeight = 0;
		double averageWidth = 0;
		double shortest = this.get_pageHeight();
		int count = 0;
		for (int i = 0; i < list.size(); i++) {
			count++;
			averageHeight += (double) list.get(i).get_height();
			averageWidth += (double) list.get(i).get_width();
			if (list.get(i).get_height() < shortest)
				shortest = list.get(i).get_height();
		}
		averageHeight = averageHeight / count;
		averageWidth = averageWidth / count;

		/*
		 * First try to find the 1st Level in left-top quarter of the slide the
		 * target line should meet requirement below: 1. Not so short in height
		 * 2. Vertically under the title 3. In left-top quarter 4. If there's
		 * multiple choices, choose the one more to the left 5. If no choice,
		 * loose 1 and 3, keep 2 for more chance
		 */
		int temp = -1;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get_height() >= averageHeight && list.get(i).get_width() >= averageWidth / 2
					&& list.get(i).get_top() < 350 * hp && list.get(i).get_top() > this._titleLocation[3] + 20 * hp
					&& list.get(i).get_left() < 450 * hp) {
				if (list.get(i).get_left() <= firstLevelLeft) {
					firstLevelLeft = list.get(i).get_left();
					temp = i;
				}
			}
		}

		LoggerSingleton.info("Static Content-Tree Loading");
		LoggerSingleton.info(list.get(0).get_slideID() + ":  " + temp + "  "
				+ (temp >= 0 ? list.get(temp).get_text() : "no text selected!"));

		if (temp == -1) {
			for (int i = 0; i < list.size(); i++) {
				if ((list.get(i).get_height() >= (averageHeight + shortest) / 2 || list.get(i).get_height() > 15 * hp)
						&& list.get(i).get_width() >= averageWidth / 2 && list.get(i).get_top() < 700 * hp
						&& list.get(i).get_top() > this._titleLocation[3] + 10 * hp
						&& list.get(i).get_left() < 700 * wp) {
					if (list.get(i).get_left() <= firstLevelLeft) {
						firstLevelLeft = list.get(i).get_left();
						temp = i;
					}
				}
			}
		}

		LoggerSingleton.info(list.get(0).get_slideID() + ":  " + temp + "  "
				+ (temp >= 0 ? list.get(temp).get_text() : "no text selected!"));

		for (int i = 0; i < temp; i++) {
			if (list.get(i).get_left() > firstLevelLeft - 5 * wp && list.get(i).get_left() < firstLevelLeft + 5 * wp) {
				temp = i;
				break;
			}
		}

		LoggerSingleton.info(list.get(0).get_slideID() + ":  " + temp + "  "
				+ (temp >= 0 ? list.get(temp).get_text() : "no text selected!"));

		/*
		 * Try to find whether there is a more 'left' choice by searching the
		 * previous textline of the very first one on the firstLevelLeft
		 */

		while (temp > 0 && list.get(temp).get_left() > list.get(temp - 1).get_left()
				&& list.get(temp).get_left() - list.get(temp - 1).get_left() < 100 * wp
				&& list.get(temp).get_top() - list.get(temp - 1).get_bottom() < 50 * hp
				&& list.get(temp - 1).get_top() > this._titleLocation[3]) {
			firstLevelLeft = list.get(temp - 1).get_left();
			for (int i = 0; i < temp; i++) {
				if (list.get(i).get_left() > firstLevelLeft - 5 * wp
						&& list.get(i).get_left() < firstLevelLeft + 5 * wp) {
					temp = i;
					break;
				}
			}
		}

		LoggerSingleton.info(list.get(0).get_slideID() + ":  " + temp + "  "
				+ (temp >= 0 ? list.get(temp).get_text() : "no text selected!"));

		/*
		 * Set the 2nd and 3rd level by searching the textlines in 'system' the
		 * target should be following a upper-level textline or surrounding by
		 * textlines horizontally near
		 */

		secondLevelLeft = this.get_pageWidth();
		int difference = this.get_pageWidth();
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).get_left() >= firstLevelLeft + 15 * wp
					&& list.get(i).get_left() <= firstLevelLeft + 255 * wp)
				if (list.get(i).get_left() - firstLevelLeft < difference) {
					if (list.get(i - 1).get_left() < firstLevelLeft + 15 * wp
							&& list.get(i - 1).get_left() > firstLevelLeft - 25 * wp) {
						secondLevelLeft = list.get(i).get_left();
						difference = secondLevelLeft - firstLevelLeft;
					} else if (list.get(i).get_left() - firstLevelLeft < 75 * wp
							&& list.get(i - 1).get_left() - firstLevelLeft < 75 * wp
							&& (i < list.size() - 1 ? list.get(i + 1).get_left() - firstLevelLeft < 75 * wp : true)) {
						secondLevelLeft = list.get(i).get_left();
						difference = secondLevelLeft - firstLevelLeft;
					}
				}
		}

		thirdLevelLeft = this.get_pageWidth();
		difference = this.get_pageWidth();
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).get_left() >= secondLevelLeft + 15 * wp
					&& list.get(i).get_left() <= secondLevelLeft + 255 * wp)
				if (list.get(i).get_left() - secondLevelLeft < difference)
					if (list.get(i - 1).get_left() < secondLevelLeft + 15 * wp
							&& list.get(i - 1).get_left() > secondLevelLeft - 25 * wp) {
						thirdLevelLeft = list.get(i).get_left();
						difference = thirdLevelLeft - secondLevelLeft;
					}
		}

		/*
		 * Load the textOntlines here Two judgment, one is strict and absolute
		 * on horizontal position another is more flexible according to the
		 * context and, footline will not be included
		 */

		LoggerSingleton.info(
				list.get(0).get_slideID() + ":  " + firstLevelLeft + "  " + secondLevelLeft + " " + thirdLevelLeft);
		this._levelCoordinates[0] = (firstLevelLeft >= 768 * wp) ? -1 : firstLevelLeft;
		this._levelCoordinates[1] = (secondLevelLeft >= this._pageWidth) ? -1 : secondLevelLeft;
		this._levelCoordinates[2] = (thirdLevelLeft >= this._pageWidth) ? -1 : thirdLevelLeft;

		int last = 0, last2 = 0, last_in_system = -1;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get_type() == TextLineType.FOOTLINE
					&& list.get(list.size() - 1).get_top() - list.get(i).get_bottom() <= 0) {
				textOutline t = new textOutline(list.get(i).get_text(), 0);
				result.add(t);
				last2 = last;
				last = 0;
			} else if (list.get(i).get_top() <= 20 * hp
					|| list.get(i).get_top() + list.get(i).get_height() < this._titleLocation[3]) {
				textOutline t = new textOutline(list.get(i).get_text(), 0);
				result.add(t);
				last2 = last;
				last = 0;
			} else {
				int left = list.get(i).get_left();
				int db12 = (int) (secondLevelLeft == this.get_pageWidth() ? firstLevelLeft + 50 * wp : secondLevelLeft)
						- firstLevelLeft;
				int db23 = (int) (thirdLevelLeft == this.get_pageWidth() ? secondLevelLeft + 50 * wp : thirdLevelLeft)
						- secondLevelLeft;
				if (left >= firstLevelLeft - 5 * wp && left <= firstLevelLeft + 5 * wp) {
					textOutline t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 1;
					last_in_system = 1;
				} else if ((last > 0 || last2 > 0 || i == 0)
						&& (left > firstLevelLeft - 50 * wp && left < firstLevelLeft + db12 / 2)) {
					textOutline t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 1;
					last_in_system = 1;
				} else if (left >= secondLevelLeft - 5 * wp && left <= secondLevelLeft + 5 * wp) {
					textOutline t;
					if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 2;
					last_in_system = 2;
				} else if ((last > 0 || last2 > 0 || i == 0)
						&& (left > secondLevelLeft - db12 / 2 && left < secondLevelLeft + db23 / 2)) {
					textOutline t;
					if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 2;
					last_in_system = 2;
				} else if (left >= thirdLevelLeft - 5 * wp && left <= thirdLevelLeft + 5 * wp) {
					textOutline t;
					if (last_in_system > 1)
						t = new textOutline(list.get(i).get_text(), 3);
					else if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 3;
					last_in_system = 3;
				} else if ((last > 0 || last2 > 0)
						&& (left > thirdLevelLeft - db23 / 2 && left < thirdLevelLeft + 100 * wp)) {
					textOutline t;
					if (last_in_system > 1)
						t = new textOutline(list.get(i).get_text(), 3);
					else if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 3;
					last_in_system = 3;
				} else {
					textOutline t = new textOutline(list.get(i).get_text(), 0);
					result.add(t);
					last2 = last;
					last = 0;
				}
			}
		}

		for (int i = 0; i < result.size(); i++)
			if (result.get(i).get_hierarchy() > 0) {
				if (result.get(i).get_hierarchy() > 1)
					result.get(i).set_hierarchy(1);
				break;
			}

		return result;
	}

	private ArrayList<textOutline> createTextOutlinesBySelfStats(ArrayList<textLine> list) {
		ArrayList<textOutline> result = new ArrayList<textOutline>();

		// remove all those textlines with height 9 or 10
		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i).get_type().isNotCommon())
				list.remove(i);
		}

		if (list.size() == 0)
			return result;

		double wp = (double) this.get_pageWidth() / 1024;
		double hp = (double) this.get_pageHeight() / 768;

		/*
		 * Up to 3 levels differed. (can be less) First adaptively find the
		 * levels and then set them
		 */

		int firstLevelLeft = this.get_pageWidth();
		int secondLevelLeft = 0;
		int thirdLevelLeft = 0;

		ArrayList<textLine> stats = new ArrayList<textLine>();
		for (int i = 0; i < list.size(); i++) {
			boolean match = false;
			for (int j = 0; j < stats.size(); j++) {
				if (stats.get(j).get_left() - 5 * wp < list.get(i).get_left()
						&& stats.get(j).get_left() + 5 * wp > list.get(i).get_left()) {
					match = true;
					stats.get(j).set_count(stats.get(j).get_count() + 1);
					break;
				}
			}
			if (!match) {
				textLine t = new textLine();
				t.set_count(1);
				t.set_left(list.get(i).get_left());
				stats.add(t);
			}
		}

		for (int i = 0; i < stats.size(); i++)
			for (int j = i + 1; j < stats.size(); j++) {
				if (stats.get(j).get_count() > stats.get(i).get_count()) {
					textLine temp = stats.get(i);
					stats.set(i, stats.get(j));
					stats.set(j, temp);
				} else if (stats.get(j).get_count() == stats.get(i).get_count()) {
					if (stats.get(j).get_left() < stats.get(i).get_left()) {
						textLine temp = stats.get(i);
						stats.set(i, stats.get(j));
						stats.set(j, temp);
					}
				}
			}

		LoggerSingleton.info("Adaptive Content-Tree Loading");
		LoggerSingleton.info("Most Frequent Pos: " + stats.get(0).get_left() + " " + stats.get(0).get_count());
		if (stats.size() > 1)
			LoggerSingleton.info("Most Frequent Pos: " + stats.get(1).get_left() + " " + stats.get(1).get_count());

		if (stats.size() > 0 && stats.get(0).get_count() > 1)
			firstLevelLeft = stats.get(0).get_left();

		for (int i = 0; i < list.size(); i++)
			if (list.get(i).get_left() > firstLevelLeft - 5 * wp && list.get(i).get_left() < firstLevelLeft + 5 * wp) {
				LoggerSingleton.info(list.get(0).get_slideID() + ":  " + i + "  " + list.get(i).get_text());
				break;
			}

		boolean previous = true;
		while (previous) {
			boolean meetEnd = true;
			for (int i = 1; i < list.size(); i++) {
				if (list.get(i).get_left() > firstLevelLeft - 5 * wp
						&& list.get(i).get_left() < firstLevelLeft + 5 * wp) {
					if (list.get(i).get_left() - list.get(i - 1).get_left() > 5 * wp
							&& list.get(i).get_left() - list.get(i - 1).get_left() < 75 * wp
							&& list.get(i).get_top() - list.get(i - 1).get_bottom() < 50 * hp
							&& list.get(i - 1).get_top() > this._titleLocation[3]) {
						firstLevelLeft = list.get(i - 1).get_left();
						LoggerSingleton
								.info(list.get(0).get_slideID() + ":  " + (i - 1) + "  " + list.get(i - 1).get_text());
						meetEnd = false;
						break;
					}
				}
			}
			if (meetEnd)
				previous = false;
		}

		/*
		 * Set the 2nd and 3rd level by searching the textlines in 'system' the
		 * target should be following a upper-level textline or surrounding by
		 * textlines horizontally near
		 */

		secondLevelLeft = this.get_pageWidth();
		int difference = this.get_pageWidth();
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).get_left() >= firstLevelLeft + 15 * wp
					&& list.get(i).get_left() <= firstLevelLeft + 255 * wp)
				if (list.get(i).get_left() - firstLevelLeft < difference) {
					if (list.get(i - 1).get_left() < firstLevelLeft + 15 * wp
							&& list.get(i - 1).get_left() > firstLevelLeft - 25 * wp) {
						secondLevelLeft = list.get(i).get_left();
						difference = secondLevelLeft - firstLevelLeft;
					} else if (list.get(i).get_left() - firstLevelLeft < 75 * wp
							&& list.get(i - 1).get_left() - firstLevelLeft < 75 * wp
							&& (i < list.size() - 1 ? list.get(i + 1).get_left() - firstLevelLeft < 75 * wp : true)) {
						secondLevelLeft = list.get(i).get_left();
						difference = secondLevelLeft - firstLevelLeft;
					}
				}
		}

		thirdLevelLeft = this.get_pageWidth();
		difference = this.get_pageWidth();
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i).get_left() >= secondLevelLeft + 15 * wp
					&& list.get(i).get_left() <= secondLevelLeft + 255 * wp)
				if (list.get(i).get_left() - secondLevelLeft < difference)
					if (list.get(i - 1).get_left() < secondLevelLeft + 15 * wp
							&& list.get(i - 1).get_left() > secondLevelLeft - 25 * wp) {
						thirdLevelLeft = list.get(i).get_left();
						difference = thirdLevelLeft - secondLevelLeft;
					}
		}

		/*
		 * Load the textOntlines here Two judgment, one is strict and absolute
		 * on horizontal position another is more flexible according to the
		 * context and, footline will not be included
		 */

		LoggerSingleton.info(
				list.get(0).get_slideID() + ":  " + firstLevelLeft + "  " + secondLevelLeft + " " + thirdLevelLeft);
		this._levelCoordinates[0] = (firstLevelLeft >= 768 * wp) ? -1 : firstLevelLeft;
		this._levelCoordinates[1] = (secondLevelLeft >= this._pageWidth) ? -1 : secondLevelLeft;
		this._levelCoordinates[2] = (thirdLevelLeft >= this._pageWidth) ? -1 : thirdLevelLeft;

		int last = 0, last2 = 0, last_in_system = -1;
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).get_type() == TextLineType.FOOTLINE
					&& list.get(list.size() - 1).get_top() - list.get(i).get_bottom() <= 0) {
				textOutline t = new textOutline(list.get(i).get_text(), 0);
				result.add(t);
				last2 = last;
				last = 0;
			} else if (list.get(i).get_top() <= 20 * hp
					|| list.get(i).get_top() + list.get(i).get_height() < this._titleLocation[3]) {
				textOutline t = new textOutline(list.get(i).get_text(), 0);
				result.add(t);
				last2 = last;
				last = 0;
			} else {
				int left = list.get(i).get_left();
				int db12 = (int) (secondLevelLeft == this.get_pageWidth() ? firstLevelLeft + 50 * wp : secondLevelLeft)
						- firstLevelLeft;
				int db23 = (int) (thirdLevelLeft == this.get_pageWidth() ? secondLevelLeft + 80 * wp : thirdLevelLeft)
						- secondLevelLeft;
				if (left >= firstLevelLeft - 5 * wp && left <= firstLevelLeft + 5 * wp) {
					textOutline t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 1;
					last_in_system = 1;
				} else if ((last > 0 || last2 > 0 || i == 0)
						&& (left > firstLevelLeft - 50 * wp && left < firstLevelLeft + db12 / 2)) {
					textOutline t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 1;
					last_in_system = 1;
				} else if (left >= secondLevelLeft - 5 * wp && left <= secondLevelLeft + 5 * wp) {
					textOutline t;
					if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 2;
					last_in_system = 2;
				} else if ((last > 0 || last2 > 0 || i == 0)
						&& (left > secondLevelLeft - db12 / 2 && left < secondLevelLeft + db23 / 2)) {
					textOutline t;
					if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 2;
					last_in_system = 2;
				} else if (left >= thirdLevelLeft - 5 * wp && left <= thirdLevelLeft + 5 * wp) {
					textOutline t;
					if (last_in_system > 1)
						t = new textOutline(list.get(i).get_text(), 3);
					else if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 3;
					last_in_system = 3;
				} else if ((last > 0 || last2 > 0)
						&& (left > thirdLevelLeft - db23 / 2 && left < thirdLevelLeft + 100 * wp)) {
					textOutline t;
					if (last_in_system > 1)
						t = new textOutline(list.get(i).get_text(), 3);
					else if (last_in_system > 0)
						t = new textOutline(list.get(i).get_text(), 2);
					else
						t = new textOutline(list.get(i).get_text(), 1);
					result.add(t);
					last2 = last;
					last = 3;
					last_in_system = 3;
				} else {
					textOutline t = new textOutline(list.get(i).get_text(), 0);
					result.add(t);
					last2 = last;
					last = 0;
				}
			}
		}

		return result;
	}

	private ArrayList<textOutline> createTextOutlinesByCenterAlignment(ArrayList<textLine> list) {
		ArrayList<textOutline> result = new ArrayList<textOutline>();

		// remove all those textlines with height 9 or 10
		for (int i = list.size() - 1; i >= 0; i--) {
			if (list.get(i).get_type().isNotCommon())
				list.remove(i);
		}

		if (list.size() < 1)
			return result;

		result.add(new textOutline(list.get(0).get_text(), 0));
		LoggerSingleton.info("Centered Content-Tree Loading: " + (list.get(0).get_left() + list.get(0).get_width()));

		for (int i = 1; i < list.size(); i++) {
			int center0 = list.get(0).get_left() + list.get(0).get_width() / 2;
			int centerI = list.get(i).get_left() + list.get(i).get_width() / 2;

			if (Math.abs(center0 - centerI) < 5) {
				result.get(0).set_hierarchy(1);
				result.add(new textOutline(list.get(i).get_text(), 1));
			} else
				result.add(new textOutline(list.get(i).get_text(), 0));
		}

		return result;
	}

	public void combineAtEnd(slidePage s) {
		/*
		 * This function is used to gather the content from two slides together.
		 * And during the process of combination, try to avoid repeated content
		 * to be retained twice, and try to logically rearrange the order of
		 * content appearance for those subtopics sharing a same topic inside
		 * two slides.
		 */

		// Firstly, add all contents from second slide to the end of first slide
		// after set time info.
		for (int i = 0; i < this.get_texts().size(); i++) {
			if (this.get_texts().get(i).get_time().before(this.get_startTime()))
				this.get_texts().get(i).set_time(this.get_startTime());
		}

		for (int i = 0; i < s.get_texts().size(); i++) {
			s.get_texts().get(i).set_time(s.get_startTime());
			this.get_texts().add(s.get_texts().get(i));
		}

		// From up to down, searching for same texts, then deleting them and
		// reorder their subtopics.
		for (int i = 0; i < this.get_texts().size(); i++) {
			/*
			 * Using (i, i2) and (ii, ii2) to tag where the same texts locate (
			 * with i and ii ), and where their subtopics end ( with i2 and ii2
			 * ), if they exist. Then catch the [ii+1, ii2-1] part out of the
			 * data structure and plug them in at [i2-1], and delete ii.
			 */
			int ii = -1;
			for (int j = i + 1; j < this.get_texts().size(); j++) {
				if (this.get_texts().get(j).get_text().contentEquals(this.get_texts().get(i).get_text()))
					if (this.get_texts().get(j).get_hierarchy() == this.get_texts().get(i).get_hierarchy())
						if (this.get_texts().get(j).get_hierarchy() > 0) {
							boolean sameParent = true;
							for (int k = i + 1; k < j; k++) {
								int h = this.get_texts().get(k).get_hierarchy();
								if (h > 0 && h < this.get_texts().get(j).get_hierarchy()) {
									sameParent = false;
									break;
								}
							}

							if (sameParent) {
								ii = j;
								break;
							}
						}
			}

			if (ii > i) {
				int i2 = ii;
				int ii2 = this.get_texts().size();

				for (int j = i + 1; j < i2; j++) {
					if (this.get_texts().get(j).get_hierarchy() == this.get_texts().get(i).get_hierarchy()) {
						i2 = j;
						break;
					}
				}

				for (int j = ii + 1; j < ii2; j++) {
					if (this.get_texts().get(j).get_hierarchy() == this.get_texts().get(ii).get_hierarchy()) {
						ii2 = j;
						break;
					}
				}

				// LoggerSingleton.info(i + " " + i2 + " " + ii + " " + ii2);

				if (i2 == ii)
					this.get_texts().remove(i2);
				else {
					ArrayList<textOutline> insertList = new ArrayList<textOutline>();
					ArrayList<textOutline> backUp = new ArrayList<textOutline>();

					for (int k = ii2 - 1; k > ii; k--) {
						insertList.add(this.get_texts().get(k));
						this.get_texts().remove(k);
					}

					for (int k = this.get_texts().size() - 1; k >= i2; k--) {
						if (k != ii)
							backUp.add(this.get_texts().get(k));
						this.get_texts().remove(k);
					}

					for (int k = insertList.size() - 1; k >= 0; k--)
						this.get_texts().add(insertList.get(k));
					insertList.clear();

					for (int k = backUp.size() - 1; k >= 0; k--)
						this.get_texts().add(backUp.get(k));
					backUp.clear();
				}

			}
		}

		this.isSlideWellOrganized();

	}

	public void isSlideWellOrganized() {

		if (get_texts().size() == 0) {
			set_pageType(SlidePageType.POSSIBLY_PICTURE_SLIDE);
			if (get_title().contentEquals(""))
				set_pageType(SlidePageType.EMPTY);
			return;
		}

		if (get_texts().size() <= 2 && get_title().contentEquals("")) {
			if (get_texts().size() <= 1)
				set_pageType(SlidePageType.POSSIBLY_PICTURE_SLIDE);
			return;
		}

		int count = 0;
		for (int i = 0; i < get_texts().size(); i++) {
			textOutline t = get_texts().get(i);
			if (t.get_hierarchy() > 0)
				count++;
		}
		if (count > 0 && (double) get_texts().size() / (double) count < 2.0)
			set_pageType(SlidePageType.WELL_ORGANAZIED);
	}

	public boolean isSamePage(slidePage s) {
		// Mainly using Levenshtein Distance and Same Words as the requirements.

		algorithmInterface ai = new algorithmInterface();

		String allTexts1 = this.get_title();
		for (int i = 0; i < this.get_texts().size(); i++)
			allTexts1 = allTexts1 + " " + this.get_texts().get(i).get_text();

		String allTexts2 = s.get_title();
		for (int i = 0; i < s.get_texts().size(); i++)
			allTexts2 = allTexts2 + " " + s.get_texts().get(i).get_text();

		if (allTexts1.contentEquals(allTexts2)) {
			return true;
		} else {
			int le = ai.getLevenshteinDistance(allTexts1, allTexts2);
			int longer = allTexts1.length() > allTexts2.length() ? allTexts1.length() : allTexts2.length();
			double lr = (double) le / (double) longer;
			double wr = ai.getSameWordsRatio(allTexts1, allTexts2);

			// if(this.get_PageNum()==81 && s.get_PageNum()==83)
			// LoggerSingleton.info(lr + " " + wr);

			if (this.get_pageType().equals(SlidePageType.POSSIBLY_PICTURE_SLIDE)
					|| s.get_pageType().equals(SlidePageType.POSSIBLY_PICTURE_SLIDE)) {
				if (this.get_title().length() > 0 && this.get_title().contentEquals(s.get_title())
						&& longer - this.get_title().length() <= 30)
					return true;
			} else if (lr <= 0.3 && wr >= 0.7)
				return true;
			else if (lr <= 0.4 && wr >= 0.9)
				return true;
			else if (lr <= 0.4 && wr >= 0.6) {
				int t = ai.getLevenshteinDistance(this.get_title(), s.get_title());
				int tl = this.get_title().length() > s.get_title().length() ? this.get_title().length()
						: s.get_title().length();
				double tr = (double) t / (double) tl;
				if (tr <= 0.25 && longer - tl > 50)
					return true;
			} else if (lr <= 0.5 && wr >= 0.8) {
				int t = ai.getLevenshteinDistance(this.get_title(), s.get_title());
				int tl = this.get_title().length() > s.get_title().length() ? this.get_title().length()
						: s.get_title().length();
				double tr = (double) t / (double) tl;
				if (tr <= 0.25 && longer - tl > 50)
					return true;
			} else if (lr <= 0.5 && wr >= 0.6) {
				int t = ai.getLevenshteinDistance(this.get_title(), s.get_title());
				int tl = this.get_title().length() > s.get_title().length() ? this.get_title().length()
						: s.get_title().length();
				double tr = (double) t / (double) tl;
				if (tr <= 0.2 && longer - tl > 100)
					return true;
			} else if (lr <= 0.6 && wr >= 0.7) {
				int tl = this.get_title().length() > s.get_title().length() ? this.get_title().length()
						: s.get_title().length();
				if (this.get_title().contentEquals(s.get_title()) && longer - tl > 200)
					return true;
			}

			/*
			 * only for temp if( _onlyForStats_1( this.get_PageNum(),
			 * s.get_PageNum() ) ) // temp( this.get_PageNum(), s.get_PageNum()
			 * ) && (this.get_PageNum() <= 48 || this.get_PageNum() >= 88) &&
			 * (this.get_PageNum() <= 27 || this.get_PageNum() >= 45) ) {
			 * //LoggerSingleton.info(this.get_PageNum() + "-" +
			 * this.get_title() + ": " + allTexts1);
			 * //LoggerSingleton.info(s.get_PageNum() + "-" + s.get_title() +
			 * ": " + allTexts2); LoggerSingleton.info((int)(lr*100) + "\t" +
			 * (int)(wr*100) + "\t(" + this.get_PageNum() + ", " +
			 * s.get_PageNum() + ")"); //LoggerSingleton.info(
			 * " Same Words Ratio: " + wr); //LoggerSingleton.info(); }
			 */

		}

		return false;
	}

	private int searchHorizontalMiddleLine(ArrayList<textLine> list, double wp) {
		if (list.size() < 2)
			return -1;

		ArrayList<int[]> xy = new ArrayList<int[]>();
		int middle = -1;

		for (int i = 0; i < list.size(); i++) {
			int[] current = { list.get(i).get_left(), list.get(i).get_left() + list.get(i).get_width() };
			xy.add(current);
		}

		// Re-order: from left to right
		for (int i = 0; i < xy.size(); i++)
			for (int j = i + 1; j < xy.size(); j++) {
				if (xy.get(i)[0] > xy.get(j)[0]) {
					int[] temp = xy.get(i);
					xy.set(i, xy.get(j));
					xy.set(j, temp);
				}
			}

		// Search for the most middle textline, whose left-edge is 'righter'
		// than all 'lefter' textlines' right-edge.
		for (int i = 1; i < xy.size(); i++) {
			int a = xy.get(i)[0];
			boolean allLeft = true;

			for (int j = 0; j < i; j++) {
				if (xy.get(j)[1] >= a) {
					allLeft = false;
					break;
				}
			}

			if (allLeft)
				if (Math.abs(a - 512 * wp) < Math.abs(middle - 512 * wp))
					middle = a;
		}

		if (middle > 0) {
			boolean nowLeft = true;
			int changes = 0;
			int interlaces = 0;
			for (int i = 0; i < list.size(); i++) {
				boolean last = nowLeft;
				if (list.get(i).get_left() < middle)
					nowLeft = true;
				else
					nowLeft = false;

				if (i > 0) {
					if (list.get(i - 1).get_bottom() > list.get(i).get_top())
						interlaces++;
					if (nowLeft != last)
						changes++;
				}

			}
			if (changes <= 1 && interlaces == 0)
				middle = 0 - middle;
		}

		return middle;
	}

	private boolean isThisPartChaos(ArrayList<textLine> list, int middleLine, boolean left) {
		if (list.size() < 1)
			return false;

		if (searchHorizontalMiddleLine(list, 1) > this._pageWidth / 8
				&& searchHorizontalMiddleLine(list, 1) < this._pageWidth * 0.875) {
			LoggerSingleton.info("The " + (left ? "left" : "right") + " column can be split again, diagram, removed.");
			return true;
		}

		/*
		 * Check the content of either column. If there are lots of digits as
		 * independent text-lines, it is quite possible that this column is a
		 * part of a chart, need to be ignored.
		 *
		 * And the average length of the text-lines also be found out, if it is
		 * too small, then this column seems not to be a valuable component of
		 * this slide.
		 */
		int countNum = 0, totalLength = 0, aveLength = 0, interlaced = 0, covered = 0, leftMost = 1024, rightMost = 1;
		for (int i = 0; i < list.size(); i++) {
			textLine t = list.get(i);
			algorithmInterface ai = new algorithmInterface();
			totalLength += t.get_text().length();
			if (ai.isOrderNum(t.get_text()) || ai.isStatNum(t.get_text()) || ai.isOptionNum(t.get_text()))
				countNum++;
			if (i > 0) {
				if (t.get_top() < list.get(i - 1).get_top() + list.get(i - 1).get_height())
					interlaced++;

				if (Math.min(t.get_left() + t.get_width(), list.get(i - 1).get_left() + list.get(i - 1).get_width())
						- Math.max(t.get_left(), list.get(i - 1).get_left()) > Math.min(t.get_width(),
								list.get(i - 1).get_width()) * 0.66)
					covered++;
			}
			if (t.get_left() < leftMost)
				leftMost = t.get_left();
			if (t.get_left() + t.get_width() > rightMost)
				rightMost = t.get_left() + t.get_width();
		}

		LoggerSingleton.info("Covered (" + covered + "/" + list.size() + ")");

		if (middleLine < this._pageWidth * 0.25 && left) {
			LoggerSingleton.info("The left column is too close to the left edge, removed.");
			return true;
		} else if (middleLine > this._pageWidth * 0.75 && !left) {
			LoggerSingleton.info("The right column is too close to the right edge, removed.");
			return true;
		} else if (list.size() > 1 && rightMost - leftMost > 0 && rightMost - leftMost < this._pageWidth / 8) {
			LoggerSingleton.info("The " + (left ? "left" : "right") + " column is too narrow, removed.");
			return true;
		} else if ((double) covered < (double) (list.size() - 1) / 2 && list.size() > 3) {
			LoggerSingleton.info("The " + (left ? "left" : "right") + " column is a potential diagram, removed.");
			return true;
		} else {
			aveLength = list.size() > 0 ? totalLength / list.size() : 0;
			LoggerSingleton.info("Column " + (left ? "Left" : "Right") + ": aveLength = " + aveLength + " totalItems = "
					+ list.size() + " digitNum = " + countNum + "; " + interlaced + " interlaces");
			if (aveLength <= 5 && list.size() > 1)
				return true;
			else if (aveLength < 10 && list.size() > 1 && (countNum >= list.size() / 2 || aveLength < list.size()))
				return true;
			else if (aveLength < list.size() && interlaced > list.size() / 4)
				return true;
		}

		return false;
	}

	private FilterableList<textLine> halfThisColumnWhenNeeded(FilterableList<textLine> list, boolean left) {
		if (list.size() <= 1)
			return list;
		else if (list.get(list.size() - 1).get_bottom() - list.get(0).get_top() < this._pageHeight * 0.4)
			return list;

		int aveHeight = 0, biggestLineSpace = 0, biggestLineSpacePosition = -1;
		for (int i = 0; i < list.size(); i++) {
			aveHeight += list.get(i).get_height();
			if (i > 0) {
				int currentLineSpace = list.get(i).get_top() - list.get(i - 1).get_bottom();
				if (currentLineSpace > biggestLineSpace) {
					biggestLineSpace = currentLineSpace;
					biggestLineSpacePosition = i;
				}
			}
		}
		aveHeight = aveHeight / list.size();

		if (biggestLineSpace > aveHeight * 4) {
			FilterableList<textLine> upperPart = new FilterableList<textLine>();
			FilterableList<textLine> bottomPart = new FilterableList<textLine>();

			for (int i = 0; i < biggestLineSpacePosition; i++)
				upperPart.add(list.get(i));
			for (int i = biggestLineSpacePosition; i < list.size(); i++)
				bottomPart.add(list.get(i));

			boolean deleteUpper = false, deleteBottom = false;

			int covered = 0;
			for (int i = 1; i < upperPart.size(); i++)
				if (Math.min(upperPart.get(i).get_left() + upperPart.get(i).get_width(),
						upperPart.get(i - 1).get_left() + upperPart.get(i - 1).get_width())
						- Math.max(upperPart.get(i).get_left(), upperPart.get(i - 1).get_left()) > Math
								.min(upperPart.get(i).get_width(), upperPart.get(i - 1).get_width()) * 0.66)
					covered++;
			if ((double) covered < (double) (upperPart.size() - 1) / 2 && upperPart.size() > 2)
				deleteUpper = true;

			covered = 0;
			for (int i = 1; i < bottomPart.size(); i++)
				if (Math.min(bottomPart.get(i).get_left() + bottomPart.get(i).get_width(),
						bottomPart.get(i - 1).get_left() + bottomPart.get(i - 1).get_width())
						- Math.max(bottomPart.get(i).get_left(), bottomPart.get(i - 1).get_left()) > Math
								.min(bottomPart.get(i).get_width(), bottomPart.get(i - 1).get_width()) * 0.66)
					covered++;
			if ((double) covered < (double) (bottomPart.size() - 1) / 2 && bottomPart.size() > 2)
				deleteBottom = true;

			if (bottomPart.size() <= 2)
				if (Math.min(bottomPart.get(0).get_left() + bottomPart.get(0).get_width(),
						upperPart.get(0).get_left() + upperPart.get(0).get_width())
						- Math.max(bottomPart.get(0).get_left(), upperPart.get(0).get_left()) < Math
								.min(bottomPart.get(0).get_width(), upperPart.get(0).get_width()) * 0.66)
					deleteBottom = true;

			if (deleteBottom && deleteUpper) {
				LoggerSingleton.info("The " + (left ? "left" : "right") + " column is a potential diagram, removed.");
				list.clear();
				return list;
			} else if (deleteBottom) {
				LoggerSingleton.info(
						"The bottom part of " + (left ? "left" : "right") + " column is a potential diagram, removed.");
				return upperPart;
			} else if (deleteUpper) {
				LoggerSingleton.info(
						"The upper part of " + (left ? "left" : "right") + " column is a potential diagram, removed.");
				return bottomPart;
			}
		}

		return list;
	}

	private ArrayList<int[]> detectTable(ArrayList<textLine> list, double wp, double hp, String lectureID) throws IOException {
		ArrayList<int[]> allTableArea = new ArrayList<int[]>();
		if (list.size() < 4)
			return allTableArea;

		/*
		 * search for textlines which share same row then saving them in
		 * 2-dimension integer array the minimal element is the textline-number
		 * in the 'list' the process is similar but simpler than
		 * column-searching, which will be explained in detail.
		 */
		ArrayList<ArrayList<Integer>> potentialTableRow = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				if (list.get(i).get_top() < this._titleLocation[3] + 20 * hp)
					continue;

				if (list.get(i).isInSameTableRow(list.get(j))) {
					boolean done = false;
					for (int k = 0; k < potentialTableRow.size(); k++) {
						for (int l = 0; l < potentialTableRow.get(k).size(); l++) {
							if (i == potentialTableRow.get(k).get(l)) {
								for (int m = 0; m < potentialTableRow.get(k).size(); m++) {
									if (j == potentialTableRow.get(k).get(m)) {
										done = true;
										break;
									}
								}
								if (!done) {
									potentialTableRow.get(k).add(j);
									done = true;
									break;
								}
							}
						}
						if (done)
							break;
					}
					if (!done) {
						ArrayList<Integer> newRow = new ArrayList<Integer>();
						newRow.add(i);
						newRow.add(j);
						potentialTableRow.add(newRow);
					}
				}
			}
		}

		/*
		 * Here we create an new data structure called tableColumn, have four
		 * attributes: columnCandidate: an integer array contain the items'
		 * number in the 'list' align: 3 boolean bit indicating whether this
		 * column is left, right or/and center aligned leftEdge and rightEdge:
		 * horizontal position info
		 */
		class tableColumn {

			public tableColumn() {
			}

			private ArrayList<Integer> columnCandidates = new ArrayList<Integer>();
			private boolean[] align = { false, false, false };
			private int leftEdge = -1;
			private int rightEdge = -1;
			private int aveHeight = -1;

			public ArrayList<Integer> getColumnCandidates() {
				return columnCandidates;
			}

			public void setColumnCandidates(ArrayList<Integer> columnCandidates) {
				this.columnCandidates = columnCandidates;
			}

			public boolean[] getAlign() {
				return align;
			}

			public void setAlign(boolean[] align) {
				this.align = align;
			}

			public int getLeftEdge() {
				return leftEdge;
			}

			public void setLeftEdge(int leftEdge) {
				this.leftEdge = leftEdge;
			}

			public int getRightEdge() {
				return rightEdge;
			}

			public void setRightEdge(int rightEdge) {
				this.rightEdge = rightEdge;
			}

			public int getAveHeight() {
				return aveHeight;
			}

			public void setAveHeight(int aveHeight) {
				this.aveHeight = aveHeight;
			}

			public int getColumnWidth() {
				return this.rightEdge - this.leftEdge;
			}

			public boolean leftThan(tableColumn that) {
				if (this.getLeftEdge() >= that.getRightEdge())
					return false;
				else if (that.getLeftEdge() >= this.getRightEdge())
					return true;
				else {
					int lineThis = -1, lineThat = -1;
					if (this.getAlign()[0])
						lineThis = this.getLeftEdge();
					else if (this.getAlign()[1])
						lineThis = this.getRightEdge();
					else
						lineThis = (this.getLeftEdge() + this.getRightEdge()) / 2;

					if (that.getAlign()[0])
						lineThat = that.getLeftEdge();
					else if (that.getAlign()[1])
						lineThat = that.getRightEdge();
					else
						lineThat = (that.getLeftEdge() + that.getRightEdge()) / 2;

					if (lineThis < lineThat)
						return true;
					else
						return false;
				}
			}

			public tableColumn combineColumn(tableColumn that, ArrayList<textLine> list) {
				tableColumn newColumn = new tableColumn();
				tableColumn c1 = this;
				tableColumn c2 = that;

				ArrayList<Integer> newColumnCandidates = new ArrayList<Integer>();
				for (int x = 0, y = 0; x < c1.getColumnCandidates().size() && y < c2.getColumnCandidates().size();) {
					if (list.get(c1.getColumnCandidates().get(x)).get_top() < list.get(c2.getColumnCandidates().get(y))
							.get_top()) {
						newColumnCandidates.add(c1.getColumnCandidates().get(x));
						x++;
						if (x >= c1.getColumnCandidates().size()) {
							for (int z = y; z < c2.getColumnCandidates().size(); z++)
								newColumnCandidates.add(c2.getColumnCandidates().get(z));
						}
					} else if (list.get(c1.getColumnCandidates().get(x)).get_top() > list
							.get(c2.getColumnCandidates().get(y)).get_top()) {
						newColumnCandidates.add(c2.getColumnCandidates().get(y));
						y++;
						if (y >= c2.getColumnCandidates().size()) {
							for (int z = x; z < c1.getColumnCandidates().size(); z++)
								newColumnCandidates.add(c1.getColumnCandidates().get(z));
						}
					} else {
						newColumnCandidates.add(c1.getColumnCandidates().get(x));
						x++;
						y++;
						if (x >= c1.getColumnCandidates().size()) {
							for (int z = y; z < c2.getColumnCandidates().size(); z++)
								newColumnCandidates.add(c2.getColumnCandidates().get(z));
						} else if (y >= c2.getColumnCandidates().size()) {
							for (int z = x; z < c1.getColumnCandidates().size(); z++)
								newColumnCandidates.add(c1.getColumnCandidates().get(z));
						}
					}
				}

				newColumn.setColumnCandidates(newColumnCandidates);
				newColumn.setAlign(c1.getAlign());
				newColumn.setLeftEdge(Math.min(c1.getLeftEdge(), c2.getLeftEdge()));
				newColumn.setRightEdge(Math.max(c1.getRightEdge(), c2.getRightEdge()));
				newColumn.setAveHeight((c1.getAveHeight() * c1.getColumnCandidates().size()
						+ c2.getAveHeight() * c2.getColumnCandidates().size())
						/ (c1.getColumnCandidates().size() + c2.getColumnCandidates().size()));

				return newColumn;
			}
		}

		/*
		 * search for textlines which share same column then saving them in
		 * tableColumn array, the process can be described as: 1. Compare each
		 * textline-pair in the list, taking 'i' and 'j' to do this loop. 2. If
		 * one textline-pair is in the same column, check whether one of the
		 * textlines is already in an existing tableColumn, taking 'k' and 'l'
		 * to do this loop. 3. If accidently both textlines in the target pair
		 * are in the existing tableColumn, pass. 4. If only one textline in the
		 * existing tableColumn, add the other one, and update column attribute.
		 * 5. If neither of the textlines in the pair belongs to any existing
		 * column, create a new one, and add these two.
		 */
		ArrayList<tableColumn> potentialTableColumn = new ArrayList<tableColumn>();
		for (int j = 1; j < list.size(); j++) {
			for (int i = j - 1; i >= 0; i--) {
				if (list.get(i).get_top() < this._titleLocation[3] + 20 * hp)
					continue;

				boolean[] alignResult = list.get(i).isInSameColumn(list.get(j), false, true);
				if (alignResult[0] || alignResult[1] || alignResult[2]) {
					boolean done = false;
					for (int k = 0; k < potentialTableColumn.size(); k++) {
						for (int l = 0; l < potentialTableColumn.get(k).getColumnCandidates().size(); l++) {
							if (i == potentialTableColumn.get(k).getColumnCandidates().get(l)) {
								if ((alignResult[0] & potentialTableColumn.get(k).getAlign()[0])
										|| (alignResult[1] & potentialTableColumn.get(k).getAlign()[1])
										|| (alignResult[2] & potentialTableColumn.get(k).getAlign()[2])) {
									for (int m = 0; m < potentialTableColumn.get(k).getColumnCandidates().size(); m++) {
										if (j == potentialTableColumn.get(k).getColumnCandidates().get(m)) {
											done = true;
											break;
										}
									}
									if (!done) {
										if (list.get(j).get_height() < potentialTableColumn.get(k).getAveHeight() * 1.5
												&& list.get(j).get_height() > potentialTableColumn.get(k).getAveHeight()
														* 0.67) {
											int newAveHeight = potentialTableColumn.get(k).getAveHeight()
													* potentialTableColumn.get(k).getColumnCandidates().size();
											newAveHeight = (newAveHeight + list.get(j).get_height())
													/ (potentialTableColumn.get(k).getColumnCandidates().size() + 1);
											potentialTableColumn.get(k).setAveHeight(newAveHeight);
											potentialTableColumn.get(k).getColumnCandidates().add(j);
											for (int x = 0; x < 3; x++)
												potentialTableColumn.get(k).getAlign()[x] = alignResult[x]
														& potentialTableColumn.get(k).getAlign()[x];
											if (list.get(j).get_left() < potentialTableColumn.get(k).getLeftEdge())
												potentialTableColumn.get(k).setLeftEdge(list.get(j).get_left());
											if (list.get(j).get_left() + list.get(j).get_width() > potentialTableColumn
													.get(k).getRightEdge())
												potentialTableColumn.get(k)
														.setRightEdge(list.get(j).get_left() + list.get(j).get_width());

										}
										done = true;
										break;
									}
								} else {
									done = true;
									break;
								}
							}
						}
						if (done)
							break;
					}
					if (!done) {
						if ((list.get(j).get_height() >= list.get(i).get_height() * 1.5)
								|| (list.get(i).get_height() >= list.get(j).get_height() * 1.5))
							continue;

						ArrayList<Integer> newColumnCandidates = new ArrayList<Integer>();
						newColumnCandidates.add(i);
						newColumnCandidates.add(j);

						tableColumn newColumn = new tableColumn();
						newColumn.setColumnCandidates(newColumnCandidates);
						newColumn.setAlign(alignResult);
						newColumn.setLeftEdge(list.get(i).get_left() < list.get(j).get_left() ? list.get(i).get_left()
								: list.get(j).get_left());
						newColumn.setRightEdge(list.get(i).get_left() + list.get(i).get_width() > list.get(j).get_left()
								+ list.get(j).get_width() ? list.get(i).get_left() + list.get(i).get_width()
										: list.get(j).get_left() + list.get(j).get_width());
						newColumn.setAveHeight((list.get(i).get_height() + list.get(j).get_height()) / 2);

						potentialTableColumn.add(newColumn);
					}
				}
			}
		}

		// order the columns from left to right by their location info
		for (int i = 0; i < potentialTableColumn.size(); i++)
			for (int j = i + 1; j < potentialTableColumn.size(); j++)
				if (!potentialTableColumn.get(i).leftThan(potentialTableColumn.get(j))) {
					tableColumn temp = new tableColumn();
					temp = potentialTableColumn.get(i);
					potentialTableColumn.set(i, potentialTableColumn.get(j));
					potentialTableColumn.set(j, temp);
				}

		/*
		 * Here we need to combine overlapped columns, it happens because of OCR
		 * error When the left column's right-edge is to the right of the right
		 * column's left-edage: overlapped and need to be judged. 2 boolean
		 * judgments: 'share' and 'separate' used to decide whether these two
		 * columns should be combined: 'share' means whether there is a textline
		 * shared by two column (different alignment, possible), which prove a
		 * combination 'separate' means whether there are two textline from
		 * different column but sharing the same row, which deny a combination
		 * So the combine prerequisite is: separate == false OR share == true;
		 *
		 * When combining, also use 'Merge sort'.
		 *
		 */
		for (int j = 0; j < potentialTableColumn.size() - 1; j++) {
			boolean restart = false;
			for (int h = j + 1; h < potentialTableColumn.size(); h++) {
				if (potentialTableColumn.get(j).getRightEdge() > potentialTableColumn.get(h).getLeftEdge()) {
					boolean seperat = false, share = false, interlaced = false, fullyCovered = false;
					for (int k = 0; k < potentialTableColumn.get(j).getColumnCandidates().size(); k++) {
						for (int l = 0; l < potentialTableColumn.get(h).getColumnCandidates().size(); l++) {
							if (potentialTableColumn.get(j).getColumnCandidates().get(k) == potentialTableColumn.get(h)
									.getColumnCandidates().get(l)) {
								if ((potentialTableColumn.get(j).getAlign()[0]
										& potentialTableColumn.get(h).getAlign()[0])
										|| (potentialTableColumn.get(j).getAlign()[1]
												& potentialTableColumn.get(h).getAlign()[1])
										|| (potentialTableColumn.get(j).getAlign()[2]
												& potentialTableColumn.get(h).getAlign()[2])) {
									share = true;
									break;
								} else if ((j > 0 && potentialTableColumn.get(h).getLeftEdge() > potentialTableColumn
										.get(j - 1).getRightEdge())
										|| (h < potentialTableColumn.size() - 1 && potentialTableColumn.get(j)
												.getRightEdge() < potentialTableColumn.get(h + 1).getLeftEdge())) {
									share = true;
									break;
								} else {
									int less = potentialTableColumn.get(j).getColumnCandidates()
											.size() >= potentialTableColumn.get(h).getColumnCandidates().size() ? h : j;
									if (potentialTableColumn.get(less).getColumnCandidates().size() <= 2) {
										potentialTableColumn.remove(less);
										restart = true;
										break;
									} else {
										if (less == h) {
											potentialTableColumn.get(h).getColumnCandidates().remove(l);
											l--;
										} else {
											potentialTableColumn.get(j).getColumnCandidates().remove(k);
											k--;
										}
										break;
									}
								}
							}
							textLine c1 = list.get(potentialTableColumn.get(j).getColumnCandidates().get(k));
							textLine c2 = list.get(potentialTableColumn.get(h).getColumnCandidates().get(l));
							if (c1.isInSameRow(c2)) {
								seperat = true;
								break;
							}
						}
						if (restart)
							break;
						if (seperat && share)
							break;
					}
					if (restart)
						break;

					int atop = list.get(potentialTableColumn.get(j).getColumnCandidates().get(0)).get_top();
					int abottom = list.get(potentialTableColumn.get(j).getColumnCandidates()
							.get(potentialTableColumn.get(j).getColumnCandidates().size() - 1)).get_bottom();
					int btop = list.get(potentialTableColumn.get(h).getColumnCandidates().get(0)).get_top();
					int bbottom = list.get(potentialTableColumn.get(h).getColumnCandidates()
							.get(potentialTableColumn.get(h).getColumnCandidates().size() - 1)).get_bottom();
					if ((atop - bbottom) * (abottom - btop) < 0)
						interlaced = true;

					int covered = Math.min(potentialTableColumn.get(j).getRightEdge(),
							potentialTableColumn.get(h).getRightEdge())
							- Math.max(potentialTableColumn.get(j).getLeftEdge(),
									potentialTableColumn.get(h).getLeftEdge());
					if (covered > Math.min(potentialTableColumn.get(j).getColumnWidth(),
							potentialTableColumn.get(h).getColumnWidth()) * 0.66) {
						if ((potentialTableColumn.get(j).getAlign()[0] & potentialTableColumn.get(h).getAlign()[0])
								|| (potentialTableColumn.get(j).getAlign()[1]
										& potentialTableColumn.get(h).getAlign()[1])
								|| (potentialTableColumn.get(j).getAlign()[2]
										& potentialTableColumn.get(h).getAlign()[2])) {
							if ((j > 0 && potentialTableColumn.get(h).getLeftEdge() > potentialTableColumn.get(j - 1)
									.getRightEdge())
									|| (h < potentialTableColumn.size() - 1 && potentialTableColumn.get(j)
											.getRightEdge() < potentialTableColumn.get(h + 1).getLeftEdge())) {
								if (atop - btop < 0) {
									int distance = btop - abottom;
									if (distance < list.get(potentialTableColumn.get(j).getColumnCandidates().get(0))
											.get_height() * 2)
										if (Math.max(potentialTableColumn.get(h).getAveHeight(),
												potentialTableColumn.get(j).getAveHeight()) < Math.min(
														potentialTableColumn.get(h).getAveHeight(),
														potentialTableColumn.get(j).getAveHeight()) * 1.25)
											fullyCovered = true;
								} else {
									int distance = atop - bbottom;
									if (distance < list.get(potentialTableColumn.get(j).getColumnCandidates().get(0))
											.get_height() * 2)
										if (Math.max(potentialTableColumn.get(h).getAveHeight(),
												potentialTableColumn.get(j).getAveHeight()) < Math.min(
														potentialTableColumn.get(h).getAveHeight(),
														potentialTableColumn.get(j).getAveHeight()) * 1.25)
											fullyCovered = true;
								}
							}
						}
					}

					if (!seperat && (share || interlaced || fullyCovered)) {
						tableColumn newColumn = new tableColumn();
						newColumn = potentialTableColumn.get(j).combineColumn(potentialTableColumn.get(h), list);

						ArrayList<tableColumn> temp = new ArrayList<tableColumn>();
						for (int k = 0; k < j; k++)
							temp.add(potentialTableColumn.get(k));
						temp.add(newColumn);
						for (int k = j + 1; k < h; k++)
							temp.add(potentialTableColumn.get(k));
						for (int k = h + 1; k < potentialTableColumn.size(); k++)
							temp.add(potentialTableColumn.get(k));
						potentialTableColumn.clear();
						potentialTableColumn = temp;

						j = -1;
						break;
					}
				}
				if (restart)
					break;
			}
			if (restart)
				j = -1;
		}

		/*
		 * crossedItem indicate a textline both in a row and a column detected
		 * above, they are the foundational table item searching pool. 3
		 * elements in crossedItemsInfo: [textlineNr in the list, RowNr,
		 * ColumnNr]
		 */
		ArrayList<int[]> crossedItemsInfo = new ArrayList<int[]>();
		for (int i = 0; i < potentialTableRow.size(); i++)
			for (int j = 0; j < potentialTableColumn.size(); j++) {
				for (int k = 0; k < potentialTableRow.get(i).size(); k++)
					for (int l = 0; l < potentialTableColumn.get(j).getColumnCandidates().size(); l++) {
						if (potentialTableRow.get(i).get(k) == potentialTableColumn.get(j).getColumnCandidates()
								.get(l)) {
							int[] info = { potentialTableRow.get(i).get(k), i, j };
							crossedItemsInfo.add(info);
						}
					}
			}

		/*
		 * slip those crossed items into groups, by shared row or column. The
		 * process is easy: build first group by the first crossedItem, then
		 * check the next item on whether it shares a same row or same column
		 * with any item already concluded in the group. If so, add the current
		 * to the certain group, otherwise build a new group by the current
		 * item.
		 *
		 * the groups save in 2-dimension array, first dimension is group,
		 * second is crossedItem, which contain 3 elements.
		 */
		ArrayList<ArrayList<int[]>> supposedTableItemsGroup = new ArrayList<ArrayList<int[]>>();
		for (int i = 0; i < crossedItemsInfo.size(); i++) {
			int[] currentItem = crossedItemsInfo.get(i);
			if (i == 0) {
				ArrayList<int[]> newGroup = new ArrayList<int[]>();
				newGroup.add(currentItem);
				supposedTableItemsGroup.add(newGroup);
				continue;
			}

			boolean needNewGroup = true;
			for (int j = 0; j < supposedTableItemsGroup.size(); j++) {
				for (int k = 0; k < supposedTableItemsGroup.get(j).size(); k++) {
					if (currentItem[1] == supposedTableItemsGroup.get(j).get(k)[1]
							|| currentItem[2] == supposedTableItemsGroup.get(j).get(k)[2]) {
						supposedTableItemsGroup.get(j).add(currentItem);
						needNewGroup = false;
						break;
					}
				}
			}

			if (needNewGroup) {
				ArrayList<int[]> newGroup = new ArrayList<int[]>();
				newGroup.add(currentItem);
				supposedTableItemsGroup.add(newGroup);
			}
		}

		/*
		 * for some reason, groups logically overlap and need to be combined.
		 *
		 * 'i' and 'j' used to go over all the groups 'k' and 'l' used to go
		 * over all the items in group(i) and group(j) if there's any shared
		 * item in two groups, combine them together when combining, using
		 * 'Merge sort', ordered by vertical coordinate
		 */
		if (supposedTableItemsGroup.size() > 1) {
			for (int i = 0; i < supposedTableItemsGroup.size(); i++)
				for (int j = i + 1; j < supposedTableItemsGroup.size(); j++) {
					boolean combine = false;
					for (int k = 0; k < supposedTableItemsGroup.get(i).size(); k++) {
						for (int l = 0; l < supposedTableItemsGroup.get(j).size(); l++) {
							if (supposedTableItemsGroup.get(i).get(k) == supposedTableItemsGroup.get(j).get(l)) {
								ArrayList<int[]> newGroup = new ArrayList<int[]>();
								for (int x = 0, y = 0; x < supposedTableItemsGroup.get(i).size()
										&& y < supposedTableItemsGroup.get(j).size();) {
									if (supposedTableItemsGroup.get(i).get(x)[0] < supposedTableItemsGroup.get(j)
											.get(y)[0]) {
										newGroup.add(supposedTableItemsGroup.get(i).get(x));
										x++;
										if (x >= supposedTableItemsGroup.get(i).size()) {
											for (int z = y; z < supposedTableItemsGroup.get(j).size(); z++)
												newGroup.add(supposedTableItemsGroup.get(j).get(z));
										}
									} else if (supposedTableItemsGroup.get(i).get(x)[0] > supposedTableItemsGroup.get(j)
											.get(y)[0]) {
										newGroup.add(supposedTableItemsGroup.get(j).get(y));
										y++;
										if (y >= supposedTableItemsGroup.get(j).size()) {
											for (int z = x; z < supposedTableItemsGroup.get(i).size(); z++)
												newGroup.add(supposedTableItemsGroup.get(i).get(z));
										}
									} else {
										newGroup.add(supposedTableItemsGroup.get(i).get(x));
										x++;
										y++;
										if (x >= supposedTableItemsGroup.get(i).size()) {
											for (int z = y; z < supposedTableItemsGroup.get(j).size(); z++)
												newGroup.add(supposedTableItemsGroup.get(j).get(z));
										} else if (y >= supposedTableItemsGroup.get(j).size()) {
											for (int z = x; z < supposedTableItemsGroup.get(i).size(); z++)
												newGroup.add(supposedTableItemsGroup.get(i).get(z));
										}
									}
								}
								supposedTableItemsGroup.set(i, newGroup);
								supposedTableItemsGroup.remove(j);
								combine = true;
								break;
							}
						}
						if (combine)
							break;
					}
					if (combine)
						j--;
				}
		}

		/*
		 * Now draw a rectangle for each crossed item group. Make it an area.
		 *
		 * Then search inside the area to find any textlines inside the area but
		 * not "crossed". Such situation happens when the accuracy of OCR is not
		 * so high. The textline found will be added into the group.
		 *
		 * Please note: only when a textline belong to row or column, it may be
		 * added.
		 */
		for (int i = 0; i < supposedTableItemsGroup.size(); i++) {
			if (supposedTableItemsGroup.get(i).size() <= 1)
				continue;

			int[] tableEdge = { -1, -1, -1, -1 };
			for (int j = 0; j < supposedTableItemsGroup.get(i).size(); j++) {
				textLine current = list.get(supposedTableItemsGroup.get(i).get(j)[0]);
				if (tableEdge[0] < 0 || current.get_left() < tableEdge[0])
					tableEdge[0] = current.get_left();
				if (tableEdge[1] < 0 || current.get_left() + current.get_width() > tableEdge[1])
					tableEdge[1] = current.get_left() + current.get_width();
				if (tableEdge[2] < 0 || current.get_top() < tableEdge[2])
					tableEdge[2] = current.get_top();
				if (tableEdge[3] < 0 || current.get_bottom() > tableEdge[3])
					tableEdge[3] = current.get_bottom();
			}

			ArrayList<int[]> newGroup = new ArrayList<int[]>();
			for (int j = 0; j < list.size(); j++) {
				if (list.get(j).isInside(tableEdge)) {
					int rowNum = -1, columnNum = -1;
					for (int k = 0; k < potentialTableRow.size() && rowNum < 0; k++)
						for (int l = 0; l < potentialTableRow.get(k).size(); l++)
							if (j == potentialTableRow.get(k).get(l)) {
								rowNum = k;
								break;
							}
					for (int k = 0; k < potentialTableColumn.size() && columnNum < 0; k++)
						for (int l = 0; l < potentialTableColumn.get(k).getColumnCandidates().size(); l++)
							if (j == potentialTableColumn.get(k).getColumnCandidates().get(l)) {
								columnNum = k;
								break;
							}

					if (rowNum >= 0 && columnNum >= 0) {
						int[] cell = { j, rowNum, columnNum };
						newGroup.add(cell);
					} else if (rowNum >= 0) {
						ArrayList<Integer> newColumnCandidates = new ArrayList<Integer>();
						newColumnCandidates.add(j);
						boolean[] alignResult = { false, false, true };

						tableColumn newColumn = new tableColumn();
						newColumn.setColumnCandidates(newColumnCandidates);
						newColumn.setAlign(alignResult);
						newColumn.setLeftEdge(list.get(j).get_left());
						newColumn.setRightEdge(list.get(j).get_left() + list.get(j).get_width());
						newColumn.setAveHeight(list.get(j).get_height());

						for (int k = 0; k < potentialTableColumn.size(); k++) {
							if (newColumn.leftThan(potentialTableColumn.get(k))) {
								if ((k > 0 ? newColumn.getLeftEdge() > potentialTableColumn.get(k - 1).getRightEdge()
										: true)
										&& newColumn.getRightEdge() < potentialTableColumn.get(k).getLeftEdge()) {
									ArrayList<tableColumn> temp = new ArrayList<tableColumn>();
									for (int l = 0; l < k; l++)
										temp.add(potentialTableColumn.get(l));
									temp.add(newColumn);
									for (int l = k; l < potentialTableColumn.size(); l++)
										temp.add(potentialTableColumn.get(l));

									for (int l = 0; l < newGroup.size(); l++)
										if (newGroup.get(l)[2] >= k)
											newGroup.get(l)[2]++;

									int[] cell = { j, rowNum, k };
									newGroup.add(cell);

									potentialTableColumn.clear();
									potentialTableColumn = temp;
									break;
								} else if (newColumn.getRightEdge() > potentialTableColumn.get(k).getLeftEdge()
										&& newColumn.getRightEdge() - potentialTableColumn.get(k).getLeftEdge() > Math
												.min(newColumn.getColumnWidth(),
														potentialTableColumn.get(k).getColumnWidth() * 0.5)) {
									boolean seperate = false;
									for (int l = 0; l < potentialTableColumn.get(k).getColumnCandidates().size(); l++)
										if (list.get(j).isInSameRow(
												list.get(potentialTableColumn.get(k).getColumnCandidates().get(l)))) {
											seperate = true;
											break;
										}

									if (!seperate) {
										newColumn = newColumn.combineColumn(potentialTableColumn.get(k), list);
										potentialTableColumn.set(k, newColumn);
										int[] cell = { j, rowNum, k };
										newGroup.add(cell);
									}
									break;
								} else if (k > 0
										&& newColumn.getLeftEdge() < potentialTableColumn.get(k - 1).getRightEdge()
										&& potentialTableColumn.get(k - 1).getRightEdge()
												- newColumn.getLeftEdge() > Math.min(newColumn.getColumnWidth(),
														potentialTableColumn.get(k - 1).getColumnWidth() * 0.5)) {
									boolean seperate = false;
									for (int l = 0; l < potentialTableColumn.get(k - 1).getColumnCandidates()
											.size(); l++)
										if (list.get(j).isInSameRow(list
												.get(potentialTableColumn.get(k - 1).getColumnCandidates().get(l)))) {
											seperate = true;
											break;
										}

									if (!seperate) {
										newColumn = newColumn.combineColumn(potentialTableColumn.get(k - 1), list);
										potentialTableColumn.set(k - 1, newColumn);
										int[] cell = { j, rowNum, k - 1 };
										newGroup.add(cell);
									}
									break;
								} else
									break;
							}
						}
					} else if (columnNum >= 0) { }
				}
			}
			supposedTableItemsGroup.get(i).clear();
			supposedTableItemsGroup.set(i, newGroup);
		}

		// Here we check whether a potential table area is really a table,
		// remove the fake ones
		// The whole process is complicated, so we explain it step by step
		for (int i = 0; i < supposedTableItemsGroup.size(); i++) {
			if (supposedTableItemsGroup.get(i).size() <= 1) {
				supposedTableItemsGroup.remove(i);
				i--;
				continue;
			}

			/*
			 * Two tasks here in this traversal process: 1. find all columns and
			 * all rows in this group, build a X*Y table prototype. 2. calculate
			 * item mark (how possible to be a table item) by its text type and
			 * length
			 */
			ArrayList<Integer> allColumn = new ArrayList<Integer>();
			ArrayList<Integer> allRow = new ArrayList<Integer>();
			double fullMark = 0;
			for (int j = 0; j < supposedTableItemsGroup.get(i).size(); j++) {
				boolean newColumn = true;
				for (int k = 0; k < allColumn.size(); k++)
					if (allColumn.get(k) == supposedTableItemsGroup.get(i).get(j)[2]) {
						newColumn = false;
						break;
					}
				if (newColumn)
					allColumn.add(supposedTableItemsGroup.get(i).get(j)[2]);

				boolean newRow = true;
				for (int k = 0; k < allRow.size(); k++)
					if (allRow.get(k) == supposedTableItemsGroup.get(i).get(j)[1]) {
						newRow = false;
						break;
					}

				if (newRow)
					allRow.add(supposedTableItemsGroup.get(i).get(j)[1]);

				textLine current = list.get(supposedTableItemsGroup.get(i).get(j)[0]);
				algorithmInterface ai = new algorithmInterface();
				double mark = 0;
				if (ai.isStatNum(current.get_text())) {
					mark = 2.5;
				} else {
					if (!current.get_text().contains(" "))
						mark = 1;
					else if (ai.isOptionNum(current.get_text()))
						mark = 2.5;
					else if (current.get_text().length() > 40)
						mark = -2;
					else if (current.get_text().length() >= 25)
						mark = -1;
					else if (current.get_text().length() <= 10)
						mark = 0.5;
					else
						mark = 0;
				}
				fullMark += mark;
			}
			if (allColumn.size() > 1)
				for (int j = 0; j < allColumn.size(); j++)
					for (int k = j + 1; k < allColumn.size(); k++)
						if (allColumn.get(k) < allColumn.get(j)) {
							int temp = allColumn.get(k);
							allColumn.set(k, allColumn.get(j));
							allColumn.set(j, temp);
						}

			double aveMark = fullMark / (double) supposedTableItemsGroup.get(i).size();

			// columnBonus: if all item in a column are numbers or single-word,
			// this group is much more possible to be a real table.
			double columnBonus = 0;
			for (int j = 0; j < allColumn.size(); j++) {
				if (potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().size() < 4)
					continue;

				int totalNum = 0;
				int totalSingleWord = 0;
				for (int k = 0; k < potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().size(); k++) {
					int current = potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().get(k);
					algorithmInterface ai = new algorithmInterface();
					if (ai.isStatNum(list.get(current).get_text()))
						totalNum++;
					else if (!list.get(current).get_text().contains(" "))
						totalSingleWord++;
				}

				double tolorentRate = 0;
				if (potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().size() >= 6)
					tolorentRate = 0.5;
				else if (potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().size() >= 4)
					tolorentRate = 0.34;

				if (totalNum * (1 + tolorentRate) >= potentialTableColumn.get(allColumn.get(j)).getColumnCandidates()
						.size())
					columnBonus += 1.01;
				else if (totalSingleWord * (1 + tolorentRate) >= potentialTableColumn.get(allColumn.get(j))
						.getColumnCandidates().size())
					columnBonus += 0.5;
			}

			// distanceDeduction: if neighboring columns are far away, less
			// possible to be a table.
			double distanceDeduction = 0;
			for (int j = 0; j < allColumn.size() - 1; j++) {
				if (potentialTableColumn.get(allColumn.get(j)).getRightEdge() > potentialTableColumn
						.get(allColumn.get(j + 1)).getLeftEdge())
					continue;
				else {
					boolean overlap = false;
					for (int k = 0; k < j; k++)
						if (potentialTableColumn.get(allColumn.get(k)).getRightEdge() > potentialTableColumn
								.get(allColumn.get(j)).getRightEdge())
							overlap = true;
					if (overlap)
						continue;

					int gap = potentialTableColumn.get(allColumn.get(j + 1)).getLeftEdge()
							- potentialTableColumn.get(allColumn.get(j)).getRightEdge();
					int width = Math.min(potentialTableColumn.get(allColumn.get(j)).getColumnWidth(),
							potentialTableColumn.get(allColumn.get(j + 1)).getColumnWidth());
					if (gap < width / 2) {
						distanceDeduction -= 0.2;
						continue;
					}

					width = Math.max(width, this._pageWidth / 8);
					if (gap > width * 3)
						distanceDeduction += 3;
					else if (gap > width * 2)
						distanceDeduction += 2;
					else if (gap > width * 1.75)
						distanceDeduction += 1;
					else if (gap > width * 1.5)
						distanceDeduction += 0.5;
					else if (gap > width)
						distanceDeduction += 0.2;

				}
			}

			// If there's only two columns, they are more likely to be
			// multi-column text layout system, not a table
			double twoColumnTextDeduction = 0;
			if (allColumn.size() == 2) {
				if (potentialTableColumn.get(allColumn.get(0)).getRightEdge() < potentialTableColumn
						.get(allColumn.get(1)).getLeftEdge()) {
					if (potentialTableColumn.get(allColumn.get(0)).getRightEdge() < this._pageWidth * 0.55
							&& potentialTableColumn.get(allColumn.get(1)).getLeftEdge() > this._pageWidth * 0.45) {
						twoColumnTextDeduction = 1;
						if (potentialTableColumn.size() == 2)
							twoColumnTextDeduction += 1;
						if (potentialTableColumn.get(allColumn.get(0)).getColumnCandidates().size()
								+ potentialTableColumn.get(allColumn.get(1)).getColumnCandidates().size() >= list.size()
										* 0.8)
							twoColumnTextDeduction += 1;
						if (columnBonus > 1)
							twoColumnTextDeduction = twoColumnTextDeduction - 2 < 0 ? 0 : twoColumnTextDeduction - 2;
						if (potentialTableColumn.get(allColumn.get(0)).getColumnCandidates().size()
								+ potentialTableColumn.get(allColumn.get(1)).getColumnCandidates()
										.size() == supposedTableItemsGroup.get(i).size())
							twoColumnTextDeduction = twoColumnTextDeduction - 2 < 0 ? 0 : twoColumnTextDeduction - 1;
						if (list.get(supposedTableItemsGroup.get(i).get(0)[0]).get_top() > this._pageHeight * 0.55)
							twoColumnTextDeduction = twoColumnTextDeduction - 1 < 0 ? 0 : twoColumnTextDeduction - 1;
					}
				}
			}

			// consider all factors and then remove fake tables.
			aveMark = aveMark + columnBonus - distanceDeduction - twoColumnTextDeduction;
			if ((allColumn.size() < 2 || allRow.size() < 2) && aveMark < 2) {
				supposedTableItemsGroup.remove(i);
				i--;
				continue;
			} else if (aveMark < 0.5 && allColumn.size() < 3 && supposedTableItemsGroup.get(i).size() <= 6) {
				supposedTableItemsGroup.remove(i);
				i--;
				continue;
			} else if (aveMark < 0) {
				if (distanceDeduction >= 1.5) {
					boolean removeOneRowAndRetry = false;
					for (int j = allColumn.size() - 1; j > 0; j--) {
						int gap = potentialTableColumn.get(allColumn.get(j)).getLeftEdge()
								- potentialTableColumn.get(allColumn.get(j - 1)).getRightEdge();
						if (gap <= 0)
							continue;
						int width = Math.min(potentialTableColumn.get(allColumn.get(j)).getColumnWidth(),
								potentialTableColumn.get(allColumn.get(j - 1)).getColumnWidth());
						width = Math.max(width, this._pageWidth / 8);
						if (gap > width * 1.75) {
							int cellInRight = 0, cellInLeft = 0;
							for (int k = 0; k < supposedTableItemsGroup.get(i).size(); k++) {
								if (supposedTableItemsGroup.get(i).get(k)[2] == allColumn.get(j - 1))
									cellInLeft++;
								else if (supposedTableItemsGroup.get(i).get(k)[2] == allColumn.get(j))
									cellInRight++;
							}
							// LoggerSingleton.info("****" + j + '\t' +
							// cellInLeft + '\t' + cellInRight);
							if (cellInRight <= 1 || cellInRight < cellInLeft / 3) {
								for (int k = 0; k < supposedTableItemsGroup.get(i).size(); k++)
									if (supposedTableItemsGroup.get(i).get(k)[2] == allColumn.get(j)) {
										// LoggerSingleton.info("****Remove <" +
										// list.get(supposedTableItemsGroup.get(i).get(k)[0]).get_text()
										// + "> and retry this area****");
										supposedTableItemsGroup.get(i).remove(k);
										removeOneRowAndRetry = true;
										k--;
									}
								if (removeOneRowAndRetry)
									break;
							} else if (cellInLeft <= 1 || cellInLeft < cellInRight / 3) {
								for (int k = 0; k < supposedTableItemsGroup.get(i).size(); k++)
									if (supposedTableItemsGroup.get(i).get(k)[2] == allColumn.get(j - 1)) {
										// LoggerSingleton.info("****Remove <" +
										// list.get(supposedTableItemsGroup.get(i).get(k)[0]).get_text()
										// + "> and retry this area****");
										supposedTableItemsGroup.get(i).remove(k);
										removeOneRowAndRetry = true;
										k--;
									}
								if (removeOneRowAndRetry)
									break;
							}
						}
					}
					if (removeOneRowAndRetry)
						continue;
				}

				supposedTableItemsGroup.remove(i);
				i--;
				continue;
			}

		}

		/*
		 * Here extend the confirmed table area by searching nearby text First,
		 * find all columns and rows, and calculate an initial table area. Then,
		 * find the largest column gap between existing columns and calculate an
		 * average row height (including the text and the gap) Next, check all
		 * free textlines (not the crossed item in the group), if its horizontal
		 * or vertical distance to the initial table area is acceptable (1.25 *
		 * columnGap and 1.5 * rowHeight), expand the table area by this new
		 * table item. Repeat last step until the table area hasn't changed
		 * anymore. Moreover, search the renewed area to check whether there's
		 * "half-involved" text-line, if so, expand to involve all. Finally,
		 * search for the potential header line (Don't need to be aligned, but
		 * close and not too wide)
		 */
		for (int i = 0; i < supposedTableItemsGroup.size(); i++) {
			ArrayList<Integer> allColumn = new ArrayList<Integer>();
			ArrayList<Integer> allRow = new ArrayList<Integer>();
			int[] tableEdge = { -1, -1, -1, -1 };
			for (int j = 0; j < supposedTableItemsGroup.get(i).size(); j++) {
				boolean newColumn = true;
				for (int k = 0; k < allColumn.size(); k++)
					if (allColumn.get(k) == supposedTableItemsGroup.get(i).get(j)[2]) {
						newColumn = false;
						break;
					}
				if (newColumn)
					allColumn.add(supposedTableItemsGroup.get(i).get(j)[2]);

				boolean newRow = true;
				for (int k = 0; k < allRow.size(); k++)
					if (allRow.get(k) == supposedTableItemsGroup.get(i).get(j)[1]) {
						newRow = false;
						break;
					}
				if (newRow)
					allRow.add(supposedTableItemsGroup.get(i).get(j)[1]);

				textLine current = list.get(supposedTableItemsGroup.get(i).get(j)[0]);
				if (tableEdge[0] < 0 || current.get_left() < tableEdge[0])
					tableEdge[0] = current.get_left();
				if (tableEdge[1] < 0 || current.get_left() + current.get_width() > tableEdge[1])
					tableEdge[1] = current.get_left() + current.get_width();
				if (tableEdge[2] < 0 || current.get_top() < tableEdge[2])
					tableEdge[2] = current.get_top();
				if (tableEdge[3] < 0 || current.get_bottom() > tableEdge[3])
					tableEdge[3] = current.get_bottom();
			}
			// LoggerSingleton.info("Table Area: [" + tableEdge[0] + ", " +
			// tableEdge[1] + ", " + tableEdge[2] + ", " + tableEdge[3] + "]");
			if ((tableEdge[0] > tableEdge[1]) || (tableEdge[2] > tableEdge[3])) {
				LoggerSingleton.info("Table Detection Error!");
				continue;
			}

			if (allColumn.size() > 1)
				for (int j = 0; j < allColumn.size(); j++)
					for (int k = j + 1; k < allColumn.size(); k++)
						if (allColumn.get(k) < allColumn.get(j)) {
							int temp = allColumn.get(k);
							allColumn.set(k, allColumn.get(j));
							allColumn.set(j, temp);
						}

			int biggestColumnGap = 0;
			for (int j = 0; j < allColumn.size() - 1; j++) {
				if (potentialTableColumn.get(allColumn.get(j)).getColumnWidth() > biggestColumnGap * 2.5)
					biggestColumnGap = (int) (potentialTableColumn.get(allColumn.get(j)).getColumnWidth() / 2.5);
				if (potentialTableColumn.get(allColumn.get(j + 1)).getColumnWidth() > biggestColumnGap * 2.5)
					biggestColumnGap = (int) (potentialTableColumn.get(allColumn.get(j + 1)).getColumnWidth() / 2.5);
				if (potentialTableColumn.get(allColumn.get(j + 1)).getLeftEdge()
						- potentialTableColumn.get(allColumn.get(j)).getRightEdge() > biggestColumnGap)
					biggestColumnGap = potentialTableColumn.get(allColumn.get(j + 1)).getLeftEdge()
							- potentialTableColumn.get(allColumn.get(j)).getRightEdge();
			}

			int aveRowHeight = (tableEdge[3] - tableEdge[2]) / allRow.size();

			boolean stable = false;
			while (!stable) {
				stable = true;
				for (int j = 0; j < allRow.size(); j++) {
					for (int k = 0; k < potentialTableRow.get(allRow.get(j)).size(); k++) {
						textLine current = list.get(potentialTableRow.get(allRow.get(j)).get(k));
						if (!current.isInside(tableEdge)) {
							if (current.isAwayFrom(tableEdge)) {
								if (current.get_left() < tableEdge[0]
										&& current.get_left() + current.get_width() > tableEdge[1])
									continue;

								double bonusRate = 1;
								algorithmInterface ai = new algorithmInterface();
								if (ai.isStatNum(current.get_text()))
									bonusRate = 2;
								else if (!current.get_text().contains(" "))
									bonusRate = 1.5;

								if (current.get_left() + current.get_width() < tableEdge[0]) {
									if (tableEdge[0] - current.get_left() - current.get_width() <= biggestColumnGap
											* 1.5 * bonusRate) {
										if (current.get_left() < tableEdge[0])
											tableEdge[0] = current.get_left();
										if (current.get_top() < tableEdge[2])
											tableEdge[2] = current.get_top();
										if (current.get_bottom() > tableEdge[3])
											tableEdge[3] = current.get_bottom();
										stable = false;
									}
								} else if (current.get_left() > tableEdge[1]) {
									if (current.get_left() - tableEdge[1] <= biggestColumnGap * 1.5 * bonusRate) {
										if (current.get_left() + current.get_width() > tableEdge[1])
											tableEdge[1] = current.get_left() + current.get_width();
										if (current.get_top() < tableEdge[2])
											tableEdge[2] = current.get_top();
										if (current.get_bottom() > tableEdge[3])
											tableEdge[3] = current.get_bottom();
										stable = false;
									}
								}
							} else {
								if (current.get_left() < tableEdge[0])
									tableEdge[0] = current.get_left();
								if (current.get_left() + current.get_width() > tableEdge[1])
									tableEdge[1] = current.get_left() + current.get_width();
								if (current.get_top() < tableEdge[2])
									tableEdge[2] = current.get_top();
								if (current.get_bottom() > tableEdge[3])
									tableEdge[3] = current.get_bottom();
								stable = false;
							}
						}
					}
				}
				for (int j = 0; j < allColumn.size(); j++) {
					for (int k = 0; k < potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().size(); k++) {
						textLine current = list
								.get(potentialTableColumn.get(allColumn.get(j)).getColumnCandidates().get(k));
						if (!current.isInside(tableEdge)) {
							if (current.isAwayFrom(tableEdge)) {
								if (current.get_left() < tableEdge[0]
										&& current.get_left() + current.get_width() > tableEdge[1])
									continue;

								double bonusRate = 1;
								algorithmInterface ai = new algorithmInterface();
								if (ai.isStatNum(current.get_text()))
									bonusRate = 2;
								else if (!current.get_text().contains(" "))
									bonusRate = 1.5;
								else {
									if (j > 0 && current.get_left() < potentialTableColumn.get(allColumn.get(j - 1))
											.getRightEdge())
										bonusRate = 0.25;
									else if (j < allColumn.size() - 1
											&& current.get_left() + current.get_width() > potentialTableColumn
													.get(allColumn.get(j + 1)).getLeftEdge())
										bonusRate = 0.25;
								}

								if (current.get_top() + current.get_height() < tableEdge[2]) {
									if (tableEdge[2] - current.get_top() - current.get_height() <= aveRowHeight * 1.5
											* bonusRate) {
										if (current
												.get_height() < potentialTableColumn.get(allColumn.get(0))
														.getAveHeight() * 1.5
												&& current.get_height() > potentialTableColumn.get(allColumn.get(0))
														.getAveHeight() * 0.67) {
											if (current.get_left() < tableEdge[0])
												tableEdge[0] = current.get_left();
											if (current.get_left() + current.get_width() > tableEdge[1])
												tableEdge[1] = current.get_left() + current.get_width();
											if (current.get_top() < tableEdge[2])
												tableEdge[2] = current.get_top();
											stable = false;
										}
									}
								} else if (current.get_top() > tableEdge[3]) {
									if (current.get_top() - tableEdge[3] <= aveRowHeight * 1.5 * bonusRate) {
										if (current
												.get_height() < potentialTableColumn.get(allColumn.get(0))
														.getAveHeight() * 1.5
												&& current.get_height() > potentialTableColumn.get(allColumn.get(0))
														.getAveHeight() * 0.67) {
											if (current.get_left() < tableEdge[0])
												tableEdge[0] = current.get_left();
											if (current.get_left() + current.get_width() > tableEdge[1])
												tableEdge[1] = current.get_left() + current.get_width();
											if (current.get_bottom() > tableEdge[3])
												tableEdge[3] = current.get_bottom();
											stable = false;
										}
									}
								}
							} else {
								if (current.get_left() < tableEdge[0])
									tableEdge[0] = current.get_left();
								if (current.get_left() + current.get_width() > tableEdge[1])
									tableEdge[1] = current.get_left() + current.get_width();
								if (current.get_top() < tableEdge[2])
									tableEdge[2] = current.get_top();
								if (current.get_bottom() > tableEdge[3])
									tableEdge[3] = current.get_bottom();
								stable = false;
							}
						}
					}
				}
			}

			// here starts the "half-involved" check
			stable = false;
			while (!stable) {
				stable = true;
				for (int j = 0; j < list.size(); j++)
					if (!list.get(j).isInside(tableEdge) && !list.get(j).isAwayFrom(tableEdge)) {
						if (list.get(j).get_left() < tableEdge[0])
							tableEdge[0] = list.get(j).get_left();
						if (list.get(j).get_left() + list.get(j).get_width() > tableEdge[1])
							tableEdge[1] = list.get(j).get_left() + list.get(j).get_width();
						if (list.get(j).get_top() < tableEdge[2])
							tableEdge[2] = list.get(j).get_top();
						if (list.get(j).get_bottom() > tableEdge[3])
							tableEdge[3] = list.get(j).get_bottom();
						stable = false;
					}
			}
			if (tableEdge[1] - tableEdge[0] > (tableEdge[3] - tableEdge[2]) * 10) {
				continue;
			} else {

				double aveMark = 0;
				int aveHeight = 0;
				int allInTable = 0;
				int accumulatedArea = 0;
				for (int j = 0; j < list.size(); j++) {
					textLine current = list.get(j);
					algorithmInterface ai = new algorithmInterface();
					double mark = 0;

					if (!current.isInside(tableEdge))
						continue;

					if (ai.isStatNum(current.get_text())) {
						mark = 2.5;
					} else {
						if (!current.get_text().contains(" "))
							mark = 1;
						else if (ai.isOptionNum(current.get_text()))
							mark = 2.5;
						else if (current.get_text().length() > 40)
							mark = -1;
						else if (current.get_text().length() >= 25)
							mark = -0.5;
						else if (current.get_text().length() <= 10)
							mark = 0.5;
						else
							mark = 0;
					}
					aveMark += mark;
					aveHeight += current.get_height();
					allInTable++;
					accumulatedArea += list.get(j).get_height() * list.get(j).get_width();
				}

				aveMark = aveMark / (double) allInTable;
				aveHeight = aveHeight / allInTable;

				int wholeArea = (tableEdge[1] - tableEdge[0]) * (tableEdge[3] - tableEdge[2]);
				double rate = (double) accumulatedArea / (double) wholeArea;
				double aspectRatio = (double) (tableEdge[1] - tableEdge[0]) / (double) (tableEdge[3] - tableEdge[2]);
				double finalEvaluation = Math.pow(Math.E, aveMark) / 3;
				finalEvaluation += Math.log(allInTable) * supposedTableItemsGroup.get(i).size()
						/ (allColumn.size() * allRow.size());
				finalEvaluation += Math.log(Math.pow(aveHeight, 2))
						* Math.pow(Math.log(Math.log(Math.pow(wholeArea, 0.25))), 3)
						/ Math.pow(Math.E, Math.pow(Math.abs(rate - 0.2), 1.0 / 3));

				BufferedWriter output = new BufferedWriter(new FileWriter(get_tog_stats_file(), true));
				output.append(lectureID + "-" + (this.get_PageNum() > 9 ? "" : "0") + this.get_PageNum() + '\t'
						+ wholeArea + '\t' + rate + '\t' + allInTable + '\t' + supposedTableItemsGroup.get(i).size()
						+ "\t" + allColumn.size() + "\t" + allRow.size() + "\t" + aveMark + "\t" + aveHeight + "\t"
						+ aspectRatio);
				output.newLine();
				output.close();

				if (finalEvaluation > 6.5) {
					allTableArea.add(tableEdge);
					LoggerSingleton.info("Table Detected: [" + tableEdge[0] + ", " + tableEdge[1] + ", " + tableEdge[2]
							+ ", " + tableEdge[3] + "]");
				} else
					LoggerSingleton.info(
							"Group " + (i + 1) + " fails the general check ( " + finalEvaluation + " ), removed.");
			}

		}
		return allTableArea;
	}

	private File get_tog_stats_file() {
		File f = new File(Constants.DEFAULT_TOG_STATS);
		if (!f.canWrite())
			f = new File("tog_stats.txt");

		return f;
	}

}
