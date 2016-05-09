package helper;

import java.io.File;

import com.google.common.base.Joiner;

public final class Constants {

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
}
