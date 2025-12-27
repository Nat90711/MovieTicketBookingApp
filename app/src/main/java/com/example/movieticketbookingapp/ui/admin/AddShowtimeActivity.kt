package com.example.movieticketbookingapp.ui.admin

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.CinemaRoom
import com.example.movieticketbookingapp.model.Movie
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddShowtimeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var spinnerMovies: Spinner
    private lateinit var spinnerRooms: Spinner
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var etPrice: EditText
    private lateinit var btnSave: TextView

    // Data Lists
    private val movieList = ArrayList<Movie>()
    private val roomList = ArrayList<CinemaRoom>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_showtime)

        db = FirebaseFirestore.getInstance()
        initViews()

        // Xử lý nút Back
        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadMoviesToSpinner()
        loadRoomsToSpinner()

        setupPickers()

        btnSave.setOnClickListener {
            if (validateInput()) {
                checkConflictAndSave()
            }
        }
    }

    private fun initViews() {
        spinnerMovies = findViewById(R.id.spinnerMovies)
        spinnerRooms = findViewById(R.id.spinnerRooms)
        tvDate = findViewById(R.id.tvSelectDate)
        tvTime = findViewById(R.id.tvSelectTime)
        etPrice = findViewById(R.id.etPrice)
        btnSave = findViewById(R.id.btnSaveShowtime)
    }

    private fun loadMoviesToSpinner() {
        db.collection("movies").get().addOnSuccessListener { result ->
            movieList.clear()
            val movieTitles = ArrayList<String>()
            for (doc in result) {
                val movie = doc.toObject(Movie::class.java)
                movieList.add(movie)
                movieTitles.add(movie.title)
            }
            spinnerMovies.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, movieTitles)
        }
    }

    private fun loadRoomsToSpinner() {
        db.collection("rooms").get().addOnSuccessListener { result ->
            roomList.clear()
            val roomNames = ArrayList<String>()
            for (doc in result) {
                val room = doc.toObject(CinemaRoom::class.java)
                roomList.add(room)
                roomNames.add(room.name)
            }
            spinnerRooms.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roomNames)
        }
    }

    private fun setupPickers() {
        tvDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                tvDate.text = String.format("%02d/%02d/%d", day, month + 1, year)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        tvTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                tvTime.text = String.format("%02d:%02d", hour, minute)
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
    }

    // --- 1. HÀM KIỂM TRA ĐẦU VÀO ---
    private fun validateInput(): Boolean {
        if (movieList.isEmpty() || roomList.isEmpty()) {
            Toast.makeText(this, "Đang tải dữ liệu, vui lòng chờ...", Toast.LENGTH_SHORT).show()
            return false
        }
        if (tvDate.text.toString().contains("Chọn") || tvTime.text.toString().contains("Chọn")) {
            Toast.makeText(this, "Vui lòng chọn ngày giờ!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etPrice.text.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập giá vé!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    // --- 2. HÀM CHECK TRÙNG LỊCH ---
    private fun checkConflictAndSave() {
        val selectedRoom = roomList[spinnerRooms.selectedItemPosition]
        val selectedDate = tvDate.text.toString()
        val selectedMovie = movieList[spinnerMovies.selectedItemPosition]
        val timeString = tvTime.text.toString()

        val duration = selectedMovie.duration
        val newStartMinutes = convertTimeToMinutes(timeString)
        val gap = 15 // Thời gian dọn dẹp
        val newEndMinutes = newStartMinutes + duration + gap

        // Query Firestore: Lấy các suất chiếu đã có trong PHÒNG này vào NGÀY này
        db.collection("showtimes")
            .whereEqualTo("roomId", selectedRoom.id)
            .whereEqualTo("date", selectedDate)
            .get()
            .addOnSuccessListener { documents ->
                var isConflict = false

                for (doc in documents) {
                    val oldTimeString = doc.getString("time") ?: "00:00"
                    val oldStartMinutes = convertTimeToMinutes(oldTimeString)

                    // Lấy duration cũ từ DB (nếu không có thì mặc định 120)
                    val oldDuration = doc.getLong("duration")?.toInt() ?: 120
                    val oldEndMinutes = oldStartMinutes + oldDuration + gap

                    // Kiểm tra chồng lấn
                    if (newStartMinutes < oldEndMinutes && oldStartMinutes < newEndMinutes) {
                        isConflict = true
                        break
                    }
                }

                if (isConflict) {
                    Toast.makeText(this, "Lỗi: Trùng giờ chiếu với phim khác trong phòng này!", Toast.LENGTH_LONG).show()
                } else {
                    saveToFirebase(selectedMovie, selectedRoom, timeString)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kiểm tra: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- 3. HÀM LƯU (Đã update) ---
    private fun saveToFirebase(movie: Movie, room: CinemaRoom, time: String) {
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val showtimeId = db.collection("showtimes").document().id

        val showtimeData = hashMapOf(
            "id" to showtimeId,
            "movieId" to movie.id,
            "movieTitle" to movie.title,
            "posterUrl" to movie.posterUrl,
            "roomId" to room.id,
            "roomName" to room.name,
            "date" to tvDate.text.toString(),
            "time" to time,
            "price" to price,
            "duration" to movie.duration,
            "bookedSeats" to listOf<Long>()
        )

        db.collection("showtimes").document(showtimeId)
            .set(showtimeData)
            .addOnSuccessListener {
                Toast.makeText(this, "Tạo suất chiếu thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Hàm phụ: Đổi giờ "HH:mm" -> Phút
    private fun convertTimeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }
}