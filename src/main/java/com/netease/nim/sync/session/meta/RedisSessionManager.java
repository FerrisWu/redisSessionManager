package com.netease.nim.sync.session.meta;

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.netease.framework.dao.cache.NRedisConnectionImpl;
import com.netease.framework.dao.templet.cache.CacheTempletImpl;
import com.netease.framework.dao.templet.cache.ICacheTemplet;
import com.netease.nim.sync.session.event.RequestEventObserver;
import com.netease.nim.sync.session.event.RequestEventSubject;
import com.netease.nim.sync.session.exception.SessionException;
import com.netease.nim.sync.session.listener.SessionListenerAdaptor;
import com.netease.nim.sync.session.util.CommonUtil;

@Component()
public class RedisSessionManager {
	 public static final String SESSION_ID_PREFIX = "R_JSID_";
	    public static final String SESSION_ID_COOKIE = "JSESSIONID";
	    @Autowired
	    private ICacheTemplet redisClient;
	    private int expirationUpdateInterval = 300;
	    private int maxInactiveInterval = 1800;
	    private Logger log = Logger.getLogger(this.getClass());

	    public RedisSessionManager() {
	    }

	    
	    public NRedisConnectionImpl getRedis()
	    {
	    	return (NRedisConnectionImpl)redisClient.getConnectionManager().getConnection();
	    }

	    public void setExpirationUpdateInterval(int expirationUpdateInterval) {
	        this.expirationUpdateInterval = expirationUpdateInterval;
	    }

	    public void setMaxInactiveInterval(int maxInactiveInterval) {
	        this.maxInactiveInterval = maxInactiveInterval;
	    }

	    public RedisHttpSession createSession(SessionHttpServletRequestWrapper request, HttpServletResponse response, RequestEventSubject requestEventSubject, boolean create) {
	        String sessionId = this.getRequestedSessionId(request);
	        RedisHttpSession session = null;
	        if(StringUtils.isEmpty(sessionId) && !create) {
	            return null;
	        } else {
	            if(StringUtils.isNotEmpty(sessionId)) {
	                session = this.loadSession(sessionId);
	            }

	            if(session == null && create) {
	                session = this.createEmptySession(request, response);
	            }

	            if(session != null) {
	                this.attachEvent(session, request, response, requestEventSubject);
	            }

	            return session;
	        }
	    }

	    private String getRequestedSessionId(HttpServletRequestWrapper request) {
	        Cookie[] cookies = request.getCookies();
	        if(cookies != null && cookies.length != 0) {
	            Cookie[] arr = cookies;
	            int len = cookies.length;

	            for(int i = 0; i < len; ++i) {
	                Cookie cookie = arr[i];
	                if(SESSION_ID_COOKIE.equals(cookie.getName())) {
	                    return cookie.getValue();
	                }
	            }

	            return null;
	        } else {
	            return null;
	        }
	    }

	    private void saveSession(RedisHttpSession session) {
	        try {
	            if(this.log.isDebugEnabled()) {
	                this.log.debug("RedisHttpSession saveSession [ID=" + session.id + ",isNew=" + session.isNew + ",isDiry=" + session.isDirty + ",isExpired=" + session.expired + "]");
	            }

	            if(session.expired) {
	                this.getRedis().del(this.generatorSessionKey(session.id));
	            } else {
	            	byte[] key = this.generatorSessionKey(session.id).getBytes();
	                this.getRedis().setex(key, session.maxInactiveInterval + this.expirationUpdateInterval, session);
	            }

	        } catch (Exception var3) {
	            throw new SessionException(var3);
	        }
	    }

	    private RedisHttpSession createEmptySession(SessionHttpServletRequestWrapper request, HttpServletResponse response) {
	    	RedisHttpSession session = new RedisHttpSession();
	        session.id = this.createSessionId();
	        session.creationTime = System.currentTimeMillis();
	        session.maxInactiveInterval = this.maxInactiveInterval;
	        session.isNew = true;
	        if(this.log.isDebugEnabled()) {
	            this.log.debug("RedisHttpSession Create [ID=" + session.id + "]");
	        }

	        this.saveCookie(session, request, response);
	        return session;
	    }

	    private String createSessionId() {
	        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
	    }

	    private void attachEvent(final RedisHttpSession session, final HttpServletRequestWrapper request, final HttpServletResponse response, RequestEventSubject requestEventSubject) {
	        session.setListener(new SessionListenerAdaptor() {
	            public void onInvalidated(RedisHttpSession session) {
	                RedisSessionManager.this.saveCookie(session, request, response);
	            }
	        });
	        requestEventSubject.attach(new RequestEventObserver() {
	            public void completed(HttpServletRequest servletRequest, HttpServletResponse response) {
	                int updateInterval = (int)((System.currentTimeMillis() - session.lastAccessedTime) / 1000L);
	                if(RedisSessionManager.this.log.isDebugEnabled()) {
	                    RedisSessionManager.this.log.debug("RedisHttpSession Request completed [ID=" + session.id + ",lastAccessedTime=" + CommonUtil.transFormDate(session.lastAccessedTime) + ",updateInterval=" + updateInterval + "]");
	                }

	                if(updateInterval >= RedisSessionManager.this.expirationUpdateInterval) {
	                        session.lastAccessedTime = System.currentTimeMillis();
	                        RedisSessionManager.this.saveSession(session);
	                }
	            }
	        });
	    }

	    private void saveCookie(RedisHttpSession session, HttpServletRequestWrapper request, HttpServletResponse response) {
	        if(session.isNew || session.expired) {
	            Cookie cookie = new Cookie(SESSION_ID_COOKIE, (String)null);
	            cookie.setPath(request.getContextPath());
	            if(session.expired) {
	                cookie.setMaxAge(0);
	            } else if(session.isNew) {
	                cookie.setValue(session.getId());
	            }

	            response.addCookie(cookie);
	            if(this.log.isDebugEnabled()) {
	                this.log.debug("RedisHttpSession saveCookie [ID=" + session.id + "]");
	            }

	        }
	    }

	    private RedisHttpSession loadSession(String sessionId) {
	        try {
	        	RedisHttpSession e = (RedisHttpSession)this.getRedis().get(this.generatorSessionKey(sessionId).getBytes());
	            if(this.log.isDebugEnabled()) {
	                this.log.debug("RedisHttpSession Load [ID=" + sessionId + ",exist=" + (e != null) + "]");
	            }

	            if(e != null) {
	                e.isNew = false;
	                e.isDirty = false;
	            }

	            return e;
	        } catch (Exception var3) {
	            this.log.warn("exception loadSession [Id=" + sessionId + "]", var3);
	            return null;
	        }
	    }

	    private String generatorSessionKey(String sessionId) {
	        return SESSION_ID_PREFIX.concat(sessionId);
	    }


		public void setRedisClient(CacheTempletImpl redisClient) {
			this.redisClient = redisClient;
		}
}
