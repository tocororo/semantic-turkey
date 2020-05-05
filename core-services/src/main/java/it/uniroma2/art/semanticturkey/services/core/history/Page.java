package it.uniroma2.art.semanticturkey.services.core.history;

import java.util.List;

/**
 * A page of a paged-list.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class Page<T> {

	private List<T> items;
	private boolean next;
	
	public Page(List<T> items, boolean next) {
		this.items = items;
		this.next = next;
	}

	public List<T> getItems() {
		return items;
	}
	
	public void setItems(List<T> items) {
		this.items = items;
	}
	
	public boolean isNext() {
		return next;
	}
	
	public void setNext(boolean next) {
		this.next = next;
	}
	
	public static <Q> Page<Q> build(List<Q> items, boolean next) {
		return new Page<>(items, next);
	}

	public static <Q> Page<Q> build(List<Q> commitInfos, int limit) {
		boolean next = commitInfos.size() > limit;
		return build(next ? commitInfos.subList(0, limit): commitInfos, next);
	}
}
