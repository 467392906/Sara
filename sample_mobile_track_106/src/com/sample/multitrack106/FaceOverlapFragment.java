package com.sample.multitrack106;

import java.util.ArrayList;
import java.util.List;

import com.sensetime.stmobileapi.AuthCallback;
import com.sensetime.stmobileapi.STMobile106;
import com.sensetime.stmobileapi.STMobileFaceAction;
import com.sensetime.stmobileapi.STMobileMultiTrack106;
import com.sensetime.stmobileapi.STUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * 
 * @author MatrixCV
 * 
 *         实时人脸检测接口调用示例
 * 
 */
public class FaceOverlapFragment extends CameraOverlapFragment{
    ///< 检测脸部动作：张嘴、眨眼、抬眉、点头、摇头
    private static final int ST_MOBILE_TRACKING_MULTI_THREAD = 0x00000000;
    private static final int ST_MOBILE_TRACKING_RESIZE_IMG_320W = 0x00000001;
    private static final int ST_MOBILE_TRACKING_ENABLE_DEBOUNCE  = 0x00000010;
    private static final int ST_MOBILE_TRACKING_ENABLE_FACE_ACTION   = 0x00000020;
    private static final int ST_MOBILE_TRACKING_DEFAULT_CONFIG = ST_MOBILE_TRACKING_MULTI_THREAD | ST_MOBILE_TRACKING_RESIZE_IMG_320W ;
    private static final int ST_MOBILE_FACE_DETECT   =  0x00000001;    ///<  人脸检测
    private static final int ST_MOBILE_EYE_BLINK     =  0x00000002;  ///<  眨眼
    private static final int ST_MOBILE_MOUTH_AH      =  0x00000004;    ///<  嘴巴大张
    private static final int ST_MOBILE_HEAD_YAW      =  0x00000008;    ///<  摇头
    private static final int ST_MOBILE_HEAD_PITCH    =  0x00000010;    ///<  点头
    private static final int ST_MOBILE_BROW_JUMP     =  0x00000020;    ///<  眉毛挑动

	// private FaceTrackerBase tracker = null;
	private Context mContext;
	private STMobileMultiTrack106 tracker = null;
	TrackCallBack mListener;
	private Thread thread;
	private boolean killed = false;
	private byte nv21[];
	private Bitmap bitmap;
	public static int fps;
	static boolean DEBUG = false;
	private boolean isNV21ready = false;
	private Paint mPaint;
	
	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		nv21 = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];

		mPaint = new Paint(); 
		mPaint.setColor(Color.rgb(57, 138, 243));
		int strokeWidth = Math.max(PREVIEW_HEIGHT / 240, 2);
		mPaint.setStrokeWidth(strokeWidth);
		mPaint.setStyle(Style.FILL);

		this.setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				synchronized (nv21) {
					System.arraycopy(data, 0, nv21, 0, data.length);
					isNV21ready = true;
				}
			}

		});
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (MultitrackerActivity.acc != null)
			MultitrackerActivity.acc.start();

		/**
		 * 
		 * 初始化实时人脸检测的帧宽高 目前只支持宽640*高480
		 * 
		 */

		if (tracker == null) {
			long start_init = System.currentTimeMillis();
			AuthCallback authCallback = new AuthCallback() {
				@Override
				public void authErr(String err) {
					Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show();
				}			
			};
            int config = ST_MOBILE_TRACKING_DEFAULT_CONFIG | ST_MOBILE_TRACKING_ENABLE_DEBOUNCE | ST_MOBILE_TRACKING_ENABLE_FACE_ACTION;
			if(authCallback != null) {
				mContext = getActivity();
				tracker = new STMobileMultiTrack106(mContext, config, authCallback);
			}
			int max = 40;
			tracker.setMaxDetectableFaces(max);
			long end_init = System.currentTimeMillis();
			Log.i("track106", "init cost "+(end_init - start_init) +" ms");
		}

		killed = false;
		final byte[] tmp = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
		thread = new Thread() {
			@Override
			public void run() {
				List<Long> timeCounter = new ArrayList<Long>();
				int start = 0;
				while (!killed) {
					
					if(!isNV21ready)
						continue;

					synchronized (nv21) {
						System.arraycopy(nv21, 0, tmp, 0, nv21.length);
						isNV21ready = false;
					}

					/**
					 * 如果使用前置摄像头，请注意显示的图像与帧图像左右对称，需处理坐标
					 */
					boolean frontCamera = (CameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT);

					/**
					 * 获取重力传感器返回的方向
					 */
					int dir = Accelerometer.getDirection();

					/**
					 * 请注意前置摄像头与后置摄像头旋转定义不同
					 * 请注意不同手机摄像头旋转定义不同
					 */
					if (frontCamera && 
							((mCameraInfo.orientation == 270 && (dir & 1) == 1) ||
							 (mCameraInfo.orientation == 90 && (dir & 1) == 0)))
						dir = (dir ^ 2);

					/**
					 * 调用实时人脸检测函数，返回当前人脸信息
					 */
					long start_track = System.currentTimeMillis();
                    STMobileFaceAction[] faceActions = tracker.trackFaceAction(tmp, dir, PREVIEW_WIDTH, PREVIEW_HEIGHT);
					long end_track = System.currentTimeMillis();
					Log.i("track106", "track cost "+(end_track - start_track)+" ms");

					long timer = System.currentTimeMillis();
					timeCounter.add(timer);
					while (start < timeCounter.size()
							&& timeCounter.get(start) < timer - 1000) {
						start++;
					}
					fps = timeCounter.size() - start;
					try {
						if (faceActions != null && faceActions.length > 0) {
							int length = (faceActions == null ?  0 : faceActions.length - 1);
							STMobileFaceAction faceAction = faceActions[length];
	                        mListener.onTrackdetected(fps,  faceAction.face.pitch, faceAction.face.roll, faceAction.face.yaw, faceAction.face.eye_dist, faceAction.face.ID,
	                                        checkFlag(faceAction.face_action, ST_MOBILE_EYE_BLINK), checkFlag(faceAction.face_action, ST_MOBILE_MOUTH_AH), checkFlag(faceAction.face_action, ST_MOBILE_HEAD_YAW),
	                                        checkFlag(faceAction.face_action, ST_MOBILE_HEAD_PITCH), checkFlag(faceAction.face_action, ST_MOBILE_BROW_JUMP));
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
					if (start > 100) {
						timeCounter = timeCounter.subList(start,
								timeCounter.size() - 1);
						start = 0;
					}

					/**
					 * 绘制人脸框
					 */
                    if(faceActions != null) {
						if(DEBUG){						
                            for(int i=0; i<faceActions.length; i++) {
                              Log.i("Test", "detect faces: "+ faceActions[i].getFace().getRect().toString());
							}
							
						}
						
						Canvas canvas = mOverlap.getHolder().lockCanvas();

						if (canvas == null)
							continue;

						canvas.drawColor(0, PorterDuff.Mode.CLEAR);
						canvas.setMatrix(getMatrix());
						boolean rotate270 = mCameraInfo.orientation == 270;
                        for (STMobileFaceAction r : faceActions) {
							// Rect rect = r.getRect();
							Rect rect;
							if (rotate270) {
                                rect = STUtils.RotateDeg270(r.getFace().getRect(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
							} else {
                                rect = STUtils.RotateDeg90(r.getFace().getRect(), PREVIEW_WIDTH, PREVIEW_HEIGHT);
							}
							
                            PointF[] points = r.getFace().getPointsArray();
							for (int i = 0; i < points.length; i++) {
								if (rotate270) {
									points[i] = STUtils.RotateDeg270(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
								} else {
									points[i] = STUtils.RotateDeg90(points[i], PREVIEW_WIDTH, PREVIEW_HEIGHT);
								}
								
							}
							STUtils.drawFaceRect(canvas, rect, PREVIEW_HEIGHT,
									PREVIEW_WIDTH, frontCamera);
							STUtils.drawPoints(canvas, mPaint, points, PREVIEW_HEIGHT,
									PREVIEW_WIDTH, frontCamera);

						}
						mOverlap.getHolder().unlockCanvasAndPost(canvas);
					}
				}
			}
		};

		thread.start();
	}

	@Override
	public void onPause() {
		if (MultitrackerActivity.acc != null)
			MultitrackerActivity.acc.stop();
		killed = true;
		if (thread != null)
			try {
				thread.join(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		if (tracker != null) {
			System.out.println("destroy tracker");
			tracker.destory();
			tracker = null;
		}
		super.onPause();
	}

	public void registTrackCallback(TrackCallBack callback) {
		mListener = callback;
	}

	public interface TrackCallBack {
//		public void onTrackdetected(int value, float pitch, float roll, float yaw);
        public void onTrackdetected(int value, float pitch, float roll, float yaw, int eye_dist,
                        int id, int eyeBlink, int mouthAh, int headYaw, int headPitch, int browJump);
	}
	
    private int checkFlag(int action, int flag) {
        int res = action & flag;
        return res==0?0:1;
    }

}
