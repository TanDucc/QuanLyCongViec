package com.example.nhom20;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private BottomNavigationView bottomNavigationView;
    private TasksFragment tasksFragment = new TasksFragment();
    private CalendarFragment calendarFragment = new CalendarFragment();
    private ProfileFragment profileFragment = new ProfileFragment();
    private Fragment currentFragment;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Khởi tạo Firestore và Authentication
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Kiểm tra đăng nhập
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(tasksFragment, false);
            bottomNavigationView.setSelectedItemId(R.id.nav_tasks);
            currentFragment = tasksFragment;
        }
    }

    public FirebaseFirestore getFirestore() {
        return db;
    }

    public String getCurrentUserUID() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_tasks) {
                selectedFragment = tasksFragment;
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = calendarFragment;
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = profileFragment;
            }

            boolean loaded = loadFragment(selectedFragment, true);
            if (loaded) {
                currentFragment = selectedFragment;
                invalidateOptionsMenu();
            }
            return loaded;
        });
    }

    private boolean loadFragment(Fragment fragment, boolean addToBackStack) {
        if (fragment != null) {
            try {
                Fragment currentFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (currentFragment != null && currentFragment.getClass() == fragment.getClass()) {
                    return true;
                }

                var transaction = getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out,
                                android.R.anim.fade_in,
                                android.R.anim.fade_out)
                        .replace(R.id.fragment_container, fragment);

                if (addToBackStack) {
                    transaction.addToBackStack(null);
                }

                transaction.commit();
                return true;
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error loading fragment", e);
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_item", bottomNavigationView.getSelectedItemId());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int selectedItem = savedInstanceState.getInt("selected_item", R.id.nav_tasks);
        bottomNavigationView.setSelectedItemId(selectedItem);
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof TasksFragment) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Công việc");
            }
        } else if (fragment instanceof CalendarFragment) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Lịch");
            }
        } else if (fragment instanceof ProfileFragment) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Hồ sơ");
            }
        }
        currentFragment = fragment;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (currentFragment instanceof TasksFragment) {
            getMenuInflater().inflate(R.menu.tasks_menu, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (currentFragment instanceof TasksFragment) {
                        ((TasksFragment) currentFragment).onSearchQueryChanged(newText);
                    }
                    return true;
                }
            });
        } else if (currentFragment instanceof CalendarFragment) {
            getMenuInflater().inflate(R.menu.calendar_menu, menu);

        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (currentFragment instanceof TasksFragment) {
            TasksFragment tasksFragment = (TasksFragment) currentFragment;
            if (id == R.id.action_sort_due_date) {
                tasksFragment.onSortDueDate();
                return true;
            } else if (id == R.id.action_sort_priority) {
                tasksFragment.onSortPriority();
                return true;
            } else if (id == R.id.action_sort_status) {
                tasksFragment.onSortStatus();
                return true;
            } else if (id == R.id.action_filter_status) {
                tasksFragment.onFilterStatus();
                return true;
            } else if (id == R.id.action_filter_due) {
                tasksFragment.onFilterDue();
                return true;
            } else if (id == R.id.action_filter_priority) {
                tasksFragment.onFilterPriority();
                return true;
            }
        } else if (currentFragment instanceof CalendarFragment) {
            CalendarFragment calendarFragment = (CalendarFragment) currentFragment;
            if (id == R.id.filter_status) {
                calendarFragment.onFilterStatus();
                return true;
            } else if (id == R.id.filter_priority) {
                calendarFragment.onFilterPriority();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void signOut() {
        mAuth.signOut();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}