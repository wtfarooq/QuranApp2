package com.example.quranapp2.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class QuranData(val id: Int, val name: String, val description: String, val pageNumber: Int)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "quran.db"
        private const val DATABASE_VERSION = 4

        private const val TABLE_JUZ = "juz"
        private const val TABLE_SURAH = "surah"
        private const val TABLE_PAGE_TIMING = "page_timing"
        private const val TABLE_JUZ_COMPLETIONS = "juz_completions"

        private const val COL_ID = "id"
        private const val COL_NAME = "name"
        private const val COL_DESCRIPTION = "description"
        private const val COL_PAGE_NUMBER = "page_number"
        private const val COL_PAGE = "page"
        private const val COL_ACTIVE_MS = "active_ms"
        private const val COL_JUZ = "juz"
        private const val COL_COMPLETED_AT = "completed_at"
        private const val COL_TOTAL_MS = "total_ms"
        private const val COL_PAGE_COUNT = "page_count"

        private const val MIN_READ_TIME_MS = 1_000L

        fun juzForPage(page: Int): Int {
            if (page <= 21) return 1
            return minOf(((page - 2) / 20) + 1, 30)
        }

        fun juzStartPage(juz: Int): Int = if (juz <= 1) 1 else (juz - 1) * 20 + 2

        fun juzEndPage(juz: Int): Int = if (juz >= 30) 604 else juzStartPage(juz + 1) - 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_JUZ ($COL_ID INTEGER PRIMARY KEY, $COL_NAME TEXT, $COL_DESCRIPTION TEXT, $COL_PAGE_NUMBER INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_SURAH ($COL_ID INTEGER PRIMARY KEY, $COL_NAME TEXT, $COL_DESCRIPTION TEXT, $COL_PAGE_NUMBER INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_PAGE_TIMING ($COL_PAGE INTEGER PRIMARY KEY, $COL_ACTIVE_MS INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE $TABLE_JUZ_COMPLETIONS ($COL_JUZ INTEGER PRIMARY KEY, $COL_COMPLETED_AT INTEGER NOT NULL, $COL_TOTAL_MS INTEGER NOT NULL, $COL_PAGE_COUNT INTEGER NOT NULL)")

        populateJuzTable(db)
        populateSurahTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_JUZ")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SURAH")
        db.execSQL("CREATE TABLE $TABLE_JUZ ($COL_ID INTEGER PRIMARY KEY, $COL_NAME TEXT, $COL_DESCRIPTION TEXT, $COL_PAGE_NUMBER INTEGER)")
        db.execSQL("CREATE TABLE $TABLE_SURAH ($COL_ID INTEGER PRIMARY KEY, $COL_NAME TEXT, $COL_DESCRIPTION TEXT, $COL_PAGE_NUMBER INTEGER)")
        populateJuzTable(db)
        populateSurahTable(db)

        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_PAGE_TIMING ($COL_PAGE INTEGER PRIMARY KEY, $COL_ACTIVE_MS INTEGER DEFAULT 0)")
            db.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_JUZ_COMPLETIONS ($COL_JUZ INTEGER PRIMARY KEY, $COL_COMPLETED_AT INTEGER NOT NULL, $COL_TOTAL_MS INTEGER NOT NULL, $COL_PAGE_COUNT INTEGER NOT NULL)")
        }
    }

    private fun populateJuzTable(db: SQLiteDatabase) {
        val juzNames = arrayOf(
            "Alif Lam Meem",
            "Sayaqool",
            "Tilkal Rusul",
            "Lan Tana Loo",
            "Wal Mohsanat",
            "La Yuhibbullah",
            "Wa Iza Samiu",
            "Wa Lau Annana",
            "Qalal Malao",
            "Wa A'lamu",
            "Yatazeroon",
            "Wa Mamin Da'abat",
            "Wa Ma Ubrioo",
            "Rubama",
            "Subhanallazi",
            "Qal Alam",
            "Aqtarabo",
            "Qadd Aflaha",
            "Wa Qalallazina",
            "A'man Khalaq",
            "Utlu Ma Oohi",
            "Wa Manyaqnut",
            "Wa Mali",
            "Faman Azlam",
            "Elahe Yuruddo",
            "Ha'a Meem",
            "Qala Fama Khatbukum",
            "Qadd Sami Allah",
            "Tabarakallazi",
            "Amma Yatasa'aloon"
        )
        val startsAt = arrayOf(
            "Surah Al-Fatiha, Ayah 1",
            "Surah Al-Baqarah, Ayah 142",
            "Surah Al-Baqarah, Ayah 253",
            "Surah Al-Imran, Ayah 93",
            "Surah An-Nisa, Ayah 24",
            "Surah An-Nisa, Ayah 148",
            "Surah Al-Ma'idah, Ayah 82",
            "Surah Al-An'am, Ayah 111",
            "Surah Al-A'raf, Ayah 88",
            "Surah Al-Anfal, Ayah 41",
            "Surah At-Tauba, Ayah 93",
            "Surah Hud, Ayah 6",
            "Surah Yusuf, Ayah 53",
            "Surah Al-Hijr, Ayah 1",
            "Surah Al-Isra (or Bani Isra'il), Ayah 1",
            "Surah Al-Kahf, Ayah 75",
            "Surah Al-Anbiyaa, Ayah 1",
            "Surah Al-Muminum, Ayah 1",
            "Surah Al-Furqan, Ayah 21",
            "Surah An-Naml, Ayah 56",
            "Surah Al-Ankabut, Ayah 46",
            "Surah Al-Azhab, Ayah 31",
            "Surah Ya-Sin, Ayah 28",
            "Surah Az-Zumar, Ayah 32",
            "Surah Fussilat, Ayah 47",
            "Surah Al-Ahqaf, Ayah 1",
            "Surah Az-Dhariyat, Ayah 31",
            "Surah Al-Mujadilah, Ayah 1",
            "Surah Al-Mulk, Ayah 1",
            "Surah An-Naba, Ayah 1"
        )
        for (i in juzNames.indices) {
            val values = ContentValues()
            values.put(COL_ID, i + 1)
            values.put(COL_NAME, juzNames[i])
            values.put(COL_DESCRIPTION, startsAt[i])
            values.put(COL_PAGE_NUMBER, if (i == 0) 1 else (i) * 20 + 2)
            db.insert(TABLE_JUZ, null, values)
        }
    }

    private fun populateSurahTable(db: SQLiteDatabase) {
        val surahNames = arrayOf(
            "Al-Fatihah (the Opening)",
            "Al-Baqarah (the Cow)",
            "Aali Imran (the Family of Imran)",
            "An-Nisa’ (the Women)",
            "Al-Ma’idah (the Table)",
            "Al-An’am (the Cattle)",
            "Al-A’raf (the Heights)",
            "Al-Anfal (the Spoils of War)",
            "At-Taubah (the Repentance)",
            "Yunus (Yunus)",
            "Hud (Hud)",
            "Yusuf (Yusuf)",
            "Ar-Ra’d (the Thunder)",
            "Ibrahim (Ibrahim)",
            "Al-Hijr (the Rocky Tract)",
            "An-Nahl (the Bees)",
            "Al-Isra’ (the Night Journey)",
            "Al-Kahf (the Cave)",
            "Maryam (Maryam)",
            "Ta-Ha (Ta-Ha)",
            "Al-Anbiya’ (the Prophets)",
            "Al-Haj (the Pilgrimage)",
            "Al-Mu’minun (the Believers)",
            "An-Nur (the Light)",
            "Al-Furqan (the Criterion)",
            "Ash-Shu’ara’ (the Poets)",
            "An-Naml (the Ants)",
            "Al-Qasas (the Stories)",
            "Al-Ankabut (the Spider)",
            "Ar-Rum (the Romans)",
            "Luqman (Luqman)",
            "As-Sajdah (the Prostration)",
            "Al-Ahzab (the Combined Forces)",
            "Saba’ (the Sabeans)",
            "Al-Fatir (the Originator)",
            "Ya-Sin (Ya-Sin)",
            "As-Saffah (Those Ranges in Ranks)",
            "Sad (Sad)",
            "Az-Zumar (the Groups)",
            "Ghafar (the Forgiver)",
            "Fusilat (Distinguished)",
            "Ash-Shura (the Consultation)",
            "Az-Zukhruf (the Gold)",
            "Ad-Dukhan (the Smoke)",
            "Al-Jathiyah (the Kneeling)",
            "Al-Ahqaf (the Valley)",
            "Muhammad (Muhammad)",
            "Al-Fat’h (the Victory)",
            "Al-Hujurat (the Dwellings)",
            "Qaf (Qaf)",
            "Adz-Dzariyah (the Scatterers)",
            "At-Tur (the Mount)",
            "An-Najm (the Star)",
            "Al-Qamar (the Moon)",
            "Ar-Rahman (the Most Gracious)",
            "Al-Waqi’ah (the Event)",
            "Al-Hadid (the Iron)",
            "Al-Mujadilah (the Reasoning)",
            "Al-Hashr (the Gathering)",
            "Al-Mumtahanah (the Tested)",
            "As-Saf (the Row)",
            "Al-Jum’ah (Friday)",
            "Al-Munafiqun (the Hypocrites)",
            "At-Taghabun (the Loss & Gain)",
            "At-Talaq (the Divorce)",
            "At-Tahrim (the Prohibition)",
            "Al-Mulk – (the Kingdom)",
            "Al-Qalam (the Pen)",
            "Al-Haqqah (the Inevitable)",
            "Al-Ma’arij (the Elevated Passages)",
            "Nuh (Nuh)",
            "Al-Jinn (the Jinn)",
            "Al-Muzammil (the Wrapped)",
            "Al-Mudaththir (the Cloaked)",
            "Al-Qiyamah (the Resurrection)",
            "Al-Insan (the Human)",
            "Al-Mursalat (Those Sent Forth)",
            "An-Naba’ (the Great News)",
            "An-Nazi’at (Those Who Pull Out)",
            "‘Abasa (He Frowned)",
            "At-Takwir (the Overthrowing)",
            "Al-Infitar (the Cleaving)",
            "Al-Mutaffifin (Those Who Deal in Fraud)",
            "Al-Inshiqaq (the Splitting Asunder)",
            "Al-Buruj (the Stars)",
            "At-Tariq (the Nightcomer)",
            "Al-A’la (the Most High)",
            "Al-Ghashiyah (the Overwhelming)",
            "Al-Fajr (the Dawn)",
            "Al-Balad (the City)",
            "Ash-Shams (the Sun)",
            "Al-Layl (the Night)",
            "Adh-Dhuha (the Forenoon)",
            "Al-Inshirah (the Opening Forth)",
            "At-Tin (the Fig)",
            "Al-‘Alaq (the Clot)",
            "Al-Qadar (the Night of Decree)",
            "Al-Bayinah (the Proof)",
            "Az-Zalzalah (the Earthquake)",
            "Al-‘Adiyah (the Runners)",
            "Al-Qari’ah (the Striking Hour)",
            "At-Takathur (the Piling Up)",
            "Al-‘Asr (the Time)",
            "Al-Humazah (the Slanderer)",
            "Al-Fil (the Elephant)",
            "Quraish (Quraish)",
            "Al-Ma’un (the Assistance)",
            "Al-Kauthar (the River of Abundance)",
            "Al-Kafirun (the Disbelievers)",
            "An-Nasr (the Help)",
            "Al-Masad (the Palm Fiber)",
            "Al-Ikhlas (the Sincerity)",
            "Al-Falaq (the Daybreak)",
            "An-Nas (Mankind)"
        )
        val pageNos = intArrayOf(
            1,
            2,
            50,
            76,
            106,
            128,
            151,
            177,
            187,
            207,
            221,
            235,
            249,
            255,
            262,
            267,
            282,
            293,
            305,
            312,
            322,
            331,
            341,
            349,
            359,
            366,
            376,
            385,
            396,
            404,
            411,
            414,
            417,
            428,
            434,
            440,
            445,
            452,
            458,
            467,
            477,
            483,
            489,
            496,
            498,
            502,
            506,
            511,
            515,
            518,
            520,
            523,
            525,
            528,
            531,
            534,
            537,
            542,
            545,
            548,
            551,
            553,
            554,
            555,
            557,
            560,
            562,
            564,
            566,
            568,
            570,
            572,
            574,
            575,
            577,
            578,
            580,
            582,
            583,
            584,
            586,
            586,
            587,
            589,
            590,
            590,
            591,
            592,
            593,
            594,
            594,
            595,
            596,
            596,
            597,
            597,
            598,
            598,
            599,
            599,
            600,
            600,
            601,
            601,
            601,
            602,
            602,
            602,
            603,
            603,
            603,
            604,
            604,
            604
        )
        val makkimadani = arrayOf(
            "Makki",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Makki",
            "Makki",
            "Madani",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Makki",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Madani",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Makki",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Makki",
            "Madani",
            "Makki",
            "Makki",
            "Makki",
            "Makki"
        )
        val ayat = arrayOf(
            "7",
            "286",
            "200",
            "176",
            "120",
            "165",
            "206",
            "75",
            "129",
            "109",
            "123",
            "111",
            "43",
            "52",
            "99",
            "128",
            "111",
            "110",
            "98",
            "135",
            "112",
            "78",
            "118",
            "64",
            "77",
            "227",
            "93",
            "88",
            "69",
            "60",
            "34",
            "30",
            "73",
            "54",
            "45",
            "83",
            "182",
            "88",
            "75",
            "85",
            "54",
            "53",
            "89",
            "59",
            "37",
            "35",
            "38",
            "29",
            "18",
            "45",
            "60",
            "49",
            "62",
            "55",
            "78",
            "96",
            "29",
            "22",
            "24",
            "13",
            "14",
            "11",
            "11",
            "18",
            "12",
            "12",
            "30",
            "52",
            "52",
            "44",
            "28",
            "28",
            "20",
            "56",
            "40",
            "31",
            "50",
            "40",
            "46",
            "42",
            "29",
            "19",
            "36",
            "25",
            "22",
            "17",
            "19",
            "26",
            "30",
            "20",
            "15",
            "21",
            "11",
            "8",
            "8",
            "19",
            "5",
            "8",
            "8",
            "11",
            "11",
            "8",
            "3",
            "9",
            "5",
            "4",
            "7",
            "3",
            "6",
            "3",
            "5",
            "4",
            "5",
            "6"
        )

        for (i in surahNames.indices) {
            val values = ContentValues()
            values.put(COL_ID, i + 1)
            values.put(COL_NAME, surahNames[i])
            values.put(COL_DESCRIPTION, "${makkimadani[i]} - ${ayat[i]} Ayat")
            values.put(COL_PAGE_NUMBER, pageNos[i])
            db.insert(TABLE_SURAH, null, values)
        }
    }

    fun getJuzList(): ArrayList<QuranData> {
        val list = ArrayList<QuranData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_JUZ", null)
        if (cursor.moveToFirst()) {
            do {
                val item = QuranData(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAGE_NUMBER))
                )
                list.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getSurahList(): ArrayList<QuranData> {
        val list = ArrayList<QuranData>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_SURAH", null)
        if (cursor.moveToFirst()) {
            do {
                val item = QuranData(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAGE_NUMBER))
                )
                list.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getSurahForPage(page: Int): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_NAME FROM $TABLE_SURAH WHERE $COL_PAGE_NUMBER <= ? ORDER BY $COL_PAGE_NUMBER DESC LIMIT 1",
            arrayOf(page.toString())
        )
        val name = if (cursor.moveToFirst()) cursor.getString(0) else ""
        cursor.close()
        return name
    }

    fun getJuzForPage(page: Int): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_ID, $COL_NAME FROM $TABLE_JUZ WHERE $COL_PAGE_NUMBER <= ? ORDER BY $COL_PAGE_NUMBER DESC LIMIT 1",
            arrayOf(page.toString())
        )
        val name = if (cursor.moveToFirst()) "Juz ${cursor.getInt(0)} - ${cursor.getString(1)}" else ""
        cursor.close()
        return name
    }

    fun addPageTime(page: Int, elapsedMs: Long) {
        val db = this.writableDatabase
        db.execSQL(
            "INSERT OR IGNORE INTO $TABLE_PAGE_TIMING ($COL_PAGE, $COL_ACTIVE_MS) VALUES (?, 0)",
            arrayOf<Any>(page)
        )
        db.execSQL(
            "UPDATE $TABLE_PAGE_TIMING SET $COL_ACTIVE_MS = $COL_ACTIVE_MS + ? WHERE $COL_PAGE = ?",
            arrayOf<Any>(elapsedMs, page)
        )
    }

    fun allPagesReadInJuz(juz: Int): Boolean {
        val startPage = juzStartPage(juz)
        val endPage = juzEndPage(juz)
        val totalPages = endPage - startPage + 1

        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PAGE_TIMING WHERE $COL_PAGE BETWEEN ? AND ? AND " +
                "(($COL_PAGE <= 2 AND $COL_ACTIVE_MS > 0) OR ($COL_PAGE > 2 AND $COL_ACTIVE_MS >= ?))",
            arrayOf(startPage.toString(), endPage.toString(), MIN_READ_TIME_MS.toString())
        )
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()
        return count >= totalPages
    }

    fun getJuzTimingStats(juz: Int): Pair<Long, Int>? {
        val startPage = juzStartPage(juz)
        val endPage = juzEndPage(juz)

        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT SUM($COL_ACTIVE_MS), COUNT(*) FROM $TABLE_PAGE_TIMING " +
                "WHERE $COL_PAGE BETWEEN ? AND ? AND $COL_ACTIVE_MS > 0",
            arrayOf(startPage.toString(), endPage.toString())
        )
        val result = if (cursor.moveToFirst() && cursor.getInt(1) > 0) {
            Pair(cursor.getLong(0), cursor.getInt(1))
        } else null
        cursor.close()
        return result
    }

    fun recordJuzCompletion(juz: Int, totalMs: Long, pageCount: Int) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_JUZ, juz)
            put(COL_COMPLETED_AT, System.currentTimeMillis())
            put(COL_TOTAL_MS, totalMs)
            put(COL_PAGE_COUNT, pageCount)
        }
        db.insertWithOnConflict(TABLE_JUZ_COMPLETIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getOverallAvgJuzTime(excludeJuz: Int): Long? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT AVG($COL_TOTAL_MS) FROM $TABLE_JUZ_COMPLETIONS WHERE $COL_JUZ != ?",
            arrayOf(excludeJuz.toString())
        )
        val avg = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        cursor.close()
        return avg
    }

    fun clearPageTimingForJuz(juz: Int) {
        val startPage = juzStartPage(juz)
        val endPage = juzEndPage(juz)
        this.writableDatabase.delete(
            TABLE_PAGE_TIMING,
            "$COL_PAGE BETWEEN ? AND ?",
            arrayOf(startPage.toString(), endPage.toString())
        )
    }

    fun getJuzNameByNumber(juz: Int): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_NAME FROM $TABLE_JUZ WHERE $COL_ID = ?",
            arrayOf(juz.toString())
        )
        val name = if (cursor.moveToFirst()) cursor.getString(0) else ""
        cursor.close()
        return name
    }

    fun getJuzDescriptionByNumber(juz: Int): String {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COL_DESCRIPTION FROM $TABLE_JUZ WHERE $COL_ID = ?",
            arrayOf(juz.toString())
        )
        val desc = if (cursor.moveToFirst()) cursor.getString(0) else ""
        cursor.close()
        return desc
    }

    fun getFastestJuzTime(): Long? {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT MIN($COL_TOTAL_MS) FROM $TABLE_JUZ_COMPLETIONS", null
        )
        val min = if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        cursor.close()
        return min
    }
}