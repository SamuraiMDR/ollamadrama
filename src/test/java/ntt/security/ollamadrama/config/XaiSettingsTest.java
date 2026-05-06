package ntt.security.ollamadrama.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ntt.security.ollamadrama.agent.AppSettings;
import ntt.security.ollamadrama.singletons.XaiService;
import ntt.security.ollamadrama.utils.JSONUtils;

public class XaiSettingsTest {

	@Test
	public void xai_defaults_are_disabled_and_blank() {
		OllamaDramaSettings s = new OllamaDramaSettings();
		assertFalse(s.isUse_xai());
		assertEquals("", s.getXaikey());
	}

	@Test
	public void xai_settings_round_trip_through_json() {
		OllamaDramaSettings s = new OllamaDramaSettings();
		s.setUse_xai(true);
		s.setXaikey("xai-test-key");

		String json = JSONUtils.createJSONFromPOJO(s);
		assertTrue("serialised JSON must include the new xai fields", json.contains("\"use_xai\":true"));
		assertTrue(json.contains("\"xaikey\":\"xai-test-key\""));

		OllamaDramaSettings round = JSONUtils.createPOJOFromJSONOpportunistic(json, OllamaDramaSettings.class);
		assertNotNull(round);
		assertTrue(round.isUse_xai());
		assertEquals("xai-test-key", round.getXaikey());
	}

	@Test
	public void appsettings_propagate_xai_via_updateWithAppSettings() {
		AppSettings a = new AppSettings();
		a.setUse_xai(true);
		a.setXaikey("xai-from-app");
		a.setSelected_model("grok-3"); // updateWithAppSettings requires non-empty model selection

		OllamaDramaSettings s = new OllamaDramaSettings();
		s.updateWithAppSettings(a);

		assertTrue(s.isUse_xai());
		assertEquals("xai-from-app", s.getXaikey());
	}

	@Test
	public void xai_base_url_is_the_official_endpoint() {
		// The constant is the contract surface that XaiService uses to point the
		// OpenAI-compatible client at xAI. Keep it pinned to the documented URL so
		// callers reading the SPEC see the same value as the running code.
		assertEquals("https://api.x.ai/v1", XaiService.XAI_BASE_URL);
	}

	@Test
	public void grok_thresholds_are_registered() {
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("grok-4"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("grok-3"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("grok-3-mini"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("grok-2-latest"));
	}

	@Test
	public void xai_model_lists_are_non_empty_and_contain_grok() {
		assertTrue(Globals.MODEL_NAMES_XAI_TIER1.contains("grok-4"));
		assertTrue(Globals.MODEL_NAMES_XAI_ALL.contains("grok-3-mini"));
		assertTrue(Globals.MODEL_NAMES_XAI_CHEAP.contains("grok-3-mini"));
	}
}
