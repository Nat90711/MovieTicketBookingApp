package com.example.movieticketbookingapp.ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
        // Gọi loadRooms ở onResume để khi sửa xong quay lại list tự cập nhật
        // loadRooms() -> Đã chuyển xuống onResume

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnAddRoom.setOnClickListener {
            showAddRoomDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadRooms()
    }

    private fun setupRecyclerView() {
        rvRooms.layoutManager = LinearLayoutManager(this)

        // Khởi tạo Adapter với 2 sự kiện: Click (Sửa) và Delete (Xóa)
        adapter = RoomAdapter(
            roomList,
            onItemClick = { selectedRoom ->
                // --- LOGIC SỬA/CẤU HÌNH (Giữ nguyên) ---
                val intent = Intent(this, RoomConfigActivity::class.java)
                intent.putExtra("room_id", selectedRoom.roomId)
                intent.putExtra("room_name", selectedRoom.roomName)
                intent.putExtra("room_type", selectedRoom.type)
                intent.putExtra("rows", selectedRoom.totalRows)
                intent.putExtra("cols", selectedRoom.totalCols)
                intent.putExtra("is_edit_mode", true)

                // Truyền danh sách cấu hình ghế hiện tại sang
                val configArrayList = ArrayList(selectedRoom.seatConfiguration)
                intent.putIntegerArrayListExtra("seat_config", configArrayList)

                startActivity(intent)
            },
            onDeleteClick = { roomToDelete ->
                // --- LOGIC XÓA ---
                showDeleteConfirmationDialog(roomToDelete)
            }
        )

        rvRooms.adapter = adapter
    }

    private fun loadRooms() {
        db.collection("rooms")
            .get()
            .addOnSuccessListener { result ->
                roomList.clear()
                for (document in result) {
                    val room = document.toObject(CinemaRoom::class.java)
                    room.roomId = document.id
                    roomList.add(room)
                }
                adapter.notifyDataSetChanged()
            }
    }

    // --- HỘP THOẠI XÁC NHẬN XÓA ---
    private fun showDeleteConfirmationDialog(room: CinemaRoom) {
        AlertDialog.Builder(this)
            .setTitle("Xóa Phòng")
            .setMessage("Bạn có chắc chắn muốn xóa '${room.roomName}' không?\nLưu ý: Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa") { _, _ ->
                performDeleteRoom(room)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // --- THỰC HIỆN XÓA TRÊN FIREBASE ---
    private fun performDeleteRoom(room: CinemaRoom) {
        // RÀNG BUỘC 1: Kiểm tra xem phòng có đang được sử dụng cho suất chiếu nào không
        db.collection("showtimes")
            .whereEqualTo("roomId", room.roomId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Nếu tìm thấy suất chiếu -> CHẶN XÓA
                    showErrorDialog("Không thể xóa phòng này!",
                        "Phòng '${room.roomName}' đang có ${documents.size()} suất chiếu đã lên lịch.\nVui lòng xóa hoặc đổi phòng cho các suất chiếu đó trước.")
                } else {
                    // Nếu không có suất chiếu -> TIẾN HÀNH XÓA
                    deleteRoomFromFirestore(room)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kiểm tra dữ liệu: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteRoomFromFirestore(room: CinemaRoom) {
        db.collection("rooms").document(room.roomId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa phòng thành công!", Toast.LENGTH_SHORT).show()
                loadRooms()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi khi xóa: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setIcon(R.drawable.ic_warning) // Nhớ tạo icon warning hoặc dùng null
            .setPositiveButton("Đã hiểu", null)
            .show()
    }

    private fun showAddRoomDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_room, null)
        val etId = dialogView.findViewById<EditText>(R.id.etRoomId)
        val etName = dialogView.findViewById<EditText>(R.id.etRoomName)
        val etType = dialogView.findViewById<EditText>(R.id.etRoomType)
        val etRows = dialogView.findViewById<EditText>(R.id.etRows)
        val etCols = dialogView.findViewById<EditText>(R.id.etCols)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        dialogView.findViewById<Button>(R.id.btnSaveRoom).setOnClickListener {
            val id = etId.text.toString().trim()
            val name = etName.text.toString().trim()
            val type = etType.text.toString().trim()
            val rowsStr = etRows.text.toString().trim()
            val colsStr = etCols.text.toString().trim()

            if (id.isEmpty() || name.isEmpty() || type.isEmpty() || rowsStr.isEmpty() || colsStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rows = rowsStr.toIntOrNull() ?: 0
            val cols = colsStr.toIntOrNull() ?: 0

            if (rows <= 0 || cols <= 0) {
                Toast.makeText(this, "Số hàng và cột phải lớn hơn 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Đóng dialog nhập liệu
            dialog.dismiss()

            // Chuyển sang màn hình Cấu hình
            val intent = Intent(this, RoomConfigActivity::class.java)
            intent.putExtra("room_id", id)
            intent.putExtra("room_name", name)
            intent.putExtra("room_type", type)
            intent.putExtra("rows", rows)
            intent.putExtra("cols", cols)
            intent.putExtra("is_edit_mode", false)

            startActivity(intent)
        }
    }

    // --- ADAPTER CẬP NHẬT (HỖ TRỢ XÓA) ---
    inner class RoomAdapter(
        private val list: List<CinemaRoom>,
        private val onItemClick: (CinemaRoom) -> Unit,
        private val onDeleteClick: (CinemaRoom) -> Unit
    ) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

        inner class RoomViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvRoomName)
            val tvInfo: TextView = itemView.findViewById(R.id.tvRoomInfo)
            val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteRoom) // Ánh xạ nút xóa
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_room, parent, false)
            return RoomViewHolder(view)
        }

        override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
            val room = list[position]
            holder.tvName.text = room.roomName
            holder.tvInfo.text = "Loại: ${room.type} | Kích thước: ${room.totalRows}x${room.totalCols} (${room.getCapacity()} ghế)"

            // Sự kiện click vào cả dòng -> Sửa
            holder.itemView.setOnClickListener {
                onItemClick(room)
            }

            // Sự kiện click vào nút xóa -> Xóa
            holder.btnDelete.setOnClickListener {
                onDeleteClick(room)
            }
        }

        override fun getItemCount() = list.size
    }
}