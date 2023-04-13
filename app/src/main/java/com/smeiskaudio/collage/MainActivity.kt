package com.smeiskaudio.collage

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.*
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

    private lateinit var imageButton1: ImageButton
    private lateinit var uploadImageFab: FloatingActionButton
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var mainView: View

    private var newPhotoPath: String? = null
    private var visibleImagePath: String? = null
    private var imageFileName: String? = null
    private var photoUri: Uri? = null

    private val NEW_PHOTO_PATH_KEY = "com.smeiskaudio.collage.NEW_PHOTO_PATH_KEY"
    private val VISIBLE_IMAGE_PATH_KEY = "com.smeiskaudio.collage.VISIBLE_IMAGE_PATH_KEY"

    private val cameraActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result -> handleImage(result)
        }

    // Firebase storage
    private val storage = Firebase.storage


    private fun handleImage(result: ActivityResult) {
        when (result.resultCode) {
            RESULT_OK -> {
                Log.d(TAG, "Result ok, user took picture, image at $newPhotoPath")
                visibleImagePath = newPhotoPath
            }
            RESULT_CANCELED -> {
                Log.d(TAG, "Result canceled, no picture taken")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate $newPhotoPath")

        newPhotoPath = savedInstanceState?.getString(NEW_PHOTO_PATH_KEY)
        visibleImagePath = savedInstanceState?.getString(VISIBLE_IMAGE_PATH_KEY)

        mainView = findViewById(R.id.content)

        uploadProgressBar = findViewById(R.id.uploadProgressBar)

        uploadImageFab = findViewById(R.id.upload_image_button)
        uploadImageFab.setOnClickListener {
            uploadImage()
        }

        imageButton1 = findViewById(R.id.imageButton1)
        imageButton1.setOnClickListener {
            takePicture()
        }
    }

    private fun uploadImage() {
        if (photoUri != null && imageFileName != null) {
            val imageStorageRootReference = storage.reference
            val imageCollectionReference = imageStorageRootReference.child("images")
            val imageFileReference = imageCollectionReference.child(imageFileName!!)
            imageFileReference.putFile(photoUri!!)
                .addOnCompleteListener {
                    Snackbar.make(mainView, "Image uploaded",Snackbar.LENGTH_LONG).show()
            }
                .addOnFailureListener { error ->
                    Snackbar.make(mainView,"Error uploading image", Snackbar.LENGTH_LONG).show()
                    Log.e(TAG, "Error uploading image -- $error")
                }
        } else {
            Snackbar.make(
                mainView,
                "Take a picture first",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(NEW_PHOTO_PATH_KEY, newPhotoPath)
        outState.putString(VISIBLE_IMAGE_PATH_KEY, visibleImagePath)
    }

    private fun takePicture() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val (photoFile, photoFilePath) = createImageFile()
        if (photoFile != null) {
            newPhotoPath = photoFilePath // send to global variables instead of variables inside the function.
            photoUri = FileProvider.getUriForFile( // reference work to tell camera app where to save file
                this,
                "com.smeiskaudio.collage.fileprovider",
                photoFile
            )
            Log.d(TAG, "$photoUri\n$photoFilePath")
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraActivityLauncher.launch(takePictureIntent)
        }
    }

    private fun createImageFile(): Pair<File?, String?> {
        return try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            imageFileName = "COLLAGE_$dateTime" // non-null asserted here, because a value happens.
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File.createTempFile(imageFileName!!, ".jpg", storageDir)
            val filePath = file.absolutePath
            file to filePath
        } catch (ex: IOException) {
            null to null
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        Log.d(TAG, "on window focus changed $hasFocus visible image at $visibleImagePath")
        if (hasFocus) {
            visibleImagePath?.let { imagePath ->
                loadImage(imageButton1, imagePath) }
        }
    }

    private fun loadImage(imageButton: ImageButton, imagePath: String) {
        // could code this ourselves, but lets use libraries available to us instead.
        Picasso.get()
            .load(File(imagePath))// need to convert to File for Picasso to .load.
            .error(android.R.drawable.stat_notify_error)
            .fit()
            .centerCrop()
            .into(imageButton, object: Callback {
                override fun onSuccess() {
                    Log.d(TAG, "Loaded image $imagePath")
                }

                override fun onError(e: Exception?) {
                    Log.e(TAG, "Error loading image $imagePath", e)
                }
            })
    }
}