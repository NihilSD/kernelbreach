package com.kernelbreach.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kernelbreach.core.database.entity.CheckpointAttemptEntity
import com.kernelbreach.core.database.entity.ConceptStateEntity
import com.kernelbreach.core.database.entity.ContentVersionEntity
import com.kernelbreach.core.database.entity.LabProgressEntity
import com.kernelbreach.core.database.entity.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLessonProgress(progress: LessonProgressEntity)

    @Query("SELECT * FROM lesson_progress")
    fun observeLessonProgress(): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE lessonId = :id")
    suspend fun lessonProgress(id: String): LessonProgressEntity?

    @Query("SELECT * FROM lesson_progress WHERE lessonId IN (:ids)")
    suspend fun lessonProgressFor(ids: List<String>): List<LessonProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLabProgress(progress: LabProgressEntity)

    @Query("SELECT * FROM lab_progress WHERE labId = :id")
    suspend fun labProgress(id: String): LabProgressEntity?

    @Query("SELECT * FROM lab_progress WHERE labId IN (:ids)")
    suspend fun labProgressFor(ids: List<String>): List<LabProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpointAttempt(attempt: CheckpointAttemptEntity): Long

    @Query("SELECT * FROM checkpoint_attempt WHERE moduleCode = :code ORDER BY dateEpochMillis DESC")
    suspend fun attemptsForModule(code: String): List<CheckpointAttemptEntity>

    @Query("SELECT DISTINCT moduleCode FROM checkpoint_attempt WHERE passed = 1")
    fun observePassedCheckpoints(): Flow<List<String>>

    @Query("SELECT DISTINCT moduleCode FROM checkpoint_attempt WHERE passed = 1")
    suspend fun passedCheckpoints(): List<String>
}

@Dao
interface ConceptStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: ConceptStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(states: List<ConceptStateEntity>)

    @Query("SELECT * FROM concept_state")
    suspend fun all(): List<ConceptStateEntity>

    @Query("SELECT * FROM concept_state WHERE dueDateEpochDay <= :today ORDER BY dueDateEpochDay")
    suspend fun due(today: Long): List<ConceptStateEntity>

    @Query("SELECT * FROM concept_state WHERE moduleCode = :code")
    suspend fun forModule(code: String): List<ConceptStateEntity>

    @Query("SELECT * FROM concept_state WHERE id = :id")
    suspend fun byId(id: String): ConceptStateEntity?
}

@Dao
interface ContentVersionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(version: ContentVersionEntity)

    @Query("SELECT * FROM content_version WHERE id = 1")
    suspend fun current(): ContentVersionEntity?
}
