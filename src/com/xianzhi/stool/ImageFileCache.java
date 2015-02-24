package com.xianzhi.stool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import com.xianzhi.tesseract.TrainingApp;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.StatFs;


public class ImageFileCache {
    private static final String WHOLESALE_CONV = "";
                                                            
    private static final int MB = 1024*1024;
    private static final int CACHE_SIZE = 10;
    private static final int FREE_SD_SPACE_NEEDED_TO_CACHE = 10;
    /**
	 * 检查存储卡是否插入
	 * 
	 * @return
	 */
	public static boolean isHasSdcard() {
		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
	}                                                  
    public ImageFileCache() {
        //清理文件缓存
        removeCache(getDirectory());
    }
    public static void compressImage(String from , String to,boolean delete){
    	File file = new File(from);
    	Bitmap bitmap;
		if (file.length() > 1024*1024 * 2) {
			L.i("getimage", "480*800");
			bitmap = PictureUtil.getimage(from, 1000, 1000, 1024*1024 * 2);
		}else{
			L.i("getSmallBitmap", "getSmallBitmap");
			bitmap=PictureUtil.getimage(from, 1000, 1000, 1024*1024 * 2);
		}
		if(bitmap==null){
			L.i("bitmap=null", "getSmallBitmap");
			return;
		}
		File tofile = new File(to);
        try {
        	FileOutputStream outStream = new FileOutputStream(tofile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outStream);
            bitmap.recycle();
            if(delete){
            	new File(from).delete();
        	}
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            L.w("ImageFileCache", "FileNotFoundException");
        } catch (IOException e) {
            L.w("ImageFileCache", "IOException");
        }
	}                                                      
    /** 从缓存中获取图片 **/
    public Bitmap getImage(final String url,String type) {    
        final String path = getDirectory()  +type+ convertUrlToFileName(url);
        L.i(path, path);
        File file = new File(path);
        if (file.exists()) {
            Bitmap bmp;
			bmp = PictureUtil.getSmallBitmap(path);
			if (bmp == null) {
			    file.delete();
			} else {
			    updateFileTime(path);
			    return bmp;
			}
            
        }
        return null;
    }
    private Bitmap convertBitmap(File file) throws IOException {
        Bitmap bitmap = null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        FileInputStream fis = new FileInputStream(file.getAbsolutePath());
        BitmapFactory.decodeStream(fis, null, o);
        fis.close();
        final int REQUIRED_SIZE = 70;
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                        break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
        }
        BitmapFactory.Options op = new BitmapFactory.Options();
        op.inSampleSize = scale;
        fis = new FileInputStream(file.getAbsolutePath());
        bitmap = BitmapFactory.decodeStream(fis, null, op);
        fis.close();
        return bitmap;
}                                                          
    /** 将图片存入文件缓存 **/
    public void saveBitmap(Bitmap bm, String url,String type) {
        if (bm == null) {
            return;
        }
        //判断sdcard上的空间
        if (FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSd()) {
            //SD空间不足
            return;
        }
        String filename = convertUrlToFileName(url);
        String dir = getDirectory();
        File dirFile = new File(dir);
        if (!dirFile.exists())
            dirFile.mkdirs();            
        File file = new File(dir +"/" + type+filename);
        
        try {
            file.createNewFile();
            OutputStream outStream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 60, outStream);
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            L.w("ImageFileCache", "FileNotFoundException");
        } catch (IOException e) {
            L.w("ImageFileCache", "IOException");
        }
    } 
                                                                
    /**
     * 计算存储目录下的文件大小，
     * 当文件总大小大于规定的CACHE_SIZE或者sdcard剩余空间小于FREE_SD_SPACE_NEEDED_TO_CACHE的规定
     * 那么删除40%最近没有被使用的文件
     */
    private boolean removeCache(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null) {
            return true;
        }
        if (!android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED)) {
            return false;
        }
                                                            
        int dirSize = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains(WHOLESALE_CONV)) {
                dirSize += files[i].length();
            }
        }
                                                            
        if (dirSize > CACHE_SIZE * MB || FREE_SD_SPACE_NEEDED_TO_CACHE > freeSpaceOnSd()) {
            int removeFactor = (int) ((0.4 * files.length) + 1);
            Arrays.sort(files, new FileLastModifSort());
            for (int i = 0; i < removeFactor; i++) {
                if (files[i].getName().contains(WHOLESALE_CONV)) {
                    files[i].delete();
                }
            }
        }
                                                            
        if (freeSpaceOnSd() <= CACHE_SIZE) {
            return false;
        }
                                                                    
        return true;
    }
                                                                
    /** 修改文件的最后修改时间 **/
    public void updateFileTime(String path) {
        File file = new File(path);
        long newModifiedTime = System.currentTimeMillis();
        file.setLastModified(newModifiedTime);
    }
                                                                
    /** 计算sdcard上的剩余空间 **/
    private int freeSpaceOnSd() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdFreeMB = ((double)stat.getAvailableBlocks() * (double) stat.getBlockSize()) / MB;
        return (int) sdFreeMB;
    } 
                                                                
    /** 将url转成文件名 **/
    private String convertUrlToFileName(String url) {
        String[] strs = url.split("/");
        return strs[strs.length - 1] + WHOLESALE_CONV;
    }
                                                                
    /** 获得缓存目录 **/
    private String getDirectory() {
        String dir = TrainingApp.appPath;
        
        return dir;
    }
                                                                
    /** 取SD卡路径 **/
    private String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();  //获取根目录
        }
        if (sdDir != null) {
            return sdDir.toString();
        } else {
            return "";
        }
    } 
                                                            
    /**
     * 根据文件的最后修改时间进行排序
     */
    private class FileLastModifSort implements Comparator<File> {
        public int compare(File arg0, File arg1) {
            if (arg0.lastModified() > arg1.lastModified()) {
                return 1;
            } else if (arg0.lastModified() == arg1.lastModified()) {
                return 0;
            } else {
                return -1;
            }
        }
    }
                                                            
}