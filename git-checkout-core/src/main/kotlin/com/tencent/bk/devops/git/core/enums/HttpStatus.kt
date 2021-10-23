package com.tencent.bk.devops.git.core.enums

@SuppressWarnings("MagicNumber")
enum class HttpStatus(val statusCode: Int) {
    BAD_REQUEST(400),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500);
}
