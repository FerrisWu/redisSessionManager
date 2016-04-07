package com.netease.nim.sync.session.event;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RequestEventSubject {
    private RequestEventObserver listener;

    public RequestEventSubject() {
    }

    public void attach(RequestEventObserver eventObserver) {
        this.listener = eventObserver;
    }

    public void detach() {
        this.listener = null;
    }

    public void completed(HttpServletRequest servletRequest, HttpServletResponse response) {
        if(this.listener != null) {
            this.listener.completed(servletRequest, response);
        }

    }
}
