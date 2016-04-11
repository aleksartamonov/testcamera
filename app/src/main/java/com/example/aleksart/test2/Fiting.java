package com.example.aleksart.test2;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Random;

/**
 * Created by sergej on 4/2/16.
 */
public class Fiting {

    public static double count(Mat img, Line line, Scalar colour,int width) {
        Mat temp = img.clone();

//        System.out.println(line.getP1());
//        System.out.println(line.getP2());

//        Imgproc.line(temp, line.getP1(), line.getP2(), colour, width);

        Scalar response = Core.sumElems(temp);
        System.out.println("scalar");
//        System.out.println("after sumElems");

        return response.val[0];
    }




    public static Line[] improve(Line[] lines, Mat img) {

        Imgproc.threshold(img, img, 120, 255, 0);
        Imgproc.Laplacian(img, img,0);


        Mat thresh = new Mat();
        Imgproc.threshold(img, thresh, 192, 255, 0);

        Line[] directions = {
                new Line(1, 0, 0, 0),
                new Line(0, 1, 0, 0),
                new Line(0, 0, 1, 0),
                new Line(0, 0, 0, 1),
                new Line(-1, 0, 0, 0),
                new Line(0, -1, 0, 0),
                new Line(0, 0, -1, 0),
                new Line(0, 0, 0, -1),
                new Line(1, 1, 0, 0),
                new Line(0, 0, 1, 1),
                new Line(-1, -1, 0, 0),
                new Line(0, 0, -1, -1),
                new Line(1, -1, 0, 0),
                new Line(-1, 1, 0, 0),
                new Line(0, 0, 1, -1),
                new Line(2, 0, 0, 0),
                new Line(0, 2, 0, 0),
                new Line(0, 0, 2, 0),
                new Line(0, 0, 0, 2),
                new Line(-2, 0, 0, 0),
                new Line(0, -2, 0, 0),
                new Line(0, 0, -2, 0),
                new Line(0, 0, 0, -2),
                new Line(3, 0, 0, 0),
                new Line(0, 3, 0, 0),
                new Line(0, 0, 3, 0),
                new Line(0, 0, 0, 3),
                new Line(-3, 0, 0, 0),
                new Line(0, -3, 0, 0),
                new Line(0, 0, -3, 0),
                new Line(0, 0, 0, -3)
        };
        Scalar colour = new Scalar(70);
        Random random = new Random();
//        for (int i = 0;i < lines.length; i++) {
//            double best = count(thresh, lines[i],colour,1);
//            double delta = 1;
//            while (delta > 0) {
//                delta = 0;
//                for (int j = 0; j < 10; j++) {
//                    Line direction = directions[random.nextInt(lines.length)];
//                    double resp = count(thresh, lines[i].add(direction),colour,1);
//                    if (resp<best) {
//                        delta = Math.max(best - resp, delta);
//                        best = resp;
//                        lines[i].add(direction);
//                    }
//                }
//                System.out.println(delta);
//            }
//
//        }


        return lines;
    }
}
