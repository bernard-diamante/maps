package com.bernarddiamante.mymaps

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bernarddiamante.mymaps.models.UserMap

private const val TAG = "MapsAdapter"
class MapsAdapter(
    private val context: Context,
    private val userMaps: List<UserMap>,
    private val recyclerViewInterface: RecyclerViewInterface )
    : RecyclerView.Adapter<MapsAdapter.ViewHolder>() {

    interface RecyclerViewInterface  {
        fun onItemClick(position: Int) {
            Log.i(TAG, "Tapped on position $position")
        }
        fun onItemLongClick(position: Int) {
            Log.i(TAG, "Long click on position: $position")
        }
    }

    // Creates a new view - EXPENSIVE
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_user_map, parent, false)
        return ViewHolder(view)
    }

    // Bind the data at position into the ViewHolder - INEXPENSIVE
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userMap = userMaps[position]
        // Notify MainActivity when a view is clicked
        holder.itemView.setOnClickListener{
            recyclerViewInterface.onItemClick(position)
        }

        holder.itemView.setOnLongClickListener{
            recyclerViewInterface.onItemLongClick(position)
            return@setOnLongClickListener true
        }
        val textViewTitle = holder.itemView.findViewById<TextView>(R.id.tvMapTitle)
        textViewTitle.text = userMap.title
    }

    override fun getItemCount(): Int = userMaps.size


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView)
}
