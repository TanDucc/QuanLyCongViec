package com.example.nhom20;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class CalendarFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Chỉ inflate layout hiển thị chữ "Lịch"
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    // Giữ lại các phương thức này để tránh lỗi trong HomeActivity
    public void onFilterStatus() {
        // Không làm gì cả
    }

    public void onFilterPriority() {
        // Không làm gì cả
    }
}