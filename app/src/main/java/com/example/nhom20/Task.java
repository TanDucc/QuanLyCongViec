package com.example.nhom20;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Task {
    private String id;
    private String title;
    private String description;
    private long dueDateTime;
    private boolean completed;
    private int priority;
    private boolean reminderEnabled;
    private String notificationType; // "none", "daily", "weekly"
    private String notificationTime; // "HH:mm"
    private List<Integer> notificationDays; // Danh sách ngày trong tuần (1=Thứ 2, 2=Thứ 3, ..., 7=Chủ Nhật)

    public Task(String id, String title, String description, long dueDateTime, boolean completed, int priority, boolean reminderEnabled) {
        this(id, title, description, dueDateTime, completed, priority, reminderEnabled, "none", null, new ArrayList<>());
    }

    public Task(String id, String title, String description, long dueDateTime, boolean completed, int priority, boolean reminderEnabled,
                String notificationType, String notificationTime, List<Integer> notificationDays) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDateTime = dueDateTime;
        this.completed = completed;
        this.priority = priority;
        this.reminderEnabled = reminderEnabled;
        this.notificationType = notificationType != null ? notificationType : "none";
        this.notificationTime = notificationTime;
        this.notificationDays = notificationDays != null ? notificationDays : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getDueDateTime() {
        return dueDateTime;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getNotificationTime() {
        return notificationTime;
    }

    public List<Integer> getNotificationDays() {
        return notificationDays;
    }

    public String getFormattedDueDateTime() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(dueDateTime));
    }

    // Thêm phương thức isOverdue() để kiểm tra công việc có quá hạn hay không
    public boolean isOverdue() {
        return !completed && System.currentTimeMillis() > dueDateTime;
    }
}