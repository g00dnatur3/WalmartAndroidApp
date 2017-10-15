package com.walmart.products.view;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.walmart.products.activity.ProductDetailActivity;
import com.walmart.products.R;

import com.walmart.products.service.WalmartService;
import com.walmart.products.util.Function;

import java.util.HashMap;
import java.util.Map;

import static com.walmart.products.service.WalmartServiceConfig.PAGE_SIZE;

public class ProductDetailFragment extends Fragment {

    protected final String TAG = getClass().getCanonicalName();

    // needed to know when to stop showing loading indicator
    private static final Map<Integer, Boolean> mFragmentsLoading = new HashMap<Integer, Boolean>();

    // called by ProductDetailAdapter constructor
    public static void clearFragmentsLoading() {
        mFragmentsLoading.clear();
    }

    private ProductDetailActivity mActivity;

    private int mPosition;

    // TODO: Replace these with data binding
    private ImageView mImage;
    private TextView mName;
    private TextView mDesc;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mPosition = bundle.getInt("position", 0);
        }
        mActivity = (ProductDetailActivity) getActivity();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.product_detail_item, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImage = (ImageView) view.findViewById(R.id.detail_image);
        mName = (TextView) view.findViewById(R.id.detail_name);
        mDesc = (TextView) view.findViewById(R.id.detail_desc);
        Log.i(TAG, "onViewCreated - position: " + mPosition);

        // viewPager has 3 views at any given moment and only one is visible
        // in order to know when they are all completed loading
        // we check if the mFragmentsLoading.isEmpty
        showLoadingIndicator();

        final Function onComplete = new Function() {
            @Override
            public void call(Object... args) {
                showProduct(mPosition);
            }
        };

        // if we are near the end, load more
        if (mActivity.getItemCount()-mPosition <= 20) {
            Log.i(TAG, "Calling mActivity.loadMore");
            mActivity.loadMore(onComplete, false); //false=do not update loading indicator
        }
        else {
            // ensure the data we want is loaded
            int fromIndex = mPosition - (PAGE_SIZE/2);
            if (fromIndex < 0) fromIndex = 0;
            int toIndex = mPosition + (PAGE_SIZE/2);
            mActivity.ensureDataLoaded(fromIndex, toIndex, onComplete, false); //false=do not update loading indicator
        }
    }

    private void showProduct(int index) {
        final WalmartService service = mActivity.getService();
        if (service != null) {
            JsonNode productNode = service.getProduct(index);
            if (productNode != null) {
                if (productNode.get("name") != null) {
                    mName.setText(productNode.get("name").textValue());
                }
                if (productNode.get("shortDescription") != null) {
                    mDesc.setText(productNode.get("shortDescription").textValue());
                }
                Function onComplete = new Function() {
                    @Override
                    public void call(Object... args) {
                        if (args[0] != null) {
                            Log.e(TAG, args[0].toString());
                        } else {
                            mImage.setImageBitmap((Bitmap) args[1]);
                        }
                        hideLoadingIndicator();
                    }
                };
                service.getMediumImage(mPosition, onComplete);
            } else {
                hideLoadingIndicator();
                Log.e(TAG, "showProduct failed  - productNode is null, position: " + mPosition);
            }
        } else {
            hideLoadingIndicator();
            Log.e(TAG, "showProduct failed  - WalmartService is not bound, position: " + mPosition);
        }
    }

    private void showLoadingIndicator() {
        mFragmentsLoading.put(mPosition, true);
        mActivity.showLoadingIndicator();
    }

    private void hideLoadingIndicator() {
        mFragmentsLoading.remove(mPosition);
        if (mFragmentsLoading.isEmpty()) mActivity.hideLoadingIndicator();
    }
}
