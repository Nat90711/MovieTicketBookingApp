package com.example.movieticketbookingapp.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movieticketbookingapp.R
import com.example.movieticketbookingapp.model.CinemaRoom
import com.google.firebase.firestore.FirebaseFirestore

class AdminRoomActivity : AppCompatActivity() {

    private lateinit var rvRooms: RecyclerView
    private lateinit var btnAddRoom: Button
    private lateinit var db: FirebaseFirestore
    private val roomList = mutableListOf<CinemaRoom>()
    private lateinit var adapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_room_list)

        db = FirebaseFirestore.getInstance()
        rvRooms = findViewById(R.id.rvRooms)
        btnAddRoom = findViewById(R.id.btnAddRoom)

        setupRecyclerView()
        loadRooms()

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnAddRoom.setOnClickListener {
            showAddRoomDialog()
        }

    }

    private fun setupRecyclerView() {
        adapter = RoomAdapter(roomList)
        rvRooms.layoutManager = LinearLayoutManager(this)
        rvRooms.adapter = adapter
    }

    private fun loadRooms() {
        db.collection("rooms")
            .get()
            .addOnSuccessListener { result ->
                roomList.clear()
                for (document in result) {
                    val room = document.toObject(CinemaRoom::class.java)
                    room.id = document.id
                    roomList.add(room)
                }
                adapter.notifyDataSetChanged()
            }
    }

    private fun showAddRoomDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etType = dialogView.findViewById<EditText>(R.id.etRoomType)
        val etRows = dialogView.findViewById<EditText>(R.id.etRows)
        val etCols = dialogView.findViewById<EditText>(R.id.etCols)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveRoom)
        val etRoomId = dialogView.findViewById<EditText>(R.id.etRoomId)

        btnSave.setOnClickListener {
            val id = etRoomId.text.toString().trim()
            val name = etName.text.toString()
            val type = etType.text.toString()
            val rows = etRows.text.toString().toIntOrNull() ?: 0
            val cols = etCols.text.toString().toIntOrNull() ?: 0

            if (id.isNotEmpty() && name.isNotEmpty() && rows > 0 && cols > 0) {
                checkAndSaveRoom(id, name, type, rows, cols, dialog)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    private fun checkAndSaveRoom(id: String, name: String, type: String, rows: Int, cols: Int, dialog: AlertDialog) {
        val roomRef = db.collection("rooms").document(id) // <--- DÙNG ID CỦA BẠN TẠI ĐÂY

        roomRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Toast.makeText(this, "Mã phòng '$id' đã tồn tại! Vui lòng chọn mã khác.", Toast.LENGTH_SHORT).show()
            } else {
                // Nếu chưa có thì mới Lưu
                val room = CinemaRoom(id, name, type, rows, cols)

                // Dùng hàm .set() thay vì .add() để ép dùng ID này
                roomRef.set(room)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Thêm phòng thành công!", Toast.LENGTH_SHORT).show()
                        loadRooms()
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    // --- INNER CLASS ADAPTER (Để chung file cho gọn, hoặc tách ra tùy bạn) ---
    inner class RoomAdapter(private val list: List<CinemaRoom>) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {
        inner class RoomViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvRoomName)
            val tvInfo: TextView = itemView.findViewById(R.id.tvRoomInfo)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_room, parent, false)
            return RoomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
            val room = list[position]
            holder.tvName.text = room.name
            holder.tvInfo.text = "Loại: ${room.type} | Kích thước: ${room.totalRows}x${room.totalCols} (${room.getCapacity()} ghế)"
        }

        override fun getItemCount() = list.size
    }
}