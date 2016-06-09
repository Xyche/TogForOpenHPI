package helper.enums;

public enum OCROriginMode {
	/*
	 * Mode 0: Load from mySQL database 
	 * Mode 1: Load from xml files of teleTASK
	 * Mode 2: Load from xml files of ACM GC 
	 * Mode 3: Load from additional PDF
	 * file
	 */
	mySQL, teleTaskXML, ACM_XML, PDF, JSON;
	
	public static final OCROriginMode DEFAULT_MODE = JSON;

	public static String description(OCROriginMode mode) {
		String desc = "%d";
		switch (mode) {
		case mySQL:
			desc = "%d - Load from mySQL database";
			break;
		case ACM_XML:
			desc =  "%d - Load from xml files of ACM GC";
			break;
		case PDF:
			desc =  "%d - Load from additional PDF file";
			break;
		case teleTaskXML:
			desc =  "%d - Load from xml files of teleTASK";
			break;
		case JSON:
			desc = "%d - Load from JSON file";
			break;
		}
		return String.format(desc, mode.ordinal()) + (DEFAULT_MODE == mode ? " (default)" : "");
	}

	public static OCROriginMode parseFromOption(String optionValue) {
		return OCROriginMode.values()[Integer.valueOf(optionValue)];
	}
}
