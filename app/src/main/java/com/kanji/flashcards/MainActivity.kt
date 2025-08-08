package com.kanji.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.time.Instant
import java.time.temporal.ChronoUnit

private val ComponentActivity.dataStore by preferencesDataStore(name = "srs_prefs")

data class KanjiEntry(
    val kanji: String,
    val meanings: List<String>,
    val onYomi: List<String>,
    val kunYomi: List<String>,
    val examples: List<String>,
    val jlpt: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                KanjiFlashcardsApp()
            }
        }
    }

    @Composable
    fun KanjiFlashcardsApp() {
        val scope = rememberCoroutineScope()
        var dataset by remember { mutableStateOf(listOf<KanjiEntry>()) }
        var currentIndex by remember { mutableStateOf(0) }
        var showBack by remember { mutableStateOf(false) }
        var total by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            dataset = loadKanji()
            total = dataset.size
        }

        fun schedule(id: Int, ease: Int) {
            // ease mapping: 0=Again (reset), 1=Hard, 2=Good, 3=Easy
            scope.launch(Dispatchers.IO) {
                val now = Instant.now()
                val keyInterval = intPreferencesKey("interval_$id")
                val keyDue = stringPreferencesKey("due_$id")

                val prefs = dataStore.data.first()
                val oldInterval = prefs[keyInterval] ?: 0

                val newInterval = when (ease) {
                    0 -> 0
                    1 -> maxOf(1, (oldInterval * 7) / 10) + 1 // ~70% of previous + 1 day
                    2 -> maxOf(1, (oldInterval + 1) * 2)      // double
                    else -> maxOf(1, (oldInterval + 1) * 3)   // triple
                }

                val due = now.plus(newInterval.toLong(), ChronoUnit.DAYS).toEpochMilli()
                dataStore.edit { ds ->
                    ds[keyInterval] = newInterval
                    ds[keyDue] = due.toString()
                }
            }
        }

        Scaffold(
            topBar = {
                SmallTopAppBar(title = { Text("Kanji Flashcards") })
            },
            bottomBar = {
                BottomAppBar {
                    Text(
                        "Total: $total",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        ) { pad ->
            if (dataset.isEmpty()) {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(pad), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val card = dataset[currentIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                            .clickable { showBack = !showBack }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!showBack) {
                            Text(
                                card.kanji,
                                fontSize = 96.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Meanings: " + card.meanings.joinToString(", "), fontSize = 18.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Text("On: " + card.onYomi.joinToString("・"), fontSize = 18.sp, textAlign = TextAlign.Center)
                                Text("Kun: " + card.kunYomi.joinToString("・"), fontSize = 18.sp, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Text("Examples:", fontWeight = FontWeight.SemiBold)
                                for (ex in card.examples.take(6)) {
                                    Text("・$ex")
                                }
                                card.jlpt?.let {
                                    Spacer(Modifier.height(8.dp))
                                    Text("JLPT: $it", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = {
                            schedule(currentIndex, 0)
                            showBack = false
                            currentIndex = (currentIndex + 1) % dataset.size
                        }) { Text("Again") }
                        Button(onClick = {
                            schedule(currentIndex, 1)
                            showBack = false
                            currentIndex = (currentIndex + 1) % dataset.size
                        }) { Text("Hard") }
                        Button(onClick = {
                            schedule(currentIndex, 2)
                            showBack = false
                            currentIndex = (currentIndex + 1) % dataset.size
                        }) { Text("Good") }
                        Button(onClick = {
                            schedule(currentIndex, 3)
                            showBack = false
                            currentIndex = (currentIndex + 1) % dataset.size
                        }) { Text("Easy") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        showBack = !showBack
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showBack) "Show Kanji" else "Show Answer")
                    }
                }
            }
        }
    }

    private fun loadKanji(): List<KanjiEntry> {
        val jsonStr = assets.open("kanji.json").bufferedReader().use(BufferedReader::readText)
        val arr = JSONArray(jsonStr)
        val out = mutableListOf<KanjiEntry>();
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                KanjiEntry(
                    kanji = o.getString("kanji"),
                    meanings = List(o.getJSONArray("meanings").length()) { idx -> o.getJSONArray("meanings").getString(idx) },
                    onYomi = List(o.getJSONArray("onYomi").length()) { idx -> o.getJSONArray("onYomi").getString(idx) },
                    kunYomi = List(o.getJSONArray("kunYomi").length()) { idx -> o.getJSONArray("kunYomi").getString(idx) },
                    examples = List(o.getJSONArray("examples").length()) { idx -> o.getJSONArray("examples").getString(idx) },
                    jlpt = if (o.has("jlpt")) o.getString("jlpt") else null
                )
            )
        }
        return out
    }
}