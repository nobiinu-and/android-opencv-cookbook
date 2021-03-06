package study.self.opencv_cookbook;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import com.camera.simplemjpeg.MjpegInputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MjpegDetectActivity extends AppCompatActivity {

    final String TAG = MjpegActivity.class.getSimpleName();

    Thread mStreamThread;
    MjpegStreamOperator mStreamOperator;

    SurfaceView mPreviewView;
    SurfaceView mResultView;
    Spinner mResolutions;

    Context mAppContext;

    class Resolution {
        public int mWidth;
        public int mHeight;

        public Resolution(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public String toString() {
            return "" + mWidth + "x" + mHeight;
        }
    }

    class SetupMjpegStreamTask extends AsyncTask<String, Void, MjpegInputStream> {
        final String TAG = SetupMjpegStreamTask.class.getSimpleName();

        @Override
        protected MjpegInputStream doInBackground(String... urls) {
            Log.d(TAG, "doInBackground url:" + urls[0]);
            return MjpegInputStream.read(urls[0]);
        }

        @Override protected void onPostExecute(MjpegInputStream result) {
            Log.d(TAG, "onPostExecute");

            Resolution selectedRes = (Resolution)mResolutions.getSelectedItem();

            Log.d(TAG, "selected resolution:" + selectedRes.toString());

            mStreamOperator = new MjpegStreamOperator(mAppContext, result, selectedRes.mWidth, selectedRes.mHeight, mPreviewView.getHolder(), mResultView.getHolder());
            mStreamThread = new Thread(mStreamOperator);
            mStreamThread.start();
        }
    }

    public class MjpegStreamOperator implements Runnable {

        private final String TAG = MjpegStreamOperator.class.getSimpleName();
        private final int UPDATE_INTERVAL_DEFAULT = 1000;

        private SurfaceHolder mPreview;
        private Rect mPrevieRect;

        private SurfaceHolder mResult;
        private Rect mResultRect;

        private MjpegInputStream mStream;

        private Bitmap mBitmapCurrent;
        private Mat mMatCurrent;

        private Bitmap mBitmapResult;
        private Mat mMatResult;

        private Mat mMatTemplateLady;

        private boolean mDone = false;

        public MjpegStreamOperator(Context context, MjpegInputStream stream, int streamWidth, int streamHeght, SurfaceHolder preview, SurfaceHolder result) {
            mPreview = preview;
            mResult = result;
            mStream = stream;

            mPrevieRect = preview.getSurfaceFrame();
            mResultRect = result.getSurfaceFrame();

            mBitmapCurrent = Bitmap.createBitmap(streamWidth, streamHeght, Bitmap.Config.ARGB_8888);
            mMatCurrent = new Mat();

            mBitmapResult = Bitmap.createBitmap(streamWidth, streamHeght, Bitmap.Config.ARGB_8888);
            mMatResult = new Mat();

            mMatTemplateLady = new Mat();
            Bitmap template = BitmapFactory.decodeResource(context.getResources(), R.raw.toire_pctglm_lady);
            template = Bitmap.createScaledBitmap(template, 40, 90, false);
            Utils.bitmapToMat(template, mMatTemplateLady);
        }

        @Override
        public synchronized void run() {
            Log.d(TAG, "Thread start");
            while (!mDone) {
                Log.d(TAG, "Read from stream");

                int ret = 0;
                try {
                    ret = mStream.readMjpegFrame(mBitmapCurrent);
                } catch (IOException e) {
                    Log.e(TAG, "readMjpegFrame error", e);
                }

                if (ret == -1) {
                    Log.e(TAG, "readMjpegFrame error");
                    return;
                }

                // Previewへの画像表示
                drawBitmapToCanvas(mBitmapCurrent, mPreview);

                // グレイスケール変換
                Utils.bitmapToMat(mBitmapCurrent, mMatCurrent);
                //Imgproc.cvtColor(mMatCurrent, mMatResult, Imgproc.COLOR_BGR2GRAY);
                mMatResult = mMatCurrent.clone();

                Mat matDetectResult = new Mat();
                Imgproc.matchTemplate(mMatCurrent, mMatTemplateLady, matDetectResult, Imgproc.TM_CCOEFF_NORMED);
                Core.MinMaxLocResult detectedMax = Core.minMaxLoc(matDetectResult);
                org.opencv.core.Point locLF = detectedMax.maxLoc;
                org.opencv.core.Point locBR = new Point(locLF.x + mMatTemplateLady.cols(), locLF.y + mMatTemplateLady.rows());
                Imgproc.rectangle(mMatResult, locLF, locBR, new Scalar(255, 0, 0, 255), 5);

                Utils.matToBitmap(mMatResult, mBitmapResult);

                // Resultへの画像描画
                drawBitmapToCanvas(mBitmapResult, mResult);

                try {
                    Thread.sleep(UPDATE_INTERVAL_DEFAULT);
                } catch (InterruptedException e) {
                    // 何もしない
                }
            }
        }

        public void shutdown() {
            Log.d(TAG, "Shutdown");
            mDone = true;
        }

        private void drawBitmapToCanvas(Bitmap source, SurfaceHolder target) {
            Rect sourceRect = new Rect(0, 0, source.getWidth(), source.getHeight());
            Rect targetRect = target.getSurfaceFrame();
            Canvas canvas = target.lockCanvas();
            canvas.drawBitmap(source, sourceRect, targetRect, null);
            target.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mjpeg_detect);

        List<Resolution> resolutions = new ArrayList<Resolution>();
        resolutions.add(new Resolution(320, 240));
        resolutions.add(new Resolution(640, 480));

        ArrayAdapter<Resolution> resAdapter = new ArrayAdapter<Resolution>(this, R.layout.support_simple_spinner_dropdown_item, resolutions);

        mResolutions = (Spinner)findViewById(R.id.mjpegdetect_resolutions);
        mResolutions.setAdapter(resAdapter);

        mAppContext = this;
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        mPreviewView = (SurfaceView)findViewById(R.id.mjpegdetect_preview);
        mResultView = (SurfaceView)findViewById(R.id.mjpegdetect_result);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onStop");
        super.onPause();

        if (mStreamOperator != null) {
            mStreamOperator.shutdown();
        }
        if (mStreamThread != null) {
            try {
                mStreamThread.join();
            } catch (InterruptedException e) {
                // 何もしない
            }
        }
    }

    public void playMovie(View view) {
        EditText editURL = (EditText)findViewById(R.id.mjpegdetect_movie_url);
        String url = editURL.getText().toString();

        if (url == null || url.isEmpty()) {
            Log.d(TAG, "URL is empty");
            return;
        }

        Log.d(TAG, "URL:" + url);

        if (mStreamOperator != null) {
            mStreamOperator.shutdown();
            try {
                mStreamThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "thread join failed", e);
            }
        }

        new SetupMjpegStreamTask().execute(url);
    }

    public void stopMovie(View view) {
        mStreamOperator.shutdown();
    }

    public void BtnOmake(View view) {
        EditText editURL = (EditText) findViewById(R.id.mjpeg_webiopi_url);
        String tmp = editURL.getText().toString();
        Uri url = Uri.parse(tmp);

        Log.d(TAG, "URL:" + url);
        Intent i = new Intent(Intent.ACTION_VIEW, url);
        startActivity(i);
    }

}
