package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TallySessionState {
    READY,
    ACTIVE,
    PAUSED
}

@Entity(tableName = "zikr_entries")
data class ZikrEntry(
    @PrimaryKey val id: String,
    val name: String,
    val arabic: String = "",
    val meaning: String = "",
    val count: Int = 0,
    val target: Int = 33,
    val roundsCompleted: Int = 0,
    val totalCount: Int = 0,
    val elapsedSeconds: Long = 0L,
    val sessionState: String = TallySessionState.READY.name,
    val isCustom: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val parsedSessionState: TallySessionState
        get() = runCatching { TallySessionState.valueOf(sessionState) }
            .getOrDefault(TallySessionState.READY)
}

val DefaultZikrSeeds: List<ZikrEntry> = listOf(
    ZikrEntry(
        id = "subhanallah",
        name = "SubhanAllah",
        arabic = "سُبْحَانَ اللَّهِ",
        meaning = "Glory be to Allah",
        target = 33,
        isCustom = false,
        sortOrder = 0
    ),
    ZikrEntry(
        id = "alhamdulillah",
        name = "Alhamdulillah",
        arabic = "الْحَمْدُ لِلَّهِ",
        meaning = "All praise is due to Allah",
        target = 33,
        isCustom = false,
        sortOrder = 1
    ),
    ZikrEntry(
        id = "allahu_akbar",
        name = "Allahu Akbar",
        arabic = "اللَّهُ أَكْبَرُ",
        meaning = "Allah is the Greatest",
        target = 34,
        isCustom = false,
        sortOrder = 2
    ),
    ZikrEntry(
        id = "astaghfirullah",
        name = "Astaghfirullah",
        arabic = "أَسْتَغْفِرُ اللَّهَ",
        meaning = "I seek forgiveness from Allah",
        target = 100,
        isCustom = false,
        sortOrder = 3
    ),
    ZikrEntry(
        id = "tahlil",
        name = "La ilaha illallah",
        arabic = "لَا إِلَٰهَ إِلَّا اللَّهُ",
        meaning = "There is no deity worthy of worship except Allah",
        target = 100,
        isCustom = false,
        sortOrder = 4
    ),
    ZikrEntry(
        id = "salawat",
        name = "Salawat",
        arabic = "اللَّهُمَّ صَلِّ عَلَىٰ مُحَمَّدٍ",
        meaning = "O Allah, send blessings upon Muhammad",
        target = 100,
        isCustom = false,
        sortOrder = 5
    )
)

val QuickTargetOptions: List<Int> = listOf(33, 99, 100, 0)
