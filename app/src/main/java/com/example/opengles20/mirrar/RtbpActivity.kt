package com.example.opengles20.mirrar

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.opengles20.R
import com.example.opengles20.databinding.ActivityRtbpBinding
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import kotlinx.coroutines.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class RtbpActivity : AppCompatActivity(), GLSurfaceRendererCallback, IDialogEventListener {
    private var isProgressBarDisplaying: Boolean = false
    private lateinit var mHandler: Handler
    private var mCameraPermissionDeniedOnceDialog: GenericCustomDialogFragment? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var mCameraPermissionDoNotShowAgainDialog: GenericCustomDialogFragment? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    lateinit var mViewModel: RtbpViewModel
    private var mBeardsItemList: List<BeardsItem?> = mutableListOf()
    private lateinit var mRenderFaceNode: BarbeoStyleRender
    private lateinit var mBinding: ActivityRtbpBinding

    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private var surfaceView: BarbeoGLSurfaceView? = null
    private var isArcoreInstallRequested = false
    private var session: Session? = null
    private var displayRotationHelper: DisplayRotationHelper? = null

    private lateinit var carouselView: DiscreteScrollView
    private var mStylePosition: Int = -1

    //default hair color
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var mHairColor: HairColor = HairColor.BLACK
    private var isFABOpen: Boolean = false
    private var selectedStyle: Style? = null
    private var mBeardConfig: BeardConfig? = null
    private var mBeardsItem: BeardsItem? = null

    companion object {
        private val TAG = RtbpActivity::class.java.simpleName
        private val HINT_TEXT_ANIMATION_DURATION: Long = 7000 //7 Seconds
        private val RECOMMENDED_FADE_ANIMATION_DURATION: Long = 3000 //3 Seconds
        private val CAROUSEL_MAX_SCALE_VALUE: Float = 1.3f
        private val CAROUSEL_MIN_SCALE_VALUE: Float = 0.95f
    }

    private val mAdapter by lazy {
        RtbpStyleAdapter(this) { position: Int ->
            carouselView.smoothScrollToPosition(position)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        mBeardsItem = intent?.getParcelableExtra(VsBeardStyleConstants.INTENT_EXTRAS_BEARDS_ITEM)
        movePositionToSelectedBeardItem()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_rtbp)

        mHandler = Handler(Looper.getMainLooper())
        hideActionBar()
        displayRotationHelper = DisplayRotationHelper(this)
        mBeardsItem = intent.getParcelableExtra(VsBeardStyleConstants.INTENT_EXTRAS_BEARDS_ITEM)
        mViewModel = ViewModelProvider(this).get(RtbpViewModel::class.java)
        mRenderFaceNode = BarbeoStyleRender(this, displayRotationHelper!!)
        isArcoreInstallRequested = false
        initView()
    }

    private fun initView() {

        mBinding.vsBrStylePermissionText.text = String.format(getString(com.philips.vitaskin.beardstyle.R.string.vitaskin_male_br_rtbp_camera_permission_hint_text), VitaSkinInfraUtil.getAppName(this))
        // Set up renderer with delay of 100ms and without the delay UI will be non-responsive
        CoroutineScope(Dispatchers.Main).launch {
            delay(100)
            surfaceView = BarbeoGLSurfaceView(this@RtbpActivity)
            mBinding.vsBrRtbpSurfaceviewFrameLayout.addView(surfaceView)
            surfaceView?.setupSurfaceView(object : ScaleController.OnScaleValueChangeListener {
                override fun onScaleValueChanged(x: Float, y: Float, z: Float) {
                    mBinding.vsBrRtbpBtnReset.visibility = View.VISIBLE
                    mRenderFaceNode.onScaleValueChanged(x, y)
                    surfaceView?.requestRender()
                    mViewModel.sendHairIntensityAnalyticsData(this@RtbpActivity, selectedStyle)
                }
            }, this@RtbpActivity)
        }

        //Beard style carosuel view
        carouselView = mBinding.vsBrRtbpCarosuelview
        carouselView.adapter = mAdapter
        carouselView.setHasFixedSize(true)
        carouselView.setItemTransformer(ScaleTransformer.Builder()
            .setMaxScale(CAROUSEL_MAX_SCALE_VALUE)
            .setMinScale(CAROUSEL_MIN_SCALE_VALUE)
            .setPivotX(Pivot.X.CENTER)
            .setPivotY(Pivot.Y.CENTER)
            .build())
        carouselView.addOnItemChangedListener(disOnItemChangedListener)

        //Hint Bg
        mBinding.vsBrRtbpHintBg.setOnSingleClickListener(View.OnClickListener {
            mBinding.vsRtbpHintGroup.visibility = View.GONE
        })

        //Download beardConfig json
        initDownloadBeardConfigJson()

        //Retain the selected hair color
        initFABHairColorSelection()

        mBinding.vsBrRtbpBtnMain.setOnSingleClickListener(View.OnClickListener {
            if (!isFABOpen) {
                expandFabMenu()
            } else {
                hideFabMenu()
            }
        })

        mBinding.vsBrRtbpBtnReset.setOnSingleClickListener(View.OnClickListener {
            mViewModel.sendResetAnalyticsData(this, selectedStyle)
            mRenderFaceNode.resetScaling()
            renderSelectedStyleLayer(selectedStyle)
            mBinding.vsBrRtbpBtnReset.visibility = View.GONE
        })

        mBinding.vsBrRtbpClose.setOnSingleClickListener(View.OnClickListener {
            finish()
        })

        mBinding.vsBrRtbpTvTitleLayout.setOnSingleClickListener(View.OnClickListener {
            val beardId = selectedStyle?.id
            val intent = Intent(this, VsBeardStyleActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(VsBeardStyleConstants.INTENT_EXTRAS_IS_JOURNEY_MODE, null as Boolean?)
            intent.putExtra(VsBeardStyleConstants.SELECTED_STYLE_ID_EXTRA_KEY, beardId)
            this.startActivity(intent)
            overridePendingTransition(com.philips.cdpp.vitaskin.base.R.anim.vitaskin_stay, com.philips.cdpp.vitaskin.base.R.anim.vitaskin_bottom_slide_out)
        })

        checkStyleIsRecommended(mBeardsItem)

        mBinding.vsBrStylePermissionButton.setOnSingleClickListener(View.OnClickListener {
            CameraPermissionHelper.requestCameraPermission(this@RtbpActivity)
        })

        mBinding.vsBrRtbpBtnBuildStyle.setOnSingleClickListener(View.OnClickListener {
            val beardId = selectedStyle?.id
            startActivity(MirrArActivity.newInstance(this, beardId!!, mBinding.vsBrRtbpTvStyleTitle.text))
        })
    }

    private suspend fun checkAssetAvailability(mBeardConfig: BeardConfig?) {
        mBeardConfig?.assets?.let {
            if (!mViewModel.checkBeardStyleAssetAvailability(this, it)) {
                withContext(Dispatchers.Main) {
                    showDownloadDialog()
                }
            }
        }
    }

    private fun showDownloadDialog() {
        val downloadDialog = mViewModel.getAssetDownloadDialog(this, this)
        supportFragmentManager.beginTransaction()
            .add(downloadDialog, GenericCustomDialogFragment.TAG).commitAllowingStateLoss()
    }

    private fun initFABHairColorSelection() {
        val selectedHairColor = mViewModel.getSelectedHairColor()
        mHairColor = HairColor.valueOf(selectedHairColor)
        mBinding.vsBrRtbpBtnMain.setImageResource(mHairColor.image)

        mBinding.vsBrRtbpBtnBlack.setOnSingleClickListener(View.OnClickListener {
            mHairColor = HairColor.BLACK
            updateHairColor(mHairColor)
        })

        mBinding.vsBrRtbpBtnBrown.setOnSingleClickListener(View.OnClickListener {
            mHairColor = HairColor.BROWN
            updateHairColor(mHairColor)
        })

        mBinding.vsBrRtbpBtnBlonde.setOnSingleClickListener(View.OnClickListener {
            mHairColor = HairColor.BLONDE
            updateHairColor(mHairColor)
        })

        mBinding.vsBrRtbpBtnRed.setOnSingleClickListener(View.OnClickListener {
            mHairColor = HairColor.RED
            updateHairColor(mHairColor)
        })
    }

    private fun updateHairColor(hairColor: HairColor) {
        mViewModel.setSelectedHairColor(hairColor.toString())
        mBinding.vsBrRtbpBtnMain.setImageResource(hairColor.image)
        hideFabMenu()
        mViewModel.sendHairColorAnalyticsData(this, selectedStyle, hairColor)
        renderSelectedStyleLayer(selectedStyle)
    }

    private fun initDownloadBeardConfigJson() {
        CoroutineScope(Dispatchers.IO).launch {
            val beardStyleParser: BeardStyleParser = BeardStyleDataProvider.getInstance(this@RtbpActivity).getParser()
            mBeardConfig = RTBPManager.getInstance().getBeardStyleConfig(this@RtbpActivity)
            mRenderFaceNode.setBeardConfigData(mBeardConfig)
            checkAssetAvailability(mBeardConfig)
            withContext(Dispatchers.Main) {
                getBeardStyles(beardStyleParser)
            }
        }
    }

    private fun getBeardStyles(beardStyleParser: BeardStyleParser) {
        beardStyleParser.beardStyles.observe(this@RtbpActivity, androidx.lifecycle.Observer { beardStyles ->
            if (beardStyles?.sortedBeards != null && beardStyles.sortedBeards?.isNotEmpty()!!) {
                mBeardsItemList = beardStyles.sortedBeards!!
                mBeardsItemList = mBeardsItemList.filter {
                    it?.livepreview?.available == 1
                }
                //For RTL support the items are reversed
                if (VitaSkinInfraUtil.isRtlMode()) {
                    mBeardsItemList = mBeardsItemList.reversed()
                }
                if (mBeardConfig != null) {
                    mAdapter.updateStyleData(mBeardsItemList)
                }
                movePositionToSelectedBeardItem()
            }
        })
    }

    private fun movePositionToSelectedBeardItem() {
        if (mBeardsItem != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                val index = mBeardsItemList.withIndex().filter { it.value?.id == mBeardsItem?.id }.map { it.index }
                carouselView.smoothScrollToPosition(index[0])
            }, 300)
        } else {
            if (VitaSkinInfraUtil.isRtlMode()) {
                Handler(Looper.getMainLooper()).postDelayed({
                    carouselView.smoothScrollToPosition(mBeardsItemList.size - 1)
                }, 200)
            }
        }
    }

    private fun renderSelectedStyleLayer(style: Style?) {
        mRenderFaceNode.renderSelectedStyleLayer(style, object : BarbeoStyleRender.RenderStyleCallback {
            override fun onRenderStart() {
            }

            override fun onRenderEnd() {
                surfaceView?.requestRender()
            }
        }, mHairColor)
    }

    private fun dismissProgressbar() {
        CoroutineScope(Dispatchers.Main).launch {
            mBinding.progressbar.visibility = View.GONE
        }
    }

    private fun showProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            mBinding.progressbar.visibility = View.VISIBLE
        }
    }

    private fun hideFabMenu() {
        isFABOpen = false
        mBinding.vsBrRtbpTvHairColor.visibility = View.VISIBLE
        mBinding.vsRtbpFabMenuGroup.visibility = View.GONE
    }

    private fun expandFabMenu() {
        isFABOpen = true
        mBinding.vsBrRtbpTvHairColor.visibility = View.GONE
        mBinding.vsRtbpFabMenuGroup.visibility = View.VISIBLE
    }

    private val disOnItemChangedListener = DiscreteScrollView.OnItemChangedListener<RtbpStyleAdapter.ViewHolder> { _, adapterPosition ->
        VSLog.d(TAG, "adapterPosition: $adapterPosition")
        if (mStylePosition != adapterPosition) {
            mStylePosition = adapterPosition
            mAdapter.onItemSelected(adapterPosition)
            val beardItem = mBeardsItemList[adapterPosition]
            selectedStyle = mBeardConfig?.styles?.find {
                it.id == beardItem?.id
            }
            ADBMobile.trackPage(String.format(resources.getString(R.string.com_philips_vitaskin_analytics_rtbp_pagename), selectedStyle?.id), this)
            checkStyleIsRecommended(beardItem)
            mBinding.vsBrRtbpTvStyleTitle.text = beardItem?.shortTitle
            renderSelectedStyleLayer(selectedStyle)
        }
    }

    override fun onResume() {
        super.onResume()
        dismissExistingPermissionDialog()
        // ARCore requires camera permissions to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            enableCameraPermissionLayout(true)
            mBinding.vsBrRtbpBtnBuildStyle.invisible()
            return
        } else {
            enableCameraPermissionLayout(false)
            mBinding.vsBrRtbpBtnBuildStyle.visible()
        }
        if (initiateSessionCreation()) return
        VSLog.printTimeTaken(TAG, "onResume")
    }

    fun initiateSessionCreation(): Boolean {
        if (session == null) {
            val sessionCreated = ArSessionUtil().createARCoreSession(this, isArcoreInstallRequested)
            if (sessionCreated != null) {
                session = sessionCreated
                configureSession()
            } else {
                isArcoreInstallRequested = true
                return true
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            VSLog.d(TAG, "The Camera is not available yet. Please try to open a camera app.")
            session = null
            return true
        }
        surfaceView?.onResume()
        displayRotationHelper!!.onResume()
        displayRtbpHintText()
        return false
    }

    fun displayRtbpHintText() {
        if (mViewModel.isRtbpVisitedFirsTime()) {
            Glide.with(this).load(R.raw.vs_rtbp_hint).into(mBinding.vsBrRtbpIvHintImage)
            mViewModel.fadeInAnimation(mBinding.vsRtbpHintGroup, startDelay = 500)
            mViewModel.setRtbpVisitedFirsTime(false)
            CoroutineScope(Dispatchers.IO).launch {
                delay(HINT_TEXT_ANIMATION_DURATION)
                withContext(Dispatchers.Main) {
                    mViewModel.fadeOutAnimation(mBinding.vsRtbpHintGroup, duration = 1000)
                }
            }
        } else {
            mViewModel.setRtbpVisitedFirsTime(false)
            mBinding.vsRtbpHintGroup.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                showCameraPermissionDialog(VsCameraAPIGlobal.CAMERA_PERMISSION_DENIED)
            } else {
                showCameraPermissionDialog(GenericCustomDialogFragment.CUSTOM_CAMERA_PERMISSION_DENIED_RTBP_DIALOG)
            }
        } else {
            enableCameraPermissionLayout(false)
        }
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        VSLog.printTimeTaken(TAG, "onSurfaceCreated")
        showProgressBar()
        isProgressBarDisplaying = true
        mRenderFaceNode.onSurfaceCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (isProgressBarDisplaying) {
            dismissProgressbar()
            isProgressBarDisplaying = false
        }
        mRenderFaceNode.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        VSLog.printTimeTaken(TAG, "onDrawFrame")
        mRenderFaceNode.onDrawFrame(session)
    }

    private fun configureSession() {
        val config = Config(session)
        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
        // Check whether the user's device supports the Depth API.
        if (session!!.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            config.depthMode = Config.DepthMode.AUTOMATIC
        } else {
            config.depthMode = Config.DepthMode.DISABLED
        }
        config.lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
        session!!.configure(config)
    }

    override fun onPause() {
        super.onPause()
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper!!.onPause()
            surfaceView?.onPause()
            session!!.pause()
        }
    }

    override fun getContainerId(): Int {
        return 0
    }

    override fun onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            // Review the API reference for important considerations before calling close() in apps with
            // more complicated lifecycle requirements:
            // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
            session!!.close()
            session = null
        }
        LoadHelper.flushHttpCache()
        ResourceManager.getInstance().destroyAllResources()
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun checkStyleIsRecommended(beardsItem: BeardsItem?) {
        if (beardsItem == null) {
            return
        }
        if (BeardUtils.isThisStyleIsRecommended(beardsItem.id.toString())) {
            mHandler.removeCallbacksAndMessages(null)
            mBinding.vsBrRtbpRecommandationStarLayout.animation = null
            mViewModel.fadeInAnimation(mBinding.vsBrRtbpRecommandationStarLayout)
            startFadeoutAnimation(mBinding.vsBrRtbpRecommandationStarLayout)
        } else {
            mHandler.removeCallbacksAndMessages(null)
            mBinding.vsBrRtbpRecommandationStarLayout.animation = null
            mBinding.vsBrRtbpRecommandationStarLayout.visibility = View.INVISIBLE
        }
    }

    private fun startFadeoutAnimation(view: View) {
        mHandler.postDelayed({
            mViewModel.fadeOutAnimation(viewToFadeOut = view, visibility = View.INVISIBLE)
        }, RECOMMENDED_FADE_ANIMATION_DURATION)
    }

    private fun enableCameraPermissionLayout(isVisible: Boolean) {
        mBinding.vsBrStylePermissionLayout.visibility = if (isVisible) View.VISIBLE else View.GONE
        mBinding.vsBrRtbpFabBtnGroup.visibility = if (!isVisible) View.VISIBLE else View.GONE
        if(isFABOpen){
            mBinding.vsBrRtbpTvHairColor.visibility = View.GONE
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showCameraPermissionDialog(permissionDenied: Int) {
        when (permissionDenied) {
            VsCameraAPIGlobal.CAMERA_PERMISSION_DENIED -> {
                mCameraPermissionDoNotShowAgainDialog = mViewModel.getCameraPermissionDeniedDialog(this, this)
                supportFragmentManager.beginTransaction().add(mCameraPermissionDoNotShowAgainDialog!!, CustomDialogFragment.TAG).commitAllowingStateLoss()
            }
            GenericCustomDialogFragment.CUSTOM_CAMERA_PERMISSION_DENIED_RTBP_DIALOG -> {
                mCameraPermissionDeniedOnceDialog = mViewModel.getCameraPermissionDeniedOnceDialog(this, this)
                supportFragmentManager.beginTransaction().add(mCameraPermissionDeniedOnceDialog!!, CustomDialogFragment.TAG).commitAllowingStateLoss()
            }
        }
    }

    private fun dismissExistingPermissionDialog() {
        mViewModel.dismissDialog(mCameraPermissionDoNotShowAgainDialog)
        mViewModel.dismissDialog(mCameraPermissionDeniedOnceDialog)
    }

    override fun onDialogButtonClicked(action: IDialogEventListener.ACTION, dialogId: Int, dialog: DialogFragment) {
        when (action) {
            IDialogEventListener.ACTION.LEFT_BUTTON -> {
                when (dialogId) {
                    GenericCustomDialogFragment.CUSTOM_PERMISSION_DIALOG_ID -> {
                        mViewModel.dismissDialog(mCameraPermissionDoNotShowAgainDialog)
                    }
                    GenericCustomDialogFragment.CUSTOM_CAMERA_PERMISSION_DENIED_RTBP_DIALOG -> {
                        mViewModel.dismissDialog(mCameraPermissionDeniedOnceDialog)
                    }
                    GenericCustomDialogFragment.CUSTOM_ERROR_DIALOG_ID -> {
                        dialog.dismiss()
                        finish()
                    }
                    else -> {
                        dialog.dismiss()
                    }
                }
            }
            IDialogEventListener.ACTION.RIGHT_BUTTON -> {
                if (dialogId == GenericCustomDialogFragment.CUSTOM_ERROR_DIALOG_ID) {
                    dialog.dismiss()
                    startDownloadBeardstyleAssets()
                } else if (dialogId ==
                    GenericCustomDialogFragment.USER_CONF_LOG_OUT_DIALOG_ID) {
                    dialog.dismiss()
                    finish()
                } else {
                    CameraPermissionHelper.launchPermissionSettings(this)
                }
            }
            else -> {
                //DO NOTHING
                dialog.dismiss()
            }
        }
    }

    private fun startDownloadBeardstyleAssets() {
        if (VitaSkinInfraUtil.isOnline(this)) {
            showProgressBar()
            mViewModel.startAssetDownload(this, object : RtbpAssetDownloaderCallback {
                override fun onAssetDownloadError() {
                    VSLog.d(TAG, "onAssetDownloadError")
                    dismissProgressbar()
                    showErrorDialog()
                }

                override fun onAssetDownloadInitiated() {
                    VSLog.d(TAG, "onAssetDownloadInitiated")
                }

                override fun onAssetDownloadCompleted() {
                    dismissProgressbar()
                    VSLog.d(TAG, "onAssetDownloadCompleted")
                    CoroutineScope(Dispatchers.Main).launch {
                        renderSelectedStyleLayer(selectedStyle)
                    }
                }
            })
        } else {
            VSLog.d(TAG, "NO internet")
            showErrorDialog()
        }
    }

    private fun showErrorDialog() {
        if (!isFinishing) {
            CoroutineScope(Dispatchers.Main).launch {
                val errorDialog = mViewModel.getDownloadFailedDialog(this@RtbpActivity, this@RtbpActivity)
                supportFragmentManager.beginTransaction().add(errorDialog, CustomDialogFragment.TAG).commitAllowingStateLoss()
            }
        }
    }

    override fun onSpannableTextClicked(dialog: DialogFragment, DialogType: Int) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", this.packageName, null))
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityForResult(intent, 100)
    }

}