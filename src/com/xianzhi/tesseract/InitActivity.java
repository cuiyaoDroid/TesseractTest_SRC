package com.xianzhi.tesseract;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.mikewong.tool.tesseract.MainActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;


public class InitActivity extends Activity {
	public static String SDCardRoot = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + File.separator;
	public static String RAIL = ".cc_rail";

	public static void putVersion(String s) {
		try {
			FileOutputStream outStream = new FileOutputStream(SDCardRoot + RAIL
					+ "/30.txt", false);
			OutputStreamWriter writer = new OutputStreamWriter(outStream,
					"gb2312");
			writer.write(s);
			writer.flush();
			writer.close();// 记得关闭

			outStream.close();
		} catch (Exception e) {
			System.out.println("write to sdcard for error");
		}
	}

	/*private void upGradeDBifnessage() {
		DBHelper helper = new DBHelper(getApplicationContext());
		helper.close();
	}*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		//upGradeDBifnessage();
		createPath(getSDPath()+"/tessdata");
		createPath(TrainingApp.appPath);
		
		CopyAssets("eng.traineddata");
		putVersion(getString(R.string.version));
		Intent intent = new Intent(getApplicationContext(), CamareActivity.class);
		startActivityForResult(intent, RESULT_OK);
		finish();

	}
	private void createPath(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
	}
	private void CopyAssets(String file) {
		try {
			String fileName = file;
			File outFile = new File(getSDPath()+"/tessdata", fileName);
			if (outFile.exists())
				outFile.delete();
			InputStream in = null;
			in = getAssets().open(fileName);
			OutputStream out = new FileOutputStream(outFile);
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String getSDPath() {
		String sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory().toString();// 获取外存目录
		}
		return sdDir;
	}
}
