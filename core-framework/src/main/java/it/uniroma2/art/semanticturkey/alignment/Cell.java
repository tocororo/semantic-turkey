package it.uniroma2.art.semanticturkey.alignment;

import org.eclipse.rdf4j.model.IRI;

public class Cell {
	
	private IRI entity1;
	private IRI entity2;
	private float measure;
	private String relation;
	private IRI mappingProperty;
	private AlignmentModel.Status status;
	private String comment;
	
	public Cell(IRI entity1, IRI entity2, float measure, String relation) {
		this.entity1 = entity1;
		this.entity2 = entity2;
		this.measure = measure;
		this.relation = relation;
	}

	public IRI getEntity1() {
		return entity1;
	}

	public void setEntity1(IRI entity1) {
		this.entity1 = entity1;
	}

	public IRI getEntity2() {
		return entity2;
	}

	public void setEntity2(IRI entity2) {
		this.entity2 = entity2;
	}

	public float getMeasure() {
		return measure;
	}

	public void setMeasure(float measure) {
		this.measure = measure;
	}

	public String getRelation() {
		return relation;
	}

	public void setRelation(String relation) {
		this.relation = relation;
	}
	
	
	public IRI getMappingProperty() {
		return mappingProperty;
	}

	public void setMappingProperty(IRI mappingProperty) {
		this.mappingProperty = mappingProperty;
	}

	public AlignmentModel.Status getStatus() {
		return status;
	}

	public void setStatus(AlignmentModel.Status status) {
		this.status = status;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public boolean equals(Object cell) {
		return (
			((Cell) cell).getEntity1().equals(this.getEntity1()) &&
			((Cell) cell).getEntity2().equals(this.getEntity2()) &&
			((Cell) cell).getMeasure() == this.getMeasure() &&
			((Cell) cell).getRelation().equals(this.getRelation())
		);
	}
	
	@Override
	public String toString() {
		String toStr = "{ Entity1: " + entity1.stringValue() + ", " +
				"Entity2: " + entity2.stringValue() + ", " +
				"Relation: " + relation + ", " +
				"Measure: " + measure;
		if (status != null) {
			toStr = toStr + "Status: " + status;
		}
		if (mappingProperty != null) {
			toStr = toStr + ", MappingProperty: " + mappingProperty.stringValue();
		}
		if (comment != null) {
			toStr = toStr + ", Comment: " + comment;
		}
		toStr = toStr + "}";
		return toStr;
	}

}
