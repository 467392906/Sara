package com.sample.multitrack106;

import com.sample.multitrack106.FaceOverlapFragment;

import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * 
 * @author MatrixCV
 *
 * Activity
 * 
 */
public class MultitrackerActivity extends Activity
 {
	private final static int CAMERA_REQUEST_CODE = 0x111;
	static MultitrackerActivity instance = null;
    TextView fpstText, actionText;
	
	/**
	 * 
	 * 重力传感器
	 * 
	 */
	static Accelerometer acc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_multitracker);
		fpstText = (TextView)findViewById(R.id.fpstext);
        actionText = (TextView) findViewById(R.id.actionText);
		instance = this;
		
		/**
		 * 
		 * 开启重力传感器监听
		 * 
		 */
		acc = new Accelerometer(this);
		acc.start();
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
			if (permission != PackageManager.PERMISSION_GRANTED) {
				if (ActivityCompat.shouldShowRequestPermissionRationale(this,
	                    Manifest.permission.CAMERA)) {
	                Toast.makeText(this,"Please grant camera permission first",Toast.LENGTH_SHORT).show();
	            } else {
	                ActivityCompat.requestPermissions(this,
	                        new String[]{Manifest.permission.CAMERA},
	                        CAMERA_REQUEST_CODE);
	            }
			}
		}
	}

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // 如果请求被拒绝，那么通常grantResults数组为空
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //申请成功，进行相应操作

                } else {
                    //申请失败，可以继续向用户解释。
                }
                return;
            }
        }
    }
	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		getMenuInflater().inflate(R.menu.multitracker, menu);
//		return true;
//	}
	
	@Override
	public void onResume() {
		super.onResume();
		final FaceOverlapFragment fragment = (FaceOverlapFragment) getFragmentManager()
				.findFragmentById(R.id.overlapFragment);
		fragment.registTrackCallback(new FaceOverlapFragment.TrackCallBack() {
			
			@Override
            public void onTrackdetected(final int value, final float pitch, final float roll, final float yaw, final int eye_dist,
                            final int id, final int eyeBlink, final int mouthAh, final int headYaw, final int headPitch, final int browJump) {
				// TODO Auto-generated method stub
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
                        fpstText.setText("FPS: " + value+"\nPITCH: "+pitch+"\nROLL: "+roll+"\nYAW: "+yaw+"\nEYE_DIST:"+eye_dist);
                        actionText.setText("ID:"+id+"\nEYE_BLINK:"+eyeBlink+"\nMOUTH_AH:"+mouthAh+"\nHEAD_YAW:"+headYaw+"\nHEADPITCH:"+headPitch+"\nBROWJUMP:"+browJump);
					}
				});
			}
		});
	}
	
}
