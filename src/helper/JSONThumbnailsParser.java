package helper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import dataStructure.textLine;

public abstract class JSONThumbnailsParser {
	public enum keys{
		images, path, start, textlines, content, boundingBox, 
		left, top, width, height
	}
	
	private final static class JSONObjectWrapper{
		private JSONObject object = null;
		public JSONObjectWrapper(JSONObject obj) {
			this.object = obj;
		}
		public JSONObjectWrapper(Object obj) {
			this((JSONObject)obj);
		}
		@SuppressWarnings("unchecked")
		public <T> T get(keys key, T _){
			return (T) this.object.get(key.name());
		}
		
		public JSONObjectWrapper newWrapper(keys key){
			return new JSONObjectWrapper(this.get(key, new JSONObject()));
		}
	}
	
	
	public static ArrayList<textLine> parse(String file_path) throws UnsupportedEncodingException, FileNotFoundException, IOException, ParseException {
		ArrayList<textLine> res = new ArrayList<textLine>();
		
		final JSONParser parser = new JSONParser();
		JSONObjectWrapper content = new JSONObjectWrapper(parser.parse(new InputStreamReader(new FileInputStream(file_path),"UTF-8")));
		final Pattern slideID_regex = Pattern.compile("^(\\d+)\\.?.*$");
		
		for(Object raw_slide : content.get(keys.images, new JSONArray())){
			JSONObjectWrapper slide = new JSONObjectWrapper(raw_slide);
			Path path = Paths.get(slide.get(keys.path, ""));
			Time startTime = StaticMethods.parseJsonTimeTag(slide.get(keys.start, ""));

			for (Object textLines : slide.get(keys.textlines, new JSONArray())){
				JSONObjectWrapper textLinesWrapper = new JSONObjectWrapper(textLines);
				textLine line = new textLine();

				line.set_time(startTime);
				String fileName = path.getFileName().toString();
				Matcher m = slideID_regex.matcher(fileName);
				if(!m.matches()) {
					String mess = String.format("Could not extract SlidesID from %s", fileName);
					LoggerSingleton.error(mess);
					throw new RuntimeException(mess);
				}
				line.set_slideID(Integer.parseInt(m.group(1)));
				
				line.set_text(textLinesWrapper.get(keys.content, "").replace("\n", " "));
				JSONObjectWrapper bb = textLinesWrapper.newWrapper(keys.boundingBox);
				line.set_top(bb.get(keys.top, 0L).intValue());
				line.set_left(bb.get(keys.left, 0L).intValue());
				line.set_width(bb.get(keys.width, 0L).intValue());
				line.set_height(bb.get(keys.height, 0L).intValue());

				line.set_bottom(line.get_top() + line.get_height());
				line.set_lastLineWidth(line.get_width());
				line.set_lastLineLeft(line.get_left());
				line.set_type(0);

				if(!line.get_text().contentEquals(" "))
					//if(t.get_left() >= 320) //Only for "pip" stream, to cut out the non-slide area
					res.add(line);

			}
		}
		
		return res;
	}

}
