package ntt.security.ollamadrama.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ntt.security.ollamadrama.objects.sessions.OpenAISession;

public class OpenAIReasoningModelTest {

	@Test
	public void o_family_models_are_reasoning() {
		assertTrue(OpenAISession.isReasoningModel("o1"));
		assertTrue(OpenAISession.isReasoningModel("o1-mini"));
		assertTrue(OpenAISession.isReasoningModel("o1-preview"));
		assertTrue(OpenAISession.isReasoningModel("o3"));
		assertTrue(OpenAISession.isReasoningModel("o3-mini"));
		assertTrue(OpenAISession.isReasoningModel("o4-mini"));
	}

	@Test
	public void chat_models_are_not_reasoning() {
		assertFalse(OpenAISession.isReasoningModel("gpt-3.5-turbo"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4-turbo"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4o"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4o-mini"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4.1"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4.1-mini"));
		assertFalse(OpenAISession.isReasoningModel("gpt-4.1-nano"));
		// gpt-5 accepts temperature/top_p — must NOT be classified as reasoning-only.
		assertFalse(OpenAISession.isReasoningModel("gpt-5"));
		assertFalse(OpenAISession.isReasoningModel("gpt-5-mini"));
		assertFalse(OpenAISession.isReasoningModel("gpt-5-nano"));
	}

	@Test
	public void case_and_whitespace_tolerant() {
		assertTrue(OpenAISession.isReasoningModel("  O3-MINI  "));
		assertTrue(OpenAISession.isReasoningModel("O1"));
	}

	@Test
	public void null_and_empty_are_not_reasoning() {
		assertFalse(OpenAISession.isReasoningModel(null));
		assertFalse(OpenAISession.isReasoningModel(""));
		assertFalse(OpenAISession.isReasoningModel("   "));
	}

	@Test
	public void thresholds_registered_for_new_models() {
		// Confidence-vote logic falls back to the session-level threshold (default 70)
		// when no per-model entry exists. We want explicit entries for the newly-adopted
		// models so callers don't accidentally apply the wrong default.
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("o1"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("o3"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("o3-mini"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("o4-mini"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("gpt-4.1"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("gpt-5"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("gpt-5-mini"));
		assertTrue(Globals.MODEL_PROBABILITY_THRESHOLDS.containsKey("gpt-5-nano"));
	}
}
