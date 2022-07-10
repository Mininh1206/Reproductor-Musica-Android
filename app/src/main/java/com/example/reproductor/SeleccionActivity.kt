package com.example.reproductor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reproductor.db.AdaptadorCanciones
import com.example.reproductor.db.AdminSQLiteOpenHelper
import java.util.ArrayList

class SeleccionActivity : AppCompatActivity() {

    lateinit var db: AdminSQLiteOpenHelper

    private lateinit var canciones: List<Cancion>
    private lateinit var cancionesUsu: ArrayList<Cancion>
    lateinit var listaCanciones: RecyclerView

    var idUsu: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion)

        listaCanciones = findViewById(R.id.rvCanciones)
        listaCanciones.layoutManager = LinearLayoutManager(this)
        listaCanciones.setHasFixedSize(true)

        db = AdminSQLiteOpenHelper(this, "pmul", null, 3)

        idUsu = intent.extras!!.getInt("idUsu")

        canciones = MainActivity.canciones

        canciones = canciones.sortedBy { it.titulo }

        cancionesUsu = db.sacarCancionesPorUsuario(canciones, idUsu)

        if (canciones.isNotEmpty()){
            listaCanciones.adapter = AdaptadorCanciones(canciones, idUsu, db)
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intentMusica = Intent(this, MusicaActivity::class.java)
        intentMusica.putExtra("idUsu", idUsu)

        startActivity(intentMusica)

        super.finish()
    }
}