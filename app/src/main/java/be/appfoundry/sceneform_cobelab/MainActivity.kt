package be.appfoundry.sceneform_cobelab

import android.graphics.Point
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import android.widget.Toast
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import android.view.PixelCopy
import android.os.HandlerThread
import android.graphics.Bitmap
import android.os.Environment
import android.os.Handler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    // ArCore Fragment
    private lateinit var arFragment: ArFragment
    private lateinit var modelLoader: ModelLoader
    // pointers
    private val pointerDrawable = PointerDrawable()
    private var isTracking: Boolean = false
    private var isHitting: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Init ArFragment
        arFragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        modelLoader = ModelLoader(WeakReference(this))
        //
        arFragment.arSceneView.scene.addOnUpdateListener {
            arFragment.onUpdate(it)
            onUpdate()
        }
        // add custom models
        initGallery()
        fab.setOnClickListener { _ -> takePhoto() }
    }

    private fun onUpdate() {
        val trackingChanged = updateTracking()

        val contentView = findViewById<View>(android.R.id.content)
        if (trackingChanged) {
            if (isTracking) {
                contentView.overlay.add(pointerDrawable)
            } else {
                contentView.overlay.remove(pointerDrawable)
            }
        }

        if (isTracking) {
            val hitTestedChanged = updateHitTest()
            if (hitTestedChanged) {
                pointerDrawable.enabled = isHitting
                contentView.invalidate()
            }
        }
    }

    private fun updateTracking(): Boolean {
        val frame: Frame? = arFragment.arSceneView.arFrame
        val wasTracking = isTracking
        isTracking = frame != null && frame.camera.trackingState == TrackingState.TRACKING


        return isTracking != wasTracking
    }

    private fun updateHitTest(): Boolean {
        val frame: Frame = arFragment.arSceneView.arFrame as Frame
        val point: Point = getScreenCenter()
        val hitList: List<HitResult>
        val wasHitting = isHitting
        isHitting = false
        if (frame != null) {
            hitList = frame.hitTest(point.x + 0f, point.y + 0f)
            for (hit in hitList) {
                val trackable = hit.trackable
                if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                    isHitting = true
                    break
                }
            }
        }
        return wasHitting != isHitting
    }

    private fun getScreenCenter(): Point {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
    }

    private fun initGallery() {
        val layout: LinearLayout = findViewById(R.id.gallery_layout)
        // Andy
        val andyImageView = ImageView(this)
        andyImageView.setImageResource(R.drawable.droid_thumb)
        andyImageView.setOnClickListener {
            addObject(Uri.parse("andy_dance.sfb"))
        }
        gallery_layout.addView(andyImageView)

        val cabinImageView = ImageView(this)
        cabinImageView.setImageResource(R.drawable.cabin_thumb)
        cabinImageView.setOnClickListener {
            addObject(Uri.parse("Cabin.sfb"))
        }
        gallery_layout.addView(cabinImageView)

        val houseImageView = ImageView(this)
        houseImageView.setImageResource(R.drawable.house_thumb)
        houseImageView.setOnClickListener {
            addObject(Uri.parse("House.sfb"))
        }
        gallery_layout.addView(houseImageView)

        val iglooImageView = ImageView(this)
        iglooImageView.setImageResource(R.drawable.igloo_thumb)
        iglooImageView.setOnClickListener {
            addObject(Uri.parse("igloo.sfb"))
        }
        gallery_layout.addView(iglooImageView)
    }

    private fun addObject(model: Uri) {
        val frame = arFragment.arSceneView.arFrame
        val point: Point = getScreenCenter()

        val hits: List<HitResult>
        if (frame != null) {
            hits = frame.hitTest(point.x + 0f, point.y + 0f)
            for (hit in hits) {
                val trackable: Trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    modelLoader.loadModel(hit.createAnchor(), model)
                    break
                }
            }
        }
    }

    fun onException(throwable: Throwable) {
        val alertBuilder = AlertDialog.Builder(this)
        alertBuilder.setMessage(throwable.message).setTitle("ModelLoader Throwable")
        val alertDialog = alertBuilder.create()
        alertDialog.show()
        return
    }

    fun addNodeToScene(anchor: Anchor, renderable: ModelRenderable) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(arFragment.transformationSystem)

        node.renderable = renderable
        node.setParent(anchorNode)
        arFragment.arSceneView.scene.addChild(anchorNode)
        node.select()
        // start animation
        startAnimation(node, renderable)
    }

    fun startAnimation(node: TransformableNode, renderable: ModelRenderable?) {
        if (renderable == null || renderable.animationDataCount == 0) {
            return
        }
        for (i in 0 until renderable.animationDataCount) {
            val animationData = renderable.getAnimationData(i)
        }
        val animator = ModelAnimator(renderable.getAnimationData(0), renderable)
        animator.start()
        node.setOnTapListener { _, _ ->
            toggleAnimator(animator)
        }
    }

    fun toggleAnimator(animator: ModelAnimator) {
        if (animator.isPaused) {
            animator.resume()
        } else if (animator.isStarted) {
            animator.pause()
        } else {
            animator.start()
        }
    }

    private fun takePhoto() {
        val filename = generateFilename()
        val view = arFragment.arSceneView
        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult === PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                } catch (e: IOException) {
                    showToast(e.toString())
                    // return@request
                }
                showSnackbar(filename)
            } else {
                showToast("Failed to copy pixels")
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }


    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    // helper functions
    private fun showToast(message: String) {
        Toast.makeText(this, message.toUpperCase(), Toast.LENGTH_SHORT).show()
    }

    private fun showSnackbar(filename: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content), "Photo has been saved", Snackbar.LENGTH_LONG)
        snackbar.setAction("Open in Photos") { _ ->
            val photoFile = File(filename)

            val photoURI = FileProvider.getUriForFile(this, this.packageName + ".provider", photoFile)
            val intent = Intent(Intent.ACTION_VIEW, photoURI)
            intent.setDataAndType(photoURI, "image/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
        snackbar.show()
    }

    private fun generateFilename(): String {
        val uuid = UUID.randomUUID().toString()

        val access = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return "$access+Seneform/$uuid.jpg"

    }
}
