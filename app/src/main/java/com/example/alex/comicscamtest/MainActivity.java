package com.example.alex.comicscamtest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.squareup.picasso.Picasso;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import org.json.*;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
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

    File file;
    String dirPath, fileName;
    int counterImg = 0;

    private static final String TAG = "UpVideo";
    private static final String TAG2 = "Compress";

    private static final String baseURL = "http://comixify.ai"; //Путь ?
    //private static final String baseURL = "http://comixify.ai/comixify/"; //Путь ?
    private String pathToStoredVideo;

    ImageButton btnVideo, btnGallery, btn_load;
    VideoView videoView;
    TextView textView;
    Button btnCompr;

    public MainActivity() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AndroidNetworking.initialize(getApplicationContext());
        dirPath = Environment.getExternalStorageDirectory() + "/ComicsImage";

        videoView = (VideoView) findViewById(R.id.videoView);
        textView = (TextView) findViewById(R.id.textView);
        btn_load = (ImageButton) findViewById(R.id.imgBtnPush);
        btnCompr = (Button) findViewById(R.id.btnCompr);
        btnGallery = (ImageButton) findViewById(R.id.btnGallery);
        btnVideo = (ImageButton) findViewById(R.id.btnVideo);

        //Полноэкранный режим
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Убрать ActionBar
        Objects.requireNonNull(getSupportActionBar()).hide();

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
                    e.printStackTrace();
                }
                Log.i("URL»»»»»", url);
                downloadImg(url);
            }
        });
    }

    private void downloadImg(String url){
        counterImg = counterImg++;
        fileName = "image"+ counterImg + ".jpeg";
        file = new File(dirPath, fileName);

        AndroidNetworking.download(url, dirPath, fileName)
                .build()
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {

                        Toast.makeText(MainActivity.this, "DownLoad Complete", Toast.LENGTH_SHORT).show();


//                        Intent intent = new Intent();
//                        intent.setAction(android.content.Intent.ACTION_VIEW);
//                        Uri uri =Uri.parse("file://" + dirPath + "/" + fileName);
//                        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
//                                MimeTypeMap.getFileExtensionFromUrl(uri.toString()));
//                        intent.setDataAndType(uri, type == null ? "*/*" : type);
//                        startActivity((Intent.createChooser(intent,
//                                "Открыть с помощью")));

//                        File file = ...;
//                        final Intent intent = new Intent(Intent.ACTION_VIEW)//
//                                .setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
//                                                android.support.v4.content.FileProvider.getUriForFile(this,getPackageName() + ".provider", file) : Uri.fromFile(file),
//                                        "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


                    }

                    @Override
                    public void onError(ANError anError) {

                    }
                });
    }
}