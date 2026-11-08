package com.calias.tv.app.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.calias.tv.app.R

/**
 * The fonts the user can pick in Settings. The three bundled ones are variable fonts (one file, any
 * weight), all under the SIL Open Font License (see THIRD_PARTY_LICENSES.md). SYSTEM is Android's default font.
 */
enum class AppFont(val label: String, val family: FontFamily) {
    MANROPE("Manrope", variable(R.font.manrope)),
    INTER("Inter", variable(R.font.inter)),
    FIGTREE("Figtree", variable(R.font.figtree)),
    SYSTEM("System", FontFamily.Default),
}

private val WEIGHTS = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private fun variable(resId: Int): FontFamily = FontFamily(
    WEIGHTS.map { weight ->
        Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)
