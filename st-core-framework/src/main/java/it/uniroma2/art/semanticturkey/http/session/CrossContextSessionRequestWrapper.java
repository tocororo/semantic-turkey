package it.uniroma2.art.semanticturkey.http.session;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.apache.commons.collections.EnumerationUtils;

public class CrossContextSessionRequestWrapper extends HttpServletRequestWrapper {

	protected static ConcurrentHashMap<String, SimpleHttpSessionData> sessionManager = new ConcurrentHashMap();

	private HttpServletResponse response;

	public CrossContextSessionRequestWrapper(HttpServletRequest request, HttpServletResponse response) {
		super(request);
		this.response = response;
	}

	@Override
	public HttpSession getSession() {
		return this.getSession(true);
	}

	@Override
	public HttpSession getSession(boolean create) {
		HttpServletRequest request = (HttpServletRequest) getRequest();

		SimpleHttpSessionData sessionData = null;
		if (request.getCookies() != null) {
			for (Cookie c : request.getCookies()) {
				if (c.getName().equals("JSESSIONID")) {
					sessionData = sessionManager.get(c.getValue());
					if (sessionData != null)
						break;
				}
			}
		}

		if (!create && sessionData == null) {
			if (request.getCookies() != null) {
				for (Cookie c : request.getCookies()) {
					if (c.getName().equals("JSESSIONID")) {
						c.setMaxAge(0);
						response.addCookie(c);
					}
				}
			}
		}

		if (sessionData != null) {
			sessionData.setIsNew(false);
		} else {
			sessionData = new SimpleHttpSessionData();
			sessionData.setId(UUID.randomUUID().toString());
			sessionData.setCreationTime(System.currentTimeMillis());
			sessionData.setIsNew(true);
			sessionData.setMaxInactiveInterval(60 * 60);
			sessionManager.put(sessionData.getId(), sessionData);
		}

		sessionData.updateLastAccessedTime();

		if (sessionData.isNew()) {
			Cookie cookie = new Cookie("JSESSIONID", sessionData.getId());
			cookie.setMaxAge(24 * 60 * 60);
			cookie.setPath("/semanticturkey");
			response.addCookie(cookie);
		}

		return new SimpleHttpSession(this, sessionData, request);
	}
}

class SimpleHttpSession implements HttpSession {
	private CrossContextSessionRequestWrapper wrapper;
	private SimpleHttpSessionData sessionData;
	private HttpServletRequest httpServletRequest;

	public SimpleHttpSession(CrossContextSessionRequestWrapper wrapper, SimpleHttpSessionData sessionData,
			HttpServletRequest httpServletRequest) {
		this.wrapper = wrapper;
		this.sessionData = sessionData;
		this.httpServletRequest = httpServletRequest;
	}

	@Override
	public long getCreationTime() {
		return sessionData.getCreationTime();
	}

	@Override
	public String getId() {
		return sessionData.getId();
	}

	@Override
	public long getLastAccessedTime() {
		return sessionData.getLastAccessedTime();
	}

	@Override
	public ServletContext getServletContext() {
		return httpServletRequest.getSession().getServletContext();
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		sessionData.setMaxInactiveInterval(interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		return sessionData.getMaxInactiveInterval();
	}

	@Override
	public HttpSessionContext getSessionContext() {
		return new HttpSessionContext() {

			@Override
			public HttpSession getSession(String sessionId) {
				return null;
			}

			@Override
			public Enumeration getIds() {
				return Collections.emptyEnumeration();
			}
		};
	}

	@Override
	public Object getAttribute(String name) {
		return sessionData.getAttribute(name);
	}

	@Override
	public Object getValue(String name) {
		return this.getAttribute(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		return sessionData.getAttributeNames();
	}

	@Override
	public String[] getValueNames() {
		String[] array = new String[] {};
		return ((List<String>) EnumerationUtils.toList(getAttributeNames())).toArray(array);
	}

	@Override
	public void setAttribute(String name, Object value) {
		sessionData.setAttribute(name, value);
	}

	@Override
	public void putValue(String name, Object value) {
		sessionData.setAttribute(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		sessionData.removeAttribute(name);
	}

	@Override
	public void removeValue(String name) {
		sessionData.removeAttribute(name);
	}

	@Override
	public void invalidate() {
		CrossContextSessionRequestWrapper.sessionManager.remove(sessionData.getId());
		if (httpServletRequest.getCookies() != null) {
			Arrays.stream(httpServletRequest.getCookies())
					.filter(c -> c.getName().equals("JSESSIONID") && sessionData.getId().equals(c.getValue()))
					.forEach(c -> c.setMaxAge(0));
		}
	}

	@Override
	public boolean isNew() {
		return sessionData.isNew();
	}

}

class SimpleHttpSessionData {

	private long creationTime;
	private String id;
	private long lastAccessedTime;
	private int maxInactiveInterval;
	private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
	private boolean isNew;

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public long getLastAccessedTime() {
		return lastAccessedTime;
	}

	public void setLastAccessedTime(long lastAccessedTime) {
		this.lastAccessedTime = lastAccessedTime;
	}

	public void updateLastAccessedTime() {
		this.lastAccessedTime = System.currentTimeMillis();
	}

	public void setMaxInactiveInterval(int interval) {
		this.maxInactiveInterval = interval;
	}

	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		return attributes.keys();
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	public boolean isNew() {
		return isNew;
	}

	public void setIsNew(boolean v) {
		this.isNew = v;
	}

}