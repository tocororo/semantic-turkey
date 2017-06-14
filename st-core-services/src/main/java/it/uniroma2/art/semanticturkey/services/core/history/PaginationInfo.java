package it.uniroma2.art.semanticturkey.services.core.history;

/**
 * Information about a paginated sequence of commits.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class PaginationInfo {

	private final long tipRevisionNumber;
	private final long pageCount;
	
	public PaginationInfo(long tipRevisionNumber, long pageCount) {
		this.tipRevisionNumber = tipRevisionNumber;
		this.pageCount = pageCount;
	}
	
	public long getTipRevisionNumber() {
		return tipRevisionNumber;
	}
	
	public long getPageCount() {
		return pageCount;
	}
}
