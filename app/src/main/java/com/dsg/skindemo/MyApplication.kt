package com.dsg.skindemo

import android.app.Application

/**
 * @Project skinDemo
 * @author  DSG
 * @date    2020/6/8
 * @describe
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SkinEngine.init(this)
    }
}