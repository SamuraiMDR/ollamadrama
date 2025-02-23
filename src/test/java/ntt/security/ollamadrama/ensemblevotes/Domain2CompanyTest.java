package ntt.security.ollamadrama.ensemblevotes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.response.SingleStringEnsembleResponse;
import ntt.security.ollamadrama.singletons.OpenAIService;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;
import ntt.security.ollamadrama.utils.OllamaUtils;
import ntt.security.ollamadrama.utils.OpenAIUtils;


public class Domain2CompanyTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Domain2CompanyTest.class);

	@Test
	public void strict_ENSAMBLE_OllamaOpenAICombo_Domain2CompanyKnowledge() {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		SingleStringEnsembleResponse sser = OllamaDramaUtils.collectEnsembleVotes(
				"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
				Globals.MODEL_NAMES_OPENAI_ALL,
				ollama_settings,
				true);
		sser.printEnsemble();
		
		/*
		uniq confident responses: 9
		response #1: NIPPON TELEGRAPH AND TELEPHONE
		 - openhermes:7b-mistral-v2.5-q4_0::85d7af98-7238-4996-81c9-49320b46e907
		response #2: NTT CORPORATION
		 - gpt-4-turbo::5bf417f7-123a-492f-b40f-2223a92246ea
		response #3: NTT LTD.
		 - gpt-4::6ef810ed-5752-4a9a-beec-6cffecaaf566
		response #4: NTT LIMITED
		 - exaone3.5:32b::1949f946-b1aa-434c-9365-2ef677290615
		 - nemotron:70b::94da5c79-3c1b-4444-b75e-e076e9b07ba5
		response #5: NIPPON TELEGRAPH AND TELEPHONE CORPORATION
		 - dolphin-mistral:7b::fdafeb01-c606-40ce-b789-e20a245276dd
		response #6: NTT COMMUNICATIONS CORPORATION
		 - aya-expanse:32b::5d12e34c-1482-4fe0-a53e-8fdbb3c3bd35
		response #7: NIPPON TELEGRAPH AND TELEPHONE EXCHANGE
		 - openchat:7b::1164cfdc-2d07-444e-b4bf-a61ffe6bbf8d
		response #8: NTT
		 - llama3.1:8b::abe5564e-8aaa-4fec-8360-9ec031f770b4
		 - gpt-3.5-turbo::9e27b8b1-5b2f-4024-bc48-606d41348304
		 - gpt-4o::d1df788e-9dd2-49bc-82fb-901baf7cbcb9
		 - sailor2:20b::5497b38b-32e0-47bb-bd2c-fc0fe3cee114
		 - qwen2.5:72b::44e81a3b-cb95-4b10-93b7-07691f87feec
		 - athene-v2:72b::bc7bd144-6c86-4cdc-93c2-c688191b9960
		 - wizard-vicuna-uncensored:30b::dec9b7cc-f533-4612-811f-6f706b2011f7
		 - olmo2:7b::5c8cdfe0-d3f5-46c1-b6c1-e47a468d9537
		 - marco-o1:7b::da07e8a7-4e0b-4406-9f5c-50f6b2c1e8ec
		 - tulu3:70b::18728afb-816d-4fcd-b3b1-6db52ae88363
		 - llama3.1:70b::303edbcb-afaa-4adc-8964-2aff9a5e6a2a
		 - gemma2:9b::5725ab79-f4d8-4e13-8011-02d5062f029e
		 - qwen2.5:7b::25f859a8-5f34-42e2-b4e7-bf55447ed9d0
		 - llama3.3:70b::3581d10d-5ce1-4d4e-9f42-371be619b5c3
		 - mistral:7b::d2acab90-c9e8-4bf5-b563-e6fdc56dd5ba
		 - tulu3:8b::28a07d21-5ee3-4b82-b9ac-8ea65d3a3517
		 - gemma2:27b::3fecdf08-0c8c-439c-8252-a7dd4f78d875
		 - aya-expanse:8b::f9a84f36-8f2c-4e85-a78b-862c0a5ca443
		response #9: NTT GLOBAL
		 - granite3.1-dense:8b::795bf40c-6487-4d4c-8044-10e4d7d59e82
		 */
		
		// Assert
		String best_response = sser.getBestResponse(5);
		assertEquals("Make sure the winning company name is simply 'NTT'", "NTT", best_response);
	}

	@Test
	public void strict_ENSAMBLE_Ollama_Domain2CompanyKnowledge_Ollama() {
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(
				"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, true);
		sser1.printEnsemble();
		
		/*
		uniq confident responses: 7
		response #1: NIPPON TELEGRAPH AND TELEPHONE
		 - openhermes:7b-mistral-v2.5-q4_0::fc7cf6c0-00ec-4689-8b45-b9c7af0adb30
		response #2: NTT LIMITED
		 - exaone3.5:32b::60a586f9-09fb-42da-85d3-77c9c7d33da9
		 - nemotron:70b::3d1a79d7-6f10-4bcf-b982-9e0e36622e37
		response #3: NIPPON TELEGRAPH AND TELEPHONE CORPORATION
		 - dolphin-mistral:7b::78e51268-8ce4-4cfe-b8c1-7805a2f4db50
		response #4: NTT COMMUNICATIONS CORPORATION
		 - aya-expanse:32b::b2c131f0-1305-402f-a474-a4e343b9ffc7
		response #5: NIPPON TELEGRAPH AND TELEPHONE EXCHANGE
		 - openchat:7b::415fd724-d3da-4685-8773-3794caff5a18
		response #6: NTT
		 - sailor2:20b::832af0c3-c788-4f2b-802f-2d962d766d05
		 - qwen2.5:7b::7f9c894d-c8b4-4393-b763-952c878209fa
		 - wizard-vicuna-uncensored:30b::0ea50ae3-4e3f-49c9-8308-0444e18c3d1e
		 - marco-o1:7b::c9b4da02-90d2-4eb8-b738-a178dfb3f2b5
		 - llama3.3:70b::719f0913-ec6c-4c82-a335-22007e033d16
		 - tulu3:70b::eba4b325-c5e6-487a-b111-adc8baf53b4f
		 - olmo2:7b::8ba14b5c-2a75-4e7b-9b7f-06248ca4dd63
		 - mistral:7b::a9414601-0ead-4c23-8700-46ae9523a17d
		 - tulu3:8b::3a5f1b11-e0de-404c-867f-f5b1a47a324c
		 - qwen2.5:72b::ef21f248-c04f-4416-928e-4cbfc10ea32e
		 - aya-expanse:8b::c35993c8-1478-43e6-a02b-a02ef7835e37
		 - llama3.1:70b::c1d5970a-bb0f-4fe5-a78d-d34128064954
		 - llama3.1:8b::8c615a6f-7217-4d4a-aade-0d197e38226c
		 - gemma2:27b::b9e090a8-eec8-4c65-9ed3-c0fd1074858c
		 - athene-v2:72b::fcb8d979-074f-4bcd-a81c-d350e9623a5f
		 - gemma2:9b::866510d9-4cae-479b-9b5d-c8eac2665f50
		response #7: NTT GLOBAL
		 - granite3.1-dense:8b::9320abd9-5479-410c-a564-4841a9daf721
		 */
		
		// Assert
		String best_response = sser1.getBestResponse(5);
		assertEquals("Make sure the winning company name is simply 'NTT'", "NTT", best_response);
	}
	
	@Test
	public void strict_ENSAMBLE_OllamaOpenAICombo_Domain2CompanyKnowledge_collective() {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		SingleStringEnsembleResponse sser = OllamaUtils.collectiveFullEnsembleRun(
				"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL,
				Globals.MODEL_NAMES_OPENAI_ALL,
				ollama_settings, true, true);
		sser.printEnsemble();
		
		/*
		uniq confident responses: 5
		response #1: NIPPON TELEGRAPH AND TELEPHONE
		 - qwen2.5:7b::367d7023-9bb2-4a47-bb55-6eda4581ea1c
		 - gpt-3.5-turbo::e8eb39e0-40d5-44f6-afcd-fe30111585af
		response #2: NTT CORPORATION
		 - mistral:7b::b6293b67-969c-4c90-8f3c-b963215a5243
		 - gpt-4o::11e89d4c-b33f-489c-b8ea-4de2107786f0
		 - llama3.2:3b::6c51fe04-47db-4dae-a3c9-b6cdd6843f08
		 - gemma2:9b::1a8e35c0-d412-4628-8c2b-57a77389cc21
		 - openchat:7b::26dc6ad3-6834-4fca-a0a2-12ecaaab112e
		 - openhermes:7b-mistral-v2.5-q4_0::49a25995-b5c7-4ddd-add4-5e907b112f72
		 - exaone3.5:32b::e93a9545-5786-4bac-8317-1e427859d093
		 - sailor2:20b::8f8e84ab-0bda-4dd4-a007-074ef9bd4f05
		 - olmo2:7b::734b36bb-000a-4080-adc6-924ce5310531
		 - llama3.1:8b::3fbd7b2b-90d8-4d3d-8d3c-05478f7588f8
		response #3: NTT LIMITED
		 - llama3.1:70b::27b02868-f6c9-4268-be2d-c191287d8f02
		 - nemotron:70b::6b44f82e-be29-48c2-8826-503cce2bdf17
		response #4: NIPPON TELEGRAPH AND TELEPHONE CORPORATION
		 - granite3.1-dense:8b::679bce7a-891f-4aaf-8ce6-d6d78671d775
		 - gpt-4::ce6f9ac3-c40c-400d-8bb3-9b0319470bc4
		 - gpt-4-turbo::057c6e39-77fc-4014-9b51-95bddebfec26
		response #5: NTT
		 - qwen2:7b::30c90252-47d8-44c2-b5fe-e94f284ee86d
		 - qwen2.5:72b::03f91579-ee1f-41da-ad8d-5d9b1a3da79b
		 - llama3.3:70b::1a28c89a-f222-445a-bb14-cef418fc516b
		 - marco-o1:7b::3995ffff-35eb-4ac2-82e8-f1d99df6bcb8
		 - athene-v2:72b::958469f1-c7cd-4c8c-86a5-1b8a320a4795
		 - dolphin3:8b::aa97e714-d923-4714-8c11-ae8e9451c89f
		 - gemma2:27b::1e92d6a5-1932-4087-8d64-2c89cf5b59c7
		 - dolphin-mistral:7b::d3873987-c53c-4a0f-aaea-00d1a2a21761
		 - tulu3:70b::7ca8536a-2ab1-44ff-b608-fa9dc68ac659
		 - tulu3:8b::a8eab019-0880-49b9-847d-612fce3e327b
		 */
	}

	@Test
	public void strict_ENSAMBLE_DOMAIN_OpenAI() {
		// need the openai key
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		if (ollama_settings.getOpenaikey().length() > 10) {
			OpenAIService.getInstance(ollama_settings);
			SingleStringEnsembleResponse sser = OpenAIUtils.strictEnsembleRun("Is a known company or organization associated with the domain global.ntt? Reply with only the name.", 
					Globals.MODEL_NAMES_OPENAI_ALL,
					ollama_settings, // Globals.MODEL_NAMES_OPENAI_ALL, gpt-3.5-turbo
					true);
			sser.printEnsemble();
		}
		
		/*
		uniq responses: 5
		response #1: NTT Ltd.
		 - gpt-4o::bd807656-ca3c-4477-ab96-61f90675ccf8
		response #2: LOWPROBA
		 - gpt-4o-mini::efe74b84-cddc-40a4-ae5f-ccdf04b0708a
		response #3: NTT Global
		 - gpt-4::1897dd0f-2738-4c65-a11f-b592387449fb
		response #4: NTT Corporation
		 - gpt-4-turbo::604376c6-a594-4457-a0a8-1e3d67d1e6ce
		response #5: NTT
		 - gpt-3.5-turbo::371aadab-ca17-4504-ba31-48a215ddb87e
		
		uniq confident responses: 4
		response #1: NTT Ltd.
		 - gpt-4o::bd807656-ca3c-4477-ab96-61f90675ccf8
		response #2: NTT Global
		 - gpt-4::1897dd0f-2738-4c65-a11f-b592387449fb
		response #3: NTT Corporation
		 - gpt-4-turbo::604376c6-a594-4457-a0a8-1e3d67d1e6ce
		response #4: NTT
		 - gpt-3.5-turbo::371aadab-ca17-4504-ba31-48a215ddb87e
		 */
	}

}
