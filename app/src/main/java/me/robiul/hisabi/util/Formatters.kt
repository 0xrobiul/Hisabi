package me.robiul.hisabi.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

object Formatters {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun formatMoney(
        cents: Long,
        currencySymbol: String,
        masked: Boolean = false,
        showPlus: Boolean = false,
    ): String {
        if (masked) return "$currencySymbol••••••"
        val isNeg = cents < 0L
        val absCents = abs(cents)
        val wholePart = absCents / 100L
        val fracPart = (absCents % 100L).toInt()
        val wholeFormatted = formatWithGrouping(wholePart)
        val formatted = "$wholeFormatted.${fracPart.toString().padStart(2, '0')}"
        return when {
            isNeg -> "-$currencySymbol$formatted"
            showPlus && cents > 0L -> "+$currencySymbol$formatted"
            else -> "$currencySymbol$formatted"
        }
    }

    fun formatCompactMoney(cents: Long, currencySymbol: String): String {
        val isNeg = cents < 0L
        val absCents = abs(cents)
        val sign = if (isNeg) "-" else ""
        return when {
            absCents >= 100_000_000L -> {
                val tenths = (absCents / 10_000_000L)
                "$sign$currencySymbol${tenths / 10}.${tenths % 10}M"
            }
            absCents >= 1_000_000L -> {
                val tenths = (absCents / 10_000L)
                "$sign$currencySymbol${tenths / 10}.${tenths % 10}K"
            }
            else -> "$sign$currencySymbol${formatWithGrouping(absCents / 100L)}"
        }
    }

    fun centsToEditableString(cents: Long): String {
        val absCents = abs(cents)
        val whole = absCents / 100L
        val frac = (absCents % 100L).toInt()
        val sign = if (cents < 0L) "-" else ""
        return "$sign$whole.${frac.toString().padStart(2, '0')}"
    }

    /**
     * Parses user currency input into exact integer minor units (paisa/cents) using BigDecimal.
     */
    fun parseAmountToCents(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return runCatching {
            val bd = BigDecimal(cleaned)
            bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        }.getOrNull()
    }

    fun computePercentageCents(amountCents: Long, basisPoints: Int): Long {
        if (amountCents <= 0L || basisPoints <= 0) return 0L
        return ((amountCents * basisPoints.toLong()) + 5_000L) / 10_000L
    }

    private fun formatWithGrouping(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val sb = StringBuilder()
        var count = 0
        for (i in str.length - 1 downTo 0) {
            sb.append(str[i])
            count++
            if (count % 3 == 0 && i > 0) sb.append(',')
        }
        return sb.reverse().toString()
    }

    fun formatDateTime(timestampMs: Long, datePattern: String = "dd MMM yyyy"): String {
        val instant = Instant.ofEpochMilli(timestampMs)
        val zdt = instant.atZone(zone)
        val today = LocalDate.now(zone)
        val date = zdt.toLocalDate()
        val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val timeStr = zdt.format(timeFmt)

        return when (date) {
            today -> "Today, $timeStr"
            today.minusDays(1) -> "Yesterday, $timeStr"
            else -> {
                val df = runCatching {
                    DateTimeFormatter.ofPattern(datePattern, Locale.US)
                }.getOrDefault(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
                "${date.format(df)}, $timeStr"
            }
        }
    }

    fun formatDateOnly(timestampMs: Long, datePattern: String = "dd MMM yyyy"): String {
        val date = Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
        val df = runCatching {
            DateTimeFormatter.ofPattern(datePattern, Locale.US)
        }.getOrDefault(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US))
        return date.format(df)
    }

    fun formatGroupDateHeader(localDate: LocalDate, datePattern: String = "dd MMM yyyy"): String {
        val today = LocalDate.now(zone)
        return when (localDate) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> {
                val df = runCatching {
                    DateTimeFormatter.ofPattern("EEE, $datePattern", Locale.US)
                }.getOrDefault(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.US))
                localDate.format(df)
            }
        }
    }

    fun toLocalDate(timestampMs: Long): LocalDate {
        return Instant.ofEpochMilli(timestampMs).atZone(zone).toLocalDate()
    }

    /**
     * Returns [startMs, endMs) for a custom billing month starting on [firstDayOfMonth].
     */
    fun billingMonthBounds(
        year: Int,
        monthOneBased: Int,
        firstDayOfMonth: Int = 1,
    ): Pair<Long, Long> {
        val ym = YearMonth.of(year, monthOneBased)
        val clampedDay = firstDayOfMonth.coerceIn(1, ym.lengthOfMonth())
        val start = ym.atDay(clampedDay).atStartOfDay(zone)
        val nextYm = ym.plusMonths(1)
        val nextClampedDay = firstDayOfMonth.coerceIn(1, nextYm.lengthOfMonth())
        val end = nextYm.atDay(nextClampedDay).atStartOfDay(zone)
        return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
    }

    fun currentWeekBounds(): Pair<Long, Long> {
        val today = LocalDate.now(zone)
        val monday = today.minusDays(((today.dayOfWeek.value - 1).toLong()))
        val nextMonday = monday.plusWeeks(1)
        return monday.atStartOfDay(zone).toInstant().toEpochMilli() to
            nextMonday.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun currentYearBounds(year: Int = LocalDate.now(zone).year): Pair<Long, Long> {
        val start = LocalDate.of(year, 1, 1).atStartOfDay(zone)
        val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone)
        return start.toInstant().toEpochMilli() to end.toInstant().toEpochMilli()
    }

    fun formatBillingMonthLabel(year: Int, monthOneBased: Int): String {
        val ym = YearMonth.of(year, monthOneBased)
        return ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    }

    fun computeNextDueDate(
        fromMs: Long,
        frequency: me.robiul.hisabi.data.local.RecurringFrequency,
        intervalCount: Int = 1,
    ): Long {
        val count = intervalCount.coerceAtLeast(1).toLong()
        val zdt = Instant.ofEpochMilli(fromMs).atZone(zone)
        val next = when (frequency) {
            me.robiul.hisabi.data.local.RecurringFrequency.DAILY -> zdt.plusDays(count)
            me.robiul.hisabi.data.local.RecurringFrequency.WEEKLY -> zdt.plusWeeks(count)
            me.robiul.hisabi.data.local.RecurringFrequency.MONTHLY -> zdt.plusMonths(count)
            me.robiul.hisabi.data.local.RecurringFrequency.YEARLY -> zdt.plusYears(count)
            me.robiul.hisabi.data.local.RecurringFrequency.CUSTOM_DAYS -> zdt.plusDays(count)
        }
        return next.toInstant().toEpochMilli()
    }
}
