/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.multitrack106;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Calendar;

import com.sensetime.stmobileapi.STMobile106;
import com.sensetime.stmobileapi.STMobileMultiTrack106;
import com.sensetime.stmobileapi.STUtils;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.nfc.cardemulation.OffHostApduService;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;


public class GalleryActivity extends Activity implements OnClickListener{

    private static final int REQUEST_PICK_IMAGE = 1;
    public static final String JPEG_MIME_TYPE = "image/jpeg";
    private ImageView mImageView;
    private Bitmap mSrcbmp;
    private Bitmap mSrcFace;
	private STMobileMultiTrack106 mTracker = null;
	private int mImageWidth;
	private int mImageHeight;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_gallery);

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
		if (mTracker != null) {
			System.out.println("destroy tracker");
			mTracker.destory();
			mTracker = null;
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

    	Button facetrack = (Button)findViewById(R.id.track_face);
    	facetrack.setOnClickListener(this);
    }
    
    private void initFaceDetect()
    {	
		if (mTracker == null) {
			mSrcFace = mSrcbmp.copy(Config.ARGB_8888, true);
			long start_init = System.currentTimeMillis();
			int config = 1;
			mTracker = new STMobileMultiTrack106(this, config);
			int max = 40;
			mTracker.setMaxDetectableFaces(max);
			long end_init = System.currentTimeMillis();
			Log.i("Gallery track106", "init cost "+(end_init - start_init) +" ms");
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
	            		 mSrcbmp = getBitmapAfterRotate(uri);
	            		 if(mSrcbmp != null)
	            		 {
	            			 mImageView.setImageBitmap(mSrcbmp);
	            		     mImageWidth = mSrcbmp.getWidth();
	            		     mImageHeight = mSrcbmp.getHeight();
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
	    	 srcbitmap = BitmapFactory.decodeFile(picturePath);
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
    
    @Override
    public void onClick(final View v) {
        switch (v.getId()) {
            case R.id.track_face:
            	handleFaceDetect(mSrcFace);
            	break;
            default:
                break;
        }
    }
    

	
    private void handleImage(final Uri selectedImage) {

    }
	

    private void handleFaceDetect(Bitmap bmp)
    {
		long start_track = System.currentTimeMillis();
		STMobile106[] faces = mTracker.track(bmp, 0);
		long end_track = System.currentTimeMillis();
		Log.i("track106", "track cost "+(end_track - start_track)+" ms");
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
				STUtils.drawPoints(canvas, points, mImageHeight,
						mImageWidth, false);
			}
		}
		
		mImageView.setImageBitmap(bmp);

    }

}
