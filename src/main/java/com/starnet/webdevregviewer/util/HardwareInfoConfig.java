package com.starnet.webdevregviewer.util;

import android.content.Context;

import com.starnet.device.conf.params.StarnetParamsManagerImpl;
import com.starnet.sgcapi.SgcClient;
import com.starnet.sgcapi.bean.Response;
import com.starnet.sgcapi.dev.bean.HardwareInfo;
import com.starnet.vsdk.vbase.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bingo on 2018/3/27.
 */
public class HardwareInfoConfig {
    private static final String TAG = HardwareInfoConfig.class.getSimpleName();
    private static HardwareInfoConfig sInstance;
    private Logger mLogger = LogFactory.newLogger();
    private boolean mHasVoIP;
    private Context mContext;
    private List<Callback> mCallbacks = new ArrayList<>();
    private Worker mWorker = new Worker("VoIP_Config_Worker");
    private final Object mLocked = new Object();
    private StarnetParamsManagerImpl starnetParamsManager;
    public int mLanPortNum = 0;
    public int mSSIDPortNum = 0;

    private HardwareInfoConfig(Context context) {
        mContext = context;
        starnetParamsManager = new StarnetParamsManagerImpl(mContext);
    }

    public static synchronized HardwareInfoConfig getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HardwareInfoConfig(context);
        }

        return sInstance;
    }

    public boolean hasVoIP() {
        synchronized (mLocked) {
            return mHasVoIP;
        }
    }

    private void setHasVoIP(boolean hasVoIP) {
        synchronized (mLocked) {
            mHasVoIP = hasVoIP;
        }
    }

    public interface Callback {
        void onVoIPConfigChange(boolean hasVoIP);

        void onWanPortTypeChange();

        void onWanPortNumberChange(int LanPortNum, int SSIDPortNum);
    }

    public void addConfigChangeCallback(Callback callback) {
        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    public void removeConfigChangeCallback(Callback callback) {
        if(mCallbacks.contains(callback)) {
            mCallbacks.remove(callback);
        }
    }

    public void refreshConfig() {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Response<HardwareInfo> resp =
                        SgcClient.getSgcClient(mContext).getDeviceManage().getHardwareInfo().setTimeout(25).sync();
                mLogger.i(TAG, " refreshConfig resp: " + resp);
                if (resp != null &&
                        resp.isSuccessful()
                        && resp.getData() != null) {
                    HardwareInfo info = resp.getData();
                    boolean hasVoIP = (1 == info.getSupportVoip());
                    mLanPortNum = info.getLanPortNum();
                    mSSIDPortNum = info.getSSIDPortNum();
                    mLogger.d(TAG, "mLanPortNum = " + mLanPortNum + ",mSSIDPortNum = " + mSSIDPortNum);
                    //VOIP配置信息
                    starnetParamsManager.setBoolean(BaseConfigUtils.BaseConfigKeys.HAS_VOIP, hasVoIP);
//                    starnetParamsManager.setString(BaseConfigUtils.BaseConfigKeys.LAST_CONFIG_VIEW,
//                            hasVoIP ? BaseConfigUtils.LAST_CONFIG_VIEW_VOIP : BaseConfigUtils.LAST_CONFIG_VIEW_NET);

                    //WanPortType配置信息
                    starnetParamsManager.setInt(BaseConfigUtils.BaseConfigKeys.WAN_PORT_TYPE, info.getWanPortType());
                    setHasVoIP(hasVoIP);

                    for (Callback callback : mCallbacks) {
                        callback.onVoIPConfigChange(hasVoIP);
                        callback.onWanPortTypeChange();
                        callback.onWanPortNumberChange(mLanPortNum, mSSIDPortNum);
                    }
                }
            }
        });


    }
}
