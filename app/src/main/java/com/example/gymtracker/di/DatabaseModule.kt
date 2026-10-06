package com.example.gymtracker.di

import android.content.Context
import androidx.room.Room
import com.example.gymtracker.data.local.AppDatabase
import com.example.gymtracker.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        provider: Provider<AppDatabase>
    ): AppDatabase {
        val scope = CoroutineScope(SupervisorJob())
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gym_tracker.db"
        )
            .addCallback(AppDatabase.PrepopulateCallback(provider, scope))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideWorkoutDao(db: AppDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun provideTemplateDao(db: AppDatabase): TemplateDao = db.templateDao()

    @Provides
    fun provideProgramDao(db: AppDatabase): ProgramDao = db.programDao()
}
