package io.github.wulkanowy.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.wulkanowy.data.db.entities.Subject
import io.reactivex.Maybe

@Dao
interface SubjectDao : BaseDao<Subject> {

    @Query("SELECT * FROM Subjects WHERE diary_id = :diaryId AND student_id = :studentId")
    fun loadAll(diaryId: Int, studentId: Int): Maybe<List<Subject>>
}
