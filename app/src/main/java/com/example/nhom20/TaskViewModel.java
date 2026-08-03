package com.example.nhom20;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class TaskViewModel extends ViewModel {
    private final MutableLiveData<List<Task>> filteredTaskList = new MutableLiveData<>(new ArrayList<>());
    private String searchQuery = "";
    private String sortType = "";
    private String filterStatus = "all";
    private String filterDue = "all";
    private String filterPriority = "all";

    public LiveData<List<Task>> getFilteredTaskList() {
        return filteredTaskList;
    }

    public void setTaskList(List<Task> tasks) {
        applyFiltersAndSort(tasks);
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query.toLowerCase();
        applyFiltersAndSort(filteredTaskList.getValue());
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
        applyFiltersAndSort(filteredTaskList.getValue());
    }

    public void setFilterStatus(String filterStatus) {
        this.filterStatus = filterStatus;
        applyFiltersAndSort(filteredTaskList.getValue());
    }

    public void setFilterDue(String filterDue) {
        this.filterDue = filterDue;
        applyFiltersAndSort(filteredTaskList.getValue());
    }

    public void setFilterPriority(String filterPriority) {
        this.filterPriority = filterPriority;
        applyFiltersAndSort(filteredTaskList.getValue());
    }

    private void applyFiltersAndSort(List<Task> tasks) {
        if (tasks == null) return;

        List<Task> filteredTasks = new ArrayList<>(tasks);

        if (!searchQuery.isEmpty()) {
            filteredTasks = filteredTasks.stream()
                    .filter(task -> task.getTitle().toLowerCase().contains(searchQuery) ||
                            task.getDescription().toLowerCase().contains(searchQuery))
                    .collect(Collectors.toList());
        }

        if (!filterStatus.equals("all")) {
            filteredTasks = filteredTasks.stream()
                    .filter(task -> filterStatus.equals("completed") == task.isCompleted())
                    .collect(Collectors.toList());
        }

        if (!filterDue.equals("all")) {
            long currentTime = System.currentTimeMillis();
            filteredTasks = filteredTasks.stream()
                    .filter(task -> {
                        boolean isOverdue = task.getDueDateTime() < currentTime;
                        return filterDue.equals("overdue") == isOverdue;
                    })
                    .collect(Collectors.toList());
        }

        if (!filterPriority.equals("all")) {
            filteredTasks = filteredTasks.stream()
                    .filter(task -> {
                        String priority = task.getPriority() == 1 ? "high" :
                                task.getPriority() == 2 ? "medium" : "low";
                        return filterPriority.equals(priority);
                    })
                    .collect(Collectors.toList());
        }

        switch (sortType) {
            case "dueDate":
                Collections.sort(filteredTasks, Comparator.comparingLong(Task::getDueDateTime));
                break;
            case "priority":
                Collections.sort(filteredTasks, Comparator.comparingInt(Task::getPriority));
                break;
            case "status":
                Collections.sort(filteredTasks, Comparator.comparing(Task::isCompleted));
                break;
        }

        filteredTaskList.setValue(filteredTasks);
    }
}