package it.uniroma2.art.semanticturkey.versioning;

import java.util.HashMap;
import java.util.Map;

public class OperationContext {

	public static final class OpCtxKeys {
		public static final String resource = "resource";
		public static final String user = "user";
	}

	private Map<String, Object> ctx;

	public OperationContext() {
		ctx = new HashMap<>();
	}

	public void addEntry(String key, String value) {
		ctx.put(key, value);
	}

	public void removeEntry(String key) {
		ctx.remove(key);
	}

	public Map<String, Object> asMap() {
		return ctx;
	}

}
