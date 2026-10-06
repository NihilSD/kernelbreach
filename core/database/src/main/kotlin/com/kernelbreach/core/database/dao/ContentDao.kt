package com.kernelbreach.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kernelbreach.core.database.entity.LabEntity
import com.kernelbreach.core.database.entity.LessonEntity
import com.kernelbreach.core.database.entity.ModuleEntity
import com.kernelbreach.core.database.entity.PathEntity
import com.kernelbreach.core.database.entity.QuizQuestionEntity
import com.kernelbreach.core.database.entity.TermEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaths(items: List<PathEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(items: List<ModuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(items: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabs(items: List<LabEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(items: List<QuizQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerms(items: List<TermEntity>)

    @Query("DELETE FROM path")
    suspend fun clearPaths()

    @Query("DELETE FROM module")
    suspend fun clearModules()

    @Query("DELETE FROM lesson")
    suspend fun clearLessons()

    @Query("DELETE FROM lab")
    suspend fun clearLabs()

    @Query("DELETE FROM quiz_question")
    suspend fun clearQuestions()

    @Query("DELETE FROM term")
    suspend fun clearTerms()

    // --- reads ---

    @Query("SELECT * FROM path ORDER BY `order`")
    fun observePaths(): Flow<List<PathEntity>>

    @Query("SELECT * FROM path ORDER BY `order`")
    suspend fun allPaths(): List<PathEntity>

    @Query("SELECT * FROM module ORDER BY `order`")
    suspend fun allModules(): List<ModuleEntity>

    @Query("SELECT * FROM module WHERE pathKey = :pathKey ORDER BY `order`")
    suspend fun modulesForPath(pathKey: String): List<ModuleEntity>

    @Query("SELECT * FROM module WHERE code = :code")
    suspend fun module(code: String): ModuleEntity?

    @Query("SELECT * FROM lesson ORDER BY orderIndex")
    suspend fun allLessons(): List<LessonEntity>

    @Query("SELECT * FROM lab ORDER BY orderIndex")
    suspend fun allLabs(): List<LabEntity>

    @Query("SELECT * FROM quiz_question ORDER BY orderIndex")
    suspend fun allQuestions(): List<QuizQuestionEntity>

    @Query("SELECT * FROM lesson WHERE moduleCode = :code ORDER BY orderIndex")
    suspend fun lessonsForModule(code: String): List<LessonEntity>

    @Query("SELECT * FROM lesson WHERE id = :id")
    suspend fun lesson(id: String): LessonEntity?

    @Query("SELECT * FROM lab WHERE moduleCode = :code ORDER BY orderIndex")
    suspend fun labsForModule(code: String): List<LabEntity>

    @Query("SELECT * FROM lab WHERE id = :id")
    suspend fun lab(id: String): LabEntity?

    @Query("SELECT * FROM quiz_question WHERE ownerId = :ownerId ORDER BY orderIndex")
    suspend fun questionsForOwner(ownerId: String): List<QuizQuestionEntity>

    @Query("SELECT COUNT(*) FROM module WHERE hasContent = 1")
    suspend fun contentModuleCount(): Int

    // --- glossary ---

    @Query("SELECT * FROM term ORDER BY term COLLATE NOCASE")
    fun observeTerms(): Flow<List<TermEntity>>

    @Query("SELECT * FROM term ORDER BY term COLLATE NOCASE")
    suspend fun allTerms(): List<TermEntity>

    @Query(
        """
        SELECT term.* FROM term
        JOIN term_fts ON term.id = term_fts.rowid
        WHERE term_fts MATCH :query
        ORDER BY term.term COLLATE NOCASE
        """,
    )
    suspend fun searchTerms(query: String): List<TermEntity>

    @Query("SELECT * FROM term WHERE termLower = :termLower LIMIT 1")
    suspend fun term(termLower: String): TermEntity?
}
