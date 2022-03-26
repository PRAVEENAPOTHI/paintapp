package com.example.paintapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.kyanogen.signatureview.SignatureView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    int defaultColor;
    SignatureView signatureView;
    ImageButton imgEraser, imgColor, imgSave;
    SeekBar seekBar;
    TextView txtPenSize;

    private static String fileName;

    String name = "My Paintings";
    File file;

    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // checking if the app has permission to write to the external storage
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            // if yes, creating a directory
            createDirectory();
        }
        else{
            // else, asking the user for the permission
            askPermission();
        }

        signatureView = findViewById(R.id.signature_view);
        seekBar = findViewById(R.id.penSize);
        txtPenSize = findViewById(R.id.txtPenSize);
        imgColor = findViewById(R.id.btnColor);
        imgEraser = findViewById(R.id.btnEraser);
        imgSave = findViewById(R.id.btnSave);

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String date = format.format(new Date());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "My Paintings");
        }
        else{
            file = new File(Environment.getExternalStorageDirectory(), name);
        }
        fileName = file + "/" + date + ".png";

        defaultColor = ContextCompat.getColor(MainActivity.this,R.color.black);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                txtPenSize.setText(i + "dp");
                signatureView.setPenSize(i);
                seekBar.setMax(50);

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        imgColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openColorPicker();
            }
        });

        imgEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signatureView.clearCanvas();
            }
        });

        imgSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(! signatureView.isBitmapEmpty()){
                    try {
                        saveImage();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,"Couldn't Save!",Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this,"Draw something",Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

    private void saveImage() throws IOException {
        File file = new File(fileName);
        Bitmap bitmap = signatureView.getSignatureBitmap();

        byte[] bitmapData;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
            bitmapData = bos.toByteArray();
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bitmapData);
        fos.flush();

        fos.close();

        Toast.makeText(this,"Painting Saved!",Toast.LENGTH_SHORT).show();
    }

    private void openColorPicker() {
        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(this, defaultColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {

                defaultColor = color;
                signatureView.setPenColor(color);
            }
        });
        ambilWarnaDialog.show();
    }

    // method to ask the permissions that are needed for the application to perform
    private void askPermission(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE
        );
    }

    // method which returns the result for our askPermission() method
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                createDirectory();
            }
            else{
                Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // method to create a separate directory for the app
    private void createDirectory() {
        // creating a directory for the Paint app
        // let the name be My Paintings
        folderNameDetermination();

        if(! file.exists()){
            // creating a folder if the folder does not already exist
            if(file.mkdir()){
                Toast.makeText(this, "Folder created", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Folder not created", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Folder already exists", Toast.LENGTH_SHORT).show();
        }
    }

    // determining the folder destination for the application
    // for android 11 and above devices, creating a folder in the Documents folder
    // else, creating a folder in the outer main region
    private void folderNameDetermination(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "My Paintings");
        }
        else{
            file = new File(Environment.getExternalStorageDirectory(), name);
        }
    }
}