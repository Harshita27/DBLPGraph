package cs8674.DBLPGraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.io.fs.DefaultFileSystemAbstraction;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.kernel.internal.StoreLocker;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;
import org.neo4j.unsafe.batchinsert.BatchInserterIndexProvider;
import org.neo4j.unsafe.batchinsert.BatchInserterIndex;
import org.neo4j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;

import cs8674.core.Article;

public class DatabaseCreator {
	private static final File DB_PATH = new File( "/home/ec2-user/dbCoAuthorImp" );
	static ArrayList<String> visitedCites = new ArrayList<String>();
	GraphDatabaseService graphDb;
	BatchInserter inserter;
	BatchInserterIndex index;
	BatchInserterIndex relIndex;
	BatchInserterIndexProvider lucene;
	//static RelationshipIndex citations ;
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
		//graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
		createIndexOnNode();
		//createIndexOnRelation();
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
			//	System.out.println("created a new node");
			Map<String, Object> prop = new HashMap<String, Object>();
			prop.put("authorName", article.getMainAuthor());
			authorsId = inserter.createNode( prop, label );
			index.add(authorsId, prop);
			index.flush();
			//System.out.println("Inserted:"+ article.getMainAuthor());
		} /*else {
			inserter.setNodeProperties(authorsId, prop);
		}*/

		if(!article.getAuthor().isEmpty())
		{
			//Label labelForCoAuth = Label.label( "author" );
			//System.out.println("co author creation:");
			for(String coAuthors: article.getAuthor()){
				//System.out.println("auth:"+ coAuthors);	
				createRelatedNode(authorsId, coAuthors, article.getMainAuthor(), confId);
			}
			//	inserter.setNodeLabels(authorsId, label, labelForCoAuth);

		}


		
		return authorsId;
	}

	public long nodeExists(Article article) {
		IndexHits<Long> authorExists= index.get("authorName", article.getMainAuthor());
		//	IndexHits<Long> citeExists= index.get("key", article.getKey());
		// if there are nodes that exist with the same key as the cited article
		if(authorExists.size() != 0 ){ 
			return authorExists.getSingle();
		} /*else if(citeExists.size() != 0) {
			return citeExists.getSingle();
		} */else return Long.MIN_VALUE;
	}

	/*********************************************************
	 * 
	 * @param relProp
	 ********************************************************/
	public Map<String, Object> updateProperties(Map<String, Object> relProp) {
		//	System.out.println("author pair:"+ relProp.get("authorPair"));
		int count = (int)relProp.get("articleCount");
		relProp.put("articleCount", count+1);
		return relProp;
	}

	/*********************************************************
	 * 
	 * @param authorId
	 * @param coAuthor
	 * @param mainAuthor
	 ********************************************************/
	public void createRelatedNode(long authorId, String coAuthor, String mainAuthor, long confId) {
		long relId = Long.MIN_VALUE;
		Label labelForCoAuth = Label.label( "author" );
		//	Label label = Label.label( "articleKey" );
		String authorPair = mainAuthor+"--"+coAuthor;
		//System.out.println("author pair:"+ authorPair);


		Map<String, Object> relProp =null;
		IndexHits<Long> authorExists= index.get("authorName",coAuthor);
		IndexHits<Long> relExists= relIndex.get("authorPair", authorPair);
		try {

			if(relExists.size() != 0) {

				long id = relExists.getSingle();
				//for(long id: relExists) {
				relProp = inserter.getRelationshipProperties(id);
				relProp = updateProperties(relProp);
				/*for(String propt: relProp.keySet()){
					System.out.println("properties updated to:"+ relProp.get(propt));
				}*/

				relId = id;
				inserter.setRelationshipProperties(relId, relProp);
				relIndex.updateOrAdd(relId, relProp);
				//relIndex.flush();
				//}
			} else if(authorExists.size() != 0 ) {
				relProp = new HashMap<String, Object>();
				relProp.put("authorPair", authorPair);
				relProp.put("articleCount", 1);
				long id = authorExists.getSingle();
				//	for(long id: authorExists) {
				relId = inserter.createRelationship(authorId, id, RelTypes.CO_AUTHOR, relProp );
				relIndex.add(relId, relProp);
				//inserter.setNodeLabels(id, labelForCoAuth);
				//	long confRelId = 
				inserter.createRelationship(confId,id, RelTypes.HAS, confRelProp);
				//relIndex.updateOrAdd(relId, relProp);
				//	relIndex.updateOrAdd(confRelId, confRelProp);
				//	relIndex.flush();
				//	}
			}
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
				//	long confRelId =
				inserter.createRelationship(confId,coAuthorNode, RelTypes.HAS, confRelProp);
				relIndex.add(relId, relProp);

				//			/relIndex.updateOrAdd(confRelId, confRelProp);
				//	relIndex.flush();
			}
		}
		finally {
			relExists.close();
			authorExists.close();
		}

		
		relIndex.flush();

	}


	public long addArticleNode(Article article) {
		Label label = Label.label( "article" );

		long articleId = Long.MIN_VALUE;

		IndexHits<Long> articleExists= index.get("key",article.getKey());

		try{
			if(articleExists.size() != 0) {
				//	for(long id: articleExists) {
				articleId = articleExists.getSingle();
				//	}
			}
			else {
				Map<String, Object> prop = new HashMap<String, Object>();
				prop.put("title", article.getTitle());
				prop.put("key", article.getKey());
				prop.put("year", article.getYearPublished());
				articleId =	inserter.createNode(prop, label);
				index.add(articleId, prop);
				index.flush();

				//return confId;
			}
		}
		finally {
			articleExists.close();
		}
		
		return articleId;
	}


	public long addConfNode(String key) {
		String[] confName = key.split("/");
		Label label = Label.label( "confName" );

		//System.out.println("conf:"+ confName[1]);
		long confId = Long.MIN_VALUE;

		IndexHits<Long> confExists= index.get("confName",confName[1]);
		try {
			if(confExists.size() != 0) {
				//for(long id: confExists) {
				confId = confExists.getSingle();
				//}
			}
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

		inserter.createRelationship(actualConfId,confId, RelTypes.PUBLISHES, confRelProp);


		//System.out.println("conference id:"+ confId);
		// add author node  to the graph
		authorId = addNodeToGraph(article, confId);

		// new: create relation between main author and conf node
		inserter.createRelationship(confId,authorId, RelTypes.HAS, confRelProp);



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
						confRelId=	inserter.createRelationship(citeConfId, citedId, RelTypes.PUBLISHES, confRelProp );
						//	relIndex.updateOrAdd(confRelId, confRelProp);
						index.add(citedId, prop);
						index.flush();
						/*long relId = inserter.createRelationship(confId, citedId, RelTypes.CITES, prop );
					relIndex.updateOrAdd(relId, prop);*/
					}
					// create relation between the parent article and the cited article
				}
				finally {
					citeExists.close();
				}
				long relId = inserter.createRelationship(confId, citedId, RelTypes.CITES, prop);
				//index.updateOrAdd(citedId, prop);
				//relIndex.updateOrAdd(relId, prop);
				
				relIndex.flush();
			}
		}
		//	return true;
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
