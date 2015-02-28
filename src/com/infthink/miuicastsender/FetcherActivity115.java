package com.infthink.miuicastsender;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Vector;

public class FetcherActivity115 extends Activity {

    class VideoInfo {
        public int width;
        public int height;
        public int definition;
        public String url;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null) {
            final Uri uri = intent.getData();
            Log.d("Netcast", uri.toString());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpClient client = new DefaultHttpClient();
                        HttpGet get = new HttpGet(uri.toString());
                        HttpResponse response = client.execute(get);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                        StringBuilder builder = new StringBuilder();
                        for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                            builder.append(s);
                        }
                        JSONObject json = new JSONObject(builder.toString());

                        Vector<VideoInfo> videoInfos = new Vector<VideoInfo>();

                        JSONObject videoObject = json.getJSONObject("video");
                        String videoName = videoObject.getString("file_name");

                        JSONArray videoURLs = videoObject.getJSONArray("video_url");
                        for (int i = 0; i < videoURLs.length(); i++) {
                            JSONObject url = videoURLs.getJSONObject(i);
                            VideoInfo info = new VideoInfo();
                            info.width = url.getInt("width");
                            info.height = url.getInt("height");
                            info.definition = url.getInt("definition");
                            info.url = url.getString("url");
                            videoInfos.add(info);
                        }

                        Collections.sort(videoInfos, new java.util.Comparator<VideoInfo>() {
                            @Override
                            public int compare(VideoInfo lhs, VideoInfo rhs) {
                                return Integer.valueOf(lhs.height).compareTo(Integer.valueOf(rhs.height));
                            }
                        });

                        if (videoInfos.size() <= 0) return;

                        VideoInfo topInfo = videoInfos.get(videoInfos.size() - 1);

                        JSONArray downURLs = json.getJSONObject("video").getJSONArray("down_url");
                        for (int i = 0; i < downURLs.length(); i++) {
                            JSONObject url = downURLs.getJSONObject(i);
                            int definition = url.getInt("definition");
                            if (definition == topInfo.definition) {
                                Log.i("Netcast", url.getString("definition"));
                                Log.i("Netcast", url.getString("url"));

                                Intent intent = new Intent("duokan.intent.action.VIDEO_PLAY");
                                intent.setData(Uri.parse(url.getString("url")));
                                intent.putExtra("mediaTitle", videoName);
                                startActivity(intent);
                            }
                        }
                    } catch (Exception ex) {
                        Log.e("Netcast", "Cannot fetch 115 video data", ex);
                    }

                }
            }).start();
        }

        finish();
    }

}