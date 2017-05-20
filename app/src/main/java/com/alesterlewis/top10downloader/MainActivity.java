package com.alesterlewis.top10downloader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView listApps;
    private String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private String feedCachedUrl = "INVALIDATED";
    private static final String CURRENT_URL = "current url";
    private static final String LIMIT = "limit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listApps = (ListView) findViewById(R.id.xmlListView);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        feedUrl = (String) savedInstanceState.get(CURRENT_URL);
        feedLimit = (Integer) savedInstanceState.get(LIMIT);
        Log.d(TAG, "onRestoreInstanceState: called with feedUrl being " + feedUrl + " and feedLimit being "+feedLimit);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(CURRENT_URL, feedUrl);
        outState.putInt(LIMIT,feedLimit);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: is getting a feedUrl of "+feedUrl+ " and a feedLimit of "+ feedLimit);
        downloadUrl(String.format(feedUrl, feedLimit));
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);
        if(feedLimit == 10){
            menu.findItem(R.id.mnu10).setChecked(true);
        }
        else{
            menu.findItem(R.id.mnu25).setChecked(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id){
            case R.id.mnuFree:
                feedUrl ="http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                String.format(feedUrl, feedLimit);
                break;
            case R.id.mnuPaid:
                feedUrl= "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                String.format(feedUrl, feedLimit);
                break;
            case R.id.mnuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                String.format(feedUrl, feedLimit);
                break;
            case R.id.mnu10:
            case R.id.mnu25:
                if(!item.isChecked()){
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle()+ "setting feedLimit to "+ feedLimit);
                }else{
                    Log.d(TAG, "onOptionsItemSelected: " + item.getTitle()+ "feedLimit unchanged");
                }
                break;
            case R.id.mnuRefresh:
                feedCachedUrl="INVALIDATED";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        downloadUrl(String.format(feedUrl, feedLimit));
        return true;
    }

    private void downloadUrl(String url){
        if(!feedUrl.equalsIgnoreCase(feedCachedUrl)){
            Log.d(TAG, "downloadUrl: Starting AsyncTask");
            DowloadData downloadData = new DowloadData();
            downloadData.execute(url);
            feedCachedUrl = feedUrl;
            Log.d(TAG, "downloadUrl: done");
        }else{
            Log.d(TAG, "downloadUrl: url not changed");
        }
    }

    private class DowloadData extends AsyncTask<String, Void, String> {
        private static final String TAG = "DownloadData";

        @Override
        protected String doInBackground(String... params) {
            Log.d(TAG, "doInBackground: parameter is " + params[0]);
            String rssFeed = downloadXML(params[0]);
            if (rssFeed == null) {
                Log.e(TAG, "doInBackground: Error downloading");
            }
            return rssFeed;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
//            Log.d(TAG, "onPostExecute: The result is " + s);
            ParseApplications parseApplications = new ParseApplications();
            parseApplications.parse(s);

//            ArrayAdapter<FeedEntry> arrayAdapter = new ArrayAdapter<FeedEntry>(
//                    MainActivity.this, R.layout.list_item , parseApplications.getApplications());
//            listApps.setAdapter(arrayAdapter);

            FeedAdapter feedAdapter = new FeedAdapter(MainActivity.this, R.layout.list_record,
                    parseApplications.getApplications());
            listApps.setAdapter(feedAdapter);
        }

        private String downloadXML(String urlPath) {
            StringBuilder xmlResult = new StringBuilder(); //String Builder provides and efficient way to append to a string

            try {
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadXML: The response code was" + response);
//                InputStream inputStream = connection.getInputStream();
//                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
//                BufferedReader reader = new BufferedReader(inputStreamReader);
                // equivalent to the three lines above
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                int charsRead;
                char[] inputBuffer = new char[500];
                while (true) {
                    //  read method returns an int >0 <0 or =0
                    // read method also reads characters in the char buffer given
                    charsRead = reader.read(inputBuffer);
                    if (charsRead < 0) {
                        break;
                    }
                    if (charsRead > 0) {
                        xmlResult.append(String.copyValueOf(inputBuffer, 0, charsRead));
                    }
                }
                reader.close();
                return xmlResult.toString();

            } catch (MalformedURLException e) {//to catch any exceptions that may arise , the website being down for example
                Log.e(TAG, "downloadXML: Invalid Url " + e.getMessage());
            } catch (IOException e) {// to catch  any exceptions that may arise, during I/O, operations e.g. misspelling a website
                Log.e(TAG, "downloadXML: IO Exception reading data: " + e.getMessage());
            } catch (SecurityException e) {
                Log.e(TAG, "downloadXML: Security Exception, Needs permission? " + e.getMessage());
//                e.printStackTrace();
            }
            return null;
        }
    }
}
