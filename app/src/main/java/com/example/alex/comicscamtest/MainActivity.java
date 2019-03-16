package com.example.alex.comicscamtest;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_VIDEO_CAPTURE = 1;
    private final int Pick_image = 1;
    private static final int REQUEST_ID_READ_WRITE_PERMISSION = 99;
    private Uri uri;
    private File f = null;
    String selectedImagePath;

    File file;
    String dirPath, fileName;

    //Названия комиксов
    public int counterImg = 0;
    public String strCounterImg = "1";
    public SharedPreferences sPref;

    private static final String TAG = "UpVideo";
    private static final String TAG2 = "Compress";

    private static final String baseURL = "http://comixify.ai"; //Путь ?
    //private static final String baseURL = "http://comixify.ai/comixify/"; //Путь ?
    private String pathToStoredVideo;

    ImageButton btnVideo, btnGallery, btn_load;
    VideoView videoView;
    ImageView imageView;
    TextView textView;
    Button btnCompr;

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //loadCntrImg();

        AndroidNetworking.initialize(getApplicationContext());
        dirPath = Environment.getExternalStorageDirectory() + "/ComicsImage";

        videoView = (VideoView) findViewById(R.id.videoView);
        textView = (TextView) findViewById(R.id.textView);
        btn_load = (ImageButton) findViewById(R.id.imgBtnPush);
        btnCompr = (Button) findViewById(R.id.btnCompr);
        btnGallery = (ImageButton) findViewById(R.id.btnGallery);
        btnVideo = (ImageButton) findViewById(R.id.btnVideo);
        imageView = (ImageView) findViewById(R.id.imageView);

        //Полноэкранный режим
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Убрать ActionBar
        Objects.requireNonNull(getSupportActionBar()).hide();

        //Permission permission = new Permission(MainActivity.this);
        //permission.permissionCheck();

        permissionCheck();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //loadCntrImg();
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

            case R.id.imgBtnPush:

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

    private void uploadVideoToServer(String pathToVideoFile){
        AsyncHttpPost post = new AsyncHttpPost("http://comixify.ai/comixify/");
        MultipartFormDataBody body = new MultipartFormDataBody();
        body.addFilePart("file", new File(pathToVideoFile));
        post.setBody(body);
        AsyncHttpClient.getDefaultInstance().executeString(post, new AsyncHttpClient.StringCallback(){
            @Override
            public void onCompleted(Exception ex, AsyncHttpResponse source, String result) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                Log.i("Response»»»»»","Server says: " + result);

                JSONObject obj = null;
                try {
                    obj = new JSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = "";
                try {
                    url = obj.getString("comics");
                    url = "http://comixify.ai" + url;
                } catch (JSONException e) {
                    try {
                        String err = obj.getString("error_code");
                        Log.d("................",err);
                        if(err.equals("too_short_file")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Слишком короткое видео", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                }
                Log.i("URL»»»»»", url);
                downloadImg(url);
            }
        });
    }

    private void downloadImg(String url){

        //Второй способ названия комикаса (ERROR)
        //counterImg = counterImg++;
        //saveCntrImg();

        DateFormat();
        fileName = "Comics" + strCounterImg + ".jpeg";
        file = new File(dirPath, fileName);

        AndroidNetworking.download(url, dirPath, fileName)
                .build()
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {

                        Toast.makeText(MainActivity.this, "DownLoad Complete", Toast.LENGTH_SHORT).show();
                        intImg(dirPath, fileName);
                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }

    private void intImg (String dirPath, String fileName){
        Intent intent = new Intent(this, ActivityImg.class);
        intent.putExtra("dirPath", dirPath);
        intent.putExtra("fileName", fileName);
        startActivity(intent);
    }


    //Второй способ названия комикаса (ERROR)
    private void loadCntrImg(){
        sPref = getPreferences(MODE_PRIVATE);
        counterImg = sPref.getInt("counterImg", 0);
    }
    private void saveCntrImg(){
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putInt("counterImg", counterImg);
        editor.apply();
    }

    private void DateFormat(){
        Date dateNow = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("yy.MM.dd_HH.mm.ss");

        strCounterImg = formatForDateNow.format(dateNow);
    }
}