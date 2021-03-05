package it.uniroma2.art.semanticturkey.extension.impl.reformattingexporter.spreadsheetserializer.structures;

import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.List;

public class CollectionInfo extends ResourceInfo {
	private final List<ResourceInfo> memeberList = new ArrayList<>();

	public CollectionInfo(IRI collectionIRI) {
		super(collectionIRI);
	}

	public List<ResourceInfo> getMemeberList() {
		return memeberList;
	}

	public boolean addMember(ResourceInfo member){
		if(memeberList.contains(member)){
			return false;
		}
		memeberList.add(member);
		return true;
	}
}
