package com.sample.multitrack106;

import com.sample.multitrack106.FaceOverlapFragment;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

/**
 * 
 * @author MatrixCV
 *
 * Activity
 * 
 */
public class MultitrackerActivity extends Activity {
	
	static MultitrackerActivity instance = null;
	TextView fpstText;
	
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
		instance = this;
		
		/**
		 * 
		 * 开启重力传感器监听
		 * 
		 */
		acc = new Accelerometer(this);
		acc.start();
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
			public void onTrackdetected(final int value, final float pitch, final float roll, final float yaw) {
				// TODO Auto-generated method stub
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						fpstText.setText("FPS: " + value+"\nPITCH: "+pitch+"\nROLL: "+roll+"\nYAW: "+yaw);
					}
				});
			}
		});
	}
	
}
