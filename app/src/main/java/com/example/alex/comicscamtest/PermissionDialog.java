package com.example.alex.comicscamtest;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

public class PermissionDialog {
    Context mContext;

    public PermissionDialog(Context context){
        mContext = context;
    }

    public void dialog(){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage("Для работы приложения необходимо разрешение на использование внутреннего хранилища, " +
                "оно используется для сохранения комикса.")

                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();

                        Permission permissions = new Permission(mContext);
                        permissions.permissionCheck();
                    }
                });

        AlertDialog alert = alertDialog.create();
        alert.setTitle("Внимание");
        alert.show();
    }
}
