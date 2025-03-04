package io.github.wulkanowy.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.reactivex.Maybe
import javax.inject.Singleton

@Singleton
@Dao
interface GradeSummaryDao : BaseDao<GradeSummary> {

    @Query("SELECT * FROM GradesSummary WHERE student_id = :studentId AND semester_id = :semesterId")
    fun loadAll(semesterId: Int, studentId: Int): Maybe<List<GradeSummary>>
}
