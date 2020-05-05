package it.uniroma2.art.semanticturkey.mvc;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.RequestContextHolder;

/**
 * Holder class to expose the (optionally wrapped) web request in the form of a thread-bound object. It has
 * been introduced, because {@link RequestContextHolder} only stores the original request (i.e. without the
 * wrapper used to resolve multi-part request bodies).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class ResolvedRequestHolder {
	private static ThreadLocal<HttpServletRequest> requestHolder = new ThreadLocal<>();

	/**
	 * A package-protected setter
	 * 
	 * @param request
	 */
	static void setRequest(HttpServletRequest request) {
		requestHolder.set(request);
	}

	/**
	 * A package-protected operation to remove the thread-bound request
	 */
	static void removeRequest() {
		requestHolder.remove();
	}

	/**
	 * Returns the request bound to the current thread
	 * 
	 * @return
	 */
	static public HttpServletRequest get() {
		return requestHolder.get();
	}
}
