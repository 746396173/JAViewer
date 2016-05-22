package io.github.javiewer.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import io.github.javiewer.adapter.ActressAdapter;
import io.github.javiewer.adapter.MovieAdapter;
import io.github.javiewer.network.Network;
import io.github.javiewer.network.converter.HtmlConverter;
import io.github.javiewer.network.wrapper.ActressWrapper;
import io.github.javiewer.network.wrapper.MovieWrapper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ActressesFragment extends RecyclerFragment<ActressAdapter.ViewHolder, StaggeredGridLayoutManager> {

    public List<ActressWrapper> actresses = new ArrayList<>();

    public SwipeRefreshLayout.OnRefreshListener mRefreshListener;

    public EndlessOnScrollListener mScrollListener;

    public ActressesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.setLayoutManager(new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL));
        this.setAdapter(new ActressAdapter(actresses));

        mRecyclerView.addOnScrollListener(mScrollListener = new EndlessOnScrollListener(getLayoutManager()) {
            @Override
            public void onLoad(final long loadingTime, final boolean refresh) {
                final int page = currentPage;
                Call<ResponseBody> call = getCall(page + 1);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (loadingTime == latestLoadingTime && (!mRefreshLayout.isRefreshing() || refresh)) {
                            try {
                                List<ActressWrapper> wrappers = HtmlConverter.parseActresses(response.body().string());

                                if (refresh) {
                                    actresses.clear();
                                }
                                actresses.addAll(wrappers);
                                getAdapter().notifyDataSetChanged();

                                currentPage++;
                            } catch (Throwable e) {
                                onFailure(call, e);
                            }
                        }

                        loading = false;
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        loading = false;
                        mRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });

        mRefreshLayout.setOnRefreshListener(mRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mScrollListener.loading = true;
                mScrollListener.reset();
                mScrollListener.onLoad(mScrollListener.latestLoadingTime = System.currentTimeMillis(), true);
            }
        });

        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mRefreshLayout.setRefreshing(true);
                mRefreshListener.onRefresh();
            }
        });
    }

    public Call<ResponseBody> getCall(int page) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://avmo.pw")
                .build();

        Network network = retrofit.create(Network.class);

        return network.getActresses(page);
    }

    public static abstract class EndlessOnScrollListener extends RecyclerView.OnScrollListener {

        public StaggeredGridLayoutManager mLayoutManager;

        public boolean loading = false;

        private int loadThreshold = 5;
        public int currentPage = 0;

        public long latestLoadingTime;

        public EndlessOnScrollListener(StaggeredGridLayoutManager mLayoutManager) {
            this.mLayoutManager = mLayoutManager;
        }

        public void reset() {
            loading = false;
            loadThreshold = 5;
            currentPage = 0;
        }

        public void onLoad(long loadingTime, boolean refresh) {

        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            int visibleItemCount = recyclerView.getChildCount();
            int totalItemCount = mLayoutManager.getItemCount();
            int[] firstVisibleItems = mLayoutManager.findFirstVisibleItemPositions(null);

            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItems[0] + loadThreshold)) {
                onLoad(latestLoadingTime = System.currentTimeMillis(), false);
                loading = true;
            }
        }
    }
}
