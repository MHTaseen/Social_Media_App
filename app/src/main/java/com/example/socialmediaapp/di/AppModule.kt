package com.example.socialmediaapp.di

import android.content.Context
import com.example.socialmediaapp.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    fun provideFirebaseAuth() = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore() = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseStorage() = FirebaseStorage.getInstance()
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth, 
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): AuthRepository = 
        AuthRepositoryImpl(auth, firestore, context)

    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): UserRepository = 
        UserRepositoryImpl(firestore, context)

    @Provides
    @Singleton
    fun providePostRepository(firestore: FirebaseFirestore): PostRepository = 
        PostRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideStorageRepository(storage: FirebaseStorage): StorageRepository = 
        StorageRepositoryImpl(storage)

    @Provides
    @Singleton
    fun provideMessageRepository(firestore: FirebaseFirestore): MessageRepository = 
        MessageRepositoryImpl(firestore)

    @Provides
    @Singleton
    fun provideReportRepository(firestore: FirebaseFirestore): ReportRepository = 
        ReportRepositoryImpl(firestore)
}