package study.self.opencv_cookbook;

import android.content.Context;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class ColorInfoActivity extends AppCompatActivity {

    private static final String TAG = ColorInfoActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_info);

        CameraSurfaceView view = (CameraSurfaceView)findViewById(R.id.colorinfo_preview);
        view.setColorInfoView((TextView)findViewById(R.id.colorinfo_color_info));
    }
}
