package com.example.danboorubutbetter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public ArrayList<String> imageUrls;
    public ArrayList<String> tags;
    public RecyclerView recyclerView;
    public ArrayAdapter<String> adapter;
    public ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tags= new ArrayList<>();
        imageUrls= new ArrayList<>();
        listView = findViewById(R.id.list_view);
        adapter= new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tags);
        listView.setAdapter(adapter);


    }


    //Search View
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final String[] prevSearch = new String[1];
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem menuItem = menu.findItem(R.id.search_view);
       SearchView searchView= (SearchView) menuItem.getActionView();
       listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
           @Override
           public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tag= tags.get(position);
                String currentSearch= searchView.getQuery().toString();
                if(!currentSearch.contains(";")){
                    prevSearch[0] = tag;
                    searchView.setQuery(tag+";", false);
                    tags.clear();
                }
                else{
                    prevSearch[0] = currentSearch.substring(0, currentSearch.lastIndexOf(";"));
                    searchView.setQuery(prevSearch[0] +";"+tag, true);
                    listView.setVisibility(View.GONE);
                    tags.clear();
                }

           }
       });
       searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
           @Override
           public boolean onQueryTextSubmit(String query) {
               tags.clear();
               return false;
           }

           @Override
           public boolean onQueryTextChange(String newText) {
               //timertask search with 500ms delay
                Timer timer= new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        fetchTags(newText);
                    }
                }, 500);
               return false;
           }
       });
        return super.onCreateOptionsMenu(menu);
    }

    //fetch Tags from danbooru
    public void fetchTags(String changedText) {
        String query = changedText;
        if (changedText.contains(";")){
            if (changedText.split(";").length>1){
                if (changedText.split(";")[1].length()>3){
                    query = changedText.split(";")[1];
                }else {
                    query = changedText.split(";")[0];
                }
            }
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new okhttp3.Request.Builder()
                .url("https://danbooru.donmai.us/wiki_pages.json?commit=Search&search%5Border%5D=post_count&limit=100&search%5Btitle_or_body_matches%5D=" + query)
                .build();
        String finalQuery = query;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = Objects.requireNonNull(response.body()).string();
                try {
                    JSONArray jsonArray = new JSONArray(responseData);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String tag = jsonObject.getString("title");
                        tags.add(tag);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setVisibility(ListView.VISIBLE);
                        if (tags.size() > 0) {
                            //remove that doesnot contain the changedText
                            for (int i = 0; i < tags.size(); i++) {
                                if (!tags.get(i).contains(finalQuery)) {
                                    tags.remove(i);
                                }
                            }
                            //remove duplicates
                            for (int i = 0; i < tags.size(); i++) {
                                for (int j = i + 1; j < tags.size(); j++) {
                                    if (tags.get(i).equals(tags.get(j))) {
                                        tags.remove(j);
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
    }

}