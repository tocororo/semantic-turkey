package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.semanticturkey.resources.DatasetMetadata;

public class RemoteResourcePosition extends ResourcePosition {

	private DatasetMetadata meta;

	public RemoteResourcePosition(DatasetMetadata meta) {
		this.meta = meta;
	}

	public DatasetMetadata getDatasetMetadata() {
		return meta;
	}

	@Override
	public String toString() {
		return "remote:" + meta.getBaseURI();
	}
}
