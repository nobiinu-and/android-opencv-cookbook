package study.self.opencv_cookbook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class CameraTemplateMachActivity extends AppCompatActivity {

    private final String TAG = CameraTemplateMachActivity.class.getSimpleName();

    private SurfaceHolder mSurfaceHodlerChanged;

    private Mat mMatChanged;
    private Mat mMatTemplateMan;
    private Mat mMatTemplateLady;
    private Bitmap mBitmapChanged;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    // 検出のたびに、Matを生成しているとコストがかかりそうなので、事前に初期化
                    // MatはOpenCVの初期化が終わってからでないとnewできない(はず)なので、ここで初期化する
                    mMatChanged = new Mat();

                    Bitmap template;

                    mMatTemplateMan = new Mat();
                    template = BitmapFactory.decodeResource(this.mAppContext.getResources(), R.raw.toire_pctglm_man);
                    template = Bitmap.createScaledBitmap(template, 20, 45, false);
                    Utils.bitmapToMat(template, mMatTemplateMan);

                    mMatTemplateLady = new Mat();
                    template = BitmapFactory.decodeResource(this.mAppContext.getResources(), R.raw.toire_pctglm_lady);
                    template = Bitmap.createScaledBitmap(template, 20, 45, false);
                    Utils.bitmapToMat(template, mMatTemplateLady);

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
            mCamera = Camera.open();

            Log.d(TAG, "camera width:" + mCamera.getParameters().getPreviewSize().width);
            Log.d(TAG, "camera height:" + mCamera.getParameters().getPreviewSize().height);

            // 検出時に利用するBitmapを毎回作成すると時間がかかるので事前に初期化しておく
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

            mMatChanged = matBase.clone();

            Mat matDetectResult;
            Core.MinMaxLocResult detectedMax;
            Point locLF, locBR;

            // 男検出
            matDetectResult = new Mat();
            Imgproc.matchTemplate(matBase, mMatTemplateMan, matDetectResult, Imgproc.TM_CCOEFF_NORMED);
            detectedMax = Core.minMaxLoc(matDetectResult);
            locLF = detectedMax.maxLoc;
            locBR = new Point(locLF.x + mMatTemplateMan.width(), locLF.y + mMatTemplateMan.height());
            Imgproc.rectangle(mMatChanged, locLF, locBR, new Scalar(0, 0, 255), 2);

            // 女検出
            Imgproc.matchTemplate(matBase, mMatTemplateLady, matDetectResult, Imgproc.TM_CCOEFF_NORMED);
            detectedMax = Core.minMaxLoc(matDetectResult);
            locLF = detectedMax.maxLoc;
            locBR = new Point(locLF.x + mMatTemplateLady.width(), locLF.y + mMatTemplateLady.height());
            Imgproc.rectangle(mMatChanged, locLF, locBR, new Scalar(255, 0, 0), 2);

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
        setContentView(R.layout.activity_camera_template_mach);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);

        // SurfaceViewにコールバックを設定する
        SurfaceView surfacePreview = (SurfaceView)findViewById(R.id.cameratemplatematch_preview);
        SurfaceHolder holder = surfacePreview.getHolder();
        holder.addCallback(mSurfacePreviewCallback);

        SurfaceView surfaceChanged = (SurfaceView)findViewById(R.id.cameratemplatematch_result);
        mSurfaceHodlerChanged = surfaceChanged.getHolder();
    }
}
