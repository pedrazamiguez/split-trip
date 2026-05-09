package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.pedrazamiguez.splittrip.data.local.entity.CurrencyEntity

@Dao
interface CurrencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencies(currencies: List<CurrencyEntity>)

    @Query("SELECT * FROM currencies ORDER BY code ASC")
    suspend fun getCurrencies(): List<CurrencyEntity>
}
