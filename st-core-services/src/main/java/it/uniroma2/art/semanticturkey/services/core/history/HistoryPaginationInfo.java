package it.uniroma2.art.semanticturkey.services.core.history;

/**
 * Information about a paginated history.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class HistoryPaginationInfo {

	private final long tipRevisionNumber;
	private final long pageCount;
	
	public HistoryPaginationInfo(long tipRevisionNumber, long pageCount) {
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
