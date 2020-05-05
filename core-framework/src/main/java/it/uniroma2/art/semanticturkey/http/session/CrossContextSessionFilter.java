package it.uniroma2.art.semanticturkey.http.session;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CrossContextSessionFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
        CrossContextSessionRequestWrapper customRequest =
                new CrossContextSessionRequestWrapper((HttpServletRequest)request, (HttpServletResponse)response);
		chain.doFilter(customRequest, response);
	}

	@Override
	public void destroy() {
		// nothing to do
	}
	
}
