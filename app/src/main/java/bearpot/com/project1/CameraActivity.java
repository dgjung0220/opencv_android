package bearpot.com.project1;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;


/**
 * Created by dgjun on 2018-03-20.
 */

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = CameraActivity.class.getSimpleName();
    private Context context;

    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX = "imageDetectionIndexFilterIndex";
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

    private static final int MENU_GROUP_ID_SIZE = 2;

    private Filter[] mImageDetectionFilters;

    private int mImageDetectionFilterIndex;
    private int mCameraIndex;
    private int mImageSizeIndex;

    private boolean mIsCameraFrontFacing;
    private int mNumCameras;

    private CameraBridgeViewBase mCameraView;
    private Size[] mSupportedImageSizes;
    private boolean mIsPhotoPending;

    private Mat mBgr;
    private boolean mIsMenuLocked;

    private boolean hasPermissions(String[] permissions) {
        int result;
        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");

                    System.loadLibrary("opencv_java3");
                    System.loadLibrary("native-lib");

                    mCameraView.enableView();
                    mBgr = new Mat();

                    final Filter starryNight;
                    try {
                        starryNight = new ImageDetectionFilter(CameraActivity.this, "/mnt/sdcard/Assets/starry_night.jpg");
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }

                    final Filter portrait;
                    try {
                        portrait = new ImageDetectionFilter(CameraActivity.this, "/mnt/sdcard/Assets/self_portrait.jpg");
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }

                    final Filter book;
                    try {
                        book = new ImageDetectionFilter(CameraActivity.this, "/mnt/sdcard/Assets/book.jpg");
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                    mImageDetectionFilters = new Filter[] { new NoneFilter(), starryNight, portrait, book};

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(final Bundle savedInstanceState) {

        int PERMISSIONS_REQUEST_CODE = 1000;
        String[] PERMISSIONS = {"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!hasPermissions(PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
            mImageDetectionFilterIndex = savedInstanceState.getInt(STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mImageDetectionFilterIndex = 0;
        }

        initializeCameraSetting();
    }

    @Override
    public void recreate() {
        super.recreate();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mImageDetectionFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        mIsMenuLocked = false;
    }

    @Override
    public void onPause() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);

        if (mNumCameras < 2) {
            menu.removeItem(R.id.menu_next_camera);
        }

        int numSupprotedImageSizes = mSupportedImageSizes.length;

        if (numSupprotedImageSizes > 1) {
            final SubMenu sizeSubMenu = menu.addSubMenu(R.string.menu_image_size);

            for (int i = 0; i < numSupprotedImageSizes; i++) {
                final Size size = mSupportedImageSizes[i];
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE, String.format("%dx%d", size.getWidth(), size.getHeight()));
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mIsMenuLocked) {
            return true;
        }

        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            mImageSizeIndex = item.getItemId();
            recreate();
            return true;
        }

        switch(item.getItemId()) {
            case R.id.menu_next_image_detection_filter:
                mImageDetectionFilterIndex++;
                if (mImageDetectionFilterIndex == mImageDetectionFilters.length) {
                    mImageDetectionFilterIndex = 0;
                }
                return true;

            case R.id.menu_next_camera:
                mIsMenuLocked = true;

                mCameraIndex++;
                if (mCameraIndex == mNumCameras) {
                    mCameraIndex = 0;
                }
                mImageSizeIndex = 0;
                recreate();
                return true;

            case R.id.menu_take_photo:
                mIsMenuLocked = true;
                mIsPhotoPending = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initializeCameraSetting() {
        /*
        android.hardware.camera, deprecated.... use camera2 instead.

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraIndex, cameraInfo);

        mIsCameraFrontFacing = (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        mNumCameras = Camera.getNumberOfCameras();
        final Camera camera = Camera.open(mCameraIndex);

        final Camera.Parameters parameters = camera.getParameters();
        camera.release();

        mSupportedImageSizes = parameters.getSupportedPreviewSizes();
        final Camera.Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = new JavaCameraView(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
        */

        /*
        android.hardware.camera2
         */
        try {
            CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            String cameraId = camManager.getCameraIdList()[mCameraIndex];
            mIsCameraFrontFacing = camManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;
            mNumCameras = camManager.getCameraIdList().length;

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            StreamConfigurationMap parameters = camManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSupportedImageSizes = parameters.getOutputSizes(ImageFormat.JPEG);

            Size size = mSupportedImageSizes[mImageSizeIndex];
            mCameraView = new JavaCameraView(this, mCameraIndex);
            mCameraView.setMaxFrameSize(size.getWidth(), size.getHeight());
            mCameraView.setCvCameraViewListener(this);

            camManager.openCamera(cameraId, mStateCallback, null);
            setContentView(mCameraView);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

        if (mImageDetectionFilters != null) {
            mImageDetectionFilters[mImageDetectionFilterIndex].apply(rgba, rgba);
        }

        if (mIsPhotoPending) {
            mIsPhotoPending = false;
            takePhoto(rgba);
        }

        if (mIsCameraFrontFacing) {
            Core.flip(rgba, rgba, 1);
        }
        return rgba;
    }

    private void takePhoto(final Mat rgba) {
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator + appName;
        final String photoPath = albumPath + File.separator + currentTimeMillis + LabActivity.PHOTO_FILE_EXTENSION;

        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE, LabActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);

        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " + albumPath);
            onTakePhotoFailed();
            return;
        }

        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGB2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }

        Uri uri;
        try {
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();

            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }
            onTakePhotoFailed();
            return;
        }

        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH, photoPath);

        startActivity(intent);
    }

    private void onTakePhotoFailed() {
        mIsMenuLocked = false;

        final String errorMessage = getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
