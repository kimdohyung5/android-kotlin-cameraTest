package com.kimdo.cameratestup.views


import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Bundle

import android.util.Log
import android.view.View
import android.webkit.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.kimdo.cameratestup.MyApplication
import com.kimdo.cameratestup.databinding.ActivityMainBinding
import com.kimdo.cameratestup.models.InputInfo
import com.kimdo.cameratestup.models.LoginResponse
import com.kimdo.cameratestup.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.kimdo.cameratestup.R
import com.kimdo.cameratestup.models.RecogResponse

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding
    private val TAG:String = "MainActivity"

    private var imagepath:String = ""

    private var localFontSize: Float = 0F
    private var awsurl:String = ""
    private var ocr_result:String = ""
    private var rec_result:String = ""

    private val startCameraForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result: ActivityResult ->


        if( MyApplication.instance.regcogResponse != null ) {
            val regcogResponse = MyApplication.instance.regcogResponse!!
            MyApplication.instance.regcogResponse = null
//            ocr_bodystring = MyApplication.instance.bodystring?:""
//            Log.i(TAG, "WebViewActivity.kt MyApplication.instance.bodystring=${ocr_bodystring}")
//            MyApplication.instance.bodystring = null

            imagepath = MyApplication.instance.imagepath!!
            MyApplication.instance.imagepath = null

            displayResult(regcogResponse)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDebugging.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)

            startCameraForResult.launch( intent )
        }

        binding.btnReport.setOnClickListener {
            dataSendTest()
        }

        localFontSize = resources.getDimensionPixelSize(R.dimen.localFontSize).toFloat()

        binding.webview.apply {
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
            webViewClient = MyWebViewClient(this@MainActivity)
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
        binding.webview.addJavascriptInterface(
            WebViewActivity.WebBrideg(this@MainActivity) {

            },
            "tex_result"
        )
        binding.webview.loadUrl("file:///android_asset/tex_result.htm")
    }

    private fun drawText(canvas: Canvas, text:String, x:Float, y:Float ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.setColor( Color.RED )
        paint.textSize = localFontSize
        paint.setShadowLayer(1F, 0F, 1F, Color.BLACK)

        var rc: Rect = Rect()
        paint.getTextBounds(text, 0, text.length, rc)
        canvas.drawText(text, x, y +  rc.height(), paint)
    }

    private fun drawLine(canvas: Canvas, pointList:ArrayList<PointF>) {
        val path = Path().apply {
            moveTo(pointList[0].x, pointList[0].y)
            for ( i in 1.. pointList.size -1) {
                lineTo(pointList[i].x, pointList[i].y)
            }
            lineTo(pointList[0].x, pointList[0].y)
            close()
        }
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLUE
            strokeWidth = 5F
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath( path, paint )
    }


    private fun displayResult(recogResponse:RecogResponse) {

        binding.txtOcrresult.setText("")
        binding.txtRecresult.setText("")
        var bitmap: Bitmap = BitmapFactory.decodeFile( imagepath )
        var copied: Bitmap = bitmap.copy( Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copied)

        val objOcrresult = recogResponse.ocrresult
        val tex_result = objOcrresult.text ?.replace("\\", "\\\\").replace("\n", "\\n") ?: ""

        Log.d("retrofit","objOcrresult : ${ objOcrresult.toString() }")
        Log.d("retrofit","ocr_result : ${ objOcrresult.toString() }")
        Log.d("retrofit","tex_result : ${ tex_result }")

        binding.webview.loadUrl("javascript:convert('" +  tex_result + "')")


        val objRecresult = recogResponse.recresult
        Log.d("retrofit","objRecresult : ${ objRecresult.toString() }")
        rec_result = objRecresult.toString()

        awsurl = recogResponse.fulls3path

        var recResult = objRecresult.result
        val recPercent = objRecresult.percent
        Log.d("retrofit","recPercent : ${ recPercent }")

        val showing = recResult.joinToString("-") + " <==> percent: " + recPercent.toString()
        binding.txtRecresult.setText( showing )

        val line_data = objOcrresult.line_data
        Log.d("retrofit","line_data : ${line_data.toString() }")
        for (i in 0 until line_data.size ) {
            val innerObj = line_data[i]
            if( innerObj.included == false ) {
                continue
            }

            val cnt = innerObj.cnt
            val cntArray = ArrayList<PointF>()
            var ptMinimum = PointF(100000F, 100000F)
            for( j in 0 until cnt.size) {
                val insideValue = cnt.get(j)
                val ptValue = PointF( insideValue.get(0).toFloat(), insideValue.get(1).toFloat() )
                ptMinimum.x = Math.min( ptValue.x, ptMinimum.x )
                ptMinimum.y = Math.min( ptValue.y, ptMinimum.y )
                cntArray.add( ptValue )
            }
            val text = innerObj.text
            var localT = binding.txtOcrresult.text.toString()  + text
            binding.txtOcrresult.setText( localT )
            var insideIndex:Int = 0
            for( inner in text.split("\n") ) {
                drawText( canvas, inner , ptMinimum.x, ptMinimum.y + insideIndex * localFontSize )
                insideIndex ++
            }
            drawLine( canvas, cntArray )

//            val mtype = innerObj.getString("type")

            //Log.d("retrofit", cnt.toString())
            //Log.d("retrofit", text.toString())
            //Log.d("retrofit", mtype.toString())
        }

        binding.ivResult.setImageBitmap(bitmap)
        binding.ivResultCopied.setImageBitmap( copied )
    }

    private fun dataSendTest() {
        val inputInfo = InputInfo("id", "password")
        RetrofitClient.instance.postLogin( inputInfo )
            .enqueue(object: Callback<LoginResponse> {
                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    if( response.isSuccessful.not()) {
                        Log.d(TAG, "response.isSuccessful is not " + response.toString() )
                        return
                    }
                    response.body()?.let {
                        val loginResponse = response.body()
                        Log.d(TAG, "loginResult=" + loginResponse)
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.d(TAG, "error:::")
                }
            })
    }
    private fun getOCR_Report() {
        Log.d(TAG,"getOCR_Report is called.")
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
                else ->
                    message = "앱에서 알수 없는 오류가 발생하였습니다.\n문제가 지속되는 경우\n고객센터로 연락해 주세요."
            }
            WebViewActivity.CustomDialog(activity, message).show()
        }
    }
}