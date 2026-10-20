package com.walmart.rondaguardias

import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Wrapper WebView liviano: la logica de verdad (camara, formularios,
 * guardado en la base) vive en el servidor FastAPI de RondaGuardias,
 * esta app solo la muestra a pantalla completa con icono propio y se
 * encarga de 2 cosas que un navegador comun no resuelve solo:
 *
 * 1. Pedir el permiso de camara de Android (sin esto, el permiso que
 *    pide la pagina web via getUserMedia queda sin respuesta).
 * 2. Aceptar el certificado HTTPS autofirmado del servidor de la
 *    tienda (es nuestro propio servidor, en la red local, no hay
 *    riesgo real en confiar en el).
 *
 * La direccion del servidor (por defecto "rondaguardias.local", el
 * nombre mDNS que publica el servidor) se guarda en SharedPreferences
 * y se puede cambiar desde el menu, por si en algun local no se puede
 * usar mDNS y hay que escribir la IP a mano como respaldo.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREFS = "rondaguardias_prefs"
        private const val KEY_SERVIDOR = "servidor"
        private const val VALOR_POR_DEFECTO = "rondaguardias.local:8800"
    }

    private lateinit var webView: WebView

    private val pedirPermisoCamara = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> cargarApp() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.webChromeClient = PermisoCamaraWebChromeClient()
        webView.webViewClient = ConfianzaLocalWebViewClient()
        setContentView(webView)

        if (tienePermisoCamara()) {
            cargarApp()
        } else {
            pedirPermisoCamara.launch(android.Manifest.permission.CAMERA)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, 1, Menu.NONE, "Configurar servidor")
        menu.add(Menu.NONE, 2, Menu.NONE, "Recargar")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> mostrarDialogoConfiguracion()
            2 -> cargarApp()
        }
        return true
    }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun obtenerServidorGuardado(): String {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        return prefs.getString(KEY_SERVIDOR, VALOR_POR_DEFECTO) ?: VALOR_POR_DEFECTO
    }

    private fun guardarServidor(valor: String) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SERVIDOR, valor).apply()
    }

    /**
     * El campo ya viene precargado con "rondaguardias.local:8800" (el
     * nombre que publica el servidor solo, via mDNS) para que en el
     * caso normal el guardia no tenga que escribir ni editar nada: solo
     * confirma. Si en algun local el nombre no resuelve, se puede
     * reemplazar por la IP de la PC como respaldo.
     */
    private fun mostrarDialogoConfiguracion() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(obtenerServidorGuardado())
        input.hint = "ej: rondaguardias.local:8800 o 22.121.136.110:8800"

        AlertDialog.Builder(this)
            .setTitle("Servidor de Ronda de Guardias")
            .setMessage("Dejalo como esta salvo que te pidan cambiarlo (ej: si en tu local no funciona el nombre, pedi la IP de la PC).")
            .setView(input)
            .setPositiveButton("Guardar y entrar") { _, _ ->
                val valor = input.text.toString().trim()
                if (valor.isNotEmpty()) {
                    guardarServidor(valor)
                    cargarApp()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(false)
            .show()
    }

    private fun cargarApp() {
        val servidor = obtenerServidorGuardado()
        webView.loadUrl("https://$servidor/escanear")
    }

    /** Sin esto, la pagina pide camara via getUserMedia y Android nunca
     * responde: el video queda esperando para siempre. */
    private inner class PermisoCamaraWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            runOnUiThread {
                if (tienePermisoCamara()) {
                    request.grant(request.resources)
                } else {
                    request.deny()
                }
            }
        }
    }

    /** El certificado del servidor es autofirmado (es nuestro propio
     * servidor, en la red local de la tienda) asi que lo aceptamos
     * directo, sin mostrarle al guardia ningun dialogo tecnico. Si el
     * servidor no responde (IP/nombre mal, PC apagada, etc.) se lo
     * decimos con un mensaje simple en vez de dejar la pantalla en
     * blanco. */
    private inner class ConfianzaLocalWebViewClient : WebViewClient() {
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
            handler.proceed()
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                Toast.makeText(
                    this@MainActivity,
                    "No se pudo conectar al servidor. Revisa que estes en la red Eagle WiFi, o toca el menu > Configurar servidor.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
