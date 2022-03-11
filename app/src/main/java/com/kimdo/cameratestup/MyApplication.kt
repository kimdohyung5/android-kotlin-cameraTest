package com.kimdo.cameratestup

import android.app.Application
import com.kimdo.cameratestup.models.RecogResponse

class MyApplication : Application() {

    init{
        instance = this
    }

    var regcogResponse:RecogResponse?= null
    var gotoWhere:String? = null
    var imagepath:String? = null

    companion object {
        lateinit var instance: MyApplication
    }

}