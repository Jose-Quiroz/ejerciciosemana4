package com.example.ejerciciosemana4

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Keep
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

// Modelo de datos para mapear Firestore
@Keep
@IgnoreExtraProperties
data class Post(
    val texto: String = "",
    val fecha: Timestamp? = null
)

class MainActivity : ComponentActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    // Estado reactivo de Compose
    private val listaPosts = mutableStateListOf<Post>()
    private var ultimoDocumento: DocumentSnapshot? = null
    private var hayMasPosts by mutableStateOf(true)
    private var cargando by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Carga inicial
        cargarNuevosPosts()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FeedScreen(
                        posts = listaPosts,
                        cargando = cargando,
                        hayMas = hayMasPosts,
                        onCargarMasClick = { cargarMasPosts() }
                    )
                }
            }
        }
    }

    // Trae los primeros 5 posts descendentes por fecha
    private fun cargarNuevosPosts() {
        cargando = true
        db.collection("posts")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                cargando = false
                if (!snapshot.isEmpty) {
                    ultimoDocumento = snapshot.documents.last()
                    listaPosts.clear()
                    for (doc in snapshot.documents) {
                        try {
                            doc.toObject(Post::class.java)?.let { listaPosts.add(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    hayMasPosts = false
                }
            }
            .addOnFailureListener { e ->
                cargando = false
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Paginación: trae los siguientes 5 empezando después del cursor
    private fun cargarMasPosts() {
        val cursor = ultimoDocumento ?: return
        cargando = true

        db.collection("posts")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .startAfter(cursor)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                cargando = false
                if (!snapshot.isEmpty) {
                    ultimoDocumento = snapshot.documents.last()
                    for (doc in snapshot.documents) {
                        try {
                            doc.toObject(Post::class.java)?.let { listaPosts.add(it) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    hayMasPosts = false
                    Toast.makeText(this, "No hay más posts", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                cargando = false
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}

@Composable
fun FeedScreen(
    posts: List<Post>,
    cargando: Boolean,
    hayMas: Boolean,
    onCargarMasClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Feed de Posts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(posts) { post ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = post.texto,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val fechaTexto = post.fecha?.toDate()?.let {
                            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(it)
                        } ?: "Sin fecha"

                        Text(
                            text = fechaTexto,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCargarMasClick,
            enabled = !cargando && hayMas,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (hayMas) "Cargar más" else "No hay más publicaciones")
            }
        }
    }
}
