package com.xianzhi.tesseract;


import android.app.Application;
import android.os.Environment;

public class TrainingApp extends Application{
	
	public static final String appPath = Environment
			.getExternalStorageDirectory() + "/.tesseractTest/";
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
	}
}
