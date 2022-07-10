package com.example.reproductor.db

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.reproductor.Cancion
import com.example.reproductor.R

class AdaptadorCanciones(private val canciones: List<Cancion>, private val idUsu: Int, private val db: AdminSQLiteOpenHelper): RecyclerView.Adapter<AdaptadorCanciones.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_list, null, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cancion = canciones[position]

        val portada = cancion.portada

        if (portada!=null)
            holder.ivPortadaV.setImageBitmap(portada)
        else
            holder.ivPortadaV.setBackgroundResource(R.drawable.default_cover)

        holder.tvTituloV.text = cancion.titulo
        holder.tvArtistaV.text = cancion.artista

        val idCancion = db.getIdCancion(cancion.uri.toString())

        holder.cbSeleccionadaV.isChecked = db.cancionSeleccionada(idUsu, idCancion)

        holder.cbSeleccionadaV.setOnClickListener { e ->
            if ((e as CheckBox).isChecked){
                db.insertarCancionUsu(idUsu, idCancion)
            } else{
                db.borrarCancionUsu(idUsu, idCancion)
            }
        }
    }

    override fun getItemCount(): Int {
        return canciones.size
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val ivPortadaV: ImageView = itemView.findViewById(R.id.ivPortadaV)
        val tvTituloV: TextView = itemView.findViewById(R.id.tvTituloV)
        val tvArtistaV: TextView = itemView.findViewById(R.id.tvArtistaV)
        val cbSeleccionadaV: CheckBox = itemView.findViewById(R.id.cbSeleccionadaV)


    }

}