package com.lge.project1;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dg.jung on 2018-03-20.
 */

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "OPENCV::TEST";

    public ImageView imageView;

    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS  = {"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    // Feature Extraction, Compute & Matching
    ORB orb;
    DescriptorMatcher matcher;
    Mat descriptors_object, descriptors_scene;
    MatOfKeyPoint keypoints_object, keypoints_scene;
    Mat img_object;
    Mat img_scene;
    Mat img_matches;

    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load native library after(!) OpenCV initialization

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

    public TestActivity() {
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
        img_object = new Mat();
        img_object = Imgcodecs.imread("/mnt/sdcard/Assets/book.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        img_scene = new Mat();
        img_scene = Imgcodecs.imread("/mnt/sdcard/Assets/book_scene.jpg", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        img_matches = new Mat();

        if ( img_object.empty() || img_scene.empty()) {
            Log.d(TAG, "Error reading images");
        }

        orb = ORB.create();
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        keypoints_object = new MatOfKeyPoint();
        descriptors_object = new Mat();

        orb.detectAndCompute(img_object, new Mat(), keypoints_object, descriptors_object);

        keypoints_scene = new MatOfKeyPoint();
        descriptors_scene = new Mat();

        orb.detectAndCompute(img_scene, new Mat(), keypoints_scene, descriptors_scene);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.test_main);

        imageView = (ImageView)findViewById(R.id.imageView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        /*MatOfDMatch matches = new MatOfDMatch();
        if (img_object.type() == img_scene.type()) {
            matcher.match(descriptors_object, descriptors_scene, matches);
        } else {
            return ;
        }
        double max_dist = 0;
        double min_dist = 100;
        List<DMatch> matches_list= matches.toList();
        for (int i = 0; i < descriptors_object.rows(); i++) {
            double dist = matches_list.get(i).distance;
            if (dist < min_dist) min_dist = dist;
            if (dist > max_dist) max_dist = dist;
        }

        Log.d(TAG, "--Max dist : " + max_dist);
        Log.d(TAG, "--Min dist : " + min_dist);

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (int i = 0; i < matches_list.size(); i++) {
            if (matches_list.get(i).distance <= (1.5 * min_dist))
                good_matches.addLast(matches_list.get(i));
        }

        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);
        Mat img_matches = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        Features2d.drawMatches(img_object, keypoints_object, img_scene, keypoints_scene, goodMatches, img_matches, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);

        Imgproc.resize(img_matches, img_matches, img_scene.size());

        // 여기서부터....
        MatOfPoint2f obj = new MatOfPoint2f();
        MatOfPoint2f scene = new MatOfPoint2f();

        LinkedList<Point> obj_list = new LinkedList<>();
        LinkedList<Point> scene_list = new LinkedList<>();

        List<KeyPoint> kp_object_list = keypoints_object.toList();
        List<KeyPoint> kp_scene_list = keypoints_scene.toList();

        for (int i = 0; i < good_matches.size(); i++) {
            obj_list.addLast(kp_object_list.get(good_matches.get(i).queryIdx).pt);
            scene_list.addLast(kp_scene_list.get(good_matches.get(i).trainIdx).pt);
        }

        obj.fromList(obj_list);
        scene.fromList(scene_list);

        Log.d(TAG, "obj_list " + " 개수 : " + obj_list.size());
        for (int i = 0; i < obj_list.size(); i++) {
            Log.d(TAG, obj_list.get(i).toString());
        }

        Log.d(TAG, "scene_list"+ " 개수 : " + scene_list.size());
        for (int i = 0; i < scene_list.size(); i++) {
            Log.d(TAG, scene_list.get(i).toString());
        }

        Mat H = Calib3d.findHomography(obj, scene);
        Mat obj_corners = new Mat(4,1, CvType.CV_32FC2);
        Mat scene_corners = new Mat(4,1, CvType.CV_32FC2);

        obj_corners.put(0, 0, new double[] {0,0});
        obj_corners.put(1, 0, new double[] {img_object.cols(),0});
        obj_corners.put(2, 0, new double[] {img_object.cols(),img_object.rows()});
        obj_corners.put(3, 0, new double[] {0,img_object.rows()});

        Core.perspectiveTransform(obj_corners,scene_corners, H);

        Imgproc.line(img_matches, new Point(scene_corners.get(0,0)[0] + img_object.cols(), scene_corners.get(0,0)[1] ) , new Point(scene_corners.get(1,0)[0] + img_object.cols(), scene_corners.get(1,0)[1]), RED,4);
        Imgproc.line(img_matches, new Point(scene_corners.get(1,0)[0] + img_object.cols(), scene_corners.get(1,0)[1] ) , new Point(scene_corners.get(2,0)[0] + img_object.cols(), scene_corners.get(2,0)[1]), RED,4);
        Imgproc.line(img_matches, new Point(scene_corners.get(2,0)[0] + img_object.cols(), scene_corners.get(2,0)[1] ) , new Point(scene_corners.get(3,0)[0] + img_object.cols(), scene_corners.get(3,0)[1]), RED,4);
        Imgproc.line(img_matches, new Point(scene_corners.get(3,0)[0] + img_object.cols(), scene_corners.get(3,0)[1] ) , new Point(scene_corners.get(0,0)[0] + img_object.cols(), scene_corners.get(0,0)[1]), RED,4);*/

        int res = FindFeatures(img_object.getNativeObjAddr(), img_scene.getNativeObjAddr(), img_matches.getNativeObjAddr());

        if (res == 1) {
            if (img_matches.empty()) {
                Log.d(TAG, "empty");
            } else {
                Bitmap bmp = null;

                try {
                    bmp = Bitmap.createBitmap(img_matches.cols(), img_matches.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(img_matches, bmp);
                } catch(CvException e) {

                }

                imageView.setImageBitmap(bmp);
            }
        }

    }

    public native int FindFeatures(long matAddrObj, long presave_imageAddrScene, long img_matchesAddr);
}
