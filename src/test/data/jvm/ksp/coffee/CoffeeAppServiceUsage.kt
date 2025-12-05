package coffee

import java.util.ServiceLoader

class CoffeeAppServiceUsage {
  val service = ServiceLoader.load(CoffeeAppService::class.java).findFirst().get()
}
