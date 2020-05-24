package ru.gmasalskih.l32;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
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
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private String myImageJpg = "myImage.jpg";
    private String myImagePng = "myImage.png";
    private String imageUrl = "https://cloud.githubusercontent.com/assets/5489943/14237225/ef4e3666-f9ee-11e5-886e-9e15b1f1b09d.png";
    private ImageView ivImage;
    private TextView tvLog;
    private Button btnConvert;
    private AlertDialog alertDialog;
    private List<Disposable> disposableList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivImage = findViewById(R.id.iv_image);
        tvLog = findViewById(R.id.tv_log);
        btnConvert = findViewById(R.id.btn_load_from_disk);
    }

    private void disposeJob() {
        for (Disposable d : disposableList) d.dispose();
        disposableList.clear();
    }

    private void updateUI(Bitmap bitmap, String msg, boolean isBtnActive) {
        tvLog.setText(msg);
        ivImage.setImageBitmap(bitmap);
        btnConvert.setEnabled(isBtnActive);
    }

    private Bitmap openFIle(String imageUrl) {
        Bitmap bitmap = null;
        try {
            FileInputStream fiStream = getApplicationContext().openFileInput(imageUrl);
            bitmap = BitmapFactory.decodeStream(fiStream);
            fiStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Something went wrong!");
            e.printStackTrace();
        }
        return bitmap;
    }

    private void createAlertDialog() {
        if (alertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Cancel Job");
            builder.setMessage("Do you want cancel Job?");
            builder.setNegativeButton("No", null);
            builder.setPositiveButton("Yes", (dialog, which) -> disposeJob());
            alertDialog = builder.create();
        }
        alertDialog.show();
    }

    private void convertAndSaveFile(Bitmap bitmap, Bitmap.CompressFormat format, String imageUrl) {
        try {
            FileOutputStream foStream = getApplicationContext().openFileOutput(imageUrl, Context.MODE_PRIVATE);
            bitmap.compress(format, 100, foStream);
            foStream.flush();
            foStream.close();
        } catch (Exception e) {
            Log.d("convertAndSaveFile", "Something went wrong!");
            e.printStackTrace();
        }
    }

    public void asyncDownloadSaveImageFromUrl(View view) {
        Single<Bitmap> single = Single.create(emitter -> {
            try {
                InputStream inputStream = new URL(imageUrl).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                emitter.onSuccess(bitmap);
            } catch (Exception e) {
                Log.d("DownloadImage", "Exception 1, Something went wrong!");
                e.printStackTrace();
                emitter.onError(e);
            }
        });
        Disposable disposable = single.subscribeOn(Schedulers.io())
                .doOnSuccess(bitmap -> convertAndSaveFile(bitmap, Bitmap.CompressFormat.PNG, myImagePng))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        bitmap -> updateUI(bitmap, "Load image from url and save it to disk", true),
                        err -> Log.d("err", "Something went wrong!", err)
                );
        disposableList.add(disposable);
    }

    public void loadImageFromDisk(View view) {
        Single<Bitmap> single = Single.create(emitter -> emitter.onSuccess(openFIle(myImagePng)));
        Disposable disposable = single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(bitmap -> createAlertDialog())
                .observeOn(Schedulers.io())
                .delay(3L, TimeUnit.SECONDS)
                .doOnSuccess(bitmap -> convertAndSaveFile(bitmap, Bitmap.CompressFormat.JPEG, myImageJpg))
                .map(b -> openFIle(myImageJpg))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        bitmap -> {
                            updateUI(bitmap, "Load image from disk", false);
                            if (alertDialog.isShowing()) alertDialog.cancel();
                        },
                        err -> Log.d("err", "Something went wrong!", err)
                );
        disposableList.add(disposable);
    }

    public void deleteImageFromDisk(View view) {
        for (String fileName : getApplicationContext().fileList()) {
            getApplicationContext().deleteFile(fileName);
        }
        updateUI(null, "Delete all file",false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disposeJob();
    }
}

