package com.example.gymtracker.data.local

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gymtracker.data.local.dao.*
import com.example.gymtracker.data.local.entity.*
import com.example.gymtracker.data.model.ExerciseType
import com.example.gymtracker.data.model.MuscleGroup
import com.example.gymtracker.data.model.SetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Provider

class Converters {
    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup): String = value.name

    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup = enumValueOf<MuscleGroup>(value)

    @TypeConverter
    fun fromExerciseType(value: ExerciseType): String = value.name

    @TypeConverter
    fun toExerciseType(value: String): ExerciseType = try { enumValueOf<ExerciseType>(value) } catch(e: Exception) { ExerciseType.WEIGHT_AND_REPS }

    @TypeConverter
    fun fromSetType(value: SetType): String = value.name

    @TypeConverter
    fun toSetType(value: String): SetType = try { enumValueOf<SetType>(value) } catch(e: Exception) { SetType.NORMAL }
}

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntryEntity::class,
        ProgramEntity::class,
        ProgramDayEntity::class,
        ProgramDayExerciseEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun templateDao(): TemplateDao
    abstract fun programDao(): ProgramDao

    class PrepopulateCallback(
        private val databaseProvider: Provider<AppDatabase>,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                val dbInstance = databaseProvider.get()
                dbInstance.exerciseDao().insertAll(INITIAL_EXERCISES)
                prepopulateDefaultPrograms(dbInstance)
            }
        }

        private suspend fun prepopulateDefaultPrograms(db: AppDatabase) {
            val p1Id = db.programDao().insertProgram(
                ProgramEntity(
                    title = "Классический сплит (3 дня)",
                    description = "Грудь/Трицепс, Спина/Бицепс, Ноги/Плечи — классическая программа на силу и мышечную массу.",
                    isActive = true
                )
            )
            val day1Id = db.programDao().insertProgramDay(ProgramDayEntity(programId = p1Id, name = "День 1: Грудь и трицепс", orderIndex = 0))
            val day2Id = db.programDao().insertProgramDay(ProgramDayEntity(programId = p1Id, name = "День 2: Спина и бицепс", orderIndex = 1))
            val day3Id = db.programDao().insertProgramDay(ProgramDayEntity(programId = p1Id, name = "День 3: Ноги и плечи", orderIndex = 2))

            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day1Id, exerciseId = 1, orderIndex = 0, targetSets = 4, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day1Id, exerciseId = 2, orderIndex = 1, targetSets = 3, targetReps = "10-12"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day1Id, exerciseId = 3, orderIndex = 2, targetSets = 3, targetReps = "10-12"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day1Id, exerciseId = 42, orderIndex = 3, targetSets = 3, targetReps = "12-15"))

            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day2Id, exerciseId = 11, orderIndex = 0, targetSets = 4, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day2Id, exerciseId = 12, orderIndex = 1, targetSets = 4, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day2Id, exerciseId = 13, orderIndex = 2, targetSets = 3, targetReps = "10-12"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day2Id, exerciseId = 39, orderIndex = 3, targetSets = 3, targetReps = "10-12"))

            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day3Id, exerciseId = 20, orderIndex = 0, targetSets = 4, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day3Id, exerciseId = 22, orderIndex = 1, targetSets = 3, targetReps = "10-12"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day3Id, exerciseId = 31, orderIndex = 2, targetSets = 4, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = day3Id, exerciseId = 33, orderIndex = 3, targetSets = 4, targetReps = "12-15"))

            val p2Id = db.programDao().insertProgram(
                ProgramEntity(
                    title = "Фуллбоди (Все мышцы)",
                    description = "Базовые движения на всё тело за одну тренировку. Отлично подходит для 2-3 тренировок в неделю.",
                    isActive = false
                )
            )
            val fbDayId = db.programDao().insertProgramDay(ProgramDayEntity(programId = p2Id, name = "Основной тренировочный день", orderIndex = 0))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = fbDayId, exerciseId = 20, orderIndex = 0, targetSets = 3, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = fbDayId, exerciseId = 1, orderIndex = 1, targetSets = 3, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = fbDayId, exerciseId = 12, orderIndex = 2, targetSets = 3, targetReps = "8-10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = fbDayId, exerciseId = 31, orderIndex = 3, targetSets = 3, targetReps = "10"))
            db.programDao().insertProgramDayExercise(ProgramDayExerciseEntity(dayId = fbDayId, exerciseId = 49, orderIndex = 4, targetSets = 3, targetReps = "15"))
        }
    }

    companion object {
        val INITIAL_EXERCISES = listOf(
            // ГРУДЬ
            ExerciseEntity(
                name = "Жим штанги лёжа",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Лягте на скамью, лопатки сведены и опущены, стопы плотно упираются в пол, хват чуть шире плеч.",
                executionTip = "На вдохе плавно опустите гриф к середине груди (угол в локтях 45-60°), на выдохе мощно выжмите вверх.",
                mistakeTip = "Не разводите локти под 90° к корпусу (риск травмы плеч) и не отрывайте таз от скамьи.",
                imagePath = "Barbell_Bench_Press"
            ),
            ExerciseEntity(
                name = "Жим гантелей на наклонной скамье",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Установите угол скамьи 30-45°, прижмите спину и лопатки к спинке скамьи.",
                executionTip = "На вдохе опускайте гантели по дуге по сторонам от груди, на выдохе сводите их в верхней точке без удара.",
                mistakeTip = "Не поднимайте скамью слишком высоко (более 45°), иначе нагрузка уйдёт с верха груди в передние дельты.",
                imagePath = "Incline_Dumbbell_Bench_Press"
            ),
            ExerciseEntity(
                name = "Отжимания на брусьях (акцент на грудь)",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHTED_BODYWEIGHT,
                setupTip = "Упор на брусьях, наклоните корпус вперёд примерно на 30°, локти направлены чуть в стороны.",
                executionTip = "На вдохе опускайтесь до угла 90° в локтях, чувствуя растяжение груди, на выдохе поднимитесь вверх.",
                mistakeTip = "Не опускайтесь слишком глубоко при дискомфорте в плечах и не держите корпус строго вертикально.",
                imagePath = "Dips_-_Chest_Version"
            ),
            ExerciseEntity(
                name = "Разведение гантелей лёжа",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Лёжа на горизонтальной скамье, локти слегка согнуты и зафиксированы под одним углом.",
                executionTip = "На вдохе плавно разводите руки в стороны до уровня груди, на выдохе обнимающим движением верните наверх.",
                mistakeTip = "Не сгибайте и не разгибайте локти во время движения — это изолирующее движение, а не жим.",
                imagePath = "Dumbbell_Flyes"
            ),
            ExerciseEntity(
                name = "Сведение рук в кроссовере",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Блоки установлены сверху, шаг одной ногой вперёд для устойчивости, корпус слегка наклонён.",
                executionTip = "На выдохе сводите рукояти перед собой на уровне низа груди с секундной паузой в пиковом сокращении.",
                mistakeTip = "Не округляйте плечи вперёд и не помогайте себе наклонами корпуса.",
                imagePath = "Cable_Crossover"
            ),
            ExerciseEntity(
                name = "Жим в хаммере на грудь",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Отрегулируйте сиденье так, чтобы рукояти тренажёра находились на уровне середины груди.",
                executionTip = "Прижмите лопатки к спинке, на выдохе выжмите рукояти вперёд, на вдохе подконтрольно верните.",
                mistakeTip = "Не отрывайте плечи и спину от сиденья в конце жима.",
                imagePath = "Lever_Chest_Press"
            ),
            ExerciseEntity(
                name = "Жим штанги головой вниз",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Закрепите ноги в валиках наклонной скамьи с отрицательным наклоном, хват средней ширины.",
                executionTip = "Опускайте штангу на нижнюю часть грудных мышц, на выдохе выжимайте вверх.",
                mistakeTip = "Не делайте резких движений головой во избежание прилива давления.",
                imagePath = "Decline_Barbell_Bench_Press"
            ),
            ExerciseEntity(
                name = "Пуловер с гантелью",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Лягте поперёк скамьи, держа гантель обеими ладонями за верхний диск над грудью.",
                executionTip = "На глубоком вдохе плавно заведите гантель за голову, растягивая грудную клетку, на выдохе верните.",
                mistakeTip = "Не опускайте гантель слишком низко, чтобы не травмировать плечевой сустав.",
                imagePath = "Straight-Arm_Dumbbell_Pullover"
            ),
            ExerciseEntity(
                name = "Отжимания от пола",
                muscleGroup = MuscleGroup.CHEST,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Упор лёжа, ладони чуть шире плеч, тело вытянуто в одну прямую линию, пресс напряжён.",
                executionTip = "На вдохе опуститесь грудью почти до пола (локти под 45°), на выдохе отожмитесь вверх.",
                mistakeTip = "Не прогибайте поясницу вниз и не задирайте таз домиком.",
                imagePath = "Pushups"
            ),

            // СПИНА
            ExerciseEntity(
                name = "Становая тяга классическая",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стопы на ширине плеч под грифом, хват чуть шире колен, спина прямая, таз отведён назад.",
                executionTip = "Срывайте штангу ногами, прижимая гриф к голеням, и синхронно выпрямляйте корпус на выдохе.",
                mistakeTip = "Категорически не допускайте круглой поясницы при подъёме веса.",
                imagePath = "Barbell_Deadlift"
            ),
            ExerciseEntity(
                name = "Подтягивания широким хватом",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHTED_BODYWEIGHT,
                setupTip = "Хват шире плеч, прямой хват, вис на прямых руках, грудь направлена вверх к перекладине.",
                executionTip = "Тянитесь грудью к перекладине за счёт сведения лопаток и опускания локтей к бокам.",
                mistakeTip = "Избегайте раскачки телом (киппинга) и рывковых движений подбородком.",
                imagePath = "Pullups"
            ),
            ExerciseEntity(
                name = "Тяга штанги в наклоне",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Наклон корпуса 45°, колени слегка согнуты, поясница прогнута и зафиксирована, хват на ширине плеч.",
                executionTip = "На выдохе тяните гриф к низу живота, ведя локти вплотную к корпусу и сводя лопатки.",
                mistakeTip = "Не раскачивайте корпус и не тяните штангу руками вместо спины.",
                imagePath = "Bent_Over_Barbell_Row"
            ),
            ExerciseEntity(
                name = "Тяга верхнего блока к груди",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Сядьте в тренажёр, зафиксируйте бёдра валиками, возьмитесь широким хватом, слегка отклонитесь назад.",
                executionTip = "На выдохе тяните рукоять к ключицам, сводя лопатки и опуская плечи вниз.",
                mistakeTip = "Не отклоняйтесь назад слишком сильно и не тяните рукоять за шею.",
                imagePath = "Wide-Grip_Lat_Pulldown"
            ),
            ExerciseEntity(
                name = "Тяга горизонтального блока к поясу",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Сядьте с прямой спиной, колени чуть согнуты, удерживайте V-рукоять на прямых руках.",
                executionTip = "На выдохе притяните рукоять к поясу, полностью сведя лопатки и удерживая паузу 1 секунду.",
                mistakeTip = "Не округляйте поясницу при отпускании веса вперёд.",
                imagePath = "Seated_Cable_Rows"
            ),
            ExerciseEntity(
                name = "Тяга гантели одной рукой в наклоне",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Опирайтесь одним коленом и рукой на скамью, спина параллельна полу, гантель в свободной руке.",
                executionTip = "На выдохе тяните гантель к тазу, направляя локоть назад и вверх вдоль корпуса.",
                mistakeTip = "Не разворачивайте корпус в сторону тянущей руки.",
                imagePath = "One-Arm_Dumbbell_Row"
            ),
            ExerciseEntity(
                name = "Тяга Т-грифа",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Встаньте над грифом, спина прямая под углом 45°, колени согнуты, хват за рукоять двумя руками.",
                executionTip = "Тяните гриф к поясу за счёт сведения лопаток, не меняя угла наклона корпуса.",
                mistakeTip = "Не округляйте верх спины и не выпрямляйтесь во время движения.",
                imagePath = "T-Bar_Row_with_Handle"
            ),
            ExerciseEntity(
                name = "Гиперэкстензия",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Отрегулируйте подушку тренажёра чуть ниже сгиба тазобедренного сустава, стопы зафиксированы.",
                executionTip = "На вдохе плавно наклонитесь вниз, на выдохе разогнитесь до одной прямой линии с ногами.",
                mistakeTip = "Избегайте сильного переразгибания назад в пояснице в верхней точке.",
                imagePath = "Hyperextensions_Back_Extensions"
            ),
            ExerciseEntity(
                name = "Шраги со штангой",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя прямо, держите штангу перед собой прямым хватом на ширине плеч.",
                executionTip = "На выдохе поднимите плечи строго вверх к ушам, задержитесь на 1-2 секунды и плавно опустите.",
                mistakeTip = "Никогда не вращайте плечами по кругу — это травмирует плечевой сустав.",
                imagePath = "Barbell_Shrug"
            ),
            ExerciseEntity(
                name = "Пулловер на блоке стоя",
                muscleGroup = MuscleGroup.BACK,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Встаньте перед верхним блоком с прямой рукоятью, руки почти прямые, корпус слегка наклонён.",
                executionTip = "На выдохе по широкой дуге опускайте рукоять к бёдрам, напрягая широчайшие мышцы спины.",
                mistakeTip = "Не сгибайте руки в локтях, превращая упражнение в трицепс.",
                imagePath = "Rope_Straight-Arm_Pulldown"
            ),

            // НОГИ
            ExerciseEntity(
                name = "Приседания со штангой на плечах",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Гриф на верхней части трапеций, стопы чуть шире плеч, носки развёрнуты на 30°, грудь колесом.",
                executionTip = "На вдохе отводите таз назад и вниз, опускаясь минимум до параллели бёдер с полом, колени смотрят по носкам.",
                mistakeTip = "Не сводите колени внутрь при подъёме и не отрывайте пятки от пола.",
                imagePath = "Barbell_Squat"
            ),
            ExerciseEntity(
                name = "Фронтальные приседания",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Штанга лежит на передних дельтах, локти подняты высоко параллельно полу.",
                executionTip = "Приседайте с максимально вертикальным положением корпуса, колени двигаются вперёд и наружу.",
                mistakeTip = "Не опускайте локти вниз — штанга скатится с плеч.",
                imagePath = "Front_Barbell_Squat"
            ),
            ExerciseEntity(
                name = "Жим ногами в тренажёре",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Спина и таз плотно прижаты к спинке сиденья, стопы на ширине плеч на середине платформы.",
                executionTip = "На вдохе плавно опускайте платформу до угла 90° в коленях, на выдохе выжмите платформу пятками.",
                mistakeTip = "Ни в коем случае не выпрямляйте колени до щелчка в замок в верхней точке.",
                imagePath = "Leg_Press"
            ),
            ExerciseEntity(
                name = "Румынская тяга со штангой",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя прямо, колени мягкие (чуть согнуты), гриф скользит по передней поверхности бёдер.",
                executionTip = "Отводите таз далеко назад, наклоняя корпус с прямой спиной до растяжения задней поверхности бедра.",
                mistakeTip = "Не сгибайте ноги как в приседе и не горбите поясницу.",
                imagePath = "Romanian_Deadlift"
            ),
            ExerciseEntity(
                name = "Выпады с гантелями",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Гантели в руках по бокам, сделайте широкий шаг вперёд, корпус удерживайте вертикально.",
                executionTip = "Опускайтесь вертикально вниз, пока оба колена не образуют угол 90°, затем оттолкнитесь пяткой.",
                mistakeTip = "Не заваливайте переднее колено внутрь.",
                imagePath = "Dumbbell_Lunges"
            ),
            ExerciseEntity(
                name = "Болгарские сплит-приседания",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Одна нога стоит впереди, носок задней ноги лежит на скамье позади вас.",
                executionTip = "Опускайтесь вниз на передней ноге до прямого угла в бедре, удерживая равновесие.",
                mistakeTip = "Не переносите вес на заднюю ногу — 85% нагрузки должно быть на передней ноге.",
                imagePath = "Split_Squat_with_Dumbbells"
            ),
            ExerciseEntity(
                name = "Разгибание ног сидя в тренажёре",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Коленный сустав строго на одной оси с шарниром тренажёра, валик на нижней части голени.",
                executionTip = "На выдохе разогните ноги, задержавшись на 1 секунду в пиковом сокращении квадрицепсов.",
                mistakeTip = "Не бросайте вес резко вниз в конце движения.",
                imagePath = "Leg_Extensions"
            ),
            ExerciseEntity(
                name = "Сгибание ног лёжа в тренажёре",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Лягте на тренажёр, валик зафиксируйте чуть ниже ахилловых сухожилий, держитесь за ручки.",
                executionTip = "На выдохе сгибайте голени к ягодицам, концентрируясь на бицепсе бедра.",
                mistakeTip = "Не отрывайте таз от скамьи при подъёме веса.",
                imagePath = "Lying_Leg_Curls"
            ),
            ExerciseEntity(
                name = "Подъёмы на носки стоя",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Носки на краю подставки, пятки свободно опущены вниз, подушки тренажёра на плечах.",
                executionTip = "Медленно опуститесь пятками до полного растяжения икры, затем мощно поднимитесь на носки как можно выше.",
                mistakeTip = "Не пружиньте быстро — делайте паузу в нижней и верхней точке.",
                imagePath = "Standing_Calf_Raises"
            ),
            ExerciseEntity(
                name = "Гакк-приседания",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Спина прижата к каретке тренажёра, плечи под упорами, стопы на платформе чуть впереди.",
                executionTip = "Плавно опускайтесь в сед до угла 90°, затем плавно выжмите каретку вверх пятками.",
                mistakeTip = "Не отрывайте поясницу от спинки каретки.",
                imagePath = "Hack_Squat"
            ),
            ExerciseEntity(
                name = "Ягодичный мост со штангой",
                muscleGroup = MuscleGroup.LEGS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Верх спины на скамье, гриф со смягчителем лежит на сгибе бёдер, стопы стоят на полу.",
                executionTip = "На выдохе мощно поднимите таз вверх за счёт сжатия ягодиц до прямой линии тела.",
                mistakeTip = "Не прогибайте поясницу — работа должна выполняться исключительно ягодицами.",
                imagePath = "Barbell_Glute_Bridge"
            ),

            // ПЛЕЧИ
            ExerciseEntity(
                name = "Армейский жим стоя",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стопы на ширине плеч, гриф на уровне верха груди, хват чуть шире плеч, ягодицы и пресс напряжены.",
                executionTip = "Выжимайте штангу вертикально вверх над головой, в верхней точке голова слегка подаётся вперёд.",
                mistakeTip = "Не прогибайтесь назад в пояснице при выталкивании штанги.",
                imagePath = "Standing_Military_Press"
            ),
            ExerciseEntity(
                name = "Жим гантелей сидя",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Спинка скамьи установлена почти вертикально (75-80°), гантели на уровне ушей.",
                executionTip = "На выдохе выжимайте гантели вверх по естественной дуге, не ударяя их друг о друга.",
                mistakeTip = "Не опускайте локти ниже параллели с полом при проблемах с плечами.",
                imagePath = "Seated_Dumbbell_Press"
            ),
            ExerciseEntity(
                name = "Махи гантелями через стороны",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя прямо, гантели по бокам, локти слегка согнуты и зафиксированы.",
                executionTip = "Поднимайте руки через стороны локтями вверх до уровня плеч (не выше).",
                mistakeTip = "Не поднимайте кисти выше локтей и не раскачивайте корпус.",
                imagePath = "Side_Lateral_Raise"
            ),
            ExerciseEntity(
                name = "Тяга штанги к подбородку",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Хват на ширине плеч или чуть шире, стоя прямо, штанга у бёдер.",
                executionTip = "Тяните гриф вдоль корпуса вверх, ведя локти в стороны и вверх до уровня груди/подбородка.",
                mistakeTip = "Не используйте слишком узкий хват — он перегружает кисти и плечевой сустав.",
                imagePath = "Upright_Barbell_Row"
            ),
            ExerciseEntity(
                name = "Разведение гантелей в наклоне",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Наклоните корпус параллельно полу или обопритесь грудью о наклонную скамью.",
                executionTip = "На выдохе разводите гантели в стороны локтями вверх, акцентируя заднюю дельту.",
                mistakeTip = "Не сводите лопатки в верхней точке — это забирает нагрузку у дельт в спину.",
                imagePath = "Seated_Bent-Over_Rear_Delt_Raise"
            ),
            ExerciseEntity(
                name = "Жим Арнольда",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Гантели перед лицом ладонями к себе на уровне подбородка, локти прижаты к корпусу.",
                executionTip = "Во время жима вверх разворачивайте ладони наружу на 180°, выжимая вес над головой.",
                mistakeTip = "Делайте разворот плавно синхронно с жимом, без рывков.",
                imagePath = "Arnold_Dumbbell_Press"
            ),
            ExerciseEntity(
                name = "Махи в кроссовере назад",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Блоки установлены на уровне плеч, возьмите левый трос правой рукой, а правый — левой без рукоятей.",
                executionTip = "Разводите руки крест-накрест в стороны, удерживая локти слегка согнутыми.",
                mistakeTip = "Не отклоняйтесь корпусом назад.",
                imagePath = "Cable_Rear_Delt_Fly"
            ),
            ExerciseEntity(
                name = "Подъём гантелей перед собой",
                muscleGroup = MuscleGroup.SHOULDERS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя прямо, гантели на передней поверхности бёдер.",
                executionTip = "На выдохе поднимите гантель перед собой до уровня глаз, задержитесь на секунду.",
                mistakeTip = "Не забрасывайте вес телом по инерции.",
                imagePath = "Front_Dumbbell_Raise"
            ),

            // РУКИ
            ExerciseEntity(
                name = "Подъём штанги на бицепс стоя",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя прямо, хват снизу на ширине плеч, локти прижаты по бокам туловища.",
                executionTip = "На выдохе сгибайте руки в локтях, поднимая штангу к груди и сохраняя локти неподвижными.",
                mistakeTip = "Не выводите локти вперёд и не раскачивайтесь поясницей (читинг).",
                imagePath = "Barbell_Curl"
            ),
            ExerciseEntity(
                name = "Молотковые сгибания с гантелями",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя прямо, нейтральный хват (ладони смотрят друг на друга).",
                executionTip = "Сгибайте руки поочерёдно или вместе, поднимая гантели к плечам нейтральным хватом.",
                mistakeTip = "Не вращайте кисти во время подъёма.",
                imagePath = "Hammer_Curls"
            ),
            ExerciseEntity(
                name = "Французский жим лёжа с EZ-грифом",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Лёжа на скамье, держите EZ-гриф на вытянутых руках чуть под углом за голову.",
                executionTip = "Сгибайте только локти, опуская гриф за макушку, затем мощно разогните руки за счёт трицепса.",
                mistakeTip = "Не разводите локти широко в стороны и не опускайте гриф на лоб.",
                imagePath = "EZ-Bar_Skullcrusher"
            ),
            ExerciseEntity(
                name = "Разгибание на трицепс на верхнем блоке",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя у блока с канатной или прямой рукоятью, локти плотно прижаты к бокам.",
                executionTip = "На выдохе полностью разогните руки вниз, в нижней точке разведите концы каната в стороны.",
                mistakeTip = "Не отрывайте локти от корпуса и не нависайте всем телом на рукоять.",
                imagePath = "Triceps_Pushdown"
            ),
            ExerciseEntity(
                name = "Сгибания на скамье Скотта",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Подмышки плотно упираются в верхний срез парты, трицепсы лежат на наклонной подушке.",
                executionTip = "Сгибайте руки со штангой до прямого угла, напрягая бицепс в пике.",
                mistakeTip = "Не разгибайте руки до конца внизу — это опасно для сухожилий бицепса.",
                imagePath = "Preacher_Curl"
            ),
            ExerciseEntity(
                name = "Отжимания с узкой постановкой рук",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Упор лёжа, кисти на расстоянии 15-20 см друг от друга, локти скользят вдоль рёбер.",
                executionTip = "Опускайтесь вниз, держа локти прижатыми к телу, на выдохе выжмите себя трицепсами.",
                mistakeTip = "Не расставляйте пальцы слишком широко и не прогибайте таз.",
                imagePath = "Close-Grip_Push-Up"
            ),
            ExerciseEntity(
                name = "Концентрированный подъём на бицепс",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Сидя на скамье, локоть упирается во внутреннюю поверхность бедра.",
                executionTip = "Поднимайте гантель к плечу, фиксируя локоть неподвижно.",
                mistakeTip = "Не помогайте себе плечом рабочей руки.",
                imagePath = "Concentration_Curls"
            ),
            ExerciseEntity(
                name = "Разгибание гантели из-за головы",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Сидя или стоя, удерживайте гантель обеими ладонями под верхним диском над головой.",
                executionTip = "Опускайте гантель за голову, сгибая локти, затем разогните руки на выдохе.",
                mistakeTip = "Держите локти как можно ближе к голове, не разводя их широко.",
                imagePath = "Standing_Dumbbell_Triceps_Extension"
            ),
            ExerciseEntity(
                name = "Сгибания рук с гантелями на наклонной скамье",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Угол скамьи 45-60°, руки с гантелями свободно опущены вниз, локти назад.",
                executionTip = "Сгибайте руки с одновременным разворотом кистей наружу (супинацией).",
                mistakeTip = "Не отрывайте спину и затылок от скамьи.",
                imagePath = "Incline_Dumbbell_Curl"
            ),
            ExerciseEntity(
                name = "Отжимания на брусьях (акцент на трицепс)",
                muscleGroup = MuscleGroup.ARMS,
                exerciseType = ExerciseType.WEIGHTED_BODYWEIGHT,
                setupTip = "Упор на узких параллельных брусьях, корпус держите строго вертикально, локти близко к телу.",
                executionTip = "Опускайтесь вниз до угла 90° в локтях, локти смотрят строго назад, на выдохе выжмите себя вверх.",
                mistakeTip = "Не наклоняйте корпус вперёд — иначе нагрузка сместится на грудные мышцы.",
                imagePath = "Dips_-_Triceps_Version"
            ),

            // ПРЕСС
            ExerciseEntity(
                name = "Скручивания на наклонной скамье",
                muscleGroup = MuscleGroup.ABS,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Зафиксируйте ноги в валиках наклонной скамьи, руки у висков или на груди.",
                executionTip = "Скручивайте верх корпуса к тазу за счёт сжатия пресса, округляя спину на выдохе.",
                mistakeTip = "Не тяните шею руками вперёд и не отрывайте поясницу полностью.",
                imagePath = "Incline_Crunch"
            ),
            ExerciseEntity(
                name = "Подъёмы ног в висе на турнике",
                muscleGroup = MuscleGroup.ABS,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Вис на перекладине, плечи зафиксированы, ноги сведены вместе.",
                executionTip = "На выдохе поднимите колени или прямые ноги к перекладине, обязательно подкручивая таз к рёбрам.",
                mistakeTip = "Не раскачивайтесь по инерции — движение должно быть подконтрольным.",
                imagePath = "Hanging_Leg_Raise"
            ),
            ExerciseEntity(
                name = "Планка статическая",
                muscleGroup = MuscleGroup.ABS,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Упор на предплечья и носки, локти строго под плечами, тело в струну.",
                executionTip = "Напрягите ягодицы и пресс, втяните живот, дышите ровно и спокойно.",
                mistakeTip = "Не допускайте провисания поясницы вниз.",
                imagePath = "Plank"
            ),
            ExerciseEntity(
                name = "Скручивания «Молитва» на верхнем блоке",
                muscleGroup = MuscleGroup.ABS,
                exerciseType = ExerciseType.WEIGHT_AND_REPS,
                setupTip = "Стоя на коленях перед блоком, держите канатную рукоять у лба/висков.",
                executionTip = "На выдохе силой пресса скручивайте корпус вниз, приближая локти к бёдрам.",
                mistakeTip = "Не садитесь на пятки — таз должен оставаться неподвижным.",
                imagePath = "Cable_Crunch"
            ),
            ExerciseEntity(
                name = "Колесо / ролик для пресса",
                muscleGroup = MuscleGroup.ABS,
                exerciseType = ExerciseType.BODYWEIGHT_ONLY,
                setupTip = "Стоя на коленях, удерживайте ролик под плечами, спина слегка скруглена.",
                executionTip = "Катите ролик вперёд настолько далеко, насколько можете контролировать поясницу, затем вернитесь силой пресса.",
                mistakeTip = "Не прогибайте поясницу в нижней точке — если не хватает сил, уменьшите амплитуду.",
                imagePath = "Ab_Roller"
            ),
            ExerciseEntity(
                name = "Русские скручивания с отягощением",
                muscleGroup = MuscleGroup.ABS,
                exerciseType = ExerciseType.WEIGHTED_BODYWEIGHT,
                setupTip = "Сидя на полу, колени согнуты, стопы на весу, корпус отклонён назад на 45°.",
                executionTip = "Поворачивайте корпус из стороны в сторону, касаясь гантелью или диском пола у бедра.",
                mistakeTip = "Поворачивайте именно грудную клетку и плечи, а не просто водите руками.",
                imagePath = "Russian_Twist"
            )
        )
    }
}
