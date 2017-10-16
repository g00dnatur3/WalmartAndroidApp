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
        loadProducts(new Function() {
            @Override
            public void call(Object... args) {
                showProduct(mPosition);
            }
        });
    }

    private void loadProducts(Function onComplete) {
        showLoadingIndicator();
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
            mActivity.ensureDataLoaded(
                    fromIndex,
                    toIndex,
                    onComplete,
                    false); //false=do not update loading indicator
        }
    }

    private void showProduct(final int index) {
        final WalmartService service = mActivity.getService();
        if (service != null) {
            JsonNode productNode = service.getProduct(index);
            if (productNode != null) {
                showProduct(service, productNode);
            } else {
                Log.e(TAG, "showProduct failed  - productNode is null, retrying at position: " + mPosition);
                // sometimes if the user is being very busy with the device, it can get here.
                // - explanation:
                // the reason this can happen revolves around the fact that I am only allowing 2 pages
                // to be cached.. so if the user scrolls fast enuff and clicks on an item, it can  get here
                // because more than 2 pages are loading, the lru policy kicks in and purges
                // the page the user happens to be on...
                // -
                // lets have a single retry to be more robust
                // - fix:
                // not an easy bug to reproduce, fix verified from the log:
                // Log: WalmartServiceUtils: getPage failed - at: 13, reason: page not loaded into cache
                // Log: ProductDetailFragment: showProduct failed  - productNode is null, retrying at position: 1389
                // prdouct shows up corretly after retry... before it was blank.
                loadProducts(new Function() {
                    @Override
                    public void call(Object... args) {
                        JsonNode _productNode = service.getProduct(index);
                        if (_productNode == null) {
                            Log.e(TAG, "showProduct retry failed  - productNode is null, position: " + mPosition);
                            hideLoadingIndicator();
                        } else {
                            showProduct(service, _productNode);
                        }
                    }
                });
            }
        } else {
            hideLoadingIndicator();
            Log.e(TAG, "showProduct failed  - WalmartService is not bound, position: " + mPosition);
        }
    }

    private void showProduct(WalmartService service, JsonNode productNode) {
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
