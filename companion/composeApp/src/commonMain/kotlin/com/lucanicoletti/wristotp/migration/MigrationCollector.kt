package com.lucanicoletti.wristotp.migration

import com.lucanicoletti.wristotp.otp.ImportedAccount

class MigrationCollector {
    private val batches = mutableMapOf<Int, MigrationBatch>()
    private var batchId: Int? = null
    private var batchSize = 0

    sealed interface Progress {
        data class Waiting(val scanned: Int, val total: Int) : Progress
        data class Complete(val accounts: List<ImportedAccount>) : Progress
        data object DifferentExport : Progress
    }

    fun add(batch: MigrationBatch): Progress {
        val currentId = batchId
        if (currentId != null && (currentId != batch.batchId || batchSize != batch.batchSize)) {
            return Progress.DifferentExport
        }
        batchId = batch.batchId
        batchSize = batch.batchSize
        batches[batch.batchIndex] = batch
        if (batches.size < batchSize) {
            return Progress.Waiting(scanned = batches.size, total = batchSize)
        }
        return Progress.Complete(batches.entries.sortedBy { it.key }.flatMap { it.value.accounts })
    }

    fun reset() {
        batches.clear()
        batchId = null
        batchSize = 0
    }
}
