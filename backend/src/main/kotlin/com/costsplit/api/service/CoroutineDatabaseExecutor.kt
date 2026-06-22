package com.costsplit.api.service

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Component
class CoroutineDatabaseExecutor(
    transactionManager: PlatformTransactionManager,
    @Qualifier("databaseDispatcher") private val databaseDispatcher: CoroutineDispatcher,
) {
    private val readTransaction = TransactionTemplate(transactionManager).apply {
        isReadOnly = true
    }
    private val writeTransaction = TransactionTemplate(transactionManager)

    suspend fun <T : Any> read(block: () -> T): T = withContext(databaseDispatcher) {
        readTransaction.execute { block() }
    }

    suspend fun <T : Any> write(block: () -> T): T = withContext(databaseDispatcher) {
        writeTransaction.execute { block() }
    }
}
