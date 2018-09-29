package com.starnet.webdevregviewer;

import android.content.Context;

import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.dialog.WebDevRegInfoDialog;
import com.starnet.webdevregviewer.util.LogFactory;

/**
 * Created by Bingo on 2018/9/26.
 */
public class WebDevRegManager {
    private static final String TAG = WebDevRegManager.class.getSimpleName();
    private static WebDevRegManager sInstance;
    private Logger mLogger = LogFactory.newLogger();
    private WebDevRegInfoDialog mDialog;
    private Context mContext;

    private WebDevRegManager(Context context) {
        mContext = context;
    }

    public static synchronized WebDevRegManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new WebDevRegManager(context);
        }

        return sInstance;
    }

    public void showWebDevRegDialog() {
        mLogger.i(TAG, " showWebDevRegDialog mDialog: " + mDialog
                + ", isShowing: " + (mDialog != null && mDialog.isShowing()));

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

        mDialog = new WebDevRegInfoDialog(mContext);
        mDialog.show();
    }

}
