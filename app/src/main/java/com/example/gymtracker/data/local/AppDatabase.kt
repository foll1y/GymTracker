package com.example.gymtracker.data.local

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gymtracker.data.local.dao.*
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.MuscleGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

class Converters {
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup): String = value.name

    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup = enumValueOf<MuscleGroup>(value)
}

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntryEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun templateDao(): TemplateDao

    class PrepopulateCallback(
        private val databaseProvider: Provider<AppDatabase>,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                databaseProvider.get().exerciseDao().insertAll(INITIAL_EXERCISES)
            }
        }
    }

    companion object {
        val INITIAL_EXERCISES = listOf(
            ExerciseEntity(name = "Жим штанги лёжа", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Жим гантелей на наклонной скамье", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Отжимания на брусьях (акцент на грудь)", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Разведение гантелей лёжа", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Сведение рук в кроссовере", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Жим в хаммере на грудь", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Жим штанги головой вниз", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Пуловер с гантелью", muscleGroup = MuscleGroup.CHEST),
            ExerciseEntity(name = "Отжимания от пола", muscleGroup = MuscleGroup.CHEST),

            ExerciseEntity(name = "Становая тяга классическая", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Подтягивания широким хватом", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Тяга штанги в наклоне", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Тяга верхнего блока к груди", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Тяга горизонтального блока к поясу", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Тяга гантели одной рукой в наклоне", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Тяга Т-грифа", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Гиперэкстензия", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Шраги со штангой", muscleGroup = MuscleGroup.BACK),
            ExerciseEntity(name = "Пулловер на блоке стоя", muscleGroup = MuscleGroup.BACK),

            ExerciseEntity(name = "Приседания со штангой на плечах", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Фронтальные приседания", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Жим ногами в тренажёре", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Румынская тяга со штангой", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Выпады с гантелями", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Болгарские сплит-приседания", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Разгибание ног сидя в тренажёре", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Сгибание ног лёжа в тренажёре", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Подъёмы на носки стоя", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Гакк-приседания", muscleGroup = MuscleGroup.LEGS),
            ExerciseEntity(name = "Ягодичный мост со штангой", muscleGroup = MuscleGroup.LEGS),

            ExerciseEntity(name = "Армейский жим стоя", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Жим гантелей сидя", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Махи гантелями через стороны", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Тяга штанги к подбородку", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Разведение гантелей в наклоне", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Жим Арнольда", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Махи в кроссовере назад", muscleGroup = MuscleGroup.SHOULDERS),
            ExerciseEntity(name = "Подъём гантелей перед собой", muscleGroup = MuscleGroup.SHOULDERS),

            ExerciseEntity(name = "Подъём штанги на бицепс стоя", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Молотковые сгибания с гантелями", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Французский жим лёжа с EZ-грифом", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Разгибание на трицепс на верхнем блоке", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Сгибания на скамье Скотта", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Отжимания с узкой постановкой рук", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Концентрированный подъём на бицепс", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Разгибание гантели из-за головы", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Сгибания рук с гантелями на наклонной скамье", muscleGroup = MuscleGroup.ARMS),
            ExerciseEntity(name = "Отжимания на брусьях (акцент на трицепс)", muscleGroup = MuscleGroup.ARMS),

            ExerciseEntity(name = "Скручивания на наклонной скамье", muscleGroup = MuscleGroup.ABS),
            ExerciseEntity(name = "Подъёмы ног в висе на турнике", muscleGroup = MuscleGroup.ABS),
            ExerciseEntity(name = "Планка статическая", muscleGroup = MuscleGroup.ABS),
            ExerciseEntity(name = "Скручивания «Молитва» на верхнем блоке", muscleGroup = MuscleGroup.ABS),
            ExerciseEntity(name = "Колесо / ролик для пресса", muscleGroup = MuscleGroup.ABS),
            ExerciseEntity(name = "Русские скручивания с отягощением", muscleGroup = MuscleGroup.ABS)
        )
    }
}
