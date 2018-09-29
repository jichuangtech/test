package com.starnet.webdevregviewer;

import android.content.Context;
import android.widget.SeekBar;
import android.widget.TextView;

import com.starnet.sgcapi.bean.Response;
import com.starnet.sgcapi.register.bean.DevRegInfo;
import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.util.BaseConfigUtils;
import com.starnet.webdevregviewer.util.LogFactory;
import com.starnet.webdevregviewer.util.MainThread;
import com.starnet.webdevregviewer.util.NumberUtils;
import com.starnet.webdevregviewer.util.ResponseStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class RegisterInfoRender implements NetPresenter.DeviceRegisterCallback {
    private static final String TAG = RegisterInfoRender.class.getSimpleName();
    private final NetPresenter mNetPresenter;
    private Logger mLogger = LogFactory.newLogger();
    public static final int MAX_COUNT_TIME_OUT = 20;
    private TextView mTipTv;
    private SeekBar mSeekBar;
    private int mInterval;
    private Timer mTimer;
    private boolean mIsStart = false;
    private MyTask mTask;
    private Map<Integer, Integer> mStatusTipMap = new HashMap<>();
    private Map<Integer, Integer> mStatusCountMap = new HashMap<>();
    private Map<String, Integer> mStatusServiceMap = new HashMap<>();
    private List<RenderCallback> mCallbacks = new ArrayList<>();
    private int mTimeoutCount = 0;
    private Context mContext;
    private static final int MAX_TIMEOUT_COUNT = 20;

    public RegisterInfoRender(TextView tipTv, SeekBar seekBar, int interval) {
        mTipTv = tipTv;
        mSeekBar = seekBar;
        mInterval = interval;
        mLogger.i(TAG, " mInterval: " + mInterval);
        mNetPresenter = NetPresenter.getPresenter();
        mContext = mSeekBar.getContext();
    }

    {

        if (BaseConfigUtils.isElectricPort()) {
            // 电口 默认 集团用
            mLogger.d(TAG, " init 20% tip electric");
            mStatusTipMap.put(20, R.string.dev_reg_info_20_electric);
        } else {
            // 光口 重庆用
            mLogger.d(TAG, " init 20% tip light");
            mStatusTipMap.put(20, R.string.dev_reg_info_20_light);
        }

        mStatusTipMap.put(30, R.string.dev_reg_info_30);
        mStatusTipMap.put(40, R.string.dev_reg_info_40);
        mStatusTipMap.put(50, R.string.dev_reg_info_50);
        mStatusTipMap.put(60, R.string.dev_reg_info_60);

        mStatusServiceMap.put("INTERNET", R.string.dev_reg_service_internet);
        mStatusServiceMap.put("VOIP", R.string.dev_reg_service_voip);
        mStatusServiceMap.put("OTHER", R.string.dev_reg_service_other);
    }

    public void refreshStatusTip20() {
        if (BaseConfigUtils.isElectricPort()) {
            // 电口 默认 集团用
            mLogger.d(TAG, " refreshStatusTip20 20% tip electric");
            mStatusTipMap.put(20, R.string.dev_reg_info_20_electric);
        } else {
            // 光口 重庆用
            mLogger.d(TAG, " refreshStatusTip20 20% tip light");
            mStatusTipMap.put(20, R.string.dev_reg_info_20_light);
        }
    }

    private class MyTask extends java.util.TimerTask {
        @Override
        public void run() {
            mNetPresenter.getDevRegInfo();
        }
    }

    private boolean checkStatusTimeout(int status) {

        if (!mStatusCountMap.containsKey(status)) {
            mStatusCountMap.put(status, 0);
        }

        int count = mStatusCountMap.get(status);
        if (count >= MAX_COUNT_TIME_OUT) {
            return true;
        }
        mStatusCountMap.put(status, mStatusCountMap.get(status) + 1);
        return false;
    }

    public boolean isStart() {
        return mIsStart;
    }

    public boolean isComplete() {
        return mSeekBar.getProgress() == 100;
    }

    public void start() {
        mLogger.i(TAG, "RegInfoRender start ");
        mIsStart = true;
        mNetPresenter.addDevRegCallback(RegisterInfoRender.this);
        mTimer = new Timer();
        mTask = new MyTask();
        mTimer.schedule(mTask, 0, mInterval);
    }

    public void stop() {
        mLogger.e(TAG, "RegInfoRender stop progress: " + mSeekBar.getProgress());
        mIsStart = false;
        mNetPresenter.removeDevRegCallback(RegisterInfoRender.this);
        mStatusCountMap.clear();
        mTimeoutCount = 0;
        if (mTask != null) {
            mTask.cancel();
            mTask = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

    }

    @Override
    public void onRegInfoUpdate(Response<DevRegInfo> response) {
        if (!mIsStart) {
            mLogger.e(TAG, " onRegInfoUpdate failed, render is stop... ");
            return;
        }
        mLogger.d(TAG, " RegisterInfoRender onRegInfoUpdate response: " + response);
        final DevRegInfo info = response.getData();
        int status = info.getRegisterStatus();
//            if (checkStatusTimeout(status)) {
//                showRegErrorTip(R.string.dev_reg_error_time_out_tip);
//                stop();
//                return;
//            }

        MainThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                mSeekBar.setProgress(info.getRegisterStatus());
                refreshTip(info);
            }
        });

        if (status == 100) {
            stop();
            MainThread.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyDevRegSuccess();
                }
            }, 2000);
        }
    }

    @Override
    public void onRegInfoFailed(Response<DevRegInfo> response) {
        if (!mIsStart) {
            mLogger.e(TAG, " onRegInfoFailed failed, render is stop... ");
            return;
        }

        if (ResponseStatus.TIME_OUT == response.getResultCode() && mTimeoutCount < MAX_COUNT_TIME_OUT) {
            mTimeoutCount++;
            mLogger.e(TAG, " onRegInfoFailed failed, getResultCode is 4001, mTimeoutCount: " + mTimeoutCount);
            return;
        }

        stop();
        int errorCode = getErrorCode(response);
        notifyShowRegErrorTip(ResponseStatus.getStatusTipRes(errorCode, R.string.dev_reg_error_unknown_tip));
        mLogger.e(TAG, " RegisterInfoRender onRegInfoFailed response: " + response + ", mTimeoutCount: " + mTimeoutCount);
    }


    private void refreshTip(DevRegInfo info) {
        int status = info.getRegisterStatus();
        if (status >= 20 && status <= 60) {
            //20<x<60
            mTipTv.setText(mContext.getString(mStatusTipMap.get(status)));
        } else if (status == 100) {
            //100
            show100Tip(info);
        } else {
            // 61～99
            mTipTv.setText(String.format(mContext.getString(R.string.dev_reg_info_61_99),
                    parseService(info.getCurServiceName())));
        }
    }

    public void show100Tip(DevRegInfo info) {
        if (BaseConfigUtils.isShandong()) {
            mLogger.i(TAG, " 100% tip for shan dong...");
            mTipTv.setText(R.string.dev_reg_info_100_for_sd);
        } else {
            mLogger.i(TAG, " 100% tip for common...");
            mTipTv.setText(String.format(mContext.getString(R.string.dev_reg_info_100_format),
                    parseService(info.getServiceName()),
                    getChineseNum(info)));
        }

    }

    private String getChineseNum(DevRegInfo info) {
        int num = info.getServiceNum();
        return num == 0 ? "0" : NumberUtils.toChinese(String.valueOf(info.getServiceNum()));
    }

    private void resetTip() {
        mTipTv.setText("");
    }

    private String parseService(String service) {
        StringBuffer services = new StringBuffer();
        if (service != null) {
            String[] serArr = service.split(",");

            for (String str : serArr) {
                if (!services.toString().isEmpty()) {
                    services.append("、");
                }

                if (!mStatusServiceMap.containsKey(str)) {
                    services.append(mContext.getString(R.string.dev_reg_service_unknown));
                } else {
                    services.append(mContext.getString(mStatusServiceMap.get(str)));
                }
            }
        }

        return services.toString();
    }


    public int getErrorCode(Response<DevRegInfo> response) {
        int errorCode = ResponseStatus.DEF_ERROR;
        try {
            if (!response.isSuccessful()) {
                errorCode = response.getResultCode();
            } else if (response.getData() != null) {
                errorCode = response.getData().getErrorCode();
            }
        } catch (Exception e) {
            errorCode = ResponseStatus.DEF_ERROR;
            mLogger.e(TAG, " getErrorCode exce: " + e.getMessage());
            e.printStackTrace();
        }

        mLogger.e(TAG, " getErrorCode errorCode: " + errorCode);
        return errorCode;
    }

    @Override
    public void onRegStatusSuccess(Response response) {

    }

    @Override
    public void onRegStatusFailed(Response response) {

    }

    @Override
    public void onRegSuccess(Response response) {
        mLogger.i(TAG, " RegisterInfoRender onRegSuccess response: " + response);
    }

    @Override
    public void onRegFailed(Response response) {
    }

    public interface RenderCallback {
        void onShowRegErrorTip(int errorTipRes);

        void onDevRegSuccess();
    }

    public void addCallback(RenderCallback callback) {
        synchronized (mCallbacks) {
            if (callback != null &&
                    !mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
        }
        mLogger.i(TAG, " addCallback callback.size: " + mCallbacks.size());
    }

    public void removeCallback(RenderCallback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    private void notifyShowRegErrorTip(int errorTipRes) {
        for (RenderCallback callback : mCallbacks) {
            callback.onShowRegErrorTip(errorTipRes);
        }
    }

    private void notifyDevRegSuccess() {
        for (RenderCallback callback : mCallbacks) {
            callback.onDevRegSuccess();
        }
    }
}