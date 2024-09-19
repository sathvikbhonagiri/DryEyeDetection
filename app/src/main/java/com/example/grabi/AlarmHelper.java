package com.example.grabi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmHelper {
    private static final String TAG = "AlarmHelper";

    public static void setAlarm(Context context, int hour, int minute, String message, int requestCode) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Permission required to set exact alarms.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Permission required to set exact alarms.");
                    return;
                }
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("message", message);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            int setHour = calendar.get(Calendar.HOUR_OF_DAY);
            int setMinute = calendar.get(Calendar.MINUTE);
           // Toast.makeText(context, "Set time: " + setHour + ":" + setMinute, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Set time: " + setHour + ":" + setMinute);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                //Toast.makeText(context, "Set time adjusted to next day: " + setHour + ":" + setMinute, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Set time adjusted to next day: " + setHour + ":" + setMinute);
            }

            if (alarmManager != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    }
                    //Toast.makeText(context, "Alarm set for " + hour + ":" + minute + " with message: " + message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Alarm set for " + hour + ":" + minute + " with message: " + message);
                } catch (SecurityException e) {
                    Toast.makeText(context, "SecurityException: Cannot set exact alarm. " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "SecurityException: Cannot set exact alarm. ", e);
                }
            }
        } catch (Exception e) {
            Toast.makeText(context, "Error setting alarm: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Error setting alarm: ", e);
        }
    }

    public static void setDailyAlarms(Context context) {
        setAlarm(context, 7, 0, "Update data!", 1);
        // Uncomment the lines below to set additional alarms
         setAlarm(context, 12, 0, "Update data!", 2);
        setAlarm(context, 16, 0, "Update data!", 3);
         setAlarm(context, 22, 0, "Update data!", 4);
    }
}
