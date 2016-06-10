import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.cli.*;

import com.google.common.base.Joiner;

import dataStructure.*;
import helper.ArgumentParser;
import helper.Constants;
import helper.FilterableList;
import helper.LoggerSingleton;
import helper.OCRLoader;
import helper.StaticMethods;
import helper.enums.OCROriginMode;

public class runTogForOpenHPI {

	private static final int localTimeZoneOffset = TimeZone.getDefault().getRawOffset();

	public static void main(String[] args) throws Exception {
		Options opt = ArgumentParser.defineCLI();
		CommandLine cmd = ArgumentParser.parseCLI(opt, args);
		if (cmd == null)
			return;

		final boolean changeBBImageNames = cmd.hasOption(ArgumentParser.CHANGE_NAMES_KEY);
		String modeString = cmd.getOptionValue(ArgumentParser.MODE_KEY,
				String.valueOf(OCROriginMode.DEFAULT_MODE.ordinal()));
		final OCROriginMode OCR_Origin = OCROriginMode.parseFromOption(modeString);

		final int pageWidth = ArgumentParser.width(cmd), pageHeight = ArgumentParser.height(cmd);
		
		boolean havePDF = cmd.hasOption(ArgumentParser.PDF_KEY);
		boolean havePPTX = cmd.hasOption(ArgumentParser.PPTX_KEY);
		if (havePDF == havePPTX) {
			havePDF = true;
			havePPTX = false;
		}
		final Path slidesPath = Paths.get(cmd.getOptionValue(ArgumentParser.SLIDES_KEY)).toAbsolutePath();
		final String lecture_id = cmd.getOptionValue(ArgumentParser.LECTURE_KEY, slidesPath.getFileName().toString());
		final String workingFolder = cmd.getOptionValue(ArgumentParser.FOLDER_KEY, Paths.get(".").toAbsolutePath().toString());
		
		LoggerSingleton.setUp(cmd.getOptionValue(ArgumentParser.LOGGER_KEY,
				StaticMethods.joinPath(workingFolder, lecture_id + ".log")));

		FilterableList<textLine> tll = OCRLoader.loadOcrResults(OCR_Origin, workingFolder, lecture_id, localTimeZoneOffset, changeBBImageNames);

		LoggerSingleton.info("< STEP 1: Loading from Database >");
		for (textLine t: tll)
			LoggerSingleton.info(Joiner.on(" ")
					.join(new String[] { String.valueOf(t.get_slideID()), String.valueOf(t.get_type()),
							String.valueOf(t.get_text()), String.valueOf(t.get_top()), String.valueOf(t.get_left()),
							String.valueOf(t.get_width()), String.valueOf(t.get_height()),
							String.valueOf(t.get_time()) }));
		// LoggerSingleton.info("$$$$\tOCR text-lines:\t" + tll.size());

		outlineGenerator og = new outlineGenerator(pageWidth, pageHeight, workingFolder, lecture_id);
		
		ArrayList<textOutline> finalResults = og.generate(tll, havePPTX, havePDF, slidesPath.toString(), changeBBImageNames);

		og.set_topicParams(textOutline.count(finalResults));

		// To do: a self-check system for adaptive round

		int roundNum = 0;
		while (true) {
			LoggerSingleton.info("------------------ADAPTIVE ROUND " + ++roundNum + "---------------------------");
			// Recode the adaptive status in the last round, for future comparison
			
			og.copyStateForAdaptiveRound();

			// Do the adaptive round

			tll = OCRLoader.loadOcrResults(OCR_Origin, workingFolder, lecture_id, localTimeZoneOffset, changeBBImageNames);
			finalResults = og.generate(tll, havePPTX, havePDF, slidesPath.toString(), changeBBImageNames);

			// Update the status of "LowCaseStart" and "HavingDot" for this
			// round
			og.set_topicParams(textOutline.count(finalResults));

			if(!og.chechAdaptiveDifference() || ++roundNum > 3) break;
		}
		
		/* show result */
		LoggerSingleton.info("< Final Results: >");

		File outlineFile = new File(StaticMethods.joinPath(workingFolder, "outline"));
		if (outlineFile.exists()) outlineFile.delete();

		BufferedWriter outlineOutput = new BufferedWriter(new FileWriter(outlineFile));
		for (textOutline text : finalResults) {
			outlineOutput.append(Joiner.on("\t").join(new Object[]{text.get_hierarchy(), text.get_child(), text.get_text()}));
			outlineOutput.newLine();

			// Control of hierarchy inside single page
			if (text.get_child() > 2) continue;

			// Control of total hierarchy in the presentation
			if (text.get_hierarchy() > 3) continue;

			if (text.get_child() == 0)
				LoggerSingleton.info("$$ ");
			LoggerSingleton.info(text.get_text() + " " + text.get_time());
		}
		outlineOutput.close();

		/*
		 * Until now, the content structure generation process has been
		 * finished! Below there will be some further methods towards
		 * segmentation & annotation Especially for ACM Multimedia 2013 Grand
		 * Challenge.
		 */

		LoggerSingleton.info("-------------------------------------------------------");

		/* autoSegmentationAndAnnotation */

		ArrayList<textOutline> onlyTitles = new ArrayList<textOutline>();
		for (textOutline text: finalResults) 
			if (text.get_child() == 0) 
				onlyTitles.add(text);

		ArrayList<Integer> durationBySeconds = new ArrayList<Integer>();

		for (int i = 0; i < onlyTitles.size() - 1; i++) {
			Long t = onlyTitles.get(i + 1).get_time().getTime() - onlyTitles.get(i).get_time().getTime();
			durationBySeconds.add(t.intValue() / 1000);
		}
		durationBySeconds.add(Constants.MIN_DURATION);

		int totalTime = 0, count = 0;
		boolean haveSegment = false;
		
		for (int i = 0; i < onlyTitles.size() - 1; i++) {
			textOutline current = onlyTitles.get(i), next = onlyTitles.get(i + 1);
			if (current.get_hierarchy() == 0 && next.get_hierarchy() > 0) {
				count++;
				haveSegment = true;
				int segmentDurationBySeconds = durationBySeconds.get(i);
				for (int j = i + 1; j < onlyTitles.size(); j++) {
					textOutline innerCurrent = onlyTitles.get(j); 
					if (innerCurrent.get_hierarchy() > 0)
						segmentDurationBySeconds += durationBySeconds.get(j);
					else {
						innerCurrent.set_childEnd(0);
						break;
					}
				}
				current.set_childEnd(segmentDurationBySeconds);
				totalTime += segmentDurationBySeconds;
			}
		}

		int averageSegLength = 0;
		if (haveSegment) averageSegLength = totalTime / count;

		if (onlyTitles.isEmpty())
			totalTime = Constants.MIN_DURATION;
		else {
			textOutline first = onlyTitles.get(0), last = onlyTitles.get(onlyTitles.size() - 1);
			Long time = (last.get_time().getTime() - first.get_time().getTime()) / 1000 + Constants.MIN_DURATION;
			totalTime = time.intValue();
		}

		int lastPos = 0, currentPos = 0, currentSum = 0;

		averageSegLength = Math.max(haveSegment ? averageSegLength : Constants.MAX_AVG_DURATION, onlyTitles.isEmpty() ? 0 : totalTime / onlyTitles.size());
		LoggerSingleton.info(averageSegLength);
		
		if(!onlyTitles.isEmpty()){
			for(Integer duration: durationBySeconds) {
				int i = durationBySeconds.indexOf(duration);
				currentSum += duration;
				
				boolean isLast = i == durationBySeconds.size() - 1;
				textOutline title = onlyTitles.get(i), currentTitle = onlyTitles.get(currentPos);
				
				int bias = 0;
				if(haveSegment){
					if (title.get_hierarchy() > 0) continue;
					
					if (title.get_childEnd() == 0 || i == 0) {
						lastPos = currentPos = i;
						if (isLast) 
							title.set_childEnd(currentSum);
						continue;
						
					} else if (title.get_childEnd() < 0) {
						if (currentSum > averageSegLength || isLast) {
							lastPos = currentPos;
							
							if (currentPos == 0 && duration >= Constants.MAX_AVG_DURATION / 2)
								currentSum -= duration;
							else
								bias = 1;
							
							currentTitle.set_childEnd(currentSum);
							currentPos = i + bias;
							currentSum = 0;
						}
					} else {
						if (currentSum > averageSegLength * 0.3) {
							currentTitle.set_childEnd(currentSum);
							lastPos = currentPos;
						} else {
							textOutline lastTitle = onlyTitles.get(lastPos); 
							lastTitle.set_childEnd(currentSum + lastTitle.get_childEnd());
							if (currentTitle.get_childEnd() == 0)
								currentTitle.set_childEnd(-1);
						}
						currentPos = i + bias;
						currentSum = 0;
					}
				} else {
					if (isLast)
						currentTitle.set_childEnd(currentSum);
					else
						if (currentSum > averageSegLength) {
							
							if (currentPos == 0 && duration >= Constants.MAX_AVG_DURATION / 2)
								currentSum -= duration;
							else
								bias = 1;
							
							currentTitle.set_childEnd(currentSum);
							currentPos = i + bias;
							currentSum = 0;
						}
					
				}
			}
		
		}
		count = 0;
		int sum = 0;
		File segmentsFile = new File(StaticMethods.joinPath(workingFolder, "seg"));
		if (segmentsFile.exists()) segmentsFile.delete();

		BufferedWriter segmentsOutput = new BufferedWriter(new FileWriter(segmentsFile));		
		for (textOutline title: onlyTitles){
			final int i = onlyTitles.indexOf(title), currentDuration = durationBySeconds.get(i);
			final boolean isLast = i == onlyTitles.size() - 1;
			final textOutline nextTitle = isLast ? null : onlyTitles.get(i + 1);
			
			if (title.get_childEnd() >= 0) {
				count++;
				int longestPos = i, longestTime = currentDuration;
				if (!isLast && nextTitle.get_hierarchy() == 0)
					for (int j = i + 1; j < onlyTitles.size() && onlyTitles.get(j).get_childEnd() < 0; j++)
						if (durationBySeconds.get(j) > longestTime)
							// assignment and accessing in one
							longestTime = durationBySeconds.get(longestPos = j);
				
				textOutline titleWithLongestDuration = onlyTitles.get(longestPos);
				LoggerSingleton.info("< Segment '" + titleWithLongestDuration.get_text() + "' > Duration: "
						+ StaticMethods.secondsToTime(title.get_childEnd()));

				segmentsOutput.append(Joiner.on("\t").join(new String[]{
						title.get_time().toString(),
						isLast ? "<ending>" : (nextTitle.get_hierarchy() == 0 ? "<casual>" : "<logical>"), 
						titleWithLongestDuration.get_text()
				}));
				segmentsOutput.newLine();

				sum += title.get_childEnd();
			}

			for (int j = 0; j <= title.get_hierarchy(); j++) LoggerSingleton.info("--");

			LoggerSingleton.info(title.get_text() + " " + onlyTitles.get(i).get_time());
			if (isLast) LoggerSingleton.info("Average Segment-Length: " + StaticMethods.secondsToTime(sum / count));
		}
		segmentsOutput.close();

//		 ArrayList<textOutline> segments = onlyTitles;
	}

}
