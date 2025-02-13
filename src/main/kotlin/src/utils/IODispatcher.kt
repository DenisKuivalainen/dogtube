import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> ioOperation(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

suspend fun <T> ioOperationWithErrorHandling(
    defaultMsg: String, fn: suspend () -> T
): T {
    return try {
        ioOperation {
            fn()
        }
    } catch (e: Exception) {
        throw Exception(defaultMsg, e)
    }
}
