package helper;

import java.io.File;
import java.sql.Time;
import java.util.ArrayList;
import java.util.TimeZone;

import com.google.common.base.Joiner;

public abstract class StaticMethods {

	public static String joinPath(String... parts){
		return Joiner.on(File.separator).join(parts);		
	}
	
	public static <T> ArrayList<T> notNullObjects(ArrayList<T> list){
		ArrayList<T> result = new ArrayList<>();
		for(T obj: list) if(obj != null) result.add(obj);
		return result;
	}

	public static boolean isInSimilarPosition(int pos1, int pos2, int length1, int length2) {
		int maxLength = Math.max(length1, length2);

		if (maxLength < 4)
			return true;

		if (Math.abs(pos1 - pos2) <= maxLength / 3)
			return true;

		if (Math.abs(length1 - pos1 - length2 + pos2) <= maxLength / 3)
			return true;

		if (Math.abs((double) pos1 / (double) length1 - (double) pos2 / (double) length2) <= 0.25)
			return true;

		LoggerSingleton.info("Ignore remote perfectly-matching pair: video-" + (pos2 + 1) + ", file-" + (pos1 + 1));

		return false;
	}
	
	

	// Time parsing

	public static String secondsToTime(int seconds) {

		String result = "";
		int h = seconds / 3600;
		int m = ( seconds % 3600 ) / 60;
		int s = seconds % 60;
		
		if(h > 24)
			return "Too long";
		
		result = h < 10 ? "0" + Integer.toString(h) : Integer.toString(h);
		result += ":" + ( m < 10 ? "0" + Integer.toString(m) : Integer.toString(m) );
		result += ":" + ( s < 10 ? "0" + Integer.toString(s) : Integer.toString(s) );
		
		return result;
	}

	public static int timeToSeconds(String s) {

		String[] parts = s.split(":");
		int seconds = 0;
		
		if(parts.length != 3)
			return 0;
		
		seconds = Integer.parseInt(parts[0]) * 3600;
		seconds += Integer.parseInt(parts[1]) * 60;
		seconds += Integer.parseInt(parts[2]);
		
		return seconds;
	}
	
	public static Time parseJsonTimeTag(String tag) {

		Time time = new Time(0);
		
		long hours = Long.parseLong(tag.split(":")[0].trim());		 
        long minutes = Long.parseLong(tag.split(":")[1].trim()); 
        long seconds = Long.parseLong(tag.split(":")[2].trim());
        
        long localTimeZoneOffset = TimeZone.getDefault().getRawOffset();
        long timeByMillisecond = (hours * 3600 + minutes * 60 + seconds) * 1000 - localTimeZoneOffset;
        time.setTime(timeByMillisecond);
//		LoggerSingleton.info(tag + ": " + hours + '\t' + minutes + '\t' + seconds + '\t' + timeByMillisecond);
		return time;
	}
	
}
