package me.robiul.hisabi.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import me.robiul.hisabi.data.local.AccountEntity
import me.robiul.hisabi.data.local.TransactionEntity
import me.robiul.hisabi.data.local.TransactionType
import java.io.OutputStream

object PdfReportGenerator {

    fun writeFinancialReportPdf(
        outputStream: OutputStream,
        periodLabel: String,
        currencySymbol: String,
        datePattern: String,
        totalLiquidCents: Long,
        totalCreditDebtCents: Long,
        incomeCents: Long,
        expenseCents: Long,
        categoryBreakdown: List<Pair<String, Long>>,
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
    ) {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.rgb(16, 24, 39)
            textSize = 20f
            isFakeBoldText = true
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(30, 158, 112)
            textSize = 13f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f
        }
        val mutedPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9.5f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
        }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 52f

        fun newPageIfNeeded(requiredHeight: Float = 24f) {
            if (y + requiredHeight > pageHeight - 40f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 48f
            }
        }

        // Header
        canvas.drawText("Hisabi Financial Report", margin, y, titlePaint)
        y += 18f
        canvas.drawText("Period: $periodLabel  |  Generated offline", margin, y, mutedPaint)
        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 22f

        // Summary
        canvas.drawText("ACCOUNT & CASHFLOW SUMMARY", margin, y, sectionPaint)
        y += 18f
        val netCashflow = incomeCents - expenseCents
        val summaryRows = listOf(
            "Total Liquid Balance" to Formatters.formatMoney(totalLiquidCents, currencySymbol),
            "Credit Card Payable" to Formatters.formatMoney(totalCreditDebtCents, currencySymbol),
            "Period Income" to Formatters.formatMoney(incomeCents, currencySymbol, showPlus = true),
            "Period Expenses" to Formatters.formatMoney(-expenseCents, currencySymbol),
            "Net Cash Flow" to Formatters.formatMoney(netCashflow, currencySymbol, showPlus = netCashflow > 0),
        )
        for ((label, value) in summaryRows) {
            canvas.drawText(label, margin, y, bodyPaint)
            canvas.drawText(value, margin + 260f, y, bodyPaint)
            y += 16f
        }

        y += 12f
        if (categoryBreakdown.isNotEmpty()) {
            newPageIfNeeded(40f)
            canvas.drawText("EXPENSE CATEGORY BREAKDOWN", margin, y, sectionPaint)
            y += 18f
            for ((catName, amount) in categoryBreakdown.take(12)) {
                newPageIfNeeded(18f)
                canvas.drawText(catName, margin, y, bodyPaint)
                canvas.drawText(Formatters.formatMoney(amount, currencySymbol), margin + 260f, y, bodyPaint)
                y += 15f
            }
            y += 12f
        }

        // Transactions Table
        newPageIfNeeded(48f)
        canvas.drawText("TRANSACTION STATEMENT (${transactions.size} ENTRIES)", margin, y, sectionPaint)
        y += 16f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 14f

        val accountMap = accounts.associateBy { it.id }
        for (tx in transactions) {
            newPageIfNeeded(20f)
            val dateStr = Formatters.formatDateOnly(tx.timestamp, datePattern)
            val accName = accountMap[tx.accountId]?.name ?: "Account"
            val titleClip = tx.title.take(28)
            val catClip = tx.categoryNameSnapshot.ifBlank { tx.type.label }.take(16)
            val amtStr = when (tx.type) {
                TransactionType.INCOME -> Formatters.formatMoney(tx.amountCents, currencySymbol, showPlus = true)
                TransactionType.EXPENSE -> Formatters.formatMoney(-(tx.amountCents + tx.feeCents), currencySymbol)
                else -> Formatters.formatMoney(tx.amountCents, currencySymbol)
            }

            canvas.drawText(dateStr, margin, y, mutedPaint)
            canvas.drawText(titleClip, margin + 84f, y, bodyPaint)
            canvas.drawText("$catClip • ${accName.take(12)}", margin + 245f, y, mutedPaint)
            canvas.drawText(amtStr, pageWidth - margin - 85f, y, bodyPaint)
            y += 16f
        }

        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }
}
