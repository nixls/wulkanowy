package io.github.wulkanowy.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration17 : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE Attendance ADD COLUMN excusable INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE Attendance ADD COLUMN time_id INTEGER NOT NULL DEFAULT 0")
    }
}
