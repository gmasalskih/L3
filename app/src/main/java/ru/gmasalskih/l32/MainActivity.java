package ru.gmasalskih.l32;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private String myImageName = "myImage.png";
    private String imageUrl = "https://cloud.githubusercontent.com/assets/5489943/14237225/ef4e3666-f9ee-11e5-886e-9e15b1f1b09d.png";
    private ImageView ivImage;
    private TextView tvLog;
    private Button btnConvert;
    private List<Disposable> disposableList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivImage = findViewById(R.id.iv_image);
        tvLog = findViewById(R.id.tv_log);
        btnConvert = findViewById(R.id.btn_load_from_disk);
    }

    private void log(String logStr) {
        Log.d("ImageDownloadDemo", logStr);
        tvLog.setText(logStr);
    }

    private Bitmap openFIle(String imageUrl) {
        Bitmap bitmap = null;
        try {
            FileInputStream fiStream = getApplicationContext().openFileInput(imageUrl);
            bitmap = BitmapFactory.decodeStream(fiStream);
            fiStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 3, Something went wrong!");
            e.printStackTrace();
        }
        return bitmap;
    }

    private void convertAndSaveFile(Bitmap bitmap, Bitmap.CompressFormat format, String imageUrl) {
        try {
            FileOutputStream foStream = getApplicationContext().openFileOutput(imageUrl, Context.MODE_PRIVATE);
            bitmap.compress(format, 100, foStream);
            foStream.flush();
            foStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 2, Something went wrong!");
            e.printStackTrace();
        }
    }

    public void asyncDownloadSaveImageFromUrl(View view) {
        Observable<Bitmap> observable = Observable.create(emitter -> {
            try {
                InputStream inputStream = new URL(imageUrl).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                emitter.onNext(bitmap);
            } catch (Exception e) {
                Log.d("DownloadImage", "Exception 1, Something went wrong!");
                e.printStackTrace();
                emitter.onError(e);
            }
        });
        Disposable disposable = observable.subscribeOn(Schedulers.io())
                .doOnNext(bitmap -> convertAndSaveFile(bitmap, Bitmap.CompressFormat.PNG, myImageName))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(bitmap -> {
                    log(bitmap.toString() + " Load image from url and save it to disk");
                    ivImage.setImageBitmap(bitmap);
                }).subscribe();
        btnConvert.setEnabled(true);
        disposableList.add(disposable);
    }

    public void loadImageFromDisk(View view) {

        Observable<Bitmap> observable = Observable.create(emitter -> emitter.onNext(openFIle("myImage.png")));
        Disposable disposable = observable.subscribeOn(Schedulers.io())
                .doOnNext(bitmap -> convertAndSaveFile(bitmap, Bitmap.CompressFormat.JPEG, "myImage.jpg"))
                .map(b -> openFIle("myImage.jpg"))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        bitmap -> ivImage.setImageBitmap(bitmap),
                        err -> Log.d("err", "Something went wrong!", err),
                        () -> log("Load image from disk")
                );
        disposableList.add(disposable);
    }

    public void deleteImageFromDisk(View view) {
        for (String fileName : getApplicationContext().fileList()){
            getApplicationContext().deleteFile(fileName);
        }
        log("Delete all file");
        ivImage.setImageBitmap(null);
        btnConvert.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Disposable d : disposableList) {
            d.dispose();
        }
    }
}
