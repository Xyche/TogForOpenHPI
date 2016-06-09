package helper.enums;

public enum SlidePageType {

	/**
	 * -3
	 * Absolutely empty slide
	 * */
	EMPTY(-3),
	/**
	 * -2
	 * There's only a title or a unique text in this slide, maybe this is a picture slide.
	 * */
	POSSIBLY_PICTURE_SLIDE(-2),
	/**
	 * -1
	 * Most of the texts in this slide haven't been organized
	 * */
	MOST_TEXT_UNORGANIZED(-1),
	/**
	 * 0
	 * Common well-organized slide
	 * */
	WELL_ORGANAZIED(0),
	/**
	 * 1
	 * Index slide
	 * */
	INDEX_SLIDE(1),
	/**
	 * 2
	 * Tag slide
	 * */
	TAG_SLIDE(2),
	/**
	 * 3
	 * Split slide
	 * */
	SPLIT_SLIDE(3);
	
	int value;
	private SlidePageType(int val){ value = val; }
	
	public boolean gte(int other){
		return value >= other;
	}
	public boolean gt(int other){
		return value > other;
	}
	public boolean lte(int other){
		return value <= other;
	}
	public boolean lt(int other){
		return value < other;
	}
	


	public boolean gte(SlidePageType other){
		return gte(other.value);
	}
	public boolean gt(SlidePageType other){
		return gt(other.value);
	}
	public boolean lt(SlidePageType other) {
		return lt(other.value);
	}
	public boolean lte(SlidePageType other){
		return lte(other.value);
	}


	/** =< -3*/
	public boolean isEmpty(){
		return this.lte(EMPTY);
	}

	/** > -3*/
	public boolean isNotEmpty(){
		return this.gt(EMPTY);
	}

	/** <= 0*/
	public boolean isNotSpecial(){
		return this.lte(WELL_ORGANAZIED);
	}
	
	/** <= 2*/
	public boolean isNotSplitSlide(){
		return this.lte(SPLIT_SLIDE);
	}
	
	
	/** >= 0*/
	public boolean isCommon(){
		return this.gte(WELL_ORGANAZIED);
	}
	
	/** < 0*/
	public boolean isNotCommon(){
		return this.lt(WELL_ORGANAZIED);
	}

	public int getValue() {
		return this.value;
	}

}
