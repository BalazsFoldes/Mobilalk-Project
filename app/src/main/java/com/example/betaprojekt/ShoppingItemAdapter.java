package com.example.betaprojekt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> implements Filterable {
    public enum Mode {
        SHOP,
        CART
    }

    private Mode mode;

    private ArrayList<Medicine> mShoppingItemsData = new ArrayList<>();
    private ArrayList<Medicine> mShoppingItemsDataAll = new ArrayList<>();
    private Context mContext;
    private int lastPosition = -1;
    ShoppingItemAdapter(Context context, ArrayList<Medicine> itemsData, Mode mode) {
        this.mShoppingItemsData = itemsData;
        this.mShoppingItemsDataAll = itemsData;
        this.mContext = context;
        this.mode = mode;
    }

    @Override
    public ShoppingItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ShoppingItemAdapter.ViewHolder holder, int position) {
        Medicine currentItem = mShoppingItemsData.get(position);
        
        holder.bindTo(currentItem);

        if (holder.getAdapterPosition() > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_row);
            holder.itemView.startAnimation(animation);
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return mShoppingItemsData.size();
    }

    @Override
    public Filter getFilter() {
        return shoppingFilter;
    }

    private Filter shoppingFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            ArrayList<Medicine> filteredList = new ArrayList<>();
            FilterResults results = new FilterResults();

            if(charSequence == null || charSequence.length() == 0) {
                results.count = mShoppingItemsDataAll.size();
                results.values = mShoppingItemsDataAll;
            } else {
                String filterPattern = charSequence.toString().toLowerCase().trim();
                for (Medicine item : mShoppingItemsDataAll){
                    if(item.getName().toLowerCase().contains(filterPattern)){
                        filteredList.add(item);
                    }
                }
                results.count = filteredList.size();
                results.values = filteredList;
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            mShoppingItemsData = (ArrayList) filterResults.values;
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mTitleText;
        private TextView mInfoText;
        private TextView mPriceText;
        private ImageView mItemImage;
        private TextView mElerheto;
        public ViewHolder(View itemView){
            super(itemView);

            mTitleText = itemView.findViewById(R.id.itemTitle);
            mInfoText = itemView.findViewById(R.id.subTitle);
            mPriceText = itemView.findViewById(R.id.price);
            mItemImage = itemView.findViewById(R.id.itemImage);
            mElerheto = itemView.findViewById(R.id.elerheto);
        }

        public void bindTo(Medicine currentItem) {
            mTitleText.setText(currentItem.getName());
            mInfoText.setText(currentItem.getInfo());
            mPriceText.setText(currentItem.getPrice());
            mElerheto.setText(currentItem.getAvailable());

            Glide.with(mContext).load(currentItem.getImageResource()).into(mItemImage);

            if (mode == Mode.SHOP) {
                itemView.findViewById(R.id.add_to_cart).setVisibility(View.VISIBLE);
                itemView.findViewById(R.id.delete).setVisibility(View.GONE);

                itemView.findViewById(R.id.add_to_cart).setOnClickListener(view -> {
                    if (mContext instanceof ShopListActivity) {
                        ((ShopListActivity) mContext).updateAlertIcon(currentItem);
                    }
                });

            } else if (mode == Mode.CART) {
                itemView.findViewById(R.id.add_to_cart).setVisibility(View.GONE);
                itemView.findViewById(R.id.delete).setVisibility(View.VISIBLE);

                itemView.findViewById(R.id.delete).setOnClickListener(view -> {
                    if (mContext instanceof CartActivity) {
                        ((CartActivity) mContext).deleteCartItem(currentItem);
                    }
                });
            }
        }
    };
};