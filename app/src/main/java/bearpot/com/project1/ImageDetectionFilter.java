package bearpot.com.project1;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dg.jung on 2018-03-23.
 */

public class ImageDetectionFilter implements Filter {

    private final Mat mReferenceImage;

    private final MatOfKeyPoint mReferenceKeypoints = new MatOfKeyPoint();
    private final MatOfKeyPoint mSceneKeypoints = new MatOfKeyPoint();

    private final Mat mReferenceDescriptors = new Mat();
    private final Mat mSceneDescriptors = new Mat();

    private final Mat mReferenceCorners = new Mat(4, 1, CvType.CV_32FC2);
    private final Mat mCandidateSceneCorners = new Mat(4,1, CvType.CV_32FC2);
    private final Mat mSceneCorners = new Mat(4, 1, CvType.CV_32FC2);
    private final MatOfPoint mIntSceneCorners = new MatOfPoint();

    private final Mat mGraySrc = new Mat();
    private final Mat mWarpedSrc = new Mat();

    private final MatOfDMatch mMatches = new MatOfDMatch();
    private LinkedList<MatOfDMatch> m_knnMatches;

    private final Mat rough_homography = new Mat();
    private final Mat refine_homography = new Mat();

    private ORB orb = ORB.create();
    private final DescriptorMatcher mDescriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);

    public ImageDetectionFilter(final Context context, final String filePath) throws IOException {
        mReferenceImage = Imgcodecs.imread(filePath);

        orb.setMaxFeatures(1000);
        m_knnMatches = new LinkedList<>();

        final Mat referenceImageGray = new Mat();
        Imgproc.cvtColor(mReferenceImage, referenceImageGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(mReferenceImage, mReferenceImage, Imgproc.COLOR_BGR2RGB);

        mReferenceCorners.put(0, 0, new double[]{0.0, 0.0});
        mReferenceCorners.put(1, 0, new double[]{referenceImageGray.cols(), 0.0});
        mReferenceCorners.put(2, 0, new double[]{referenceImageGray.cols(), referenceImageGray.rows()});
        mReferenceCorners.put(3, 0, new double[]{0.0, referenceImageGray.rows()});

        orb.detectAndCompute(referenceImageGray, new Mat(), mReferenceKeypoints, mReferenceDescriptors);
    }

    @Override
    public void apply(Mat src, Mat dst) {
        Imgproc.cvtColor(src, mGraySrc, Imgproc.COLOR_RGB2GRAY);
        orb.detectAndCompute(mGraySrc, new Mat(), mSceneKeypoints, mSceneDescriptors);

        mDescriptorMatcher.match(mSceneDescriptors, mReferenceDescriptors, mMatches);
        findSceneCorners(src, dst);
    }

    private void findSceneCorners(Mat src, Mat dst) {
        List<DMatch> matchesList = mMatches.toList();

        if (matchesList.size() < 4) {
            return;
        }

        List<KeyPoint> referenceKeyPointsList = mReferenceKeypoints.toList();
        List<KeyPoint> sceneKeyPointsList = mSceneKeypoints.toList();

        double max_dist = 0.0;
        double min_dist = Double.MAX_VALUE;

        for (DMatch match : matchesList) {
            double dist = match.distance;
            if (dist < min_dist) min_dist = dist;
            if (dist > max_dist) max_dist = dist;
        }

        if (min_dist > 50.0) {
            mSceneCorners.create(0,0, mSceneCorners.type());
            return;
        } else if (min_dist > 25.0) {
            // target lost, but still close;
            return;
        }

        ArrayList<Point> goodReferencePointsList = new ArrayList<>();
        ArrayList<Point> goodScenePointsList = new ArrayList<>();

        double maxGoodMatchDist = 1.75 * min_dist;

        for (DMatch match : matchesList) {
            if (match.distance < maxGoodMatchDist) {
                goodReferencePointsList.add(referenceKeyPointsList.get(match.trainIdx).pt);
                goodScenePointsList.add(sceneKeyPointsList.get(match.queryIdx).pt);
            }
        }

        if (goodReferencePointsList.size() < 4 || goodScenePointsList.size() < 4) {
            return;
        }

        MatOfPoint2f goodReferencePoints = new MatOfPoint2f();
        MatOfPoint2f goodScenePoints = new MatOfPoint2f();

        goodReferencePoints.fromList(goodReferencePointsList);
        goodScenePoints.fromList(goodScenePointsList);

        Mat homography = Calib3d.findHomography(goodReferencePoints, goodScenePoints, Calib3d.RANSAC, 5);

        if (! homography.empty()) {
            Core.perspectiveTransform(mReferenceCorners, mCandidateSceneCorners, homography);
            mCandidateSceneCorners.convertTo(mIntSceneCorners, CvType.CV_32S);
        }

        if (Imgproc.isContourConvex(mIntSceneCorners)) {
            mCandidateSceneCorners.copyTo(mSceneCorners);
        }
        draw(src, dst);
    }

    protected void draw(Mat src, Mat dst) {

        if (dst != src) {
            src.copyTo(dst);
        }

        if (mSceneCorners.height() < 4) {
            int height = mReferenceImage.height();
            int width = mReferenceImage.width();
            int maxDimension = Math.min(dst.width(), dst.height()) / 2;

            double aspectRatio = width / (double) height;

            if (height > width) {
                height = maxDimension;
                width = (int) (height * aspectRatio);
            } else {
                width = maxDimension;
                height = (int) (width / aspectRatio);
            }

            Mat dstROI = dst.submat(0, height, 0, width);
            Imgproc.resize(mReferenceImage, dstROI, dstROI.size(), 0.0, 0.0, Imgproc.INTER_AREA);

            return;
        }

        Imgproc.line(dst, new Point(mSceneCorners.get(0, 0)), new Point(mSceneCorners.get(1, 0)), ScalarColors.RED, 10);
        Imgproc.line(dst, new Point(mSceneCorners.get(1, 0)), new Point(mSceneCorners.get(2, 0)), ScalarColors.RED, 10);
        Imgproc.line(dst, new Point(mSceneCorners.get(2, 0)), new Point(mSceneCorners.get(3, 0)), ScalarColors.RED, 10);
        Imgproc.line(dst, new Point(mSceneCorners.get(3, 0)), new Point(mSceneCorners.get(0, 0)), ScalarColors.RED, 10);

        CameraActivity.scene_corners = mSceneCorners;
    }


}
