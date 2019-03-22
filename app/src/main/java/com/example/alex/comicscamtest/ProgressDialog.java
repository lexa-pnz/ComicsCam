package com.example.alex.comicscamtest;

import android.content.Context;
import android.util.Log;

public class ProgressDialog extends MainActivity {

    static private android.app.ProgressDialog progressDialog = null;

    static public void showProgress(Context context, String text) {

        if (progressDialog == null) {
            try {
                progressDialog = android.app.ProgressDialog.show(context, "", text);
                progressDialog.setCancelable(false);
            } catch (Exception e) {
                Log.i("myERR", String.valueOf(e));
            }
        }
    }

    static public void hideProgress() {

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }
}
