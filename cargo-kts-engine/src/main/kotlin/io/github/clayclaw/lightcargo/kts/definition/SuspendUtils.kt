package io.github.clayclaw.lightcargo.kts.definition

import java.util.concurrent.CountDownLatch
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun <T> runSuspendBlocking(block: suspend () -> T): T {
    val latch = CountDownLatch(1)
    var completedResult: Result<T>? = null

    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(result: Result<T>) {
            completedResult = result
            latch.countDown()
        }
    })

    latch.await()
    return completedResult
        ?.getOrThrow()
        ?: throw IllegalStateException("Suspend block did not complete")
}
