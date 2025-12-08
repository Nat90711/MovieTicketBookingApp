package com.example.movieticketbookingapp

object MockUserDatabase {
    // Danh sách người dùng (Ban đầu có sẵn 1 Admin và 1 User test)
    val users = mutableListOf(
        User("admin", "Quản trị viên", "admin@movie.com", "123456", "admin"),
        User("user1", "Nguyễn Văn A", "user@gmail.com", "123456", "user")
    )

    // Hàm kiểm tra đăng nhập
    fun login(username: String, password: String): User? {
        return users.find { it.username == username && it.password == password }
    }

    // Hàm kiểm tra trùng username
    fun isUsernameTaken(username: String): Boolean {
        return users.any { it.username == username }
    }

    // Hàm thêm user mới
    fun register(user: User) {
        users.add(user)
    }
}