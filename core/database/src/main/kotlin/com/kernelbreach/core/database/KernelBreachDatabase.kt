package com.kernelbreach.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kernelbreach.core.database.dao.ConceptStateDao
import com.kernelbreach.core.database.dao.ContentDao
import com.kernelbreach.core.database.dao.ContentVersionDao
import com.kernelbreach.core.database.dao.ProgressDao
import com.kernelbreach.core.database.entity.CheckpointAttemptEntity
import com.kernelbreach.core.database.entity.ConceptStateEntity
import com.kernelbreach.core.database.entity.ContentVersionEntity
import com.kernelbreach.core.database.entity.Converters
import com.kernelbreach.core.database.entity.LabEntity
import com.kernelbreach.core.database.entity.LabProgressEntity
import com.kernelbreach.core.database.entity.LessonEntity
import com.kernelbreach.core.database.entity.LessonProgressEntity
import com.kernelbreach.core.database.entity.ModuleEntity
import com.kernelbreach.core.database.entity.PathEntity
import com.kernelbreach.core.database.entity.QuizQuestionEntity
import com.kernelbreach.core.database.entity.TermEntity
import com.kernelbreach.core.database.entity.TermFtsEntity

@Database(
    entities = [
        PathEntity::class,
        ModuleEntity::class,
        LessonEntity::class,
        LabEntity::class,
        QuizQuestionEntity::class,
        TermEntity::class,
        TermFtsEntity::class,
        ContentVersionEntity::class,
        LessonProgressEntity::class,
        LabProgressEntity::class,
        CheckpointAttemptEntity::class,
        ConceptStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class KernelBreachDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun progressDao(): ProgressDao
    abstract fun conceptStateDao(): ConceptStateDao
    abstract fun contentVersionDao(): ContentVersionDao

    companion object {
        const val NAME = "kernel_breach.db"
    }
}
