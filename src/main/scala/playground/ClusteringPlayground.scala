package playground

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object ClusteringPlayground extends App{
  def startNode(port: Int) = {
    val config = ConfigFactory.parseString(
      s"""
        |akka {
        |  actor {
        |    provider = cluster
        |  }
        |
        |  remote {
        |    artery {
        |      enabled = on
        |      transport = aeron-udp
        |      canonical.hostname = "localhost"
        |      canonical.port = $port
        |    }
        |  }
        |
        |  cluster {
        |    seed-nodes = ["akka://JAVTCluster@localhost:2551", "akka://JAVTCluster@localhost:2552"]
        |  }
        |}
        |""".stripMargin
    )
    ActorSystem("JAVTCluster", config)
  }

  (2551 to 2553).foreach(startNode)
}
