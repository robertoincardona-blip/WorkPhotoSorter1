package com.example.workphotosorter

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Calendar

object PhotoClassifier {
    private val workLabels = setOf("screenshot", "text", "document", "receipt", "whiteboard", "laptop", "computer", "chart", "graph", "presentation", "office", "diagram")
    private const val TEXT_WORD_THRESHOLD = 50

    fun isWorkPhoto(context: Context, uri: Uri, isInOffice: Boolean, callback: (Boolean) -> Unit) {
        val cal = Calendar.getInstance()
        val isWeekend = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        if (!isInOffice && !isWeekend) { callback(false); return }
        val fileName = getFileName(context, uri)
        if (fileName?.contains("Screenshot", ignoreCase = true) == true) { callback(true); return }
        val image = InputImage.fromFilePath(context, uri)
        var ocrDone = false; var labelDone = false; var isWorkByOcr = false; var isWorkByLabel = false
        fun checkResults() {
            if (ocrDone && labelDone) {
                val isOfficeHours = cal.get(Calendar.HOUR_OF_DAY) in 8..19
                callback(isWorkByOcr || isWorkByLabel || isOfficeHours && isInOffice)
            }
        }
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener { visionText ->
                isWorkByOcr = visionText.text.split("\\s+".toRegex()).size > TEXT_WORD_THRESHOLD
                ocrDone = true; checkResults()
            }.addOnFailureListener { ocrDone = true; checkResults() }
        ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS).process(image)
            .addOnSuccessListener { labels ->
                isWorkByLabel = labels.any { workLabels.contains(it.text.lowercase()) && it.confidence > 0.75f }
                labelDone = true; checkResults()
            }.addOnFailureListener { labelDone = true; checkResults() }
    }

    fun getFileName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) return cursor.getString(nameIndex)
            }
        }
        return null
    }
}
