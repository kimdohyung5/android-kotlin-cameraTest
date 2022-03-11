package com.kimdo.cameratestup.views

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.kimdo.cameratestup.MyApplication
import com.kimdo.cameratestup.R
import com.kimdo.cameratestup.models.RecogResponse
import com.kimdo.cameratestup.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class CropActivity : AppCompatActivity() {

    private lateinit var mCropBox: CropBox
    private lateinit var mLayout : ConstraintLayout
    private var mImagePath:String?= null
    private var mFromWhere:String?= null
    lateinit var retrofit : Retrofit

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crop_layout)

        hideSystemUI()

        val imgpath = intent.getStringExtra(CameraActivity.IMAGE_PATH)
        mImagePath = imgpath
        mFromWhere = intent.getStringExtra(CameraActivity.FROM_WHERE)
        Log.i("kkkk", "CropActivity mFromWhere=" + mFromWhere)

        val imageView: ImageView = findViewById(R.id.imageviewResult)
        imageView.setImageURI( Uri.parse( imgpath ))
        Log.i("kkkk", "imgpath=" + imgpath)


        val btnClose: Button = findViewById(R.id.button_close_crop)
        btnClose.setOnClickListener {
            val intent  = Intent(this, CameraActivity::class.java)
//            intent.putExtra("imgpath", result)
            startActivity( intent )
            finish()
        }

        mLayout = findViewById(R.id.layout_crop)
        mCropBox = CropBox( this )
        mLayout.addView( mCropBox )

        mCropBox.viewTreeObserver.addOnGlobalLayoutListener(
            object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mCropBox.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    mCropBox.firstCalculateRect()
                }
            }
        )

        val btnCrop: Button = findViewById(R.id.button_crop)
        btnCrop.setOnClickListener {
            val bitmap = BitmapFactory.decodeFile( mImagePath )
            val koefX =  bitmap.width.toFloat() / imageView.width.toFloat()
            val koefY =  bitmap.height.toFloat() / imageView.height.toFloat()


            val cropStartX = Math.round(mCropBox.rect.left * koefX)
            val cropStartY = Math.round(mCropBox.rect.top * koefY)
            val cropWidthX = Math.round(mCropBox.rect.width() * koefX)
            val cropHeightY = Math.round(mCropBox.rect.height() * koefY)

            var croppedBitmap: Bitmap? = null
            if (cropStartX + cropWidthX <= bitmap.width && cropStartY + cropHeightY <= bitmap.height &&
                cropStartX > 0 && cropStartY > 0
            ) {
                croppedBitmap = Bitmap.createBitmap(bitmap, cropStartX, cropStartY, cropWidthX, cropHeightY)
            }
            processAfterCrop( croppedBitmap )
        }
    }

    private fun processAfterCrop( croppedBitmap: Bitmap?) {
        if( croppedBitmap != null ) {
            CoroutineScope(Dispatchers.Main).launch {
                var returnValue  = ""
                CoroutineScope(Dispatchers.Default).async {
                    var outStream: FileOutputStream?
                    try {
                        val path = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + "/camtestx" )
                        returnValue += path.absolutePath
                        if (!path.exists()) {
                            path.mkdirs()
                        }
                        returnValue += "/"
                        val fileName = String.format("%d.jpg", System.currentTimeMillis())
                        returnValue += fileName
                        val outputFile = File(path, fileName)
                        outStream = FileOutputStream(outputFile)
                        val bitmap: Bitmap? = croppedBitmap
                        if( bitmap != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                            outStream.flush()
                            outStream.close()
                            Log.d("CameraPreview", "onPictureTaken - wrote bytes: " + 0 + " to "      + outputFile.absolutePath)
                        }
                        returnValue = outputFile.absolutePath
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.await()
                this@CropActivity.saveCallback(returnValue)
            }
        } else {
            saveCallback(null );
        }
    }

    private fun getOCR_Result( imagepath:String ) {

        val ocrAPI = RetrofitClient.instance
        val file  = File(imagepath)
        val fileBody: RequestBody = RequestBody.create(MediaType.parse("image/*"), file)
        val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)

        Runnable {
            ocrAPI.getOCR_Result(filePart, "LRN19001231123456789" ).enqueue(object :
                Callback<RecogResponse> {
                override fun onFailure(call: Call<RecogResponse>, t: Throwable) {
                    Log.d("retrofit",t.message?.toString()!!)
                }

                override fun onResponse(call: Call<RecogResponse>, response: Response<RecogResponse>) {
                    Log.d("retrofit","response : ${response.body()}")


                    if( response.body() == null) {
                        Log.i("kkkk", "서버의 ocr 결과값이 null입니다. 서버를 기동시켜주세요...")
                        Log.i("kkkk", "서버의 ocr 결과값이 null입니다. 서버를 기동시켜주세요...")
                        Log.i("kkkk", "서버의 ocr 결과값이 null입니다. 서버를 기동시켜주세요...")
                        finish()
                    } else {
                        MyApplication.instance.regcogResponse = response.body()
                        MyApplication.instance.imagepath = imagepath
                        finish()
                    }
                }
            })
        }.run()
    }
    fun saveCallback(filepath: String?){
        if(filepath == null) {
            Toast.makeText(this, "먼저 영역을 선택하세요.", Toast.LENGTH_LONG).show()
            return
        }
        // 준비영상을 뿌려주고 시작을 한다.
//        Toast.makeText(this, "이제 인식중입니다. ${filepath}", Toast.LENGTH_LONG).show()
        getOCR_Result( filepath )
    }

}