package ntt.security.ollamadrama.memoryloss;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.config.Globals;
import ntt.security.ollamadrama.objects.ModelsScoreCard;
import ntt.security.ollamadrama.utils.OllamaDramaUtils;

public class MemoryLossTest {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLossTest.class);
	private static final String tag = "Ollama0.11.4+2x5090+128G+64kctx";
	
	// Guided 'needle' test
	//@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_FindTheNeedleTest_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingRandomWordNeedleTest("cogito:70b", 0, true, tag);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
		
				/*
				Ollama0.11.4 2x5090 128G 64kCTX +flashatt
		
			           model  max(actual)
			       qwen3:32b        35000
			       qwen3:14b        31000
			    llama3.1:70b        26000
			   athene-v2:72b        23000
			     qwen2.5:72b        16000
			     ------------------------
			      cogito:70b        13000
			    nemotron:70b        12000
			       tulu3:70b        10000
			        qwen3:4b        10000
			      qwen2.5:7b        10000
			    llama3.3:70b        10000
			     marco-o1:7b         9000
			        qwen3:8b         8000
			     r1-1776:70b         8000
			   exaone3.5:32b         7000
			        phi4:14b         4000
			      gemma3:27b         4000
			 aya-expanse:32b         2000
			        olmo2:7b         2000
			      gemma3:12b         2000
			       cogito:8b         2000
			   granite3.3:8b         1000
			     gpt-oss:20b         1000
			       gemma2:9b         1000
			exaone-deep:7.8b         1000
			     sailor2:20b         1000
			        tulu3:8b         1000
		 */
		
		/*
				Ollama0.11.4 2x5090 128G 32kCTX
		
		          	  model  max(actual)
			    qwen2.5:72b        30000
			      qwen3:14b        29000
			  athene-v2:72b        21000
			      qwen3:32b        18000
			      ----------------------
			   nemotron:70b        13000
			     cogito:70b        13000
			    r1-1776:70b        13000
			   llama3.1:70b        13000
			   llama3.3:70b        13000
			  exaone3.5:32b        12000
			     qwen2.5:7b        10000
			      tulu3:70b         6000
			       qwen3:4b         6000
			    gpt-oss:20b         5000
			    marco-o1:7b         4000
			    gemma3n:e4b         4000
			     gemma3:27b         4000
			      gemma2:9b         4000
			aya-expanse:32b         3000
			    llama3.1:8b         3000
			     gemma3:12b         3000
			       qwen3:8b         3000
			       olmo2:7b         2000
			       phi4:14b         1000
			      cogito:8b         1000
			       tulu3:8b         1000
		 */
	}
	
	// Abandoned since models gets confused about target word length (interprets and chops)
	@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_RandomWords_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingRandomWords(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 3, true, tag);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}

	// Abandoned since models begin to guess sequence patterns
	@Ignore
	@Test
	public void checkMemoryLengthForAllEnsembleModels_SequenceOfNumbers_CSV() {
		ModelsScoreCard scorecard = OllamaDramaUtils.performMemoryTestUsingSequenceOfNumbers(Globals.MODEL_NAMES_OLLAMA_ALL_UP_TO_XL, 3, true, 900L);
		System.out.println("SCORECARD:");
		scorecard.evaluate();
		scorecard.print();
	}
}
