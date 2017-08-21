package cs8674.DBLPGraph;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
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

/**************************************************************************
 * 
 * @author harshita
 *
 * Class used to perform adhoc operations on the graph database
 *************************************************************************/
public class NodeInfo {
	private static GraphDatabaseService db;
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


	NodeInfo () throws IOException {
		db = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
	}


	// main method
	public static void main(String[] args) throws IOException {
		NodeInfo tg = new NodeInfo();
		try ( Transaction ignored = db.beginTx();
				Result result = db.execute("MATCH () RETURN COUNT(*) AS node_count");	
				ResourceIterator<Node> articles= result.columnAs("node_count"))
		{
			while ( articles.hasNext() )
			{
				System.out.println("Number of nodes:"+articles.next());

			}
		}

		try ( Transaction ignored = db.beginTx();
				Result result2 = db.execute("MATCH ()-->() RETURN COUNT(*) AS rel_count");	
				ResourceIterator<Node> articles= result2.columnAs("rel_count"))
		{
			while ( articles.hasNext() )
			{	System.out.println("Number of relationships:"+articles.next());

			}
		}

	}
}

