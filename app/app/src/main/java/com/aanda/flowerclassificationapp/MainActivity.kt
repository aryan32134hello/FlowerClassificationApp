package com.aanda.flowerclassificationapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import org.tensorflow.lite.Interpreter
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import java.io.FileInputStream
import java.io.IOException
import java.lang.Byte
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MainActivity : AppCompatActivity() {
    var iv: ImageView? = null
    var choose_btn: Button? = null
    var predict: Button? = null
    var tv_classify: TextView? = null
    var tv_accuracy: TextView? = null
    var tflite:Interpreter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        iv= findViewById(R.id.imageView)
        choose_btn= findViewById(R.id.image_choose)
        predict= findViewById(R.id.predict)
        tv_classify= findViewById(R.id.classification)
        tv_accuracy= findViewById(R.id.accuracy)
        try {
            tflite = Interpreter(loadModelFile()!!)
        }
        catch (ex: Exception) {
            ex.printStackTrace()
        }
        choose_btn?.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                val iGallery: Intent = Intent(Intent.ACTION_PICK)
                iGallery.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                getResult.launch(iGallery)
            }
        })
        predict?.setOnClickListener {
            if (iv!!.drawable == null) {
                Toast.makeText(this, "Choose Image First!", Toast.LENGTH_SHORT).show()
            } else {
                val drawable: Drawable = iv!!.drawable
                val input_image: Bitmap = getBitmapFromDrawable(drawable)
                val bitmap = Bitmap.createScaledBitmap(input_image, 150, 150, true)
                val input =
                    ByteBuffer.allocateDirect(150 * 150 * 3 * 4).order(ByteOrder.nativeOrder())
                for (y in 0 until 150) {
                    for (x in 0 until 150) {
                        val px = bitmap.getPixel(x, y)
                        val r = Color.red(px)
                        val g = Color.green(px)
                        val b = Color.blue(px)

                        input.putFloat(r.toFloat())
                        input.putFloat(g.toFloat())
                        input.putFloat(b.toFloat())
                    }
                }
                val bufferSize = 5 *( java.lang.Float.SIZE / Byte.SIZE)
                val modelOutput =
                    ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
                tflite?.run(input, modelOutput)
                modelOutput.rewind()
                val probablity = modelOutput.asFloatBuffer()
//                Log.i("Value","${probablity[0]}")
                classify(probablity)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer? {
        val fileDescriptor = this.assets.openFd("model_lite.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declareLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declareLength)
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    private val getResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                iv!!.setImageURI(it.data?.data)
            }
        }
    @SuppressLint("SetTextI18n")
    private fun classify(probablity: FloatBuffer){
        var index = 0
        var max = 0f
        var i = 0
        while (i<5){
            if(probablity[i]>max){
                max = probablity[i]
                index = i
            }
            i++
        }
        val labels = arrayOf("Daisy","Dandelion","Rose","Sunflower","Tulip")
        tv_classify!!.text = "Predicted Flower : ${labels[index]}"
        tv_accuracy!!.text = "Accuracy is : ${max*100}"
    }
}