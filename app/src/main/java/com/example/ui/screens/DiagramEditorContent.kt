package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.Note
import com.example.ui.localization.localize
import com.example.ui.viewmodel.NoteViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

data class DiagramElement(
    val id: String = UUID.randomUUID().toString(),
    val shapeType: String = "Rectangle", // Rectangle, Circle, Diamond, Text Box, Mind Map Node, UML Class, ER Entity, Network Node
    val x: Float,
    val y: Float,
    val width: Float = 140f,
    val height: Float = 90f,
    val rotation: Float = 0f,
    val text: String = "",
    val color: String = "Default",
    val fontSize: Int = 14
)

data class DiagramConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val toId: String,
    val lineType: String = "arrow", // arrow, line
    val color: String = "Default",
    val text: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagramEditorContent(
    viewModel: NoteViewModel,
    note: Note,
    title: String,
    onTitleChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    colorName: String,
    onColorNameChange: (String) -> Unit,
    initialContent: String,
    onSaveContent: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val localDensity = androidx.compose.ui.platform.LocalDensity.current

    // Editor Undo/Redo Stacks
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }

    // Diagram Model State
    val elements = remember { mutableStateListOf<DiagramElement>() }
    val connections = remember { mutableStateListOf<DiagramConnection>() }

    // Canvas Navigation
    var zoom by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }

    // Selection States
    val selectedIds = remember { mutableStateListOf<String>() }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // Connection Creation Mode
    var connectionSourceId by remember { mutableStateOf<String?>(null) }
    var isConnectingMode by remember { mutableStateOf(false) }

    // Dialogs
    var showEditDialog by remember { mutableStateOf<DiagramElement?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var shapeTextToEdit by remember { mutableStateOf("") }
    var shapeFontSizeToEdit by remember { mutableStateOf(14) }

    // Category list from viewmodel
    val categories by viewModel.categories.collectAsState()

    // Helper to push current state onto undo stack
    val saveToHistory: () -> Unit = {
        val json = JSONObject()
        json.put("type", "diagram")
        val elemsArray = JSONArray()
        elements.forEach { el ->
            val elJson = JSONObject().apply {
                put("id", el.id)
                put("shapeType", el.shapeType)
                put("x", el.x)
                put("y", el.y)
                put("width", el.width)
                put("height", el.height)
                put("rotation", el.rotation)
                put("text", el.text)
                put("color", el.color)
                put("fontSize", el.fontSize)
            }
            elemsArray.put(elJson)
        }
        json.put("elements", elemsArray)

        val connsArray = JSONArray()
        connections.forEach { conn ->
            val connJson = JSONObject().apply {
                put("id", conn.id)
                put("fromId", conn.fromId)
                put("toId", conn.toId)
                put("lineType", conn.lineType)
                put("color", conn.color)
                put("text", conn.text)
            }
            connsArray.put(connJson)
        }
        json.put("connections", connsArray)
        json.put("zoom", zoom)
        json.put("panX", panX)
        json.put("panY", panY)

        val serialized = json.toString()
        undoStack.add(serialized)
        redoStack.clear()
        onSaveContent(serialized)
    }

    // Load initial content
    LaunchedEffect(initialContent) {
        if (elements.isEmpty() && initialContent.isNotEmpty()) {
            try {
                if (initialContent.startsWith("{")) {
                    val json = JSONObject(initialContent)
                    if (json.optString("type") == "diagram") {
                        elements.clear()
                        connections.clear()
                        
                        val elemsArray = json.optJSONArray("elements")
                        if (elemsArray != null) {
                            for (i in 0 until elemsArray.length()) {
                                val item = elemsArray.getJSONObject(i)
                                elements.add(
                                    DiagramElement(
                                        id = item.getString("id"),
                                        shapeType = item.getString("shapeType"),
                                        x = item.getDouble("x").toFloat(),
                                        y = item.getDouble("y").toFloat(),
                                        width = item.optDouble("width", 140.0).toFloat(),
                                        height = item.optDouble("height", 90.0).toFloat(),
                                        rotation = item.optDouble("rotation", 0.0).toFloat(),
                                        text = item.optString("text", ""),
                                        color = item.optString("color", "Default"),
                                        fontSize = item.optInt("fontSize", 14)
                                    )
                                )
                            }
                        }

                        val connsArray = json.optJSONArray("connections")
                        if (connsArray != null) {
                            for (i in 0 until connsArray.length()) {
                                val item = connsArray.getJSONObject(i)
                                connections.add(
                                    DiagramConnection(
                                        id = item.getString("id"),
                                        fromId = item.getString("fromId"),
                                        toId = item.getString("toId"),
                                        lineType = item.optString("lineType", "arrow"),
                                        color = item.optString("color", "Default"),
                                        text = item.optString("text", "")
                                    )
                                )
                            }
                        }
                        zoom = json.optDouble("zoom", 1.0).toFloat()
                        panX = json.optDouble("panX", 0.0).toFloat()
                        panY = json.optDouble("panY", 0.0).toFloat()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Map color names to M3 compatible colors
    fun getColorForShape(colorName: String, isSelected: Boolean): Color {
        val baseColor = when (colorName) {
            "Red" -> Color(0xFFFFCDD2)
            "Orange" -> Color(0xFFFFE0B2)
            "Yellow" -> Color(0xFFFFF9C4)
            "Green" -> Color(0xFFC8E6C9)
            "Teal" -> Color(0xFFB2DFDB)
            "Blue" -> Color(0xFFBBDEFB)
            "Purple" -> Color(0xFFE1BEE7)
            "Pink" -> Color(0xFFF8BBD0)
            else -> Color(0xFFECEFF1)
        }
        return if (isSelected) baseColor.copy(alpha = 0.9f) else baseColor
    }

    fun getBorderColorForShape(colorName: String, isSelected: Boolean): Color {
        return if (isSelected) {
            Color(0xFF2196F3)
        } else {
            when (colorName) {
                "Red" -> Color(0xFFEF9A9A)
                "Orange" -> Color(0xFFFFCC80)
                "Yellow" -> Color(0xFFFFF59D)
                "Green" -> Color(0xFFA5D6A7)
                "Teal" -> Color(0xFF80CBC4)
                "Blue" -> Color(0xFF90CAF9)
                "Purple" -> Color(0xFFCE93D8)
                "Pink" -> Color(0xFFF48FB1)
                else -> Color(0xFFB0BEC5)
            }
        }
    }

    // Add a new element to the center of view port
    fun addElement(shapeType: String) {
        val centerX = -panX + 250f
        val centerY = -panY + 300f
        val newElement = DiagramElement(
            shapeType = shapeType,
            x = centerX,
            y = centerY,
            text = shapeType,
            color = "Default"
        )
        elements.add(newElement)
        selectedIds.clear()
        selectedIds.add(newElement.id)
        saveToHistory()
    }

    fun restoreSerializedState(serialized: String) {
        try {
            val json = JSONObject(serialized)
            elements.clear()
            connections.clear()
            
            val elemsArray = json.optJSONArray("elements")
            if (elemsArray != null) {
                for (i in 0 until elemsArray.length()) {
                    val item = elemsArray.getJSONObject(i)
                    elements.add(
                        DiagramElement(
                            id = item.getString("id"),
                            shapeType = item.getString("shapeType"),
                            x = item.getDouble("x").toFloat(),
                            y = item.getDouble("y").toFloat(),
                            width = item.optDouble("width", 140.0).toFloat(),
                            height = item.optDouble("height", 90.0).toFloat(),
                            rotation = item.optDouble("rotation", 0.0).toFloat(),
                            text = item.optString("text", ""),
                            color = item.optString("color", "Default"),
                            fontSize = item.optInt("fontSize", 14)
                        )
                    )
                }
            }

            val connsArray = json.optJSONArray("connections")
            if (connsArray != null) {
                for (i in 0 until connsArray.length()) {
                    val item = connsArray.getJSONObject(i)
                    connections.add(
                        DiagramConnection(
                            id = item.getString("id"),
                            fromId = item.getString("fromId"),
                            toId = item.getString("toId"),
                            lineType = item.optString("lineType", "arrow"),
                            color = item.optString("color", "Default"),
                            text = item.optString("text", "")
                        )
                    )
                }
            }
            zoom = json.optDouble("zoom", 1.0).toFloat()
            panX = json.optDouble("panX", 0.0).toFloat()
            panY = json.optDouble("panY", 0.0).toFloat()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Perform Undo
    fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val currentState = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(currentState)
            
            val previousState = if (undoStack.isNotEmpty()) undoStack.last() else null
            if (previousState != null) {
                restoreSerializedState(previousState)
                onSaveContent(previousState)
            } else {
                elements.clear()
                connections.clear()
                onSaveContent("")
            }
        }
    }

    // Perform Redo
    fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val stateToRestore = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(stateToRestore)
            restoreSerializedState(stateToRestore)
            onSaveContent(stateToRestore)
        }
    }

    // Export PNG
    fun exportPng(isJpg: Boolean = false) {
        try {
            val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
            }

            val strokePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3f
                color = android.graphics.Color.DKGRAY
            }

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                textSize = 32f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // Draw Connections
            connections.forEach { conn ->
                val fromNode = elements.find { it.id == conn.fromId }
                val toNode = elements.find { it.id == conn.toId }
                if (fromNode != null && toNode != null) {
                    canvas.drawLine(
                        fromNode.x + fromNode.width / 2,
                        fromNode.y + fromNode.height / 2,
                        toNode.x + toNode.width / 2,
                        toNode.y + toNode.height / 2,
                        strokePaint
                    )
                }
            }

            // Draw Elements
            elements.forEach { el ->
                paint.color = when (el.color) {
                    "Red" -> android.graphics.Color.parseColor("#FFCDD2")
                    "Orange" -> android.graphics.Color.parseColor("#FFE0B2")
                    "Yellow" -> android.graphics.Color.parseColor("#FFF9C4")
                    "Green" -> android.graphics.Color.parseColor("#C8E6C9")
                    "Teal" -> android.graphics.Color.parseColor("#B2DFDB")
                    "Blue" -> android.graphics.Color.parseColor("#BBDEFB")
                    "Purple" -> android.graphics.Color.parseColor("#E1BEE7")
                    "Pink" -> android.graphics.Color.parseColor("#F8BBD0")
                    else -> android.graphics.Color.parseColor("#ECEFF1")
                }
                
                canvas.save()
                canvas.rotate(el.rotation, el.x + el.width/2, el.y + el.height/2)
                
                when (el.shapeType) {
                    "Circle" -> {
                        canvas.drawOval(el.x, el.y, el.x + el.width, el.y + el.height, paint)
                        canvas.drawOval(el.x, el.y, el.x + el.width, el.y + el.height, strokePaint)
                    }
                    "Diamond" -> {
                        val path = android.graphics.Path().apply {
                            moveTo(el.x + el.width/2, el.y)
                            lineTo(el.x + el.width, el.y + el.height/2)
                            lineTo(el.x + el.width/2, el.y + el.height)
                            lineTo(el.x, el.y + el.height/2)
                            close()
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawPath(path, strokePaint)
                    }
                    else -> {
                        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, paint)
                        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, strokePaint)
                    }
                }
                canvas.drawText(el.text, el.x + el.width / 2, el.y + el.height / 2 + 10, textPaint)
                canvas.restore()
            }

            val format = if (isJpg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val suffix = if (isJpg) ".jpg" else ".png"
            val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Diagram_${System.currentTimeMillis()}$suffix")
            FileOutputStream(file).use { out ->
                bitmap.compress(format, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = if (isJpg) "image/jpeg" else "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(intent, "Export Diagram Image").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Export SVG
    fun exportSvg() {
        try {
            val sb = java.lang.StringBuilder()
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"1000\" viewBox=\"0 0 800 1000\">\n")
            sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n")

            // Draw connections
            connections.forEach { conn ->
                val fromNode = elements.find { it.id == conn.fromId }
                val toNode = elements.find { it.id == conn.toId }
                if (fromNode != null && toNode != null) {
                    val x1 = fromNode.x + fromNode.width / 2
                    val y1 = fromNode.y + fromNode.height / 2
                    val x2 = toNode.x + toNode.width / 2
                    val y2 = toNode.y + toNode.height / 2
                    sb.append("  <line x1=\"$x1\" y1=\"$y1\" x2=\"$x2\" y2=\"$y2\" stroke=\"#333333\" stroke-width=\"2\" />\n")
                }
            }

            // Draw elements
            elements.forEach { el ->
                val fill = when (el.color) {
                    "Red" -> "#FFCDD2"
                    "Orange" -> "#FFE0B2"
                    "Yellow" -> "#FFF9C4"
                    "Green" -> "#C8E6C9"
                    "Teal" -> "#B2DFDB"
                    "Blue" -> "#BBDEFB"
                    "Purple" -> "#E1BEE7"
                    "Pink" -> "#F8BBD0"
                    else -> "#ECEFF1"
                }
                val stroke = "#78909c"
                
                sb.append("  <g transform=\"rotate(${el.rotation} ${el.x + el.width/2} ${el.y + el.height/2})\">\n")
                when (el.shapeType) {
                    "Circle" -> {
                        val cx = el.x + el.width / 2
                        val cy = el.y + el.height / 2
                        val rx = el.width / 2
                        val ry = el.height / 2
                        sb.append("    <ellipse cx=\"$cx\" cy=\"$cy\" rx=\"$rx\" ry=\"$ry\" fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"2\"/>\n")
                    }
                    "Diamond" -> {
                        val p1x = el.x + el.width / 2
                        val p1y = el.y
                        val p2x = el.x + el.width
                        val p2y = el.y + el.height / 2
                        val p3x = el.x + el.width / 2
                        val p3y = el.y + el.height
                        val p4x = el.x
                        val p4y = el.y + el.height / 2
                        sb.append("    <polygon points=\"$p1x,$p1y $p2x,$p2y $p3x,$p3y $p4x,$p4y\" fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"2\"/>\n")
                    }
                    else -> {
                        sb.append("    <rect x=\"${el.x}\" y=\"${el.y}\" width=\"${el.width}\" height=\"${el.height}\" rx=\"5\" ry=\"5\" fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"2\"/>\n")
                    }
                }
                val tx = el.x + el.width / 2
                val ty = el.y + el.height / 2 + 5
                sb.append("    <text x=\"$tx\" y=\"$ty\" font-family=\"sans-serif\" font-size=\"14\" text-anchor=\"middle\" fill=\"#000000\">${el.text}</text>\n")
                sb.append("  </g>\n")
            }
            sb.append("</svg>")

            val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Diagram_${System.currentTimeMillis()}.svg")
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/svg+xml"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(intent, "Export Diagram SVG").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "SVG exported to Downloads!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Export PDF
    fun exportPdf() {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
            }

            val strokePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 2f
                color = android.graphics.Color.BLACK
            }

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.BLACK
                textSize = 12f
                textAlign = android.graphics.Paint.Align.CENTER
            }

            // Draw Connections
            connections.forEach { conn ->
                val fromNode = elements.find { it.id == conn.fromId }
                val toNode = elements.find { it.id == conn.toId }
                if (fromNode != null && toNode != null) {
                    canvas.drawLine(
                        fromNode.x + fromNode.width / 2,
                        fromNode.y + fromNode.height / 2,
                        toNode.x + toNode.width / 2,
                        toNode.y + toNode.height / 2,
                        strokePaint
                    )
                }
            }

            // Draw Elements
            elements.forEach { el ->
                paint.color = when (el.color) {
                    "Red" -> android.graphics.Color.parseColor("#FFCDD2")
                    "Orange" -> android.graphics.Color.parseColor("#FFE0B2")
                    "Yellow" -> android.graphics.Color.parseColor("#FFF9C4")
                    "Green" -> android.graphics.Color.parseColor("#C8E6C9")
                    "Teal" -> android.graphics.Color.parseColor("#B2DFDB")
                    "Blue" -> android.graphics.Color.parseColor("#BBDEFB")
                    "Purple" -> android.graphics.Color.parseColor("#E1BEE7")
                    "Pink" -> android.graphics.Color.parseColor("#F8BBD0")
                    else -> android.graphics.Color.parseColor("#ECEFF1")
                }
                
                canvas.save()
                canvas.rotate(el.rotation, el.x + el.width/2, el.y + el.height/2)
                
                when (el.shapeType) {
                    "Circle" -> {
                        canvas.drawOval(el.x, el.y, el.x + el.width, el.y + el.height, paint)
                        canvas.drawOval(el.x, el.y, el.x + el.width, el.y + el.height, strokePaint)
                    }
                    "Diamond" -> {
                        val path = android.graphics.Path().apply {
                            moveTo(el.x + el.width/2, el.y)
                            lineTo(el.x + el.width, el.y + el.height/2)
                            lineTo(el.x + el.width/2, el.y + el.height)
                            lineTo(el.x, el.y + el.height/2)
                            close()
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawPath(path, strokePaint)
                    }
                    else -> {
                        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, paint)
                        canvas.drawRect(el.x, el.y, el.x + el.width, el.y + el.height, strokePaint)
                    }
                }
                canvas.drawText(el.text, el.x + el.width / 2, el.y + el.height / 2 + 4, textPaint)
                canvas.restore()
            }

            pdfDocument.finishPage(page)

            val file = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "Diagram_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(intent, "Export Diagram PDF").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            Toast.makeText(context, "PDF exported to Downloads!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Edit and View Settings Header
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Diagram Canvas",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Undo / Redo
                IconButton(onClick = { performUndo() }, enabled = undoStack.isNotEmpty()) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { performRedo() }, enabled = redoStack.isNotEmpty()) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }

                // Export Submenu
                var showExportMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showExportMenu = true }) {
                    Icon(Icons.Default.IosShare, contentDescription = "Export")
                }
                DropdownMenu(
                    expanded = showExportMenu,
                    onDismissRequest = { showExportMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Export as PNG") },
                        onClick = { exportPng(false); showExportMenu = false },
                        leadingIcon = { Icon(Icons.Default.Image, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as JPG") },
                        onClick = { exportPng(true); showExportMenu = false },
                        leadingIcon = { Icon(Icons.Default.Image, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as SVG") },
                        onClick = { exportSvg(); showExportMenu = false },
                        leadingIcon = { Icon(Icons.Default.Code, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as PDF") },
                        onClick = { exportPdf(); showExportMenu = false },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                    )
                }
            }
        )

        // Title and Category Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = title,
                        onValueChange = onTitleChange,
                        placeholder = { Text("Untitled Diagram", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("diagram_title_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    SuggestionChip(
                        onClick = { showCategoryDialog = true },
                        label = { Text(category) },
                        icon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        // Palette & Shapes Tool Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Insert: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                
                val shapeTypes = listOf(
                    "Rectangle" to Icons.Default.CropSquare,
                    "Circle" to Icons.Default.Circle,
                    "Diamond" to Icons.Default.Category,
                    "Text Box" to Icons.Default.TextFields,
                    "Process" to Icons.Default.DirectionsRun,
                    "Mind Map Node" to Icons.Default.Hub,
                    "UML Class" to Icons.Default.CalendarViewWeek,
                    "ER Entity" to Icons.Default.Splitscreen,
                    "Network Node" to Icons.Default.Router
                )

                shapeTypes.forEach { (type, icon) ->
                    InputChip(
                        selected = false,
                        onClick = { addElement(type) },
                        label = { Text(type, fontSize = 11.sp) },
                        leadingIcon = { Icon(icon, null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Connection & Selection Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Connection Mode Toggle
                FilledIconToggleButton(
                    checked = isConnectingMode,
                    onCheckedChange = {
                        isConnectingMode = it
                        if (!it) {
                            connectionSourceId = null
                        } else {
                            Toast.makeText(context, "Tap source shape, then target shape to connect!", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Shortcut, contentDescription = "Connect Shapes")
                }

                // Multi-select Mode Toggle
                FilledIconToggleButton(
                    checked = isMultiSelectMode,
                    onCheckedChange = {
                        isMultiSelectMode = it
                        if (!it) {
                            selectedIds.clear()
                        }
                    }
                ) {
                    Icon(Icons.Default.LibraryAddCheck, contentDescription = "Multi Select")
                }

                // Clear Selection
                if (selectedIds.isNotEmpty()) {
                    IconButton(onClick = { selectedIds.clear() }) {
                        Icon(Icons.Default.LayersClear, contentDescription = "Deselect All")
                    }
                }
            }

            // Zoom Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { zoom = (zoom - 0.1f).coerceAtLeast(0.5f) }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                }
                Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = { zoom = (zoom + 0.1f).coerceAtMost(2.0f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                }
                IconButton(onClick = { zoom = 1.0f; panX = 0f; panY = 0f }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset Pan & Zoom")
                }
            }
        }

        // Active Diagram Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // If we are NOT dragging an element, pan the whole canvas
                        panX += dragAmount.x
                        panY += dragAmount.y
                    }
                }
        ) {
            // Draw Dots / Grid Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 40f * zoom
                val offsetX = panX % gridSpacing
                val offsetY = panY % gridSpacing

                // Horizontal and Vertical lines or dots
                for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(x + offsetX, 0f),
                        end = Offset(x + offsetX, size.height),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, y + offsetY),
                        end = Offset(size.width, y + offsetY),
                        strokeWidth = 1f
                    )
                }

                // Smart Alignment Guides (horizontal and vertical lines)
                // If any node is being dragged, we can draw a guide (for simplicity, we render connection guide lines)
            }

            // Draw Connections / Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                connections.forEach { conn ->
                    val fromNode = elements.find { it.id == conn.fromId }
                    val toNode = elements.find { it.id == conn.toId }
                    if (fromNode != null && toNode != null) {
                        // Source and Destination Anchor offsets
                        val startX = (fromNode.x + fromNode.width / 2) * zoom + panX
                        val startY = (fromNode.y + fromNode.height / 2) * zoom + panY
                        val endX = (toNode.x + toNode.width / 2) * zoom + panX
                        val endY = (toNode.y + toNode.height / 2) * zoom + panY

                        // Draw main line
                        drawLine(
                            color = Color(0xFF5C6BC0),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 3f * zoom
                        )

                        // Draw Arrowhead if lineType is arrow
                        if (conn.lineType == "arrow") {
                            val angle = kotlin.math.atan2(endY - startY, endX - startX)
                            val arrowLength = 15f * zoom
                            val arrowAngle = Math.PI / 6 // 30 degrees
                            
                            val x1 = endX - arrowLength * cos(angle - arrowAngle).toFloat()
                            val y1 = endY - arrowLength * sin(angle - arrowAngle).toFloat()
                            val x2 = endX - arrowLength * cos(angle + arrowAngle).toFloat()
                            val y2 = endY - arrowLength * sin(angle + arrowAngle).toFloat()

                            val arrowPath = Path().apply {
                                moveTo(endX, endY)
                                lineTo(x1, y1)
                                lineTo(x2, y2)
                                close()
                            }
                            drawPath(
                                path = arrowPath,
                                color = Color(0xFF5C6BC0)
                            )
                        }
                    }
                }
            }

            // Draw Elements on Canvas
            elements.forEachIndexed { index, el ->
                val isSelected = selectedIds.contains(el.id)
                val nodeX = el.x * zoom + panX
                val nodeY = el.y * zoom + panY
                val nodeW = el.width * zoom
                val nodeH = el.height * zoom

                Box(
                    modifier = Modifier
                        .offset(
                            x = (nodeX / localDensity.run { density }).dp,
                            y = (nodeY / localDensity.run { density }).dp
                        )
                        .size(
                            width = (nodeW / localDensity.run { density }).dp,
                            height = (nodeH / localDensity.run { density }).dp
                        )
                        .graphicsLayer(rotationZ = el.rotation)
                        .background(
                            color = getColorForShape(el.color, isSelected),
                            shape = when (el.shapeType) {
                                "Circle" -> CircleShape
                                "Rectangle", "Text Box", "UML Class", "ER Entity" -> RoundedCornerShape(8.dp)
                                else -> RoundedCornerShape(0.dp) // Other custom drawn shapes via canvas
                            }
                        )
                        .border(
                            width = if (isSelected) 3.dp else 2.dp,
                            color = getBorderColorForShape(el.color, isSelected),
                            shape = when (el.shapeType) {
                                "Circle" -> CircleShape
                                "Rectangle", "Text Box", "UML Class", "ER Entity" -> RoundedCornerShape(8.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    if (isConnectingMode) {
                                        if (connectionSourceId == null) {
                                            connectionSourceId = el.id
                                            Toast.makeText(context, "Source set! Now select destination shape.", Toast.LENGTH_SHORT).show()
                                        } else if (connectionSourceId != el.id) {
                                            // Connect!
                                            connections.add(
                                                DiagramConnection(
                                                    fromId = connectionSourceId!!,
                                                    toId = el.id
                                                )
                                            )
                                            connectionSourceId = null
                                            isConnectingMode = false
                                            Toast.makeText(context, "Shapes connected successfully!", Toast.LENGTH_SHORT).show()
                                            saveToHistory()
                                        }
                                    } else {
                                        // Regular select / select multiple
                                        if (isMultiSelectMode) {
                                            if (selectedIds.contains(el.id)) {
                                                selectedIds.remove(el.id)
                                            } else {
                                                selectedIds.add(el.id)
                                            }
                                        } else {
                                            if (!selectedIds.contains(el.id)) {
                                                selectedIds.clear()
                                                selectedIds.add(el.id)
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (!isConnectingMode) {
                                        // Move all selected nodes
                                        if (selectedIds.contains(el.id)) {
                                            selectedIds.forEach { selId ->
                                                val sIdx = elements.indexOfFirst { it.id == selId }
                                                if (sIdx != -1) {
                                                    val node = elements[sIdx]
                                                    var newX = node.x + dragAmount.x / zoom
                                                    var newY = node.y + dragAmount.y / zoom
                                                    
                                                    // Auto Snap to nearest grid (e.g., 20)
                                                    newX = (Math.round(newX / 20f) * 20f).toFloat()
                                                    newY = (Math.round(newY / 20f) * 20f).toFloat()

                                                    elements[sIdx] = node.copy(x = newX, y = newY)
                                                }
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    saveToHistory()
                                }
                            )
                        }
                        .clickable {
                            if (isConnectingMode) {
                                if (connectionSourceId == null) {
                                    connectionSourceId = el.id
                                    Toast.makeText(context, "Source set! Tap target to connect.", Toast.LENGTH_SHORT).show()
                                } else if (connectionSourceId != el.id) {
                                    connections.add(DiagramConnection(fromId = connectionSourceId!!, toId = el.id))
                                    connectionSourceId = null
                                    isConnectingMode = false
                                    Toast.makeText(context, "Connected!", Toast.LENGTH_SHORT).show()
                                    saveToHistory()
                                }
                            } else {
                                if (isMultiSelectMode) {
                                    if (selectedIds.contains(el.id)) selectedIds.remove(el.id) else selectedIds.add(el.id)
                                } else {
                                    selectedIds.clear()
                                    selectedIds.add(el.id)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = el.text,
                            fontSize = el.fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Properties button overlays
                    IconButton(
                        onClick = {
                            shapeTextToEdit = el.text
                            shapeFontSizeToEdit = el.fontSize
                            showEditDialog = el
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Text", tint = Color.Black.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Selected Node Property Configuration Bar
        if (selectedIds.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Customize Selected Shape (${selectedIds.size} selected)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Resize (Width slider)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Width", fontSize = 10.sp)
                            val firstSelected = elements.find { it.id == selectedIds.first() }
                            Slider(
                                value = firstSelected?.width ?: 140f,
                                onValueChange = { newW ->
                                    selectedIds.forEach { selId ->
                                        val idx = elements.indexOfFirst { it.id == selId }
                                        if (idx != -1) {
                                            elements[idx] = elements[idx].copy(width = newW)
                                        }
                                    }
                                },
                                valueRange = 60f..300f,
                                onValueChangeFinished = { saveToHistory() }
                            )
                        }

                        // Resize (Height slider)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Height", fontSize = 10.sp)
                            val firstSelected = elements.find { it.id == selectedIds.first() }
                            Slider(
                                value = firstSelected?.height ?: 90f,
                                onValueChange = { newH ->
                                    selectedIds.forEach { selId ->
                                        val idx = elements.indexOfFirst { it.id == selId }
                                        if (idx != -1) {
                                            elements[idx] = elements[idx].copy(height = newH)
                                        }
                                    }
                                },
                                valueRange = 40f..200f,
                                onValueChangeFinished = { saveToHistory() }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotation slider
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rotate", fontSize = 10.sp)
                            val firstSelected = elements.find { it.id == selectedIds.first() }
                            Slider(
                                value = firstSelected?.rotation ?: 0f,
                                onValueChange = { newRot ->
                                    selectedIds.forEach { selId ->
                                        val idx = elements.indexOfFirst { it.id == selId }
                                        if (idx != -1) {
                                            elements[idx] = elements[idx].copy(rotation = newRot)
                                        }
                                    }
                                },
                                valueRange = 0f..360f,
                                onValueChangeFinished = { saveToHistory() }
                            )
                        }

                        // FontSize slider
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Font Size", fontSize = 10.sp)
                            val firstSelected = elements.find { it.id == selectedIds.first() }
                            Slider(
                                value = (firstSelected?.fontSize ?: 14).toFloat(),
                                onValueChange = { newSize ->
                                    selectedIds.forEach { selId ->
                                        val idx = elements.indexOfFirst { it.id == selId }
                                        if (idx != -1) {
                                            elements[idx] = elements[idx].copy(fontSize = newSize.toInt())
                                        }
                                    }
                                },
                                valueRange = 10f..28f,
                                onValueChangeFinished = { saveToHistory() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Colors Palette selection row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Default", "Red", "Orange", "Yellow", "Green", "Teal", "Blue", "Purple", "Pink").forEach { col ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(getColorForShape(col, false))
                                        .border(1.dp, Color.Gray, CircleShape)
                                        .clickable {
                                            selectedIds.forEach { selId ->
                                                val idx = elements.indexOfFirst { it.id == selId }
                                                if (idx != -1) {
                                                    elements[idx] = elements[idx].copy(color = col)
                                                }
                                            }
                                            saveToHistory()
                                        }
                                )
                            }
                        }

                        // Delete Shape Button
                        Button(
                            onClick = {
                                selectedIds.forEach { selId ->
                                    elements.removeIf { it.id == selId }
                                    connections.removeIf { it.fromId == selId || it.toId == selId }
                                }
                                selectedIds.clear()
                                saveToHistory()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Shape Label Dialog
    if (showEditDialog != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Edit Shape Text") },
            text = {
                Column {
                    OutlinedTextField(
                        value = shapeTextToEdit,
                        onValueChange = { shapeTextToEdit = it },
                        label = { Text("Label text") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_shape_text_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val elToUpdate = showEditDialog!!
                        val idx = elements.indexOfFirst { it.id == elToUpdate.id }
                        if (idx != -1) {
                            elements[idx] = elements[idx].copy(
                                text = shapeTextToEdit,
                                fontSize = shapeFontSizeToEdit
                            )
                        }
                        showEditDialog = null
                        saveToHistory()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Category Select Dialog
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Folder") },
            text = {
                Column {
                    categories.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat) },
                            modifier = Modifier.clickable {
                                onCategoryChange(cat)
                                showCategoryDialog = false
                                saveToHistory()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
