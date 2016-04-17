package com.example.aleksart.test2;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by sergej on 4/2/16.
 */
public class Fiting {

    public static double count(Mat img, Line line, Scalar colour,int width) {
        Mat temp = img.clone();

//        System.out.println(line.getP1());
//        System.out.println(line.getP2());

        Imgproc.line(temp, line.getP1(), line.getP2(), colour, width);

        Scalar response = Core.sumElems(temp);
//        System.out.println("after sumElems");
        temp = null;
        return response.val[0];
    }




    public static Mat  improve(List<Line> lines, Mat img) {

        Imgproc.threshold(img, img, 120, 255, 0);
        Imgproc.Laplacian(img, img, 0);


        Mat thresh = new Mat();
        Imgproc.threshold(img, thresh, 192, 255, 0);
//        Imgproc.blur(thresh, thresh, new Size(5, 5));
//        Imgproc.threshold(img, thresh, 32, 255, 0);

        int k = 3;
        List<Line> directions = new ArrayList<Line>();
        for (int i1 = -k;i1 < k+1;i1 += 1) {
            for (int i2 = -k;i2 < k+1;i2 += 1) {
                directions.add(new Line(i1, 0, i2, 0));
            }
        }
        for (int i1 = -k;i1 < k+1;i1 += 1) {
            for (int i2 = -k;i2 < k+1;i2 += 1) {
                directions.add(new Line(0, i1, 0, i2));
            }
        }
        Scalar colour = new Scalar(70);
        Random random = new Random();
        for (int i = 0;i < lines.size(); i++) {

            double min = 1e20;
            Line bestLine = new Line(0,0,0,0);
            for (int j = 0; j < directions.size(); j++) {
                if (count(thresh, lines.get(i).add1(directions.get(j)), colour, 1) < min) {
                    min = count(thresh, lines.get(i).add1(directions.get(j)), colour, 1);
                    bestLine = lines.get(i).add1(directions.get(j));
                }
            }
            lines.set(i, bestLine);
        }









//                    double best = count(thresh, lines.get(i),colour,1);
//            double delta = 1;
//            while (delta > 0) {
//                delta = 0;
//                for (int j = 0; j < 20; j++) {
//                    Line direction = directions.get(directions.size());
//                    double resp = count(thresh, lines.get(i).add1(direction),colour,1);
//                    if (resp<best) {
//                        System.out.println("Scalar " + resp);
//                        delta = best - resp;
//                        best = resp;
//                        lines.set(i, lines.get(i).add1(direction));
//                    }
//                }
//                System.out.println(delta);
//            }
//
//        }


        return thresh;
    }
}
