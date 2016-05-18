package sharedMethods;

import java.sql.Time;
import java.util.TimeZone;

public class TimeProblem {
	
	public TimeProblem() {}
	
	public String toStringFromSeconds(int seconds)
	{
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
	
	public int toSecondsFromString(String s)
	{
		String[] parts = s.split(":");
		int seconds = 0;
		
		if(parts.length != 3)
			return 0;
		
		seconds = Integer.parseInt(parts[0]) * 3600;
		seconds += Integer.parseInt(parts[1]) * 60;
		seconds += Integer.parseInt(parts[2]);
		
		return seconds;
	}

	public Time parseJsonTimeTag(String tag) {

		Time time = new Time(0);
		
		long hours = Long.parseLong(tag.split(":")[0].trim());		 
        long minutes = Long.parseLong(tag.split(":")[1].trim()); 
        long seconds = Long.parseLong(tag.split(":")[2].trim());
        
        long localTimeZoneOffset = TimeZone.getDefault().getRawOffset();
        long timeByMillisecond = (hours * 3600 + minutes * 60 + seconds) * 1000 - localTimeZoneOffset;
        time.setTime(timeByMillisecond);
		//System.out.println(tag + ": " + hours + '\t' + minutes + '\t' + seconds + '\t' + millies);
		return time;
	}

}
