# DBLPGraph
<h2> CONFIGURTION INSTRUCTIONS: </h2>
Environment used to build the project: JDK 1.8 , JAVA v.8<br>
Pre-requisite: Java Development Environment, Maven, neo4j 3.2.0
<h2>Project structure</h2>
src/main

 |_ core: contains the core classes used to encapsulate the parsed xml information
 
 |_ dblpgraph: contains the application logic to create a neo4j database and traverse it
 
<h2> Aim and Design </h2>
This project investigates and builds an initial version of a tool to ingest, process and handle queries on the dblp academic citations dataset. This dataset covers papers from over 5000 conferences and hundreds of journals, and is a rich source for investigating creating novel tools for analyzing academic communities

The main aims were to:

· Develop text/citation processing tools to ingest raw information from the dplb dataset. This is a large XML file and will require careful processing to handle its scale.

· Design a flexible database schema to store bibliographic information and the relationships between publications that are expressed as citations

· Investigate, propose and prototype queries that can be asked of the data in the database. As the data naturally forms a graph, investigating graph analysis techniques will lead to some interesting approaches that have not been tried before on such data.

· Prototype a query frontend to demonstrate the proposed queries. 
 
<h2> What is Neo4j? </h2>
Neo4j is a graph database management system developed by Neo Technology, Inc. Described by its developers as an ACID-compliant transactional database with native graph storage and processing, Neo4j is the most popular graph database according to db-engines.com.

<h2> What kind of graph the application creates? </h2>
For each article, the application parses the information to get the conference, author name, year of publication, title and the article key.
it is essential to get a relationship between all these sets of information in order to analyze and visualize an authors conrtribution.
Each of the information is modeled into nodes and properties.
It is very important to model the data right in order to achieve max throughput from the neo4j engine.


This application creates the following nodes:
1. Conference Node: conference where an article was published
2. Article node: The article under concern
3. Author node: The authors of the articles

Relations:
Each node is related to another node by some relationship type:
1. Conferece - Article relation is "PUBLISHES"
2. Article - Article relation is "CITES", for the articles which cite another article
3. Article - Author relation is "HAS"
4. Author - Author relation is "CO_AUTHOR". This relation has a relationship property called the "article count" which gives the total number of articles this author pair has published together.

<h2>Visualize the graph</h2>
The entire graph db is hosted on the ec2 instance. 
Follow the link to visualize the graph in the neo4j browser: 
http://ec2-18-220-59-135.us-east-2.compute.amazonaws.com:7474/browser/

[Screenshot of how the graph looks for the author "Sridhar Ramaswamy"](/image/image.png?raw=true "Optional Title")



<h2>Front end to the graph database</h2>
The repository https://github.com/Harshita27/AuthorGeneologyUI contains the frontend web app to connect to the ec2 instance and fires up sample queries on the DBLP graph created. This repository lists some example queries that you can run.


<h2>How to run the application</h2>
It is advised to get the database created in an ec2 instance because of the large size of the database.

1. Database Creation: Run the jar at Jars/DBLP_GraphCreation.jar with the following arguments:

java -jar DBLP_GraphCreation.jar /path/tp/dblp/xml 

The application currently creates the database on an ec2 instance, and the database path has been set to that.

2. Database Traversal: Run the jar at Jars/DBLP_GraphTraversal.jar

java -jar DBLP_GraphTraversal.jar

This jar looks for the database stored at the ec2 location as specified in the previous step
