package com.dsg.skindemo

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * @Project skinDemo
 * @author  DSG
 * @date    2020/6/8
 * @describe
 */
class ZeroView : View {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
    }
}