package io.github.wulkanowy.data.db.migrations

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.wulkanowy.data.db.AppDatabase
import org.junit.Rule

abstract class AbstractMigrationTest {

    val dbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    fun getMigratedRoomDatabase(): AppDatabase {
        val database = Room.databaseBuilder(ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java, dbName)
            .addMigrations(
                Migration12(),
                Migration13(),
                Migration14(),
                Migration15(),
                Migration16(),
                Migration17()
            )
            .build()
        // close the database and release any stream resources when the test finishes
        helper.closeWhenFinished(database)
        return database
    }
}
