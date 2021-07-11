package com.example.opengles20.mirrar

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import com.example.opengles20.R
import java.util.*

class RtbpStyleAdapter(val context: Context, val itemClick: (position: Int) -> Unit): RecyclerView.Adapter<RtbpStyleAdapter.ViewHolder>() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var mSelectedPosition: Int = 0
    private val TAG: String? = "RtbpStyleAdapter"
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var beardDataItems = mutableListOf<BeardsItem?>()
    private var STYLE_IMAGE_PREFIX = "vs_rtbp_icon_"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.vs_rtbp_style_row_carousel_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val style = beardDataItems[position]
        var styleId = style?.id
        styleId = styleId?.replace(" ", "")?.toLowerCase(Locale.getDefault())
        Log.d(TAG, "styleName: vs_rtbp_icon_$styleId")
        try {
            holder.imageView.setImageDrawable(VitaSkinInfraUtil.getDrawableByName(context, "${STYLE_IMAGE_PREFIX}${styleId}"))
        } catch (e: Exception) {
            holder.imageView.setImageResource(R.drawable.vs_rtbp_icon_fullbeard)
        }

        holder.itemView.setOnClickListener {
            itemClick(position)
        }
    }

    override fun getItemCount(): Int {
        return beardDataItems.size
    }

    fun updateStyleData(styleList: List<BeardsItem?>) {
        beardDataItems.clear()
        beardDataItems.addAll(styleList.toList())
        notifyDataSetChanged()
    }

    fun onItemSelected(adapterPosition: Int) {
        mSelectedPosition = adapterPosition
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.style_model_imageview)
    }

}