package com.braveberry.data_resource

sealed class ResourceFailure(cause: Throwable) : Exception(cause) {
    class PermissionRequired(cause: Throwable) : ResourceFailure(cause)
    class Unsupported(cause: Throwable) : ResourceFailure(cause)
    class ServiceUnavailable(cause: Throwable) : ResourceFailure(cause)
}