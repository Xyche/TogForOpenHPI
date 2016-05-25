package helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import dataStructure.pdfParser;
import dataStructure.textLine;

public class OCRLoader {
	static ArrayList<textLine> loadFromMySQL(String lecture_id)
			throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		ArrayList<textLine> tll = new ArrayList<textLine>();
		ArrayList<textLine> tl2 = new ArrayList<textLine>();

		String url = "jdbc:mysql://localhost/ak?user=Xyche&password=123&useUnicode=true&characterEncoding=8859_1";
		String sql = "select t.*, l.start from ocrtool_textline as t, ocrtool_lectureslide as l where t.lectureSlide_id = l.id and l.lecture_id = "
				+ lecture_id + " order by id";
		Class.forName("com.mysql.jdbc.Driver").newInstance();

		Connection connection = DriverManager.getConnection(url);
		PreparedStatement ps = null;
		ps = connection.prepareStatement(sql);
		ResultSet rs1 = ps.executeQuery();

		int slideNumBase = 0;
		int currentSlide = 0;

		// Load data from database and set them into textLine structure
		// Then change the order from descend to ascend by id inside each slide

		while (rs1.next()) {
			int tempSlideID = rs1.getInt("lectureSlide_id");
			if (slideNumBase == 0)
				slideNumBase = tempSlideID;
			tempSlideID -= slideNumBase;

			int intType = -1;
			String temp = rs1.getString("type");
			if (temp.contentEquals("Title"))
				intType = 1;
			else if (temp.contentEquals("Subtitle"))
				intType = 2;
			else if (temp.contentEquals("Footline"))
				intType = 3;
			else
				intType = 0;

			textLine t = new textLine(tempSlideID + 1, rs1.getString("content"), intType, rs1.getInt("top"),
					rs1.getInt("left"), rs1.getInt("width"), rs1.getInt("height"), rs1.getTime("start"));

			if (tempSlideID + 1 > currentSlide) {
				if (!tl2.isEmpty()) {
					for (int i = tl2.size() - 1; i >= 0; i--) {
						tll.add(tl2.get(i));
					}
				}
				tl2.clear();
				currentSlide = tempSlideID + 1;
			}

			tl2.add(t);
		}

		if (!tl2.isEmpty()) {
			for (int i = tl2.size() - 1; i >= 0; i--) {
				tll.add(tl2.get(i));
			}
		}

		tl2.clear();
		rs1.close();
		ps.close();
		connection.close();

		return tll;
	}

	static ArrayList<textLine> loadFromTeleTaskXML(String workingFolder, String lecture_id, boolean changeBBImageNames)
			throws ParserConfigurationException, SAXException, IOException {

		ArrayList<textLine> tll = new ArrayList<textLine>();

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder
				.parse(new File(StaticsMethods.joinPath(workingFolder, lecture_id, "recognition", "recognition.xml")));

		Element root = doc.getDocumentElement();
		NodeList nodes = root.getElementsByTagName("TextObject");

		if (nodes != null && nodes.getLength() > 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				Element textLine = (Element) nodes.item(i);
				NodeList nn = textLine.getChildNodes();
				textLine t = new textLine();
				for (int j = 0; j < nn.getLength(); j++) {
					if (nn.item(j).getNodeType() == Node.ELEMENT_NODE) {
						if (nn.item(j).getNodeName().equals("FrameName"))
							t.set_slideID(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("StartSecond")) {
							int temp = Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()) * 1000
									- TimeZone.getDefault().getRawOffset();
							Time ti = new Time(temp);
							t.set_time(ti);
						} else if (nn.item(j).getNodeName().equals("Text")) {
							String temp = nn.item(j).getFirstChild().getNodeValue();
							String[] words = temp.split("\n");
							temp = words[0];
							for (int k = 1; k < words.length; k++) {
								if (words[k].length() > 0)
									temp = temp + " " + words[k];
							}
							t.set_text(temp);
						} else if (nn.item(j).getNodeName().equals("X"))
							t.set_left(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("Y"))
							t.set_top(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("Height"))
							t.set_height(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("Width"))
							t.set_width(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));

					}
				}
				t.set_bottom(t.get_top() + t.get_height());
				t.set_lastLineWidth(t.get_width());
				t.set_lastLineLeft(t.get_left());
				t.set_type(0);
				if (!t.get_text().contentEquals(" "))
					tll.add(t);
			}
		}

		Comparator<textLine> tlc = new Comparator<textLine>() {
			public int compare(textLine t1, textLine t2) {
				if (t1.get_slideID() >= t2.get_slideID())
					return 1;
				return -1;
			}
		};

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(tll, tlc);

		int currentSlideNumNew = 0;
		int currentSlideNumOriginal = 0;
		for (int i = 0; i < tll.size(); i++) {
			if (tll.get(i).get_slideID() > currentSlideNumOriginal) {
				currentSlideNumOriginal = tll.get(i).get_slideID();
				currentSlideNumNew++;
				tll.get(i).set_slideID(currentSlideNumNew);

				if (changeBBImageNames) {
					File original = new File(StaticsMethods.joinPath(workingFolder, lecture_id, "thumbnails",
							currentSlideNumOriginal + ".jpg"));
					File renamed = new File(
							StaticsMethods.joinPath(workingFolder, lecture_id, "thumbnails", currentSlideNumNew + ".jpg"));
					if (!renamed.exists())
						original.renameTo(renamed);

					original = new File(StaticsMethods.joinPath(workingFolder, lecture_id, "tmp" + "BBImages",
							currentSlideNumOriginal + ".jpg"));
					renamed = new File(StaticsMethods.joinPath(workingFolder, lecture_id, "tmp" + "BBImages",
							currentSlideNumNew + ".jpg"));
					if (!renamed.exists())
						original.renameTo(renamed);
				}
			} else if (tll.get(i).get_slideID() == currentSlideNumOriginal) {
				tll.get(i).set_slideID(currentSlideNumNew);
			} else {
				LoggerSingleton.info("Error");
				tll.remove(i);
				i--;
			}
		}
		return tll;
	}

	static ArrayList<textLine> loadFromACMXML(String lecture_id)
			throws ParserConfigurationException, SAXException, IOException {
		ArrayList<textLine> tll = new ArrayList<textLine>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder
				.parse(new File(StaticsMethods.joinPath(Constants.DEFAULT_ACM_DIR, "ocr_result_xml", lecture_id + ".xml")));

		Element root = doc.getDocumentElement();
		NodeList nodes = root.getElementsByTagName("TextObject");

		ArrayList<String> pagePic = new ArrayList<String>();
		ArrayList<Integer> pageMilisec = new ArrayList<Integer>();
		File timeFile = new File(StaticsMethods.joinPath(Constants.DEFAULT_ACM_DIR, lecture_id + ".txt"));
		if (timeFile.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(timeFile));
			for (String a = br.readLine(); a != null; a = br.readLine()) {
				String words[] = a.split("\t");
				for (int i = 0; i < words[1].length(); i++) {
					if (words[1].charAt(i) == '.') {
						pagePic.add(words[1].substring(0, i));
						pageMilisec.add((int) (Double.parseDouble(words[2]) * 1000) - 3600000);
						break;
					}
				}
			}
			br.close();
		}

		if (nodes != null && nodes.getLength() > 0) {
			for (int i = 0; i < nodes.getLength(); i++) {
				Element textLine = (Element) nodes.item(i);
				NodeList nn = textLine.getChildNodes();
				textLine t = new textLine();
				for (int j = 0; j < nn.getLength(); j++) {
					if (nn.item(j).getNodeType() == Node.ELEMENT_NODE) {
						if (nn.item(j).getNodeName().equals("FrameName")) {
							t.set_slideID(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
							for (int k = 0; k < pagePic.size(); k++) {
								if (pagePic.get(k).contentEquals(nn.item(j).getFirstChild().getNodeValue())) {
									Time ti = new Time(pageMilisec.get(k));
									t.set_time(ti);
									break;
								}
							}
						} else if (nn.item(j).getNodeName().equals("Text")) {
							String temp = nn.item(j).getFirstChild().getNodeValue();
							String[] words = temp.split("\n");
							temp = words[0];
							for (int k = 1; k < words.length; k++) {
								if (words[k].length() > 0)
									temp = temp + " " + words[k];
							}
							t.set_text(temp);
						} else if (nn.item(j).getNodeName().equals("X"))
							t.set_left(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("Y"))
							t.set_top(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("Height"))
							t.set_height(Integer.parseInt(nn.item(j).getFirstChild().getNodeValue()));
						else if (nn.item(j).getNodeName().equals("Width"))
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
			public int compare(textLine t1, textLine t2) {
				if (t1.get_slideID() >= t2.get_slideID())
					return 1;
				return -1;
			}
		};

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(tll, tlc);
		return tll;

	}

	static ArrayList<textLine> loadFromJSON(String workingFolder, String lecture_id)
			throws UnsupportedEncodingException, FileNotFoundException, IOException, ParseException {

		ArrayList<textLine> tll = JSONThumbnailsParser
				.parse(StaticsMethods.joinPath(workingFolder, lecture_id, "thumbnails.json"));

		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		Collections.sort(tll, new Comparator<textLine>() {
			public int compare(textLine t1, textLine t2) {
				if (t1.get_slideID() >= t2.get_slideID())
					return 1;
				return -1;
			}
		});

		int currentSlideNumNew = 0;
		int currentSlideNumOriginal = 0;
		for (textLine tl : tll) {
			if (tl.get_slideID() > currentSlideNumOriginal) {
				currentSlideNumOriginal = tl.get_slideID();
				currentSlideNumNew++;
				tl.set_slideID(currentSlideNumNew);
			} else if (tl.get_slideID() == currentSlideNumOriginal) {
				tl.set_slideID(currentSlideNumNew);
			} else {
				LoggerSingleton.error(String.format("Error with textLine %d", tl.get_slideID()));
				tll.remove(tl);
			}
		}

		return tll;
	}

	public static ArrayList<textLine> loadOcrResults(OCROriginMode OCR_Origin, String workingFolder, String lecture_id,
			int localTimeZoneOffset, boolean changeBBImageNames)
					throws SQLException, ParserConfigurationException, SAXException, IOException,
					InstantiationException, IllegalAccessException, ClassNotFoundException, ParseException {
		switch (OCR_Origin) {
		case mySQL:
			return loadFromMySQL(lecture_id);
		case teleTaskXML:
			return loadFromTeleTaskXML(workingFolder, lecture_id, changeBBImageNames);
		case ACM_XML:
			return loadFromACMXML(lecture_id);
		case PDF:
			return new pdfParser()
					.analyzePDF(StaticsMethods.joinPath(workingFolder, lecture_id, Constants.DEFAULT_SLIDES_PDF));
		case JSON:
			return loadFromJSON(workingFolder, lecture_id);

		}

		return new ArrayList<>();
	} 
}
