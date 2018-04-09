package bearpot.com.project1.vo;

import android.net.Uri;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;

public class Target {
    private String imageName;
    private String imagePath;
    private String imageUrl;

    private Size keyPoint_size;
    private Size descriptor_size;

    public Target() {}
    public Target(String imageName, String imagePath, String imageUrl, ImageDescription imageDescription) {
        this.imageName = imageName;
        this.imagePath = imagePath;
        this.imageUrl = imageUrl;
        this.keyPoint_size = imageDescription.getKeyPoint().size();
        this.descriptor_size = imageDescription.getDescriptor().size();
    }
    public Target(String imageName, String imagePath, String imageUrl, MatOfKeyPoint keyPoint, Mat descriptor) {
        this.imageName = imageName;
        this.imagePath = imagePath;
        this.imageUrl = imageUrl;
        this.keyPoint_size = keyPoint.size();
        this.descriptor_size = descriptor.size();
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void setKeyPoint(MatOfKeyPoint keyPoint) {
        this.keyPoint_size = keyPoint.size();
    }
    public void setDescriptor(Mat descriptor) {
        this.descriptor_size = descriptor.size();
    }

    public String getImageName() {
        return imageName;
    }
    public String getImagePath() {
        return imagePath;
    }
    public String getImageUrl() {
        return imageUrl;
    }
    public Size getKeyPoint() {
        return keyPoint_size;
    }
    public Size getDescriptor() {
        return descriptor_size;
    }

}
