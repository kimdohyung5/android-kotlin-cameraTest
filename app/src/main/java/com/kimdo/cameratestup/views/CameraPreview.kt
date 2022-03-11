package com.kimdo.cameratestup.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.*


class CameraPreview(context: Context, activity: AppCompatActivity, cameraID: Int, surfaceView: SurfaceView, cropBox: CropBox?) : SurfaceHolder.Callback {

    private var mContext:Context;
    private var mActivity: AppCompatActivity
    private val mCameraID: Int
    private var mCropBox: CropBox? = null

    private val mHolder: SurfaceHolder
    private var mCamera: Camera? = null

    private var isPreview = false

    private var TAG = "CameraPreview"

//    private fun setFocus(parameter: String) {
//        val parameters = mCamera!!.parameters
//        parameters.focusMode =  parameter
//        mCamera!!.parameters = parameters
//    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getRotation():Int {
//        val nRotation = mActivity.windowManager.defaultDisplay.rotation
        val nRotation = mActivity.display?.rotation?:0
        if (nRotation == Surface.ROTATION_0) return 90
        if (nRotation == Surface.ROTATION_90) return 0
        if (nRotation == Surface.ROTATION_180) return 270
        if (nRotation == Surface.ROTATION_270) return 180
        return 0
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

        Log.i(TAG, "surfaceCreated")
        try {
            mCamera = Camera.open(mCameraID) // attempt to get a Camera instance
        } catch (e: Exception) {
            Log.e("CameraPreview", "Camera " + mCameraID + " is not available: " + e.message)
        }
//        // retrieve camera's info.
//        val cameraInfo = Camera.CameraInfo()
//        Camera.getCameraInfo(mCameraID, cameraInfo)
//        mCameraInfo = cameraInfo
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Release the camera for other applications.
        if (mCamera != null) {
            if (isPreview) mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
            isPreview = false
        }
    }

//    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
//        val ASPECT_TOLERANCE = 0.1
//        val targetRatio = w.toDouble() / h
//        if (sizes == null) return null
//        var optimalSize: Camera.Size? = null
//        var minDiff = Double.MAX_VALUE
//
//        // Try to find an size match aspect ratio and size
//        for (size in sizes) {
//            val ratio = size.width.toDouble() / size.height
//            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
//            if (Math.abs(size.height - h) < minDiff) {
//                optimalSize = size
//                minDiff = Math.abs(size.height - h).toDouble()
//            }
//        }
//
//        // Cannot find the one match the aspect ratio, ignore the requirement
//        if (optimalSize == null) {
//            minDiff = Double.MAX_VALUE
//            for (size in sizes) {
//                if (Math.abs(size.height - h) < minDiff) {
//                    optimalSize = size
//                    minDiff = Math.abs(size.height - h).toDouble()
//                }
//            }
//        }
//        return optimalSize
//    }

    private fun setSize(parameters: Camera.Parameters): Camera.Parameters? {
        // TODO Auto-generated method stub
        var tempWidth = parameters.pictureSize.width
        var tempHeight = parameters.pictureSize.height
        var Result = 0
        var Result2 = 0
        var picSum = 0
        var picSum2 = 0
        var soin = 2
        while (tempWidth >= soin && tempHeight >= soin) {
            Result = tempWidth % soin
            Result2 = tempHeight % soin
            if (Result == 0 && Result2 == 0) {
                picSum = tempWidth / soin
                picSum2 = tempHeight / soin
                tempWidth = picSum
                tempHeight = picSum2
            } else {
                soin++
            }
        }
        val previewSizeList = parameters.supportedPreviewSizes
        for (size in previewSizeList) {
            tempWidth = size.width
            tempHeight = size.height
            Result = 0
            Result2 = 0
            var preSum = 0
            var preSum2 = 0
            soin = 2
            while (tempWidth >= soin && tempHeight >= soin) {
                Result = tempWidth % soin
                Result2 = tempHeight % soin
                if (Result == 0 && Result2 == 0) {
                    preSum = tempWidth / soin
                    preSum2 = tempHeight / soin
                    tempWidth = preSum
                    tempHeight = preSum2
                } else {
                    soin++
                }
            }
            if (picSum == preSum && picSum2 == preSum2) {
                parameters.setPreviewSize(size.width, size.height)
                break
            }
        }
        return parameters
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mHolder.surface == null) {
            // preview surface does not exist
            Log.d(TAG, "Preview surface does not exist")
            return
        }

        Log.i(TAG, "surfaceChanged")

        if( isPreview) {
            try {
                mCamera!!.stopPreview()
                isPreview = false
                Log.d(TAG, "Preview stopped.")
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: " + e.message)
            }
        }

        val params = mCamera!!.parameters
        setSize(params)

        val focusModes = params.supportedFocusModes
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        val flashModes = params.supportedFlashModes
        if( flashModes!= null ) {
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                params.flashMode = Camera.Parameters.FLASH_MODE_AUTO
            }
        }

        mCamera!!.parameters = params

        val nRotation = getRotation()
        mCamera!!.setDisplayOrientation(nRotation)

        try {
            mCamera!!.setPreviewDisplay(mHolder)
            mCamera!!.startPreview()
            isPreview = true
            Log.d(TAG, "Camera preview started.")
        } catch (e: Exception) {
            Log.d(TAG, "Error starting camera preview: " + e.message)
        }
    }

    fun takePicture() {
        mCamera!!.takePicture(shutterCallback, rawCallback, jpegCallback)
    }

    fun saveCallback(result: String?){
        val intent  = Intent(mActivity, CropActivity::class.java)
        intent.putExtra(CameraActivity.IMAGE_PATH, result)
        intent.putExtra(CameraActivity.FROM_WHERE, CameraActivity.FROM_CAMERA)
        mActivity.startActivity( intent )
        mActivity.finish()
    }

    var shutterCallback = Camera.ShutterCallback { }
    var rawCallback = Camera.PictureCallback { data, camera -> }

    //참고 : http://stackoverflow.com/q/37135675
    @RequiresApi(Build.VERSION_CODES.R)
    var jpegCallback = Camera.PictureCallback { data, camera ->
        val w = camera.parameters.pictureSize.width
        val h = camera.parameters.pictureSize.height

        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        val bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.size, options)
//        Log.d("CameraPreview", "bitmap width=${bitmap.width} bitmap height=${bitmap.height}")

        val matrix = Matrix()
        val orientation = getRotation()
        matrix.postRotate(orientation.toFloat())

        val rotatedBitmap = Bitmap.createBitmap(bitmapPicture, 0, 0, bitmapPicture.width, bitmapPicture.height, matrix, true)

        var croppedBitmap: Bitmap? = null
        if( mCropBox == null) {
            croppedBitmap = rotatedBitmap
        } else {
            val roiParent:RelativeLayout = mCropBox!!.parent as RelativeLayout
            val koefX =  rotatedBitmap.width.toFloat() / roiParent.width.toFloat()
            val koefY =  rotatedBitmap.height.toFloat() / roiParent.height.toFloat()

            val cropStartX = Math.round(mCropBox!!.rect.left * koefX)
            val cropStartY = Math.round(mCropBox!!.rect.top * koefY)
            val cropWidthX = Math.round(mCropBox!!.rect.width() * koefX)
            val cropHeightY = Math.round(mCropBox!!.rect.height() * koefY)

            if (cropStartX + cropWidthX <= rotatedBitmap.width && cropStartY + cropHeightY <= rotatedBitmap.height &&
                cropStartX > 0 && cropStartY > 0
            ) {
                croppedBitmap = Bitmap.createBitmap(rotatedBitmap, cropStartX, cropStartY, cropWidthX, cropHeightY)
            }
        }

        processAfterCrop( croppedBitmap )
    }

    private fun processAfterCrop( croppedBitmap: Bitmap?) {
        if( croppedBitmap != null ) {
            CoroutineScope(Dispatchers.Main).launch {
                var returnValue  = ""
                CoroutineScope(Dispatchers.Default).async {
                    var outStream: FileOutputStream?
                    try {
//                val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/camtest")
                        val path = File(mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.absolutePath + "/camtest" )

                        if (!path.exists()) {
                            path.mkdirs()
                        }
                        val fileName = String.format("%d.jpg", System.currentTimeMillis())
                        val outputFile = File(path, fileName)
                        outStream = FileOutputStream(outputFile)
                        val bitmap:Bitmap? = croppedBitmap
                        if( bitmap != null) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                            outStream.flush()
                            outStream.close()
//                            Log.d("CameraPreview", "onPictureTaken - wrote bytes: " + 0 + " to "      + outputFile.absolutePath)
                        }
                        returnValue = outputFile.absolutePath
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }.await()
                this@CameraPreview.saveCallback( returnValue )
            }
        } else {
            saveCallback("");
        }
    }

    init {
        mContext = context
        mActivity = activity
        mCameraID = cameraID
        mCropBox = cropBox
        mHolder = surfaceView.holder
        mHolder.addCallback(this)
    }
}