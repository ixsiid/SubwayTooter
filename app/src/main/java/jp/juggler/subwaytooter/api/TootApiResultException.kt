package jp.juggler.subwaytooter.api

class TootApiResultException(val result: TootApiResult?) :
    Exception(result?.error ?: "cancelled.") {
    constructor(error: String) : this(TootApiResult(error))
}

fun errorApiResult(result: TootApiResult?): Nothing =
    throw TootApiResultException(result)

fun errorApiResult(error: String): Nothing =
    throw TootApiResultException(error)
