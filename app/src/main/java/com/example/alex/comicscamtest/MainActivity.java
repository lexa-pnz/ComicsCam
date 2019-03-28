package com.example.alex.comicscamtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidnetworking.AndroidNetworking;


import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.io.File;

public class MainActivity extends AppCompatActivity{

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private Uri uri;
    String selectedImagePath = null;

    private static final String TAG = "UpVideo";
    private static final String TAG2 = "Compress";

    private SharedPreferences sPref;
    private boolean checkFirstPlay = false;

    ImageButton btnVideo, btnGallery;
    VideoView videoView;
    Button btnUpload;

    Permission permissions = new Permission(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
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

        permissions.permissionCheck();

        HelloDialog helloDialog = new HelloDialog(this);
        if (!checkFirstPlay) {
            helloDialog.dialog();
            checkFirstPlay = true;
            saveInfoInFirstPlay();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInfoInFirstPlay();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permission permission = new Permission(this);
        permission.onRequestPermResult(requestCode, permissions, grantResults);
    }

    public void buttonClick(View view) {
        switch (view.getId()) {

            case R.id.btnVideo:
                btnVideo.setImageResource(R.drawable.camera_icon2);

                dispatchTakeVideoIntent();
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
                break;
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
        //Видео из instagram обрабатываются некорректно. ( Меняют ориентацию экрана )
        new VideoCompressAsyncTask(this).execute(selectedImagePath, direct.getPath());
    }


    // Не работает в стандартной камере LinageOS (есть звук, нет видео)
    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
            takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
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