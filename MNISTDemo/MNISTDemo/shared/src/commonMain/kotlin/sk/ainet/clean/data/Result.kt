package sk.ainet.clean.data

/**
 * Result/Error model for repository/data-source operations (PRD §9).
 */
sealed class RepositoryError(message: String? = null, cause: Throwable? = null) : Exception(message, cause) {
    class NotFound(val modelId: String, msg: String = "Weights not found for modelId=$modelId") : RepositoryError(msg)
    class Io(msg: String, cause: Throwable? = null) : RepositoryError(msg, cause)
    class Parse(msg: String, cause: Throwable? = null) : RepositoryError(msg, cause)
    class Unknown(msg: String, cause: Throwable? = null) : RepositoryError(msg, cause)
}

/** Convenience exception wrapper for repository boundary to avoid leaking platform exceptions. */
class RepositoryException(val error: RepositoryError) : Exception(error.message, error)
