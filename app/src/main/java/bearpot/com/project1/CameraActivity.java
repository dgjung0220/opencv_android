package bearpot.com.project1;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCamera2View;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import java.util.List;

/**
 * Created by dgjun on 2018-03-20.
 */

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

    private static final int MENU_GROUP_ID_SIZE = 2;

    private int mCameraIndex;
    private int mImageSizeIndex;

    private boolean mIsCameraFrontFacing;
    private int mNumCameras;

    private CameraBridgeViewBase mCameraView;
    private List<Camera.Size> mSupportedImageSizes;
    private boolean mIsPhotoPending;

    private Mat mBgr;
    private boolean mIsMenuLocked;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch(status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    mCameraView.enableView();
                    mBgr = new Mat();
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
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
        }

        initializeCameraSetting();
    }

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

        mCameraView = new JavaCamera2View(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
        */

        /*
        android.hardware.camera2
         */
        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            String cameraId = manager.getCameraIdList()[mCameraIndex];
            mIsCameraFrontFacing = manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT;

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        return null;
    }
}
