package com.example.alex.comicscamtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;
    private Uri uri;
    private File f = null;
    String selectedImagePath;

    private static final String TAG = "MyINFO";
    private static final String TAG2 = "Compress";

    private static final String baseURL = "http://comixify.ai/comixify/"; //Путь ?
    private String pathToStoredVideo;

    ImageButton btnVideo, btnGallery;
    VideoView videoView;
    TextView textView;
    Button btn_load, btnCompr;

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoView = (VideoView) findViewById(R.id.videoView);
        textView = (TextView) findViewById(R.id.textView);
        btn_load = (Button) findViewById(R.id.btnPush);
        btnCompr = (Button) findViewById(R.id.btnCompr);
        btnGallery = (ImageButton) findViewById(R.id.btnGallery);
        btnVideo = (ImageButton) findViewById(R.id.btnVideo);

        //Permission permission = new Permission(MainActivity.this);
        //permission.permissionCheck();

        permissionCheck();
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

                dispatchTakeVideoIntent();
                permissionCheck();
                break;

            case R.id.btnGallery:

                Intent videoPickerIntent = new Intent(Intent.ACTION_PICK);
                videoPickerIntent.setType("video/*");
                startActivityForResult(videoPickerIntent, Pick_image);
                break;

            case R.id.btnPush:

                RealPathFromURI realPathFromURI = new RealPathFromURI(MainActivity.this);
                selectedImagePath = realPathFromURI.getRealPathFromURI(uri);
                Log.d(TAG, "Recorded Video Path: " + selectedImagePath);

                try {
                    uploadVideoToServer(selectedImagePath);
                }
                catch (Exception e){
                    Toast.makeText(this, "Ошибка при загрузке", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.btnCompr:

                // Директория сохранения сжатого файла
                File direct = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/ComicsCam");
                if (!direct.exists()) direct.mkdirs();


                //Сжатие файла
                //Видео из instagram обрабатываются некорректно.
                //( Меняют ориентацию экрана )
                new VideoCompressAsyncTask(this).execute(selectedImagePath, direct.getPath());
                break;
        }
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

            // Запуск ImageView
            uri = intent.getData();
            videoView.setVideoURI(uri);
            videoView.requestFocus();
            videoView.start();
        }
        catch (Exception e){
            Toast toast = Toast.makeText(MainActivity.this, "Повторите выбор камеры/файла", Toast.LENGTH_SHORT);
            toast.show();
        }
    }



    //Загрузка на сервер
    private void uploadVideoToServer(String pathToVideoFile){

        File videoFile = new File(pathToVideoFile);
        RequestBody videoBody = RequestBody.create(MediaType.parse("video/*"), videoFile);
        MultipartBody.Part vFile = MultipartBody.Part.createFormData("video", videoFile.getName(), videoBody);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        VideoInterface vInterface = retrofit.create(VideoInterface.class);
        Call<ResultObject>  serverCom = vInterface.uploadVideoToServer(vFile); //ОШИБКА at $Proxy0.uploadVideoToServer(Unknown Source)

        serverCom.enqueue(new Callback<ResultObject>() {
            @Override
            public void onResponse(Call<ResultObject> call, Response<ResultObject> response) {

                try {
                    ResultObject result = response.body();
                    if(!TextUtils.isEmpty(result.getComic())){
                        Toast.makeText(MainActivity.this, "Comic " + result.getComic(), Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Comic " + result.getComic());
                    }
                    else if (!TextUtils.isEmpty(result.getStatus())){
                        Toast.makeText(MainActivity.this, "Status " + result.getStatus(), Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Status " + result.getStatus());
                    }
                }
                catch (Exception e){
                    Toast.makeText(MainActivity.this, "Нет данных", Toast.LENGTH_SHORT).show();
                }

            }
            @Override
            public void onFailure(Call<ResultObject> call, Throwable t) {
                Log.d(TAG, "Error message " + t.getMessage());
            }
        });
    }
}