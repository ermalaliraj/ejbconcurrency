package com.ea.ejbconcurrency.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PointEntity implements Serializable {

	private static final long serialVersionUID = 9132153729708637906L;

	@Id
	private String name;
	
	public PointEntity() {}
	
	public PointEntity(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
       String rv = "["+name+"]";
       return rv;
       
    }
	
}
