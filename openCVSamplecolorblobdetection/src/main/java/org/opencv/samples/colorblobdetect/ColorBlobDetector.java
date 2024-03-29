package org.opencv.samples.colorblobdetect;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ColorBlobDetector {
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);

    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.3;

    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

    // Cache
    private Mat mPyrDownMat = new Mat();
    private Mat mHsvMat = new Mat();
    private Mat mMask = new Mat();
    private Mat mDilatedMask = new Mat();
    private Mat mHierarchy = new Mat();

    public void setHsvColor(Scalar hsvColor, Scalar minHsv) {
        double minH;
        double maxH;

        if (minHsv.val[0] - 10 < 0) {
            minH = 0;
        } else {
            minH = minHsv.val[0] - 10;
        }

        if (hsvColor.val[0] + 10 > 255) {
            maxH = hsvColor.val[0];
        } else {
            maxH = hsvColor.val[0] + 10;
        }

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = minHsv.val[1];
        mUpperBound.val[1] = hsvColor.val[1];

        mLowerBound.val[2] = minHsv.val[2];
        mUpperBound.val[2] = hsvColor.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        Mat spectrumHsv = new Mat(1, (int)(maxH-minH), CvType.CV_8UC3);

        for (int j = 0; j < maxH-minH; j++) {
            byte[] tmp = {(byte)(minH+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }

        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage, Scalar hsvColor, Scalar minHsv, boolean flipValues) {
        this.setHsvColor(hsvColor, minHsv);
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        if (flipValues) {
            Mat tempM = new Mat();
            Mat tempM2 = new Mat();
            Core.inRange(mHsvMat, new Scalar(0, ColorBlobDetectionActivity.ballMaxMinVS[3], ColorBlobDetectionActivity.ballMaxMinVS[2]), mLowerBound, tempM);
            Core.inRange(mHsvMat, mUpperBound, new Scalar(255, ColorBlobDetectionActivity.ballMaxMinVS[1], ColorBlobDetectionActivity.ballMaxMinVS[0]), tempM2);
            Core.bitwise_or(tempM, tempM2, mMask);

        } else {
            Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
        }

        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
