package com.keywordalert.wa

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.keywordalert.wa.databinding.ItemMatchBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchAdapter(private var items: List<MatchItem>) :
    RecyclerView.Adapter<MatchAdapter.VH>() {

    private val fmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    class VH(val binding: ItemMatchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMatchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvKeywords.text = item.keywords
        holder.binding.tvText.text = item.text
        holder.binding.tvApp.text = item.app
        holder.binding.tvMeta.text = fmt.format(Date(item.time))
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<MatchItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
