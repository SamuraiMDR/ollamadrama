package ntt.security.ollamadrama.agent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ntt.security.ollamadrama.agent.Task;

public class TaskStateManager {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static void saveState(List<Task> tasks, String statefile) {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(statefile), StandardCharsets.UTF_8)))) {
            writer.println("{");
            writer.println("  \"tasks\": [");
            
            for (int i = 0; i < tasks.size(); i++) {
                Task task = tasks.get(i);
                writer.println("    {");
                writer.println("      \"id\": " + escapeJson(task.getId()) + ",");
                writer.println("      \"schedule\": \"" + task.getSchedule().name() + "\",");
                
                String lastExec = task.getLastExecuted() != null 
                    ? "\"" + task.getLastExecuted().format(FORMATTER) + "\""
                    : "null";
                writer.println("      \"lastExecuted\": " + lastExec);
                
                writer.print("    }");
                if (i < tasks.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            
            writer.println("  ]");
            writer.println("}");
            
            System.out.println("  -> State saved to " + statefile);
        } catch (IOException e) {
            System.err.println("Error saving state: " + e.getMessage());
        }
    }

    public static void loadState(List<Task> tasks, String statefile) {
        File stateFile = new File(statefile);
        if (!stateFile.exists()) {
            System.out.println("No previous state found. Starting fresh.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(statefile), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line.trim());
            }

            String jsonStr = json.toString();
            
            int tasksArrayStart = jsonStr.indexOf("[");
            int tasksArrayEnd = jsonStr.lastIndexOf("]");
            
            if (tasksArrayStart == -1 || tasksArrayEnd == -1) {
                System.err.println("Invalid JSON format in state file");
                return;
            }

            String tasksJson = jsonStr.substring(tasksArrayStart + 1, tasksArrayEnd);
            String[] taskObjects = splitJsonObjects(tasksJson);

            // Create a map of tasks by ID for quick lookup
            Map<String, Task> taskMap = new HashMap<>();
            for (Task task : tasks) {
                taskMap.put(task.getId(), task);
            }

            for (String taskJson : taskObjects) {
                String id = extractJsonValue(taskJson, "id");
                String schedule = extractJsonValue(taskJson, "schedule");
                String lastExecutedStr = extractJsonValue(taskJson, "lastExecuted");

                Task task = taskMap.get(id);
                if (task != null && schedule != null && schedule.equals(task.getSchedule().name())) {
                    if (lastExecutedStr != null && !lastExecutedStr.equals("null")) {
                        LocalDateTime lastExecuted = LocalDateTime.parse(lastExecutedStr, FORMATTER);
                        task.setLastExecuted(lastExecuted);
                    }
                }
            }

            System.out.println("Loaded previous state from " + statefile);
            
        } catch (IOException e) {
            System.err.println("Error loading state: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing state file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String escapeJson(String str) {
        return "\"" + str.replace("\\", "\\\\")
                         .replace("\"", "\\\"")
                         .replace("\n", "\\n")
                         .replace("\r", "\\r")
                         .replace("\t", "\\t") + "\"";
    }

    private static String[] splitJsonObjects(String json) {
        List<String> objects = new ArrayList<>();
        int braceCount = 0;
        int start = -1;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceCount == 0) {
                    start = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && start != -1) {
                    objects.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }

        return objects.toArray(new String[0]);
    }

    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        if (json.startsWith("null", valueStart)) {
            return "null";
        }

        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) {
                return null;
            }
            return json.substring(valueStart + 1, valueEnd);
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && 
               json.charAt(valueEnd) != ',' && 
               json.charAt(valueEnd) != '}' && 
               json.charAt(valueEnd) != ']') {
            valueEnd++;
        }

        return json.substring(valueStart, valueEnd).trim();
    }
}

