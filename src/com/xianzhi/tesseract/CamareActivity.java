package com.xianzhi.tesseract;


import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.mikewong.tool.tesseract.ImgPretreatment;
import com.xianzhi.stool.DensityUtil;
import com.xianzhi.stool.ImageFileCache;
import com.xianzhi.stool.L;
import com.xianzhi.stool.PictureUtil;
import com.xianzhi.view.CameraManager;
import com.xianzhi.view.CaptureLayout;

public class CamareActivity extends Activity implements SurfaceHolder.Callback{
	private CaptureLayout m_clCapture;
	private boolean m_bHasSurface;
	private boolean m_bPreviewReady = false;  
	private Bitmap m_bmOCRBitmap; 
	private boolean m_bScreenRequestPicture=false;
	
	private String textResult;
	private static String LANGUAGE = "eng";
	private final BroadcastReceiver m_brSDcardEvent = new BroadcastReceiver() 
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
		}
		
	};
	private int rect_height;
	private int screenWidth;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		CameraManager.Initialize(getApplication());
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
		rect_height=DensityUtil.dip2px(getApplicationContext(), 25);
		
		WindowManager wm = this.getWindowManager();
		 
		DisplayMetrics  dm = new DisplayMetrics();     
		   
		getWindowManager().getDefaultDisplay().getMetrics(dm);     
		   
		screenWidth = dm.widthPixels;               
		
		setContentView(R.layout.activity_camera);
		m_clCapture = (CaptureLayout)findViewById(R.id.mezzofanti_capturelayout_view);
		//CameraManager.SetImgDivisor(2);
		m_clCapture.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				m_bScreenRequestPicture=true;
				//RequestCameraFocus();
			}
		});
		m_bHasSurface=false;
	}
	@Override
	protected void onPause() 
	{
		unregisterReceiver(m_brSDcardEvent);
		StopCamera();
		if(timer!=null){
			timer.cancel();
		}
		super.onPause();
	} 
	@Override  
	public void onDestroy() 
	{
		StopCamera();
		super.onDestroy();
	}    
	@Override
	protected void onResume() 
	{
		super.onResume();

		// remove ocr results
		m_bmOCRBitmap = null;
		System.gc();

		// install an intent filter to receive SD card related events.
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
		intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
		intentFilter.addAction(Intent.ACTION_MEDIA_CHECKING);
		intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
		intentFilter.addDataScheme("file");
		registerReceiver(m_brSDcardEvent, intentFilter);

		m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_startCamera);


		// returned in Capture-mode     	  
		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.mezzofanti_preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (m_bHasSurface) 
		{
			// The activity was paused but not stopped, so the surface still exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			InitCamera(surfaceHolder);
		} else 
		{
			// Install the callback and wait for surfaceCreated() to init the camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		if(timer!=null){
			timer.cancel();
		}
		timer = new Timer(true);
		TimerTask task = new TimerTask(){  
			public void run() {  
				Message message = new Message();      
				message.what = R.id.time_task;      
				m_MezzofantiMessageHandler.sendMessage(message);    
			}  
		};
		timer.schedule(task,3000, 3000); //延时1000ms后执行，1000ms执行一次
		
	}
	Timer timer;
	  
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		Log.i(TAG, "surfaceChanged");
		
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		if (!m_bHasSurface) 
		{
			// set the surface holder for the camera's use
			m_bHasSurface = true;
			SetCameraSurfaceHolder(holder);
			m_MezzofantiMessageHandler.sendEmptyMessage(R.id.mezzofanti_startCamera);
		} 	
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		m_bHasSurface = false;
	}
	
	
	private Handler m_MezzofantiMessageHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
			case R.id.mezzofanti_startCamera:
				InitCamera();
				break;
			case R.id.cameramanager_focus_succeded:
				Log.i(TAG, "cameramanager_focus_succeded");
				PlaySoundOnFocus();
				
				if (m_bScreenRequestPicture)
					RequestCameraTakePicture();
				m_bScreenRequestPicture = false;
				break;
				
			case R.id.cameramanager_requestpicture:
				Log.v(TAG, "handleMessage() R.id.decode");
				
				try
				{
					// save the file on disk
					FileOutputStream fs = new FileOutputStream(TrainingApp.appPath + "img.jpg");
					fs.write((byte[])msg.obj, 0, ((byte[]) msg.obj).length);
					fs.close();

					m_bmOCRBitmap = BitmapFactory.decodeByteArray((byte[]) msg.obj, 0, ((byte[]) msg.obj).length);
					m_bmOCRBitmap = PictureUtil.createRotateBitmap(m_bmOCRBitmap);
					msg.obj = null;
					System.gc();
					ImageFileCache file=new ImageFileCache();
					file.saveBitmap(m_bmOCRBitmap, "/1.jpg", "0");
					Log.v(TAG, "w="+m_bmOCRBitmap.getWidth() + " h="+m_bmOCRBitmap.getHeight());
					
					
					rect_height=rect_height*CameraManager.PreviewWidth/screenWidth;
					m_bmOCRBitmap = Bitmap.createBitmap(m_bmOCRBitmap, CameraManager.PreviewHeight/6, CameraManager.PreviewWidth/2-rect_height+15, CameraManager.PreviewHeight/6*4, rect_height*2, null, false);
					file.saveBitmap(m_bmOCRBitmap, "/2.jpg", "0");
					
					// 新线程来处理识别
					new Thread(new Runnable() {
						@Override
						public void run() {
							Bitmap bitmapTreated = ImgPretreatment
									.converyToGrayImg(m_bmOCRBitmap);
							//L.i("asdadasdas");
							m_bmOCRBitmap.recycle();
							textResult = doOcr(bitmapTreated, LANGUAGE);
							L.i("asdadasdas:"+textResult);;
						}
					}).start();
					
				}
				catch (Throwable th) 
				{
					Log.v(TAG, "exception: handler-cmrequestpic: "+ th.toString());
					m_clCapture.ShowWaiting("");
					break;
				}
				break;
			case R.id.time_task:
				m_bScreenRequestPicture=true;
				RequestCameraFocus();
				break;
			}
			super.handleMessage(msg);
		}

	};
	/**
	 * 获取sd卡的路径
	 * 
	 * @return 路径的字符串
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取外存目录
		}
		return sdDir.toString();
	}
	/**
	 * 进行图片识别
	 * 
	 * @param bitmap
	 *            待识别图片
	 * @param language
	 *            识别语言
	 * @return 识别结果字符串
	 */
 	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getSDPath(), language);

		// 必须加此行，tess-two要求BMP必须为此配置
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}
	
	/**
	 * Play a predefined sound, when camera focused.
	 */
	private void PlaySoundOnFocus()
	{
		ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, 100);
		tg.startTone(ToneGenerator.TONE_PROP_BEEP2);
	}
	/*
	 * ----------------------------------------------------------------------------------------
	 * Camera
	 * ----------------------------------------------------------------------------------------
	 */

	/**
	 * Set the camera surface holder.
	 */
	private void SetCameraSurfaceHolder(SurfaceHolder surfaceHolder)
	{
		if (surfaceHolder == null || CameraManager.get() == null)
			return;

		CameraManager.get().SetSurfaceHolder(surfaceHolder);
	}

	/**
	 * Initialize the camera, open the driver.
	 * @param surfaceHolder the local surface holder.
	 */
	private void InitCamera(SurfaceHolder surfaceHolder) 
	{
		if (CameraManager.get() == null)
			return;

		m_bPreviewReady = true;
		CameraManager.get().OpenDriver(surfaceHolder);
		CameraManager.get().StartPreview();		
		//RequestCameraFocus();
	}

	/**
	 *  Initialize the camera, open the driver (no params), we assume the surface holder was set apriori with SetCameraSurfaceHolder.
	 */
	private static final String TAG="CarmareActivity";
	private void InitCamera() 
	{
		Log.v(TAG, "InitCamera: start");		
		if (CameraManager.get() == null)
			return;

		Log.v(TAG, "InitCamera: OpenDriver");

		m_bPreviewReady = true;
		if (CameraManager.get().OpenDriver())
		{
			Log.v(TAG, "InitCamera: StartPreview");
			CameraManager.get().StartPreview();
			//RequestCameraFocus();
		}
		Log.v(TAG, "InitCamera: end");
	}

	/**
	 * Stop the camera preview and driver.
	 */
	private void StopCamera()
	{
		CameraManager.get().StopPreview();
		CameraManager.get().CloseDriver();
		m_bPreviewReady = false;	
	}

	
	
	
	/**
	 * Request the camera focus
	 * @return if function call succeeded
	 */
	private boolean RequestCameraFocus()
	{
		//m_clCapture.DrawFocusIcon(true, m_bHorizontalDisplay);  
		CameraManager.get().RequestCameraFocus(m_MezzofantiMessageHandler);
		CameraManager.get().RequestAutoFocus();
		return true;
	}

	/**
	 * Request camera to take the picture
	 * @return if function call succeeded
	 */
	private boolean RequestCameraTakePicture()
	{
		if (m_bPreviewReady)
		{
			m_clCapture.DrawFocused(false, false);
			//m_clCapture.DrawFocusIcon(false, m_bHorizontalDisplay);
			CameraManager.get().RequestPicture(m_MezzofantiMessageHandler);
			CameraManager.get().GetPicture();
			m_clCapture.ShowWaiting(getString(R.string.mezzofanti_capturelayout_takingpicture));
			
			return true;
		}
		return false;
	}
	
}
