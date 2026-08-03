package com.example.nhom20;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {
    private TextView completedTasksCount;
    private TextView pendingTasksCount;
    private Button signOutButton;
    private FirebaseFirestore db;
    private String userUID;
    private List<Task> allTasks = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = ((HomeActivity) requireActivity()).getFirestore();
        userUID = ((HomeActivity) requireActivity()).getCurrentUserUID();
        if (userUID == null) {
            Toast.makeText(getContext(), "Người dùng chưa đăng nhập", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return view;
        }

        completedTasksCount = view.findViewById(R.id.completedTasksCount);
        pendingTasksCount = view.findViewById(R.id.pendingTasksCount);
        signOutButton = view.findViewById(R.id.signOutButton);

        signOutButton.setOnClickListener(v -> {
            ((HomeActivity) requireActivity()).signOut();
        });

        loadTasksFromFirestore();

        return view;
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

                    allTasks.clear();

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
                            allTasks.add(task);
                        }
                    }

                    updateStatistics();
                });
    }

    private void updateStatistics() {
        // Tính toán thống kê
        int completedCount = 0;
        int pendingCount = 0;

        for (Task task : allTasks) {
            if (task.isCompleted()) {
                completedCount++;
            } else {
                pendingCount++;
            }
        }

        completedTasksCount.setText(String.valueOf(completedCount));
        pendingTasksCount.setText(String.valueOf(pendingCount));
    }
}