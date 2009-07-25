package com.gulag.neo.model;

import com.gulag.neo.Ownership;
import com.gulag.neo.annotations.Persistence;

public class Artifact extends DomainModelAbstraction<Artifact>{

	public static enum Type{
		Axe, flower
	}
	
	@Persistence(type=Persistence.Type.Property)
	public Type type;
	
	@Persistence(type=Persistence.Type.Relationship, ownership=Ownership.Owns)
	public Person owner;
	
}
