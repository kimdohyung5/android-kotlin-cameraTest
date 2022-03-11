package com.kimdo.cameratestup.views

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.*
import android.webkit.*
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.hypot

import com.kimdo.cameratestup.R

class WebViewActivity : AppCompatActivity() {
    private val hostUrl_dev: String = "https://dev-brs-fo-frontend.wjtb.kr"
    private val hostUrl_real: String = "https://www.mathpid.com"
    private lateinit var mWebView: WebView
    private lateinit var mWebViewLoading: WebView
    private var initData: Array<String>? = null
    private var isDev: Boolean = true
    private var isHide: Boolean = false
    private val minSecond: Int = 3
    private val maxSecond: Int = 15
    private var second: Int = 0
    private var timerTask: Timer? = null

    /////////////////////////////////////////////////////////
    val MAX_CLICK_DURATION: Int = 500
    val MAX_CLICK_DISTANCE: Int = 10
    val preventTouchEventTime: Long = 250
    var pressStartTime: Long = 0L
    var pressedX: Float = 0.0F
    var pressedY: Float = 0.0F
    var stayedWithinClickDistance: Boolean = false
    ///////////////////////////////////////////////////////////

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        hideSystemUI()


        mWebViewLoading = findViewById(R.id.webViewLoading)
        mWebViewLoading.loadUrl("file:///android_asset/LC_00_00.html")
        mWebViewLoading.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (isNetworkOnline(this@WebViewActivity)) {
                    val handler: Handler = object : Handler(Looper.getMainLooper()) {
                        override fun handleMessage(msg: Message) {
                            initProcess()
                        }
                    }
                    handler.obtainMessage().sendToTarget()
                } else {
                    CustomDialog(this@WebViewActivity, "WIFI에 연결해주세요").show()
                }
            }
        }
    }

    private fun initProcess() {
        WebView.setWebContentsDebuggingEnabled(true)


        mWebView = findViewById(R.id.webView)
        mWebView.clearCache(true)
        mWebView.clearHistory()
        mWebView.setOnLongClickListener { true }
        mWebView.isLongClickable = false

        setTouchListener(true)
        timerStart()

        if (isDev) {
            setAIWebView("${hostUrl_dev}?ENTER_TYPE=APPS")
        } else {
            setAIWebView("${hostUrl_real}?ENTER_TYPE=APPS")
        }

    }

    private fun setTouchListener(flag: Boolean) {
        if (flag) {
            mWebView.setOnTouchListener { _: View, event: MotionEvent ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        pressStartTime = System.currentTimeMillis()
                        pressedX = event.x
                        pressedY = event.y
                        stayedWithinClickDistance = true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (stayedWithinClickDistance && distance(
                                pressedX,
                                pressedY,
                                event.x,
                                event.y
                            ) > MAX_CLICK_DISTANCE
                        ) {
                            stayedWithinClickDistance = false
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        val pressDuration = System.currentTimeMillis() - pressStartTime
                        if (pressDuration < MAX_CLICK_DURATION && stayedWithinClickDistance) {
                            Log.d("JWHAN", "click")
                            setTouchListener(false)
                            Handler().postDelayed({
                                setTouchListener(true)
                            }, preventTouchEventTime)
                        }
                    }
                }
                //리턴값은 return 없이 아래와 같이
                false // or false
            }
        } else {
            mWebView.setOnTouchListener { _: View, event: MotionEvent ->
                //리턴값은 return 없이 아래와 같이
                Log.d("JWHAN", "click prevent")
                true // or false
            }

        }
    }

    private fun distance(startX: Float, startY: Float, endX: Float, endY: Float): Float {
        return hypot((endX - startX), (endY - startY))
    }

    private fun timerStart() {
        timerTask = timer(period = 1000, initialDelay = 1000) {
            second++
            if (second >= minSecond && isHide) {
                second = 0
                cancel()
                val handler: Handler = object : Handler(Looper.getMainLooper()) {
                    override fun handleMessage(msg: Message) {


                        Log.i("kimdo", "timeStart is called")

                        val parentLodingView = mWebViewLoading.parent as ViewGroup
                        parentLodingView.removeView(mWebViewLoading)
                        timerTask = null
                    }
                }
                handler.obtainMessage().sendToTarget()
            } else if (second >= maxSecond) {
                timerTask = null
                second = 0
                cancel()
                CustomDialog(this@WebViewActivity, "나중에 다시 실행해주세요").show()
            }
        }
    }

    private fun timerStop() {
        second = 0
        timerTask?.cancel()
    }

    class WebBrideg(private val activity: Activity, private val callback: () -> Unit) {
        @JavascriptInterface
        fun killApp() {
            activity.finish()
        }

        @JavascriptInterface
        fun hideLoading() {
            callback.invoke()
        }

        @JavascriptInterface
        fun callCamera() {
            val intent = Intent(activity, CameraActivity::class.java)
            activity.startActivityForResult(intent,200)
        }
    }

    private fun setAIWebView(url: String) {

        Log.i("kkkk",  "url=${url}")
        mWebView.apply {
            settings.javaScriptEnabled = true
            //검토
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            // Enable and setup web view cache
            settings.setAppCacheEnabled(false)
            settings.cacheMode = WebSettings.LOAD_NO_CACHE

            settings.setAppCachePath(cacheDir.path)
            // Enable zooming in web view
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            // Enable disable images in web view
            settings.blockNetworkImage = false
            // Whether the WebView should load image resources
            settings.loadsImagesAutomatically = true

            settings.domStorageEnabled = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = true  // api 26
            }

            //검토
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
                settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN)
                settings.setEnableSmoothTransition(true)
            }

            //settings.pluginState = WebSettings.PluginState.ON
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false

            // More optional settings, you can enable it by yourself
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            settings.allowContentAccess = true
            settings.setGeolocationEnabled(true)
            settings.allowUniversalAccessFromFileURLs = true

            settings.allowFileAccess = true

            // WebView settings
            fitsSystemWindows = true
            webViewClient = MyWebViewClient(this@WebViewActivity)
            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView,
                    url: String?,
                    message: String?,
                    result: JsResult
                ): Boolean {
                    AlertDialog.Builder(view.context)
                        .setTitle("메세지")
                        .setMessage(message)
                        .setPositiveButton(
                            android.R.string.ok
                        ) { _, _ -> result.confirm() }
                        .setCancelable(false)
                        .create()
                        .show()
                    return true
                }

                override fun onJsConfirm(
                    view: WebView,
                    url: String?,
                    message: String?,
                    result: JsResult
                ): Boolean {
                    AlertDialog.Builder(view.context)
                        .setTitle("메세지")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok) { _, _ -> result.confirm() }
                        .setNegativeButton(android.R.string.cancel) { _, _ -> result.cancel() }
                        .setCancelable(false)
                        .create()
                        .show()
                    return true
                }
            }
        }

        mWebView.addJavascriptInterface(
            WebBrideg(this@WebViewActivity) {
                isHide = true
            },
            "aicalculator"
        )
//Log.e("SETAIWEBVIEW","$url")

        mWebView.loadUrl("file:///android_asset/sample.html")
//        mWebView.loadUrl(url)
    }

    class MyWebViewClient internal constructor(private val activity: Activity) : WebViewClient() {
        private var showFlag = false

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url: String = request?.url.toString()
//Log.e("SHOULDOVERRIDEURLLOA","LOLLIPOP $url")
            view?.loadUrl(url)
            return true
        }

        override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
//Log.e("SHOULDOVERRIDEURLLOA","$url")
            webView.loadUrl(url)
            return true
        }


        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
//Log.e("webview", "GOT Page error : code : " + error.errorCode + " Desc : " + error.description)
            if (!showFlag) {
                showFlag = true
                showError(error.errorCode)
            }

        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            if (!showFlag) {
                showFlag = true
                showError(errorCode)
            }
        }

        private fun showError(errorCode: Int) {
            //Prepare message
            val message: String
            when (errorCode) {
                ERROR_TIMEOUT ->
                    message = "서버 접속에 실패하였습니다.\n잠시 후 다시 이용해 주세요.\n문제가 지속되는 경우\n고객센터로 연락해 주세요."
                ERROR_UNKNOWN ->
                    message = "앱에서 알수 없는 오류가 발생하였습니다.\n문제가 지속되는 경우\n고객센터로 연락해 주세요."
                ERROR_BAD_URL ->
                    message = "올바르지 않는 접근 경로 입니다.\n확인 후 이용해 주세요."
                ERROR_CONNECT ->
                    message = "인터넷 연결 상태가 좋지 않습니다.\n네트워크 설정을 확인해주세요."
                ERROR_UNSAFE_RESOURCE ->
                    message = "사용자 브라우저에 문제가 있어 페이지를 로딩할 수 없습니다."
                SAFE_BROWSING_THREAT_UNKNOWN ->
                    message = "앱에서 알수 없는 오류가 발생하였습니다.\n문제가 지속되는 경우\n고객센터로 연락해 주세요."
                SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE ->
                    message = "사용자 브라우저에 문제가 있어 페이지를 로딩할 수 없습니다."
                ERROR_HOST_LOOKUP ->
                    message = "서버 접속에 실패하였습니다.\n잠시 후 다시 이용해 주세요.\n문제가 지속되는 경우\n고객센터로 연락해 주세요."
                else ->
                    message = "앱에서 알수 없는 오류가 발생하였습니다.\n문제가 지속되는 경우\n고객센터로 연락해 주세요."
            }
            CustomDialog(activity, message).show()
        }
    }

    override fun onPause() {
        super.onPause()
        timerStop()
    }

    override fun onResume() {
        super.onResume()
        if (timerTask != null)
            timerStart()
    }

    override fun onBackPressed() {
        if (isDev) {
            if (mWebView.url == hostUrl_dev) {
                finish()
            } else {
                if (mWebView.canGoBack()) {
                    mWebView.goBack() // 이전 페이지로 갈 수 있다면 이동하고
                } else {
                    super.onBackPressed() // 더 이상 이전 페이지가 없을 때 앱이 종료된다.
                }
            }
        } else { //운영
            if (mWebView.url == hostUrl_real) {
                finish()
            } else {
                if (mWebView.canGoBack()) {
                    mWebView.goBack() // 이전 페이지로 갈 수 있다면 이동하고
                } else {
                    super.onBackPressed() // 더 이상 이전 페이지가 없을 때 앱이 종료된다.
                }
            }
        }
    }

    class CustomDialog(private val activity: Activity, private val message: String) :
        Dialog(activity) {

        private lateinit var btnOK: Button
        private lateinit var text: TextView

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContentView(R.layout.custom_dialog)
            setCanceledOnTouchOutside(false)
            setCancelable(false)

            window?.setBackgroundDrawableResource(android.R.color.transparent)


            try {
                window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            } catch (e: Exception) {

                window?.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
            text = findViewById(R.id.content)
            btnOK = findViewById(R.id.ok)
            text.text = message.replace("\\n","\n")
            btnOK.setOnClickListener {
                dismiss()
                activity.finish()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun isNetworkOnline(context: Context): Boolean {
        var isOnline = false
        try {
            val manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities =
                manager.getNetworkCapabilities(manager.activeNetwork) // need ACCESS_NETWORK_STATE permission
            isOnline =
                capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return isOnline
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

//        Log.i("kkkk", "WebViewActivity.kt is called....data=${data} requestCode=${requestCode} resultCode=${resultCode}")




//////////////////////////// 우선 주석처리를 하고 나중에 풀어놓을지 고려하자.. 2022.03.10 김도..
//        if( MyApplication.instance.bodystring != null ) {
//            val bodystring = MyApplication.instance.bodystring
//            Log.i("kkkk", "WebViewActivity.kt MyApplication.instance.bodystring=${bodystring}")
//            MyApplication.instance.bodystring = null
//
////            mWebView.loadUrl("file:///android_asset/sample2.html")
//            mWebView.loadUrl("javascript:setMessage('" + bodystring + "')")
//        }
//
//        if( MyApplication.instance.gotoWhere != null ) {
//            if ( MyApplication.instance.gotoWhere == "history") {
//                mWebView.loadUrl("javascript:gotoHistory('" + "gotoHistory: arguments abc" + "')")
//            }
//            MyApplication.instance.gotoWhere = null
//        }

    }
}