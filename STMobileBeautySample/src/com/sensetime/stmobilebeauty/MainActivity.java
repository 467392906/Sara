package com.sensetime.stmobilebeauty;

import com.sensetime.stmobilesample.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

    @Override public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_gallery).setOnClickListener(this);
        findViewById(R.id.button_camera).setOnClickListener(this);
    }

    @Override public void onClick(final View v) {
         startActivity(v.getId());
    }

    private void startActivity(int id) {
        switch (id) {
            case R.id.button_gallery:
                startActivity(new Intent(this, GalleryActivity.class));
                break;
            case R.id.button_camera:
                startActivity(new Intent(this, CameraActivity.class));
                break;

            default:
                break;
        }
    }
}
