import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ArgumentParser {
	
	ArgumentParser() {
		throw new RuntimeException("Do not create instances of this class");
	}

	static final String LECTURE_KEY = "id";
	static final String FOLDER_KEY = "folder";
	static final String LOGGER_KEY = "log";
	static final String PDF_KEY = "pdf";
	static final String PPTX_KEY = "pptx";

	@SuppressWarnings("static-access")
	public static Options defineCLI(){
		Options opt = new Options();

		opt.addOption(new Option("help", "print this message"));
		opt.addOption(new Option(PDF_KEY, "assume the slides are in PDF format and enforce PDF parsing"));
		opt.addOption(new Option(PPTX_KEY, "assume the slides are in PPTX format and enforce PPTX parsing"));
		
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
                .withDescription(  "folder with the preprocessed slides" )
                .create( FOLDER_KEY ));
		
		opt.addOption(
				OptionBuilder
					.withArgName( "logger_file" )
	                .hasArg()
	                .withDescription(  "file for logging" )
	                .create( LOGGER_KEY ));
		
		return opt;
	}
	
	public static CommandLine parseCLI(Options opt, String[] args){
		CommandLine cmd = null;
		try {
			cmd = new BasicParser().parse(opt, args);
			if(cmd.hasOption("help"))
				throw new ParseException("");
			
			if(cmd.hasOption(PDF_KEY) && cmd.hasOption(PPTX_KEY))
				throw new ParseException("Please choose either -pdf OR -pptx option, but NOT BOTH!");

			return cmd;
		} catch (ParseException e){
			System.err.println(e.getMessage());
			printHelp(opt);
			return null;
		}
		
		
	}
	
	public static void printHelp(Options opt){
		new HelpFormatter().printHelp(new runTogForOpenHPI().getClass().getName(), opt);
	}

	
}
