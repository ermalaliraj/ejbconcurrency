package com.ea.ejbconcurrency.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@Entity
public class SegmentEntity implements Serializable {

	private static final long serialVersionUID = 9132153729708637906L;

	@Id
	@GeneratedValue
	private Integer id;
	private String point1;
	private String point2;
	
	public SegmentEntity() {}
	
	public SegmentEntity(String s1, String s2) {
		this.point1 = s1;
		this.point2 = s2;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getPoint1() {
		return point1;
	}
	public void setPoint1(String point1) {
		this.point1 = point1;
	}
	public String getPoint2() {
		return point2;
	}
	public void setPoint2(String point2) {
		this.point2 = point2;
	}

	public boolean equals(final Object other) {
        if (!(other instanceof SegmentEntity))
            return false;
        SegmentEntity o = (SegmentEntity) other;
      
        return new EqualsBuilder()
        	.append(id, o.id)
        	.append(point1, o.point1)
        	.append(point1, o.point1)
			.isEquals()
			;
    }
	
    public int hashCode() {
        return new HashCodeBuilder()
        		.append(id)
        		.append(point1)
        		.append(point2)
        		.toHashCode();
    }
	
	public String toString() {
		String rv = "[" + id + ", " + point1 + "-" + point2 + "]";
		return rv;
    }
	
}
