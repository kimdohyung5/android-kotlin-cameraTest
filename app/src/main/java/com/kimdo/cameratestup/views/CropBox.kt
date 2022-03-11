package com.kimdo.cameratestup.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CropBox : View {
    private val paint = Paint()
    lateinit var rect: Rect
        private set

    constructor(context: Context?) : super(context) {
        rect = Rect()
        setOnTouchListener(Crop())
    }

    constructor(context: Context?, attributeSet: AttributeSet?) : super(context, attributeSet) {}

    fun firstCalculateRect() {
        val nParentWidth = (this.parent as View).width
        val nParentHeight = (this.parent as View).height
        val nHalfWidth = nParentWidth / 3
        val nHalfHeight = nParentHeight / 10
        val nHalfX = nParentWidth / 2
        val nHalfY = nParentHeight / 2
        rect = Rect(nHalfX - nHalfWidth, nHalfY - nHalfHeight, nHalfX + nHalfWidth, nHalfY + nHalfHeight)
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        paint.strokeWidth = 2f
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        canvas.drawRect(rect!!, paint)
        canvas.drawLine((rect!!.right - DRAG_SQUARE).toFloat(), (rect!!.bottom - DRAG_SQUARE).toFloat(),
            rect!!.right.toFloat(), (rect!!.bottom - DRAG_SQUARE).toFloat(), paint)
        canvas.drawLine((rect!!.right - DRAG_SQUARE).toFloat(), (rect!!.bottom - DRAG_SQUARE).toFloat(), (
                rect!!.right - DRAG_SQUARE).toFloat(), rect!!.bottom.toFloat(), paint)
    }

    internal inner class Crop : OnTouchListener {
        private var mode = Companion.NONE
        private val start = PointF()
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    start[event.x - rect!!.left] = event.y - rect!!.top
                    mode = if (event.x <= rect!!.right && event.x >= rect!!.right - DRAG_SQUARE
                        && event.y >= rect!!.top && event.y >= rect!!.bottom - DRAG_SQUARE) {
                        Companion.BORDER_DRAG
                    } else if (rect!!.contains(event.x.toInt(), event.y.toInt())) {
                        Companion.BOX_DRAG
                    } else {
                        Companion.NONE
                    }
                }
                MotionEvent.ACTION_UP -> mode = Companion.NONE
                MotionEvent.ACTION_MOVE -> if (mode == Companion.BOX_DRAG) {
                    val targetLeft = event.x.toInt() - start.x.toInt() + view.left
                    val targetTop = event.y.toInt() - start.y.toInt() + view.top
                    val targetRight = targetLeft + rect!!.width()
                    val targetBottom = targetTop + rect!!.height()
                    if (targetLeft <= 0 || targetTop <= 0 || targetRight >= width || targetBottom >= height) {}
                    else {
                        rect!!.left = targetLeft
                        rect!!.right = targetRight
                        rect!!.top = targetTop
                        rect!!.bottom = targetBottom
                    }

                } else if (mode == Companion.BORDER_DRAG) {
                    if (event.x >= width || event.y >= height) {}
                    else {
                        rect!!.right = event.x.toInt()
                        rect!!.bottom = event.y.toInt()
                    }

                }
            }
            invalidate()
            return true
        }


    }

    companion object {
        private const val DRAG_SQUARE = 75
        private const val NONE = 0
        private const val BOX_DRAG = 1
        private const val BORDER_DRAG = 2
    }
}
