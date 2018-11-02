package com.starnet.webdevregviewer.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.starnet.stbsystemapi.util.SystemProperties;
import com.starnet.vsdk.vbase.logger.Logger;
import com.starnet.webdevregviewer.R;
import com.starnet.webdevregviewer.RegisterInfoRender;
import com.starnet.webdevregviewer.util.ActivityUtils;
import com.starnet.webdevregviewer.util.BaseConfigUtils;
import com.starnet.webdevregviewer.util.HardwareInfoConfig;
import com.starnet.webdevregviewer.util.LogFactory;
import com.starnet.webdevregviewer.util.MainThread;
import com.starnet.widget.dialog.FlexDialog;
import com.starnet.widget.dialog.Theme;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Bingo on 2018/9/26.
 */
public class WebDevRegInfoDialog extends FlexDialog implements RegisterInfoRender.RenderCallback, HardwareInfoConfig.Callback {
    private static final String TAG = WebDevRegInfoDialog.class.getSimpleName();
    public static final String GUIDE_SUCCESS_KEY = "persist.sys.router.registered";
    private Logger mLogger = LogFactory.newLogger();
    private HardwareInfoConfig mHardwareInfoConfig;
    public static final Theme THEME;
    private RegisterInfoRender mRender;

    @BindView(R.id.progressTipTv)
    TextView mProgressTipTv;

    @BindView(R.id.seekProgressTv)
    TextView mSeekBarProgressTv;

    @BindView(R.id.registerProgressBar)
    SeekBar mRegisterSeekBar;

    @BindView(R.id.registerProgressBarView)
    View mRegisterSeekBarView;

    @BindView(R.id.regDevErrorLayout)
    View mRegDevErrorLayout;

    @BindView(R.id.errorTipTv)
    TextView mRegDevErrorTv;

    static {
        THEME = new Theme();
        THEME.dialogBgRes = R.drawable.guide_prompt_dialog_bg;
        THEME.dialogTitleBgRes = R.drawable.guide_prompt_dialog_title_bg;
        THEME.promptColorRes = R.color.guide_prompt_dialog_tv_color;
        THEME.buttonLayout1BgRes = R.drawable.guide_dialog_3_left_btn_selector;
        THEME.buttonLayout2BgRes = R.drawable.guide_dialog_3_middle_btn_selector;
        THEME.buttonLayout3BgRes = R.drawable.guide_dialog_3_right_btn_selector;
    }

    private TextView mLeftBtn;
    private TextView mMiddleBtn;
    private TextView mRightBtn;
    private ViewGroup leftBtnLayout;
    private ViewGroup middleBtnLayout;
    private ViewGroup rightBtnLayout;

    public WebDevRegInfoDialog(Context context) {
        super(context, THEME);
        deleteBtnLayout();
        addNewBtnLayout();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.web_dev_reg_layout;
    }

    @Override
    protected void initModel() {
        mHardwareInfoConfig = HardwareInfoConfig.getInstance(mContext);
    }

    @Override
    protected void initCustomizedView() {
        View webDevRegRootView = findViewById(R.id.webDevRegRootView);
        ButterKnife.bind(this);
        sLogger.i(TAG, " initCustomizedView webDevRegRootView: " + webDevRegRootView + ", mRegisterSeekBar: " + mRegisterSeekBar);
        mRender = new RegisterInfoRender(mProgressTipTv, mRegisterSeekBar,
                BaseConfigUtils.getDevRegInfoInterval());
    }


    @Override
    protected void customizeDialogSize() {
        mDialogWidthPercent = 0.7;
        mDialogHeightPercent = 0.5;
    }


    @Override
    public void show() {
        super.show();
        mDialogTitle.setText(R.string.device_register);
        mRender.addCallback(this);
        mHardwareInfoConfig.addConfigChangeCallback(this);
        mHardwareInfoConfig.refreshConfig();
        mRender.start();
    }


    @Override
    protected void setStyle() {
        setMode(DialogType.SYSTEM_ALERT_DIALOG);
        setCancelable(false);

    }

    @Override
    protected void renderView() {

    }

    @Override
    protected void initListener() {
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mLogger.e(TAG, " WebDevRegInfoDialog onDismiss... ");
                mRender.stop();
                mRender.removeCallback(WebDevRegInfoDialog.this);
                mHardwareInfoConfig.removeConfigChangeCallback(WebDevRegInfoDialog.this);
            }
        });

        mRegisterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekBarProgressTv.setText(progress + "");
                int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mSeekBarProgressTv.measure(spec, spec);
                int quotaWidth = mSeekBarProgressTv.getMeasuredWidth();

                int spec2 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                mSeekBarProgressTv.measure(spec2, spec2);
                int sbWidth = mRegisterSeekBar.getMeasuredWidth();
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSeekBarProgressTv.getLayoutParams();
                params.leftMargin = (int) (((double) progress / mRegisterSeekBar.getMax()) * sbWidth - (double) quotaWidth * progress / mRegisterSeekBar.getMax());
                mSeekBarProgressTv.setLayoutParams(params);


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private void startLauncher() {
        String pkgName = "com.starnet.bootstrap";
        String className = "com.starnet.bootstrap.BootActivity";

        Intent intent = new Intent();
        intent.putExtra("NeedZeroConf", 1);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(pkgName, className);

        ActivityUtils.startActivity(getContext(), intent);
    }

    @Override
    public void onShowRegErrorTip(int errorTipRes) {
        mLogger.e(TAG, " onShowRegErrorTip errorTipRes");
        showRegErrorTip(errorTipRes);
    }

    @Override
    public void onDevRegSuccess() {
        MainThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                mRegisterSeekBarView.setVisibility(View.GONE);
                mRegDevErrorLayout.setVisibility(View.VISIBLE);
                mRegDevErrorTv.setText(R.string.dev_reg_success);

                leftBtnLayout.setVisibility(View.VISIBLE);

                mLeftBtn.setText(R.string.enter_launcher);
                mLeftBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SystemProperties.set(GUIDE_SUCCESS_KEY, "1");
                        startLauncher();
                        dismiss();
                    }
                });
            }
        });
    }

    private void showRegErrorTip(final int errorTipRes) {
        MainThread.getInstance().post(new Runnable() {
            @Override
            public void run() {
                mRegisterSeekBarView.setVisibility(View.GONE);
                mRegDevErrorLayout.setVisibility(View.VISIBLE);
                mRegDevErrorTv.setText(errorTipRes);

                leftBtnLayout.setVisibility(View.VISIBLE);

                mLeftBtn.setText(R.string.back);
                mLeftBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });
            }
        });
    }

    @Override
    public void onVoIPConfigChange(boolean hasVoIP) {

    }

    @Override
    public void onWanPortTypeChange() {
        mLogger.i(TAG, " onWanPortTypeChange ");
        mRender.refreshStatusTip20();
    }

    @Override
    public void onWanPortNumberChange(int LanPortNum, int SSIDPortNum) {

    }

    private void deleteBtnLayout() {
        dividerAboveFooter.setVisibility(View.GONE);
        (footerLayout).removeAllViews();
    }

    private void addNewBtnLayout() {
        View newBtnLayout = View.inflate(getContext(), com.starnet.widget.R.layout.guide_promt_dialog_btn_layout, footerLayout);
        mLeftBtn = (TextView) newBtnLayout.findViewById(com.starnet.widget.R.id.guideLeftBtn);
        mMiddleBtn = (TextView) newBtnLayout.findViewById(com.starnet.widget.R.id.guideMiddleBtn);
        mRightBtn = (TextView) newBtnLayout.findViewById(com.starnet.widget.R.id.guideRightBtn);

        leftBtnLayout = (ViewGroup)findViewById(com.starnet.widget.R.id.guideButtonLayout1);
        middleBtnLayout = (ViewGroup)findViewById(com.starnet.widget.R.id.guideButtonLayout2);
        rightBtnLayout = (ViewGroup)findViewById(com.starnet.widget.R.id.guideButtonLayout3);

        mLeftBtn.setText(mContext.getResources().getString(R.string.back));
        middleBtnLayout.setVisibility(View.GONE);
        rightBtnLayout.setVisibility(View.GONE);
        leftBtnLayout.setVisibility(View.GONE);
        setDefOnlyLeftBtnLayout();
    }

    private void setDefOnlyLeftBtnLayout() {
        mLeftBtn.requestFocus();
    }
}
