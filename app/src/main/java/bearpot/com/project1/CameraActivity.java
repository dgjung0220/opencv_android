package bearpot.com.project1;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * Created by dgjung on 2018-03-20.
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

    // 스크린 사이즈
    private int width;
    private int height;
    private Rect roi;
    private Point A;
    private Point B;
    public static Mat scene_corners;

    private Intent intent;
    private String target;
    private Filter target_filter;

    private int mViewMode;

    private final int NORMAL_MODE = 0;
    private final int DETECTING_MODE= 1;
    private final int TRACKING_MODE = 2;

    // Tracking (camShift)
    private boolean SELECTED_OBJ = false;
    private Rect selection = null;
    private Mat hsv, mask, hue, backproj, hist;
    private Rect trackWindow;
    private int trackObject = 0;

    //Firebase Auth
    //private FirebaseAuth mFirebaseAuth;


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

                    if (target != null) {
                        try {
                            target_filter = new ImageDetectionFilter(CameraActivity.this, target);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        mViewMode = DETECTING_MODE;
                    }

                    /*final Filter starryNight;
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
                    }*/
                    //mImageDetectionFilters = new Filter[] {new NoneFilter()};

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

        DisplayMetrics mDisplayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        width = mDisplayMetrics.widthPixels;
        height = mDisplayMetrics.heightPixels;
        int patternWidth = (int) (Math.min(width, height) * 0.8f);
        A = new Point(width / 2 - patternWidth / 2, height / 2 - patternWidth /2);
        B = new Point(width - patternWidth, height - patternWidth /2);

        intent = getIntent();
        if (intent != null) {
            target = intent.getStringExtra(LabActivity.EXTRA_PHOTO_DATA_PATH);
            Log.d(TAG, ""+target);
        }

        roi = new Rect((int)A.x,(int)A.y,(int)(B.x-A.x),(int)(B.y-A.y));

        mViewMode = NORMAL_MODE;
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
                ActivityCompat.finishAffinity(this);
                startActivity(new Intent(CameraActivity.this, CameraActivity.class));

                mViewMode = NORMAL_MODE;
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

            case R.id.menu_receive:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(intent, 1112);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1112:
                    sendPicture(data.getData());
                    break;

                default:
                    break;
            }
        }
    }

    private void sendPicture(Uri imgUri) {

        String imagePath = getRealPathFromURI(imgUri);
        ExifInterface exif = null;

        try {
            exif = new ExifInterface(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int exifDegree = exifOrientationToDegrees(exifOrientation);

        try {
            target_filter = new ImageDetectionFilter(CameraActivity.this, imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mViewMode = DETECTING_MODE;

        //Bitmap bitmap = BitmapFactory.decodeFile(imagePath);//경로를 통해 비트맵으로 전환
        //ivImage.setImageBitmap(rotate(bitmap, exifDegree));//이미지 뷰에 비트맵 넣기
    }

    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private String getRealPathFromURI(Uri contentUri) {
        int column_index=0;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }

        return cursor.getString(column_index);
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
        hsv = new Mat();
        hue = new Mat();
        mask = new Mat();
        hist = new Mat();
        backproj = new Mat();
        trackWindow = new Rect();
    }

    @Override
    public void onCameraViewStopped() { }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

        switch (mViewMode) {
            case NORMAL_MODE:
                new NoneFilter().apply(rgba, rgba);
                Imgproc.rectangle(rgba, A , B, ScalarColors.GREEN,10,8,0);

                SELECTED_OBJ = false;

                if (mIsPhotoPending) {
                    mIsPhotoPending = false;
                    takePhoto(rgba);
                }

                if (mIsCameraFrontFacing) {
                    Core.flip(rgba, rgba, 1);
                }
                return rgba;

            case DETECTING_MODE:
                Imgproc.rectangle(rgba, A , B, ScalarColors.GREEN,10,8,0);
                target_filter.apply(rgba, rgba);
                return rgba;

            case TRACKING_MODE:
                camShift(rgba);
                return rgba;
        }

        return rgba;
    }

    private void camShift(Mat rgba) {
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_BGR2HSV);

        if (trackObject != 0) {
            int vmin = 10;
            int vmax = 256;
            int smin = 30;

            Core.inRange(hsv, new Scalar(0, smin, Math.min(vmin, vmax)), new Scalar(180, 256, Math.max(vmin, vmax)), mask);
            hue.create(hsv.size(), hsv.depth());

            List<Mat> hueList = new LinkedList<Mat>();
            List<Mat> hsvList = new LinkedList<Mat>();
            hsvList.add(hsv);
            hueList.add(hue);

            MatOfInt ch = new MatOfInt(0, 0);
            Core.mixChannels(hsvList, hueList, ch);
            MatOfFloat histRange = new MatOfFloat(0, 180);

            if (trackObject < 0) {
                Mat subHue = hue.submat(selection);

                Imgproc.calcHist(Arrays.asList(subHue), new MatOfInt(0), new Mat(), hist, new MatOfInt(16), histRange);
                Core.normalize(hist, hist, 0, 255, Core.NORM_MINMAX);
                trackWindow = selection;
                trackObject = 1;
            }

            MatOfInt ch2 = new MatOfInt(0,1);
            Imgproc.calcBackProject(Arrays.asList(hue), ch2, hist, backproj, histRange, 1);

            Core.bitwise_and(backproj, mask, backproj);
            RotatedRect trackBox = Video.CamShift(backproj, trackWindow, new TermCriteria(TermCriteria.EPS | TermCriteria.MAX_ITER, 10,1));

            Imgproc.ellipse(rgba, trackBox, new Scalar(0,0,255),4);

            if (trackWindow.area() <= 1) {
                trackObject = 0;
            }

            if (selection != null) {
                Imgproc.rectangle(rgba, selection.tl(), selection.br(), new Scalar(0,255, 255),2);
            }
        }
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
        Mat cropped = new Mat(mBgr, roi);
        if (!Imgcodecs.imwrite(photoPath, cropped)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }

        /* Not Cropped.
        if (!Imgcodecs.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }*/

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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (scene_corners != null) {
            mViewMode = TRACKING_MODE;
        }

        if (mViewMode == TRACKING_MODE) {

            selection = new Rect((int) scene_corners.get(0,0)[0], (int) scene_corners.get(0,0)[1],
                    (int) Math.abs(scene_corners.get(0,0)[0] - scene_corners.get(1,0)[0]),
                    (int) Math.abs(scene_corners.get(0,0)[1] - scene_corners.get(3,0)[1]));

            SELECTED_OBJ = true;
            trackObject = -1;

            if (SELECTED_OBJ) {
                selection.x = (int) scene_corners.get(0,0)[0];
                selection.y = (int) scene_corners.get(0,0)[1];

                int width = subsTwoD(new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)));
                int height = subsTwoD(new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)));

                selection.width = width;
                selection.height = height;
            }
        }

        return super.onTouchEvent(event);
    }

    public int subsTwoD(Point a, Point b) {
        int result = (int) Math.abs(Math.sqrt(Math.pow((a.x-b.x),2) + Math.pow((a.y-b.y),2)));
        return result;
    }
}
