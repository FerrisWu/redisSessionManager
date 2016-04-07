package com.netease.nim.sync.session.event;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestEventObserver {
	void completed(HttpServletRequest var1, HttpServletResponse var2);
}
