package com.dsg.skindemo

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import java.io.File

/**
 * @Project skinDemo
 * @author  DSG
 * @date    2020/6/8
 * @describe
 */
open class BaseActivity : AppCompatActivity() {
    var ifAllowChangeSkin = true

    lateinit var skinFactory: SkinFactory

    var currentSkin: String? = null
    var skins = arrayOf("skin1.apk", "skin2.apk")


    override fun onCreate(savedInstanceState: Bundle?) {
        if (ifAllowChangeSkin) {
            skinFactory = SkinFactory()
            skinFactory.delegate = delegate
            val layoutInflater = LayoutInflater.from(this)
            if (layoutInflater.factory == null) {
                LayoutInflaterCompat.setFactory2(layoutInflater, skinFactory)
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (null != currentSkin) {
            changeSkin(currentSkin!!) // 换肤操作必须在setContentView之后
        }
    }

    fun getPath(): String {
        val path: String
        path = if (null == currentSkin) {
            skins[0]
        } else if (skins[0] == currentSkin) {
            skins[1]
        } else if (skins[1] == currentSkin) {
            skins[0]
        } else {
            return "unknown skin"
        }
        return path
    }

    fun changeSkin(path: String) {
        if (ifAllowChangeSkin) {
            var file = File(getExternalFilesDir(""), path)
            SkinEngine.load(file.absolutePath)
            skinFactory.changeSkins()
            currentSkin = path
        }
    }
}