package study.self.opencv_cookbook;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// Surfaceにカメラからの動画を表示する
// 今更カメラ制御の基礎（STEP1:とりあえずプレビュー表示）
// http://qiita.com/zaburo/items/d9d07eb4d87d21308124
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = CameraActivity.class.getSimpleName();

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // SurfaceViewにコールバックを設定する
        SurfaceView surfaceView = (SurfaceView)findViewById(R.id.camera_camera_preview);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(mSurfaceCallback);
    }
}
