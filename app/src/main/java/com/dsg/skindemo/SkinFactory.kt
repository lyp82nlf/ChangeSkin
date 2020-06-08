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

    lateinit var delegate: AppCompatDelegate

    var skinList = arrayListOf<SkinView>()


    override fun onCreateView(
        parent: View?,
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        var view = delegate.createView(parent, name, context, attrs)
        //TODO: 关键点2 收集需要换肤的View
        if (view == null) {
            //万一系统创建出来是空，那么我们来补救
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
        collectSkinView(context, attrs, view)

        return view
    }

    private fun collectSkinView(context: Context, attrs: AttributeSet, view: View?) {
        var obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.Skinable)
        var isSupport = obtainStyledAttributes.getBoolean(R.styleable.Skinable_isSupport, false)
        if (isSupport) {
            var skinView = SkinView()


            //找到支持换肤的view
            val Len = attrs.attributeCount
            val attrMap = hashMapOf<String, String>()
            for (i in 0 until Len) { //遍历所有属性
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
    //***********************************************************************************************************************************


}