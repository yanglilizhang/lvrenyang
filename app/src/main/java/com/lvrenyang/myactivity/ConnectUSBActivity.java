package com.lvrenyang.myactivity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;

import com.lvrenyang.R;
import com.lvrenyang.myprinter.WorkService;
import com.lvrenyang.myprinter.Global;
import com.lvrenyang.utils.DataUtils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class ConnectUSBActivity extends Activity implements OnClickListener {

	private static Handler mHandler = null;
	private static String TAG = "ConnectUSBActivity";

	private LinearLayout linearLayoutUSBDevices;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_connectusb);

		linearLayoutUSBDevices = (LinearLayout) findViewById(R.id.linearLayoutUSBDevices);

		mHandler = new MHandler(this);
		WorkService.addHandler(mHandler);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
			probe();
		} else {
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WorkService.delHandler(mHandler);
		mHandler = null;
	}

	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		switch (arg0.getId()) {

		default:
			break;

		}

	}

	private void probe() {
		final UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		if (deviceList.size() > 0) {
			// 初始化选择对话框布局，并添加按钮和事件

			while (deviceIterator.hasNext()) { // 这里是if不是while，说明我只想支持一种device
				final UsbDevice device = deviceIterator.next();
				/*Toast.makeText(
						this,
						"" + device.getDeviceId() + device.getDeviceName()
								+ device.toString(), Toast.LENGTH_LONG).show();*/
				Toast.makeText(this,"<==== Interface Count && Device ====>"+device.getInterfaceCount()+" & "+device.getVendorId(),Toast.LENGTH_SHORT).show();

				Button btDevice = new Button(
						linearLayoutUSBDevices.getContext());
				btDevice.setLayoutParams(new LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
				btDevice.setGravity(Gravity.CENTER_VERTICAL
						| Gravity.LEFT);
				btDevice.setText(String.format(" VID:%04X PID:%04X",
						device.getVendorId(), device.getProductId()));
				btDevice.setOnClickListener(new OnClickListener() {

					public void onClick(View v) {



						PendingIntent mPermissionIntent = PendingIntent
								.getBroadcast(
										ConnectUSBActivity.this,
										0,
										new Intent(
												ConnectUSBActivity.this
														.getApplicationInfo().packageName),
										0);
						if (!mUsbManager.hasPermission(device)) {
							mUsbManager.requestPermission(device,
									mPermissionIntent);
							Toast.makeText(getApplicationContext(),
									Global.toast_usbpermit, Toast.LENGTH_LONG)
									.show();
						} else {
							WorkService.workThread.connectUsb(mUsbManager,
									device);
						}
					}
				});
				linearLayoutUSBDevices.addView(btDevice);
			}
		}
	}
	private Bitmap getImageFromAssetsFile(String fileName) {
		Bitmap image = null;
		AssetManager am = getResources().getAssets();
		try {
			InputStream is = am.open(fileName);
			image = BitmapFactory.decodeStream(is);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return image;

	}
	static class MHandler extends Handler {

		WeakReference<ConnectUSBActivity> mActivity;

		MHandler(ConnectUSBActivity activity) {
			mActivity = new WeakReference<ConnectUSBActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			ConnectUSBActivity theActivity = mActivity.get();
			switch (msg.what) {
			/**
			 * DrawerService 的 onStartCommand会发送这个消息
			 */

			case Global.MSG_WORKTHREAD_SEND_CONNECTUSBRESULT: {
				int result = msg.arg1;
				Toast.makeText(
						theActivity,
						(result == 1) ? Global.toast_success
								: Global.toast_fail, Toast.LENGTH_SHORT).show();
				Log.v(TAG, "Connect Result: " + result);
				if (1 == result) {
					PrintTest();
				}
				break;
			}

			}
		}

		void PrintTest() {
			String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789\n";
			byte[] tmp1 = { 0x1b, 0x40, (byte) 0xB2, (byte) 0xE2, (byte) 0xCA,
					(byte) 0xD4, (byte) 0xD2, (byte) 0xB3, 0x0A };
			byte[] tmp2 = { 0x1b, 0x21, 0x01 };
			byte[] tmp3 = { 0x0A, 0x0A, 0x0A, 0x0A };
			byte[] buf = DataUtils.byteArraysToBytes(new byte[][] { tmp1,
					str.getBytes(), tmp2, str.getBytes(), tmp3 });
			if (WorkService.workThread.isConnected()) {
				Bundle data = new Bundle();
				data.putByteArray(Global.BYTESPARA1, buf);
				data.putInt(Global.INTPARA1, 0);
				data.putInt(Global.INTPARA2, buf.length);
				WorkService.workThread.handleCmd(Global.CMD_WRITE, data);
			} else {
				Toast.makeText(mActivity.get(), Global.toast_notconnect,
						Toast.LENGTH_SHORT).show();
			}
		}
	}

}