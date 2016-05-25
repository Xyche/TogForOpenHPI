package helper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.base.Joiner;


public class ArgumentParser {
	
	ArgumentParser() {
		throw new RuntimeException("Do not create instances of this class");
	}

	public static final String LECTURE_KEY = "id";
	public static final String FOLDER_KEY = "folder";
	public static final String LOGGER_KEY = "log";
	public static final String MODE_KEY = "mode";
	public static final String PDF_KEY = "pdf";
	public static final String PPTX_KEY = "pptx";
	public static final String CHANGE_NAMES_KEY = "change_names";
	public static final String SIZE_KEY = "size";
	
	private static final Pattern sizePattern = Pattern.compile("(\\d+)x(\\d+)");

	@SuppressWarnings("static-access")
	public static Options defineCLI(){
		Options opt = new Options();

		opt.addOption(new Option("help", "print this message"));
		opt.addOption(new Option(PDF_KEY, "assume the slides are in PDF format and enforce PDF parsing"));
		opt.addOption(new Option(PPTX_KEY, "assume the slides are in PPTX format and enforce PPTX parsing"));
		opt.addOption(new Option(CHANGE_NAMES_KEY, "rename thumbnails"));
		
		opt.addOption(
			OptionBuilder
				.withArgName( "lecture_id" )
                .hasArg()
                .isRequired()
                .withDescription(  "id of the processed lecture" )
                .create( LECTURE_KEY ));

		opt.addOption(
			OptionBuilder
				.withArgName( "lecture_folder" )
				.isRequired()
                .hasArg()
                .withDescription( "folder with the preprocessed slides" )
                .create( FOLDER_KEY ));

		opt.addOption(
				OptionBuilder
					.withArgName( "logger_file" )
	                .hasArg()
	                .withDescription( "file for logging" )
	                .create( LOGGER_KEY ));

		opt.addOption(
				OptionBuilder
					.withArgName( "OCR_origin_mode" )
	                .hasArg()
	                .withDescription( "OCR mode:\n" + ocr_modes_desc() )
	                .create( MODE_KEY ));
		
		opt.addOption(
				OptionBuilder
					.withArgName( "WIDTHxHEIGHT" )
	                .hasArg()
	                .withDescription( "Screenshot dimensions" )
	                .create( SIZE_KEY ));	
		
		return opt;
	}
	
	private static int size(CommandLine cmd, int idx){
		return Integer.valueOf(sizePattern.matcher(cmd.getOptionValue(SIZE_KEY)).group(idx));
	}

	private static final int WIDTH_IDX = 0, HEIGHT_IDX = 1;
	public static int width(CommandLine cmd){
		if(cmd.hasOption(SIZE_KEY))
			return size(cmd, WIDTH_IDX);
		else
			return Constants.DEFAULT_WIDTH;
	}
	
	public static int height(CommandLine cmd){
		if(cmd.hasOption(SIZE_KEY))
			return size(cmd, HEIGHT_IDX);
		else
			return Constants.DEFAULT_HEIGHT;
	}
	
	private static String ocr_modes_desc() {
		return Joiner.on("\n").join(new String[]{
				OCROriginMode.description(OCROriginMode.mySQL),
				OCROriginMode.description(OCROriginMode.teleTaskXML),
				OCROriginMode.description(OCROriginMode.ACM_XML),
				OCROriginMode.description(OCROriginMode.PDF),
				OCROriginMode.description(OCROriginMode.JSON),
		});
	}

	public static CommandLine parseCLI(Options opt, String[] args){
		CommandLine cmd = null;
		try {
			cmd = new BasicParser().parse(opt, args);
			if(cmd.hasOption("help"))
				throw new ParseException("");
			
			if(cmd.hasOption(PDF_KEY) && cmd.hasOption(PPTX_KEY))
				throw new ParseException("Please choose either -pdf OR -pptx option, but NOT BOTH!");
			
			if(cmd.hasOption(SIZE_KEY)){
				Matcher sizeMatcher = sizePattern.matcher(cmd.getOptionValue(SIZE_KEY));
				if (!sizeMatcher.matches()){
					throw new ParseException("Please give screenshot dimensions in the format \"WIDTHxHEIGHT\"");
				}
			}

			return cmd;
		} catch (ParseException e){
			System.err.println(e.getMessage());
			printHelp(opt);
			return null;
		}
		
		
	}
	
	public static void printHelp(Options opt){
		new HelpFormatter().printHelp("runTogForOpenHPI", opt);
	}

	
}
