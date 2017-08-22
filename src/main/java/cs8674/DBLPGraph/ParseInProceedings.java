package cs8674.DBLPGraph;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * 
 * @author harshita
 * This is the main driver class that parses the input dblp.xml 
 * Since the raw file is parsed on the ec2 machine, the path is set by
 * default to the ec2 location.
 */

public class ParseInProceedings {
	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException {
		System.setProperty("jdk.xml.entityExpansionLimit", "0");
		System.out.println("Start:"+ System.currentTimeMillis());
		ReadXML reader = new ReadXML("abc");
		//reader.parseXML("/home/ec2-user/dblp.xml");
		// args[0] to contain the path of the dblp.xml file
		reader.parseXML(args[0]);
	}
}
