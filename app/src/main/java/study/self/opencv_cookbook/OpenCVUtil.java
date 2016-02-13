package study.self.opencv_cookbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.support.annotation.RawRes;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenCVUtil {

    private static final String TAG = OpenCVUtil.class.getSimpleName();

    public static CascadeClassifier loadCascadeClassifier(Context appContext, @RawRes int id) throws IOException, IllegalArgumentException {
        try {
            /**
             * この辺のファイルをopencv/sdk/etc/haarcascades もしくは、opencv/sdk/etc/lbpcascadesにある
             * xmlファイルと書き換えると、検出する場所が変わるっぽい
             *
             * 1. res/raw/に、検出したいファイルを置く。
             * 2. InputStream isとmCascadeFileの引数をファイル名に変える
             *
             *
             */
            // load cascade file from application resources

            File cascadeFile;
            InputStream is = appContext.getResources().openRawResource(id);

            File cascadeDir = appContext.getDir("cascade", Context.MODE_PRIVATE);
            cascadeFile = new File(cascadeDir, "target.xml");

            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            CascadeClassifier cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());

            if (cascadeClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                throw new IllegalArgumentException("Failed to load cascade classifier");
            } else {
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            }

            cascadeDir.delete();

            return cascadeClassifier;

        } catch (IOException ex) {
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + ex);
            throw ex;
        }
    }

    public static Mat convertYuv2Mat(byte[] data, int width, int height) {

        Mat yuvMat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        yuvMat.put(0, 0, data);
        Mat bmpMat = new Mat();
        Imgproc.cvtColor(yuvMat, bmpMat, Imgproc.COLOR_YUV420sp2RGB, 4);

        return bmpMat;
    }
}
