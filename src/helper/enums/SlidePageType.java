package helper.enums;

/* 'pageType' will be explained below:
 * -3: Absolutely empty slide.
 * -2: There's only a title or a unique text in this slide, maybe this is a picture slide.
 * -1: Most of the texts in this slide haven't been organized.
 *  0: Common well-organized slide
 *  1: Index slide
 *  2: Tag slide
 *  3: Split slide
 */
public enum SlidePageType {

	EMPTY(-3),
	POSSIBLY_PICTURE_SLIDE(-2),
	MOST_TEXT_UNORGANIZED(-1),
	WELL_ORGANAZIED(0),
	INDEX_SLIDE(1),
	TAG_SLIDE(2),
	SPLIT_SLIDE(3);
	
	int value;
	private SlidePageType(int val){ value = val; }
}
