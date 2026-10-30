package com.rohit.spendsense

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // Flow means the screen updates automatically whenever the data changes.
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(transaction: Transaction)

    @Insert
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)


    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}