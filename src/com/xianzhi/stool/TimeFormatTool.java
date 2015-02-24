package com.xianzhi.stool;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.text.format.Time;

public class TimeFormatTool {
	public static String getWeekDay(Time time){
		String dataWeek = "";
		switch (time.weekDay) {
		case 1:
			dataWeek+="周一";
			break;
		case 2:
			dataWeek+="周二";
			break;
		case 3:
			dataWeek+="周三";
			break;
		case 4:
			dataWeek+="周四";
			break;
		case 5:
			dataWeek+="周五";
			break;
		case 6:
			dataWeek+="周六";
			break;
		case 7:
			dataWeek+="周日";
			break;
		default:
			break;
		}
		return dataWeek;
	}
	public static String getMonthDay(Time time){
		String dataMonth = String.format("%02d月%02d日", time.month+1,
				time.monthDay);
		return dataMonth;
	}
	public static String getHourMin(Time time){
		String HourMin = String.format("%02d:%02d", time.hour,
				time.minute);
		return HourMin;
	}
    // Get today 00:00 milliseconds from epoch.
    public static long todayMilliSeconds(final Calendar cal) {
    	Calendar calendar=(Calendar) cal.clone();
    	calendar.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0, 0);  
    	calendar.set(Calendar.MILLISECOND, 0); 
        return calendar.getTimeInMillis();
    }

    // Get today 23:59 milliseconds from epoch.
    public static long todayEndMilliSeconds(final Calendar cal){
    	Calendar calendar=(Calendar) cal.clone();
    	calendar.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE) + 1, 0, 0, 0);  
    	calendar.set(Calendar.MILLISECOND, 0); 
        return calendar.getTimeInMillis();
    }

    public static TimeZone timeZone(){
        String timeZoneId = "RPC";
        return TimeZone.getTimeZone(timeZoneId);
    }

    public static String timeString(long milliseconds){
        Date date = new Date(milliseconds);
        SimpleDateFormat formater = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        return formater.format(date);
    }
}
