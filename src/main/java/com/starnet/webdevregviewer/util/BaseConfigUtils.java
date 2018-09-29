package com.starnet.webdevregviewer.util;

import android.content.Context;

import com.starnet.baseconfig.AConfigListener;
import com.starnet.baseconfig.ConfigManager;
import com.starnet.baseconfig.type.BoolType;
import com.starnet.baseconfig.type.ConfigType;
import com.starnet.baseconfig.type.IntType;
import com.starnet.baseconfig.type.StringType;
import com.starnet.device.conf.params.StarnetParamsConstant;
import com.starnet.device.conf.params.StarnetParamsManager;
import com.starnet.device.conf.params.StarnetParamsManagerImpl;
import com.starnet.vsdk.vbase.logger.LogLevel;
import com.starnet.vsdk.vbase.logger.LogManager;
import com.starnet.vsdk.vbase.logger.Logger;
import static com.starnet.webdevregviewer.util.BaseConfigUtils.BaseConfigKeys.*;

/**
 * Created by Bingo on 2018/1/29.
 */
public class BaseConfigUtils {
    private static final String TAG = BaseConfigUtils.class.getSimpleName();
    private static final Logger sLogger = LogFactory.newLogger();
    public static final String BASE_CONFIG_PROVIDER_URI = "cn.starnet.gatewayguide.config";
    public static final String CONFIG_FILE_NAME = "gatewayguide.config";
    public static final int MIN_WAN_NUMBER = 1;
    public static final int MAX_WAN_NUMBER = 8;
    private static StarnetParamsManager sStarnetParamsManager;

    public static class BaseConfigKeys {
        //1 日志登记
        public static final String LOG_LEVEL = "Loglevel";
        //2 是 电口 还是 光口
        public static final String WAN_PORT_TYPE = "WanPortType";
        //3 最后一个配置的界面
        public static final String LAST_CONFIG_VIEW = "LastConfigView";
        //4 设备注册间隔
        public static final String DEV_REG_INFO_INTERVAL = "DevRegInfoInterval";
        //5 是否带VOIP
        public static final String HAS_VOIP = "HasVoIP";
        //6 MaxWanNumber
        public static final String MAX_WAN_NUMBER = "MaxWanNumber";
        //7 是否显示 设置按钮
        public static final String SETTING_BTN_VISIBLE = "SettingBtnVisible";
        //8 配置方式
        public static final String CONFIGURATION_TYPE = "ConfigurationType";
        //9 设置应用的包名
        public static final String SETTINGS_PKG_NAME = "SettingsPkgName";
        //10 设置应用的类名
        public static final String SETTINGS_CLS_NAME = "SettingsClsName";
        //11  CPU型号
        public static final String CPU_MODE = "CPUMode";
        //12  是否隐藏 "隐藏wifi" 的UI配置项目
        public static final String IS_SHOW_WIFI_ENABLE_HIDE = "IsShowWifiEnableHide";
        //13地区码
        public static final String AREA_CODE = "AreaCode";
    }

    public static final int WAN_PORT_TYPE_ELECTRIC = 0; //电口   默认 集团用
    public static final int WAN_PORT_TYPE_LIGHT = 1;   //光口   重庆用

    public static final String LAST_CONFIG_VIEW_NET = "net";
    public static final String LAST_CONFIG_VIEW_VOIP = "voip";

//    public static final int DEF_WAN_PORT_TYPE = WAN_PORT_TYPE_LIGHT;
    public static final int DEF_WAN_PORT_TYPE = WAN_PORT_TYPE_ELECTRIC;
    public static final int DEF_DEV_REG_INFO_INTERVAL = 5000;
    public static final boolean DEF_HAS_VOIP = false;
    public static final String DEF_LAST_CONFIG_VIEW= LAST_CONFIG_VIEW_NET;
    public static final int DEF_MAX_WAN_NUMBER= 5;
//    public static final int DEF_DEV_REG_INFO_INTERVAL = 4000;
    public static final int CONFIGURATION_TYPE_MANUAL = 1;
    public static final int CONFIGURATION_TYPE_AUTO = 2;
    public static final int CONFIGURATION_TYPE_BOTH = 3;
    public static final int DEF_CONFIGURATION_TYPE = CONFIGURATION_TYPE_BOTH;

    public static final String DEF_SETTINGS_PKG_NAME = "";
    public static final String DEF_SETTINGS_CLS_NAME = "";
    public static final String CPU_MODE_HISI = "HiSi";
    public static final String CPU_MODE_AMLOGIC = "Amlogic";
    public static final String CPU_MODE_ZXIC = "Zxic";
    public static final String DEF_CPU_MODE = CPU_MODE_HISI;

    public static void initConfig(final Context context, ConfigManager baseConfigMrg) {
        sStarnetParamsManager = new StarnetParamsManagerImpl(context);
        baseConfigMrg.init(context, BaseConfigUtils.CONFIG_FILE_NAME);
        baseConfigMrg.setProviderUri(BaseConfigUtils.BASE_CONFIG_PROVIDER_URI);

        baseConfigMrg.registerListener(null, new AConfigListener() {
            @Override
            public void onConfigChanged(String key, ConfigType configType) {
                sLogger.i(TAG, "config change : '" + key + "' = '" + configType.getValue() + "'");
            }
        });

        //1 日志等级
        LogManager.init(context);
        baseConfigMrg.addConfigKey(new IntType(LOG_LEVEL, 3, 0, 5) {
            @Override
            public boolean set(int value) {
                LogManager.setLevel(LogLevel.getLogLevel(value));
                return true;
            }
        });

        //2 是 电口 还是 光口
        LogManager.init(context);
        baseConfigMrg.addConfigKey(new IntType(WAN_PORT_TYPE, DEF_WAN_PORT_TYPE,
                WAN_PORT_TYPE_ELECTRIC, WAN_PORT_TYPE_LIGHT) {}, false);

        //3 最后一个配置界面
        baseConfigMrg.addConfigKey(new StringType(LAST_CONFIG_VIEW, DEF_LAST_CONFIG_VIEW), false);

        //4 设备注册间隔
        baseConfigMrg.addConfigKey(new IntType(DEV_REG_INFO_INTERVAL, DEF_DEV_REG_INFO_INTERVAL), false);

        //5 是否带VoIP  默认不带
        baseConfigMrg.addConfigKey(new BoolType(HAS_VOIP, DEF_HAS_VOIP), false);

        //6 最大wan连接数量
        baseConfigMrg.addConfigKey(new IntType(BaseConfigKeys.MAX_WAN_NUMBER, DEF_MAX_WAN_NUMBER, MIN_WAN_NUMBER, MAX_WAN_NUMBER), false);

        baseConfigMrg.initKeyEnd();
    }

    public static boolean isElectricPort() {
        int type = sStarnetParamsManager.getInt(WAN_PORT_TYPE, DEF_WAN_PORT_TYPE);
        sLogger.e(TAG, "  customized conf WAN_PORT_TYPE type: "  + type);
        return WAN_PORT_TYPE_ELECTRIC == type;
    }

    public static String getAreaCode() {
        return sStarnetParamsManager.getString(BaseConfigKeys.AREA_CODE, StarnetParamsConstant.AREA_BJ_CMCC);
    }

    public static boolean isShandong() {
        return StarnetParamsConstant.AREA_SD.equals(getAreaCode());
    }

    public static int getMaxWanNumber() {
        return sStarnetParamsManager.getInt(BaseConfigKeys.MAX_WAN_NUMBER, DEF_MAX_WAN_NUMBER);
    }

    public static boolean hasVoIP() {
        return sStarnetParamsManager.getBoolean(HAS_VOIP, DEF_HAS_VOIP);
    }

    public static int getDevRegInfoInterval() {
        return sStarnetParamsManager.getInt(DEV_REG_INFO_INTERVAL, DEF_DEV_REG_INFO_INTERVAL);
    }

    public static boolean isShowSettingsBtn() {
        return sStarnetParamsManager.getBoolean(BaseConfigKeys.SETTING_BTN_VISIBLE, true);
    }

    public static boolean isShowWifiEnableHidden() {
        return sStarnetParamsManager.getBoolean(BaseConfigKeys.IS_SHOW_WIFI_ENABLE_HIDE, false);
    }

    public static int getConfigurationType() {
        return sStarnetParamsManager.getInt(CONFIGURATION_TYPE, DEF_CONFIGURATION_TYPE);
    }

    public static String getSettingsPkgName() {
        return sStarnetParamsManager.getString(BaseConfigKeys.SETTINGS_PKG_NAME, DEF_SETTINGS_PKG_NAME);
    }

    public static String getSettingsClsName() {
        return sStarnetParamsManager.getString(BaseConfigKeys.SETTINGS_CLS_NAME, DEF_SETTINGS_CLS_NAME);
    }

    public static String getCPUMode() {
        return sStarnetParamsManager.getString(BaseConfigKeys.CPU_MODE, DEF_CPU_MODE);
    }
}
