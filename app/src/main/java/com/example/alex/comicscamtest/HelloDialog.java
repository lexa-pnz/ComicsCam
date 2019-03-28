package com.example.alex.comicscamtest;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;




public class HelloDialog extends MainActivity{

    Context mContext;

    public HelloDialog(Context context){
        mContext = context;
    }

    public void dialog(){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage("Комикс создается с помощью нейросети. " +
                "Обработка видео может занять продолжительное время, " +
                "пожалуйста не закрывайте приложение во время обработки. \n\n" +
                "10c видео = 30c обработки")
                .setCancelable(false)

                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = alertDialog.create();
        alert.setTitle("INFO");
        alert.show();
    }
}
