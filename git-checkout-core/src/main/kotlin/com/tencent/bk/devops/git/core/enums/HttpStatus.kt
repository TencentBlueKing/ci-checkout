package com.tencent.bk.devops.git.core.enums

@SuppressWarnings("MagicNumber")
enum class HttpStatus(val statusCode: Int) {
    OK(200),
    HTTP_MOVED_PERM(301),
    HTTP_MOVED_TEMP(302),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500);
}
