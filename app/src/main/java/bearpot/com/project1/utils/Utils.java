package bearpot.com.project1.utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;

import bearpot.com.project1.vo.ImageDescription;

public class Utils {

    // Utils
    public static int subsTwoD(Point a, Point b) {
        int result = (int) Math.abs(Math.sqrt(Math.pow((a.x-b.x),2) + Math.pow((a.y-b.y),2)));
        return result;
    }

    public static Point addPoints(Point a, Point b) {
        double x, y;

        x = a.x + b.x;
        y = a.y + b.y;

        return new Point(x,y);
    }

    public static ImageDescription calculateKpAndDescriptor(String imagePath) {
        ORB orb = ORB.create();
        orb.setMaxFeatures(1000);

        Mat A = Imgcodecs.imread(imagePath, Imgcodecs.IMREAD_GRAYSCALE);
        MatOfKeyPoint kp = new MatOfKeyPoint();
        Mat descriptor = new Mat();

        orb.detectAndCompute(A, new Mat(), kp, descriptor);

        return new ImageDescription(kp, descriptor);
    }
}
