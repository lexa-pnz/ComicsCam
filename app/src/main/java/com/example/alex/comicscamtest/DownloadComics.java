package com.example.alex.comicscamtest;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DownloadComics {
    Context mContext;
    private String strCounterImg;
    private String fileName, dirPath;


    public DownloadComics(Context context){
        mContext = context;
    }

    public void downloadImg(String url){
        ProgressDialog.showProgress(mContext, "Загрузка");

        DateFormat();
        fileName = "Comics" + strCounterImg + ".jpeg";
        dirPath = Environment.getExternalStorageDirectory() + "/ComicsImage";

        Log.i("information", "dirPath = " + dirPath);

        AndroidNetworking.download(url, dirPath, fileName)
                .build()
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        ProgressDialog.hideProgress();

                        Log.i("information", "DownLoad Complete");
                        //Toast.makeText(mContext, "DownLoad Complete", Toast.LENGTH_SHORT).show();
                        intImg(dirPath, fileName);
                    }

                    @Override
                    public void onError(ANError anError) {
                        Log.i("information", "Ошибка при скачивании комикса");
                    }
                });
    }



    private void intImg (String dirPath, String fileName){
        Intent intent = new Intent(mContext, ActivityImg.class);
        intent.putExtra("dirPath", dirPath);
        intent.putExtra("fileName", fileName);
        mContext.startActivity(intent);
    }


    private void DateFormat(){
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("_yyMMdd_HHmmss");
        strCounterImg = formatForDateNow.format(dateNow);
    }
}
