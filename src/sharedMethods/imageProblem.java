package sharedMethods;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;

import helper.Constants;
import helper.LoggerSingleton;

public class imageProblem {
	
	public imageProblem() {}
	
	public void cropTableFromSlide(String lectureID, int slideID, int[] tableRange) throws IOException
	{
		BufferedImage pic = ImageIO.read(new File(Constants.joinPath(Constants.DEFAULT_WORKING_DIR, lectureID, "thumbnails", slideID + ".jpg")));
		
		int left = tableRange[0] - 5 >= 0 ? tableRange[0] - 5 : 0;
		int top = tableRange[2] - 5 >= 0 ? tableRange[2] - 5 : 0;
		int width = tableRange[1] + 5 > pic.getWidth() ? pic.getWidth() - tableRange[0] : tableRange[1] - tableRange[0] + 10;
		int height = tableRange[3] + 5 > pic.getHeight() ? pic.getHeight() - tableRange[2] : tableRange[3] - tableRange[2] + 10;
		
		File dir = new File(Constants.joinPath(Constants.DEFAULT_TOG_FOLDER, lectureID));
		if(!dir.exists())
			dir.mkdir();
		
		//pic = pic.getSubimage(left, top, width, height);
		
		Graphics2D g = pic.createGraphics();
		g.setColor(Color.YELLOW);
		g.drawRect(left, top, width, height);
		g.drawRect(left-1, top-1, width+2, height+2);
		g.drawRect(left-2, top-2, width+4, height+4);
		g.drawRect(left-3, top-3, width+6, height+6);
		g.dispose();
		
		File outputfile = new File(Constants.joinPath(
				Constants.DEFAULT_TOG_FOLDER, 
				lectureID, 
				String.format("%s-%s-%s-%s.jpg", lectureID, slideID, tableRange[0], tableRange[2])));
		ImageIO.write(pic, "jpg", outputfile);
		
		return;
	}
	
	public void runTessract(String lectureID, int slideID) throws IOException
	{
		BufferedImage pic = ImageIO.read(new File(Constants.joinPath(
				Constants.DEFAULT_WORKING_DIR, lectureID, "thumbnails", slideID + ".jpg")));
		File outputfile = new File(Constants.joinPath(Constants.DEFAULT_TESSERACT_FOLDER, "input.jpg"));
		ImageIO.write(pic, "jpg", outputfile);
		
		//LoggerSingleton.info("here");
		File tessFolder = new File(Constants.joinPath(Constants.DEFAULT_TESSERACT_FOLDER, "Tesseract-OCR-executable"));
		ProcessBuilder pb = new ProcessBuilder(Constants.joinPath(tessFolder.getAbsolutePath(), "run.bat"));
		pb.directory(tessFolder);
		Process p = pb.start();
		int isFinished = -1;
		try {
			isFinished = p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(isFinished != 0)
			LoggerSingleton.info("Tesseract Error!");
		p.destroy();
		//LoggerSingleton.info("there");
		
		pic = ImageIO.read(new File(Constants.joinPath(Constants.DEFAULT_TESSERACT_FOLDER, "output.jpg")));
		File resultsDir = new File(Constants.joinPath(Constants.DEFAULT_TESSERACT_FOLDER, "Result", lectureID));
		if(!resultsDir.exists())
			resultsDir.mkdir();
		outputfile = new File(Constants.joinPath(resultsDir.getAbsolutePath(), slideID + ".jpg"));
		ImageIO.write(pic, "jpg", outputfile);
		
		File iFile = new File(Constants.joinPath(tessFolder.getAbsolutePath(), "tess-table.txt"));
		if(iFile.exists())
		{
			BufferedReader br = new BufferedReader(new FileReader(iFile));
			for(String a = br.readLine(); a != null; a = br.readLine())
			{
				File oFile = new File(Constants.joinPath(resultsDir.getAbsolutePath(), "stats.txt"));
				BufferedWriter output = new BufferedWriter(new FileWriter(oFile, true));										
				output.append(slideID + ":\t" + a);
				output.newLine();
				output.close();
			}
			br.close();
		}
		
		return;
	}
	
}
