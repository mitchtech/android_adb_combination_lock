package net.mitchtech.adb;

import java.io.IOException;

import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

import net.mitchtech.adb.combinationlock.R;

import org.microbridge.server.Server;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;

public class CombinationLockActivity extends Activity {
	private final String TAG = CombinationLockActivity.class.getSimpleName();

	// Port number connected to lock control servo
	private final byte mServo = 0x5;

	// Adjust the lock engaged/disengaged positions.
	// Some servos cannot go all the way to 0 or 180
	private final int mLockUnlocked = 175;
	private final int mLockLocked = 5;

	// True when unlocked; false otherwise
	private boolean mUnlocked = false;

	// Microbridge TCP server
	Server mServer = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		initWheel(R.id.pin_0);
		initWheel(R.id.pin_1);
		initWheel(R.id.pin_2);
		initWheel(R.id.pin_3);

		Button btnMix = (Button) findViewById(R.id.btn_random);
		btnMix.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mixWheel(R.id.pin_0);
				mixWheel(R.id.pin_1);
				mixWheel(R.id.pin_2);
				mixWheel(R.id.pin_3);
			}
		});

		Button btnReset = (Button) findViewById(R.id.btn_reset);
		btnReset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setWheel(R.id.pin_0, 0);
				setWheel(R.id.pin_1, 0);
				setWheel(R.id.pin_2, 0);
				setWheel(R.id.pin_3, 0);
			}
		});

		// Create new TCP Server
		try {
			mServer = new Server(4567);
			mServer.start();
		} catch (IOException e) {
			Log.e(TAG, "Unable to start TCP server", e);
			System.exit(-1);
		}

		updateStatus();
	}

	// Wheel scrolled flag
	private boolean mWheelScrolled = false;

	// Wheel scrolled listener
	OnWheelScrollListener mScrolledListener = new OnWheelScrollListener() {
		public void onScrollingStarted(WheelView wheel) {
			mWheelScrolled = true;
		}

		public void onScrollingFinished(WheelView wheel) {
			mWheelScrolled = false;
			updateStatus();
		}
	};

	// Wheel changed listener
	private OnWheelChangedListener mWheelChangedListener = new OnWheelChangedListener() {
		public void onChanged(WheelView wheel, int oldValue, int newValue) {
			if (!mWheelScrolled) {
				updateStatus();
			}
		}
	};

	/**
	 * Updates entered PIN status
	 */
	private void updateStatus() {
		TextView text = (TextView) findViewById(R.id.pwd_status);
		if (testPin(1, 2, 3, 4)) {
			text.setText("Unlocked!");
			lockUnlock();
			mUnlocked = true;
		} else {
			text.setText("Invalid PIN");
			if (mUnlocked) {
				lockLock();
				mUnlocked = false;
			}
		}
	}

	/**
	 * Initializes wheel
	 * 
	 * @param id
	 *            the wheel widget Id
	 */
	private void initWheel(int id) {
		WheelView mWheel = getWheel(id);
		mWheel.setViewAdapter(new NumericWheelAdapter(this, 0, 9));
		mWheel.setCurrentItem((int) (Math.random() * 10));
		mWheel.addChangingListener(mWheelChangedListener);
		mWheel.addScrollingListener(mScrolledListener);
		mWheel.setCyclic(true);
		mWheel.setInterpolator(new AnticipateOvershootInterpolator());
	}

	/**
	 * Returns wheel by Id
	 * 
	 * @param id
	 *            the wheel Id
	 * @return the wheel with passed Id
	 */
	private WheelView getWheel(int id) {
		return (WheelView) findViewById(id);
	}

	/**
	 * Tests entered PIN
	 * 
	 * @param v1
	 * @param v2
	 * @param v3
	 * @param v4
	 * @return true
	 */
	private boolean testPin(int v0, int v1, int v2, int v3) {
		return testWheelValue(R.id.pin_0, v0) && testWheelValue(R.id.pin_1, v1)
				&& testWheelValue(R.id.pin_2, v2) && testWheelValue(R.id.pin_3, v3);
	}

	/**
	 * Tests wheel value
	 * 
	 * @param id
	 *            the wheel Id
	 * @param position
	 *            the value to test
	 * @return true if wheel value is equal to passed value
	 */
	private boolean testWheelValue(int id, int position) {
		return getWheel(id).getCurrentItem() == position;
	}

	/**
	 * Mixes wheel
	 * 
	 * @param id
	 *            the wheel id
	 */
	private void mixWheel(int id) {
		WheelView mWheel = getWheel(id);
		mWheel.scroll(-50 + (int) (Math.random() * 50), 500);
	}

	/**
	 * Set wheel to position
	 * 
	 * @param id
	 *            the wheel id
	 * @param position
	 *            the value to set
	 */
	private void setWheel(int id, int position) {
		WheelView mWheel = getWheel(id);
		mWheel.setCurrentItem(position);
	}

	/**
	 * Set servo to position
	 * 
	 * @param pin
	 *            the servo id
	 * @param position
	 *            the value to set
	 */
	private void setServo(byte pin, int position) {
		try {
			mServer.send(new byte[] { pin, (byte) position });
		} catch (IOException e) {
			Log.e(TAG, "problem sending TCP message", e);
		}
	}

	/**
	 * Set SERVO1 to LOCK_ENGAGE position
	 * 
	 */
	private void lockUnlock() {
		setServo(mServo, mLockUnlocked);
	}

	/**
	 * Set SERVO1 to LOCK_DISENGAGE position
	 * 
	 */
	private void lockLock() {
		setServo(mServo, mLockLocked);
	}

}
