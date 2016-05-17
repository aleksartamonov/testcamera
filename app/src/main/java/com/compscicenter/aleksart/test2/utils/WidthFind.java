package com.compscicenter.aleksart.test2.utils;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by egor on 09.05.16.
 */
public class WidthFind {
    public static Mat findLines(Mat img) {
        Mat gray = new Mat();
        Mat hsv = new Mat();
        Mat hsvgray = new Mat();
        Imgproc.cvtColor(img,hsv,Imgproc.COLOR_BGR2HSV);
        Imgproc.cvtColor(hsv,hsvgray,Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray, gray, 192, 255, 0);
        Imgproc.blur(gray, gray, new Size(5, 5));
        Imgproc.Laplacian(gray, gray, 0);
        Imgproc.threshold(gray, gray, 5, 255, 0);
        Imgproc.threshold(hsvgray, hsvgray, 128, 255, 0);
        Imgproc.blur(hsvgray, hsvgray, new Size(5, 5));
        Imgproc.Laplacian(hsvgray, hsvgray, 0);
        return gray;
    }
}
