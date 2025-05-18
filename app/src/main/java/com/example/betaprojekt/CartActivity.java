package com.example.betaprojekt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.MenuItemCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {
    private static final String LOG_TAG = CartActivity.class.getName();
    private FirebaseUser user;

    private RecyclerView mRecyclerView;
    private ShoppingItemAdapter mAdapter;
    private ArrayList<Medicine> mCartItems = new ArrayList<>();

    private FirebaseFirestore mFirestore;
    private CollectionReference mCartItemsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(LOG_TAG, "Authenticated user!");
        } else {
            Log.d(LOG_TAG, "Unauthenticated user!");
            finish();
            return;
        }

        mFirestore = FirebaseFirestore.getInstance();
        mCartItemsRef = mFirestore
                .collection("carts")
                .document(user.getUid())
                .collection("items");

        mRecyclerView = findViewById(R.id.recyclerViewCart);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ShoppingItemAdapter(this, mCartItems, ShoppingItemAdapter.Mode.CART);
        mRecyclerView.setAdapter(mAdapter);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Kosár");

        loadCartItems();
    }

    private void loadCartItems() {
        mCartItems.clear();

        TextView emptyCartText = findViewById(R.id.emptyCartText);

        mCartItemsRef
                .whereEqualTo("available", "ONLINE ELÉRHETŐ")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Medicine item = document.toObject(Medicine.class);
                        item.setId(document.getId());
                        mCartItems.add(item);
                    }

                    mAdapter.notifyDataSetChanged();

                    if (mCartItems.isEmpty()) {
                        emptyCartText.setVisibility(View.VISIBLE);
                    } else {
                        emptyCartText.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Hiba az ONLINE ELÉRHETŐ elemek lekérdezésében", e);
                    emptyCartText.setVisibility(View.VISIBLE);
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.cart_list_menu, menu);

        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(LOG_TAG, "Keresés: " + newText);
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
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;

        } else if (id == R.id.setting_button) {
            Log.d(LOG_TAG, "Setting clicked");
            return true;

        } else if (id == R.id.home) {
            Log.d(LOG_TAG, "Home clicked");
            Intent intent = new Intent(this, ShopListActivity.class);
            startActivity(intent);
            finish();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void deleteCartItem(Medicine item) {
        mCartItemsRef.document(item._getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    mCartItems.remove(item);
                    mAdapter.notifyDataSetChanged();

                    TextView emptyCartText = findViewById(R.id.emptyCartText);
                    if (mCartItems.isEmpty()) {
                        emptyCartText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(LOG_TAG, "Nem sikerült törölni az elemet a kosárból", e);
                });
    }
}
