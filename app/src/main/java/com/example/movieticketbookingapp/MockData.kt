package com.example.movieticketbookingapp

object MockData {

    // Đây là danh sách gốc, chứa TẤT CẢ phim
    private val allMovies = arrayListOf(
        Movie(1, "Zootopia 2", "https://upload.wikimedia.org/wikipedia/en/9/96/Zootopia_%28movie_poster%29.jpg", 9.0, "1h30m", "Animation/Adventure", "Zootopia 2 tiếp tục câu chuyện về bộ đôi cảnh sát Thỏ Judy Hopps và Cáo Nick Wilde..."),
        Movie(2, "Conan Movie 28", "https://upload.wikimedia.org/wikipedia/en/8/86/DetectiveConanOneeyedFlashback.jpg", 8.5, "1h50m", "Anime", "Trên núi tuyết Nagano, vụ tấn công tại Đài quan sát Nobeyama buộc Conan và các thám tử đào lại quá khứ. Thanh tra Kansuke đối mặt ký ức trận tuyết lở năm xưa, trong khi Kogoro nhận cuộc gọi hé lộ liên hệ với vụ án cũ. Sự xuất hiện của Takaaki, Amuro và cảnh sát Tokyo làm mọi thứ rối thêm. Bí ẩn chỉ có thể được giải khi Kansuke nhớ lại sự thật đã mất."),
        Movie(3, "Avatar: The Way of Water", "https://upload.wikimedia.org/wikipedia/en/5/54/Avatar_The_Way_of_Water_poster.jpg", 9.0, "1h30m", "Sci-Fi/Action", "Jake Sully sống cùng gia đình mới của mình trên hành tinh Pandora..."),
        Movie(4, "Oppenheimer", "https://upload.wikimedia.org/wikipedia/en/4/4a/Oppenheimer_%28film%29.jpg", 9.0, "3h00m", "Biography/Drama", "Bộ phim kể về cuộc đời của nhà vật lý lý thuyết J. Robert Oppenheimer..."),
        Movie(5, "Godzilla x Kong: The New Empire", "https://upload.wikimedia.org/wikipedia/en/b/be/Godzilla_x_kong_the_new_empire_poster.jpg", 8.5, "1h55m", "Action/Sci-F", "Kong và Godzilla phải đối mặt với một mối đe dọa tử thần ẩn sâu trong lòng Trái Đất..."),
        Movie(6, "Kung Fu Panda 4", "https://upload.wikimedia.org/wikipedia/en/7/7f/Kung_Fu_Panda_4_poster.jpg"),
        Movie(7, "Dune: Part Two", "https://upload.wikimedia.org/wikipedia/en/5/52/Dune_Part_Two_poster.jpeg"),
        Movie(8, "Inside Out 2", "https://upload.wikimedia.org/wikipedia/en/f/f7/Inside_Out_2_poster.jpg")
    )

    fun getAllMovies(): ArrayList<Movie> {
        return allMovies
    }

    fun getNowShowing(): List<Movie> {
        return allMovies.take(5)
    }

    fun getComingSoon(): List<Movie> {
        return allMovies.drop(5)
    }

    fun addMovie(movie: Movie) {
        // Thêm vào vị trí đầu tiên (index 0) để khi quay lại Dashboard thấy ngay
        allMovies.add(0, movie)
    }

    fun deleteMovie(movie: Movie) {
        allMovies.remove(movie)
    }

    fun updateMovie(updatedMovie: Movie) {
        // Tìm vị trí của phim cần sửa dựa vào ID
        val index = allMovies.indexOfFirst { it.id == updatedMovie.id }

        if (index != -1) {
            // Thay thế phim cũ bằng phim mới
            allMovies[index] = updatedMovie
        }
    }
}