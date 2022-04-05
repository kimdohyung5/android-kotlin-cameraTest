package com.kimdo.cameratestup.views

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings

import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.kimdo.cameratestup.MyApplication
import com.kimdo.cameratestup.R
import com.kimdo.cameratestup.utils.Constants
import java.io.File
import java.io.FileOutputStream

class CameraActivity: AppCompatActivity()
    , ActivityCompat.OnRequestPermissionsResultCallback
{
    private val PERMISSIONS_REQUEST_CODE = 100
    private var REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
    private val CAMERA_FACING = Camera.CameraInfo.CAMERA_FACING_BACK // Camera.CameraInfo.CAMERA_FACING_FRONT

    private lateinit var surfaceView: SurfaceView
    private lateinit var mCameraPreview: CameraPreview
    private lateinit var mLayout : RelativeLayout

    private var mFromWhere:String?= null

    companion object {
        const val IMAGE_PATH = "imgpath"
        const val FROM_WHERE = "from_where"
        const val FROM_GALLERY = "from_gallery"
        const val FROM_CAMERA = "from_camera"

        const val TAG = "CameraActivity"
    }

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

    private val startGalleryForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        result: ActivityResult ->
            if( result.resultCode == Activity.RESULT_OK ) {
                result.data?.data?.let { it ->
                    val imagePath = createLocalFileFromUri(it)
                    Log.i(TAG, "it= ${it} imagePath=${imagePath}" )
                    val intent  = Intent(this@CameraActivity, CropActivity::class.java)
                    intent.putExtra(IMAGE_PATH, imagePath)
                    intent.putExtra(FROM_WHERE, FROM_GALLERY)
                    this@CameraActivity.startActivity( intent )
                    this@CameraActivity.finish()
                }
            }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 화면 켜진 상태를 유지합니다.
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.camera_layout)

        hideSystemUI()

        mFromWhere = intent.getStringExtra(FROM_WHERE)
//        Log.i(TAG, "CropActivity mFromWhere=" + mFromWhere)
        if( mFromWhere != null) {
            if(mFromWhere == FROM_GALLERY) {
                val intent = Intent( Intent.ACTION_GET_CONTENT)
                intent.setType("image/*")
                startGalleryForResult.launch( intent )
            }
        }


        mLayout = findViewById(R.id.layout_main)
        surfaceView = findViewById(R.id.camera_preview_main)

        val btnClose: Button = findViewById(R.id.button_close)
        btnClose.setOnClickListener(View.OnClickListener {  finish(); })

        val btnHistory: Button = findViewById(R.id.button_history)
        btnHistory.setOnClickListener(View.OnClickListener {
            Constants.gotoWhere = "history"
            finish()
        })

        val btnCapture: Button = findViewById(R.id.button_main_capture)
        btnCapture.setOnClickListener(View.OnClickListener { mCameraPreview.takePicture() })

        val btnHelp: Button = findViewById(R.id.button_help)
        btnHelp.setOnClickListener(View.OnClickListener {
            Log.i(TAG, "btnHelp")

            // 우선 막아둠.. 2022.03.10.
//            val intent = Intent( this, HelpActivity::class.java)
//            startActivity( intent )

        })

        val btnGallery: Button = findViewById(R.id.button_gallery)
        btnGallery.setOnClickListener(View.OnClickListener {
            val intent = Intent( Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startGalleryForResult.launch( intent )
        })

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            val snackbar = Snackbar.make(
                mLayout, "디바이스가 카메라를 지원하지 않습니다.",
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.setAction("확인") { snackbar.dismiss() }
            snackbar.show()
            return;
        }
        // 현재 min sdk는 m이하로 설정이 되어 있다.
//        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            startCamera()
//            return
//        }

        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val readExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (cameraPermission == PackageManager.PERMISSION_GRANTED
            && readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
        ) {
            restartCamera()
        } else {
//            surfaceView.visibility = View.GONE
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])
            ) {
                Snackbar.make(mLayout, "이 앱을 실행하려면 카메라와 외부 저장소 접근 권한이 필요합니다.",
                    Snackbar.LENGTH_INDEFINITE).setAction("확인") {
                    ActivityCompat.requestPermissions(this@CameraActivity, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE)
                }.show()
            } else {
                // 2. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                    PERMISSIONS_REQUEST_CODE)
            }
        }


    }

    private fun askShowSettingsDialog() {
        AlertDialog.Builder(this)
            .setMessage("퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri

                    finish()
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                finish()
                dialog.dismiss()
            }.show()
    }

    private fun restartCamera() {
        mCameraPreview = CameraPreview(this, this, CAMERA_FACING, surfaceView, null)
        surfaceView.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grandResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grandResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE && grandResults.size == REQUIRED_PERMISSIONS.size) {
            var check_result = true
            for (result in grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false
                    break
                }
            }
            if (check_result) {
                restartCamera()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])
                ) {
                    Snackbar.make(mLayout!!, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인") { finish() }.show()
                } else {
                    askShowSettingsDialog()
                }
            }
        }
    }

    private fun createLocalFileFromUri(uri: Uri): String {
        //val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/camtest")
        val path = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + "/camtest" )
        if (!path.exists()) {
            path.mkdirs()
        }
        val fileName = String.format("%d.jpg", System.currentTimeMillis())
        val file = File(path, fileName)

        try {
            var inputStream = contentResolver.openInputStream( uri )
            if( inputStream == null) return ""

            val outputStream = FileOutputStream( file )
            var buf = ByteArray(1024 )

            while(true) {
                val length = inputStream.read( buf )
                if( length <= 0) break
                outputStream.write( buf, 0, length)
            }
            outputStream.flush()
            return file.absolutePath
        } catch ( t: Throwable) {

        }
        return ""
    }
}

