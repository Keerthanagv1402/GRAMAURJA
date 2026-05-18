package com.gramaUrja.data

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.gramaUrja.data.local.GramaUrjaDatabase
import com.gramaUrja.data.local.PowerHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GramaUrjaDatabase =
        Room.databaseBuilder(context, GramaUrjaDatabase::class.java, "grama_urja.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePowerHistoryDao(db: GramaUrjaDatabase): PowerHistoryDao =
        db.powerHistoryDao()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase =
        FirebaseDatabase.getInstance("https://gramaurja-744ba-default-rtdb.asia-southeast1.firebasedatabase.app")

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance()
}
