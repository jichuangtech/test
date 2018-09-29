package com.starnet.webdevregviewer;

import android.content.Context;
import android.support.v4.util.Pair;

import com.starnet.sgcapi.SgcClient;
import com.starnet.sgcapi.bean.Response;
import com.starnet.sgcapi.callback.Callback;
import com.starnet.sgcapi.dev.bean.DeviceRegStatus;
import com.starnet.sgcapi.interf.IDevRegManager;
import com.starnet.sgcapi.interf.IDeviceManager;
import com.starnet.sgcapi.interf.IRouterManager;
import com.starnet.sgcapi.network.WanChangeCallback;
import com.starnet.sgcapi.network.bean.WanBean;
import com.starnet.sgcapi.network.bean.WanState;
import com.starnet.sgcapi.network.bean.WanType;
import com.starnet.sgcapi.register.bean.DevRegInfo;
import com.starnet.sgcapi.register.bean.DevRegReq;
import com.starnet.sgcapi.register.bean.DevRegStatus;
import com.starnet.sgcapi.register.bean.DevRegStatusResult;
import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.util.LogFactory;
import com.starnet.webdevregviewer.util.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Bingo on 2017/12/22.
 */
public class NetPresenter {
    private static final String TAG = NetPresenter.class.getSimpleName();
    public int mMaxWanNumber;
    private Logger mLogger = LogFactory.newLogger();
    private Context mContext;
    private static NetPresenter sInstance;
    private List<WanBean> mAvailableWanList = new ArrayList<>();

    private NetCallback mCallback;
    private Worker mWorker;
    private int mConnNameCount = 0;
    private Pair<String, String>[] mConnNameArr;
    private static final Pair<String, String> CREATE_CONN_PAIR = new Pair("新建连接", WanBean.CREATE_CONN_PAIR_VALUE);
    private final List<DeviceRegisterCallback> mDevRegCallbacks = new ArrayList<>();
    private DevRegStatus mDevRegStatus = DevRegStatus.No;
    private IRouterManager mRouterMrg;
    private IDevRegManager mDevRegMrg;
    private IDeviceManager mDeviceMrg;
    private boolean mIsInternetConnected;
    private final Object mLocked = new Object();
    private Worker mCheckConnWorker;
    private boolean mHasTr069Wan = false;
    private final List<Integer> mTr069WanIndexes = new ArrayList<>();
    private final List<Integer> mVOIPWanIndexes = new ArrayList<>();
    public HashMap<Integer, Integer> mLanHashMap;
    private static final int LAN_1 = 1;
    private static final int LAN_2 = 2;
    private static final int LAN_3 = 4;
    private static final int LAN_4 = 8;
    private static final int SSID_1 = 16;
    private static final int SSID_2 = 32;
    private static final int SSID_3 = 64;
    private static final int SSID_4 = 128;

    public void stopCheckInternet() {

    }

    public static synchronized NetPresenter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new NetPresenter(context);
        }

        return sInstance;
    }

    public static  NetPresenter getPresenter() {
       return sInstance;
    }

    private NetPresenter(Context context) {
        mContext = context;
        mWorker = new Worker("Net_Presenter_Worker");
        mMaxWanNumber = 8;
        mConnNameArr = new Pair[]{CREATE_CONN_PAIR};
        mRouterMrg = SgcClient.getSgcClient(context).getRouterManager();
        mDevRegMrg = SgcClient.getSgcClient(context).getDevRegManage();
        mDeviceMrg = SgcClient.getSgcClient(context).getDeviceManage();
        mCheckConnWorker = new Worker("Check_Conn_Thread");
        mRouterMrg.registerWanChangeCallback(new WanChangeCallback() {
            @Override
            public void onChange(WanType.Type type, WanState.State state) {
                mLogger.i(TAG, " wan change emun type: " + type + ", state: " + state);
            }
        });
        mLogger.i(TAG, " customized conf mMaxWanNumber: " + mMaxWanNumber);

        mLanHashMap = new HashMap<Integer, Integer>();
    }

    private boolean containTR069(WanBean wan) {
        int serverMode = wan.getServList();
        return (serverMode & WanType.SERVER_TR069) == WanType.SERVER_TR069;
    }

    private boolean containVOIP(WanBean wan) {
        int serverMode = wan.getServList();
        return (serverMode & WanType.SERVER_VOIP) == WanType.SERVER_VOIP;
    }


    private boolean isSupportedWan(WanBean wan) {
        // TODO: 2018/9/25 这次改版后支持所有的ip版本，后期有其他需求的话，修改返回值的业务逻辑
//        WanBean.IP_VERSION_IPV4
        return true;
    }




    private Callback<DevRegStatusResult> mGetDevRegStatusCallBack = new Callback<DevRegStatusResult>() {
        @Override
        public void onResponse(final Response<DevRegStatusResult> response) {
            mLogger.d(TAG, " mGetDevRegStatusCallBack response: " + response);
            if (response != null
                    && response.isSuccessful()
                    && response.getData() != null) {
                mDevRegStatus = response.getData().getRegisterStatus();
                notifyRegStatusSuccess(response);
            } else {
                notifyRegStatusFailed(response);
            }
        }
    };


    private Callback<String> mRegisterDevCallBack = new Callback<String>() {
        @Override
        public void onResponse(Response<String> response) {
            mLogger.d(TAG, " regDev resp: " + response);
            if (response != null) {
                if (response.isSuccessful()) {
                    notifyRegisterSuccess(response);
                } else {
                    notifyRegisterFailed(response);
                }
            }
        }
    };


    private Callback<DevRegInfo> mGetDevRegInfoCallBack = new Callback<DevRegInfo>() {
        @Override
        public void onResponse(Response<DevRegInfo> response) {
            mLogger.d(TAG, " GetDevRegInfo response: " + response);
            if (response != null
                    && response.isSuccessful()
                    && response.getData() != null
                    && response.getData().isSuccess()) {
                notifyRegisterInfoUpdate(response);
            } else {
                notifyRegisterInfoFailed(response);
            }
        }
    };

    //1 获取路由器是否已经 进行设备注册了
    public void getDevRegStatus() {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                mDevRegMrg.getDevRegStatus().asycn(mGetDevRegStatusCallBack);
            }
        });
    }

    //2 进行路由器的设备注册
    public void registerDev(final String password) {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                mDevRegMrg.registerDev(new DevRegReq(password)).asycn(mRegisterDevCallBack);
            }
        });
    }

    //3 获取设备住的过程中的注册信息
    public void getDevRegInfo() {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                mDevRegMrg.getDevRegInfo().setTimeout(28).asycn(mGetDevRegInfoCallBack);
            }
        });
    }

    public void forbindJump() {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Response resp = mDeviceMrg.setStatusResult(new DeviceRegStatus(0, 1)).
                        setTimeout(28).sync();
                mLogger.i(TAG, " forbindJump response: " + resp);
            }
        });
    }

    public void setDevRegStatusSuccessWhenManual() {
        mWorker.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                Response resp = mDeviceMrg.setStatusResult(new DeviceRegStatus(1, 1)).
                        setTimeout(28).sync();
                mLogger.i(TAG, " setDevRegStatusSuccessWhenManual response: " + resp);
            }
        });
    }

    public void addNetCallback(NetCallback callback) {
        mCallback = callback;
    }

    public void removeNetCallback(NetCallback callback) {
        if (mCallback == callback) {
            mCallback = null;
        }
    }

    public void addDevRegCallback(DeviceRegisterCallback callback) {
        mLogger.i(TAG, " addDevRegCallback mDevRegCallback.size: " + mDevRegCallbacks.size()
                + ", callback: " + callback);
        synchronized (mDevRegCallbacks) {
            if (!mDevRegCallbacks.contains(callback)) {
                mDevRegCallbacks.add(callback);
            }
        }
    }

    public void removeDevRegCallback(DeviceRegisterCallback callback) {
        synchronized (mDevRegCallbacks) {
            mDevRegCallbacks.remove(callback);
        }
    }

    private void notifyRegStatusSuccess(com.starnet.sgcapi.bean.Response<DevRegStatusResult> response) {
        synchronized (mDevRegCallbacks) {
            for (DeviceRegisterCallback callback : mDevRegCallbacks) {
                callback.onRegStatusSuccess(response);
            }
        }
    }

    private void notifyRegStatusFailed(Response<DevRegStatusResult> response) {
        synchronized (mDevRegCallbacks) {
            for (DeviceRegisterCallback callback : mDevRegCallbacks) {
                callback.onRegStatusFailed(response);
            }
        }
    }

    private void notifyRegisterSuccess(Response response) {
        mLogger.d(TAG, " notifyRegisterSuccess mDevRegCallbacks.size: " + mDevRegCallbacks.size());
        synchronized (mDevRegCallbacks) {
            for (DeviceRegisterCallback callback : mDevRegCallbacks) {
                callback.onRegSuccess(response);
            }
        }
    }

    private void notifyRegisterFailed(Response response) {
        synchronized (mDevRegCallbacks) {
            for (DeviceRegisterCallback callback : mDevRegCallbacks) {
                callback.onRegFailed(response);
            }
        }
    }

    private void notifyRegisterInfoUpdate(Response<DevRegInfo> response) {
        mLogger.d(TAG, " notifyRegisterInfoUpdate mDevRegCallbacks.size: " + mDevRegCallbacks.size());
        synchronized (mDevRegCallbacks) {
            for (DeviceRegisterCallback callback : mDevRegCallbacks) {
                callback.onRegInfoUpdate(response);
            }
        }
    }

    private void notifyRegisterInfoFailed(Response<DevRegInfo> response) {
        mLogger.d(TAG, " notifyRegisterInfoFailed cb.size: " + mDevRegCallbacks.size());
        synchronized (mDevRegCallbacks) {
            for (DeviceRegisterCallback callback : mDevRegCallbacks) {
                callback.onRegInfoFailed(response);
            }
        }
    }

    public boolean isInternetConnected() {
        return mIsInternetConnected;
    }


    public interface NetCallback {
        void onSetCallback(Response response);

        void onInfoUpdate();

        void onInfoGetFailed(Response response);
    }

    public interface DeviceRegisterCallback {

        void onRegStatusSuccess(Response<DevRegStatusResult> response);

        void onRegStatusFailed(Response<DevRegStatusResult> response);

        void onRegSuccess(Response response);

        void onRegFailed(Response response);

        void onRegInfoUpdate(Response<DevRegInfo> response);

        void onRegInfoFailed(Response<DevRegInfo> response);
    }

    public Pair<String, String>[] getConnNameArr() {
        return mConnNameArr;
    }

    public int getMaxConnNameCount() {
        return mMaxWanNumber;
    }

    public int getConnNameCount() {
        return mConnNameCount;
    }

    public WanBean getCurrWanBean(int index) {
        return mAvailableWanList.get(index);
    }

    public List<WanBean> getWanList() {
        return mAvailableWanList;
    }

    public boolean hasDevReg() {
        return DevRegStatus.Yes == mDevRegStatus;
    }

    public boolean canUseTR069Wan(int wanIndex) {
        synchronized (mTr069WanIndexes) {
            return mTr069WanIndexes.isEmpty() || mTr069WanIndexes.contains(wanIndex);
        }
    }


    public boolean canUseVOIPWan(int wanIndex) {
        synchronized (mVOIPWanIndexes) {
            return mVOIPWanIndexes.isEmpty() || mVOIPWanIndexes.contains(wanIndex);
        }
    }
}
