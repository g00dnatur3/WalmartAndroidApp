package com.walmart.products.view;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.walmart.products.activity.ProductListActivity;
import com.walmart.products.R;
import com.walmart.products.service.WalmartService;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    protected final String TAG = getClass().getCanonicalName();

    private ProductListActivity mActivity;

    public ProductListAdapter(ProductListActivity mActivity) {
        this.mActivity = mActivity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.product_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        WalmartService service = mActivity.getService();
        if (service != null) {
            JsonNode productNode = service.getProduct(position);
            if (productNode != null) {
                if (productNode.get("name") != null) {
                    holder.textView.setText(productNode.get("name").textValue());
                }
                Bitmap bmp = service.getThumbnail(position);
                if (bmp != null) {
                    holder.imageView.setImageBitmap(bmp);
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mActivity.startProductDetailActivity(position);
                    }
                });
            } else {
                holder.textView.setText(null);
                holder.imageView.setImageBitmap(null);
                holder.itemView.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mActivity.getItemCount();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.text);
            // increase text size a lil
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() + 4);
            imageView = (ImageView) itemView.findViewById(R.id.thumb);
        }
    }
}
