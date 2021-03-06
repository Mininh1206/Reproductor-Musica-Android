package com.example.reproductor

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reproductor.db.AdminSQLiteOpenHelper
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private lateinit var db: AdminSQLiteOpenHelper
    private var registrando = false

    lateinit var datePickerDialog: DatePickerDialog

    val REQUEST_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (registrando){

            setContentView(R.layout.activity_crear_cuenta)

            val etFechaNac: EditText = findViewById(R.id.etFechaNac)

            etFechaNac.setOnClickListener{
                val c: Calendar = Calendar.getInstance()
                val mYear: Int = c.get(Calendar.YEAR)

                val mMonth: Int = c.get(Calendar.MONTH)

                val mDay: Int = c.get(Calendar.DAY_OF_MONTH)

                datePickerDialog = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                    etFechaNac.setText(dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)
                }, mYear, mMonth, mDay
                )
                datePickerDialog.show()
            }
        }
        else
            setContentView(R.layout.activity_main)

        db = AdminSQLiteOpenHelper(this, "pmul", null, 3)

        db.insertarUsuario(null, "Root", "Root", "Root", "Otro", "-", imageViewToByteArray(null), "ruta")

        verificarPermisos()

    }

    private fun verificarPermisos(){
        val pRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        val permisos = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

        if (PackageManager.PERMISSION_GRANTED==pRead){
            actualizarCanciones()
        } else{
            requestPermissions(permisos, REQUEST_CODE)
            actualizarCanciones()
        }

    }

    private fun actualizarCanciones(){
        val t = Thread{
            while (true){
                canciones = ArrayList()

                val songLibraryUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE
                )

                val sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC"

                contentResolver.query(songLibraryUri, projection, null, null, sortOrder).use { e ->
                    val idColumn = e!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameColumn = e.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val artistColumn = e.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val titleColumn = e.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

                    val idCancion = 0

                    while (e.moveToNext()) {
                        val id = e.getLong(idColumn)

                        val name = e.getString(nameColumn)
                        val extension = name.substring(name.lastIndexOf(".") + 1)

                        if (extension == "mp3") {

                            val artista = e.getString(artistColumn)
                            val titulo = e.getString(titleColumn)
                            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)


                            val mmr = MediaMetadataRetriever()
                            mmr.setDataSource(this, uri)

                            val bm = try {
                                val artBytes = mmr.embeddedPicture
                                BitmapFactory.decodeByteArray(artBytes, 0, artBytes!!.size)
                            } catch (e: Exception) {
                                val bitmapDrawable = getDrawable(R.drawable.default_cover) as BitmapDrawable
                                bitmapDrawable.bitmap
                            }

                            val cancion = Cancion(idCancion, titulo, artista, uri, bm)

                            if (db.getIdCancion(uri.toString())==-1){
                                db.insertarCancion(cancion)
                            }

                            canciones.add(cancion)

                        }
                    }

                }

                Thread.sleep(1000*60)

            }
        }

        t.isDaemon = true
        t.start()

    }

    private fun getSexo(): String{
        val rH = findViewById<RadioButton>(R.id.rbHombreR)
        val rM = findViewById<RadioButton>(R.id.rbMujerR)
        val rO = findViewById<RadioButton>(R.id.rbOtroR)

        if (rH.isChecked)
            return "Hombre"
        else if (rM.isChecked)
            return "Mujer"
        else if (rO.isChecked)
            return "Otro"
        else
            return ""
    }

    private fun imageViewToByteArray(image: ImageView?): ByteArray {
        try{
            val bitmap = (image!!.drawable as BitmapDrawable).bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        } catch (e: Exception){
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.default_cover)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            return stream.toByteArray()
        }
    }

    fun seleccionarFoto(view: View) {
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = "image/*"
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.type = "image/*"
        val chooserIntent = Intent.createChooser(getIntent, "Selecciona una aplicaci??n")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))
        startActivityForResult(chooserIntent, PICK_IMAGE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val foto = findViewById<ImageView>(R.id.ivFoto)

        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            try {
                val dataIs = data?.data?.let { contentResolver.openInputStream(it) }
                if (dataIs!=null){
                    val bitmap = BitmapFactory.decodeStream(dataIs)
                    foto.setImageBitmap(bitmap)
                    registrando = true
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    fun crearCuenta(view: View){
        val nombre = findViewById<EditText>(R.id.etNombreR).text.toString().trim()
        val usuario = findViewById<EditText>(R.id.etUsuarioR).text.toString().trim()
        val contrase??a = findViewById<EditText>(R.id.etContrase??aR).text.toString().trim()
        val contrase??aC = findViewById<EditText>(R.id.etCContrase??aR).text.toString().trim()
        val sexo = getSexo().trim()
        val fechaNac = findViewById<EditText>(R.id.etFechaNac).text.toString().trim()
        val foto = findViewById<ImageView>(R.id.ivFoto)

        if (!nombre.isEmpty() && !usuario.isEmpty() && !contrase??a.isEmpty() && !sexo.isEmpty() && !fechaNac.isEmpty()) {

            var valido = true

            val cNombre = Regex("[a-z???????????????????? ]{10,}", RegexOption.IGNORE_CASE)
            if (!cNombre.matches(nombre)){
                valido = false
                Toast.makeText(this, "Nombre: Introduce al menos 10 letras", Toast.LENGTH_LONG).show()
            }

            val cUsuario = Regex("[A-Z][A-Za-z0-9]{7,}")
            if (!cUsuario.matches(usuario)){
                valido = false
                Toast.makeText(this, "Usuario: Tiene que comenzar por una letra e introduce al menos 8 numeros o letras", Toast.LENGTH_LONG).show()
            }

            val cContrase??a = Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&\\-_+]{8,}$")
            if (!cContrase??a.matches(contrase??a)){
                valido = false
                Toast.makeText(this, "Contrase??a: Introduce al menos 8 numeros, letras o car??cteres especiales, m??nimo 1 may??scula", Toast.LENGTH_LONG).show()
            } else if (contrase??a!=contrase??aC){
                valido = false
                Toast.makeText(this, "Contrase??a: No coinciden", Toast.LENGTH_LONG).show()
            }

            if (valido)
                if (db.insertarUsuario(this, nombre, usuario, contrase??a, sexo, fechaNac, imageViewToByteArray(foto), "ruta"))
                    setContentView(R.layout.activity_main)
        } else
            Toast.makeText(this, "Rellena todos los datos", Toast.LENGTH_SHORT).show()
    }

    fun iniciarSesion(view: View){
        val usuario = findViewById<EditText>(R.id.etUsuario).text.toString()
        val contrase??a = findViewById<EditText>(R.id.etContrase??a).text.toString()

        val cUsu = db.getUsuario(usuario, contrase??a)

        if (!usuario.isEmpty() && !contrase??a.isEmpty()){
            if (cUsu.moveToFirst()){
                val id = cUsu.getInt(0)

                val intentMusica = Intent(this, MusicaActivity::class.java)
                intentMusica.putExtra("idUsu", id)

                startActivity(intentMusica)

                super.finish()

            }else
                Toast.makeText(this, "Usuario y/o contrase??a incorrectos", Toast.LENGTH_SHORT).show()
        }else
            Toast.makeText(this, "Rellena los datos", Toast.LENGTH_SHORT).show()
    }

    fun intentCrearCuenta(view: View){
        setContentView(R.layout.activity_crear_cuenta)

        val etFechaNac: EditText = findViewById(R.id.etFechaNac)

        etFechaNac.setOnClickListener{
            val c: Calendar = Calendar.getInstance()
            val mYear: Int = c.get(Calendar.YEAR)

            val mMonth: Int = c.get(Calendar.MONTH)

            val mDay: Int = c.get(Calendar.DAY_OF_MONTH)

            datePickerDialog = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->
                etFechaNac.setText(dayOfMonth.toString() + "/" + (monthOfYear + 1) + "/" + year)
            }, mYear, mMonth, mDay
            )
            datePickerDialog.show()
        }
    }

    fun intentIniciarSesion(view: View){
        setContentView(R.layout.activity_main)
    }

    companion object {
        var canciones = ArrayList<Cancion>()
        const val PICK_IMAGE = 1
    }
}