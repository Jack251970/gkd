package li.songe.gkd.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import li.songe.gkd.data.ClickLog
import li.songe.gkd.data.Snapshot
import li.songe.gkd.data.SubsConfig
import li.songe.gkd.data.SubsItem

@Database(
    version = 2,
    entities = [SubsItem::class, Snapshot::class, SubsConfig::class, ClickLog::class],
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AppDb : RoomDatabase() {
    abstract fun subsItemDao(): SubsItem.SubsItemDao
    abstract fun snapshotDao(): Snapshot.SnapshotDao
    abstract fun subsConfigDao(): SubsConfig.SubsConfigDao
    abstract fun clickLogDao(): ClickLog.TriggerLogDao
}