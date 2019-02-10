package com.example.alex.comicscamtest;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class RealPathFromURI {

    Context mContext;

    public RealPathFromURI (Context context){
        mContext = context;
    }

    public String getRealPathFromURI(Uri uri) {
        if (uri == null) return null;
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            String result = cursor.getString(column_index);
            cursor.close();
            return result;
        }
        return uri.getPath();
    }
}
