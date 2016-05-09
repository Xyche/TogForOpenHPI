package helper;
import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

public class LoggerSingleton{

	private static Logger m_log = Logger.getLogger(LoggerSingleton.class);

	public static void setUp(String fileName) throws SecurityException, IOException{
		m_log.removeAllAppenders();
		System.out.println(String.format("Logs are written to %s", fileName));
		m_log.addAppender(new FileAppender(new SimpleLayout(), fileName));
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
