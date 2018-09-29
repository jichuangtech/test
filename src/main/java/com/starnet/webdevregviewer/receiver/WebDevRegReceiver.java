package com.starnet.webdevregviewer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.WebDevRegManager;
import com.starnet.webdevregviewer.util.LogFactory;

/**
 * Created by Bingo on 2018/9/25.
 */
public class WebDevRegReceiver extends BroadcastReceiver {
    private static final String TAG = WebDevRegReceiver.class.getSimpleName();
    private Logger mLogger = LogFactory.newLogger();

    @Override
    public void onReceive(Context context, Intent intent) {
        mLogger.i(TAG, " onReceive intent: " + intent.getAction());
        WebDevRegManager.getInstance(context).showWebDevRegDialog();
    }
}
