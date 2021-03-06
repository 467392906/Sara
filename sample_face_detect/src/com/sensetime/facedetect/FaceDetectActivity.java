package com.sensetime.facedetect;

import javax.microedition.khronos.opengles.GL10;

import com.sensetim.facedetect.R;
import com.sensetime.stmobileapi.AuthCallback;
import com.sensetime.stmobileapi.STMobile106;
import com.sensetime.stmobileapi.STMobileFaceDetection;
import com.sensetime.stmobileapi.STUtils;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class FaceDetectActivity extends Activity implements OnClickListener {

    private static final int REQUEST_PICK_IMAGE = 1;
    public static final String JPEG_MIME_TYPE = "image/jpeg";
    public String TAG = "FaceDetect";
    private ImageView mImageView;
    private Bitmap mSrcbmp;
    private Bitmap mSrcFace;
	private STMobileFaceDetection mDetect = null;
	private int mImageWidth;
	private int mImageHeight;
	private Button mFaceDetectBtn; 
	private Paint mPaint;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        initView();  
    }
    
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
        destory();
    }
    
    private void destory()
    {
		if (mDetect != null) {
			System.out.println("destroy detect");
			mDetect.destory();
			mDetect = null;
		}
    }

    private void initView()
    {
    	mImageView = (ImageView)findViewById(R.id.imageView);
    	
		Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, REQUEST_PICK_IMAGE);
    	
		/*
    	mSrcbmp = BitmapFactory.decodeResource(getResources(), R.drawable.test);
    	mImageView.setImageBitmap(mSrcbmp);
    	
    	mImageWidth = mSrcbmp.getWidth();
    	mImageHeight = mSrcbmp.getHeight();
    	initFaceDetect();
    	*/

		mFaceDetectBtn = (Button)findViewById(R.id.track_face);
		mFaceDetectBtn.setOnClickListener(this);
    }
    
    private void initFaceDetect()
    {	
		if (mDetect == null) {
			AuthCallback authCallback = new AuthCallback() {
				@Override
				public void authErr(String err) {
					Toast.makeText(getApplicationContext(), err, Toast.LENGTH_SHORT).show();
				}			
			};
			mSrcFace = mSrcbmp.copy(Config.ARGB_8888, true);
			long start_init = System.currentTimeMillis();
			int config = STMobileFaceDetection.ST_MOBILE_DETECT_FAST; //fast
			if(authCallback != null) {
				mDetect = new STMobileFaceDetection(getApplicationContext(), config, authCallback);
			}
			long end_init = System.currentTimeMillis();
			Log.i(TAG, "init cost "+(end_init - start_init) +" ms");
		}	
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();

    }
    

    
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
	    switch (requestCode) {
	        case REQUEST_PICK_IMAGE:
	            if (resultCode == RESULT_OK) {
	            	 try {
	            		 Uri uri = data.getData(); 
	            		 if("file".equals(uri.getScheme())){
	            	    	 BitmapFactory.Options opts=new BitmapFactory.Options();
	            	    	 opts.inJustDecodeBounds = true;
	            	    	 mSrcbmp = BitmapFactory.decodeFile(uri.getPath(), opts);
	            	    	 opts.inSampleSize = 2;
	            	    	 opts.inJustDecodeBounds = false;
	            	    	 mSrcbmp = BitmapFactory.decodeFile(uri.getPath(),opts);
	            		 }
	            		 else {
	            			 mSrcbmp = getBitmapAfterRotate(uri);
						}
	            		
	            		 if(mSrcbmp != null)
	            		 {
	            			 mImageView.setImageBitmap(mSrcbmp);
	            		     mImageWidth = mSrcbmp.getWidth();
	            		     mImageHeight = mSrcbmp.getHeight();
            				mPaint = new Paint(); 
            				mPaint.setColor(Color.rgb(57, 138, 243));
            				int strokeWidth = Math.max(mImageHeight / 240, 2);
            				mPaint.setStrokeWidth(strokeWidth);
            				mPaint.setStyle(Style.FILL);
	            		     initFaceDetect();
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
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.track_face:
            	handleFaceDetect(mSrcFace);
            	mFaceDetectBtn.setTextColor(Color.GRAY);
            	mFaceDetectBtn.setEnabled(false);
            	break;
            default:
                break;
        }
    }
    

	
    private void handleImage(final Uri selectedImage) {

    }
	

    private void handleFaceDetect(Bitmap bmp)
    {
    	if(mDetect == null){
    		return;
    	}
		long start_track = System.currentTimeMillis();
		STMobile106[] faces = mDetect.detect(bmp, 0);
		long end_track = System.currentTimeMillis();
		Log.i(TAG, "detect cost "+(end_track - start_track)+" ms");
		/**
		 * 绘制人脸框
		 */
		if (faces != null) {
			Canvas canvas = new Canvas(bmp);

			if (canvas == null)
				return;

			for (STMobile106 r : faces) {
				Rect rect;
				rect = r.getRect();
				
				PointF[] points = r.getPointsArray();

				STUtils.drawFaceRect(canvas, rect, mImageHeight,
						mImageWidth, false);
				STUtils.drawPoints(canvas, mPaint, points, mImageHeight,
						mImageWidth, false);
			}
		}
		
		mImageView.setImageBitmap(bmp);

    }

}
