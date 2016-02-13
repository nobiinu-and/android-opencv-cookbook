package study.self.opencv_cookbook;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;

import com.camera.simplemjpeg.MjpegInputStream;
import com.camera.simplemjpeg.MjpegView;

public class MjpegDetectActivity extends AppCompatActivity {

    final String TAG = MjpegActivity.class.getSimpleName();

    MjpegStreamOperator mStreamOperator;

    class SetupMjpegStreamTask extends AsyncTask<String, Void, MjpegInputStream> {
        final String TAG = SetupMjpegStreamTask.class.getSimpleName();

        @Override
        protected MjpegInputStream doInBackground(String... urls) {
            Log.d(TAG, "doInBackground url:" + urls[0]);
            return MjpegInputStream.read(urls[0]);
        }

        @Override protected void onPostExecute(MjpegInputStream result) {
            Log.d(TAG, "onPostExecute");
            MjpegView view = (MjpegView)findViewById(R.id.mjpegdetect_preview);
            view.setSource(result);
        }
    }

    public class MjpegStreamOperator implements Runnable {

        private final String TAG = MjpegStreamOperator.class.getSimpleName();

        private SurfaceHolder mPreview;
        private SurfaceHolder mResult;
        private String mUrl;
        private volatile boolean done = false;

        public MjpegStreamOperator(SurfaceHolder preview, SurfaceHolder result, String url) {
            mPreview = preview;
            mResult = result;
            mUrl = url;
        }

        @Override
        public synchronized void run() {
            Log.d(TAG, "Thread start");
            while (!done) {
                Log.d(TAG, "Read from stream");
            }
        }

        public void shutdown() {
            Log.d(TAG, "Shutdown");
            done = true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mjpeg_detect);
    }

    public void playMovie(View view) {
        EditText editURL = (EditText)findViewById(R.id.mjpegdetect_movie_url);
        String url = editURL.getText().toString();

        if (url == null || url.isEmpty()) {
            Log.d(TAG, "URL is empty");
            return;
        }

        Log.d(TAG, "URL:" + url);

        MjpegView preview = (MjpegView)findViewById(R.id.mjpegdetect_preview);
        preview.setResolution(320, 240);

        SurfaceView result = (SurfaceView)findViewById(R.id.mjpegdetect_result);

        new SetupMjpegStreamTask().execute(url);

        MjpegStreamOperator mjpegStreamOperator = new MjpegStreamOperator(preview.getHolder(), result.getHolder(), url);
        Thread thread = new Thread(mjpegStreamOperator);
        thread.start();
    }

    public void stopMovie(View view) {
        mStreamOperator.shutdown();
    }
}
