package com.example.bened.androidnews;

import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity implements LoaderCallbacks<List<News>> {

    /**
     * URL for news data from the GUARDIAN data set
     */
    private static final String GUARDIAN_REQUEST_URL =
            "https://content.guardianapis.com/search?";
    /**
     * Constant value for the news loader ID. We can choose any integer.
     * This really only comes into play if you're using multiple loaders.
     */
    private static final int NEWS_LOADER_ID = 1;

    /**
     * Adapter for the list of news
     */
    private NewsAdapter mAdapter;

    /**
     * TextView that is displayed when the list is empty
     */
    private TextView mEmptyStateTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news_activity);

        //Find the ListView in the layout
        ListView newsListView = findViewById(R.id.list);

        //Display an empty view in case there is no data
        mEmptyStateTextView = findViewById(R.id.empty_view);
        newsListView.setEmptyView(mEmptyStateTextView);

        // Create a new adapter that takes an empty list of news as input
        mAdapter = new NewsAdapter(this, new ArrayList<News>());

        // Set the adapter on the {@link ListView}
        // so the list can be populated in the user interface
        newsListView.setAdapter(mAdapter);

        // Set an item click listener on the ListView, which sends an intent to a web browser
        // to open a website with more information about the selected news.
        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // Find the current earthquake that was clicked on
                News currentNews = mAdapter.getItem(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri newsUri = null;
                if (currentNews != null) {
                    newsUri = Uri.parse(currentNews.getWebSiteUrl());
                }

                // Create a new intent to view the news URI
                Intent webIntent = new Intent(Intent.ACTION_VIEW, newsUri);

                // Send the intent to launch a new activity
                startActivity(webIntent);
            }
        });

        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        // Get details on the currently active default data network
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }

        // If there is a network connection, fetch data
        if (networkInfo != null && networkInfo.isConnected()) {
            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            loaderManager.initLoader(NEWS_LOADER_ID, null, this);
        } else {
            // Otherwise, display error
            // First, hide loading indicator so error message will be visible
            View loadingIndicator = findViewById(R.id.loading_spinner);
            loadingIndicator.setVisibility(View.GONE);

            // Update empty state with no connection error message
            mEmptyStateTextView.setText(R.string.no_network);
        }
    }


    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param i          The ID whose loader is to be created.
     * @param preference Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<List<News>> onCreateLoader(int i, final Bundle preference) {

        /*initialize the associated SharedPreferences file with default values
        for each Preference when the user first opens your app.*/
        PreferenceManager.setDefaultValues(this, R.xml.settings_pref, false);

       /* Gets a {@link SharedPreferences} instance that points to the default file that is used by
        the preference framework in the given context that can be used to retrieve and listen
        to values of the preferences.*/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /*getString retrieves a String value from the preferences.
        The second parameter is the default value for this preference.*/
        String orderByDefault = prefs.getString(
                getString(R.string.settings_order_by_key), getString(R.string.settings_order_by_default));

        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(GUARDIAN_REQUEST_URL);

        //buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        final Uri.Builder uriBuilder = baseUri.buildUpon();

        // Append query parameter and its value
        uriBuilder.appendQueryParameter("q", "android");
        uriBuilder.appendQueryParameter("page-size", "10");
        uriBuilder.appendQueryParameter("pages", "1");
        uriBuilder.appendQueryParameter("format", "json");
        uriBuilder.appendQueryParameter("show-tags", "contributor");
        uriBuilder.appendQueryParameter("show-fields", "headline,thumbnail");
        uriBuilder.appendQueryParameter("from-date", "2018-05-01");
        uriBuilder.appendQueryParameter("api-key", "f1de4252-13ef-41bc-ac5a-7604cccf6a7a");
        uriBuilder.appendQueryParameter("order-by", orderByDefault);

        // Return the completed uri
        return new NewsLoader(this, uriBuilder.toString());
    }

    /**
     * Called when a previously created loader has finished its load.
     * <p>
     * This function is guaranteed to be called prior to the release of
     * the last data that was supplied for this Loader.  At this point
     * you should remove all use of the old data (since it will be released
     * soon), but should not do your own release of the data since its Loader
     * owns it and will take care of that.  The Loader will take care of
     * management of its data so you don't have to.
     * <p>
     * The Loader will monitor for changes to the data, and report
     * them to you through new calls here.  You should not monitor the
     * data yourself.
     * The Loader will release the data once it knows the application
     * is no longer using it.
     * Called when a previously created loader has finished its load.*
     *
     * @param loader The Loader that has finished.
     * @param news   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> news) {
        // Set empty state text to display "No earthquakes found."
        mEmptyStateTextView.setText(R.string.no_data);

        // Clear the adapter of previous earthquake data
        mAdapter.clear();

        /*If there is a valid list of {@link News} news, then add them to the adapter's
        data set. This will trigger the ListView to update.*/
        if (news != null && !news.isEmpty()) {
            mAdapter.addAll(news);
        }

        // Hide loading indicator because the data has been loaded
        View loadingIndicator = findViewById(R.id.loading_spinner);
        loadingIndicator.setVisibility(View.GONE);
    }


    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        // Loader reset, so we can clear out our existing data.
        mAdapter.clear();
    }

    /**
     * Inflates the menu
     *
     * @param menu Menu to inflate
     * @return Returns true if the menu is inflated.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    /*This method is where we can setup the specific action that occurs when
    any of the items in the Options Menu are selected
    This method is called whenever an item in the options menu is selected.*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
