package com.kernelbreach.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

// ---------------------------------------------------------------------------
// Content tables. Replaced wholesale on re-import (keyed by stable ids), never
// touching the user-state tables below.
// ---------------------------------------------------------------------------

@Entity(tableName = "path")
data class PathEntity(
    @PrimaryKey val key: String,
    val name: String,
    val tagline: String,
    val bestFor: String,
    val needs: String,
    val align: String,
    val color: String,
    val order: Int,
    val capstoneTitle: String?,
    val capstoneDesc: String?,
    val certNote: String?,
)

@Entity(
    tableName = "module",
    indices = [Index("pathKey"), Index("order")],
)
data class ModuleEntity(
    @PrimaryKey val code: String,
    val pathKey: String,
    val order: Int,
    val title: String,
    val topics: String,
    val level: String,
    val stageTitle: String,
    val checkpointFocus: String?,
    val canDo: List<String>,
    val reviewed: Boolean,
    val hasContent: Boolean,
)

@Entity(
    tableName = "lesson",
    indices = [Index("moduleCode"), Index("orderIndex")],
)
data class LessonEntity(
    @PrimaryKey val id: String, // CODE#L<n>
    val moduleCode: String,
    val orderIndex: Int,
    val title: String,
    val why: String,
    val sections: List<SectionRecord>,
    val snippetLabel: String?,
    val snippetText: String?,
    val example: String?,
    val recap: List<String>,
)

@Entity(
    tableName = "lab",
    indices = [Index("moduleCode"), Index("orderIndex")],
)
data class LabEntity(
    @PrimaryKey val id: String, // CODE#LAB<n>
    val moduleCode: String,
    val orderIndex: Int,
    val title: String,
    val sim: String,
    val scenario: String,
    val objective: String,
    val steps: List<StepRecord>,
    val answer: String,
    val hint: String,
)

/** Covers both lesson check questions and module checkpoint questions. */
@Entity(
    tableName = "quiz_question",
    indices = [Index("ownerId"), Index("moduleCode")],
)
data class QuizQuestionEntity(
    @PrimaryKey val id: String, // e.g. CODE#L1#Q0 or CODE#CP0
    val moduleCode: String,
    val ownerId: String, // lessonId for checks, moduleCode for checkpoint
    val kind: String, // CHECK | CHECKPOINT
    val orderIndex: Int,
    val prompt: String,
    val options: List<String>,
    val answerIndex: Int,
    val why: String,
)

@Entity(
    tableName = "term",
    indices = [Index(value = ["termLower"], unique = true), Index("moduleCode")],
)
data class TermEntity(
    @PrimaryKey val id: String, // termLower
    val termLower: String,
    val term: String,
    val definition: String,
    val moduleCode: String,
)

/** FTS mirror of [TermEntity] for the Library search. */
@Fts4(contentEntity = TermEntity::class)
@Entity(tableName = "term_fts")
data class TermFtsEntity(
    val term: String,
    val definition: String,
)

@Entity(tableName = "content_version")
data class ContentVersionEntity(
    @PrimaryKey val id: Int = 1,
    val hash: String,
    val importedAtEpochMillis: Long,
)

// ---------------------------------------------------------------------------
// User-state tables. Preserved across content re-imports.
// ---------------------------------------------------------------------------

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val status: String, // LessonStatus name
    val bestScore: Int,
    val lastOpenedEpochMillis: Long,
)

@Entity(tableName = "lab_progress")
data class LabProgressEntity(
    @PrimaryKey val labId: String,
    val stepIndex: Int,
    val completed: Boolean,
)

@Entity(
    tableName = "checkpoint_attempt",
    indices = [Index("moduleCode")],
)
data class CheckpointAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleCode: String,
    val score: Int,
    val total: Int,
    val passed: Boolean,
    val dateEpochMillis: Long,
    val answers: List<Int>,
)

@Entity(
    tableName = "concept_state",
    indices = [Index("moduleCode"), Index("dueDateEpochDay")],
)
data class ConceptStateEntity(
    @PrimaryKey val id: String, // term id or lesson id or question id
    val moduleCode: String,
    val kind: String, // ReviewKind name
    val intervalDays: Int,
    val dueDateEpochDay: Long,
    val lapses: Int,
    val lastReviewedEpochDay: Long?,
)
