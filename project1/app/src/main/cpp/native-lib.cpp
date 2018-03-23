#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <vector>

#include "Log.h"

using namespace std;
using namespace cv;

extern "C"
JNIEXPORT jstring

JNICALL
Java_com_lge_project1_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"

JNIEXPORT void JNICALL
Java_com_lge_project1_MainActivity_FindFeatures(JNIEnv *env, jobject instance, jlong addrGray, jlong addrRgba, jlong presaved_imageAddr) {

    // TODO
    Mat& img_scene  = *(Mat*)addrGray;
    Mat& img_sceneRGB = *(Mat*)addrRgba;
    Mat& img_obj = *(Mat*)presaved_imageAddr;

    vector<KeyPoint> keypoints_object, keypoints_scene;
    Mat descriptors_object, descriptors_scene;

    if ( !img_scene.data || !img_obj.data) {
        LOGD("Error reading images");
    }

    int minHessian = 400;

    Ptr<ORB> orb = ORB::create(minHessian);

    orb->detectAndCompute(img_scene, Mat(), keypoints_scene, descriptors_scene);
    orb->detectAndCompute(img_obj, Mat(), keypoints_object, descriptors_object);

    /*for( unsigned int i = 0; i < keypoints_scene.size(); i++ )
    {
        const KeyPoint& kp = keypoints_scene[i];
        circle(img_sceneRGB, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }*/

    BFMatcher matcher;
    vector<DMatch> matches;
    matcher.match(descriptors_object, descriptors_scene, matches, noArray());

    double max_dist = 0;
    double min_dist = 100;

    for (int i = 0; i < descriptors_object.rows; i++) {
        double dist = matches[i].distance;
        if (dist < min_dist) min_dist = dist;
        if (dist > max_dist) max_dist = dist;
    }

    vector<DMatch> good_matches;

    for (int i = 0; i < descriptors_object.rows; i++) {
        if (matches[i].distance < 3 * min_dist) {
            good_matches.push_back(matches[i]);
        }
    }

    vector<Point2f> obj;
    vector<Point2f> scene;

    for (int i = 0; i < good_matches.size(); i++) {
        obj.push_back(keypoints_object[good_matches[i].queryIdx].pt);
        scene.push_back(keypoints_scene[good_matches[i].trainIdx].pt);
    }

    Mat H = findHomography(obj, scene, CV_RANSAC);

    vector<Point2f> obj_corners(4);
    obj_corners[0] = cvPoint(0, 0);
    obj_corners[1] = cvPoint(img_obj.cols, 0);
    obj_corners[2] = cvPoint(img_obj.cols, img_obj.rows);
    obj_corners[3] = cvPoint(0, img_obj.rows);
    vector<Point2f> scene_corners(4);

    perspectiveTransform(obj_corners, scene_corners, H);
    line(img_sceneRGB, scene_corners[0], scene_corners[1], Scalar(0, 255, 0), 4);
    line(img_sceneRGB, scene_corners[1], scene_corners[2], Scalar(0, 255, 0), 4);
    line(img_sceneRGB, scene_corners[2], scene_corners[3], Scalar(0, 255, 0), 4);
    line(img_sceneRGB, scene_corners[3], scene_corners[0], Scalar(0, 255, 0), 4);
}

extern "C"
JNIEXPORT int JNICALL
Java_com_lge_project1_TestActivity_FindFeatures(JNIEnv *env, jobject instance, jlong matAddrObj, jlong presave_imageAddrScene, jlong img_matchesAddr) {

    // TODO
    Mat& img_obj  = *(Mat*)matAddrObj;
    Mat& img_scene = *(Mat*)presave_imageAddrScene;

    Mat img_matches = *(Mat*)img_matchesAddr;

    vector<KeyPoint> keypoints_object, keypoints_scene;
    Mat descriptors_object, descriptors_scene;

    if ( !img_obj.data || !img_scene.data) {
        LOGD("Error reading images");
    }

    int minHessian = 400;

    Ptr<ORB> orb = ORB::create(minHessian);

    orb->detectAndCompute(img_obj, Mat(), keypoints_object, descriptors_object);
    orb->detectAndCompute(img_scene, Mat(), keypoints_scene, descriptors_scene);

    BFMatcher matcher;
    vector<DMatch> matches;
    matcher.match(descriptors_object, descriptors_scene, matches, noArray());

    double max_dist = 0;
    double min_dist = 100;

    for (int i = 0; i < descriptors_object.rows; i++) {
        double dist = matches[i].distance;
        if (dist < min_dist) min_dist = dist;
        if (dist > max_dist) max_dist = dist;
    }

    vector<DMatch> good_matches;

    for (int i = 0; i < descriptors_object.rows; i++) {
        if (matches[i].distance < 3 * min_dist) {
            good_matches.push_back(matches[i]);
        }
    }

    drawMatches(img_obj, keypoints_object, img_scene, keypoints_scene,
                good_matches, img_matches, Scalar::all(-1), Scalar::all(-1),
                vector<char>(), DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS);

    vector<Point2f> obj;
    vector<Point2f> scene;

    for (int i = 0; i < good_matches.size(); i++) {
        obj.push_back(keypoints_object[good_matches[i].queryIdx].pt);
        scene.push_back(keypoints_scene[good_matches[i].trainIdx].pt);
    }

    Mat H = findHomography(obj, scene, CV_RANSAC);

    vector<Point2f> obj_corners(4);
    obj_corners[0] = cvPoint(0, 0);
    obj_corners[1] = cvPoint(img_obj.cols, 0);
    obj_corners[2] = cvPoint(img_obj.cols, img_obj.rows);
    obj_corners[3] = cvPoint(0, img_obj.rows);

    vector<Point2f> scene_corners(4);

    perspectiveTransform(obj_corners, scene_corners, H);
    line(img_matches, scene_corners[0] + Point2f(img_obj.cols, 0), scene_corners[1] + Point2f(img_obj.cols, 0), Scalar(0, 255, 0), 4);
    line(img_matches, scene_corners[1] + Point2f(img_obj.cols, 0), scene_corners[2] + Point2f(img_obj.cols, 0), Scalar(0, 255, 0), 4);
    line(img_matches, scene_corners[2] + Point2f(img_obj.cols, 0), scene_corners[3] + Point2f(img_obj.cols, 0), Scalar(0, 255, 0), 4);
    line(img_matches, scene_corners[3] + Point2f(img_obj.cols, 0), scene_corners[0] + Point2f(img_obj.cols, 0), Scalar(0, 255, 0), 4);

    if (!img_matches.empty()) {
        return 1;
    } else {
        return 0;
    }
}