package com.netease.nim.sync.session.listener;

import com.netease.nim.sync.session.meta.RedisHttpSession;

public interface SessionListener {
    void onAttributeChanged(RedisHttpSession session);

    void onInvalidated(RedisHttpSession session);
}

