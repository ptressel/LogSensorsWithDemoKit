package org.jigsawrenaissance.LogSensors;

import java.io.IOException;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.DemoKit.DemoKitActivity;

//import org.jigsawrenaissance.LogSensors.R;

/**
 * Read from USB accessory, and log and display received data.
 * Don't bother with persisting displayed data across app state changes.
 * 
 * This uses DemoKitActivity from the DemoKit app (supplied by Google as an
 * example of interacting with the Arduino over USB).  DemoKitActivity has
 * no UI operations other than the minimal setContentView -- it defers those
 * to subclasses.  It implements run() and starts a Thread in which to do the
 * USB I/O.  We override run() here, and also handle our UI operations.
 * 
 * @author Pat Tressel
 */
//public class LogSensorsActivity extends MockDemoKitActivity {
public class LogSensorsActivity extends DemoKitActivity {
    /** App name for log messages. */
    static final String TAG = "LogSensors";
    
    // Arduino USB handling
    
    /** Max expected length of a single message from the Arduino. */
    public static final int MAX_MESSAGE_LEN = 1024;
    /** Size of buffer for reads from USB. */
    public static final int BUFFER_LEN = 16384;
    // Chars with meaning in our messages.  These are 8-bit ASCII bytes.
    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;
    public static final byte AMP = '&';
    public static final byte UPPER_I = 'I';
    public static final byte LOWER_I = 'i';
    public static final byte COMMA = ',';
    
    /** 
     * Read from USB off the UI thread.  Our messages are CRLF-terminated
     * 8-bit text.  We read into a fixed size buffer and assemble messages
     * until a CRLF is reached (or rather, for simplicity, we strip CR and
     * read until we get an LF).
     * 
     * Normal operation seems to be to let run() exit when there's no data.
     * That means (presumably) it will exit between messages from the board.
     * When it exits, the thread dies.  Thus a new thread is started for each
     * message, which is inefficient.  This version allows that to happen.
     * 
     * @ToDo: Would be better to wait between messages.
     */
    public void run() {
        Log.d(TAG, "Worker thread started.");
        
        // Return value from USB read.
        int ret = 0;
        // Buffer is longer than any expected packet, but we're not yet sure
        // that packets might not be split across several reads.  This initial
        // version has no recourse if that happens -- it does not have a way to
        // wait for more data to arrive later.  Thus we treat each read as a
        // separate packet, regardless of whether it's properly terminated.
        byte[] buffer = new byte[BUFFER_LEN];
        // Index into buffer.
        int i;
        // Flag for end of message seen.
        boolean msgDone = false;
        // Timestamp -- we'll use elapsed time since boot, not wall clock, as
        // the latter can get changed arbitrarily, and we want accurate
        // intervals.
        long time;
        // This is a stand-in for a data class.  Here we are just going to
        // display and log the messages.  We'll assemble the message here.
        StringBuilder msg = new StringBuilder(MAX_MESSAGE_LEN);


        try {
            // Returns # bytes read or -1 if no more data.  Note -1 is not
            // a temporary condition -- it's "end of file".  This
            // FileInputStream object won't start delivering data again
            // after end of file even if another USB message shows up --
            // that would need a fresh FileInputStream.  So we can't just
            // loop back and do another read again after we see a -1.
            ret = mInputStream.read(buffer);
        } catch (IOException e) {
            return;
        }
        if (ret < 0) return;
        
        // Loop over all the data we just got, which may contain multiple
        // messages.
        i = 0;
        while (i < ret) {
            // For now, we believe we'll receive entire messages in one read,
            // so here, we're at the start of a new message.
            msgDone = false;
            // Empty our message storage.
            msg.setLength(0);
            // Get the time and put it in the message.  Don't bother with
            // formatting.
            time = SystemClock.elapsedRealtime();
            msg.append(time).append((char)COMMA);
            
            // Read until we see a CRLF, which ends one message, or we run out
            // of data.
            while (i < ret && !msgDone) {
                int len = ret - i;
                byte b = buffer[i];
                switch (b) {
                case CR:
                    if (len <= 2 || buffer[i+1] == LF) {
                        // At end of message -- send it to our handler.
                        // This is Linux -- don't need the CR.
                        msg.append((char)LF);
                        Message m = Message.obtain(mHandler);
                        m.obj = new String(msg);
                        mHandler.sendMessage(m);
                        // In case there is more in the buffer, skip the CRLF.
                        i += 2;
                        msgDone = true;
                    } else {
                        // CR in the middle of a message -- probably a mistake.
                        msg.append((char)CR);
                        ++i;
                    }
                    break;
                    
                case AMP:
                    // &I prefixes a two-byte analog value, high byte first.
                    if (len >= 4 && ((buffer[i+1] == UPPER_I) || (buffer[i+1] == LOWER_I))) {
                        int val = composeInt(buffer[i+2], buffer[i+3]);
                        msg.append(val);
                        i += 4;
                    } else {
                        // It's just an &...
                        msg.append((char)AMP);
                        i++;
                    }
                    break;
                
                default:
                    msg.append((char)buffer[i]);
                    ++i;
                    break;
                }
            }
        }
    }
    
    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            updateLog((String)msg.obj);
        }
    };
    
    // UI handling
    
    /** True if user has paused scrolling. Normally, we scroll the text region
     *  up as each new message is added to the end. */
    private boolean scrollPaused = false;
    
	/** Add a message to the log and display. Messages should end with a newline
	 *  or crlf. */
	private void updateLog(String msg) {
        TextView logView = (TextView)findViewById(R.id.log);
        logView.append(msg);
        if (!scrollPaused) {
            ScrollView scrollView = (ScrollView)findViewById(R.id.scroll);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        }
        Log.i(TAG, msg);
	}
    
    /** Clear the log view. */
    public void clearClick(View view) {
        TextView logView = (TextView)findViewById(R.id.log);
        logView.setText("");
    }
    
    /**
     * Handle a pause or unpause request -- stop or restart scrolling.
     * (Received messages will continue to be added to the text view.)
     */
    public void pauseClick(View view) {
        if (scrollPaused) {
            scrollPaused = false;
            ScrollView scrollView = (ScrollView)findViewById(R.id.scroll);
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            ((Button)view).setText(getString(R.string.pause));
            
        } else {
            scrollPaused = true;
            ((Button)view).setText(getString(R.string.unpause));
        }
    }
}
