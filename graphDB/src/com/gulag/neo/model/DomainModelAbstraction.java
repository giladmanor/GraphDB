package com.gulag.neo.model;

import com.gulag.neo.NeoPersistanceUtility;

public abstract class DomainModelAbstraction<T> {

	public void save(){
		NeoPersistanceUtility.save(this);
	}
	
	public T findById(long id){
		return (T)NeoPersistanceUtility.findById(id);
	}
	
	
	
}
