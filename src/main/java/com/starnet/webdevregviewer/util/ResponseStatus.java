package com.starnet.webdevregviewer.util;

import com.starnet.webdevregviewer.R;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by Bingo on 2018/1/17.
 */
public class ResponseStatus {
    public static final int DEF_ERROR = 10000;
    public static final int TIME_OUT = 4001;
    public static final int ERROR_INTERNA = 4002;

    public static final int DEV_REG_ERROR_1 = -1;
    public static final int DEV_REG_ERROR_2 = -2;
    public static final int DEV_REG_ERROR_3 = -3;
    public static final int DEV_REG_ERROR_4 = -4;
    public static final int DEV_REG_ERROR_5 = -5;

    private static Map<Integer ,Integer> sStatusMap = new HashMap<>();

    static {
        sStatusMap.put(DEF_ERROR, R.string.dev_reg_error_def_tip);

        //1 Sgc 与 C代码的错误提示
        sStatusMap.put(TIME_OUT, R.string.time_out_tip);
        sStatusMap.put(ERROR_INTERNA, R.string.error_internal_tip);

        //2 设备注册的错误提示
        sStatusMap.put(DEV_REG_ERROR_1, R.string.dev_reg_error_1_tip);
        sStatusMap.put(DEV_REG_ERROR_2, R.string.dev_reg_error_2_tip);
        sStatusMap.put(DEV_REG_ERROR_3, R.string.dev_reg_error_3_tip);
        sStatusMap.put(DEV_REG_ERROR_4, R.string.dev_reg_error_4_tip);
        sStatusMap.put(DEV_REG_ERROR_5, R.string.dev_reg_error_5_tip);
    }

    public static int getStatusTipRes(int statusCode, int defRes) {
        return !sStatusMap.containsKey(statusCode) ? defRes : sStatusMap.get(statusCode);
    }
}
