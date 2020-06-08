package com.dsg.skindemo

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import java.io.File
import java.lang.Exception

/**
 * @Project skinDemo
 * @author  DSG
 * @date    2020/6/8
 * @describe
 */
object SkinEngine {

    lateinit var mContext: Context
    private var mOutPkgName: String? = null
    private var mOutResource: Resources? = null

    fun init(context: Context) {
        this.mContext = context
    }

    fun load(path: String) {
        val file = File(path)
        if (!file.exists()) {
            return
        }
        try {
            val pm = mContext.packageManager
            val packageInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
            mOutPkgName = packageInfo.packageName
            val assetManager: AssetManager = AssetManager::class.java.newInstance()
            val method = assetManager::class.java.getMethod("addAssetPath", String::class.java)
            method.invoke(assetManager, path)
            mOutResource = Resources(
                assetManager,
                mContext.resources.displayMetrics,
                mContext.resources.configuration
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getDrawable(resId: Int): Drawable? {
        if (mOutResource == null) {
            return ContextCompat.getDrawable(mContext, resId)
        }
        val resName = mOutResource!!.getResourceEntryName(resId)
        val outResId = mOutResource!!.getIdentifier(resName, "drawable", mOutPkgName)
        return if (outResId == 0) {
            ContextCompat.getDrawable(mContext, resId)
        } else mOutResource!!.getDrawable(outResId)
    }

    fun getColor(resId: Int): Int {
        if (mOutResource == null) {
            return resId
        }
        val resName = mOutResource!!.getResourceEntryName(resId)
        val outResId = mOutResource!!.getIdentifier(resName, "color", mOutPkgName)
        return if (outResId == 0) {
            resId
        } else mOutResource!!.getColor(outResId)
    }
}