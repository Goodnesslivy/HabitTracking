package com.example.habittracking.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()

        // ✅ FIXED: Built using the correct, simplified modern builder structure
        val cacheSettings = PersistentCacheSettings.newBuilder()
            .setSizeBytes(100 * 1024 * 1024) // 100 MB maximum local disk allocation limit
            .build()

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(cacheSettings)
            .build()

        firestore.firestoreSettings = settings
        return firestore
    }
}
