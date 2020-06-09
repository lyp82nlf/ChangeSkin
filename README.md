我们已经学习了Activity  setContentView 和Resource加载的过程 &nbsp; 没看过可以先阅读一下[手撸动态换肤框架(一)](https://www.jianshu.com/p/cbd9cacb71b7)

接下来我们就直接手撸一个动态换肤框架 

我们分析一下思路 &nbsp; 首先,我们需要代理AppCompatActivity类&nbsp;将我们需要换肤的View收集到一个数组中  然后创建一个使用新的皮肤包的代理Resource类 &nbsp;将所有的View改变背景和图片

心急的同学请直接转移[源码](https://github.com/lyp82nlf/ChangeSkin)

#### 收集所有需要换肤的View
这边就可以使用我们上一章学习到的Factory2类 动态代理AppCompatActivity的加载过程 &nbsp;具体请看代码

```
package com.dsg.skindemo

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import java.lang.reflect.Constructor

/**
 * @Project skinDemo
 * @author  DSG
 * @date    2020/6/8
 * @describe
 */
class SkinFactory : LayoutInflater.Factory2 {

    //我们并不需要代理所有的创建过程 所以很多事情都可以交给原先的Delegate类实现
    lateinit var delegate: AppCompatDelegate

    //收集需要换肤的View
    var skinList = arrayListOf<SkinView>()


    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        //因为我们并不需要实现所有流程 所以createView可以交给原先的代理类完成
        var view = delegate.createView(parent, name, context, attrs)
        if (view == null) {
            //万一系统创建出来是空，那么我们来补救 这边也是参考了系统实现 我们之前应该也都分析了
            mConstructorArgs[0] = context
            try {
                if (-1 == name.indexOf('.')) { //不包含. 说明不带包名，那么我们帮他加上包名
                    view = createViewByPrefix(context, name, sClassPrefixList, attrs)
                } else { //包含. 说明 是权限定名的view name，
                    view = createViewByPrefix(context, name, null, attrs)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        //关键点 收集需要换肤的View
        collectSkinView(context, attrs, view)

        return view
    }

    private fun collectSkinView(context: Context, attrs: AttributeSet, view: View?) {
        //这边判断了是否使用isSupport自定义属性 但是并没有对自定义View做很好的兼容 
        //对于自定义View  我们可以采用换肤接口来实现 具体实现可以大家自己尝试一下
        val obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.Skinable)
        val isSupport = obtainStyledAttributes.getBoolean(R.styleable.Skinable_isSupport, false)
        if (isSupport) {
            val skinView = SkinView()
            //找到支持换肤的view
            val len = attrs.attributeCount
            val attrMap = hashMapOf<String, String>()
            for (i in 0 until len) { //遍历所有属性
                val attrName = attrs.getAttributeName(i)
                val attrValue = attrs.getAttributeValue(i)
                attrMap[attrName] = attrValue //全部存起来
            }

            if (view != null) {
                skinView.view = view
            }
            skinView.attrsMap = attrMap
            skinList.add(skinView)
        }
    }
    
    
    fun changeSkins() {
        //遍历换肤类 实现换肤
        for (skin in skinList) {
            skin.changeSkin()
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return null
    }

    class SkinView {
        lateinit var view: View
        lateinit var attrsMap: HashMap<String, String>

        fun changeSkin() {
            if (!TextUtils.isEmpty(attrsMap["background"])) { //属性名,例如，这个background，text，textColor....
                val bgId = attrsMap["background"]!!.substring(1).toInt() //属性值，R.id.XXX ，int类型，
                // 这个值，在app的一次运行中，不会发生变化
                val attrType = view.resources.getResourceTypeName(bgId) // 属性类别：比如 drawable ,color
                if (TextUtils.equals(attrType, "drawable")) { //区分drawable和color
                    view.setBackgroundDrawable(SkinEngine.getDrawable(bgId)) //加载外部资源管理器，拿到外部资源的drawable
                } else if (TextUtils.equals(attrType, "color")) {
                    view.setBackgroundColor(SkinEngine.getColor(bgId))
                }
            }

            if (view is TextView) {
                if (!TextUtils.isEmpty(attrsMap["textColor"])) {
                    val textColorId = attrsMap["textColor"]!!.substring(1).toInt()
                    (view as TextView).setTextColor(SkinEngine.getColor(textColorId))
                }
            }

            //那么如果是自定义组件呢
            //那么如果是自定义组件呢
            if (view is ZeroView) { //那么这样一个对象，要换肤，就要写针对性的方法了，每一个控件需要用什么样的方式去换，尤其是那种，自定义的属性，怎么去set，
                // 这就对开发人员要求比较高了，而且这个换肤接口还要暴露给 自定义View的开发人员,他们去定义
                // ....
            }
        }
    }


    val mConstructorSignature = arrayOf(Context::class.java, AttributeSet::class.java) //
    val mConstructorArgs = arrayOfNulls<Any>(2) //View的构造函数的2个"实"参对象
    private val sConstructorMap = HashMap<String, Constructor<out View?>>() //用映射，将View的反射构造函数都存起来
    private val sClassPrefixList = arrayOf(
        "android.widget.",
        "android.view.",
        "android.webkit."
    )

    /**
     * 反射创建View
     *
     * @param context
     * @param name
     * @param prefixs
     * @param attrs
     * @return
     */
    private fun createViewByPrefix(
        context: Context,
        name: String,
        prefixs: Array<String>?,
        attrs: AttributeSet
    ): View? {
        var constructor: Constructor<out View?>? = sConstructorMap.get(name)
        var clazz: Class<out View?>? = null
        if (constructor == null) {
            try {
                if (prefixs != null && prefixs.size > 0) {
                    for (prefix in prefixs) {
                        clazz = context.classLoader.loadClass(
                            if (prefix != null) prefix + name else name
                        ).asSubclass(View::class.java) //控件
                        if (clazz != null) break
                    }
                } else {
                    if (clazz == null) {
                        clazz = context.classLoader.loadClass(name)
                            .asSubclass(View::class.java)
                    }
                }
                if (clazz == null) {
                    return null
                }
                constructor = clazz.getConstructor(*mConstructorSignature) //拿到 构造方法，
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                return null
            }
            constructor.isAccessible = true //
            sConstructorMap.put(name, constructor) //然后缓存起来，下次再用，就直接从内存中去取
        }
        val args = mConstructorArgs
        args[1] = attrs
        try { //通过反射创建View对象
            return constructor.newInstance(*args)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }
    //********************************************************************************
}
```


代码写的也比较详细 &nbsp; 具体实现流程也就是收集需要换肤的View和属性值 然后在合适的时机调用换肤

#### 自定义Resource类
我们思考一下这边需要做哪些事情 &nbsp; 大致上来说 &nbsp; 也就是使用原先的Resource和AssetManager &nbsp; 实现一个新的Resource &nbsp; 然后使用新的Resource来加载资源

```
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
```

具体注释也写的比较详细 &nbsp; 需要注意的点就是resource.arsc问题 &nbsp; 然后在高版本中 addAssetPath已经被废弃了 &nbsp; 可以使用setApkAssets替代

####Hook AppCompatActivity代理
```
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
        //要在super.onCreate调用 否则会报错
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
```
主要看onCreate方法 

####总结
换肤框架 &nbsp; 基本原理就是使用Resource类 &nbsp; 加载皮肤apk中的资源 来动态改变项目中的所有资源

