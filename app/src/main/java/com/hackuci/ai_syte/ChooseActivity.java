package com.hackuci.ai_syte;

import android.app.ActionBar;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class ChooseActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    // define the variable that will show the gallery images
    ImageView imageViewGallery;
    View decorView;
    Spinner language;
    ImageView logo;
    private String[] choices = new String[]{"English", "Spanish", "German", "Japanese"};

    public static String LANGUAGE_CHOICE = "English"; //default
    public static Bitmap theChosenOne;

    private boolean isGalleryOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        decorView = getWindow().getDecorView();

        /* HIDES STATUS BAR */
        ActionBar actionBar;
        if((actionBar = getActionBar()) != null)
            actionBar.hide();
        /* HIDES STATUS BAR */

        // connect the variable to the images_proj.xml
        imageViewGallery = findViewById(R.id.gallery);
        logo = findViewById(R.id.logo);
        logo.setBackgroundResource(R.drawable.smaller_round_layout);


        language = findViewById(R.id.language);
        language.setOnItemSelectedListener(this);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        language.setAdapter(dataAdapter);

        language.setBackgroundResource(R.drawable.smaller_round_layout);
        language.setPopupBackgroundResource(R.drawable.round_layout);

        ImageButton cameraButton = findViewById(R.id.camera);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                startActivity(new Intent(ChooseActivity.this,
                        CameraActivity.class));
            }
        });

    }

    // This function is called after clicking the Browse From Gallery button
    public boolean onButtonClicked(View view) {
        isGalleryOpen = true;
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 2);
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // HIDES NOTIFICATION BAR
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        // HIDES NOTIFICATION BAR

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            File f = new File(Environment.getExternalStorageDirectory().toString());
            Toast.makeText(getApplication().getApplicationContext(), " not null ", Toast.LENGTH_LONG).show();
            try {
                Bitmap bitmap;
                BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();

                bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(),
                        bitmapOptions);

                theChosenOne = bitmap;

                String path = android.os.Environment
                        .getExternalStorageDirectory()+File.separator;
                f.delete();
                OutputStream outFile;
                File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");
                try {
                    outFile = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
                    outFile.flush();
                    outFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == 2) {

            Uri selectedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            assert selectedImage != null;
            Cursor c = getApplication().getContentResolver().query(selectedImage, filePath, null, null, null);
            assert c != null;
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String picturePath = c.getString(columnIndex);
            if(picturePath.startsWith("/")) picturePath = picturePath.substring(1);
            c.close();
            theChosenOne = (BitmapFactory.decodeFile(picturePath));

            //switches to camera
            startActivity(new Intent(ChooseActivity.this, CameraActivity.class));
            CameraActivity.fromGallery = true;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        LANGUAGE_CHOICE = (String) adapterView.getItemAtPosition(i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public boolean onMoreButtonClicked(View view) {
        startActivity(new Intent(ChooseActivity.this, MoreActivity.class));
        return true;
    }

    @Override
    public void onBackPressed(){
        if(isGalleryOpen){
            startActivity(new Intent(ChooseActivity.this, ChooseActivity.class));
            isGalleryOpen = false;
        }
    }

}
