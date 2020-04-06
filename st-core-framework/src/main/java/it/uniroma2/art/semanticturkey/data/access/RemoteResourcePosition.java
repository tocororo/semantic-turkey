package it.uniroma2.art.semanticturkey.data.access;

import it.uniroma2.art.semanticturkey.mdr.core.DatasetMetadata;

public class RemoteResourcePosition extends ResourcePosition {

	private DatasetMetadata meta;

	public RemoteResourcePosition(DatasetMetadata meta) {
		this.meta = meta;
	}

	public DatasetMetadata getDatasetMetadata() {
		return meta;
	}

	@Override
	public String getPosition() {
		return "remote:" + meta.getIdentity();
	}

}
