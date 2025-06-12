import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "light_readings")
data class LightReading(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val lux: Float
)
