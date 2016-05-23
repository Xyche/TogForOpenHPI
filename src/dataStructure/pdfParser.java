package dataStructure;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.pdfbox.util.TextPosition;

import helper.Constants;
import sharedMethods.algorithmInterface;


//import dataStructure.textLine;

public class pdfParser extends PDFTextStripper{
	
	public pdfParser()
            throws IOException {
        super.setSortByPosition(true);
    }
	
	public ArrayList<textLine> analyzePDF(String fileName) throws IOException
	{
		PDDocument pdf = PDDocument.load(new File(fileName));
		//System.out.println(pdf.getNumberOfPages());
		pdfParser pdfStripper = new pdfParser();
		
		List<?> allPages = pdf.getDocumentCatalog().getAllPages();
		ArrayList<textLine> tls = new ArrayList<textLine>();
		
		
		for(int k = 0; k < allPages.size(); k++)
		{
			PDPage page = (PDPage) allPages.get(k);
	        PDStream contents = page.getContents();
	        
	        pdfStripper.setStartPage(k+1);
			pdfStripper.setEndPage(k+1);
			pdfStripper.getText(pdf);
	        	        
	        if(contents != null)
	        {
	        	float width = page.findMediaBox().getWidth();
	        	float height = page.findMediaBox().getHeight();
	        	
	        	if(page.getRotation() != null && page.getRotation() == 90)
	        	{
	        		float temp = width;
	        		width = height;
	        		height = temp;
	        	}
	        	
	        	float rate = width/height >= 0.75 ? 1024/width : 768/height;        	

	        	textLine pre = new textLine();
	        	pre.set_count(-1);
	        	
	        	for(List<TextPosition> chars: pdfStripper.getCharactersByArticle()){
					if (chars.size() <= 0) {
						// Add an empty image page by setting one single
						// textline
						textLine current = new textLine(k + 1, "**Image Page, No Text**", 0,
								(int) (height * rate * 0.2), (int) (width * rate * 0.4), (int) (width * rate * 0.2),
								(int) (height * rate * 0.05), new Time(0));
						tls.add(current);
						continue;
					}

					int last_idx = -1;
	        		for(TextPosition text: chars)
	        		{
	        			TextPosition last = chars.get(Math.max(last_idx++, 0));
	        	        
	        	        if(pre.get_count() < 0)
	        	        {
	        	        	pre.set_text(text.getCharacter());
	        	        	pre.set_left((int)text.getXDirAdj());
	        	        	pre.set_top((int)text.getYDirAdj());
	        	        	pre.set_width((int)text.getWidthDirAdj());
	        	        	pre.set_height((int)text.getHeightDir());
	        	        	pre.set_count(1);
	        	        }
	        	        else if(pre.get_left() + pre.get_width() + Math.max(text.getWidthDirAdj(), last.getWidthDirAdj())*1.5 < text.getXDirAdj())
	        	        {
	        	        	//System.out.println("Horizontal");
	        	        	int Diff = width/height >= 0.75 ? Constants.DEFAULT_HEIGHT - (int)(height*rate) : Constants.DEFAULT_WIDTH - (int)(width*rate);
	        	        	
	        	        	int sWidth = (int)(pre.get_width()*rate);
	        	        	int sLeft = width/height >= 0.75 ? (int)(pre.get_left()*rate) : (int)(pre.get_left()*rate) + Diff/2;	        	        		        	        	
	        	        	int sHeight = (int)(pre.get_height()*rate);
	        	        	int sTop = width/height >= 0.75 ? (int)(pre.get_top()*rate) + Diff/2 : (int)(pre.get_top()*rate);
	        	        	
	        	        	//System.out.println(pre.get_height() + "\t" + sHeight);
	        	        	
	        	        	textLine current = new textLine(k+1, pre.get_text(), 0, sTop, sLeft, sWidth, sHeight, pre.get_time());
	        	        	tls.add(current);
	        	        	
	        	        	pre.set_text(text.getCharacter());
	        	        	pre.set_left((int)text.getXDirAdj());
	        	        	pre.set_top((int)text.getYDirAdj());
	        	        	pre.set_width((int)text.getWidthDirAdj());
	        	        	pre.set_height((int)text.getHeightDir());
	        	        	pre.set_count(1);
	        	        }
	        	        else if(Math.abs(pre.get_top() - text.getYDirAdj()) >= 1)
	        	        {
	        	        	//System.out.println("Vertical");
	        	        	int Diff = width/height >= 0.75 ? Constants.DEFAULT_HEIGHT - (int)(height*rate) : Constants.DEFAULT_WIDTH - (int)(width*rate);
	        	        	
	        	        	int sWidth = (int)(pre.get_width()*rate);
	        	        	int sLeft = width/height >= 0.75 ? (int)(pre.get_left()*rate) : (int)(pre.get_left()*rate) + Diff/2;	        	        		        	        	
	        	        	int sHeight = (int)(pre.get_height()*rate);
	        	        	int sTop = width/height >= 0.75 ? (int)(pre.get_top()*rate) + Diff/2 : (int)(pre.get_top()*rate);
	        	        	
	        	        	//System.out.println(pre.get_height() + "\t" + sHeight);
	        	        	
	        	        	textLine current = new textLine(k+1, pre.get_text(), 0, sTop, sLeft, sWidth, sHeight, pre.get_time());
	        	        	tls.add(current);
	        	        	
	        	        	pre.set_text(text.getCharacter());
	        	        	pre.set_left((int)text.getXDirAdj());
	        	        	pre.set_top((int)text.getYDirAdj());
	        	        	pre.set_width((int)text.getWidthDirAdj());
	        	        	pre.set_height((int)text.getHeightDir());
	        	        	pre.set_count(1);
	        	        }
	        	        else
	        	        {
	        	        	algorithmInterface ai = new algorithmInterface();
	        	        	
	        	        	if(pre.get_left() + pre.get_width() + Math.max(text.getWidthDirAdj(), last.getWidthDirAdj()) / 3 < text.getXDirAdj())
	        	        		pre.set_text(pre.get_text() + " " + text.getCharacter());
	        	        	else if(pre.get_text().length() == 1 && ai.isSignal(pre.get_text().charAt(0)))
	        	        		pre.set_text(pre.get_text() + " " + text.getCharacter());
	        	        	else
	        	        		pre.set_text(pre.get_text() + text.getCharacter());
	        	        	//pre.set_left((int)text.getXDirAdj());
	        	        	//pre.set_top((int)text.getYDirAdj());
	        	        	pre.set_width((int)(text.getWidthDirAdj() + text.getXDirAdj()) - pre.get_left());
	        	        	//pre.set_height((int)text.getHeightDir());
	        	        	
	        	        	if(chars.lastIndexOf(text) == chars.size() - 1)
	        	        	{
	        	        		//System.out.println("End");

		        	        	int Diff = width/height >= 0.75 ? Constants.DEFAULT_HEIGHT - (int)(height*rate) : Constants.DEFAULT_WIDTH - (int)(width*rate);
		        	        	
		        	        	int sWidth = (int)(pre.get_width()*rate);
		        	        	int sLeft = width/height >= 0.75 ? (int)(pre.get_left()*rate) : (int)(pre.get_left()*rate) + Diff/2;	        	        		        	        	
		        	        	int sHeight = (int)(pre.get_height()*rate);
		        	        	int sTop = width/height >= 0.75 ? (int)(pre.get_top()*rate) + Diff/2 : (int)(pre.get_top()*rate);
		        	        	
		        	        	//System.out.println(pre.get_height() + "\t" + sHeight);
		        	        	
		        	        	textLine current = new textLine(k+1, pre.get_text(), 0, sTop, sLeft, sWidth, sHeight, pre.get_time());
		        	        	tls.add(current);
	        	        	}
	        	        }
	        	        /*
	        	        if(k+1 == 8)
	        	        System.out.println("String [" + text.getXDirAdj() + ","
	        	                + text.getYDirAdj() + " fs=" + text.getFontSize() + " xscale="
	        	                + text.getXScale() + " height=" + text.getHeightDir() + " space="
	        	                + text.getWidthOfSpace() + " width="
	        	                + text.getWidthDirAdj() + "] " + text.getCharacter());*/
	        		}
	        	}
	        }
		}

        pdf.close();
        
		//System.out.println("Done!");
		return tls;
	}

}
