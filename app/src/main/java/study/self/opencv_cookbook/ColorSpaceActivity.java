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
import org.opencv.imgproc.Imgproc;

// カメラ
public class ColorSpaceActivity extends AppCompatActivity {

    private final String TAG = ColorSpaceActivity.class.getSimpleName();

    private SurfaceHolder mSurfaceHodlerChanged;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
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

            // HSVに変換する
            Mat matHsv = new Mat();
            Imgproc.cvtColor(matBase, matHsv, Imgproc.COLOR_BGR2HSV);
            Bitmap bmpChanged = Bitmap.createBitmap(cameraSize.width, cameraSize.height, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(matHsv, bmpChanged);

            // 変換結果を描画する
            Rect source = new Rect(0, 0, bmpChanged.getWidth(), bmpChanged.getHeight());
            Rect dest = mSurfaceHodlerChanged.getSurfaceFrame();
            Canvas canvas = mSurfaceHodlerChanged.lockCanvas();
            canvas.drawBitmap(bmpChanged, source, dest, null);
            mSurfaceHodlerChanged.unlockCanvasAndPost(canvas);

            camera.setPreviewCallback(this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_color_space);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

        // SurfaceViewにコールバックを設定する
        SurfaceView surfacePreview = (SurfaceView)findViewById(R.id.changecolorspace_preview);
        SurfaceHolder holder = surfacePreview.getHolder();
        holder.addCallback(mSurfacePreviewCallback);

        SurfaceView surfaceChanged = (SurfaceView)findViewById(R.id.changecolorspace_changed);
        mSurfaceHodlerChanged = surfaceChanged.getHolder();
    }
}
