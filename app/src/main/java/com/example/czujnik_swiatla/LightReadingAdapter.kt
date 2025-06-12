import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.czujnik_swiatla.databinding.ItemReadingBinding
import java.text.SimpleDateFormat
import java.util.*

class LightReadingAdapter(private var readings: List<LightReading>) :
    RecyclerView.Adapter<LightReadingAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemReadingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(reading: LightReading) {
            val date = Date(reading.timestamp)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            binding.timestamp.text = sdf.format(date)
            binding.lux.text = "Lux: ${reading.lux}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemReadingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = readings.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(readings[position])
    }

    fun updateData(newReadings: List<LightReading>) {
        readings = newReadings
        notifyDataSetChanged()
    }
}
