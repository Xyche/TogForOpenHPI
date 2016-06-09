package helper.enums;

public enum TextLineType {
	
	/** -3 */
	INVALID(-3),
	/** -2 too high text, maybe a combination of several lines, used only in helping locating the title candidates. */
	TOO_HIGH(-2),
	/** -1 text cannot be recognized, used only in locating title candidates and calculating the average height. */
	//  *** these 3 kind above will never be included in the text system, and will be definitely deleted.

	CANNOT_RECOGNIZE(-1),
	/** 0 common text in OCR.*/
	COMMON_TEXT(0),
	/** 1 'Title' in OCR*/
	TITLE(1),
	/** 2 'Subtitle' in OCR*/
	SUBTITLE(2),
	/** 3 'Footline' in OCR*/
	FOOTLINE(3);

	int value;
	private TextLineType(int val){ value = val; }

	/** <= -2 */
	public boolean isInvalid(){
		return this.value <= -2;
	}

	/** < 0 */
	public boolean isNotCommon(){
		return this.value < 0;
	}

	/** >= 0 */
	public boolean isCommon(){
		return this.value >= 0;
	}
	
	public int getValue(){
		return value;
	}
	
	public static TextLineType fromInt(int value){
		switch(value){
			case -2: return TOO_HIGH;
			case -1: return CANNOT_RECOGNIZE;
			case 0:  return COMMON_TEXT;
			case 1:  return TITLE;
			case 2:  return SUBTITLE;
			case 3:  return FOOTLINE;
			default: return INVALID;
		}
	}
}
