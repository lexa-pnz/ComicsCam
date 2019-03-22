package com.example.alex.comicscamtest;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


class VideoCompressAsyncTask extends AsyncTask<String, String, String> {

    Context mContext;



    public VideoCompressAsyncTask(Context context){
        mContext = context;
    }

    //Вывод тоста о начале
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ProgressDialog.showProgress(mContext, "Сжатие видео");
            //Toast.makeText(mContext, "Компрессия", Toast.LENGTH_LONG).show();
        }

    //Работа в фоновом режиме (Компрессия)
    @Override
    protected String doInBackground(String... paths){

        String filePath = null;
        try {

            filePath = SiliCompressor.with(mContext).compressVideo(paths[0], paths[1]);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return  filePath;

    }

    //Расчет размера файла и вывод инофрмции
        @Override
        protected void onPostExecute(String compressedFilePath) {
            super.onPostExecute(compressedFilePath);

            File imageFile = new File(compressedFilePath);
            float length = imageFile.length() / 1024f; // Size in KB
            String value;
            if(length >= 1024)
                value = length/1024f+" MB";
            else
                value = length+" KB";

            String text = String.format(Locale.US, "%s\nName: %s\nSize: %s", "Успешное сжатие", imageFile.getName(), value);


            //Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
            Log.i("Silicompressor", "Path: "+compressedFilePath);
            Log.i("Silicompressor", text);
            Log.i("information", text);

            ProgressDialog.hideProgress();

            UploadVideo uploadVideo = new UploadVideo(mContext);
            uploadVideo.videoUpload(compressedFilePath);
        }
}