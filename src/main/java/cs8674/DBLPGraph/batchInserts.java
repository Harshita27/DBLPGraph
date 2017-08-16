package cs8674.DBLPGraph;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.kernel.internal.EmbeddedGraphDatabase;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

public class batchInserts {
	private static final File DB_PATH = new File( "/home/harshita/dFull-3" );
	public void batchDb() throws IOException
    {
   //     FileUtils.deleteRecursively( new File( "target/batchdb-example" ) );
        
		  // START SNIPPET: insert
		
		
        BatchInserter inserter = BatchInserters.inserter( DB_PATH );
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "name", "Mattias" );
        long mattiasNode = inserter.createNode( properties );
        properties.put( "name", "Chris" );
        long chrisNode = inserter.createNode( properties );
        RelationshipType knows = DynamicRelationshipType.withName( "KNOWS" );
        // To set properties on the relationship, use a properties map
        // instead of null as the last parameter.
        inserter.createRelationship( mattiasNode, chrisNode, knows, null );
        inserter.shutdown();
        // END SNIPPET: insert

        // try it out from a normal db
        GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        Node mNode = db.getNodeById( mattiasNode );
        Node cNode = mNode.getSingleRelationship( knows, Direction.OUTGOING )
                .getEndNode();
       db.shutdown();
    }
}
