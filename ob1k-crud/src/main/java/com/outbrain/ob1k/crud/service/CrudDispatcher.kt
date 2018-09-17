package com.outbrain.ob1k.crud.service

import com.google.gson.JsonObject
import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.Service
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.crud.dao.ICrudAsyncDao
import com.outbrain.ob1k.crud.model.Model
import com.outbrain.ob1k.server.netty.ResponseBuilder
import com.outbrain.ob1k.swagger.service.ISwaggerAware
import io.swagger.models.Swagger

class CrudDispatcher(private val registered: Map<String, ICrudAsyncDao<JsonObject>> = emptyMap(),
                     private val model: Model = Model()) : Service, ISwaggerAware {

    fun register(dao: ICrudAsyncDao<JsonObject>, model: Model) = CrudDispatcher(registered + (dao.resourceName() to dao), model)

    fun list(request: Request): ComposableFuture<Response> {
        val resource = request.getPathParam("resource")
        val range = (request.getQueryParam("range") ?: "[0,100000]").range()
        val filter = (request.getQueryParam("filter") ?: "{}").json()
        val sort = (request.getQueryParam("sort") ?: "[\"id\",\"ASC\"]").unqoutedPair()
        val dao = dao(resource)
        val future = filter.ids()?.let { dao.list(it) } ?: dao.list(range, sort, filter)
        return future.map { it.toResponse(range) }
    }

    fun get(request: Request): ComposableFuture<Response> {
        val resource = request.getPathParam("resource")
        val id = request.getPathParam("id")
        return dao(resource).read(id).map { it.toResponse() }
    }

    fun create(request: Request): ComposableFuture<Response> {
        val resource = request.getPathParam("resource")
        val json = request.requestBody.json().asJsonObject
        return dao(resource).create(json).map { it.toResponse() }
    }

    fun update(request: Request): ComposableFuture<Response> {
        val resource = request.getPathParam("resource")
        val id = request.getPathParam("id")
        val json = request.requestBody.json().asJsonObject
        return dao(resource).update(id, json).map { it.toResponse() }
    }

    fun delete(request: Request): ComposableFuture<Response> {
        val resource = request.getPathParam("resource")
        val id = request.getPathParam("id")
        return dao(resource).delete(id).map { ResponseBuilder.ok().build() }
    }

    override fun invoke(swagger: Swagger, key: String) = model.registerToSwagger(swagger, key)

    fun dao(resource: String) = registered[resource]!!
}