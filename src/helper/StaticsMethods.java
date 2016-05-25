package helper;

import java.io.File;
import java.util.ArrayList;

import com.google.common.base.Joiner;

public abstract class StaticsMethods {

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
}
