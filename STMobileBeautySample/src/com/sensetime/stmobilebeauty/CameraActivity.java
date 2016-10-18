package com.sensetime.stmobilebeauty;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.sensetime.stmobilebeauty.display.CameraDisplay;
import com.sensetime.stmobilebeauty.display.CameraDisplay.FpsChangeListener;
import com.sensetime.stmobilebeauty.utils.Constants;
import com.sensetime.stmobilebeauty.utils.SaveTask.onPictureSaveListener;
import com.sensetime.stmobilesample.R;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class CameraActivity extends Activity implements OnClickListener{
	private String TAG = "CameraActivity";
	private CameraDisplay mCameraDisplay;
	private boolean mIsShowBeauty = false;
	private TextView mCapturePhoto;
	private Button mCurrentBtn;
	private Button mCompareBtn;
	private RadioButton mTabBeautify;
	private RadioButton mTabEyeFace;
	private LinearLayout mTabEyeFaceLevel, mTabBeautifyLevel;
	private TextView mTvFaceNum, mTvEyeNum;
	private SeekBar mSbEye, mSbFace;
	private boolean mIsFirstEyeFace = true, mIsFirstBeautify = true;

	private int mCurrentRotation = 0;
	private long mStartTakePicTime;
	
	private Spinner mPreviewSizeSpinner;
	private TextView mVelocity;
	private TextView mCpuUtility;
	private Thread mCpuInfoThread;
	
	private OrientationEventListener mOrientationEventListener;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_camera);
		Log.i(TAG, "onCreate");
		initView();
		//Start Orientation Listener
		startOrientationChangeListener();
	}
	
	private void initView(){
		GLSurfaceView glSurfaceView = (GLSurfaceView)findViewById(R.id.glsurface_view);
		mCameraDisplay = new CameraDisplay(this, glSurfaceView);
		
		mCameraDisplay.setFpsChangeListener(new FpsChangeListener() {
			
			@Override
			public void onFpsChanged(final int value) {
				// TODO Auto-generated method stub
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mVelocity.setText(String.valueOf(value));
					}
				});
			}
		});
		
		mPreviewSizeSpinner = (Spinner)findViewById(R.id.preview_size);
	
		mCapturePhoto = (TextView) findViewById(R.id.capture);
		mCapturePhoto.setOnClickListener(this);
		
		mCpuUtility = (TextView)findViewById(R.id.cpuutility_value);
		mVelocity = (TextView)findViewById(R.id.velocity_value);
		
		mCpuUtility.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		mVelocity.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		mCompareBtn = (Button)findViewById(R.id.compare);
		mCompareBtn.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mCameraDisplay.setShowOriginal(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                	mCameraDisplay.setShowOriginal(false);
                }
				return true;
			}
		});

		mTabEyeFaceLevel = (LinearLayout)findViewById(R.id.tab_eyeface_level);
		mTabBeautifyLevel = (LinearLayout) findViewById(R.id.tab_beautify_level);
		mTabBeautify = (RadioButton) findViewById(R.id.rb_beautify);
		mTabEyeFace = (RadioButton) findViewById(R.id.rb_eyeface);
		mTabEyeFace.setOnClickListener(this);
		mTabBeautify.setOnClickListener(this);

		if(mIsFirstBeautify) {
			initLevelView();
			mIsFirstBeautify = false;
		}
		mTabBeautifyLevel.setVisibility(View.VISIBLE);
	}
		
	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause");
		if(mCameraDisplay != null)
			mCameraDisplay.onPause();
		this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		if(mCameraDisplay != null)
			mCameraDisplay.onResume();
		
		String[] previewSizes = this.getResources().getStringArray(R.array.preview_picturesize);
		final ArrayList<String> resultSizeList = mCameraDisplay.mCameraProxy.getSupportedPreviewSize(previewSizes);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.previewsize_spinner, resultSizeList);
		mPreviewSizeSpinner.setAdapter(adapter);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mPreviewSizeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String size = resultSizeList.get(position);
				int index = size.indexOf('x');
			    int width = Integer.parseInt(size.substring(0, index));
			    int height = Integer.parseInt(size.substring(index + 1));
				mCameraDisplay.changePreviewSize(width, height);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		mCpuInfoThread = new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(true)
				{
					final float rate = getProcessCpuRate();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if(mCpuUtility != null) {
								mCpuUtility.setText(String.valueOf(rate));
							}
						}
					});
				}
			}
		};
		mCpuInfoThread.start();
	}
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		if(mCameraDisplay != null)
			mCameraDisplay.onDestroy();
		mOrientationEventListener.disable();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.capture:
		{
			mCapturePhoto.setEnabled(false);
			mCapturePhoto.setTextColor(Color.GRAY);
			mStartTakePicTime = System.currentTimeMillis();
			mCameraDisplay.capture(null,mPictureCallback);
			mPreviewSizeSpinner.setEnabled(false);
			break;
		}
		case R.id.rb_beautify:
			mTabEyeFaceLevel.setVisibility(View.INVISIBLE);
			if(mIsFirstBeautify) {
				initLevelView();
				mIsFirstBeautify = false;
			}
			mTabBeautifyLevel.setVisibility(View.VISIBLE);
			break;
		case R.id.rb_eyeface:
			mTabBeautifyLevel.setVisibility(View.INVISIBLE);
			if(mIsFirstEyeFace) {
				initLevelBar();
				mIsFirstEyeFace = false;
			}
			mTabEyeFaceLevel.setVisibility(View.VISIBLE);
			break;
		default:
			if(mCurrentBtn != null){
				mCurrentBtn.setSelected(false);
			}
			v.setSelected(true);
			mCurrentBtn = (Button)v;
			float[] params = new float[1];
			params[0] = v.getId()/7.0f;
			mCameraDisplay.setEffectParams(params[0]);
			break;
		}
	}
	
	private void initLevelView()
	{
    	int size = 7;
    	for(int i=0; i< size; i++)
    	{
    		Button button=new Button(this);			
    		button.setId(i+1);			
    		button.setText(String.valueOf(i+1));
    		button.setBackgroundResource(R.drawable.selector_btn_background);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
            params.weight = 1.0f;
			params.topMargin = 60;
			params.bottomMargin = 60;
            button.setLayoutParams(params);
    		button.setOnClickListener(this);
    		if(i == 3)
    		{
    			button.setSelected(true);
    			mCurrentBtn = button;
    		}
    		button.setActivated(true);
    		mTabBeautifyLevel.addView(button);
    	}
	}

	private void initLevelBar() {
		mTvEyeNum = (TextView) mTabEyeFaceLevel.findViewById(R.id.tv_eye_num);
		mTvFaceNum = (TextView) findViewById(R.id.tv_face_num);
		mSbEye = (SeekBar) findViewById(R.id.sb_eye);
		mSbFace = (SeekBar) findViewById(R.id.sb_face);

		mSbEye.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mTvEyeNum.setText(progress+"");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		mSbFace.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mTvFaceNum.setText(progress+"");
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});
	}

	private final void startOrientationChangeListener()
	{
		mOrientationEventListener = new OrientationEventListener(this) {
			
			@Override
			public void onOrientationChanged(int orientation) {
		        // i的范围是0～359
		        // 屏幕左边在顶部的时候 i = 90;
		        // 屏幕顶部在底部的时候 i = 180;
		        // 屏幕右边在顶部的时候 i = 270;
		        // 正常放置的时候i = 0;
				
				int nNewRotation;
		        if(45 <= orientation && orientation < 135) 
		        {
		        	nNewRotation = 90;
		        } 
		        else if(135 <= orientation && orientation < 225) 
		        {
		        	nNewRotation = 180;
		        } 
		        else if(225 <= orientation && orientation < 315) 
		        {
		        	nNewRotation = 270;
		        } 
		        else 
		        {
		        	nNewRotation = 0;
		        }
		        
		        if(nNewRotation != mCurrentRotation)
		        {
		        	mCurrentRotation = nNewRotation;
		        	mCameraDisplay.onLayoutOrientationChanged(nNewRotation);
		        }
			}
		};
		
		mOrientationEventListener.enable();
	}
	
	private float getProcessCpuRate()
	{  
	    float totalCpuTime1 = getTotalCpuTime();
	    float processCpuTime1 = getAppCpuTime();
	    try
	    {
	        Thread.sleep(1000);
	            
	    }
	    catch (Exception e)
	    {
	    }
	        
	    float totalCpuTime2 = getTotalCpuTime();
	    float processCpuTime2 = getAppCpuTime();
	        
	    float cpuRate = 100 * (processCpuTime2 - processCpuTime1)
	               / (totalCpuTime2 - totalCpuTime1);
	        
	    return cpuRate;
    }
	    
    private long getTotalCpuTime()
	{ // 获取系统总CPU使用时间
	    String[] cpuInfos = null;
	    try
	    {
	        BufferedReader reader = new BufferedReader(new InputStreamReader(
	                   new FileInputStream("/proc/stat")), 1000);
	        String load = reader.readLine();
	        reader.close();
	        cpuInfos = load.split(" ");
	    }
	    catch (IOException ex)
	    {
	        ex.printStackTrace();
	    }
	    long totalCpu = Long.parseLong(cpuInfos[2])
	               + Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])
	               + Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])
	               + Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
	    return totalCpu;
	}
	    
    private long getAppCpuTime()
	{ // 获取应用占用的CPU时间
	    String[] cpuInfos = null;
	    try
	    {
	        int pid = android.os.Process.myPid();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(
	                   new FileInputStream("/proc/" + pid + "/stat")), 1000);
	        String load = reader.readLine();
	        reader.close();
	        cpuInfos = load.split(" ");
	    }
	    catch (IOException ex)
	    {
	        ex.printStackTrace();
	    }
	    long appCpuTime = Long.parseLong(cpuInfos[13])
	               + Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
	               + Long.parseLong(cpuInfos[16]);
	    return appCpuTime;
	}
	
	private PictureCallback mPictureCallback = new PictureCallback() {
		
		@Override
		public void onPictureTaken(final byte[] data,Camera camera) {
			Log.d(TAG, "onPictureTaken TAKE PICTURE TIME is "+ (System.currentTimeMillis()-mStartTakePicTime));		
			mCameraDisplay.onPictureTaken(data,Constants.getOutputMediaFile(),mPictureSaveListener);
		}
	};
	
	private onPictureSaveListener mPictureSaveListener = new onPictureSaveListener()
	{
		public void onSaved(String result) {
			Log.d(TAG, "TAKE PICTURE TIME is "+ (System.currentTimeMillis()-mStartTakePicTime));
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mCapturePhoto.setEnabled(true);
					mPreviewSizeSpinner.setEnabled(true);
					mCapturePhoto.setTextColor(Color.BLACK);
				}
			});	
		}
	};

}
