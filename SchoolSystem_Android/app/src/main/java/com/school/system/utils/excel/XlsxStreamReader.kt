package com.school.system.utils.excel

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

data class RawSheetData(
    val sheetName: String,
    val rows: List<List<String>>
)

object XlsxStreamReader {

    /**
     * Reads all worksheets from an .xlsx file using native ZipFile and XmlPullParser.
     * Zero external dependencies. Extremely fast and low memory footprint.
     */
    fun readWorkbook(context: Context, uri: Uri): List<RawSheetData> {
        val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.xlsx")
        try {
            // Copy Uri stream to temp file for random Zip entry access
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return emptyList()

            ZipFile(tempFile).use { zip ->
                // 1. Parse shared strings
                val sharedStrings = parseSharedStrings(zip)

                // 2. Parse workbook sheet list and relationships
                val sheetMappings = parseSheetMappings(zip)

                // 3. Parse each sheet
                val results = mutableListOf<RawSheetData>()
                for ((sheetName, sheetPath) in sheetMappings) {
                    val entry = zip.getEntry(sheetPath) ?: continue
                    val rows = parseWorksheet(zip.getInputStream(entry), sharedStrings)
                    if (rows.isNotEmpty()) {
                        results.add(RawSheetData(sheetName, rows))
                    }
                }
                return results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Extracts shared strings lookup table from xl/sharedStrings.xml
     */
    private fun parseSharedStrings(zip: ZipFile): List<String> {
        val entry: ZipEntry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        val strings = mutableListOf<String>()

        zip.getInputStream(entry).use { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")

            var eventType = parser.eventType
            var inStringItem = false
            var inTextTag = false
            val currentSb = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "si" -> {
                                inStringItem = true
                                currentSb.setLength(0)
                            }
                            "t" -> {
                                if (inStringItem) inTextTag = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inStringItem && inTextTag) {
                            currentSb.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "t" -> inTextTag = false
                            "si" -> {
                                inStringItem = false
                                strings.add(currentSb.toString())
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        }
        return strings
    }

    /**
     * Maps sheet names from xl/workbook.xml to their actual worksheet file path in xl/
     */
    private fun parseSheetMappings(zip: ZipFile): List<Pair<String, String>> {
        // Parse relationship targets from xl/_rels/workbook.xml.rels
        val relMap = mutableMapOf<String, String>()
        val relEntry = zip.getEntry("xl/_rels/workbook.xml.rels")
        if (relEntry != null) {
            zip.getInputStream(relEntry).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                        val id = parser.getAttributeValue(null, "Id")
                        val target = parser.getAttributeValue(null, "Target")
                        if (id != null && target != null) {
                            val resolvedPath = if (target.startsWith("/")) {
                                target.removePrefix("/")
                            } else if (target.startsWith("worksheets/")) {
                                "xl/$target"
                            } else {
                                "xl/$target"
                            }
                            relMap[id] = resolvedPath
                        }
                    }
                    event = parser.next()
                }
            }
        }

        // Parse sheets from xl/workbook.xml
        val wbEntry = zip.getEntry("xl/workbook.xml") ?: return emptyList()
        val sheetList = mutableListOf<Pair<String, String>>()
        var sheetCounter = 1

        zip.getInputStream(wbEntry).use { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                    val name = parser.getAttributeValue(null, "name") ?: "Sheet$sheetCounter"
                    val rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                        ?: parser.getAttributeValue(null, "r:id")

                    val path = if (rId != null && relMap.containsKey(rId)) {
                        relMap[rId]!!
                    } else {
                        "xl/worksheets/sheet$sheetCounter.xml"
                    }
                    sheetList.add(Pair(name, path))
                    sheetCounter++
                }
                event = parser.next()
            }
        }

        return sheetList
    }

    /**
     * Parses a single worksheet XML into 2D List of cells.
     * Takes cell references like "C5" into account to maintain exact column alignment.
     */
    private fun parseWorksheet(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var currentRow = mutableMapOf<Int, String>()
        var currentCellCol = -1
        var currentCellType = ""
        var inValueTag = false
        var inInlineTextTag = false
        val cellValueSb = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRow = mutableMapOf()
                        }
                        "c" -> {
                            val cellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellCol = extractColIndex(cellRef)
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            cellValueSb.setLength(0)
                        }
                        "v" -> inValueTag = true
                        "t" -> {
                            if (currentCellType == "inlineStr") inInlineTextTag = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValueTag || inInlineTextTag) {
                        cellValueSb.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValueTag = false
                        "t" -> inInlineTextTag = false
                        "c" -> {
                            val rawVal = cellValueSb.toString().trim()
                            val finalVal = when (currentCellType) {
                                "s" -> {
                                    val idx = rawVal.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) {
                                        sharedStrings[idx]
                                    } else {
                                        rawVal
                                    }
                                }
                                "inlineStr" -> rawVal
                                "b" -> if (rawVal == "1") "صحيح" else "خطأ"
                                else -> rawVal
                            }
                            if (currentCellCol >= 0 && finalVal.isNotBlank()) {
                                currentRow[currentCellCol] = finalVal
                            }
                        }
                        "row" -> {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = (currentRow.keys.maxOrNull() ?: 0)
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (c in 0..maxCol) {
                                    rowList.add(currentRow[c] ?: "")
                                }
                                rows.add(rowList)
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }

    /**
     * Converts Excel cell reference column letters (e.g. "A", "B", "Z", "AA") to 0-based column index.
     */
    private fun extractColIndex(cellRef: String): Int {
        var col = 0
        var foundChar = false
        for (ch in cellRef) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
                foundChar = true
            } else if (ch in 'a'..'z') {
                col = col * 26 + (ch - 'a' + 1)
                foundChar = true
            } else {
                break
            }
        }
        return if (foundChar) col - 1 else -1
    }
}
