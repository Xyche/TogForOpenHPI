package dataStructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;

import helper.FilterableList;


public class pptParser {
	
	public pptParser() {}
	
	public FilterableList<slidePage> analyzePPTX(String fileName) throws IOException
	{
		XMLSlideShow ppt = new XMLSlideShow(new FileInputStream(fileName));
		XSLFSlide[] slides = ppt.getSlides();
		//LoggerSingleton.info(slides.length);
		
		FilterableList<slidePage> sps = new FilterableList<slidePage>();
		
		for(int x = 0; x < slides.length; x++)
		{
			slidePage sp = new slidePage();
			sp.set_PageNum(x+1);
			
			//LoggerSingleton.info(slides[x].getTitle());
			if(slides[x].getTitle().length() > 0)
			{
				String text = slides[x].getTitle();
				text = text.replace("\n", " ");
				while(text.contains("  "))
					text = text.replace("  ", " ");
				while(text.charAt(0) == ' ')
					text = text.substring(1);
				
				sp.set_title(text);
			}
			
			ArrayList<textOutline> tos = new ArrayList<textOutline>();
			
			for(int i = 0; i < slides[x].getPlaceholders().length; i++)
			{
				//LoggerSingleton.info();
				//LoggerSingleton.info("Component " + (i+1) + ": " + slides[x].getPlaceholder(i).getShapeName());
				
				if(slides[x].getPlaceholder(i).getText().contentEquals(slides[x].getTitle()))
					continue;
				else if(slides[x].getPlaceholder(i).getText().length() <= 3)
					continue;
				else if(slides[x].getPlaceholder(i).getAnchor().getY() > ppt.getPageSize().height * 0.85)
					continue;
				else if(slides[x].getPlaceholder(i).getAnchor().getX() > ppt.getPageSize().width * 0.85)
					continue;

				
				for(int j = 0; j < slides[x].getPlaceholder(i).getTextParagraphs().size(); j++)
				{					
					XSLFTextParagraph p = slides[x].getPlaceholder(i).getTextParagraphs().get(j);
					
					if(p.getText().length() <= 0)
						continue;
					
					int base = slides[x].getPlaceholder(i).getTextParagraphs().get(0).getLevel();
					
					//System.out.print(slides[x].getPlaceholder(i).getTextParagraphs().get(j).getLevel() + ": ");
					//LoggerSingleton.info(slides[x].getPlaceholder(i).getTextParagraphs().get(j).getText() + "  ########");
					
					String text = p.getText();
					text = text.replace("\n", " ");
					while(text.contains("  "))
						text = text.replace("  ", " ");
					while(text.length() > 0 && text.charAt(0) == ' ')
						text = text.substring(1);
					
					if(text.length() <= 0)
						continue;
					
					textOutline to = new textOutline();
					to.set_hierarchy(p.getLevel() + 1 - base);
					to.set_text(text);
					tos.add(to);
					
				}
			}
			
			//LoggerSingleton.info();
			//LoggerSingleton.info("--------------------------");
			//LoggerSingleton.info();
			
			sp.set_texts(tos);
			sp.isSlideWellOrganized();
			
			if(sp.get_pageType().isNotEmpty())
				sps.add(sp);
		}
		/*
		LoggerSingleton.info();
		for(int i = 0; i < sps.size(); i++)
		{
			slidePage sp = sps.get(i);
			System.out.print(sp.get_PageNum() + "  ");
			LoggerSingleton.info(sp.get_title());
			
			for(int j = 0; j < sp.get_texts().size(); j++)
			{
				textOutline to = sp.get_texts().get(j);
				if(to.get_hierarchy()==1)
					System.out.print("--");
				else if(to.get_hierarchy()==2)
					System.out.print("----");
				else if(to.get_hierarchy()==3)
					System.out.print("------");
				LoggerSingleton.info(to.get_text());
			}
			
			LoggerSingleton.info("PageType: " + sp.get_pageType());
			LoggerSingleton.info();
		} 
		*/
		return sps;
	}
}
