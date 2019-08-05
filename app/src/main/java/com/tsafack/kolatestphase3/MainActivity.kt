package com.tsafack.kolatestphase3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.vision.text.TextRecognizer
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.tsafack.kolatestphase3.entities.Coordone
import com.tsafack.kolatestphase3.entities.SingleWord
import com.tsafack.kolatestphase3.recycle_view_items.WordItem
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    private val CAMERA_REQUEST_CODE = 0
    private val CAMERAPERMISSION = 1

    val wordLit = arrayListOf<Item>()
    private var shouldInitrecycleView = true
    private lateinit var wordSection: Section

    private val TAILLE_DE_DECOUPAGE_PAR_LIGNE = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        image_grille.setOnClickListener {

            wordLit.clear()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERAPERMISSION
                )
            } else {

                val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (callCameraIntent.resolveActivity(packageManager) != null) {
                    startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
                }
            }
        }

        //createWordList()

        /* // le texte recupérer de l'image
         val wordList = "sqetkbonjoursazpemqventcommentaporvasportvaaumarchezpemqventcommentaporvasportvaaumarche"

         // on le converti en Arrayliste afin d'obtenir des lignes et des colones
         val myList = convertStringToArray(wordList)
         Log.i("MainActivity", "Taille de la liste: "+myList.size)
         myList.forEach {
             Log.i("MainActivity", "element courent : "+it)
         }

         // on parcout la matrice ligne par ligne et on recherche les mots qui existent
         for (i in 0 ..myList.size-1){
             findingWordintext(myList[i], i)
         }*/

        /* val textRec = TextRecognizer.Builder(this).build()
         if (!textRec.isOperational) {
             // Note: The first time that an app using a Vision API is installed on a
             // device, GMS will download a native libraries to the device in order to do detection.
             // Usually this completes before the app is run for the first time.  But if that
             // download has not yet completed, then the above call will not detect any text,
             // barcodes, or faces.
             // isOperational() can be used to check if the required native libraries are currently
             // available.  The detectors will automatically become operational once the library
             // downloads complete on device.
             Log.w("MainActivity", "Detector dependencies are not yet available.");

             // Check for low storage.  If there is low storage, the native library will not be
             // downloaded, so detection will not become operational.
             val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
             val hasLowStorage = registerReceiver(null, lowstorageFilter) != null
             if (hasLowStorage) {
                 Toast.makeText(this, "Low Storage", Toast.LENGTH_LONG).show();
                 Log.w("MainActivity", "Low Storage");
             }
         } else {
             Toast.makeText(this, "tout va bien", Toast.LENGTH_LONG).show();
         }*/
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERAPERMISSION) {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (callCameraIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    image_grille.setImageBitmap(data.extras!!.get("data") as Bitmap)
                    setUpTextRecognition(data)
                }
            }
            else -> {
                Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_SHORT).show()
            }
        }
    }

/*
    private fun createWordList() {
        wordLit.add(WordItem(SingleWord("hellow", "","","", Coordone(2, 1)), this))
        wordLit.add(WordItem(SingleWord("How", "","","",  Coordone(3, 9)), this))
        wordLit.add(WordItem(SingleWord("School", "","","",  Coordone(8, 7)), this))
        wordLit.add(WordItem(SingleWord("hellow", "","","", Coordone(10, 11)), this))
        wordLit.add(WordItem(SingleWord("Casa", "","","", Coordone(5, 1)), this))
        wordLit.add(WordItem(SingleWord("Breakfast", "","","",  Coordone(7, 6)), this))
        updateRecycleViewCategories(wordLit)
    }*/

    private fun updateRecycleViewCategories(listCategorieTransaction: ArrayList<Item>) {
        fun init() {
            id_recycleview_groupe_item.apply {
                layoutManager = LinearLayoutManager(this.context).apply {
                    //orientation = LinearLayoutManager.VERTICAL
                }
                adapter = GroupAdapter<ViewHolder>().apply {
                    wordSection = Section(listCategorieTransaction)
                    add(wordSection)
                    // setOnItemClickListener(onItemClick)
                }
            }
            shouldInitrecycleView = false
        }

        fun updateItems() = wordSection.update(listCategorieTransaction)

        if (shouldInitrecycleView) {
            try {
                init()
            } catch (e: Exception) {
                Log.e("Hommefragent", "Erreur Null: " + e.message)
            }
        } else
            updateItems()
    }

    private fun setUpTextRecognition(data: Intent) {

        val image = FirebaseVisionImage.fromBitmap(data.extras!!.get("data") as Bitmap)
        val detector = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        val progressdialog = indeterminateProgressDialog("détection du text en cours")

        toast("avant...")
        val result = detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                progressdialog.dismiss()
                processingRecognition(firebaseVisionText)
            }
            .addOnFailureListener {
                progressdialog.dismiss()
                toast("error: " + it.message.toString())

            }
    }


    private fun colorationDesmots(firebaseVisionText: FirebaseVisionText?){
        val blocks = firebaseVisionText!!.textBlocks
        if(blocks.size == 0){
            return;
        }


    }

    private fun processingRecognition(firebaseVisionText: FirebaseVisionText?) {
        val resultText = firebaseVisionText!!.text
        var noSpacingText = resultText.replace("\\s".toRegex(), "")
        //var textResult = ""
        Toast.makeText(applicationContext, "Texte brut:$resultText", Toast.LENGTH_LONG).show()
        val mBuilder = AlertDialog.Builder(this)
            .setTitle("Texte Brut")
            .setMessage(noSpacingText)
        val mAlertDialog = mBuilder.show()

        // le texte recupérer de l'image
        val wordList = noSpacingText

        // on le converti en Arrayliste afin d'obtenir des lignes et des colones
        val myList = convertStringToArray(wordList)
        Log.i("MainActivity", "Taille de la liste: " + myList.size)
        myList.forEach {
            Log.i("MainActivity", "element courent : $it")
        }

        // on parcout la matrice ligne par ligne et on recherche les mots qui existent
        for (i in 0..myList.size - 1) {
            findingWordintext(myList[i], i)
        }

        /*for (block in firebaseVisionText!!.textBlocks) {
            val blockText = block.text
            val blockConfidence = block.confidence
            val blockLanguages = block.recognizedLanguages
            val blockCornerPoints = block.cornerPoints
            val blockFrame = block.boundingBox
            //Toast.makeText(applicationContext, "Block de text:$blockText", Toast.LENGTH_LONG).show()
            // textResult.plus(block)

            for (line in block.lines) {
                val lineText = line.text
                val lineConfidence = line.confidence
                val lineLanguages = line.recognizedLanguages
                val lineCornerPoints = line.cornerPoints
                val lineFrame = line.boundingBox
                //Toast.makeText(applicationContext, "ligne  de text:$lineText", Toast.LENGTH_LONG).show()
                for (element in line.elements) {
                    val elementText = element.text
                    val elementConfidence = element.confidence
                    val elementLanguages = element.recognizedLanguages
                    val elementCornerPoints = element.cornerPoints
                    val elementFrame = element.boundingBox
                    //Toast.makeText(applicationContext, "element  de text:" + elementText, Toast.LENGTH_LONG).show()
                }
            }
        }*/
    }

    private fun findingWordintext(noSpacingText: String, numeroLigne: Int) {
        val result = decoupageverticale(noSpacingText, numeroLigne)
        result.forEach {
            println("Chaine: " + it.wordText + "  coordonne" + it.cordonnes.toString())
            Log.i("MainActivity", "Chaine: " + it.wordText + "  coordonne" + it.cordonnes.toString())
            wordLit.add(WordItem(it, this))
        }
        updateRecycleViewCategories(wordLit)
    }


    // cette fonction a pour but de rechercher toutes les combinaisons de mot possible
    // dans une chaine de caractère et les retourne sous forme d'objet au formatn que nous avons
    // définit

    fun decoupageverticale(text: String, numeroLigne: Int): ArrayList<SingleWord> {
        val correctWordList = ArrayList<SingleWord>()
        var curentWord: CharSequence = ""

        for (i in 0..text.length) {
            if (i == 0) {
                curentWord = text.subSequence(0, i + 1)
            } else {
                curentWord = text.subSequence(0, i)
            }

            if (findisWordExiste(curentWord)) {
                val coordonne = Coordone(numeroLigne, 0)
                val singleWord = SingleWord(curentWord as String,"","","", coordonne)
                correctWordList.add(singleWord)
            }

            //var nexSize = text.length -i
            val minJ = i + 1
            for (j in minJ until text.length) {
                curentWord = text.subSequence(minJ, j)

                if (findisWordExiste(curentWord)) {
                    val coordonne = Coordone(numeroLigne, minJ)
                    val singleWord = SingleWord(curentWord as String, "", "","",coordonne)
                    correctWordList.add(singleWord)
                }
            }

        }
        return correctWordList
    }

    fun findisWordExiste(word: CharSequence): Boolean {
        return true
    }

    fun convertStringToArray(text: String): ArrayList<String> {

        var completLine: String = ""
        val arrayListofWord = ArrayList<String>()

        text.forEach {
            completLine += it
            if (completLine.length == TAILLE_DE_DECOUPAGE_PAR_LIGNE) {
                arrayListofWord.add(completLine)
                completLine = ""
            }
        }

        return arrayListofWord
    }

}
