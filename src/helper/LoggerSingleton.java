package helper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.google.common.base.Joiner;

import dataStructure.slidePage;
import dataStructure.textOutline;

public class LoggerSingleton{

	private static Logger m_log = Logger.getLogger(LoggerSingleton.class);

	public static void setUp(String fileName) throws SecurityException, IOException{
		m_log.removeAllAppenders();
		System.out.println(String.format("Logs are written to %s", fileName));
		m_log.addAppender(new FileAppender(new SimpleLayout(), fileName, false));
	}

	public static void show(String text, List<slidePage> sps){
		LoggerSingleton.info(text);
		for(slidePage page: sps){
			LoggerSingleton.info(String.format("%d %s %s [%d, %d, %d, %d]", page.get_PageNum(),
					page.get_startTime().toString(), page.get_title(), page.get_titleLocation()[0], page.get_titleLocation()[2],
					(page.get_titleLocation()[1] - page.get_titleLocation()[0]),
					(page.get_titleLocation()[3] - page.get_titleLocation()[2])));

			for(textOutline to: page.get_texts()){
				String indent = "";
				if (to.get_hierarchy() == 1)
					indent = "--";
				else if (to.get_hierarchy() == 2)
					indent = "----";
				else if (to.get_hierarchy() == 3)
					indent = "------";
				LoggerSingleton.info(indent + to.get_text() + " -> ( " + to.get_child() + ", " + to.get_childEnd() + " ) " + to.get_time());
			}

			LoggerSingleton.info("PageType: " + page.get_pageType());
			if (page.get_pageType().isCommon())
				LoggerSingleton.info("3 Levels: " + page.get_levelCoordinates()[0] + ", " + page.get_levelCoordinates()[1]
						+ ", " + page.get_levelCoordinates()[2]);
		}
		LoggerSingleton.info("-------------------------------------------------------");
		
	}
	
	public static void show(String text, ArrayList<ArrayList<Integer>> samePageGroups){
		ArrayList<String> groups = new ArrayList<>();
		for (ArrayList<Integer> group : samePageGroups)
			groups.add(Joiner.on(" ").join(group));
		LoggerSingleton.info(String.format("Same Page Group (%s)\n", Joiner.on("\n").join(groups)));
	}
	
	public static void info(Object obj){ m_log.info(obj); }
	public static void debug(Object obj){ m_log.debug(obj); }
	public static void error(Object obj){ m_log.error(obj); }
	public static void warn(Object obj){ m_log.warn(obj); }
	public static void info(){ }
	public static void debug(){ }
	public static void error(){ }
	public static void warn(){ }
}
