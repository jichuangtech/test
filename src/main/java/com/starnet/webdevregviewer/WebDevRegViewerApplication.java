package com.starnet.webdevregviewer;

import android.app.Application;

import com.starnet.baseconfig.ConfigManager;
import com.starnet.sgcapi.SgcClient;
import com.starnet.sgcapi.interf.ISgcClient;
import com.starnet.stbsystemapi.STBSystemApi;
import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.util.BaseConfigUtils;
import com.starnet.webdevregviewer.util.LogFactory;
import com.starnet.webdevregviewer.util.Worker;

/**
 * Created by Bingo on 2018/9/28.
 */
public class WebDevRegViewerApplication extends Application {
    private static final String TAG = WebDevRegViewerApplication.class.getSimpleName();
    public static final String MODULE_NAME = "WebDevRegViewer";
    private Logger mLogger = LogFactory.newLogger();
    private Worker mWorker;
    private ISgcClient mSgcClient;


    @Override
    public void onCreate() {
        super.onCreate();
        mLogger.i(TAG, " onCreate ");
        initModel();
        connectRemoteService();
    }

    private void initModel() {
        BaseConfigUtils.initConfig(getApplicationContext(), ConfigManager.getInstance());
        mSgcClient = SgcClient.getSgcClient(this);
        mWorker = new Worker("Gateway_Guide_Thread");
        STBSystemApi.init(this);
        NetPresenter.getInstance(this);
        WebDevRegManager.getInstance(this);
    }

    private void connectRemoteService() {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                mSgcClient.connect();
            }
        });
    }
}
