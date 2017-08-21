package cs8674.DBLPGraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;

import cs8674.core.Article;


/*********************************************************************************
 * 
 * @author harshita
 * This class calls the neo4j batch inserter to insert the created article
 * objects into the neo4j database. The conferences, articles and authors
 * are all nodes that are created and are related based on the following relations:
 * [Conf] -publishes- [Article] -has- [Author]
 * 
 * Relationship properties that exist:
 * 1. artcileCount: between [Author]  -  [Author], gives the total articles 
 *    published together by the two authors
 * 2. authorPair: between [Author]  -  [Author], used to identify the coauthors
  **********************************************************************************/
public class DatabaseCreator {
	// path where you want to store your database
	private static final File DB_PATH = new File( "/home/ec2-user/dbCoAuthorImp" );
	static ArrayList<String> visitedCites = new ArrayList<String>();
	GraphDatabaseService graphDb;
	BatchInserter inserter;

	// batch inserts use lucene indexing for easy updation and insertion
	BatchInserterIndex index;
	BatchInserterIndex relIndex;
	BatchInserterIndexProvider lucene;

	// relationship types 
	private  enum RelTypes implements RelationshipType
	{ 
		CITES,
		CO_AUTHOR,
		HAS, 
		PUBLISHES
	}
	Map<String, Object> confRelProp = new HashMap<String, Object>();


	/*****************************************************
	 * The constructor that initializes the batch inserter
	 * and the indexer
	 * @throws IOException
	 ******************************************************/
	public DatabaseCreator() throws IOException {
		FileUtils.deleteRecursively( DB_PATH );

		inserter = BatchInserters.inserter( DB_PATH );
		lucene = new LuceneBatchInserterIndexProvider(inserter);
		createIndexOnNode();
	}

	/*******************************************************
	 * Creates lucene index on the nodes
	 ******************************************************/
	public void createIndexOnNode() {
		index = lucene.nodeIndex( "node_auto_index", MapUtil.stringMap("type", "exact") );
		relIndex = lucene.relationshipIndex("rel_auto_index", MapUtil.stringMap("type", "exact"));
	}

	/*******************************************************
	 * Helper method to insert an article node into the graph.
	 * Article has the properties of title and the key
	 * Key is unique to each article
	 * @param article
	 * @return
	 ******************************************************/
	public long addNodeToGraph(Article article, long confId) {
		Label label = Label.label( "author" );

		// if node exists then create new relationships to the co -authors
		long authorsId = nodeExists(article);
		if(authorsId == Long.MIN_VALUE ) {
			Map<String, Object> prop = new HashMap<String, Object>();
			prop.put("authorName", article.getMainAuthor());
			authorsId = inserter.createNode( prop, label );
			index.add(authorsId, prop);
			index.flush();
		} 

		// if there are more than one authors for an article, create co-authors
		if(!article.getAuthor().isEmpty())
		{
			for(String coAuthors: article.getAuthor()){
				createRelatedNode(authorsId, coAuthors, article.getMainAuthor(), confId);
			}
		}
		return authorsId;
	}


	/*******************************************************
	 * Helper method to check if a node exists in the graph 
	 * @param article
	 * @return
	 ********************************************************/
	public long nodeExists(Article article) {
		IndexHits<Long> authorExists= index.get("authorName", article.getMainAuthor());
		// if there are nodes that exist with the same key as the cited article
		if(authorExists.size() != 0 ){ 
			return authorExists.getSingle();
		} 
		else return Long.MIN_VALUE;
	}

	/*********************************************************
	 * Helper method used to update the relationship properties
	 * of two co authors, based on the number of articles they 
	 * published together
	 * @param relProp
	 ********************************************************/
	public Map<String, Object> updateProperties(Map<String, Object> relProp) {
		int count = (int)relProp.get("articleCount");
		relProp.put("articleCount", count+1);
		return relProp;
	}

	/*********************************************************
	 * Method to create a co author node if doesnt exist or
	 * update the relationship if co author exists
	 * @param authorId
	 * @param coAuthor
	 * @param mainAuthor
	 ********************************************************/
	public void createRelatedNode(long authorId, String coAuthor, String mainAuthor, long confId) {
		long relId = Long.MIN_VALUE;
		Label labelForCoAuth = Label.label( "author" );
		String authorPair = mainAuthor+"--"+coAuthor;
		
		Map<String, Object> relProp =null;
		IndexHits<Long> authorExists= index.get("authorName",coAuthor);
		IndexHits<Long> relExists= relIndex.get("authorPair", authorPair);
		try {
			// if coauthor relation exists in the graph update the properties
			if(relExists.size() != 0) {

				long id = relExists.getSingle();
				relProp = inserter.getRelationshipProperties(id);
				relProp = updateProperties(relProp);
				relId = id;
				inserter.setRelationshipProperties(relId, relProp);
				relIndex.updateOrAdd(relId, relProp);
				
			} 
			// else create a new relationship if the node exists but relation doesnt
			else if(authorExists.size() != 0 ) {
				relProp = new HashMap<String, Object>();
				relProp.put("authorPair", authorPair);
				relProp.put("articleCount", 1);
				long id = authorExists.getSingle();
				relId = inserter.createRelationship(authorId, id, RelTypes.CO_AUTHOR, relProp );
				relIndex.add(relId, relProp);
				inserter.createRelationship(confId,id, RelTypes.HAS, confRelProp);

			}
			// else create new node and relation
			else {
				Map<String, Object> prop = new HashMap<String, Object>();
				prop.put("authorName", coAuthor);
				long coAuthorNode = inserter.createNode( prop , labelForCoAuth);
				index.add(coAuthorNode, prop);
				relProp = new HashMap<String, Object>();
				relProp.put("authorPair", authorPair);
				relProp.put("articleCount", 1);
				index.flush();
				relId = inserter.createRelationship(authorId, coAuthorNode, RelTypes.CO_AUTHOR, relProp );
				inserter.createRelationship(confId,coAuthorNode, RelTypes.HAS, confRelProp);
				relIndex.add(relId, relProp);
				}
		}
		finally {
			relExists.close();
			authorExists.close();
		}
		relIndex.flush();

	}


	/*********************************************************
	 * Method to create an article node from the parsed xml
	 * @param article
	 * @return
	 *********************************************************/
	public long addArticleNode(Article article) {
		Label label = Label.label( "article" );
		long articleId = Long.MIN_VALUE;
		IndexHits<Long> articleExists= index.get("key",article.getKey());

		try{
			if(articleExists.size() != 0) {
				articleId = articleExists.getSingle();
			}
			// create new article node with node properties as title, key and year
			else {
				Map<String, Object> prop = new HashMap<String, Object>();
				prop.put("title", article.getTitle());
				prop.put("key", article.getKey());
				prop.put("year", article.getYearPublished());
				articleId =	inserter.createNode(prop, label);
				index.add(articleId, prop);
				index.flush();
			}
		}
		finally {
			articleExists.close();
		}

		return articleId;
	}


	
	/**********************************************************
	 * Method to add a conference as a node into the graph
	 * @param key
	 * @return
	 *********************************************************/
	public long addConfNode(String key) {
		String[] confName = key.split("/");
		// conf name is extracted from the article's key
		Label label = Label.label( "confName" );
		long confId = Long.MIN_VALUE;

		IndexHits<Long> confExists= index.get("confName",confName[1]);
		try {
			if(confExists.size() != 0) {
				confId = confExists.getSingle();
			}
			// if node already doesnt exist then create a new node and add properties
			else {
				Map<String, Object> prop = new HashMap<String, Object>();
				prop.put("confName", confName[1]);
				confId =	inserter.createNode(prop, label);
				index.add(confId, prop);
				index.flush();
				//return confId;
			}
		}
		finally{
			confExists.close();
		}

		return confId;
	}

	@SuppressWarnings("unused")
	/*******************************************************
	 * Method to create the entire graph by inspecting each of the 
	 * article objects and creating nodes and their relationships
	 * @param article
	 * @return
	 ******************************************************/
	public void createGraph(Article article) {
		long authorId;
		Label label = Label.label( "article" );
		// add conf node  to the graph
		long actualConfId = addConfNode(article.getKey());
		long confId = addArticleNode(article);
		
		// add relation between conference and article
		inserter.createRelationship(actualConfId,confId, RelTypes.PUBLISHES, confRelProp);

		// add author node  to the graph
		authorId = addNodeToGraph(article, confId);

		// new: create relation between main author and conf node
		inserter.createRelationship(confId,authorId, RelTypes.HAS, confRelProp);


		// for each of the citations given by this article, create new article nodes or
		// update existing ones based on article key
		for(String cite: article.getCite()) {
			long confRelId = Long.MIN_VALUE;
			// filter out the empty citations
			if(!cite.equals("...")){
				// create properties for each citation
				Map<String, Object> prop = new HashMap<String, Object>();
				prop.put("key", cite );
				long citedId = Long.MIN_VALUE;

				IndexHits<Long> citeExists= index.get("key", cite);
				try{
					// if there are nodes that exist with the same key as the cited article
					if(citeExists.size() != 0) {
						citedId = citeExists.getSingle();
					}
					// create a new node for citation
					else {
						long citeConfId = addConfNode(cite);
						citedId = inserter.createNode(prop, label);
						// create relation between conf and article
						confRelId=	inserter.createRelationship(citeConfId, citedId, RelTypes.PUBLISHES, confRelProp );
						index.add(citedId, prop);
						index.flush();
					}
				}
				finally {
					citeExists.close();
				}
				
				// create relation between two articles based on the citation
				long relId = inserter.createRelationship(confId, citedId, RelTypes.CITES, prop);
				relIndex.flush();
			}
		}
	}

	public void closeInserter() {
		lucene.shutdown();
		inserter.shutdown();
	}







	/********************Graph db service code: without inserter*****************************/

	/*public boolean createGraph(Article article) {
		long articleNode =Long.MIN_VALUE;
		Label label = Label.label( "articleKey" );
	//	try ( Transaction tx = graphDb.beginTx	() )
		{	




			if(articleExists(article.getKey())) {
				articleNode = getFoundArticle();
				articleNode.setProperty("title", article.getBooktitle());
			}
			else 
		/	{
			Node foundNode = graphDb.findNode( label , "key", article.getKey());
			if(foundNode != null) {
				articleNode = foundNode.getId();
			}

			if(foundNode == null){
				System.out.println("here in null find");
				Map<String, Object> prop = new HashMap<String, Object>();
				prop.put("title", article.getBooktitle());
				prop.put("key", article.getKey());
				articleNode = inserter.createNode( prop, label );

		}
			else {
				System.out.println("here in not null find");
				articleNode = foundNode;
				articleNode.setProperty("title", article.getBooktitle());
				articleNode.setProperty("key", article.getKey());
			}

		//	tx.success();
			try {
				tx.close();
			}catch(Exception e) {
				System.out.println("exeption on commit:"+ e);
			}
		}		

	//	try ( Transaction tx = graphDb.beginTx	() )
	//	{	//}

			Node citedNode ;
			for(String cite: article.getCite()) {
				Node foundCiteNode = graphDb.findNode( label , "key", cite);

				Relationship relationship;
				if(!cite.equals("...")){


					if(foundCiteNode != null){
						long citedId = foundCiteNode.getId();
						System.out.println(foundCiteNode.getProperty("key"));
					//	citedNode = foundCiteNode;
						Map<String, Object> prop = new HashMap<String, Object>();
						prop.put("message", "cites"+cite );
						inserter.createRelationship(articleNode, citedId, RelTypes.CITES, prop );
						//createRelationship( articleNode, citedId, RelTypes.CITES, prop );
						//relationship.setProperty( "message", "cites"+cite );			
					//	citations.add( relationship, "name", relationship.getProperty( "message" ) );
					} else if(foundCiteNode == null){
					//	visitedCites.add(cite);
						System.out.println("new cite node:"+ cite);
						Map<String, Object> prop = new HashMap<String, Object>();
						//prop.put("title", article.getBooktitle());
						prop.put("key", cite);
						articleNode = inserter.createNode( prop, label );

					}

				}

			}
			tx.success();
			try {
				tx.close();
			}catch(Exception e) {
				System.out.println("exeption on commit:"+ e);
			}

		return true;
	}*/


	/*	public boolean createGraph(Article article) {
		Node articleNode;
		Label label = Label.label( "articleKey" );
		try ( Transaction tx = graphDb.beginTx	() )
		{	




			if(articleExists(article.getKey())) {
				articleNode = getFoundArticle();
				articleNode.setProperty("title", article.getBooktitle());
			}
			else 
			{
			Node foundNode = graphDb.findNode( label , "key", article.getKey());
			if(foundNode == null){
				System.out.println("here in null find");
				articleNode = graphDb.createNode( label );
				articleNode.setProperty("title", article.getBooktitle());
				articleNode.setProperty("key", article.getKey());
			} else {
				System.out.println("here in not null find");
				articleNode = foundNode;
				articleNode.setProperty("title", article.getBooktitle());
				articleNode.setProperty("key", article.getKey());
			}

			tx.success();
			try {
				tx.close();
			}catch(Exception e) {
				System.out.println("exeption on commit:"+ e);
			}
		}		

		try ( Transaction tx = graphDb.beginTx	() )
		{	//}

			Node citedNode ;
			for(String cite: article.getCite()) {
				Node foundCiteNode = graphDb.findNode( label , "key", cite);

				Relationship relationship;
				if(!cite.equals("...")){


					if(foundCiteNode != null){
						System.out.println(foundCiteNode.getProperty("key"));
						citedNode = foundCiteNode;
						relationship = articleNode.createRelationshipTo( citedNode, RelTypes.CITES );
						relationship.setProperty( "message", "cites"+cite );			
					//	citations.add( relationship, "name", relationship.getProperty( "message" ) );
					} else {
					//	visitedCites.add(cite);
						System.out.println("new cite node:"+ cite);
						citedNode = graphDb.createNode( label );
						citedNode.setProperty("key", cite);
						relationship = articleNode.createRelationshipTo( citedNode, RelTypes.CITES );
						relationship.setProperty( "message", "cites"+cite );
				//		citations.add( relationship, "name", relationship.getProperty( "message" ) );
					}

				}

			}
			tx.success();
			try {
				tx.close();
			}catch(Exception e) {
				System.out.println("exeption on commit:"+ e);
			}
		}
		return true;
	}
	 */

	/*public boolean articleExists(String keyForUpdate) {
		{
			Label label = Label.label( "articleKey" );
			try ( Transaction tx = graphDb.beginTx() )
			{
				try ( ResourceIterator<Node> articles =
						graphDb.findNodes( label, "key", keyForUpdate ) )
				{
					while ( articles.hasNext() )
					{
						Node newArticle = articles.next();
						if(newArticle.getProperty("key").equals(keyForUpdate)) {
							setFoundArticle(newArticle);

							tx.success();
							tx.close();
							return true;

						}
					}       
				}
				tx.success();
				tx.close();
			}catch(Exception e) {
				System.out.println("exec:"+e);
			}
			// END SNIPPET: findUsers
			return false;
		}
	}

	Node existingNode;
	public void setFoundArticle(Node article) {
		existingNode = article;
	}

	public Node getFoundArticle() {
		return existingNode;
	}*/

	/*public void createIndexOnNode() {

		IndexDefinition indexDefinition;
		try ( Transaction tx = graphDb.beginTx() )
		{
			Schema schema = graphDb.schema();
			indexDefinition = schema.indexFor( Label.label( "articleKey" ) )
					.on( "key" )
					.create();
			tx.success();
		}


		// END SNIPPET: createIndex
		// START SNIPPET: wait

		try ( Transaction tx = graphDb.beginTx() )
		{
			Schema schema = graphDb.schema();
			schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );
		}
		// END SNIPPET: wait


		try ( Transaction tx = graphDb.beginTx() )
		{
			Schema schema = graphDb.schema();
			System.out.println( String.format( "Percent complete: %1.0f%%",
					schema.getIndexPopulationProgress( indexDefinition ).getCompletedPercentage() ) );
		}
	}
	 */
	/*public void createIndexOnRelation() {
		try ( Transaction tx = graphDb.beginTx() )
		{
			IndexManager index = graphDb.index();
			citations = index.forRelationships( "cites" );
			tx.success();
		}
	}*/

	/*
	public static RelationshipIndex getRelIndex() {
		return citations;
	}*/

	public  GraphDatabaseService getGraphInstance() {
		return graphDb;
	}

	/*public void getCitedBy(String inputKey){
		try ( Transaction tx = graphDb.beginTx() )
		{
			IndexHits<Relationship> citedNoteHits;
			if(articleExists(inputKey)){
				citedNoteHits = citations.query( "name", "*", null, getFoundArticle() );

				for(int i =0; i< citedNoteHits.size(); i++) {
					Relationship citeRelation = citedNoteHits.iterator().next();
					System.out.println("startnode::"+citeRelation.getStartNode().getProperty("key"));
				}
				citedNoteHits.close();
			}
			tx.success();
		}
	}*/
}
