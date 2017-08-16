package cs8674.DBLPGraph;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.io.fs.FileUtils;

import cs8674.core.Article;

import static org.neo4j.driver.v1.Values.parameters;

import java.io.File;
import java.io.IOException;
/**
 * Hello world!
 *
 */

public class App implements AutoCloseable
{

    public String greeting;
    private static final File DB_PATH = new File( "/home/harshita/dblpFull" );
    // START SNIPPET: vars
    GraphDatabaseService graphDb;
    Node firstNode;
    Node secondNode;
    Relationship relationship;
    // END SNIPPET: vars
    private  Driver driver = null;;
    // START SNIPPET: createReltype
    private static enum RelTypes implements RelationshipType
    {
        KNOWS
    }
    // END SNIPPET: createReltype
    
    App() {}
   /* public App( String uri, String user, String password )
    {
        driver = GraphDatabase.driver( uri, AuthTokens.basic( user, password ) );
    }*/
  /*  public static void main( final String[] args ) throws IOException
    {
    	try{
    		App hello = new App( "bolt://localhost:7687", "neo4j", "password" );
    		//ReadXML reader = new ReadXML("/home/harshita/Desktop/small_in.xml", "");
    	
    		  hello.createDb(reader.article);
    	       // hello.removeData();
    	      hello.shutDown();
    	} catch(Exception e) {
    		
    	}
   //	App hello = new App();
      
    }*/

    //@SuppressWarnings("deprecation")
	void createDb(Article article) throws IOException
    {
    	
    	 //   FileUtils.deleteRecursively( DB_PATH );

        // START SNIPPET: startDb
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
    	
        // END SNIPPET: startDb

        // START SNIPPET: transaction
        try{
        	Transaction tx = graphDb.beginTx(); 
        	System.out.println("CDB--"+ article.getCite().get(1));
            
            // Database operations go here
            // END SNIPPET: transaction
            // START SNIPPET: addData
            /*firstNode = graphDb.createNode();
            firstNode.setProperty( "message", "Hello, " );
            secondNode = graphDb.createNode();
            secondNode.setProperty( "message", "World!" );

            relationship = firstNode.createRelationshipTo( secondNode, RelTypes.KNOWS );
            relationship.setProperty( "message", "brave Neo4j " );*/
        	
        	Node articleNode = graphDb.createNode();
        	articleNode.setProperty("title", article.getBooktitle());
        	articleNode.setProperty("key", article.getKey());
        	
        	for(String cite: article.getCite()) {
        		Node citedNode = graphDb.createNode();
        		citedNode.setProperty("key", cite);
        		relationship = articleNode.createRelationshipTo( citedNode, RelTypes.KNOWS );
        		relationship.setProperty( "message", "brave Neo4j " );
        		
        		  System.out.print( articleNode.getProperty( "key" ) );
                  System.out.print( relationship.getProperty( "message" ) );
                  System.out.print( citedNode.getProperty( "message" ) );
                  System.out.println("********************************");
        	}
        	
        	
        	
            // END SNIPPET: addData

            // START SNIPPET: readData
          
            // END SNIPPET: readData

            greeting = ( (String) firstNode.getProperty( "message" ) )
                       + ( (String) relationship.getProperty( "message" ) )
                       + ( (String) secondNode.getProperty( "message" ) );

            // START SNIPPET: transaction
            tx.success();
        }catch(Exception e) {
        	
        }
        // END SNIPPET: transaction
    }

    /*void removeData()
    {
        try ( Transaction tx = graphDb.beginTx() )
        {
            // START SNIPPET: removingData
            // let's remove the data
            firstNode.getSingleRelationship( RelTypes.KNOWS, Direction.OUTGOING ).delete();
            firstNode.delete();
            secondNode.delete();
            // END SNIPPET: removingData

            tx.success();
        }
    }*/

    void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        // START SNIPPET: shutdownServer
        graphDb.shutdown();
        // END SNIPPET: shutdownServer
    }

    // START SNIPPET: shutdownHook
    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
    // END SNIPPET: shutdownHook

	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
