package ntt.security.ollamadrama.agent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntt.security.ollamadrama.agent.*;
import ntt.security.ollamadrama.config.OllamaDramaSettings;
import ntt.security.ollamadrama.objects.sessions.OllamaSession;
import ntt.security.ollamadrama.singletons.OllamaService;
import ntt.security.ollamadrama.utils.SystemUtils;

public class TaskSchedulerApp {
	private final ArrayList<Task> tasks;
	private final Random random;

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskSchedulerApp.class);

	public TaskSchedulerApp(String foldername, String taskstatefile) {
		this.tasks = new ArrayList<>();
		this.random = new Random();
		loadTasksFromFolder(foldername);
		TaskStateManager.loadState(tasks, taskstatefile);
	}

	private void loadTasksFromFolder(String foldername) {
		File tasksDir = new File(foldername);

		if (!tasksDir.exists()) {
			System.err.println("Tasks folder '" + foldername + "' does not exist. Creating it...");
			if (tasksDir.mkdirs()) {
				System.out.println("Created tasks folder. Please add task files.");
			}
			return;
		}

		if (!tasksDir.isDirectory()) {
			System.err.println("'" + foldername + "' is not a directory!");
			return;
		}

		File[] files = tasksDir.listFiles();
		if (files == null || files.length == 0) {
			System.out.println("No task files found in '" + foldername + "' folder.");
			return;
		}

		int loadedCount = 0;
		for (File file : files) {
			if (file.isFile()) {
				String fileName = file.getName();

				// Find the scheduler based on file extension
				Scheduler scheduler = null;
				String taskId = null;

				for (Scheduler s : Scheduler.values()) {
					if (fileName.endsWith(s.getExtension())) {
						scheduler = s;
						taskId = fileName.substring(0, fileName.length() - s.getExtension().length());
						break;
					}
				}

				if (scheduler != null && taskId != null && !taskId.isEmpty()) {
					try {
						String prompt = readFileContent(file);
						tasks.add(new Task(taskId, prompt, scheduler));
						loadedCount++;
						System.out.println("Loaded task: " + taskId + " (" + scheduler + ")");
					} catch (IOException e) {
						System.err.println("Error reading file " + fileName + ": " + e.getMessage());
					}
				} else {
					System.out.println("Skipping file with unknown extension: " + fileName);
				}
			}
		}

		System.out.println("Total tasks loaded: " + loadedCount);
		System.out.println("----------------------------------------");
	}

	private String readFileContent(File file) throws IOException {
		StringBuilder content = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (content.length() > 0) {
					content.append("\n");
				}
				content.append(line);
			}
		}
		return content.toString().trim();
	}

	private List<Task> getEligibleTasks() {
		return tasks.stream()
				.filter(Task::isEligibleToRun)
				.collect(Collectors.toList());
	}

	private Task selectRandomTask(List<Task> eligibleTasks) {
		if (eligibleTasks.isEmpty()) {
			return null;
		}
		int randomIndex = random.nextInt(eligibleTasks.size());
		return eligibleTasks.get(randomIndex);
	}

	public void run(OllamaDramaSettings _settings, AppSettings _appsettings) {
		if (tasks.isEmpty()) {
			System.err.println("No tasks loaded. Exiting.");
			return;
		}

		System.out.println("Task Scheduler Application Started");
		System.out.println("Total tasks: " + tasks.size());
		System.out.println("----------------------------------------");

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\nShutting down... Saving state.");
			TaskStateManager.saveState(tasks, _appsettings.getTaskstatefile());
		}));

		System.out.println("Running " + _appsettings.getRounds_per_pass() + " rounds for agent pass with " + _appsettings.getPersona());
		for (int i=1; i<=_appsettings.getRounds_per_pass(); i++) {
			try {
				List<Task> eligibleTasks = getEligibleTasks();

				if (eligibleTasks.isEmpty()) {
					System.out.println("[" + LocalDateTime.now() + "] No eligible tasks to run. Waiting...");
				} else {
					Task selectedTask = selectRandomTask(eligibleTasks);
					if (selectedTask != null) {
						System.out.println("[" + LocalDateTime.now() + "] [" + selectedTask.getId() + "] waiting for lock \n");
						OllamaLock lock = new OllamaLock(Path.of("/ollama_lock"), _appsettings.getPersona());
						lock.acquire();
						try {
							System.out.println("[" + LocalDateTime.now() + "] [" + selectedTask.getId() + "] \n");
							executeTask(selectedTask.getPrompt(), _settings, _appsettings);
							selectedTask.markExecuted();
							TaskStateManager.saveState(tasks, _appsettings.getTaskstatefile());
							System.out.println("  -> Executed. Schedule: " + selectedTask.getSchedule() + 
									" | Eligible tasks: " + eligibleTasks.size());
						} finally {
							lock.release();
						}
					}
				}

				LOGGER.info("Rescanning for available MCP tools");
				OllamaService.wireMCPs(true);
				LOGGER.info("Sleeping 4.2 seconds");
				Thread.sleep(4200);

			} catch (InterruptedException e) {
				System.err.println("Application interrupted: " + e.getMessage());
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				System.err.println("Error during execution: " + e.getMessage());
				e.printStackTrace();
			}
		}

		TaskStateManager.saveState(tasks, _appsettings.getTaskstatefile());
		System.out.println("Task Scheduler Application Stopped");
	}

	private void executeTask(String _prompt, OllamaDramaSettings _settings, AppSettings _appsettings) {
		try {
			OllamaSession a1 = OllamaService.getStrictProtocolSession(_appsettings.getSelected_model(), false, _appsettings.isUse_random_seed(), _appsettings.getInitial_prompt(), _appsettings.isMake_mcp_tools_available());
			if (a1.getOllama().ping()) System.out.println(" - STRICT ollama session [" + _appsettings.getSelected_model() + "] is operational\n");

			// Agent interaction loop
			String new_data_prompt = _prompt + "\n" + "* DATE: " + " " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toString() + "\n\n";
			a1.askStrictChatQuestion(new_data_prompt, _appsettings.getRecursive_question(), _appsettings.getSession_tokens_maxlen(), _appsettings.isHide_llm_reply_if_uncertain(), _appsettings.getMax_retries(), _settings.getOllama_timeout(), 0, _appsettings.getMax_recursive_toolcall_depth(), _appsettings.getToolcall_pausetime_in_seconds(), _appsettings.isReturn_toolcall(), _appsettings.isHalt_on_tool_error(), null, _appsettings.isUnload_model_after_query(), _appsettings.isDebug(), _appsettings.getMcp_preprocess(), _appsettings.isPrompt_logging());

			int chatsize_wordcount_a1 = a1.getChatSizeWordCount();
			LOGGER.info("session wordcount: " + chatsize_wordcount_a1);
		} catch (Exception e) {
			LOGGER.error("Caught exception: " + e.getMessage());
			SystemUtils.halt();
		}
	}

}