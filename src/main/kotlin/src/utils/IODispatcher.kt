import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> ioOperation(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
