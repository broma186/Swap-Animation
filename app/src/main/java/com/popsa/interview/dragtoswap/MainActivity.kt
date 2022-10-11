package com.popsa.interview.dragtoswap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import com.popsa.interview.dragtoswap.MainActivityCoordinator.Events.ImageDropped
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_scrolling.*

/**
 * Place for applying view data to views, and passing actions to coordinator
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var coordinator: MainActivityCoordinator
    private var floatingThumbnail: ShapeableImageView? = null
    private lateinit var runnable: Runnable
    private val handler = Handler()

    private val imageViews: List<ImageView> by lazy {
        listOf(
            suspensionBridge,
            brightBridge,
            times1000,
            niceBuildings
        )
    }

    companion object {
        const val HALF_A_SECOND = 500L
        const val QUARTER_OF_A_SECOND = 250L
        const val FLOATING_THUMBNAIL_DIAMETER = 200
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        coordinator = MainActivityCoordinator(viewModel)
        setSupportActionBar(toolbar)
        toolbar.title = title
        viewModel.images.observe(this, Observer { images ->
            imageViews.forEachIndexed { index, imageView ->
                Glide.with(this)
                    .load(images[index].imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(imageView)
                imageView.tag = index
            }
        })
        var upWasCalled = false
        mainLayout.setOnTouchListener { view, event ->
            val eventX = event.x.toInt()
            val eventY = event.y.toInt()
            runnable = Runnable {
                runOnUiThread {
                    if (!upWasCalled) {
                        val downImageView = getImageViewAt(eventX, eventY)
                        showFloatingThumbnail(view, eventX, eventY, downImageView)
                        vibratePhone()

                        val index = downImageView?.tag as Int
                        coordinator.startedSwap(index)
                    }
                }
            }
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    upWasCalled = false
                    getImageViewAt(eventX, eventY)?.let {
                        handler.postDelayed(runnable, HALF_A_SECOND) // Trigger floating image representation of imageView to appear in half a second.
                    }
                }
                MotionEvent.ACTION_UP -> {
                    upWasCalled = true
                    coordinator.imageDropped(view, eventX, eventY)
                }
                MotionEvent.ACTION_MOVE -> {
                    getImageViewAt(eventX, eventY)?.let { currentImage ->
                        floatingThumbnail?.x = event.x - (floatingThumbnail?.width?.div(2) ?: 0) // move left by radius of thumbnail so your thumb is right on it.
                        floatingThumbnail?.y = event.y - (floatingThumbnail?.height?.div(2) ?: 0)
                        imageViews.forEach {
                            it.drawable.clearColorFilter() // Clear all yellow (ostentatious) colours every time we move over an image.
                        }
                        currentImage.setColorFilter( // Apply colour filter to only the imageview we are touching.
                            resources.getColor(
                                R.color.colorOstentatiousBackground,
                                null
                            ), PorterDuff.Mode.SRC_ATOP
                        )
                    }
                }
            }
            true
        }
        viewModel.events.observe(this, Observer {
            when (it) {
                is ImageDropped -> dropImage(it.view, it.x, it.y)
            }
        })
    }

    private fun dropImage(view: View, eventX: Int, eventY: Int) {
        val sourceImageIndex = viewModel.draggingIndex.value
        val targetImage = getImageViewAt(eventX, eventY)
        val targetImageIndex = targetImage
            ?.let { it.tag as Int }
        if (targetImageIndex != null && sourceImageIndex != null && targetImageIndex != sourceImageIndex) {
            coordinator.swapImages(sourceImageIndex, targetImageIndex)
        } else {
            coordinator.cancelSwap()
        }
        imageViews.forEach { // Reset the colour filters
            it.clearColorFilter()
        }
        floatingThumbnail?.animate()?.apply { // Thumbnail fades out after swap
            duration = QUARTER_OF_A_SECOND
        }?.alpha(0.0f)?.withEndAction {
            resetFloatingThumbnail(view)
        }
    }

    private fun resetFloatingThumbnail(view: View) {
        (view as ViewGroup).removeView(floatingThumbnail)
        handler.removeCallbacks(runnable)
    }

    private fun getImageViewAt(x: Int, y: Int): ImageView? {
        val hitRect = Rect()
        return imageViews.firstOrNull {
            it.getHitRect(hitRect)
            hitRect.contains(x, y)
        }
    }

    private fun showFloatingThumbnail(
        view: View,
        eventX: Int,
        eventY: Int,
        selectedImageView: ImageView?
    ) {
        floatingThumbnail = ShapeableImageView(baseContext).apply {
            val coordinates = IntArray(2)
            getLocationOnScreen(coordinates)
            val x = eventX - coordinates[0]
            val y = eventY - coordinates[1]
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = FLOATING_THUMBNAIL_DIAMETER
                height = FLOATING_THUMBNAIL_DIAMETER
                setMargins(
                    x - (width / 2),
                    y - (height / 2),
                    0,
                    0
                )
            }
            setPadding(5, 5, 5, 5)
            setImageDrawable(selectedImageView?.drawable)
            scaleType = ImageView.ScaleType.CENTER_CROP
            shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(ShapeAppearanceModel.PILL)
                .build()
            strokeColor = resources.getColorStateList(
                R.color.colorOstentatious,
                null
            )
            strokeWidth = 8f
        }
        (view as ViewGroup).addView(floatingThumbnail)
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }
}