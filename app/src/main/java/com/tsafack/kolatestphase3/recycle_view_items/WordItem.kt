package com.tsafack.kolatestphase3.recycle_view_items

import android.content.Context
import com.tsafack.kolatestphase3.R
import com.tsafack.kolatestphase3.entities.SingleWord
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.row_item_transaction.*

class WordItem(val wordEntitie:SingleWord,
               val context:Context): Item() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.apply {
            id_text_view_word.text = wordEntitie.wordText
            var absc = "X ${wordEntitie.cordonnes.abscisse}"
            var ordone = "Y ${wordEntitie.cordonnes.ordonne} + ${wordEntitie.wordText.length}"
            id_tv_abscisse.text = absc
            id_tv_ordonne.text = ordone
        }
    }

    override fun getLayout() = R.layout.row_item_transaction
}