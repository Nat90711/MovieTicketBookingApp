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
import com.example.movieticketbookingapp.model.Showtime
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class EditShowtimeActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    // Views
    private lateinit var spinnerMovies: Spinner
    private lateinit var spinnerRooms: Spinner
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var etPrice: EditText
    private lateinit var btnSave: TextView
    private lateinit var tvHeader: TextView

    // Data Lists
    private val movieList = ArrayList<Movie>()
    private val roomList = ArrayList<CinemaRoom>()

    // Data cần sửa
    private var currentShowtimeId: String = ""
    private var currentMovieId: String = "" // Hoặc Int tùy model của bạn
    private var currentRoomId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tận dụng lại layout Add, chỉ cần đổi text header
        setContentView(R.layout.activity_admin_add_showtime)

        db = FirebaseFirestore.getInstance()
        initViews()

        // Đổi tên Header thành "Sửa Suất Chiếu"
        // (Bạn cần id tvHeader trong layout, nếu chưa có thì findViewById theo cấu trúc layout)
        try {
            findViewById<TextView>(R.id.tvHeader)?.text = "Cập Nhật Suất Chiếu"
        } catch (e: Exception) {}

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }

        setupPickers()

        // QUAN TRỌNG: Load dữ liệu xong mới điền data cũ vào
        loadDataAndSetupUI()

        btnSave.setOnClickListener {
            if (validateInput()) {
                updateShowtime()
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

    private fun loadDataAndSetupUI() {
        // 1. Nhận dữ liệu từ Intent
        val showtimeId = intent.getStringExtra("showtime_id")
        if (showtimeId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID suất chiếu", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentShowtimeId = showtimeId

        // Lấy thông tin chi tiết từ Firebase để đảm bảo mới nhất
        db.collection("showtimes").document(currentShowtimeId).get()
            .addOnSuccessListener { doc ->
                val showtime = doc.toObject(Showtime::class.java)
                if (showtime != null) {
                    // Fill dữ liệu text
                    tvDate.text = showtime.date
                    tvTime.text = showtime.time
                    etPrice.setText(showtime.price.toInt().toString())

                    // Lưu tạm ID để tí nữa set Spinner
                    currentMovieId = showtime.movieId.toString()
                    currentRoomId = showtime.roomId

                    // Sau khi có data cũ -> Load danh sách Spinner
                    loadSpinners()
                }
            }
    }

    private fun loadSpinners() {
        // Load Movies trước
        db.collection("movies").get().addOnSuccessListener { movieResult ->
            movieList.clear()
            val movieTitles = ArrayList<String>()
            var selectedMovieIndex = 0

            for ((index, doc) in movieResult.withIndex()) {
                val movie = doc.toObject(Movie::class.java)
                movieList.add(movie)
                movieTitles.add(movie.title)

                // Kiểm tra xem phim này có phải phim cũ không
                if (movie.id.toString() == currentMovieId) {
                    selectedMovieIndex = index
                }
            }
            spinnerMovies.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, movieTitles)
            spinnerMovies.setSelection(selectedMovieIndex)

            // Sau đó Load Rooms
            loadRooms(selectedMovieIndex) // Truyền index phim vào chỉ để flow chạy tuần tự
        }
    }

    private fun loadRooms(movieIndex: Int) {
        db.collection("rooms").get().addOnSuccessListener { roomResult ->
            roomList.clear()
            val roomNames = ArrayList<String>()
            var selectedRoomIndex = 0

            for ((index, doc) in roomResult.withIndex()) {
                val room = doc.toObject(CinemaRoom::class.java)
                room.roomId = doc.id // Map ID thủ công
                roomList.add(room)
                roomNames.add(room.roomName)

                // Kiểm tra phòng cũ
                if (room.roomId == currentRoomId) {
                    selectedRoomIndex = index
                }
            }
            spinnerRooms.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roomNames)
            spinnerRooms.setSelection(selectedRoomIndex)
        }
    }

    private fun updateShowtime() {
        val selectedMovie = movieList[spinnerMovies.selectedItemPosition]
        val selectedRoom = roomList[spinnerRooms.selectedItemPosition]
        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0

        val updateData = hashMapOf<String, Any>(
            "movieId" to selectedMovie.id,
            "movieTitle" to selectedMovie.title,
            "posterUrl" to selectedMovie.posterUrl,
            "roomId" to selectedRoom.roomId,
            "roomName" to selectedRoom.roomName,
            "date" to tvDate.text.toString(),
            "time" to tvTime.text.toString(),
            "price" to price,
            "duration" to selectedMovie.duration
        )

        db.collection("showtimes").document(currentShowtimeId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi cập nhật: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInput(): Boolean {
        if (movieList.isEmpty() || roomList.isEmpty()) return false
        if (tvDate.text.toString().contains("Chọn") || tvTime.text.toString().contains("Chọn")) {
            Toast.makeText(this, "Vui lòng chọn ngày giờ!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (etPrice.text.isEmpty()) return false
        return true
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
}