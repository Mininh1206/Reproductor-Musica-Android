package com.example.reproductor

import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.example.reproductor.db.AdminSQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*


class MusicaActivity : AppCompatActivity() {

    lateinit var usuarioC: Cursor
    lateinit var db: AdminSQLiteOpenHelper
    lateinit var mp: MediaPlayer
    lateinit var canciones: ArrayList<Cancion>

    lateinit var ivPortada: ImageView
    lateinit var tvTituloCancion: TextView
    lateinit var tvArtistaCancion: TextView
    lateinit var tvTiempoT: TextView

    lateinit var btPausar: Button
    lateinit var btAnterior: Button
    lateinit var btSiguiente: Button
    lateinit var btAleatorio: Button
    lateinit var btPrimera: Button
    lateinit var btUltima: Button
    lateinit var btBucle: Button
    lateinit var sbTiempo: SeekBar

    val timer = Timer()

    var pista = 0
    var bucle = 0
    var aleatorio = false
    var pausado = true

    var idUsu: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_musica)

        btPausar = findViewById(R.id.btPausar)
        btAnterior = findViewById(R.id.btAnterior)
        btSiguiente = findViewById(R.id.btSiguiente)
        btAleatorio = findViewById(R.id.btAleatorio)
        btPrimera = findViewById(R.id.btPrimera)
        btUltima = findViewById(R.id.btUltima)
        btBucle = findViewById(R.id.btBucle)
        sbTiempo = findViewById(R.id.sbTiempo)

        ivPortada = findViewById(R.id.ivPortada)
        tvTituloCancion = findViewById(R.id.tvTituloCancion)
        tvArtistaCancion = findViewById(R.id.tvArtistaCancion)
        tvTiempoT = findViewById(R.id.tvTiempoT)

        mp = MediaPlayer()

        db = AdminSQLiteOpenHelper(this, "pmul", null, 3)

        idUsu = intent.extras!!.getInt("idUsu")

        usuarioC = db.getUsuario(idUsu)
        usuarioC.moveToFirst()

        canciones = db.sacarCancionesPorUsuario(MainActivity.canciones, idUsu)

        if (canciones.isNotEmpty()){
            val c = canciones[pista]
            mp = MediaPlayer.create(applicationContext, c.uri)

            actualizarDatos()
        }

        timer.scheduleAtFixedRate(object : TimerTask() {
            var tvTiempo: TextView = findViewById(R.id.tvTiempo)

            override fun run() = try{
                sbTiempo.max = mp.duration
                sbTiempo.progress = mp.currentPosition

                runOnUiThread(object : TimerTask() {
                    override fun run() {
                        tvTiempo.text = formatearTiempo(mp.currentPosition)
                    }
                })

            } catch (e:IllegalStateException){

            }
        }, 0, 1000)

        sbTiempo.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (canciones.isNotEmpty() && fromUser){
                    mp.seekTo(progress)

                    if (!mp.isPlaying){
                        mp.start()

                        pausado = false
                        btPausar.setBackgroundResource(R.drawable.pausa)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

    }

    fun formatearTiempo(miliSegundos: Int): String {
        val date = Date(miliSegundos.toLong())
        val formatter = SimpleDateFormat("m:ss")

        return formatter.format(date)
    }

    override fun onDestroy() {
        super.onDestroy()

        mp.release()
    }

    private fun sacarPortada(uri: Uri): Bitmap{
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(this, uri)

        val bm = try {
            val artBytes = mmr.embeddedPicture
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes!!.size)
        } catch (e: Exception) {
            val bitmapDrawable = getDrawable(R.drawable.default_cover) as BitmapDrawable
            bitmapDrawable.bitmap
        }

        return bm
    }

    private fun actualizarDatos() {
        tvTituloCancion.text = canciones[pista].titulo
        tvArtistaCancion.text = canciones[pista].artista
        ivPortada.setImageBitmap(sacarPortada(canciones[pista].uri!!))

        tvTiempoT.text = formatearTiempo(mp.duration)

        mp.setOnCompletionListener {
            if (bucle==1)
                siguiente(btSiguiente)
            else{
                pausado = true
                btPausar.setBackgroundResource(R.drawable.play)
            }
        }

        if (pausado){
            btPausar.setBackgroundResource(R.drawable.play)
        }
        else{
            btPausar.setBackgroundResource(R.drawable.pausa)
        }
    }

    fun siguiente(view: View){
        if (canciones.isNotEmpty()){
            pista++

            if (pista==canciones.size)
                pista = 0

            cambiarCancion()
        }
    }

    fun anterior(view: View){
        if (canciones.isNotEmpty()){
            if (mp.currentPosition<2000){
                pista--

                if (pista<0)
                    pista = canciones.size-1

                cambiarCancion()
            } else{
                mp.seekTo(0)

                if (!mp.isPlaying){
                    mp.start()

                    pausado = false
                    btPausar.setBackgroundResource(R.drawable.pausa)
                }
            }
        }
    }

    fun ultima(view: View){
        if (canciones.isNotEmpty()){
            pista = canciones.size-1

            cambiarCancion()
        }
    }

    fun primera(view: View){
        if (canciones.isNotEmpty()){
            pista = 0

            cambiarCancion()
        }
    }

    private fun cambiarCancion() {
        mp.stop()

        val c = canciones[pista]
        mp = MediaPlayer.create(applicationContext, c.uri)
        mp.start()

        pausado = false
        btPausar.setBackgroundResource(R.drawable.pausa)

        actualizarDatos()
    }

    fun pausarContinuar(view: View){
        if (canciones.isNotEmpty()){
            pausado = !pausado

            if (pausado){
                mp.pause()
                btPausar.setBackgroundResource(R.drawable.play)
            }
            else{
                mp.start()
                btPausar.setBackgroundResource(R.drawable.pausa)
            }

        }

    }

    fun continuoBucle(view: View){
        if (bucle<2)
            bucle++
        else
            bucle = 0

        when (bucle){
            0->{
                btBucle.setBackgroundResource(R.drawable.bucle)
                mp.isLooping = false
            }

            1->{
                btBucle.setBackgroundResource(R.drawable.repetirlista)
            }

            2->{
                btBucle.setBackgroundResource(R.drawable.repetircancion)
                mp.isLooping = true
            }
        }
    }

    fun aleatorio(view: View){
        aleatorio = !aleatorio

        if (aleatorio){
            btAleatorio.setBackgroundResource(R.drawable.aleatorioactivado)
            canciones.shuffle()
        }
        else{
            btAleatorio.setBackgroundResource(R.drawable.aleatorio)
            canciones = db.sacarCancionesPorUsuario(MainActivity.canciones, idUsu)
        }
    }

    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap{
        val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        return bitmap
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        val menuNombre = menu[0]
        menuNombre.title = usuarioC.getString(2)
        val menuFoto = menu[1]
        val foto = byteArrayToBitmap(usuarioC.getBlob(6))
        val drawable = BitmapDrawable(resources, foto)
        menuFoto.setIcon(drawable)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btMusica -> {
                mp.release()

                val intentSeleccion = Intent(this, SeleccionActivity::class.java)
                intentSeleccion.putExtra("idUsu", idUsu)

                startActivity(intentSeleccion)

                super.finish()

            }

            R.id.btVideo -> {
                mp.release()

                val intentVideo = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                startActivityForResult(intentVideo, VIDEO_RECORD_CODE)
            }

            R.id.btCerrarS -> {
                AlertDialog.Builder(this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                    .setPositiveButton("Sí") { dialog, which ->
                        val intentMain = Intent(this, MainActivity::class.java)
                        startActivity(intentMain)
                        super.finish()
                    }
                    .setNegativeButton("No") { dialog, which -> }
                    .show()

            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val VIDEO_RECORD_CODE = 101
    }

}