package com.kinetik.arsurvey.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: UploadJob)

    @Update
    suspend fun update(job: UploadJob)

    @Query("SELECT * FROM upload_jobs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<UploadJob>>

    @Query("SELECT * FROM upload_jobs ORDER BY createdAt DESC")
    suspend fun getAllOnce(): List<UploadJob>

    @Query("SELECT * FROM upload_jobs WHERE id = :id")
    suspend fun getById(id: String): UploadJob?

    @Query("SELECT * FROM upload_jobs WHERE id = :id")
    fun getByIdFlow(id: String): Flow<UploadJob?>

    @Query("SELECT * FROM upload_jobs WHERE status = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String): List<UploadJob>

    @Query("DELETE FROM upload_jobs WHERE id = :id")
    suspend fun delete(id: String)
}
