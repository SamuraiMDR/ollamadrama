package ntt.security.ollamadrama.agent;

import java.time.LocalDateTime;

public class Task {
    private final String id;
    private final String prompt;
    private final Scheduler schedule;
    private LocalDateTime lastExecuted;

    public Task(String id, String prompt, Scheduler schedule) {
        this.id = id;
        this.prompt = prompt;
        this.schedule = schedule;
        this.lastExecuted = null;
    }

    public String getId() {
        return id;
    }

    public String getPrompt() {
        return prompt;
    }

    public Scheduler getSchedule() {
        return schedule;
    }

    public LocalDateTime getLastExecuted() {
        return lastExecuted;
    }

    public void markExecuted() {
        this.lastExecuted = LocalDateTime.now();
    }

    public void setLastExecuted(LocalDateTime lastExecuted) {
        this.lastExecuted = lastExecuted;
    }

    public boolean isEligibleToRun() {
        if (lastExecuted == null) {
            return true;
        }

        if (schedule == Scheduler.ALWAYS) {
            return true;
        }

        long minutesSinceExecution = java.time.temporal.ChronoUnit.MINUTES.between(lastExecuted, LocalDateTime.now());
        return minutesSinceExecution >= schedule.getWaitMinutes();
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", prompt='" + prompt + '\'' +
                ", schedule=" + schedule +
                ", lastExecuted=" + lastExecuted +
                '}';
    }
}