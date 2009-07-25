package com.gulag.neo;

import org.neo4j.api.core.RelationshipType;

public class NamedRelationshipType implements RelationshipType {

	private String name;
	
	public NamedRelationshipType(String name){
		this.name = name;
	}
	
	@Override
	public String name() {
		return name;
	}

}
