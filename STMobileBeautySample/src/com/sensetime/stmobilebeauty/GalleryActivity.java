package com.sensetime.stmobilebeauty;

import java.io.InputStream;
import java.net.URL;

import com.sensetime.stmobilebeauty.display.ImageDisplay;
import com.sensetime.stmobilebeauty.utils.Constants;
import com.sensetime.stmobilebeauty.utils.SaveTask.onPictureSaveListener;
import com.sensetime.stmobilesample.R;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class GalleryActivity extends Activity implements OnClickListener{
	ImageDisplay mImageDisplay;
	private final int REQUEST_PICK_IMAGE = 1;
	private TextView mSaveImageText;
	private TextView mSaveInfo;
	private Button mCurrentBtn;
	private Button mCompareBtn;
	
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_gallery);
        
        mSaveImageText = (TextView)findViewById(R.id.gallery_save);
        mSaveImageText.setOnClickListener(this);
        
        mSaveInfo = (TextView)findViewById(R.id.saveing_info);
        mSaveInfo.setVisibility(View.GONE);
        
        initLevelView();
        initView();
        initDisplayView();
    }
    
    public void initDisplayView()
    {
    	GLSurfaceView glSurfaceView = (GLSurfaceView)findViewById(R.id.glsurfaceview_image);
    	mImageDisplay = new ImageDisplay(this, glSurfaceView);

		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);
    }
    
	private void initLevelView()
	{
		LinearLayout beautyLevel = (LinearLayout)findViewById(R.id.gallery_beautylevel);
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
            button.setLayoutParams(params);
    		if(i == 3)
    		{
    			button.setSelected(true);
    			mCurrentBtn = button;
    		}
    		button.setOnClickListener(this);
    		beautyLevel.addView(button);
    	}
	}
	
	private void initView(){
		mCompareBtn = (Button)findViewById(R.id.gallery_compare);
		mCompareBtn.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mImageDisplay.setShowOriginal(true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                	mImageDisplay.setShowOriginal(false);
                }
				return true;
			}
		});
	}
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
	    switch (requestCode) {
	        case REQUEST_PICK_IMAGE:
	            if (resultCode == RESULT_OK) {
	            	 try {
	            		 Uri uri = data.getData();
	            		 Bitmap bitmap = null;
	            		 if("file".equals(uri.getScheme())){
	            	    	 BitmapFactory.Options opts=new BitmapFactory.Options();
	            	    	 opts.inJustDecodeBounds = true;
	            	    	 bitmap = BitmapFactory.decodeFile(uri.getPath(), opts);
	            	    	 opts.inSampleSize = 2;
	            	    	 opts.inJustDecodeBounds = false;
	            	    	 bitmap = BitmapFactory.decodeFile(uri.getPath(),opts);
	            		 } else {
	            			 bitmap  = getBitmapAfterRotate(uri);
	            		 }
	            		 if(bitmap != null)
	            		 {
	            			 mImageDisplay.setImageBitmap(bitmap);
	            		 }
	                 } catch (Exception e) {
	                     e.printStackTrace();
	                 }
	            
	            } else {
	                finish();
	            }
	            break;
	
	        default:
	            super.onActivityResult(requestCode, resultCode, data);
	            break;
	    }
	}
  
	private Bitmap getBitmapAfterRotate(Uri uri)
	{
		 Bitmap rotatebitmap = null;
		 Bitmap srcbitmap = null;
	     String[] filePathColumn = { MediaStore.Images.Media.DATA,MediaStore.Images.Media.ORIENTATION};
	     Cursor cursor = null;
	     String picturePath = null;
	     String orientation = null;
	     
	     try {
	    	 cursor = getContentResolver().query(uri,filePathColumn, null, null, null); 
	     
		     if(cursor != null)
		     {
		    	 cursor.moveToFirst();  
		    	 int columnIndex = cursor.getColumnIndex(filePathColumn[0]);  
		    	 picturePath = cursor.getString(columnIndex);  
		    	 orientation = cursor.getString(cursor.getColumnIndex(filePathColumn[1]));
		     }
		 } catch (SQLiteException e) {
		            // Do nothing
		 } catch (IllegalArgumentException e) {
		            // Do nothing
		 } catch (IllegalStateException e) {
		            // Do nothing
		 } finally {
		      if(cursor != null)
		          cursor.close();
		 }
	     if(picturePath != null)
	     {
	    	 int angle = 0;
	    	 if (orientation != null && !"".equals(orientation)) {
	    		 angle = Integer.parseInt(orientation);
	    	 }
	    	 
	    	 BitmapFactory.Options opts=new BitmapFactory.Options();
	    	 opts.inJustDecodeBounds = true;
	    	 srcbitmap = BitmapFactory.decodeFile(picturePath, opts);
	    	 
	    	 opts.inSampleSize = computeSampleSize(opts);
	    	 opts.inJustDecodeBounds = false;
	    	 srcbitmap = BitmapFactory.decodeFile(picturePath,opts);
	    	 
	    	 if (angle != 0) {
			// 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
	    		 Matrix m = new Matrix();
	    		 int width = srcbitmap.getWidth();
	    		 int height = srcbitmap.getHeight();
	    		 m.setRotate(angle); // 旋转angle度
	    		 try {
	    		 rotatebitmap = Bitmap.createBitmap(srcbitmap, 0, 0, width, height,m, true);// 新生成图片	
	    		 } catch(Exception e)
	    		 {
	    			 
	    		 }
	    		 
	    		 if(rotatebitmap == null)
	    		 {
	    			 rotatebitmap = srcbitmap;
	    		 }
	    		 
	    		 if(srcbitmap != rotatebitmap)
	    		 {
	    			 srcbitmap.recycle();
	    		 }
	    	 }
	    	 else
	    	 {
	    		 rotatebitmap = srcbitmap;
	    	 }
	    }
	     
	    return rotatebitmap;
	}
	
	private int computeSampleSize(BitmapFactory.Options opts)
	{
		int sampleSize = 1;
		int width = opts.outWidth;
		int height = opts.outHeight;
		if(width > 4096 || height > 4096)
		{
			if(((width % 8) == 0) &&((height % 8) == 0))
			{
				sampleSize = 4;
			}
			else
			{
				if(((width % 4) == 0) &&((height % 4) == 0))
				{
					sampleSize = 2;
				}
			}
		}
		return sampleSize;
	}
	
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	mImageDisplay.onDestroy();
    }
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	    switch (v.getId()) {
	    case R.id.gallery_save:
			mSaveImageText.setEnabled(false);
			mSaveImageText.setTextColor(Color.GRAY);
			mSaveInfo.setVisibility(View.VISIBLE);
			mImageDisplay.savaImage(Constants.getOutputMediaFile(), mPictureSaveListener);
	    	break;
         default:
 			if(mCurrentBtn != null){
				mCurrentBtn.setSelected(false);
			}
			v.setSelected(true);
			mCurrentBtn = (Button)v;
 			float[] params = new float[1];
 			params[0] = (float)v.getId()/7.0f;
 			mImageDisplay.setEffectParams(params[0]);
            break;
        }	
	}
	
	private onPictureSaveListener mPictureSaveListener = new onPictureSaveListener()
	{
		public void onSaved(String result) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mSaveImageText.setEnabled(true);
					mSaveImageText.setTextColor(Color.BLACK);
					mSaveInfo.setVisibility(View.GONE);
				}
			});
		}
	};

}
