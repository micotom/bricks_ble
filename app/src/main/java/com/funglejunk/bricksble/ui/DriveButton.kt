package com.funglejunk.bricksble.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView

class DriveButton : AppCompatImageView {

    private var onHoldListener : (() -> Unit)? = null
    private var onReleaseListener : (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun onPressHold(f: () -> Unit) {
        onHoldListener = f
    }

    fun onRelease(f: () -> Unit) {
        onReleaseListener = f
    }

    init {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> onReleaseListener?.invoke()
                MotionEvent.ACTION_DOWN -> onHoldListener?.invoke()
            }
            v.performClick()
        }
    }

}