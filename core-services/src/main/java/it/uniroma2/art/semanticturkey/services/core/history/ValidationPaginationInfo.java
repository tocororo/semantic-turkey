package it.uniroma2.art.semanticturkey.services.core.history;

import java.util.GregorianCalendar;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

/**
 * Information about a paginated set of staged commits to be validated.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ValidationPaginationInfo {

	private final GregorianCalendar tipTime;
;
	private final long pageCount;
	
	public ValidationPaginationInfo(GregorianCalendar tipTime, long pageCount) {
		this.tipTime = tipTime;
		this.pageCount = pageCount;
	}
	
	@JsonFormat(shape=Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public GregorianCalendar getTipTime() {
		return tipTime;
	}
	
	public long getPageCount() {
		return pageCount;
	}
}
