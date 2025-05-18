package com.example.betaprojekt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ShopListActivity extends AppCompatActivity {
    private static final String LOG_TAG = ShopListActivity.class.getName();
    private FirebaseUser user;


    private RecyclerView mRecyclerView;
    private ArrayList<Medicine> mItemList;
    private ShoppingItemAdapter mAdapter;


    private FirebaseFirestore mFirestore;
    private CollectionReference mItems;
    private CollectionReference mItemsCart;


    private NotificationHandler mNotificationHandler;
    private AlarmManager mAlarmManager;
    private JobScheduler mJobScheduler;


    private FrameLayout greenCircle;
    private TextView contentTextView;

    private int gridNumber = 1;
    private int cartItems = 0;
    private int queryLimit = 10;
    private boolean viewRow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shop_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
        }

        mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, gridNumber));
        mItemList = new ArrayList<>();

        mAdapter = new ShoppingItemAdapter(this, mItemList, ShoppingItemAdapter.Mode.SHOP);
        mRecyclerView.setAdapter(mAdapter);


        mFirestore = FirebaseFirestore.getInstance();
        mItems = mFirestore.collection("meds");
        mItemsCart = mFirestore.collection("carts");

        queryData();



        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(powerReceiver, filter);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }

        mNotificationHandler = new NotificationHandler(this);
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mJobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

        setJobScheduler();


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Gyógyszerbolt");
    }

    BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null)
                return;
            switch (action){
                case Intent.ACTION_POWER_CONNECTED:
                    queryLimit = 10;
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    queryLimit = 10;
                    break;
            }

            queryData();
        }
    };

    private void queryData(){
        mItemList.clear();

        mItems.orderBy("cartedCount", Query.Direction.DESCENDING).limit(queryLimit).get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot document : queryDocumentSnapshots){
                Medicine item = document.toObject(Medicine.class);
                item.setId(document.getId());
                mItemList.add(item);
            }
            if (mItemList.size() == 0){
                initializeData();
                queryData();
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    public void deleteItem(Medicine item){
        DocumentReference ref = mItems.document(item._getId());

        ref.delete().addOnSuccessListener(succes -> {
            Log.d(LOG_TAG, "Item is succesfully deleted: " + item._getId());
        })
        .addOnFailureListener(failure -> {
            Toast.makeText(this, "Item " + item._getId() + " cannot be deleted", Toast.LENGTH_LONG).show();
        });

        queryData();
        mNotificationHandler.cancel();
    }

    private void initializeData(){
        String[] itemsList = getResources().getStringArray(R.array.shopping_item_names);
        String[] itemsInfo = getResources().getStringArray(R.array.shopping_item_desc);
        String[] itemsPrice = getResources().getStringArray(R.array.shopping_item_price);
        TypedArray itemsImageResources = getResources().obtainTypedArray(R.array.shopping_item_images);
        String[] elerheto = getResources().getStringArray(R.array.shopping_item_available);

        for (int i = 0; i < itemsList.length; i++) {
            mItems.add(new Medicine(
                    itemsList[i],
                    itemsInfo[i],
                    itemsPrice[i],
                    elerheto[i],
                    itemsImageResources.getResourceId(i, 0),
                    0));
        }

        itemsImageResources.recycle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.shop_list_menu_jo, menu);
        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(LOG_TAG, newText);
                mAdapter.getFilter().filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.logout_button) {
            Log.d(LOG_TAG, "Logout clicked");
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        } else if (id == R.id.setting_button) {
            Log.d(LOG_TAG, "Setting clicked");
            return true;
        } else if (id == R.id.cart) {
            Log.d(LOG_TAG, "Cart clicked");
            Intent intent = new Intent(this, CartActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.view_selector) {
            Log.d(LOG_TAG, "View clicked");
            if (viewRow) {
                changeSpanCount(item, R.drawable.view, 1);
            } else {
                changeSpanCount(item, R.drawable.view, 2);
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void changeSpanCount(MenuItem item, int drawableId, int spanCount) {
        viewRow = !viewRow;
        item.setIcon(drawableId);
        GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
        layoutManager.setSpanCount(spanCount);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        final MenuItem alertMenuItem = menu.findItem(R.id.cart);
        FrameLayout rootView = (FrameLayout) alertMenuItem.getActionView();

        greenCircle = (FrameLayout) rootView.findViewById(R.id.view_alert_green_circle);
        contentTextView = (TextView) rootView.findViewById(R.id.view_alert_count_textview);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("carts")
                .document(userId)
                .collection("items")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cartItems = queryDocumentSnapshots.size();
                    updateCartIcon();
                });


        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(alertMenuItem);
            }
        });

        return super.onPrepareOptionsMenu(menu);
    }

    private void updateCartIcon() {
        if (greenCircle != null && contentTextView != null) {
            if (cartItems > 0) {
                greenCircle.setVisibility(View.VISIBLE);
                contentTextView.setText(String.valueOf(cartItems));
            } else {
                greenCircle.setVisibility(View.GONE);
                contentTextView.setText("");
            }
        }
    }

    public void updateAlertIcon(Medicine item) {
        if (!"ONLINE ELÉRHETŐ".equalsIgnoreCase(item.getAvailable())) {
            Toast.makeText(this, "Ez a termék jelenleg nem elérhető online.", Toast.LENGTH_SHORT).show();
            return;
        }

        mItems.document(item._getId()).update("cartedCount", item.getCartedCount() + 1)
                .addOnFailureListener(failure -> {
                    Toast.makeText(this, "Item " + item._getId() + " cannot be changed", Toast.LENGTH_LONG).show();
                });

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("carts")
                .document(userId)
                .collection("items")
                .document(item._getId())
                .set(item)
                .addOnSuccessListener(aVoid -> {
                    Log.d(LOG_TAG, "Kosárba rakva: " + item.getName());

                    // Kosár ikon frissítése
                    FirebaseFirestore.getInstance()
                            .collection("carts")
                            .document(userId)
                            .collection("items")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                cartItems = queryDocumentSnapshots.size();
                                updateCartIcon();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Hiba kosárba rakás közben", e);
                });
        mNotificationHandler.send(item.getName());
        queryData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(powerReceiver);
    }

    private void setAlarmManager(){
        long repeatInterval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        long triggerTime = SystemClock.elapsedRealtime() + repeatInterval;
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        mAlarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                repeatInterval,
                pendingIntent);
    }

    private void setJobScheduler() {
        int networkType = JobInfo.NETWORK_TYPE_UNMETERED;
        int hardDeadLine = 5000;

        ComponentName name = new ComponentName(getPackageName(), NotificationJobService.class.getName());
        JobInfo.Builder builder = new JobInfo.Builder(0, name)
                .setRequiredNetworkType(networkType)
                .setRequiresCharging(true)
                .setOverrideDeadline(hardDeadLine);

        mJobScheduler.schedule(builder.build());

        // mJobScheduler.cancel(0);
    }
}