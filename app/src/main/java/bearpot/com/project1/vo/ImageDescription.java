package bearpot.com.project1.vo;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class ImageDescription {
    private MatOfKeyPoint keyPoint;
    private Mat descriptor;

    public ImageDescription() {}
    public ImageDescription(MatOfKeyPoint keyPoint, Mat descriptor) {
        this.keyPoint = keyPoint;
        this.descriptor = descriptor;
    }

    public void setKeyPoint(MatOfKeyPoint keyPoint) {
        this.keyPoint = keyPoint;
    }
    public void setDescriptor(Mat descriptor) {
        this.descriptor = descriptor;
    }

    public MatOfKeyPoint getKeyPoint() {
        return keyPoint;
    }

    public Mat getDescriptor() {
        return descriptor;
    }
}
