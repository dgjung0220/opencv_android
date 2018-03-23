package com.lge.project1;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    //private NaverLens mNaverLens;
    private static final String TAG = "OPENCV::Activity";

    boolean selectObject = false;
    Rect selection = null;
    Point origin;

    Mat hsv, mask, hue, backproj;
    Rect trackWindow;

    private Mat hist;
    int trackObject = 0;

    private static final int VIEW_MODE_RGBA     = 0;
    private static final int VIEW_MODE_GRAY     = 1;
    private static final int VIEW_MODE_CANNY    = 2;
    private static final int VIEW_MODE_TEST      = 100;
    private static final int VIEW_MODE_FEATURES = 5;

    private int mViewMode;
    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    private MenuItem mItemPreviewRGBA;
    private MenuItem mItemPreviewGray;
    private MenuItem mItemPreviewCanny;
    private MenuItem mItemPreviewFeatures;
    private MenuItem mItemPreviewTest;

    private CameraBridgeViewBase   mOpenCvCameraView;

    // For Permission
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS  = {"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    // Feature Extraction, Compute & Matching
    ORB orb;
    DescriptorMatcher matcher;

    Mat descriptors_object, descriptors_scene;
    MatOfKeyPoint keypoints_object, keypoints_scene;

    Mat img_object;
    Mat img_scene;

    Mat obj_corners, scene_corners;
    List<KeyPoint> kp_object_list;
    List<KeyPoint> kp_scene_list;

    Mat objectHistogram;
    Mat globalHistogram;

    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("opencv_java3");
                    System.loadLibrary("native-lib");
                    try {
                        initializeOpenCVDependencies();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

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

    private void initializeOpenCVDependencies() throws IOException {
        mOpenCvCameraView.enableView();

        img_object = new Mat();
        //img_object = Imgcodecs.imread("/mnt/sdcard/Assets/book.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        img_object = Imgcodecs.imread("/mnt/sdcard/Assets/starry_night.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        img_scene = new Mat();
        img_scene = Imgcodecs.imread("/mnt/sdcard/Assets/book_scene.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        if ( img_object.empty() || img_scene.empty()) {
            Log.d(TAG, "Error reading images");
        }

        objectHistogram = new Mat();
        globalHistogram = new Mat();

        orb = ORB.create();
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

        keypoints_object = new MatOfKeyPoint();
        descriptors_object = new Mat();

        orb.detectAndCompute(img_object, new Mat(), keypoints_object, descriptors_object);

        obj_corners = new Mat(4,1, CvType.CV_32FC2);
        scene_corners = new Mat(4,1, CvType.CV_32FC2);
        obj_corners.put(0, 0, new double[] {0,0});
        obj_corners.put(1, 0, new double[] {img_object.cols(),0});
        obj_corners.put(2, 0, new double[] {img_object.cols(),img_object.rows()});
        obj_corners.put(3, 0, new double[] {0,img_object.rows()});

        kp_object_list = keypoints_object.toList();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewFeatures = menu.add("Find features");
        mItemPreviewTest = menu.add("TEST");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        //mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat();
        mGray = new Mat(height, width, CvType.CV_8UC1);

        hsv = new Mat();
        hue = new Mat();
        mask = new Mat();

        hist = new Mat();
        backproj = new Mat();
        trackWindow = new Rect();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
            case VIEW_MODE_GRAY:
                // input frame has gray scale format
                Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case VIEW_MODE_RGBA:
                // input frame has RBGA format
                mRgba = inputFrame.rgba();
                break;
            case VIEW_MODE_CANNY:
                // input frame has gray scale format
                mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mIntermediateMat, 80, 100);
                Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case VIEW_MODE_FEATURES:
                // input frame has RGBA format
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                return featureAndMatching(mGray, mRgba);
            case VIEW_MODE_TEST:
                mRgba = inputFrame.rgba();
                Imgproc.cvtColor(mRgba, hsv, Imgproc.COLOR_BGR2HSV);

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

                    Imgproc.ellipse(mRgba, trackBox, new Scalar(0,0,255),4);

                    if (trackWindow.area() <= 1) {
                        trackObject = 0;
                    }
                }

                if (selection != null) {
                    Imgproc.rectangle(mRgba, selection.tl(), selection.br(), new Scalar(0,255, 255),2);
                }
                return mRgba;
        }
        return mRgba;
    }

    public Mat featureAndMatching(Mat mGr, Mat mRgba) {
        try {
            keypoints_scene = new MatOfKeyPoint();
            descriptors_scene = new Mat();

            orb.detectAndCompute(mGr, new Mat(), keypoints_scene, descriptors_scene);

            MatOfDMatch matches = new MatOfDMatch();
            if (img_object.type() == mGr.type()) {
                matcher.match(descriptors_object, descriptors_scene, matches);
            } else {
                return mRgba;
            }

            double max_dist = 0;
            double min_dist = 100;
            List<DMatch> matches_list= matches.toList();
            for (int i = 0; i < descriptors_object.rows(); i++) {
                double dist = matches_list.get(i).distance;
                if (dist < min_dist) min_dist = dist;
                if (dist > max_dist) max_dist = dist;
            }

            Log.d(TAG, "distance : " + min_dist);
            if (min_dist < 20) {
                LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
                for (int i = 0; i < matches_list.size(); i++) {
                    if (matches_list.get(i).distance <= (3 * min_dist))
                        good_matches.addLast(matches_list.get(i));
                }

                MatOfPoint2f obj = new MatOfPoint2f();
                MatOfPoint2f scene = new MatOfPoint2f();

                LinkedList<Point> obj_list = new LinkedList<>();
                LinkedList<Point> scene_list = new LinkedList<>();

                kp_scene_list = keypoints_scene.toList();

                for (int i = 0; i < good_matches.size(); i++) {
                    obj_list.addLast(kp_object_list.get(good_matches.get(i).queryIdx).pt);
                    scene_list.addLast(kp_scene_list.get(good_matches.get(i).trainIdx).pt);
                    Imgproc.circle(mRgba, new Point(scene_list.get(i).x , scene_list.get(i).y), 3, GREEN );
                }
                obj.fromList(obj_list);
                scene.fromList(scene_list);

                Mat H = new Mat();
                if (good_matches.size() > 3) {
                    H = Calib3d.findHomography(obj, scene, Calib3d.RANSAC , 5);
                }

                if (! H.empty()) {
                    Core.perspectiveTransform(obj_corners,scene_corners, H);
                    Imgproc.line(mRgba, new Point(scene_corners.get(0,0)) , new Point(scene_corners.get(1,0)), RED,4);
                    Imgproc.line(mRgba, new Point(scene_corners.get(1,0)) , new Point(scene_corners.get(2,0)), RED,4);
                    Imgproc.line(mRgba, new Point(scene_corners.get(2,0)) , new Point(scene_corners.get(3,0)), RED,4);
                    Imgproc.line(mRgba, new Point(scene_corners.get(3,0)) , new Point(scene_corners.get(0,0)), RED,4);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return mRgba;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewCanny) {
            mViewMode = VIEW_MODE_CANNY;
        } else if (item == mItemPreviewFeatures) {
            mViewMode = VIEW_MODE_FEATURES;
        } else if (item == mItemPreviewTest) {
            mViewMode = VIEW_MODE_TEST;
        }

        return true;
    }

    @SuppressLint("ShowToast")
    @Override public boolean onTouchEvent(MotionEvent event) {
        mViewMode = VIEW_MODE_TEST;

        if (!scene_corners.empty()) {

            Log.d(TAG, "Camshift");

            selection = new Rect((int) scene_corners.get(0,0)[0], (int) scene_corners.get(0,0)[1],
                    (int) Math.abs(scene_corners.get(0,0)[0] - scene_corners.get(1,0)[0]),
                    (int) Math.abs(scene_corners.get(0,0)[1] - scene_corners.get(3,0)[1]));

            selectObject = true;
            trackObject = -1;


            if (selectObject) {
                selection.x = (int) scene_corners.get(0,0)[0];
                selection.y = (int) scene_corners.get(0,0)[1];

                int width = subsTwoD(new Point(scene_corners.get(0,0)), new Point(scene_corners.get(1,0)));
                int height = subsTwoD(new Point(scene_corners.get(3,0)), new Point(scene_corners.get(0,0)));

                selection.width = width;
                selection.height = height;
            }

        }

        /*if (selectObject) {
            selection.x = (int) Math.min(event.getX(), origin.x);
            selection.y = (int) Math.min(event.getY(), origin.y);
            selection.width = (int) Math.abs(event.getX() - origin.x);
            selection.height = (int) Math.abs(event.getY() - origin.y);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            origin = new Point(event.getX(), event.getY());
            selection = new Rect((int) event.getX(), (int) event.getY(), 0, 0);
            selectObject = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            selectObject = false;
            if (selection.width > 0 && selection.height > 0)
                trackObject = -1;
        }*/
        return super.onTouchEvent(event);
    }

    public int subsTwoD(Point a, Point b) {

        int result = (int) Math.abs(Math.sqrt(Math.pow((a.x-b.x),2) + Math.pow((a.y-b.y),2)));
        return result;
    }

    public native void FindFeatures(long matAddrGr, long matAddrRgba, long presave_imageAddr);
}
