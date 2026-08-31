package cz.hlidacspoju.android.ui

import androidx.compose.runtime.compositionLocalOf
import cz.hlidacspoju.android.model.AppLanguage

/**
 * Minimal in-app localization. Mirrors the Windows app's LocalizationManager but adapted to
 * Compose: strings are looked up by key for the currently selected [AppLanguage] via [LocalStrings].
 * Czech and English are fully translated; Russian/German/French currently fall back to English
 * until translated (matching the Windows app's language list so users can already pick them).
 */
private val Cs = mapOf(
    "app_title" to "Hlídač spojů",
    "settings" to "Nastavení",
    "back" to "Zpět",
    "add_connection" to "Přidat spoj",
    "no_connections" to "Zatím nemáte žádné sledované spoje.",
    "not_scheduled_today" to "Na dnešek není nastaveno hlídání spoje",
    "no_current_departures" to "Žádné aktuální spoje v okně",
    "window_passed_today" to "Dnes již žádný zbývající spoj v nastaveném okně",
    "next_departure_on_day" to "Další spoj v nastaveném okně jede v %1\$s",
    "no_departure_found_in_window" to "V zadaném okně nenalezen žádný spoj",
    "delayed_on_time" to "Zpožděno: %1\$d, na čas: %2\$d",
    "delete" to "Smazat",
    "edit_connection" to "Upravit spoj",
    "stop" to "Zastávka",
    "line" to "Linka",
    "direction" to "Směr",
    "time_from" to "Od (HH:mm)",
    "time_to" to "Do (HH:mm)",
    "custom_name" to "Vlastní název (nepovinné)",
    "days_of_week" to "Dny v týdnu",
    "save" to "Uložit",
    "cancel" to "Zrušit",
    "skip" to "Přeskočit",
    "welcome_title" to "Vítejte v Hlídači spojů",
    "welcome_body" to "Aby aplikace mohla sledovat zpoždění, potřebuje bezplatný API klíč od Golemio (Pražská datová platforma).",
    "open_golemio_registration" to "Otevřít registraci Golemio",
    "golemio_api_key" to "Golemio API klíč",
    "finish" to "Dokončit",
    "poll_interval" to "Kontrola každých (sekund)",
    "check_now" to "Zkontrolovat nyní",
    "language" to "Jazyk",
    "theme" to "Motiv",
    "theme_system" to "Podle systému",
    "theme_light" to "Světlý",
    "theme_dark" to "Tmavý",
    "logging_enabled" to "Povolit logování (vypněte před publikováním)",
    "day_mon" to "Po", "day_tue" to "Út", "day_wed" to "St", "day_thu" to "Čt",
    "day_fri" to "Pá", "day_sat" to "So", "day_sun" to "Ne",
    "connection_detail" to "Detail spoje",
    "not_departed" to "Ještě nevyjelo",
    "close" to "Zavřít",
    "edit" to "Upravit",
    "notification_lead_time" to "Upozornit předem (minut)",
    "notification_channel_service_name" to "Sledování spojů",
    "notification_channel_delay_name" to "Zpoždění spojů",
    "notification_service_title" to "Hlídač spojů",
    "notification_service_text" to "Sledování zpoždění je aktivní",
    "notification_line_title" to "Linka %1\$s – %2\$s",
    "notification_delayed_text" to "Odjezd %1\$s v %2\$s má zpoždění %3\$d min.",
    "notification_on_time_text" to "Odjezd %1\$s v %2\$s jede na čas.",
    "notification_other_on_time_suffix" to " Ostatní spoje této linky jedou na čas."
)

private val En = mapOf(
    "app_title" to "Connection Watcher",
    "settings" to "Settings",
    "back" to "Back",
    "add_connection" to "Add connection",
    "no_connections" to "You have no watched connections yet.",
    "not_scheduled_today" to "Not scheduled for monitoring today",
    "no_current_departures" to "No current departures in this window",
    "window_passed_today" to "No more connections left in the configured window today",
    "next_departure_on_day" to "Next departure in the configured window is on %1\$s",
    "no_departure_found_in_window" to "No departure found in the configured window",
    "delayed_on_time" to "Delayed: %1\$d, on time: %2\$d",
    "delete" to "Delete",
    "edit_connection" to "Edit connection",
    "stop" to "Stop",
    "line" to "Line",
    "direction" to "Direction",
    "time_from" to "From (HH:mm)",
    "time_to" to "To (HH:mm)",
    "custom_name" to "Custom name (optional)",
    "days_of_week" to "Days of week",
    "save" to "Save",
    "cancel" to "Cancel",
    "welcome_title" to "Welcome to Hlídač spojů",
    "welcome_body" to "To track delays, the app needs a free API key from Golemio (Prague data platform).",
    "open_golemio_registration" to "Open Golemio registration",
    "golemio_api_key" to "Golemio API key",
    "finish" to "Finish",
    "skip" to "Skip",
    "poll_interval" to "Check every (seconds)",
    "check_now" to "Check now",
    "language" to "Language",
    "theme" to "Theme",
    "theme_system" to "System default",
    "theme_light" to "Light",
    "theme_dark" to "Dark",
    "logging_enabled" to "Enable logging (turn off before publishing)",
    "day_mon" to "Mon", "day_tue" to "Tue", "day_wed" to "Wed", "day_thu" to "Thu",
    "day_fri" to "Fri", "day_sat" to "Sat", "day_sun" to "Sun",
    "connection_detail" to "Connection detail",
    "not_departed" to "Not yet departed",
    "close" to "Close",
    "edit" to "Edit",
    "notification_lead_time" to "Notify ahead by (minutes)",
    "notification_channel_service_name" to "Connection monitoring",
    "notification_channel_delay_name" to "Connection delays",
    "notification_service_title" to "Hlídač spojů",
    "notification_service_text" to "Delay monitoring is active",
    "notification_line_title" to "Line %1\$s – %2\$s",
    "notification_delayed_text" to "Departure %1\$s at %2\$s is delayed by %3\$d min.",
    "notification_on_time_text" to "Departure %1\$s at %2\$s is on time.",
    "notification_other_on_time_suffix" to " Other departures on this line are on time."
)

private val translations: Map<AppLanguage, Map<String, String>> = mapOf(
    AppLanguage.CZECH to Cs,
    AppLanguage.ENGLISH to En,
    AppLanguage.RUSSIAN to En,
    AppLanguage.GERMAN to En,
    AppLanguage.FRENCH to En
)

class Strings(private val language: AppLanguage) {
    fun get(key: String): String =
        translations[language]?.get(key) ?: Cs[key] ?: key

    fun get(key: String, vararg args: Any): String =
        String.format(get(key), *args)

    operator fun invoke(key: String): String = get(key)
}

val LocalStrings = compositionLocalOf { Strings(AppLanguage.CZECH) }
