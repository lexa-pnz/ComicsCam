package com.example.alex.comicscamtest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

public class Permission {

    Context mContext;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;


    public Permission (Context context){
        mContext = context;
    }

    public void permissionCheck(){
        if (Build.VERSION.SDK_INT >= 23) {

            int readPermission = ActivityCompat.checkSelfPermission(mContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ActivityCompat.checkSelfPermission(mContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions((Activity) mContext,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_ID_READ_WRITE_PERMISSION
                );
            }
        }
    }


    public void onRequestPermResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case REQUEST_ID_READ_WRITE_PERMISSION: {

                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    Log.i("information", "Permission granted!");

                }
                else {
                    Log.i("information", "Permission denied!");

                    PermissionDialog permissionDialog = new PermissionDialog(mContext);
                    permissionDialog.dialog();
                }
                break;
            }
        }
    }
}
