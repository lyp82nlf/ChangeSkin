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
    //外部皮肤包包名
    private var mOutPkgName: String? = null
    //自定义Resource
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
            //通过反射获取AssetManager类
            val assetManager: AssetManager = AssetManager::class.java.newInstance()
            //反射调用addAssetPath方法 将皮肤Apk资源加入到AssetManger中
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
        //这边有一个问题 就是在皮肤apk和与原先资源文件中 resource.arsc文件中 同一资源的资源id会改变
        //但是我没有遇到这个问题 不知道是因为我的资源文件太少 还是因为R8优化导致
        //解决思路就是 id虽然不一样  但是type和name是一样的
        //通过id找到type和name 然后再找到资源
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