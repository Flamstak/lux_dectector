import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LightReadingDao {
    @Insert
    suspend fun insert(reading: LightReading)

    @Query("SELECT * FROM light_readings ORDER BY timestamp DESC")
    suspend fun getAll(): List<LightReading>

    @Query("DELETE FROM light_readings")
    suspend fun deleteAll()
}