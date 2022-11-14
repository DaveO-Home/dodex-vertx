package golf.handicap.routes

import io.vertx.rxjava3.ext.web.Router
import io.vertx.core.Promise

interface HandicapRoutes {

    abstract fun getVertxRouter(): Router
    abstract fun setRoutePromise(promise: Promise<Void>)
    abstract fun routes(router: Router): Router

}
