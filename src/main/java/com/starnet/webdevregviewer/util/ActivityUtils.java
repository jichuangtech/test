package com.starnet.webdevregviewer.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.starnet.vsdk.vbase.logger.Logger;


public class ActivityUtils {
    private static final String TAG = ActivityUtils.class.getSimpleName();
    private static Logger sLogger = LogFactory.newLogger();

    public static boolean isActivityOnTop(Context context, Class activityClass) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null && manager.getRunningTasks(1).size() > 0) {
                ComponentName component = manager.getRunningTasks(1).get(0).topActivity;
                if (component.getClassName().contains(activityClass.getName())) {
                    return true;
                }
            }
        } catch (SecurityException e) {
            sLogger.e(TAG, " isActivityOnTop error: " + e.getMessage());
        } catch (Exception e) {
        }

        return false;
    }

    public static void startActivity(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            sLogger.e(TAG, " startActivity , " + e.toString());
        }
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, String tip) {
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            sLogger.e(TAG, " startActivity , " + e.toString());
        }
    }

}
