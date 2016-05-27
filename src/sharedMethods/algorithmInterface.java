package sharedMethods;

import java.io.IOException;
import java.sql.Time;

import helper.LoggerSingleton;

public class algorithmInterface {
	
	public algorithmInterface () {}
	

	public Time parseJsonTimeTag(String tag)
	{
		TimeProblem tp = new TimeProblem();
		return tp.parseJsonTimeTag(tag);
	}


	
	public int getLevenshteinDistance(String a, String b)
	{
		Levenshtein l = new Levenshtein();
		return l.getLevenshtein(a, b);
	}
	
	public int getSameWordsNum(String a, String b)
	{
		sameWords sw = new sameWords();
		return sw.getSameWordsNum(a, b);
	}
	
	public double getSameWordsRatio(String a, String b)
	{
		sameWords sw = new sameWords();
		return sw.getSameWordsRatio(a, b);
	}
	
	public int getSimilarWordsNumByLevenshtein(String a, String b)
	{
		Levenshtein l = new Levenshtein();
		return l.getSimilarWordsNumByLevenshtein(a, b);
	}
	
	public boolean isOrderNum(String a)
	{
		sameWords sw = new sameWords();
		return sw.isOrderNum(a);
	}
	
	public boolean isNum(String a)
	{
		sameWords sw = new sameWords();
		return sw.isNum(a);
	}
	
	public boolean isArabicNum(String a)
	{
		sameWords sw = new sameWords();
		return sw.isArabicNum(a);
	}
	
	public boolean isStatNum(String a)
	{
		sameWords sw = new sameWords();
		return sw.isStatNum(a);
	}
	
	public boolean isOptionNum(String a)
	{
		sameWords sw = new sameWords();
		return sw.isOptionNum(a);
	}
	
	public boolean isLogicalSameWords(String a, String b)
	{
		sameWords sw = new sameWords();
		return sw.isLogicalSameWords(a, b);
	}
	
	public String secondsToTime(int seconds)
	{
		TimeProblem tp = new TimeProblem();
		return tp.toStringFromSeconds(seconds);
	}
	
	public int timeToSeconds(String s)
	{
		TimeProblem tp = new TimeProblem();
		return tp.toSecondsFromString(s);
	}
	
	public boolean isSignal(char a)
	{
		sameWords sw = new sameWords();
		return sw.isSignal(a);
	}
	
	public boolean is_aA(String a)
	{
		sameWords sw = new sameWords();
		return sw.is_aA(a);
	}
	
	public boolean containTooManySameLetter(String a)
	{
		sameWords sw = new sameWords();
		return sw.containTooManySameLetter(a);
	}
	
	public void cropTable(String workingDir, String lectureID, int slideID, int[] tableRange)
	{
		imageProblem ip = new imageProblem();
		try {
			ip.cropTableFromSlide(workingDir, lectureID, slideID, tableRange);
		} catch (IOException e) {
			LoggerSingleton.info("Table Cropping Error!" + lectureID + "-" + slideID);
		}
	}
	
	public void runTesseract(String workingDir, String lectureID, int slideID)
	{
		imageProblem ip = new imageProblem();
		try {
			ip.runTessract(workingDir, lectureID, slideID);
		} catch (IOException e) {
			e.printStackTrace();
			LoggerSingleton.info("Run Tesseract Error on " + lectureID + "-" + slideID);
		}
	}

}
