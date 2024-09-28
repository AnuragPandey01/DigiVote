package xyz.droidev.eventsync.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/save-user-data")
    suspend fun postUserData(
        @Body
        userData: UserData
    ): GenerationResponse

    @GET("/political-parties")
    suspend fun getPoliticalParty(): List<PoliticalParty>

    @POST("/votes")
    suspend fun submitVote(
        @Body
        submitVoteBody: SubmitVoteBody
    )
}

data class PoliticalParty(
    val id: Int,
    val party_name: String,
    val party_code: String,
    val leader: String
)

data class UserData(
    val aadhaar: String,
    val full_name: String,
    val phone: String,
    val bounds: List<Float>,
    val face_points: List<FaceMeshPoint>,
    val triangle_indices: List<List<Int>>? = null
)
data class FaceMeshPoint(
    val index: Int,
    val position: List<Float>
)

data class SubmitVoteBody(
    val user_id: Int,
    val party_code: String,
    val party_name: String
)

data class GenerationResponse(
    val id: Int
)