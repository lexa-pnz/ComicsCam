package com.example.alex.comicscamtest;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.MultipartFormDataBody;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import org.json.*;

public class MainActivity extends AppCompatActivity{

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;
    private Uri uri;
    String selectedImagePath = null;

    private static final String TAG = "UpVideo";
    private static final String TAG2 = "Compress";

    static private ProgressDialog progressDialog = null;

    public SharedPreferences sPref;
    private boolean checkFirstPlay = false;

    ImageButton btnVideo, btnGallery;
    VideoView videoView;
    Button btnUpload;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidNetworking.initialize(getApplicationContext());

        loadInfoInFirstPlay();

        videoView = (VideoView) findViewById(R.id.videoView);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnGallery = (ImageButton) findViewById(R.id.btnGallery);
        btnVideo = (ImageButton) findViewById(R.id.btnVideo);

        //Полноэкранный режим
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Убрать ActionBar
        //Objects.requireNonNull(getSupportActionBar()).hide();
        setTitle("ComicsCam");

        permissionCheck();

        if (!checkFirstPlay)
            HelloDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInfoInFirstPlay();
    }

    public void permissionCheck(){
        if (android.os.Build.VERSION.SDK_INT >= 23) {

            int readPermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            int writePermission = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (writePermission != PackageManager.PERMISSION_GRANTED ||
                    readPermission != PackageManager.PERMISSION_GRANTED) {
                // If don't have permission so prompt the user.
                this.requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_ID_READ_WRITE_PERMISSION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ID_READ_WRITE_PERMISSION: {

                // Note: If request is cancelled, the result arrays are empty.
                // Permissions granted (read/write).
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();

                }
                // Cancelled or denied.
                else {
                    Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    public void buttonClick(View view) {
        switch (view.getId()) {

            case R.id.btnVideo:
                btnVideo.setImageResource(R.drawable.camera_icon2);

                dispatchTakeVideoIntent();
                permissionCheck();
                break;

            case R.id.btnGallery:
                btnGallery.setImageResource(R.drawable.gallery_icon2);

                Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
                videoPickerIntent.setType("video/*");
                startActivityForResult(videoPickerIntent, Pick_image);
                break;

            case R.id.btnUpload:

                boolean checkInternet = isOnline();

                if (selectedImagePath == null)
                    Toast.makeText(this, "Выберите видео", Toast.LENGTH_SHORT).show();
                else if (!checkInternet)
                    Toast.makeText(this, "Необходим интернет", Toast.LENGTH_SHORT).show();
                else
                    videoCompess();
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void videoCompess (){
        Log.i("information", "Сжатие видео началось");

        // Директория сохранения сжатого файла
        File direct = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/ComicsCam");
        if (!direct.exists()) direct.mkdirs();

        //Сжатие файла
        //Видео из instagram обрабатываются некорректно.
        //( Меняют ориентацию экрана )

        new VideoCompressAsyncTask(this).execute(selectedImagePath, direct.getPath());
    }


    // Не работает в стандартной камере LinageOS (есть звук, нет видео)
    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10); //Лимит (В новой камере не работает)
            takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); //Качество (В новой камере не работает)
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        try {
            RealPathFromURI realPathFromURI = new RealPathFromURI(MainActivity.this);

            //Получение пути к файлу из URI (Добавить низкие API)
            Uri selectedImageUri = intent.getData();
            selectedImagePath = realPathFromURI.getRealPathFromURI(selectedImageUri);
            Log.d(TAG2, "Путь файла " + selectedImagePath.toString());

            // Запуск VideoView
            uri = intent.getData();
            videoView.setVideoURI(uri);
            videoView.requestFocus();
            videoView.start();
        }
        catch (Exception e){
            Toast toast = Toast.makeText(MainActivity.this, "Повторите выбор камеры/файла", Toast.LENGTH_SHORT);
            toast.show();
        }

        btnGallery.setImageResource(R.drawable.gallery_icon);
        btnVideo.setImageResource(R.drawable.camera_icon);
    }

    static protected void showProgress(Context context, String text) {

        if (progressDialog == null) {
            try {
                progressDialog = ProgressDialog.show(context, "", text);
                progressDialog.setCancelable(false);
            } catch (Exception e) {

            }

        }

    }

    static public void hideProgress() {

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    private void HelloDialog(){
        checkFirstPlay = true;
        saveInfoInFirstPlay();

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
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

    private void saveInfoInFirstPlay() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putBoolean("FirstPlay", checkFirstPlay);
        editor.apply();
    }

    private void loadInfoInFirstPlay() {
        sPref = getPreferences(MODE_PRIVATE);
        checkFirstPlay = sPref.getBoolean("FirstPlay", false);
    }

}