package com.example.test.starter

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.pgclient.PgPool

import io.vertx.sqlclient.PoolOptions

import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlConnection


class MainVerticle : AbstractVerticle() {
  fun getDb() :PgPool {
    val connectOptions = PgConnectOptions()
      .setPort(5432)
      .setHost("localhost")
      .setDatabase("test")
      .setUser("postgres")

    // Pool options
    val poolOptions = PoolOptions()
      .setMaxSize(500)
    // Create the pooled client

    // Create the pooled client
    val client = PgPool.pool(vertx, connectOptions, poolOptions)
    return client
  }

  override fun start(startPromise: Promise<Void>) {
    val router = Router.router(vertx)
    val client = getDb()
    router["/"]
      .respond { ctx: RoutingContext ->
        val result = JsonObject()
        client.connection.compose { conn: SqlConnection ->
          println("Got a connection from the pool")
          conn
            .query("SELECT * FROM a")
            .execute()
            .compose { res: RowSet<Row?>? ->
              res?.forEach {
                println(it?.getString("value"))
              }
              conn
                .query("SELECT * FROM a")
                .execute()
            }
            .onComplete { ar: AsyncResult<RowSet<Row>>? ->
              // Release the connection to the pool
              conn.close()
            }
        }.onComplete { ar: AsyncResult<RowSet<Row>> ->
          if (ar.succeeded()) {
            println("Done")
            var x = 0
            ar.result().forEach {
              result.put("value$x", it.getString("value"))
              x++
            }
            ctx.response().end(result.toString())
          } else {
            println("Something went wrong " + ar.cause().message)
            result.put("result","fail")
            ctx.response().end(result.toString())
          }
        }
      }
    router["/a"]
      .respond { ctx: RoutingContext ->
        ctx
          .response()
          .end("hello world!")
      }

    vertx
      .createHttpServer().requestHandler(router).listen(8080)

  }
}
