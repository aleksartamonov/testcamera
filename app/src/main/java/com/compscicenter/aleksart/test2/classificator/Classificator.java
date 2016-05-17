package com.compscicenter.aleksart.test2.classificator;

import android.support.annotation.NonNull;

import com.compscicenter.aleksart.test2.Descriptor;
import com.compscicenter.aleksart.test2.utils.TypePoint;

import org.opencv.core.KeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.CSVLoader;
import weka.core.converters.Loader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;

/**
 * Created by sergej on 5/17/16.
 */
public class Classificator {
    private Classifier rf;
    private final int MIN_GOOD_POINTS = 2;
    private final int batchSize = 100;
    public Classificator(InputStream inputStream) {
        try {
            rf = (RandomForest) SerializationHelper.read(inputStream);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public List<TypePoint> classify(@NonNull final List<Descriptor> descriptors) {

        StringBuilder sb = new StringBuilder();
        int number = 0;
        int count = 0;
        List<TypePoint> answer = new ArrayList<TypePoint>();
        try {
            while (true) {
                sb.append((descriptors.get(0).getHeader()));
                for (int i = number * batchSize; i < descriptors.size() && i < number * batchSize + batchSize; i++) {
                    sb.append(descriptors.get(i));
                    sb.append("\n");
                }
                InputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
//                System.out.println(sb.toString());
                sb.setLength(0);
                count += findKeyPoints(inputStream, answer);
                System.out.println(count + "..................");
                if (number * batchSize >= descriptors.size()) {
                    System.out.println("count = " + count + "  summary..." + descriptors.size());
                    break;
                }

                number++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (count < MIN_GOOD_POINTS) {
            throw new RuntimeException("dont find min count good points");
        }
        return answer;

    }
    private int findKeyPoints(InputStream input,List<TypePoint> answer) {

        int count = 0;
        try {
            Loader loader = new CSVLoader();
            loader.setSource(input);

            Instances data = loader.getDataSet();
            data.setClassIndex(0);

            NumericToNominal filter = new NumericToNominal();
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = "1";
            filter.setOptions(options);
            filter.setInputFormat(data);
            data = Filter.useFilter(data, filter);

            Scalar red = new Scalar(255, 0, 0);
            Scalar yellow = new Scalar(255, 255, 0);

            for (int i = 0; i < data.numInstances(); i++) {
                double result = rf.classifyInstance(data.get(i));
                if (result == 1) {
//                    Imgproc.circle(imageCV, keyPoints[start + i].pt, radius, red);
                    count++;
                    answer.add(TypePoint.GOOD_POINT);
                } else {
//                    Imgproc.circle(imageCV, keyPoints[start + i].pt, radius, yellow);
                    answer.add(TypePoint.BAD_POINT);
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Utils.matToBitmap(imageCV, bitmap);
        return count;
    }
}
