package com.tsafack.kolatestphase3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.tsafack.kolatestphase3.entities.Coordone
import com.tsafack.kolatestphase3.entities.SingleWord
import com.tsafack.kolatestphase3.recycle_view_items.WordItem
import com.tsafack.kolatestphase3.untils.MlKitUntil
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Section
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.activity_reading_file.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ReadingFileActivity : AppCompatActivity() {

    var myWordDictionary = listOf<String>()
    private val CAMERA_REQUEST_CODE = 0
    private val CAMERAPERMISSION = 1
    private val wordLit = arrayListOf<Item>()

    private var shouldInitrecycleView = true
    private lateinit var wordSection: Section

    private val TAILLE_DE_DECOUPAGE_PAR_LIGNE = 20

    private final val TAG = "ReadingFileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading_file)

        readDictionary()
        image_grille2.setOnClickListener {

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

        laodDefinition(createUrl("configuration"), onComplete = {
            Log.i("WordItem", it)
        })
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
                    image_grille2.setImageBitmap(data.extras!!.get("data") as Bitmap)
                    MlKitUntil.getTextFromImage(data, this, onComplete = {
                        processingText(it)
                        //findWordInImage(it!!)
                    })
                }
            }
            else -> {
                Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_SHORT).show()
            }
        }
    }


/*    private fun findWordInImage(firebaseVisionText: FirebaseVisionText) {
        val blocks = firebaseVisionText.textBlocks
        if(blocks.size == 0){
            Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show()
            return;
        }

        graphicalOverlay.clear()
        for(i in  0..blocks.size){
            val lines = blocks.get(i).lines
            for(j in  0..lines.size){
                val elements = lines.get(j).elements
                for(k in 0..elements.size){
                    val textGraphic= TextGraphic(graphicalOverlay, elements[k])
                    graphicalOverlay!!.add(textGraphic)
                }
            }
        }
    }*/

    private fun processingText(text: FirebaseVisionText?) {

        val fulltext = text!!.text

        var noSpacingText = fulltext!!.replace("\\s".toRegex(), "")
        /* val mBuilder = AlertDialog.Builder(this)
             .setTitle("Texte Brut")
             .setMessage(noSpacingText)
         val mAlertDialog = mBuilder.show()

 */
        val progressdialog = indeterminateProgressDialog("text processing...")
        val horizontalList = convertStringToHorizontalArray(noSpacingText)
        val verticallList: ArrayList<String> = convertTovertical(horizontalList)

        /* maintenant on parcout la liste et on vérifi si elle contient
         * un mot du dictionaire, si oui, on l'ajoute à la liste */

        // possibilité 1: avec recupération des coordonnées complexite censiblement = log(n^2)

        // on Recherche les mots par ligne
        horizontalList.forEachIndexed { index, stringLigne ->
            myWordDictionary.forEach {
                if (stringLigne.contains(it.trim(), true)
                    && it.trim().length > 1
                ) {
                    val ordonne = stringLigne.indexOf(it, 0, true)
                    val coordonne = Coordone(index, ordonne)
                    val singleWord = SingleWord(it.trim(), "", "", "", coordonne)
                    wordLit.add(WordItem(singleWord, this))
                }
            }
        }

        // on Recherche les mots par colones
        verticallList.forEachIndexed { index, stringLigne ->
            myWordDictionary.forEach {
                if (stringLigne.contains(it.trim(), true)
                    && it.trim().length > 1
                ) {
                    // TODO recuperation de la postion du mot dans la ligne
                    val ordonne = stringLigne.indexOf(it, 0, true)

                    /* Puisque nous faisons un parcourt vertical, le repère change
                    * et par consequent, le point de cordonnées (X, Y) deviendra (-Y, X)
                    * exemple le point (0, 3) va devenir (-3, 0)*/

                    val coordonne = Coordone(-ordonne, index)
                    val singleWord = SingleWord(it.trim(), "", "", "", coordonne)
                    wordLit.add(WordItem(singleWord, this))
                }
            }
        }

        // possibilité 2: avec recupération des coordonnées complexite censiblement = n
        // TODO ce code ne marche pas, il est à reviser
/*        myWordDictionary.forEach {
            if(noSpacingText.contains(it.trim())
                && it.trim().length >1){
                val coordonne = Coordone(0, 0)
                val singleWord = SingleWord(it.trim(),"","","", coordonne)
                wordLit.add(WordItem(singleWord, this))
            }
        }*/
        progressdialog.dismiss()
        updateRecycleViewCategories(wordLit)
    }


    /** cette fonction a pour but de convertir le texte brut recupéré *
     * dans l'image en text liste de chaine de carractères ayant une taille bien défini**/
    fun convertStringToHorizontalArray(text: String): ArrayList<String> {
        var completLine: String = ""
        val arrayListofWord = ArrayList<String>()

        text.forEach {
            completLine += it
            if (completLine.length == TAILLE_DE_DECOUPAGE_PAR_LIGNE) {
                arrayListofWord.add(completLine)
                Log.i(TAG, "curent ligne: $completLine")
                completLine = ""
            }
        }
        return arrayListofWord
    }

    /** Cette fonction a pour but de convertir une liste
     * horizontale en liste verticale**/
    private fun convertTovertical(horizontalListofStringLine: ArrayList<String>): ArrayList<String> {
        val verticalStringList = ArrayList<String>()
        var tmpStringColone = ""
        // var indexCount = 0

        for (i in 0 until TAILLE_DE_DECOUPAGE_PAR_LIGNE) {
            horizontalListofStringLine.forEachIndexed { index, s ->
                tmpStringColone += s[i].toString()
            }
            verticalStringList.add(tmpStringColone)
            Log.i(TAG, "curent Colone: $tmpStringColone")
            tmpStringColone = ""
        }

        return verticalStringList
    }


    /** cette méthode a pour but de lire les mot du fichier dictionary
     * afin de creer le dictionaire qui va nous servire dans le code **/
    private fun readDictionary() {
        var abuffer = StringBuffer()
        val iS = resources.openRawResource(R.raw.dictionary)
        val aReader = BufferedReader(InputStreamReader(iS))

        myWordDictionary = aReader.readLines()
        iS.close()
        myWordDictionary.forEach {
            abuffer.append(it.trim() + "\n")
            Log.i(TAG, it)
        }
        //id_text_word.text = abuffer
    }

    private fun updateRecycleViewCategories(wordlist: ArrayList<Item>) {
        fun init() {
            id_recycleview_groupe_item.apply {
                layoutManager = LinearLayoutManager(this.context).apply {
                    //orientation = LinearLayoutManager.VERTICAL
                }
                adapter = GroupAdapter<ViewHolder>().apply {
                    wordSection = Section(wordLit)
                    add(wordSection)
                    // setOnItemClickListener(onItemClick)
                }
            }
            shouldInitrecycleView = false
        }

        fun updateItems() = wordSection.update(wordLit)

        if (shouldInitrecycleView) {
            try {
                init()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur Null: " + e.message)
            }
        } else
            updateItems()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val mySreachViewItem = menu!!.findItem(R.id.id_searchView)
        val mySearchView = mySreachViewItem.actionView as SearchView
        mySearchView.isSubmitButtonEnabled = true
        mySearchView.queryHint = "Rechercher..."
        mySearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(searchingText: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(searchingText: String?): Boolean {
                if (searchingText != null && searchingText.isNotEmpty()) {
                    val newWordList = arrayListOf<Item>()
                    wordLit.forEachIndexed { index, item ->
                       val newItem: WordItem = item as WordItem
                        if (newItem.wordEntitie.wordText.toLowerCase().contains(searchingText.toLowerCase())) {
                            newWordList.add(item)
                        }
                    }

                    updateRecycleViewCategories(newWordList)
                } else {
                    updateRecycleViewCategories(wordLit)
                }
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    private fun createUrl(word: String = "Ace"): String {
        val language = "en-gb"
        val word = word
        val fields = "definitions"
        val strictMatch = "false"
        val word_id = word.toLowerCase()
        return "https://od-api.oxforddictionaries.com:443/api/v2/entries/$language/$word_id?fields=$fields&strictMatch=$strictMatch"
    }


    private fun laodDefinition(url: String, onComplete: (text: String?) -> Unit) {

        val app_id = "94d845a3"
        val app_key = "f4c42104a90879079b5da1ba37e3d6cf"

        //creating volley string request
        val stringRequest = object : StringRequest(Method.GET, url,
            Response.Listener<String> { response ->
                try {
                    Toast.makeText(applicationContext, response.toString(), Toast.LENGTH_LONG).show()
                    Log.d(TAG, "RESULTAT: $response")
                    val obj = JSONObject(response)
                    Log.d(TAG, "All jsonOb: $obj")
                    val jsResulat = obj.getJSONArray("results")
                    Log.d(TAG, "result JSON1: $jsResulat")
                    val lexicalEntries = jsResulat.getJSONObject(0).getJSONArray("lexicalEntries")
                    Log.d(TAG, "lexicalEntries: $lexicalEntries")
                    val entries = lexicalEntries.getJSONObject(0).getJSONArray("entries")
                    Log.d(TAG, "lexicalEntries: $entries")
                    val senses = entries.getJSONObject(0).getJSONArray("senses")
                    Log.d(TAG, "lexicalEntries: $senses")
                    val premiereDefinition = senses.getJSONObject(0).getJSONArray("definitions").getString(0)
                    Log.d(TAG, "definition: $premiereDefinition")

                    onComplete(premiereDefinition)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { volleyError ->
                Toast.makeText(
                    applicationContext,
                    volleyError.message,
                    Toast.LENGTH_LONG
                ).show()
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["app_id"] = app_id
                headers["app_key"] = app_key
                return headers
            }
        }

        //adding request to queue
        VolleySingleton.instance?.addToRequestQueue(stringRequest)
    }

}
