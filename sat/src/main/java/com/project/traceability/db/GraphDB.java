package com.project.traceability.db;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import com.project.traceability.model.ArtefactElement;
import com.project.traceability.model.ArtefactSubElement;
import com.project.traceability.model.AttributeModel;
import com.project.traceability.model.MethodModel;
import com.project.traceability.model.ParameterModel;


/**
 * Model to add data to graph DB and visualize it.
 * 
 * @author Thanu
 * 
 */
public class GraphDB{

	/**
	 * Define relationship type.
	 * 
	 * @author Thanu
	 * 
	 */
	public static enum RelTypes implements RelationshipType {
		SUB_ELEMENT("Sub Element"), SOURCE_TO_TARGET("Source To Target");
		private final String value;

		private RelTypes(String val) {
			this.value = val;
		}

		@Override
		public String toString() {
			return value;
		}

		public String getValue() {
			return value;
		}

		public static RelTypes parseEnum(final String val) {

			RelTypes relType = null;
			for (RelTypes type : RelTypes.values()) {
				if (type.getValue().equals(val)) {
					relType = type;
				}
			}
			return relType;
		}
	}

	/**
	 * Define Node types.
	 * 
	 * @author Thanu
	 * 
	 */
	private static enum NodeTypes implements RelationshipType {
		CLASS("Class"), FIELD("Field"), METHOD("Method"), UMLATTRIBUTE(
				"UMLAttribute"), UMLOPERATION("UMLOperation");
		private final String value;

		private NodeTypes(String val) {
			this.value = val;
		}

		@Override
		public String toString() {
			return value;
		}

		public String getValue() {
			return value;
		}

		public static NodeTypes parseEnum(final String val) {

			NodeTypes nodeType = null;
			for (NodeTypes type : NodeTypes.values()) {
				if (type.getValue().equals(val)) {
					nodeType = type;
				}
			}
			return nodeType;
		}
	}

	GraphDatabaseService graphDb;
	Relationship relationship;
	private JPanel contentPane;

	public void initiateGraphDB() {

		graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
				"D:\\Neo4j\\atomdb.graphdb").newGraphDatabase();
		Transaction tx = graphDb.beginTx();
		
		try {
			// cleanUp(graphDb);
			tx.success();

		} finally {
			tx.finish();
		}
		registerShutdownHook(graphDb);
	}

	public void addNodeToGraphDB(Map<String, ArtefactElement> aretefactElements) {
		Transaction tx = graphDb.beginTx();
		try {

			Iterator<Entry<String, ArtefactElement>> iterator = aretefactElements
					.entrySet().iterator();
			
			while (iterator.hasNext()) {

				Map.Entry pairs = iterator.next();
				ArtefactElement artefactElement = (ArtefactElement) pairs
						.getValue();
				Label myLabel = DynamicLabel.label(artefactElement.getType());

				IndexManager index = graphDb.index();
				Index<Node> artefacts = index.forNodes("ArtefactElement");

				IndexHits<Node> hits = artefacts.get("ID",
						artefactElement.getArtefactElementId());
				Node node = hits.getSingle();
				if (node == null) {
					Node n = graphDb.createNode();
					node_count++;
					n.addLabel(myLabel);
					n.setProperty("ID", artefactElement.getArtefactElementId());
					n.setProperty("Name", artefactElement.getName());
					n.setProperty("Type", artefactElement.getType());
					addNode(artefactElement.getArtefactElementId(),50,node_count*100);
					artefacts.add(n, "ID", n.getProperty("ID"));
					List<ArtefactSubElement> artefactsSubElements = artefactElement
							.getArtefactSubElements();

					for (int i = 0; i < artefactsSubElements.size(); i++) {
						Node m = graphDb.createNode();
						ArtefactSubElement temp = artefactsSubElements.get(i);
						myLabel = DynamicLabel.label(temp.getType());
						m.addLabel(myLabel);
						m.setProperty("ID", temp.getSubElementId());
						m.setProperty("Name", temp.getName());
						m.setProperty("Type", temp.getType());
						//node_count++;
						int x=node_count*50;
						int y = node_count*50;
						addNode(temp.getSubElementId(),y,x+(100*(i+1)));
						if (null != temp.getVisibility()) {
							m.setProperty("Visibility", temp.getVisibility());
						}
						if (temp.getType().equalsIgnoreCase("UMLOperation")
								|| temp.getType().equalsIgnoreCase("Method")) {
							MethodModel mtemp = (MethodModel) temp;
							if (null != mtemp.getReturnType()) {
								m.setProperty("Return Type",
										mtemp.getReturnType());
							}
							if (null != mtemp.getParameters()) {
								List<ParameterModel> params = mtemp
										.getParameters();
								String parameters = "";
								for (int p = 0; p < params.size(); p++) {
									parameters += params.get(p).getName() + ":"
											+ params.get(p).getVariableType();
									if (p < params.size() - 1)
										parameters += ",";
								}
								m.setProperty("Parameters", parameters);
							}
							if (null != mtemp.getContent()) {
								m.setProperty("Content", mtemp.getContent());
							}
						} else if (temp.getType().equalsIgnoreCase(
								"UMLAttribute")
								|| temp.getType().equalsIgnoreCase("Field")) {
							AttributeModel mtemp = (AttributeModel) temp;
							if (null != mtemp.getVariableType()) {
								m.setProperty("Variable Type",
										mtemp.getVariableType());
							}

						}
						relationship = n.createRelationshipTo(m,
								RelTypes.SUB_ELEMENT);
						addEdge(node_count-1,nodes.size()-1);
						relationship.setProperty("message",
								RelTypes.SUB_ELEMENT.getValue());
					}
				} else {
					if (!node.getProperty("Name").equals(
							artefactElement.getName())) {
						System.out.println("Node name updated");
					} else if (!node.getProperty("Type").equals(
							artefactElement.getType())) {
						System.out.println("Node type updated");
					} else {
						Iterator<Relationship> relations = node
								.getRelationships(RelTypes.SUB_ELEMENT)
								.iterator();
						List<ArtefactSubElement> artefactsSubElements = artefactElement
								.getArtefactSubElements();

						while (relations.hasNext()) {
							Node test = relations.next().getOtherNode(node);
							for (int i = 0; i < artefactsSubElements.size(); i++) {
								if (test.getProperty("ID").equals(
										artefactsSubElements.get(i)
												.getSubElementId())) {
									System.out
											.println("SubElement already exists.....");
									break;
								}
								// System.out.println(test.getProperty("ID")+" "+artefactsSubElements
								// .get(i).getSubElementId());
							}
						}
						System.out.println("Node already exists.....");
					}
				}
			}
			tx.success();

		} finally {
			tx.finish();
		}
	}

	public void addRelationTOGraphDB(List<String> relation) {
		Transaction tx = graphDb.beginTx();
		try {
			IndexManager index = graphDb.index();
			Index<Node> artefacts = index.forNodes("ArtefactElement");

			for (int i = 0; i < relation.size(); i++) {
				IndexHits<Node> hits = artefacts.get("ID", relation.get(i));
				Node source = hits.getSingle();
				hits = artefacts.get("ID", relation.get(++i));
				Node target = hits.getSingle();

				Iterator<Relationship> relations = source.getRelationships()
						.iterator();
				boolean exist = false;
				while (relations.hasNext()) {
					if (relations.next().getOtherNode(source).equals(target)) {
						exist = true;
						System.out.println("Relationship already exists.....");
					}
				}
				if (!exist) {
					relationship = source.createRelationshipTo(target,
							RelTypes.SOURCE_TO_TARGET);
					relationship.setProperty("message",
							RelTypes.SOURCE_TO_TARGET.getValue());
				}
			}
			tx.success();
			PreviewJFrame preview = new PreviewJFrame();
			preview.script(graphDb);
		} finally {
			tx.finish();
		}
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	@SuppressWarnings("deprecation")
	public void cleanUp(final GraphDatabaseService graphDb) {
		// ReadableIndex<Node> autoNodeIndex = graphDb.index()
		// .getNodeAutoIndexer().getAutoIndex();
		IndexManager index = graphDb.index();
		Index<Node> actors = index.forNodes("ArtefactElement");
		actors.delete();
		for (Node node : graphDb.getAllNodes()) {
			for (Relationship rel : node.getRelationships()) {
				rel.delete();
			}
			// nodeIndex.remove(node);
			node.delete();
		}
	}

	public GraphDB() {
//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		setBounds(100, 100, 794, 653);
//		contentPane = new JPanel();
//		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
//		contentPane.setLayout(null);
//		setContentPane(contentPane);
//		
//		nodes = new ArrayList<GraphNode>();
//		edges = new ArrayList<GraphEdge>();
//		width = 60;
//		height = 60;
//	      

//		final JTextArea queryTextArea = new JTextArea();
//		queryTextArea.setBounds(25, 40, 702, 109);
//		queryTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
//		queryTextArea.setText("start n=node(*) return n, n.name");
//		contentPane.add(queryTextArea);
//
//		JLabel cypherQueryLabel = new JLabel("Cypher Query");
//		cypherQueryLabel.setBounds(25, 12, 105, 23);
//		contentPane.add(cypherQueryLabel);
//
//		final JTextArea resultTextArea = new JTextArea();
//		resultTextArea.setBounds(25, 181, 702, 341);
//		resultTextArea.setEditable(false);
//		resultTextArea.setBorder(new EmptyBorder(5, 5, 5, 5));
//		contentPane.add(resultTextArea);
//
//		JLabel resultLabel = new JLabel("Result:");
//		resultLabel.setBounds(25, 323, 55, 23);
//
//		contentPane.add(resultLabel);
//
//		JButton btnNewButton_2 = new JButton("Execute Query");
//		btnNewButton_2.setBounds(274, 540, 176, 41);
//		btnNewButton_2.setVisible(true);
//		contentPane.add(btnNewButton_2);
//
//		btnNewButton_2.addActionListener(new ActionListener() {
//
//			public void actionPerformed(ActionEvent e) {
//
//				ExecutionEngine engine = new ExecutionEngine(graphDb);
//				ExecutionResult result = engine.execute(queryTextArea.getText());
//				String rows = "";
//				for (Map<String, Object> row : result) {
//					for (Entry<String, Object> column : row.entrySet()) {
//						rows += column.getKey() + ": " + column.getValue()
//								+ "; ";
//					}
//					rows += "\n";
//				}
//				resultTextArea.setText(rows);
//			}
//		});
	}
	int width;
    int height;
    int node_count=0;
    ArrayList<GraphNode> nodes;
    ArrayList<GraphEdge> edges;

	class GraphNode {
		int x, y;
		String name;

		public GraphNode(String myName, int myX, int myY) {
			x = myX;
			y = myY;
			name = myName;
		}
	}

	class GraphEdge {
		int i, j;
		public GraphEdge(int ii, int jj) {
			i = ii;
			j = jj;
		}
	}

	public void addNode(String name, int x, int y) {
		// add a node at pixel (x,y)
		nodes.add(new GraphNode(name, x, y));
		//this.repaint();
	}

	public void addEdge(int i, int j) {
		// add an edge between nodes i and j
		edges.add(new GraphEdge(i, j));
		//this.repaint();
	}

//	public void paint(Graphics g) { // draw the nodes and edges
//		FontMetrics f = g.getFontMetrics();
//		int nodeHeight = Math.max(height, f.getHeight());
//
//		g.setColor(Color.black);
//		for (GraphEdge e : edges) {
//			g.drawLine(nodes.get(e.i).x, nodes.get(e.i).y, nodes.get(e.j).x,
//					nodes.get(e.j).y);
//			g.setColor(Color.YELLOW);
//		}
//
//		for (GraphNode n : nodes) {
//			int nodeWidth = Math.max(width, f.stringWidth(n.name) + width / 2);
//			g.setColor(new Color(new Random().nextInt()));
//			g.fillOval(n.x - nodeWidth / 2, n.y - nodeHeight / 2, nodeWidth,
//					nodeHeight);
//			g.setColor(Color.black);
//			g.drawOval(n.x - nodeWidth / 2, n.y - nodeHeight / 2, nodeWidth,
//					nodeHeight);
//
//			g.drawString(n.name, n.x - f.stringWidth(n.name) / 2,
//					n.y + f.getHeight() / 2);
//			
//		}
//	}

}
