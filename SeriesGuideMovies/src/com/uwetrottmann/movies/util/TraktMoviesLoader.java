
package com.uwetrottmann.movies.util;

import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.movies.ui.MovieDetailsFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraktMoviesLoader extends AsyncTaskLoader<List<Movie>> {

    public interface InitBundle {
        String CATEGORY = "category";
    }

    private List<Movie> mData;

    private Bundle mArgs;

    public TraktMoviesLoader(Context context, Bundle args) {
        super(context);
        mArgs = args;
    }

    @Override
    public List<Movie> loadInBackground() {
        TraktCategory category = TraktCategory.fromValue(mArgs.getInt(InitBundle.CATEGORY));
        try {
            ServiceManager serviceManager = Utils.getServiceManager(getContext());

            switch (category) {
                case TRENDING: {
                    return serviceManager.movieService().trending().fire();
                }
                case SUMMARY: {
                    // will just return a single item
                    Movie movie = serviceManager.movieService()
                            .summary(mArgs.getString(MovieDetailsFragment.InitBundle.IMDBID))
                            .fire();
                    ArrayList<Movie> list = new ArrayList<Movie>();
                    list.add(movie);
                    return list;
                }
            }
        } catch (TraktException te) {
            return null;
        } catch (ApiException ae) {
            return null;
        }

        return null;
    }

    /**
     * Called when there is new data to deliver to the client. The super class
     * will take care of delivering it; the implementation here just adds a
     * little more logic.
     */
    @Override
    public void deliverResult(List<Movie> data) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We
            // don't need the result.
            if (data != null) {
                onReleaseResources(data);
            }
        }
        List<Movie> oldData = data;
        mData = data;

        if (isStarted()) {
            // If the Loader is currently started, we can immediately
            // deliver its results.
            super.deliverResult(data);
        }

        if (oldData != null) {
            onReleaseResources(oldData);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            deliverResult(mData);
        } else {
            forceLoad();
        }
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    /**
     * Handles a request to cancel a load.
     */
    @Override
    public void onCanceled(List<Movie> data) {
        super.onCanceled(data);

        onReleaseResources(data);
    }

    /**
     * Handles a request to completely reset the Loader.
     */
    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        // At this point we can release resources
        if (mData != null) {
            onReleaseResources(mData);
            mData = null;
        }
    }

    /**
     * Helper function to take care of releasing resources associated with an
     * actively loaded data set.
     */
    protected void onReleaseResources(List<Movie> data) {
        // For a simple List<> there is nothing to do. For something
        // like a Cursor, we would close it here.
    }

    public enum TraktCategory {
        TRENDING(0), SUMMARY(1);

        private final int index;

        private TraktCategory(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        private static final Map<Integer, TraktCategory> INT_MAPPING = new HashMap<Integer, TraktCategory>();

        static {
            for (TraktCategory via : TraktCategory.values()) {
                INT_MAPPING.put(via.index(), via);
            }
        }

        public static TraktCategory fromValue(int value) {
            return INT_MAPPING.get(value);
        }
    }
}