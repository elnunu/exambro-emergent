package com.secureexam.browser.di

import com.secureexam.browser.data.repository.ExamRepositoryImpl
import com.secureexam.browser.data.repository.ViolationRepositoryImpl
import com.secureexam.browser.domain.repository.ExamRepository
import com.secureexam.browser.domain.repository.ViolationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindExamRepository(
        examRepositoryImpl: ExamRepositoryImpl
    ): ExamRepository

    @Binds
    @Singleton
    abstract fun bindViolationRepository(
        violationRepositoryImpl: ViolationRepositoryImpl
    ): ViolationRepository
}
