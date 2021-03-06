package com.hackuci.ai_syte;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.ibm.watson.developer_cloud.discovery.v1.Discovery;
import com.ibm.watson.developer_cloud.language_translator.v2.LanguageTranslator;
import com.ibm.watson.developer_cloud.language_translator.v2.model.Translation;
import com.ibm.watson.developer_cloud.language_translator.v2.model.TranslationResult;
import com.ibm.watson.developer_cloud.language_translator.v2.util.Language;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.apache.commons.lang3.text.WordUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CameraActivity extends AppCompatActivity {

    private static final int PIXEL_WIDTH = 1080;
    private static final int PIXEL_HEIGHT = 1920;

    private static final int MAX_RESULTS = 30;

    //start transparency values
    float start = 0.4f;
    float end = 1;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final String TAG = "CameraActivity";
    private static String FILE_PATH;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //list information
    List<EntityAnnotation> entity_list = new ArrayList<>();
    List<String> array_list = Arrays.asList(
            "Nothing's here yet!",
            "Try adding a picture or video,",
            "and results will populate here, like this!");

    TextView textView;
    ListView listview;
    ProgressBar progress;
    PopupWindow popUp;

    public static boolean fromGallery = false;

    private SlidingUpPanelLayout mLayout;
    private TextureView textureView;
    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    //Save to FILE
    private File file;
    private Handler mBackgroundHandler;
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
        }
    };
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };
    private HandlerThread mBackgroundThread;

    public CameraActivity() {
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* HIDES STATUS BAR */
        ActionBar actionBar;
        if ((actionBar = getActionBar()) != null)
            actionBar.hide();
        /* HIDES STATUS BAR */

        textureView = findViewById(R.id.textureView);
        TextView tx = findViewById(R.id.list_main);
        assert textureView != null;
        tx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress=findViewById(R.id.progressBar);
                progress.setVisibility(View.VISIBLE);
                takePicture();
            }
        });

        init();    // call init method
        setListview();
        panelListener();

        if(fromGallery){

            Feature feature = new Feature();
            feature.setType("LABEL_DETECTION");
            feature.setMaxResults(MAX_RESULTS);

            final List<Feature> featureList = new ArrayList<>();
            featureList.add(feature);
            final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

            AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
            annotateImageReq.setFeatures(featureList);
            annotateImageReq.setImage(getImageEncodeImage(ChooseActivity.theChosenOne));
            annotateImageRequests.add(annotateImageReq);


            new AsyncTask<Object, Void, String>() {
                @Override
                protected String doInBackground(Object... params) {
                    try {

                        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                        VisionRequestInitializer requestInitializer = new VisionRequestInitializer("AIzaSyB7nfLCVc-97Zfagn8KhRJvSMBrz1j716w");

                        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                        builder.setVisionRequestInitializer(requestInitializer);

                        Vision vision = builder.build();

                        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                        batchAnnotateImagesRequest.setRequests(annotateImageRequests);

                        Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                        annotateRequest.setDisableGZipContent(true);
                        BatchAnnotateImagesResponse response = annotateRequest.execute();

                        System.out.println(response);

                        return convertResponseToString(response);
                    } catch (GoogleJsonResponseException e) {
                        Log.d(TAG, "failed to make API request because " + e.getContent());
                    } catch (IOException e) {
                        Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
                    }
                    return "Cloud Vision API request failed. Check logs for details.";
                }

                protected void onPostExecute(String result) {
                    setListview();
                }
            }.execute();

        }

        fromGallery = false;

    }

    /**
     * Initialization of the textview and SlidingUpPanelLayout
     */
    public void init() {
        mLayout = findViewById(R.id.sliding_layout);
        textView = findViewById(R.id.list_main);
        listview = findViewById(R.id.list);
    }

    @SuppressLint({"StaticFieldLeak", "DefaultLocale"})
    public List<String> array_list() {

        final List<String> results = new ArrayList<>();

        if(!entity_list.isEmpty()){

            final LanguageTranslator service = new LanguageTranslator();
            service.setUsernameAndPassword("2e2b7763-6804-40bb-8038-99c92f84f5ff","obCwHhx8rPlY");

            //additions
            final ArrayList<String> addon = new ArrayList<>();

            //For percentage number
            for(EntityAnnotation e : entity_list){
                results.add(WordUtils.capitalizeFully(e.getDescription()));
                addon.add("  |  " +
                        String.format("%.2f", e.getScore() * 100) +
                        "%");
            }

            new AsyncTask<Object, Void, String>() {
                @Override
                protected String doInBackground(Object... params) {

                    if(Objects.equals(ChooseActivity.LANGUAGE_CHOICE, "English")){

                        for(int i = 0; i < results.size(); i++)
                            results.set(i, results.get(i) + addon.get(i));

                        return "";
                    }

                    com.ibm.watson.developer_cloud.language_translator.v2.model.TranslateOptions
                            translateOptions;

                    switch (ChooseActivity.LANGUAGE_CHOICE){
                        case "German":
                            translateOptions = new
                                    com.ibm.watson.developer_cloud.language_translator.v2.model.TranslateOptions
                                            .Builder()
                                    .text(results)
                                    .source(Language.ENGLISH)
                                    .target(Language.GERMAN)
                                    .build();
                            break;
                        case "Spanish":
                            translateOptions = new
                                    com.ibm.watson.developer_cloud.language_translator.v2.model.TranslateOptions
                                            .Builder()
                                    .text(results)
                                    .source(Language.ENGLISH)
                                    .target(Language.SPANISH)
                                    .build();
                            break;
                        default:
                            translateOptions = new
                                    com.ibm.watson.developer_cloud.language_translator.v2.model.TranslateOptions
                                            .Builder()
                                    .text(results)
                                    .source(Language.ENGLISH)
                                    .target(Language.JAPANESE)
                                    .build();
                            break;
                    }

                    //gets results
                    TranslationResult result = service.translate(translateOptions).execute();

                    List<Translation> translations = result.getTranslations();

                    results.clear();
                    int count = 0;
                    for(Translation t : translations){
                        results.add(t.getTranslation() + addon.get(count));
                        count++;
                    }

                    return "";
                }
            }.execute();

            array_list = results;

        }

        return array_list;
    }

    /**
     * Set array adapter to display a list of items.
     * Called a callback setOnItemClickListener method,
     * It calls when user click on the list of item.
     */
    public void setListview() {

        mLayout.setAlpha(start);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

                String name = (String) listview.getItemAtPosition(position);

                String search = name.split("\\|")[0];
                search = search.trim();

                Pattern p = Pattern.compile("%20");
                Matcher m = p.matcher(search);

                String link = "http://www.google.com/search?q=" + m.replaceAll(" ");
                //open up wiki
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            }
        });

        /**
         * This is array adapter, it takes context of the activity as a first parameter,
         * layout of the listview as a second parameter and array as a third parameter.
         */
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_entry,
                array_list());

        listview.setAdapter(arrayAdapter);

    }

    /**
     * Call setPanelSlidelistener method to listen open and close of slide panel
     **/
    public void panelListener() {
        mLayout.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            // During the transition of expand and collapse onPanelSlide function will be called.
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

                mLayout.setAlpha((start + (slideOffset * (end - start))));
                Log.e(TAG, "onPanelSlide, offset " + slideOffset);
            }

            // Called when secondary layout is dragged up by user
            @Override
            public void onPanelExpanded(View panel) {

                Log.e(TAG, "onPanelExpanded");
            }

            // Called when secondary layout is dragged down by user
            @Override
            public void onPanelCollapsed(View panel) {

                Log.e(TAG, "onPanelCollapsed");
            }

            @Override
            public void onPanelAnchored(View panel) {

                Log.e(TAG, "onPanelAnchored");
            }

            @Override
            public void onPanelHidden(View panel) {

                Log.e(TAG, "onPanelHidden");
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // HIDES NOTIFICATION BAR
        View view = getWindow().getDecorView();
        if (hasFocus && view != null) {
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        // HIDES NOTIFICATION BAR

    }

    private void takePicture() {
        if (cameraDevice == null)
            return;

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            manager.getCameraCharacteristics(cameraDevice.getId());

            //Capture image with custom size
            final ImageReader reader = ImageReader.newInstance(PIXEL_WIDTH, PIXEL_HEIGHT, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            FILE_PATH = Environment.getExternalStorageDirectory() + "/" + UUID.randomUUID().toString() + ".jpg";

            file = new File(FILE_PATH);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    android.media.Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null)
                            image.close();
                    }
                }

                @SuppressLint("StaticFieldLeak")
                private void save(byte[] bytes) throws IOException {

                    OutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    } finally {
                        if (outputStream != null)
                            outputStream.close();
                    }

                    Feature feature = new Feature();
                    feature.setType("LABEL_DETECTION");
                    feature.setMaxResults(MAX_RESULTS);

                    ChooseActivity.theChosenOne = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    final List<Feature> featureList = new ArrayList<>();
                    featureList.add(feature);
                    final List<AnnotateImageRequest> annotateImageRequests = new ArrayList<>();

                    AnnotateImageRequest annotateImageReq = new AnnotateImageRequest();
                    annotateImageReq.setFeatures(featureList);
                    annotateImageReq.setImage(getImageEncodeImage(ChooseActivity.theChosenOne));
                    annotateImageRequests.add(annotateImageReq);


                    new AsyncTask<Object, Void, String>() {
                        @Override
                        protected String doInBackground(Object... params) {
                            try {

                                HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                                JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                                VisionRequestInitializer requestInitializer = new VisionRequestInitializer("AIzaSyB7nfLCVc-97Zfagn8KhRJvSMBrz1j716w");

                                Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                                builder.setVisionRequestInitializer(requestInitializer);

                                Vision vision = builder.build();

                                BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                                batchAnnotateImagesRequest.setRequests(annotateImageRequests);

                                Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                                annotateRequest.setDisableGZipContent(true);
                                BatchAnnotateImagesResponse response = annotateRequest.execute();

                                System.out.println(response);

                                return convertResponseToString(response);
                            } catch (GoogleJsonResponseException e) {
                                Log.d(TAG, "failed to make API request because " + e.getContent());
                            } catch (IOException e) {
                                Log.d(TAG, "failed to make API request because of other IOException " + e.getMessage());
                            }
                            return "Cloud Vision API request failed. Check logs for details.";
                        }

                        protected void onPostExecute(String result) {
                            setListview();
                            onResume();
                            progress.setVisibility(View.INVISIBLE);
                        }
                    }.execute();

                    onPause();

                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private Image getImageEncodeImage(Bitmap bitmap) {
        Image base64EncodedImage = new Image();
        // Convert the bitmap to a JPEG
        // Just in case it's a format that Android understands but Cloud Vision
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        // Base64 encode the JPEG
        base64EncodedImage.encodeContent(imageBytes);
        return base64EncodedImage;
    }

    private String convertResponseToString(BatchAnnotateImagesResponse response) {
        AnnotateImageResponse imageResponses = response.getResponses().get(0);

        List<EntityAnnotation> entityAnnotations;

        entityAnnotations = imageResponses.getLabelAnnotations();
        return formatAnnotation(entityAnnotations);
    }

    private String formatAnnotation(List<EntityAnnotation> entityAnnotation) {
        String message = "";

        entity_list = entityAnnotation;

        if (entityAnnotation != null) {
            for (EntityAnnotation entity : entityAnnotation) {
                message = message + "    " + entity.getDescription() + " " + entity.getScore();
                message += "\n";
            }
        } else {
            message = "Nothing Found";
        }
        return message;
    }


    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),
                    (int)(imageDimension.getWidth() / 1440f * 2560f));
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Can't operate without permission rights.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Collapsed layout, when user press back button
     **/
    @Override
    public void onBackPressed() {

        if (mLayout != null
                && (mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED
                || mLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {

            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        } else {

            startActivity(new Intent(CameraActivity.this,
                    ChooseActivity.class));

        }
    }

}
