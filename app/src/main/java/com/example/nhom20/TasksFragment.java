package com.example.nhom20;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TasksFragment extends Fragment implements TaskAdapter.OnTaskInteractionListener {
    private TaskViewModel viewModel;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private TextView emptyView;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private Calendar selectedDateTime;
    private Calendar selectedNotificationTime;
    private FirebaseFirestore db;
    private String userUID;
    private static final long DEFAULT_REMINDER_OFFSET = 10 * 60 * 1000; // 10 phút trước sự kiện

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        db = ((HomeActivity) requireActivity()).getFirestore();
        userUID = ((HomeActivity) requireActivity()).getCurrentUserUID();
        if (userUID == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return view;
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        FloatingActionButton fab = view.findViewById(R.id.fab);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TaskAdapter(this);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(TaskViewModel.class);
        viewModel.getFilteredTaskList().observe(getViewLifecycleOwner(), tasks -> {
            adapter.setTasks(tasks);
            emptyView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        });

        fab.setOnClickListener(v -> showTaskDialog(null));

        checkStoragePermission();
        migrateDataToFirestore();
        loadTasksFromFirestore();

        return view;
    }

    private void checkStoragePermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Quyền truy cập bộ nhớ đã được cấp", Toast.LENGTH_SHORT).show();
                migrateDataToFirestore();
            } else {
                Toast.makeText(getContext(), "Quyền truy cập bộ nhớ bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void migrateDataToFirestore() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            File file = new File(Environment.getExternalStorageDirectory(), "tasks.json");
            if (!file.exists()) {
                return;
            }

            StringBuilder jsonData = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
            reader.close();

            JSONArray tasksArray = new JSONArray(jsonData.toString());
            for (int i = 0; i < tasksArray.length(); i++) {
                JSONObject taskJson = tasksArray.getJSONObject(i);
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", taskJson.getString("id"));
                taskMap.put("title", taskJson.getString("title"));
                taskMap.put("description", taskJson.getString("description"));
                taskMap.put("dueDateTime", taskJson.getLong("dueDateTime"));
                taskMap.put("completed", taskJson.getBoolean("completed"));
                taskMap.put("priority", taskJson.getInt("priority"));
                taskMap.put("reminderEnabled", taskJson.getBoolean("reminderEnabled"));
                taskMap.put("notificationType", "none");
                taskMap.put("notificationTime", null);
                taskMap.put("notificationDays", new ArrayList<>());

                db.collection("tasks")
                        .document(userUID)
                        .collection("tasks")
                        .document(taskJson.getString("id"))
                        .set(taskMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Di chuyển công việc thành công", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Lỗi khi di chuyển công việc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            file.delete();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Lỗi khi di chuyển dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadTasksFromFirestore() {
        db.collection("tasks")
                .document(userUID)
                .collection("tasks")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(getContext(), "Lỗi khi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Task> tasks = new ArrayList<>();
                    if (value != null) {
                        for (var doc : value.getDocuments()) {
                            List<Integer> notificationDays = new ArrayList<>();
                            Object daysObj = doc.get("notificationDays");
                            if (daysObj instanceof List<?>) {
                                for (Object day : (List<?>) daysObj) {
                                    if (day instanceof Long) {
                                        notificationDays.add(((Long) day).intValue());
                                    }
                                }
                            }

                            Task task = new Task(
                                    doc.getString("id"),
                                    doc.getString("title"),
                                    doc.getString("description"),
                                    doc.getLong("dueDateTime"),
                                    doc.getBoolean("completed"),
                                    doc.getLong("priority").intValue(),
                                    doc.getBoolean("reminderEnabled"),
                                    doc.getString("notificationType"),
                                    doc.getString("notificationTime"),
                                    notificationDays
                            );
                            tasks.add(task);
                        }
                    }
                    viewModel.setTaskList(tasks);
                    scheduleAllReminders(tasks);
                });
    }

    private void scheduleAllReminders(List<Task> tasks) {
        for (Task task : tasks) {
            if (task.isCompleted()) {
                continue;
            }

            long dueDateTime = task.getDueDateTime();
            long currentTime = System.currentTimeMillis();
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            long startOfDay = today.getTimeInMillis();
            long endOfDay = startOfDay + 24 * 60 * 60 * 1000;

            // 1. Công việc sắp đến hạn (nhắc trước 10 phút)
            if (task.isReminderEnabled() && dueDateTime > currentTime) {
                scheduleReminder(task, "Công việc sắp đến hạn: " + task.getTitle(), DEFAULT_REMINDER_OFFSET, 1);
            }

            // 2. Công việc hôm nay (thông báo vào 8:00 sáng)
            if (dueDateTime >= startOfDay && dueDateTime < endOfDay) {
                Calendar reminderTime = Calendar.getInstance();
                reminderTime.setTimeInMillis(startOfDay);
                reminderTime.set(Calendar.HOUR_OF_DAY, 8);
                reminderTime.set(Calendar.MINUTE, 0);
                if (reminderTime.getTimeInMillis() > currentTime) {
                    scheduleReminder(task, "Công việc hôm nay: " + task.getTitle(), reminderTime.getTimeInMillis() - dueDateTime, 2);
                }
            }

            // 3. Công việc đã quá hạn
            if (dueDateTime < currentTime) {
                scheduleOverdueReminder(task);
            }

            // 4. Thông báo hàng ngày hoặc theo ngày trong tuần
            String notificationType = task.getNotificationType();
            if (notificationType != null && !notificationType.equals("none")) {
                scheduleCustomReminders(task);
            }
        }
    }

    private void scheduleReminder(Task task, String message, long offset, int type) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        intent.putExtra("taskTitle", task.getTitle());
        intent.putExtra("message", message);
        intent.putExtra("notificationId", task.getId().hashCode() + type);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                task.getId().hashCode() + type,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long reminderTime = task.getDueDateTime() - offset;
        if (reminderTime > System.currentTimeMillis()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }

    private void scheduleOverdueReminder(Task task) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        intent.putExtra("taskTitle", task.getTitle());
        intent.putExtra("message", "Công việc đã quá hạn: " + task.getTitle());
        intent.putExtra("notificationId", task.getId().hashCode() + 3);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                task.getId().hashCode() + 3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
    }

    private void scheduleCustomReminders(Task task) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        String notificationType = task.getNotificationType();
        String notificationTime = task.getNotificationTime();
        List<Integer> notificationDays = task.getNotificationDays();

        if (notificationTime == null) return;

        String[] timeParts = notificationTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        Calendar now = Calendar.getInstance();
        Calendar reminderTime = Calendar.getInstance();
        reminderTime.set(Calendar.HOUR_OF_DAY, hour);
        reminderTime.set(Calendar.MINUTE, minute);
        reminderTime.set(Calendar.SECOND, 0);
        reminderTime.set(Calendar.MILLISECOND, 0);

        if (notificationType.equals("daily")) {
            // Lên lịch thông báo hàng ngày
            while (reminderTime.getTimeInMillis() <= now.getTimeInMillis()) {
                reminderTime.add(Calendar.DAY_OF_MONTH, 1);
            }

            Intent intent = new Intent(requireContext(), ReminderReceiver.class);
            intent.putExtra("taskTitle", task.getTitle());
            intent.putExtra("message", "Nhắc nhở hàng ngày: " + task.getTitle());
            intent.putExtra("notificationId", task.getId().hashCode() + 4);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    task.getId().hashCode() + 4,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        } else if (notificationType.equals("weekly")) {
            // Lên lịch thông báo theo ngày trong tuần
            for (Integer day : notificationDays) {
                reminderTime = Calendar.getInstance();
                reminderTime.set(Calendar.HOUR_OF_DAY, hour);
                reminderTime.set(Calendar.MINUTE, minute);
                reminderTime.set(Calendar.SECOND, 0);
                reminderTime.set(Calendar.MILLISECOND, 0);

                // Tính ngày tiếp theo phù hợp với ngày trong tuần
                int currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
                int daysToAdd = (day - currentDayOfWeek + 7) % 7;
                if (daysToAdd == 0 && now.getTimeInMillis() > reminderTime.getTimeInMillis()) {
                    daysToAdd = 7; // Nếu đã qua giờ thông báo trong ngày hôm nay, lên lịch cho tuần sau
                }
                reminderTime.add(Calendar.DAY_OF_MONTH, daysToAdd);

                Intent intent = new Intent(requireContext(), ReminderReceiver.class);
                intent.putExtra("taskTitle", task.getTitle());
                intent.putExtra("message", "Nhắc nhở: " + task.getTitle());
                intent.putExtra("notificationId", task.getId().hashCode() + day + 4);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        task.getId().hashCode() + day + 4,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                );
            }
        }
    }

    private void showTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_task, null);
        EditText titleEditText = dialogView.findViewById(R.id.titleEditText);
        EditText descriptionEditText = dialogView.findViewById(R.id.descriptionEditText);
        EditText dueDateEditText = dialogView.findViewById(R.id.dueDateEditText);
        Spinner prioritySpinner = dialogView.findViewById(R.id.prioritySpinner);
        CheckBox reminderCheckBox = dialogView.findViewById(R.id.reminderCheckBox);
        Spinner notificationTypeSpinner = dialogView.findViewById(R.id.notificationTypeSpinner);
        LinearLayout notificationTimeLayout = dialogView.findViewById(R.id.notificationTimeLayout);
        EditText notificationTimeEditText = dialogView.findViewById(R.id.notificationTimeEditText);
        LinearLayout notificationDaysLayout = dialogView.findViewById(R.id.notificationDaysLayout);
        CheckBox[] dayCheckBoxes = new CheckBox[]{
                dialogView.findViewById(R.id.dayMonday),
                dialogView.findViewById(R.id.dayTuesday),
                dialogView.findViewById(R.id.dayWednesday),
                dialogView.findViewById(R.id.dayThursday),
                dialogView.findViewById(R.id.dayFriday),
                dialogView.findViewById(R.id.daySaturday),
                dialogView.findViewById(R.id.daySunday)
        };

        selectedDateTime = Calendar.getInstance();
        selectedNotificationTime = Calendar.getInstance();

        if (task != null) {
            titleEditText.setText(task.getTitle());
            descriptionEditText.setText(task.getDescription());
            selectedDateTime.setTimeInMillis(task.getDueDateTime());
            dueDateEditText.setText(task.getFormattedDueDateTime());
            prioritySpinner.setSelection(task.getPriority() - 1);
            reminderCheckBox.setChecked(task.isReminderEnabled());

            String notificationType = task.getNotificationType();
            if (notificationType.equals("daily")) {
                notificationTypeSpinner.setSelection(1);
                notificationTimeLayout.setVisibility(View.VISIBLE);
            } else if (notificationType.equals("weekly")) {
                notificationTypeSpinner.setSelection(2);
                notificationTimeLayout.setVisibility(View.VISIBLE);
                notificationDaysLayout.setVisibility(View.VISIBLE);
            } else {
                notificationTypeSpinner.setSelection(0);
            }

            if (task.getNotificationTime() != null) {
                notificationTimeEditText.setText(task.getNotificationTime());
                String[] timeParts = task.getNotificationTime().split(":");
                selectedNotificationTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                selectedNotificationTime.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            }

            List<Integer> notificationDays = task.getNotificationDays();
            for (int i = 0; i < dayCheckBoxes.length; i++) {
                dayCheckBoxes[i].setChecked(notificationDays.contains(i + 1));
            }

            builder.setTitle("Chỉnh sửa công việc");
        } else {
            builder.setTitle("Thêm công việc mới");
        }

        dueDateEditText.setOnClickListener(v -> showDateTimePicker(dueDateEditText));

        notificationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Không thông báo
                    notificationTimeLayout.setVisibility(View.GONE);
                    notificationDaysLayout.setVisibility(View.GONE);
                } else if (position == 1) { // Hàng ngày
                    notificationTimeLayout.setVisibility(View.VISIBLE);
                    notificationDaysLayout.setVisibility(View.GONE);
                } else { // Theo ngày trong tuần
                    notificationTimeLayout.setVisibility(View.VISIBLE);
                    notificationDaysLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        notificationTimeEditText.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        selectedNotificationTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedNotificationTime.set(Calendar.MINUTE, minute);
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        notificationTimeEditText.setText(timeStr);
                    },
                    selectedNotificationTime.get(Calendar.HOUR_OF_DAY),
                    selectedNotificationTime.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });

        builder.setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String title = titleEditText.getText().toString().trim();
                    String description = descriptionEditText.getText().toString().trim();
                    long dueDateTime = selectedDateTime.getTimeInMillis();
                    int priority = prioritySpinner.getSelectedItemPosition() + 1;
                    boolean reminderEnabled = reminderCheckBox.isChecked();
                    String notificationType = notificationTypeSpinner.getSelectedItemPosition() == 0 ? "none" :
                            notificationTypeSpinner.getSelectedItemPosition() == 1 ? "daily" : "weekly";
                    String notificationTime = notificationType.equals("none") ? null :
                            String.format(Locale.getDefault(), "%02d:%02d",
                                    selectedNotificationTime.get(Calendar.HOUR_OF_DAY),
                                    selectedNotificationTime.get(Calendar.MINUTE));
                    List<Integer> notificationDays = new ArrayList<>();
                    if (notificationType.equals("weekly")) {
                        for (int i = 0; i < dayCheckBoxes.length; i++) {
                            if (dayCheckBoxes[i].isChecked()) {
                                notificationDays.add(i + 1); // 1=Thứ 2, 2=Thứ 3, ..., 7=Chủ Nhật
                            }
                        }
                    }

                    if (title.isEmpty()) {
                        Toast.makeText(getContext(), "Vui lòng nhập tiêu đề", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String taskId = task != null ? task.getId() : UUID.randomUUID().toString();
                    Task newTask = new Task(taskId, title, description, dueDateTime, false, priority, reminderEnabled,
                            notificationType, notificationTime, notificationDays);

                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("id", newTask.getId());
                    taskMap.put("title", newTask.getTitle());
                    taskMap.put("description", newTask.getDescription());
                    taskMap.put("dueDateTime", newTask.getDueDateTime());
                    taskMap.put("completed", newTask.isCompleted());
                    taskMap.put("priority", newTask.getPriority());
                    taskMap.put("reminderEnabled", newTask.isReminderEnabled());
                    taskMap.put("notificationType", newTask.getNotificationType());
                    taskMap.put("notificationTime", newTask.getNotificationTime());
                    taskMap.put("notificationDays", newTask.getNotificationDays());

                    db.collection("tasks")
                            .document(userUID)
                            .collection("tasks")
                            .document(taskId)
                            .set(taskMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Lưu công việc thành công", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Lỗi khi lưu công việc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Hủy", null);

        builder.show();
    }

    private void showDateTimePicker(EditText dueDateEditText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDateTime.set(year, month, dayOfMonth);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (view1, hourOfDay, minute) -> {
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                dueDateEditText.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(selectedDateTime.getTime()));
                            },
                            selectedDateTime.get(Calendar.HOUR_OF_DAY),
                            selectedDateTime.get(Calendar.MINUTE),
                            true
                    );
                    timePickerDialog.show();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    public void onSearchQueryChanged(String query) {
        viewModel.setSearchQuery(query);
    }

    public void onSortDueDate() {
        viewModel.setSortType("dueDate");
    }

    public void onSortPriority() {
        viewModel.setSortType("priority");
    }

    public void onSortStatus() {
        viewModel.setSortType("status");
    }

    public void onFilterStatus() {
        showFilterStatusDialog();
    }

    public void onFilterDue() {
        showFilterDueDialog();
    }

    public void onFilterPriority() {
        showFilterPriorityDialog();
    }

    private void showFilterStatusDialog() {
        String[] options = {"Tất cả", "Đã hoàn thành", "Chưa hoàn thành"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc theo trạng thái")
                .setItems(options, (dialog, which) -> {
                    String filter = which == 0 ? "all" : which == 1 ? "completed" : "not_completed";
                    viewModel.setFilterStatus(filter);
                })
                .show();
    }

    private void showFilterDueDialog() {
        String[] options = {"Tất cả", "Hết hạn", "Còn hạn"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc theo hạn")
                .setItems(options, (dialog, which) -> {
                    String filter = which == 0 ? "all" : which == 1 ? "overdue" : "not_overdue";
                    viewModel.setFilterDue(filter);
                })
                .show();
    }

    private void showFilterPriorityDialog() {
        String[] options = {"Tất cả", "Cao", "Trung bình", "Thấp"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Lọc theo ưu tiên")
                .setItems(options, (dialog, which) -> {
                    String filter = which == 0 ? "all" : which == 1 ? "high" : which == 2 ? "medium" : "low";
                    viewModel.setFilterPriority(filter);
                })
                .show();
    }

    @Override
    public void onTaskChecked(Task task, boolean isChecked) {
        task.setCompleted(isChecked);
        db.collection("tasks")
                .document(userUID)
                .collection("tasks")
                .document(task.getId())
                .update("completed", isChecked)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                    if (isChecked) {
                        cancelReminders(task);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi khi cập nhật trạng thái: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void cancelReminders(Task task) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        for (int type = 1; type <= 11; type++) { // Bao gồm cả thông báo hàng ngày và theo ngày
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    task.getId().hashCode() + type,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onTaskLongClick(Task task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Quản lý công việc")
                .setItems(new String[]{"Chỉnh sửa", "Xóa"}, (dialog, which) -> {
                    if (which == 0) {
                        showTaskDialog(task);
                    } else {
                        db.collection("tasks")
                                .document(userUID)
                                .collection("tasks")
                                .document(task.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Xóa công việc thành công", Toast.LENGTH_SHORT).show();
                                    cancelReminders(task);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Lỗi khi xóa công việc: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .show();
    }
}