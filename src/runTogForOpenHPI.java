import java.io.*;
import java.sql.*;
import java.util.*;

import org.apache.commons.cli.*;
import org.json.simple.parser.ParseException;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;

import sharedMethods.algorithmInterface;
import dataStructure.*;
import helper.ArgumentParser;
import helper.Constants;
import helper.JSONThumbnailsParser;
import helper.LoggerSingleton;
import helper.OCROriginMode;

public class runTogForOpenHPI {

	private static final int localTimeZoneOffset = TimeZone.getDefault().getRawOffset();

	public static void main(String[] args) throws SQLException, ParserConfigurationException, SAXException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, ParseException {
		Options opt = ArgumentParser.defineCLI();
		CommandLine cmd = ArgumentParser.parseCLI(opt, args);
		if(cmd == null) return;

		final boolean changeBBImageNames = cmd.hasOption(ArgumentParser.CHANGE_NAMES_KEY);
		String modeString = cmd.getOptionValue(ArgumentParser.MODE_KEY, String.valueOf(OCROriginMode.DEFAULT_MODE.ordinal()));
		final OCROriginMode OCR_Origin = OCROriginMode.parseFromOption(modeString);
		
		final int pageWidth = ArgumentParser.width(cmd), pageHeight = ArgumentParser.height(cmd);

		boolean havePDF = cmd.hasOption(ArgumentParser.PDF_KEY);
		boolean havePPTX = cmd.hasOption(ArgumentParser.PPTX_KEY);
		if(havePDF == havePPTX){
			havePDF = true;
			havePPTX = false;
		}
		final String lecture_id = cmd.getOptionValue(ArgumentParser.LECTURE_KEY);
		final String workingFolder = cmd.getOptionValue(ArgumentParser.FOLDER_KEY, Constants.DEFAULT_WORKING_DIR);
		LoggerSingleton.setUp(cmd.getOptionValue(ArgumentParser.LOGGER_KEY, Constants.joinPath(Constants.DEFAULT_WORKING_DIR, lecture_id + ".log")));


		ArrayList<textLine> tll = new ArrayList<textLine>();
		tll = loadOcrResults(OCR_Origin, workingFolder, lecture_id, localTimeZoneOffset, changeBBImageNames);

		LoggerSingleton.info("< STEP 1: Loading from Database >");
		for(int i = 0; i < tll.size(); i++) {
			textLine t = tll.get(i);
			LoggerSingleton.info(Joiner.on(" ").join(new String[] {
		      String.valueOf(t.get_slideID()),
		      String.valueOf(t.get_type()),
		      String.valueOf(t.get_text()),
		      String.valueOf(t.get_top()),
		      String.valueOf(t.get_left()),
		      String.valueOf(t.get_width()),
		      String.valueOf(t.get_height()),
		      String.valueOf(t.get_time())
			}));
		}
		//LoggerSingleton.info("$$$$\tOCR text-lines:\t" + tll.size());

		outlineGenerator og = new outlineGenerator(pageWidth, pageHeight, workingFolder, lecture_id);
		ArrayList<textOutline> finalResults = new ArrayList<textOutline>();


		if(havePPTX)
			finalResults = og.generateOutlineWithPPTX(tll, Constants.joinPath(workingFolder, lecture_id, Constants.DEFAULT_SLIDES_PPTX));
		else if(havePDF){
			ArrayList<textLine> tll_pdf = new ArrayList<textLine>();
			tll_pdf = loadOcrResults(OCROriginMode.PDF, workingFolder, lecture_id, localTimeZoneOffset, changeBBImageNames);
			finalResults = og.generateOutlineWithPDF(tll, tll_pdf);
			tll_pdf.clear();
		} else 
			finalResults = og.generateOutline(tll);
		tll.clear();

		int count1 = 1, count2 = 1, count1low = 0, count2low = 0, count1dot = 0, count2dot = 0;
		for(int i = 0; i < finalResults.size(); i++)
		{
			if(finalResults.get(i).get_child() == 0)
				continue;

			if(finalResults.get(i).get_child() == 1)
				count1++;
			else
				count2++;

			char a = finalResults.get(i).get_text().charAt(0);
			if(a >= 'a' && a <= 'z')
			{
				if(finalResults.get(i).get_text().length() > 3 && finalResults.get(i).get_text().charAt(1) != ' '
					&& ( finalResults.get(i).get_text().charAt(2) < 'A' || finalResults.get(i).get_text().charAt(2) > 'Z') )
				{
					if(finalResults.get(i).get_child() == 1)
						count1low++;
					else
						count2low++;
				}
			}

			if(finalResults.get(i).get_text().length() > 3 && finalResults.get(i).get_text().charAt(1) == ' ')
			{
				if(finalResults.get(i).get_child() == 1)
					count1dot++;
				else
					count2dot++;
			}
		}
		
		double 
			topicCaseStartRatio = count1 == 0 && count2 == 0 ? 0 : 100 * (count1low + count2low) / (count1 + count2),
			lev1TopicCaseStartRatio = count1 == 0 ? 0 : 100 * count1low / count1, 
			lev2TopicCaseStartRatio = count2 == 0 ? 0 : 100 * count2low / count2,
			topicWithDotRatio = count1 == 0 && count2 == 0 ? 0 : 100 * (count1dot + count2dot) / (count1 + count2),
			lev1WithDotRatio = count1 == 0 ? 0 : 100 * count1dot / count1, 
			lev2WithDotRatio = count2 == 0 ? 0 : 100 * count2dot / count2;
		
		if (topicCaseStartRatio > 30)
			og.set_beginWithLowCaseLetter(true);
		else if (topicCaseStartRatio > 20 && lev2TopicCaseStartRatio >= lev1TopicCaseStartRatio * 1.5)
			og.set_beginWithLowCaseLetter(true);
		else
			og.set_beginWithLowCaseLetter(false);
		
		if(topicWithDotRatio >= 10)
			og.set_haveSignBeforeSubtopic(true);
		else
			og.set_haveSignBeforeSubtopic(false);
			

		LoggerSingleton.info("Total Topic: " + (count1+count2) + " Low Case Start: " + (count1low+count2low) + " Ratio: " + topicCaseStartRatio + "% " + og.is_beginWithLowCaseLetter());
		LoggerSingleton.info("Lev-1 Topic: " + count1 + " Low Case Start: " + count1low + " Ratio: " + lev1TopicCaseStartRatio + "%");
		LoggerSingleton.info("Lev-2 Topic: " + count2 + " Low Case Start: " + count2low + " Ratio: " + lev2TopicCaseStartRatio + "%");

		LoggerSingleton.info("Total Topic: " + (count1+count2) + " With a dot: " + (count1dot+count2dot) + " Ratio: " + topicWithDotRatio + "% " + og.is_haveSignBeforeSubtopic());
		LoggerSingleton.info("Lev-1 Topic: " + count1 + " With a dot: " + count1dot + " Ratio: " + lev1WithDotRatio + "%");
		LoggerSingleton.info("Lev-2 Topic: " + count2 + " With a dot: " + count2dot + " Ratio: " + lev2WithDotRatio + "%");

		// To do: a self-check system for adaptive round
		boolean havingAdaptiveDifference = true;
		int roundNum = 1;

		while(havingAdaptiveDifference && roundNum <= 3)
		{
			// Recode the adaptive status in the last round, for future comparison
			boolean lastRoundLowCaseStart = og.is_beginWithLowCaseLetter();
			boolean lastRoundBeginningDot = og.is_haveSignBeforeSubtopic();

			ArrayList<int[]> lastRoundTableAreas = new ArrayList<int[]>();
			for(int i = 0; i < og.get_potentialTitleArea().size(); i++)
			{
				int[] temp = og.get_potentialTitleArea().get(i);
				lastRoundTableAreas.add(temp);
			}

			ArrayList<Integer> lastRoundGaps = new ArrayList<Integer>();
			for(int i = 0; i < og.get_potentialHierarchicalGap().size(); i++)
			{
				int temp = og.get_potentialHierarchicalGap().get(i);
				lastRoundGaps.add(temp);
			}

			LoggerSingleton.info("------------------ADAPTIVE ROUND " + roundNum + "---------------------------");

			// Do the adaptive round
			tll = loadOcrResults(OCR_Origin, workingFolder, lecture_id, localTimeZoneOffset, false);

			if(havePPTX)
				finalResults = og.generateOutlineWithPPTX(tll, Constants.joinPath(workingFolder, lecture_id, Constants.DEFAULT_SLIDES_PPTX));
			else if(havePDF)
			{
				ArrayList<textLine> tll_pdf = new ArrayList<textLine>();
				tll_pdf = loadOcrResults(OCROriginMode.PDF, workingFolder, lecture_id, localTimeZoneOffset, changeBBImageNames);

				finalResults = og.generateOutlineWithPDF(tll, tll_pdf);
				tll_pdf.clear();
			}
			else
				finalResults = og.generateOutline(tll);
			tll.clear();

			// Update the status of "LowCaseStart" and "HavingDot" for this round
			count1 = 1;
			count2 = 1;
			count1low = 0;
			count2low = 0;
			count1dot = 0;
			count2dot = 0;
			for(int i = 0; i < finalResults.size(); i++)
			{
				if(finalResults.get(i).get_child() == 0)
					continue;

				if(finalResults.get(i).get_child() == 1)
					count1++;
				else
					count2++;

				char a = finalResults.get(i).get_text().charAt(0);
				if(a >= 'a' && a <= 'z')
				{
					if(finalResults.get(i).get_text().length() > 3 && finalResults.get(i).get_text().charAt(1) != ' '
						&& ( finalResults.get(i).get_text().charAt(2) < 'A' || finalResults.get(i).get_text().charAt(2) > 'Z') )
					{
						if(finalResults.get(i).get_child() == 1)
							count1low++;
						else
							count2low++;
					}
				}

				if(!lastRoundBeginningDot)
					if(finalResults.get(i).get_text().length() > 3 && finalResults.get(i).get_text().charAt(1) == ' ')
					{
						if(finalResults.get(i).get_child() == 1)
							count1dot++;
						else
							count2dot++;
					}
			}

			topicCaseStartRatio = count1 == 0 && count2 == 0 ? 0 : 100 * (count1low + count2low) / (count1 + count2);
			lev1TopicCaseStartRatio = count1 == 0 ? 0 : 100 * count1low / count1;
			lev2TopicCaseStartRatio = count2 == 0 ? 0 : 100 * count2low / count2;
			topicWithDotRatio = count1 == 0 && count2 == 0 ? 0 : 100 * (count1dot + count2dot) / (count1 + count2);
			lev1WithDotRatio = count1 == 0 ? 0 : 100 * count1dot / count1;
			lev2WithDotRatio = count2 == 0 ? 0 : 100 * count2dot / count2;
		

			if(topicCaseStartRatio > 30)
				og.set_beginWithLowCaseLetter(true);
			else if(topicCaseStartRatio > 20 && lev2TopicCaseStartRatio >= 1.5 * lev1TopicCaseStartRatio)
				og.set_beginWithLowCaseLetter(true);
			else
				og.set_beginWithLowCaseLetter(false);

			LoggerSingleton.info("Total Topic: " + (count1+count2) + " Low Case Start: " + (count1low+count2low) + " Ratio: " + topicCaseStartRatio + "% " + og.is_beginWithLowCaseLetter());
			LoggerSingleton.info("Lev-1 Topic: " + count1 + " Low Case Start: " + count1low + " Ratio: " + lev1TopicCaseStartRatio + "%");
			LoggerSingleton.info("Lev-2 Topic: " + count2 + " Low Case Start: " + count2low + " Ratio: " + lev2TopicCaseStartRatio + "%");

			if(!lastRoundBeginningDot)
			{
				if(topicWithDotRatio >= 10)
					og.set_haveSignBeforeSubtopic(true);
				else
					og.set_haveSignBeforeSubtopic(false);

				LoggerSingleton.info("Total Topic: " + (count1+count2) + " With a dot: " + (count1dot+count2dot) + " Ratio: " + topicWithDotRatio + "% " + og.is_haveSignBeforeSubtopic());
				LoggerSingleton.info("Lev-1 Topic: " + count1 + " With a dot: " + count1dot + " Ratio: " + lev1WithDotRatio + "%");
				LoggerSingleton.info("Lev-2 Topic: " + count2 + " With a dot: " + count2dot + " Ratio: " + lev2WithDotRatio + "%");
			}
			else
			{
				og.set_haveSignBeforeSubtopic(true);
				LoggerSingleton.info("All dots have been removed in this round, we must keep this status as TRUE.");
			}


			roundNum++;
			havingAdaptiveDifference = false;

			if(og.is_beginWithLowCaseLetter() != lastRoundLowCaseStart)
			{
				havingAdaptiveDifference = true;
				LoggerSingleton.info("LowCaseStart status changed!!!");
			}

			if(og.is_haveSignBeforeSubtopic() != lastRoundBeginningDot)
			{
				havingAdaptiveDifference = true;
				LoggerSingleton.info("BeginningDot status changed!!!");
			}

			if(og.get_potentialTitleArea().size() != lastRoundTableAreas.size())
			{
				havingAdaptiveDifference = true;
				LoggerSingleton.info("Potential Title Area changed!!!");
			}
			else
			{
				for(int i = 0; i < og.get_potentialTitleArea().size(); i++)
				{
					int[] ref = og.get_potentialTitleArea().get(i);
					boolean match = false;
					for(int j = 0; j < lastRoundTableAreas.size(); j++)
					{
						int[] current = lastRoundTableAreas.get(j);
						if(Math.abs(current[0] - ref[0]) <= pageWidth / 256 && Math.abs(current[1] - ref[1]) <= pageHeight/256
								&& current[2] == ref[2] && Math.abs(current[3] - ref[3]) <= pageHeight / 256)
						{
							match = true;
							break;
						}
					}
					if(!match)
					{
						havingAdaptiveDifference = true;
						LoggerSingleton.info("Potential Title Area changed!!!");
						break;
					}

				}
			}

			if(og.get_potentialHierarchicalGap().size() != lastRoundGaps.size())
			{
				havingAdaptiveDifference = true;
				LoggerSingleton.info("Hierarchical Gaps changed!!!");
			}
			else
			{
				for(int i = 0; i < og.get_potentialHierarchicalGap().size(); i++)
				{
					int ref = og.get_potentialHierarchicalGap().get(i);
					boolean match = false;
					for(int j = 0; j < lastRoundGaps.size(); j++)
					{
						int current = lastRoundGaps.get(j);
						if(Math.abs(current - ref) <= pageWidth/256)
						{
							match = true;
							break;
						}
					}
					if(!match)
					{
						havingAdaptiveDifference = true;
						LoggerSingleton.info("Hierarchical Gaps changed!!!");
						break;
					}
				}
			}

		}
		/*
		LoggerSingleton.info();
		LoggerSingleton.info("------------------ADAPTIVE ROUND---------------------------");
		LoggerSingleton.info();

		tll = loadOcrResults(OCR_Origin, lecture_id, localTimeZoneOffset, false);
		finalResults = og.generateOutline(tll);
		*/
		/* show result */
		LoggerSingleton.info("< Final Results: >");

		File newFile = new File(Constants.joinPath(workingFolder, lecture_id, "outline"));
		if(newFile.exists()) newFile.delete();

		for(int i = 0; i < finalResults.size(); i++)
		{
			BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));
			output.append(finalResults.get(i).get_hierarchy() + "\t" + finalResults.get(i).get_child() + "\t" + finalResults.get(i).get_text());
			output.newLine();
			output.close();

			// Control of hierarchy inside single page
			if(finalResults.get(i).get_child() > 2)
				continue;

			// Control of total hierarchy in the presentation
			if(finalResults.get(i).get_hierarchy() > 3)
				continue;


//			String temp = "";
//
//			for(int j = 0; j <= finalResults.get(i).get_hierarchy(); j++)
//			{
//				LoggerSingleton.info("--");
//				if(j > 0)
//					temp += "\t";
//			}

			if(finalResults.get(i).get_child() == 0)
			{
				LoggerSingleton.info("$$ ");
			}
			LoggerSingleton.info(finalResults.get(i).get_text() + " " + finalResults.get(i).get_time());
		}

		/* Until now, the content structure generation process has been finished!
		 * Below there will be some further methods towards segmentation & annotation
		 * Especially for ACM Multimedia 2013 Grand Challenge.
		 */

		LoggerSingleton.info("-------------------------------------------------------");

//		ArrayList<textOutline> segments = new ArrayList<textOutline>();
		/* segments = */autoSegmentationAndAnnotation(finalResults, workingFolder, lecture_id);

	}

	static ArrayList<textLine> loadFromMySQL(String lecture_id) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException{

		ArrayList<textLine> tll = new ArrayList<textLine>();
		ArrayList<textLine> tl2 = new ArrayList<textLine>();

		String url = "jdbc:mysql://localhost/ak?user=Xyche&password=123&useUnicode=true&characterEncoding=8859_1";
		String sql = "select t.*, l.start from ocrtool_textline as t, ocrtool_lectureslide as l where t.lectureSlide_id = l.id and l.lecture_id = " + lecture_id + " order by id";
		Class.forName("com.mysql.jdbc.Driver").newInstance();

		Connection connection = DriverManager.getConnection(url);
		PreparedStatement ps = null;
		ps = connection.prepareStatement(sql);
		ResultSet rs1 = ps.executeQuery();

		int slideNumBase = 0;
		int currentSlide = 0;

		// Load data from database and set them into textLine structure
		// Then change the order from descend to ascend by id inside each slide

		while(rs1.next())
		{
			int tempSlideID = rs1.getInt("lectureSlide_id");
			if(slideNumBase == 0) slideNumBase = tempSlideID;
			tempSlideID -= slideNumBase;

			int intType = -1;
			String temp = rs1.getString("type");
			if(temp.contentEquals("Title")) intType = 1;
			else if (temp.contentEquals("Subtitle")) intType = 2;
			else if (temp.contentEquals("Footline")) intType = 3;
			else intType = 0;

			textLine t = new textLine(tempSlideID + 1,
									  rs1.getString("content"),
									  intType,
									  rs1.getInt("top"),
									  rs1.getInt("left"),
									  rs1.getInt("width"),
									  rs1.getInt("height"),
									  rs1.getTime("start"));

			if(tempSlideID + 1 > currentSlide)
			{
				if(!tl2.isEmpty())
				{
					for(int i = tl2.size() - 1; i >= 0; i--)
					{
						tll.add(tl2.get(i));
					}
				}
				tl2.clear();
				currentSlide = tempSlideID + 1;
			}

			tl2.add(t);
		}

		if(!tl2.isEmpty())
		{
			for(int i = tl2.size() - 1; i >= 0; i--)
			{
				tll.add(tl2.get(i));
			}
		}

		tl2.clear();
		rs1.close();
		ps.close();
		connection.close();

		return tll;
	}

	static ArrayList<textLine> loadFromTeleTaskXML(String workingFolder, String lecture_id, boolean changeBBImageNames) throws ParserConfigurationException, SAXException, IOException{

		ArrayList<textLine> tll = new ArrayList<textLine>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(Constants.joinPath(workingFolder, lecture_id, "recognition", "recognition.xml")));

		Element root = doc.getDocumentElement();
		NodeList nodes = root.getElementsByTagName("TextObject");

		if (nodes != null && nodes.getLength() > 0)
		{
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Element textLine = (Element) nodes.item(i);
				NodeList nn = textLine.getChildNodes();
				textLine t = new textLine();
				for (int j = 0; j < nn.getLength(); j++)
				{
					if (nn.item(j).getNodeType() == Node.ELEMENT_NODE)
					{
						if (nn.item(j).getNodeName().equals("FrameName"))
							t.set_slideID(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("StartSecond"))
						{
							int temp = Integer.parseInt(nn.item(j).getFirstChild().getNodeValue())*1000 - localTimeZoneOffset;
							Time ti = new Time(temp);
							t.set_time(ti);
						}
						else if(nn.item(j).getNodeName().equals("Text"))
						{
							String temp = nn.item(j).getFirstChild().getNodeValue();
							String[] words = temp.split("\n");
							temp = words[0];
							for(int k = 1; k < words.length; k++)
							{
								if(words[k].length() > 0)
									temp = temp + " " + words[k];
							}
							t.set_text(temp);
						}
						else if(nn.item(j).getNodeName().equals("X"))
							t.set_left(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("Y"))
							t.set_top(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("Height"))
							t.set_height(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("Width"))
							t.set_width(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));


					}
				}
				t.set_bottom(t.get_top() + t.get_height());
				t.set_lastLineWidth(t.get_width());
				t.set_lastLineLeft(t.get_left());
				t.set_type(0);
				if(!t.get_text().contentEquals(" "))
					tll.add(t);
			}
		}

		Comparator<textLine> tlc = new Comparator<textLine>() {
			public int compare(textLine t1, textLine t2)
			{
				if(t1.get_slideID() >= t2.get_slideID())
					return 1;
				return -1;
			}
		};

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(tll, tlc);

		int currentSlideNumNew = 0;
		int currentSlideNumOriginal = 0;
		for(int i = 0; i < tll.size(); i++)
		{
			if(tll.get(i).get_slideID() > currentSlideNumOriginal)
			{
				currentSlideNumOriginal = tll.get(i).get_slideID();
				currentSlideNumNew++;
				tll.get(i).set_slideID(currentSlideNumNew);

				if(changeBBImageNames)
				{
					File original = new File(Constants.joinPath(workingFolder, lecture_id, "thumbnails", currentSlideNumOriginal + ".jpg"));
					File renamed  = new File(Constants.joinPath(workingFolder, lecture_id, "thumbnails", currentSlideNumNew + ".jpg"));
					if(!renamed.exists())
						original.renameTo(renamed);

					original = new File(Constants.joinPath(workingFolder, lecture_id, "tmp" + "BBImages", currentSlideNumOriginal + ".jpg"));
					renamed  = new File(Constants.joinPath(workingFolder, lecture_id, "tmp" + "BBImages", currentSlideNumNew + ".jpg"));
					if(!renamed.exists())
						original.renameTo(renamed);
				}
			}
			else if(tll.get(i).get_slideID() == currentSlideNumOriginal)
			{
				tll.get(i).set_slideID(currentSlideNumNew);
			}
			else
			{
				LoggerSingleton.info("Error");
				tll.remove(i);
				i--;
			}
		}
		return tll;
	}

	static ArrayList<textLine> loadFromACMXML(String lecture_id) throws ParserConfigurationException, SAXException, IOException{
		ArrayList<textLine> tll = new ArrayList<textLine>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new File(Constants.joinPath(Constants.DEFAULT_ACM_DIR, "ocr_result_xml", lecture_id + ".xml")));

		Element root = doc.getDocumentElement();
		NodeList nodes = root.getElementsByTagName("TextObject");

		ArrayList<String> pagePic = new ArrayList<String>();
		ArrayList<Integer> pageMilisec = new ArrayList<Integer>();
		File timeFile = new File(Constants.joinPath(Constants.DEFAULT_ACM_DIR, lecture_id + ".txt"));
		if(timeFile.exists())
		{
			BufferedReader br = new BufferedReader(new FileReader(timeFile));
			for(String a = br.readLine(); a != null; a = br.readLine())
			{
				String words[] = a.split("\t");
				for(int i = 0; i < words[1].length(); i++)
				{
					if(words[1].charAt(i) == '.')
					{
						pagePic.add(words[1].substring(0, i));
						pageMilisec.add( (int) (Double.parseDouble(words[2]) * 1000) - 3600000);
						break;
					}
				}
			}
			br.close();
		}

		if (nodes != null && nodes.getLength() > 0)
		{
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Element textLine = (Element) nodes.item(i);
				NodeList nn = textLine.getChildNodes();
				textLine t = new textLine();
				for (int j = 0; j < nn.getLength(); j++)
				{
					if (nn.item(j).getNodeType() == Node.ELEMENT_NODE)
					{
						if (nn.item(j).getNodeName().equals("FrameName"))
						{
							t.set_slideID(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
							for(int k = 0; k < pagePic.size(); k++)
							{
								if(pagePic.get(k).contentEquals(nn.item(j).getFirstChild().getNodeValue()))
								{
									Time ti = new Time(pageMilisec.get(k));
									t.set_time(ti);
									break;
								}
							}
						}
						else if(nn.item(j).getNodeName().equals("Text"))
						{
							String temp = nn.item(j).getFirstChild().getNodeValue();
							String[] words = temp.split("\n");
							temp = words[0];
							for(int k = 1; k < words.length; k++)
							{
								if(words[k].length() > 0)
									temp = temp + " " + words[k];
							}
							t.set_text(temp);
						}
						else if(nn.item(j).getNodeName().equals("X"))
							t.set_left(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("Y"))
							t.set_top(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("Height"))
							t.set_height(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if(nn.item(j).getNodeName().equals("Width"))
							t.set_width(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));


					}
				}
				t.set_bottom(t.get_top() + t.get_height());
				t.set_lastLineWidth(t.get_width());
				t.set_lastLineLeft(t.get_left());
				t.set_type(0);
				tll.add(t);
			}
		}

		Comparator<textLine> tlc = new Comparator<textLine>() {
			public int compare(textLine t1, textLine t2)
			{
				if(t1.get_slideID() >= t2.get_slideID())
					return 1;
				return -1;
			}
		};

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(tll, tlc);
		return tll;

	}

	static ArrayList<textLine> loadFromJSON(String workingFolder, String lecture_id) throws UnsupportedEncodingException, FileNotFoundException, IOException, ParseException {
		
		ArrayList<textLine> tll = JSONThumbnailsParser.parse(Constants.joinPath(workingFolder, lecture_id,  "thumbnails.json"));
		
		Comparator<textLine> tlc = new Comparator<textLine>() {
			public int compare(textLine t1, textLine t2)
			{
				if(t1.get_slideID() >= t2.get_slideID())
					return 1;
				return -1;
			}
		};
		
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(tll, tlc);
		
		int currentSlideNumNew = 0;
		int currentSlideNumOriginal = 0;
		for(textLine tl: tll){
			if(tl.get_slideID() > currentSlideNumOriginal){
				currentSlideNumOriginal = tl.get_slideID();
				currentSlideNumNew++;
				tl.set_slideID(currentSlideNumNew);
			} else if(tl.get_slideID() == currentSlideNumOriginal) {
				tl.set_slideID(currentSlideNumNew);
			} else {
				LoggerSingleton.error(String.format("Error with textLine %d", 
						tl.get_slideID()));
				tll.remove(tl);
			}			
		}
		
		return tll;
	}

	public static ArrayList<textLine> loadOcrResults(
			OCROriginMode OCR_Origin, 
			String workingFolder, 
			String lecture_id, 
			int localTimeZoneOffset, boolean changeBBImageNames) 
					throws SQLException, ParserConfigurationException, 
						SAXException, IOException, InstantiationException, 
						IllegalAccessException, ClassNotFoundException, ParseException
	{
		switch(OCR_Origin){
		case mySQL:
			return loadFromMySQL(lecture_id);
		case teleTaskXML:
			return loadFromTeleTaskXML(workingFolder, lecture_id, changeBBImageNames);
		case ACM_XML:
			return loadFromACMXML(lecture_id);
		case PDF:
			return new pdfParser().analyzePDF(Constants.joinPath(workingFolder, lecture_id, Constants.DEFAULT_SLIDES_PDF));
		case JSON:
			return loadFromJSON(workingFolder, lecture_id);
			
		}
		
		return new ArrayList<>();
	}

	public static ArrayList<textOutline> autoSegmentationAndAnnotation(ArrayList<textOutline> fr, String workingFolder, String lecture_id) throws IOException
	{
		ArrayList<textOutline> onlyTitles = new ArrayList<textOutline>();

		for(int i = 0; i < fr.size(); i++)
		{
			if(fr.get(i).get_child() == 0)
				onlyTitles.add(fr.get(i));
		}

		ArrayList<Integer> durationBySeconds = new ArrayList<Integer>();

		for(int i = 0; i < onlyTitles.size(); i++)
		{
			if(i == onlyTitles.size()-1)
				durationBySeconds.add(30);
			else
			{
				int t = (int) (onlyTitles.get(i+1).get_time().getTime() - onlyTitles.get(i).get_time().getTime()) / 1000;
				durationBySeconds.add(t);
			}
		}

		//int totalTime = (int) (onlyTitles.get(onlyTitles.size()-1).get_time().getTime() - onlyTitles.get(0).get_time().getTime()) / 1000 + 30;
		int totalTime = 0;
		boolean haveSegment = false;
		int count = 0;
		for(int i = 0; i < onlyTitles.size()-1; i++)
		{
			if(onlyTitles.get(i).get_hierarchy() == 0 && onlyTitles.get(i+1).get_hierarchy() > 0)
			{
				haveSegment = true;
				count++;
				int segmentDurationBySeconds = durationBySeconds.get(i);
				for(int j = i + 1; j < onlyTitles.size(); j++)
				{
					if(onlyTitles.get(j).get_hierarchy() > 0)
						segmentDurationBySeconds += durationBySeconds.get(j);
					else
					{
						onlyTitles.get(j).set_childEnd(0);
						break;
					}
				}
				onlyTitles.get(i).set_childEnd(segmentDurationBySeconds);
				totalTime += segmentDurationBySeconds;
			}
		}

		int averageSegLength = 0;
		if(haveSegment)
			averageSegLength = totalTime / count;

		if(onlyTitles.isEmpty())
			totalTime = 30;
		else
			totalTime = (int) (onlyTitles.get(onlyTitles.size()-1).get_time().getTime() - onlyTitles.get(0).get_time().getTime()) / 1000 + 30;


		if(haveSegment)
		{
			averageSegLength = Math.max(averageSegLength, onlyTitles.isEmpty() ? 0 : totalTime / onlyTitles.size() );
			
			//averageSegLength = averageSegLength > totalTime/5 ? averageSegLength : totalTime/5;
			LoggerSingleton.info(averageSegLength);
			int lastPos = -1;
			int currentPos = 0;
			int currentSum = 0;
			for(int i = 0; i < onlyTitles.size(); i++)
			{
				if(onlyTitles.get(i).get_hierarchy() > 0)
					continue;

				if(onlyTitles.get(i).get_childEnd() == 0 || i == 0)
				{
					lastPos = currentPos;
					currentPos = i;
					currentSum = durationBySeconds.get(i);
					if(i == onlyTitles.size()-1)
						onlyTitles.get(currentPos).set_childEnd(currentSum);

				}
				else if(onlyTitles.get(i).get_childEnd() < 0)
				{
					currentSum += durationBySeconds.get(i);
					if(currentSum > averageSegLength || i == onlyTitles.size()-1)
					{
						if(currentPos == 0 && durationBySeconds.get(i) >= 90)
						{
							currentSum -= durationBySeconds.get(i);
							onlyTitles.get(currentPos).set_childEnd(currentSum);
							lastPos = currentPos;
							currentPos = i;
							//i--;
						}
						else
						{
							onlyTitles.get(currentPos).set_childEnd(currentSum);
							lastPos = currentPos;
							currentPos = i + 1;
						}
						currentSum = 0;
					}
				}
				else
				{
					if(currentSum > averageSegLength/3)
					{
						onlyTitles.get(currentPos).set_childEnd(currentSum);
						lastPos = currentPos;
						currentPos = i;
						currentSum = 0;
					}
					else
					{
						onlyTitles.get(lastPos).set_childEnd(currentSum + onlyTitles.get(lastPos).get_childEnd());
						if(onlyTitles.get(currentPos).get_childEnd() == 0)
							onlyTitles.get(currentPos).set_childEnd(-1);
						currentPos = i;
						currentSum = 0;
					}
				}
			}
		}
		else
		{
			// !!!! INF loop, if first duration is longer, than the averageSegLength
			averageSegLength = Math.max(180, onlyTitles.isEmpty() ? 0 : totalTime / onlyTitles.size() );
			LoggerSingleton.info(averageSegLength);
			int currentPos = 0;
			int currentSum = 0;
			for(int i = 0; i < onlyTitles.size(); i++)
			{
				if(i == onlyTitles.size()-1)
				{
					currentSum += durationBySeconds.get(i);
					onlyTitles.get(currentPos).set_childEnd(currentSum);
				}
				else
				{
					currentSum += durationBySeconds.get(i);
					if(currentSum > averageSegLength)
					{
						if(currentPos == 0 && durationBySeconds.get(i) >= 90)
						{
							currentSum -= durationBySeconds.get(i);
							onlyTitles.get(currentPos).set_childEnd(currentSum);
							currentPos = i;
//							i--;
						}
						else
						{
							onlyTitles.get(currentPos).set_childEnd(currentSum);
							currentPos = i + 1;
						}
						currentSum = 0;
					}
				}
			}
		}

		count = 0;
		int sum = 0;
		File newFile = new File(Constants.joinPath(workingFolder, lecture_id, "seg"));
		if(newFile.exists())
		{
			newFile.delete();
		}
		for(int i = 0; i < onlyTitles.size(); i++)
		{
			algorithmInterface ai = new algorithmInterface();

			if(onlyTitles.get(i).get_childEnd() >= 0)
			{
				int longestPos = i, longestTime = durationBySeconds.get(i);
				if( i < onlyTitles.size() - 1 && onlyTitles.get(i+1).get_hierarchy() == 0)
					for(int j = i + 1; j < onlyTitles.size() && onlyTitles.get(j).get_childEnd() < 0 ; j++)
					{
						if(durationBySeconds.get(j) > longestTime)
						{
							longestTime = durationBySeconds.get(j);
							longestPos = j;
						}
					}
				LoggerSingleton.info("< Segment '" + onlyTitles.get(longestPos).get_text() + "' > Duration: " + ai.secondsToTime(onlyTitles.get(i).get_childEnd()));

				BufferedWriter output = new BufferedWriter(new FileWriter(newFile, true));
				output.append(onlyTitles.get(i).get_time().toString() + "\t");
				if(i < onlyTitles.size() - 1)
				{
					if(onlyTitles.get(i+1).get_hierarchy() == 0)
						output.append("<casual>");
					else
						output.append("<logical>");
				}
				else
					output.append("<ending>");
				output.append("\t" + onlyTitles.get(longestPos).get_text());
				output.newLine();
				output.close();


				count++;
				sum += onlyTitles.get(i).get_childEnd();
			}

			for(int j = 0; j <= onlyTitles.get(i).get_hierarchy(); j++)
				LoggerSingleton.info("--");


			LoggerSingleton.info(onlyTitles.get(i).get_text() + " " + onlyTitles.get(i).get_time());
			if(i == onlyTitles.size() - 1)
				LoggerSingleton.info('\n' + "Average Segment-Length: " + ai.secondsToTime(sum/count));
		}


		return onlyTitles;
	}

}
