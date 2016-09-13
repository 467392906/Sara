package com.sensetime.stmobilebeauty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sensetime.stmobile.STImageFilterNative;
import com.sensetime.stmobilesample.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	private final static String TAG = "MainActivity";
    private static final String LICENSE_NAME = "SENSEME_E8BD0360-ED44-43FE-AA59-50EB14005B58.lic";

	private final static String PREF_ACTIVATE_CODE_FILE = "pref_activate_code_file";
	private final static String PREF_ACTIVATE_CODE = "pref_activate_code";

	private Context mContext;
    @Override public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_gallery).setOnClickListener(this);
        findViewById(R.id.button_camera).setOnClickListener(this);

        mContext = this;
    }

    @Override public void onClick(final View v) {
    	if (hasAuthentificatd()) {
    		startActivity(v.getId());
    	} else {
    		Toast.makeText(getApplicationContext(), "Please make sure you have granted", Toast.LENGTH_LONG).show();
    	}
    }

 // 授权
    private boolean hasAuthentificatd() {
        SharedPreferences sp = mContext.getSharedPreferences(PREF_ACTIVATE_CODE_FILE, 0);
        int rst = Integer.MIN_VALUE;
        String activateCode = sp.getString(PREF_ACTIVATE_CODE, null);

        synchronized(this.getClass())
		{
		   copyModelIfNeed(LICENSE_NAME);
		}
        String licensePath = getModelPath(LICENSE_NAME);

        if(activateCode == null) {
            Log.e(TAG, "licensePath======:" + licensePath);
            activateCode = STImageFilterNative.generateActivateCode(licensePath, activateCode);
            Log.e(TAG, "-->> activateCode = "+activateCode);
            if(activateCode==null || activateCode.length()==0) {
                Log.e(TAG, "-->> generate active code failed!");
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString(PREF_ACTIVATE_CODE, activateCode);
            editor.commit();
        }

        if(activateCode==null || activateCode.length()==0) {
            Log.e(TAG, "-->> activeCode is null in SharedPreference");
            return false;
        }

        rst = STImageFilterNative.checkActivateCode(licensePath, activateCode);
        if(rst != 0) {
            // check失败，也有可能是新的license替换，但是还是用的原来lincense生成的activecode。在这里重新生成一次activecode
            activateCode = STImageFilterNative.generateActivateCode(licensePath, activateCode);

            if(activateCode==null || activateCode.length()==0) {
                Log.e(TAG, "-->> again generate active code failed! license may invalide");
                return false;
            }
            rst = STImageFilterNative.checkActivateCode(licensePath, activateCode);
            if(rst != 0) {
                Log.e(TAG, "-->> again invalide active code, you need a new license");
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString(PREF_ACTIVATE_CODE, activateCode);
            editor.commit();
        }

        return true;
    }

	private void copyModelIfNeed(String modelName) {
		String path = getModelPath(modelName);
		if (path != null) {
			File modelFile = new File(path);
			if (!modelFile.exists()) {
				//如果模型文件不存在或者当前模型文件的版本跟sdcard中的版本不一样
				try {
					if (modelFile.exists())
						modelFile.delete();
					modelFile.createNewFile();
					InputStream in = mContext.getApplicationContext().getAssets().open(modelName);
					if(in == null)
					{
						Log.e(TAG, "the src module is not existed");
					}
					OutputStream out = new FileOutputStream(modelFile);
					byte[] buffer = new byte[4096];
					int n;
					while ((n = in.read(buffer)) > 0) {
						out.write(buffer, 0, n);
					}
					in.close();
					out.close();
				} catch (IOException e) {
					modelFile.delete();
				}
			}
		}
	}

	protected String getModelPath(String modelName) {
		String path = null;
		File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
		if (dataDir != null) {
			path = dataDir.getAbsolutePath() + File.separator + modelName;
		}
		return path;
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
