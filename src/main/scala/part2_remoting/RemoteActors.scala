package part2_remoting

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorSystem, Identify, Props}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object RemoteActors extends App {
  val localSystem = ActorSystem("LocalSystem", ConfigFactory.load("part2_remoting/remoteActors.conf"))
  val localSimpleActor = localSystem.actorOf(Props[SimpleActor], "localSimpleActor")
  localSimpleActor ! "hello, local actor!"

  // send a message to the REMOTE simple actor

  // Method 1: actor selection
  val remoteActorSelection = localSystem.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleActor")
  remoteActorSelection ! "hello from the \"local\" JVM"

  // Method 2: resolve the actor selection to an actor ref

  import localSystem.dispatcher // resolving an actor selection will return an a future
  implicit val timeout = Timeout(3 seconds)
  val remoteActorRefFuture = remoteActorSelection.resolveOne()
  remoteActorRefFuture.onComplete {
    case Success(actorRef) => actorRef ! "I've resolved you in a future"
    case Failure(exception) => println(s"I failed to resolved the remote actor because: $exception")
  }

  // Method 3: actor identification via messages (it's more actor friendly)
  /*
    - ActorResolver will ask for an actor selection from the local actor system
    - ActorResolver will send a special message called Identify with a small number to the actor selection
      e.g. Identify(38) // The value is not really important, but it's a correlation value.
    - the remote actor will AUTOMATICALLY respond with ActorIdentity(38, actorRef)
    - the actor resolver is free to use the remote actorRef
   */
  class ActorResolver extends Actor with ActorLogging {

    override def preStart(): Unit = {
      val selection = context.actorSelection("akka://RemoteSystem@localhost:2552/user/remoteSimpleActor")
      selection ! Identify(38)
    }

    override def receive: Receive = {
      case ActorIdentity(38, Some(actorRef)) =>
        actorRef ! "Thank you for identifying yourself"
    }
  }

  localSystem.actorOf(Props[ActorResolver], "localActorResolver")

}

object RemoteActors_Remote extends App {
  val remoteSystem = ActorSystem("RemoteSystem", ConfigFactory.load("part2_remoting/remoteActors.conf").getConfig("remoteSystem"))
  val remoteSimpleActor = remoteSystem.actorOf(Props[SimpleActor], "remoteSimpleActor")
  remoteSimpleActor ! "hello, remote actor"
}