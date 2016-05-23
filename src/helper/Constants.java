package helper;

import java.io.File;

import com.google.common.base.Joiner;

public final class Constants {
	
	public static final int DEFAULT_WIDTH = 1024, DEFAULT_HEIGHT = 768, MIN_DURATION = 30, MAX_AVG_DURATION = 180;
	public static final int GAP_MARGIN = 3;
	

	public static final String DEFAULT_SLIDES_PPTX = "slides.pptx";
	public static final String DEFAULT_SLIDES_PDF = "slides.pdf";

	public static final String DEFAULT_WORKING_DIR = "C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\_OCR_result\\";
	public static final String DEFAULT_TESSERACT_FOLDER = "C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\table\\Tesseract";
	public static final String DEFAULT_TOG_FOLDER = "C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\table\\TOG\\";
	public static final String DEFAULT_TOG_STATS = DEFAULT_TOG_FOLDER + "\\stats.txt";
	public static final String DEFAULT_ACM_DIR = "C:\\_HPI-tasks\\20130101_TreeOutlineGeneration\\ACM2013\\Dataset_public_OCR_files\\";
	
	public static String joinPath(String... parts){
		return Joiner.on(File.separator).join(parts);		
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
