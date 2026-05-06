package ntt.security.ollamadrama.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.Function;

import org.junit.Test;

import ntt.security.ollamadrama.agent.AppSettings;

public class McpPreprocessTest {

	@Test
	public void default_map_is_non_null_and_empty() {
		OllamaDramaSettings s = new OllamaDramaSettings();
		assertNotNull("default mcp_preprocess must be non-null so OllamaSession's lookup is NPE-safe",
				s.getMcp_preprocess());
		assertTrue(s.getMcp_preprocess().isEmpty());

		AppSettings a = new AppSettings();
		assertNotNull(a.getMcp_preprocess());
		assertTrue(a.getMcp_preprocess().isEmpty());
	}

	@Test
	public void registered_function_is_applied_to_tool_output() {
		OllamaDramaSettings s = new OllamaDramaSettings();
		Function<String, String> truncate2k = raw ->
				raw.length() > 2048 ? raw.substring(0, 2048) + "\n…[truncated]…" : raw;
		s.getMcp_preprocess().put("fetch", truncate2k);

		String raw = repeat("x", 5000);

		Function<String, String> looked_up = s.getMcp_preprocess().get("fetch");
		assertNotNull("registered function must be retrievable by tool name", looked_up);

		String processed = looked_up.apply(raw);
		assertEquals(2048 + "\n…[truncated]…".length(), processed.length());
		assertTrue(processed.endsWith("…[truncated]…"));
	}

	@Test
	public void lookup_for_unregistered_tool_returns_null() {
		// Mirrors the OllamaSession.askStrictChatQuestion guard:
		//   if ((null != _mcp_preprocess) && (null != _mcp_preprocess.get(tcr.getToolname()))) { ... }
		// The "no entry" branch must yield null so the default wrapper path is taken.
		OllamaDramaSettings s = new OllamaDramaSettings();
		s.getMcp_preprocess().put("fetch", raw -> raw);

		assertNull(s.getMcp_preprocess().get("get_current_time_in_UTC"));
	}

	@Test
	public void identity_function_preserves_raw_text_byte_for_byte() {
		OllamaDramaSettings s = new OllamaDramaSettings();
		s.getMcp_preprocess().put("echo", Function.identity());

		String raw = "line1\nline2\twith tab\nunicode: π → 42";
		assertEquals(raw, s.getMcp_preprocess().get("echo").apply(raw));
	}

	@Test
	public void appsettings_mcp_preprocess_propagates_via_updateWithAppSettings() {
		AppSettings a = new AppSettings();
		Function<String, String> redactor = raw -> raw.replace("SECRET", "***");
		a.getMcp_preprocess().put("read_file", redactor);

		OllamaDramaSettings s = new OllamaDramaSettings();
		// updateWithAppSettings requires ollama_models to be non-empty; selected_model
		// is the documented fall-back path.
		a.setSelected_model("qwen3:4b");
		s.updateWithAppSettings(a);

		assertSame("AppSettings map must be propagated, not deep-copied",
				a.getMcp_preprocess(), s.getMcp_preprocess());

		Function<String, String> looked_up = s.getMcp_preprocess().get("read_file");
		assertNotNull(looked_up);
		assertEquals("user=alice token=***", looked_up.apply("user=alice token=SECRET"));
	}

	@Test
	public void multiple_tools_can_have_distinct_preprocessors() {
		OllamaDramaSettings s = new OllamaDramaSettings();
		s.getMcp_preprocess().put("fetch", raw -> "FETCH:" + raw);
		s.getMcp_preprocess().put("read_file", raw -> "FILE:" + raw);

		assertEquals("FETCH:hello", s.getMcp_preprocess().get("fetch").apply("hello"));
		assertEquals("FILE:hello", s.getMcp_preprocess().get("read_file").apply("hello"));
	}

	private static String repeat(String s, int n) {
		StringBuilder sb = new StringBuilder(s.length() * n);
		for (int i = 0; i < n; i++) sb.append(s);
		return sb.toString();
	}
}
