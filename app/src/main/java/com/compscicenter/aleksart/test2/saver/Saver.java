package com.compscicenter.aleksart.test2.saver;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by sergej on 5/17/16.
 */
public class Saver {
    private static String folderToSave;

    public static void savePhoto(Bitmap bitmap) {
        FileOutputStream fOut = null;
        System.out.println("save photo +" +folderToSave);
        try {

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            File file = new File(folderToSave, timeStamp + ".jpg");
            fOut = new FileOutputStream(file);
//            text.setText(file.getAbsolutePath());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void saveMat(Mat imageCV) {
        Bitmap targetBitmap = Bitmap.createBitmap(imageCV.width(), imageCV.height(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageCV, targetBitmap);
        Saver.savePhoto(targetBitmap);
    }

    public static void setFolderToSave(String path) {
        folderToSave = path;
    }
}
