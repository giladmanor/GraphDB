package com.gulag.neo;

import com.gulag.neo.model.Person;
import static com.gulag.neo.NeoPersistanceUtility.*;

public class NeoTest {

	/**
	 * what can i say, this is the test..
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		Person p = new Person("gilad", "g");
		Person o = new Person("other", "o");
		p.addFriend(o);
		o.addFoe(p);
		
		
		log("the id for the new person = "+save(p));
		
		shutdown();
		

	}


}
