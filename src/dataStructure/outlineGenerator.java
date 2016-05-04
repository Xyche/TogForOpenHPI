package dataStructure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.TimeZone;


import sharedMethods.algorithmInterface;

public class outlineGenerator {
	
	public outlineGenerator(){}
	
	public outlineGenerator(int pageWidth, int pageHeight, String lectureID)
	{
		set_pageWidth(pageWidth);
		set_pageHeight(pageHeight);
		set_lectureID(lectureID);
	}
	
	private int _pageWidth = 1024;
	private int _pageHeight = 768;
	private String _lectureID = "";
	private boolean _isInitial = true;
	
	// 4 parameters in _potentialTitleArea : top, height, align(0:left, 1:middle), axis(left or middle)
	private ArrayList<int[]> _potentialTitleArea = new ArrayList<int[]>();
	
	private ArrayList<Integer> _potentialHierarchicalGap = new ArrayList<Integer>(); 
		
	private boolean _beginWithLowCaseLetter = false;
	private boolean _haveSignBeforeSubtopic = false;
	
 	public boolean is_isInitial() {
		return _isInitial;
	}
 	
	public void set_isInitial(boolean _isInitial) {
		this._isInitial = _isInitial;
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
	
	public String get_lectureID() {
		return _lectureID;
	}

	public void set_lectureID(String _lectureID) {
		this._lectureID = _lectureID;
	}

	public ArrayList<int[]> get_potentialTitleArea() {
		return _potentialTitleArea;
	}
	
	public void set_potentialTitleArea(ArrayList<int[]> _potentialTitleArea) {
		this._potentialTitleArea = _potentialTitleArea;
	}

	public ArrayList<Integer> get_potentialHierarchicalGap() {
		return _potentialHierarchicalGap;
	}

	public void set_potentialHierarchicalGap(
			ArrayList<Integer> _potentialHierarchicalGap) {
		this._potentialHierarchicalGap = _potentialHierarchicalGap;
	}

	public boolean is_beginWithLowCaseLetter() {
		return _beginWithLowCaseLetter;
	}

	public void set_beginWithLowCaseLetter(boolean _beginWithLowCaseLetter) {
		this._beginWithLowCaseLetter = _beginWithLowCaseLetter;
	}

	public boolean is_haveSignBeforeSubtopic() {
		return _haveSignBeforeSubtopic;
	}

	public void set_haveSignBeforeSubtopic(boolean _haveSignBeforeSubtopic) {
		this._haveSignBeforeSubtopic = _haveSignBeforeSubtopic;
	}

	//Real Functions below
	
	public ArrayList<textOutline> generateOutline(ArrayList<textLine> tll)
	{
		System.out.println("< STEP 2: Delete logo which appears in same position of many pages >");
		ArrayList<textLine> tl2 = new ArrayList<textLine>();
		tll = removeLogo(tll, this._isInitial);
				
		/* Generate slidePage structure from textLine, separated by slideID */
		ArrayList<slidePage> sps = new ArrayList<slidePage>();
		int tempPageNum = 1;
		System.out.println("< STEP 3: Find title and load in hierarchy >");
		for(int i = 0; i < tll.size(); i++)
		{
			textLine t = tll.get(i);
			if(t.get_slideID() == tempPageNum)
				tl2.add(t);
			else
			{
				slidePage sp = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
							 : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
				sps.add(sp);
				System.out.println();
				tl2.clear();
				tempPageNum = t.get_slideID();
				tl2.add(t);
			}
		}
		slidePage spl = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
					  : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
		sps.add(spl);
		tl2.clear();
		tll.clear();
		
		//Analyze title position for further adaptive procedure
		int totalPage = sps.size();
		this._potentialTitleArea.clear();
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_PageNum() < 0)
			{
				sps.remove(i);
				totalPage--;
				i--;
				continue;
			}
			else if(sps.get(i).get_title().length() < 1)
				continue;
			else
			{
				if(sps.get(i).get_titleLocation().length != 4)
					continue;
				textLine t = new textLine();
				t.set_left(sps.get(i).get_titleLocation()[0]);
				t.set_top(sps.get(i).get_titleLocation()[2]);
				t.set_bottom(sps.get(i).get_titleLocation()[3]);
				t.set_height(sps.get(i).get_titleLocation()[3] - sps.get(i).get_titleLocation()[2]);
				t.set_width(sps.get(i).get_titleLocation()[1] - sps.get(i).get_titleLocation()[0]);
				
				boolean match = false;
				for(int j = 0; j < tl2.size(); j++)
				{
					if(t.isSameTitlePosition(tl2.get(j), _pageWidth, _pageHeight, false))
					{
						tl2.get(j).set_left((t.get_left() < tl2.get(j).get_left()) ? t.get_left() : tl2.get(j).get_left());
						tl2.get(j).set_top((t.get_top() < tl2.get(j).get_top()) ? t.get_top() : tl2.get(j).get_top());
						tl2.get(j).set_bottom((t.get_bottom() > tl2.get(j).get_bottom()) ? t.get_bottom() : tl2.get(j).get_bottom());
						tl2.get(j).set_height(tl2.get(j).get_bottom() - tl2.get(j).get_top());
						tl2.get(j).set_width((tl2.get(j).get_width() * tl2.get(j).get_count() + t.get_width())/(tl2.get(j).get_count()+1));
						tl2.get(j).set_count(tl2.get(j).get_count()+1);
						match = true;
						break;
					}
				}
				if(!match)
				{
					tl2.add(t);
					tl2.get(tl2.size()-1).set_count(1);
					tl2.get(tl2.size()-1).set_type(0);
				}
			}
		}
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 15)
				divider = 3;
			else if(totalPage < 25)
				divider = 4;
			else if(totalPage < 40)
				divider = 5;
			else if(totalPage < 60)
				divider = 6;
			else
				divider = 7;
			
			if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
				tl2.remove(i);
		}
		System.out.println();
		System.out.println("Frequently Used Title Area: [top, height, alignment: axis | count]");
		for(int i = 0; i < tl2.size(); i++)
		{
			textLine t = tl2.get(i);
			int[] pta = {t.get_top(), t.get_bottom()-t.get_top(), t.get_type(), t.get_left()};
			this._potentialTitleArea.add(pta);
			System.out.println("[" + pta[0] + ", " + pta[1] + ", " + (pta[2] == 1 ? "Center: " : "Left: ") + pta[3] + " | " + t.get_count() + "]");
		}
		tl2.clear();
		
		//Search for centered PTA
		totalPage = sps.size();
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_PageNum() < 0)
			{
				sps.remove(i);
				totalPage--;
				i--;
				continue;
			}
			else if(sps.get(i).get_title().length() < 1)
				continue;
			else
			{
				if(sps.get(i).get_titleLocation().length != 4)
					continue;
				textLine t = new textLine();
				t.set_left(sps.get(i).get_titleLocation()[0]);
				t.set_top(sps.get(i).get_titleLocation()[2]);
				t.set_bottom(sps.get(i).get_titleLocation()[3]);
				t.set_height(sps.get(i).get_titleLocation()[3] - sps.get(i).get_titleLocation()[2]);
				t.set_width(sps.get(i).get_titleLocation()[1] - sps.get(i).get_titleLocation()[0]);
				
				boolean match = false;
				for(int j = 0; j < tl2.size(); j++)
				{
					if(t.isSameTitlePosition(tl2.get(j), _pageWidth, _pageHeight, true))
					{
						tl2.get(j).set_left((t.get_left() < tl2.get(j).get_left()) ? t.get_left() : tl2.get(j).get_left());
						tl2.get(j).set_top((t.get_top() < tl2.get(j).get_top()) ? t.get_top() : tl2.get(j).get_top());
						tl2.get(j).set_bottom((t.get_bottom() > tl2.get(j).get_bottom()) ? t.get_bottom() : tl2.get(j).get_bottom());
						tl2.get(j).set_height(tl2.get(j).get_bottom() - tl2.get(j).get_top());
						tl2.get(j).set_width((t.get_width() > tl2.get(j).get_width()) ? t.get_width() : tl2.get(j).get_width());
						tl2.get(j).set_count(tl2.get(j).get_count()+1);
						match = true;
						break;
					}
				}
				if(!match)
				{
					tl2.add(t);
					tl2.get(tl2.size()-1).set_count(1);
					tl2.get(tl2.size()-1).set_type(1);
				}
			}
		}
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 15)
				divider = 3;
			else if(totalPage < 25)
				divider = 4;
			else if(totalPage < 40)
				divider = 5;
			else if(totalPage < 60)
				divider = 6;
			else
				divider = 7;
			
			if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
				tl2.remove(i);
		}
		for(int i = 0; i < tl2.size(); i++)
		{
			textLine t = tl2.get(i);
			int[] pta = {t.get_top(), t.get_bottom()-t.get_top(), t.get_type(), t.get_left() + t.get_width()/2};
			this._potentialTitleArea.add(pta);
			System.out.println("[" + pta[0] + ", " + pta[1] + ", " + (pta[2] == 1 ? "Center: " : "Left: ") + pta[3] + " | " + t.get_count() + "]");
		}
		tl2.clear();
		

		//Analyze hierarchical gap for adaptive procedure
		totalPage = 0;
		this._potentialHierarchicalGap.clear();
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_levelCoordinates().length != 3)
				continue;
			else if(sps.get(i).get_levelCoordinates()[0] < 0 || sps.get(i).get_levelCoordinates()[1] < 0)
				continue;
			else
			{
				totalPage++;
				int gap = sps.get(i).get_levelCoordinates()[1] - sps.get(i).get_levelCoordinates()[0];
				//Here we use only 2 attributes of textLine, slideID to mark the gap, and the count.
				textLine t = new textLine();
				t.set_slideID(gap);
				
				boolean match = false;
				for(int j = 0; j < tl2.size(); j++)
				{
					if(tl2.get(j).get_slideID() >= gap - 3 && tl2.get(j).get_slideID() <= gap + 3)
					{
						tl2.get(j).set_count(tl2.get(j).get_count() + 1);
						match = true;
						break;
					}
				}
				if(!match)
				{
					tl2.add(t);
					tl2.get(tl2.size()-1).set_count(1);
				}
			}
		}
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 25)
				divider = 4;
			else if(totalPage < 40)
				divider = 5;
			else if(totalPage < 60)
				divider = 6;
			else
				divider = 7;
			
			if(tl2.get(i).get_count() <= 3 || tl2.get(i).get_count() < totalPage / divider)
				tl2.remove(i);
		}
		System.out.println();
		for(int i = 0; i < tl2.size(); i++)
		{
			this._potentialHierarchicalGap.add(tl2.get(i).get_slideID());
			System.out.println("Potential Gap: " + tl2.get(i).get_slideID() + '\t' + tl2.get(i).get_count());
		}
		tl2.clear();
		
		
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 3 >");
		System.out.println();
		//int count = 0;

		/*
		File newFile = new File("C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\IEEE-TLT\\Evaluation\\Adaptive\\" + this.get_lectureID() + "-Output.txt");
		if(newFile.exists())
		{
			newFile.delete();
		}*/
		
		
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			/*
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
				output.newLine();
				output.append("Page " + sp.get_PageNum());
				output.newLine();
				output.append("0" + '\t' + sp.get_title());
				output.newLine();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
				
				/*
				try {
					if(to.get_hierarchy() > 0)
					{
						BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
						output.append(Integer.toString(to.get_hierarchy()) + '\t' + to.get_text());
						output.newLine();
						output.close();
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
			
			//count += sp.get_texts().size();
			//if(sp.get_title().length() > 0)
				//count++;
		}
		System.out.println("-------------------------------------------------------");
		/*
		System.out.println("Original Slides:\t" + sps.size());
		System.out.println("Modified text-lines:\t" + count);
		System.out.println("$$$$");
		*/
		ArrayList<ArrayList<Integer>> samePageGroups = new ArrayList<ArrayList<Integer>>();
		samePageGroups = findSamePages(sps);
		
		
		
		System.out.println();
		System.out.println("< STEP 4: Remove repeated and empty pages >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		
		
		sps = removeRepeatedPages(sps, samePageGroups);
		
		samePageGroups.clear();
		samePageGroups = findSamePages(sps);
		
		System.out.println();
		System.out.println("< STEP 5: Remove live show >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		sps = removeLiveShow(sps, samePageGroups);
		
		System.out.println();
		System.out.println("< STEP 6: Combine continuous slides >");
		
		sps = combineContinuedSlides(sps, 0, sps.size()-1);
						
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 6 >");
		System.out.println();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			System.out.println(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			System.out.println();
		} 
		System.out.println("-------------------------------------------------------");
		
		
		
		samePageGroups.clear();
		samePageGroups = findSamePages(sps);
		
		System.out.println();
		System.out.println("< STEP 7: Find tag page >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		sps = dealWithTagPage(sps, samePageGroups);
		
		System.out.println();
		System.out.println("< STEP 8: Find split page and make them as visual tag page>");
		
		boolean isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}		
		
		if(!isTag)	
			sps = dealWithSplitPage(sps);
		
		System.out.println();
		System.out.println("< STEP 9: Find section pages and make a visual tag page for them>");
		
		isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}
		
		if(!isTag)	
			sps = dealWithSectionPage(sps);
			
		//Now, delete all unorganized texts and pages without title
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_title().length() < 1)
			{
				if(sps.get(i).get_pageType() < 0)
				{
					sps.remove(i);
					i--;
					continue;
				}
				else if(sps.get(i).get_texts().get(0).get_hierarchy() == 1)
				{
					sps.get(i).set_title(sps.get(i).get_texts().get(0).get_text());
					sps.get(i).get_texts().remove(0);
				}
				else
				{
					sps.remove(i);
					i--;
					continue;
				}
			}
			for(int j = 0; j < sps.get(i).get_texts().size(); j++)
			{
				if(sps.get(i).get_texts().get(j).get_hierarchy() == 0)
				{
					sps.get(i).get_texts().remove(j);
					j--;
				}
			}
		}
		
		isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}
		
		
		if(!isTag)
			sps = combineContinuedSlides(sps, 0, sps.size()-1);
		else
		{
			int tagPos = 0;
			for(int i = 0; i < sps.size(); i++)
			{
				if(sps.get(i).get_pageType() == 2)
				{
					tagPos = i;
					break;
				}
			}
			sps = combineContinuedSlides(sps, 0, tagPos-1);
		}
		
		System.out.println();
		System.out.println("< STEP 10: Search index page >");
		System.out.println();
		
		if(!isTag)
			sps = findIndexPage(sps, 1, sps.size()-1);
		else
		{
			for(int i = 0; i < sps.size(); i++)
			{
				if(sps.get(i).get_pageType() == 2)
				{
					sps = findIndexPage(sps, 1, i-1);
					for(int j = 0; j < sps.get(i).get_texts().size(); j++)
					{
						textOutline currentTo = sps.get(i).get_texts().get(j);
						int beginPos = i, endPos = i;
						for(int k = i + 1; k < sps.size(); k++)
						{
							if(sps.get(k).get_PageNum() >= currentTo.get_child())
							{
								beginPos = k;
								break;
							}
						}
						
						for(int k = beginPos; k < sps.size(); k++)
						{							
							if(sps.get(k).get_PageNum() >= currentTo.get_childEnd())
							{
								endPos = k;
								break;
							}
						}
						sps = findIndexPage(sps, beginPos, endPos);
					}
				}
			}
		}
		
		System.out.println();
		System.out.println("< STEP 11: Conclude a visual index page for pages with similar titles >");
		System.out.println();
				
		int beginPos = 1;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() <= 0)
				continue;
			else if(sps.get(i).get_pageType() == 1)
			{
				int currentSlideNum = sps.get(i).get_PageNum();
				if(beginPos < i-1)
					sps = concludeTheme(sps, beginPos, i-1, 3);
				
				for(int j = i; j < sps.size(); j++)
				{
					if(sps.get(j).get_PageNum() >= currentSlideNum)
					{
						i = j;
						break;
					}					
				}
				currentSlideNum = sps.get(i).get_texts().get(sps.get(i).get_texts().size()-1).get_childEnd();
				for(int j = i; j < sps.size(); j++)
				{
					if(sps.get(j).get_PageNum() >= currentSlideNum)
					{
						i = j;
						beginPos = j + 1;
						
						break;
					}
				}
				
			}
			else if(sps.get(i).get_pageType() == 2)
			{
				sps = concludeTheme(sps, beginPos, i-1, 3);
				isTag = true;
				break;
			}
		}
		if(!isTag && beginPos < sps.size()-1)
			sps = concludeTheme(sps, beginPos, sps.size()-2, 3);
			

		
		//Add time info to each text
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() <= 0)
			{
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					if(to.get_time().before(sps.get(i).get_startTime()))
						to.set_time(sps.get(i).get_startTime());
				}
			}
			else if(sps.get(i).get_pageType() <= 2)
			{
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					for(int k = i+1; k < sps.size(); k++)
					{
						if(to.get_child() == sps.get(k).get_PageNum() && to.get_time().before(sps.get(i).get_startTime()))
							to.set_time(sps.get(k).get_startTime());
					}
				}
			}
		}
		
		
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 10 >");
		System.out.println();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime() + "  ");
			System.out.println(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text() + " -> ( " + to.get_child() + ", " + to.get_childEnd() + " ) " + to.get_time());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			System.out.println();
		}
		System.out.println("-------------------------------------------------------");
		
		//System.out.println("$$$$\tFinal Slides:\t" + sps.size());
		
		ArrayList<textOutline> finalResults = new ArrayList<textOutline>();
		finalResults = makeFinalTextOutlinesFromSlidePages(sps);
		
		this.set_isInitial(false);
		return finalResults;
	}

	public ArrayList<textOutline> generateOutlineWithPDF(ArrayList<textLine> tll, ArrayList<textLine> tll_pdf)
	{
		System.out.println("< STEP 2-1: Delete logo which appears in same position of many pages for video stream>");
		ArrayList<textLine> tl2 = new ArrayList<textLine>();
		tll = removeLogo(tll, this._isInitial);
		System.out.println("< STEP 2-2: Delete logo which appears in same position of many pages for PDF file>");
		tll_pdf = removeLogo(tll_pdf, this._isInitial);
				
		/* Generate slidePage structure from textLine, separated by slideID */
		ArrayList<slidePage> sps = new ArrayList<slidePage>();
		int tempPageNum = 1;
		System.out.println("< STEP 3-1: Find title and load in hierarchy for video stream>");
		for(int i = 0; i < tll.size(); i++)
		{
			textLine t = tll.get(i);
			if(t.get_slideID() == tempPageNum)
				tl2.add(t);
			else
			{
				slidePage sp = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
							 : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
				sps.add(sp);
				System.out.println();
				tl2.clear();
				tempPageNum = t.get_slideID();
				tl2.add(t);
			}
		}
		slidePage spl = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
					  : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
		sps.add(spl);
		tl2.clear();
		tll.clear();
		
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 3 (video stream)>");
		System.out.println();
		
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			/* show result */
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
		}
		System.out.println("-------------------------------------------------------");
		
		
		/* Generate slidePage structure from textLine, separated by slideID */
		ArrayList<slidePage> sps_pdf = new ArrayList<slidePage>();
		tempPageNum = 1;
		System.out.println("< STEP 3-2: Find title and load in hierarchy for PDF file>");
		for(int i = 0; i < tll_pdf.size(); i++)
		{
			textLine t = tll_pdf.get(i);
			if(t.get_slideID() == tempPageNum)
				tl2.add(t);
			else
			{
				slidePage sp = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
							 : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
				sps_pdf.add(sp);
				System.out.println();
				tl2.clear();
				tempPageNum = t.get_slideID();
				tl2.add(t);
			}
		}
		slidePage spl_pdf = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
					  : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
		sps_pdf.add(spl_pdf);
		tl2.clear();
		tll_pdf.clear();
		
		
		
		//Analyze title position for further adaptive procedure only for PDF file (better accuracy)
		int totalPage = sps_pdf.size();
		this._potentialTitleArea.clear();
		for(int i = 0; i < sps_pdf.size(); i++)
		{
			if(sps_pdf.get(i).get_PageNum() < 0)
			{
				sps_pdf.remove(i);
				totalPage--;
				i--;
				continue;
			}
			else if(sps_pdf.get(i).get_title().length() < 1)
				continue;
			else
			{
				if(sps_pdf.get(i).get_titleLocation().length != 4)
					continue;
				textLine t = new textLine();
				t.set_left(sps_pdf.get(i).get_titleLocation()[0]);
				t.set_top(sps_pdf.get(i).get_titleLocation()[2]);
				t.set_bottom(sps_pdf.get(i).get_titleLocation()[3]);
				t.set_height(sps_pdf.get(i).get_titleLocation()[3] - sps_pdf.get(i).get_titleLocation()[2]);
				t.set_width(sps_pdf.get(i).get_titleLocation()[1] - sps_pdf.get(i).get_titleLocation()[0]);
				
				boolean match = false;
				for(int j = 0; j < tl2.size(); j++)
				{
					if(t.isSameTitlePosition(tl2.get(j), _pageWidth, _pageHeight, false))
					{
						tl2.get(j).set_left((t.get_left() < tl2.get(j).get_left()) ? t.get_left() : tl2.get(j).get_left());
						tl2.get(j).set_top((t.get_top() < tl2.get(j).get_top()) ? t.get_top() : tl2.get(j).get_top());
						tl2.get(j).set_bottom((t.get_bottom() > tl2.get(j).get_bottom()) ? t.get_bottom() : tl2.get(j).get_bottom());
						tl2.get(j).set_height(tl2.get(j).get_bottom() - tl2.get(j).get_top());
						tl2.get(j).set_width((tl2.get(j).get_width() * tl2.get(j).get_count() + t.get_width())/(tl2.get(j).get_count()+1));
						tl2.get(j).set_count(tl2.get(j).get_count()+1);
						match = true;
						break;
					}
				}
				if(!match)
				{
					tl2.add(t);
					tl2.get(tl2.size()-1).set_count(1);
					tl2.get(tl2.size()-1).set_type(0);
				}
			}
		}
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 15)
				divider = 3;
			else if(totalPage < 25)
				divider = 4;
			else if(totalPage < 40)
				divider = 5;
			else if(totalPage < 60)
				divider = 6;
			else
				divider = 7;
			
			if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
				tl2.remove(i);
		}
		System.out.println();
		System.out.println("Frequently Used Title Area: [top, height, alignment: axis | count]");
		for(int i = 0; i < tl2.size(); i++)
		{
			textLine t = tl2.get(i);
			int[] pta = {t.get_top(), t.get_bottom()-t.get_top(), t.get_type(), t.get_left()};
			this._potentialTitleArea.add(pta);
			System.out.println("[" + pta[0] + ", " + pta[1] + ", " + (pta[2] == 1 ? "Center: " : "Left: ") + pta[3] + " | " + t.get_count() + "]");
		}
		tl2.clear();
		
		//Search for centered PTA
		totalPage = sps_pdf.size();
		for(int i = 0; i < sps_pdf.size(); i++)
		{
			if(sps_pdf.get(i).get_PageNum() < 0)
			{
				sps_pdf.remove(i);
				totalPage--;
				i--;
				continue;
			}
			else if(sps_pdf.get(i).get_title().length() < 1)
				continue;
			else
			{
				if(sps_pdf.get(i).get_titleLocation().length != 4)
					continue;
				textLine t = new textLine();
				t.set_left(sps_pdf.get(i).get_titleLocation()[0]);
				t.set_top(sps_pdf.get(i).get_titleLocation()[2]);
				t.set_bottom(sps_pdf.get(i).get_titleLocation()[3]);
				t.set_height(sps_pdf.get(i).get_titleLocation()[3] - sps_pdf.get(i).get_titleLocation()[2]);
				t.set_width(sps_pdf.get(i).get_titleLocation()[1] - sps_pdf.get(i).get_titleLocation()[0]);
				
				boolean match = false;
				for(int j = 0; j < tl2.size(); j++)
				{
					if(t.isSameTitlePosition(tl2.get(j), _pageWidth, _pageHeight, true))
					{
						tl2.get(j).set_left((t.get_left() < tl2.get(j).get_left()) ? t.get_left() : tl2.get(j).get_left());
						tl2.get(j).set_top((t.get_top() < tl2.get(j).get_top()) ? t.get_top() : tl2.get(j).get_top());
						tl2.get(j).set_bottom((t.get_bottom() > tl2.get(j).get_bottom()) ? t.get_bottom() : tl2.get(j).get_bottom());
						tl2.get(j).set_height(tl2.get(j).get_bottom() - tl2.get(j).get_top());
						tl2.get(j).set_width((t.get_width() > tl2.get(j).get_width()) ? t.get_width() : tl2.get(j).get_width());
						tl2.get(j).set_count(tl2.get(j).get_count()+1);
						match = true;
						break;
					}
				}
				if(!match)
				{
					tl2.add(t);
					tl2.get(tl2.size()-1).set_count(1);
					tl2.get(tl2.size()-1).set_type(1);
				}
			}
		}
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 15)
				divider = 3;
			else if(totalPage < 25)
				divider = 4;
			else if(totalPage < 40)
				divider = 5;
			else if(totalPage < 60)
				divider = 6;
			else
				divider = 7;
			
			if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
				tl2.remove(i);
		}
		for(int i = 0; i < tl2.size(); i++)
		{
			textLine t = tl2.get(i);
			int[] pta = {t.get_top(), t.get_bottom()-t.get_top(), t.get_type(), t.get_left() + t.get_width()/2};
			this._potentialTitleArea.add(pta);
			System.out.println("[" + pta[0] + ", " + pta[1] + ", " + (pta[2] == 1 ? "Center: " : "Left: ") + pta[3] + " | " + t.get_count() + "]");
		}
		tl2.clear();
		

		//Analyze hierarchical gap for adaptive procedure
		totalPage = 0;
		this._potentialHierarchicalGap.clear();
		for(int i = 0; i < sps_pdf.size(); i++)
		{
			if(sps_pdf.get(i).get_levelCoordinates().length != 3)
				continue;
			else if(sps_pdf.get(i).get_levelCoordinates()[0] < 0 || sps_pdf.get(i).get_levelCoordinates()[1] < 0)
				continue;
			else
			{
				totalPage++;
				int gap = sps_pdf.get(i).get_levelCoordinates()[1] - sps_pdf.get(i).get_levelCoordinates()[0];
				//Here we use only 2 attributes of textLine, slideID to mark the gap, and the count.
				textLine t = new textLine();
				t.set_slideID(gap);
				
				boolean match = false;
				for(int j = 0; j < tl2.size(); j++)
				{
					if(tl2.get(j).get_slideID() >= gap - 3 && tl2.get(j).get_slideID() <= gap + 3)
					{
						tl2.get(j).set_count(tl2.get(j).get_count() + 1);
						match = true;
						break;
					}
				}
				if(!match)
				{
					tl2.add(t);
					tl2.get(tl2.size()-1).set_count(1);
				}
			}
		}
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 25)
				divider = 4;
			else if(totalPage < 40)
				divider = 5;
			else if(totalPage < 60)
				divider = 6;
			else
				divider = 7;
			
			if(tl2.get(i).get_count() <= 3 || tl2.get(i).get_count() < totalPage / divider)
				tl2.remove(i);
		}
		System.out.println();
		for(int i = 0; i < tl2.size(); i++)
		{
			this._potentialHierarchicalGap.add(tl2.get(i).get_slideID());
			System.out.println("Potential Gap: " + tl2.get(i).get_slideID() + '\t' + tl2.get(i).get_count());
		}
		tl2.clear();
		
		
		

		System.out.println();
		System.out.println("< RESULT after step 3 (PDF file)>");
		System.out.println();
		
		for(int i = 0; i < sps_pdf.size(); i++)
		{
			slidePage sp = sps_pdf.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
		}
		System.out.println("-------------------------------------------------------");
		
		//sps = synchronizeVideoAndFile(sps, sps_pdf);
		sps = synchronizeVideoToFile(sps, sps_pdf);
		//sps_pdf.clear();
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 3.5: synchronization >");
		System.out.println();
		
		File newFile = new File("C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\_OCR_result\\" + this.get_lectureID() + "\\sync");
		if(newFile.exists())
		{
			newFile.delete();
		}/**/
		
		
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			/**/
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
				output.append(sp.get_PageNum() + "\t" + sp.get_startTime().toString());
				output.newLine();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/* show result */
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
				
				/*
				try {
					if(to.get_hierarchy() > 0)
					{
						BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
						output.append(Integer.toString(to.get_hierarchy()) + '\t' + to.get_text());
						output.newLine();
						output.close();
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
			
			//count += sp.get_texts().size();
			//if(sp.get_title().length() > 0)
				//count++;
		}
		System.out.println("-------------------------------------------------------");
		
		
		
		
		ArrayList<ArrayList<Integer>> samePageGroups = new ArrayList<ArrayList<Integer>>();
		samePageGroups = findSamePages(sps);
				
		System.out.println();
		System.out.println("< STEP 4: Remove repeated and empty pages >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		
		
		sps = removeRepeatedPages(sps, samePageGroups);
		
		samePageGroups.clear();
		samePageGroups = findSamePages(sps);
		
		System.out.println();
		System.out.println("< STEP 5: Remove live show >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		sps = removeLiveShow(sps, samePageGroups);
		

		
		System.out.println();
		System.out.println("< STEP 6: Combine continuous slides >");
		
		sps = combineContinuedSlides(sps, 0, sps.size()-1);
						
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 6 >");
		System.out.println();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			System.out.println(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			System.out.println();
		} 
		System.out.println("-------------------------------------------------------");
		

		
		samePageGroups.clear();
		samePageGroups = findSamePages(sps);
		
		System.out.println();
		System.out.println("< STEP 7: Find tag page >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		sps = dealWithTagPage(sps, samePageGroups);
		
		System.out.println();
		System.out.println("< STEP 8: Find split page and make them as visual tag page>");
		
		boolean isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}		
		
		if(!isTag)	
			sps = dealWithSplitPage(sps);
		
		System.out.println();
		System.out.println("< STEP 9: Find section pages and make a visual tag page for them>");
		
		isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}
		
		if(!isTag)	
			sps = dealWithSectionPage(sps);
			
		//Now, delete all unorganized texts and pages without title
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_title().length() < 1)
			{
				if(sps.get(i).get_pageType() < 0)
				{
					sps.remove(i);
					i--;
					continue;
				}
				else if(sps.get(i).get_texts().get(0).get_hierarchy() == 1)
				{
					sps.get(i).set_title(sps.get(i).get_texts().get(0).get_text());
					sps.get(i).get_texts().remove(0);
				}
				else
				{
					sps.remove(i);
					i--;
					continue;
				}
			}
			for(int j = 0; j < sps.get(i).get_texts().size(); j++)
			{
				if(sps.get(i).get_texts().get(j).get_hierarchy() == 0)
				{
					sps.get(i).get_texts().remove(j);
					j--;
				}
			}
		}
		
		isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}
		
		
		if(!isTag)
			sps = combineContinuedSlides(sps, 0, sps.size()-1);
		else
		{
			int tagPos = 0;
			for(int i = 0; i < sps.size(); i++)
			{
				if(sps.get(i).get_pageType() == 2)
				{
					tagPos = i;
					break;
				}
			}
			sps = combineContinuedSlides(sps, 0, tagPos-1);
		}
		
		System.out.println();
		System.out.println("< STEP 10: Search index page >");
		System.out.println();
		
		if(!isTag)
			sps = findIndexPage(sps, 1, sps.size()-1);
		else
		{
			for(int i = 0; i < sps.size(); i++)
			{
				if(sps.get(i).get_pageType() == 2)
				{
					sps = findIndexPage(sps, 1, i-1);
					for(int j = 0; j < sps.get(i).get_texts().size(); j++)
					{
						textOutline currentTo = sps.get(i).get_texts().get(j);
						int beginPos = i, endPos = i;
						for(int k = i + 1; k < sps.size(); k++)
						{
							if(sps.get(k).get_PageNum() >= currentTo.get_child())
							{
								beginPos = k;
								break;
							}
						}
						
						for(int k = beginPos; k < sps.size(); k++)
						{							
							if(sps.get(k).get_PageNum() >= currentTo.get_childEnd())
							{
								endPos = k;
								break;
							}
						}
						sps = findIndexPage(sps, beginPos, endPos);
					}
				}
			}
		}
		
		System.out.println();
		System.out.println("< STEP 11: Conclude a visual index page for pages with similar titles >");
		System.out.println();
				
		int beginPos = 1;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() <= 0)
				continue;
			else if(sps.get(i).get_pageType() == 1)
			{
				int currentSlideNum = sps.get(i).get_PageNum();
				if(beginPos < i-1)
					sps = concludeTheme(sps, beginPos, i-1, 3);
				
				for(int j = i; j < sps.size(); j++)
				{
					if(sps.get(j).get_PageNum() >= currentSlideNum)
					{
						i = j;
						break;
					}					
				}
				currentSlideNum = sps.get(i).get_texts().get(sps.get(i).get_texts().size()-1).get_childEnd();
				for(int j = i; j < sps.size(); j++)
				{
					if(sps.get(j).get_PageNum() >= currentSlideNum)
					{
						i = j;
						beginPos = j + 1;
						
						break;
					}
				}
				
			}
			else if(sps.get(i).get_pageType() == 2)
			{
				sps = concludeTheme(sps, beginPos, i-1, 3);
				isTag = true;
				break;
			}
		}
		if(!isTag && beginPos < sps.size()-1)
			sps = concludeTheme(sps, beginPos, sps.size()-2, 3);
			

		
		//Add time info to each text
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() <= 0)
			{
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					if(to.get_time().before(sps.get(i).get_startTime()))
						to.set_time(sps.get(i).get_startTime());
				}
			}
			else if(sps.get(i).get_pageType() <= 2)
			{
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					for(int k = i+1; k < sps.size(); k++)
					{
						if(to.get_child() == sps.get(k).get_PageNum() && to.get_time().before(sps.get(i).get_startTime()))
							to.set_time(sps.get(k).get_startTime());
					}
				}
			}
		}
		
		
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 10 >");
		System.out.println();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime() + "  ");
			System.out.println(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text() + " -> ( " + to.get_child() + ", " + to.get_childEnd() + " ) " + to.get_time());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			System.out.println();
		}
		System.out.println("-------------------------------------------------------");
		
		//System.out.println("$$$$\tFinal Slides:\t" + sps.size());
		
		ArrayList<textOutline> finalResults = new ArrayList<textOutline>();
		finalResults = makeFinalTextOutlinesFromSlidePages(sps);
		
		this.set_isInitial(false);
		return finalResults;
	}
	
	public ArrayList<textOutline> generateOutlineWithPPTX(ArrayList<textLine> tll, String pptxName) throws IOException
	{
		System.out.println("< STEP 2: Delete logo which appears in same position of many pages for video stream>");
		ArrayList<textLine> tl2 = new ArrayList<textLine>();
		tll = removeLogo(tll, this._isInitial);
				
		/* Generate slidePage structure from textLine, separated by slideID */
		ArrayList<slidePage> sps = new ArrayList<slidePage>();
		int tempPageNum = 1;
		System.out.println("< STEP 3-1: Find title and load in hierarchy for video stream>");
		for(int i = 0; i < tll.size(); i++)
		{
			textLine t = tll.get(i);
			if(t.get_slideID() == tempPageNum)
				tl2.add(t);
			else
			{
				slidePage sp = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
							 : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
				sps.add(sp);
				System.out.println();
				tl2.clear();
				tempPageNum = t.get_slideID();
				tl2.add(t);
			}
		}
		slidePage spl = this._isInitial ? new slidePage(tl2, _pageWidth, _pageHeight) 
					  : new slidePage(tl2, _pageWidth, _pageHeight, _potentialTitleArea, _potentialHierarchicalGap, _beginWithLowCaseLetter, _haveSignBeforeSubtopic, this._lectureID);
		sps.add(spl);
		tl2.clear();
		tll.clear();
		
		//Analyze title position for further adaptive procedure
				int totalPage = sps.size();
				this._potentialTitleArea.clear();
				for(int i = 0; i < sps.size(); i++)
				{
					if(sps.get(i).get_PageNum() < 0)
					{
						sps.remove(i);
						totalPage--;
						i--;
						continue;
					}
					else if(sps.get(i).get_title().length() < 1)
						continue;
					else
					{
						if(sps.get(i).get_titleLocation().length != 4)
							continue;
						textLine t = new textLine();
						t.set_left(sps.get(i).get_titleLocation()[0]);
						t.set_top(sps.get(i).get_titleLocation()[2]);
						t.set_bottom(sps.get(i).get_titleLocation()[3]);
						t.set_height(sps.get(i).get_titleLocation()[3] - sps.get(i).get_titleLocation()[2]);
						t.set_width(sps.get(i).get_titleLocation()[1] - sps.get(i).get_titleLocation()[0]);
						
						boolean match = false;
						for(int j = 0; j < tl2.size(); j++)
						{
							if(t.isSameTitlePosition(tl2.get(j), _pageWidth, _pageHeight, false))
							{
								tl2.get(j).set_left((t.get_left() < tl2.get(j).get_left()) ? t.get_left() : tl2.get(j).get_left());
								tl2.get(j).set_top((t.get_top() < tl2.get(j).get_top()) ? t.get_top() : tl2.get(j).get_top());
								tl2.get(j).set_bottom((t.get_bottom() > tl2.get(j).get_bottom()) ? t.get_bottom() : tl2.get(j).get_bottom());
								tl2.get(j).set_height(tl2.get(j).get_bottom() - tl2.get(j).get_top());
								tl2.get(j).set_width((tl2.get(j).get_width() * tl2.get(j).get_count() + t.get_width())/(tl2.get(j).get_count()+1));
								tl2.get(j).set_count(tl2.get(j).get_count()+1);
								match = true;
								break;
							}
						}
						if(!match)
						{
							tl2.add(t);
							tl2.get(tl2.size()-1).set_count(1);
							tl2.get(tl2.size()-1).set_type(0);
						}
					}
				}
				for(int i = tl2.size()-1; i >= 0; i--)
				{
					int divider = totalPage;
					if(totalPage < 15)
						divider = 3;
					else if(totalPage < 25)
						divider = 4;
					else if(totalPage < 40)
						divider = 5;
					else if(totalPage < 60)
						divider = 6;
					else
						divider = 7;
					
					if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
						tl2.remove(i);
				}
				System.out.println();
				System.out.println("Frequently Used Title Area: [top, height, alignment: axis | count]");
				for(int i = 0; i < tl2.size(); i++)
				{
					textLine t = tl2.get(i);
					int[] pta = {t.get_top(), t.get_bottom()-t.get_top(), t.get_type(), t.get_left()};
					this._potentialTitleArea.add(pta);
					System.out.println("[" + pta[0] + ", " + pta[1] + ", " + (pta[2] == 1 ? "Center: " : "Left: ") + pta[3] + " | " + t.get_count() + "]");
				}
				tl2.clear();
				
				//Search for centered PTA
				totalPage = sps.size();
				for(int i = 0; i < sps.size(); i++)
				{
					if(sps.get(i).get_PageNum() < 0)
					{
						sps.remove(i);
						totalPage--;
						i--;
						continue;
					}
					else if(sps.get(i).get_title().length() < 1)
						continue;
					else
					{
						if(sps.get(i).get_titleLocation().length != 4)
							continue;
						textLine t = new textLine();
						t.set_left(sps.get(i).get_titleLocation()[0]);
						t.set_top(sps.get(i).get_titleLocation()[2]);
						t.set_bottom(sps.get(i).get_titleLocation()[3]);
						t.set_height(sps.get(i).get_titleLocation()[3] - sps.get(i).get_titleLocation()[2]);
						t.set_width(sps.get(i).get_titleLocation()[1] - sps.get(i).get_titleLocation()[0]);
						
						boolean match = false;
						for(int j = 0; j < tl2.size(); j++)
						{
							if(t.isSameTitlePosition(tl2.get(j), _pageWidth, _pageHeight, true))
							{
								tl2.get(j).set_left((t.get_left() < tl2.get(j).get_left()) ? t.get_left() : tl2.get(j).get_left());
								tl2.get(j).set_top((t.get_top() < tl2.get(j).get_top()) ? t.get_top() : tl2.get(j).get_top());
								tl2.get(j).set_bottom((t.get_bottom() > tl2.get(j).get_bottom()) ? t.get_bottom() : tl2.get(j).get_bottom());
								tl2.get(j).set_height(tl2.get(j).get_bottom() - tl2.get(j).get_top());
								tl2.get(j).set_width((t.get_width() > tl2.get(j).get_width()) ? t.get_width() : tl2.get(j).get_width());
								tl2.get(j).set_count(tl2.get(j).get_count()+1);
								match = true;
								break;
							}
						}
						if(!match)
						{
							tl2.add(t);
							tl2.get(tl2.size()-1).set_count(1);
							tl2.get(tl2.size()-1).set_type(1);
						}
					}
				}
				for(int i = tl2.size()-1; i >= 0; i--)
				{
					int divider = totalPage;
					if(totalPage < 15)
						divider = 3;
					else if(totalPage < 25)
						divider = 4;
					else if(totalPage < 40)
						divider = 5;
					else if(totalPage < 60)
						divider = 6;
					else
						divider = 7;
					
					if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
						tl2.remove(i);
				}
				for(int i = 0; i < tl2.size(); i++)
				{
					textLine t = tl2.get(i);
					int[] pta = {t.get_top(), t.get_bottom()-t.get_top(), t.get_type(), t.get_left() + t.get_width()/2};
					this._potentialTitleArea.add(pta);
					System.out.println("[" + pta[0] + ", " + pta[1] + ", " + (pta[2] == 1 ? "Center: " : "Left: ") + pta[3] + " | " + t.get_count() + "]");
				}
				tl2.clear();
				

				//Analyze hierarchical gap for adaptive procedure
				totalPage = 0;
				this._potentialHierarchicalGap.clear();
				for(int i = 0; i < sps.size(); i++)
				{
					if(sps.get(i).get_levelCoordinates().length != 3)
						continue;
					else if(sps.get(i).get_levelCoordinates()[0] < 0 || sps.get(i).get_levelCoordinates()[1] < 0)
						continue;
					else
					{
						totalPage++;
						int gap = sps.get(i).get_levelCoordinates()[1] - sps.get(i).get_levelCoordinates()[0];
						//Here we use only 2 attributes of textLine, slideID to mark the gap, and the count.
						textLine t = new textLine();
						t.set_slideID(gap);
						
						boolean match = false;
						for(int j = 0; j < tl2.size(); j++)
						{
							if(tl2.get(j).get_slideID() >= gap - 3 && tl2.get(j).get_slideID() <= gap + 3)
							{
								tl2.get(j).set_count(tl2.get(j).get_count() + 1);
								match = true;
								break;
							}
						}
						if(!match)
						{
							tl2.add(t);
							tl2.get(tl2.size()-1).set_count(1);
						}
					}
				}
				for(int i = tl2.size()-1; i >= 0; i--)
				{
					int divider = totalPage;
					if(totalPage < 25)
						divider = 4;
					else if(totalPage < 40)
						divider = 5;
					else if(totalPage < 60)
						divider = 6;
					else
						divider = 7;
					
					if(tl2.get(i).get_count() <= 3 || tl2.get(i).get_count() < totalPage / divider)
						tl2.remove(i);
				}
				System.out.println();
				for(int i = 0; i < tl2.size(); i++)
				{
					this._potentialHierarchicalGap.add(tl2.get(i).get_slideID());
					System.out.println("Potential Gap: " + tl2.get(i).get_slideID() + '\t' + tl2.get(i).get_count());
				}
				tl2.clear();
		
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 3 (video stream)>");
		System.out.println();
		//int count = 0;

		/*
		File newFile = new File("C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\ECTEL2015\\Evaluation\\" + this.get_lectureID() + "-Output.txt");
		if(newFile.exists())
		{
			newFile.delete();
		}
		*/
		
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			/*
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
				output.newLine();
				output.append("Page " + sp.get_PageNum());
				output.newLine();
				output.append("0" + '\t' + sp.get_title());
				output.newLine();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			/* show result */
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
				
				/*
				try {
					if(to.get_hierarchy() > 0)
					{
						BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
						output.append(Integer.toString(to.get_hierarchy()) + '\t' + to.get_text());
						output.newLine();
						output.close();
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
			
			//count += sp.get_texts().size();
			//if(sp.get_title().length() > 0)
				//count++;
		}
		System.out.println("-------------------------------------------------------");
		
		
		/* Generate slidePage structure from textLine, separated by slideID */
		ArrayList<slidePage> sps_pptx = new ArrayList<slidePage>();
		pptParser pp = new pptParser();		
		sps_pptx = pp.analyzePPTX(pptxName);
		

		System.out.println();
		System.out.println("< RESULT after step 3 (PPTX file)>");
		System.out.println();
		
		for(int i = 0; i < sps_pptx.size(); i++)
		{
			slidePage sp = sps_pptx.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			/*
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
				output.newLine();
				output.append("Page " + sp.get_PageNum());
				output.newLine();
				output.append("0" + '\t' + sp.get_title());
				output.newLine();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
				
				/*
				try {
					if(to.get_hierarchy() > 0)
					{
						BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
						output.append(Integer.toString(to.get_hierarchy()) + '\t' + to.get_text());
						output.newLine();
						output.close();
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
			
			//count += sp.get_texts().size();
			//if(sp.get_title().length() > 0)
				//count++;
		}
		System.out.println("-------------------------------------------------------");
		
		
		/*
		System.out.println("Original Slides:\t" + sps.size());
		System.out.println("Modified text-lines:\t" + count);
		System.out.println("$$$$");
		*/
		
		sps = synchronizeVideoToFile(sps, sps_pptx);
		//sps = synchronizeVideoAndFile(sps, sps_pptx);
		//sps_pptx.clear();
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 3.5: synchronization >");
		System.out.println();


		File newFile = new File("C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\_OCR_result\\" + this.get_lectureID() + "\\sync");
		if(newFile.exists())
		{
			newFile.delete();
		}/**/
		
		
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime().toString() + "  ");
			System.out.print(sp.get_title() + "  ");
			System.out.println("[" + sp.get_titleLocation()[0] + ", " + sp.get_titleLocation()[2] + ", " 
				+ (sp.get_titleLocation()[1]-sp.get_titleLocation()[0]) + ", " + (sp.get_titleLocation()[3]-sp.get_titleLocation()[2]) + "]");
			
			/**/
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
				output.append(sp.get_PageNum() + "\t" + sp.get_startTime().toString());
				output.newLine();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/* show result */
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
				
				/*
				try {
					if(to.get_hierarchy() > 0)
					{
						BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));										
						output.append(Integer.toString(to.get_hierarchy()) + '\t' + to.get_text());
						output.newLine();
						output.close();
					}					
				} catch (IOException e) {
					e.printStackTrace();
				}*/
				
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			if(sp.get_pageType() >= 0)
				System.out.println("3 Levels: " + sp.get_levelCoordinates()[0] + ", " + sp.get_levelCoordinates()[1] + ", " + sp.get_levelCoordinates()[2]);
			System.out.println();
			
			//count += sp.get_texts().size();
			//if(sp.get_title().length() > 0)
				//count++;
		}
		System.out.println("-------------------------------------------------------");
		
		
		
		
		ArrayList<ArrayList<Integer>> samePageGroups = new ArrayList<ArrayList<Integer>>();
		samePageGroups = findSamePages(sps);
				
		System.out.println();
		System.out.println("< STEP 4: Remove repeated and empty pages >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		
		
		sps = removeRepeatedPages(sps, samePageGroups);
		
		samePageGroups.clear();
		samePageGroups = findSamePages(sps);
		
		System.out.println();
		System.out.println("< STEP 5: Remove live show >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		sps = removeLiveShow(sps, samePageGroups);
		

		
		System.out.println();
		System.out.println("< STEP 6: Combine continuous slides >");
		
		sps = combineContinuedSlides(sps, 0, sps.size()-1);
						
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 6 >");
		System.out.println();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			System.out.println(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			System.out.println();
		} 
		System.out.println("-------------------------------------------------------");
		

		
		samePageGroups.clear();
		samePageGroups = findSamePages(sps);
		
		System.out.println();
		System.out.println("< STEP 7: Find tag page >");
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			System.out.print("Same Page Group: ( ");
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
				System.out.print(samePageGroups.get(i).get(j) + " ");
			System.out.println(")");
		}
		System.out.println();
		
		sps = dealWithTagPage(sps, samePageGroups);
		
		System.out.println();
		System.out.println("< STEP 8: Find split page and make them as visual tag page>");
		
		boolean isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}		
		
		if(!isTag)	
			sps = dealWithSplitPage(sps);
		
		System.out.println();
		System.out.println("< STEP 9: Find section pages and make a visual tag page for them>");
		
		isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}
		
		if(!isTag)	
			sps = dealWithSectionPage(sps);
			
		//Now, delete all unorganized texts and pages without title
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_title().length() < 1)
			{
				if(sps.get(i).get_pageType() < 0)
				{
					sps.remove(i);
					i--;
					continue;
				}
				else if(sps.get(i).get_texts().get(0).get_hierarchy() == 1)
				{
					sps.get(i).set_title(sps.get(i).get_texts().get(0).get_text());
					sps.get(i).get_texts().remove(0);
				}
				else
				{
					sps.remove(i);
					i--;
					continue;
				}
			}
			for(int j = 0; j < sps.get(i).get_texts().size(); j++)
			{
				if(sps.get(i).get_texts().get(j).get_hierarchy() == 0)
				{
					sps.get(i).get_texts().remove(j);
					j--;
				}
			}
		}
		
		isTag = false;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() == 2)
			{
				isTag = true;
				break;
			}
		}
		
		
		if(!isTag)
			sps = combineContinuedSlides(sps, 0, sps.size()-1);
		else
		{
			int tagPos = 0;
			for(int i = 0; i < sps.size(); i++)
			{
				if(sps.get(i).get_pageType() == 2)
				{
					tagPos = i;
					break;
				}
			}
			sps = combineContinuedSlides(sps, 0, tagPos-1);
		}
		
		System.out.println();
		System.out.println("< STEP 10: Search index page >");
		System.out.println();
		
		if(!isTag)
			sps = findIndexPage(sps, 1, sps.size()-1);
		else
		{
			for(int i = 0; i < sps.size(); i++)
			{
				if(sps.get(i).get_pageType() == 2)
				{
					sps = findIndexPage(sps, 1, i-1);
					for(int j = 0; j < sps.get(i).get_texts().size(); j++)
					{
						textOutline currentTo = sps.get(i).get_texts().get(j);
						int beginPos = i, endPos = i;
						for(int k = i + 1; k < sps.size(); k++)
						{
							if(sps.get(k).get_PageNum() >= currentTo.get_child())
							{
								beginPos = k;
								break;
							}
						}
						
						for(int k = beginPos; k < sps.size(); k++)
						{							
							if(sps.get(k).get_PageNum() >= currentTo.get_childEnd())
							{
								endPos = k;
								break;
							}
						}
						sps = findIndexPage(sps, beginPos, endPos);
					}
				}
			}
		}
		
		System.out.println();
		System.out.println("< STEP 11: Conclude a visual index page for pages with similar titles >");
		System.out.println();
				
		int beginPos = 1;
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() <= 0)
				continue;
			else if(sps.get(i).get_pageType() == 1)
			{
				int currentSlideNum = sps.get(i).get_PageNum();
				if(beginPos < i-1)
					sps = concludeTheme(sps, beginPos, i-1, 3);
				
				for(int j = i; j < sps.size(); j++)
				{
					if(sps.get(j).get_PageNum() >= currentSlideNum)
					{
						i = j;
						break;
					}					
				}
				currentSlideNum = sps.get(i).get_texts().get(sps.get(i).get_texts().size()-1).get_childEnd();
				for(int j = i; j < sps.size(); j++)
				{
					if(sps.get(j).get_PageNum() >= currentSlideNum)
					{
						i = j;
						beginPos = j + 1;
						
						break;
					}
				}
				
			}
			else if(sps.get(i).get_pageType() == 2)
			{
				sps = concludeTheme(sps, beginPos, i-1, 3);
				isTag = true;
				break;
			}
		}
		if(!isTag && beginPos < sps.size()-1)
			sps = concludeTheme(sps, beginPos, sps.size()-2, 3);
			

		
		//Add time info to each text
		for(int i = 0; i < sps.size(); i++)
		{
			if(sps.get(i).get_pageType() <= 0)
			{
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					if(to.get_time().before(sps.get(i).get_startTime()))
						to.set_time(sps.get(i).get_startTime());
				}
			}
			else if(sps.get(i).get_pageType() <= 2)
			{
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					for(int k = i+1; k < sps.size(); k++)
					{
						if(to.get_child() == sps.get(k).get_PageNum() && to.get_time().before(sps.get(i).get_startTime()))
							to.set_time(sps.get(k).get_startTime());
					}
				}
			}
		}
		
		
		/* show result */
		System.out.println();
		System.out.println("< RESULT after step 10 >");
		System.out.println();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			System.out.print(sp.get_startTime() + "  ");
			System.out.println(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text() + " -> ( " + to.get_child() + ", " + to.get_childEnd() + " ) " + to.get_time());
			}
			
			System.out.println("PageType: " + sp.get_pageType());
			System.out.println();
		}
		System.out.println("-------------------------------------------------------");
		
		//System.out.println("$$$$\tFinal Slides:\t" + sps.size());
		
		ArrayList<textOutline> finalResults = new ArrayList<textOutline>();
		finalResults = makeFinalTextOutlinesFromSlidePages(sps);
		
		this.set_isInitial(false);
		return finalResults;
	}
		
	public ArrayList<textLine> removeLogo(ArrayList<textLine> tll, boolean isInitial)
	{
		int totalPage = tll.get(tll.size()-1).get_slideID();
		ArrayList<textLine> tl2 = new ArrayList<textLine>();
		for(int i = 0; i < tll.size(); i++)
		{
			textLine t = tll.get(i);
			boolean match = false;
			for(int j = 0; j < tl2.size(); j++)
			{
				if(t.isSame(tl2.get(j), _pageWidth, _pageHeight))
				{
					tl2.get(j).set_count(tl2.get(j).get_count()+1);
					match = true;
					break;
				}
			}
			if(!match)
			{
				tl2.add(t);
				tl2.get(tl2.size()-1).set_count(1);
			}
		}
		
		for(int i = tl2.size()-1; i >= 0; i--)
		{
			int divider = totalPage;
			if(totalPage < 15)
				divider = 2;
			else if(totalPage < 25)
				divider = 3;
			else if(totalPage < 40)
				divider = 4;
			else if(totalPage < 60)
				divider = 5;
			else
				divider = 6;
			
			
			
			if(tl2.get(i).get_count() <= 2 || tl2.get(i).get_count() < totalPage / divider)
			{
				tl2.remove(i);
				continue;
			}
			
			if(!isInitial)
			{
				for(int j = 0; j < this.get_potentialTitleArea().size(); j++)
				{
					int[] pta = this.get_potentialTitleArea().get(j);
					if(pta.length != 4)
						continue;
					textLine t = new textLine();
					t.set_top(pta[0]);
					t.set_height(pta[1]);
					t.set_bottom(pta[0] + pta[1]);
					t.set_type(pta[2]);
					boolean centered = false;
					if(pta[2] == 0)
					{
						t.set_left(pta[3]);
						t.set_width(this._pageWidth / 3);
						centered = false;
					}
					else
					{
						t.set_left(pta[3]/2);
						t.set_width(pta[3]);
						centered = true;
					}
					if(tl2.get(i).isSameTitlePosition(t, this._pageWidth, this._pageHeight, centered))
					{
						tl2.remove(i);
						break;
					}
				}
			}
			else
			{
				double hp = this._pageHeight/768;
				//double wp = this._pageWidth/1024;
				if(tl2.get(i).get_bottom() < 256*hp)
				{
					int rightPart = tl2.get(i).get_left() + tl2.get(i).get_width() - this._pageWidth/2;
					int leftPart = this._pageWidth/2 - tl2.get(i).get_left();
					if(rightPart > 0 && leftPart > 0 && Math.abs(leftPart - rightPart) < tl2.get(i).get_width()/4)
						tl2.remove(i);
				}
			}			
		}
		
		for(int i = 0; i < tl2.size(); i++)
		{
			textLine t = tl2.get(i);
			System.out.print(t.get_count() + " " + t.get_text() + " ");
			System.out.print(t.get_top() + "  ");
			System.out.print(t.get_left() + "  ");
			System.out.print(t.get_width() + "  ");
			//System.out.print((t.get_width() + t.get_left()) + "  ");
			System.out.println(t.get_height()); 
		}
		System.out.println();
		System.out.println();
		
		for(int i = tll.size() - 1; i >= 0; i--)
		{
			for(int j = 0; j < tl2.size(); j++)
			{
				//Here 'isInitial' equals to 'isStrict' in value only...
				if(tl2.get(j).isSamePosition(tll.get(i), _pageWidth, _pageHeight, isInitial))
				{
					tll.remove(i);
					break;
				}
			}
		}
		
		tl2.clear();
		
		return tll;
	}

	public ArrayList<ArrayList<Integer>> findSamePages(ArrayList<slidePage> sps)
	{
		/* In this function, totally empty slides will be deleted and similar slides
		 * will be find out and saved in group, to be further dealt with.
		 * Finally bubble-reorder each of the similar slide groups.
		 */
		
		ArrayList<ArrayList<Integer>> samePageGroups = new ArrayList<ArrayList<Integer>>();
		
		for(int i = 0; i < sps.size(); i++)
		{
			// Remove empty slides
			slidePage sp = sps.get(i);
			if(sp.get_pageType() <= -3)
			{
				sps.remove(i);
				i--;
				continue;
			}
			
			/* Search for similar slides, when founded....
			 * If both slides have already been in a group, skip it.
			 * If only 1 slides exists in a group, add the 2nd slide in.
			 * If both slides haven't in a group, create a new group for them.
			 */
			for(int j = i + 1; j < sps.size(); j++)
			{
				slidePage sp2 = sps.get(j);
				if(sp2.get_pageType() <= -3)
					continue;
				if(sp.isSamePage(sp2))
				{
					boolean done = false;
					for(int k = 0; k < samePageGroups.size(); k++)
					{
						for(int l = 0; l < samePageGroups.get(k).size(); l++)
						{
							if(sp.get_PageNum() == samePageGroups.get(k).get(l))
							{
								for(int m = 0; m < samePageGroups.get(k).size(); m++)
								{
									if(sp2.get_PageNum() == samePageGroups.get(k).get(m))
									{
										done = true;
										break;
									}
								}
								if(!done)
								{
									samePageGroups.get(k).add(sp2.get_PageNum());
									done = true;
									break;
								}								
							}
						}
						if(done)
							break;
					}
					if(!done)
					{
						ArrayList<Integer> newGroup = new ArrayList<Integer>();
						newGroup.add(sp.get_PageNum());
						newGroup.add(sp2.get_PageNum());
						samePageGroups.add(newGroup);
					}
				}
			}
		}
		
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			for(int j = 0; j < samePageGroups.get(i).size(); j++)
			{
				for(int k = j + 1; k < samePageGroups.get(i).size(); k++)
				{
					int xj = samePageGroups.get(i).get(j);
					int xk = samePageGroups.get(i).get(k);
					
					if(xj > xk)
					{
						samePageGroups.get(i).set(j, xk);
						samePageGroups.get(i).set(k, xj);
					}
				}
			}
		}
		
		return samePageGroups;
		
	}

	public ArrayList<slidePage> removeRepeatedPages(ArrayList<slidePage> sps, ArrayList<ArrayList<Integer>> samePageGroups)
	{
		/* Remove those 'logically' repeated slides from the slides series
		 * such as : ABAB( to AB ) , ABCBABC( to ABC ) AABBB( to AB )
		 */
		
		System.out.println("Pages removed:");
		for(int i = 0; i < sps.size() - 1; i++)
		{
			if(isInSamePageGroup(sps.get(i).get_PageNum(), sps.get(i+1).get_PageNum(), samePageGroups))
			{
				System.out.print(sps.get(i+1).get_PageNum() + ", ");
				Time time = sps.get(i).get_startTime();
				sps.set(i, combineSameSlides(sps.get(i), sps.get(i+1), true));
				sps.remove(i+1);
				sps.get(i).set_startTime(time);
				i--;
				continue;
			}
			
			if( i > 1 && isInSamePageGroup(sps.get(i-1).get_PageNum(), sps.get(i+1).get_PageNum(), samePageGroups))
			{
				System.out.print(sps.get(i+1).get_PageNum() + ", ");
				Time time = sps.get(i-1).get_startTime();
				sps.set(i-1, combineSameSlides(sps.get(i-1), sps.get(i+1), true));
				sps.remove(i+1);
				sps.get(i-1).set_startTime(time);
				
				for(int j = i + 2; j < sps.size(); j++)
				{
					if(isInSamePageGroup(sps.get(i).get_PageNum(), sps.get(j).get_PageNum(), samePageGroups))
					{
						System.out.print(sps.get(i).get_PageNum() + ", ");
						time = sps.get(j).get_startTime();
						sps.set(j, combineSameSlides(sps.get(i), sps.get(j), false));
						sps.remove(i);
						sps.get(j).set_startTime(time);
						break;
					}
				}
						
				i = i - 2;
				continue;
			}
		}
		System.out.println();
		System.out.println();
		
		return sps;
	}
	
	public ArrayList<slidePage> removeLiveShow(ArrayList<slidePage> sps, ArrayList<ArrayList<Integer>> samePageGroups)
	{
		/* In this function, if a group of continuous pages existing between two same slides
		 * are mainly ill-organized pages or empty pages, they would be treated as LiveShow
		 * and deleted from the data structure.
		 * 
		 * Re-order the same page pairs first, treat the shorter pairs first.
		 */
		
		ArrayList<int[]> pairs = new ArrayList<int[]>();
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			for(int j = 0; j < samePageGroups.get(i).size() - 1; j++)
			{
				int pair[] = {-1, -1, 0};
				pair[0] = samePageGroups.get(i).get(j);
				pair[1] = samePageGroups.get(i).get(j+1);
				
				int beginPagePos = -1;
				int endPagePos = -1;
				for(int k = 0; k < sps.size(); k++)
				{
					if(sps.get(k).get_PageNum() == pair[0])
						beginPagePos = k;
					else if(sps.get(k).get_PageNum() == pair[1])
					{
						endPagePos = k;
						break;
					}
				}
				pair[2] = endPagePos - beginPagePos;
				pairs.add(pair);
			}
		}
		
		for(int i = 0; i < pairs.size(); i++)
		{
			for(int j = i+1; j < pairs.size(); j++)
			{
				if(pairs.get(j)[2] < pairs.get(i)[2])
				{
					int temp[] = {0, 0, 0};
					temp = pairs.get(i);
					pairs.set(i, pairs.get(j));
					pairs.set(j, temp);
				}
			}
		}
		
		for(int i = 0; i < pairs.size(); i++)
			System.out.println("Pair: <" + pairs.get(i)[0] + ", " + pairs.get(i)[1] + "> " + pairs.get(i)[2]);
		
		for(int i = 0; i < pairs.size(); i++)
		{
			int beginPageNum = pairs.get(i)[0];
			int endPageNum = pairs.get(i)[1];
			int beginPagePos = -1;
			int endPagePos = -1;
			for(int k = 0; k < sps.size(); k++)
			{
				if(sps.get(k).get_PageNum() == beginPageNum)
				{
					beginPagePos = k;
					break;
				}
			}
			if(beginPagePos < 0)
				continue;
			
			double ave = 0;
			int flashPageCount = 0;
			for(int k = beginPagePos + 1; k < sps.size(); k++)
			{
				if(sps.get(k).get_PageNum() == endPageNum)
				{
					if(sps.get(k).get_startTime().getTime() - sps.get(k-1).get_startTime().getTime() < 5001)
						flashPageCount ++;
					endPagePos = k;
					break;
				}
				
				if(sps.get(k).get_pageType() == -1)
				{
					int totalLength = 0;
					for(int x = 0; x < sps.get(k).get_texts().size(); x++)
						totalLength += sps.get(k).get_texts().get(x).get_text().length();
					if(sps.get(k).get_texts().size() >= 6 || totalLength >= 100)
					{
						ave += 0.5;
						if(totalLength >= 200)
							ave += 0.5;
					}
					else
						ave += 1;
				}
				else
					ave -= sps.get(k).get_pageType();
				
				if(sps.get(k+1).get_startTime().getTime() - sps.get(k).get_startTime().getTime() < 5001)
					flashPageCount ++;
			}
			if(endPagePos < 0)
				continue;
						
			if(endPagePos - beginPagePos == 1)
			{
				Time time = sps.get(beginPagePos).get_startTime();
				sps.set(beginPagePos, combineSameSlides(sps.get(beginPagePos), sps.get(endPagePos), true));
				sps.remove(endPagePos);
				sps.get(beginPagePos).set_startTime(time);
			}
			else
			{
				ave = ave / (double)(endPagePos - beginPagePos - 1);
				long avt = (sps.get(endPagePos).get_startTime().getTime() - sps.get(beginPagePos).get_startTime().getTime()) / (endPageNum - beginPageNum);
				boolean delete = ave >= 0.5 && (avt < 15001 || flashPageCount * 2 >= (endPagePos - beginPagePos - 1)) 
						&& sps.get(beginPagePos).get_pageType() == sps.get(endPagePos).get_pageType();
				System.out.println("Potential Live Show: " + "( " + beginPageNum + ", " + endPageNum + " ) AveType-"
						+ ave + " AveDuration-" + avt/1000  + " FlashPageCount" + flashPageCount  + " " + (delete ? "Yes" : "No"));
				if(delete)
				{
					Time time = sps.get(beginPagePos).get_startTime();
					sps.set(beginPagePos, combineSameSlides(sps.get(beginPagePos), sps.get(endPagePos), true));
					sps.remove(endPagePos);
					sps.get(beginPagePos).set_startTime(time);
					for(int k = endPagePos - 1; k > beginPagePos; k--)
						sps.remove(k);
				}
			}
		}
		
		return sps;
	}

	public ArrayList<slidePage> combineContinuedSlides(ArrayList<slidePage> sps, int beginPos, int endPos)
	{
		/* In this function, several continuous slides with a same topic will be gathered together
		 * as a single, large slide. And the title of the new slide will use the best recognized
		 * one from all the old slides included, and delete the potential number such as (1) or (2/3).
		 */
		
		for(int i = beginPos; i < endPos; i++)
		{
			boolean continuePage  = false;
			
			// Same title, no doubt will be combined.
			if(sps.get(i).get_title().contentEquals(sps.get(i+1).get_title()))
				continuePage = true;
			else
			{
				// And for similar title, do further tests.
				String titleA = sps.get(i).get_title();
				String titleB = sps.get(i+1).get_title();
				
				algorithmInterface ai = new algorithmInterface();
				int le = ai.getLevenshteinDistance(titleA, titleB);
				int longer = titleA.length() > titleB.length() ? titleA.length() : titleB.length();
				double lr = (double)le / (double)longer;
				double wr = ai.getSameWordsRatio(titleA, titleB);
				
				if(lr > 0.5 && wr < 0.5)
					continue;
				
				// If the difference of two whole titles is minus enough, done.
				if(lr <= 0.15 || le <= 2)
					continuePage = true;
				
				
				// Judging whether the last word in a title is a order number like (2/3) or <1>
				// If so, remove this number and compare the titles again, with a less strict standard.
				boolean withOrderA = false, withOrderB = false;
				String[] wordsA = titleA.split(" ");
				if(ai.isOrderNum(wordsA[wordsA.length - 1]))
				{
					titleA = wordsA[0];
					for(int j = 1; j < wordsA.length - 1; j++)
						titleA = titleA + " " + wordsA[j]; 
					withOrderA = true;
				}
				else if(ai.isOrderNum(wordsA[0]) && wordsA.length > 1)
				{
					titleA = wordsA[1];
					for(int j = 2; j < wordsA.length; j++)
						titleA = titleA + " " + wordsA[j];
					withOrderA = true;
				}
				else
				{
					if(wordsA.length > 2 && wordsA[wordsA.length - 1].length() + wordsA[wordsA.length - 2].length() < 7)
					{
						String temp = wordsA[wordsA.length - 2] + wordsA[wordsA.length - 1];
						if(ai.isOrderNum(temp))
						{
							titleA = wordsA[0];
							for(int j = 1; j < wordsA.length - 2; j++)
								titleA = titleA + " " + wordsA[j]; 
							withOrderA = true;
						}
					}
				}
				
				
				
				String[] wordsB = titleB.split(" ");
				if(ai.isOrderNum(wordsB[wordsB.length - 1]))
				{
					titleB = wordsB[0];
					for(int j = 1 ; j < wordsB.length - 1; j++)
						titleB = titleB + " " + wordsB[j]; 
					withOrderB = true;
				}
				else if(ai.isOrderNum(wordsB[0]) && wordsB.length > 1)
				{
					titleB = wordsB[1];
					for(int j = 2; j < wordsB.length; j++)
						titleB = titleB + " " + wordsB[j];
					withOrderB = true;
				}
				else
				{
					if(wordsB.length > 2 && wordsB[wordsB.length - 1].length() + wordsB[wordsB.length - 2].length() < 7)
					{
						String temp = wordsB[wordsB.length - 2] + wordsB[wordsB.length - 1];
						if(ai.isOrderNum(temp))
						{
							titleB = wordsB[0];
							for(int j = 1; j < wordsB.length - 2; j++)
								titleB = titleB + " " + wordsB[j]; 
							withOrderB = true;
						}
					}
				}
				
				le = ai.getLevenshteinDistance(titleA, titleB);
				longer = titleA.length() > titleB.length() ? titleA.length() : titleB.length();
				lr = (double)le / (double)longer;
				wr = ai.getSameWordsRatio(titleA, titleB);
				if(lr <= 0.15 || le <= 2)
					continuePage = true;
				else if( ((wr == 1 && lr <= 0.75) || ((wr > 0.6 && lr <= 0.25))) && (withOrderA || withOrderB))
					continuePage = true;
				
				
				// Change title to most recognized one and delete order number.
				if(continuePage && (withOrderA || withOrderB) )
				{
					String finalTitle =  titleA.length() > titleB.length() ? titleA : titleB;
					sps.get(i).set_title(finalTitle);
				}			
			}
			
			if(continuePage)
			{
				System.out.println("Combine page: ( " + sps.get(i).get_PageNum() + " <- " + sps.get(i+1).get_PageNum() + " )");
				sps.get(i).combineAtEnd(sps.get(i+1));
				sps.remove(i+1);
				endPos--;
				i--;
			}
				
		}
		
		return sps;
	}
	
	public boolean isInSamePageGroup(int a, int b, ArrayList<ArrayList<Integer>> samePageGroups)
	{
		int match = -1, order = -1;
		for(int i = 0; i < samePageGroups.size(); i++)
		{
			ArrayList<Integer> temp = samePageGroups.get(i);
			for(int j = 0; j < temp.size(); j++)
			{
				if(a == temp.get(j))
				{
					match = i;
					order = j;
				}
					
			}
		}
		
		if(match >= 0)
		{
			ArrayList<Integer> temp = samePageGroups.get(match);
			for(int j = order + 1; j < temp.size(); j++)
				if(b == temp.get(j))
					return true;
		}
		
		return false;
	}
	
	public slidePage combineSameSlides(slidePage a, slidePage b, boolean keepA)
	{
		/* In this function, one of the two similar slides will be deleted,
		 * but all the content will be kept in the remaining one.
		 * The better-organized or better-recognized one will take a more important
		 * role in the reconstruction.
		 */

		if(a.get_pageType() < -1)
		{
			if(keepA)
				b.set_PageNum(a.get_PageNum());
			return b;
		}
		else if(b.get_pageType() < -1)
		{
			if(!keepA)
				a.set_PageNum(b.get_PageNum());
			return a;
		}
		
		slidePage target = new slidePage();
		slidePage reference = new slidePage();
		boolean useA = true;
		
		/* one target, one reference, and the choice will follow rules below, in priority:
		 * 1. By pageType. (one well-organized with over 50% texts in the hierarchy, one not)
		 * 2. By title existence and the title initial is Block or not.
		 * 3. By ratio of well-organized texts.
		 * 4. By total length.
		 */
		if(a.get_pageType() != b.get_pageType())
		{
			if(a.get_pageType() > b.get_pageType())
				useA = true;
			else
				useA = false;
		}
		else if(!a.get_title().contentEquals(b.get_title()))
		{
			if(a.get_title().length() > 0 && b.get_title().length() > 0)
			{
				boolean taBlock = false;
				boolean tbBlock = false;
				if(a.get_title().charAt(0) < 97 || a.get_title().charAt(0) > 123)
					taBlock = true;
				else taBlock = false;
				if(b.get_title().charAt(0) < 97 || b.get_title().charAt(0) > 123)
					tbBlock = true;
				else tbBlock = false;
				
				if(taBlock ^ tbBlock)
				{
					if(taBlock)
						useA = true;
					else
						useA = false;
				}
				else
				{
					if(a.get_title().length() >= b.get_title().length())
						useA = true;
					else
						useA = false;
				}
			}
			else
			{
				if(a.get_title().length() == 0)
					useA = false;
				else
					useA = true;
			}
		}
		else
		{
			int count = 0;
			for(int i = 0; i < a.get_texts().size(); i++)
			{
				textOutline t = a.get_texts().get(i);
				if(t.get_hierarchy() > 0)
					count++;
			}
			double ra = (double)count / (double)a.get_texts().size();
			
			count = 0;
			for(int i = 0; i < b.get_texts().size(); i++)
			{
				textOutline t = b.get_texts().get(i);
				if(t.get_hierarchy() > 0)
					count++;
			}
			double rb = (double)count / (double)b.get_texts().size();
			
			if(ra > rb)
				useA = true;
			else if( rb > ra )
				useA = false;
			else
			{
				String allTexts1 = "";
				for(int i = 0; i < a.get_texts().size(); i++)
					allTexts1 = allTexts1 + " " + a.get_texts().get(i).get_text();
				
				String allTexts2 = "";
				for(int i = 0; i < b.get_texts().size(); i++)
					allTexts2 = allTexts2 + " " + b.get_texts().get(i).get_text();
				
				if(allTexts1.length() >= allTexts2.length())
					useA = true;
				else
					useA = false;
			}
		}
		
		if(useA)
		{
			target.contentCopy(a);
			reference.contentCopy(b);
		}
		else
		{
			target.contentCopy(b);
			reference.contentCopy(a);
		}
		
		// Add potential text from the Reference to the Target strict in order
		int targetMatch = -1;
		for(int i = 0; i < reference.get_texts().size(); i++)
		{
			// Find whether there is a textOutline matched from the Reference to the Target.
			int match = -1;
			textOutline ref = reference.get_texts().get(i);
			for(int j = targetMatch + 1; j < target.get_texts().size(); j++)
			{
				textOutline tar = target.get_texts().get(j);
				algorithmInterface ai = new algorithmInterface();
				int dis = ai.getLevenshteinDistance(ref.get_text(), tar.get_text());
				int longer = ref.get_text().length() > tar.get_text().length() ? ref.get_text().length() : tar.get_text().length();
				double wr = ai.getSameWordsRatio(ref.get_text(), tar.get_text());
				if(longer >= dis*2 || wr >= 0.9)
				{
					match = j;					
					break;
				}				
			}
			
			// If match, use the longer one as final result, else plug in the Reference text at the end of the last match point.
			if(match >= 0)
			{
				targetMatch = match;
				if(ref.get_text().length() > target.get_texts().get(match).get_text().length())
					target.get_texts().get(match).set_text(ref.get_text());
			}
			else
			{
				ArrayList<textOutline> temp = new ArrayList<textOutline>();
				for(int j = target.get_texts().size() - 1; j > targetMatch; j--)
				{
					temp.add(target.get_texts().get(j));
					target.get_texts().remove(j);
				}
				
				target.get_texts().add(ref);
				for(int j = temp.size() - 1; j >= 0; j--)
					target.get_texts().add(temp.get(j));
				
				targetMatch++;

			}
		}
		
		if(keepA)
			target.set_PageNum(a.get_PageNum());
		else
			target.set_PageNum(b.get_PageNum());
		
		target.isSlideWellOrganized();
		
		return target;
	}

	public ArrayList<slidePage> findIndexPage(ArrayList<slidePage> sps, int beginPosition, int endPosition)
	{
		/* In this function, Index Page (having a lot of texts as titles of following page)
		 * will be single out. Connection between the text and the title will be created
		 * by the parameter "child" and "childEnd" of the textOutline. And unmatched text in 
		 * the index page will be removed.
		 */
		System.out.println("Searching Index Page in << " + sps.get(beginPosition).get_PageNum() + " " + sps.get(endPosition).get_PageNum() + " >>");
		ArrayList<Integer> IndexPos = new ArrayList<Integer>();
		for(int i = beginPosition; i <= endPosition; i++)
		{
			if(sps.get(i).get_pageType() < 0)
				continue;
			if(sps.get(i).get_texts().size() < 3)
				continue;
			/* Only well-organized page can be signed as index page.
			 * First, go over through all the text in a page, try to find a matched title
			 * from a following page. Then count the 'matched' text ratio inside a page,
			 * if the ratio is over 50%, a Index Page appears.
			 */
			
			int matchCount = 0;
			int matchPoint = i + 1;
			// Using 'matchPoint' to control the order, a later text should not find a matched title before a previous text.
			for(int j = 0; j < sps.get(i).get_texts().size(); j++)
			{
				if(sps.get(i).get_texts().get(j).get_hierarchy() == 0)
					continue;
				
				String currentText = sps.get(i).get_texts().get(j).get_text();
				for(int k = matchPoint; k <= endPosition; k++)
				{
					String currentTitle = sps.get(k).get_title();
					algorithmInterface ai = new algorithmInterface();
					int le = ai.getLevenshteinDistance(currentText, currentTitle);
					int longer = currentText.length() > currentTitle.length() ? currentText.length() : currentTitle.length();
					double lr = (double)le / (double)longer;
					double wr = ai.getSameWordsRatio(currentText, currentTitle);
					if(lr <= 0.2 || (lr < 0.34 && wr >= 0.75) || currentText.toLowerCase().contains(currentTitle.toLowerCase()) 
							|| currentTitle.toLowerCase().contains(currentText.toLowerCase()))
					{						
						sps.get(i).get_texts().get(j).set_child(sps.get(k).get_PageNum());
						matchCount++;
						matchPoint = k + 1;						
						break;
					}
				}
			}
			// for those page only contains a few matched text, connection will be retained, but never used.
			if( matchCount * 2 >= sps.get(i).get_texts().size())
			{
				sps.get(i).set_pageType(1);
				IndexPos.add(i);
				i = matchPoint - 1;
			}
			
			
		}
		
		
		for(int i = 0; i < IndexPos.size(); i++)
		{
			/* In this part, for a index page, all the texts will find a matched title, empty ones will be removed.
			 * And from the first page after the index page, to the last page got matched, all them will be included
			 * inside the one [child, childEnd] area of a text in the index page.
			 * 
			 * Mainly, a text will be treated differently by whether it is a matched one, and then differed again by
			 * whether it is the last text, if not, differed again by whether the next text is matched. So, there will
			 * be 6 different treatment.
			 * 
			 * 'currentPos' is used to control the moving oder.
			 */
			int currentPos = IndexPos.get(i) + 1;
			for(int j = 0; j < sps.get(IndexPos.get(i)).get_texts().size(); j++)
			{				
				textOutline currentTo = sps.get(IndexPos.get(i)).get_texts().get(j);
				if(currentTo.get_child() < 0)
				{
					if(j == sps.get(IndexPos.get(i)).get_texts().size() - 1)
					{
						/* Condition 1: not matched and as the last text.
						 * Only when this text partly matched the next page's title in the matching oder,
						 * and that page located before a next index page and the last page of the presentation,
						 * it will match the slide, or else, it will be removed.
						 */
						
						if(currentPos > (i==IndexPos.size()-1 ? endPosition : IndexPos.get(i+1)-1))
						{
							sps.get(IndexPos.get(i)).get_texts().remove(j);
							j--;
						}
						else
						{
							String currentText = currentTo.get_text();
							String currentTitle = sps.get(currentPos).get_title();
							algorithmInterface ai = new algorithmInterface();
							int le = ai.getLevenshteinDistance(currentText, currentTitle);
							int longer = currentText.length() > currentTitle.length() ? currentText.length() : currentTitle.length();
							double lr = (double)le / (double)longer;
							double wr = ai.getSameWordsRatio(currentText, currentTitle);
							if(lr <= 0.5 || wr >= 0.5)
							{
								currentTo.set_child(sps.get(currentPos).get_PageNum());
								currentTo.set_childEnd(sps.get(currentPos).get_PageNum());
								currentPos++;
							}
							else
							{
								sps.get(IndexPos.get(i)).get_texts().remove(j);
								j--;
							}
						}
					}
					else if(sps.get(IndexPos.get(i)).get_texts().get(j+1).get_child() < 0)
					{
						/* Condition 2: Not matched, Not last text, and the next text is not matched either
						 * 
						 * In this case, first moving forward until find a matching text. 
						 * 
						 * If found, and there are some 'free' page between currentPos and next matched page, try to find whether 
						 * there is a page partly match the current text. If so, assign it, or else, delete it.
						 * 
						 * If not found, and currentPos is still before a next indexPage and the end of the presentation,
						 * try to partly match the page at 'currentPos', matched, assign it, or else delet it.
						 */
						boolean stillHaveMatch = false;
						int endPos = currentPos;
						for(int k = j + 2; k < sps.get(IndexPos.get(i)).get_texts().size(); j++)
						{
							if(sps.get(IndexPos.get(i)).get_texts().get(k).get_child() > 0)
							{
								stillHaveMatch = true;
								for(int l = currentPos; l <= endPosition; l++)
								{
									if(sps.get(l).get_PageNum() == sps.get(IndexPos.get(i)).get_texts().get(k).get_child())
									{
										endPos = l;
										break;
									}
								}
								break;
							}
						}
						
						if(stillHaveMatch)
						{
							if(currentPos >= endPos)
							{
								sps.get(IndexPos.get(i)).get_texts().remove(j);
								j--;
							}
							else
							{
								String currentText = currentTo.get_text();
								String currentTitle = sps.get(currentPos).get_title();
								algorithmInterface ai = new algorithmInterface();
								int le = ai.getLevenshteinDistance(currentText, currentTitle);
								int longer = currentText.length() > currentTitle.length() ? currentText.length() : currentTitle.length();
								double lr = (double)le / (double)longer;
								double wr = ai.getSameWordsRatio(currentText, currentTitle);
								if(lr <= 0.5 || wr >= 0.5)
								{
									currentTo.set_child(sps.get(currentPos).get_PageNum());
									currentTo.set_childEnd(sps.get(currentPos).get_PageNum());
									currentPos++;
								}
								else
								{
									sps.get(IndexPos.get(i)).get_texts().remove(j);
									j--;
								}
							}
						}
						else
						{
							if(currentPos > (i==IndexPos.size()-1 ? endPosition : IndexPos.get(i+1)-1))
							{
								sps.get(IndexPos.get(i)).get_texts().remove(j);
								j--;
							}
							else
							{
								String currentText = currentTo.get_text();
								String currentTitle = sps.get(currentPos).get_title();
								algorithmInterface ai = new algorithmInterface();
								int le = ai.getLevenshteinDistance(currentText, currentTitle);
								int longer = currentText.length() > currentTitle.length() ? currentText.length() : currentTitle.length();
								double lr = (double)le / (double)longer;
								double wr = ai.getSameWordsRatio(currentText, currentTitle);
								if(lr <= 0.5 || wr >= 0.5)
								{
									currentTo.set_child(sps.get(currentPos).get_PageNum());
									currentTo.set_childEnd(sps.get(currentPos).get_PageNum());
									currentPos++;
								}
								else
								{
									sps.get(IndexPos.get(i)).get_texts().remove(j);
									j--;
								}
							}
						}
					}
					else
					{
						/* Condition 3: Not matched, not last text, but next text is matched.
						 * In this case, assign all the pages between currentPos and the next matched page
						 * under current text.
						 */
						currentTo.set_child(sps.get(currentPos).get_PageNum());
						int last = currentTo.get_child();
						for(int k = currentPos; k <= (i==IndexPos.size()-1 ? endPosition : IndexPos.get(i+1)-1); k++)
						{
							if(sps.get(k).get_PageNum() >= sps.get(IndexPos.get(i)).get_texts().get(j+1).get_child())
							{
								currentPos = k;
								break;
							}
							else
								last = sps.get(k).get_PageNum();
						}						
						currentTo.set_childEnd(last);
					}
				}
				else
				{
					/* Condition 4: matched and last text -> just keep it.
					 * Condition 5: matched, not last text, and next text unmatched -> keep it also.
					 */
					if(j == sps.get(IndexPos.get(i)).get_texts().size() - 1)
					{
						currentTo.set_childEnd(currentTo.get_child());
						if(sps.get(currentPos).get_PageNum() < currentTo.get_child())
							currentTo.set_child(sps.get(currentPos).get_PageNum());
						for(int k = currentPos; k <= (i==IndexPos.size()-1 ? endPosition : IndexPos.get(i+1)-1); k++)
						{
							if(sps.get(k).get_PageNum() == currentTo.get_childEnd())
							{
								currentPos = k + 1;
								break;
							}
							else if(sps.get(k).get_PageNum() >= currentTo.get_childEnd())
							{
								currentPos = k;
								break;
							}
						}
					}
					else if(sps.get(IndexPos.get(i)).get_texts().get(j+1).get_child() < 0)
					{
						currentTo.set_childEnd(currentTo.get_child());
						if(sps.get(currentPos).get_PageNum() < currentTo.get_child())
							currentTo.set_child(sps.get(currentPos).get_PageNum());
						for(int k = currentPos; k <= (i==IndexPos.size()-1 ? endPosition : IndexPos.get(i+1)-1); k++)
						{
							if(sps.get(k).get_PageNum() == currentTo.get_childEnd())
							{
								currentPos = k + 1;
								break;
							}
							else if(sps.get(k).get_PageNum() >= currentTo.get_childEnd())
							{
								currentPos = k;
								break;
							}
						}
					}
					else
					{
						// Condition 6: matched and next text matched too -> assign all possible pages in the area
						int last = currentTo.get_child();
						if(sps.get(currentPos).get_PageNum() < currentTo.get_child())
							currentTo.set_child(sps.get(currentPos).get_PageNum());
						for(int k = currentPos; k <= (i==IndexPos.size()-1 ? endPosition : IndexPos.get(i+1)-1); k++)
						{
							if(sps.get(k).get_PageNum() >= sps.get(IndexPos.get(i)).get_texts().get(j+1).get_child())
							{
								currentPos = k;
								break;
							}
							else
								last = sps.get(k).get_PageNum();
						}
						currentTo.set_childEnd(last);
					}
				}
			}
		}
		
		for(int i = beginPosition; i <= endPosition; i++)
		{
			if(sps.get(i).get_pageType() == 1)
			{
				System.out.print("Index Page: " + sps.get(i).get_PageNum() + " < ");
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					textOutline to = sps.get(i).get_texts().get(j);
					if(to.get_child() > 0)
						System.out.print(to.get_child() + " ");
				}
				System.out.println(">");
				
			}
		}
		
		
		return sps;
	}

	public ArrayList<slidePage> dealWithTagPage(ArrayList<slidePage> sps, ArrayList<ArrayList<Integer>> samePageGroups)
	{
		/* TagPage is those appears many time indicating starting a new topic
		 * It will be recognized by special title, Agenda, Outline or something like them.
		 * Other same pages will be combined directly, and tagPage will be combined either.
		 * All the pages between 2 tagPages will be assigned to a single text in the tag page.
		 */
		
		String[] tags = {"Agenda", "Topics", "Topic", "Outline"};
		
		boolean haveTags = false;
		boolean havePartlyTags = false;
		for(int i = 0; i < tags.length && !haveTags; i++)
		{
			ArrayList<Integer> partlyTagsNum = new ArrayList<Integer>();
			for(int j = 0; j < sps.size(); j++)
			{
				if(sps.get(j).get_title().equalsIgnoreCase(tags[i]))
				{					
					if(partlyTagsNum.size() > 0)
					{
						//System.out.println("^^^^^^" + sps.get(j).get_title() + " " + sps.get(j).get_PageNum());
						if(!isInSamePageGroup(partlyTagsNum.get(0), sps.get(j).get_PageNum(), samePageGroups))
							partlyTagsNum.add(sps.get(j).get_PageNum());
						else
						{
							haveTags = true;
							break;
						}
					}
					else
						partlyTagsNum.add(sps.get(j).get_PageNum());
				}
			}
			if(partlyTagsNum.size() > 1)
			{
				samePageGroups.add(partlyTagsNum);
				haveTags = true;
				havePartlyTags = true;
				break;
			}
		}
				
		for(int i = 0; i < samePageGroups.size() && haveTags; i++)
		{
			int firstPos = -1;
			boolean isTag = false;
			ArrayList<Integer> tagPos = new ArrayList<Integer>();
			for(int j = 0; j < sps.size(); j++)
			{
				if(sps.get(j).get_PageNum() == samePageGroups.get(i).get(0))
				{
					firstPos = j;
					break;
				}
			}
			
			for(int j = 0; j < tags.length; j++)
			{
				if(sps.get(firstPos).get_title().equalsIgnoreCase(tags[j]))
				{
					isTag = true;
					tagPos.add(firstPos);
					break;
				}
			}
			
			for(int j = firstPos + 1; j < sps.size(); j++)
			{
				if(isInSamePageGroup(sps.get(firstPos).get_PageNum(), sps.get(j).get_PageNum(), samePageGroups))
				{					
					Time time = sps.get(firstPos).get_startTime();
					if(havePartlyTags)
					{
						sps.get(firstPos).combineAtEnd(sps.get(j));
						sps.get(firstPos).set_startTime(time);
					}
					else
					{
						sps.set(firstPos, combineSameSlides(sps.get(firstPos), sps.get(j), true));
						sps.get(firstPos).set_startTime(time);
					}
					if(!isTag)
					{
						sps.remove(j);
						j--;
					}
					else
						tagPos.add(j);
				}
			}
			
			// Above, finish the process of judging who's tag and who's not.
			// Using ArrayList 'tagPos' to save where those tagPages locate in the 'sps' serie.
			// And ArrayList 'matchTextOutlineNum' means an area is assigned to which textOutline in the tag page.
			
			if(isTag)
			{
				for(int j = 0; j < tagPos.size(); j++)
					System.out.println("tagPos: " + tagPos.get(j) + " tagNum: " + sps.get(tagPos.get(j)).get_PageNum());
				
				slidePage target = new slidePage();
				target.contentCopy(sps.get(firstPos));
				ArrayList<Integer> matchTextOutlineNum = new ArrayList<Integer>();

				boolean haveEnding = isHavingEndingPage(sps);
				boolean atLeastOneMatch = false;
				boolean enough = false;
				int lastMatchedTextOutline = -1;
				
				/* firstly, search all pages between each tagPages, try to find a matched page,
				 * if found, sign this area to the matched text in the tagPage,
				 * or else, just keep it empty now.
				 * 
				 * if the number of areas led by tag pages equals the number of hierarchical TextOutline
				 * in the tag page, skip this matching process
				 */
				
				int count = 0;
				/*
				for(int j = 0; j < target.get_texts().size(); j++)
				{
					if(target.get_texts().get(j).get_hierarchy() > 0)
						count++;
				}
				if(count == tagPos.size())
				{
					enough = true;
					for(int x = 0; x < tagPos.size(); x++)
						matchTextOutlineNum.add(-1);
						
					System.out.println("One subtitle for one area, perfect tag page situation!");
				}
				*/
				for(int j = 0; j < tagPos.size() && !enough; j++)
				{
					double matchRate = 0;
					int matchPos = tagPos.get(j);					
					String matchText = "";
					matchTextOutlineNum.add(j, -1);
										
					for(int k = tagPos.get(j) + 1; k < ( j == tagPos.size() - 1 ? sps.size() - (haveEnding ? 1 : 0) : tagPos.get(j+1)); k++)
					{
						String currentTitle = sps.get(k).get_title();
						algorithmInterface ai = new algorithmInterface();
						for(int l = (lastMatchedTextOutline >= 0 ? lastMatchedTextOutline + 1 : 0); l < target.get_texts().size(); l++)
						{
							if(target.get_texts().get(l).get_hierarchy() == 0)
								continue;
							String currentText = target.get_texts().get(l).get_text();
							int le = ai.getLevenshteinDistance(currentText, currentTitle);
							int longer = currentText.length() > currentTitle.length() ? currentText.length() : currentTitle.length();
							double lr = (double)le / (double)longer;
							double wr = ai.getSameWordsRatio(currentText, currentTitle);
							int wn = ai.getSameWordsNum(currentTitle, currentText);
							if(wr >= matchRate && lr <= 0.5 && wn > 1)
							{
								matchRate = wr;
								matchPos = k;
								matchText = currentText;
								matchTextOutlineNum.set(j, l);
								atLeastOneMatch = true;
								lastMatchedTextOutline = l;
							}
						}						
					}
					System.out.print("range ( " + sps.get(tagPos.get(j)).get_PageNum() + ", " + sps.get( (j == tagPos.size() - 1 ? sps.size()-(haveEnding ? 2 : 1) : tagPos.get(j+1)) ).get_PageNum() + " ) :");
					System.out.println(" Page " + sps.get(matchPos).get_PageNum() + "; Text: " + matchText + "; with match rate " + matchRate);
					
					/* When a matched area found, check the number of following unassigned areas, and the number of
					 * unassigned textOutlines in the tag page. If the numbers are exactly equal, stop the matching
					 * process, in order to avoid cross-match.
					 */
					
					if(matchTextOutlineNum.get(j) >= 0)
					{
						count = 0;
						for(int x = matchTextOutlineNum.get(j)+1; x < target.get_texts().size(); x++)
						{
							if(target.get_texts().get(x).get_hierarchy() > 0)
								count++;
						}
						System.out.println(count + " " + (tagPos.size()-j-1));
						if(count == tagPos.size()-j-1)
						{
							enough = true;
							for(int x = j+1; x < tagPos.size(); x++)
								matchTextOutlineNum.add(-1);
								
							System.out.println("TextOutlines left equals subtopic areas left, no need to proceed.");
						}
					}					
				}
				
				/* If there's absolutely no matched area, we will single out the longest in-hierarchy textOutlines to be
				 * assigned with the areas. This action will simplify the following procedure. 
				 */

				atLeastOneMatch = false;
				for(int x = 0; x < matchTextOutlineNum.size(); x++)
					if(matchTextOutlineNum.get(x) >= 0)
					{
						atLeastOneMatch = true;
						break;
					}

				if(!atLeastOneMatch)
				{
					count = 0;
					int countFirstLevel = 0;
					for(int j = 0; j < target.get_texts().size(); j++)
					{
						if(target.get_texts().get(j).get_hierarchy() > 0)
						{
							count++;
							if(target.get_texts().get(j).get_hierarchy() == 1)
								countFirstLevel++;
						}
					}
					
					if(count >= tagPos.size())
					{
						ArrayList<Integer> longest = new ArrayList<Integer>();
						int shortest = -1; 
						int shortestLength = 1000;
						for(int j = 0; j < target.get_texts().size(); j++)
						{
							textOutline to = target.get_texts().get(j);
							if(to.get_hierarchy() == 0)
								continue;
							if(to.get_hierarchy() != 1 && countFirstLevel >= tagPos.size())
								continue;
							
							if(longest.size() < tagPos.size())
							{
								longest.add(j);
								for(int k = 0; k < longest.size(); k++)
								{
									if(target.get_texts().get(k).get_text().length() < shortestLength)
									{
										shortestLength = target.get_texts().get(k).get_text().length();
										shortest = k;
									}
								}
							}
							else
							{
								if(to.get_text().length() < shortestLength)
									continue;
								else
								{
									longest.remove(shortest);
									longest.add(j);
									shortestLength = 1000;
									for(int k = 0; k < longest.size(); k++)
									{
										if(target.get_texts().get(k).get_text().length() < shortestLength)
										{
											shortestLength = target.get_texts().get(k).get_text().length();
											shortest = k;
										}
									}
								}
							}
						}
						
						for(int j = 0; j < matchTextOutlineNum.size(); j++)
							matchTextOutlineNum.set(j, longest.get(j));
					}
					else
					{
						for(int j = firstPos + 1; j < sps.size(); j++)
						{
							if(isInSamePageGroup(sps.get(firstPos).get_PageNum(), sps.get(j).get_PageNum(), samePageGroups))
							{
								sps.remove(j);
								j--;
							}
						}
						return sps;
					}
				}

				/* Here, try to complete the matching from tagPage and the related areas.
				 * 
				 * tagPos: where the tagPages locate in sps series.
				 * matchTextOutlineNum: the area following a related tagPage matches which text.
				 * these two lists contains exactly the same number of elements.
				 * 
				 * the completing process will go step by step, from one already matched area to another
				 * (the beginning and the ending will also be treated as a already matched text)
				 * this method will be controlled by 'startPoint' and 'endPoint', 
				 * startPoint initials as -1 and ends when it step in the last tagPage area,
				 * and endPoint always indicates the next 'pre-matched' text.
				 */
				
				int startPoint = -1;
				int endPoint = tagPos.size()-1;
				while(startPoint < tagPos.size() - 1)
				{
					endPoint = tagPos.size();
					for(int j = startPoint + 1; j < tagPos.size(); j++)
					{
						if(matchTextOutlineNum.get(j) >= 0)
						{
							endPoint = j;
							break;
						}
					}
					
					if(startPoint + 1 == endPoint)
					{
						/* When startPoint is next to endPoint, the area at endPoint can be sure
						 * to match the related text, just do it.
						 */
						if(endPoint == tagPos.size() - 1)
						{
							target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_child(sps.get(tagPos.get(endPoint)+1).get_PageNum());
							target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());							
						}
						else
						{
							target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_child(sps.get(tagPos.get(endPoint)+1).get_PageNum());
							target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_childEnd(sps.get(tagPos.get(endPoint+1)-1).get_PageNum());
						}
					}
					else
					{
						/* When there's distance between startPoint and endPoint, that means inside this step
						 * there must be some 'empty' area need to find a matching text. Mainly there are 3 conditions:
						 * Condition 1: startPoint is empty (-1) and endPoint is matched
						 * Condition 2: startPoint is matched and endPoint is empty (last page area) 
						 * Condition 3: both side is matched.
						 * No matter which condition it belongs to, the method to solve it is similar,
						 * 1. find whether there are enough free texts with the same hierarchy of the matched one for the areas
						 * 2. find whether there are enough free texts with same hierarchy for the areas
						 * 3. find whether there are enough free texts for the areas
						 * 4. conclude texts for extra areas.
						 * for 1~3, if so, assign them and jump out.
						 */						
						int hierarchy = 10;
						if(startPoint < 0 && endPoint == tagPos.size())
							break;
						else if(startPoint < 0)
						{
							//Condition 1
							hierarchy = target.get_texts().get(matchTextOutlineNum.get(endPoint)).get_hierarchy();
							
							int countSame = 0, countHigher = 0, countAll = matchTextOutlineNum.get(endPoint) + 1;
							
							for(int k = matchTextOutlineNum.get(endPoint); k >= 0; k--)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() == hierarchy)
									countSame++;
								else if(target.get_texts().get(k).get_hierarchy() < hierarchy)
									countHigher++;
							}

							if(countSame >= endPoint + 1)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(endPoint); k >= 0 && a <= endPoint; k--)
								{
									if(target.get_texts().get(k).get_hierarchy() == hierarchy)
									{
										if(endPoint - a == tagPos.size() - 1)
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										}
										else
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							}
							else if(countHigher >= endPoint + 1)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(endPoint); k >= 0 && a <= endPoint; k--)
								{
									if(target.get_texts().get(k).get_hierarchy() < hierarchy)
									{
										if(endPoint - a == tagPos.size() - 1)
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										}
										else
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							}
							else if(countAll >= endPoint + 1)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(endPoint); k >= 0 && a <= endPoint; k--)
								{
									if(endPoint - a == tagPos.size() - 1)
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
									}
									a++;
								}
							}
							else
							{								
								if(endPoint == tagPos.size() - 1)
								{
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_child(sps.get(tagPos.get(endPoint)+1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());									
								}
								else
								{
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_child(sps.get(tagPos.get(endPoint)+1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_childEnd(sps.get(tagPos.get(endPoint + 1)-1).get_PageNum());
								}
								
								int a = 1;
								for(int k = matchTextOutlineNum.get(endPoint) - 1; k >= 0 && a <= endPoint; k--)
								{
									target.get_texts().get(k).set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
									target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
									a++;
								}
								
								while(a <= endPoint)
								{
									textOutline to = new textOutline();
									to.set_text("One Sub Topic");
									to.set_hierarchy(1);
									to.set_child(sps.get(tagPos.get(endPoint - a) + 1).get_PageNum());
									to.set_childEnd(sps.get(tagPos.get(endPoint - a + 1) - 1).get_PageNum());
									a++;
									
									ArrayList<textOutline> temp = new ArrayList<textOutline>();
									for(int k = target.get_texts().size()-1; k >= 0; k--)
									{
										temp.add(target.get_texts().get(k));
										target.get_texts().remove(k);
									}
									target.get_texts().add(to);
									for(int k = temp.size() - 1; k >= 0; k--)
									{
										target.get_texts().add(temp.get(k));
									}
									for(int k = 0; k < matchTextOutlineNum.size(); k++)
									{
										if( matchTextOutlineNum.get(k) > 0 )
											matchTextOutlineNum.set(k, matchTextOutlineNum.get(k)+1);
									}
								}
							}
							
						}
						else if(endPoint == tagPos.size())
						{
							//Condition 2
							hierarchy = target.get_texts().get(matchTextOutlineNum.get(startPoint)).get_hierarchy();
							
							int countSame = 0;
							for(int k = matchTextOutlineNum.get(startPoint); k < target.get_texts().size() && target.get_texts().get(k).get_hierarchy() >= hierarchy; k++)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() == hierarchy)
									countSame++;								
							}
							
							int temp = matchTextOutlineNum.get(startPoint);
							for(int k = matchTextOutlineNum.get(startPoint); k >= 0; k--)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() < hierarchy)
								{
									temp = k;
									break;
								}
							}
							
							int countHigher = 0, countAll = target.get_texts().size() - matchTextOutlineNum.get(startPoint);
							for(int k = temp; k < target.get_texts().size(); k++)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() < hierarchy)
									countHigher++;	
							}
							
							if(countSame >= tagPos.size() - startPoint)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(startPoint); k < target.get_texts().size() && startPoint + a < tagPos.size(); k++)
								{
									if(target.get_texts().get(k).get_hierarchy() == hierarchy)
									{
										if(startPoint + a == tagPos.size() - 1)
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										}
										else
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							}
							else if(countHigher >= tagPos.size() - startPoint)
							{
								target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(-1);
								int a = 0;
								for(int k = temp; k < target.get_texts().size() && startPoint + a < tagPos.size(); k++)
								{
									if(target.get_texts().get(k).get_hierarchy() < hierarchy)
									{
										if(startPoint + a == tagPos.size() - 1)
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										}
										else
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							}
							else if(countAll >= tagPos.size() - startPoint)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(startPoint); k < target.get_texts().size() && startPoint + a < tagPos.size(); k++)
								{
									if(startPoint + a == tagPos.size() - 1)
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}
							}
							else
							{
								if(startPoint == tagPos.size() - 1)
								{
									target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(sps.get(tagPos.get(startPoint)+1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
								}
								else
								{
									target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(sps.get(tagPos.get(startPoint)+1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_childEnd(sps.get(tagPos.get(startPoint + 1)-1).get_PageNum());
								}
								
								int a = 1;
								int closestHierarchy = 1;
								for(int k = matchTextOutlineNum.get(startPoint) + 1; k < target.get_texts().size() && startPoint + a < tagPos.size(); k++)
								{
									closestHierarchy = target.get_texts().get(k).get_hierarchy();
									if(startPoint + a == tagPos.size() - 1)
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}
								
								while(startPoint + a < tagPos.size())
								{
									textOutline to = new textOutline();
									to.set_text("One Sub Topic");
									to.set_hierarchy(closestHierarchy);
									if(startPoint + a == tagPos.size() - 1)
									{
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
									target.get_texts().add(to);
								}
								
							}
							
							
						}
						else
						{
							//Condition 3
							int hs = target.get_texts().get(matchTextOutlineNum.get(startPoint)).get_hierarchy();
							int he = target.get_texts().get(matchTextOutlineNum.get(endPoint)).get_hierarchy();
							hierarchy = hs > he ? he : hs;
							
							int countSame = 0;
							for(int k = matchTextOutlineNum.get(startPoint); k <= matchTextOutlineNum.get(endPoint) && target.get_texts().get(k).get_hierarchy() >= hierarchy; k++)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() == hierarchy)
									countSame++;								
							}
							
							int temp = matchTextOutlineNum.get(startPoint);
							for(int k = matchTextOutlineNum.get(startPoint); k >= 0; k--)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() < hierarchy)
								{
									temp = k;
									break;
								}
							}
							
							int countHigher = 0, countAll = matchTextOutlineNum.get(endPoint) - matchTextOutlineNum.get(startPoint) + 1;
							for(int k = temp; k <= matchTextOutlineNum.get(endPoint); k++)
							{
								if(target.get_texts().get(k).get_hierarchy() == 0)
									continue;
								
								if(target.get_texts().get(k).get_hierarchy() < hierarchy)
									countHigher++;	
							}
							
							if(countSame >= endPoint - startPoint + 1)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(startPoint); k <= matchTextOutlineNum.get(endPoint) && startPoint + a <= endPoint; k++)
								{
									if(target.get_texts().get(k).get_hierarchy() == hierarchy)
									{
										if(startPoint + a == tagPos.size() - 1)
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										}
										else
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							}
							else if(countHigher >= endPoint - startPoint + 1)
							{
								target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(-1);
								int a = 0;
								for(int k = temp; k <= matchTextOutlineNum.get(endPoint) && startPoint + a <= endPoint; k++)
								{
									if(target.get_texts().get(k).get_hierarchy() < hierarchy)
									{
										if(startPoint + a == tagPos.size() - 1)
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
										}
										else
										{
											target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
											target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
										}
										a++;
									}
								}
							}
							else if(countAll >= endPoint - startPoint + 1)
							{
								int a = 0;
								for(int k = matchTextOutlineNum.get(startPoint); k <= matchTextOutlineNum.get(endPoint) && startPoint + a <= endPoint; k++)
								{
									if(startPoint + a == tagPos.size() - 1)
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}
							}
							else
							{
								target.get_texts().get(matchTextOutlineNum.get(startPoint)).set_child(sps.get(tagPos.get(startPoint)+1).get_PageNum());
								if(endPoint == tagPos.size() - 1)
								{
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_child(sps.get(tagPos.get(endPoint)+1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
								}
								else
								{
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_child(sps.get(tagPos.get(endPoint)+1).get_PageNum());
									target.get_texts().get(matchTextOutlineNum.get(endPoint)).set_childEnd(sps.get(tagPos.get(endPoint + 1)-1).get_PageNum());
								}
								
								int a = 1;
								int closestHierarchy = 1;
								for(int k = matchTextOutlineNum.get(startPoint) + 1; k < matchTextOutlineNum.get(endPoint) && startPoint + a < endPoint; k++)
								{
									closestHierarchy = target.get_texts().get(k).get_hierarchy();
									if(startPoint + a == tagPos.size() - 1)
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										target.get_texts().get(k).set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										target.get_texts().get(k).set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
								}
								
								while(startPoint + a < endPoint)
								{
									textOutline to = new textOutline();
									to.set_text("One Sub Topic");
									to.set_hierarchy(closestHierarchy);
									if(startPoint + a == tagPos.size() - 1)
									{
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(sps.size() - (haveEnding ? 2 : 1)).get_PageNum());
									}
									else
									{
										to.set_child(sps.get(tagPos.get(startPoint + a) + 1).get_PageNum());
										to.set_childEnd(sps.get(tagPos.get(startPoint + a + 1) - 1).get_PageNum());
									}
									a++;
									
									ArrayList<textOutline> storage = new ArrayList<textOutline>();
									for(int k = target.get_texts().size()-1; k >= matchTextOutlineNum.get(endPoint); k--)
									{
										storage.add(target.get_texts().get(k));
										target.get_texts().remove(k);
									}
									target.get_texts().add(to);
									for(int k = storage.size() - 1; k >= 0; k--)
									{
										target.get_texts().add(storage.get(k));
									}
									for(int k = endPoint; k < matchTextOutlineNum.size(); k++)
									{
										if( matchTextOutlineNum.get(k) > 0 )
											matchTextOutlineNum.set(k, matchTextOutlineNum.get(k)+1);
									}
								}
							}
						}
						
						
						
					}
					startPoint = endPoint;
				}
				
				target.set_pageType(2);
				
				// Now delete all unmatched texts
				
				for(int j = 0; j < target.get_texts().size(); j++)
				{
					if(target.get_texts().get(j).get_child() < 0)
					{
						target.get_texts().remove(j);
						j--;
					}
				}
				
				// Set time info for subtitles in Tag Page -> to corresponding tagPage which will be deleted in the structure.
				for(int j = 0; j < target.get_texts().size(); j++)
				{
					for(int k = tagPos.get(0); k < sps.size(); k++)
					{
						if(target.get_texts().get(j).get_child() == sps.get(k).get_PageNum())
						{
							target.get_texts().get(j).set_time(sps.get(k>0 ? k-1 : k).get_startTime());
							break;
						}
					}
				}
				
				// Adjust potential hierarchy chaos 
				int hierarchyDiff = target.get_texts().get(0).get_hierarchy() - 1;
				if(hierarchyDiff > 0)
				{
					for(int j = 0; j < target.get_texts().size(); j++)
					{
						int ch = target.get_texts().get(j).get_hierarchy();
						int nh = ch - hierarchyDiff < 1 ? 1 : ch - hierarchyDiff;
						target.get_texts().get(j).set_hierarchy(nh);
					}
				}
				
				sps.set(firstPos, target);
				
				for(int j = firstPos + 1; j < sps.size(); j++)
				{
					if(isInSamePageGroup(sps.get(firstPos).get_PageNum(), sps.get(j).get_PageNum(), samePageGroups))
					{
						sps.remove(j);
						j--;
					}
				}				
				
				//There could be only 1 group of tag pages, so eliminated other options.
				break;
			}			
		}
		return sps;
	}
	
	public ArrayList<slidePage> dealWithSplitPage(ArrayList<slidePage> sps)
	{
		ArrayList<Integer> splitPos = new ArrayList<Integer>();
		
		for(int i = 1; i < sps.size(); i++)
		{
			if(sps.get(i).get_title().length() == 0)
			{
				int count = 0;
				int pos[] = { -1, -1 };
				for(int j = 0; j < sps.get(i).get_texts().size(); j++)
				{
					if(sps.get(i).get_texts().get(j).get_hierarchy() > 0)
					{
						count++;
						if(pos[0] < 0)
							pos[0] = j;
						else
							pos[1] = j;
					}
				}
				if(count > 0 && count <= 2 && sps.get(i).get_texts().size() <= 2)
				{
					if(count == 1)
					{
						String[] words = sps.get(i).get_texts().get(pos[0]).get_text().split(" ");
						int wordsCount = 0;
						int wordsLength = 0;					
						for (String w : words) {
							wordsCount++;
							wordsLength += w.length();
						}					
						double averageLength = (double)wordsLength / (double)wordsCount;
						if(averageLength < 4)
							continue;
						
						sps.get(i).set_title(sps.get(i).get_texts().get(pos[0]).get_text());
						sps.get(i).get_texts().remove(pos[0]);
					}
					else
					{
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
						double averageLength = (double)wordsLength / (double)wordsCount;
						if(averageLength < 4)
							continue;
						
						sps.get(i).set_title(sps.get(i).get_texts().get(pos[0]).get_text() + " " + sps.get(i).get_texts().get(pos[1]).get_text());
						sps.get(i).get_texts().remove(pos[1]);
						sps.get(i).get_texts().remove(pos[0]);
					}
					
					sps.get(i).isSlideWellOrganized();
					if( i != sps.size() - 1)
					{
						if( splitPos.size() > 0 && splitPos.get(splitPos.size()-1) == i-1)
							splitPos.remove(splitPos.size()-1);
						splitPos.add(i);
						System.out.println("Potential Split Page: " + sps.get(i).get_PageNum() + " - " + sps.get(i).get_title());
					}					
				}
			}
		}
		
		if(splitPos.size() > 1)
		{
			slidePage visualTagPage = new slidePage();
			visualTagPage.contentCopy(sps.get(splitPos.get(0)));
			visualTagPage.set_title("Topics");
			visualTagPage.get_texts().clear();
			visualTagPage.set_pageType(2);
			
			for(int i = 0; i < splitPos.size(); i++)
			{
				System.out.println("Used Split Page: " + sps.get(splitPos.get(i)).get_PageNum() + " - " + sps.get(splitPos.get(i)).get_title());
				textOutline to = new textOutline();
				to.set_text(sps.get(splitPos.get(i)).get_title());
				to.set_hierarchy(1);
				to.set_child(sps.get(splitPos.get(i)+1).get_PageNum());
				
				boolean haveEnding = isHavingEndingPage(sps);
				if( i == splitPos.size() - 1)
					to.set_childEnd(sps.get(sps.size()- (haveEnding ? 2 : 1) ).get_PageNum());
				else
					to.set_childEnd(sps.get(splitPos.get(i+1)-1).get_PageNum());
				visualTagPage.get_texts().add(to);
			}
			
			for(int i = 0; i < visualTagPage.get_texts().size(); i++)
			{
				for(int j = splitPos.get(0); j < sps.size(); j++)
				{
					if(visualTagPage.get_texts().get(i).get_child() == sps.get(j).get_PageNum())
					{
						visualTagPage.get_texts().get(i).set_time(sps.get( j>0 ? j-1 : j ).get_startTime());
						break;
					}
				}
			}
			
			/*			 
			System.out.print(visualTagPage.get_PageNum() + "  ");
			System.out.println(visualTagPage.get_title());			
			for(int j = 0; j < visualTagPage.get_texts().size(); j++)
			{
				textOutline to = visualTagPage.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text() + " -> ( " + to.get_child() + ", " + to.get_childEnd() + " )");
			}			
			System.out.println("PageType: " + visualTagPage.get_pageType());
			System.out.println();
			*/
			
			for(int i = splitPos.size()-1; i >= 0; i-- )
			{
				if(i == 0)
					sps.set(splitPos.get(0), visualTagPage);
				else
				{
					int temp = splitPos.get(i);
					sps.remove(temp);
				}
			}
		}
		
		return sps;
	}
	
	private boolean isSplitPage(slidePage sp)
	{
		if(sp.get_title().length() == 0)
		{
			int count = 0;
			int pos[] = { -1, -1 };
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				if(sp.get_texts().get(j).get_hierarchy() > 0)
				{
					count++;
					if(pos[0] < 0)
						pos[0] = j;
					else
						pos[1] = j;
				}
			}
			if(count > 0 && count <= 2 && sp.get_texts().size() <= 2)
			{
				if(count == 1)
				{
					String[] words = sp.get_texts().get(pos[0]).get_text().split(" ");
					int wordsCount = 0;
					int wordsLength = 0;					
					for (String w : words) {
						wordsCount++;
						wordsLength += w.length();
					}					
					
					double averageLength = (double)wordsLength / (double)wordsCount;
					if(averageLength < 4)
						return false;
					else
						return true;

				}
				else
				{
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
					
					double averageLength = (double)wordsLength / (double)wordsCount;
					if(averageLength < 4)
						return false;
					else
						return true;
				}
			}
		}

		return false;
	}
	
	public ArrayList<slidePage> dealWithSectionPage(ArrayList<slidePage> sps)
	{
		String[] tags = {"Part", "Topic", "Theme", "Chapter", "Section"};
		
		ArrayList<Integer> sectionPos = new ArrayList<Integer>();
		for(int i = 1; i < sps.size(); i++)
		{
			String[] words = sps.get(i).get_title().split(" ");
			if(words.length < 2)
				continue;
			
			for(int j = 0; j < tags.length; j++)
			{
				if(words[0].equalsIgnoreCase(tags[j]))
				{
					algorithmInterface ai = new algorithmInterface();
					if(ai.isNum(words[1]) || ai.isNum(words[1].substring(0, words[1].length()-1)))
					{
						sectionPos.add(i);
						continue;
					}
				}					
			}			
		}
		
		if(sectionPos.size() > 1)
		{
			slidePage visualTagPage = new slidePage();
			visualTagPage.contentCopy(sps.get(sectionPos.get(0)));
			visualTagPage.set_title("Topics");
			visualTagPage.set_PageNum(visualTagPage.get_PageNum()-1);
			visualTagPage.get_texts().clear();
			visualTagPage.set_pageType(2);
			
			for(int i = 0; i < sectionPos.size(); i++)
			{
				System.out.println("Used Section Page: " + sps.get(sectionPos.get(i)).get_PageNum() + " - " + sps.get(sectionPos.get(i)).get_title());
				textOutline to = new textOutline();
				to.set_text(sps.get(sectionPos.get(i)).get_title());
				to.set_hierarchy(1);
				to.set_child(sps.get(sectionPos.get(i)).get_PageNum());
				to.set_time(sps.get(sectionPos.get(i)).get_startTime());
				
				boolean haveEnding = isHavingEndingPage(sps);
				if( i == sectionPos.size() - 1)
					to.set_childEnd(sps.get(sps.size()- (haveEnding ? 2 : 1) ).get_PageNum());
				else
					to.set_childEnd(sps.get(sectionPos.get(i+1)-1).get_PageNum());
				
				String[] words = sps.get(sectionPos.get(i)).get_title().split(" ");
				if(words.length > 2)
				{
					String newTitle = words[2];
					for(int j = 3; j < words.length; j++)
						newTitle = newTitle + " " + words[j];
					sps.get(sectionPos.get(i)).set_title(newTitle);
				}
				else
				{
					if(sps.get(sectionPos.get(i)).get_texts().size() > 0)
						sps.get(sectionPos.get(i)).set_title(sps.get(sectionPos.get(i)).get_texts().get(0).get_text());
					else
						sps.get(sectionPos.get(i)).set_title("<subtopic overview>");
					to.set_text(to.get_text() + " " + sps.get(sectionPos.get(i)).get_title());
				}
				
				visualTagPage.get_texts().add(to);				
			}
			
			/*			 
			System.out.print(visualTagPage.get_PageNum() + "  ");
			System.out.println(visualTagPage.get_title());			
			for(int j = 0; j < visualTagPage.get_texts().size(); j++)
			{
				textOutline to = visualTagPage.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				System.out.println(to.get_text() + " -> ( " + to.get_child() + ", " + to.get_childEnd() + " )");
			}			
			System.out.println("PageType: " + visualTagPage.get_pageType());
			System.out.println();
			*/
			
			ArrayList<slidePage> tempStack = new ArrayList<slidePage>();
			for(int i = sps.size()-1; i >= sectionPos.get(0); i--)
			{
				tempStack.add(sps.get(i));
				sps.remove(i);
			}
			sps.add(visualTagPage);
			for(int i = tempStack.size()-1; i >= 0; i--)
				sps.add(tempStack.get(i));
			tempStack.clear();
			
		}
		
		return sps;
	}
	
	public boolean isHavingEndingPage(ArrayList<slidePage> sps)
	{
		slidePage lsp = sps.get(sps.size()-1);
		String title = lsp.get_title();
		if(title.length() == 0)
			title = lsp.get_texts().get(0).get_text();
		
		String head = "";
		if(title.length() >= 5)
			head = (String) title.subSequence(0, 5);
		//System.out.println(head);
		String[] tags = {"Thank", "Summa", "Concl", "Refer", "Quest"};
		for(int i = 0; i < tags.length; i++)
		{
			if(head.equalsIgnoreCase(tags[i]))
				return true;
		}
		
		String titleB = sps.get(0).get_title();
		algorithmInterface ai = new algorithmInterface();
		int le = ai.getLevenshteinDistance(title, titleB);
		int longer = title.length() > titleB.length() ? title.length() : titleB.length();
		double lr = (double)le / (double)longer;
		double wr = ai.getSameWordsRatio(title, titleB);
		if(lr < 0.5 || wr >= 0.6)
			return true;
		
		return false;
	}
	
	public ArrayList<slidePage> concludeTheme(ArrayList<slidePage> sps, int beginPos, int endPos, int limit)
	{
		/* This function is used to create a virtual Index Page for a group of slides sharing some keywords in their titles.
		 * The whole process will be marching recursively. For each section, the number of the words in slides will be counted.
		 * When a group of titles sharing some words above a certain ratio, done.
		 * 
		 * The most important parameters in this function are 4 'Pos' to mark 
		 * the searching region (beginPos, endPos) 
		 * and the searched region (firstPos, currentPos)
		 * Generally, beginPos <= firstPos < currentPos <= endPos.
		 */
		ArrayList<textLine> wordsCount = new ArrayList<textLine>();
		boolean found = false;
		int currentPos = beginPos;
		for(int i = beginPos; i <= endPos; i++)
		{
			// Generally, this loop will not hit the endPos, 
			// and the currentPos will only be used to record where the 'shared' word last locates.
			String title = sps.get(i).get_title();
			if(title.length() == 0)
				continue;
			String[] words = title.split(" ");
			algorithmInterface ai = new algorithmInterface();
			
			// Avoid counting twice for the same word in a single title
			for(int j = 0; j < words.length; j++)
				for(int k = j + 1; k < words.length; k++)
					if(ai.isLogicalSameWords(words[j], words[k]))
						words[k] = "aa";
			
			for (String w : words)
			{
				// Abandon short meaningless words (is, the, etc)
				if(w.length() <= 3)
				{
					char a = w.charAt(0);
					if(( a >= 'a' && a <= 'z') || ai.isSignal(a))
						continue;
				}
				
				/* Go through all the words in the current title, if it has already saved, count++
				 * or else, make this word as a new candidate.
				 * Attention: slideID here is used to save in which slide the certain word appears.
				 *            and 'left' here to represent in which slide this word appeared for 1st time.
				 */
				boolean match = false;
				for(int j = 0; j < wordsCount.size(); j++)
				{
					if(ai.isLogicalSameWords(w, wordsCount.get(j).get_text()))
					{
						wordsCount.get(j).set_count(wordsCount.get(j).get_count() + 1);
						wordsCount.get(j).set_slideID(i);
						match = true;
						break;
					}
				}
				if(!match)
				{
					textLine t = new textLine();
					t.set_text(w);
					t.set_slideID(i);
					t.set_left(i);
					wordsCount.add(t);
					
				}				
			}
			
			/* After go through the words in the current title, examine the 'word' storage,
			 * if the total number of the appearance of a word cannot reach the half of
			 * the number of slides counted, delete it.
			 * 
			 * And if it need to delete a multi-appearance word, end this process,
			 * find where this word appears last, and create a index page for the pages
			 * between beginPos and the last-appearing-point. (Situation 1)
			 */
			boolean jump = false;
			for(int j = 0; j < wordsCount.size(); j++)
			{
				int pageSum = i - beginPos + 1;
				if(wordsCount.get(j).get_count() * 2 < pageSum)
				{
					if(wordsCount.get(j).get_count() >= limit)
					{
						found = true;
						jump = true;
						currentPos = wordsCount.get(j).get_slideID();
						break;
					}
					wordsCount.remove(j);
					j--;
				}
			}

			if(jump)
				break;
			
			/* If all the words have been removed, means there's no shared word available,
			 * quit either, enter situation 2. commonly in this situation, there's 2 slides.
			 */
			if(wordsCount.size() == 0)
			{				
				currentPos = i;
				break;
			}
		}
		
		if(found)
		{
			/* Situation 1:
			 * In this situation, we know exactly where the begin and the end are,
			 * so just create a virtual index page before the beginning point, with
			 * all the slide titles as its sub texts.
			 * Attention: the pageNum of the new slide will be 1 less than the pageNum
			 * of the beginning point. It is possible that the new slide share a same
			 * pageNum with its previous slide.
			 */
			System.out.println("< " + sps.get(beginPos).get_PageNum() + ", " + sps.get(currentPos).get_PageNum() + " > : found");
			for(int i = 0; i < wordsCount.size(); i++)
			{
				if(wordsCount.get(i).get_count() < limit)
					continue;
				textLine t = wordsCount.get(i);
				System.out.println(t.get_text() + "\t" + t.get_count() + "\t" + sps.get(t.get_slideID()).get_PageNum());
			}
			
			int firstPos = currentPos;
			String newTitle = "";
			for(int i = 0; i < wordsCount.size(); i++)
			{
				if(wordsCount.get(i).get_count() < limit)
					continue;				
				
				if(wordsCount.get(i).get_left() < firstPos)
					firstPos = wordsCount.get(i).get_left();
				newTitle += wordsCount.get(i).get_text();
				if( i < wordsCount.size() - 1)
					newTitle += " ";
			}
			
			slidePage nsp = new slidePage();
			nsp.set_title(newTitle);
			nsp.set_pageType(1);
			nsp.set_PageNum(sps.get(firstPos).get_PageNum() - 1);
			nsp.set_startTime(sps.get(firstPos).get_startTime());
			
			for(int i = firstPos; i <= currentPos; i++)
			{
				slidePage sp = sps.get(i);
				textOutline t = new textOutline(sp.get_title(), 1);
				t.set_child(sp.get_PageNum());
				t.set_childEnd(sp.get_PageNum());
				nsp.get_texts().add(t);
			}
			
			ArrayList<slidePage> storage = new ArrayList<slidePage>();
			for(int i = sps.size()-1; i >= firstPos; i--)
			{
				storage.add(sps.get(i));
				sps.remove(i);
			}
			sps.add(nsp);
			for(int i = storage.size()-1; i >= 0; i--)
				sps.add(storage.get(i));
			storage.clear();
			currentPos++;
			endPos++;
			
			// Recursively continue the process, if possible
			if(currentPos < endPos - 1)
				sps = concludeTheme(sps, currentPos + 1, endPos, limit);
		}
		else
		{
			if(wordsCount.size() > 0)
			{
				/* Situation 2:
				 * Meet the end of all the slides, and with some potential "shared words" left.
				 * First deleting all the words with their count less than the standard ( 3 or 2 ),
				 * if there's still some words left, using currenPos to find where they appears last
				 * and create a index page for them, then continue the process.
				 * If there's no word left, just end the process.
				 */
				int max = 0;
				int firstPos = endPos;
				for(int i = 0; i < wordsCount.size(); i++)
				{
					if(wordsCount.get(i).get_count() < limit)
					{
						wordsCount.remove(i);
						i--;
					}
					else
					{
						if(max < wordsCount.get(i).get_count())
							currentPos = wordsCount.get(i).get_slideID();
						if(wordsCount.get(i).get_left() < firstPos)
							firstPos = wordsCount.get(i).get_left();
					}
				}
				
				if(wordsCount.size() == 0)
					return sps;
				
				System.out.println("< " + sps.get(beginPos).get_PageNum() + ", " + sps.get(currentPos).get_PageNum() + " > : meet end");
				for(int i = 0; i < wordsCount.size(); i++)
				{
					textLine t = wordsCount.get(i);
					System.out.println(t.get_text() + "\t" + t.get_count() + "\t" + sps.get(t.get_slideID()).get_PageNum());
				}
				
				String newTitle = "";
				for(int i = 0; i < wordsCount.size(); i++)
				{
					if(wordsCount.get(i).get_count() < limit)
						continue;				
					newTitle += wordsCount.get(i).get_text();
					if( i < wordsCount.size() - 1)
						newTitle += " ";
				}
				
				slidePage nsp = new slidePage();
				nsp.set_title(newTitle);
				nsp.set_pageType(1);
				nsp.set_PageNum(sps.get(firstPos).get_PageNum() - 1);
				nsp.set_startTime(sps.get(firstPos).get_startTime());
				
				for(int i = firstPos; i <= currentPos; i++)
				{
					slidePage sp = sps.get(i);
					textOutline t = new textOutline(sp.get_title(), 1);
					t.set_child(sp.get_PageNum());
					t.set_childEnd(sp.get_PageNum());
					nsp.get_texts().add(t);
				}
				
				ArrayList<slidePage> storage = new ArrayList<slidePage>();
				for(int i = sps.size()-1; i >= firstPos; i--)
				{
					storage.add(sps.get(i));
					sps.remove(i);
				}
				sps.add(nsp);
				for(int i = storage.size()-1; i >= 0; i--)
					sps.add(storage.get(i));
				storage.clear();
				currentPos++;
				endPos++;
				
				if(currentPos < endPos)
					sps = concludeTheme(sps, currentPos+1, endPos, limit);
			}
			else
			{
				//Situation 3: no shared word found, continue the process...
				System.out.println("< " + sps.get(beginPos).get_PageNum() + ", " + sps.get(currentPos).get_PageNum() + " > : no result");
				if(currentPos < endPos)
					sps = concludeTheme(sps, beginPos + 1, endPos, limit);
			}
		}
		
		return sps;
	}
	
	public ArrayList<textOutline> makeFinalTextOutlinesFromSlidePages(ArrayList<slidePage> sps)
	{
		/* In this function, all texts from all slide will be reorganized together as the output.
		 * 
		 * there's several different possibilities of the slide:
		 * 1. Common page at root. -- hierarchy 0
		 * 2. Index page at root. -- hierarchy 0
		 * 3. Common page under a root Index page occupying a sub text. -- hierarchy 1
		 * 4. Common page under a root Index page sharing a sub text. -- hierarchy 2, that sub text will be hierarchy 1
		 * 5. Tag page (can only be at root) -- no hierarchy, but its all sub text will be hierarchy 0
		 * 6. Common page under Tag Page. -- hierarchy 1
		 * 7. Index page under Tag page. -- hierarchy 1
		 * 8. common page under a under-tag Index page occupying a sub text. -- hierarchy 2
		 * 9. common page under a under-tag Index page sharing a sub text. -- hierarchy 3, that sub text will be hierarchy 2
		 */
		ArrayList<textOutline> finalResults = new ArrayList<textOutline>();
		
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			
			//Page situation 1
			if(sp.get_pageType() < 1)
			{
				ArrayList<textOutline> temp = new ArrayList<textOutline>();
				temp = makeTextOutlinesFromOneSlidePage(sp, 0);
				finalResults.addAll(temp);
				System.out.println("Page " + sp.get_PageNum() + " done! It is a root-common page.");
				continue;
			}
			
			if(sp.get_pageType() == 1)
			{
				//Page situation 2
				textOutline toTitle = new textOutline(sp.get_title(), 0, 0);
				toTitle.set_time(sp.get_startTime());
				finalResults.add(toTitle);
				System.out.println("Page " + sp.get_PageNum() + "-0 done! It is the root-index page title.");
				int currentPos = i + 1;
				for(int j = 0; j < sp.get_texts().size(); j++)
				{
					int beginPageNum = sp.get_texts().get(j).get_child();
					int endPageNum = sp.get_texts().get(j).get_childEnd();
					
					if(beginPageNum < endPageNum)
					{
						//Page situation 4 special
						textOutline newTo = new textOutline(sp.get_texts().get(j).get_text(), 1, 0);
						newTo.set_time(sp.get_texts().get(j).get_time());
						finalResults.add(newTo);
						System.out.println("Page " + sp.get_PageNum() + "-" + (j+1) + " done! It is the root-index page subtitle");
						
						//Page situation 4
						for(int k = currentPos; k < sps.size(); k++)
						{
							if(sps.get(k).get_PageNum() < beginPageNum)
								continue;
							else if(sps.get(k).get_PageNum() > endPageNum)
								break;
							else
							{
								ArrayList<textOutline> temp = new ArrayList<textOutline>();
								temp = makeTextOutlinesFromOneSlidePage(sps.get(k), 2);
								finalResults.addAll(temp);
								System.out.println("Page " + sps.get(k).get_PageNum() + " done! It is a root-index-multi-sub page");
								currentPos = k + 1;
							}
						}
					}
					else
					{
						//Page situation 3
						for(int k = currentPos; k < sps.size(); k++)
						{
							if(sps.get(k).get_PageNum() >= beginPageNum)
							{
								ArrayList<textOutline> temp = new ArrayList<textOutline>();
								temp = makeTextOutlinesFromOneSlidePage(sps.get(k), 1);
								finalResults.addAll(temp);
								System.out.println("Page " + sp.get_PageNum() + "-" + (j+1) + " done! It is the root-index page subtitle");
								System.out.println("Page " + sps.get(k).get_PageNum() + " done! It is a root-index-unique-sub page");
								currentPos = k + 1;
								break;
							}
						}
					}					
				}
				i = currentPos - 1;
				continue;
			}
			
			if(sp.get_pageType() == 2)
			{				
				int currentPos = i + 1;
				
				for(int j = 0; j < sp.get_texts().size(); j++)
				{
					int beginPageNum = sp.get_texts().get(j).get_child();
					int endPageNum = sp.get_texts().get(j).get_childEnd();
					
					//Page situation 5 special
					textOutline newTo = new textOutline(sp.get_texts().get(j).get_text(), 0, 0);
					newTo.set_time(sp.get_texts().get(j).get_time());
					finalResults.add(newTo);
					System.out.println("Page " + sp.get_PageNum() + "-" + (j+1)  + " done! It is tag page subtitle");
					
					for(int k = currentPos; k < sps.size(); k++)
					{
						if(sps.get(k).get_PageNum() < beginPageNum)
							continue;
						else if(sps.get(k).get_PageNum() > endPageNum)
							break;
						else
						{
							//Page situation 6
							if(sps.get(k).get_pageType() < 1)
							{
								ArrayList<textOutline> temp = new ArrayList<textOutline>();
								temp = makeTextOutlinesFromOneSlidePage(sps.get(k), 1);
								finalResults.addAll(temp);
								System.out.println("Page " + sps.get(k).get_PageNum() + " done! It is a tag-common page");
								currentPos = k + 1;
							}
							else if(sps.get(k).get_pageType() == 1)
							{
								//Page situation 7
								textOutline toTitle = new textOutline(sps.get(k).get_title(), 1, 0);
								toTitle.set_time(sps.get(k).get_startTime());
								finalResults.add(toTitle);
								System.out.println("Page " + sps.get(k).get_PageNum() + "-0 done! It is a tag-index page title");
								currentPos = k + 1;
								for(int l = 0; l < sps.get(k).get_texts().size(); l++)
								{
									int subBeginNum = sps.get(k).get_texts().get(l).get_child();
									int subEndNum = sps.get(k).get_texts().get(l).get_childEnd();
									
									if(subBeginNum < subEndNum)
									{
										//Page situation 9 special
										textOutline subNewTo = new textOutline(sps.get(k).get_texts().get(l).get_text(), 2, 0);
										subNewTo.set_time(sps.get(k).get_texts().get(l).get_time());
										finalResults.add(subNewTo);
										System.out.println("Page " + sps.get(k).get_PageNum() + "-" + (l+1) + " done! It is a tag-index page subtitle");
										
										//Page situation 9
										for(int m = currentPos; m < sps.size(); m++)
										{
											if(sps.get(m).get_PageNum() < subBeginNum)
												continue;
											else if(sps.get(m).get_PageNum() > subEndNum)
												break;
											else
											{
												ArrayList<textOutline> temp = new ArrayList<textOutline>();
												temp = makeTextOutlinesFromOneSlidePage(sps.get(m), 3);
												finalResults.addAll(temp);
												System.out.println("Page " + sps.get(m).get_PageNum() + " done! It is a tag-index-multi-sub page");
												currentPos = m + 1;
											}
										}
									}
									else
									{
										//Page situation 8
										for(int m = currentPos; m < sps.size(); m++)
										{
											if(sps.get(m).get_PageNum() >= subBeginNum)
											{
												ArrayList<textOutline> temp = new ArrayList<textOutline>();
												temp = makeTextOutlinesFromOneSlidePage(sps.get(m), 2);
												finalResults.addAll(temp);
												System.out.println("Page " + sps.get(k).get_PageNum() + "-" + (l+1) + " done! It is a tag-index page subtitle");
												System.out.println("Page " + sps.get(m).get_PageNum() + " done! It is a tag-index-unique-sub page");
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
	
	public ArrayList<textOutline> makeTextOutlinesFromOneSlidePage(slidePage sp, int sp_Hierarchy)
	{
		ArrayList<textOutline> results = new ArrayList<textOutline>();
		
		textOutline toTitle = new textOutline(sp.get_title(), sp_Hierarchy, 0);
		toTitle.set_time(sp.get_startTime());
		results.add(toTitle);
		
		//Important Notice: Here we alternately use the Integer Attribute "_child" to represent the text's hierarchy inside this page
		//                  And "_hierarchy" is used to represent the final text hierarchy inside the presentation
        //                  _child <= _hierarchy
		for(int i = 0; i < sp.get_texts().size(); i++)
		{
			textOutline origin = sp.get_texts().get(i);
			textOutline to = new textOutline(origin.get_text(), origin.get_hierarchy() + sp_Hierarchy, origin.get_hierarchy());
			to.set_time(origin.get_time());
			results.add(to);
		}
		
		return results;
	}
	
	private ArrayList<slidePage> synchronizeVideoAndFile(ArrayList<slidePage> sps, ArrayList<slidePage> sps_f)
	{
		System.out.println("Sychronization: video-" + sps.size() + ", file-" + sps_f.size());
		
		algorithmInterface ai = new algorithmInterface();
		
		/*
		 * Search for matching evidence globally: (NOT exclusive)
		 * 0: No Match
		 * 1: Perfectly Match: by levenshtein distance of all textual content
		 * 2: Title Match: same titles
		 * 3: Title Similar: similar titles
		 */		
		int[][] similarityMatrix = new int[sps.size()][sps_f.size()];
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage spv = sps.get(i);
			String title_v = spv.get_title().length() > 0 ? spv.get_title() : (spv.get_texts().size() > 0 ? spv.get_texts().get(0).get_text() : "");
			
			for(int j = 0; j < sps_f.size(); j++)
			{
				slidePage spf = sps_f.get(j);
				String title_f = spf.get_title().length() > 0 ? spf.get_title() : (spf.get_texts().size() > 0 ? spf.get_texts().get(0).get_text() : "");
				if(spv.isSamePage(spf))
					similarityMatrix[i][j] = 1;
				else if( title_v.contentEquals(title_f))
					similarityMatrix[i][j] = 2;
				else if( ai.getLevenshteinDistance(title_v, title_f) < Math.max(title_v.length(), title_f.length()) * 0.5)
					similarityMatrix[i][j] = 3;
				else
					similarityMatrix[i][j] = 0;
			}
			
		}
		
		for(int i = 0; i < sps.size(); i++)
		{
			for(int j = 0; j < sps_f.size(); j++)
			{
				if(j == sps_f.size()-1)
					System.out.print(similarityMatrix[i][j] + "\n");
				else
					System.out.print(similarityMatrix[i][j] + "\t");
			}
		}
		
		/*
		 * Matching process based on the order of video screenshot. 1 by 1, no rewind.
		 * If a video page cannot find a "Perfectly Match" file page, temporarily skip, continue.
		 * Once "Perfectly Matched", all pages between this and last matched video page will be processed.
		 * The basic idea is to use file pages "modify" the video pages.
		 */
		
		int lastSyncPosInVideo = -1;
		int lastSyncPosInFile = -1;
		ArrayList<Integer[]> changeLog = new ArrayList<Integer[]>();
		for(int i = 0; i < sps.size(); i++)
		{
			int syncPosInFile = 0 - sps_f.size();
			
			/*
			 * Search for a "Perfectly Match" file page.
			 * It is possible that a video page matches multiple file pages, but very rare.
			 * If so, choose the ?????
			 */
			for(int j = 0; j < sps_f.size(); j++)
			{
				if(similarityMatrix[i][j] == 1 && Math.abs(j-i) < Math.abs(j - syncPosInFile))
					syncPosInFile = j;
			}
			
			if(syncPosInFile >= 0)
			{	//Synchronize the matched page: from file to video
				
				slidePage spf = new slidePage();
				spf.contentCopy(sps_f.get(syncPosInFile));
				
				spf.set_startTime(sps.get(i).get_startTime());
				spf.set_PageNum(sps.get(i).get_PageNum());
				for(int k = 0; k < spf.get_texts().size(); k++)
					spf.get_texts().get(k).set_time(sps.get(i).get_startTime());
				
				if(this.isSplitPage(sps.get(i)) && !this.isSplitPage(spf))
					System.out.println("Split-page protect {" + (syncPosInFile+1) + " <-> " + (i+1) + "}");
				else
				{
					sps.set(i, spf);
					System.out.println("Synchronize {" + (syncPosInFile+1) + " -> " + (i+1) + "}");
				}
				
				/* 
				 * Above Code: Use the matched file page to replace video page, because of higher accuracy
				 * Below Code: Deal with the pages in between (if existing)
				 */

				if(syncPosInFile - lastSyncPosInFile > 1)
				{	// When there's some pages in file not being matched to video pages
					
					if(syncPosInFile - lastSyncPosInFile == i - lastSyncPosInVideo)
					{   //The unmatched interval of video and file is the same, synchronize 1 by 1, regardless of content
						for(int k = 1; lastSyncPosInVideo + k < i; k++)
						{
							slidePage sp_free = new slidePage();
							sp_free.contentCopy(sps_f.get(lastSyncPosInFile + k));
							
							sp_free.set_startTime(sps.get(lastSyncPosInVideo + k).get_startTime());
							sp_free.set_PageNum(sps.get(lastSyncPosInVideo + k).get_PageNum());
							for(int l = 0; l < sp_free.get_texts().size(); l++)
								sp_free.get_texts().get(l).set_time(sps.get(lastSyncPosInVideo + k).get_startTime());
							
							
							if(this.isSplitPage(sps.get(lastSyncPosInVideo + k)) && !this.isSplitPage(sp_free))
								System.out.println("Split-page protect {" + (lastSyncPosInFile + k + 1) + " <-> " + (lastSyncPosInVideo + k + 1) + "}");
							else
							{
								sps.set(lastSyncPosInVideo + k, sp_free);							
								System.out.println("Synchronize-Pos {" + (lastSyncPosInFile + k + 1) + " -> " + (lastSyncPosInVideo + k + 1) + "}");
							}
						}
					}
					else if(syncPosInFile - lastSyncPosInFile < i - lastSyncPosInVideo)
					{	
						/*
						 * If the video unmatched interval is larger, go through them.
						 * Try to find a title matched file page for the current video page
						 * If not found, attempt title similar page
						 * If still not found, leave the video page as before
						 * NOTICE: no orders here! May have error of vp-a matches fp-b, but vp-b matches fp-a......
						 */
						for(int x = lastSyncPosInVideo + 1; x < i; x++)
						{
							slidePage sp = sps.get(x);
							boolean found = false;
							for(int y = lastSyncPosInFile + 1; y < syncPosInFile; y++)
							{								
								if(similarityMatrix[x][y] == 2)
								{
									slidePage sp_free = new slidePage();
									sp_free.contentCopy(sps_f.get(y));
									
									sp_free.set_startTime(sp.get_startTime());
									sp_free.set_PageNum(sp.get_PageNum());
									for(int l = 0; l < sp_free.get_texts().size(); l++)
										sp_free.get_texts().get(l).set_time(sp.get_startTime());
									
									if(this.isSplitPage(sps.get(x)) && !this.isSplitPage(sp_free))
										System.out.println("Split-page protect {" + (y+1) + " <-> " + (x+1) + "}");
									else
									{
										sps.set(x, sp_free);									
										System.out.println("Synchronize-Title-e {" + (y+1) + " -> " + (x+1) + "}");
									}
									found = true;
									break;
								}								
							}
							
							if(found)
								continue;
							
							for(int y = lastSyncPosInFile + 1; y < syncPosInFile; y++)
							{								
								if(similarityMatrix[x][y] == 3)
								{
									slidePage sp_free = new slidePage();
									sp_free.contentCopy(sps_f.get(y));
									
									sp_free.set_startTime(sp.get_startTime());
									sp_free.set_PageNum(sp.get_PageNum());
									for(int l = 0; l < sp_free.get_texts().size(); l++)
										sp_free.get_texts().get(l).set_time(sp.get_startTime());
									
									if(this.isSplitPage(sps.get(x)) && !this.isSplitPage(sp_free))
										System.out.println("Split-page protect {" + (y+1) + " <-> " + (x+1) + "}");
									else
									{
										sps.set(x, sp_free);								
										System.out.println("Synchronize-Title-s {" + (y+1) + " -> " + (x+1) + "}");
									}
									break;
								}
								
							}
						}
					}
					else
					{   
						/*
						 * If the file unmatched interval is larger...
						 * 
						 * Add the missing file page
						 * Currently only for single missing page.
						 */
						if(i - lastSyncPosInVideo == 1 && syncPosInFile - lastSyncPosInFile == 2)
						{
							Integer[] insert = {i, syncPosInFile-1};
							changeLog.add(insert);
						}
					}
				}
				else if(syncPosInFile - lastSyncPosInFile == 1 || syncPosInFile - lastSyncPosInFile == 0)
				{
					/*
					 * When there is no interval between two adjacent matched file pages, delete all redundant video pages in between. 
					 */
					if(i - lastSyncPosInVideo > 1)
						for(int x = lastSyncPosInVideo + 1; x < i; x++)
						{
							Integer[] delete = {x, -1};
							changeLog.add(delete);
						}
				}
				
				//move forward
				lastSyncPosInVideo = i;
				lastSyncPosInFile = syncPosInFile;
			}
		}
		
		for(int x = changeLog.size()-1; x >= 0; x--)
		{
			Integer[] change = changeLog.get(x);
			System.out.println("Slide " + sps.get(change[0]).get_PageNum() + ": " + (change[1] >= 0 ? "Insert before this page" : "Delete this page"));
			
			if(change[1] < 0)
				sps.remove((int)change[0]);
			else
			{
				slidePage spf = new slidePage();
				spf.contentCopy(sps_f.get(change[1]));
				
				spf.set_startTime(sps.get(change[0]).get_startTime());
				spf.set_PageNum(sps.get(change[0]).get_PageNum());
				for(int k = 0; k < spf.get_texts().size(); k++)
					spf.get_texts().get(k).set_time(sps.get(change[0]).get_startTime());
				
				for(int i = change[0]; i < sps.size(); i++)
					sps.get(i).set_PageNum(sps.get(i).get_PageNum() + 1);
				
				sps.add(change[0], spf);
			}
		}

		return sps;
	}

	private ArrayList<slidePage> synchronizeVideoToFile(ArrayList<slidePage> sps, ArrayList<slidePage> sps_f)
	{
		System.out.println("Sychronization: video-" + sps.size() + ", file-" + sps_f.size());
		
		algorithmInterface ai = new algorithmInterface();
		
		/*
		 * Search for matching evidence globally: (NOT exclusive)
		 * 0: No Match
		 * 1: Perfectly Match: by levenshtein distance of all textual content
		 * 2: Title Match: same titles
		 * 3: Title Similar: similar titles
		 */		
		int[][] similarityMatrix = new int[sps_f.size()][sps.size()];
		for(int i = 0; i < sps_f.size(); i++)
		{
			slidePage spf = sps_f.get(i);
			String title_f = spf.get_title().length() > 0 ? spf.get_title() : (spf.get_texts().size() > 0 ? spf.get_texts().get(0).get_text() : "");
			
			for(int j = 0; j < sps.size(); j++)
			{
				slidePage spv = sps.get(j);
				String title_v = spv.get_title().length() > 0 ? spv.get_title() : (spv.get_texts().size() > 0 ? spv.get_texts().get(0).get_text() : "");
				if(spf.isSamePage(spv))
					similarityMatrix[i][j] = 1;
				else if( title_f.contentEquals(title_v))
					similarityMatrix[i][j] = 2;
				else if( ai.getLevenshteinDistance(title_v, title_f) < Math.max(title_v.length(), title_f.length()) * 0.5)
					similarityMatrix[i][j] = 3;
				else
					similarityMatrix[i][j] = 0;
			}
			
		}
		
		
		for(int i = 0; i < sps_f.size(); i++)
		{
			for(int j = 0; j < sps.size(); j++)
			{
				if(j == sps.size()-1)
					System.out.print(similarityMatrix[i][j] + "\n");
				else
					System.out.print(similarityMatrix[i][j] + "\t");
			}
		}
		
		int lastSyncPosInVideo = -1;
		int lastSyncPosInFile = -1;
		ArrayList<Integer> unmatchedList = new ArrayList<Integer>();
		for(int i = 0; i < sps_f.size(); i++)
		{
			int syncPosInVideo = -1;
			for(int j = 0; j < sps.size(); j++)
			{
				if(similarityMatrix[i][j] == 1)
				{
					if(syncPosInVideo < 0)
						syncPosInVideo = j;
					else if(Math.abs(j - i) < Math.abs(syncPosInVideo - i))
						syncPosInVideo = j;
				}
				
				if(j == sps.size() - 1 && i == sps_f.size()-1 && syncPosInVideo < 0)
					syncPosInVideo = j;
			}
			
			if(syncPosInVideo >= 0)
			{
				
				//First synchronize the pages between two "perfectly matched" pages
				if(i - lastSyncPosInFile > 1)
				{
					//Same distance of file gap and video gap
					if(i - lastSyncPosInFile == syncPosInVideo - lastSyncPosInVideo)
					{
						for(int k = 1; lastSyncPosInFile + k < i; k++)
						{
							sps_f.get(lastSyncPosInFile + k).set_startTime(sps.get(lastSyncPosInVideo + k).get_startTime());
							for(int l = 0; l < sps_f.get(lastSyncPosInFile + k).get_texts().size(); l++)
								sps_f.get(lastSyncPosInFile + k).get_texts().get(l).set_time(sps.get(lastSyncPosInVideo + k).get_startTime());
							
							if(this.isSplitPage(sps.get(lastSyncPosInVideo + k)) && !this.isSplitPage(sps_f.get(lastSyncPosInFile + k)))
							{
								sps.get(lastSyncPosInVideo + k).set_PageNum(sps_f.get(lastSyncPosInFile + k).get_PageNum());
								sps_f.set(lastSyncPosInFile + k, sps.get(lastSyncPosInVideo + k));
								System.out.println("Synchronize-pos {" + (lastSyncPosInVideo + k + 1) + " -> " + (lastSyncPosInFile + k + 1) + "} with split-page protection");
							}
							else
								System.out.println("Synchronize-pos {" + (lastSyncPosInVideo + k + 1) + " -> " + (lastSyncPosInFile + k + 1) + "}");
						}
					}
					//different gaps
					else// if(i - lastSyncPosInFile < syncPosInVideo - lastSyncPosInVideo)
					{
						int tempSyncPosInVideo = lastSyncPosInVideo;
						for(int x = lastSyncPosInFile + 1; x < i; x++)
						{
							if(i - x + 1 == syncPosInVideo - tempSyncPosInVideo)
							{
								for(int k = 0; x + k < i; k++)
								{
									sps_f.get(x + k).set_startTime(sps.get(tempSyncPosInVideo + k + 1).get_startTime());
									for(int l = 0; l < sps_f.get(x + k).get_texts().size(); l++)
										sps_f.get(x + k).get_texts().get(l).set_time(sps.get(tempSyncPosInVideo + k + 1).get_startTime());
									
									if(this.isSplitPage(sps.get(tempSyncPosInVideo + k)) && !this.isSplitPage(sps_f.get(x + k)))
									{
										sps.get(tempSyncPosInVideo + k).set_PageNum(sps_f.get(x + k).get_PageNum());
										sps_f.set(x + k, sps.get(tempSyncPosInVideo + k));
										System.out.println("Synchronize-subPos {" + (tempSyncPosInVideo + k + 2) + " -> " + (x + k + 1) + "} with split-page protection");
									}
									else
										System.out.println("Synchronize-subPos {" + (tempSyncPosInVideo + k + 2) + " -> " + (x + k + 1) + "}");
								}
								
								break;
							}
							
							
							boolean found = false;
							for(int y = tempSyncPosInVideo + 1; y < syncPosInVideo; y++)
							{
								if(similarityMatrix[x][y] == 2)
								{
									sps_f.get(x).set_startTime(sps.get(y).get_startTime());
									for(int l = 0; l < sps_f.get(x).get_texts().size(); l++)
										sps_f.get(x).get_texts().get(l).set_time(sps.get(y).get_startTime());
									
									if(this.isSplitPage(sps.get(y)) && !this.isSplitPage(sps_f.get(x)))
									{
										sps.get(y).set_PageNum(sps_f.get(x).get_PageNum());
										sps_f.set(x, sps.get(y));
										System.out.println("Synchronize-Title {" + (y+1) + " -> " + (x + 1) + "} with split-page protection");
									}
									else
										System.out.println("Synchronize-Title {" + (y+1) + " -> " + (x + 1) + "}");
									
									found = true;
									tempSyncPosInVideo = y;
									break;
								}
							}
							

							if(found)
								continue;
							
							for(int y = tempSyncPosInVideo + 1; y < syncPosInVideo; y++)
							{
								if(similarityMatrix[x][y] == 3)
								{
									sps_f.get(x).set_startTime(sps.get(y).get_startTime());
									for(int l = 0; l < sps_f.get(x).get_texts().size(); l++)
										sps_f.get(x).get_texts().get(l).set_time(sps.get(y).get_startTime());
									
									if(this.isSplitPage(sps.get(y)) && !this.isSplitPage(sps_f.get(x)))
									{
										sps_f.get(x).set_texts(sps.get(y).get_texts());
										System.out.println("Synchronize-partTitle {" + (y+1) + " -> " + (x + 1) + "} with split-page protection");
									}
									else
										System.out.println("Synchronize-partTitle {" + (y+1) + " -> " + (x + 1) + "}");
									
									found = true;
									tempSyncPosInVideo = y;
									break;
								}
							}
							
							if(!found)
							{
								unmatchedList.add(x);
								System.out.println("Synchronizing Failed at [ " + (x+1) + " ]");
							}
						}
					}
				}
				
				//Then synchronize the "perfectly matched" page.
				sps_f.get(i).set_startTime(sps.get(syncPosInVideo).get_startTime());
				for(int k = 0; k < sps_f.get(i).get_texts().size(); k++)
					sps_f.get(i).get_texts().get(k).set_time(sps.get(syncPosInVideo).get_startTime());
				
				if(this.isSplitPage(sps.get(syncPosInVideo)) && !this.isSplitPage(sps_f.get(i)))
				{
					sps_f.get(i).set_texts(sps.get(syncPosInVideo).get_texts());
					System.out.println("Synchronize {" + (syncPosInVideo+1) + " -> " + (i+1) + "} with split-page protection");
				}
				else
					System.out.println("Synchronize {" + (syncPosInVideo+1) + " -> " + (i+1) + "}");
				
				
				//move forward
				lastSyncPosInFile = i;
				lastSyncPosInVideo = syncPosInVideo;
			}
			
		}
		
		for(int i = 0; i < unmatchedList.size(); i++)
		{
			int previousSec, afterSec = 0, continuous = 1;
			previousSec = unmatchedList.get(i) == 0 ? 0 : ai.timeToSeconds(sps_f.get(unmatchedList.get(i)-1).get_startTime().toString());
			afterSec = unmatchedList.get(i) == sps_f.size() - 1 ? 0 : ai.timeToSeconds(sps_f.get(unmatchedList.get(i)+1).get_startTime().toString());
			
			
			for(int j = 1; i+j < unmatchedList.size(); j++)
			{
				if(unmatchedList.get(i+j) - unmatchedList.get(i+j-1) == 1)
				{
					//System.out.println("%%%%%%%% " + unmatchedList.get(i+j));
					continuous++;
					afterSec = unmatchedList.get(i+j) == sps_f.size() - 1 ? 0 : ai.timeToSeconds(sps_f.get(unmatchedList.get(i+j)+1).get_startTime().toString());
				}
				else
					break;
			}
			
			int gap = (afterSec - previousSec > 0) ? (afterSec - previousSec) / (continuous + (unmatchedList.get(i) == 0 ? 0 : 1)) : 1;
			//System.out.println("$$$$$$$$$$$$$ " + afterSec + " " + previousSec + " " + gap);
			
			for(int j = 0; j < continuous; j++)
			{
				Time ti;
				if(unmatchedList.get(i) == 0)
					ti = new Time(0-TimeZone.getDefault().getRawOffset() + gap*1000*(j));
				else
					ti = new Time(sps_f.get(unmatchedList.get(i)-1).get_startTime().getTime() + gap*1000*(j+1));
				
				sps_f.get(unmatchedList.get(i+j)).set_startTime(ti);
			}
			
			i += continuous - 1;
		}
		
		return sps_f;
	}
}
