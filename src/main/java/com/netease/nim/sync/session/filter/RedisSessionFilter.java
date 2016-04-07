package com.netease.nim.sync.session.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.netease.nim.sync.session.event.RequestEventSubject;
import com.netease.nim.sync.session.meta.RedisSessionManager;
import com.netease.nim.sync.session.meta.SessionHttpServletRequestWrapper;


public class RedisSessionFilter implements Filter {

	private static final String[] IGNORE_SUFFIX = new String[] { ".png", ".jpg", ".jpeg", ".gif", ".css", ".js",
			".html", ".htm","config.ftl","macro.ftl","function.ftl" };
	private ServletContext servletContext = null;
	private RedisSessionManager sessionManager;

	public RedisSessionFilter() {
	}
	

	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
		sessionManager = (RedisSessionManager)wac.getBean("redisSessionManager");
	}

	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		if (!this.shouldFilter(request)) {
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			RequestEventSubject eventSubject = new RequestEventSubject();
			SessionHttpServletRequestWrapper requestWrapper = new SessionHttpServletRequestWrapper(request, response,
					this.sessionManager, eventSubject);

			try {
				filterChain.doFilter(requestWrapper, servletResponse);
			} finally {
				eventSubject.completed(request, response);
			}

		}
	}

	private boolean shouldFilter(HttpServletRequest request) {
		String uri = request.getRequestURI().toLowerCase();
		String[] arr = IGNORE_SUFFIX;
		int len = arr.length;

		for (int i = 0; i < len; ++i) {
			String suffix = arr[i];
			if (uri.endsWith(suffix)) {
				return false;
			}
		}
		return true;
	}

	public void destroy() {

	}

}
