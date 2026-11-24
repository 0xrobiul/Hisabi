package me.robiul.hisabi.util

object InputValidation {
    /**
     * Filters currency/decimal text to digits, optional single decimal point (up to 2 decimals),
     * and optional leading minus sign when [allowNegative] is true.
     */
    fun sanitizeDecimalInput(raw: String, allowNegative: Boolean = false): String {
        val cleaned = raw.replace(",", "")
        val sb = StringBuilder()
        var seenDot = false
        var decimals = 0
        for (i in cleaned.indices) {
            val ch = cleaned[i]
            when {
                ch == '-' && allowNegative && i == 0 && sb.isEmpty() -> sb.append(ch)
                ch.isDigit() -> {
                    if (seenDot) {
                        if (decimals < 2) {
                            sb.append(ch)
                            decimals++
                        }
                    } else if (sb.length < 12) {
                        sb.append(ch)
                    }
                }
                ch == '.' && !seenDot -> {
                    if (sb.isEmpty() || (sb.length == 1 && sb[0] == '-')) {
                        sb.append('0')
                    }
                    sb.append('.')
                    seenDot = true
                }
            }
        }
        return sb.toString()
    }

    fun sanitizeDigitsOnly(raw: String, maxLength: Int): String {
        return raw.filter { it.isDigit() }.take(maxLength)
    }

    fun validatePositiveAmountError(
        input: String,
        currencySymbol: String = "৳",
        minCents: Long = 1L,
        minErrorLabel: String? = null,
        maxCents: Long? = null,
        maxErrorLabel: String? = null,
    ): String? {
        if (input.isBlank()) return "Amount is required"
        val cents = Formatters.parseAmountToCents(input) ?: return "Enter a valid amount"
        if (cents <= 0L) return "Amount must be greater than 0"
        if (cents < minCents) {
            return minErrorLabel ?: "Minimum allowed is ${Formatters.formatMoney(minCents, currencySymbol)}"
        }
        if (maxCents != null && cents > maxCents) {
            return maxErrorLabel ?: "Cannot exceed ${Formatters.formatMoney(maxCents, currencySymbol)}"
        }
        return null
    }

    fun validateOptionalNonNegativeAmountError(input: String): String? {
        if (input.isBlank()) return null
        val cents = Formatters.parseAmountToCents(input) ?: return "Enter a valid number"
        return if (cents < 0L) "Cannot be negative" else null
    }

    fun validateLastFourDigitsError(input: String): String? {
        if (input.isEmpty()) return null
        return if (input.length == 4 && input.all { it.isDigit() }) null else "Enter all 4 digits or leave empty"
    }

    fun validatePinPair(pin: String, confirmPin: String): Pair<String?, String?> {
        val pinErr = if (pin.isNotEmpty() && pin.length < 4) "PIN must be 4 digits" else null
        val confirmErr = if (confirmPin.isNotEmpty() && pin != confirmPin) "PINs do not match" else null
        return pinErr to confirmErr
    }
}
