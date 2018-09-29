package com.starnet.webdevregviewer.util;

import com.starnet.vsdk.vbase.logger.LogManager;
import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.WebDevRegViewerApplication;

/**
 * Created by Bingo on 2018/1/22.
 */
public class LogFactory {
    public static Logger newLogger() {
        return LogManager.getLogger(WebDevRegViewerApplication.MODULE_NAME);
    }
}
