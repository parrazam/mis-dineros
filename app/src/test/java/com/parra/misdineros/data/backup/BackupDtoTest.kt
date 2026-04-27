package com.parra.misdineros.data.backup

import com.parra.misdineros.designsystem.theme.AppTheme
import com.parra.misdineros.domain.model.AppSettings
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Category
import com.parra.misdineros.domain.model.FxRate
import com.parra.misdineros.domain.model.Subscription
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BackupDtoTest {

    private val ts = 1745712000000L
    private val date = LocalDate.of(2026, 5, 1)

    private fun sub(iconRef: String = "bundled:netflix") = Subscription(
        id = "sub1", name = "Netflix", iconRef = iconRef,
        amountMinor = 1299L, currencyCode = "EUR",
        billingCycle = BillingCycle.MONTHLY, nextRenewalDate = date,
        categoryId = "cat1", isPaused = false,
        notifyDaysBefore = 3, notes = "nota", createdAt = ts, updatedAt = ts,
    )

    private fun cat() = Category(
        id = "cat1", name = "Streaming", iconKey = "play_circle",
        colorArgb = 0xFFE53935.toInt(), isBuiltIn = true, sortOrder = 1,
    )

    // ─── Subscription ─────────────────────────────────────────────────────────

    @Test
    fun `subscription round-trip domain-dto-domain preserves todos los campos`() {
        assertEquals(sub(), sub().toDto().toDomain())
    }

    @Test
    fun `subscription toDto serializa billingCycle como nombre del enum`() {
        assertEquals("MONTHLY", sub().toDto().billingCycle)
        assertEquals("ANNUAL", sub().copy(billingCycle = BillingCycle.ANNUAL).toDto().billingCycle)
    }

    @Test
    fun `subscription toDto serializa nextRenewalDate como ISO`() {
        assertEquals("2026-05-01", sub().toDto().nextRenewalDate)
    }

    @Test
    fun `subscription toDomain acepta iconRef sobrescrito`() {
        val dto = sub("asset:icon.png").toDto()
        val resolved = dto.toDomain(resolvedIconRef = "file:/data/icons/icon.png")
        assertEquals("file:/data/icons/icon.png", resolved.iconRef)
    }

    @Test
    fun `subscription con campos opcionales null round-trip`() {
        val noOptionals = sub().copy(notifyDaysBefore = null, notes = null)
        assertEquals(noOptionals, noOptionals.toDto().toDomain())
    }

    // ─── Category ─────────────────────────────────────────────────────────────

    @Test
    fun `category round-trip domain-dto-domain`() {
        assertEquals(cat(), cat().toDto().toDomain())
    }

    @Test
    fun `category isBuiltIn false round-trip`() {
        val custom = cat().copy(isBuiltIn = false, id = "custom1")
        assertEquals(custom, custom.toDto().toDomain())
    }

    // ─── FxRate ───────────────────────────────────────────────────────────────

    @Test
    fun `fxRate round-trip domain-dto-domain`() {
        val fx = FxRate(base = "EUR", quote = "USD", rate = 1.08, updatedAt = ts)
        assertEquals(fx, fx.toDto().toDomain())
    }

    // ─── AppSettings ──────────────────────────────────────────────────────────

    @Test
    fun `appSettings round-trip con valores por defecto`() {
        val s = AppSettings()
        assertEquals(s, s.toDto().toDomain())
    }

    @Test
    fun `appSettings round-trip con todos los temas`() {
        for (theme in AppTheme.entries) {
            val s = AppSettings(appTheme = theme)
            assertEquals(s, s.toDto().toDomain())
        }
    }

    @Test
    fun `appSettings round-trip con configuracion personalizada`() {
        val s = AppSettings(
            globalCurrencyCode = "USD", notificationsEnabled = false,
            notificationHour = 22, defaultNotifyDaysBefore = 7,
            monthlySummaryEnabled = false, appTheme = AppTheme.DARK,
        )
        assertEquals(s, s.toDto().toDomain())
    }

    // ─── BackupJson serialización ─────────────────────────────────────────────

    @Test
    fun `backupJson serializa y deserializa sin perdida de datos`() {
        val original = BackupJson(
            exportedAt = "2026-04-27T10:00:00Z",
            subscriptions = listOf(sub().toDto()),
            categories = listOf(cat().toDto()),
            fxRates = listOf(FxRate("EUR", "USD", 1.08, ts).toDto()),
            settings = AppSettings().toDto(),
            assets = mapOf("icon.png" to "base64data=="),
        )
        val decoded = Json.decodeFromString<BackupJson>(Json.encodeToString(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `backupJson ignoraUnknownKeys al deserializar`() {
        val jsonConCampoExtra = """
            {"version":1,"exportedAt":"2026-04-27T10:00:00Z",
             "subscriptions":[],"categories":[],"fxRates":[],
             "settings":{"globalCurrencyCode":"EUR","notificationsEnabled":true,
               "notificationHour":9,"defaultNotifyDaysBefore":3,
               "monthlySummaryEnabled":true,"appTheme":"SYSTEM"},
             "campoDesconocido":"ignorado"}
        """.trimIndent()
        val lenient = Json { ignoreUnknownKeys = true }
        val result = lenient.decodeFromString<BackupJson>(jsonConCampoExtra)
        assertEquals(1, result.version)
        assertEquals("EUR", result.settings.globalCurrencyCode)
    }

    @Test
    fun `backupJson assets vacio por defecto`() {
        val backup = BackupJson(
            exportedAt = "2026-04-27T10:00:00Z",
            subscriptions = emptyList(), categories = emptyList(),
            fxRates = emptyList(), settings = AppSettings().toDto(),
        )
        assertEquals(emptyMap<String, String>(), backup.assets)
    }
}
