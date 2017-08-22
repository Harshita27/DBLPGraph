# DBLPGraph
<h2> CONFIGURTION INSTRUCTIONS: </h2>
Environment used to build the project: JDK 1.8 , JAVA v.8<br>
Pre-requisite: Java Development Environment, Maven, neo4j 3.2.0<br> <br>

<h2>Project structure</h2>
src/main

 |_ core: contains the core classes used to encapsulate the parsed xml information
 
 |_ dblpgraph: contains the application logic to create a neo4j database and traverse it
 
<h2> What is Neo4j? </h2>
Neo4j is a graph database management system developed by Neo Technology, Inc. Described by its developers as an ACID-compliant transactional database with native graph storage and processing, Neo4j is the most popular graph database according to db-engines.com.

<h2> What graph the application creates? </h2>
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
For example, below is a screenshot of how the graph looks for the author "Sridhar Ramaswamy"

[Alt text](/image/image.png?raw=true "Optional Title")



<h2>Front end to the graph database</h2>
The repository https://github.com/Harshita27/AuthorGeneologyUI contains the frontend web app to connect to the ec2 instance and fires up sample queries on the DBLP graph created.

