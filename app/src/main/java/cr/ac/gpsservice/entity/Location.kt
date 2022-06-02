package cr.ac.gpsservice.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "location_table")

data class Location (
    @PrimaryKey (autoGenerate = true)
    val locationId: Long?,
    val latitude:Double,
    val longitude:Double
)