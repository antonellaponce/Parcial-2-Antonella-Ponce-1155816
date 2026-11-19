package ar.jornadas.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.jornadas.data.Charla

@Composable
fun Catalogo(model: ProgramaModel, abrir: (String) -> Unit) {
    val dia by model.dia.collectAsStateWithLifecycle()
    val tema by model.tema.collectAsStateWithLifecycle()
    val programa by model.programa.collectAsStateWithLifecycle()
    val fichas = programa?.charlas.orEmpty()
    val descargado = programa?.edicionValida == true
    val aviso by model.aviso.collectAsStateWithLifecycle()
    val cargando by model.cargando.collectAsStateWithLifecycle()
    val temas = listOf("Todos", "Android", "Datos", "Calidad")
    val visibles = fichas.filter { (dia == 0 || it.dia == dia) && (tema == "Todos" || it.tema == tema) }
    // Un único scroll incluye los controles: también deja ver resultados en horizontal.
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Selector(listOf("Ambos días", "Día 1", "Día 2"), dia, model::elegirDia)
        }
        item { Selector(temas, temas.indexOf(tema)) { model.elegirTema(temas[it]) } }
        item { TextButton(onClick = model::recargar) { Text("Actualizar") } }
        if (cargando) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (aviso.isNotEmpty()) {
            item { Text(if (descargado) "$aviso. Usando datos guardados." else aviso) }
            item { OutlinedButton(onClick = model::recargar) { Text("Reintentar") } }
        }
        when {
            programa == null -> item { Text("Leyendo programa guardado…") }
            descargado && fichas.isEmpty() -> item { Text("Sin charlas programadas") }
            descargado && visibles.isEmpty() -> item { Text("No hay charlas para estos filtros") }
            descargado -> items(visibles, key = { it.id }) { ficha -> TarjetaCharla(ficha, abrir) }
            cargando -> item { Text("Cargando programa…") }
        }
    }
}

@Composable
private fun Selector(opciones: List<String>, indice: Int, elegir: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEachIndexed { i, texto ->
            FilterChip(selected = i == indice, onClick = { elegir(i) }, label = { Text(texto) })
        }
    }
}

@Composable
fun Grilla(fichas: List<Charla>, abrir: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 12.dp)) {
        items(fichas, key = { it.id }) { ficha ->
            TarjetaCharla(ficha, abrir)
        }
    }
}

@Composable
private fun TarjetaCharla(ficha: Charla, abrir: (String) -> Unit) {
    ElevatedCard(onClick = { abrir(ficha.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Día ${ficha.dia} · ${ficha.hora} · ${ficha.sala}", style = MaterialTheme.typography.labelMedium)
            Text(ficha.titulo, style = MaterialTheme.typography.titleLarge)
            Text(if (ficha.favorita) "${ficha.tema} · En mi agenda" else ficha.tema)
        }
    }
}
