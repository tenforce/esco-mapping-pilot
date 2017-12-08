package eu.esco.demo.mapping.tool.enhancement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class SynonymTool {
	
	private final static Logger log = LoggerFactory.getLogger(SynonymTool.class);
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		
		String[][] files = {
				//{"th_nl_v2.dat","ISO8859-1"},
				{"th_es_ES_v2.dat","ISO8859-1"}
				//{"thes_fr.dat","UTF-8"},
				//{"th_cs_CZ_v3.dat","UTF-8"}
				};
		
		for(String[] file : files) {
			String filePath = file[0];
			String encoding = file[1];
			
			BufferedReader br = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(filePath), encoding));
			
			if(!encoding.equals(br.readLine())) { // Verify Encoding
				log.warn("File is not encoded in the expected format, this may result in unexpected issues.");
			}
			
			StringBuilder output = new StringBuilder();
			
			int synLeft = -1;
			for(String line = br.readLine() ; line != null ; line = br.readLine()) {
				// New line
				if(!line.startsWith("-") && !line.startsWith("(")) {
					String[] header = line.split("\\|"); 
					try { synLeft = Integer.parseInt(header[1]);  } catch(Exception e) { System.out.println(line); } // Parse header
//					if(synLeft == 1) { // Dead line, no synonym
//						br.readLine(); // Scrap next line
//					} else {
						output.append("\n"); // Setup next line
//					}
				} else {
					String[] lineParts = line.split("\\|");
					for(int part = 1; part < lineParts.length; part++) {
						String synonym = lineParts[part];
						// String cleanup
						if(synonym.contains("(")) {
							synonym = StringUtils.substringBefore(synonym, "(");
						}
						if(synonym.contains("<")) {
							synonym = StringUtils.substringBefore(synonym, "<");
						}
						output.append(synonym);
						if(lineParts.length > 2 && part < lineParts.length - 1) { output.append(","); }
					}
					if(synLeft > 1) { output.append(","); }
					synLeft--;
				}
			}
			
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("src/main/resources/" + filePath.substring(0, StringUtils.lastIndexOf(filePath,".")) + "-encoded.txt"), encoding));
			out.write(output.toString());
			out.flush();
			out.close();
		}
	}	
}