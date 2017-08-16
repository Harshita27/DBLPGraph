package cs8674.DBLPGraph;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;

public class TraverseGraph {
	private static GraphDatabaseService db;
	private static TraversalDescription friendsTraversal;
	private static final File DB_PATH = new File( "/home/ec2-user/dbCoAuthorImp" );
	private  enum RelTypes implements RelationshipType
	{ 
		CITES,
		CO_AUTHOR,
		HAS
	}
	BatchInserterIndex index;
	BatchInserterIndex relIndex;
	BatchInserterIndexProvider lucene;
	BatchInserter inserter;
	long idOne;
	long idSec;


	TraverseGraph () throws IOException {
		db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		friendsTraversal = db.traversalDescription()
				.depthFirst()
				.relationships( RelTypes.CO_AUTHOR )
				.evaluator( Evaluators.toDepth( 1 ) )
				.uniqueness( Uniqueness.RELATIONSHIP_GLOBAL );
	}


	public static void main(String[] args) throws IOException {
		TraverseGraph tg = new TraverseGraph();
		try ( Transaction tx = db.beginTx	() ) {
			tg.findAuthorsByCoAuthor("Sridhar Ramaswamy"); //
			tg.findCommonCoAuthors("Paris C. Kanellakis", "Gerd G. Hillebrand"); //
			tg.findUncommonCoAuthors("Paris C. Kanellakis", "Sridhar Ramaswamy");
			tg.findAuthorsForJournal("pods", "sigmod"); //
			tg.findAuthorsForConf("pods"); //
			tg.findPercentageCommonAuthorIn("sigmod", "pods");
			tg.searchAuthorsByArticleTitle("Constraint"); //
			tg.searchAllArticlesForAuthor("Paris C. Kanellakis");
			tg.findArticleCitationsByAuthor("Sridhar Ramaswamy");
			tg.findArticleCitationsForAuthor("Sridhar Ramaswamy");
		}
	}
	
	
	

	public void findArticleCitationsForAuthor(String authorName) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("All articles that cite the author-->"+ authorName);

		try ( Transaction ignored = db.beginTx();
				Result resultsForAuthor = db.execute("MATCH (a:article)-[:CITES]->(b:article)-[:HAS]->(m) WHERE m.authorName = '"+authorName+"' RETURN DISTINCT a");
				ResourceIterator<Node> articles= resultsForAuthor.columnAs("a"))
		{
			while ( articles.hasNext() )
			{


				Node article = articles.next();
				if(article.hasProperty("title"))
				{
					System.out.println(article.getProperty("title"));
				} else {
					System.out.println(article.getProperty("key"));
				}
			}
		}
	}

	public void findArticleCitationsByAuthor(String authorName) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("All articles cited by author-->"+ authorName);


		try ( Transaction ignored = db.beginTx();
				Result resultsForAuthor = db.execute("MATCH (a:article)-[:CITES]-(b:article), (a:article)-[:HAS]->(m) WHERE m.authorName = '"+authorName+"' RETURN DISTINCT b");
				ResourceIterator<Node> articles= resultsForAuthor.columnAs("b"))
		{
			while ( articles.hasNext() )
			{

				Node article = articles.next();
				if(article.hasProperty("title"))
				{
					System.out.println(article.getProperty("title"));
				} else {
					System.out.println(article.getProperty("key"));
				}
			}
		}
	}

	public void searchAllArticlesForAuthor(String authorName) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("All articles by "+ authorName);


		try ( Transaction ignored = db.beginTx();
				Result resultsForAuthor = db.execute("MATCH (a:article)-[:HAS]->(m) WHERE m.authorName = '"+authorName+"' RETURN DISTINCT a");
				ResourceIterator<Node> articles= resultsForAuthor.columnAs("a"))
		{
			while ( articles.hasNext() )
			{

				Node article = articles.next();
				if(article.hasProperty("title"))
				{
					System.out.println(article.getProperty("title"));
				}else {
					System.out.println(article.getProperty("key"));
				}
			}
		}
	}


	public void searchAuthorsByArticleTitle(String articleTitleKeyword) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("Articles with title containign the word ---->> " + articleTitleKeyword);


		try ( Transaction ignored = db.beginTx();
				//MATCH (:confName { confName:'"+conf1+"' })-[:PUBLISHES]->(n)-[:HAS]->(m) RETURN DISTINCT m
				Result resultsConf1 = db.execute("MATCH (a:article)-[:HAS]-(m) WHERE a.title CONTAINS '"+articleTitleKeyword+"' RETURN m");
				ResourceIterator<Node> authorsInConf1= resultsConf1.columnAs("m"))
		{
			while ( authorsInConf1.hasNext() )
			{

				Node author = authorsInConf1.next();
				if(author.hasProperty("authorName"))
				{
					System.out.println(author.getProperty("authorName"));
				}
			}
		}
	}

	/************************************************************************
	 * Method to find the percentage of authors who have been a part of the given 
	 * journals/conferences
	 * @param confName
	 ************************************************************************/

	public void findPercentageCommonAuthorIn(String conf1, String conf2) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("Percentage authors common between " + conf1 + " and "+ conf2);

		Set<String> namesForAuth1 = new HashSet<String>();
		Set<String> namesForAuth2 = new HashSet<String>();

		try ( Transaction ignored = db.beginTx();
				Result resultsConf1 = db.execute("MATCH (:confName { confName:'"+conf1+"' })-[:PUBLISHES]->(n)-[:HAS]->(m) RETURN DISTINCT m");
				ResourceIterator<Node> authorsInConf1= resultsConf1.columnAs("m");
				Result resultsConf2 = db.execute("MATCH (:confName { confName:'"+conf2+"' })-[:PUBLISHES]->(n)-[:HAS]->(m) RETURN DISTINCT m");
				ResourceIterator<Node> authorsInConf2= resultsConf2.columnAs("m"))	
		{
			while ( authorsInConf1.hasNext() )
			{
				Node author = authorsInConf1.next();
				if(author.hasProperty("authorName"))
				{
					namesForAuth1.add((String)author.getProperty("authorName"));
				}
			}
			while ( authorsInConf2.hasNext() )
			{
				Node author = authorsInConf2.next();
				if(author.hasProperty("authorName"))
				{	
					namesForAuth2.add((String)author.getProperty("authorName"));
				}
			}
			namesForAuth2.retainAll(namesForAuth1);
			System.out.println("size of other conf:"+ namesForAuth2.size());
			System.out.println("size of this conf:"+ namesForAuth1.size());
			double percentage = ((double)namesForAuth2.size()/ (double)namesForAuth1.size())*100;
			System.out.println("Percentage of authors common in "+ conf1+" and "+ conf2+":"+ percentage);

		}
	}

	/************************************************************************
	 * Method to find all the authors who have been a part of the given 
	 * journals/conferences
	 * @param confName
	 ************************************************************************/
	public void findAuthorsForConf(String confName) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("Authors who published in "+ confName);


		try ( Transaction ignored = db.beginTx();

				Result result = db.execute("MATCH (:confName { confName:'"+confName+"' })-[:PUBLISHES]->(n)-[:HAS]->(m) RETURN DISTINCT m.authorName");
				ResourceIterator<String> authorsInConf= result.columnAs("m.authorName"))	
		{
			while ( authorsInConf.hasNext() )
			{
				System.out.println(authorsInConf.next());
				}
		}

	}
	/************************************************************************
	 * Method to find all the authors who have been a part of the given two
	 * journals/conferences
	 * @param journ1
	 * @param journ2
	 ************************************************************************/
	public void findAuthorsForJournal(String journ1, String journ2) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("Authors who have published in "+journ1+ " and "+ journ2);


		try ( Transaction ignored = db.beginTx();
				Result result = db.execute("MATCH (:confName { confName:'"+journ1+"' })-[:PUBLISHES]->(n)-[:HAS]->(m)"+
						"<-[:HAS]-(t)<-[:PUBLISHES]-(:confName { confName:'"+journ2+"' })  RETURN DISTINCT m.authorName");	
				ResourceIterator<String> authorsInConf= result.columnAs("m.authorName"))
		{
			while ( authorsInConf.hasNext() )
			{
				System.out.println(authorsInConf.next());
			}
		}
	}


	/**************************************************************************
	 * Method to find all the co authors for a given author
	 * @param authorName
	 *************************************************************************/
	public void findAuthorsByCoAuthor(String authorName) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("All Authors whi have co authored with "+ authorName);

		try ( Transaction ignored = db.beginTx();
				Result result = db.execute("MATCH (:author { authorName: '"+authorName+
						"' })-[:CO_AUTHOR]-(m) RETURN DISTINCT m.authorName");	
				ResourceIterator<String> authorsInConf= result.columnAs("m.authorName"))
		{
			while ( authorsInConf.hasNext() )
			{
				System.out.println(authorsInConf.next());
			}
		}

		/*
		Label label = Label.label( "author" );
		ResourceIterator<Node> foundNodes = db.findNodes(label, "authorName", authorName);
		if( foundNodes.hasNext()) {
			Node myNode = foundNodes.next();
			System.out.println(traverseAllCoAuths(myNode));
		}*/


	}


	/**************************************************************************
	 * Method to find common co authors for given set of authors
	 * @param auth1
	 * @param auth2
	 *************************************************************************/
	public void findCommonCoAuthors(String auth1, String auth2) {
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("Common co authors of two authors:"+ auth1 +" and "+ auth2);

		try ( Transaction ignored = db.beginTx();
				Result result = db.execute("MATCH (:author { authorName: '"+auth1+
						"' })-[:CO_AUTHOR]-(m)-[:CO_AUTHOR]-(:author { authorName: '"+auth2+
						"' }) RETURN DISTINCT m"))	
		{
			while ( result.hasNext() )
			{
				Map<String,Object> row = result.next();
				for ( Entry<String,Object> column : row.entrySet() )
				{
					Node found = (Node)column.getValue();
					if(found.hasProperty("authorName"))
						System.out.println(column.getKey() + ": " + found.getProperty("authorName") + "; ");
				}
			}
		}
	}

	/**************************************************************************
	 * Method to find uncommon co authors for given set of authors
	 * @param auth1
	 * @param auth2
	 *************************************************************************/
	public void findUncommonCoAuthors(String auth1, String auth2) {

		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~`");
		System.out.println("UNCommon co authors of two authors:"+ auth1 +" and "+ auth2);


		Set<String> namesForAuth1 = new HashSet<String>();
		Set<String> namesForAuth2 = new HashSet<String>();

		try ( Transaction ignored = db.beginTx();
				Result resultsForAuth1 = db.execute("MATCH (:author { authorName: '"+auth1+
						"' })-[:CO_AUTHOR]-(m) RETURN  DISTINCT m" ); 
				ResourceIterator<Node> columnsAuth1= resultsForAuth1.columnAs("m");
				Result resultsForAuth2 = db.execute("MATCH (:author { authorName: '"+auth2+
						"' })-[:CO_AUTHOR]-(n) RETURN DISTINCT n" ); 
				ResourceIterator<Node> columnsAuth2= resultsForAuth2.columnAs("n"))

		{
			while ( columnsAuth1.hasNext() )
			{
				//System.out.println(columnsAuth1.next().getProperty("authorName"));
				Node author = columnsAuth1.next();
				if(author.hasProperty("authorName"))
					namesForAuth1.add((String)author.getProperty("authorName"));
			}
			while ( columnsAuth2.hasNext() )
			{
				//System.out.println(columnsAuth2.next().getProperty("authorName"));
				Node author = columnsAuth2.next();
				if(author.hasProperty("authorName"))
					namesForAuth2.add((String)author.getProperty("authorName"));
			}

			if(namesForAuth2.size() > namesForAuth1.size()) {
				namesForAuth2.removeAll(namesForAuth1);
				for(String authors: namesForAuth2) {
					System.out.println(authors);
				}
			}
			else {
				namesForAuth1.removeAll(namesForAuth2);
				for(String authors: namesForAuth1) {
					System.out.println(authors);
				}
			}
		}
	}


	/**************************************************************************
	 * Helper method to find co authors of a given author
	 * @param node
	 * @return
	 *************************************************************************/
	public String traverseAllCoAuths( Node node )
	{
		TreeMap<Integer, String> repToRecordMap = new TreeMap<Integer, String>(Collections.reverseOrder());
		String output = "";
		for ( Relationship relationship : friendsTraversal
				.traverse( node )
				.relationships() )
		{
			if((int) relationship.getProperty("articleCount") >=1) {
				repToRecordMap.put((int)relationship.getProperty("articleCount"), (String)relationship.getEndNode().getProperty("authorName"));
				if (repToRecordMap.size() > 2) {
					repToRecordMap.remove(repToRecordMap.lastKey());
				}
				for(int key: repToRecordMap.keySet()) {
					output += repToRecordMap.get(key);
				}

			}

		}
		return output;
	}

}
