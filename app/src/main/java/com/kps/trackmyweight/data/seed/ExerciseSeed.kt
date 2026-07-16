package com.kps.trackmyweight.data.seed

import com.kps.trackmyweight.data.db.entity.ExerciseEntity
import com.kps.trackmyweight.data.db.enums.ExerciseForce
import com.kps.trackmyweight.data.db.enums.ExerciseMechanics
import com.kps.trackmyweight.data.db.enums.MuscleGroup
import kotlinx.datetime.Instant

/**
 * Référentiel d'exercices seedé au premier lancement.
 * Couvre les principaux exercices pour chaque groupe musculaire, avec les
 * équipements requis (par slug — voir EquipmentSeed).
 */
object ExerciseSeed {

    fun items(now: Instant): List<Pair<ExerciseEntity, List<String>>> = listOf(
        // ─────── PECTORAUX ───────
        ex("bench_press", "Développé couché", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_FRONT),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("barbell_olympic", "bench_flat", "plates_olympic"), now),
        ex("incline_bench_press", "Développé incliné", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS_FRONT, MuscleGroup.TRICEPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("barbell_olympic", "bench_incline", "plates_olympic"), now),
        ex("db_bench_press", "Développé couché haltères", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_FRONT),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("dumbbells_fixed", "bench_flat"), now),
        ex("db_incline_press", "Développé incliné haltères", MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS_FRONT),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("dumbbells_fixed", "bench_incline"), now),
        ex("db_flye", "Écarté haltères", MuscleGroup.CHEST, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("dumbbells_fixed", "bench_flat"), now),
        ex("cable_flye", "Écarté poulies", MuscleGroup.CHEST, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("cable_crossover"), now),
        ex("pec_deck", "Pec deck", MuscleGroup.CHEST, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("pec_deck"), now),
        ex("dips_chest", "Dips (version pectoraux)", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_FRONT),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("dip_bars"), now),
        ex("pushup", "Pompes", MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_FRONT),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, emptyList(), now),

        // ─────── DOS ───────
        ex("deadlift", "Soulevé de terre", MuscleGroup.LOWER_BACK, listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.TRAPS, MuscleGroup.BACK_UPPER),
            ExerciseMechanics.COMPOUND, ExerciseForce.HINGE, listOf("barbell_olympic", "plates_olympic"), now),
        ex("pullup", "Traction", MuscleGroup.BACK_LATS, listOf(MuscleGroup.BICEPS, MuscleGroup.BACK_UPPER),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("pullup_bar"), now),
        ex("chinup", "Chin-up (supination)", MuscleGroup.BACK_LATS, listOf(MuscleGroup.BICEPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("pullup_bar"), now),
        ex("lat_pulldown", "Tirage vertical", MuscleGroup.BACK_LATS, listOf(MuscleGroup.BICEPS, MuscleGroup.BACK_UPPER),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("lat_pulldown"), now),
        ex("barbell_row", "Rowing barre", MuscleGroup.BACK_UPPER, listOf(MuscleGroup.BACK_LATS, MuscleGroup.BICEPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("barbell_olympic", "plates_olympic"), now),
        ex("db_row", "Rowing haltère un bras", MuscleGroup.BACK_LATS, listOf(MuscleGroup.BACK_UPPER, MuscleGroup.BICEPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("dumbbells_fixed", "bench_flat"), now),
        ex("seated_row", "Tirage horizontal poulie", MuscleGroup.BACK_UPPER, listOf(MuscleGroup.BACK_LATS, MuscleGroup.BICEPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("seated_row"), now),
        ex("face_pull", "Face pull", MuscleGroup.SHOULDERS_REAR, listOf(MuscleGroup.BACK_UPPER, MuscleGroup.TRAPS),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("cable_column_single"), now),
        ex("t_bar_row", "Rowing T-Bar", MuscleGroup.BACK_UPPER, listOf(MuscleGroup.BACK_LATS, MuscleGroup.BICEPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("barbell_olympic", "plates_olympic"), now),
        ex("back_extension", "Extension lombaires", MuscleGroup.LOWER_BACK, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.ISOLATION, ExerciseForce.HINGE, listOf("back_extension"), now),

        // ─────── ÉPAULES ───────
        ex("ohp", "Développé militaire barre", MuscleGroup.SHOULDERS_FRONT, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_SIDE),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("barbell_olympic", "plates_olympic"), now),
        ex("db_ohp", "Développé haltères assis", MuscleGroup.SHOULDERS_FRONT, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS_SIDE),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("dumbbells_fixed", "bench_incline"), now),
        ex("lateral_raise", "Élévations latérales", MuscleGroup.SHOULDERS_SIDE, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("dumbbells_fixed"), now),
        ex("cable_lateral_raise", "Élévations latérales poulie", MuscleGroup.SHOULDERS_SIDE, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("cable_column_single"), now),
        ex("rear_delt_flye", "Écarté oiseau (postérieurs)", MuscleGroup.SHOULDERS_REAR, listOf(MuscleGroup.BACK_UPPER),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("dumbbells_fixed"), now),
        ex("front_raise", "Élévations frontales", MuscleGroup.SHOULDERS_FRONT, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("dumbbells_fixed"), now),
        ex("upright_row", "Rowing menton", MuscleGroup.SHOULDERS_SIDE, listOf(MuscleGroup.TRAPS),
            ExerciseMechanics.COMPOUND, ExerciseForce.PULL, listOf("barbell_ez"), now),

        // ─────── BICEPS ───────
        ex("barbell_curl", "Curl barre", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("barbell_olympic", "plates_olympic"), now),
        ex("ez_curl", "Curl barre EZ", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("barbell_ez"), now),
        ex("db_curl", "Curl haltères", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("dumbbells_fixed"), now),
        ex("hammer_curl", "Curl marteau", MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("dumbbells_fixed"), now),
        ex("preacher_curl", "Curl Larry Scott", MuscleGroup.BICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("barbell_ez", "preacher_bench"), now),
        ex("cable_curl", "Curl poulie basse", MuscleGroup.BICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("cable_column_single"), now),
        ex("concentration_curl", "Curl concentration", MuscleGroup.BICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("dumbbells_fixed", "bench_flat"), now),

        // ─────── TRICEPS ───────
        ex("close_grip_bench", "Développé prise serrée", MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS_FRONT),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("barbell_olympic", "bench_flat", "plates_olympic"), now),
        ex("dips_triceps", "Dips triceps (buste droit)", MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST),
            ExerciseMechanics.COMPOUND, ExerciseForce.PUSH, listOf("dip_bars"), now),
        ex("triceps_pushdown", "Extension poulie corde", MuscleGroup.TRICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("cable_column_single"), now),
        ex("overhead_extension", "Extension nuque haltère", MuscleGroup.TRICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("dumbbells_fixed"), now),
        ex("skull_crusher", "Barre au front", MuscleGroup.TRICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("barbell_ez", "bench_flat"), now),
        ex("kickback", "Kickback haltère", MuscleGroup.TRICEPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("dumbbells_fixed"), now),

        // ─────── QUADRICEPS ───────
        ex("back_squat", "Squat barre (dos)", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK, MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("barbell_olympic", "squat_rack", "plates_olympic"), now),
        ex("front_squat", "Squat frontal", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("barbell_olympic", "squat_rack", "plates_olympic"), now),
        ex("leg_press", "Presse à cuisses", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("leg_press"), now),
        ex("hack_squat", "Hack squat machine", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("hack_squat"), now),
        ex("bulgarian_split_squat", "Fente bulgare", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("dumbbells_fixed", "bench_flat"), now),
        ex("lunge", "Fente avant", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("dumbbells_fixed"), now),
        ex("leg_extension", "Extension quadriceps", MuscleGroup.QUADS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.SQUAT, listOf("leg_extension"), now),
        ex("goblet_squat", "Goblet squat", MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES),
            ExerciseMechanics.COMPOUND, ExerciseForce.SQUAT, listOf("dumbbells_fixed"), now),

        // ─────── ISCHIOS / FESSIERS ───────
        ex("romanian_deadlift", "Soulevé roumain", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
            ExerciseMechanics.COMPOUND, ExerciseForce.HINGE, listOf("barbell_olympic", "plates_olympic"), now),
        ex("db_rdl", "Soulevé roumain haltères", MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES),
            ExerciseMechanics.COMPOUND, ExerciseForce.HINGE, listOf("dumbbells_fixed"), now),
        ex("leg_curl", "Leg curl", MuscleGroup.HAMSTRINGS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.HINGE, listOf("leg_curl"), now),
        ex("hip_thrust", "Hip thrust barre", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.COMPOUND, ExerciseForce.HINGE, listOf("barbell_olympic", "bench_flat", "plates_olympic"), now),
        ex("glute_bridge", "Glute bridge", MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS),
            ExerciseMechanics.ISOLATION, ExerciseForce.HINGE, emptyList(), now),

        // ─────── MOLLETS ───────
        ex("standing_calf_raise", "Mollets debout", MuscleGroup.CALVES, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("calf_raise_machine"), now),
        ex("seated_calf_raise", "Mollets assis", MuscleGroup.CALVES, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("calf_raise_machine"), now),
        ex("db_calf_raise", "Mollets haltère (unilatéral)", MuscleGroup.CALVES, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PUSH, listOf("dumbbells_fixed"), now),

        // ─────── TRAPÈZES ───────
        ex("shrug_barbell", "Shrug barre", MuscleGroup.TRAPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("barbell_olympic", "plates_olympic"), now),
        ex("shrug_db", "Shrug haltères", MuscleGroup.TRAPS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("dumbbells_fixed"), now),

        // ─────── ABDOMINAUX / OBLIQUES ───────
        ex("plank", "Planche", MuscleGroup.ABS, listOf(MuscleGroup.OBLIQUES),
            ExerciseMechanics.ISOLATION, ExerciseForce.STATIC, emptyList(), now),
        ex("crunch", "Crunch", MuscleGroup.ABS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.STATIC, emptyList(), now),
        ex("cable_crunch", "Crunch poulie", MuscleGroup.ABS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.STATIC, listOf("cable_column_single"), now),
        ex("hanging_leg_raise", "Leg raise suspendu", MuscleGroup.ABS, listOf(MuscleGroup.OBLIQUES),
            ExerciseMechanics.ISOLATION, ExerciseForce.STATIC, listOf("pullup_bar"), now),
        ex("ab_wheel_rollout", "Roue abdominale", MuscleGroup.ABS, emptyList(),
            ExerciseMechanics.COMPOUND, ExerciseForce.STATIC, listOf("ab_wheel"), now),
        ex("russian_twist", "Russian twist", MuscleGroup.OBLIQUES, listOf(MuscleGroup.ABS),
            ExerciseMechanics.ISOLATION, ExerciseForce.ROTATION, emptyList(), now),
        ex("side_plank", "Planche latérale", MuscleGroup.OBLIQUES, listOf(MuscleGroup.ABS),
            ExerciseMechanics.ISOLATION, ExerciseForce.STATIC, emptyList(), now),

        // ─────── AVANT-BRAS ───────
        ex("wrist_curl", "Curl poignets", MuscleGroup.FOREARMS, emptyList(),
            ExerciseMechanics.ISOLATION, ExerciseForce.PULL, listOf("dumbbells_fixed"), now),
        ex("farmer_walk", "Farmer walk", MuscleGroup.FOREARMS, listOf(MuscleGroup.TRAPS, MuscleGroup.ABS),
            ExerciseMechanics.COMPOUND, ExerciseForce.CARRY, listOf("dumbbells_fixed"), now),
    )

    private fun ex(
        slug: String,
        name: String,
        primary: MuscleGroup,
        secondary: List<MuscleGroup>,
        mechanics: ExerciseMechanics,
        force: ExerciseForce,
        equipmentKeys: List<String>,
        now: Instant,
    ): Pair<ExerciseEntity, List<String>> = ExerciseEntity(
        name = name,
        slug = slug,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        mechanics = mechanics,
        force = force,
        defaultRestSec = when (mechanics) {
            ExerciseMechanics.COMPOUND -> 180
            ExerciseMechanics.ISOLATION -> 90
        },
        cues = null,
        createdAt = now,
        updatedAt = now,
    ) to equipmentKeys
}
