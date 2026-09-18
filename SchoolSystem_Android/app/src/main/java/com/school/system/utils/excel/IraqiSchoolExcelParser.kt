package com.school.system.utils.excel

import java.util.UUID

data class ParsedStudent(
    val id: String = UUID.randomUUID().toString(),
    val recordNumber: String,
    val fullName: String,
    val firstName: String,
    val secondName: String,
    val thirdName: String,
    val fourthName: String,
    val titleName: String,
    val grade: String,
    val section: String
)

data class ParsedSchoolClass(
    val sheetName: String,
    val grade: String,
    val section: String,
    val students: List<ParsedStudent>
)

object IraqiSchoolExcelParser {

    /**
     * Normalizes Arabic string for robust matching
     */
    fun normalizeArabic(str: String?): String {
        if (str.isNullOrBlank()) return ""
        return str
            .trim()
            .replace("[أإآ]".toRegex(), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("^(الصف|صف)\\s+".toRegex(), "")
            .replace("(^|\\s)ال".toRegex(), "$1")
            .replace("\\s+".toRegex(), "")
    }

    /**
     * Standardizes school grade names to Iraqi Ministry standard nomenclature
     */
    fun standardizeGradeName(gradeStr: String?): String {
        if (gradeStr.isNullOrBlank()) return "الأول المتوسط"
        val s = gradeStr.trim().replace("^(الصف|صف)\\s+".toRegex(), "").trim()
        val lower = s.replace("[أإآ]".toRegex(), "ا").replace("ة", "ه").replace("ى", "ي")

        var base = "الأول"
        when {
            lower.contains("سادس") || lower.contains("6") || lower.contains("٦") -> base = "السادس"
            lower.contains("خامس") || lower.contains("5") || lower.contains("٥") -> base = "الخامس"
            lower.contains("رابع") || lower.contains("4") || lower.contains("٤") -> base = "الرابع"
            lower.contains("ثالث") || lower.contains("3") || lower.contains("٣") -> base = "الثالث"
            lower.contains("ثاني") || lower.contains("2") || lower.contains("٢") -> base = "الثاني"
            lower.contains("اول") || lower.contains("1") || lower.contains("١") -> base = "الأول"
        }

        var branch = ""
        when {
            lower.contains("احيائي") -> branch = "الأحيائي"
            lower.contains("تطبيقي") -> branch = "التطبيقي"
            lower.contains("علمي") -> branch = "العلمي"
            lower.contains("ادبي") -> branch = "الأدبي"
            lower.contains("مهني") -> branch = "المهني"
            lower.contains("صناعي") -> branch = "الصناعي"
            lower.contains("تجاري") -> branch = "التجاري"
            lower.contains("اعدادي") || lower.contains("ثانوي") -> branch = "الإعدادي"
            lower.contains("ابتدائي") -> branch = "الابتدائي"
            lower.contains("متوسط") || base == "الأول" || base == "الثاني" || base == "الثالث" -> branch = "المتوسط"
            base == "الرابع" || base == "الخامس" || base == "السادس" -> branch = "الإعدادي"
        }

        return if (branch.isNotEmpty()) "$base $branch" else "$base المتوسط"
    }

    /**
     * Standardizes section name (أ, ب, ج, د, هـ, etc.)
     */
    fun standardizeSectionName(secStr: String?): String {
        if (secStr.isNullOrBlank()) return "أ"
        var clean = secStr.trim()
            .replace("^[\\[\\(\\{\\<\"'\\s]+|[\\]\\)\\}\\>\"'\\s]+$".toRegex(), "")
            .trim()

        clean = clean.replace("^(شعبة|الشعبة|ش|الفرع|فرع|رمز|مجموعة)\\s*[:\\-\\/\\]?\\s*".toRegex(RegexOption.IGNORE_CASE), "").trim()

        val mapToken = { token: String ->
            val t = token.trim().replace("[\\[\\(\\)\\{\\}\\<\\>\"']".toRegex(), "").lowercase()
            when (t) {
                "ا", "أ", "إ", "آ", "a", "1", "١" -> "أ"
                "ب", "b", "2", "٢" -> "ب"
                "ج", "c", "3", "٣" -> "ج"
                "د", "d", "4", "٤" -> "د"
                "ه", "هـ", "e", "5", "٥" -> "هـ"
                "و", "f", "6", "٦" -> "و"
                "ز", "z", "7", "٧" -> "ز"
                "ح", "h", "8", "٨" -> "ح"
                "ط", "9", "٩" -> "ط"
                "ي", "10", "١٠" -> "ي"
                "خ" -> "خ"
                else -> if (t.matches("^[أ-يa-zA-Z]$".toRegex())) t.uppercase() else null
            }
        }

        mapToken(clean)?.let { return it }

        val slashOrDash = "[\\/\\-]\\s*([أ-يa-zA-Z0-9]+)".toRegex().find(clean)
        if (slashOrDash != null && slashOrDash.groupValues.size > 1) {
            mapToken(slashOrDash.groupValues[1])?.let { return it }
        }

        return "أ"
    }

    /**
     * Extracts Grade and Section from a title string or sheet name
     */
    fun extractGradeAndSection(text: String?): Pair<String, String?> {
        if (text.isNullOrBlank()) return Pair("الأول المتوسط", null)
        val raw = text.trim()
        var section: String? = null

        // 1. Check for explicit keywords: شعبة ب, ش: ب, فرع ب
        val secMatch = "(?:شعبة|الشعبة|ش|الفرع|فرع)\\s*[:\\-\\/\\]?\\s*[\\[\\(]?([أ-يa-zA-Z0-9]+)[\\]\\)]?".toRegex(RegexOption.IGNORE_CASE).find(raw)
        if (secMatch != null && secMatch.groupValues.size > 1) {
            section = standardizeSectionName(secMatch.groupValues[1])
        } else {
            // 2. Bracketed: (ب) or [ج]
            val bracketMatch = "[\\[\\(\\{]([أ-يa-zA-Z0-9]+)[\\]\\)\\}]".toRegex().find(raw)
            if (bracketMatch != null && bracketMatch.groupValues.size > 1) {
                section = standardizeSectionName(bracketMatch.groupValues[1])
            }
        }

        // 3. Trailing letter: "الأول متوسط ب"
        if (section == null) {
            val trailingMatch = "[\\s\\_\\-\\/]([أ-يa-zA-Z0-9])\\s*$".toRegex().find(raw)
            if (trailingMatch != null && trailingMatch.groupValues.size > 1) {
                section = standardizeSectionName(trailingMatch.groupValues[1])
            }
        }

        // 4. Clean grade name
        var gradeText = raw
        if (section != null) {
            gradeText = raw.replace("(?:شعبة|الشعبة|ش|الفرع|فرع)?\\s*[:\\-\\/\\]\\[\\(\\)\\{\\}]?\\s*$section\\s*[\\]\\)\\}\\>]*$".toRegex(RegexOption.IGNORE_CASE), "").trim()
        }
        val grade = standardizeGradeName(if (gradeText.isNotBlank()) gradeText else raw)

        return Pair(grade, section)
    }

    /**
     * Parses all sheets in a workbook into classified classes and students
     */
    fun parseWorkbook(sheets: List<RawSheetData>, startingSequence: Int = 1): List<ParsedSchoolClass> {
        val resultClasses = mutableListOf<ParsedSchoolClass>()
        var currentSeq = startingSequence

        for (sheet in sheets) {
            val (gradeFromSheet, secFromSheet) = extractGradeAndSection(sheet.sheetName)
            var inferredGrade = gradeFromSheet
            var inferredSection = secFromSheet ?: ""

            val rows = sheet.rows
            if (rows.isEmpty()) continue

            // 1. Scan banner rows (top 15) for title
            for (r in 0 until minOf(rows.size, 15)) {
                val rowStr = rows[r].joinToString(" ")
                if (rowStr.contains("شعب") || rowStr.contains("الصف") || rowStr.contains("مرحل") || rowStr.contains("متوسط")) {
                    val (g, s) = extractGradeAndSection(rowStr)
                    if (s != null && inferredSection.isBlank()) inferredSection = s
                    if (g.isNotBlank() && secFromSheet == null && (inferredGrade == "الأول المتوسط" || inferredGrade == "الأول الابتدائي")) {
                        inferredGrade = g
                    }
                }
            }

            // 2. Locate header row
            var headerRowIndex = -1
            var nameColIdx = -1
            var firstNameColIdx = -1
            var secondNameColIdx = -1
            var thirdNameColIdx = -1
            var fourthNameColIdx = -1
            var titleColIdx = -1
            var recordNumColIdx = -1
            var gradeColIdx = -1
            var sectionColIdx = -1

            for (r in 0 until minOf(rows.size, 25)) {
                val row = rows[r]
                val rowStr = row.joinToString(" ")

                if (rowStr.contains("الاسم") || rowStr.contains("اسم الطالب") || rowStr.contains("اسم التلميذ") ||
                    rowStr.contains("الرباعي") || rowStr.contains("الثلاثي") || rowStr.contains("اسم الاب") ||
                    (rowStr.contains("ت") && (rowStr.contains("الصف") || rowStr.contains("الشعبة") || rowStr.contains("الرقم")))
                ) {
                    headerRowIndex = r
                    row.forEachIndexed { cIdx, cell ->
                        val norm = normalizeArabic(cell)
                        when {
                            norm.contains("ابالجد") || norm.contains("الرابع") || norm.contains("جدرابع") -> fourthNameColIdx = cIdx
                            norm.contains("اسمالجد") || norm.contains("الجد") || norm == "جد" -> thirdNameColIdx = cIdx
                            norm.contains("اسمالاب") || norm.contains("الاب") || norm == "اب" || norm.contains("والد") -> secondNameColIdx = cIdx
                            norm.contains("لقب") || norm.contains("عشير") || norm.contains("شهر") -> titleColIdx = cIdx
                            (norm.contains("اسماول") || norm.contains("اسمالتلميذ") || norm.contains("اسمالطالب") || norm == "اسم" || norm == "الاسم") &&
                                    !norm.contains("ام") && !norm.contains("مدرس") && !norm.contains("معلم") -> {
                                firstNameColIdx = cIdx
                                if (nameColIdx == -1) nameColIdx = cIdx
                            }
                            (norm.contains("رباعي") || norm.contains("ثلاثي") || norm.contains("كامل")) && !norm.contains("ام") -> nameColIdx = cIdx
                            norm.contains("قيد") || norm.contains("سجلعام") || norm.contains("امتحاني") || norm.contains("رقمطالب") -> recordNumColIdx = cIdx
                            norm.contains("صف") && (norm.contains("شعب") || norm.contains("فرع")) -> {
                                gradeColIdx = cIdx
                                sectionColIdx = cIdx
                            }
                            norm.contains("صف") || norm.contains("مرحل") -> gradeColIdx = cIdx
                            norm.contains("شعب") || norm.contains("شعبه") || norm == "ش" || norm.startsWith("ش/") -> sectionColIdx = cIdx
                        }
                    }
                    break
                }
            }

            // Fallback for name column if no header
            if (headerRowIndex == -1 || (nameColIdx == -1 && firstNameColIdx == -1)) {
                headerRowIndex = 0
                var bestCol = 0
                var maxArabic = 0
                val maxCols = rows.maxOfOrNull { it.size } ?: 0
                for (c in 0 until maxCols) {
                    var count = 0
                    for (row in rows) {
                        val v = if (c < row.size) row[c].trim() else ""
                        if (v.length > 5 && v.matches("^[\\u0600-\\u06FF\\s]+$".toRegex()) && v.split("\\s+".toRegex()).size >= 2) {
                            count++
                        }
                    }
                    if (count > maxArabic) {
                        maxArabic = count
                        bestCol = c
                    }
                }
                nameColIdx = bestCol
            }

            // Parse student rows
            val students = mutableListOf<ParsedStudent>()
            for (r in (headerRowIndex + 1) until rows.size) {
                val row = rows[r]
                var firstName = ""
                var secondName = ""
                var thirdName = ""
                var fourthName = ""
                var titleName = ""

                if (secondNameColIdx >= 0 || thirdNameColIdx >= 0) {
                    val rawFirst = if (firstNameColIdx >= 0 && firstNameColIdx < row.size) row[firstNameColIdx].trim()
                    else if (nameColIdx >= 0 && nameColIdx < row.size) row[nameColIdx].trim() else ""
                    secondName = if (secondNameColIdx >= 0 && secondNameColIdx < row.size) row[secondNameColIdx].trim() else ""
                    thirdName = if (thirdNameColIdx >= 0 && thirdNameColIdx < row.size) row[thirdNameColIdx].trim() else ""
                    fourthName = if (fourthNameColIdx >= 0 && fourthNameColIdx < row.size) row[fourthNameColIdx].trim() else ""
                    titleName = if (titleColIdx >= 0 && titleColIdx < row.size) row[titleColIdx].trim() else ""

                    val firstTokens = rawFirst.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    firstName = firstTokens.firstOrNull() ?: ""
                    if (secondName.isBlank() && firstTokens.size > 1) secondName = firstTokens[1]
                    if (thirdName.isBlank() && firstTokens.size > 2) thirdName = firstTokens[2]
                    if (fourthName.isBlank() && firstTokens.size > 3) fourthName = firstTokens[3]
                    if (titleName.isBlank() && firstTokens.size > 4) titleName = firstTokens.drop(4).joinToString(" ")
                } else {
                    val nameStr = if (nameColIdx >= 0 && nameColIdx < row.size) row[nameColIdx].trim()
                    else if (row.isNotEmpty()) row[0].trim() else ""

                    // Skip noise rows (Ministry headers, totals, counts)
                    if (nameStr.isBlank() || nameStr.length < 2 ||
                        nameStr.contains("الاسم") || nameStr.contains("المجموع") ||
                        nameStr.contains("العدد") || nameStr.contains("مدير") ||
                        nameStr.contains("المشرف") || nameStr.contains("وزارة التربية") ||
                        nameStr.contains("جمهورية العراق") || nameStr.contains("الملاحظات")
                    ) {
                        continue
                    }

                    val tokens = nameStr.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    if (tokens.isEmpty()) continue

                    firstName = tokens[0]
                    secondName = if (tokens.size > 1) tokens[1] else ""
                    thirdName = if (tokens.size > 2) tokens[2] else ""
                    fourthName = if (tokens.size > 3) tokens[3] else ""
                    titleName = if (tokens.size > 4) tokens.drop(4).joinToString(" ")
                    else if (titleColIdx >= 0 && titleColIdx < row.size) row[titleColIdx].trim() else ""
                }

                if (firstName.isBlank() || firstName.contains("الاسم") || firstName.contains("المجموع")) {
                    continue
                }

                val rowGrade = if (gradeColIdx >= 0 && gradeColIdx < row.size && row[gradeColIdx].isNotBlank()) {
                    extractGradeAndSection(row[gradeColIdx]).first
                } else inferredGrade

                val rowSection = if (sectionColIdx >= 0 && sectionColIdx < row.size && row[sectionColIdx].isNotBlank()) {
                    standardizeSectionName(row[sectionColIdx])
                } else if (inferredSection.isNotBlank()) inferredSection else "أ"

                val recordNum = if (recordNumColIdx >= 0 && recordNumColIdx < row.size && row[recordNumColIdx].isNotBlank()) {
                    row[recordNumColIdx].trim()
                } else {
                    (1000 + currentSeq).toString()
                }

                val fullName = listOf(firstName, secondName, thirdName, fourthName, titleName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")

                students.add(
                    ParsedStudent(
                        recordNumber = recordNum,
                        fullName = fullName,
                        firstName = firstName,
                        secondName = secondName,
                        thirdName = thirdName,
                        fourthName = fourthName,
                        titleName = titleName,
                        grade = rowGrade,
                        section = rowSection
                    )
                )
                currentSeq++
            }

            if (students.isNotEmpty()) {
                val finalGrade = inferredGrade
                val finalSection = if (inferredSection.isNotBlank()) inferredSection else students.first().section
                resultClasses.add(
                    ParsedSchoolClass(
                        sheetName = sheet.sheetName,
                        grade = finalGrade,
                        section = finalSection,
                        students = students
                    )
                )
            }
        }

        return resultClasses
    }
}
