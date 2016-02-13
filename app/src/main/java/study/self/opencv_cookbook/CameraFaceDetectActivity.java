package study.self.opencv_cookbook;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;

public class CameraFaceDetectActivity extends AppCompatActivity {

    private final String TAG = CameraFaceDetectActivity.class.getSimpleName();

    private SurfaceHolder mSurfaceHodlerChanged;
    private CascadeClassifier mDetector;

    private Mat mMatChanged;
    private Bitmap mBitmapChanged;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    try {
                        mDetector = OpenCVUtil.loadCascadeClassifier(this.mAppContext, R.raw.lbpcascade_frontalface);

                        mMatChanged = new Mat();

                    } catch (IOException e) {
                        Log.e(TAG, "onManagerConnected: loadCascadeCassifier failed", e);
                    }

                    Log.i(TAG, "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private SurfaceHolder.Callback mSurfacePreviewCallback = new SurfaceHolder.Callback() {
        Camera mCamera;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surface created");

            //CameraOpen
            mCamera = Camera.open(0);

            Log.d(TAG, "camera width:" + mCamera.getParameters().getPreviewSize().width);
            Log.d(TAG, "camera height:" + mCamera.getParameters().getPreviewSize().height);

            Camera.Size cameraSize = mCamera.getParameters().getPictureSize();
            mBitmapChanged = Bitmap.createBitmap(cameraSize.width, cameraSize.height, Bitmap.Config.ARGB_8888);

            //出力をSurfaceViewに設定
            try{
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(mCameraPreviewCallback);
            }catch(Exception ex){
                Log.e(TAG, "Failed to setup camera", ex);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surface changed");

            //プレビュースタート（Changedは最初にも1度は呼ばれる）
            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surface destroyed");
            //片付け
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    };

    private Camera.PreviewCallback mCameraPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            camera.setPreviewCallback(null);

            // CameraのデータはYUV形式なので、それをMatに変換する
            Camera.Size cameraSize = camera.getParameters().getPictureSize();
            Mat matBase = OpenCVUtil.convertYuv2Mat(data, cameraSize.width, cameraSize.height);

            // GrayScaleに変換する
            Imgproc.cvtColor(matBase, mMatChanged, Imgproc.COLOR_BGR2GRAY);

            // 顔を検出する
            MatOfRect faces = new MatOfRect();
            int absoluteFaceSize = 0;
            mDetector.detectMultiScale(
                    mMatChanged
                    , faces
                    , 1.1
                    , 2
                    , 2 // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    , new Size(absoluteFaceSize, absoluteFaceSize)
                    , new Size());

            // 検出結果を描画する
            org.opencv.core.Rect[] facesArray = faces.toArray();
            Scalar faceRectColor = new Scalar(255, 255, 255, 255);
            for (int i = 0; i < facesArray.length; i++) {
                Imgproc.rectangle(mMatChanged, facesArray[i].tl(), facesArray[i].br(), faceRectColor, 3);
            }

            // 変換結果を描画する
            Utils.matToBitmap(mMatChanged, mBitmapChanged);
            drawBitmap(mBitmapChanged, mSurfaceHodlerChanged);

            camera.setPreviewCallback(this);
        }

        // 指定したSurfaceにフィットするようBitmapを描画する
        private void drawBitmap(Bitmap target, SurfaceHolder holder) {
            Rect source = new Rect(0, 0, target.getWidth(), target.getHeight());
            Rect dest = holder.getSurfaceFrame();
            Canvas canvas = holder.lockCanvas();
            canvas.drawBitmap(target, source, dest, null);
            holder.unlockCanvasAndPost(canvas);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_face_detect);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

        // SurfaceViewにコールバックを設定する
        SurfaceView surfacePreview = (SurfaceView)findViewById(R.id.camerafacedetect_preview);
        SurfaceHolder holder = surfacePreview.getHolder();
        holder.addCallback(mSurfacePreviewCallback);

        SurfaceView surfaceChanged = (SurfaceView)findViewById(R.id.camerafacedetect_result);
        mSurfaceHodlerChanged = surfaceChanged.getHolder();
    }
}
