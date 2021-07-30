package ru.bozaro.gitlfs.client.internal

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.AbstractHttpEntity
import org.apache.http.entity.ByteArrayEntity
import ru.bozaro.gitlfs.common.Constants.HEADER_ACCEPT
import ru.bozaro.gitlfs.common.Constants.MIME_LFS_JSON
import ru.bozaro.gitlfs.common.data.DeleteLockReq
import ru.bozaro.gitlfs.common.data.DeleteLockRes
import ru.bozaro.gitlfs.common.data.Lock
import ru.bozaro.gitlfs.common.data.Ref
import java.io.IOException

class LockDelete(private val force: Boolean, private val ref: Ref?) : Request<Lock?> {
    @Throws(JsonProcessingException::class)
    override fun createRequest(mapper: ObjectMapper, url: String): LfsRequest {
        val req = HttpPost(url)
        req.addHeader(HEADER_ACCEPT, MIME_LFS_JSON)
        val createLockReq = DeleteLockReq(force, ref)
        val entity: AbstractHttpEntity = ByteArrayEntity(mapper.writeValueAsBytes(createLockReq))
        entity.setContentType(MIME_LFS_JSON)
        req.entity = entity
        return LfsRequest(req, entity)
    }

    @Throws(IOException::class)
    override fun processResponse(mapper: ObjectMapper, response: HttpResponse): Lock? {
        return when (response.statusLine.statusCode) {
            HttpStatus.SC_OK -> mapper.readValue(response.entity.content, DeleteLockRes::class.java).lock
            HttpStatus.SC_NOT_FOUND -> null
            else -> throw IllegalStateException()
        }
    }

    override fun statusCodes(): IntArray {
        return intArrayOf(
                HttpStatus.SC_OK,
                HttpStatus.SC_NOT_FOUND
        )
    }
}
