package cs8674.DBLPGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;


public class ParseInProceedings {
	public static void main(String args[]) throws ParserConfigurationException, SAXException, IOException {
		 System.setProperty("jdk.xml.entityExpansionLimit", "0");
		System.out.println("Start:"+ System.currentTimeMillis());
		// ReadXML reader = new ReadXML("/home/harshita/Documents/MSD/DBLP Data/dblp.xml");
		ReadXML reader = new ReadXML("abc");
		//reader.parseXML("/home/harshita/Desktop/small_in.xml");
		//	reader.parseXML("/home/ec2-user/small_in.xml");
		reader.parseXML("/home/ec2-user/dblp.xml");
		
		//reader.parseXML("/home/harshita/Documents/MSD/DBLP Data/dblp.xml");
		//reader.getCitedBy();
	//	ReadXML reader = new ReadXML();
		//reader.getCitedBy("conf/sigmod/StonebrakerR86");tns
	}
}
