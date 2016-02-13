package study.self.opencv_cookbook;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.camera.simplemjpeg.MjpegInputStream;
import com.camera.simplemjpeg.MjpegView;

// https://bitbucket.org/neuralassembly/simplemjpegview を使ってMjpegを表示するプログラム
//
// 現在画面を切り替えるとExceptionが発生します (スレッドをうまく止められてない...)
public class MjpegActivity extends AppCompatActivity {

    private static final String TAG = MjpegActivity.class.getSimpleName();

    class SetupMjpegStreamTask extends AsyncTask<String, Void, MjpegInputStream> {
        final String TAG = SetupMjpegStreamTask.class.getSimpleName();

        @Override
        protected MjpegInputStream doInBackground(String... urls) {
            Log.d(TAG, "doInBackground url:" + urls[0]);
            return MjpegInputStream.read(urls[0]);
        }

        @Override protected void onPostExecute(MjpegInputStream result) {
            Log.d(TAG, "onPostExecute");
            MjpegView view = (MjpegView)findViewById(R.id.mjpeg_preview);
            view.setSource(result);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mjpeg);
    }

    public void playMovie(View view) {
        EditText editURL = (EditText)findViewById(R.id.mjpeg_movie_url);
        String url = editURL.getText().toString();

        if (url == null || url.isEmpty()) {
            Log.d(TAG, "URL is empty");
            return;
        }

        Log.d(TAG, "URL:" + url);

        MjpegView mjpegView = (MjpegView)findViewById(R.id.mjpeg_preview);
        mjpegView.setResolution(320, 240);

        new SetupMjpegStreamTask().execute(url);
    }
}
