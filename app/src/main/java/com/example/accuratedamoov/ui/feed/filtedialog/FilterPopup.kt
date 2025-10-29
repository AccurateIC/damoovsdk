package com.example.accuratedamoov.ui.feed.filtedialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import com.example.accuratedamoov.R

class FilterPopup(
    private val context: Context,
    private val anchorView: View,
    private val title: String,
    private val items: List<String>,
    private val onSelected: (String) -> Unit
) {
    private var popupWindow: PopupWindow? = null

    fun show() {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.filter_dialog, null)
        val listView = view.findViewById<ListView>(R.id.filterListView)


        val adapter = object : ArrayAdapter<String>(context, R.layout.item_filter_option, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_filter_option, parent, false)
                val textView = view.findViewById<TextView>(R.id.itemText)
                textView.text = items[position]
                return view
            }
        }

        listView.adapter = adapter

        // Create popup
        popupWindow = PopupWindow(
            view,
            anchorView.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true // focusable so taps outside dismiss it
        )

        // Appearance
        popupWindow?.apply {
            elevation = 8f
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(context.getDrawable(android.R.color.transparent))
        }

        // Handle item click
        listView.setOnItemClickListener { _, _, position, _ ->
            onSelected(items[position])
            popupWindow?.dismiss()
        }
        listView.divider = null
        listView.dividerHeight = 0
        // Show below the anchor
        popupWindow?.showAsDropDown(anchorView, 0, 8)
    }

    fun dismiss() {
        popupWindow?.dismiss()
    }
}

