package study.self.opencv_cookbook;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

// タッチイベントを補足するため、SurfaceViewを継承
// ついでにカメラ画像表示もする
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = CameraSurfaceView.class.getSimpleName();

    Camera mCamera;

    Bitmap mBitmap;
    TextView mColorInfoView;

    public CameraSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        SurfaceHolder holder = this.getHolder();
        holder.addCallback(this);
    }

    public void setColorInfoView(TextView view) {
        this.mColorInfoView = view;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surface created");

        //CameraOpen
        mCamera = Camera.open();

        Camera.Size size = mCamera.getParameters().getPictureSize();

        Log.d(TAG, "camera width:" + size.width);
        Log.d(TAG, "camera height:" + size.height);

        mCamera.setPreviewCallback(this);
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

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // getX, getYはSurfaceViewを基準とした座標
        Log.d(TAG, "onTouchEvent x:" + (int) ev.getX() + " y:" + (int) ev.getY());

        // 画面は拡張されているので画像サイズの座標に変換する
        float scale = (float)mBitmap.getWidth() / (float)this.getWidth();
        int x = (int)(ev.getX() * scale);
        int y = (int)(ev.getY() * scale);

        Log.d(TAG, "bitmap size width:" + mBitmap.getWidth() + " height:" + mBitmap.getHeight());

        int pixel = mBitmap.getPixel(x, y);
        Log.d(TAG, "location x:" + x + " y:" + y);
        Log.d(TAG, "pixel red  :" + Color.red((pixel)));
        Log.d(TAG, "pixel green:" + Color.green((pixel)));
        Log.d(TAG, "pixel blue :" + Color.blue((pixel)));

        if (mColorInfoView != null) {
            float[] hsv = new float[3];
            Color.colorToHSV(pixel, hsv);

            String colorInfo =
                    "R:" + Color.red(pixel) + " G:" + Color.green(pixel) + " B:" + Color.blue(pixel) +
                    "\n" +
                    "H:" + hsv[0] + " S:" + hsv[1] + " V:" + hsv[2];

            mColorInfoView.setText(colorInfo);
        }

        return true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.setPreviewCallback(null);

        Camera.Size size = mCamera.getParameters().getPictureSize();
        Bitmap bitmap = ImageUtil.convertYuv2Bitmap(data, size.width, size.height);

        Canvas canvas = this.getHolder().lockCanvas();
        Rect source = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        Rect dest = this.getHolder().getSurfaceFrame();
        canvas.drawBitmap(bitmap, source, dest, null);
        this.getHolder().unlockCanvasAndPost(canvas);

        // SurfaceViewから直接Bitmapが取れないようなので、
        // 描画元のBitmapを保存しておく
        mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        camera.setPreviewCallback(this);
    }
}