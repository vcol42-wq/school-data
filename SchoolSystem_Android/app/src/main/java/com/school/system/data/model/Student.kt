package com.school.system.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordNumber: String,
    val fullName: String,
    val grade: String,
    val section: String,
    val subject: String,
    @Embedded val marks: StudentMarks = StudentMarks(),
    val historicalAbsences: Int = 0
)

data class StudentMarks(
    // الفصل الأول - الشهر الأول
    var m1Daily: List<Float> = List(5) { 0f },
    var m1Written: Float = 0f,
    var m1MonthAvg: Float = 0f, // Sum(m1Daily) + m1Written
    
    // الفصل الأول - الشهر الثاني
    var m2Daily: List<Float> = List(5) { 0f },
    var m2Written: Float = 0f,
    var m2MonthAvg: Float = 0f, // Sum(m2Daily) + m2Written

    var term1Avg: Float = 0f, // (m1MonthAvg + m2MonthAvg) / 2
    
    // نصف السنة
    var midtermOral: List<Float> = List(5) { 0f },
    var midtermScore: Float = 0f,
    var midtermTotal: Float = 0f, // Sum(midtermOral) + midtermScore
    var midtermFinalGrade: Float = 0f, // Result stored for display in registers

    // الفصل الثاني - الشهر الثالث
    var m3Daily: List<Float> = List(5) { 0f },
    var m3Written: Float = 0f,
    var m3MonthAvg: Float = 0f, // Sum(m3Daily) + m3Written

    // الفصل الثاني - الشهر الرابع
    var m4Daily: List<Float> = List(5) { 0f },
    var m4Written: Float = 0f,
    var m4MonthAvg: Float = 0f, // Sum(m4Daily) + m4Written

    var term2Avg: Float = 0f, // (m3MonthAvg + m4MonthAvg) / 2
    
    var annualAverage: Float = 0f, // (term1Avg + midtermFinalGrade + term2Avg) / 3
    
    // الامتحان النهائي
    var finalOral: List<Float> = List(5) { 0f },
    var finalWrittenD1: Float = 0f,
    var finalWrittenD2: Float? = null,
    
    var finalExamTotal: Float = 0f, // (Sum(finalOral) + (D2 ?: D1)) / 2
    var finalGrade: Float = 0f,     // (annualAverage + finalExamTotal) / 2
    
    var result: String = "مستمر",
    var status: String = "مستمر"
)
