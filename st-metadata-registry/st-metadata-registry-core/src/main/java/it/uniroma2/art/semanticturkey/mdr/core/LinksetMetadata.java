package it.uniroma2.art.semanticturkey.mdr.core;

import java.util.List;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;

import com.google.common.base.MoreObjects;

/**
 * Metadata describing a lexicalization set
 *
 */
public class LinksetMetadata {
	public static class Target {
		private IRI dataset;
		private String projectName;
		private Optional<String> uriSpace;
		private List<Literal> titles;

		public IRI getDataset() {
			return dataset;
		}

		public String getProjectName() {
			return projectName;
		}

		public void setProjectName(String projectName) {
			this.projectName = projectName;
		}

		public void setDataset(IRI dataset) {
			this.dataset = dataset;
		}

		public Optional<String> getUriSpace() {
			return uriSpace;
		}

		public void setUriSpace(Optional<String> uriSpace) {
			this.uriSpace = uriSpace;
		}

		public List<Literal> getTitles() {
			return titles;
		}

		public void setTitles(List<Literal> titles) {
			this.titles = titles;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("dataset", dataset).add("projectName", projectName)
					.add("uriSpace", uriSpace).add("titles", titles).toString();
		}

	}

	private IRI sourceDataset;
	private Target targetDataset;
	private List<Target> registeredTargets;
	private Optional<Integer> linkCount;
	private Optional<IRI> linkPredicate;

	public IRI getSourceDataset() {
		return sourceDataset;
	}

	public void setSourceDataset(IRI sourceDataset) {
		this.sourceDataset = sourceDataset;
	}

	public Target getTargetDataset() {
		return targetDataset;
	}

	public void setTargetDataset(Target targetDataset) {
		this.targetDataset = targetDataset;
	}

	public void setRegisteredTargets(List<Target> registeredTargets) {
		this.registeredTargets = registeredTargets;
	}

	public List<Target> getRegisteredTargets() {
		return registeredTargets;
	}

	public Optional<Integer> getLinkCount() {
		return linkCount;
	}

	public void setLinkCount(Optional<Integer> linkCount) {
		this.linkCount = linkCount;
	}

	public Optional<IRI> getLinkPredicate() {
		return linkPredicate;
	}

	public void setLinkPredicate(Optional<IRI> linkPredicate) {
		this.linkPredicate = linkPredicate;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("sourceDataset", sourceDataset)
				.add("targetDataset", targetDataset).add("registeredTargets", registeredTargets)
				.add("linkCount", linkCount).add("linkPredicate", linkPredicate).toString();
	}
}
