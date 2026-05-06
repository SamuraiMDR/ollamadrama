package ntt.security.ollamadrama.strict.builddate;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.OllamaEndpoint;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.utils.OllamaUtils;

public class BuildDateTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(BuildDateTest.class);

	//@Ignore
	@Test
	public void findBuildDateForAllModels_Test_CSV() {
		
		String selected_models = Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL;
		OllamaDramaSettings settings = OllamaUtils.parseOllamaDramaConfigENV();
		settings.setSatellites(new ArrayList<>(Arrays.asList(new OllamaEndpoint("http://127.0.0.1:11434", "", ""))));
		settings.setMcp_ports_csv(""); // lets not actually scan for 
		settings.setOllama_scan(false);
		settings.setOllama_models(selected_models);
		
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(
				"What is your knowledge cutoff date (or training completion date) of your model? Provide the most "
				+ "precise information available. If you do not know, try to provide a rough estimate.",
				settings, false, false);
		sser1.printEnsemble();
		
		/*
		uniq responses: 27
		response #1: October 2024
		 - qwen3:4b::90bc1ea1-45a6-4c92-a5e0-a0a339c8c863
		response #2: 2021-03-01
		 - mistral:7b::4399ed8f-5cca-415c-9ae0-bd084534a758
		response #3: My knowledge cutoff date is September 2023.
		 - cogito:8b::4dcb2f2c-5c45-4509-a959-9d6cce3afe53
		response #4: Unknown.
		 - gemma3n:e4b::4dc6f555-cb61-44a0-90c2-fc13c94af7ce
		response #5: I don't have information about my knowledge cutoff date.
		 - glm-4.7-flash:q8_0::a478ca78-9432-4fa3-af6c-68c7374f198f
		response #6: I don't have precise training completion date information available.
		 - huihui_ai/glm-4.7-flash-abliterated:q8_0::6eaafb5a-446e-4ac9-aa72-bfc562c377ed
		response #7: My knowledge cutoff date is December 2024.
		 - olmo-3:32b::aea8e8c0-53a7-481b-83ff-464919b8dc83
		response #8: Unknown
		 - gemma3:27b::2de2eade-abd1-457a-af4f-e20dc505a75f
		 - granite3.3:8b::b88a36fc-b57e-40b5-84d7-94bdc31a2f19
		 - llama3.1:70b::76c6b1b4-6618-4f48-91fe-32dabe8f9b0a
		 - gemma3:12b::edc17967-701c-4b4e-a359-9acc39059284
		 - llama3.3:70b::d258d2ff-ce59-4ea7-8f8c-b0caca4b8687
		response #9: 2023-10
		 - athene-v2:72b::e898ebcc-093a-4ea2-8b4f-1e864fabb19f
		 - phi4:14b::e7d5d791-a4bb-4340-83a7-22bc80ea862b
		 - qwen2.5:72b::ba328f1d-5ffd-41fb-b63b-b7181af3dac2
		 - cogito:70b::3dc4e024-12ec-4171-8bdc-b67243dce8e9
		response #10: 2023-02-01
		 - llama3.1:8b::10385e8e-0414-4673-8eae-d968da063998
		response #11: I do not have access to that information.
		 - gemma2:9b::3713a84e-4bf1-4bde-95b3-d7500b716cee
		response #12: I don't know
		 - huihui_ai/qwen3-abliterated:32b::8563b2e0-9651-4686-b55e-c319b6250391
		 - qwen3:8b::95c8b19f-ceee-473f-b9d2-1c94ede6215a
		 - qwen3:32b::037dada6-15b0-4193-b9ab-2976595dfc90
		response #13: December 2024
		 - qwq:32b::c2a227b9-ab2f-44d6-a9c6-f14220d40c23
		 - olmo-3:7b::18b32ca3-0b05-4400-9cb3-a8179f0e17de
		response #14: My knowledge cutoff is approximately mid-2023, though I don't have access to my specific training completion date.
		 - glm-4.7-flash:q4_K_M::3329f156-24ae-4495-a8e9-5d173e1910d1
		response #15: I don't know the exact date. Consult official sources for accurate information.
		 - qwen3:14b::6feeec57-a3e3-46ab-af98-2dd16d1d328c
		response #16: 2023-04
		 - tulu3:70b::a734830a-0759-4543-9866-1f14b2d25d55
		 - tulu3:8b::b19c4c7b-3bf9-4d84-8f85-72de7726ccec
		response #17: 2023-03-15
		 - qwen2.5:7b::417ea5aa-7b1e-45c9-8cb8-76109fe14c95
		response #18: June 2024
		 - nemotron-3-nano:30b::78428c24-3863-4ca6-9734-8e6891e46a44
		response #19: I do not have access to specific details about my training or cutoff dates.
		 - aya-expanse:32b::2767ed4b-73aa-41a4-8dae-db3f182aab53
		response #20: 2023-09
		 - gpt-oss:20b::15f90f28-4c53-4698-8807-38cf2dc756a0
		response #21: I was last updated in 2023, but I don't have access to my specific training completion date.
		 - huihui_ai/glm-4.7-flash-abliterated:q4_K_S::6ed6a704-f61a-439b-bca1-3faf70db5d1b
		response #22: My knowledge cutoff is approximately 2021. I was trained on data up to that point, though I may have some information about events and developments after that date through my training data.
		 - huihui_ai/glm-4.7-flash-abliterated:q4_K::1c31dbaa-c2ba-46cb-8707-378c0aa00e94
		response #23: 2023-01-01
		 - llava:34b::13fb14d3-1de8-492f-bd30-d2a1d2ad7153
		 - aya-expanse:8b::89d18560-20b4-4d77-a93b-75e291c7dde8
		response #24: September 2021
		 - sailor2:20b::1270d7cc-e5a0-41f6-a083-c2038806b314
		response #25: Unknown, Rough Estimate: 2021-2022
		 - nemotron:70b::8e8cb86f-50b9-4936-9637-82ed40fe2c93
		response #26: October 2021
		 - openchat:7b::9d9e967f-b382-4ecb-8376-ca4ddbd44e54
		response #27: October 2023
		 - cogito:14b::bc8d8d5c-8b74-4bff-9d68-9b079fd92499
		 */
		
	}
	
}
