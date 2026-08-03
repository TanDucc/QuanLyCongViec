package com.example.nhom20;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks = new ArrayList<>();
    private final OnTaskInteractionListener listener;

    public interface OnTaskInteractionListener {
        void onTaskChecked(Task task, boolean isChecked);
        void onTaskLongClick(Task task);
    }

    public TaskAdapter(OnTaskInteractionListener listener) {
        this.listener = listener;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.titleTextView.setText(task.getTitle());
        holder.descriptionTextView.setText(task.getDescription());
        holder.dueDateTextView.setText(task.getFormattedDueDateTime());
        holder.priorityTextView.setText("Ưu tiên: " + (task.getPriority() == 1 ? "Thấp" : task.getPriority() == 2 ? "Trung bình" : "Cao"));
        holder.completedCheckBox.setChecked(task.isCompleted());

        // Tô màu theo ưu tiên
        int priorityColor = task.getPriority() == 3 ? Color.RED : task.getPriority() == 2 ? Color.YELLOW : Color.GREEN;
        holder.priorityTextView.setTextColor(priorityColor);

        // Đánh dấu quá hạn
        if (task.isOverdue()) {
            holder.dueDateTextView.setText(task.getFormattedDueDateTime() + " (ĐÃ HẾT HẠN)");
            holder.dueDateTextView.setTextColor(Color.RED);
        } else {
            holder.dueDateTextView.setTextColor(Color.BLACK);
        }

        holder.completedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            listener.onTaskChecked(task, isChecked);
        });

        holder.itemView.setOnLongClickListener(v -> {
            listener.onTaskLongClick(task);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, descriptionTextView, dueDateTextView, priorityTextView;
        CheckBox completedCheckBox;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            descriptionTextView = itemView.findViewById(R.id.descriptionTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
            priorityTextView = itemView.findViewById(R.id.priorityTextView);
            completedCheckBox = itemView.findViewById(R.id.completedCheckBox);
        }
    }
}