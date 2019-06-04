package com.example.newsreaderapp;


import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> titles;
    ArrayAdapter arrayAdapter;
    DownloadTask task;
    SQLiteDatabase database;


    public class DownloadTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... urls) {

            URL url;
            HttpURLConnection urlConnection = null;
            String result = "";

            try{

                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection)url.openConnection();

                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while(data!=-1){
                    char ch = (char) data;
                    result+=ch;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int maxArticles = 25;
                if(jsonArray.length()<maxArticles){
                    maxArticles = jsonArray.length();
                }

                for(int i=0; i<maxArticles; i++){

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+ jsonArray.getString(i) + ".json?print=pretty");
                    urlConnection = (HttpURLConnection)url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    data = reader.read();
                    String article = "";
                    while(data!=-1){

                        char ch = (char)data;
                        article+=ch;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(article);

                    String articleId = "";
                    String title = "";
                    String articleUrl = "" ;

                    if(!jsonObject.isNull("id")){
                        articleId = jsonObject.getString("id");
                    }
                    if(!jsonObject.isNull("title")){
                        title = jsonObject.getString("title");
                    }
                    if(!jsonObject.isNull("url")){
                        articleUrl = jsonObject.getString("url");
                    }

                    Log.i("Contents", articleId + " " + title + " " + articleUrl);

                    SQLiteStatement statement = database.compileStatement("INSERT INTO articleData(articleId, title, url) VALUES (?, ?, ?)");
                    statement.bindString(1, articleId);
                    statement.bindString(2, title);
                    statement.bindString(3, articleUrl);
                    statement.execute();
                }

            }
            catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();

            //Log.i("Complete", "Task is completed");
        }
    }


    public void updateListView()
    {
        Cursor cursor = database.rawQuery("SELECT * FROM articleData", null);
        //int idIndex = cursor.getColumnIndex("articleId");
        int titleIndex = cursor.getColumnIndex("title");
        //int urlIndex = cursor.getColumnIndex("url");

        if(cursor.moveToFirst()){
            titles.clear();
            do{
                titles.add(cursor.getString(titleIndex));
            }while(cursor.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = (ListView)findViewById(R.id.listView);
        titles = new ArrayList<String>();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        database = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        database.execSQL("CREATE TABLE IF NOT EXISTS articleData (articleId INTEGER PRIMARY KEY, title VARCHAR, url VARCHAR)");

        updateListView();

        task = new DownloadTask();

        //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String sql = "SELECT url FROM articleData WHERE title = '" + titles.get(position) + "'";

                Cursor c = database.rawQuery(sql, null);

                int urlIndex = c.getColumnIndex("url");

                String url = "";

                if(c.moveToFirst()) {
                     url = c.getString(urlIndex);
                }

                Log.i("URL", url);

                Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);

                intent.putExtra("url", url);

                startActivity(intent);
            }
        });
    }
}
