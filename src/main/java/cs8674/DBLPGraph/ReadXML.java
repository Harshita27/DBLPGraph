package cs8674.DBLPGraph;


/*********************************************************
 * This class is used to parse the dblp.xml data
 *********************************************************/
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.neo4j.graphdb.index.RelationshipIndex;
import cs8674.core.Article;

public class ReadXML extends DefaultHandler {

	Article article;
	String text;
	static boolean hasArticle = false;
	//private static final File DB_PATH = new File( "/home/harshita/dblp-index-4" );
	GraphDatabaseService graphDb;
	public DatabaseCreator dbCreator1;
	RelationshipIndex citations;
/*	private  enum RelTypes implements RelationshipType
	{
		CITES
	}
*/	/**
	 * The default constructor
	 * @throws IOException 
	 */
	public ReadXML() throws IOException {
		dbCreator1 = new DatabaseCreator();
	}

	public void setGraphDbInstance(GraphDatabaseService graphDb) {
		this.graphDb = graphDb;
	}
	
	public GraphDatabaseService getGraphDbInstance() {
		return this.graphDb;
	}
	
	/**
	 * reads the xml using SAX parser
	 * @param xmlPath : path of the xml file
	 * @param uri : connection string of mongo
	 * @throws ParserConfigurationException : throws  ParserConfigurationException
	 * @throws SAXException : throws SAXException
	 * @throws IOException : throws IOException
	 */
	public ReadXML(String abc) throws ParserConfigurationException, SAXException, IOException {
	//	dbCreator1 = new DatabaseCreator();
		//setGraphDbInstance(dbCreator);
	//	this.dbCreator = dbCreator;
		//DatabaseCreator dbCreator;
	//	this.citations = dbCreator.getRelIndex();	
	}


	public void parseXML(String xmlPath) throws SAXException, IOException, ParserConfigurationException {
		SAXParserFactory spfac = SAXParserFactory.newInstance();

		//Now use the parser factory to create a SAXParser object
		SAXParser sp = spfac.newSAXParser();

		//Create an instance of this class; it defines all the handler methods 
		ReadXML handler = new ReadXML();

		
		//Finally, tell the parser to parse the input and notify the handler
		sp.parse(xmlPath, handler);
		handler.closeInserter();
	}

	/*
	 * When the parser encounters plain text (not XML elements),
	 * it calls(this method, which accumulates them in a string buffer
	 */
	@Override
	public void characters(char[] buffer, int start, int length) {
		text = new String(buffer, start, length);
	}


	/*
	 * Every time the parser encounters the beginning of a new element,
	 * it calls this method, which resets the string buffer
	 */ 
	@Override
	public void startElement(String uri, String localName,
			String qName, Attributes attributes) throws SAXException {
		if  ("inproceedings".equalsIgnoreCase(qName)|| "article".equalsIgnoreCase(qName)) {
			setHasArticle(true);
			article = new Article();
			article.setKey(attributes.getValue("key"));
			article.setMdate(attributes.getValue("mdate"));
		}
	}

	/*
	 * When the parser encounters the end of an element, it calls this method
	 */
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
	
		try {
			checkHasArticle(uri, localName, qName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Method that helps in processing the article data and store in mongo
	 * @param uri : connection string for mongo
	 * @param localName : local name
	 * @param qName : tag name
	 * @throws IOException 
	 */
	public void checkHasArticle(String uri, String localName, String qName) throws IOException {
		if ( "inproceedings".equalsIgnoreCase(qName) || "article".equalsIgnoreCase(qName)) {
			setHasArticle(false);
			// call the database creator
			dbCreator1.createGraph(article);

		} else if(hasArticle){
			if ("volume".equalsIgnoreCase(qName)) {
				article.setVolume(text);
			} else if ("ee".equalsIgnoreCase(qName)) {
				article.setEe(text);
			} else if ("journal".equalsIgnoreCase(qName)) {
				article.setJournalName(text);
			} else if ("conference".equalsIgnoreCase(qName)) {
				article.setConferenceName(text);
			}else if ("author".equalsIgnoreCase(qName)) {
				article.setAuthor(text);
			}else if ("title".equalsIgnoreCase(qName)) {
				article.setTitle(text);
			}else if ("pages".equalsIgnoreCase(qName)) {
				article.setPages(text);
			}else if ("year".equalsIgnoreCase(qName)) {
				article.setYearPublished(Integer.parseInt(text));
			}else if ("crossref".equalsIgnoreCase(qName)) {
				article.seCrossRef(text);
			}else if ("booktitle".equalsIgnoreCase(qName)) {
				article.setBooktitle(text);
			}else if ("cite".equalsIgnoreCase(qName)) {
				article.setCite(text);
			}
		}
	}


	
/**************Getters and Setters*******************/



public void closeInserter() {
	dbCreator1.closeInserter();
}

public static void setHasArticle(boolean hasArticle) {
	ReadXML.hasArticle = hasArticle;
}


}



