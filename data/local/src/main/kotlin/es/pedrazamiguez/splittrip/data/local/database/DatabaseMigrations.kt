package es.pedrazamiguez.splittrip.data.local.database

import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_10_11
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_11_12
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_12_13
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_13_14
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_14_15
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_15_16
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_16_17
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_17_18
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_18_19
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_19_20
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_1_2
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_20_21
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_21_22
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_22_23
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_23_24
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_24_25
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_25_26
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_2_3
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_3_4
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_4_5
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_5_6
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_6_7
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_7_8
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_8_9
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_9_10

/**
 * All Room database migrations, ordered sequentially.
 * Each migration lives in its own file under the [migration] subpackage —
 * add new migrations there to keep this file stable.
 *
 * Referenced by [es.pedrazamiguez.splittrip.data.local.di.dataLocalModule]
 * when building the [androidx.room.RoomDatabase].
 */
internal val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
    MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
    MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
    MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
    MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25,
    MIGRATION_25_26
)
