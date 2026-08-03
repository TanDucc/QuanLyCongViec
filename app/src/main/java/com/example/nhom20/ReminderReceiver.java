package com.example.nhom20;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskTitle = intent.getStringExtra("taskTitle");
        String message = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("notificationId", 0);

        // Tạo Notification Channel
        NotificationUtils.createNotificationChannel(context);

        // Hiển thị thông báo
        NotificationUtils.showNotification(context, taskTitle, message, notificationId);
    }
}