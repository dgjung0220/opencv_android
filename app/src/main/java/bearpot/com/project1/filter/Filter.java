package bearpot.com.project1.filter;

import org.opencv.core.Mat;

/**
 * Created by dg.jung on 2018-03-23.
 */

public interface Filter {
    public abstract void apply(final Mat src, final Mat dst);
}
