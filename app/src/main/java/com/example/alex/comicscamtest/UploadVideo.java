package com.example.alex.comicscamtest;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.MultipartFormDataBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class UploadVideo {
    Context mContext;
    private String url;
    String mVideoStorage;


    public UploadVideo(Context context){
        mContext = context;
    }

    public void videoUpload(String videoStorage){

        Log.i("information", "Загрузка видео началась");
        Log.i("information", "Storage file: " + videoStorage);

        try {
            mVideoStorage = videoStorage;
            ProgressDialog.showProgress(mContext, "Обработка");
            uploadVideoToServer(videoStorage);
        }
        catch (Exception e){
            Toast.makeText(mContext, "Ошибка при загрузке", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadVideoToServer(String pathToVideoFile){
        AsyncHttpPost post = new AsyncHttpPost("http://comixify.ai/comixify/");
        MultipartFormDataBody body = new MultipartFormDataBody();
        body.addFilePart("file", new File(pathToVideoFile));
        post.setBody(body);
        AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback(){
            @Override
            public void onCompleted(Exception ex, AsyncHttpResponse source, String result) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                Log.i("Response»»»»»","Server says: " + result);

                JSONObject obj = null;
                try {
                    obj = new JSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                try {
                    url = obj.getString("comics");
                    url = "http://comixify.ai" + url;

                    Log.i("information","URL»»»»»" + url);

                    ProgressDialog.hideProgress();

                    DownloadComics downloadComics = new DownloadComics(mContext);
                    downloadComics.downloadImg(url);

                    deleteCompressFile();

                } catch (JSONException e) {
                    try {
                        String err = obj.getString("error_code");
                        Log.d("................",err);
                        if(err.equals("too_short_file")) {

                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "Слишком короткое видео", Toast.LENGTH_SHORT).show();
                                    ProgressDialog.hideProgress();
                                    deleteCompressFile();
                                }
                            });
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
    }

    private void deleteCompressFile(){
        File file = new File(mVideoStorage);
        boolean checkDelete = file.delete();
        Log.i("information","Delete Video ? " + checkDelete);
    }
}
