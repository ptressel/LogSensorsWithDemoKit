package org.jigsawrenaissance.LogSensors;

import java.io.FileInputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

/**
 * A test mock to substitute for USB data from the Arduino -- allows running
 * tests in the emulator with fake data from a file in assets.
 * 
 * To use the mock, substitute MockDemoKitActivity as the parent class for
 * LogSensorsActivity.
 * 
 * @author Pat Tressel
 */
public class MockDemoKitActivity extends Activity implements Runnable {
    private static final String TAG = "MockDemoKit";
    // Crude hack: The APK build process compresses files in assets unless it
    // believes they're already compressed.  But one cannot open a compressed
    // file using AssetManager's openFd, which is the only path to getting a
    // FileInputStream from the file.  The "workaround" is to rename the file
    // with an extension indicating a compressed file type like zip or png.
    // People have reported that zip does not work, but png does.  Regardless
    // of the extension, this is not an image.
    public static final String MOCK_DATA_FILENAME = "mock_sensor_messages.png";
    protected AssetManager mAssetManager;
    protected AssetFileDescriptor mFileDescriptor;
    protected FileInputStream mInputStream;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        try {
            mAssetManager = getAssets();
            mFileDescriptor = mAssetManager.openFd(MOCK_DATA_FILENAME);
            if (mFileDescriptor != null) {
                mInputStream = mFileDescriptor.createInputStream();
                if (mInputStream != null) {
                    Thread thread = new Thread(null, this, TAG);
                    thread.start();
                    Log.d(TAG, "Thread started.");
                } else {
                    Log.d(TAG, "Could not read test data.");
                }
            } else {
                Log.d(TAG, "Could not find test data.");
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception", e);
        }
    }
    
    @Override
    public void run() {
        Log.d(TAG, "Should not get here.");
    }
    
    // Borrowed from the real DemoKitActivity.
    protected int composeInt(byte hi, byte lo) {
        int val = (int) hi & 0xff;
        val *= 256;
        val += (int) lo & 0xff;
        return val;
    }
}
