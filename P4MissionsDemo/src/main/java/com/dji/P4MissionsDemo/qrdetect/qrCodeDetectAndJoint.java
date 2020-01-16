/**
 * Created by wbzhang on 2019/7/15.
 */
package com.dji.P4MissionsDemo.qrdetect;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

public interface qrCodeDetectAndJoint {
    void loadPics();
    Point getCenter(MatOfPoint contour);
    double eulerDist(Point point1,Point point2);
    double calRoundness(MatOfPoint contour);
    List<Mat> detectAndJoint();
    Boolean detectAndFetch();
    List<Mat> imgProcess(Mat pic);
    qrCodeDetect.fetchPack fetchBrokenCode(Mat thresholdOpen10);
    qrCodeDetect.fetchPack findBrokenCode(Mat thresholdOpen2);
    Boolean roiCheck();
    Point findFixedPts(Point[] rtPts, int order, Point fixedPt);
    Point[] sortOrder(Point[] pts);
    List<Integer> childContours(ArrayList<MatOfPoint> contours, Mat hierarchy);
    int getCornerOrder(MatOfPoint2f contour);
    Point[] getPuryPts(MatOfPoint2f contour, int order);
    void fetchPuryCode();
    Mat bruteJoint();
}
