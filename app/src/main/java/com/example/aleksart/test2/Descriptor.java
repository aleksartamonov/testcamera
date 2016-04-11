package com.example.aleksart.test2;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergej on 3/26/16.
 */
public class Descriptor {
    private List<Integer> data;


    public Descriptor(List<Integer> e) {
        data = e;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size() - 1; i++) {
            sb.append(data.get(i));
            sb.append(",");
        }
        sb.append(data.get(data.size() - 1));
        return sb.toString();
    }
    public static List<Descriptor> getDescriptors(Bitmap thumbnail,FeatureDetector fd,MatOfKeyPoint points ) {
        Mat imageCV = new Mat();
        Utils.bitmapToMat(thumbnail, imageCV);
//        Imgproc.resize(imageCV,imageCV,new Size(300,400));

        Imgproc.cvtColor(imageCV, imageCV, Imgproc.COLOR_RGB2HSV_FULL);

        fd.detect(imageCV, points);


        List<Descriptor> result = new ArrayList<Descriptor>();
        int hist_size = 15;
        KeyPoint[] keyPoints = points.toArray();
        int count = 0;
        for (int i = 0;i < keyPoints.length; i++) {

            List<Integer> d = new ArrayList<Integer>();
            d.add(count % 2);
            count++;
            d.add((int)keyPoints[i].response);
            d.add((int)keyPoints[i].size);
            int minx = Math.max((int) keyPoints[i].pt.x - hist_size, 0);
            int miny = Math.max((int) keyPoints[i].pt.y - hist_size, 0);
            int maxx = Math.min((int) keyPoints[i].pt.x + hist_size, imageCV.width());
            int maxy = Math.min((int) keyPoints[i].pt.y + hist_size, imageCV.height());

            Rect roi = new Rect(minx, miny, maxx - minx, maxy - miny);
            Mat cropped = new Mat(imageCV, roi);
            List<Mat> list = new ArrayList<Mat>();
            list.add(cropped);

            MatOfInt[] channels = new MatOfInt[3];
            Mat[] hists = new Mat[3];
            MatOfInt histSize = new MatOfInt(8);
            MatOfFloat ranges = new MatOfFloat(0f,256.0f);
            for (int j = 0; j < hists.length;j++) {
                channels[j] = new MatOfInt(j);
                hists[j] = new Mat();
                Imgproc.calcHist(list, channels[j], new Mat(), hists[j], histSize, ranges);
                for (int p = 0; p < hists[j].height(); p++) {
                    d.add((int)hists[j].get(p, 0)[0]);
                }
            }
            result.add(new Descriptor(d));

        }
        return result;
    }

    public String getHeader() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size() - 1; i++) {
            sb.append("a");
            sb.append(i);
            sb.append(",");
        }
        sb.append("a");
        sb.append(data.size() - 1);
        sb.append("\n");
        return sb.toString();
    }

}
