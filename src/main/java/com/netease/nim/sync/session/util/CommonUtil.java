package com.netease.nim.sync.session.util;

import java.text.SimpleDateFormat;

public class CommonUtil {
	public static String transFormDate(long time) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy.MM.dd '-' hh:mm:ss z");
		return df.format(time);
	}
}
