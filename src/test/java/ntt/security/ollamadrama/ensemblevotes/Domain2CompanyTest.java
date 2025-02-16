package ntt.security.ollamadrama.ensemblevotes;

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
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_L,
				Globals.MODEL_NAMES_OPENAI_ALL,
				ollama_settings,
				true);
		sser.printEnsemble();
		
		/*
		uniq confident responses: 7
		response #1: NIPPON TELEGRAPH AND TELEPHONE
		 - openhermes:7b-mistral-v2.5-q4_0::9d6f73a1-6679-4344-84b5-3dae8b68f763
		response #2: NTT CORPORATION
		 - gpt-4-turbo::fdc25e89-2e48-4240-9937-48bebf88f9d4
		response #3: NTT LIMITED
		 - exaone3.5:32b::f64dae26-2253-425c-8f44-4a9349f05b71
		 - nemotron:70b::3ec98f9a-7807-4b1b-82dd-484b3033bd59
		response #4: NIPPON TELEGRAPH AND TELEPHONE CORPORATION
		 - dolphin-mistral:7b::ee0c686e-e91a-4c50-b035-0ed8fd4cf6cb
		response #5: NIPPON TELEGRAPH AND TELEPHONE EXCHANGE
		 - openchat:7b::e81d44c9-f27a-4c9a-b75c-34bcc3f408c6
		response #6: NTT
		 - olmo2:7b::16f3558a-1dd9-41e7-8a30-aaa88e0cb810
		 - sailor2:20b::ca885eec-e393-4b41-b5b0-ad0d0d57343f
		 - tulu3:70b::313826c2-d4f1-4ca3-b418-1508306cde78
		 - gpt-4o::3a3b86ea-b7c2-4008-8939-30d15debdb9c
		 - marco-o1:7b::0d8a2320-0f3f-4899-ab01-bfd543647c3d
		 - qwen2.5:7b::5fd5e7b6-3115-4b15-9bf8-2b17867cd3eb
		 - llama3.3:70b::1f19a4c3-b0e1-4f5c-92df-4f2d88d343db
		 - gpt-4::73fe0ac2-cc0b-4ad1-9beb-79696860c743
		 - gemma2:9b::adab0e8a-2588-441a-81c2-327c5b22a84b
		 - qwen2.5:72b::9fa183e9-00aa-49f6-8734-55653475520f
		 - athene-v2:72b::7cede0ec-ae1a-4ba1-950f-1dfbae7983a6
		 - gpt-3.5-turbo::dbfc1cc0-f4a9-4bb5-8ce4-153f95e689f0
		 - mistral:7b::3dbdf54e-ff53-455b-bc8c-bae0b38fb92e
		 - llama3.1:70b::095542cd-099a-48a4-bb25-1677707fb6a3
		 - llama3.1:8b::7bd00e64-7295-4d84-b4a8-0063c1a1279d
		 - gemma2:27b::1694647c-8460-4258-b11f-9c273e189280
		 - tulu3:8b::0619d972-ae07-420c-94f9-ee81018ac924
		response #7: NTT GLOBAL
		 - granite3.1-dense:8b::8f46c6e2-3730-4fad-9a36-4a9854d69f73
		 */
	}

	@Test
	public void strict_ENSAMBLE_Ollama_Domain2CompanyKnowledge() {
		SingleStringEnsembleResponse sser1 = OllamaUtils.strictEnsembleRun(
				"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_L, true);
		sser1.printEnsemble();
		
		/*
		uniq confident responses: 6
		response #1: NIPPON TELEGRAPH AND TELEPHONE
		 - openhermes:7b-mistral-v2.5-q4_0::05179084-b959-417e-8c69-227aefe2e394
		response #2: NTT LIMITED
		 - exaone3.5:32b::c13d63ac-24c1-4c88-9a92-a68e22ac781b
		 - nemotron:70b::df74779d-1185-49e6-9cc3-c04aeb2f5d6b
		response #3: NIPPON TELEGRAPH AND TELEPHONE CORPORATION
		 - dolphin-mistral:7b::c20d4bf1-8a60-4f4f-9740-8203cfa89f44
		response #4: NIPPON TELEGRAPH AND TELEPHONE EXCHANGE
		 - openchat:7b::fa49aead-830e-48a7-a20c-935c36a5a051
		response #5: NTT
		 - qwen2.5:7b::d2bd7d1d-7e00-410e-ba06-5efefdc48852
		 - qwen2.5:72b::0d0c7978-c80d-4090-b9dd-1ef33c77b1ff
		 - olmo2:7b::b640b3ba-d219-4226-b03b-53c6868afc34
		 - gemma2:9b::620024ff-8bc6-408c-9750-9a83402cd1e7
		 - llama3.1:8b::82b28f07-34f7-41ac-bca9-91430fbbe645
		 - llama3.3:70b::07c0d5a8-ae39-4919-9d64-1dffa3e7e38b
		 - athene-v2:72b::76644c74-9e3f-4f86-9e45-77786305fc6c
		 - mistral:7b::6a6bbb1c-2ebe-4c59-8ee3-f35825c7207a
		 - sailor2:20b::14834743-5571-43f0-8893-fa72993e0e3f
		 - marco-o1:7b::5d106070-199b-44b6-b72b-11502d9417ef
		 - tulu3:70b::a1f0020d-f5eb-423b-afd9-56ea56e18a71
		 - gemma2:27b::50662dbe-6e09-4595-b101-89088226044e
		 - llama3.1:70b::de974c3f-5195-4629-bd9f-c5ee7685f6cf
		 - tulu3:8b::6698b856-5c01-404c-996d-a9a2bcf40fe3
		response #6: NTT GLOBAL
		 - granite3.1-dense:8b::2f60951b-00e4-4d4a-b815-def213d4d987
		 */
	}
	
	@Test
	public void strict_ENSAMBLE_OllamaOpenAICombo_Domain2CompanyKnowledge_collective() {
		OllamaDramaSettings ollama_settings = OllamaUtils.parseOllamaDramaConfigENV();
		ollama_settings.sanityCheck();

		SingleStringEnsembleResponse sser = OllamaUtils.collectiveFullEnsembleRun(
				"What company or organization is associated with the domain global.ntt? Reply with only the name in uppercase",
				Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_L,
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
