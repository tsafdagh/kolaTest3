package com.tsafack.kolatestphase3.recycle_view_items

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.tsafack.kolatestphase3.R
import com.tsafack.kolatestphase3.VolleySingleton
import com.tsafack.kolatestphase3.entities.SingleWord
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.row_item_transaction.*
import org.json.JSONException
import org.json.JSONObject

class WordItem(
    val wordEntitie: SingleWord,
    val context: Context
) : Item() {

    val TAG = "wordItem"
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.apply {
            id_text_view_word.text = wordEntitie.wordText
            var absc = "X ${wordEntitie.cordonnes.abscisse}"
            var ordone = "Y ${wordEntitie.cordonnes.ordonne} + ${wordEntitie.wordText.length}"
            id_tv_abscisse.text = absc
            id_tv_ordonne.text = ordone

            // on recupère la définition du mot
            laodDefinition(createUrl(wordEntitie.wordText), onComplete = {
                id_text_definition.text = "definition: $it"
                Log.i("WordItem", it)
            })
        }
    }

    override fun getLayout() = R.layout.row_item_transaction



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
                    //Toast.makeText(context, response.toString(), Toast.LENGTH_LONG).show()
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
                    context,
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