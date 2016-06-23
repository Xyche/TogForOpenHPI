package dataStructure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import dataStructure.textOutline.counts;
import helper.Constants;
import helper.FilterableList;
import helper.FilterableList.FilterFunc;
import helper.LoggerSingleton;
import helper.StaticMethods;
import helper.enums.SlidePageType;
import helper.enums.TextLineType;
import sharedMethods.algorithmInterface;

public class outlineGenerator {

	private int pageWidth = 1024;
	private int pageHeight = 768;
	private String lectureID = "";
	private String workingDir = "";
	private boolean initial = true;
	
	private boolean isTag = false;
	
	public outlineGenerator() {
		this(Constants.DEFAULT_WIDTH, Constants.DEFAULT_HEIGHT, "", "");
	}

	public outlineGenerator(int pageWidth, int pageHeight, String workingDir, String lectureID) {
		set_pageWidth(pageWidth);
		set_pageHeight(pageHeight);
		set_lectureID(lectureID);
		set_workingDir(workingDir);
	}


	// 4 parameters in _potentialTitleArea : top, height, align(0:left,
	// 1:middle), axis(left or middle)
	private ArrayList<int[]> potentialTitleArea = new ArrayList<int[]>();
	private ArrayList<int[]> lastRoundTableAreas = new ArrayList<int[]>();

	private ArrayList<Integer> potentialHierarchicalGap = new ArrayList<Integer>();
	private ArrayList<Integer> lastRoundGaps = new ArrayList<Integer>();

	private boolean beginWithLowCaseLetter = false;
	private boolean haveSignBeforeSubtopic = false;

	private boolean lastRoundLowCaseStart = false;
	private boolean lastRoundBeginningDot = false;

	public boolean isInitial() {
		return initial;
	}

	public int get_pageWidth() {
		return pageWidth;
	}

	public void set_pageWidth(int _pageWidth) {
		this.pageWidth = _pageWidth;
	}

	public int get_pageHeight() {
		return pageHeight;
	}

	public void set_pageHeight(int _pageHeight) {
		this.pageHeight = _pageHeight;
	}

	public String get_workingDir() {
		return this.workingDir;
	}

	public String get_lectureID() {
		return lectureID;
	}

	public void set_workingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	public void set_lectureID(String _lectureID) {
		this.lectureID = _lectureID;
	}

	
	
	
	public ArrayList<int[]> get_potentialTitleArea() {
		return potentialTitleArea;
	}

	public void set_potentialTitleArea(ArrayList<int[]> _potentialTitleArea) {
		this.potentialTitleArea = _potentialTitleArea;
	}

	public ArrayList<Integer> get_potentialHierarchicalGap() {
		return potentialHierarchicalGap;
	}

	public void set_potentialHierarchicalGap(ArrayList<Integer> _potentialHierarchicalGap) {
		this.potentialHierarchicalGap = _potentialHierarchicalGap;
	}

	public boolean is_beginWithLowCaseLetter() {
		return beginWithLowCaseLetter;
	}

	public void set_beginWithLowCaseLetter(boolean _beginWithLowCaseLetter) {
		this.beginWithLowCaseLetter = _beginWithLowCaseLetter;
	}

	public boolean is_haveSignBeforeSubtopic() {
		return haveSignBeforeSubtopic;
	}

	public void set_haveSignBeforeSubtopic(boolean _haveSignBeforeSubtopic) {
		this.haveSignBeforeSubtopic = _haveSignBeforeSubtopic;
	}


	// main generation method
	public FilterableList<textOutline> generate(FilterableList<textLine> tll, boolean havePPTX, boolean havePDF, String referenceFilePath,
			boolean changeBBImageNames) throws Exception {

		this.potentialTitleArea.clear();
		this.potentialHierarchicalGap.clear();
		
		tll = removeLogo(tll);
		if (havePPTX){
			LoggerSingleton.info("< STEP 2: Delete logo which appears in same position of many pages for video stream>");
			
			FilterableList<slidePage> sps = generateSlidePageStructure(tll, "< STEP 3-1: Find title and load in hierarchy for video stream>");

			LoggerSingleton.show("< RESULT after step 3 (video stream)>", sps);
			step3(sps);

			FilterableList<slidePage> sps_pptx = new pptParser().analyzePPTX(referenceFilePath);
			LoggerSingleton.show("< RESULT after step 3 (PPTX file)>", sps_pptx);

			return generate(sps, sps_pptx);
		} else if (havePDF){
			LoggerSingleton.info("< STEP 2-1: Delete logo which appears in same position of many pages for video stream>");
			
			FilterableList<textLine> tll_pdf = new pdfParser().analyzePDF(referenceFilePath); 
			LoggerSingleton.info("< STEP 2-2: Delete logo which appears in same position of many pages for PDF file>");
			tll_pdf = removeLogo(tll_pdf);

			FilterableList<slidePage> sps = generateSlidePageStructure(tll, "< STEP 3-1: Find title and load in hierarchy for video stream>");
			LoggerSingleton.show("< RESULT after step 3 (video stream)>", sps);

			FilterableList<slidePage> sps_pdf = generateSlidePageStructure(tll_pdf, "< STEP 3-2: Find title and load in hierarchy for PDF file>");

			LoggerSingleton.show("< RESULT after step 3 (PDF file)>", sps_pdf);
			step3(sps_pdf);
			return generate(sps, sps_pdf);
			
		} else {
			LoggerSingleton.info("< STEP 2: Delete logo which appears in same position of many pages >");

			FilterableList<slidePage> sps = generateSlidePageStructure(tll, "< STEP 3: Find title and load in hierarchy >");

			LoggerSingleton.show("< RESULT after step 3 >", sps);
			step3(sps);
			return generate(sps);
		}
	}

	
	// private methods needed by generate method

	private void analyzeTitlePosition(ArrayList<slidePage> sps, boolean centered){

		ArrayList<textLine> tl2 = new ArrayList<textLine>();
		for (slidePage page: sps){
			if (page.get_PageNum() < 0) {
				sps.remove(page);
				continue;
			} else if (page.get_title().length() < 1 || page.get_titleLocation().length != 4)
				continue;
			else {
				textLine t = new textLine();
				t.set_left(page.get_titleLocation()[0]);
				t.set_top(page.get_titleLocation()[2]);
				t.set_bottom(page.get_titleLocation()[3]);
				t.set_height(page.get_titleLocation()[3] - page.get_titleLocation()[2]);
				t.set_width(page.get_titleLocation()[1] - page.get_titleLocation()[0]);

				boolean match = false;
				for (textLine line: tl2){
					if (t.isSameTitlePosition(line, pageWidth, pageHeight, centered)) {
						line.set_left(Math.min(t.get_left(), line.get_left()));
						line.set_top(Math.min(t.get_top(), line.get_top()));
						line.set_bottom(Math.max(t.get_bottom(), line.get_bottom()));
						line.set_height(line.get_height());
						if(centered)
							line.set_width(Math.max(t.get_width(), line.get_width()));
						else
							line.set_width((line.get_width() * line.get_count() + t.get_width()) / (line.get_count() + 1));
						line.set_count(line.get_count() + 1);
						match = true;
						break;
					}
				}
				if (!match) {
					t.set_count(1);
					t.set_type(centered ? TextLineType.TITLE : TextLineType.COMMON_TEXT);
					tl2.add(t);
				}
			}
		}

		int totalPage = sps.size();

		int divider = 0;
		if (totalPage < 15)
			divider = 3;
		else if (totalPage < 25)
			divider = 4;
		else if (totalPage < 40)
			divider = 5;
		else if (totalPage < 60)
			divider = 6;
		else
			divider = 7;

		
		for (textLine line: tl2)
			if (line.get_count() > 2 && line.get_count() >= totalPage / divider){
				int[] pta = { line.get_top(), line.get_bottom() - line.get_top(), line.get_type().getValue(), line.get_left() + line.get_width() / 2 };
				this.potentialTitleArea.add(pta);
				LoggerSingleton.info(String.format("[%d, %d, %s: %d | %d]", pta[0], pta[1], pta[2] == 1 ? "Center" : "Left", pta[3], line.get_count()));
			}

	}

	private void analyzeHerarchicalGap(ArrayList<slidePage> sps) {

		ArrayList<textLine> tl2 = new ArrayList<textLine>();
		int totalPage = 0;
		for (slidePage page: sps){
			if (page.get_levelCoordinates().length != 3 || page.get_levelCoordinates()[0] < 0 || page.get_levelCoordinates()[1] < 0)
				continue;
			else {
				totalPage++;
				int gap = page.get_levelCoordinates()[1] - page.get_levelCoordinates()[0];
				// Here we use only 2 attributes of textLine, slideID to mark
				// the gap, and the count.
				textLine t = new textLine();
				t.set_slideID(gap);

				boolean match = false;
				for (textLine line: tl2){
					if (line.get_slideID() >= gap - Constants.GAP_MARGIN && line.get_slideID() <= gap + Constants.GAP_MARGIN) {
						line.set_count(line.get_count() + 1);
						match = true;
						break;
					}
				}
				if (!match) {
					t.set_count(1);
					tl2.add(t);
				}
			}
		}
		int divider;
		if (totalPage < 25)
			divider = 4;
		else if (totalPage < 40)
			divider = 5;
		else if (totalPage < 60)
			divider = 6;
		else
			divider = 7;

		for(textLine line: tl2)
			if (line.get_count() > 3 && line.get_count() >= totalPage / divider){
				this.potentialHierarchicalGap.add(line.get_slideID());
				LoggerSingleton.info("Potential Gap: " + line.get_slideID() + '\t' + line.get_count());
			}

	}

	private void addInfoToEachText(ArrayList<slidePage> sps) {
		for(slidePage page: sps){
			if (page.get_pageType().isNotSpecial()) {
				for(textOutline to: page.get_texts())
					if (to.get_time().before(page.get_startTime()))
						to.set_time(page.get_startTime());
			} else if (page.get_pageType().isNotSplitSlide()) {
				for(textOutline to: page.get_texts())
					for (slidePage innerPage: sps.subList(sps.indexOf(page) + 1, sps.size()))
						if (to.get_child() == innerPage.get_PageNum()
								&& to.get_time().before(page.get_startTime()))
							to.set_time(innerPage.get_startTime());


			}
		}

	}

	private FilterableList<slidePage> generateSlidePageStructure(ArrayList<textLine> tll, String text) throws IOException{
		LoggerSingleton.info(text);
		
		FilterableList<textLine> tl2 = new FilterableList<textLine>();
		FilterableList<slidePage> sps = new FilterableList<slidePage>();
		int tempPageNum = 1;
		
		for (textLine t : tll)
			if (t.get_slideID() == tempPageNum)
				tl2.add(t);
			else {
				sps.add(new slidePage(tl2, this));
				tl2.clear();
				tempPageNum = t.get_slideID();
				tl2.add(t);
			}

		sps.add(new slidePage(tl2, this));
		tll.clear();

		return sps;
	}

	private FilterableList<slidePage> deleteAllUnorganizedTexts(FilterableList<slidePage> sps) {
		FilterableList<slidePage> new_sps = new FilterableList<>();
		for (slidePage page: sps) {
			if(page.get_texts().isEmpty()) continue;
			textOutline firstText = page.get_texts().get(0);
			if (page.get_title().length() < 1 && (page.get_pageType().isNotCommon() || firstText.get_hierarchy() != 1))
				continue;
			
			if (page.get_title().length() < 1){
				page.set_title(firstText.get_text());
				page.get_texts().remove(firstText);
			}
			
			ArrayList<textOutline> old_texts = new ArrayList<>(page.get_texts()); 
			page.get_texts().clear();
			for (textOutline to: old_texts)
				if (to.get_hierarchy() != 0)
					page.get_texts().add(to);

			new_sps.add(page);
		}
		return new_sps;
	}

	private FilterableList<slidePage> concludeVisualIndexPage(FilterableList<slidePage> sps) {
		int beginPos = 1;
		for (int i = 0; i < sps.size(); i++) {
			slidePage page = sps.get(i);
			if (page.get_pageType().isNotSpecial()) continue;
			else if (page.get_pageType() == SlidePageType.INDEX_SLIDE) {
				int currentSlideNum = page.get_PageNum();
				if (beginPos < i - 1)
					sps = concludeTheme(sps, beginPos, i - 1, 3);

				for (int j = i; j < sps.size(); j++)
					if (sps.get(j).get_PageNum() >= currentSlideNum) {
						i = j;
						break;
					}

				currentSlideNum = page.get_texts().get(page.get_texts().size() - 1).get_childEnd();
				for (int j = i; j < sps.size(); j++)
					if (sps.get(j).get_PageNum() >= currentSlideNum) {
						i = j;
						beginPos = j + 1;
						break;
					}

			} else if (page.get_pageType() == SlidePageType.TAG_SLIDE) {
				sps = concludeTheme(sps, beginPos, i - 1, 3);
				isTag = true;
				break;
			}
		}
		if (!isTag && beginPos < sps.size() - 1)
			sps = concludeTheme(sps, beginPos, sps.size() - 2, 3);

		return sps;
	}

	private FilterableList<slidePage> searchIndexPage(FilterableList<slidePage> sps) {
		if (!isTag)
			return findIndexPage(sps, 0, sps.size() - 1);
		else {
			for (slidePage page: sps){
				int i = sps.indexOf(page);
				if (page.get_pageType() == SlidePageType.TAG_SLIDE) {
					sps = findIndexPage(sps, 0, i - 1);
					for(textOutline currentTo: page.get_texts()){
						int beginPos = i, endPos = i;
						for (int k = i + 1; k < sps.size(); k++)
							if (sps.get(k).get_PageNum() >= currentTo.get_child()) {
								beginPos = k;
								break;
							}
						for (int k = beginPos; k < sps.size(); k++)
							if (sps.get(k).get_PageNum() >= currentTo.get_childEnd()) {
								endPos = k;
								break;
							}
						sps = findIndexPage(sps, beginPos, endPos);
					}
				}
			}
		return sps;
		}
	}

	private int setIsTagAndGetTagPos(ArrayList<slidePage> sps) {
		isTag = false;
		for (slidePage page: sps){
			isTag = page.get_pageType() == SlidePageType.TAG_SLIDE;
			if (isTag)
				return sps.indexOf(page);
		}
		return 0;
	}

	private void step3(ArrayList<slidePage> sps) {
		analyzeTitlePosition(sps, false);
		analyzeTitlePosition(sps, true);
		analyzeHerarchicalGap(sps);
	}
	
	private FilterableList<textOutline> generate(FilterableList<slidePage> sps) {
		ArrayList<ArrayList<Integer>> samePageGroups = findSamePages(sps);
		LoggerSingleton.show("< STEP 4: Remove repeated and empty pages >", samePageGroups);


		sps = removeRepeatedPages(sps, samePageGroups).notNullObjects();
		samePageGroups = findSamePages(sps);
		LoggerSingleton.show("< STEP 5: Remove live show >", samePageGroups);
		sps = removeLiveShow(sps, samePageGroups).notNullObjects();

		LoggerSingleton.info("< STEP 6: Combine continuous slides >");
		sps = combineContinuedSlides(sps, 0, sps.size() - 1).notNullObjects();
		LoggerSingleton.show("< RESULT after step 6 >", sps);

		samePageGroups = findSamePages(sps);
		LoggerSingleton.show("< STEP 7: Find tag page >", samePageGroups);

		sps = dealWithTagPage(sps, samePageGroups);

		LoggerSingleton.info("< STEP 8: Find split page and make them as visual tag page>");
		sps = dealWithSplitPage(sps);

		LoggerSingleton.info("< STEP 9: Find section pages and make a visual tag page for them>");
		sps = dealWithSectionPage(sps);

		sps = deleteAllUnorganizedTexts(sps);

		int tagPos = setIsTagAndGetTagPos(sps);

		sps = combineContinuedSlides(sps, 0, (isTag ? tagPos : sps.size())).notNullObjects();

		LoggerSingleton.info("< STEP 10: Search index page >");
		sps = searchIndexPage(sps);

		LoggerSingleton.info("< STEP 11: Conclude a visual index page for pages with similar titles >");
		sps = concludeVisualIndexPage(sps);

		addInfoToEachText(sps);

		LoggerSingleton.show("< RESULT after step 10 >", sps);

		this.initial = false;
		return makeFinalTextOutlinesFromSlidePages(sps);
	}
	
	private FilterableList<textOutline> generate(FilterableList<slidePage> sps, FilterableList<slidePage> other) throws Exception{
		sps = synchronizeVideoToFile(sps, other);
		LoggerSingleton.show("< RESULT after step 3.5: synchronization >", sps);
		writeSyncFile(sps);
		
		return generate(sps);
	}
	
	private void writeSyncFile(ArrayList<slidePage> sps) throws IOException {
		File newFile = new File(StaticMethods.joinPath(this.get_workingDir(), "sync"));
		if (newFile.exists()) newFile.delete();

		BufferedWriter output = new BufferedWriter(new FileWriter(newFile));
		for (slidePage page: sps) {
			output.append(page.get_PageNum() + "\t" + page.get_startTime().toString());
			output.newLine();
		}
		output.close();
		
	}

	private FilterableList<textLine> removeLogo(FilterableList<textLine> inputLines) throws Exception {
		if(inputLines.isEmpty()) return inputLines;
		int totalPage = inputLines.get(inputLines.size() - 1).get_slideID();
		final ArrayList<textLine> tl2 = new ArrayList<textLine>();
		for (textLine line: inputLines) {
			boolean match = false;
			for (textLine other: tl2)
				if (line.isSame(other, pageWidth, pageHeight)) {
					other.set_count(other.get_count() + 1);
					match = true;
					break;
				}
			if (!match) {
				line.set_count(1);
				tl2.add(line);
			}
		}

		int divider = 0;
		if (totalPage < 15)
			divider = 2;
		else if (totalPage < 25)
			divider = 3;
		else if (totalPage < 40)
			divider = 4;
		else if (totalPage < 60)
			divider = 5;
		else
			divider = 6;

		ArrayList<textLine> old_tl2 = new ArrayList<>(tl2);
		tl2.clear();
		for (textLine line : old_tl2) {
			if (line.get_count() <= 2 || line.get_count() < totalPage / divider) continue;
			if (!this.initial) {
				boolean foundSameTitlePosition = false;
				for (int[] pta: potentialTitleArea) {
					if (pta.length != 4) continue;
					boolean centered = pta[2] != 0;

					textLine t = new textLine();
					
					t.set_top(pta[0]);
					t.set_height(pta[1]);
					t.set_bottom(pta[0] + pta[1]);
					t.set_type(TextLineType.fromInt(pta[2]));
					t.set_left(centered ? pta[3] / 2 : pta[3]);
					t.set_width(centered ? pta[3] : pageWidth / 3);
					
					if (line.isSameTitlePosition(t, pageWidth, pageHeight, centered)){
						foundSameTitlePosition = true;
						break;
					}
				}
				if(foundSameTitlePosition) 
					tl2.add(line);
			} else {
				double hp = pageHeight / Constants.DEFAULT_HEIGHT;
				if (line.get_bottom() < 256 * hp) {
					int rightPart = line.get_left() + line.get_width() - pageWidth / 2;
					int leftPart = pageWidth / 2 - line.get_left();
					if (rightPart > 0 && leftPart > 0 && Math.abs(leftPart - rightPart) < line.get_width() / 4)
						continue;
				}
				tl2.add(line);
			}	
		}

		LoggerSingleton.info("Filtered text lines: ");
		for (textLine t: tl2)
			LoggerSingleton.info(  
					Joiner.on(" ").join(new Object[] { 
							t.get_count(), t.get_text(), t.get_top(),
							t.get_left(), t.get_width(), t.get_height()}));
		
		return inputLines.filter(new FilterFunc<Boolean, textLine>() {
			@Override
			public Boolean call() { return true; }

			@Override
			public Boolean call(textLine inputLine) {
				for (textLine filteredLine: tl2)
					if (filteredLine.isSamePosition(inputLine, pageWidth, pageHeight, initial))
						return false;
				return true;
			}
		});
	}

	// old methods bellow
	/**
	 * In this function, totally empty slides will be deleted and similar
	 * slides will be found out and saved in group, to be further dealt with.
	 * Finally bubble-reorder each of the similar slide groups.
	 * @throws Exception 
	 */
	private ArrayList<ArrayList<Integer>> findSamePages(FilterableList<slidePage> sps) {

		ArrayList<ArrayList<Integer>> samePageGroups = new ArrayList<ArrayList<Integer>>();
		
		sps = sps.filter(new FilterFunc<Boolean, slidePage>() {
			@Override
			public Boolean call(slidePage page) { return page.get_pageType().isNotEmpty(); }

			@Override
			public Boolean call() { return true; }
		});
		
		for (slidePage page: sps){
			/**
			 * Search for similar slides, when found.... If both slides already have
			 * been in a group, skip it. If only 1 slides exists in a
			 * group, add the 2nd slide in. If both slides haven't in a group,
			 * create a new group for them.
			 */
			for(slidePage page2: sps.subList(sps.indexOf(page) + 1, sps.size())){
				if (page2.get_pageType().isEmpty() || !page.isSamePage(page2)) continue;

				boolean done = false;
				for(ArrayList<Integer> pageGroup: samePageGroups){
					for (int firstPageNum: pageGroup)
						if (page.get_PageNum() == firstPageNum) {
							for (int secondPageNum: pageGroup)
								if (page2.get_PageNum() == secondPageNum) {
									done = true;
									break;
								}
							if (!done) {
								pageGroup.add(page2.get_PageNum());
								done = true;
								break;
							}
						}
					if (done)
						break;
				}
				if (!done)
					samePageGroups.add(Lists.newArrayList(page.get_PageNum(), page2.get_PageNum()));
			}
		}
		
		for(ArrayList<Integer> pageGroup: samePageGroups)
			Collections.sort(pageGroup, new Comparator<Integer>() {
				@Override
				public int compare(Integer first, Integer second) {
					return first.compareTo(second);
				}
			});
		return samePageGroups;

	}

	
	private FilterableList<slidePage> removeRepeatedPages(FilterableList<slidePage> sps,
			ArrayList<ArrayList<Integer>> samePageGroups){
		/*
		 * Remove those 'logically' repeated slides from the slides series such
		 * as : ABAB( to AB ) , ABCBABC( to ABC ) AABBB( to AB )
		 */

		LoggerSingleton.info("Pages removed:");
		String info = ""; 
		for(slidePage page: sps.subList(0, sps.size() - 1)){
			if (page == null) continue;
			slidePage nextPage = sps.get(sps.indexOf(page) + 1);
			if (isInSamePageGroup(page.get_PageNum(), nextPage.get_PageNum(), samePageGroups)) {
				info += page.get_PageNum() + ", ";
				Time time = page.get_startTime();
				sps.set(sps.indexOf(page), combineSameSlides(page, nextPage, true));
				sps.set(sps.indexOf(nextPage), null);
				page.set_startTime(time);
				continue;
			}

			slidePage previousPage = sps.indexOf(page) > 1 ? sps.get(sps.indexOf(page) - 1) : null;
			if (previousPage != null && isInSamePageGroup(previousPage.get_PageNum(), nextPage.get_PageNum(), samePageGroups)) {
				info += nextPage.get_PageNum() + ", ";
				Time time = previousPage.get_startTime();
				sps.set(sps.indexOf(previousPage), combineSameSlides(previousPage, nextPage, true));
				sps.set(sps.indexOf(nextPage), null);
				previousPage.set_startTime(time);

				for (slidePage futurePage: sps.subList(sps.indexOf(page) + 2, sps.size()))
					if (isInSamePageGroup(page.get_PageNum(), futurePage.get_PageNum(), samePageGroups)) {
						info += page.get_PageNum() + ", ";
						time = futurePage.get_startTime();
						sps.set(sps.indexOf(futurePage), combineSameSlides(page, futurePage, false));
						sps.set(sps.indexOf(page), null);
						futurePage.set_startTime(time);
						break;
					}
				continue;
			}
		}
		LoggerSingleton.info(info);

		return sps;
	}
	

	/**
	 * In this function, if a group of continuous pages existing between two
	 * same slides are mainly ill-organized pages or empty pages, they would
	 * be treated as LiveShow and deleted from the data structure.
	 *
	 * Re-order the same page pairs first, treat the shorter pairs first.
	 * @throws Exception 
	 */
	private FilterableList<slidePage> removeLiveShow(FilterableList<slidePage> sps, ArrayList<ArrayList<Integer>> samePageGroups){

		ArrayList<int[]> pairs = new ArrayList<int[]>();
		for (ArrayList<Integer> pageGroup: samePageGroups ){
			for(Integer curValue: pageGroup.subList(0, pageGroup.size() - 1)){
				Integer nextValue = pageGroup.get(pageGroup.indexOf(curValue) + 1);
				int pair[] = { curValue, nextValue, 0 };

				int beginPagePos = -1;
				int endPagePos = -1;
				for(slidePage page: sps){
					if (page.get_PageNum() == pair[0])
						beginPagePos = sps.indexOf(page);
					else if (page.get_PageNum() == pair[1]) {
						endPagePos = sps.indexOf(page);
						break;
					}
				}
				pair[2] = endPagePos - beginPagePos;
				pairs.add(pair);
			}
		}
		
		Collections.sort(pairs, new Comparator<int[]>() {
			@Override
			public int compare(int[] first, int[] second) {
				return new Integer(first[2]).compareTo(second[2]);
			}
		});
		

		for (int[] pair: pairs)
			LoggerSingleton.info("Pair: <" + pair[0] + ", " + pair[1] + "> " + pair[2]);

		for (int[] pair: pairs){
			int beginPageNum = pair[0];
			int endPageNum = pair[1];
			int beginPagePos = -1;
			int endPagePos = -1;
			for (slidePage page: sps) {
				if (page.get_PageNum() == beginPageNum)
					beginPagePos = sps.indexOf(page);
					break;
				}
			
			if (beginPagePos < 0)
				continue;

			double ave = 0;
			int flashPageCount = 0;
			for (slidePage page: sps.subList(beginPagePos + 1, sps.size())){
				if (page == null) continue;
				int pageIdx = sps.indexOf(page);
				slidePage previousPage = sps.get(pageIdx - 1);
				if (page.get_PageNum() == endPageNum) {
					if (page.get_startTime().getTime() - previousPage.get_startTime().getTime() < 5001)
						flashPageCount++;
					endPagePos = pageIdx;
					break;
				}

				if (page.get_pageType() == SlidePageType.MOST_TEXT_UNORGANIZED) {
					int totalLength = 0;
					for(textOutline outline: page.get_texts())
						totalLength += outline.get_text().length();
					if (page.get_texts().size() >= 6 || totalLength >= 100) {
						ave += 0.5;
						if (totalLength >= 200)
							ave += 0.5;
					} else
						ave += 1;
				} else
					ave -= page.get_pageType().getValue();
				
				if (pageIdx + 1 >= sps.size()) continue;
				slidePage nextPage = sps.get(pageIdx + 1);
				if (nextPage.get_startTime().getTime() - page.get_startTime().getTime() < 5001)
					flashPageCount++;
			}
			if (endPagePos < 0)
				continue;

			slidePage beginPage = sps.get(beginPagePos), endPage = sps.get(endPagePos);
			int posDiff = endPagePos - beginPagePos; 
			if (posDiff == 1) {
				Time time = beginPage.get_startTime();
				sps.set(beginPagePos, combineSameSlides(beginPage, endPage, true));
				sps.get(beginPagePos).set_startTime(time);
				sps.set(endPagePos, null);
			} else {
				ave = ave / (double) (posDiff - 1);
				long avt = (endPage.get_startTime().getTime() - beginPage.get_startTime().getTime()) / posDiff;
				boolean delete = ave >= 0.5 && (avt < 15001 || flashPageCount * 2 >= (posDiff - 1))
						&& beginPage.get_pageType() == endPage.get_pageType();
				
				LoggerSingleton.info("Potential Live Show: " + "( " + beginPageNum + ", " + endPageNum + " ) AveType-"
						+ ave + " AveDuration-" + avt / 1000 + " FlashPageCount" + flashPageCount + " "
						+ (delete ? "Yes" : "No"));
				if (delete) {
					Time time = beginPage.get_startTime();
					sps.set(beginPagePos, combineSameSlides(beginPage, endPage, true));
					sps.get(beginPagePos).set_startTime(time);
					for (int k = endPagePos; k > beginPagePos; k--)
						sps.set(k, null);
				}
			}
		}

		return sps;
	}
	

	/**
	 * In this function, several continuous slides with a same topic will be
	 * gathered together as a single, large slide. And the title of the new
	 * slide will use the best recognized one from all the old slides
	 * included, and delete the potential number such as (1) or (2/3).
	 * @throws Exception 
	 */
	public FilterableList<slidePage> combineContinuedSlides(FilterableList<slidePage> sps, int start, int end){

		for (slidePage page: sps.subList(start, end)){
			if(page == null) continue;
			int pageIdx = sps.indexOf(page);
			if(pageIdx >= sps.size() - 1) continue;
			slidePage nextPage = sps.get(pageIdx + 1);
//		for (int i = beginPos; i < endPos; i++) {
			boolean continuePage = false;

			// Same title, no doubt will be combined.
			if (page.get_title().contentEquals(nextPage.get_title()))
				continuePage = true;
			else {
				// And for similar title, do further tests.
				String titleA = page.get_title();
				String titleB = nextPage.get_title();

				algorithmInterface ai = new algorithmInterface();
				int le = ai.getLevenshteinDistance(titleA, titleB);
				int longer = titleA.length() > titleB.length() ? titleA.length() : titleB.length();
				double lr = (double) le / (double) longer;
				double wr = ai.getSameWordsRatio(titleA, titleB);

				if (lr > 0.5 && wr < 0.5)
					continue;

				// If the difference of two whole titles is minus enough, done.
				if (lr <= 0.15 || le <= 2)
					continuePage = true;

				// Judging whether the last word in a title is a order number
				// like (2/3) or <1>
				// If so, remove this number and compare the titles again, with
				// a less strict standard.
				boolean withOrderA = false, withOrderB = false;
				String[] wordsA = titleA.split(" ");
				if (ai.isOrderNum(wordsA[wordsA.length - 1])) {
					titleA = wordsA[0];
					for (int j = 1; j < wordsA.length - 1; j++)
						titleA = titleA + " " + wordsA[j];
					withOrderA = true;
				} else if (ai.isOrderNum(wordsA[0]) && wordsA.length > 1) {
					titleA = wordsA[1];
					for (int j = 2; j < wordsA.length; j++)
						titleA = titleA + " " + wordsA[j];
					withOrderA = true;
				} else {
					if (wordsA.length > 2
							&& wordsA[wordsA.length - 1].length() + wordsA[wordsA.length - 2].length() < 7) {
						String temp = wordsA[wordsA.length - 2] + wordsA[wordsA.length - 1];
						if (ai.isOrderNum(temp)) {
							titleA = wordsA[0];
							for (int j = 1; j < wordsA.length - 2; j++)
								titleA = titleA + " " + wordsA[j];
							withOrderA = true;
						}
					}
				}

				String[] wordsB = titleB.split(" ");
				if (ai.isOrderNum(wordsB[wordsB.length - 1])) {
					titleB = wordsB[0];
					for (int j = 1; j < wordsB.length - 1; j++)
						titleB = titleB + " " + wordsB[j];
					withOrderB = true;
				} else if (ai.isOrderNum(wordsB[0]) && wordsB.length > 1) {
					titleB = wordsB[1];
					for (int j = 2; j < wordsB.length; j++)
						titleB = titleB + " " + wordsB[j];
					withOrderB = true;
				} else {
					if (wordsB.length > 2
							&& wordsB[wordsB.length - 1].length() + wordsB[wordsB.length - 2].length() < 7) {
						String temp = wordsB[wordsB.length - 2] + wordsB[wordsB.length - 1];
						if (ai.isOrderNum(temp)) {
							titleB = wordsB[0];
							for (int j = 1; j < wordsB.length - 2; j++)
								titleB = titleB + " " + wordsB[j];
							withOrderB = true;
						}
					}
				}

				le = ai.getLevenshteinDistance(titleA, titleB);
				longer = titleA.length() > titleB.length() ? titleA.length() : titleB.length();
				lr = (double) le / (double) longer;
				wr = ai.getSameWordsRatio(titleA, titleB);
				if (lr <= 0.15 || le <= 2)
					continuePage = true;
				else if (((wr == 1 && lr <= 0.75) || ((wr > 0.6 && lr <= 0.25))) && (withOrderA || withOrderB))
					continuePage = true;

				// Change title to most recognized one and delete order number.
				if (continuePage && (withOrderA || withOrderB)) {
					String finalTitle = titleA.length() > titleB.length() ? titleA : titleB;
					page.set_title(finalTitle);
				}
			}

			if (continuePage) {
				LoggerSingleton.info("Combine page: ( " + page.get_PageNum() + " <- " + nextPage.get_PageNum() + " )");
				page.combineAtEnd(nextPage);
				sps.set(sps.indexOf(nextPage), null);
			}

		}

		return sps;
	}


	private boolean isInSamePageGroup(int a, int b, ArrayList<ArrayList<Integer>> samePageGroups) {
		int match = -1, order = -1;
		for(ArrayList<Integer> group: samePageGroups)
			for(Integer val: group)
				if (a == val.intValue()) {
					match = samePageGroups.indexOf(group);
					order = group.indexOf(val);
				}

		if (match >= 0) {
			ArrayList<Integer> group = samePageGroups.get(match);
			for(Integer val: group.subList(order + 1 , group.size()))
				if (b == val.intValue())
					return true;
		}

		return false;
	}
	

	/**
	 * In this function, one of the two similar slides will be deleted, but
	 * all the content will be kept in the remaining one. The
	 * better-organized or better-recognized one will take a more important
	 * role in the reconstruction.
	 */
	private slidePage combineSameSlides(slidePage a, slidePage b, boolean keepA) {

		if (a.get_pageType().lt(SlidePageType.MOST_TEXT_UNORGANIZED)) {
			if (keepA)
				b.set_PageNum(a.get_PageNum());
			return b;
		} else if (b.get_pageType().lt(SlidePageType.MOST_TEXT_UNORGANIZED)) {
			if (!keepA)
				a.set_PageNum(b.get_PageNum());
			return a;
		}

		slidePage target = new slidePage();
		slidePage reference = new slidePage();
		boolean useA = true;

		/*
		 * one target, one reference, and the choice will follow rules below, in
		 * priority: 1. By pageType. (one well-organized with over 50% texts in
		 * the hierarchy, one not) 2. By title existence and the title initial
		 * is Block or not. 3. By ratio of well-organized texts. 4. By total
		 * length.
		 */
		if (a.get_pageType() != b.get_pageType()) {
			if (a.get_pageType().gt(b.get_pageType()))
				useA = true;
			else
				useA = false;
		} else if (!a.get_title().contentEquals(b.get_title())) {
			if (a.get_title().length() > 0 && b.get_title().length() > 0) {
				boolean taBlock = false;
				boolean tbBlock = false;
				if (a.get_title().charAt(0) < 97 || a.get_title().charAt(0) > 123)
					taBlock = true;
				else
					taBlock = false;
				if (b.get_title().charAt(0) < 97 || b.get_title().charAt(0) > 123)
					tbBlock = true;
				else
					tbBlock = false;

				if (taBlock ^ tbBlock) {
					if (taBlock)
						useA = true;
					else
						useA = false;
				} else {
					if (a.get_title().length() >= b.get_title().length())
						useA = true;
					else
						useA = false;
				}
			} else {
				if (a.get_title().length() == 0)
					useA = false;
				else
					useA = true;
			}
		} else {
			int count = 0;
			for (int i = 0; i < a.get_texts().size(); i++) {
				textOutline t = a.get_texts().get(i);
				if (t.get_hierarchy() > 0)
					count++;
			}
			double ra = (double) count / (double) a.get_texts().size();

			count = 0;
			for (int i = 0; i < b.get_texts().size(); i++) {
				textOutline t = b.get_texts().get(i);
				if (t.get_hierarchy() > 0)
					count++;
			}
			double rb = (double) count / (double) b.get_texts().size();

			if (ra > rb)
				useA = true;
			else if (rb > ra)
				useA = false;
			else {
				String allTexts1 = "";
				for (int i = 0; i < a.get_texts().size(); i++)
					allTexts1 = allTexts1 + " " + a.get_texts().get(i).get_text();

				String allTexts2 = "";
				for (int i = 0; i < b.get_texts().size(); i++)
					allTexts2 = allTexts2 + " " + b.get_texts().get(i).get_text();

				if (allTexts1.length() >= allTexts2.length())
					useA = true;
				else
					useA = false;
			}
		}

		if (useA) {
			target.contentCopy(a);
			reference.contentCopy(b);
		} else {
			target.contentCopy(b);
			reference.contentCopy(a);
		}

		// Add potential text from the Reference to the Target strict in order
		int targetMatch = -1;
		for (int i = 0; i < reference.get_texts().size(); i++) {
			// Find whether there is a textOutline matched from the Reference to
			// the Target.
			int match = -1;
			textOutline ref = reference.get_texts().get(i);
			for (int j = targetMatch + 1; j < target.get_texts().size(); j++) {
				textOutline tar = target.get_texts().get(j);
				algorithmInterface ai = new algorithmInterface();
				int dis = ai.getLevenshteinDistance(ref.get_text(), tar.get_text());
				int longer = ref.get_text().length() > tar.get_text().length() ? ref.get_text().length()
						: tar.get_text().length();
				double wr = ai.getSameWordsRatio(ref.get_text(), tar.get_text());
				if (longer >= dis * 2 || wr >= 0.9) {
					match = j;
					break;
				}
			}

			// If match, use the longer one as final result, else plug in the
			// Reference text at the end of the last match point.
			if (match >= 0) {
				targetMatch = match;
				if (ref.get_text().length() > target.get_texts().get(match).get_text().length())
					target.get_texts().get(match).set_text(ref.get_text());
			} else {
				ArrayList<textOutline> temp = new ArrayList<textOutline>();
				for (int j = target.get_texts().size() - 1; j > targetMatch; j--) {
					temp.add(target.get_texts().get(j));
					target.get_texts().remove(j);
				}

				target.get_texts().add(ref);
				for (int j = temp.size() - 1; j >= 0; j--)
					target.get_texts().add(temp.get(j));

				targetMatch++;

			}
		}

		if (keepA)
			target.set_PageNum(a.get_PageNum());
		else
			target.set_PageNum(b.get_PageNum());

		target.isSlideWellOrganized();

		return target;
	}

	/**
	 * In this function, Index Page (having a lot of texts as titles of
	 * following page) will be single out. Connection between the text and
	 * the title will be created by the parameter "child" and "childEnd" of
	 * the textOutline. And unmatched text in the index page will be
	 * removed.
	 * @throws Exception 
	 */
	private FilterableList<slidePage> findIndexPage(FilterableList<slidePage> sps, int beginPosition, int endPosition) {
		if (endPosition <= beginPosition)
			return sps;
		LoggerSingleton.info("Searching Index Page in << " + sps.get(beginPosition).get_PageNum() + " "
				+ sps.get(endPosition).get_PageNum() + " >>");
		ArrayList<Integer> IndexPos = new ArrayList<Integer>();
		int nextIdx = beginPosition;
		for(slidePage page: sps.subList(beginPosition, endPosition + 1)){
			int pageIdx = sps.indexOf(page), matchCount = 0, matchPoint = pageIdx + 1;
			
			if (pageIdx < nextIdx || page.get_pageType().isNotCommon() || page.get_texts().size() < 3) continue;
			/*
			 * Only well-organized page can be signed as index page. First, go
			 * over through all the text in a page, try to find a matched title
			 * from a following page. Then count the 'matched' text ratio inside
			 * a page, if the ratio is over 50%, a Index Page appears.
			 */

			// Using 'matchPoint' to control the order, a later text should not
			// find a matched title before a previous text.
			for (textOutline outline: page.get_texts()){
				if (outline.get_hierarchy() == 0) continue;
				String currentText = outline.get_text();
				for (slidePage otherPage: sps.subList(matchPoint, endPosition + 1)){
					String currentTitle = otherPage.get_title();
					algorithmInterface ai = new algorithmInterface();
					int le = ai.getLevenshteinDistance(currentText, currentTitle);
					int longer = currentText.length() > currentTitle.length() ? currentText.length()
							: currentTitle.length();
					double lr = (double) le / (double) longer;
					double wr = ai.getSameWordsRatio(currentText, currentTitle);
					if (lr <= 0.2 || (lr < 0.34 && wr >= 0.75)
							|| currentText.toLowerCase().contains(currentTitle.toLowerCase())
							|| currentTitle.toLowerCase().contains(currentText.toLowerCase())) {
						outline.set_child(otherPage.get_PageNum());
						matchCount++;
						matchPoint = sps.indexOf(otherPage) + 1;
						break;
					}
				}
			}
			// for those page only contains a few matched text, connection will
			// be retained, but never used.
			if (matchCount * 2 >= page.get_texts().size()) {
				page.set_pageType(SlidePageType.INDEX_SLIDE);
				IndexPos.add(sps.indexOf(page));
				nextIdx = matchPoint - 1;
			}

		}

		/**
		 * In this part, for a index page, all the texts will find a matched
		 * title, empty ones will be removed. And from the first page after
		 * the index page, to the last page got matched, all them will be
		 * included inside the one [child, childEnd] area of a text in the
		 * index page.
		 *
		 * Mainly, a text will be treated differently by whether it is a
		 * matched one, and then differed again by whether it is the last
		 * text, if not, differed again by whether the next text is matched.
		 * So, there will be 6 different treatment.
		 *
		 * 'currentPos' is used to control the moving oder.
		 */
		for(Integer pos: IndexPos){
			int posIdx = IndexPos.indexOf(pos), currentPos = pos + 1;
			boolean isLast = posIdx == IndexPos.size() - 1;
			Integer nextPos = !isLast ? IndexPos.get(posIdx + 1) : null;
			slidePage page = sps.get(pos), currentPage = sps.get(currentPos);
			FilterableList<textOutline> outlines = new FilterableList<textOutline>(page.get_texts());
			
			for (textOutline outline: outlines) {
				int outlineIdx = outlines.indexOf(outline);
				boolean isLastOutline = outlineIdx == outlines.size() - 1;
				textOutline nextOutline = isLastOutline ? null : outlines.get(outlineIdx + 1);
				boolean matched = outline.get_child() >= 0, nextMatched = isLastOutline ? false : nextOutline.get_child() >= 0;
				
				if (!matched && isLastOutline) { 
					/**
					 * Condition 1: not matched and as the last text. Only when
					 * this text partly matched the next page's title in the
					 * matching oder, and that page located before a next index
					 * page and the last page of the presentation, it will match
					 * the slide, or else, it will be removed.
					 */

					if (currentPos > (isLast ? endPosition : nextPos - 1))
						outlines.set(outlineIdx, null);
					else {
						String currentText = outline.get_text();
						String currentTitle = currentPage.get_title();
						algorithmInterface ai = new algorithmInterface();
						int le = ai.getLevenshteinDistance(currentText, currentTitle);
						int longer = currentText.length() > currentTitle.length() ? currentText.length()
								: currentTitle.length();
						double lr = (double) le / (double) longer;
						double wr = ai.getSameWordsRatio(currentText, currentTitle);
						if (lr <= 0.5 || wr >= 0.5) {
							outline.set_child(currentPage.get_PageNum());
							outline.set_childEnd(currentPage.get_PageNum());
							currentPos++;
						} else
							outlines.set(outlineIdx, null);
					}
				} else if (!matched && !isLastOutline && !nextMatched) { 
					/**
					 * Condition 2: Not matched, Not last text, and the next
					 * text is not matched either
					 *
					 * In this case, first moving forward until find a matching
					 * text.
					 *
					 * If found, and there are some 'free' page between
					 * currentPos and next matched page, try to find whether
					 * there is a page partly match the current text. If so,
					 * assign it, or else, delete it.
					 *
					 * If not found, and currentPos is still before a next
					 * indexPage and the end of the presentation, try to partly
					 * match the page at 'currentPos', matched, assign it, or
					 * else delete it.
					 */

					boolean stillHaveMatch = false;
					int endPos = currentPos;
					for (int k = outlineIdx + 2; k < outlines.size(); k++)
						if (outlines.get(k).get_child() > 0) {
							stillHaveMatch = true;
							for (int l = currentPos; l <= endPosition; l++)
								if (sps.get(l).get_PageNum() == outlines.get(k).get_child()) {
									endPos = l;
									break;
								}
							break;
						}

					if (stillHaveMatch) {
						if (currentPos >= endPos)
							outlines.set(outlineIdx, null);
						else {
							String currentText = outline.get_text();
							String currentTitle = sps.get(currentPos).get_title();
							algorithmInterface ai = new algorithmInterface();
							int le = ai.getLevenshteinDistance(currentText, currentTitle);
							int longer = currentText.length() > currentTitle.length() ? currentText.length()
									: currentTitle.length();
							double lr = (double) le / (double) longer;
							double wr = ai.getSameWordsRatio(currentText, currentTitle);
							if (lr <= 0.5 || wr >= 0.5) {
								outline.set_child(sps.get(currentPos).get_PageNum());
								outline.set_childEnd(sps.get(currentPos).get_PageNum());
								currentPos++;
							} else
								outlines.set(outlineIdx, null);
						}
					} else {
						if (currentPos > (isLast ? endPosition : nextPos - 1))
							outlines.set(outlineIdx, null);
						else {
							String currentText = outline.get_text();
							String currentTitle = sps.get(currentPos).get_title();
							algorithmInterface ai = new algorithmInterface();
							int le = ai.getLevenshteinDistance(currentText, currentTitle);
							int longer = currentText.length() > currentTitle.length() ? currentText.length()
									: currentTitle.length();
							double lr = (double) le / (double) longer;
							double wr = ai.getSameWordsRatio(currentText, currentTitle);
							if (lr <= 0.5 || wr >= 0.5) {
								outline.set_child(sps.get(currentPos).get_PageNum());
								outline.set_childEnd(sps.get(currentPos).get_PageNum());
								currentPos++;
							} else
								outlines.set(outlineIdx, null);
						}
					}
				} else if (!matched && !isLastOutline && nextMatched) { 
					/**
					 * Condition 3: Not matched, not last text, but next text is
					 * matched. In this case, assign all the pages between
					 * currentPos and the next matched page under current text.
					 */
					outline.set_child(currentPage.get_PageNum());
					int last = outline.get_child();
					for (slidePage possiblePage : sps.subList(currentPos, isLast ? endPosition + 1 : nextPos)) {
						int pageNum = possiblePage.get_PageNum(), nextChild = nextOutline.get_child();
						if (pageNum >= nextChild) {
							currentPos = sps.indexOf(possiblePage);
							break;
						} else
							last = possiblePage.get_PageNum();
					}
					outline.set_childEnd(last);
				} else if (matched) { // matched
					/*
					 * Condition 4: matched and last text -> just keep it.
					 * Condition 5: matched, not last text, and next text
					 * unmatched -> keep it also.
					 */
					int last = outline.get_child();

					if (currentPage.get_PageNum() < outline.get_child())
						outline.set_child(currentPage.get_PageNum());

					for (slidePage possiblePage : sps.subList(currentPos, isLast ? endPosition + 1 : nextPos)) {
						int pageNum = possiblePage.get_PageNum(), nextChild = nextOutline.get_child(),
								childEnd = outline.get_childEnd();
						if (isLastOutline || nextChild < 0 && pageNum >= childEnd) {
							currentPos = sps.indexOf(possiblePage) + pageNum == childEnd ? 1 : 0;
							break;
						} else if (!isLastOutline && nextChild >= 0 && pageNum >= nextChild) {
							currentPos = sps.indexOf(possiblePage);
							break;
						} else if (!isLastOutline && nextChild >= 0 && pageNum < nextChild)
							last = page.get_PageNum();
					}

					outline.set_childEnd(last);
				}
			}
			page.set_texts(outlines.notNullObjects());
		}

		for (int i = beginPosition; i <= endPosition; i++) {
			if (sps.get(i).get_pageType() == SlidePageType.INDEX_SLIDE) {
				String info = "Index Page: " + sps.get(i).get_PageNum() + " < ";
				for (int j = 0; j < sps.get(i).get_texts().size(); j++) {
					textOutline to = sps.get(i).get_texts().get(j);
					if (to.get_child() > 0)
						info += to.get_child() + " ";
				}
				LoggerSingleton.info(info + ">");

			}
		}

		return sps;
	}

	private FilterableList<slidePage> dealWithTagPage(FilterableList<slidePage> sps,
			ArrayList<ArrayList<Integer>> samePageGroups) {
		/*
		 * TagPage is those appears many time indicating starting a new topic It
		 * will be recognized by special title, Agenda, Outline or something
		 * like them. Other same pages will be combined directly, and tagPage
		 * will be combined either. All the pages between 2 tagPages will be
		 * assigned to a single text in the tag page.
		 */

		String[] tags = { "Agenda", "Topics", "Topic", "Outline" };

		boolean haveTags = false;
		boolean havePartlyTags = false;
		for (int i = 0; i < tags.length && !haveTags; i++) {
			ArrayList<Integer> partlyTagsNum = new ArrayList<Integer>();
			for (int j = 0; j < sps.size(); j++) {
				if (sps.get(j).get_title().equalsIgnoreCase(tags[i])) {
					if (partlyTagsNum.size() > 0) {
						// LoggerSingleton.info("^^^^^^" +
						// sps.get(j).get_title() + " " +
						// sps.get(j).get_PageNum());
						if (!isInSamePageGroup(partlyTagsNum.get(0), sps.get(j).get_PageNum(), samePageGroups))
							partlyTagsNum.add(sps.get(j).get_PageNum());
						else {
							haveTags = true;
							break;
						}
					} else
						partlyTagsNum.add(sps.get(j).get_PageNum());
				}
			}
			if (partlyTagsNum.size() > 1) {
				samePageGroups.add(partlyTagsNum);
				haveTags = true;
				havePartlyTags = true;
				break;
			}
		}

		for (int i = 0; i < samePageGroups.size() && haveTags; i++) {
			int firstPos = -1;
			boolean isTag = false;
			ArrayList<Integer> tagPos = new ArrayList<Integer>();
			for (int j = 0; j < sps.size(); j++) {
				if (sps.get(j).get_PageNum() == samePageGroups.get(i).get(0)) {
					firstPos = j;
					break;
				}
			}

			for (int j = 0; j < tags.length; j++) {
				if (sps.get(firstPos).get_title().equalsIgnoreCase(tags[j])) {
					isTag = true;
					tagPos.add(firstPos);
					break;
				}
			}

			for (int j = firstPos + 1; j < sps.size(); j++) {
				if (isInSamePageGroup(sps.get(firstPos).get_PageNum(), sps.get(j).get_PageNum(), samePageGroups)) {
					Time time = sps.get(firstPos).get_startTime();
					if (havePartlyTags) {
						sps.get(firstPos).combineAtEnd(sps.get(j));
						sps.get(firstPos).set_startTime(time);
					} else {
						sps.set(firstPos, combineSameSlides(sps.get(firstPos), sps.get(j), true));
						sps.get(firstPos).set_startTime(time);
					}
					if (!isTag) {
						sps.remove(j);
						j--;
					} else
						tagPos.add(j);
				}
			}

			// Above, finish the process of judging who's tag and who's not.
			// Using ArrayList 'tagPos' to save where those tagPages locate in
			// the 'sps' serie.
			// And ArrayList 'matchTextOutlineNum' means an area is assigned to
			// which textOutline in the tag page.

			if (isTag) {
				for (int j = 0; j < tagPos.size(); j++)
					LoggerSingleton
							.info("tagPos: " + tagPos.get(j) + " tagNum: " + sps.get(tagPos.get(j)).get_PageNum());

				slidePage target = new slidePage();
				target.contentCopy(sps.get(firstPos));
				ArrayList<Integer> matchTextOutlineNum = new ArrayList<Integer>();

				boolean haveEnding = isHavingEndingPage(sps);
				boolean atLeastOneMatch = false;
				boolean enough = false;
				int lastMatchedTextOutline = -1;

				/*
				 * firstly, search all pages between each tagPages, try to find
				 * a matched page, if found, sign this area to the matched text
				 * in the tagPage, or else, just keep it empty now.
				 *
				 * if the number of areas led by tag pages equals the number of
				 * hierarchical TextOutline in the tag page, skip this matching
				 * process
				 */

				int count = 0;
				/*
				 * for(int j = 0; j < target.get_texts().size(); j++) {
				 * if(target.get_texts().get(j).get_hierarchy() > 0) count++; }
				 * if(count == tagPos.size()) { enough = true; for(int x = 0; x
				 * < tagPos.size(); x++) matchTextOutlineNum.add(-1);
				 *
				 * LoggerSingleton.info(
				 * "One subtitle for one area, perfect tag page situation!"); }
				 */
				for (int j = 0; j < tagPos.size() && !enough; j++) {
					double matchRate = 0;
					int matchPos = tagPos.get(j);
					String matchText = "";
					matchTextOutlineNum.add(j, -1);

					for (int k = tagPos.get(j) + 1; k < (j == tagPos.size() - 1 ? sps.size() - (haveEnding ? 1 : 0)
							: tagPos.get(j + 1)); k++) {
						String currentTitle = sps.get(k).get_title();
						algorithmInterface ai = new algorithmInterface();
						for (int l = (lastMatchedTextOutline >= 0 ? lastMatchedTextOutline + 1 : 0); l < target
								.get_texts().size(); l++) {
							if (target.get_texts().get(l).get_hierarchy() == 0)
								continue;
							String currentText = target.get_texts().get(l).get_text();
							int le = ai.getLevenshteinDistance(currentText, currentTitle);
							int longer = currentText.length() > currentTitle.length() ? currentText.length()
									: currentTitle.length();
							double lr = (double) le / (double) longer;
							double wr = ai.getSameWordsRatio(currentText, currentTitle);
							int wn = ai.getSameWordsNum(currentTitle, currentText);
							if (wr >= matchRate && lr <= 0.5 && wn > 1) {
								matchRate = wr;
								matchPos = k;
								matchText = currentText;
								matchTextOutlineNum.set(j, l);
								atLeastOneMatch = true;
								lastMatchedTextOutline = l;
							}
						}
					}
					LoggerSingleton
							.info(String.format("range ( %d, %d, ) : Page %d; Text: %s; with match rate %.3f",
									sps.get(tagPos.get(j)).get_PageNum(),
									sps.get((j == tagPos.size() - 1 ? sps.size() - (haveEnding ? 2 : 1)
											: tagPos.get(j + 1))).get_PageNum(),
									sps.get(matchPos).get_PageNum(), matchText, matchRate));

					/*
					 * When a matched area found, check the number of following
					 * unassigned areas, and the number of unassigned
					 * textOutlines in the tag page. If the numbers are exactly
					 * equal, stop the matching process, in order to avoid
					 * cross-match.
					 */

					if (matchTextOutlineNum.get(j) >= 0) {
						count = 0;
						for (int x = matchTextOutlineNum.get(j) + 1; x < target.get_texts().size(); x++) {
							if (target.get_texts().get(x).get_hierarchy() > 0)
								count++;
						}
						LoggerSingleton.info(count + " " + (tagPos.size() - j - 1));
						if (count == tagPos.size() - j - 1) {
							enough = true;
							for (int x = j + 1; x < tagPos.size(); x++)
								matchTextOutlineNum.add(-1);

							LoggerSingleton.info("TextOutlines left equals subtopic areas left, no need to proceed.");
						}
					}
				}

				/*
				 * If there's absolutely no matched area, we will single out the
				 * longest in-hierarchy textOutlines to be assigned with the
				 * areas. This action will simplify the following procedure.
				 */

				atLeastOneMatch = false;
				for (int x = 0; x < matchTextOutlineNum.size(); x++)
					if (matchTextOutlineNum.get(x) >= 0) {
						atLeastOneMatch = true;
						break;
					}

				if (!atLeastOneMatch) {
					count = 0;
					int countFirstLevel = 0;
					for (int j = 0; j < target.get_texts().size(); j++) {
						if (target.get_texts().get(j).get_hierarchy() > 0) {
							count++;
							if (target.get_texts().get(j).get_hierarchy() == 1)
								countFirstLevel++;
						}
					}

					if (count >= tagPos.size()) {
						ArrayList<Integer> longest = new ArrayList<Integer>();
						int shortest = -1;
						int shortestLength = 1000;
						for (int j = 0; j < target.get_texts().size(); j++) {
							textOutline to = target.get_texts().get(j);
							if (to.get_hierarchy() == 0)
								continue;
							if (to.get_hierarchy() != 1 && countFirstLevel >= tagPos.size())
								continue;

							if (longest.size() < tagPos.size()) {
								longest.add(j);
								for (int k = 0; k < longest.size(); k++) {
									if (target.get_texts().get(k).get_text().length() < shortestLength) {
										shortestLength = target.get_texts().get(k).get_text().length();
										shortest = k;
									}
								}
							} else {
								if (to.get_text().length() < shortestLength)
									continue;
								else {
									longest.remove(shortest);
									longest.add(j);
									shortestLength = 1000;
									for (int k = 0; k < longest.size(); k++) {
										if (target.get_texts().get(k).get_text().length() < shortestLength) {
											shortestLength = target.get_texts().get(k).get_text().length();
											shortest = k;
										}
									}
								}
							}
						}

						for (int j = 0; j < matchTextOutlineNum.size(); j++)
							matchTextOutlineNum.set(j, longest.get(j));
					} else {
						for (int j = firstPos + 1; j < sps.size(); j++) {
							if (isInSamePageGroup(sps.get(firstPos).get_PageNum(), sps.get(j).get_PageNum(),
									samePageGroups)) {
								sps.remove(j);
								j--;
							}
						}
						return sps;
					}
				}

				/*
				 * Here, try to complete the matching from tagPage and the
				 * related areas.
				 *
				 * tagPos: where the tagPages locate in sps series.
				 * matchTextOutlineNum: the area following a related tagPage
				 * matches which text. these two lists contains exactly the same
				 * number of elements.
				 *
				 * the completing process will go step by step, from one already
				 * matched area to another (the beginning and the ending will
				 * also be treated as a already matched text) this method will
				 * be controlled by 'startPoint' and 'endPoint', startPoint
				 * initials as -1 and ends when it step in the last tagPage
				 * area, and endPoint always indicates the next 'pre-matched'
				 * text.
				 */

				int startPoint = -1;
				int endPoint = tagPos.size() - 1;
				while (startPoint < tagPos.size() - 1) {
					endPoint = tagPos.size();
					for (int j = startPoint + 1; j < tagPos.size(); j++) {
						if (matchTextOutlineNum.get(j) >= 0) {
							endPoint = j;
							break;
						}
					}

					if (startPoint + 1 == endPoint) {
						/*
						 * When startPoint is next to endPoint, the area at
						 * endPoint can be sure to match the related text, just
						 * do it.
						 */
						int childPageIdx = tagPos.get(endPoint) + 1, childEndPageIdx = 0;
						if (endPoint == tagPos.size() - 1)
							childEndPageIdx = sps.size() - (haveEnding ? 2 : 1);
						else
							childEndPageIdx = tagPos.get(endPoint + 1) - 1;
						
						textOutline outline = target.get_texts().get(matchTextOutlineNum.get(endPoint)); 
						childPageIdx = Math.max(Math.min(childPageIdx, sps.size() - 1), 0);
						childEndPageIdx = Math.max(Math.min(childEndPageIdx, sps.size()-1), 0);
						outline.set_child(sps.get(childPageIdx).get_PageNum());
						outline.set_childEnd(sps.get(childEndPageIdx).get_PageNum());
					} else {
						/*
						 * When there's distance between startPoint and
						 * endPoint, that means inside this step there must be
						 * some 'empty' area need to find a matching text.
						 * Mainly there are 3 conditions: Condition 1:
						 * startPoint is empty (-1) and endPoint is matched
						 * Condition 2: startPoint is matched and endPoint is
						 * empty (last page area) Condition 3: both side is
						 * matched. No matter which condition it belongs to, the
						 * method to solve it is similar, 1. find whether there
						 * are enough free texts with the same hierarchy of the
						 * matched one for the areas 2. find whether there are
						 * enough free texts with same hierarchy for the areas
						 * 3. find whether there are enough free texts for the
						 * areas 4. conclude texts for extra areas. for 1~3, if
						 * so, assign them and jump out.
						 */
						int hierarchy = 10;
						if (startPoint < 0 && endPoint == tagPos.size())
							break;
						else if (startPoint < 0) {
							// Condition 1
							hierarchy = target.get_texts().get(matchTextOutlineNum.get(endPoint)).get_hierarchy();

							int countSame = 0, countHigher = 0, countAll = matchTextOutlineNum.get(endPoint) + 1;

							for (int k = matchTextOutlineNum.get(endPoint); k >= 0; k--) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() == hierarchy)
									countSame++;
								else if (target.get_texts().get(k).get_hierarchy() < hierarchy)
									countHigher++;
							}

							if (countSame >= endPoint + 1) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(endPoint); k >= 0 && a <= endPoint; k--) {
									if (target.get_texts().get(k).get_hierarchy() == hierarchy) {
										if (endPoint - a == tagPos.size() - 1) {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										} else {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							} else if (countHigher >= endPoint + 1) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(endPoint); k >= 0 && a <= endPoint; k--) {
									if (target.get_texts().get(k).get_hierarchy() < hierarchy) {
										if (endPoint - a == tagPos.size() - 1) {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										} else {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							} else if (countAll >= endPoint + 1) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(endPoint); k >= 0 && a <= endPoint; k--) {
									if (endPoint - a == tagPos.size() - 1) {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
										target.get_texts().get(k)
												.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
										target.get_texts().get(k)
												.set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
									}
									a++;
								}
							} else {
								if (endPoint == tagPos.size() - 1) {
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_child(sps.get(tagPos.get(endPoint) + 1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
								} else {
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_child(sps.get(tagPos.get(endPoint) + 1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_childEnd(sps.get(tagPos.get(endPoint + 1) - 1).get_PageNum());
								}

								int a = 1;
								for (int k = matchTextOutlineNum.get(endPoint) - 1; k >= 0 && a <= endPoint; k--) {
									target.get_texts().get(k)
											.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
									target.get_texts().get(k)
											.set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
									a++;
								}

								while (a <= endPoint) {
									textOutline to = new textOutline();
									to.set_text("One Sub Topic");
									to.set_hierarchy(1);
									to.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
									to.set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
									a++;

									ArrayList<textOutline> temp = new ArrayList<textOutline>();
									for (int k = target.get_texts().size() - 1; k >= 0; k--) {
										temp.add(target.get_texts().get(k));
										target.get_texts().remove(k);
									}
									target.get_texts().add(to);
									for (int k = temp.size() - 1; k >= 0; k--) {
										target.get_texts().add(temp.get(k));
									}
									for (int k = 0; k < matchTextOutlineNum.size(); k++) {
										if (matchTextOutlineNum.get(k) > 0)
											matchTextOutlineNum.set(k, matchTextOutlineNum.get(k) + 1);
									}
								}
							}

						} else if (endPoint == tagPos.size()) {
							// Condition 2
							hierarchy = target.get_texts().get(matchTextOutlineNum.get(startPoint)).get_hierarchy();

							int countSame = 0;
							for (int k = matchTextOutlineNum.get(startPoint); k < target.get_texts().size()
									&& target.get_texts().get(k).get_hierarchy() >= hierarchy; k++) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() == hierarchy)
									countSame++;
							}

							int temp = matchTextOutlineNum.get(startPoint);
							for (int k = matchTextOutlineNum.get(startPoint); k >= 0; k--) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() < hierarchy) {
									temp = k;
									break;
								}
							}

							int countHigher = 0,
									countAll = target.get_texts().size() - matchTextOutlineNum.get(startPoint);
							for (int k = temp; k < target.get_texts().size(); k++) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() < hierarchy)
									countHigher++;
							}

							if (countSame >= tagPos.size() - startPoint) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(startPoint); k < target.get_texts().size()
										&& startPoint + a < tagPos.size(); k++) {
									if (target.get_texts().get(k).get_hierarchy() == hierarchy) {
										if (startPoint + a == tagPos.size() - 1) {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										} else {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							} else if (countHigher >= tagPos.size() - startPoint) {
								target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(-1);
								int a = 0;
								for (int k = temp; k < target.get_texts().size()
										&& startPoint + a < tagPos.size(); k++) {
									if (target.get_texts().get(k).get_hierarchy() < hierarchy) {
										if (startPoint + a == tagPos.size() - 1) {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										} else {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							} else if (countAll >= tagPos.size() - startPoint) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(startPoint); k < target.get_texts().size()
										&& startPoint + a < tagPos.size(); k++) {
									if (startPoint + a == tagPos.size() - 1) {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k)
												.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(
												sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}
							} else {
								if (startPoint == tagPos.size() - 1) {
									target.get_texts().get(matchTextOutlineNum.get(startPoint))
											.set_child(sps.get(tagPos.get(startPoint) + 1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(startPoint))
											.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
								} else {
									target.get_texts().get(matchTextOutlineNum.get(startPoint))
											.set_child(sps.get(tagPos.get(startPoint) + 1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(startPoint))
											.set_childEnd(sps.get(tagPos.get(startPoint + 1) - 1).get_PageNum());
								}

								int a = 1;
								int closestHierarchy = 1;
								for (int k = matchTextOutlineNum.get(startPoint) + 1; k < target.get_texts().size()
										&& startPoint + a < tagPos.size(); k++) {
									closestHierarchy = target.get_texts().get(k).get_hierarchy();
									if (startPoint + a == tagPos.size() - 1) {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k)
												.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(
												sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}

								while (startPoint + a < tagPos.size()) {
									textOutline to = new textOutline();
									to.set_text("One Sub Topic");
									to.set_hierarchy(closestHierarchy);
									if (startPoint + a == tagPos.size() - 1) {
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
									target.get_texts().add(to);
								}

							}

						} else {
							// Condition 3
							int hs = target.get_texts().get(matchTextOutlineNum.get(startPoint)).get_hierarchy();
							int he = target.get_texts().get(matchTextOutlineNum.get(endPoint)).get_hierarchy();
							hierarchy = hs > he ? he : hs;

							int countSame = 0;
							for (int k = matchTextOutlineNum.get(startPoint); k <= matchTextOutlineNum.get(endPoint)
									&& target.get_texts().get(k).get_hierarchy() >= hierarchy; k++) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() == hierarchy)
									countSame++;
							}

							int temp = matchTextOutlineNum.get(startPoint);
							for (int k = matchTextOutlineNum.get(startPoint); k >= 0; k--) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() < hierarchy) {
									temp = k;
									break;
								}
							}

							int countHigher = 0, countAll = matchTextOutlineNum.get(endPoint)
									- matchTextOutlineNum.get(startPoint) + 1;
							for (int k = temp; k <= matchTextOutlineNum.get(endPoint); k++) {
								if (target.get_texts().get(k).get_hierarchy() == 0)
									continue;

								if (target.get_texts().get(k).get_hierarchy() < hierarchy)
									countHigher++;
							}

							if (countSame >= endPoint - startPoint + 1) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(startPoint); k <= matchTextOutlineNum.get(endPoint)
										&& startPoint + a <= endPoint; k++) {
									if (target.get_texts().get(k).get_hierarchy() == hierarchy) {
										if (startPoint + a == tagPos.size() - 1) {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										} else {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							} else if (countHigher >= endPoint - startPoint + 1) {
								target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(-1);
								int a = 0;
								for (int k = temp; k <= matchTextOutlineNum.get(endPoint)
										&& startPoint + a <= endPoint; k++) {
									if (target.get_texts().get(k).get_hierarchy() < hierarchy) {
										if (startPoint + a == tagPos.size() - 1) {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										} else {
											target.get_texts().get(k)
													.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(
													sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							} else if (countAll >= endPoint - startPoint + 1) {
								int a = 0;
								for (int k = matchTextOutlineNum.get(startPoint); k <= matchTextOutlineNum.get(endPoint)
										&& startPoint + a <= endPoint; k++) {
									if (startPoint + a == tagPos.size() - 1) {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k)
												.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(
												sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}
							} else {
								target.get_texts().get(matchTextOutlineNum.get(startPoint))
										.set_child(sps.get(tagPos.get(startPoint) + 1).get_PageNum());
								if (endPoint == tagPos.size() - 1) {
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_child(sps.get(tagPos.get(endPoint) + 1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
								} else {
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_child(sps.get(tagPos.get(endPoint) + 1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint))
											.set_childEnd(sps.get(tagPos.get(endPoint + 1) - 1).get_PageNum());
								}

								int a = 1;
								int closestHierarchy = 1;
								for (int k = matchTextOutlineNum.get(startPoint) + 1; k < matchTextOutlineNum
										.get(endPoint) && startPoint + a < endPoint; k++) {
									closestHierarchy = target.get_texts().get(k).get_hierarchy();
									if (startPoint + a == tagPos.size() - 1) {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k)
												.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										target.get_texts().get(k)
												.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(
												sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}

								while (startPoint + a < endPoint) {
									textOutline to = new textOutline();
									to.set_text("One Sub Topic");
									to.set_hierarchy(closestHierarchy);
									if (startPoint + a == tagPos.size() - 1) {
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									} else {
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;

									ArrayList<textOutline> storage = new ArrayList<textOutline>();
									for (int k = target.get_texts().size() - 1; k >= matchTextOutlineNum
											.get(endPoint); k--) {
										storage.add(target.get_texts().get(k));
										target.get_texts().remove(k);
									}
									target.get_texts().add(to);
									for (int k = storage.size() - 1; k >= 0; k--) {
										target.get_texts().add(storage.get(k));
									}
									for (int k = endPoint; k < matchTextOutlineNum.size(); k++) {
										if (matchTextOutlineNum.get(k) > 0)
											matchTextOutlineNum.set(k, matchTextOutlineNum.get(k) + 1);
									}
								}
							}
						}

					}
					startPoint = endPoint;
				}

				target.set_pageType(SlidePageType.TAG_SLIDE);

				// Now delete all unmatched texts

				for (int j = 0; j < target.get_texts().size(); j++) {
					if (target.get_texts().get(j).get_child() < 0) {
						target.get_texts().remove(j);
						j--;
					}
				}

				// Set time info for subtitles in Tag Page -> to corresponding
				// tagPage which will be deleted in the structure.
				for (int j = 0; j < target.get_texts().size(); j++) {
					for (int k = tagPos.get(0); k < sps.size(); k++) {
						if (target.get_texts().get(j).get_child() == sps.get(k).get_PageNum()) {
							target.get_texts().get(j).set_time(sps.get(k > 0 ? k - 1 : k).get_startTime());
							break;
						}
					}
				}

				// Adjust potential hierarchy chaos
				int hierarchyDiff = target.get_texts().get(0).get_hierarchy() - 1;
				if (hierarchyDiff > 0) {
					for (int j = 0; j < target.get_texts().size(); j++) {
						int ch = target.get_texts().get(j).get_hierarchy();
						int nh = ch - hierarchyDiff < 1 ? 1 : ch - hierarchyDiff;
						target.get_texts().get(j).set_hierarchy(nh);
					}
				}

				sps.set(firstPos, target);

				for (int j = firstPos + 1; j < sps.size(); j++) {
					if (isInSamePageGroup(sps.get(firstPos).get_PageNum(), sps.get(j).get_PageNum(), samePageGroups)) {
						sps.remove(j);
						j--;
					}
				}

				// There could be only 1 group of tag pages, so eliminated other
				// options.
				break;
			}
		}
		return sps;
	}

	private FilterableList<slidePage> dealWithSplitPage(FilterableList<slidePage> sps) {

		for (slidePage page: sps)
			if (page.get_pageType() == SlidePageType.TAG_SLIDE)
				return sps;

		ArrayList<Integer> splitPos = new ArrayList<Integer>();

		for (int i = 1; i < sps.size(); i++) {
			if (sps.get(i).get_title().length() == 0) {
				int count = 0;
				int pos[] = { -1, -1 };
				for (int j = 0; j < sps.get(i).get_texts().size(); j++) {
					if (sps.get(i).get_texts().get(j).get_hierarchy() > 0) {
						count++;
						if (pos[0] < 0)
							pos[0] = j;
						else
							pos[1] = j;
					}
				}
				if (count > 0 && count <= 2 && sps.get(i).get_texts().size() <= 2) {
					if (count == 1) {
						String[] words = sps.get(i).get_texts().get(pos[0]).get_text().split(" ");
						int wordsCount = 0;
						int wordsLength = 0;
						for (String w : words) {
							wordsCount++;
							wordsLength += w.length();
						}
						double averageLength = (double) wordsLength / (double) wordsCount;
						if (averageLength < 4)
							continue;

						sps.get(i).set_title(sps.get(i).get_texts().get(pos[0]).get_text());
						sps.get(i).get_texts().remove(pos[0]);
					} else {
						String[] words1 = sps.get(i).get_texts().get(pos[0]).get_text().split(" ");
						String[] words2 = sps.get(i).get_texts().get(pos[1]).get_text().split(" ");
						int wordsCount = 0;
						int wordsLength = 0;
						for (String w : words1) {
							wordsCount++;
							wordsLength += w.length();
						}
						for (String w : words2) {
							wordsCount++;
							wordsLength += w.length();
						}
						double averageLength = (double) wordsLength / (double) wordsCount;
						if (averageLength < 4)
							continue;

						sps.get(i).set_title(sps.get(i).get_texts().get(pos[0]).get_text() + " "
								+ sps.get(i).get_texts().get(pos[1]).get_text());
						sps.get(i).get_texts().remove(pos[1]);
						sps.get(i).get_texts().remove(pos[0]);
					}

					sps.get(i).isSlideWellOrganized();
					if (i != sps.size() - 1) {
						if (splitPos.size() > 0 && splitPos.get(splitPos.size() - 1) == i - 1)
							splitPos.remove(splitPos.size() - 1);
						splitPos.add(i);
						LoggerSingleton.info(
								"Potential Split Page: " + sps.get(i).get_PageNum() + " - " + sps.get(i).get_title());
					}
				}
			}
		}

		if (splitPos.size() > 1) {
			slidePage visualTagPage = new slidePage();
			visualTagPage.contentCopy(sps.get(splitPos.get(0)));
			visualTagPage.set_title("Topics");
			visualTagPage.get_texts().clear();
			visualTagPage.set_pageType(SlidePageType.TAG_SLIDE);

			for (int i = 0; i < splitPos.size(); i++) {
				LoggerSingleton.info("Used Split Page: " + sps.get(splitPos.get(i)).get_PageNum() + " - "
						+ sps.get(splitPos.get(i)).get_title());
				textOutline to = new textOutline();
				to.set_text(sps.get(splitPos.get(i)).get_title());
				to.set_hierarchy(1);
				to.set_child(sps.get(splitPos.get(i) + 1).get_PageNum());

				boolean haveEnding = isHavingEndingPage(sps);
				if (i == splitPos.size() - 1)
					to.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
				else
					to.set_childEnd(sps.get(splitPos.get(i + 1) - 1).get_PageNum());
				visualTagPage.get_texts().add(to);
			}

			for (int i = 0; i < visualTagPage.get_texts().size(); i++) {
				for (int j = splitPos.get(0); j < sps.size(); j++) {
					if (visualTagPage.get_texts().get(i).get_child() == sps.get(j).get_PageNum()) {
						visualTagPage.get_texts().get(i).set_time(sps.get(j > 0 ? j - 1 : j).get_startTime());
						break;
					}
				}
			}

			/*
			 * LoggerSingleton.info(visualTagPage.get_PageNum() + "  " +
			 * visualTagPage.get_title()); for(int j = 0; j <
			 * visualTagPage.get_texts().size(); j++) { textOutline to =
			 * visualTagPage.get_texts().get(j); String indent = "";
			 * if(to.get_hierarchy()==1) indent = "--"; else
			 * if(to.get_hierarchy()==2) indent = "----"; else
			 * if(to.get_hierarchy()==3) indent = "------";
			 * LoggerSingleton.info(indent + to.get_text() + " -> ( " +
			 * to.get_child() + ", " + to.get_childEnd() + " )"); }
			 * LoggerSingleton.info("PageType: " +
			 * visualTagPage.get_pageType()); LoggerSingleton.info();
			 */

			for (int i = splitPos.size() - 1; i >= 0; i--) {
				if (i == 0)
					sps.set(splitPos.get(0), visualTagPage);
				else {
					int temp = splitPos.get(i);
					sps.remove(temp);
				}
			}
		}

		return sps;
	}

	private boolean isSplitPage(slidePage sp) {
		if (sp.get_title().length() == 0) {
			int count = 0;
			int pos[] = { -1, -1 };
			for (int j = 0; j < sp.get_texts().size(); j++) {
				if (sp.get_texts().get(j).get_hierarchy() > 0) {
					count++;
					if (pos[0] < 0)
						pos[0] = j;
					else
						pos[1] = j;
				}
			}
			if (count > 0 && count <= 2 && sp.get_texts().size() <= 2) {
				if (count == 1) {
					String[] words = sp.get_texts().get(pos[0]).get_text().split(" ");
					int wordsCount = 0;
					int wordsLength = 0;
					for (String w : words) {
						wordsCount++;
						wordsLength += w.length();
					}

					double averageLength = (double) wordsLength / (double) wordsCount;
					if (averageLength < 4)
						return false;
					else
						return true;

				} else {
					String[] words1 = sp.get_texts().get(pos[0]).get_text().split(" ");
					String[] words2 = sp.get_texts().get(pos[1]).get_text().split(" ");
					int wordsCount = 0;
					int wordsLength = 0;
					for (String w : words1) {
						wordsCount++;
						wordsLength += w.length();
					}
					for (String w : words2) {
						wordsCount++;
						wordsLength += w.length();
					}

					double averageLength = (double) wordsLength / (double) wordsCount;
					if (averageLength < 4)
						return false;
					else
						return true;
				}
			}
		}

		return false;
	}

	private FilterableList<slidePage> dealWithSectionPage(FilterableList<slidePage> sps) {

		for (slidePage page: sps)
			if (page.get_pageType() == SlidePageType.TAG_SLIDE)
				return sps;

		String[] tags = { "Part", "Topic", "Theme", "Chapter", "Section" };

		ArrayList<Integer> sectionPos = new ArrayList<Integer>();
		for (int i = 1; i < sps.size(); i++) {
			String[] words = sps.get(i).get_title().split(" ");
			if (words.length < 2)
				continue;

			for (int j = 0; j < tags.length; j++) {
				if (words[0].equalsIgnoreCase(tags[j])) {
					algorithmInterface ai = new algorithmInterface();
					if (ai.isNum(words[1]) || ai.isNum(words[1].substring(0, words[1].length() - 1))) {
						sectionPos.add(i);
						continue;
					}
				}
			}
		}

		if (sectionPos.size() > 1) {
			slidePage visualTagPage = new slidePage();
			visualTagPage.contentCopy(sps.get(sectionPos.get(0)));
			visualTagPage.set_title("Topics");
			visualTagPage.set_PageNum(visualTagPage.get_PageNum() - 1);
			visualTagPage.get_texts().clear();
			visualTagPage.set_pageType(SlidePageType.TAG_SLIDE);

			for (int i = 0; i < sectionPos.size(); i++) {
				LoggerSingleton.info("Used Section Page: " + sps.get(sectionPos.get(i)).get_PageNum() + " - "
						+ sps.get(sectionPos.get(i)).get_title());
				textOutline to = new textOutline();
				to.set_text(sps.get(sectionPos.get(i)).get_title());
				to.set_hierarchy(1);
				to.set_child(sps.get(sectionPos.get(i)).get_PageNum());
				to.set_time(sps.get(sectionPos.get(i)).get_startTime());

				boolean haveEnding = isHavingEndingPage(sps);
				if (i == sectionPos.size() - 1)
					to.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
				else
					to.set_childEnd(sps.get(sectionPos.get(i + 1) - 1).get_PageNum());

				String[] words = sps.get(sectionPos.get(i)).get_title().split(" ");
				if (words.length > 2) {
					String newTitle = words[2];
					for (int j = 3; j < words.length; j++)
						newTitle = newTitle + " " + words[j];
					sps.get(sectionPos.get(i)).set_title(newTitle);
				} else {
					if (sps.get(sectionPos.get(i)).get_texts().size() > 0)
						sps.get(sectionPos.get(i)).set_title(sps.get(sectionPos.get(i)).get_texts().get(0).get_text());
					else
						sps.get(sectionPos.get(i)).set_title("<subtopic overview>");
					to.set_text(to.get_text() + " " + sps.get(sectionPos.get(i)).get_title());
				}

				visualTagPage.get_texts().add(to);
			}

			/*
			 * LoggerSingleton.info(visualTagPage.get_PageNum() + "  " +
			 * visualTagPage.get_title()); for(int j = 0; j <
			 * visualTagPage.get_texts().size(); j++) { textOutline to =
			 * visualTagPage.get_texts().get(j); String indent = "";
			 * if(to.get_hierarchy()==1) indent = "--"; else
			 * if(to.get_hierarchy()==2) indent = "----"; else
			 * if(to.get_hierarchy()==3) indent = "------";
			 * LoggerSingleton.info(indent + to.get_text() + " -> ( " +
			 * to.get_child() + ", " + to.get_childEnd() + " )"); }
			 * LoggerSingleton.info("PageType: " +
			 * visualTagPage.get_pageType()); LoggerSingleton.info();
			 */

			ArrayList<slidePage> tempStack = new ArrayList<slidePage>();
			for (int i = sps.size() - 1; i >= sectionPos.get(0); i--) {
				tempStack.add(sps.get(i));
				sps.remove(i);
			}
			sps.add(visualTagPage);
			for (int i = tempStack.size() - 1; i >= 0; i--)
				sps.add(tempStack.get(i));
			tempStack.clear();

		}

		return sps;
	}

	private boolean isHavingEndingPage(ArrayList<slidePage> sps) {
		slidePage lsp = sps.get(sps.size() - 1);
		String title = lsp.get_title();
		if (title.length() == 0)
			title = lsp.get_texts().get(0).get_text();

		String head = "";
		if (title.length() >= 5)
			head = (String) title.subSequence(0, 5);
		// LoggerSingleton.info(head);
		String[] tags = { "Thank", "Summa", "Concl", "Refer", "Quest" };
		for (int i = 0; i < tags.length; i++) {
			if (head.equalsIgnoreCase(tags[i]))
				return true;
		}

		String titleB = sps.get(0).get_title();
		algorithmInterface ai = new algorithmInterface();
		int le = ai.getLevenshteinDistance(title, titleB);
		int longer = title.length() > titleB.length() ? title.length() : titleB.length();
		double lr = (double) le / (double) longer;
		double wr = ai.getSameWordsRatio(title, titleB);
		if (lr < 0.5 || wr >= 0.6)
			return true;

		return false;
	}

	private FilterableList<slidePage> concludeTheme(FilterableList<slidePage> sps, int beginPos, int endPos, int limit) {
		/*
		 * This function is used to create a virtual Index Page for a group of
		 * slides sharing some keywords in their titles. The whole process will
		 * be marching recursively. For each section, the number of the words in
		 * slides will be counted. When a group of titles sharing some words
		 * above a certain ratio, done.
		 *
		 * The most important parameters in this function are 4 'Pos' to mark
		 * the searching region (beginPos, endPos) and the searched region
		 * (firstPos, currentPos) Generally, beginPos <= firstPos < currentPos
		 * <= endPos.
		 */
		ArrayList<textLine> wordsCount = new ArrayList<textLine>();
		boolean found = false;
		int currentPos = beginPos;
		for (int i = beginPos; i <= endPos; i++) {
			// Generally, this loop will not hit the endPos,
			// and the currentPos will only be used to record where the 'shared'
			// word last locates.
			String title = sps.get(i).get_title();
			if (title.length() == 0)
				continue;
			String[] words = title.split(" ");
			algorithmInterface ai = new algorithmInterface();

			// Avoid counting twice for the same word in a single title
			for (int j = 0; j < words.length; j++)
				for (int k = j + 1; k < words.length; k++)
					if (ai.isLogicalSameWords(words[j], words[k]))
						words[k] = "aa";

			for (String w : words) {
				// Abandon short meaningless words (is, the, etc)
				if (w.length() <= 3) {
					char a = w.charAt(0);
					if ((a >= 'a' && a <= 'z') || ai.isSignal(a))
						continue;
				}

				/*
				 * Go through all the words in the current title, if it has
				 * already saved, count++ or else, make this word as a new
				 * candidate. Attention: slideID here is used to save in which
				 * slide the certain word appears. and 'left' here to represent
				 * in which slide this word appeared for 1st time.
				 */
				boolean match = false;
				for (int j = 0; j < wordsCount.size(); j++) {
					if (ai.isLogicalSameWords(w, wordsCount.get(j).get_text())) {
						wordsCount.get(j).set_count(wordsCount.get(j).get_count() + 1);
						wordsCount.get(j).set_slideID(i);
						match = true;
						break;
					}
				}
				if (!match) {
					textLine t = new textLine();
					t.set_text(w);
					t.set_slideID(i);
					t.set_left(i);
					wordsCount.add(t);

				}
			}

			/*
			 * After go through the words in the current title, examine the
			 * 'word' storage, if the total number of the appearance of a word
			 * cannot reach the half of the number of slides counted, delete it.
			 *
			 * And if it need to delete a multi-appearance word, end this
			 * process, find where this word appears last, and create a index
			 * page for the pages between beginPos and the last-appearing-point.
			 * (Situation 1)
			 */
			boolean jump = false;
			for (int j = 0; j < wordsCount.size(); j++) {
				int pageSum = i - beginPos + 1;
				if (wordsCount.get(j).get_count() * 2 < pageSum) {
					if (wordsCount.get(j).get_count() >= limit) {
						found = true;
						jump = true;
						currentPos = wordsCount.get(j).get_slideID();
						break;
					}
					wordsCount.remove(j);
					j--;
				}
			}

			if (jump)
				break;

			/*
			 * If all the words have been removed, means there's no shared word
			 * available, quit either, enter situation 2. commonly in this
			 * situation, there's 2 slides.
			 */
			if (wordsCount.size() == 0) {
				currentPos = i;
				break;
			}
		}

		if (found) {
			/*
			 * Situation 1: In this situation, we know exactly where the begin
			 * and the end are, so just create a virtual index page before the
			 * beginning point, with all the slide titles as its sub texts.
			 * Attention: the pageNum of the new slide will be 1 less than the
			 * pageNum of the beginning point. It is possible that the new slide
			 * share a same pageNum with its previous slide.
			 */
			LoggerSingleton.info(
					"< " + sps.get(beginPos).get_PageNum() + ", " + sps.get(currentPos).get_PageNum() + " > : found");
			for (int i = 0; i < wordsCount.size(); i++) {
				if (wordsCount.get(i).get_count() < limit)
					continue;
				textLine t = wordsCount.get(i);
				LoggerSingleton
						.info(t.get_text() + "\t" + t.get_count() + "\t" + sps.get(t.get_slideID()).get_PageNum());
			}

			int firstPos = currentPos;
			String newTitle = "";
			for (int i = 0; i < wordsCount.size(); i++) {
				if (wordsCount.get(i).get_count() < limit)
					continue;

				if (wordsCount.get(i).get_left() < firstPos)
					firstPos = wordsCount.get(i).get_left();
				newTitle += wordsCount.get(i).get_text();
				if (i < wordsCount.size() - 1)
					newTitle += " ";
			}

			slidePage nsp = new slidePage();
			nsp.set_title(newTitle);
			nsp.set_pageType(SlidePageType.INDEX_SLIDE);
			nsp.set_PageNum(sps.get(firstPos).get_PageNum() - 1);
			nsp.set_startTime(sps.get(firstPos).get_startTime());

			for (int i = firstPos; i <= currentPos; i++) {
				slidePage sp = sps.get(i);
				textOutline t = new textOutline(sp.get_title(), 1);
				t.set_child(sp.get_PageNum());
				t.set_childEnd(sp.get_PageNum());
				nsp.get_texts().add(t);
			}

			ArrayList<slidePage> storage = new ArrayList<slidePage>();
			for (int i = sps.size() - 1; i >= firstPos; i--) {
				storage.add(sps.get(i));
				sps.remove(i);
			}
			sps.add(nsp);
			for (int i = storage.size() - 1; i >= 0; i--)
				sps.add(storage.get(i));
			storage.clear();
			currentPos++;
			endPos++;

			// Recursively continue the process, if possible
			if (currentPos < endPos - 1)
				sps = concludeTheme(sps, currentPos + 1, endPos, limit);
		} else {
			if (wordsCount.size() > 0) {
				/*
				 * Situation 2: Meet the end of all the slides, and with some
				 * potential "shared words" left. First deleting all the words
				 * with their count less than the standard ( 3 or 2 ), if
				 * there's still some words left, using currenPos to find where
				 * they appears last and create a index page for them, then
				 * continue the process. If there's no word left, just end the
				 * process.
				 */
				int max = 0;
				int firstPos = endPos;
				for (int i = 0; i < wordsCount.size(); i++) {
					if (wordsCount.get(i).get_count() < limit) {
						wordsCount.remove(i);
						i--;
					} else {
						if (max < wordsCount.get(i).get_count())
							currentPos = wordsCount.get(i).get_slideID();
						if (wordsCount.get(i).get_left() < firstPos)
							firstPos = wordsCount.get(i).get_left();
					}
				}

				if (wordsCount.size() == 0)
					return sps;

				LoggerSingleton.info("< " + sps.get(beginPos).get_PageNum() + ", " + sps.get(currentPos).get_PageNum()
						+ " > : meet end");
				for (int i = 0; i < wordsCount.size(); i++) {
					textLine t = wordsCount.get(i);
					LoggerSingleton
							.info(t.get_text() + "\t" + t.get_count() + "\t" + sps.get(t.get_slideID()).get_PageNum());
				}

				String newTitle = "";
				for (int i = 0; i < wordsCount.size(); i++) {
					if (wordsCount.get(i).get_count() < limit)
						continue;
					newTitle += wordsCount.get(i).get_text();
					if (i < wordsCount.size() - 1)
						newTitle += " ";
				}

				slidePage nsp = new slidePage();
				nsp.set_title(newTitle);
				nsp.set_pageType(SlidePageType.INDEX_SLIDE);
				nsp.set_PageNum(sps.get(firstPos).get_PageNum() - 1);
				nsp.set_startTime(sps.get(firstPos).get_startTime());

				for (int i = firstPos; i <= currentPos; i++) {
					slidePage sp = sps.get(i);
					textOutline t = new textOutline(sp.get_title(), 1);
					t.set_child(sp.get_PageNum());
					t.set_childEnd(sp.get_PageNum());
					nsp.get_texts().add(t);
				}

				ArrayList<slidePage> storage = new ArrayList<slidePage>();
				for (int i = sps.size() - 1; i >= firstPos; i--) {
					storage.add(sps.get(i));
					sps.remove(i);
				}
				sps.add(nsp);
				for (int i = storage.size() - 1; i >= 0; i--)
					sps.add(storage.get(i));
				storage.clear();
				currentPos++;
				endPos++;

				if (currentPos < endPos)
					sps = concludeTheme(sps, currentPos + 1, endPos, limit);
			} else {
				// Situation 3: no shared word found, continue the process...
				LoggerSingleton.info("< " + sps.get(beginPos).get_PageNum() + ", " + sps.get(currentPos).get_PageNum()
						+ " > : no result");
				if (currentPos < endPos)
					sps = concludeTheme(sps, beginPos + 1, endPos, limit);
			}
		}

		return sps;
	}

	private FilterableList<textOutline> makeFinalTextOutlinesFromSlidePages(FilterableList<slidePage> sps) {
		/*
		 * In this function, all texts from all slide will be reorganized
		 * together as the output.
		 *
		 * there's several different possibilities of the slide: 1. Common page
		 * at root. -- hierarchy 0 2. Index page at root. -- hierarchy 0 3.
		 * Common page under a root Index page occupying a sub text. --
		 * hierarchy 1 4. Common page under a root Index page sharing a sub
		 * text. -- hierarchy 2, that sub text will be hierarchy 1 5. Tag page
		 * (can only be at root) -- no hierarchy, but its all sub text will be
		 * hierarchy 0 6. Common page under Tag Page. -- hierarchy 1 7. Index
		 * page under Tag page. -- hierarchy 1 8. common page under a under-tag
		 * Index page occupying a sub text. -- hierarchy 2 9. common page under
		 * a under-tag Index page sharing a sub text. -- hierarchy 3, that sub
		 * text will be hierarchy 2
		 */
		FilterableList<textOutline> finalResults = new FilterableList<textOutline>();

		for (int i = 0; i < sps.size(); i++) {
			slidePage sp = sps.get(i);

			// Page situation 1
			if (sp.get_pageType().lt(SlidePageType.INDEX_SLIDE)) {
				ArrayList<textOutline> temp = new ArrayList<textOutline>();
				temp = makeTextOutlinesFromOneSlidePage(sp, 0);
				finalResults.addAll(temp);
				LoggerSingleton.info("Page " + sp.get_PageNum() + " done! It is a root-common page.");
				continue;
			}

			if (sp.get_pageType() == SlidePageType.INDEX_SLIDE) {
				// Page situation 2
				textOutline toTitle = new textOutline(sp.get_title(), 0, 0);
				toTitle.set_time(sp.get_startTime());
				finalResults.add(toTitle);
				LoggerSingleton.info("Page " + sp.get_PageNum() + "-0 done! It is the root-index page title.");
				int currentPos = i + 1;
				for (int j = 0; j < sp.get_texts().size(); j++) {
					int beginPageNum = sp.get_texts().get(j).get_child();
					int endPageNum = sp.get_texts().get(j).get_childEnd();

					if (beginPageNum < endPageNum) {
						// Page situation 4 special
						textOutline newTo = new textOutline(sp.get_texts().get(j).get_text(), 1, 0);
						newTo.set_time(sp.get_texts().get(j).get_time());
						finalResults.add(newTo);
						LoggerSingleton.info("Page " + sp.get_PageNum() + "-" + (j + 1)
								+ " done! It is the root-index page subtitle");

						// Page situation 4
						for (int k = currentPos; k < sps.size(); k++) {
							if (sps.get(k).get_PageNum() < beginPageNum)
								continue;
							else if (sps.get(k).get_PageNum() > endPageNum)
								break;
							else {
								ArrayList<textOutline> temp = new ArrayList<textOutline>();
								temp = makeTextOutlinesFromOneSlidePage(sps.get(k), 2);
								finalResults.addAll(temp);
								LoggerSingleton.info("Page " + sps.get(k).get_PageNum()
										+ " done! It is a root-index-multi-sub page");
								currentPos = k + 1;
							}
						}
					} else {
						// Page situation 3
						for (int k = currentPos; k < sps.size(); k++) {
							if (sps.get(k).get_PageNum() >= beginPageNum) {
								ArrayList<textOutline> temp = new ArrayList<textOutline>();
								temp = makeTextOutlinesFromOneSlidePage(sps.get(k), 1);
								finalResults.addAll(temp);
								LoggerSingleton.info("Page " + sp.get_PageNum() + "-" + (j + 1)
										+ " done! It is the root-index page subtitle");
								LoggerSingleton.info("Page " + sps.get(k).get_PageNum()
										+ " done! It is a root-index-unique-sub page");
								currentPos = k + 1;
								break;
							}
						}
					}
				}
				i = currentPos - 1;
				continue;
			}

			if (sp.get_pageType() == SlidePageType.TAG_SLIDE) {
				int currentPos = i + 1;

				for (int j = 0; j < sp.get_texts().size(); j++) {
					int beginPageNum = sp.get_texts().get(j).get_child();
					int endPageNum = sp.get_texts().get(j).get_childEnd();

					// Page situation 5 special
					textOutline newTo = new textOutline(sp.get_texts().get(j).get_text(), 0, 0);
					newTo.set_time(sp.get_texts().get(j).get_time());
					finalResults.add(newTo);
					LoggerSingleton.info("Page " + sp.get_PageNum() + "-" + (j + 1) + " done! It is tag page subtitle");

					for (int k = currentPos; k < sps.size(); k++) {
						if (sps.get(k).get_PageNum() < beginPageNum)
							continue;
						else if (sps.get(k).get_PageNum() > endPageNum)
							break;
						else {
							// Page situation 6
							if (sps.get(k).get_pageType().lt(SlidePageType.INDEX_SLIDE)) {
								ArrayList<textOutline> temp = new ArrayList<textOutline>();
								temp = makeTextOutlinesFromOneSlidePage(sps.get(k), 1);
								finalResults.addAll(temp);
								LoggerSingleton
										.info("Page " + sps.get(k).get_PageNum() + " done! It is a tag-common page");
								currentPos = k + 1;
							} else if (sps.get(k).get_pageType() == SlidePageType.INDEX_SLIDE) {
								// Page situation 7
								textOutline toTitle = new textOutline(sps.get(k).get_title(), 1, 0);
								toTitle.set_time(sps.get(k).get_startTime());
								finalResults.add(toTitle);
								LoggerSingleton.info(
										"Page " + sps.get(k).get_PageNum() + "-0 done! It is a tag-index page title");
								currentPos = k + 1;
								for (int l = 0; l < sps.get(k).get_texts().size(); l++) {
									int subBeginNum = sps.get(k).get_texts().get(l).get_child();
									int subEndNum = sps.get(k).get_texts().get(l).get_childEnd();

									if (subBeginNum < subEndNum) {
										// Page situation 9 special
										textOutline subNewTo = new textOutline(sps.get(k).get_texts().get(l).get_text(),
												2, 0);
										subNewTo.set_time(sps.get(k).get_texts().get(l).get_time());
										finalResults.add(subNewTo);
										LoggerSingleton.info("Page " + sps.get(k).get_PageNum() + "-" + (l + 1)
												+ " done! It is a tag-index page subtitle");

										// Page situation 9
										for (int m = currentPos; m < sps.size(); m++) {
											if (sps.get(m).get_PageNum() < subBeginNum)
												continue;
											else if (sps.get(m).get_PageNum() > subEndNum)
												break;
											else {
												ArrayList<textOutline> temp = new ArrayList<textOutline>();
												temp = makeTextOutlinesFromOneSlidePage(sps.get(m), 3);
												finalResults.addAll(temp);
												LoggerSingleton.info("Page " + sps.get(m).get_PageNum()
														+ " done! It is a tag-index-multi-sub page");
												currentPos = m + 1;
											}
										}
									} else {
										// Page situation 8
										for (int m = currentPos; m < sps.size(); m++) {
											if (sps.get(m).get_PageNum() >= subBeginNum) {
												ArrayList<textOutline> temp = new ArrayList<textOutline>();
												temp = makeTextOutlinesFromOneSlidePage(sps.get(m), 2);
												finalResults.addAll(temp);
												LoggerSingleton.info("Page " + sps.get(k).get_PageNum() + "-" + (l + 1)
														+ " done! It is a tag-index page subtitle");
												LoggerSingleton.info("Page " + sps.get(m).get_PageNum()
														+ " done! It is a tag-index-unique-sub page");
												currentPos = m + 1;
												break;
											}
										}
									}
								}
							}
						}

						k = currentPos - 1;
					}

					i = currentPos - 1;
				}
			}
		}

		return finalResults;
	}

	private ArrayList<textOutline> makeTextOutlinesFromOneSlidePage(slidePage sp, int sp_Hierarchy) {
		ArrayList<textOutline> results = new ArrayList<textOutline>();

		textOutline toTitle = new textOutline(sp.get_title(), sp_Hierarchy, 0);
		toTitle.set_time(sp.get_startTime());
		results.add(toTitle);

		// Important Notice: Here we alternately use the Integer Attribute
		// "_child" to represent the text's hierarchy inside this page
		// And "_hierarchy" is used to represent the final text hierarchy inside
		// the presentation
		// _child <= _hierarchy
		for (int i = 0; i < sp.get_texts().size(); i++) {
			textOutline origin = sp.get_texts().get(i);
			textOutline to = new textOutline(origin.get_text(), origin.get_hierarchy() + sp_Hierarchy,
					origin.get_hierarchy());
			to.set_time(origin.get_time());
			results.add(to);
		}

		return results;
	}

	@SuppressWarnings("unused")
	private FilterableList<slidePage> synchronizeVideoAndFile(FilterableList<slidePage> sps, FilterableList<slidePage> sps_f) {
		LoggerSingleton.info("Sychronization: video-" + sps.size() + ", file-" + sps_f.size());

		algorithmInterface ai = new algorithmInterface();

		/*
		 * Search for matching evidence globally: (NOT exclusive) 0: No Match 1:
		 * Perfectly Match: by levenshtein distance of all textual content 2:
		 * Title Match: same titles 3: Title Similar: similar titles
		 */
		int[][] similarityMatrix = new int[sps.size()][sps_f.size()];
		for (int i = 0; i < sps.size(); i++) {
			slidePage spv = sps.get(i);
			String title_v = spv.get_title().length() > 0 ? spv.get_title()
					: (spv.get_texts().size() > 0 ? spv.get_texts().get(0).get_text() : "");

			for (int j = 0; j < sps_f.size(); j++) {
				slidePage spf = sps_f.get(j);
				String title_f = spf.get_title().length() > 0 ? spf.get_title()
						: (spf.get_texts().size() > 0 ? spf.get_texts().get(0).get_text() : "");
				if (spv.isSamePage(spf))
					similarityMatrix[i][j] = 1;
				else if (title_v.contentEquals(title_f))
					similarityMatrix[i][j] = 2;
				else if (ai.getLevenshteinDistance(title_v, title_f) < Math.max(title_v.length(), title_f.length())
						* 0.5)
					similarityMatrix[i][j] = 3;
				else
					similarityMatrix[i][j] = 0;
			}

		}

		ArrayList<String> groups = new ArrayList<>();
		for (int[] group : similarityMatrix)
			groups.add(Joiner.on("\t").join(Arrays.asList(group)));

		LoggerSingleton.info(Joiner.on("\n").join(groups));

		/*
		 * Matching process based on the order of video screenshot. 1 by 1, no
		 * rewind. If a video page cannot find a "Perfectly Match" file page,
		 * temporarily skip, continue. Once "Perfectly Matched", all pages
		 * between this and last matched video page will be processed. The basic
		 * idea is to use file pages "modify" the video pages.
		 */

		int lastSyncPosInVideo = -1;
		int lastSyncPosInFile = -1;
		ArrayList<Integer[]> changeLog = new ArrayList<Integer[]>();
		for (int i = 0; i < sps.size(); i++) {
			int syncPosInFile = 0 - sps_f.size();

			/*
			 * Search for a "Perfectly Match" file page. It is possible that a
			 * video page matches multiple file pages, but very rare. If so,
			 * choose the ?????
			 */
			for (int j = 0; j < sps_f.size(); j++) {
				if (similarityMatrix[i][j] == 1 && Math.abs(j - i) < Math.abs(j - syncPosInFile))
					syncPosInFile = j;
			}

			if (syncPosInFile >= 0) { // Synchronize the matched page: from file
										// to video

				slidePage spf = new slidePage();
				spf.contentCopy(sps_f.get(syncPosInFile));

				spf.set_startTime(sps.get(i).get_startTime());
				spf.set_PageNum(sps.get(i).get_PageNum());
				for (int k = 0; k < spf.get_texts().size(); k++)
					spf.get_texts().get(k).set_time(sps.get(i).get_startTime());

				if (this.isSplitPage(sps.get(i)) && !this.isSplitPage(spf))
					LoggerSingleton.info("Split-page protect {" + (syncPosInFile + 1) + " <-> " + (i + 1) + "}");
				else {
					sps.set(i, spf);
					LoggerSingleton.info("Synchronize {" + (syncPosInFile + 1) + " -> " + (i + 1) + "}");
				}

				/*
				 * Above Code: Use the matched file page to replace video page,
				 * because of higher accuracy Below Code: Deal with the pages in
				 * between (if existing)
				 */

				if (syncPosInFile - lastSyncPosInFile > 1) { // When there's
																// some pages in
																// file not
																// being matched
																// to video
																// pages

					if (syncPosInFile - lastSyncPosInFile == i - lastSyncPosInVideo) { // The
																						// unmatched
																						// interval
																						// of
																						// video
																						// and
																						// file
																						// is
																						// the
																						// same,
																						// synchronize
																						// 1
																						// by
																						// 1,
																						// regardless
																						// of
																						// content
						for (int k = 1; lastSyncPosInVideo + k < i; k++) {
							slidePage sp_free = new slidePage();
							sp_free.contentCopy(sps_f.get(lastSyncPosInFile + k));

							sp_free.set_startTime(sps.get(lastSyncPosInVideo + k).get_startTime());
							sp_free.set_PageNum(sps.get(lastSyncPosInVideo + k).get_PageNum());
							for (int l = 0; l < sp_free.get_texts().size(); l++)
								sp_free.get_texts().get(l).set_time(sps.get(lastSyncPosInVideo + k).get_startTime());

							if (this.isSplitPage(sps.get(lastSyncPosInVideo + k)) && !this.isSplitPage(sp_free))
								LoggerSingleton.info("Split-page protect {" + (lastSyncPosInFile + k + 1) + " <-> "
										+ (lastSyncPosInVideo + k + 1) + "}");
							else {
								sps.set(lastSyncPosInVideo + k, sp_free);
								LoggerSingleton.info("Synchronize-Pos {" + (lastSyncPosInFile + k + 1) + " -> "
										+ (lastSyncPosInVideo + k + 1) + "}");
							}
						}
					} else if (syncPosInFile - lastSyncPosInFile < i - lastSyncPosInVideo) {
						/*
						 * If the video unmatched interval is larger, go through
						 * them. Try to find a title matched file page for the
						 * current video page If not found, attempt title
						 * similar page If still not found, leave the video page
						 * as before NOTICE: no orders here! May have error of
						 * vp-a matches fp-b, but vp-b matches fp-a......
						 */
						for (int x = lastSyncPosInVideo + 1; x < i; x++) {
							slidePage sp = sps.get(x);
							boolean found = false;
							for (int y = lastSyncPosInFile + 1; y < syncPosInFile; y++) {
								if (similarityMatrix[x][y] == 2) {
									slidePage sp_free = new slidePage();
									sp_free.contentCopy(sps_f.get(y));

									sp_free.set_startTime(sp.get_startTime());
									sp_free.set_PageNum(sp.get_PageNum());
									for (int l = 0; l < sp_free.get_texts().size(); l++)
										sp_free.get_texts().get(l).set_time(sp.get_startTime());

									if (this.isSplitPage(sps.get(x)) && !this.isSplitPage(sp_free))
										LoggerSingleton
												.info("Split-page protect {" + (y + 1) + " <-> " + (x + 1) + "}");
									else {
										sps.set(x, sp_free);
										LoggerSingleton
												.info("Synchronize-Title-e {" + (y + 1) + " -> " + (x + 1) + "}");
									}
									found = true;
									break;
								}
							}

							if (found)
								continue;

							for (int y = lastSyncPosInFile + 1; y < syncPosInFile; y++) {
								if (similarityMatrix[x][y] == 3) {
									slidePage sp_free = new slidePage();
									sp_free.contentCopy(sps_f.get(y));

									sp_free.set_startTime(sp.get_startTime());
									sp_free.set_PageNum(sp.get_PageNum());
									for (int l = 0; l < sp_free.get_texts().size(); l++)
										sp_free.get_texts().get(l).set_time(sp.get_startTime());

									if (this.isSplitPage(sps.get(x)) && !this.isSplitPage(sp_free))
										LoggerSingleton
												.info("Split-page protect {" + (y + 1) + " <-> " + (x + 1) + "}");
									else {
										sps.set(x, sp_free);
										LoggerSingleton
												.info("Synchronize-Title-s {" + (y + 1) + " -> " + (x + 1) + "}");
									}
									break;
								}

							}
						}
					} else {
						/*
						 * If the file unmatched interval is larger...
						 *
						 * Add the missing file page Currently only for single
						 * missing page.
						 */
						if (i - lastSyncPosInVideo == 1 && syncPosInFile - lastSyncPosInFile == 2) {
							Integer[] insert = { i, syncPosInFile - 1 };
							changeLog.add(insert);
						}
					}
				} else if (syncPosInFile - lastSyncPosInFile == 1 || syncPosInFile - lastSyncPosInFile == 0) {
					/*
					 * When there is no interval between two adjacent matched
					 * file pages, delete all redundant video pages in between.
					 */
					if (i - lastSyncPosInVideo > 1)
						for (int x = lastSyncPosInVideo + 1; x < i; x++) {
							Integer[] delete = { x, -1 };
							changeLog.add(delete);
						}
				}

				// move forward
				lastSyncPosInVideo = i;
				lastSyncPosInFile = syncPosInFile;
			}
		}

		for (int x = changeLog.size() - 1; x >= 0; x--) {
			Integer[] change = changeLog.get(x);
			LoggerSingleton.info("Slide " + sps.get(change[0]).get_PageNum() + ": "
					+ (change[1] >= 0 ? "Insert before this page" : "Delete this page"));

			if (change[1] < 0)
				sps.remove((int) change[0]);
			else {
				slidePage spf = new slidePage();
				spf.contentCopy(sps_f.get(change[1]));

				spf.set_startTime(sps.get(change[0]).get_startTime());
				spf.set_PageNum(sps.get(change[0]).get_PageNum());
				for (int k = 0; k < spf.get_texts().size(); k++)
					spf.get_texts().get(k).set_time(sps.get(change[0]).get_startTime());

				for (int i = change[0]; i < sps.size(); i++)
					sps.get(i).set_PageNum(sps.get(i).get_PageNum() + 1);

				sps.add(change[0], spf);
			}
		}

		return sps;
	}

	@SuppressWarnings("serial")
	private FilterableList<slidePage> synchronizeVideoToFile(FilterableList<slidePage> sps, FilterableList<slidePage> sps_f) {
		LoggerSingleton.info("Sychronization: video-" + sps.size() + ", file-" + sps_f.size());

		algorithmInterface ai = new algorithmInterface();

		/*
		 * Search for matching evidence globally: (NOT exclusive) 
		 * 0: No Match 
		 * 1: Perfectly Match: by levenshtein distance of all textual content 
		 * 2: Title Match: same titles 
		 * 3: Title Similar: similar titles
		 */
		int[][] similarityMatrix = new int[sps_f.size()][sps.size()];
		for (slidePage page1: sps_f) {
			String page1Title = page1.get_title().length() > 0 ? page1.get_title() : (page1.get_texts().size() > 0 ? page1.get_texts().get(0).get_text() : "");

			for (slidePage page2: sps) {
				String page2Title = page2.get_title().length() > 0 ? page2.get_title() : (page2.get_texts().size() > 0 ? page2.get_texts().get(0).get_text() : "");
				int similarity = 0;
				if (page1.isSamePage(page2))
					similarity = 1;
				else if (page1Title.contentEquals(page2Title))
					similarity = 2;
				else if (ai.getLevenshteinDistance(page2Title, page1Title) < Math.max(page2Title.length(), page1Title.length()) * 0.5)
					similarity = 3;
				similarityMatrix[sps_f.indexOf(page1)][sps.indexOf(page2)] = similarity;
			}

		}

		ArrayList<String> groups = new ArrayList<>();
		for (final int[] group : similarityMatrix)
			groups.add("[" + Joiner.on(" ").join(new ArrayList<Integer>() {{ for (int i : group) add(i); }}) + "]");

		LoggerSingleton.info("Similarity Matrix: \n" + Joiner.on("\n").join(groups));

		int lastSyncPosInVideo = -1;
		int lastSyncPosInFile = -1;
		ArrayList<Integer> unmatchedList = new ArrayList<Integer>();
		for (int i = 0; i < sps_f.size(); i++) {
			int syncPosInVideo = -1;
			for (int j = lastSyncPosInVideo + 1; j < sps.size(); j++) {
				if (similarityMatrix[i][j] == 1 && StaticMethods.isInSimilarPosition(i, j, sps_f.size(), sps.size())) {
					if (syncPosInVideo < 0)
						syncPosInVideo = j;
					else if (Math.abs(j - i) < Math.abs(syncPosInVideo - i))
						syncPosInVideo = j;
				}

				if (j == sps.size() - 1 && i == sps_f.size() - 1 && syncPosInVideo < 0)
					syncPosInVideo = j + 1;
			}

			if (syncPosInVideo >= 0) {
				// When searching at the "unmatched" end, suppose matching at
				// position "end+1"
				if (syncPosInVideo >= sps.size())
					i++;

				// First synchronize the pages between two "perfectly matched"
				// pages
				if (i - lastSyncPosInFile > 1) {
					// Same distance of file gap and video gap
					if (i - lastSyncPosInFile == syncPosInVideo - lastSyncPosInVideo && i - lastSyncPosInFile <= 3) {
						for (int k = 1; lastSyncPosInFile + k < i; k++) {
							sps_f.get(lastSyncPosInFile + k)
									.set_startTime(sps.get(lastSyncPosInVideo + k).get_startTime());
							for (int l = 0; l < sps_f.get(lastSyncPosInFile + k).get_texts().size(); l++)
								sps_f.get(lastSyncPosInFile + k).get_texts().get(l)
										.set_time(sps.get(lastSyncPosInVideo + k).get_startTime());

							if (this.isSplitPage(sps.get(lastSyncPosInVideo + k))
									&& !this.isSplitPage(sps_f.get(lastSyncPosInFile + k))) {
								sps.get(lastSyncPosInVideo + k)
										.set_PageNum(sps_f.get(lastSyncPosInFile + k).get_PageNum());
								sps_f.set(lastSyncPosInFile + k, sps.get(lastSyncPosInVideo + k));
								LoggerSingleton.info("Synchronize-pos {" + (lastSyncPosInVideo + k + 1) + " -> "
										+ (lastSyncPosInFile + k + 1) + "} with split-page protection");
							} else
								LoggerSingleton.info("Synchronize-pos {" + (lastSyncPosInVideo + k + 1) + " -> "
										+ (lastSyncPosInFile + k + 1) + "}");
						}
					}
					// different gaps
					else// if(i - lastSyncPosInFile < syncPosInVideo -
						// lastSyncPosInVideo)
					{
						int tempSyncPosInVideo = lastSyncPosInVideo;
						for (int x = lastSyncPosInFile + 1; x < i; x++) {
							if (i - x + 1 == syncPosInVideo - tempSyncPosInVideo && i - x + 1 <= 3) {
								for (int k = 0; x + k < i; k++) {
									sps_f.get(x + k).set_startTime(sps.get(tempSyncPosInVideo + k + 1).get_startTime());
									for (int l = 0; l < sps_f.get(x + k).get_texts().size(); l++)
										sps_f.get(x + k).get_texts().get(l)
												.set_time(sps.get(tempSyncPosInVideo + k + 1).get_startTime());

									if (this.isSplitPage(sps.get(tempSyncPosInVideo + k + 1))
											&& !this.isSplitPage(sps_f.get(x + k))) {
										sps.get(tempSyncPosInVideo + k + 1).set_PageNum(sps_f.get(x + k).get_PageNum());
										sps_f.set(x + k, sps.get(tempSyncPosInVideo + k + 1));
										LoggerSingleton.info("Synchronize-subPos {" + (tempSyncPosInVideo + k + 2)
												+ " -> " + (x + k + 1) + "} with split-page protection");
									} else
										LoggerSingleton.info("Synchronize-subPos {" + (tempSyncPosInVideo + k + 2)
												+ " -> " + (x + k + 1) + "}");
								}

								break;
							}

							boolean found = false;
							for (int y = tempSyncPosInVideo + 1; y < syncPosInVideo; y++) {
								if (similarityMatrix[x][y] == 2) {
									sps_f.get(x).set_startTime(sps.get(y).get_startTime());
									for (int l = 0; l < sps_f.get(x).get_texts().size(); l++)
										sps_f.get(x).get_texts().get(l).set_time(sps.get(y).get_startTime());

									if (this.isSplitPage(sps.get(y)) && !this.isSplitPage(sps_f.get(x))) {
										sps.get(y).set_PageNum(sps_f.get(x).get_PageNum());
										sps_f.set(x, sps.get(y));
										LoggerSingleton.info("Synchronize-Title {" + (y + 1) + " -> " + (x + 1)
												+ "} with split-page protection");
									} else
										LoggerSingleton.info("Synchronize-Title {" + (y + 1) + " -> " + (x + 1) + "}");

									found = true;
									tempSyncPosInVideo = y;
									break;
								}
							}

							if (found)
								continue;

							for (int y = tempSyncPosInVideo + 1; y < syncPosInVideo; y++) {
								if (similarityMatrix[x][y] == 3) {
									sps_f.get(x).set_startTime(sps.get(y).get_startTime());
									for (int l = 0; l < sps_f.get(x).get_texts().size(); l++)
										sps_f.get(x).get_texts().get(l).set_time(sps.get(y).get_startTime());

									if (this.isSplitPage(sps.get(y)) && !this.isSplitPage(sps_f.get(x))) {
										sps_f.get(x).set_texts(sps.get(y).get_texts());
										LoggerSingleton.info("Synchronize-partTitle {" + (y + 1) + " -> " + (x + 1)
												+ "} with split-page protection");
									} else
										LoggerSingleton
												.info("Synchronize-partTitle {" + (y + 1) + " -> " + (x + 1) + "}");

									found = true;
									tempSyncPosInVideo = y;
									break;
								}
							}

							if (!found) {
								unmatchedList.add(x);
								LoggerSingleton.info("Synchronizing Failed at [ " + (x + 1) + " ]");
							}
						}
					}
				}

				// For the "unmatched" end, no "perfectly matched" page
				// available, skip.
				if (syncPosInVideo >= sps.size())
					break;

				// Then synchronize the "perfectly matched" page.
				sps_f.get(i).set_startTime(sps.get(syncPosInVideo).get_startTime());
				for (int k = 0; k < sps_f.get(i).get_texts().size(); k++)
					sps_f.get(i).get_texts().get(k).set_time(sps.get(syncPosInVideo).get_startTime());

				if (this.isSplitPage(sps.get(syncPosInVideo)) && !this.isSplitPage(sps_f.get(i))) {
					sps_f.get(i).set_texts(sps.get(syncPosInVideo).get_texts());
					LoggerSingleton.info(
							"Synchronize {" + (syncPosInVideo + 1) + " -> " + (i + 1) + "} with split-page protection");
				} else
					LoggerSingleton.info("Synchronize {" + (syncPosInVideo + 1) + " -> " + (i + 1) + "}");

				// move forward
				lastSyncPosInFile = i;
				lastSyncPosInVideo = syncPosInVideo;
			}

		}

		for (int i = 0; i < unmatchedList.size(); i++) {
			int previousSec, afterSec = 0, continuous = 1;
			previousSec = unmatchedList.get(i) == 0 ? 0
					: StaticMethods.timeToSeconds(sps_f.get(unmatchedList.get(i) - 1).get_startTime().toString());
			afterSec = unmatchedList.get(i) == sps_f.size() - 1 ? 0
					: StaticMethods.timeToSeconds(sps_f.get(unmatchedList.get(i) + 1).get_startTime().toString());

			for (int j = 1; i + j < unmatchedList.size(); j++) {
				if (unmatchedList.get(i + j) - unmatchedList.get(i + j - 1) == 1) {
					// LoggerSingleton.info("%%%%%%%% " +
					// unmatchedList.get(i+j));
					continuous++;
					afterSec = unmatchedList.get(i + j) == sps_f.size() - 1 ? 0
							: StaticMethods.timeToSeconds(sps_f.get(unmatchedList.get(i + j) + 1).get_startTime().toString());
				} else
					break;
			}

			int gap = (afterSec - previousSec > 0)
					? (afterSec - previousSec) / (continuous + (unmatchedList.get(i) == 0 ? 0 : 1)) : 1;
			// LoggerSingleton.info("$$$$$$$$$$$$$ " + afterSec + " " +
			// previousSec + " " + gap);

			for (int j = 0; j < continuous; j++) {
				Time ti;
				if (unmatchedList.get(i) == 0)
					ti = new Time(0 - TimeZone.getDefault().getRawOffset() + gap * 1000 * (j));
				else
					ti = new Time(sps_f.get(unmatchedList.get(i) - 1).get_startTime().getTime() + gap * 1000 * (j + 1));

				sps_f.get(unmatchedList.get(i + j)).set_startTime(ti);
			}

			i += continuous - 1;
		}

		return sps_f;
	}

	
	public void set_topicParams(counts c) {

		if (c.topicCaseStartRatio() > 30)
			this.set_beginWithLowCaseLetter(true);
		else if (c.topicCaseStartRatio() > 20 && c.lev2TopicCaseStartRatio() >= c.lev1TopicCaseStartRatio() * 1.5)
			this.set_beginWithLowCaseLetter(true);
		else
			this.set_beginWithLowCaseLetter(false);

		LoggerSingleton.info("Total Topic: " + c.countSum() + " Low Case Start: " + c.countLowSum() + " Ratio: "
				+ c.topicCaseStartRatio() + "% " + this.is_beginWithLowCaseLetter());
		LoggerSingleton.info("Lev-1 Topic: " + c.count1 + " Low Case Start: " + c.count1low + " Ratio: "
				+ c.lev1TopicCaseStartRatio() + "%");
		LoggerSingleton.info("Lev-2 Topic: " + c.count2 + " Low Case Start: " + c.count2low + " Ratio: "
				+ c.lev2TopicCaseStartRatio() + "%");

		this.set_haveSignBeforeSubtopic(lastRoundBeginningDot || c.topicWithDotRatio() >= 10);

		if (lastRoundBeginningDot) {
			LoggerSingleton.info("All dots have been removed in this round, we must keep this status as TRUE.");
		} else {
			LoggerSingleton.info("Total Topic: " + c.countSum() + " With a dot: " + c.countDotSum() + " Ratio: "
					+ c.topicWithDotRatio() + "% " + this.is_haveSignBeforeSubtopic());
			LoggerSingleton.info("Lev-1 Topic: " + c.count1 + " With a dot: " + c.count1dot + " Ratio: "
					+ c.lev1WithDotRatio() + "%");
			LoggerSingleton.info("Lev-2 Topic: " + c.count2 + " With a dot: " + c.count2dot + " Ratio: "
					+ c.lev2WithDotRatio() + "%");
		}

	}

	public boolean chechAdaptiveDifference() {

		if (is_beginWithLowCaseLetter() != lastRoundLowCaseStart) {
			LoggerSingleton.info("LowCaseStart status changed!!!");
			return true;
		}

		if (is_haveSignBeforeSubtopic() != lastRoundBeginningDot) {
			LoggerSingleton.info("BeginningDot status changed!!!");
			return true;
		}

		if (get_potentialTitleArea().size() != lastRoundTableAreas.size()) {
			LoggerSingleton.info("Potential Title Area changed!!!");
			return true;
		} else {
			for (int[] ref : get_potentialTitleArea()) {
				boolean match = false;
				for (int[] current : lastRoundTableAreas)
					// assign and check in one step
					if (match = Math.abs(current[0] - ref[0]) <= get_pageWidth() / 256
							&& Math.abs(current[1] - ref[1]) <= get_pageHeight() / 256
							&& Math.abs(current[3] - ref[3]) <= get_pageHeight() / 256 && current[2] == ref[2])
						break;
				if (!match) {
					LoggerSingleton.info("Potential Title Area changed!!!");
					return true;
				}

			}
		}

		if (get_potentialHierarchicalGap().size() != lastRoundGaps.size()) {
			LoggerSingleton.info("Hierarchical Gaps changed!!!");
			return true;
		} else {
			for (int ref : get_potentialHierarchicalGap()) {
				boolean match = false;
				for (int current : lastRoundGaps)
					// assign and check in one step
					if (match = Math.abs(current - ref) <= get_pageWidth() / 256)
						break;
				if (!match) {
					LoggerSingleton.info("Hierarchical Gaps changed!!!");
					return true;
				}
			}
		}

		return false;

	}

	public void copyStateForAdaptiveRound() {
		lastRoundLowCaseStart = is_beginWithLowCaseLetter();
		lastRoundBeginningDot = is_haveSignBeforeSubtopic();

		lastRoundTableAreas = new ArrayList<int[]>(get_potentialTitleArea());
		lastRoundGaps = new ArrayList<Integer>(get_potentialHierarchicalGap());
	}

}
