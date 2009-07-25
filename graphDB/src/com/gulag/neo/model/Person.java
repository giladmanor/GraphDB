package com.gulag.neo.model;

import java.util.ArrayList;
import java.util.List;

import com.gulag.neo.Ownership;
import com.gulag.neo.Peers;
import com.gulag.neo.annotations.Persistence;

/**
 * 
 * 
 * 
 * this is my example POJO containing data and pear minimum functionality for
 * setters and getters i've set the fields declaration to public for easy access
 * in the runtime reflection, in real life i would probably keep them as private
 * and have a smart method for finding the getter according to a naming
 * convention.
 * 
 * i've loaded each property with an annotation to instruct the run time
 * processing on how to relate to the particular field
 * 
 * @author gilad
 */
public class Person extends DomainModelAbstraction<Person>{

	@Persistence(type = Persistence.Type.Index)
	public String name;

	@Persistence(type = Persistence.Type.Property)
	public String nickname;

	@Persistence(type = Persistence.Type.Relationship, peer = Peers.Friend)
	public List<Person> friends;

	@Persistence(type = Persistence.Type.Relationship, peer = Peers.Foe)
	public List<Person> foes;

	@Persistence(type = Persistence.Type.Relationship, ownership = Ownership.Owns)
	public Artifact artifact;
	
	public Person(String name, String nickname) {
		super();
		this.name = name;
		this.nickname = nickname;

		friends = new ArrayList<Person>();
		foes = new ArrayList<Person>();
	}

	public void setArtifact(Artifact artifact){
		if (this.artifact!=null){
			throw new RuntimeException("hands are full");
		}
		this.artifact=artifact;
	}
	
	public Artifact getArtifact(){
		if(artifact!=null){
			Artifact res = artifact;
			artifact = null;
			return res;
		}
		return null;
	}
	
	public boolean isHoldingArtifact(){
		if(artifact!=null){
			return true;
		}else{
			return false;
		}
	}
	
	public String getName() {
		return name;
	}

	public String getNickname() {
		return nickname;
	}

	public void addFriend(Person friend) {
		friends.add(friend);
	}

	public void addFoe(Person foe) {
		foes.add(foe);
	}

	public void removeFriend(Person friend) {
		friends.remove(friend);
	}

	public void removeFoe(Person foe) {
		foes.remove(foe);
	}

}
