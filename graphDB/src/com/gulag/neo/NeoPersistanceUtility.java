package com.gulag.neo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.api.core.EmbeddedNeo;
import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.RelationshipType;
import org.neo4j.api.core.Transaction;
import org.neo4j.util.index.IndexService;
import org.neo4j.util.index.LuceneIndexService;

import com.gulag.neo.annotations.Persistence;

public class NeoPersistanceUtility {

	// the store directory is equivalent to the name of the database schema
	private static String storeDir = "NeoStorage/test";

	// this is here because its only an example and i'm lazy
	private static NeoService neo;
	private static IndexService neoIndexService;
	private static Map<Class, Node> indexAncores;

	static {
		neo = new EmbeddedNeo(storeDir);
		neoIndexService = new LuceneIndexService(neo);
		indexAncores = new HashMap<Class, Node>();

		// invoke the neo shutdown when the jvm is terminated
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				shutdown();
			}
		});
	}

	/**
	 * according to the neo4j instructions, its crucial to shutdown the database
	 * before terminating the vm (i.e. this test) in case you forget to
	 * shutdown, the next time the database will initiate, it will take a moment
	 * to try and recover.
	 * 
	 */
	public static void shutdown() {
		neoIndexService.shutdown();
		neo.shutdown();

	}

	/**
	 * this is a simple finder, that utilizes the find by id method on neo
	 * 
	 * @param id
	 *            of the desired node
	 * @return Node the node sought for
	 */
	public static Node findById(long id) {
		Transaction tx = neo.beginTx();
		Node res = neo.getNodeById(id);
		tx.finish();
		return res;

	}

	/**
	 * this is a recursive method that analyzes a particular field on the POJO,
	 * if the annotation on the field indicates that this is a simple property,
	 * it will set that property if the annotation indicates that this is a
	 * relation, it will recursively create a node for the child object and link
	 * the original with the new
	 * 
	 * note: this implementation is very naive and handles a very specific
	 * scenario
	 * 
	 * @param node
	 * @param object
	 * @param field
	 * @param stack
	 * @throws RuntimeException
	 * @throws Exception
	 */
	private static void processFieldData(Node node, Object object, Field field,
			Map<Object, Node> stack) throws Throwable {

		// this bit here has allot to do with processing the annotations that
		// are set on the POJO, the neo stuff come later on
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if (annotation instanceof Persistence) {
				Persistence p = (Persistence) annotation;
				log("processing annotation: " + p.type());

				if (p.type().equals(Persistence.Type.Index)) {
					index(node, object, field);
					createProperty(node, object, field);
				}
				if (p.type().equals(Persistence.Type.Property)) {
					createProperty(node, object, field);
				} else if (p.type().equals(Persistence.Type.Relationship)) {
					Object value = field.get(object);
					if (!p.peer().equals(Peers.NA)) {

						log("setting peer: " + field.getName() + " " + value);
						if (value instanceof List) {
							for (Object item : (List) value) {
								createRelationship(node, stack, p.peer(), item);
							}
						} else {
							// handle a nested object
						}
					} else if (!p.ownership().equals(Ownership.NA)) {
						createRelationship(node, stack, p.ownership(), value);
					} else {
						// handle other options
					}

				}
				// processing the persistence annotation is enough
				return;
			}
		}
	}

	private static void index(Node node, Object object, Field field)
			throws Throwable {
		neoIndexService.index(node, field.getName(), field.get(object));

	}

	private static void createRelationship(Node node, Map<Object, Node> stack,
			RelationshipType relationType, Object otherObject) throws Throwable {
		Node otherNode = objectToNode(otherObject, stack);
		node.createRelationshipTo(otherNode, relationType);
	}

	private static void createProperty(Node node, Object object, Field field)
			throws IllegalAccessException {
		Object value = field.get(object);
		log("setting property: " + field.getName() + " " + value);

		// here is a neo setting of a property
		node.setProperty(field.getName(), value);
	}

	/**
	 * this method converts an object into a node and returns it
	 * 
	 * @param object
	 * @param stack
	 * @return
	 * @throws RuntimeException
	 * @throws Exception
	 */
	private static Node objectToNode(Object object, Map<Object, Node> stack)
			throws Throwable {

		// the stack in this case is used prevent looping forever on a circular
		// data model. in case the stack is null, i assume this is the root
		// object, i create the stack and add the current node as the first one
		if (stack == null) {
			stack = new HashMap<Object, Node>();
		} else if (stack.containsKey(object)) {
			return stack.get(object);
		}
		Node node = createNode(object.getClass());
		stack.put(object, node);
		Class cls = object.getClass();

		Field[] fields = cls.getDeclaredFields();
		for (Field field : fields) {
			// only persisting the fields that have my special annotation
			if (field.isAnnotationPresent(Persistence.class)) {
				processFieldData(node, object, field, stack);
			}
		}
		return node;

	}

	private static Node createNode(Class cls) {
		Node referenceNode = null;
		if(indexAncores.containsKey(cls)){
			referenceNode = indexAncores.get(cls);
		}else{
			referenceNode = neo.createNode();
			Node root = neo.getReferenceNode();
			root.createRelationshipTo(referenceNode,new NamedRelationshipType(cls.getName()));
			
			indexAncores.put(cls, referenceNode);
		}
		Node node = neo.createNode();
		
		referenceNode.createRelationshipTo(node, new NamedRelationshipType("type"));
		
		return node;
	}

	/**
	 * the save method takes in any object and provided it has the @Persistance
	 * annotation, it will convert it into the neo node structure the
	 * implementation is rather simple and straight forward, and the conversion
	 * into properties or links is according to the annotation specification
	 * 
	 * @param object
	 * @return long - the neo id of the root node
	 */
	public static long save(Object object) {

		Transaction tx = neo.beginTx();
		Node node = null;
		try {
			node = objectToNode(object, null);
			tx.success();
			return node.getId();
		} catch (Throwable t) {
			tx.failure();
			t.printStackTrace();
		} finally {
			tx.finish();
		}

		// getting here means something went wrong
		throw new RuntimeException();

	}

	public static void log(String message) {
		System.out.println(message);
	}

}
